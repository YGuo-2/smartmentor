package com.tricia.smartmentor.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final Object data;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    public BusinessException(Integer code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}
