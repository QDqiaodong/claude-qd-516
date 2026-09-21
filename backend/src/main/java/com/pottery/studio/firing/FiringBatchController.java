package com.pottery.studio.firing;

import com.pottery.studio.common.ApiResult;
import com.pottery.studio.greenware.Greenware;
import com.pottery.studio.greenware.GreenwareService;
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
@RequestMapping("/api/firing-batches")
public class FiringBatchController {

    private final FiringBatchService service;
    private final GreenwareService greenwareService;

    public FiringBatchController(FiringBatchService service, GreenwareService greenwareService) {
        this.service = service;
        this.greenwareService = greenwareService;
    }

    @GetMapping
    public ApiResult<List<FiringBatch>> list(@RequestParam(required = false) Long kilnId,
                                             @RequestParam(required = false) String stage) {
        return ApiResult.ok(service.list(kilnId, stage));
    }

    @GetMapping("/{id}")
    public ApiResult<FiringBatch> detail(@PathVariable Long id) {
        return ApiResult.ok(service.get(id));
    }

    /** 批次内已装窑的坯体 */
    @GetMapping("/{id}/greenwares")
    public ApiResult<List<Greenware>> greenwares(@PathVariable Long id) {
        return ApiResult.ok(greenwareService.listByBatch(id));
    }

    @PostMapping
    public ApiResult<FiringBatch> create(@RequestBody FiringBatch request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<FiringBatch> update(@PathVariable Long id, @RequestBody FiringBatch request) {
        return ApiResult.ok(service.update(id, request));
    }

    /** 推进烧成阶段（时间轴上往下走一格） */
    @PostMapping("/{id}/stage")
    public ApiResult<FiringBatch> advanceStage(@PathVariable Long id,
                                               @RequestParam String stage,
                                               @RequestParam(required = false) Integer peakTemp) {
        return ApiResult.ok(service.advanceStage(id, stage, peakTemp));
    }

    /** 装窑：往批次里放一件坯体 */
    @PostMapping("/{id}/load")
    public ApiResult<FiringBatch> load(@PathVariable Long id, @RequestParam Long greenwareId) {
        return ApiResult.ok(service.loadGreenware(id, greenwareId));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
