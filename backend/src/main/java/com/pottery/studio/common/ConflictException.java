package com.pottery.studio.common;

/**
 * 版本冲突异常：打开页面后凭证已被其他工作人员更新，
 * 旧页面持有的版本号不再是当前版本，必须重新读取后处理。
 * HTTP 409 Conflict，区别于普通业务校验失败。
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
