package com.campus.emergency.service;

import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.model.LocationResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationService {

    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final String USER_AGENT = "CampusEmergencyResponseSystem/1.0 (contact: admin@campus.edu)";

    // Default Fallback Campus Coordinates (e.g., Main Campus Quad / Administration)
    private static final double DEFAULT_CAMPUS_LAT = 37.7749;
    private static final double DEFAULT_CAMPUS_LON = -122.4194;

    private final HttpClient httpClient;

    public LocationService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build());
    }

    public LocationService(HttpClient httpClient) {
        this.httpClient = (httpClient != null) ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    /**
     * Attempts to automatically detect device coordinates and reverse-geocode them via OpenStreetMap Nominatim.
     */
    public LocationResult detectCurrentLocation() {
        try {
            // 1. Attempt hardware/OS location
            DeviceCoords coords = queryDeviceCoordinates();

            // 2. Fallback to IP geolocation if OS coordinates not available
            if (coords == null) {
                coords = queryIpGeolocation();
            }

            // 3. Fallback to Campus Default if both fail
            if (coords == null) {
                coords = new DeviceCoords(DEFAULT_CAMPUS_LAT, DEFAULT_CAMPUS_LON, 50.0, true);
            }

            // 4. Reverse geocode via Nominatim
            String displayName = reverseGeocode(coords.latitude, coords.longitude);

            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = String.format(Locale.US, "Campus Location (%.4f, %.4f)", coords.latitude, coords.longitude);
            }

            return LocationResult.ok(coords.latitude, coords.longitude, displayName, coords.accuracy);

        } catch (Exception e) {
            LoggingService.warn("Location detection failed: " + e.getMessage());
            return LocationResult.fail("Unable to automatically detect location: " + e.getMessage());
        }
    }

    /**
     * Reverse-geocode coordinates using OpenStreetMap Nominatim HTTPS API.
     */
    public String reverseGeocode(double latitude, double longitude) {
        try {
            String url = String.format(Locale.US, "%s?lat=%.6f&lon=%.6f&format=json&addressdetails=1",
                    NOMINATIM_REVERSE_URL, latitude, longitude);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseNominatimResponse(response.body());
            } else {
                LoggingService.warn("Nominatim returned HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LoggingService.warn("Nominatim reverse geocoding request failed: " + e.getMessage());
        }

        return String.format(Locale.US, "Coordinates: %.4f, %.4f", latitude, longitude);
    }

    /**
     * Parse Nominatim JSON payload to extract readable address / display name.
     */
    public String parseNominatimResponse(String json) {
        if (json == null || json.isEmpty()) return null;

        // Try extracting display_name
        Pattern displayPattern = Pattern.compile("\"display_name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher displayMatcher = displayPattern.matcher(json);
        String fullDisplayName = displayMatcher.find() ? unescapeJson(displayMatcher.group(1)) : null;

        // Try extracting concise building/road/city from address object
        String building = extractJsonField(json, "building");
        if (building == null) building = extractJsonField(json, "amenity");
        String road = extractJsonField(json, "road");
        String neighbourhood = extractJsonField(json, "neighbourhood");
        String suburb = extractJsonField(json, "suburb");
        String city = extractJsonField(json, "city");
        if (city == null) city = extractJsonField(json, "town");

        StringBuilder concise = new StringBuilder();
        if (building != null && !building.isEmpty()) concise.append(building);
        if (road != null && !road.isEmpty()) {
            if (concise.length() > 0) concise.append(", ");
            concise.append(road);
        }
        if (neighbourhood != null && !neighbourhood.isEmpty()) {
            if (concise.length() > 0) concise.append(", ");
            concise.append(neighbourhood);
        } else if (suburb != null && !suburb.isEmpty()) {
            if (concise.length() > 0) concise.append(", ");
            concise.append(suburb);
        }
        if (city != null && !city.isEmpty()) {
            if (concise.length() > 0) concise.append(", ");
            concise.append(city);
        }

        if (concise.length() > 0) {
            return concise.toString();
        }

        return fullDisplayName;
    }

    private String extractJsonField(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return null;
    }

    private String unescapeJson(String input) {
        if (input == null) return null;
        return input.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    /**
     * Query Windows Location Sensor via PowerShell on Windows systems.
     */
    private DeviceCoords queryDeviceCoordinates() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return null;
        }

        try {
            String psCmd = "$w = New-Object System.Device.Location.GeoCoordinateWatcher; "
                    + "$w.Start(); Start-Sleep -Milliseconds 600; "
                    + "$c = $w.Position.Location; "
                    + "if ($c -and !$c.IsUnknown) { Write-Output ('{0},{1},{2}' -f $c.Latitude, $c.Longitude, $c.HorizontalAccuracy) }";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", psCmd);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.contains(",")) {
                        String[] parts = line.split(",");
                        if (parts.length >= 2) {
                            double lat = Double.parseDouble(parts[0].trim());
                            double lon = Double.parseDouble(parts[1].trim());
                            Double acc = (parts.length >= 3) ? Double.parseDouble(parts[2].trim()) : 15.0;
                            return new DeviceCoords(lat, lon, acc, false);
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            LoggingService.debug("Windows native location check skipped: " + e.getMessage());
        }

        return null;
    }

    /**
     * Query IP-based location fallback.
     */
    private DeviceCoords queryIpGeolocation() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/json/"))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                Pattern latPattern = Pattern.compile("\"latitude\"\\s*:\\s*(-?[0-9.]+)");
                Pattern lonPattern = Pattern.compile("\"longitude\"\\s*:\\s*(-?[0-9.]+)");
                Matcher latM = latPattern.matcher(body);
                Matcher lonM = lonPattern.matcher(body);
                if (latM.find() && lonM.find()) {
                    double lat = Double.parseDouble(latM.group(1));
                    double lon = Double.parseDouble(lonM.group(1));
                    return new DeviceCoords(lat, lon, 100.0, false);
                }
            }
        } catch (Exception e) {
            LoggingService.debug("IP geolocation check skipped: " + e.getMessage());
        }

        return null;
    }

    private static class DeviceCoords {
        final double latitude;
        final double longitude;
        final Double accuracy;
        final boolean isFallback;

        DeviceCoords(double latitude, double longitude, Double accuracy, boolean isFallback) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.isFallback = isFallback;
        }
    }
}
