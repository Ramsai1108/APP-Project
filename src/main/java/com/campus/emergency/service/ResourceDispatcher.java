package com.campus.emergency.service;

import com.campus.emergency.model.Resource;
import com.campus.emergency.model.Responder;

import java.util.List;

/**
 * Service interface for automated and manual emergency responder & resource dispatch operations.
 */
public interface ResourceDispatcher {

    DispatchResult dispatchForIncident(Long incidentId, String emergencyCategory, Long dispatcherUserId);

    DispatchResult dispatchManual(Long incidentId, Long responderId, List<Long> resourceIds, Long dispatcherUserId);

    List<Responder> findAvailableResponders(String emergencyCategory);

    List<Resource> findSuitableResources(String emergencyCategory);
}
