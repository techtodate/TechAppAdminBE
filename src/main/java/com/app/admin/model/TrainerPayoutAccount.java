package com.app.admin.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "trainer_payout_accounts", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainerPayoutAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "trainer_user_id", nullable = false) private Long trainerUserId;
    @Column(nullable = false, length = 30) private String provider;
    @Column(name = "provider_account_id", length = 255) private String providerAccountId;
    @Column(name = "account_holder_name", length = 200) private String accountHolderName;
    @Column(name = "account_status", nullable = false, length = 30) private String accountStatus;
    @Column(name = "is_primary", nullable = false) private Boolean primaryAccount;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
