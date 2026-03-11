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
            resp.getWriter().write("{\"success\":false,\"message\":\"Email and password are required\"}");
            return;
        }

        email = email.trim().toLowerCase();

        try {
            UserDAO dao = new UserDAO();
            UserDAO.LoginResult result = dao.login(email, password);

            if (result.success) {
                HttpSession session = req.getSession(true);
                session.setAttribute("userEmail", email);

                String ipAddress = req.getRemoteAddr();

                try (Connection con = DBUtil.getConnection()) {

                    String fullName = email;

                    try (PreparedStatement psName = con.prepareStatement(
                            "SELECT full_name FROM users WHERE email = ?")) {
                        psName.setString(1, email);

                        try (ResultSet rsName = psName.executeQuery()) {
                            if (rsName.next()) {
                                String dbFullName = rsName.getString("full_name");
                                if (dbFullName != null && !dbFullName.isBlank()) {
                                    fullName = dbFullName;
                                }
                            }
                        }
                    }

                    session.setAttribute("fullName", fullName);

                    try (PreparedStatement psAudit = con.prepareStatement(
                            "INSERT INTO audit_logs (user_email, ip_address) VALUES (?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {

                        psAudit.setString(1, email);
                        psAudit.setString(2, ipAddress);
                        psAudit.executeUpdate();

                        try (ResultSet rs = psAudit.getGeneratedKeys()) {
                            if (rs.next()) {
                                session.setAttribute("auditLogId", rs.getInt(1));
                            }
                        }
                    }

                    try (PreparedStatement psOnline = con.prepareStatement(
                            "UPDATE users SET is_online = TRUE WHERE email = ?")) {
                        psOnline.setString(1, email);
                        psOnline.executeUpdate();
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
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = e.toString();
            }

            resp.getWriter().write("{\"success\":false,\"message\":\""
                    + escapeJson(errorMessage) + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}