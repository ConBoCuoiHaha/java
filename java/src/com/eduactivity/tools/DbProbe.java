package com.eduactivity.tools;

import com.eduactivity.db.ConnectionManager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbProbe {
    public static void main(String[] args) {
        try (Connection c = ConnectionManager.getConnection()) {
            System.out.println("OK: Connected to SQL Server");
            DatabaseMetaData md = c.getMetaData();
            System.out.println("Driver=" + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("URL=" + md.getURL());
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT TOP 1 name FROM sys.databases ORDER BY name")) {
                if (rs.next()) {
                    System.out.println("Sample DB name: " + rs.getString(1));
                }
            }
            try (ResultSet rs = md.getTables(null, null, "Activities", null)) {
                System.out.println("Activities table present: " + rs.next());
            }
        } catch (Exception ex) {
            System.out.println("ERR: " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.out);
            System.exit(2);
        }
    }
}
