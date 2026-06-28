package com.share.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 全链路 TraceId 过滤器
 *
 * <p>在网关入口为每个请求注入 TraceId，贯穿整个调用链。
 * 优先级最高（在 AuthFilter 之前），确保鉴权日志也带有 TraceId。</p>
 *
 * <p>配合各服务的 {@code TraceMdcFilter} 和 Feign {@code TraceFeignInterceptor}，
 * 实现 TraceId 跨服务传播。</p>
 *
 * @author share
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    /** HTTP Header 名称，与各服务保持一致 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // 注入请求头，下游 Feign 和 Filter 都会读取
        ServerHttpRequest mutated = request.mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();
        log.debug("TraceId={}", traceId);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
