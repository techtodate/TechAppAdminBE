package com.app.admin.repository;

import com.app.admin.model.AdminRefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    List<AdminRefreshToken> findAllByAdminUserIdAndRevokedAtIsNull(Long adminUserId);

    @Modifying
    @Query("update AdminRefreshToken t set t.revokedAt=:now where t.adminUser.id=:userId and t.revokedAt is null")
    int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update AdminRefreshToken t set t.revokedAt=:now where t.tokenHash=:hash and t.adminUser.id=:userId and t.revokedAt is null")
    int revokeActiveToken(@Param("hash") String hash, @Param("userId") Long userId, @Param("now") LocalDateTime now);
}
