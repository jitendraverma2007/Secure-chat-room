package com.chatroom.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DBUtil {

    public static Connection getConnection() throws Exception {

        String host = System.getenv("MYSQLHOST");
        String port = System.getenv("MYSQLPORT");
        String database = System.getenv("MYSQLDATABASE");
        String username = System.getenv("MYSQLUSER");
        String password = System.getenv("MYSQLPASSWORD");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";

        Class.forName("com.mysql.cj.jdbc.Driver");

        Connection con = DriverManager.getConnection(url, username, password);

        // Set timezone to IST
        try (PreparedStatement ps = con.prepareStatement("SET time_zone = '+05:30'")) {
            ps.execute();
        }

        return con;
    }
}