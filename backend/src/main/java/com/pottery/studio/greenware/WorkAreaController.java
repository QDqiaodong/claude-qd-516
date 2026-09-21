package com.pottery.studio.greenware;

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

import java.util.List;

@RestController
@RequestMapping("/api/work-areas")
public class WorkAreaController {

    private final WorkAreaService service;

    public WorkAreaController(WorkAreaService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<WorkArea>> list() {
        return ApiResult.ok(service.list());
    }

    @GetMapping("/tree")
    public ApiResult<List<WorkAreaNode>> tree() {
        return ApiResult.ok(service.tree());
    }

    @GetMapping("/{parentId}/children")
    public ApiResult<List<WorkAreaNode>> children(@PathVariable Long parentId) {
        return ApiResult.ok(service.children(parentId));
    }

    @GetMapping("/{id}")
    public ApiResult<WorkArea> detail(@PathVariable Long id) {
        return ApiResult.ok(service.requireExists(id));
    }

    @PostMapping
    public ApiResult<WorkArea> create(@RequestBody WorkArea request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<WorkArea> update(@PathVariable Long id, @RequestBody WorkArea request) {
        return ApiResult.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
