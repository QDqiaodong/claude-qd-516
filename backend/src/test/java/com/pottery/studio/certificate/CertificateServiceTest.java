package com.pottery.studio.certificate;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.ConflictException;
import com.pottery.studio.course.Artwork;
import com.pottery.studio.course.ArtworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 烧成履历凭证核心规则集成测试（H2 内存库）：
 * 来源核对 / 缺失与冲突拦截 / 快照防篡改 / 更正追加不覆盖 / 旧版本可追溯 /
 * 老作品无凭证可流转可补签 / 409 版本冲突。
 */
@SpringBootTest
@ActiveProfiles("test")
class CertificateServiceTest {

    @Autowired private CertificateService certificateService;
    @Autowired private ArtworkService artworkService;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDateTime T = LocalDateTime.of(2026, 3, 20, 9, 0);

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM firing_certificate");
        jdbc.update("DELETE FROM artwork");
        jdbc.update("DELETE FROM greenware");
        jdbc.update("DELETE FROM firing_batch");
        jdbc.update("DELETE FROM kiln");
        jdbc.update("DELETE FROM material");
        jdbc.update("DELETE FROM course");

        jdbc.update("INSERT INTO material(id,category_id,code,name,kind,stock_kg,firing_temp,unit_price,safety_stock,status,created_at,updated_at) "
                + "VALUES (1,3,'M-1001','宜兴粗陶泥','CLAY',120,1180,12,20,'NORMAL',NOW(),NOW())");
        jdbc.update("INSERT INTO material(id,category_id,code,name,kind,stock_kg,unit_price,safety_stock,status,created_at,updated_at) "
                + "VALUES (6,11,'M-2001','透明釉','GLAZE',40,45,10,'NORMAL',NOW(),NOW())");
        jdbc.update("INSERT INTO kiln(id,code,name,kiln_type,max_temp,volume_l,status,created_at,updated_at) "
                + "VALUES (1,'K-01','1号电窑','ELECTRIC',1300,120,'IDLE',NOW(),NOW())");
        // 已出窑批次（作品 1 用）
        jdbc.update("INSERT INTO firing_batch(id,batch_no,kiln_id,fire_type,stage,target_temp,peak_temp,greenware_count,"
                + "loaded_at,heating_at,soaking_at,cooling_at,out_at,created_at,updated_at) VALUES "
                + "(1,'FB-1',1,'GLAZE','OUT',1220,1218,6,?,?,?,?,?,NOW(),NOW())",
                T, T.plusHours(6), T.plusHours(11), T.plusHours(18), T.plusHours(24));
        // 未出窑批次（冲突场景用）
        jdbc.update("INSERT INTO firing_batch(id,batch_no,kiln_id,fire_type,stage,target_temp,peak_temp,greenware_count,"
                + "loaded_at,created_at,updated_at) VALUES "
                + "(2,'FB-2',1,'GLAZE','HEATING',1280,null,9,?,NOW(),NOW())", T);
        jdbc.update("INSERT INTO course(id,code,title,teacher,level,capacity,enrolled,price,status,created_at,updated_at) "
                + "VALUES (1,'C-1','周六拉坯体验课','陈师傅','BEGINNER',12,12,168,'OPEN',NOW(),NOW())");
        // 坯体 6：施釉，出窑后 firing_batch_id 已清空
        jdbc.update("INSERT INTO greenware(id,code,name,clay_id,glaze_id,area_id,stage,moisture,firing_batch_id,shaped_at,created_at,updated_at) "
                + "VALUES (6,'GW-0006','青瓷碟',1,6,7,'FINISHED',2.1,NULL,?,NOW(),NOW())", T.minusDays(30));
        // 坯体 7：无釉
        jdbc.update("INSERT INTO greenware(id,code,name,clay_id,glaze_id,area_id,stage,moisture,firing_batch_id,shaped_at,created_at,updated_at) "
                + "VALUES (7,'GW-0007','粗陶罐',1,NULL,7,'FINISHED',1.5,NULL,?,NOW(),NOW())", T.minusDays(29));
        // 坯体 8：当前仍挂在批次 2（与作品批次冲突场景用）
        jdbc.update("INSERT INTO greenware(id,code,name,clay_id,glaze_id,area_id,stage,moisture,firing_batch_id,shaped_at,created_at,updated_at) "
                + "VALUES (8,'GW-0008','冲突坯',1,6,7,'FIRING',1.0,2,?,NOW(),NOW())", T.minusDays(28));

