package com.pottery.studio.certificate;

/**
 * 一条来源链段的核对结果（课程 / 坯体 / 泥料 / 釉料 / 批次 / 窑炉 / 关联一致性）。
 * ok=false 时必须能指出缺的是哪一段、为什么冲突，不能静默拼成一张看似完整的凭证。
 */
public class SegmentCheck {

    /** 段编码：COURSE / GREENWARE / CLAY / GLAZE / BATCH / KILN / LINK_GW_BATCH / LINK_BATCH_OUT */
    private String code;

    /** 段名称，如“来源坯体” */
    private String label;

    /** OK 通过 / MISSING 来源缺失 / CONFLICT 关联冲突 */
    private String state;

    /** 该段当前核对到的内容描述（用于页面展示） */
    private String detail;

    /** 不通过时的具体原因，明确指出缺哪段、哪两边对不上 */
    private String message;

    public SegmentCheck() {
    }

    public SegmentCheck(String code, String label, String state, String detail, String message) {
        this.code = code;
        this.label = label;
        this.state = state;
        this.detail = detail;
        this.message = message;
    }

    public static SegmentCheck ok(String code, String label, String detail) {
        return new SegmentCheck(code, label, "OK", detail, null);
    }

    public static SegmentCheck missing(String code, String label, String message) {
        return new SegmentCheck(code, label, "MISSING", null, message);
    }

    public static SegmentCheck conflict(String code, String label, String detail, String message) {
        return new SegmentCheck(code, label, "CONFLICT", detail, message);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
