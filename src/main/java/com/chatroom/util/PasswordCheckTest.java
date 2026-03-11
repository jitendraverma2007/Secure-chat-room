package com.chatroom.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordCheckTest {
    public static void main(String[] args) {
        String plain = "12345";
        String hash = "$2a$10$S0nsepR1Xgw/B3ABjg.Zde3RBD0BtHakrabL64VX8y0rN6IkX9gp6";

        System.out.println("Match = " + BCrypt.checkpw(plain, hash));
    }
}