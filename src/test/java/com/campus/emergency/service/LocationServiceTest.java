package com.campus.emergency.service;

import com.campus.emergency.model.LocationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        LocationResult fail = LocationResult.fail("GPS signal unavailable");
        assertFalse(fail.isSuccess());
        assertEquals("GPS signal unavailable", fail.getErrorMessage());
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
    }

    @Test
    @DisplayName("5. Location Detection Returns Non-Null Result")
    public void testDetectCurrentLocationReturnsValidObject() {
        LocationService service = new LocationService();
        LocationResult result = service.detectCurrentLocation();

        assertNotNull(result);
        // Result must either succeed with valid coordinates or fail gracefully with error message
        if (result.isSuccess()) {
            assertNotNull(result.getDisplayName());
            assertNotEquals(0.0, result.getLatitude());
            assertNotEquals(0.0, result.getLongitude());
        } else {
            assertNotNull(result.getErrorMessage());
        }
    }
}
