package com.campus.emergency.service;

import com.campus.emergency.model.Resource;
import com.campus.emergency.model.Responder;

import java.util.List;

/**
 * Result object produced by ResourceDispatcher containing assigned responder, resources, and status.
 */
public record DispatchResult(
        Long incidentId,
        Responder assignedResponder,
        List<Resource> assignedResources,
        boolean success,
        String dispatchSummary
) {
    public DispatchResult {
        assignedResources = assignedResources != null ? List.copyOf(assignedResources) : List.of();
    }
}
