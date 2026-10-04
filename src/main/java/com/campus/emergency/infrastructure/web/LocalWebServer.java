package com.campus.emergency.infrastructure.web;

import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.Location;
import com.campus.emergency.model.User;
import com.campus.emergency.service.ServiceRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalWebServer {

    private static HttpServer server;
    private static int activePort = 3000;

    public static synchronized int startServer(int preferredPort, ServiceRegistry serviceRegistry) {
        if (server != null) {
            return activePort;
        }

        int[] portsToTry = new int[]{preferredPort, 3001, 8080, 8081, 8082};
        for (int port : portsToTry) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                activePort = port;
                break;
            } catch (IOException e) {
                // Try next port
            }
        }

        if (server == null) {
            LoggingService.warn("Could not start LocalWebServer on any candidate port.");
            return -1;
        }

        server.createContext("/", new IndexHandler(serviceRegistry));
        server.createContext("/admin", new AdminHandler(serviceRegistry));
        server.createContext("/api/locations", new LocationsApiHandler(serviceRegistry));
        server.createContext("/api/emergencies", new EmergenciesApiHandler(serviceRegistry));

        server.setExecutor(null);
        server.start();

        LoggingService.info("Localhost Web Application running at http://localhost:" + activePort);
        return activePort;
    }

    public static synchronized void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
            LoggingService.info("Localhost Web Server stopped.");
        }
    }

    public static int getActivePort() {
        return activePort;
    }

    private static class IndexHandler implements HttpHandler {
        private final ServiceRegistry registry;

        public IndexHandler(ServiceRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 450, "Method not allowed", "text/plain");
                return;
            }

            String html = getIndexHtmlPage(activePort);
            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }
    }

    private static class AdminHandler implements HttpHandler {
        private final ServiceRegistry registry;

        public AdminHandler(ServiceRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = getAdminHtmlPage(activePort);
            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }
    }

    private static class LocationsApiHandler implements HttpHandler {
        private final ServiceRegistry registry;

        public LocationsApiHandler(ServiceRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Location> locations = registry.getLocationDAO().findAll();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < locations.size(); i++) {
                    Location loc = locations.get(i);
                    if (i > 0) json.append(",");
                    json.append(String.format("{\"id\":%d,\"buildingName\":\"%s\",\"floor\":\"%s\",\"roomNumber\":\"%s\",\"latitude\":%.6f,\"longitude\":%.6f}",
                            loc.getId(),
                            escapeJson(loc.getBuildingName()),
                            escapeJson(loc.getFloor()),
                            escapeJson(loc.getRoomNumber()),
                            loc.getLatitude(),
                            loc.getLongitude()));
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString(), "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private static class EmergenciesApiHandler implements HttpHandler {
        private final ServiceRegistry registry;

        public EmergenciesApiHandler(ServiceRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                handleGet(exchange);
            } else if ("POST".equals(method)) {
                handlePost(exchange);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}", "application/json");
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            try {
                List<EmergencyReport> reports = registry.getReportDAO().findAll();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < reports.size(); i++) {
                    EmergencyReport rep = reports.get(i);
                    if (i > 0) json.append(",");
                    json.append(String.format("{\"id\":%d,\"reporterId\":%d,\"category\":\"%s\",\"description\":\"%s\",\"locationId\":%d,\"status\":\"%s\",\"latitude\":%s,\"longitude\":%s,\"accuracy\":%s,\"source\":\"%s\",\"campusLocationId\":%s,\"createdAt\":\"%s\"}",
                            rep.getId(),
                            rep.getReporterId(),
                            escapeJson(rep.getCategory()),
                            escapeJson(rep.getDescription()),
                            rep.getLocationId(),
                            escapeJson(rep.getStatus()),
                            rep.getLatitude() != null ? String.format("%.6f", rep.getLatitude()) : "null",
                            rep.getLongitude() != null ? String.format("%.6f", rep.getLongitude()) : "null",
                            rep.getLocationAccuracy() != null ? String.format("%.1f", rep.getLocationAccuracy()) : "null",
                            escapeJson(rep.getLocationSource() != null ? rep.getLocationSource() : "CAMPUS_MANUAL"),
                            rep.getCampusLocationId() != null ? rep.getCampusLocationId() : "null",
                            rep.getCreatedAt() != null ? rep.getCreatedAt().toString() : ""
                    ));
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString(), "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            try {
                String category = extractJsonStr(body, "category");
                String description = extractJsonStr(body, "description");
                String source = extractJsonStr(body, "source");
                Double lat = extractJsonDouble(body, "latitude");
                Double lon = extractJsonDouble(body, "longitude");
                Double acc = extractJsonDouble(body, "accuracy");
                Integer campusLocId = extractJsonInt(body, "campusLocationId");

                if (category == null || category.isEmpty()) category = "OTHER";
                if (description == null || description.trim().length() < 5) {
                    sendResponse(exchange, 400, "{\"error\":\"Please provide a situation description (at least 5 characters).\"}", "application/json");
                    return;
                }

                // Coordinate validation
                if (lat != null || lon != null) {
                    if (lat == null || lon == null || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                        sendResponse(exchange, 400, "{\"error\":\"Invalid coordinates: Latitude (-90 to 90), Longitude (-180 to 180).\"}", "application/json");
                        return;
                    }
                }

                if (lat == null && (campusLocId == null || campusLocId <= 0)) {
                    sendResponse(exchange, 400, "{\"error\":\"Please provide your current location or select a campus location.\"}", "application/json");
                    return;
                }

                User demoUser = registry.getUserDAO().findByUsername("student_jane").orElseGet(() -> {
                    List<User> all = registry.getUserDAO().findAll();
                    return !all.isEmpty() ? all.get(0) : new User(1, "demo", "hash", "Campus User", "user@campus.edu", "555", com.campus.emergency.model.Role.USER);
                });

                int locId = (campusLocId != null && campusLocId > 0) ? campusLocId : 1;

                EmergencyReport report = registry.getEmergencyManager().createReport(
                        demoUser,
                        category,
                        description,
                        locId,
                        1,
                        null,
                        null,
                        lat,
                        lon,
                        acc,
                        LocalDateTime.now(),
                        "GPS".equalsIgnoreCase(source) ? "GPS" : "CAMPUS_MANUAL",
                        campusLocId
                );

                String jsonResp = String.format("{\"success\":true,\"id\":%d,\"status\":\"%s\",\"message\":\"Emergency report submitted successfully\"}", report.getId(), report.getStatus());
                sendResponse(exchange, 201, jsonResp, "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String extractJsonStr(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    private static Double extractJsonDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?[0-9.]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try { return Double.parseDouble(matcher.group(1)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Integer extractJsonInt(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9]+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try { return Integer.parseInt(matcher.group(1)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String getIndexHtmlPage(int port) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Smart Campus Emergency Response System - Localhost Portal</title>\n" +
                "  <style>\n" +
                "    :root {\n" +
                "      --primary: #0F172A;\n" +
                "      --accent: #2563EB;\n" +
                "      --danger: #DC2626;\n" +
                "      --success: #16A34A;\n" +
                "      --warning: #D97706;\n" +
                "      --bg: #F8FAFC;\n" +
                "      --card-bg: #FFFFFF;\n" +
                "      --border: #E2E8F0;\n" +
                "      --text-main: #1E293B;\n" +
                "      --text-muted: #64748B;\n" +
                "    }\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; }\n" +
                "    body { background-color: var(--bg); color: var(--text-main); padding: 24px; }\n" +
                "    .container { max-width: 680px; margin: 0 auto; background: var(--card-bg); border-radius: 16px; border: 1px solid var(--border); box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); padding: 28px; }\n" +
                "    .header { border-bottom: 2px solid var(--border); padding-bottom: 16px; margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; }\n" +
                "    .header h1 { font-size: 1.5rem; color: var(--danger); font-weight: 800; display: flex; align-items: center; gap: 8px; }\n" +
                "    .badge { font-size: 0.75rem; font-weight: 700; background: #EEF2FF; color: var(--accent); padding: 4px 10px; border-radius: 20px; border: 1px solid #C7D2FE; }\n" +
                "    .section { margin-bottom: 24px; }\n" +
                "    .section-title { font-size: 0.95rem; font-weight: 700; color: var(--text-main); margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px; }\n" +
                "    .cat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }\n" +
                "    .cat-btn { padding: 14px 10px; border: 2px solid var(--border); border-radius: 10px; background: #FFF; cursor: pointer; text-align: center; font-size: 0.85rem; font-weight: 700; transition: all 0.2s; color: var(--text-main); }\n" +
                "    .cat-btn:hover { border-color: var(--accent); background: #F0F9FF; }\n" +
                "    .cat-btn.selected { border-color: var(--danger); background: #FEF2F2; color: var(--danger); }\n" +
                "    .btn-location { display: inline-flex; align-items: center; gap: 8px; padding: 12px 20px; background: #F1F5F9; border: 1.5px solid #CBD5E1; border-radius: 10px; font-weight: 700; color: var(--text-main); cursor: pointer; font-size: 0.9rem; transition: all 0.2s; }\n" +
                "    .btn-location:hover { background: #E2E8F0; border-color: var(--accent); }\n" +
                "    .btn-location:disabled { opacity: 0.65; cursor: not-allowed; }\n" +
                "    .status-card { margin-top: 12px; padding: 14px 16px; border-radius: 10px; font-size: 0.9rem; display: none; line-height: 1.5; }\n" +
                "    .status-card.success { background: #F0FDF4; border: 1px solid #BBF7D0; color: #166534; }\n" +
                "    .status-card.error { background: #FEF2F2; border: 1px solid #FECACA; color: #991B1B; }\n" +
                "    .status-card.detecting { background: #EFF6FF; border: 1px solid #BFDBFE; color: #1E40AF; }\n" +
                "    .coords-list { font-family: monospace; font-size: 0.85rem; margin-top: 6px; background: rgba(255,255,255,0.7); padding: 6px 10px; border-radius: 6px; }\n" +
                "    select, textarea { width: 100%; padding: 12px; border: 1.5px solid var(--border); border-radius: 10px; font-size: 0.9rem; outline: none; transition: border-color 0.2s; }\n" +
                "    select:focus, textarea:focus { border-color: var(--accent); }\n" +
                "    .btn-submit { width: 100%; padding: 16px; background: var(--danger); color: white; border: none; border-radius: 12px; font-size: 1rem; font-weight: 800; cursor: pointer; text-transform: uppercase; letter-spacing: 1px; box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3); transition: background 0.2s; }\n" +
                "    .btn-submit:hover { background: #B91C1C; }\n" +
                "    .footer-note { font-size: 0.8rem; color: var(--text-muted); text-align: center; margin-top: 18px; }\n" +
                "    .nav-links { display: flex; gap: 12px; font-size: 0.85rem; font-weight: 600; }\n" +
                "    .nav-links a { color: var(--accent); text-decoration: none; }\n" +
                "    .nav-links a:hover { text-decoration: underline; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"header\">\n" +
                "      <h1>🚨 Smart Campus Emergency</h1>\n" +
                "      <div style=\"text-align: right;\">\n" +
                "        <span class=\"badge\">LOCALHOST :" + port + "</span>\n" +
                "        <div class=\"nav-links\" style=\"margin-top: 4px;\">\n" +
                "          <a href=\"/\">Report Emergency</a> | <a href=\"/admin\">Admin Operations</a>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <form id=\"emergencyForm\" onsubmit=\"handleSubmit(event)\">\n" +
                "      <div class=\"section\">\n" +
                "        <div class=\"section-title\">1. Select Emergency Category *</div>\n" +
                "        <div class=\"cat-grid\">\n" +
                "          <div class=\"cat-btn selected\" onclick=\"selectCategory('FIRE', this)\">🔥 Fire / Smoke</div>\n" +
                "          <div class=\"cat-btn\" onclick=\"selectCategory('MEDICAL', this)\">🚑 Medical</div>\n" +
                "          <div class=\"cat-btn\" onclick=\"selectCategory('HAZARD', this)\">⚠️ Hazard / Gas</div>\n" +
                "          <div class=\"cat-btn\" onclick=\"selectCategory('SECURITY', this)\">🔒 Security</div>\n" +
                "          <div class=\"cat-btn\" onclick=\"selectCategory('FACILITY', this)\">🏢 Facility</div>\n" +
                "          <div class=\"cat-btn\" onclick=\"selectCategory('OTHER', this)\">❓ Other Alert</div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "\n" +
                "      <div class=\"section\">\n" +
                "        <div class=\"section-title\">2. Emergency Location *</div>\n" +
                "        <button type=\"button\" id=\"btnGps\" class=\"btn-location\" onclick=\"requestGpsLocation()\">\n" +
                "          📍 Use My Current Location\n" +
                "        </button>\n" +
                "\n" +
                "        <div id=\"statusCard\" class=\"status-card\"></div>\n" +
                "\n" +
                "        <div style=\"margin-top: 14px;\">\n" +
                "          <label style=\"font-size: 0.8rem; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 4px;\">Campus Location (Fallback / Specific Building):</label>\n" +
                "          <select id=\"campusLocationSelect\">\n" +
                "            <option value=\"\">-- Select Campus Location --</option>\n" +
                "          </select>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "\n" +
                "      <div class=\"section\">\n" +
                "        <div class=\"section-title\">3. Situation Description *</div>\n" +
                "        <textarea id=\"description\" rows=\"3\" placeholder=\"Describe the situation, exact location details, and immediate danger...\"></textarea>\n" +
                "      </div>\n" +
                "\n" +
                "      <button type=\"submit\" id=\"btnSubmit\" class=\"btn-submit\">🚨 SUBMIT EMERGENCY REPORT</button>\n" +
                "    </form>\n" +
                "\n" +
                "    <div class=\"footer-note\">Localhost Development & Testing Environment • OpenStreetMap Geolocation API</div>\n" +
                "  </div>\n" +
                "\n" +
                "  <script>\n" +
                "    let selectedCat = 'FIRE';\n" +
                "    let gpsState = null;\n" +
                "\n" +
                "    function selectCategory(cat, el) {\n" +
                "      selectedCat = cat;\n" +
                "      document.querySelectorAll('.cat-btn').forEach(b => b.classList.remove('selected'));\n" +
                "      el.classList.add('selected');\n" +
                "    }\n" +
                "\n" +
                "    // Load Campus Locations Dropdown\n" +
                "    fetch('/api/locations')\n" +
                "      .then(res => res.json())\n" +
                "      .then(locations => {\n" +
                "        const sel = document.getElementById('campusLocationSelect');\n" +
                "        locations.forEach(l => {\n" +
                "          const opt = document.createElement('option');\n" +
                "          opt.value = l.id;\n" +
                "          opt.textContent = `${l.buildingName} (Floor ${l.floor}, Room ${l.roomNumber})`;\n" +
                "          sel.appendChild(opt);\n" +
                "        });\n" +
                "      });\n" +
                "\n" +
                "    function requestGpsLocation() {\n" +
                "      const btn = document.getElementById('btnGps');\n" +
                "      const card = document.getElementById('statusCard');\n" +
                "      \n" +
                "      btn.disabled = true;\n" +
                "      btn.innerHTML = '🔄 Detecting current location...';\n" +
                "      \n" +
                "      card.className = 'status-card detecting';\n" +
                "      card.style.display = 'block';\n" +
                "      card.innerHTML = '🔍 Requesting browser geolocation permission on <strong>localhost</strong>...';\n" +
                "\n" +
                "      if (!navigator.geolocation) {\n" +
                "        renderGpsError('⚠ Browser geolocation is not supported on this device.');\n" +
                "        btn.disabled = false;\n" +
                "        btn.innerHTML = '📍 Use My Current Location';\n" +
                "        return;\n" +
                "      }\n" +
                "\n" +
                "      const options = { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 };\n" +
                "\n" +
                "      navigator.geolocation.getCurrentPosition(\n" +
                "        function(position) {\n" +
                "          btn.disabled = false;\n" +
                "          btn.innerHTML = '🔄 Refresh Location';\n" +
                "          \n" +
                "          const coords = position.coords;\n" +
                "          const lat = coords.latitude;\n" +
                "          const lon = coords.longitude;\n" +
                "          const acc = coords.accuracy;\n" +
                "          const ts = position.timestamp || Date.now();\n" +
                "\n" +
                "          gpsState = {\n" +
                "            latitude: lat,\n" +
                "            longitude: lon,\n" +
                "            accuracy: acc,\n" +
                "            timestamp: ts,\n" +
                "            source: 'GPS'\n" +
                "          };\n" +
                "\n" +
                "          card.className = 'status-card success';\n" +
                "          card.innerHTML = `\n" +
                "            <strong>✓ Current Location Captured</strong><br>\n" +
                "            <div class=\"coords-list\">\n" +
                "              Latitude: ${lat.toFixed(6)}<br>\n" +
                "              Longitude: ${lon.toFixed(6)}<br>\n" +
                "              Accuracy: ±${Math.round(acc)} m\n" +
                "            </div>\n" +
                "          `;\n" +
                "        },\n" +
                "        function(error) {\n" +
                "          btn.disabled = false;\n" +
                "          btn.innerHTML = '📍 Use My Current Location';\n" +
                "          gpsState = null;\n" +
                "\n" +
                "          let reason = '';\n" +
                "          if (error.code === 1) { // PERMISSION_DENIED\n" +
                "            reason = '⚠ Location permission was denied. Allow location access for localhost or select your campus location manually.';\n" +
                "          } else if (error.code === 2) { // POSITION_UNAVAILABLE\n" +
                "            reason = '⚠ Your device could not determine your current location.';\n" +
                "          } else if (error.code === 3) { // TIMEOUT\n" +
                "            reason = '⚠ Location request timed out. Please try again.';\n" +
                "          } else {\n" +
                "            reason = '⚠ ' + (error.message || 'Location request failed.');\n" +
                "          }\n" +
                "\n" +
                "          card.className = 'status-card error';\n" +
                "          card.innerHTML = `\n" +
                "            <strong>⚠ Unable to access current location</strong><br>\n" +
                "            <span style=\"font-size:0.85rem;\">${reason}</span>\n" +
                "          `;\n" +
                "        },\n" +
                "        options\n" +
                "      );\n" +
                "    }\n" +
                "\n" +
                "    function handleSubmit(e) {\n" +
                "      e.preventDefault();\n" +
                "      const desc = document.getElementById('description').value.trim();\n" +
                "      const campusLocId = document.getElementById('campusLocationSelect').value;\n" +
                "\n" +
                "      if (!desc || desc.length < 5) {\n" +
                "        alert('Please provide a detailed situation description (at least 5 characters).');\n" +
                "        return;\n" +
                "      }\n" +
                "\n" +
                "      if (!gpsState && !campusLocId) {\n" +
                "        alert('Please provide your current location or select a campus location.');\n" +
                "        return;\n" +
                "      }\n" +
                "\n" +
                "      const payload = {\n" +
                "        category: selectedCat,\n" +
                "        description: desc,\n" +
                "        source: gpsState ? 'GPS' : 'CAMPUS_MANUAL',\n" +
                "        latitude: gpsState ? gpsState.latitude : null,\n" +
                "        longitude: gpsState ? gpsState.longitude : null,\n" +
                "        accuracy: gpsState ? gpsState.accuracy : null,\n" +
                "        timestamp: gpsState ? gpsState.timestamp : Date.now(),\n" +
                "        campusLocationId: campusLocId ? parseInt(campusLocId) : null\n" +
                "      };\n" +
                "\n" +
                "      const btnSubmit = document.getElementById('btnSubmit');\n" +
                "      btnSubmit.disabled = true;\n" +
                "      btnSubmit.textContent = 'SUBMITTING REPORT...';\n" +
                "\n" +
                "      fetch('/api/emergencies', {\n" +
                "        method: 'POST',\n" +
                "        headers: { 'Content-Type': 'application/json' },\n" +
                "        body: JSON.stringify(payload)\n" +
                "      })\n" +
                "      .then(res => res.json())\n" +
                "      .then(data => {\n" +
                "        btnSubmit.disabled = false;\n" +
                "        btnSubmit.textContent = '🚨 SUBMIT EMERGENCY REPORT';\n" +
                "        if (data.success) {\n" +
                "          alert(`Emergency Report #${data.id} submitted successfully! Status: ${data.status}`);\n" +
                "          window.location.reload();\n" +
                "        } else {\n" +
                "          alert('Submission Error: ' + (data.error || 'Unknown error'));\n" +
                "        }\n" +
                "      })\n" +
                "      .catch(err => {\n" +
                "        btnSubmit.disabled = false;\n" +
                "        btnSubmit.textContent = '🚨 SUBMIT EMERGENCY REPORT';\n" +
                "        alert('Failed to connect to backend server.');\n" +
                "      });\n" +
                "    }\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private static String getAdminHtmlPage(int port) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Emergency Operations Center - Admin Console</title>\n" +
                "  <style>\n" +
                "    body { font-family: system-ui, sans-serif; background: #0F172A; color: #F8FAFC; padding: 24px; }\n" +
                "    .container { max-width: 900px; margin: 0 auto; }\n" +
                "    h1 { color: #EF4444; font-size: 1.6rem; font-weight: 800; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }\n" +
                "    .nav a { color: #60A5FA; text-decoration: none; font-size: 0.9rem; font-weight: 600; }\n" +
                "    .report-card { background: #1E293B; border: 1px solid #334155; border-radius: 12px; padding: 18px; margin-bottom: 14px; line-height: 1.6; }\n" +
                "    .badge-gps { background: #064E3B; color: #34D399; font-weight: 700; font-size: 0.8rem; padding: 3px 8px; border-radius: 4px; }\n" +
                "    .badge-manual { background: #451A03; color: #FBBF24; font-weight: 700; font-size: 0.8rem; padding: 3px 8px; border-radius: 4px; }\n" +
                "    .coords { background: #0F172A; padding: 8px 12px; border-radius: 6px; font-family: monospace; font-size: 0.9rem; color: #38BDF8; margin-top: 8px; }\n" +
                "    .osm-link { color: #60A5FA; text-decoration: none; font-weight: bold; }\n" +
                "    .osm-link:hover { text-decoration: underline; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <h1>🚨 Emergency Operations Center <span class=\"nav\"><a href=\"/\">← Report Form</a></span></h1>\n" +
                "    <div id=\"reportsList\">Loading active incidents...</div>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    fetch('/api/emergencies')\n" +
                "      .then(res => res.json())\n" +
                "      .then(reports => {\n" +
                "        const list = document.getElementById('reportsList');\n" +
                "        if (reports.length === 0) {\n" +
                "          list.innerHTML = '<div style=\"color:#94A3B8;\">No active emergency reports found.</div>';\n" +
                "          return;\n" +
                "        }\n" +
                "        list.innerHTML = reports.map(r => {\n" +
                "          const isGps = r.source === 'GPS';\n" +
                "          const locBadge = isGps ? '<span class=\"badge-gps\">📍 GPS LOCATION</span>' : '<span class=\"badge-manual\">🏢 MANUAL CAMPUS LOCATION</span>';\n" +
                "          let locDetails = '';\n" +
                "          if (isGps && r.latitude && r.longitude) {\n" +
                "            const lat = parseFloat(r.latitude);\n" +
                "            const lon = parseFloat(r.longitude);\n" +
                "            const osmUrl = `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=17/${lat}/${lon}`;\n" +
                "            locDetails = `\n" +
                "              <div class=\"coords\">\n" +
                "                Latitude: ${lat.toFixed(6)} | Longitude: ${lon.toFixed(6)} | Accuracy: ±${r.accuracy ? Math.round(r.accuracy) : 15}m<br>\n" +
                "                🔗 <a href=\"${osmUrl}\" target=\"_blank\" class=\"osm-link\">Open in OpenStreetMap</a>\n" +
                "              </div>`;\n" +
                "          } else {\n" +
                "            locDetails = `<div style=\"color:#CBD5E1; font-size:0.9rem; margin-top:6px;\">Campus Location ID: #${r.locationId} (Source: Manual campus selection)</div>`;\n" +
                "          }\n" +
                "          return `\n" +
                "            <div class=\"report-card\">\n" +
                "              <div style=\"display:flex; justify-content:space-between;\">\n" +
                "                <strong>Report #${r.id} [${r.category}]</strong>\n" +
                "                ${locBadge}\n" +
                "              </div>\n" +
                "              <div style=\"color:#94A3B8; font-size:0.9rem; margin-top:4px;\">Description: ${r.description}</div>\n" +
                "              ${locDetails}\n" +
                "            </div>`;\n" +
                "        }).join('');\n" +
                "      });\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
