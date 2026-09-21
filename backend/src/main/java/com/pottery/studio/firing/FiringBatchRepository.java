package com.pottery.studio.firing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FiringBatchRepository extends JpaRepository<FiringBatch, Long> {

    List<FiringBatch> findByKilnIdOrderByIdAsc(Long kilnId);

    List<FiringBatch> findByStageOrderByIdAsc(String stage);

    boolean existsByBatchNo(String batchNo);

    long countByKilnIdAndStageNot(Long kilnId, String stage);
}
