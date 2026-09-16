package com.campus.emergency.controller;

import com.campus.emergency.model.ResourceAllocation;
import com.campus.emergency.service.IResourceAllocationService;
import java.util.List;

/**
 * Controller mediating resource and responder dispatching actions.
 */
public class ResourceController {

    private final IResourceAllocationService allocationService;

    public ResourceController(IResourceAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    public ResourceAllocation dispatchResources(Long incidentId, List<Long> resourceIds, List<Long> responderIds, Long dispatcherId) {
        return allocationService.allocateResources(incidentId, resourceIds, responderIds, dispatcherId);
    }
}
