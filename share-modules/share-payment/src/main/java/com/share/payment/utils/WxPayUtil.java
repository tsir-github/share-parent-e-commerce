package com.share.payment.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.common.core.constant.ServiceNameConstants;
import com.share.common.core.utils.uuid.IdUtils;
import com.share.payment.config.WxPayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayUtil {

    private final WxPayConfig wxPayConfig;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** 微信平台证书缓存：serial -> X509Certificate */
    private final ConcurrentHashMap<String, X509Certificate> certificateCache = new ConcurrentHashMap<>();

    /** 最后刷新证书的时间戳 */
    private volatile long lastCertRefreshTime = 0;
    private static final long CERT_REFRESH_INTERVAL = 3600_000L; // 1 hour

    public String createRefund(String orderNo, Integer amount, String reason) {
        String url = wxPayConfig.getApiBase() + "/v3/refund/domestic/refunds";
        String refundNo = orderNo + "_" + IdUtils.fastSimpleUUID().substring(0, 8);
        String body = String.format(
                "{\"out_trade_no\":\"%s\",\"out_refund_no\":\"%s\",\"reason\":\"%s\"," +
                        "\"notify_url\":\"%s\"," +
                        "\"amount\":{\"refund\":%d,\"total\":%d,\"currency\":\"CNY\"}}",
                orderNo, refundNo,
                reason != null ? reason.replace("\"", "\\\"") : "",
                wxPayConfig.getNotifyUrl(),
                amount, amount
        );
        return sendHttpPost(url, body);
    }

    public String createOrder(String orderNo, Integer amount, String description, String openid) {
        String url = wxPayConfig.getApiBase() + "/v3/pay/transactions/jsapi";
        String body = String.format(
                "{\"appid\":\"%s\",\"mchid\":\"%s\",\"description\":\"%s\"," +
                        "\"out_trade_no\":\"%s\",\"notify_url\":\"%s\"," +
                        "\"amount\":{\"total\":%d,\"currency\":\"CNY\"}," +
                        "\"payer\":{\"openid\":\"%s\"}}",
                wxPayConfig.getAppId(), wxPayConfig.getMchId(), description,
                orderNo, wxPayConfig.getNotifyUrl(),
                amount, openid
        );
        String response = sendHttpPost(url, body);
        return extractPrepayId(response);
    }

    public Map<String, String> buildPayParams(String prepayId) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = IdUtils.fastSimpleUUID();
        String packageStr = "prepay_id=" + prepayId;
        String signStr = wxPayConfig.getAppId() + "\n" + timestamp + "\n" + nonceStr + "\n" + packageStr + "\n";
        String paySign = sign(signStr);
        return Map.of(
                "appId", wxPayConfig.getAppId(),
                "timeStamp", timestamp,
                "nonceStr", nonceStr,
                "package", packageStr,
                "signType", "RSA",
                "paySign", paySign
        );
    }

    public String verifyAndDecryptCallback(String serial, String signature,
                                           String timestamp, String nonce, String body) {
        String signStr = timestamp + "\n" + nonce + "\n" + body + "\n";
        if (!verifySignature(signStr, signature, serial)) {
            throw new SecurityException("微信回调签名校验失败");
        }
        return decryptBody(body);
    }

    private String sendHttpPost(String url, String body) {
        try {
            String token = buildToken("POST", url, body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "WECHATPAY2-SHA256-RSA2048 " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", ServiceNameConstants.PAYMENT_SERVICE)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 && response.statusCode() != 204) {
                log.error("微信支付 API 请求失败: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("微信支付 API 请求失败, status=" + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            log.error("发送 HTTP 请求异常: url={}", url, e);
            throw new RuntimeException("微信支付 HTTP 请求异常", e);
        }
    }

    private String sendHttpGet(String url) {
        try {
            String token = buildToken("GET", url, null);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "WECHATPAY2-SHA256-RSA2048 " + token)
                    .header("Accept", "application/json")
                    .header("User-Agent", ServiceNameConstants.PAYMENT_SERVICE)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.error("微信支付 GET 请求失败: status={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("微信支付 GET 请求失败, status=" + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            log.error("发送 GET 请求异常: url={}", url, e);
            throw new RuntimeException("微信支付 HTTP 请求异常", e);
        }
    }

    private String buildToken(String method, String url, String body) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = IdUtils.fastSimpleUUID();
        String uriPath = URI.create(url).getPath();
        String signStr = method + "\n" + uriPath + "\n" + timestamp + "\n" + nonce + "\n" + (body == null ? "" : body) + "\n";
        String signature = sign(signStr);
        return String.format(
                "mchid=\"%s\",nonce_str=\"%s\",timestamp=\"%s\",serial=\"%s\",signature=\"%s\"",
                wxPayConfig.getMchId(), nonce, timestamp, wxPayConfig.getCertSerial(), signature
        );
    }

    private String sign(String signStr) {
        try {
            PrivateKey privateKey = loadPrivateKey();
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(signStr.getBytes(StandardCharsets.UTF_8));
            byte[] signature = sign.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            log.error("生成微信支付签名失败", e);
            throw new RuntimeException("微信支付签名失败", e);
        }
    }

    private boolean verifySignature(String signStr, String signature, String serial) {
        try {
            X509Certificate certificate = getWechatCertificate(serial);
            if (certificate == null) {
                log.warn("未找到微信证书: serial={}", serial);
                return false;
            }
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(certificate.getPublicKey());
            sign.update(signStr.getBytes(StandardCharsets.UTF_8));
            return sign.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.error("微信回调验签异常", e);
            return false;
        }
    }

    // ==================== 证书获取与缓存 ====================

    /**
     * 获取微信平台证书（按序列号）。缓存不存在或过期时自动刷新。
     */
    private X509Certificate getWechatCertificate(String serial) {
        X509Certificate cert = certificateCache.get(serial);
        if (cert != null) {
            return cert;
        }
        // 缓存未命中，尝试刷新
        refreshCertificates();
        return certificateCache.get(serial);
    }

    /**
     * 从微信 API 刷新平台证书列表并解密缓存
     */
    void refreshCertificates() {
        long now = System.currentTimeMillis();
        if (now - lastCertRefreshTime < CERT_REFRESH_INTERVAL && !certificateCache.isEmpty()) {
            return; // 限频
        }
        try {
            String url = wxPayConfig.getApiBase() + "/v3/certificates";
            String responseBody = sendHttpGet(url);
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataArray = root.get("data");
            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode node : dataArray) {
                    String serial = node.get("serial_no").asText();
                    JsonNode encrypted = node.get("encrypt_certificate");
                    String algorithm = encrypted.get("algorithm").asText();
                    String ciphertext = encrypted.get("ciphertext").asText();
                    String nonce = encrypted.get("nonce").asText();
                    String associatedData = encrypted.get("associated_data").asText();

                    if ("AEAD_AES_256_GCM".equals(algorithm)) {
                        String pem = aesDecryptToString(ciphertext, nonce, associatedData);
                        X509Certificate cert = parsePemCertificate(pem);
                        certificateCache.put(serial, cert);
                        log.info("微信平台证书已加载: serial={}", serial);
                    }
                }
            }
            lastCertRefreshTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("刷新微信平台证书失败", e);
        }
    }

    // ==================== AES-256-GCM 加解密 ====================

    /**
     * 解密微信回调 body 中的 resource 加密数据
     * <pre>
     * {
     *   "resource": {
     *     "algorithm": "AEAD_AES_256_GCM",
     *     "ciphertext": "...",
     *     "nonce": "...",
     *     "associated_data": "..."
     *   }
     * }
     * </pre>
     */
    private String decryptBody(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode resource = root.get("resource");
            if (resource == null) {
                log.warn("回调 body 无 resource 字段，直接返回原文");
                return body;
            }
            String ciphertext = resource.get("ciphertext").asText();
            String nonce = resource.get("nonce").asText();
            String associatedData = resource.has("associated_data")
                    ? resource.get("associated_data").asText() : "";
            return aesDecryptToString(ciphertext, nonce, associatedData);
        } catch (Exception e) {
            log.error("解密回调数据失败", e);
            throw new SecurityException("回调数据解密失败", e);
        }
    }

    /**
     * AES-256-GCM 解密并返回字符串
     *
     * @param ciphertext     base64 编码的密文
     * @param nonce          base64 编码的 nonce (12 bytes)
     * @param associatedData 附加认证数据 (AAD)
     * @return 解密后的明文字符串
     */
    private String aesDecryptToString(String ciphertext, String nonce, String associatedData) {
        try {
            byte[] key = wxPayConfig.getApiKey().getBytes(StandardCharsets.UTF_8);
            byte[] cipherData = Base64.getDecoder().decode(ciphertext);
            byte[] nonceBytes = Base64.getDecoder().decode(nonce);
            byte[] aadBytes = associatedData.getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonceBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            cipher.updateAAD(aadBytes);
            byte[] plaintext = cipher.doFinal(cipherData);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-256-GCM 解密失败", e);
            throw new RuntimeException("AES 解密失败", e);
        }
    }

    // ==================== 证书解析 ====================

    private X509Certificate parsePemCertificate(String pem) {
        try {
            String content = pem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(content);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(encoded));
        } catch (Exception e) {
            log.error("解析 PEM 证书失败", e);
            throw new RuntimeException("解析微信平台证书失败", e);
        }
    }

    // ==================== 证书健康检查 ====================

    /**
     * 检查所有缓存的微信平台证书是否即将过期
     *
     * @param warnDays 提前多少天预警
     */
    public void checkCertificateExpiry(int warnDays) {
        refreshCertificates();
        if (certificateCache.isEmpty()) {
            log.warn("证书缓存为空，已触发刷新但仍无证书");
            return;
        }
        long warnMs = (long) warnDays * 24 * 3600 * 1000;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, X509Certificate> entry : certificateCache.entrySet()) {
            X509Certificate cert = entry.getValue();
            Date notAfter = cert.getNotAfter();
            long remaining = notAfter.getTime() - now;
            if (remaining < warnMs) {
                log.error("微信平台证书即将过期！serial={}, 过期时间={}, 剩余={}天",
                        entry.getKey(), notAfter, remaining / (24 * 3600 * 1000));
            } else {
                log.info("微信平台证书正常: serial={}, 过期时间={}", entry.getKey(), notAfter);
            }
        }
    }

    // ==================== 商户私钥 ====================

    private PrivateKey loadPrivateKey() {
        try {
            String privateKeyPath = wxPayConfig.getPrivateKeyPath();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(privateKeyPath), StandardCharsets.UTF_8))) {
                String content = reader.lines().collect(Collectors.joining("\n"));
                String pem = content
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] encoded = Base64.getDecoder().decode(pem);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(keySpec);
            }
        } catch (Exception e) {
            log.error("加载商户私钥失败: {}", wxPayConfig.getPrivateKeyPath(), e);
            throw new RuntimeException("加载微信支付私钥失败", e);
        }
    }

    private String extractPrepayId(String response) {
        if (response != null && response.contains("\"prepay_id\"")) {
            int start = response.indexOf("\"prepay_id\"") + 12;
            start = response.indexOf('"', start) + 1;
            int end = response.indexOf('"', start);
            if (start > 0 && end > start) {
                return response.substring(start, end);
            }
        }
        throw new RuntimeException("统一下单响应解析失败: " + response);
    }
}
