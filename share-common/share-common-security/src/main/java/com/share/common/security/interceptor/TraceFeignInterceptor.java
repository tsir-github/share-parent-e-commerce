package com.share.common.security.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Feign TraceId 传播拦截器
 *
 * <p>Feign 调用时，自动从当前线程的 MDC 中取出 TraceId，
 * 注入到请求头，确保跨服务调用链路不断。</p>
 *
 * @author share
 */
@Component
public class TraceFeignInterceptor implements RequestInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(MDC_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TRACE_ID_HEADER, traceId);
        }
    }
}
