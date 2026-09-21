package com.pottery.studio.firing;

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
@RequestMapping("/api/kilns")
public class KilnController {

    private final KilnService service;

    public KilnController(KilnService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<Kiln>> list() {
        return ApiResult.ok(service.list());
    }

    @GetMapping("/{id}")
    public ApiResult<Kiln> detail(@PathVariable Long id) {
        return ApiResult.ok(service.get(id));
    }

    @PostMapping
    public ApiResult<Kiln> create(@RequestBody Kiln request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<Kiln> update(@PathVariable Long id, @RequestBody Kiln request) {
        return ApiResult.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
