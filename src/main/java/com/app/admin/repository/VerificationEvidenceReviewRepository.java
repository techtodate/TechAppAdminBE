package com.app.admin.repository;

import com.app.admin.model.VerificationEvidenceReview;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationEvidenceReviewRepository extends JpaRepository<VerificationEvidenceReview, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from VerificationEvidenceReview r where r.sourceEvidenceId=:evidenceId and r.verificationRequestId=:requestId")
    Optional<VerificationEvidenceReview> findForUpdate(@Param("requestId") Long requestId, @Param("evidenceId") Long evidenceId);
}
