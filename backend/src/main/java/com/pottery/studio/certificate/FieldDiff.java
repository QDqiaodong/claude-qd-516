package com.pottery.studio.certificate;

/** 单个字段的"当前凭证快照 vs 业务表现值"差异，用于更正时对照。 */
public class FieldDiff {

    private String field;
    private String label;
    /** 旧值：当前版本凭证里的快照内容 */
    private String oldValue;
    /** 新值：业务表当前可核实的值 */
    private String newValue;

    public FieldDiff() {
    }

    public FieldDiff(String field, String label, String oldValue, String newValue) {
        this.field = field;
        this.label = label;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
