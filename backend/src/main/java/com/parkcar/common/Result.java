package com.parkcar.common;

import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class Result<T> {

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_BAD_REQUEST = 40000;
    public static final int CODE_UNAUTHORIZED = 40100;
    public static final int CODE_FORBIDDEN = 40300;
    public static final int CODE_NOT_FOUND = 40400;
    public static final int CODE_CONFLICT = 40900;
    public static final int CODE_ERROR = 50000;

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = CODE_SUCCESS;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
