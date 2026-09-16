package com.campus.emergency.service;

import com.campus.emergency.model.ResourceAllocation;
import java.util.List;

/**
 * Resource Allocation service interface.
 */
public interface IResourceAllocationService {
    ResourceAllocation allocateResources(Long incidentId, List<Long> resourceIds, List<Long> responderIds, Long dispatcherId);
    boolean releaseAllocation(Long allocationId, Long releasedByUserId);
}
