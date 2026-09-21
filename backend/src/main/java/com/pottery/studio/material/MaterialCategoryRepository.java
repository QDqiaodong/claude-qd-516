package com.pottery.studio.material;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long> {

    List<MaterialCategory> findByParentIdOrderBySortNoAscIdAsc(Long parentId);

    boolean existsByParentId(Long parentId);

    boolean existsByCode(String code);
}
