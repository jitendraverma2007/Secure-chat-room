package com.chatroom.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.chatroom.util.DBUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/currentUser")
public class CurrentUserServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("userEmail") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
            return;
        }

        String userEmail = (String) session.getAttribute("userEmail");
        String fullName = (String) session.getAttribute("fullName");
        String roomCode = (String) session.getAttribute("roomCode");

        if (userEmail == null) userEmail = "";
        if (fullName == null || fullName.isBlank()) fullName = userEmail;
        if (roomCode == null) roomCode = "";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET is_online = TRUE, active_room_code = ?, last_seen = NOW() WHERE email = ?")) {

            ps.setString(1, roomCode);
            ps.setString(2, userEmail);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        String json = "{"
                + "\"success\":true,"
                + "\"userEmail\":\"" + escapeJson(userEmail) + "\","
                + "\"fullName\":\"" + escapeJson(fullName) + "\","
                + "\"roomCode\":\"" + escapeJson(roomCode) + "\""
                + "}";

        resp.getWriter().write(json);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}