package com.campus.emergency.dao;

import com.campus.emergency.model.ResourceAllocation;
import java.util.List;

/**
 * Resource Allocation DAO interface.
 */
public interface IResourceAllocationDao extends IGenericDao<ResourceAllocation, Long> {
    List<ResourceAllocation> findByIncidentId(Long incidentId);
    List<ResourceAllocation> findActiveAllocations();
}
