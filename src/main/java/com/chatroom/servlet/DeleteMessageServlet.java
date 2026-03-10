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

@WebServlet("/deleteMessage")
public class DeleteMessageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Login required\"}");
            return;
        }

        String userEmail = (String) session.getAttribute("userEmail");
        String msgId = req.getParameter("msg_id");

        if (msgId == null || msgId.trim().isEmpty()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Invalid request\"}");
            return;
        }

        String sql = "DELETE FROM chat_messages WHERE msg_id = ? AND sender_email = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(msgId));
            ps.setString(2, userEmail);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                resp.getWriter().write("{\"success\":true,\"message\":\"Message deleted\"}");
            } else {
                resp.getWriter().write("{\"success\":false,\"message\":\"Delete failed\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("{\"success\":false,\"message\":\"Server error\"}");
        }
    }
}