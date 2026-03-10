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

        if (roomCode == null || roomCode.trim().isEmpty()) {
            resp.getWriter().write("{\"isTyping\":false}");
            return;
        }

        String sql = "SELECT typing_user_email, typing_user_name, is_typing, "
                   + "TIMESTAMPDIFF(SECOND, updated_at, NOW()) AS diff_sec "
                   + "FROM typing_status WHERE room_code = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, roomCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String typingUserEmail = rs.getString("typing_user_email");
                    String typingUserName = rs.getString("typing_user_name");
                    boolean isTyping = rs.getBoolean("is_typing");
                    int diffSec = rs.getInt("diff_sec");

                    if (isTyping
                            && typingUserEmail != null
                            && !typingUserEmail.equalsIgnoreCase(currentUserEmail)
                            && diffSec <= 3) {

                        if (typingUserName == null || typingUserName.trim().isEmpty()) {
                            typingUserName = "User";
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
            resp.getWriter().write("{\"isTyping\":false}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("userEmail") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false}");
            return;
        }

        String userEmail = (String) session.getAttribute("userEmail");
        String fullName = (String) session.getAttribute("fullName");
        String roomCode = (String) session.getAttribute("roomCode");
        String typingParam = req.getParameter("typing");

        boolean isTyping = "true".equalsIgnoreCase(typingParam);

        if (roomCode == null || roomCode.trim().isEmpty()) {
            resp.getWriter().write("{\"success\":false}");
            return;
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = userEmail;
        }

        String sql = "INSERT INTO typing_status "
                   + "(room_code, typing_user_email, typing_user_name, is_typing, updated_at) "
                   + "VALUES (?, ?, ?, ?, NOW()) "
                   + "ON DUPLICATE KEY UPDATE "
                   + "typing_user_email = VALUES(typing_user_email), "
                   + "typing_user_name = VALUES(typing_user_name), "
                   + "is_typing = VALUES(is_typing), "
                   + "updated_at = NOW()";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, roomCode);
            ps.setString(2, userEmail);
            ps.setString(3, fullName);
            ps.setBoolean(4, isTyping);
            ps.executeUpdate();

            resp.getWriter().write("{\"success\":true}");

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("{\"success\":false}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}