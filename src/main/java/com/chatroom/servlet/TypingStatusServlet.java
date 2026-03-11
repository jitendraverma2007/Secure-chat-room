package com.chatroom.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.chatroom.util.DBUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/typingStatus")
public class TypingStatusServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Login required\"}");
            return;
        }

        String userEmail = (String) session.getAttribute("userEmail");
        String roomCode = (String) session.getAttribute("roomCode");
        String typingParam = req.getParameter("typing");

        if (roomCode == null || roomCode.isBlank()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Room not joined\"}");
            return;
        }

        boolean isTyping = "true".equalsIgnoreCase(typingParam);

        try (Connection con = DBUtil.getConnection()) {

            String sql = "INSERT INTO typing_status (room_code, user_email, is_typing, updated_at) "
                       + "VALUES (?, ?, ?, NOW()) "
                       + "ON DUPLICATE KEY UPDATE is_typing = VALUES(is_typing), updated_at = NOW()";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, roomCode);
                ps.setString(2, userEmail);
                ps.setBoolean(3, isTyping);
                ps.executeUpdate();
            }

            resp.getWriter().write("{\"success\":true}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.toString();
            msg = msg.replace("\\", "\\\\").replace("\"", "\\\"")
                     .replace("\n", "\\n").replace("\r", "\\r");

            resp.getWriter().write("{\"success\":false,\"message\":\"" + msg + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"isTyping\":false}");
            return;
        }

        String currentUserEmail = (String) session.getAttribute("userEmail");
        String roomCode = (String) session.getAttribute("roomCode");

        if (roomCode == null || roomCode.isBlank()) {
            resp.getWriter().write("{\"isTyping\":false}");
            return;
        }

        try (Connection con = DBUtil.getConnection()) {

            String sql = "SELECT u.full_name, t.user_email, t.is_typing "
                       + "FROM typing_status t "
                       + "LEFT JOIN users u ON t.user_email = u.email "
                       + "WHERE t.room_code = ? "
                       + "AND t.user_email <> ? "
                       + "AND t.is_typing = TRUE "
                       + "AND TIMESTAMPDIFF(SECOND, t.updated_at, NOW()) <= 3 "
                       + "ORDER BY t.updated_at DESC "
                       + "LIMIT 1";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, roomCode);
                ps.setString(2, currentUserEmail);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String typingUserName = rs.getString("full_name");
                        if (typingUserName == null || typingUserName.isBlank()) {
                            typingUserName = rs.getString("user_email");
                        }

                        resp.getWriter().write("{\"isTyping\":true,\"typingUserName\":\""
                                + escapeJson(typingUserName) + "\"}");
                        return;
                    }
                }
            }

            resp.getWriter().write("{\"isTyping\":false}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.toString();
            msg = msg.replace("\\", "\\\\").replace("\"", "\\\"")
                     .replace("\n", "\\n").replace("\r", "\\r");

            resp.getWriter().write("{\"isTyping\":false,\"message\":\"" + msg + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}