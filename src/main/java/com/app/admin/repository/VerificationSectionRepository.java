package com.app.admin.repository;

import com.app.admin.model.VerificationSection;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationSectionRepository extends JpaRepository<VerificationSection, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from VerificationSection s where s.verificationRequestId=:requestId and s.sectionCode=:code")
    Optional<VerificationSection> findForUpdate(@Param("requestId") Long requestId, @Param("code") String code);

    @EntityGraph(attributePaths = "reviewedBy")
    List<VerificationSection> findAllByVerificationRequestIdOrderById(Long verificationRequestId);
}
