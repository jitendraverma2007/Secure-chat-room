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

@WebServlet("/editMessage")
public class EditMessageServlet extends HttpServlet {
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
        String msgId = req.getParameter("msg_id");
        String newMessage = req.getParameter("message");

        if (msgId == null || newMessage == null || newMessage.trim().isEmpty()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid request\"}");
            return;
        }

        String sql = "UPDATE chat_messages SET message = ?, edited = TRUE WHERE msg_id = ? AND sender_email = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String encryptedMessage = AESUtil.encrypt(newMessage.trim());

            ps.setString(1, encryptedMessage);
            ps.setInt(2, Integer.parseInt(msgId));
            ps.setString(3, userEmail);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                resp.getWriter().write("{\"success\":true,\"message\":\"Message updated\"}");
            } else {
                resp.getWriter().write("{\"success\":false,\"message\":\"Edit failed\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("{\"success\":false,\"message\":\"Server error\"}");
        }
    }
}