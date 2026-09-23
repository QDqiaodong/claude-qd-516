package com.pottery.studio.certificate;

import com.pottery.studio.common.BizException;
import com.pottery.studio.course.Artwork;
import com.pottery.studio.course.ArtworkRepository;
import com.pottery.studio.course.ArtworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 烧成履历凭证端到端测试（H2 + 真实 schema.sql 种子数据）。
 * 覆盖：首签快照、来源缺失拦截、关联冲突拦截、资料变化差异、版本追加/旧版留存、
 * 并发版本保护、升级前遗留作品无凭证照常流转。
 */
@SpringBootTest
class FiringCertificateServiceTest {

    @Autowired
    private FiringCertificateService certificateService;
    @Autowired
    private FiringCertificateRepository certificateRepository;
    @Autowired
    private ArtworkRepository artworkRepository;
    @Autowired
    private ArtworkService artworkService;

    /** 种子里的正常作品 AW-0001：坯体 GW-0002（粗陶泥、无釉）、素烧批次 FB-20260320 */
    private static final long GOOD_ARTWORK_ID = 1L;
    /** AW-0008：未施釉坯体挂到釉烧批次 —— 关联冲突 */
    private static final long CONFLICT_ARTWORK_ID = 8L;
    /** AW-0009：来源坯体 id=999 查不到 —— 来源缺失 */
    private static final long MISSING_ARTWORK_ID = 9L;

    /** 每个用例前清掉凭证并还原会被修改的作品字段，保证用例相互独立 */
    @BeforeEach
    void resetState() {
        certificateRepository.deleteAll();
        Artwork good = artworkRepository.findById(GOOD_ARTWORK_ID).orElseThrow();
        good.setTitle("素心茶盏");
        good.setOwnerStatus("CONSIGN");
        good.setConsignPrice(new java.math.BigDecimal("268.00"));
        good.setShelfNo("S-01");
        artworkRepository.save(good);
        Artwork missing = artworkRepository.findById(MISSING_ARTWORK_ID).orElseThrow();
        missing.setOwnerStatus("TAKEN");
        missing.setConsignPrice(null);
        missing.setShelfNo(null);
        artworkRepository.save(missing);
    }

    // ---------- 1. 遗留作品：无凭证照常查看；首签从当前数据生成快照 ----------

    @Test
    void legacyArtworkStartsWithoutCertificate_andIssueCreatesV1Snapshot() {
        // 升级前已登记的作品：没有凭证，但查看聚合正常
        CertificateAggregate before = certificateService.getAggregate(GOOD_ARTWORK_ID);
        assertFalse(before.isIssued());
        assertNull(before.getCurrent());
        assertTrue(before.getProvenance().isIssuable());

        // 首次补签：从当前可核实数据生成，不重建历史
        CertificateAggregate after = certificateService.issue(GOOD_ARTWORK_ID, "周师傅", null);
        assertTrue(after.isIssued());
        assertEquals(1, after.getCurrent().getVersionNo());
        assertEquals(FiringCertificate.CURRENT, after.getCurrent().getStatus());
        assertEquals("周师傅", after.getCurrent().getIssuedBy());

        // 快照内容核对（作品 / 课程 / 坯体 / 泥料 / 批次 / 温度时间）
        FiringCertificate v1 = after.getCurrent();
        assertEquals("AW-0001", v1.getArtworkCode());
        assertEquals("素心茶盏", v1.getArtworkTitle());
        assertEquals("王小舟", v1.getStudentName());
        assertEquals("C-2601", v1.getCourseCode());
        assertEquals("周六拉坯体验课", v1.getCourseTitle());
        assertEquals("GW-0002", v1.getGreenwareCode());
        assertEquals("M-1001", v1.getClayCode());
        assertEquals("宜兴粗陶泥", v1.getClayName());
        assertNull(v1.getGlazeId());
        assertEquals("FB-20260320", v1.getBatchNo());
        assertEquals("BISQUE", v1.getFireType());
        assertEquals(800, v1.getTargetTemp());
        assertEquals(805, v1.getPeakTemp());
        assertNotNull(v1.getLoadedAt());
        assertNotNull(v1.getOutAt());
    }

    // ---------- 2. 来源缺失：指出缺的是哪段，不能生成完整凭证 ----------

    @Test
    void missingGreenwareBlocksIssue_andNamesMissingSegment() {
        CertificateAggregate agg = certificateService.getAggregate(MISSING_ARTWORK_ID);
        assertFalse(agg.getProvenance().isIssuable());
        assertTrue(agg.getProvenance().isGreenwareMissing());
        assertTrue(agg.getProvenance().getBlockMessage().contains("坯体"));
        assertTrue(agg.getProvenance().getChecks().stream()
                .anyMatch(c -> "GREENWARE".equals(c.getSegment())
                        && SourceCheck.MISSING.equals(c.getStatus())));

        BizException ex = assertThrows(BizException.class,
                () -> certificateService.issue(MISSING_ARTWORK_ID, "周师傅", null));
        assertTrue(ex.getMessage().contains("缺【坯体】"));
        assertEquals(0, certificateRepository.countByArtworkId(MISSING_ARTWORK_ID),
                "来源缺失时不得留下任何凭证行");
    }

    // ---------- 3. 关联冲突：坯体与批次对不上，拦截 ----------

