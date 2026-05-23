package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KnowledgeOnnxEmbeddingModel implements KnowledgeEmbeddingModel {

    private final KnowledgeIntelligenceProperties properties;
    private final KnowledgeOnnxTokenizer tokenizer;
    private Object environment;
    private Object session;
    private boolean initialized;

    @Autowired
    public KnowledgeOnnxEmbeddingModel(KnowledgeIntelligenceProperties properties, KnowledgeOnnxTokenizer tokenizer) {
        this.properties = properties;
        this.tokenizer = tokenizer;
    }

    public KnowledgeOnnxEmbeddingModel(KnowledgeIntelligenceProperties properties) {
        this(properties, new KnowledgeOnnxTokenizer(properties));
    }

    @Override
    public boolean available() {
        return ensureLoaded();
    }

    @Override
    public double[] embed(String text) {
        List<double[]> vectors = embedBatch(List.of(text == null ? "" : text));
        return vectors.isEmpty() ? new double[0] : vectors.get(0);
    }

    @Override
    public List<double[]> embedBatch(List<String> texts) {
        if (!ensureLoaded() || texts == null || texts.isEmpty()) {
            return List.of();
        }
        Object inputTensor = null;
        Object maskTensor = null;
        Object result = null;
        try {
            KnowledgeOnnxTokenizer.TokenizedBatch batch = tokenizer.tokenizeBatch(texts);
            inputTensor = createTensor(batch.getInputIds());
            maskTensor = createTensor(batch.getAttentionMask());
            Method run = session.getClass().getMethod("run", Map.class);
            result = run.invoke(session, Map.of(
                    properties.getSemantic().getInputIdsName(), inputTensor,
                    properties.getSemantic().getAttentionMaskName(), maskTensor
            ));
            Object first = result.getClass().getMethod("get", int.class).invoke(result, 0);
            Object value = first.getClass().getMethod("getValue").invoke(first);
            return poolBatch(value, batch.getAttentionMask(), texts.size());
        } catch (Exception e) {
            log.warn("ONNX embedding 推理失败，将回退本地哈希向量: {}", e.getMessage());
            return List.of();
        } finally {
            closeQuietly(inputTensor);
            closeQuietly(maskTensor);
            closeQuietly(result);
        }
    }

    private boolean ensureLoaded() {
        if (initialized) {
            return session != null;
        }
        initialized = true;
        String modelPath = properties.getSemantic().getOnnxModelPath();
        if (modelPath == null || modelPath.isBlank()) {
            return false;
        }
        Path path = Path.of(modelPath.replace("${user.home}", System.getProperty("user.home")));
        if (!Files.exists(path)) {
            return false;
        }
        try {
            Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
            environment = envClass.getMethod("getEnvironment").invoke(null);
            Method createSession = environment.getClass().getMethod("createSession", String.class);
            session = createSession.invoke(environment, path.toString());
            return true;
        } catch (Exception e) {
            log.warn("ONNX Runtime 不可用或模型加载失败: {}", e.getMessage());
            session = null;
            return false;
        }
    }

    private Object createTensor(long[][] values) throws Exception {
        Class<?> tensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
        Method createTensor = tensorClass.getMethod("createTensor", Class.forName("ai.onnxruntime.OrtEnvironment"), Object.class);
        return createTensor.invoke(null, environment, values);
    }

    private List<double[]> poolBatch(Object value, long[][] attentionMask, int batchSize) {
        List<double[]> vectors = new java.util.ArrayList<>();
        for (int row = 0; row < batchSize; row++) {
            Object rowValue = arrayItem(value, row);
            double[] vector = "cls".equalsIgnoreCase(properties.getSemantic().getPooling())
                    ? flattenNumbers(arrayItem(rowValue, 0)).stream().mapToDouble(Number::doubleValue).toArray()
                    : meanPool(rowValue, attentionMask[row]);
            vectors.add(normalize(vector));
        }
        return vectors;
    }

    private double[] meanPool(Object rowValue, long[] mask) {
        int tokenCount = arrayLength(rowValue);
        if (tokenCount == 0) {
            return new double[0];
        }
        double[] sum = null;
        int valid = 0;
        for (int token = 0; token < tokenCount; token++) {
            if (token < mask.length && mask[token] == 0L) {
                continue;
            }
            double[] tokenVector = flattenNumbers(arrayItem(rowValue, token)).stream().mapToDouble(Number::doubleValue).toArray();
            if (sum == null) {
                sum = new double[tokenVector.length];
            }
            for (int i = 0; i < Math.min(sum.length, tokenVector.length); i++) {
                sum[i] += tokenVector[i];
            }
            valid++;
        }
        if (sum == null || valid == 0) {
            return new double[0];
        }
        for (int i = 0; i < sum.length; i++) {
            sum[i] /= valid;
        }
        return sum;
    }

    private Object arrayItem(Object value, int index) {
        if (value == null || !value.getClass().isArray() || Array.getLength(value) <= index) {
            return new double[0];
        }
        return Array.get(value, index);
    }

    private int arrayLength(Object value) {
        return value != null && value.getClass().isArray() ? Array.getLength(value) : 0;
    }

    private java.util.List<Number> flattenNumbers(Object value) {
        java.util.List<Number> numbers = new java.util.ArrayList<>();
        flattenInto(value, numbers);
        return numbers;
    }

    private void flattenInto(Object value, java.util.List<Number> numbers) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            numbers.add(number);
            return;
        }
        if (!value.getClass().isArray()) {
            return;
        }
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            flattenInto(Array.get(value, i), numbers);
        }
    }

    private double[] normalize(double[] vector) {
        double sum = 0;
        for (double item : vector) {
            sum += item * item;
        }
        if (sum == 0) {
            return vector;
        }
        double length = Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / length;
        }
        return vector;
    }

    private void closeQuietly(Object value) {
        if (value == null) {
            return;
        }
        try {
            Method close = value.getClass().getMethod("close");
            close.invoke(value);
        } catch (Exception ignored) {
        }
    }
}
