package com.eduactivity.web;

import com.eduactivity.model.User;
import com.eduactivity.model.Activity;
import com.eduactivity.model.ActivityStatus;
import com.eduactivity.model.UserRole;
import com.eduactivity.dao.ActivityDao;
import com.eduactivity.dao.RegistrationDao;
import com.eduactivity.dao.UserDao;
import com.eduactivity.service.ActivityService;
import com.eduactivity.service.AuthService;
import com.eduactivity.service.RegistrationService;
import com.google.gson.Gson;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApiServer {
    private static final Gson gson = new Gson();
    private static final AuthService authService = new AuthService();
    private static final ActivityService activityService = new ActivityService();
    private static final RegistrationService registrationService = new RegistrationService();
    private static final ActivityDao activityDao = new ActivityDao();
    private static final RegistrationDao registrationDao = new RegistrationDao();
    private static final UserDao userDao = new UserDao();
    private static final Map<String, User> sessions = new ConcurrentHashMap<>();

    public static void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/auth/login", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 405, ""); return; }
            Map body = gson.fromJson(readBody(exchange), Map.class);
            String email = Objects.toString(body.getOrDefault("email", ""));
            String password = Objects.toString(body.getOrDefault("password", ""));
            var userOpt = authService.login(email, password);
            if (userOpt.isEmpty()) { respond(exchange, 400, "Email hoặc mật khẩu không đúng"); return; }
            var user = userOpt.get();
            String token = UUID.randomUUID().toString();
            sessions.put(token, user);
            Map<String,Object> payload = new LinkedHashMap<>();
            payload.put("token", token);
            payload.put("refreshToken", UUID.randomUUID().toString());
            Map<String,Object> userDto = new LinkedHashMap<>();
            userDto.put("id", user.getId());
            userDto.put("fullName", user.getFullName());
            userDto.put("email", user.getEmail());
            userDto.put("role", user.getRole().name());
            userDto.put("class", user.getClassName());
            userDto.put("department", user.getDepartment());
            payload.put("user", userDto);
            respondJson(exchange, 200, gson.toJson(payload));
        });

        server.createContext("/api/auth/profile", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            Map<String,Object> userDto = new LinkedHashMap<>();
            userDto.put("id", user.getId());
            userDto.put("fullName", user.getFullName());
            userDto.put("email", user.getEmail());
            userDto.put("role", user.getRole().name());
            userDto.put("class", user.getClassName());
            userDto.put("department", user.getDepartment());
            respondJson(exchange, 200, gson.toJson(userDto));
        });

        server.createContext("/api/activities", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    var list = activityService.listAll();
                    respondJson(exchange, 200, gson.toJson(list));
                    return;
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map body = gson.fromJson(readBody(exchange), Map.class);
                    var a = activityService.create(
                            user,
                            Objects.toString(body.get("title"), ""),
                            Objects.toString(body.get("description"), ""),
                            java.time.LocalDateTime.parse(Objects.toString(body.get("startTime"))),
                            java.time.LocalDateTime.parse(Objects.toString(body.get("endTime"))),
                            Objects.toString(body.get("location"), ""),
                            ((Number) body.get("maxParticipants")).intValue(),
                            Boolean.TRUE.equals(body.get("requireApproval")),
                            body.get("category")!=null?body.get("category").toString():null
                    );
                    respondJson(exchange, 200, gson.toJson(a));
                    return;
                }
            } catch (Exception ex) {
                respond(exchange, 500, ex.getMessage());
                return;
            }
            respond(exchange, 405, "");
        });

        server.createContext("/api/activities/my", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            var list = activityService.listByCreator(user.getId());
            respondJson(exchange, 200, gson.toJson(list));
        });

        server.createContext("/api/activities/", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            String path = exchange.getRequestURI().getPath(); // /api/activities/{id}
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                try {
                    int id = Integer.parseInt(parts[3]);
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        var a = activityService.getById(id).orElse(null);
                        if (a == null) { respond(exchange, 404, "Not found"); return; }
                        respondJson(exchange, 200, gson.toJson(a));
                        return;
                    } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                        if (user.getRole() == UserRole.Admin) {
                            try { activityDao.deleteActivityAndRegistrations(id); respond(exchange, 200, "OK"); }
                            catch (Exception ex) { respond(exchange, 500, ex.getMessage()); }
                        } else {
                            try { activityService.cancel(user, id); respond(exchange, 200, "OK"); }
                            catch (Exception ex) { respond(exchange, 500, ex.getMessage()); }
                        }
                        return;
                    } else if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                        Map body = gson.fromJson(readBody(exchange), Map.class);
                        var patch = new Activity();
                        patch.setId(id);
                        if (body.get("title")!=null) patch.setTitle(body.get("title").toString());
                        if (body.get("description")!=null) patch.setDescription(body.get("description").toString());
                        if (body.get("startTime")!=null) patch.setStartTime(java.time.LocalDateTime.parse(body.get("startTime").toString()));
                        if (body.get("endTime")!=null) patch.setEndTime(java.time.LocalDateTime.parse(body.get("endTime").toString()));
                        if (body.get("location")!=null) patch.setLocation(body.get("location").toString());
                        if (body.get("maxParticipants")!=null) patch.setMaxParticipants(((Number)body.get("maxParticipants")).intValue());
                        if (body.get("requireApproval")!=null) patch.setRequireApproval(Boolean.TRUE.equals(body.get("requireApproval")));
                        if (body.get("category")!=null) patch.setCategory(body.get("category").toString());
                        try { activityService.update(user, patch); respond(exchange, 200, "OK"); }
                        catch (Exception ex) { respond(exchange, 400, ex.getMessage()); }
                        return;
                    }
                    return;
                } catch (NumberFormatException e) { /* fallthrough */ }
            }
            respond(exchange, 404, "Not found");
        });

        server.createContext("/api/registrations/my", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            var regs = registrationService.myRegistrations(user.getId());
            respondJson(exchange, 200, gson.toJson(regs));
        });

        server.createContext("/api/registrations", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map body = gson.fromJson(readBody(exchange), Map.class);
                int activityId = ((Number) body.get("activityId")).intValue();
                String notes = body.get("notes") != null ? body.get("notes").toString() : null;
                var r = registrationService.register(user.getId(), activityId, notes);
                respondJson(exchange, 200, gson.toJson(r));
                return;
            }
            respond(exchange, 405, "");
        });

        // Pending registrations (Teacher only)
        server.createContext("/api/registrations/pending", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            var list = registrationService.pendingForCreator(user.getId());
            // Enrich with student and activity info for frontend
            List<Map<String,Object>> out = new ArrayList<>();
            for (var r : list) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("status", r.getStatus().name());
                m.put("registrationTime", r.getRegistrationTime());
                var act = activityService.getById(r.getActivityId()).orElse(null);
                if (act != null) m.put("activity", Map.of("id", act.getId(), "title", act.getTitle()));
                try { var stu = userDao.getById(r.getStudentId()); if (stu != null) m.put("student", Map.of("id", stu.getId(), "fullName", stu.getFullName(), "className", stu.getClassName())); } catch (Exception ignored) {}
                out.add(m);
            }
            respondJson(exchange, 200, gson.toJson(out));
        });

        // Approve/Reject
        server.createContext("/api/registrations/", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            String path = exchange.getRequestURI().getPath(); // /api/registrations/{id}/approve
            String[] parts = path.split("/");
            if (parts.length >= 5) {
                try {
                    int id = Integer.parseInt(parts[3]);
                    String action = parts[4];
                    if ("approve".equalsIgnoreCase(action)) {
                        try { registrationService.processApproval(user.getId(), id, true); respond(exchange, 200, "OK"); } catch (Exception ex) { respond(exchange, 400, ex.getMessage()); }
                        return;
                    } else if ("reject".equalsIgnoreCase(action)) {
                        try { registrationService.processApproval(user.getId(), id, false); respond(exchange, 200, "OK"); } catch (Exception ex) { respond(exchange, 400, ex.getMessage()); }
                        return;
                    }
                } catch (NumberFormatException ignore) { }
            }
            // DELETE /api/registrations/{id}
            if (parts.length == 4 && "DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    int id = Integer.parseInt(parts[3]);
                    try { registrationService.cancel(user.getId(), id); respond(exchange, 200, "OK"); } catch (Exception ex) { respond(exchange, 400, ex.getMessage()); }
                    return;
                } catch (NumberFormatException ignore) {}
            }
            respond(exchange, 404, "Not found");
        });

        // Admin endpoints
        server.createContext("/api/admin/statistics", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            try {
                var users = userDao.listAll();
                var activities = activityService.listAll();
                int totalUsers = users.size();
                long students = users.stream().filter(u -> u.getRole()==UserRole.Student).count();
                long teachers = users.stream().filter(u -> u.getRole()==UserRole.Teacher).count();
                long admins = users.stream().filter(u -> u.getRole()==UserRole.Admin).count();
                int totalActivities = activities.size();
                long activeActivities = activities.stream().filter(a -> a.getStatus()==ActivityStatus.Open).count();
                int totalRegistrations = 0;
                int pendingRegistrations = 0;
                try { totalRegistrations = registrationDao.countAll(); } catch (Exception ignored) {}
                try { pendingRegistrations = registrationDao.countPending(); } catch (Exception ignored) {}

                Map<String,Object> resp = new LinkedHashMap<>();
                resp.put("userStats", Map.of("totalUsers", totalUsers, "students", students, "teachers", teachers, "admins", admins));
                resp.put("activityStats", Map.of("totalActivities", totalActivities, "activeActivities", activeActivities));
                resp.put("registrationStats", Map.of("totalRegistrations", totalRegistrations, "pendingRegistrations", pendingRegistrations));
                respondJson(exchange, 200, gson.toJson(resp));
            } catch (Exception ex) {
                respond(exchange, 500, ex.getMessage());
            }
        });

        server.createContext("/api/admin/users", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    var list = userDao.listAll();
                    List<Map<String,Object>> usersJson = new ArrayList<>();
                    for (var u: list) {
                        Map<String,Object> m = new LinkedHashMap<>();
                        m.put("id", u.getId()); m.put("fullName", u.getFullName()); m.put("email", u.getEmail()); m.put("role", u.getRole().name()); m.put("class", u.getClassName()); m.put("department", u.getDepartment());
                        usersJson.add(m);
                    }
                    respondJson(exchange, 200, gson.toJson(Map.of("users", usersJson)));
                } catch (Exception ex) { respond(exchange, 500, ex.getMessage()); }
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map body = gson.fromJson(readBody(exchange), Map.class);
                try {
                    String id = userDao.createUser(
                            Objects.toString(body.get("fullName"), ""),
                            Objects.toString(body.get("email"), ""),
                            Objects.toString(body.get("password"), ""),
                            Objects.toString(body.get("role"), "Student"),
                            body.get("class")!=null?body.get("class").toString():null,
                            body.get("department")!=null?body.get("department").toString():null
                    );
                    respondJson(exchange, 200, gson.toJson(Map.of("id", id)));
                } catch (Exception ex) { respond(exchange, 400, ex.getMessage()); }
                return;
            }
            respond(exchange, 405, "");
        });

        server.createContext("/api/admin/users/", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            String[] parts = exchange.getRequestURI().getPath().split("/");
            if (parts.length >= 5) {
                String id = parts[4];
                if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map body = gson.fromJson(readBody(exchange), Map.class);
                    try {
                        userDao.updateUser(id, Objects.toString(body.get("fullName"), null), Objects.toString(body.get("email"), null), Objects.toString(body.get("role"), null), body.get("class")!=null?body.get("class").toString():null, body.get("department")!=null?body.get("department").toString():null);
                        respond(exchange, 200, "OK");
                    } catch (Exception ex) { respond(exchange, 400, ex.getMessage()); }
                    return;
                } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try { userDao.deleteUser(id); respond(exchange, 200, "OK"); } catch (Exception ex) { respond(exchange, 400, ex.getMessage()); }
                    return;
                }
            }
            respond(exchange, 404, "Not found");
        });

        // GET /api/registrations/activity/{activityId}
        server.createContext("/api/registrations/activity/", exchange -> {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { respond(exchange, 200, ""); return; }
            var user = requireUser(exchange);
            if (user == null) return;
            String[] parts = exchange.getRequestURI().getPath().split("/");
            if (parts.length >= 5) {
                try {
                    int activityId = Integer.parseInt(parts[4]);
                    var regs = registrationService.byActivity(activityId);
                    List<Map<String,Object>> out = new ArrayList<>();
                    for (var r: regs) {
                        Map<String,Object> m = new LinkedHashMap<>();
                        m.put("id", r.getId());
                        m.put("status", r.getStatus().name());
                        m.put("registrationTime", r.getRegistrationTime());
                        try { var stu = userDao.getById(r.getStudentId()); if (stu != null) m.put("student", Map.of("id", stu.getId(), "fullName", stu.getFullName(), "className", stu.getClassName())); } catch (Exception ignored) {}
                        out.add(m);
                    }
                    respondJson(exchange, 200, gson.toJson(out));
                    return;
                } catch (NumberFormatException ignore) {}
            }
            respond(exchange, 404, "Not found");
        });

        

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("API server started at http://localhost:"+port+"/api");
    }

    private static void enableCORS(HttpExchange exchange) {
        Headers h = exchange.getResponseHeaders();
        // Use set() to avoid duplicate header values like "*, *"
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        enableCORS(exchange);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static void respondJson(HttpExchange exchange, int status, String json) throws IOException {
        Headers h = exchange.getResponseHeaders();
        h.add("Content-Type", "application/json; charset=utf-8");
        respond(exchange, status, json);
    }

    private static User requireUser(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            respond(exchange, 401, "Unauthorized");
            return null;
        }
        String token = auth.substring(7).trim();
        User u = sessions.get(token);
        if (u == null) { respond(exchange, 401, "Unauthorized"); return null; }
        return u;
    }
}
