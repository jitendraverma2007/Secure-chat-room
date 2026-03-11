package com.chatroom.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.chatroom.util.AESUtil;
import com.chatroom.util.DBUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/sendMessage")
public class SendMessageServlet extends HttpServlet {
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

        String senderEmail = (String) session.getAttribute("userEmail");
        String roomCode = (String) session.getAttribute("roomCode");
        String message = req.getParameter("message");

        if (roomCode == null || roomCode.trim().isEmpty()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Join room first\"}");
            return;
        }

        if (message == null) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Message cannot be empty\"}");
            return;
        }

        message = message.trim();

        if (message.isEmpty()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Message cannot be empty\"}");
            return;
        }

        if (message.length() > 1000) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Message too long\"}");
            return;
        }

        String sql = "INSERT INTO chat_messages "
                   + "(room_code, sender_email, message, delivery_status, delivered_at) "
                   + "VALUES (?, ?, ?, 'sent', NULL)";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String encryptedMessage = AESUtil.encrypt(message);

            ps.setString(1, roomCode);
            ps.setString(2, senderEmail);
            ps.setString(3, encryptedMessage);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                resp.getWriter().write("{\"success\":true,\"message\":\"Message sent\"}");
            } else {
                resp.getWriter().write("{\"success\":false,\"message\":\"Message not saved\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("{\"success\":false,\"message\":\"Server error\"}");
        }
    }
}