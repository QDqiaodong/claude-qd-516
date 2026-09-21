package com.pottery.studio.firing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KilnRepository extends JpaRepository<Kiln, Long> {

    boolean existsByCode(String code);
}
