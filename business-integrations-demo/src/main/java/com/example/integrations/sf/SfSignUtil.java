package com.example.integrations.sf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class SfSignUtil {

    private SfSignUtil() {
    }

    /** 顺丰示例：msgData + timestamp + checkWord -> MD5 原始字节 -> Base64。 */
    public static String sign(String msgData, String timestamp, String checkWord) {
        try {
            String source = nullToEmpty(msgData) + nullToEmpty(timestamp) + nullToEmpty(checkWord);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(source.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("顺丰签名失败", exception);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
