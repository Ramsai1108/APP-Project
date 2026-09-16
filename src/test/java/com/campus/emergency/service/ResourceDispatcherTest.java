package com.campus.emergency.service;

import com.campus.emergency.dao.IIncidentDao;
import com.campus.emergency.dao.IResourceAllocationDao;
import com.campus.emergency.dao.IResourceDao;
import com.campus.emergency.dao.IResponderDao;
import com.campus.emergency.exception.ResourceUnavailableException;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.Resource;
import com.campus.emergency.model.ResourceAllocation;
import com.campus.emergency.model.Responder;
import com.campus.emergency.service.impl.ResourceDispatcherImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceDispatcherTest {

    private ResourceDispatcher resourceDispatcher;
    private MockResponderDao mockResponderDao;
    private MockResourceDao mockResourceDao;
    private MockAllocationDao mockAllocationDao;
    private MockIncidentDao mockIncidentDao;

    private final Long dispatcherUserId = 2L;
    private final Long incidentId = 101L;

    @BeforeEach
    public void setUp() {
        mockResponderDao = new MockResponderDao();
        mockResourceDao = new MockResourceDao();
        mockAllocationDao = new MockAllocationDao();
        mockIncidentDao = new MockIncidentDao();

        resourceDispatcher = new ResourceDispatcherImpl(
                mockResponderDao,
                mockResourceDao,
                mockAllocationDao,
                mockIncidentDao
        );

        // Populate sample responders
        Responder fireOfficer = new Responder();
        fireOfficer.setId(1L);
        fireOfficer.setFullName("Officer Thomas Wayne");
        fireOfficer.setSpecialization("FIRE_SAFETY");
        fireOfficer.setDutyStatus("ON_DUTY");
        mockResponderDao.create(fireOfficer);

        Responder nurse = new Responder();
        nurse.setId(2L);
        nurse.setFullName("Head Nurse Clara");
        nurse.setSpecialization("MEDICAL");
        nurse.setDutyStatus("ON_DUTY");
        mockResponderDao.create(nurse);

        Responder secOfficer = new Responder();
        secOfficer.setId(3L);
        secOfficer.setFullName("Chief Mike Ross");
        secOfficer.setSpecialization("CAMPUS_SECURITY");
        secOfficer.setDutyStatus("ON_DUTY");
        mockResponderDao.create(secOfficer);

        // Populate sample resources
        Resource fireExt = new Resource();
        fireExt.setId(10L);
        fireExt.setResourceName("CO2 Fire Extinguisher Unit #101");
        fireExt.setResourceType("FIRE_EXTINGUISHER");
        fireExt.setCurrentStatus("AVAILABLE");
        mockResourceDao.create(fireExt);

        Resource ambulance = new Resource();
        ambulance.setId(11L);
        ambulance.setResourceName("Campus Ambulance 01");
        ambulance.setResourceType("AMBULANCE");
        ambulance.setCurrentStatus("AVAILABLE");
        mockResourceDao.create(ambulance);

        // Populate incident
        EmergencyIncident incident = new EmergencyIncident();
        incident.setId(incidentId);
        incident.setStatus("REPORTED");
        mockIncidentDao.create(incident);
    }

    @Test
    public void testFindAvailableRespondersForMedical() {
        List<Responder> medicalResponders = resourceDispatcher.findAvailableResponders("MEDICAL");
        assertFalse(medicalResponders.isEmpty());
        assertEquals("MEDICAL", medicalResponders.get(0).getSpecialization());
    }

    @Test
    public void testFindSuitableResourcesForFire() {
        List<Resource> fireResources = resourceDispatcher.findSuitableResources("FIRE");
        assertFalse(fireResources.isEmpty());
        assertEquals("FIRE_EXTINGUISHER", fireResources.get(0).getResourceType());
    }

    @Test
    public void testSuccessfulAutomatedFireDispatch() {
        DispatchResult result = resourceDispatcher.dispatchForIncident(incidentId, "FIRE", dispatcherUserId);

        assertTrue(result.success());
        assertNotNull(result.assignedResponder());
        assertEquals("Officer Thomas Wayne", result.assignedResponder().getFullName());
        assertEquals("DISPATCHED", result.assignedResponder().getDutyStatus());

        assertFalse(result.assignedResources().isEmpty());
        assertEquals("IN_USE", result.assignedResources().get(0).getCurrentStatus());

        assertEquals(1, mockAllocationDao.getAllocations().size());
        assertEquals("DISPATCHED", mockIncidentDao.findById(incidentId).get().getStatus());
    }

    @Test
    public void testSuccessfulAutomatedMedicalDispatch() {
        DispatchResult result = resourceDispatcher.dispatchForIncident(incidentId, "MEDICAL", dispatcherUserId);

        assertTrue(result.success());
        assertEquals("Head Nurse Clara", result.assignedResponder().getFullName());
        assertEquals("DISPATCHED", result.assignedResponder().getDutyStatus());
        assertEquals("IN_USE", result.assignedResources().get(0).getCurrentStatus());
    }

    @Test
    public void testManualDispatch() {
        DispatchResult result = resourceDispatcher.dispatchManual(incidentId, 3L, List.of(11L), dispatcherUserId);

        assertTrue(result.success());
        assertEquals("Chief Mike Ross", result.assignedResponder().getFullName());
        assertEquals("DISPATCHED", result.assignedResponder().getDutyStatus());
        assertEquals(11L, result.assignedResources().get(0).getId());
        assertEquals("IN_USE", result.assignedResources().get(0).getCurrentStatus());
    }

    @Test
    public void testDispatchFailsWhenNoRespondersOnDuty() {
        // Set all responders OFF_DUTY
        mockResponderDao.findAll().forEach(r -> r.setDutyStatus("OFF_DUTY"));

        assertThrows(ResourceUnavailableException.class, () -> {
            resourceDispatcher.dispatchForIncident(incidentId, "FIRE", dispatcherUserId);
        });
    }

    @Test
    public void testManualDispatchFailsForBusyResponder() {
        Responder busyResponder = mockResponderDao.findById(1L).get();
        busyResponder.setDutyStatus("BUSY");

        assertThrows(ResourceUnavailableException.class, () -> {
            resourceDispatcher.dispatchManual(incidentId, 1L, List.of(10L), dispatcherUserId);
        });
    }

    // =========================================================================
    // Mock DAOs
    // =========================================================================
    private static class MockResponderDao implements IResponderDao {
        private final List<Responder> responders = new ArrayList<>();

        @Override public Responder create(Responder entity) { responders.add(entity); return entity; }
        @Override public Optional<Responder> findById(Long id) { return responders.stream().filter(r -> r.getId().equals(id)).findFirst(); }
        @Override public List<Responder> findByDutyStatus(String dutyStatus) { return responders.stream().filter(r -> r.getDutyStatus().equalsIgnoreCase(dutyStatus)).toList(); }
        @Override public List<Responder> findAvailableBySpecialization(String specialization) {
            return responders.stream().filter(r -> r.getSpecialization().equalsIgnoreCase(specialization) && "ON_DUTY".equalsIgnoreCase(r.getDutyStatus())).toList();
        }
        @Override public List<Responder> findAll() { return new ArrayList<>(responders); }
        @Override public boolean update(Responder entity) { return true; }
        @Override public boolean deleteById(Long id) { return true; }
    }

    private static class MockResourceDao implements IResourceDao {
        private final List<Resource> resources = new ArrayList<>();

        @Override public Resource create(Resource entity) { resources.add(entity); return entity; }
        @Override public Optional<Resource> findById(Long id) { return resources.stream().filter(r -> r.getId().equals(id)).findFirst(); }
        @Override public List<Resource> findByStatus(String status) { return resources.stream().filter(r -> r.getCurrentStatus().equalsIgnoreCase(status)).toList(); }
        @Override public List<Resource> findByTypeAndStatus(String type, String status) {
            return resources.stream().filter(r -> r.getResourceType().equalsIgnoreCase(type) && r.getCurrentStatus().equalsIgnoreCase(status)).toList();
        }
        @Override public List<Resource> findAll() { return new ArrayList<>(resources); }
        @Override public boolean update(Resource entity) { return true; }
        @Override public boolean deleteById(Long id) { return true; }
    }

    private static class MockAllocationDao implements IResourceAllocationDao {
        private final List<ResourceAllocation> allocations = new ArrayList<>();
        private long idCounter = 1L;

        public List<ResourceAllocation> getAllocations() { return allocations; }

        @Override public ResourceAllocation create(ResourceAllocation entity) { entity.setId(idCounter++); allocations.add(entity); return entity; }
        @Override public Optional<ResourceAllocation> findById(Long id) { return allocations.stream().filter(a -> a.getId().equals(id)).findFirst(); }
        @Override public List<ResourceAllocation> findByIncidentId(Long incidentId) { return allocations.stream().filter(a -> a.getIncidentId().equals(incidentId)).toList(); }
        @Override public List<ResourceAllocation> findActiveAllocations() { return allocations.stream().filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus())).toList(); }
        @Override public List<ResourceAllocation> findAll() { return new ArrayList<>(allocations); }
        @Override public boolean update(ResourceAllocation entity) { return true; }
        @Override public boolean deleteById(Long id) { return true; }
    }

    private static class MockIncidentDao implements IIncidentDao {
        private final List<EmergencyIncident> incidents = new ArrayList<>();

        @Override public EmergencyIncident create(EmergencyIncident entity) { incidents.add(entity); return entity; }
        @Override public Optional<EmergencyIncident> findById(Long id) { return incidents.stream().filter(i -> i.getId().equals(id)).findFirst(); }
        @Override public List<EmergencyIncident> findByStatus(String status) { return incidents.stream().filter(i -> i.getStatus().equalsIgnoreCase(status)).toList(); }
        @Override public List<EmergencyIncident> findActiveIncidentsSortedByPriority() { return new ArrayList<>(incidents); }
        @Override public List<EmergencyIncident> findAll() { return new ArrayList<>(incidents); }
        @Override public boolean update(EmergencyIncident entity) { return true; }
        @Override public boolean deleteById(Long id) { return true; }
    }
}
