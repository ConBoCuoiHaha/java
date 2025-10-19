package com.eduactivity.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionManager {
    private static volatile boolean initialized = false;
    private static String url;
    private static String user;
    private static String pass;
    private static boolean integrated;

    public static boolean isConfigured() {
        init();
        return url != null;
    }

    public static Connection getConnection() throws SQLException {
        init();
        if (url == null) throw new SQLException("DB not configured");
        if (integrated) {
            return DriverManager.getConnection(url);
        }
        if (user != null) {
            return DriverManager.getConnection(url, user, pass);
        }
        return DriverManager.getConnection(url);
    }

    private static synchronized void init() {
        if (initialized) return;
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            // driver not found in classpath
        }
        // try read appsettings.json upward
        Path cfg = findAppSettings();
        if (cfg != null) {
            try {
                String json = Files.readString(cfg, StandardCharsets.UTF_8);
                // naive parse DefaultConnection
                String conn = match(json, "\\\"DefaultConnection\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
                if (conn != null) {
                    parseSqlServerConnectionString(conn);
                }
            } catch (IOException ignored) {}
        }
        // allow override via JVM props or env (SQL Authentication)
        String propUrl = System.getProperty("sql.url");
        String propUser = System.getProperty("sql.user");
        String propPass = System.getProperty("sql.password");
        String envUrl = System.getenv("SQLSERVER_URL");
        String envUser = System.getenv("SQLSERVER_USER");
        String envPass = System.getenv("SQLSERVER_PASSWORD");

        if (propUrl != null && !propUrl.isBlank()) url = propUrl;
        else if (envUrl != null && !envUrl.isBlank()) url = envUrl;

        if (propUser != null && !propUser.isBlank()) user = propUser;
        else if (envUser != null && !envUser.isBlank()) user = envUser;

        if (propPass != null && !propPass.isBlank()) pass = propPass;
        else if (envPass != null && !envPass.isBlank()) pass = envPass;

        // If explicit user/pass is provided, force non-integrated
        if (user != null && !user.isBlank()) {
            integrated = false;
            // Remove any integratedSecurity=true from URL if present
            if (url != null) {
                String lower = url.toLowerCase();
                int idx = lower.indexOf("integratedsecurity=true");
                if (idx >= 0) {
                    // remove parameter and optional trailing semicolon
                    int end = idx + "integratedsecurity=true".length();
                    String before = url.substring(0, idx);
                    String after = url.substring(end);
                    // remove possible ';' next to param
                    if (after.startsWith(";")) after = after.substring(1);
                    if (before.endsWith(";")) before = before.substring(0, before.length()-1);
                    url = before + (before.endsWith(";") || after.startsWith(";") || before.isEmpty() || after.isEmpty() ? "" : ";") + after;
                }
            }
        }
        initialized = true;
    }

    private static Path findAppSettings() {
        String[] candidates = new String[]{
                "appsettings.json",
                "..\\appsettings.json",
                "..\\..\\appsettings.json",
                "..//appsettings.json",
                "../../appsettings.json"
        };
        for (String p : candidates) {
            Path path = Paths.get(p).toAbsolutePath().normalize();
            if (Files.exists(path)) return path;
        }
        return null;
    }

    private static String match(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private static void parseSqlServerConnectionString(String cs) {
        // Expect: Server=HOST;Database=DB;Trusted_Connection=true;TrustServerCertificate=True
        String server = part(cs, "Server");
        String database = part(cs, "Database");
        String trusted = part(cs, "Trusted_Connection");
        String trustCert = part(cs, "TrustServerCertificate");
        integrated = "true".equalsIgnoreCase(trusted);
        boolean trust = "true".equalsIgnoreCase(trustCert);
        if (server != null && database != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("jdbc:sqlserver://").append(server).append(";");
            sb.append("databaseName=").append(database).append(";");
            sb.append("encrypt=true;");
            if (trust) sb.append("trustServerCertificate=true;");
            if (integrated) sb.append("integratedSecurity=true;");
            url = sb.toString();
        }
        // Optional SQL auth from env
    }

    private static String part(String cs, String key) {
        for (String seg : cs.split(";")) {
            String[] kv = seg.split("=", 2);
            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(key)) {
                return kv[1].trim();
            }
        }
        return null;
    }
}
