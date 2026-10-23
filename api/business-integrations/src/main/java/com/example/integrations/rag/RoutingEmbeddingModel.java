package com.example.integrations.rag;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * provider=hash 时使用本地哈希向量；
 * provider=openai 且配置 Key 时使用真实 Embedding；
 * 外部调用失败时降级到哈希向量，保证 Demo 可用。
 */
@Primary
@Component
public class RoutingEmbeddingModel implements EmbeddingModel {

    private final RagEmbeddingProperties properties;
    private final HashingEmbeddingModel hashing;
    private final OpenAiCompatibleEmbeddingModel openAi;

    public RoutingEmbeddingModel(RagEmbeddingProperties properties,
                                 HashingEmbeddingModel hashing,
                                 OpenAiCompatibleEmbeddingModel openAi) {
        this.properties = properties;
        this.hashing = hashing;
        this.openAi = openAi;
    }

    @Override
    public double[] embed(String text) {
        if ("openai".equalsIgnoreCase(properties.getProvider())
                && !isBlank(properties.getApiKey())
                && !isBlank(properties.getBaseUrl())) {
            try {
                return openAi.embed(text);
            } catch (RuntimeException ignored) {
                // 外部模型不可用时保持 Demo 可运行
            }
        }
        return hashing.embed(text);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
