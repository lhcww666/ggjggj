package com.example.integrations.rag;

import java.util.ArrayList;
import java.util.List;

public final class TextTokenizer {

    private TextTokenizer() {
    }

    public static List<String> tokens(String text) {
        List<String> result = new ArrayList<String>();
        String normalized = text == null ? "" : text.toLowerCase();
        StringBuilder word = new StringBuilder();
        StringBuilder chinese = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.toString(c).matches("[a-z0-9]")) {
                word.append(c);
                flushChinese(chinese, result);
            } else if (c >= '\u4e00' && c <= '\u9fa5') {
                chinese.append(c);
                flushWord(word, result);
            } else {
                flushWord(word, result);
                flushChinese(chinese, result);
            }
        }
        flushWord(word, result);
        flushChinese(chinese, result);
        return result;
    }

    private static void flushWord(StringBuilder word, List<String> result) {
        if (word.length() > 0) {
            result.add(word.toString());
            word.setLength(0);
        }
    }

    private static void flushChinese(StringBuilder chinese, List<String> result) {
        if (chinese.length() == 1) {
            result.add(chinese.toString());
        } else if (chinese.length() > 1) {
            for (int i = 0; i < chinese.length() - 1; i++) {
                result.add(chinese.substring(i, i + 2));
            }
        }
        chinese.setLength(0);
    }
}
