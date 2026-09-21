package com.pottery.studio.material;

import com.pottery.studio.common.ApiResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/material-categories")
public class MaterialCategoryController {

    private final MaterialCategoryService service;

    public MaterialCategoryController(MaterialCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<MaterialCategory>> list() {
        return ApiResult.ok(service.list());
    }

    @GetMapping("/tree")
    public ApiResult<List<MaterialCategoryNode>> tree() {
        return ApiResult.ok(service.tree());
    }

    @GetMapping("/{parentId}/children")
    public ApiResult<List<MaterialCategoryNode>> children(@PathVariable Long parentId) {
        return ApiResult.ok(service.children(parentId));
    }

    @GetMapping("/{id}")
    public ApiResult<MaterialCategory> detail(@PathVariable Long id) {
        return ApiResult.ok(service.requireExists(id));
    }

    @PostMapping
    public ApiResult<MaterialCategory> create(@RequestBody MaterialCategory request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<MaterialCategory> update(@PathVariable Long id, @RequestBody MaterialCategory request) {
        return ApiResult.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
