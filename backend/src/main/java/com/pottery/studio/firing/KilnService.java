package com.pottery.studio.firing;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KilnService {

    private final KilnRepository kilnRepository;
    private final FiringBatchRepository batchRepository;

    public KilnService(KilnRepository kilnRepository, FiringBatchRepository batchRepository) {
        this.kilnRepository = kilnRepository;
        this.batchRepository = batchRepository;
    }

    public List<Kiln> list() {
        List<Kiln> rows = kilnRepository.findAll();
        for (Kiln k : rows) {
            k.setRunningBatchCount((int) batchRepository.countByKilnIdAndStageNot(k.getId(), FiringBatch.OUT));
        }
        return rows;
    }

    public Kiln get(Long id) {
        return requireExists(id);
    }

    @Transactional
    public Kiln create(Kiln request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BizException("窑炉编号不能为空");
        }
        if (kilnRepository.existsByCode(request.getCode())) {
            throw new BizException("窑炉编号【" + request.getCode() + "】已存在");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException("窑炉名称不能为空");
        }
        if (request.getMaxTemp() == null || request.getMaxTemp() <= 0) {
            throw new BizException("窑炉最高温度必须大于 0");
        }
        if (request.getVolumeL() == null || request.getVolumeL() <= 0) {
            throw new BizException("窑炉容积必须大于 0");
        }
        // 默认值放在 service 的 create 里
        if (request.getKilnType() == null) {
            request.setKilnType("ELECTRIC");
        }
        if (request.getStatus() == null) {
            request.setStatus("IDLE");
        }
        return kilnRepository.save(request);
    }

    @Transactional
    public Kiln update(Long id, Kiln request) {
        Kiln exist = requireExists(id);
        if ("MAINTAIN".equals(request.getStatus()) && hasRunningBatch(id)) {
            throw new BizException("窑炉【" + exist.getName() + "】里还有未出窑的批次，不能转为检修状态");
        }
        PartialCopy.apply(request, exist, "code", "runningBatchCount");
        return kilnRepository.save(exist);
    }

    @Transactional
    public void delete(Long id) {
        Kiln exist = requireExists(id);
        if (hasRunningBatch(id)) {
            throw new BizException("窑炉【" + exist.getName() + "】里还有未出窑的批次，不能删除");
        }
        kilnRepository.delete(exist);
    }

    public Kiln requireExists(Long id) {
        return kilnRepository.findById(id)
                .orElseThrow(() -> new BizException("窑炉不存在，id=" + id));
    }

    public boolean hasRunningBatch(Long kilnId) {
        return batchRepository.countByKilnIdAndStageNot(kilnId, FiringBatch.OUT) > 0;
    }
}
