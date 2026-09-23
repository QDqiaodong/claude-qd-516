package com.pottery.studio.certificate;

import java.util.List;

/**
 * 作品凭证聚合视图：当前版本 + 全部历史版本 + 来源核对 + 当前资料与快照的差异。
 * 作品详情页凭证面板和打印页都基于它。
 */
public class CertificateAggregate {

    /** 作品主键 */
    private Long artworkId;
    /** 是否存在任何版本（升级前的老作品可能为 false） */
    private boolean issued;
    /** 当前版本；从未签发时为 null */
    private FiringCertificate current;
    /** 全部版本（含历史版，按版本号升序） */
    private List<FiringCertificate> versions;
    /** 最近一次签发/读取时数据行的版本（当前凭证 id），用于并发控制 */
    private Long currentId;
    /** 实时来源链核对结果 */
    private ProvenanceView provenance;
    /** 当前资料是否与当前凭证快照有出入（课程/材料/作品资料被修正过时为 true） */
    private boolean sourceChanged;
    /** 逐字段差异，用于更正对照 */
    private List<FieldDiff> diffs;

    public Long getArtworkId() {
        return artworkId;
    }

    public void setArtworkId(Long artworkId) {
        this.artworkId = artworkId;
    }

    public boolean isIssued() {
        return issued;
    }

    public void setIssued(boolean issued) {
        this.issued = issued;
    }

    public FiringCertificate getCurrent() {
        return current;
    }

    public void setCurrent(FiringCertificate current) {
        this.current = current;
    }

    public List<FiringCertificate> getVersions() {
        return versions;
    }

    public void setVersions(List<FiringCertificate> versions) {
        this.versions = versions;
    }

    public Long getCurrentId() {
        return currentId;
    }

    public void setCurrentId(Long currentId) {
        this.currentId = currentId;
    }

    public ProvenanceView getProvenance() {
        return provenance;
    }

    public void setProvenance(ProvenanceView provenance) {
        this.provenance = provenance;
    }

    public boolean isSourceChanged() {
        return sourceChanged;
    }

    public void setSourceChanged(boolean sourceChanged) {
        this.sourceChanged = sourceChanged;
    }

    public List<FieldDiff> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<FieldDiff> diffs) {
        this.diffs = diffs;
    }
}
