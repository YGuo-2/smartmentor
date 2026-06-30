package com.tricia.smartmentor.config;

import com.tricia.smartmentor.util.RedisUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    /** 默认限流：每用户 60秒内最多30次请求 */
    private static final int DEFAULT_MAX_COUNT = 30;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    /** Chat stream 限流：每用户 60秒内最多10次请求 */
    private static final int CHAT_MAX_COUNT = 10;
    private static final int CHAT_WINDOW_SECONDS = 60;

    public RateLimitInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();
        String userId = getUserId(request);

        int maxCount;
        int windowSeconds;

        // Chat stream 更严格的限流
        if (uri.contains("/api/chat/stream")) {
            maxCount = CHAT_MAX_COUNT;
            windowSeconds = CHAT_WINDOW_SECONDS;
        } else {
            maxCount = DEFAULT_MAX_COUNT;
            windowSeconds = DEFAULT_WINDOW_SECONDS;
        }

        String key = "rate_limit:" + userId + ":" + uri;

        try {
            if (!redisUtil.isAllowed(key, maxCount, windowSeconds)) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
                return false;
            }
        } catch (Exception e) {
            // Redis 不可用时放行，不影响正常使用
        }

        return true;
    }

    private String getUserId(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            return principal.getName();
        }
        // 未认证请求使用 IP
        String ip = request.getHeader("X-Forwarded-For");
        return ip != null ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }
}
