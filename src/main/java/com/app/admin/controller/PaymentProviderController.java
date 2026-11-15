package com.app.admin.controller;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.app.admin.model.PaymentProvider;
import com.app.admin.service.PaymentProviderService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin/payment-providers")
public class PaymentProviderController {
    private final PaymentProviderService service;
    public PaymentProviderController(PaymentProviderService service) { this.service = service; }
    @GetMapping public ResponseEntity<List<PaymentProvider>> findAll() { return ResponseEntity.ok(service.findAll()); }
    @GetMapping("/{id}") public ResponseEntity<PaymentProvider> findById(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PostMapping public ResponseEntity<PaymentProvider> create(@Valid @RequestBody PaymentProvider request) { PaymentProvider created = service.create(request); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri(); return ResponseEntity.created(location).body(created); }
    @PutMapping("/{id}") public ResponseEntity<PaymentProvider> update(@PathVariable Long id, @Valid @RequestBody PaymentProvider request) { return ResponseEntity.ok(service.update(id, request)); }
}
