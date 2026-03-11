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

@WebServlet("/joinRoom")
public class JoinRoomServlet extends HttpServlet {
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
        String roomCode = req.getParameter("room_code");

        if (roomCode == null) roomCode = "";
        roomCode = roomCode.trim();

        if (!roomCode.matches("\\d{6}")) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid room code\"}");
            return;
        }

        try (Connection con = DBUtil.getConnection()) {

            cleanupStaleRoomUsers(con, roomCode);

            String checkSql = "SELECT user1_email, user2_email FROM chat_rooms WHERE room_code = ?";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setString(1, roomCode);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        String user1 = rs.getString("user1_email");
                        String user2 = rs.getString("user2_email");

                        if (userEmail.equalsIgnoreCase(safe(user1)) || userEmail.equalsIgnoreCase(safe(user2))) {
                            session.setAttribute("roomCode", roomCode);
                            updateActiveRoom(con, userEmail, roomCode);
                            resp.getWriter().write("{\"success\":true,\"message\":\"Rejoined room\"}");
                            return;
                        }

                        if (isEmpty(user1)) {
                            String updateSql = "UPDATE chat_rooms SET user1_email = ? WHERE room_code = ?";
                            try (PreparedStatement ups = con.prepareStatement(updateSql)) {
                                ups.setString(1, userEmail);
                                ups.setString(2, roomCode);
                                ups.executeUpdate();
                            }

                            session.setAttribute("roomCode", roomCode);
                            updateActiveRoom(con, userEmail, roomCode);
                            resp.getWriter().write("{\"success\":true,\"message\":\"Joined room successfully\"}");
                            return;
                        }

                        if (isEmpty(user2)) {
                            String updateSql = "UPDATE chat_rooms SET user2_email = ? WHERE room_code = ?";
                            try (PreparedStatement ups = con.prepareStatement(updateSql)) {
                                ups.setString(1, userEmail);
                                ups.setString(2, roomCode);
                                ups.executeUpdate();
                            }

                            session.setAttribute("roomCode", roomCode);
                            updateActiveRoom(con, userEmail, roomCode);
                            resp.getWriter().write("{\"success\":true,\"message\":\"Joined room successfully\"}");
                            return;
                        }

                        resp.getWriter().write("{\"success\":false,\"message\":\"Room already full\"}");
                        return;
                    }
                }
            }

            String insertSql = "INSERT INTO chat_rooms (room_code, user1_email, user2_email) VALUES (?, ?, NULL)";
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setString(1, roomCode);
                ps.setString(2, userEmail);
                ps.executeUpdate();
            }

            session.setAttribute("roomCode", roomCode);
            updateActiveRoom(con, userEmail, roomCode);
            resp.getWriter().write("{\"success\":true,\"message\":\"Room created successfully\"}");

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = e.toString();
            }

            errorMessage = errorMessage.replace("\\", "\\\\")
                                       .replace("\"", "\\\"")
                                       .replace("\n", "\\n")
                                       .replace("\r", "\\r");

            resp.getWriter().write("{\"success\":false,\"message\":\"" + errorMessage + "\"}");
        }
    }

    private void updateActiveRoom(Connection con, String userEmail, String roomCode) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET active_room_code = ?, is_online = TRUE, last_seen = NOW() WHERE email = ?")) {
            ps.setString(1, roomCode);
            ps.setString(2, userEmail);
            ps.executeUpdate();
        }
    }

    private void cleanupStaleRoomUsers(Connection con, String roomCode) throws Exception {
        String sql = "SELECT user1_email, user2_email FROM chat_rooms WHERE room_code = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roomCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String user1 = rs.getString("user1_email");
                    String user2 = rs.getString("user2_email");

                    if (!isUserStillActive(con, user1, roomCode)) {
                        clearUserSlot(con, roomCode, "user1_email");
                    }

                    if (!isUserStillActive(con, user2, roomCode)) {
                        clearUserSlot(con, roomCode, "user2_email");
                    }
                }
            }
        }
    }

    private boolean isUserStillActive(Connection con, String email, String roomCode) throws Exception {
        if (isEmpty(email)) return false;

        String sql = "SELECT COUNT(*) FROM users "
                   + "WHERE email = ? "
                   + "AND active_room_code = ? "
                   + "AND is_online = TRUE "
                   + "AND last_seen IS NOT NULL "
                   + "AND TIMESTAMPDIFF(SECOND, last_seen, NOW()) <= 8";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, roomCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    private void clearUserSlot(Connection con, String roomCode, String columnName) throws Exception {
        String sql = "UPDATE chat_rooms SET " + columnName + " = NULL WHERE room_code = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roomCode);
            ps.executeUpdate();
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}