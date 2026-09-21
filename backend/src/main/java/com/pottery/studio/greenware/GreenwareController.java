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
@RequestMapping("/api/greenwares")
public class GreenwareController {

    private final GreenwareService service;

    public GreenwareController(GreenwareService service) {
        this.service = service;
    }

    /** areaId 会连带查出其所有后代分区下的坯体 */
    @GetMapping
    public ApiResult<List<Greenware>> list(@RequestParam(required = false) Long areaId,
                                           @RequestParam(required = false) String stage) {
        if (areaId != null) {
            return ApiResult.ok(service.listByAreaTree(areaId));
        }
        return ApiResult.ok(service.list(null, stage));
    }

    @GetMapping("/{id}")
    public ApiResult<Greenware> detail(@PathVariable Long id) {
        return ApiResult.ok(service.get(id));
    }

    @PostMapping
    public ApiResult<Greenware> create(@RequestBody Greenware request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<Greenware> update(@PathVariable Long id, @RequestBody Greenware request) {
        return ApiResult.ok(service.update(id, request));
    }

    /** 推进干燥阶段 */
    @PostMapping("/{id}/stage")
    public ApiResult<Greenware> advanceStage(@PathVariable Long id, @RequestParam String stage) {
        return ApiResult.ok(service.advanceStage(id, stage));
    }

    /** 转区 */
    @PostMapping("/{id}/move")
    public ApiResult<Greenware> move(@PathVariable Long id, @RequestParam Long areaId) {
        return ApiResult.ok(service.move(id, areaId));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
