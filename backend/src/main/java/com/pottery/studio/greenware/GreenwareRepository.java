package com.pottery.studio.greenware;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GreenwareRepository extends JpaRepository<Greenware, Long> {

    List<Greenware> findByAreaIdOrderByIdAsc(Long areaId);

    List<Greenware> findByStageOrderByIdAsc(String stage);

    List<Greenware> findByFiringBatchIdOrderByIdAsc(Long firingBatchId);

    boolean existsByCode(String code);

    boolean existsByClayId(Long clayId);

    boolean existsByGlazeId(Long glazeId);
}
