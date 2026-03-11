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

@WebServlet("/roomStatus")
public class RoomStatusServlet extends HttpServlet {
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

        String currentUserEmail = (String) session.getAttribute("userEmail");
        String roomCode = (String) session.getAttribute("roomCode");

        if (roomCode == null || roomCode.trim().isEmpty()) {
            resp.getWriter().write("{\"otherUserStatus\":\"Offline\"}");
            return;
        }

        String sql = "SELECT COUNT(*) AS total "
                   + "FROM users "
                   + "WHERE active_room_code = ? "
                   + "AND email <> ? "
                   + "AND is_online = TRUE "
                   + "AND last_seen IS NOT NULL "
                   + "AND TIMESTAMPDIFF(SECOND, last_seen, NOW()) <= 8";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, roomCode.trim());
            ps.setString(2, currentUserEmail.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("total") > 0) {
                    resp.getWriter().write("{\"otherUserStatus\":\"Online\"}");
                } else {
                    resp.getWriter().write("{\"otherUserStatus\":\"Offline\"}");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("{\"otherUserStatus\":\"Offline\"}");
        }
    }
}