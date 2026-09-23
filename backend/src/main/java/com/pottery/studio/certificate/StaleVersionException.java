package com.pottery.studio.certificate;

import com.pottery.studio.common.BizException;

/**
 * 凭证版本冲突：工作人员拿着旧页面发起签发/更正，但服务器上的当前版本已被别人更新。
 * 携带服务器最新版本号，前端必须提示"版本已变化，请重新读取后处理"，禁止把旧内容当当前版。
 */
public class StaleVersionException extends BizException {

    private final Long serverCurrentId;
    private final Integer serverCurrentVersion;

    public StaleVersionException(String message, Long serverCurrentId, Integer serverCurrentVersion) {
        super(message);
        this.serverCurrentId = serverCurrentId;
        this.serverCurrentVersion = serverCurrentVersion;
    }

    public Long getServerCurrentId() {
        return serverCurrentId;
    }

    public Integer getServerCurrentVersion() {
        return serverCurrentVersion;
    }
}
