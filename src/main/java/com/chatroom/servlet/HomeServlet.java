package com.chatroom.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/")
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Secure Chat Room</title>
</head>
<body style="font-family: Arial; text-align: center; padding-top: 80px;">
    <h1>Secure Chat Room</h1>
    <p>Backend is running successfully on Railway.</p>
    <p><a href="login">Go to Login Servlet</a></p>
</body>
</html>
""");
    }
}