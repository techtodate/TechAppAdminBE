package com.app.admin.event;

import com.app.admin.model.TechUpdate;

public record TechUpdateCreatedEvent(Long techUpdateId, Long techId) {
    public static TechUpdateCreatedEvent from(TechUpdate update) {
        return new TechUpdateCreatedEvent(update.getId(), update.getTechId());
    }
}
