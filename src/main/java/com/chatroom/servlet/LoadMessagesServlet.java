package com.chatroom.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.chatroom.util.AESUtil;
import com.chatroom.util.DBUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/loadMessages")
public class LoadMessagesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("userEmail") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("[]");
            return;
        }

        String roomCode = (String) session.getAttribute("roomCode");
        String currentUserEmail = (String) session.getAttribute("userEmail");

        if (roomCode == null || roomCode.trim().isEmpty()) {
            resp.getWriter().write("[]");
            return;
        }

        try (Connection con = DBUtil.getConnection()) {

            String markDeliveredSql =
                    "UPDATE chat_messages "
                  + "SET delivery_status = 'delivered', delivered_at = NOW() "
                  + "WHERE room_code = ? "
                  + "AND sender_email <> ? "
                  + "AND delivery_status = 'sent'";

            try (PreparedStatement ps = con.prepareStatement(markDeliveredSql)) {
                ps.setString(1, roomCode);
                ps.setString(2, currentUserEmail);
                ps.executeUpdate();
            }

            String sql = "SELECT m.msg_id, m.sender_email, m.message, "
                       + "DATE_FORMAT(m.sent_time, '%h:%i %p') AS formatted_time, "
                       + "m.edited, u.full_name, "
                       + "COALESCE(m.delivery_status, 'sent') AS delivery_status "
                       + "FROM chat_messages m "
                       + "LEFT JOIN users u ON m.sender_email = u.email "
                       + "WHERE m.room_code = ? "
                       + "ORDER BY m.msg_id ASC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, roomCode);

                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;

                    while (rs.next()) {
                        if (!first) {
                            json.append(",");
                        }
                        first = false;

                        String dbMessage = rs.getString("message");
                        String finalMessage;

                        try {
                            finalMessage = AESUtil.decrypt(dbMessage);
                        } catch (Exception ex) {
                            finalMessage = dbMessage;
                        }

                        String senderName = rs.getString("full_name");
                        if (senderName == null || senderName.trim().isEmpty()) {
                            senderName = rs.getString("sender_email");
                        }

                        json.append("{")
                            .append("\"msg_id\":").append(rs.getInt("msg_id")).append(",")
                            .append("\"sender_email\":\"").append(escapeJson(rs.getString("sender_email"))).append("\",")
                            .append("\"sender_name\":\"").append(escapeJson(senderName)).append("\",")
                            .append("\"message\":\"").append(escapeJson(finalMessage)).append("\",")
                            .append("\"sent_time\":\"").append(escapeJson(rs.getString("formatted_time"))).append("\",")
                            .append("\"edited\":").append(rs.getBoolean("edited")).append(",")
                            .append("\"delivery_status\":\"").append(escapeJson(rs.getString("delivery_status"))).append("\"")
                            .append("}");
                    }

                    json.append("]");
                    resp.getWriter().write(json.toString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("[]");
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