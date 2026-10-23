package com.example.integrations.rag;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地演示用 embedding：把 token 哈希到固定维度，再归一化。
 * 生产环境可替换为 OpenAI/DashScope/BGE 等 embedding 模型。
 */
@Component
public class HashingEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSION = 128;

    @Override
    public double[] embed(String text) {
        double[] vector = new double[DIMENSION];
        List<String> tokens = TextTokenizer.tokens(text);
        for (String token : tokens) {
            int index = Math.floorMod(token.hashCode(), DIMENSION);
            vector[index] += 1.0;
        }
        double norm = 0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
