package com.app.admin.repository;

import com.app.admin.model.VerificationIdentity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationIdentityRepository extends JpaRepository<VerificationIdentity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from VerificationIdentity i where i.verificationRequestId=:requestId")
    Optional<VerificationIdentity> findForUpdate(@Param("requestId") Long requestId);

    @EntityGraph(attributePaths = "reviewedBy")
    Optional<VerificationIdentity> findByVerificationRequestId(Long verificationRequestId);
}
