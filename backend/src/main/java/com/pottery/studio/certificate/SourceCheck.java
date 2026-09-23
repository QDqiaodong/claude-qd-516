package com.pottery.studio.certificate;

/**
 * 来源链上的一段核对结果。
 * 段：COURSE 课程 / GREENWARE 坯体 / CLAY 泥料 / GLAZE 釉料 / BATCH 烧成批次 / KILN 窑炉 / LINK 关联一致性。
 */
public class SourceCheck {

    /** OK 完整 / MISSING 来源缺失（引用对象查不到）/ CONFLICT 关联冲突（引用都在但对不上） */
    public static final String OK = "OK";
    public static final String MISSING = "MISSING";
    public static final String CONFLICT = "CONFLICT";

    private String segment;
    private String segmentName;
    private String status;
    /** 缺的是哪段来源 / 冲突点是什么，明确讲给工作人员 */
    private String message;

    public SourceCheck() {
    }

    public SourceCheck(String segment, String segmentName, String status, String message) {
        this.segment = segment;
        this.segmentName = segmentName;
        this.status = status;
        this.message = message;
    }

    public static SourceCheck ok(String segment, String segmentName) {
        return new SourceCheck(segment, segmentName, OK, "完整一致");
    }

    public static SourceCheck missing(String segment, String segmentName, String message) {
        return new SourceCheck(segment, segmentName, MISSING, message);
    }

    public static SourceCheck conflict(String segment, String segmentName, String message) {
        return new SourceCheck(segment, segmentName, CONFLICT, message);
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
