package com.pottery.studio.course;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CourseService {

    private static final Map<String, String> STATUS_LABEL = Map.of(
            "OPEN", "报名中", "ONGOING", "进行中", "FINISHED", "已结束");

    private final CourseRepository courseRepository;
    private final ArtworkRepository artworkRepository;

    public CourseService(CourseRepository courseRepository, ArtworkRepository artworkRepository) {
        this.courseRepository = courseRepository;
        this.artworkRepository = artworkRepository;
    }

    public static String statusLabel(String status) {
        return STATUS_LABEL.getOrDefault(status, status);
    }

    public List<Course> list(String status) {
        List<Course> rows = status == null || status.isBlank()
                ? courseRepository.findAll()
                : courseRepository.findByStatusOrderByIdAsc(status);
        for (Course c : rows) {
            c.setArtworkCount(artworkRepository.findByCourseIdOrderByIdAsc(c.getId()).size());
        }
        return rows;
    }

    public Course get(Long id) {
        return requireExists(id);
    }

    @Transactional
    public Course create(Course request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BizException("课程编号不能为空");
        }
        if (courseRepository.existsByCode(request.getCode())) {
            throw new BizException("课程编号【" + request.getCode() + "】已存在");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException("课程名称不能为空");
        }
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new BizException("课程容量必须大于 0");
        }
        // 默认值放在 service 的 create 里
        if (request.getEnrolled() == null) {
            request.setEnrolled(0);
        }
        if (request.getLevel() == null) {
            request.setLevel("BEGINNER");
        }
        if (request.getStatus() == null) {
            request.setStatus("OPEN");
        }
        if (request.getPrice() == null) {
            request.setPrice(BigDecimal.ZERO);
        }
        if (request.getEnrolled() > request.getCapacity()) {
            throw new BizException("已报名人数 " + request.getEnrolled() + " 人超过课程容量 " + request.getCapacity() + " 人");
        }
        return courseRepository.save(request);
    }

    @Transactional
    public Course update(Long id, Course request) {
        Course exist = requireExists(id);
        if (request.getCapacity() != null && request.getCapacity() < exist.getEnrolled()) {
            throw new BizException("课程【" + exist.getTitle() + "】已报名 " + exist.getEnrolled()
                    + " 人，容量不能下调到 " + request.getCapacity() + " 人");
        }
        PartialCopy.apply(request, exist, "code", "artworkCount");
        if (exist.getCapacity() != null && exist.getEnrolled() != null && exist.getEnrolled() > exist.getCapacity()) {
            throw new BizException("已报名人数 " + exist.getEnrolled() + " 人超过课程容量 " + exist.getCapacity() + " 人");
        }
        return courseRepository.save(exist);
    }

    /** 报名 */
    @Transactional
    public Course enroll(Long id, Integer count) {
        Course exist = requireExists(id);
        int delta = count == null ? 1 : count;
        if (delta <= 0) {
            throw new BizException("报名人数必须大于 0");
        }
        if (!"OPEN".equals(exist.getStatus())) {
            throw new BizException("课程【" + exist.getTitle() + "】当前状态为" + statusLabel(exist.getStatus())
                    + "，不能再接受报名");
        }
        if (exist.getEnrolled() + delta > exist.getCapacity()) {
            throw new BizException("课程【" + exist.getTitle() + "】容量 " + exist.getCapacity()
                    + " 人，已报名 " + exist.getEnrolled() + " 人，本次报名 " + delta + " 人后超出容量，无法报名");
        }
        exist.setEnrolled(exist.getEnrolled() + delta);
        return courseRepository.save(exist);
    }

    @Transactional
    public void delete(Long id) {
        Course exist = requireExists(id);
        long works = artworkRepository.findByCourseIdOrderByIdAsc(id).size();
        if (works > 0) {
            throw new BizException("课程【" + exist.getTitle() + "】下还有 " + works + " 件学员作品，不能删除");
        }
        courseRepository.delete(exist);
    }

    public Course requireExists(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new BizException("课程不存在，id=" + id));
    }
}
