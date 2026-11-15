package com.app.admin.repository;

import com.app.admin.model.VerificationReviewHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationReviewHistoryRepository extends JpaRepository<VerificationReviewHistory, Long> {
    @EntityGraph(attributePaths = "adminUser")
    List<VerificationReviewHistory> findAllByVerificationRequestIdOrderByCreatedAtAscIdAsc(Long verificationRequestId);
}
