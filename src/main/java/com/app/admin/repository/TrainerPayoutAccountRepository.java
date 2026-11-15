package com.app.admin.repository;

import com.app.admin.model.TrainerPayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrainerPayoutAccountRepository extends JpaRepository<TrainerPayoutAccount, Long>, JpaSpecificationExecutor<TrainerPayoutAccount> {}
