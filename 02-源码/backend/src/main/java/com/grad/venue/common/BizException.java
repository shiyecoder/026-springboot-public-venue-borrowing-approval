package com.grad.venue.common;

/** 业务异常：由全局异常处理器转成统一响应，code 非 0 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
