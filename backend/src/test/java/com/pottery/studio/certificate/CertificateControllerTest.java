package com.pottery.studio.certificate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 凭证 HTTP 契约：签发、查看、打印取版、409 版本冲突 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CertificateControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();
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
                + "VALUES (1,3,'M-1','宜兴粗陶泥','CLAY',120,1180,12,20,'NORMAL',NOW(),NOW())");
        jdbc.update("INSERT INTO kiln(id,code,name,kiln_type,max_temp,volume_l,status,created_at,updated_at) "
                + "VALUES (1,'K-01','1号电窑','ELECTRIC',1300,120,'IDLE',NOW(),NOW())");
        jdbc.update("INSERT INTO firing_batch(id,batch_no,kiln_id,fire_type,stage,target_temp,peak_temp,greenware_count,"
                + "loaded_at,heating_at,soaking_at,cooling_at,out_at,created_at,updated_at) VALUES "
                + "(1,'FB-1',1,'GLAZE','OUT',1220,1218,6,?,?,?,?,?,NOW(),NOW())",
                T, T.plusHours(6), T.plusHours(11), T.plusHours(18), T.plusHours(24));
        jdbc.update("INSERT INTO course(id,code,title,teacher,level,capacity,enrolled,price,status,created_at,updated_at) "
                + "VALUES (1,'C-1','周六拉坯体验课','陈师傅','BEGINNER',12,12,168,'OPEN',NOW(),NOW())");
        jdbc.update("INSERT INTO greenware(id,code,name,clay_id,glaze_id,area_id,stage,moisture,firing_batch_id,shaped_at,created_at,updated_at) "
                + "VALUES (6,'GW-0006','青瓷碟',1,NULL,7,'FINISHED',2.1,NULL,?,NOW(),NOW())", T.minusDays(30));
        jdbc.update("INSERT INTO artwork(id,code,title,student_name,course_id,greenware_id,firing_batch_id,owner_status,finished_at,created_at,updated_at) "
                + "VALUES (1,'AW-0001','青瓷碟','李楠',1,6,1,'TAKEN',?,NOW(),NOW())", T);
    }

    @Test
    void legacyArtwork_view_hasNoCertificate_butCheckReady() throws Exception {
        mockMvc.perform(get("/api/artworks/1/certificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.everIssued").value(false))
                .andExpect(jsonPath("$.data.currentVersion").isEmpty())
                .andExpect(jsonPath("$.data.check.ready").value(true));
    }

    @Test
    void issue_then_current_and_print_then_stale_correct_409() throws Exception {
        // 首签
        String issueBody = mockMvc.perform(post("/api/artworks/1/certificates/issue").param("issuedBy", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.status").value("CURRENT"))
                .andReturn().getResponse().getContentAsString();
        long v1Id = om.readTree(issueBody).path("data").path("id").asLong();

        // 打印取版（历史/当前同一接口按 id 取落库快照）
        mockMvc.perform(get("/api/artworks/1/certificates/versions/" + v1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapTitle").value("青瓷碟"));

        // 资料改名
        jdbc.update("UPDATE artwork SET title='青瓷碟（补）' WHERE id=1");

        // V1 → V2 更正成功
        mockMvc.perform(post("/api/artworks/1/certificates/versions")
                        .param("expectedVersionNo", "1")
                        .param("issuedBy", "李四")
                        .param("changeReason", "改名订正"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(2));

        // 旧页面仍带 expectedVersionNo=1 再更正 → HTTP 409
        String conflictBody = mockMvc.perform(post("/api/artworks/1/certificates/versions")
                        .param("expectedVersionNo", "1")
                        .param("issuedBy", "王五")
                        .param("changeReason", "过期更正"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false))
                .andReturn().getResponse().getContentAsString();
        JsonNode conflict = om.readTree(conflictBody);
        assertEquals(true, conflict.path("message").asText().contains("V2"));

        // 当前仍为 V2，历史仅 V1
        mockMvc.perform(get("/api/artworks/1/certificates"))
                .andExpect(jsonPath("$.data.currentVersion.versionNo").value(2))
                .andExpect(jsonPath("$.data.history[0].versionNo").value(1))
                .andExpect(jsonPath("$.data.history[0].status").value("SUPERSEDED"))
                .andExpect(jsonPath("$.data.history[0].snapTitle").value("青瓷碟"))
                .andExpect(jsonPath("$.data.currentVersion.snapTitle").value("青瓷碟（补）"));
    }

    @Test
    void missingSource_blocksIssue() throws Exception {
        jdbc.update("UPDATE artwork SET greenware_id=999 WHERE id=1");
        mockMvc.perform(get("/api/artworks/1/certificates/precheck"))
                .andExpect(jsonPath("$.data.ready").value(false))
                .andExpect(jsonPath("$.data.blockingSegments[0].code").exists());
        mockMvc.perform(post("/api/artworks/1/certificates/issue").param("issuedBy", "张三"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }
}
