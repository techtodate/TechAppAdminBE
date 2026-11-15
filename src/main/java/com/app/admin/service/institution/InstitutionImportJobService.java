package com.app.admin.service.institution;

import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.app.admin.model.InstitutionImport;

/** Runs potentially long import phases outside the HTTP request thread. */
@Service
public class InstitutionImportJobService {
    private final Executor executor;
    private final InstitutionImportService workflow;

    public InstitutionImportJobService(@Qualifier("institutionImportExecutor") Executor executor,
            InstitutionImportService workflow) {
        this.executor = executor;
        this.workflow = workflow;
    }

    public void validate(Long importId) {
        submit(importId, () -> workflow.validateImport(importId));
    }

    public void apply(Long importId) {
        submit(importId, () -> workflow.applyImport(importId));
    }

    private void submit(Long importId, Supplier<?> operation) {
        try {
            executor.execute(() -> {
                try {
                    operation.get();
                } catch (RuntimeException failure) {
                    workflow.markJobFailed(importId, failure.getMessage());
                }
            });
        } catch (RuntimeException rejected) {
            workflow.markJobFailed(importId, rejected.getMessage());
            throw rejected;
        }
    }
}
