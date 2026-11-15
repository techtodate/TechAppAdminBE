package com.app.admin.repository;

import com.app.admin.model.UserVerificationRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserVerificationRequestRepository extends JpaRepository<UserVerificationRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from UserVerificationRequest r where r.id=:id")
    Optional<UserVerificationRequest> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserVerificationRequest r set r.status='CHANGES_REQUIRED', r.changesRequestedAt=:changedAt, r.adminNotes=:adminNotes, r.updatedAt=:changedAt where r.id=:id")
    int markChangesRequired(@Param("id") Long id, @Param("changedAt") java.time.LocalDateTime changedAt,
            @Param("adminNotes") String adminNotes);
}
