package com.parkcar.common;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(Result.CODE_BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(Result.CODE_UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(Result.CODE_FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(Result.CODE_NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(Result.CODE_CONFLICT, message);
    }
}
