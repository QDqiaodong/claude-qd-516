package com.pottery.studio.certificate;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FiringCertificateRepository extends JpaRepository<FiringCertificate, Long> {

    /** 当前版本（每个作品至多一条） */
    Optional<FiringCertificate> findFirstByArtworkIdAndStatusOrderByVersionNoDesc(Long artworkId, String status);

    /**
     * 事务内取该作品当前版本并对行加写锁（每作品至多一条 CURRENT）。
     * 两名工作人员同时更正时后者等待，待前者提交后读到新版本号，
     * 从而不会并发产生两条 CURRENT。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FiringCertificate c where c.artworkId = :artworkId and c.status = 'CURRENT' "
            + "order by c.versionNo desc")
    List<FiringCertificate> lockCurrent(@Param("artworkId") Long artworkId);

    /** 作品的全部版本，版本号倒序（历史追溯） */
    List<FiringCertificate> findByArtworkIdOrderByVersionNoDesc(Long artworkId);

    /** 取最大版本号，用于追加新版本 */
    Optional<FiringCertificate> findFirstByArtworkIdOrderByVersionNoDesc(Long artworkId);

    boolean existsByArtworkId(Long artworkId);
}
