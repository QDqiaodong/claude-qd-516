package com.pottery.studio.course;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    List<Artwork> findByCourseIdOrderByIdAsc(Long courseId);

    List<Artwork> findByOwnerStatusOrderByIdAsc(String ownerStatus);

    boolean existsByCode(String code);

    boolean existsByGreenwareId(Long greenwareId);

    boolean existsByFiringBatchId(Long firingBatchId);

    /** 凭证签发/更正时对作品行加写锁，把两名工作人员的并发提交串行化 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Artwork a where a.id = :id")
    Optional<Artwork> findLockById(@Param("id") Long id);
}
