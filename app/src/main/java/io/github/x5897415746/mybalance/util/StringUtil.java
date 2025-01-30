package io.github.x5897415746.mybalance.util;

import androidx.annotation.NonNull;

public class StringUtil {

    @NonNull
    public static String bytesToHex(@NonNull byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
}
