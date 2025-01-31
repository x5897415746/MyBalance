package io.github.x5897415746.mybalance.util;

import androidx.annotation.NonNull;

import java.util.Objects;

public class StringUtil {

    @NonNull
    public static String bytesToHex(@NonNull byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
    
    public static boolean strArrayContains(@NonNull String[] array, String string) {
        for (String s : array) {
            if (Objects.equals(s, string)) {
                return true;
            }
        }
        return false;
    }
}
