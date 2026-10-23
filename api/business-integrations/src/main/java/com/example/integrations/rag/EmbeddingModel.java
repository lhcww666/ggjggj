package com.example.integrations.rag;

public interface EmbeddingModel {

    double[] embed(String text);
}
