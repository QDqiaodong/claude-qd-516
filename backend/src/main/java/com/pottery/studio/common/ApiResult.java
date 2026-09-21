package com.pottery.studio.common;

import java.util.List;

/** 统一响应包装：成功 {ok:true,data:...}，失败 {ok:false,message:"..."} */
public class ApiResult<T> {

    private boolean ok = true;
    private String message;
    private T data;

    public ApiResult() {
    }

    public ApiResult(boolean ok, String message, T data) {
        this.ok = ok;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, null, data);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(false, message, null);
    }

    public static ApiResult<List<?>> emptyList() {
        return new ApiResult<>(true, null, List.of());
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
