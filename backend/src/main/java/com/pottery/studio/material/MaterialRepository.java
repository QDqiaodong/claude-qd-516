package com.pottery.studio.material;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByCategoryIdOrderByIdAsc(Long categoryId);

    List<Material> findByKindOrderByIdAsc(String kind);

    boolean existsByCode(String code);

    long countByCategoryId(Long categoryId);
}