    @Test
    void unglazedGreenwareOnGlazeBatchBlocksIssue() {
        CertificateAggregate agg = certificateService.getAggregate(CONFLICT_ARTWORK_ID);
        assertFalse(agg.getProvenance().isIssuable());
        assertTrue(agg.getProvenance().isLinkConflict());
        BizException ex = assertThrows(BizException.class,
                () -> certificateService.issue(CONFLICT_ARTWORK_ID, "周师傅", null));
        assertTrue(ex.getMessage().contains("釉烧"));
        assertTrue(ex.getMessage().contains("没有施釉记录"));
    }

    // ---------- 4. 资料修正后旧凭证保持原内容；更正追加新版本，差异可见 ----------

    @Test
    void correctionAppendsVersionAndKeepsOldSnapshot() {
        // 先首签
        certificateService.issue(GOOD_ARTWORK_ID, "周师傅", null);
        FiringCertificate v1 = certificateService.getAggregate(GOOD_ARTWORK_ID).getCurrent();
        String oldTitle = v1.getArtworkTitle();
        assertEquals("素心茶盏", oldTitle);

        // 工作人员修正作品名称（课程/材料修正同理，走差异对比）
        Artwork artwork = artworkRepository.findById(GOOD_ARTWORK_ID).orElseThrow();
        artwork.setTitle("素心茶盏（修正款）");
        artworkRepository.save(artwork);

        CertificateAggregate changed = certificateService.getAggregate(GOOD_ARTWORK_ID);
        assertTrue(changed.isSourceChanged());
        assertTrue(changed.getDiffs().stream().anyMatch(d -> "作品名称".equals(d.getLabel())
                && "素心茶盏".equals(d.getOldValue())
                && "素心茶盏（修正款）".equals(d.getNewValue())));

        // 无差异更正应被拒绝
        // 先直接更正一次成功追加 V2
        CertificateAggregate corrected = certificateService.correct(
                GOOD_ARTWORK_ID, "林老师", "作品名称补录", v1.getId());
        assertEquals(2, corrected.getCurrent().getVersionNo());
        assertEquals("素心茶盏（修正款）", corrected.getCurrent().getArtworkTitle());
        assertEquals("作品名称补录", corrected.getCurrent().getChangeReason());

        // V1 仍保留且标记为历史版，内容是旧名称
        FiringCertificate old = certificateService.getVersion(GOOD_ARTWORK_ID, 1);
        assertEquals(FiringCertificate.SUPERSEDED, old.getStatus());
        assertEquals("素心茶盏", old.getArtworkTitle(), "旧凭证必须保留签发当时的快照内容");

        // 共两行版本，页面只能拿到一个 CURRENT
        List<FiringCertificate> all = certificateRepository.findByArtworkIdOrderByVersionNoAsc(GOOD_ARTWORK_ID);
        assertEquals(2, all.size());
        assertEquals(1, all.stream().filter(c -> FiringCertificate.CURRENT.equals(c.getStatus())).count());

        // 再发起无差异更正 → 拒绝，不产生新版本
        Long v2Id = corrected.getCurrent().getId();
        BizException noDiff = assertThrows(BizException.class,
                () -> certificateService.correct(GOOD_ARTWORK_ID, "林老师", "无变化测试", v2Id));
        assertTrue(noDiff.getMessage().contains("没有可更正的差异"));
        assertEquals(2, certificateRepository.countByArtworkId(GOOD_ARTWORK_ID));
    }

    // ---------- 5. 并发：拿着旧版本号提交被拒，必须重新读取 ----------

    @Test
    void staleExpectedVersionIsRejected() {
        certificateService.issue(GOOD_ARTWORK_ID, "周师傅", null);
        FiringCertificate v1 = certificateService.getAggregate(GOOD_ARTWORK_ID).getCurrent();

        // 另一人已把作品名改掉，工作人员 A 仍拿着 V1 的 id 更正
        Artwork artwork = artworkRepository.findById(GOOD_ARTWORK_ID).orElseThrow();
        artwork.setTitle("并发改名");
        artworkRepository.save(artwork);
        certificateService.correct(GOOD_ARTWORK_ID, "工作人员B", "B 已更正", v1.getId());

        // A 的旧页面再拿 v1.getId() 提交 → StaleVersionException，不能把旧内容设为当前
        StaleVersionException ex = assertThrows(StaleVersionException.class,
                () -> certificateService.correct(GOOD_ARTWORK_ID, "工作人员A", "A 的过时提交", v1.getId()));
        assertEquals(2, ex.getServerCurrentVersion());
        assertNotNull(ex.getServerCurrentId());
        assertTrue(ex.getMessage().contains("凭证版本已变化"));

        // 首签接口的并发保护：已存在凭证时再首签也被拒
        assertThrows(BizException.class,
                () -> certificateService.issue(GOOD_ARTWORK_ID, "工作人员A", null));
    }

    // ---------- 6. 遗留作品无凭证也能照常流转（改归属不追溯来源） ----------

    @Test
    void legacyArtworkCanStillChangeOwnerWithoutCertificate() {
        // 来源缺失的 AW-0009：改归属流转不应被凭证/来源核对拦截
        Artwork missing = artworkRepository.findById(MISSING_ARTWORK_ID).orElseThrow();
        missing.setShelfNo("S-09");
        artworkRepository.save(missing);
        Artwork moved = artworkService.changeOwner(MISSING_ARTWORK_ID, "CONSIGN", new java.math.BigDecimal("199.00"));
        assertEquals("CONSIGN", moved.getOwnerStatus());
        moved = artworkService.changeOwner(MISSING_ARTWORK_ID, "TAKEN", null);
        assertEquals("TAKEN", moved.getOwnerStatus());
        assertFalse(certificateService.getAggregate(MISSING_ARTWORK_ID).isIssued());
    }
}
