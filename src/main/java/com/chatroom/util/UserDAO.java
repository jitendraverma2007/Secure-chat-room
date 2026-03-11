package com.chatroom.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {

    public static class LoginResult {
        public boolean success;
        public boolean locked;
        public int remainingAttempts;
        public String message;

        public LoginResult(boolean success, boolean locked, int remainingAttempts, String message) {
            this.success = success;
            this.locked = locked;
            this.remainingAttempts = remainingAttempts;
            this.message = message;
        }
    }

    public static class SignupResult {
        public boolean success;
        public String message;

        public SignupResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public LoginResult login(String email, String plainPassword) throws Exception {
        String sql = "SELECT user_id, password_hash, failed_attempts, is_locked FROM users WHERE email = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return new LoginResult(false, false, 0, "User not found");
                }

                int userId = rs.getInt("user_id");
                String passwordHash = rs.getString("password_hash");
                int failedAttempts = rs.getInt("failed_attempts");
                boolean isLocked = rs.getBoolean("is_locked");

                if (isLocked) {
                    return new LoginResult(false, true, 0, "Account is locked");
                }

                boolean matched = BCrypt.checkpw(plainPassword, passwordHash);

                if (matched) {
                    resetFailedAttempts(con, userId);
                    return new LoginResult(true, false, 3, "Login successful");
                } else {
                    failedAttempts++;
                    boolean lockNow = failedAttempts >= 3;

                    updateFailedAttempts(con, userId, failedAttempts, lockNow);

                    int remaining = Math.max(0, 3 - failedAttempts);

                    if (lockNow) {
                        return new LoginResult(false, true, 0, "Account locked after 3 wrong attempts");
                    } else {
                        return new LoginResult(false, false, remaining, "Invalid password");
                    }
                }
            }
        }
    }

    public SignupResult signup(String fullName, String email, String plainPassword) throws Exception {
        String checkSql = "SELECT user_id FROM users WHERE email = ?";
        String insertSql = "INSERT INTO users (full_name, email, password_hash, failed_attempts, is_locked, is_online, active_room_code) "
                         + "VALUES (?, ?, ?, 0, FALSE, FALSE, NULL)";

        try (Connection con = DBUtil.getConnection()) {

            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                checkPs.setString(1, email);

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        return new SignupResult(false, "Email already registered");
                    }
                }
            }

            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

            try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                insertPs.setString(1, fullName);
                insertPs.setString(2, email);
                insertPs.setString(3, hashedPassword);
                insertPs.executeUpdate();
            }

            return new SignupResult(true, "Account created successfully");
        }
    }

    private void resetFailedAttempts(Connection con, int userId) throws Exception {
        String sql = "UPDATE users SET failed_attempts = 0, is_locked = FALSE WHERE user_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private void updateFailedAttempts(Connection con, int userId, int attempts, boolean locked) throws Exception {
        String sql = "UPDATE users SET failed_attempts = ?, is_locked = ? WHERE user_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setBoolean(2, locked);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }
}