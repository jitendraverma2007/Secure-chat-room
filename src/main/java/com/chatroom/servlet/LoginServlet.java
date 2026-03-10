package com.chatroom.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.chatroom.util.DBUtil;
import com.chatroom.util.UserDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"ok\":true,\"message\":\"/login servlet is running. Use POST for login.\"}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Email/Password required\"}");
            return;
        }

        email = email.trim();

        try {
            UserDAO dao = new UserDAO();
            UserDAO.LoginResult result = dao.login(email, password);

            if (result.success) {
                HttpSession session = req.getSession(true);
                session.setAttribute("userEmail", email);

                String ipAddress = req.getRemoteAddr();

                try (Connection con = DBUtil.getConnection()) {

                    try (PreparedStatement psName = con.prepareStatement(
                            "SELECT full_name FROM users WHERE email = ?")) {
                        psName.setString(1, email);

                        try (ResultSet rsName = psName.executeQuery()) {
                            if (rsName.next()) {
                                String fullName = rsName.getString("full_name");
                                if (fullName != null && !fullName.isBlank()) {
                                    session.setAttribute("fullName", fullName);
                                } else {
                                    session.setAttribute("fullName", email);
                                }
                            } else {
                                session.setAttribute("fullName", email);
                            }
                        }
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO audit_logs (user_email, ip_address) VALUES (?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {

                        ps.setString(1, email);
                        ps.setString(2, ipAddress);
                        ps.executeUpdate();

                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                session.setAttribute("auditLogId", rs.getInt(1));
                            }
                        }
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE users SET is_online = TRUE WHERE email = ?")) {
                        ps.setString(1, email);
                        ps.executeUpdate();
                    }
                }
            }

            String json = "{"
                    + "\"success\":" + result.success + ","
                    + "\"locked\":" + result.locked + ","
                    + "\"remainingAttempts\":" + result.remainingAttempts + ","
                    + "\"message\":\"" + escapeJson(result.message) + "\""
                    + "}";

            resp.getWriter().write(json);

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Server error\"}");
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