package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.TrainingCancellationPolicy;
import com.app.admin.repository.RefundPolicyRuleRepository;
import com.app.admin.repository.TrainingCancellationPolicyRepository;

@Service
public class TrainingCancellationPolicyService {
    private final TrainingCancellationPolicyRepository repository; private final RefundPolicyRuleRepository policies;
    public TrainingCancellationPolicyService(TrainingCancellationPolicyRepository repository, RefundPolicyRuleRepository policies) { this.repository = repository; this.policies = policies; }
    @Transactional(readOnly = true) public List<TrainingCancellationPolicy> findAll() { return repository.findAll(Sort.by(Sort.Order.asc("trainingOfferingId"))); }
    @Transactional(readOnly = true) public TrainingCancellationPolicy findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cancellation policy", id)); }
    @Transactional public TrainingCancellationPolicy create(TrainingCancellationPolicy request) { request.setId(null); resolvePolicies(request); return repository.saveAndFlush(request); }
    @Transactional public TrainingCancellationPolicy update(Long id, TrainingCancellationPolicy request) { TrainingCancellationPolicy current = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cancellation policy", id)); current.setTrainingOfferingId(request.getTrainingOfferingId()); current.setActive(request.getActive()); current.setBeforeTrainingStartPolicy(request.getBeforeTrainingStartPolicy()); current.setBetweenSessionsPolicy(request.getBetweenSessionsPolicy()); current.setAfterTrainingCompletionPolicy(request.getAfterTrainingCompletionPolicy()); resolvePolicies(current); return repository.saveAndFlush(current); }
    private void resolvePolicies(TrainingCancellationPolicy policy) {
        if (policy.getBeforeTrainingStartPolicy() == null || policy.getBeforeTrainingStartPolicy().getId() == null) throw new IllegalArgumentException("before_training_start_policy_id is required");
        policy.setBeforeTrainingStartPolicy(policies.findById(policy.getBeforeTrainingStartPolicy().getId()).orElseThrow(() -> new ResourceNotFoundException("Refund policy", policy.getBeforeTrainingStartPolicy().getId())));
        if (policy.getBetweenSessionsPolicy() != null) policy.setBetweenSessionsPolicy(policies.findById(policy.getBetweenSessionsPolicy().getId()).orElseThrow(() -> new ResourceNotFoundException("Refund policy", policy.getBetweenSessionsPolicy().getId())));
        if (policy.getAfterTrainingCompletionPolicy() != null) policy.setAfterTrainingCompletionPolicy(policies.findById(policy.getAfterTrainingCompletionPolicy().getId()).orElseThrow(() -> new ResourceNotFoundException("Refund policy", policy.getAfterTrainingCompletionPolicy().getId())));
    }
}
