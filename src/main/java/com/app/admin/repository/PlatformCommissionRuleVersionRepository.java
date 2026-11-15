package com.app.admin.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.app.admin.model.PlatformCommissionRuleVersion;
public interface PlatformCommissionRuleVersionRepository extends JpaRepository<PlatformCommissionRuleVersion, Long> {
    List<PlatformCommissionRuleVersion> findByCommissionRuleIdOrderByVersionNumberDesc(Long ruleId);
}
