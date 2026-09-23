package com.pottery.studio.certificate;

/** 首签请求体：签发人 + 页面读取时看到的当前凭证 id（首签应为 null），用于并发版本保护。 */
public class CertificateIssueRequest {

    private String issuedBy;
    private Long expectedCurrentId;

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Long getExpectedCurrentId() {
        return expectedCurrentId;
    }

    public void setExpectedCurrentId(Long expectedCurrentId) {
        this.expectedCurrentId = expectedCurrentId;
    }
}
