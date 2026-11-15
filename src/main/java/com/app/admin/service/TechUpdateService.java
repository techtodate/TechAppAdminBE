package com.app.admin.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.dto.CreateTechUpdateRequest;
import com.app.admin.dto.TechUpdateResponse;
import com.app.admin.dto.TechUpdateResponse;
import com.app.admin.dto.UpdateTechUpdateRequest;
import com.app.admin.event.TechUpdateCreatedEvent;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Category;
import com.app.admin.model.Field;
import com.app.admin.model.TechUpdate;
import com.app.admin.model.Technology;
import com.app.admin.repository.CategoryRepository;
import com.app.admin.repository.FieldRepository;
import com.app.admin.repository.TechUpdateRepository;
import com.app.admin.repository.TechnologyRepository;
import com.app.admin.security.AdminSecurityContext;

@Service
public class TechUpdateService {
    private final TechUpdateRepository repository;
    private final TechnologyRepository technologies;
    private final CategoryRepository categories;
    private final FieldRepository fields;
    private final AdminSecurityContext security;
    private final ApplicationEventPublisher events;

    public TechUpdateService(TechUpdateRepository repository, TechnologyRepository technologies,
            CategoryRepository categories, FieldRepository fields, AdminSecurityContext security,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.technologies = technologies;
        this.categories = categories;
        this.fields = fields;
        this.security = security;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<TechUpdate> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public TechUpdate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechUpdate", id));
    }

    @Transactional
    public TechUpdate create(TechUpdate request) {
        if (request.getSourceUrl() == null || request.getSourceUrl().isBlank()) {
            throw new IllegalArgumentException("Source URL is required for a technology update");
        }
        if (repository.existsBySourceUrl(request.getSourceUrl())) {
            throw new IllegalArgumentException("A technology update with this source URL already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        request.setId(null);
        request.setContent(request.getContent() == null ? "" : request.getContent());
        request.setCreatedAt(request.getCreatedAt() == null ? now : request.getCreatedAt());
        request.setPulledAt(now);
        request.setSummary(null);
        request.setSummaryHeading(null);
        request.setSummaryStatus("PENDING");
        request.setSummaryModel(null);
        request.setSummaryPromptVersion(null);
        request.setSummarizedAt(null);
        request.setSummaryAttempts(0);
        request.setSummaryError(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public TechUpdate createManualRecord(CreateTechUpdateRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_TECH_UPDATE_MANAGE);
        Technology technology = technologies.findById(request.getTechId())
                .orElseThrow(() -> new ResourceNotFoundException("Technology", request.getTechId()));

        Field technologyField = fields.findByCodeIgnoreCase("TECHNOLOGY")
                .orElseThrow(() -> new IllegalArgumentException("TECHNOLOGY field is not configured"));
        if (!technologyField.getId().equals(technology.getFieldId())) {
            throw new IllegalArgumentException("Technology '" + technology.getName()
                    + "' (id=" + technology.getId() + ") is assigned to field_id=" + technology.getFieldId()
                    + ", but the TECHNOLOGY field has id=" + technologyField.getId()
                    + ". Update the technology's field assignment before creating an update.");
        }
        if (!Boolean.TRUE.equals(technologyField.getActive())) {
            throw new IllegalArgumentException("TECHNOLOGY field is inactive");
        }

        Category leaf = technology.getCategoryId() == null ? null : categories.findById(technology.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", technology.getCategoryId()));
        if (leaf == null) throw new IllegalArgumentException("Technology must belong to an active category");
        if (!technologyField.getId().equals(leaf.getFieldId())) {
            throw new IllegalArgumentException("Technology category does not belong to the TECHNOLOGY field");
        }
        if (!Boolean.TRUE.equals(leaf.getActive())) throw new IllegalArgumentException("Technology category is inactive");

        List<Category> path = resolveCategoryPath(leaf);
        TechUpdate update = new TechUpdate();
        update.setTechId(technology.getId());
        update.setTitle(request.getTitle().trim());
        update.setContent(request.getContent().trim());
        update.setSourceUrl(request.getSourceUrl() == null || request.getSourceUrl().isBlank()
                ? null : request.getSourceUrl().trim());
        update.setSource("ADMIN_MANUAL");
        LocalDateTime now = LocalDateTime.now();
        update.setCreatedAt(now);
        update.setPulledAt(now);
        update.setSummaryStatus("PENDING");
        update.setSummaryAttempts(0);
        update.setCreatedBy(security.getCurrentUserId());

        TechUpdate saved = repository.saveAndFlush(update);
        TechUpdateCreatedEvent event = TechUpdateCreatedEvent.from(saved);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
        return saved;
    }

    @Transactional
    public TechUpdateResponse createManual(CreateTechUpdateRequest request) {
        TechUpdate saved = createManualRecord(request);
        Technology technology = technologies.findById(saved.getTechId()).orElseThrow();
        Category leaf = categories.findById(technology.getCategoryId()).orElseThrow();
        List<TechUpdateResponse.CategorySummary> categoryPath = resolveCategoryPath(leaf).stream()
                .map(category -> new TechUpdateResponse.CategorySummary(category.getId(), category.getName()))
                .toList();
        return new TechUpdateResponse(saved.getId(), saved.getTechId(),
                new TechUpdateResponse.TechnologySummary(technology.getId(), technology.getName()),
                categoryPath, saved.getTitle(), saved.getContent(), saved.getSourceUrl(), saved.getSource(),
                saved.getCreatedBy(), saved.getCreatedAt(), saved.getSummaryStatus());
    }

    private List<Category> resolveCategoryPath(Category leaf) {
        Map<Long, Category> byId = new HashMap<>();
        for (Category category : categories.findByFieldId(leaf.getFieldId())) byId.put(category.getId(), category);
        List<Category> reversed = new ArrayList<>();
        Category current = leaf;
        while (current != null) {
            if (!Boolean.TRUE.equals(current.getActive())) throw new IllegalArgumentException("Technology category hierarchy contains an inactive category");
            reversed.add(current);
            Long parentId = current.getParentId();
            current = parentId == null ? null : byId.get(parentId);
            if (parentId != null && current == null) throw new IllegalArgumentException("Technology category hierarchy is invalid");
            if (reversed.size() > byId.size()) throw new IllegalArgumentException("Technology category hierarchy contains a cycle");
        }
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    @Transactional
    public TechUpdate update(Long id, UpdateTechUpdateRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_TECH_UPDATE_MANAGE);
        TechUpdate current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechUpdate", id));
        if (!"ADMIN_MANUAL".equals(current.getSource())) {
            throw new IllegalArgumentException("Feed-generated technology updates cannot be edited here");
        }
        current.setTitle(request.getTitle().trim());
        current.setContent(request.getContent().trim());
        current.setSourceUrl(request.getSourceUrl() == null || request.getSourceUrl().isBlank()
                ? null : request.getSourceUrl().trim());
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        security.requirePermission(AdminSecurityContext.PERMISSION_TECH_UPDATE_MANAGE);
        TechUpdate current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechUpdate", id));
        if (!"ADMIN_MANUAL".equals(current.getSource())) {
            throw new IllegalArgumentException("Feed-generated technology updates cannot be deleted here");
        }
        repository.delete(current);
        repository.flush();
    }
}
