package com.chatroom.servlet;

import java.io.IOException;

import com.chatroom.util.UserDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (fullName == null || email == null || password == null
                || fullName.isBlank() || email.isBlank() || password.isBlank()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"All fields are required\"}");
            return;
        }

        try {
            UserDAO dao = new UserDAO();
            UserDAO.SignupResult result = dao.signup(fullName.trim(), email.trim(), password);

            String json = "{"
                    + "\"success\":" + result.success + ","
                    + "\"message\":\"" + escapeJson(result.message) + "\""
                    + "}";

            resp.getWriter().write(json);

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("{\"success\":false,\"message\":\"Server error\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}