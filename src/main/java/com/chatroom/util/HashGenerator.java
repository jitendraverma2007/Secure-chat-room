package com.chatroom.util;

import org.mindrot.jbcrypt.BCrypt;

public class HashGenerator {
    public static void main(String[] args) {
        String password = "12345";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        System.out.println("Generated Hash:");
        System.out.println(hash);
        System.out.println("Check = " + BCrypt.checkpw("12345", hash));
    }
}