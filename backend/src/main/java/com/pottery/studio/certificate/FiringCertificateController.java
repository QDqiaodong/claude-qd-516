package com.pottery.studio.certificate;

import com.pottery.studio.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 烧成履历凭证接口（挂在作品资源下）：
 * GET  /api/artworks/{id}/certificate        凭证聚合：当前版/历史版/来源核对/差异
 * GET  /api/artworks/{id}/certificate/{ver}  查看指定版本快照（含历史版，供打印）
 * POST /api/artworks/{id}/certificate/issue  首次签发
 * POST /api/artworks/{id}/certificate/correct 发起更正（追加新版本）
 */
@RestController
@RequestMapping("/api/artworks/{artworkId}/certificate")
public class FiringCertificateController {

    private final FiringCertificateService service;

    public FiringCertificateController(FiringCertificateService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<CertificateAggregate> aggregate(@PathVariable Long artworkId) {
        return ApiResult.ok(service.getAggregate(artworkId));
    }

    @GetMapping("/{versionNo}")
    public ApiResult<FiringCertificate> version(@PathVariable Long artworkId,
                                                @PathVariable Integer versionNo) {
        return ApiResult.ok(service.getVersion(artworkId, versionNo));
    }

    @PostMapping("/issue")
    public ApiResult<CertificateAggregate> issue(@PathVariable Long artworkId,
                                                 @RequestBody CertificateIssueRequest request) {
        String issuer = request == null ? null : request.getIssuedBy();
        Long expected = request == null ? null : request.getExpectedCurrentId();
        return ApiResult.ok(service.issue(artworkId, issuer, expected));
    }

    @PostMapping("/correct")
    public ApiResult<CertificateAggregate> correct(@PathVariable Long artworkId,
                                                   @RequestBody CertificateCorrectRequest request) {
        if (request == null) {
            request = new CertificateCorrectRequest();
        }
        return ApiResult.ok(service.correct(artworkId, request.getIssuedBy(),
                request.getReason(), request.getExpectedCurrentId()));
    }
}
