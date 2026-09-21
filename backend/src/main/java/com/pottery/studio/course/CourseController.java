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

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<Course>> list(@RequestParam(required = false) String status) {
        return ApiResult.ok(service.list(status));
    }

    @GetMapping("/{id}")
    public ApiResult<Course> detail(@PathVariable Long id) {
        return ApiResult.ok(service.get(id));
    }

    @PostMapping
    public ApiResult<Course> create(@RequestBody Course request) {
        return ApiResult.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<Course> update(@PathVariable Long id, @RequestBody Course request) {
        return ApiResult.ok(service.update(id, request));
    }

    /** 报名 */
    @PostMapping("/{id}/enroll")
    public ApiResult<Course> enroll(@PathVariable Long id,
                                    @RequestParam(required = false) Integer count) {
        return ApiResult.ok(service.enroll(id, count));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok(true);
    }
}
