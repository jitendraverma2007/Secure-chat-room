package com.chatroom.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import com.chatroom.util.DBUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);

        if (session != null) {
            String userEmail = (String) session.getAttribute("userEmail");
            String roomCode = (String) session.getAttribute("roomCode");
            Object auditIdObj = session.getAttribute("auditLogId");

            try (Connection con = DBUtil.getConnection()) {

                if (userEmail != null && !userEmail.trim().isEmpty()) {

                    // 1. Mark user offline and clear room from users table
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE users SET is_online = FALSE, active_room_code = NULL, last_seen = NULL WHERE email = ?")) {
                        ps.setString(1, userEmail.trim());
                        ps.executeUpdate();
                    }

                    // 2. Remove user from chat_rooms.user1_email or user2_email
                    if (roomCode != null && !roomCode.trim().isEmpty()) {
                        try (PreparedStatement ps = con.prepareStatement(
                                "UPDATE chat_rooms "
                              + "SET user1_email = CASE WHEN user1_email = ? THEN NULL ELSE user1_email END, "
                              + "    user2_email = CASE WHEN user2_email = ? THEN NULL ELSE user2_email END "
                              + "WHERE room_code = ?")) {
                            ps.setString(1, userEmail.trim());
                            ps.setString(2, userEmail.trim());
                            ps.setString(3, roomCode.trim());
                            ps.executeUpdate();
                        }
                    }
                }

                if (auditIdObj != null) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE audit_logs SET logout_time = ? WHERE id = ?")) {
                        ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                        ps.setInt(2, Integer.parseInt(auditIdObj.toString()));
                        ps.executeUpdate();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            session.invalidate();
        }

        resp.getWriter().write("{\"success\":true,\"message\":\"Logged out successfully\"}");
    }
}