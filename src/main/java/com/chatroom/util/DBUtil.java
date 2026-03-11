package com.chatroom.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {

    public static Connection getConnection() throws Exception {
        String host = System.getenv("MYSQLHOST");
        String port = System.getenv("MYSQLPORT");
        String database = System.getenv("MYSQLDATABASE");
        String username = System.getenv("MYSQLUSER");
        String password = System.getenv("MYSQLPASSWORD");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, username, password);
    }
}