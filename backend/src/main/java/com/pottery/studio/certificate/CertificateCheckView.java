package com.pottery.studio.certificate;

import java.util.List;

/**
 * 签发前核对结果：逐段列出作品 → 坯体 → 材料 / 烧成批次 → 窑炉的来源链。
 * ready=false 时禁止签发，blockingSegments 明确指出缺的是哪一段、哪里冲突；
 * pendingSnapshot 仅为“若现在签发将写入”的预览，缺失段对应快照列为空，
 * 在未签发前它不是凭证，不能被当作正式凭证展示或打印。
 */
public class CertificateCheckView {

    private Long artworkId;

    private String artworkCode;

    private String artworkTitle;

    /** 全部来源段核对通过、批次已出窑，才允许签发 */
    private boolean ready;

    /** 全部来源段（含通过、缺失、冲突） */
    private List<SegmentCheck> segments;

    /** 不通过的段（缺失 / 冲突），用于页面红字逐条提示 */
    private List<SegmentCheck> blockingSegments;

    /** 当前可核实数据拼出的待签发快照（预览用，尚未落库，不是正式凭证） */
    private FiringCertificate pendingSnapshot;

    public Long getArtworkId() {
        return artworkId;
    }

    public void setArtworkId(Long artworkId) {
        this.artworkId = artworkId;
    }

    public String getArtworkCode() {
        return artworkCode;
    }

    public void setArtworkCode(String artworkCode) {
        this.artworkCode = artworkCode;
    }

    public String getArtworkTitle() {
        return artworkTitle;
    }

    public void setArtworkTitle(String artworkTitle) {
        this.artworkTitle = artworkTitle;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public List<SegmentCheck> getSegments() {
        return segments;
    }

    public void setSegments(List<SegmentCheck> segments) {
        this.segments = segments;
    }

    public List<SegmentCheck> getBlockingSegments() {
        return blockingSegments;
    }

    public void setBlockingSegments(List<SegmentCheck> blockingSegments) {
        this.blockingSegments = blockingSegments;
    }

    public FiringCertificate getPendingSnapshot() {
        return pendingSnapshot;
    }

    public void setPendingSnapshot(FiringCertificate pendingSnapshot) {
        this.pendingSnapshot = pendingSnapshot;
    }
}
