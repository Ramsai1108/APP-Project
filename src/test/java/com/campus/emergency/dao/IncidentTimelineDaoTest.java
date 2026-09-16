package com.campus.emergency.dao;

import com.campus.emergency.model.IncidentTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentTimelineDaoTest {

    private IIncidentTimelineDao timelineDao;

    @BeforeEach
    public void setUp() {
        timelineDao = new IIncidentTimelineDao() {
            private final List<IncidentTimeline> storage = new ArrayList<>();
            private final AtomicLong idGen = new AtomicLong(1);

            @Override
            public IncidentTimeline create(IncidentTimeline entity) {
                if (entity.getId() == null) entity.setId(idGen.getAndIncrement());
                storage.add(entity);
                return entity;
            }

            @Override
            public Optional<IncidentTimeline> findById(Long id) {
                return storage.stream().filter(t -> id.equals(t.getId())).findFirst();
            }

            @Override
            public List<IncidentTimeline> findByIncidentId(Long incidentId) {
                return storage.stream().filter(t -> incidentId.equals(t.getIncidentId())).collect(Collectors.toList());
            }

            @Override
            public List<IncidentTimeline> findAll() {
                return new ArrayList<>(storage);
            }

            @Override public boolean update(IncidentTimeline entity) { return true; }
            @Override public boolean deleteById(Long id) { return storage.removeIf(t -> id.equals(t.getId())); }
        };
    }

    @Test
    public void testCreateAndRetrieveTimelineEvent() {
        IncidentTimeline t1 = timelineDao.create(new IncidentTimeline(100L, 2L, "STATUS_CHANGED_DISPATCHED", "Incident dispatched to responder #5"));
        assertNotNull(t1.getId());
        assertEquals("STATUS_CHANGED_DISPATCHED", t1.getActionType());

        List<IncidentTimeline> list = timelineDao.findByIncidentId(100L);
        assertEquals(1, list.size());
        assertEquals("Incident dispatched to responder #5", list.get(0).getDescription());
    }

    @Test
    public void testMultipleEventsForSameIncident() {
        timelineDao.create(new IncidentTimeline(101L, 10L, "CREATED", "Report submitted"));
        timelineDao.create(new IncidentTimeline(101L, 2L, "DISPATCHED", "Responders dispatched"));
        timelineDao.create(new IncidentTimeline(101L, 2L, "RESOLVED", "Incident resolved"));

        List<IncidentTimeline> list = timelineDao.findByIncidentId(101L);
        assertEquals(3, list.size());
    }
}
