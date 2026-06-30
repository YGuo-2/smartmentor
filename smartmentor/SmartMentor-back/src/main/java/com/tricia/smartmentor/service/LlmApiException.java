package com.tricia.smartmentor.service;

/**
 * 大模型 HTTP 调用异常，携带状态码与是否可重试标志。
 * <p>
 * 用于区分确定性错误（4xx，如密钥错误/非法请求）与瞬时错误（5xx/超时/429/输出截断）：
 * 前者重试与跨提供商回退都无意义，应立即失败；后者才值得重试或回退。
 */
public class LlmApiException extends RuntimeException {

    /** HTTP 状态码；0 表示非 HTTP 层错误（如输出被截断 finish_reason=length）。 */
    private final int statusCode;
    /** 是否值得重试 / 回退到备用提供商。 */
    private final boolean retryable;

    public LlmApiException(int statusCode, boolean retryable, String message) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * 按 HTTP 状态码判定是否可重试：
     * 4xx 视为确定性错误不重试，但 408（请求超时）与 429（限流）例外；5xx 与其它一律可重试。
     */
    public static boolean isRetryableStatus(int code) {
        if (code == 408 || code == 429) {
            return true;
        }
        return code < 400 || code >= 500;
    }
}
