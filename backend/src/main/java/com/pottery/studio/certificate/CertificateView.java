package com.pottery.studio.certificate;

import java.util.List;

/**
 * 作品详情中凭证面板的数据：
 * - currentVersion：当前有效版本（没有凭证时为 null，作品仍可照常查看流转）
 * - history：历史版本（SUPERSEDED，版本号倒序）
 * - check：当前业务数据的来源核对结果（用于判断能否补签 / 更正）
 * - drift：当前版本快照 vs 当前业务数据的逐项差异（资料是否被修正过）
 * 旧版本快照直接来自落库行，因此课程、材料、作品被修正后旧凭证仍显示原内容。
 */
public class CertificateView {

    /** 当前版本，未签发时为 null */
    private FiringCertificate currentVersion;

    /** 历史版本（不含当前版），版本号倒序 */
    private List<FiringCertificate> history;

    /** 是否已存在任何版本（区分“从未签发”与“已签发待更正”） */
    private boolean everIssued;

    /** 当前数据来源链核对结果 */
    private CertificateCheckView check;

    /** 当前版本与当前数据的字段差异；无当前版本时为 null */
    private List<FieldDrift> drift;

    /** 差异字段数（>0 表示资料自当前版签发后被修正过） */
    private int driftCount;

    public FiringCertificate getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(FiringCertificate currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<FiringCertificate> getHistory() {
        return history;
    }

    public void setHistory(List<FiringCertificate> history) {
        this.history = history;
    }

    public boolean isEverIssued() {
        return everIssued;
    }

    public void setEverIssued(boolean everIssued) {
        this.everIssued = everIssued;
    }

    public CertificateCheckView getCheck() {
        return check;
    }

    public void setCheck(CertificateCheckView check) {
        this.check = check;
    }

    public List<FieldDrift> getDrift() {
        return drift;
    }

    public void setDrift(List<FieldDrift> drift) {
        this.drift = drift;
    }

    public int getDriftCount() {
        return driftCount;
    }

    public void setDriftCount(int driftCount) {
        this.driftCount = driftCount;
    }
}