        artwork(1L, "AW-0001", "青瓷碟", "李楠", 1L, 6L, 1L, "TAKEN");
    }

    private void artwork(Long id, String code, String title, String student, Long course, Long gw, Long batch, String owner) {
        jdbc.update("INSERT INTO artwork(id,code,title,student_name,course_id,greenware_id,firing_batch_id,owner_status,finished_at,created_at,updated_at) "
                + "VALUES (?,?,?,?,?,?,?,?,?,NOW(),NOW())", id, code, title, student, course, gw, batch, owner, T);
    }

    // 1. 来源齐全：核对通过并可签发，快照内容完整
    @Test
    void ready_and_issue_snapshot() {
        CertificateCheckView check = certificateService.precheck(1L);
        assertTrue(check.isReady());
        assertTrue(check.getBlockingSegments().isEmpty());

        FiringCertificate v1 = certificateService.issue(1L, "张三");
        assertEquals(1, v1.getVersionNo());
        assertEquals(FiringCertificate.CURRENT, v1.getStatus());
        assertEquals("AW-0001-V1", v1.getCertificateNo());
        assertEquals("青瓷碟", v1.getSnapTitle());
        assertEquals("李楠", v1.getSnapStudentName());
        assertEquals("周六拉坯体验课", v1.getSnapCourseTitle());
        assertEquals("GW-0006", v1.getSnapGreenwareCode());
        assertEquals("宜兴粗陶泥", v1.getSnapClayName());
        assertEquals("透明釉", v1.getSnapGlazeName());
        assertEquals("FB-1", v1.getSnapBatchNo());
        assertEquals(1220, v1.getSnapTargetTemp());
        assertEquals(1218, v1.getSnapPeakTemp());
        assertNotNull(v1.getSnapOutAt());
        assertEquals("张三", v1.getIssuedBy());
    }

    // 2. 无釉坯体：釉料段为正常“未施釉”，不阻断签发
    @Test
    void no_glaze_is_ok() {
        artwork(2L, "AW-0002", "粗陶罐", "赵晓", 1L, 7L, 1L, "TAKEN");
        assertTrue(certificateService.precheck(2L).isReady());
        FiringCertificate v = certificateService.issue(2L, "张三");
        assertNull(v.getSnapGlazeName());
        assertNull(v.getSnapGlazeCode());
    }

    // 3. 缺少来源坯体：不能签发，并指出缺的是坯体（及依赖其的泥料段）
    @Test
    void missing_greenware_blocks_and_names_segment() {
        artwork(3L, "AW-0003", "缺坯体", "王", 1L, 999L, 1L, "TAKEN");
        CertificateCheckView check = certificateService.precheck(3L);
        assertFalse(check.isReady());
        assertTrue(check.getBlockingSegments().stream().anyMatch(s ->
                "GREENWARE".equals(s.getCode()) && "MISSING".equals(s.getState())
                        && s.getMessage().contains("坯体")));
        assertTrue(check.getBlockingSegments().stream().anyMatch(s -> "CLAY".equals(s.getCode())));
        // 缺失时返回的是预览快照，坯体段为空，且绝无已落库凭证
        assertNull(check.getPendingSnapshot().getSnapGreenwareCode());
        BizException ex = assertThrows(BizException.class, () -> certificateService.issue(3L, "张三"));
        assertTrue(ex.getMessage().contains("坯体"));
    }

    // 4. 批次未出窑：关联冲突，不能签发
    @Test
    void batch_not_out_conflict() {
        artwork(4L, "AW-0004", "未出窑", "孙", 1L, 6L, 2L, "TAKEN");
        CertificateCheckView check = certificateService.precheck(4L);
        assertFalse(check.isReady());
        SegmentCheck seg = check.getBlockingSegments().stream()
                .filter(s -> "LINK_BATCH_OUT".equals(s.getCode())).findFirst().orElseThrow();
        assertEquals("CONFLICT", seg.getState());
        assertTrue(seg.getMessage().contains("尚未出窑"));
        assertThrows(BizException.class, () -> certificateService.issue(4L, "张三"));
    }

    // 5. 坯体当前所在批次与作品批次不一致：关联冲突
    @Test
    void greenware_batch_mismatch_conflict() {
        artwork(5L, "AW-0005", "批次冲突", "周", 1L, 8L, 1L, "TAKEN");
        CertificateCheckView check = certificateService.precheck(5L);
        SegmentCheck seg = check.getBlockingSegments().stream()
                .filter(s -> "LINK_GW_BATCH".equals(s.getCode())).findFirst().orElseThrow();
        assertEquals("CONFLICT", seg.getState());
        assertTrue(seg.getMessage().contains("不一致"));
        assertFalse(check.isReady());
    }

    // 6. 资料被修正后：旧凭证仍显示原内容；差异被检出；更正追加新版本、旧版转历史且原内容保留
    @Test
    void snapshot_immutable_and_correct_appends() {
        FiringCertificate v1 = certificateService.issue(1L, "张三");
        assertEquals("青瓷碟", v1.getSnapTitle());

        // 模拟之后作品改名、泥料改名、批次实际峰值温度补录
        jdbc.update("UPDATE artwork SET title='青瓷碟（修补）' WHERE id=1");
        jdbc.update("UPDATE material SET name='宜兴粗陶泥（特选）' WHERE id=1");
        jdbc.update("UPDATE firing_batch SET peak_temp=1222 WHERE id=1");

        CertificateView view = certificateService.getView(1L);
        // 当前版快照仍是旧内容，不被当前数据覆盖
        assertEquals("青瓷碟", view.getCurrentVersion().getSnapTitle());
        assertEquals("宜兴粗陶泥", view.getCurrentVersion().getSnapClayName());
        assertEquals(1218, view.getCurrentVersion().getSnapPeakTemp());
        assertTrue(view.getDriftCount() >= 3);
        assertTrue(view.getDrift().stream().anyMatch(d -> "title".equals(d.getField()) && d.isChanged()
                && "青瓷碟".equals(d.getSnapshotValue()) && "青瓷碟（修补）".equals(d.getCurrentValue())));

        // 用旧版本号发起更正 → 生成 V2
        FiringCertificate v2 = certificateService.correct(1L, 1, "李四", "作品名称与峰值温度订正");
        assertEquals(2, v2.getVersionNo());
        assertEquals(FiringCertificate.CURRENT, v2.getStatus());
        assertEquals("青瓷碟（修补）", v2.getSnapTitle());
        assertEquals(1222, v2.getSnapPeakTemp());
        assertEquals("李四", v2.getIssuedBy());
        assertTrue(v2.getChangeReason().contains("订正"));

        // 旧版转为历史版，原内容仍可原样取出
        FiringCertificate oldV1 = certificateService.getVersion(1L, v1.getId());
        assertEquals(FiringCertificate.SUPERSEDED, oldV1.getStatus());
        assertEquals("青瓷碟", oldV1.getSnapTitle());
        assertEquals(1218, oldV1.getSnapPeakTemp());

        CertificateView after = certificateService.getView(1L);
        assertEquals(2, after.getCurrentVersion().getVersionNo());
        assertEquals(1, after.getHistory().size());
        assertEquals(1, after.getHistory().get(0).getVersionNo());
    }

    // 7. 已签当前版不能重复签发
    @Test
    void cannot_issue_twice() {
        certificateService.issue(1L, "张三");
        assertThrows(BizException.class, () -> certificateService.issue(1L, "李四"));
    }

    // 8. 版本号过期：旧页面更正返回 409，不允许用旧数据覆盖
    @Test
    void stale_version_conflict_409() {
        certificateService.issue(1L, "张三");
        jdbc.update("UPDATE artwork SET title='改名后' WHERE id=1");
        certificateService.correct(1L, 1, "李四", "改名");
        // 另一人页面还停留在 V1，用 expectedVersionNo=1 再更正 → 409
        ConflictException ex = assertThrows(ConflictException.class,
                () -> certificateService.correct(1L, 1, "王五", "重复更正"));
        assertTrue(ex.getMessage().contains("V2"));
        // 仍只有一条当前版 V2
        assertEquals(2, certificateService.getView(1L).getCurrentVersion().getVersionNo());
        assertEquals(1, certificateService.getView(1L).getHistory().size());
    }

    // 9. 升级前老作品：无凭证照常查看/流转，列表版本标记为空，可从当前数据补签
    @Test
    void legacy_artwork_without_certificate_flows_and_backfill() {
        Artwork aw = artworkService.get(1L);
        assertEquals("青瓷碟", aw.getTitle());
        assertNull(aw.getCertificateVersionNo());
        assertFalse(aw.getCertificateIssued());

        CertificateView view = certificateService.getView(1L);
        assertFalse(view.isEverIssued());
        assertNull(view.getCurrentVersion());
        assertTrue(view.getHistory().isEmpty());

        // 首次补签直接生成 V1，无需重建历史业务
        FiringCertificate v1 = certificateService.issue(1L, "张三");
        assertEquals(1, v1.getVersionNo());
    }

    // 10. 凭证仍能打印历史版本：取到的是落库快照而非当前数据
    @Test
    void history_version_printable_from_snapshot() {
        certificateService.issue(1L, "张三");
        jdbc.update("UPDATE artwork SET title='新名称' WHERE id=1");
        FiringCertificate v2 = certificateService.correct(1L, 1, "李四", "改名");
        FiringCertificate printedHistory = certificateService.getVersion(1L,
                certificateService.getView(1L).getHistory().get(0).getId());
        assertEquals("青瓷碟", printedHistory.getSnapTitle());
        assertEquals("新名称", v2.getSnapTitle());
    }

    // 11. 更正必须填写原因和版本号
    @Test
    void correct_requires_reason_and_version() {
        certificateService.issue(1L, "张三");
        assertThrows(BizException.class, () -> certificateService.correct(1L, 1, "李四", " "));
        assertThrows(BizException.class, () -> certificateService.correct(1L, null, "李四", "原因"));
        // 无凭证作品不能更正，只能先签发
        artwork(9L, "AW-0009", "无凭证", "钱", 1L, 7L, 1L, "TAKEN");
        assertThrows(BizException.class, () -> certificateService.correct(9L, 1, "李四", "原因"));
    }

    // 12. 当前来源核对不通过时，即便有旧凭证也不能追加新版本
    @Test
    void cannot_correct_when_source_broken() {
        certificateService.issue(1L, "张三");
        // 之后来源被破坏：作品指向已不存在的坯体（独立事务提交后，对更正的新事务可见）
        jdbc.update("UPDATE artwork SET greenware_id=999 WHERE id=1");
        assertThrows(BizException.class, () -> certificateService.correct(1L, 1, "李四", "尝试更正"));
        // 旧凭证仍是当前版，未被动摇
        assertEquals(1, certificateService.getView(1L).getCurrentVersion().getVersionNo());
    }
}
