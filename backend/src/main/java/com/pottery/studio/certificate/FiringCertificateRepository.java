package com.pottery.studio.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FiringCertificateRepository extends JpaRepository<FiringCertificate, Long> {

    List<FiringCertificate> findByArtworkIdOrderByVersionNoAsc(Long artworkId);

    Optional<FiringCertificate> findByArtworkIdAndVersionNo(Long artworkId, Integer versionNo);

    Optional<FiringCertificate> findFirstByArtworkIdAndStatusOrderByVersionNoDesc(Long artworkId, String status);

    long countByArtworkId(Long artworkId);
}
