package com.app.admin.dto;

import java.util.List;

import com.app.admin.model.Event;

public record EventDetailsResponse(Event event, List<String> allowedActions) {
}
