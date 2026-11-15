package com.app.admin.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.dto.CommissionRuleRequest;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.TrainingFinancialAdminAudit;
import com.app.admin.model.PlatformCommissionRule;
import com.app.admin.model.PlatformCommissionRuleVersion;
import com.app.admin.repository.PlatformCommissionRuleRepository;
import com.app.admin.repository.PlatformCommissionRuleVersionRepository;
import com.app.admin.repository.TrainingFinancialAdminAuditRepository;
import com.app.admin.security.AdminSecurityContext;

@Service
public class TrainingCommissionRuleService {
    private final PlatformCommissionRuleRepository rules; private final PlatformCommissionRuleVersionRepository versions;
    private final TrainingFinancialAdminAuditRepository auditEntries; private final AdminSecurityContext security;
    public TrainingCommissionRuleService(PlatformCommissionRuleRepository rules, PlatformCommissionRuleVersionRepository versions,
            TrainingFinancialAdminAuditRepository auditEntries, AdminSecurityContext security) {
        this.rules = rules; this.versions = versions; this.auditEntries = auditEntries; this.security = security;
    }
    @Transactional(readOnly = true) public List<PlatformCommissionRule> findAll() { return rules.findAll(); }
    @Transactional(readOnly = true) public PlatformCommissionRule findById(Long id) { return rules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commission rule", id)); }
    @Transactional(readOnly = true) public List<PlatformCommissionRuleVersion> versions(Long id) { findById(id); return versions.findByCommissionRuleIdOrderByVersionNumberDesc(id); }
    @Transactional public PlatformCommissionRule create(CommissionRuleRequest request) { validate(request); PlatformCommissionRule rule = toRule(request); rule = rules.saveAndFlush(rule); appendVersion(rule, 1, request, request.effectiveFrom() == null ? LocalDateTime.now() : request.effectiveFrom()); audit("COMMISSION_RULE_CREATED",rule.getId(),"version=1"); return rule; }
    @Transactional public PlatformCommissionRule update(Long id, CommissionRuleRequest request) {
        validate(request);
        PlatformCommissionRule rule = rules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commission rule", id));
        List<PlatformCommissionRuleVersion> existing = versions.findByCommissionRuleIdOrderByVersionNumberDesc(id);
        LocalDateTime effectiveFrom = request.effectiveFrom() == null ? LocalDateTime.now() : request.effectiveFrom();
        existing.stream().filter(PlatformCommissionRuleVersion::getActive).filter(v -> v.getEffectiveFrom().isBefore(effectiveFrom)).forEach(v -> { v.setEffectiveTo(effectiveFrom); v.setActive(false); });
        rule.setName(request.name()); rule.setDescription(request.description()); rule.setCommissionType(request.commissionType()); rule.setPercentage(request.percentage()); rule.setFixedAmount(request.fixedAmount()); rule.setCurrency(request.currency() == null ? "INR" : request.currency());
        appendVersion(rule, existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1, request, effectiveFrom);
        audit("COMMISSION_RULE_VERSION_CREATED",id,"version="+(existing.isEmpty()?1:existing.get(0).getVersionNumber()+1));
        return rules.saveAndFlush(rule);
    }
    private PlatformCommissionRule toRule(CommissionRuleRequest r) { PlatformCommissionRule rule = new PlatformCommissionRule(); rule.setCode(r.code()); rule.setName(r.name()); rule.setDescription(r.description()); rule.setCommissionType(r.commissionType()); rule.setPercentage(r.percentage()); rule.setFixedAmount(r.fixedAmount()); rule.setCurrency(r.currency() == null ? "INR" : r.currency()); rule.setActive(true); return rule; }
    private void appendVersion(PlatformCommissionRule rule, int number, CommissionRuleRequest r, LocalDateTime effectiveFrom) { PlatformCommissionRuleVersion v = new PlatformCommissionRuleVersion(); v.setCommissionRule(rule); v.setVersionNumber(number); v.setPercentage(r.percentage()); v.setFixedAmount(r.fixedAmount()); v.setEffectiveFrom(effectiveFrom); v.setActive(true); versions.save(v); }
    private void validate(CommissionRuleRequest r) {
        if (!List.of("PERCENTAGE", "FIXED", "PERCENTAGE_PLUS_FIXED").contains(r.commissionType())) throw new IllegalArgumentException("Unsupported commission_type");
        if ((r.commissionType().equals("PERCENTAGE") || r.commissionType().equals("PERCENTAGE_PLUS_FIXED")) && (r.percentage() == null || r.percentage().signum() < 0 || r.percentage().compareTo(new BigDecimal("100")) > 0)) throw new IllegalArgumentException("percentage must be between 0 and 100");
        if ((r.commissionType().equals("FIXED") || r.commissionType().equals("PERCENTAGE_PLUS_FIXED")) && (r.fixedAmount() == null || r.fixedAmount().signum() < 0)) throw new IllegalArgumentException("fixed_amount must be zero or greater");
    }
    private void audit(String action, Long id, String detail) {
        TrainingFinancialAdminAudit entry = new TrainingFinancialAdminAudit();
        var principal = security.currentPrincipal();
        entry.setAdminUsername(principal == null ? "system" : principal.username());
        entry.setAction(action);
        entry.setTargetType("platform_commission_rules");
        entry.setTargetId(id);
        entry.setResult("SUCCESS");
        entry.setDetails(detail);
        auditEntries.save(entry);
    }
}
