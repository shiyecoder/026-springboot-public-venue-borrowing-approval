package com.grad.venue.common;

import lombok.Data;

/** 统一响应体：业务码 + 提示 + 数据 + 请求编号 */
@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;
    private String traceId;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.msg = "success";
        r.data = data;
        r.traceId = String.valueOf(System.currentTimeMillis());
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.traceId = String.valueOf(System.currentTimeMillis());
        return r;
    }
}
