package com.campus.emergency.service.impl;

import com.campus.emergency.dao.IResourceDao;
import com.campus.emergency.dao.IResponderDao;
import com.campus.emergency.model.ResourceAllocation;
import com.campus.emergency.service.IResourceAllocationService;
import java.util.List;

/**
 * Resource Allocation service implementation stub.
 */
public class ResourceAllocationServiceImpl implements IResourceAllocationService {

    private final IResourceDao resourceDao;
    private final IResponderDao responderDao;

    public ResourceAllocationServiceImpl(IResourceDao resourceDao, IResponderDao responderDao) {
        this.resourceDao = resourceDao;
        this.responderDao = responderDao;
    }

    @Override
    public ResourceAllocation allocateResources(Long incidentId, List<Long> resourceIds, List<Long> responderIds, Long dispatcherId) {
        return new ResourceAllocation();
    }

    @Override
    public boolean releaseAllocation(Long allocationId, Long releasedByUserId) {
        return true;
    }
}
