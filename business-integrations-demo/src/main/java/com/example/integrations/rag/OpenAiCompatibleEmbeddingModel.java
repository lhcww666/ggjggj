package com.example.integrations.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class OpenAiCompatibleEmbeddingModel implements EmbeddingModel {

    private final RagEmbeddingProperties properties;
    private final ObjectMapper mapper;

    public OpenAiCompatibleEmbeddingModel(RagEmbeddingProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public double[] embed(String text) {
        try {
            String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/embeddings";
            ObjectNode body = mapper.createObjectNode();
            body.put("model", properties.getModel());
            body.put("input", text == null ? "" : text);

            HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(Math.max(1, properties.getTimeoutSeconds()) * 1000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            try (OutputStream output = connection.getOutputStream()) {
                output.write(mapper.writeValueAsBytes(body));
            }
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new IllegalStateException("Embedding HTTP " + connection.getResponseCode());
            }
            JsonNode response = mapper.readTree(read(connection.getInputStream()));
            JsonNode values = response.path("data").path(0).path("embedding");
            if (!values.isArray() || values.size() == 0) {
                throw new IllegalStateException("Embedding 返回为空");
            }
            double[] vector = new double[values.size()];
            double norm = 0;
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).asDouble();
                norm += vector[i] * vector[i];
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) vector[i] /= norm;
            }
            return vector;
        } catch (Exception exception) {
            throw new IllegalStateException("调用 Embedding 失败", exception);
        }
    }

    private String read(InputStream input) throws Exception {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = source.read(buffer)) >= 0) output.write(buffer, 0, length);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
