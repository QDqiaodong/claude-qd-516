package com.pottery.studio.course;

import com.pottery.studio.certificate.FiringCertificateService;
import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import com.pottery.studio.firing.FiringBatch;
import com.pottery.studio.firing.FiringBatchRepository;
import com.pottery.studio.firing.FiringBatchService;
import com.pottery.studio.greenware.Greenware;
import com.pottery.studio.greenware.GreenwareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArtworkService {

    private static final Map<String, String> OWNER_LABEL = Map.of(
            Artwork.TAKEN, "学员带走", Artwork.CONSIGN, "留馆寄售", Artwork.SOLD, "已售出");

    private static final BigDecimal ZERO = new BigDecimal("0");

    private final ArtworkRepository artworkRepository;
    private final CourseService courseService;
    private final GreenwareRepository greenwareRepository;
    private final FiringBatchRepository batchRepository;
    private final FiringCertificateService certificateService;

    public ArtworkService(ArtworkRepository artworkRepository,
                          CourseService courseService,
                          GreenwareRepository greenwareRepository,
                          FiringBatchRepository batchRepository,
                          FiringCertificateService certificateService) {
        this.artworkRepository = artworkRepository;
        this.courseService = courseService;
        this.greenwareRepository = greenwareRepository;
        this.batchRepository = batchRepository;
        this.certificateService = certificateService;
    }

    public static String ownerLabel(String status) {
        return OWNER_LABEL.getOrDefault(status, status);
    }

    public List<Artwork> list(Long courseId, String ownerStatus) {
        List<Artwork> rows;
        if (courseId != null) {
            rows = artworkRepository.findByCourseIdOrderByIdAsc(courseId);
        } else if (ownerStatus != null && !ownerStatus.isBlank()) {
            rows = artworkRepository.findByOwnerStatusOrderByIdAsc(ownerStatus);
        } else {
            rows = artworkRepository.findAll();
        }
        return decorate(rows);
    }

    public Artwork get(Long id) {
        return decorateOne(requireExists(id));
    }

    @Transactional
    public Artwork create(Artwork request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BizException("作品编号不能为空");
        }
        if (artworkRepository.existsByCode(request.getCode())) {
            throw new BizException("作品编号【" + request.getCode() + "】已存在");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException("作品名称不能为空");
        }
        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            throw new BizException("学员姓名不能为空");
        }
        // 可空外键先判 null
        if (request.getCourseId() == null) {
            throw new BizException("作品必须归属到一门课程，请先选择课程");
        }
        if (request.getGreenwareId() == null) {
            throw new BizException("作品必须关联来源坯体，请先选择坯体");
        }
        if (request.getFiringBatchId() == null) {
            throw new BizException("作品必须关联烧成批次，请先选择批次");
        }
        // 默认值放在 service 的 create 里
        if (request.getOwnerStatus() == null) {
            request.setOwnerStatus(Artwork.TAKEN);
        }
        if (request.getFinishedAt() == null) {
            request.setFinishedAt(LocalDateTime.now());
        }
        validateProvenance(request, null);
        validateOwner(request);
        return decorateOne(artworkRepository.save(request));
    }

    @Transactional
    public Artwork update(Long id, Artwork request) {
        Artwork exist = requireExists(id);
        // 作品名称/学员/归属等资料允许修正；课程、坯体、批次一旦变动需重新做来源链核对
        boolean sourceMaybeChanged = request.getCourseId() != null
                || request.getGreenwareId() != null
                || request.getFiringBatchId() != null;
        PartialCopy.apply(request, exist, "code", "courseTitle", "greenwareCode", "batchNo");
        if (sourceMaybeChanged) {
            validateProvenance(exist, id);
        }
        validateOwner(exist);
        return decorateOne(artworkRepository.save(exist));
    }

    /** 改归属：学员带走 / 留馆寄售 / 已售出。只校验归属本身，不重新核对来源链（凭证核对独立负责）。 */
    @Transactional
    public Artwork changeOwner(Long id, String ownerStatus, BigDecimal consignPrice) {
        Artwork exist = requireExists(id);
        if (ownerStatus == null || !OWNER_LABEL.containsKey(ownerStatus)) {
            throw new BizException("作品归属只能是 TAKEN（学员带走）、CONSIGN（留馆寄售）或 SOLD（已售出）");
        }
        exist.setOwnerStatus(ownerStatus);
        if (consignPrice != null) {
            exist.setConsignPrice(consignPrice);
        }
        validateOwner(exist);
        return decorateOne(artworkRepository.save(exist));
    }

    @Transactional
    public void delete(Long id) {
        Artwork exist = requireExists(id);
        if (certificateService.hasCertificate(id)) {
            throw new BizException("作品【" + exist.getTitle()
                    + "】已签发烧成履历凭证，凭证记录需永久留存，不能删除作品");
        }
        if (Artwork.SOLD.equals(exist.getOwnerStatus())) {
            throw new BizException("作品【" + exist.getTitle() + "】已售出，不能删除");
        }
        if (Artwork.CONSIGN.equals(exist.getOwnerStatus())) {
            throw new BizException("作品【" + exist.getTitle() + "】还在货架上寄售，请先下架再删除");
        }
        artworkRepository.delete(exist);
    }

    public Artwork requireExists(Long id) {
        return artworkRepository.findById(id)
                .orElseThrow(() -> new BizException("作品不存在，id=" + id));
    }

    // ---------- 校验规则 ----------

    /**
     * 来源链强校验，只在"新建作品"和"修正课程/坯体/批次关联"时执行：
     * 升级前已登记但来源缺失/冲突的遗留作品仍可查看和改归属流转，不被这里追溯拦截。
     */
    private void validateProvenance(Artwork a, Long excludeId) {
        Course course = courseService.requireExists(a.getCourseId());

        Greenware g = greenwareRepository.findById(a.getGreenwareId())
                .orElseThrow(() -> new BizException("坯体不存在，id=" + a.getGreenwareId()));
        if (artworkRepository.existsByGreenwareId(a.getGreenwareId())) {
            for (Artwork other : findByGreenware(a.getGreenwareId())) {
                if (excludeId == null || !excludeId.equals(other.getId())) {
                    throw new BizException("坯体【" + g.getCode() + "】已经登记为作品【" + other.getTitle()
                            + "】，一件坯体只能登记一次");
                }
            }
        }

        FiringBatch batch = batchRepository.findById(a.getFiringBatchId())
                .orElseThrow(() -> new BizException("烧成批次不存在，id=" + a.getFiringBatchId()));
        if (!FiringBatch.OUT.equals(batch.getStage())) {
            throw new BizException("烧成批次【" + batch.getBatchNo() + "】还没出窑（当前"
                    + FiringBatchService.stageLabel(batch.getStage()) + "），不能登记作品");
        }

        long works = artworkRepository.findByCourseIdOrderByIdAsc(a.getCourseId()).size();
        if (excludeId != null) {
            for (Artwork w : artworkRepository.findByCourseIdOrderByIdAsc(a.getCourseId())) {
                if (excludeId.equals(w.getId())) {
                    works--;
                }
            }
        }
        if (works + 1 > course.getEnrolled()) {
            throw new BizException("课程【" + course.getTitle() + "】已报名 " + course.getEnrolled()
                    + " 人，作品数已达 " + works + " 件，一人一件不能再登记了");
        }
    }

    /** 归属相关校验（与来源链解耦，遗留作品改归属照常流转） */
    private void validateOwner(Artwork a) {
        if (!OWNER_LABEL.containsKey(a.getOwnerStatus())) {
            throw new BizException("作品归属只能是 TAKEN（学员带走）、CONSIGN（留馆寄售）或 SOLD（已售出），当前传入【"
                    + (a.getOwnerStatus() == null ? "空" : a.getOwnerStatus()) + "】");
        }
        if (Artwork.CONSIGN.equals(a.getOwnerStatus())) {
            if (a.getConsignPrice() == null || a.getConsignPrice().compareTo(ZERO) <= 0) {
                throw new BizException("作品【" + a.getTitle() + "】归属为留馆寄售时必须填写寄售价格且大于 0");
            }
            if (a.getShelfNo() == null || a.getShelfNo().isBlank()) {
                throw new BizException("作品【" + a.getTitle() + "】寄售时必须指定货架位");
            }
        }
        if (Artwork.SOLD.equals(a.getOwnerStatus()) && (a.getConsignPrice() == null
                || a.getConsignPrice().compareTo(ZERO) <= 0)) {
            throw new BizException("作品【" + a.getTitle() + "】标记为已售出时必须填写成交价格且大于 0");
        }
        if (Artwork.TAKEN.equals(a.getOwnerStatus())) {
            a.setConsignPrice(null);
            a.setShelfNo(null);
        }
    }

    private List<Artwork> findByGreenware(Long greenwareId) {
        List<Artwork> hit = new ArrayList<>();
        for (Artwork w : artworkRepository.findAll()) {
            if (greenwareId.equals(w.getGreenwareId())) {
                hit.add(w);
            }
        }
        return hit;
    }

    private List<Artwork> decorate(List<Artwork> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<Long, String> courseTitles = new HashMap<>();
        for (Course c : courseService.list(null)) {
            courseTitles.put(c.getId(), c.getTitle());
        }
        Map<Long, String> greenwareCodes = new HashMap<>();
        for (Greenware g : greenwareRepository.findAll()) {
            greenwareCodes.put(g.getId(), g.getCode());
        }
        Map<Long, String> batchNos = new HashMap<>();
        for (FiringBatch b : batchRepository.findAll()) {
            batchNos.put(b.getId(), b.getBatchNo());
        }
        for (Artwork a : rows) {
            a.setCourseTitle(courseTitles.get(a.getCourseId()));
            a.setGreenwareCode(greenwareCodes.get(a.getGreenwareId()));
            a.setBatchNo(batchNos.get(a.getFiringBatchId()));
            a.setCertVersionNo(certificateService.currentVersionNo(a.getId()).orElse(null));
        }
        return rows;
    }

    private Artwork decorateOne(Artwork a) {
        return decorate(new ArrayList<>(List.of(a))).get(0);
    }
}
