package com.example.integrations.rag;

import java.util.List;
import java.util.Map;

public interface AnswerGenerator {

    String generate(String query, List<Map<String, Object>> sources);
}
