package com.pottery.studio.course;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    List<Artwork> findByCourseIdOrderByIdAsc(Long courseId);

    List<Artwork> findByOwnerStatusOrderByIdAsc(String ownerStatus);

    boolean existsByCode(String code);

    boolean existsByGreenwareId(Long greenwareId);
}
