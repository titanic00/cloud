package com.titanic00.cloud.util;

public class UserUtil {

    public static boolean validateUsername(String username) {
        return username.length() >= 2 && username.length() <= 10;
    }
}
