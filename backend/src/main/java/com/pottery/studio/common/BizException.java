package com.pottery.studio.common;

/** 业务异常：统一由 GlobalExceptionHandler 转成 {ok:false, message:"..."} */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
