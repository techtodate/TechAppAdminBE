package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.RefundPolicyRule;
import com.app.admin.repository.RefundPolicyRuleRepository;

@Service
public class RefundPolicyRuleService {
    private final RefundPolicyRuleRepository repository;
    public RefundPolicyRuleService(RefundPolicyRuleRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<RefundPolicyRule> findAll() { return repository.findAll(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name"))); }
    @Transactional(readOnly = true) public RefundPolicyRule findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Refund policy", id)); }
    @Transactional public RefundPolicyRule create(RefundPolicyRule request) { request.setId(null); return repository.saveAndFlush(request); }
    @Transactional public RefundPolicyRule update(Long id, RefundPolicyRule request) { RefundPolicyRule current = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Refund policy", id)); current.setCode(request.getCode()); current.setName(request.getName()); current.setDescription(request.getDescription()); current.setRefundPercentage(request.getRefundPercentage()); current.setActive(request.getActive()); current.setDisplayOrder(request.getDisplayOrder()); return repository.saveAndFlush(current); }
}
