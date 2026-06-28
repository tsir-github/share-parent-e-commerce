package com.share.common.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId MDC 过滤器
 *
 * <p>从请求头提取 Gateway 注入的 TraceId，写入 MDC，
 * 使当前服务的所有日志都携带该 TraceId。</p>
 *
 * <p>优先级最高，确保后续的 HeaderInterceptor 日志也能关联。</p>
 *
 * @author share
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceMdcFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        try {
            MDC.put(MDC_KEY, traceId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
