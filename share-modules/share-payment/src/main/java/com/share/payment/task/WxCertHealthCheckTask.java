package com.share.payment.task;

import com.share.payment.config.WxPayConfig;
import com.share.payment.utils.WxPayUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 微信支付证书健康检查
 *
 * <p>定期检查商户私钥和微信平台证书的有效性，
 * 在证书即将过期时提前告警。</p>
 *
 * @author share
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxCertHealthCheckTask {

    private final WxPayConfig wxPayConfig;
    private final WxPayUtil wxPayUtil;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY = "payment:task:cert-check";

    /** 证书过期预警阈值（天） */
    private static final int WARN_DAYS = 30;

    /**
     * 启动时立即检查一次
     */
    @PostConstruct
    public void checkOnStartup() {
        if (wxPayConfig.isMockMode()) {
            log.info("Mock 模式，跳过证书健康检查（上线前改为 mockMode=false）");
            return;
        }
        try {
            boolean ok = checkCertificates();
            if (!ok) {
                log.error("=== 微信支付证书检查不通过，支付功能可能异常 ===");
            }
        } catch (Exception e) {
            log.error("证书健康检查异常", e);
        }
    }

    /**
     * 每周日凌晨 3 点检查一次
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void scheduledCheck() {
        if (wxPayConfig.isMockMode()) {
            return;
        }
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return;
            }
            boolean ok = checkCertificates();
            if (!ok) {
                log.error("=== 微信支付证书即将过期，请立即更新 ===");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 检查证书状态
     *
     * @return true=正常 false=有证书即将过期
     */
    private boolean checkCertificates() {
        boolean allOk = true;

        // 1. 检查商户私钥文件
        String privateKeyPath = wxPayConfig.getPrivateKeyPath();
        if (privateKeyPath == null || privateKeyPath.isEmpty()) {
            log.error("商户私钥路径未配置: wx.pay.v3.private-key-path");
            allOk = false;
        } else {
            java.io.File keyFile = new java.io.File(privateKeyPath);
            if (!keyFile.exists()) {
                log.error("商户私钥文件不存在: {}", privateKeyPath);
                allOk = false;
            } else {
                log.info("商户私钥文件存在: {}", privateKeyPath);
            }
        }

        // 2. 尝试刷新并检查平台证书
        try {
            wxPayUtil.checkCertificateExpiry(WARN_DAYS);
            log.info("微信平台证书检查通过");
        } catch (Exception e) {
            log.error("微信平台证书检查失败: {}", e.getMessage());
            allOk = false;
        }

        return allOk;
    }
}
