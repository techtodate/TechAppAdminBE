package com.app.admin.repository;

import com.app.admin.model.VerificationEvidence;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationEvidenceRepository extends JpaRepository<VerificationEvidence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from VerificationEvidence e where e.verificationRequestId=:requestId and e.id=:id")
    Optional<VerificationEvidence> findForUpdate(@Param("requestId") Long requestId, @Param("id") Long id);

    @EntityGraph(attributePaths = "reviewedBy")
    List<VerificationEvidence> findAllByVerificationRequestIdOrderById(Long verificationRequestId);

    List<VerificationEvidence> findAllByVerificationRequestId(Long verificationRequestId);
}
