package com.pottery.studio.certificate;

import com.pottery.studio.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 烧成履历凭证：挂在作品详情下。
 * GET  current / history / precheck / versions/{id}（打印按版本取，历史版也可打印原内容）
 * POST issue 首次签发 / versions 更正追加新版本（带 expectedVersionNo 防并发覆盖）
 */
@RestController
@RequestMapping("/api/artworks/{artworkId}/certificates")
public class CertificateController {

    private final CertificateService service;

    public CertificateController(CertificateService service) {
        this.service = service;
    }

    /** 作品详情凭证面板：当前版 + 历史版 + 来源核对 + 字段差异 */
    @GetMapping
    public ApiResult<CertificateView> view(@PathVariable Long artworkId) {
        return ApiResult.ok(service.getView(artworkId));
    }

    /** 签发前核对（不落库），指出缺失段 / 冲突段 */
    @GetMapping("/precheck")
    public ApiResult<CertificateCheckView> precheck(@PathVariable Long artworkId) {
        return ApiResult.ok(service.precheck(artworkId));
    }

    /** 当前版本（打印当前版） */
    @GetMapping("/current")
    public ApiResult<FiringCertificate> current(@PathVariable Long artworkId) {
        return ApiResult.ok(service.getCurrent(artworkId));
    }

    /** 指定版本（打印/查看历史版，取的是当时落库快照，不随资料变化） */
    @GetMapping("/versions/{certificateId}")
    public ApiResult<FiringCertificate> version(@PathVariable Long artworkId,
                                                @PathVariable Long certificateId) {
        return ApiResult.ok(service.getVersion(artworkId, certificateId));
    }

    /** 首次签发 / 升级前老作品的首次补签：从当前可核实数据生成 */
    @PostMapping("/issue")
    public ApiResult<FiringCertificate> issue(@PathVariable Long artworkId,
                                              @RequestParam String issuedBy) {
        return ApiResult.ok(service.issue(artworkId, issuedBy));
    }

    /** 发起更正：追加新版本，旧版置为历史版；expectedVersionNo 不匹配返回 409 */
    @PostMapping("/versions")
    public ApiResult<FiringCertificate> correct(@PathVariable Long artworkId,
                                                @RequestParam Integer expectedVersionNo,
                                                @RequestParam String issuedBy,
                                                @RequestParam String changeReason) {
        return ApiResult.ok(service.correct(artworkId, expectedVersionNo, issuedBy, changeReason));
    }
}
