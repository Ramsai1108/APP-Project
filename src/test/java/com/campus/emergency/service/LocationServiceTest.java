package com.campus.emergency.service;

import com.campus.emergency.model.Location;
import com.campus.emergency.model.LocationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LocationServiceTest {

    @Test
    @DisplayName("1. LocationResult Model Properties & Factory Methods")
    public void testLocationResultModel() {
        LocationResult ok = LocationResult.ok(37.7749, -122.4194, "Main Quad, Campus Center", 12.5);
        assertTrue(ok.isSuccess());
        assertEquals(37.7749, ok.getLatitude(), 0.0001);
        assertEquals(-122.4194, ok.getLongitude(), 0.0001);
        assertEquals("Main Quad, Campus Center", ok.getDisplayName());
        assertEquals(12.5, ok.getAccuracy());
        assertNull(ok.getErrorMessage());

        LocationResult fail = LocationResult.fail("Location unavailable");
        assertFalse(fail.isSuccess());
        assertEquals("Location unavailable", fail.getErrorMessage());
        assertNull(fail.getDisplayName());
    }

    @Test
    @DisplayName("2. Parse Nominatim JSON Payload Successfully")
    public void testParseNominatimResponse() {
        LocationService service = new LocationService();

        String json = "{\n" +
                "  \"place_id\": 123456,\n" +
                "  \"lat\": \"37.7749\",\n" +
                "  \"lon\": \"-122.4194\",\n" +
                "  \"display_name\": \"Science Hall, University Avenue, Campus District, Tech City, State, 94000, USA\",\n" +
                "  \"address\": {\n" +
                "    \"building\": \"Science Hall\",\n" +
                "    \"road\": \"University Avenue\",\n" +
                "    \"suburb\": \"Campus District\",\n" +
                "    \"city\": \"Tech City\",\n" +
                "    \"country\": \"USA\"\n" +
                "  }\n" +
                "}";

        String parsed = service.parseNominatimResponse(json);
        assertNotNull(parsed);
        assertTrue(parsed.contains("Science Hall"));
        assertTrue(parsed.contains("University Avenue"));
    }

    @Test
    @DisplayName("3. Parse Fallback When Display Name Only")
    public void testParseFallbackDisplayName() {
        LocationService service = new LocationService();

        String json = "{\n" +
                "  \"lat\": \"37.7749\",\n" +
                "  \"lon\": \"-122.4194\",\n" +
                "  \"display_name\": \"Library Building, Campus West\"\n" +
                "}";

        String parsed = service.parseNominatimResponse(json);
        assertEquals("Library Building, Campus West", parsed);
    }

    @Test
    @DisplayName("4. Graceful Handling on Null or Empty JSON")
    public void testNullOrEmptyJsonHandling() {
        LocationService service = new LocationService();
        assertNull(service.parseNominatimResponse(null));
        assertNull(service.parseNominatimResponse(""));
        assertNull(service.parseNominatimResponse("   "));
    }

    @Test
    @DisplayName("5. Coordinate Validation Logic")
    public void testCoordinateValidation() {
        LocationService service = new LocationService();

        assertTrue(service.isValidCoordinate(37.7749, -122.4194));
        assertTrue(service.isValidCoordinate(-33.8688, 151.2093));
        assertTrue(service.isValidCoordinate(0.0001, 0.0001));

        // Invalid cases
        assertFalse(service.isValidCoordinate(0.0, 0.0)); // Null island / unset
        assertFalse(service.isValidCoordinate(91.0, 50.0)); // Lat out of bounds
        assertFalse(service.isValidCoordinate(-95.0, 50.0));
        assertFalse(service.isValidCoordinate(45.0, 185.0)); // Lon out of bounds
        assertFalse(service.isValidCoordinate(45.0, -185.0));
    }

    @Test
    @DisplayName("6. Reverse Geocode Rejects Invalid Coordinates Without Network")
    public void testReverseGeocodeInvalidCoordinates() {
        LocationService service = new LocationService();
        assertNull(service.reverseGeocode(999.0, 999.0));
        assertNull(service.reverseGeocode(0.0, 0.0));
    }

    @Test
    @DisplayName("7. Haversine Distance Calculation")
    public void testHaversineDistance() {
        LocationService service = new LocationService();

        // Distance between identical points is 0
        double distZero = service.calculateHaversineDistance(37.7749, -122.4194, 37.7749, -122.4194);
        assertEquals(0.0, distZero, 0.001);

        // Distance between two close campus points (~40 meters)
        double distClose = service.calculateHaversineDistance(37.774900, -122.419400, 37.775100, -122.419000);
        assertTrue(distClose > 20.0 && distClose < 80.0, "Expected close distance around 40m, got: " + distClose);
    }

    @Test
    @DisplayName("8. Nearest Campus Location Matching")
    public void testNearestCampusLocationMatching() {
        LocationService service = new LocationService();

        List<Location> campusLocations = new ArrayList<>();
        Location scienceHall = new Location(1, "Science Hall", "2", "204", 37.774900, -122.419400, "Lab");
        Location studentUnion = new Location(2, "Student Union", "1", "101", 37.775100, -122.419000, "Union");
        Location northResidence = new Location(3, "North Residence Hall", "3", "312", 37.779000, -122.415000, "Dorm");

        campusLocations.add(scienceHall);
        campusLocations.add(studentUnion);
        campusLocations.add(northResidence);

        // Point near Science Hall
        Location nearest = service.findNearestCampusLocation(37.774905, -122.419410, campusLocations);
        assertNotNull(nearest);
        assertEquals(1, nearest.getId());
        assertEquals("Science Hall", nearest.getBuildingName());

        // Point near North Residence
        Location nearestNorth = service.findNearestCampusLocation(37.778900, -122.415100, campusLocations);
        assertNotNull(nearestNorth);
        assertEquals(3, nearestNorth.getId());
        assertEquals("North Residence Hall", nearestNorth.getBuildingName());

        // Empty list returns null
        assertNull(service.findNearestCampusLocation(37.77, -122.41, new ArrayList<>()));
        assertNull(service.findNearestCampusLocation(37.77, -122.41, null));
    }

    @Test
    @DisplayName("9. Location Detection Returns Non-Null Result")
    public void testDetectCurrentLocationReturnsValidObject() {
        LocationService service = new LocationService();
        LocationResult result = service.detectCurrentLocation();

        assertNotNull(result);
        if (result.isSuccess()) {
            assertNotNull(result.getDisplayName());
            assertTrue(service.isValidCoordinate(result.getLatitude(), result.getLongitude()));
        } else {
            assertNotNull(result.getErrorMessage());
        }
    }
}
