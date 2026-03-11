package com.chatroom.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHashGenerator {

    public static void main(String[] args) {

        String password = "1234";

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
    }
}