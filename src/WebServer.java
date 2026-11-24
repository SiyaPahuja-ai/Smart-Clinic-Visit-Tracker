import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebServer.java
 * Lightweight HTTP server using standard Java HttpServer (JDK built-in).
 * Connects the Modern Web Dashboard (HTML5/CSS3/JS/Bootstrap 5/Chart.js)
 * to the existing Java Backend (ClinicManager, SQLite JDBC, Queue, Streams).
 * No external frameworks (No Spring Boot, No Hibernate) required!
 */
public class WebServer {
    private static final int PORT = 8080;
    private static final String WEB_ROOT = "web";
    private final ClinicManager manager;
    private HttpServer server;

    public WebServer(ClinicManager manager) {
        this.manager = manager;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Contexts
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/appointments/reschedule", new RescheduleHandler());
        server.createContext("/api/appointments/cancel", new CancelHandler());
        server.createContext("/api/appointments", new AppointmentsHandler());
        server.createContext("/api/queue/call-next", new CallNextQueueHandler());
        server.createContext("/api/queue", new QueueHandler());
        server.createContext("/api/consultation/complete", new CompleteConsultationHandler());
        server.createContext("/api/visits", new VisitsHandler());
        server.createContext("/api/reports", new ReportsHandler());

        // Static Files Context (HTML, CSS, JS)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("=================================================================");
        System.out.println("   🌐  SMART CLINIC WEB DASHBOARD SERVER RUNNING  🌐            ");
        System.out.println("=================================================================");
        System.out.println("   URL: http://localhost:" + PORT + "/");
        System.out.println("   Connected to SQLite Database: clinic.db");
        System.out.println("   FIFO Queue & Java Streams Ready.");
        System.out.println("=================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ========================================================
    // HELPER METHODS
    // ========================================================

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }

    // ========================================================
    // API HANDLERS
    // ========================================================

    /**
     * GET /api/dashboard
     */
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            long totalPatients = manager.getTotalPatientCount();
            long totalAppointments = manager.getTotalAppointmentCount();
            long scheduledCount = manager.getCountByStatus(Appointment.STATUS_SCHEDULED);
            long completedCount = manager.getCountByStatus(Appointment.STATUS_COMPLETED);
            long cancelledCount = manager.getCountByStatus(Appointment.STATUS_CANCELLED);
            double avgWaitTime = manager.getAverageWaitingTime();

            String today = LocalDate.now().toString();
            long todayAppointments = manager.getAllAppointments().stream()
                    .filter(a -> today.equals(a.getAppointmentDate()))
                    .count();

            // Recent 6 appointments sorted by token desc
            List<Appointment> recent = manager.getAppointmentsSorted(Comparator.comparingInt(Appointment::getTokenNumber).reversed())
                    .stream().limit(8).collect(Collectors.toList());

            String json = "{" +
                    "\"totalPatients\":" + totalPatients + "," +
                    "\"totalAppointments\":" + totalAppointments + "," +
                    "\"todayAppointments\":" + todayAppointments + "," +
                    "\"waitingPatients\":" + scheduledCount + "," +
                    "\"completedConsultations\":" + completedCount + "," +
                    "\"cancelledAppointments\":" + cancelledCount + "," +
                    "\"avgWaitingTime\":" + String.format("%.1f", avgWaitTime) + "," +
                    "\"recentAppointments\":" + JsonUtil.listToJson(recent, JsonUtil::appointmentToJson) +
                    "}";

            sendJsonResponse(exchange, 200, json);
        }
    }

    /**
     * GET, POST, PUT /api/patients
     */
    private class PatientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                sendJsonResponse(exchange, 204, "");
                return;
            }

            if ("GET".equals(method)) {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String q = query.get("search");
                List<Patient> list = (q != null && !q.trim().isEmpty()) ?
                        manager.searchPatients(q) : manager.getAllPatients();

                sendJsonResponse(exchange, 200, JsonUtil.listToJson(list, JsonUtil::patientToJson));
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> data = JsonUtil.parseSimpleJson(body);

                String name = data.getOrDefault("name", "").trim();
                String ageStr = data.getOrDefault("age", "0").trim();
                String gender = data.getOrDefault("gender", "Other").trim();
                String phone = data.getOrDefault("phone", "").trim();

                if (name.isEmpty()) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Patient name is required\"}");
                    return;
                }

                int age = 0;
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException ignored) {}

                Patient p = manager.registerPatient(name, age, gender, phone);
                sendJsonResponse(exchange, 201, JsonUtil.patientToJson(p));
            } else if ("PUT".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> data = JsonUtil.parseSimpleJson(body);

                String id = data.getOrDefault("id", "").trim();
                String name = data.getOrDefault("name", "").trim();
                String ageStr = data.getOrDefault("age", "0").trim();
                String gender = data.getOrDefault("gender", "Other").trim();
                String phone = data.getOrDefault("phone", "").trim();

                int age = 0;
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException ignored) {}

                boolean updated = manager.updatePatient(id, name, age, gender, phone);
                if (updated) {
                    Patient p = manager.findPatientById(id);
                    sendJsonResponse(exchange, 200, JsonUtil.patientToJson(p));
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Patient not found\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * GET, POST /api/appointments
     */
    private class AppointmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                sendJsonResponse(exchange, 204, "");
                return;
            }

            if ("GET".equals(method)) {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String status = query.get("status");
                String date = query.get("date");
                String sort = query.get("sort");

                List<Appointment> list = manager.getAllAppointments();

                if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                    list = list.stream().filter(a -> status.equalsIgnoreCase(a.getStatus())).collect(Collectors.toList());
                }

                if (date != null && !date.isEmpty()) {
                    list = list.stream().filter(a -> date.equals(a.getAppointmentDate())).collect(Collectors.toList());
                }

                // Sorting with Comparator
                Comparator<Appointment> comp = ClinicManager.BY_TOKEN;
                if ("time".equalsIgnoreCase(sort)) comp = ClinicManager.BY_TIME;
                else if ("name".equalsIgnoreCase(sort)) comp = ClinicManager.BY_PATIENT_NAME;

                list = list.stream().sorted(comp).collect(Collectors.toList());

                sendJsonResponse(exchange, 200, JsonUtil.listToJson(list, JsonUtil::appointmentToJson));
            } else if ("POST".equals(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> data = JsonUtil.parseSimpleJson(body);

                String patientId = data.getOrDefault("patientId", "").trim();
                String date = data.getOrDefault("date", "").trim();
                String time = data.getOrDefault("time", "").trim();
                String reason = data.getOrDefault("reason", "").trim();

                if (date.isEmpty()) date = LocalDate.now().toString();
                if (time.isEmpty()) time = "10:00";

                Appointment apt = manager.scheduleAppointment(patientId, date, time, reason);
                if (apt != null) {
                    sendJsonResponse(exchange, 201, JsonUtil.appointmentToJson(apt));
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Invalid patient ID or scheduling failed\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    /**
     * POST /api/appointments/reschedule
     */
    private class RescheduleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);
            String aptId = data.getOrDefault("appointmentId", "").trim();
            String newDate = data.getOrDefault("newDate", "").trim();
            String newTime = data.getOrDefault("newTime", "").trim();

            boolean success = manager.rescheduleAppointment(aptId, newDate, newTime);
            if (success) {
                Appointment apt = manager.findAppointmentById(aptId);
                sendJsonResponse(exchange, 200, JsonUtil.appointmentToJson(apt));
            } else {
                sendJsonResponse(exchange, 400, "{\"error\":\"Cannot reschedule appointment. Either not found or already completed/cancelled.\"}");
            }
        }
    }

    /**
     * POST /api/appointments/cancel
     */
    private class CancelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);
            String aptId = data.getOrDefault("appointmentId", "").trim();

            boolean success = manager.cancelAppointment(aptId);
            if (success) {
                Appointment apt = manager.findAppointmentById(aptId);
                sendJsonResponse(exchange, 200, JsonUtil.appointmentToJson(apt));
            } else {
                sendJsonResponse(exchange, 400, "{\"error\":\"Cannot cancel appointment. Either not found or already completed.\"}");
            }
        }
    }

    /**
     * GET /api/queue
     */
    private class QueueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Appointment next = manager.peekNextInQueue();
            List<Appointment> waitingList = manager.getActiveQueueList();

            String json = "{" +
                    "\"queueLength\":" + waitingList.size() + "," +
                    "\"nextPatient\":" + (next != null ? JsonUtil.appointmentToJson(next) : "null") + "," +
                    "\"waitingList\":" + JsonUtil.listToJson(waitingList, JsonUtil::appointmentToJson) +
                    "}";

            sendJsonResponse(exchange, 200, json);
        }
    }

    /**
     * POST /api/queue/call-next
     * Calls next patient using Java FIFO Queue
     */
    private class CallNextQueueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Appointment next = manager.callNextPatient();
            if (next != null) {
                sendJsonResponse(exchange, 200, "{\"success\":true,\"calledPatient\":" + JsonUtil.appointmentToJson(next) + "}");
            } else {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"message\":\"Waiting queue is currently empty.\"}");
            }
        }
    }

    /**
     * POST /api/consultation/complete
     */
    private class CompleteConsultationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtil.parseSimpleJson(body);

            String aptId = data.getOrDefault("appointmentId", "").trim();
            String diagnosis = data.getOrDefault("diagnosis", "").trim();
            String prescription = data.getOrDefault("prescription", "").trim();
            String waitStr = data.getOrDefault("waitingTime", "15").trim();

            System.out.println("[CompleteConsultation] received body: " + body);
            System.out.println("[CompleteConsultation] parsed map: " + data);
            System.out.println("[CompleteConsultation] aptId: '" + aptId + "'");

            int waitTime = 15;
            try {
                waitTime = Integer.parseInt(waitStr);
            } catch (NumberFormatException ignored) {}

            VisitRecord vr = manager.completeConsultation(aptId, diagnosis, prescription, waitTime);
            if (vr != null) {
                sendJsonResponse(exchange, 200, JsonUtil.visitRecordToJson(vr));
            } else {
                sendJsonResponse(exchange, 400, "{\"error\":\"Appointment not found or already completed/cancelled.\"}");
            }
        }
    }

    /**
     * GET /api/visits
     */
    private class VisitsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String patientId = query.get("patientId");

            if (patientId != null && !patientId.trim().isEmpty()) {
                List<VisitRecord> list = manager.getPatientHistory(patientId.trim());
                sendJsonResponse(exchange, 200, JsonUtil.listToJson(list, JsonUtil::visitRecordToJson));
            } else {
                // Return all visits
                List<VisitRecord> all = manager.getAllPatients().stream()
                        .flatMap(p -> p.getVisitHistory().stream())
                        .collect(Collectors.toList());
                sendJsonResponse(exchange, 200, JsonUtil.listToJson(all, JsonUtil::visitRecordToJson));
            }
        }
    }

    /**
     * GET /api/reports
     */
    private class ReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            long totalPatients = manager.getTotalPatientCount();
            long totalAppointments = manager.getTotalAppointmentCount();
            long scheduledCount = manager.getCountByStatus(Appointment.STATUS_SCHEDULED);
            long completedCount = manager.getCountByStatus(Appointment.STATUS_COMPLETED);
            long cancelledCount = manager.getCountByStatus(Appointment.STATUS_CANCELLED);
            double avgWaitTime = manager.getAverageWaitingTime();

            String today = LocalDate.now().toString();
            long todayAppointments = manager.getAllAppointments().stream()
                    .filter(a -> today.equals(a.getAppointmentDate())).count();

            String json = "{" +
                    "\"totalPatients\":" + totalPatients + "," +
                    "\"totalAppointments\":" + totalAppointments + "," +
                    "\"todayAppointments\":" + todayAppointments + "," +
                    "\"scheduledCount\":" + scheduledCount + "," +
                    "\"completedCount\":" + completedCount + "," +
                    "\"cancelledCount\":" + cancelledCount + "," +
                    "\"avgWaitingTime\":" + String.format("%.1f", avgWaitTime) +
                    "}";

            sendJsonResponse(exchange, 200, json);
        }
    }

    // ========================================================
    // STATIC FILE HANDLER
    // ========================================================

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File(WEB_ROOT + path);
            if (!file.exists() || file.isDirectory()) {
                String notFound = "<h1>404 Not Found</h1><p>The requested file does not exist.</p>";
                byte[] bytes = notFound.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            String mime = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                is.transferTo(os);
            }
        }

        private String getMimeType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }

    /**
     * Standalone main method to start the Web Server directly.
     */
    public static void main(String[] args) {
        try {
            ClinicManager clinicManager = new ClinicManager();
            WebServer webServer = new WebServer(clinicManager);
            webServer.start();

            // Attempt to open browser automatically
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:" + PORT + "/"));
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("[Error] Failed to start WebServer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
