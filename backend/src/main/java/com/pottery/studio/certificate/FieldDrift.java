package com.pottery.studio.certificate;

/**
 * 一个字段在“凭证快照值”和“当前业务数据值”之间的差异，
 * 用于工作人员发起更正时逐项对照。
 */
public class FieldDrift {

    /** 字段编码 */
    private String field;

    /** 字段中文名 */
    private String label;

    /** 所属分组：作品 / 课程 / 坯体 / 材料 / 烧成 */
    private String group;

    /** 凭证上当时的内容（旧版快照） */
    private String snapshotValue;

    /** 当前可核实的内容 */
    private String currentValue;

    /** 是否发生变化 */
    private boolean changed;

    public FieldDrift() {
    }

    public FieldDrift(String field, String label, String group,
                      String snapshotValue, String currentValue, boolean changed) {
        this.field = field;
        this.label = label;
        this.group = group;
        this.snapshotValue = snapshotValue;
        this.currentValue = currentValue;
        this.changed = changed;
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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSnapshotValue() {
        return snapshotValue;
    }

    public void setSnapshotValue(String snapshotValue) {
        this.snapshotValue = snapshotValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
