package com.example.integrations.rag;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 无外部 Key 的模板生成器。
 * 接入 LLM 时实现 AnswerGenerator 并替换该 Bean 即可。
 */
@Component
public class TemplateAnswerGenerator implements AnswerGenerator {

    @Override
    public String generate(String query, List<Map<String, Object>> sources) {
        if (sources.isEmpty()) {
            return "知识库中没有找到相关内容。请换一个更具体的说法，或让管理员补充文档。";
        }
        StringBuilder answer = new StringBuilder("根据当前知识库检索到以下依据：\n");
        int count = Math.min(2, sources.size());
        for (int i = 0; i < count; i++) {
            Map<String, Object> source = sources.get(i);
            answer.append(i + 1).append(". ").append(source.get("title"))
                    .append("：").append(source.get("snippet")).append("\n");
        }
        answer.append("以上回答仅基于检索到的片段，未命中的内容不应作为结论。");
        return answer.toString();
    }
}
