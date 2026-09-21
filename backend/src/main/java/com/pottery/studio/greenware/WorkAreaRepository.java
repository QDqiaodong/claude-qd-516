package com.pottery.studio.greenware;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkAreaRepository extends JpaRepository<WorkArea, Long> {

    List<WorkArea> findByParentIdOrderBySortNoAscIdAsc(Long parentId);

    boolean existsByCode(String code);
}
