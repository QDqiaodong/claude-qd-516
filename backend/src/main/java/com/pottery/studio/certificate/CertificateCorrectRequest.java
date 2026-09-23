package com.pottery.studio.certificate;

/** 更正请求体：签发人、更正原因、页面读取时看到的当前凭证 id（乐观锁）。 */
public class CertificateCorrectRequest {

    private String issuedBy;
    private String reason;
    private Long expectedCurrentId;

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getExpectedCurrentId() {
        return expectedCurrentId;
    }

    public void setExpectedCurrentId(Long expectedCurrentId) {
        this.expectedCurrentId = expectedCurrentId;
    }
}
