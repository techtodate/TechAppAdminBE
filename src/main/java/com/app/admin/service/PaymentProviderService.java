package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.PaymentProvider;
import com.app.admin.repository.PaymentProviderRepository;

@Service
public class PaymentProviderService {
    private final PaymentProviderRepository repository;
    public PaymentProviderService(PaymentProviderRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<PaymentProvider> findAll() { return repository.findAll(Sort.by(Sort.Order.asc("name"))); }
    @Transactional(readOnly = true) public PaymentProvider findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment provider", id)); }
    @Transactional public PaymentProvider create(PaymentProvider request) { request.setId(null); return repository.saveAndFlush(request); }
    @Transactional public PaymentProvider update(Long id, PaymentProvider request) { PaymentProvider current = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment provider", id)); current.setCode(request.getCode()); current.setName(request.getName()); current.setActive(request.getActive()); return repository.saveAndFlush(current); }
}
