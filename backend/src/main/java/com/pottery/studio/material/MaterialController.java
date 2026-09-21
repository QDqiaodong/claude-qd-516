package com.pottery.studio.material;

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
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    /** categoryId 会连带查出其所有后代分类下的材料 */
    @GetMapping
    public ApiResult<List<Material>> list(@RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) String kind) {
        if (categoryId != null) {
            return ApiResult.ok(service.listByCategoryTree(categoryId));
        }
        return ApiResult.ok(service.list(null, kind));
    }

    @GetMapping("/{id}")
    public ApiResult<Material> detail(@PathVariable Long id) {
        return ApiResult.ok(service.get(id));
    }

    @PostMapping
    public ApiResult<Material> create(@RequestBody Material request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<Material> update(@PathVariable Long id, @RequestBody Material request) {
        return ApiResult.ok(service.update(id, request));
    }

    @PostMapping("/{id}/consume")
    public ApiResult<Material> consume(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ApiResult.ok(service.consume(id, amount));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
