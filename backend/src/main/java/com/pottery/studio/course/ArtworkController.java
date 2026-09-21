package com.pottery.studio.course;

import com.pottery.studio.common.ApiResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/artworks")
public class ArtworkController {

    private final ArtworkService service;

    public ArtworkController(ArtworkService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<Artwork>> list(@RequestParam(required = false) Long courseId,
                                         @RequestParam(required = false) String ownerStatus) {
        return ApiResult.ok(service.list(courseId, ownerStatus));
    }

    @GetMapping("/{id}")
    public ApiResult<Artwork> detail(@PathVariable Long id) {
        return ApiResult.ok(service.get(id));
    }

    @PostMapping
    public ApiResult<Artwork> create(@RequestBody Artwork request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<Artwork> update(@PathVariable Long id, @RequestBody Artwork request) {
        return ApiResult.ok(service.update(id, request));
    }

    /** 改归属：学员带走 / 留馆寄售 / 已售出 */
    @PostMapping("/{id}/owner")
    public ApiResult<Artwork> changeOwner(@PathVariable Long id,
                                          @RequestParam String ownerStatus,
                                          @RequestParam(required = false) BigDecimal consignPrice) {
        return ApiResult.ok(service.changeOwner(id, ownerStatus, consignPrice));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
