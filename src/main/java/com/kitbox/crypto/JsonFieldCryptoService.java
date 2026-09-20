package com.kitbox.crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.util.List;

/**
 * JSON 字段级加解密服务。
 * <p>
 * 路径语法（每行一条）：
 * <ul>
 *   <li>{@code $.data.idCard} —— 对象字段</li>
 *   <li>{@code $.list[0].name} —— 数组下标</li>
 *   <li>{@code $.list[*].phone} —— 数组通配</li>
 * </ul>
 * 仅改写命中的基本类型字段值，其余结构与字段保持原样。
 */
public final class JsonFieldCryptoService {

    private JsonFieldCryptoService() {
    }

    /** 字段值加解密器：加密与解密均以字符串形式交换（编码由实现方负责，如 Base64）。 */
    public interface FieldValueCipher {

        /** 字段明文 → 字段密文文本 */
        String encrypt(String plainText) throws CryptoException;

        /** 字段密文文本 → 字段明文 */
        String decrypt(String cipherText) throws CryptoException;
    }

    /**
     * 对 JSON 的指定字段执行加密或解密。
     *
     * @param json    原始 JSON 文本
     * @param paths   字段路径列表
     * @param encrypt true=加密，false=解密
     * @param cipher  字段值加解密器
     * @param pretty  输出是否格式化
     */
    public static String process(String json, List<String> paths, boolean encrypt,
                                 FieldValueCipher cipher, boolean pretty) throws CryptoException {
        if (json == null || json.trim().isEmpty()) {
            throw new CryptoException("JSON 内容为空");
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            throw new CryptoException("JSON 解析失败：" + e.getMessage());
        }
        boolean any = false;
        for (String path : paths) {
            if (path == null || path.trim().isEmpty()) {
                continue;
            }
            Step[] steps = parsePath(path.trim());
            apply(root, steps, 0, path.trim(), cipher, encrypt);
            any = true;
        }
        if (!any) {
            throw new CryptoException("未填写任何字段路径");
        }
        return pretty ? com.kitbox.util.Gsons.gson().toJson(root) : root.toString();
    }

    // ---------------- 路径解析 ----------------

    private static final class Step {
        final String field;   // 可为 null（纯数组段）
        final Integer index;  // 数组下标
        final boolean wildcard;

        Step(String field, Integer index, boolean wildcard) {
            this.field = field;
            this.index = index;
            this.wildcard = wildcard;
        }
    }

    private static Step[] parsePath(String path) throws CryptoException {
        String p = path;
        if (p.startsWith("$.")) {
            p = p.substring(2);
        } else if (p.equals("$")) {
            throw new CryptoException("路径必须指向具体字段，如 $.data.idCard");
        } else if (p.startsWith("$")) {
            p = p.substring(1);
        }
        if (p.startsWith(".")) {
            p = p.substring(1);
        }
        if (p.isEmpty()) {
            throw new CryptoException("路径为空：" + path);
        }
        String[] segs = p.split("\\.");
        Step[] steps = new Step[segs.length];
        for (int i = 0; i < segs.length; i++) {
            String seg = segs[i];
            if (seg.isEmpty()) {
                throw new CryptoException("路径段为空：" + path);
            }
            int open = seg.indexOf('[');
            if (open < 0) {
                steps[i] = new Step(seg, null, false);
                continue;
            }
            String field = seg.substring(0, open);
            String bracket = seg.substring(open);
            if (bracket.equals("[*]")) {
                steps[i] = new Step(field.isEmpty() ? null : field, null, true);
            } else if (bracket.matches("\\[\\d+\\]")) {
                int idx = Integer.parseInt(bracket.substring(1, bracket.length() - 1));
                steps[i] = new Step(field.isEmpty() ? null : field, idx, false);
            } else {
                throw new CryptoException("路径段格式非法：'" + seg + "'（示例：$.list[0] 或 $.list[*]）");
            }
        }
        return steps;
    }

    // ---------------- 遍历改写 ----------------

    private static void apply(JsonElement current, Step[] steps, int idx, String fullPath,
                              FieldValueCipher cipher, boolean encrypt) throws CryptoException {
        Step step = steps[idx];
        boolean isLast = idx == steps.length - 1;
        if (step.field != null) {
            if (!current.isJsonObject()) {
                throw new CryptoException("路径 " + fullPath + " 的 '" + step.field + "' 处不是 JSON 对象");
            }
            JsonObject obj = (JsonObject) current;
            if (!obj.has(step.field)) {
                throw new CryptoException("未找到字段：" + fullPath);
            }
            JsonElement child = obj.get(step.field);
            boolean hasIndex = step.index != null || step.wildcard;
            if (!hasIndex) {
                if (isLast) {
                    transformLeaf(child, v -> obj.add(step.field, v), fullPath, cipher, encrypt);
                } else {
                    apply(child, steps, idx + 1, fullPath, cipher, encrypt);
                }
            } else {
                applyArray(child, steps, idx, isLast, fullPath, cipher, encrypt);
            }
        } else {
            // 纯数组段：current 本身是数组
            applyArray(current, steps, idx, isLast, fullPath, cipher, encrypt);
        }
    }

    /** 处理数组段（含通配与下标），element 为数组本身。 */
    private static void applyArray(JsonElement element, Step[] steps, int idx, boolean isLast,
                                   String fullPath, FieldValueCipher cipher, boolean encrypt)
            throws CryptoException {
        if (!element.isJsonArray()) {
            throw new CryptoException("路径 " + fullPath + " 处不是 JSON 数组");
        }
        JsonArray arr = (JsonArray) element;
        Step step = steps[idx];
        if (step.wildcard) {
            for (int k = 0; k < arr.size(); k++) {
                handleArrayElement(arr, k, steps, idx, isLast, fullPath, cipher, encrypt);
            }
        } else {
            if (step.index >= arr.size()) {
                throw new CryptoException("数组下标越界：" + fullPath + "（长度 " + arr.size() + "）");
            }
            handleArrayElement(arr, step.index, steps, idx, isLast, fullPath, cipher, encrypt);
        }
    }

    private static void handleArrayElement(JsonArray arr, int k, Step[] steps, int idx, boolean isLast,
                                           String fullPath, FieldValueCipher cipher, boolean encrypt)
            throws CryptoException {
        final int kk = k;
        JsonElement elem = arr.get(kk);
        if (isLast) {
            transformLeaf(elem, v -> arr.set(kk, v), fullPath, cipher, encrypt);
        } else {
            apply(elem, steps, idx + 1, fullPath, cipher, encrypt);
        }
    }

    private static void transformLeaf(JsonElement child, ValueSetter setter, String fullPath,
                                      FieldValueCipher cipher, boolean encrypt) throws CryptoException {
        if (!child.isJsonPrimitive()) {
            throw new CryptoException("路径 " + fullPath + " 指向的不是基本类型值（对象/数组无法整体加解密）");
        }
        String value = child.getAsString();
        String result;
        try {
            result = encrypt ? cipher.encrypt(value) : cipher.decrypt(value);
        } catch (CryptoException e) {
            throw new CryptoException("处理字段 " + fullPath + " 失败：" + e.getMessage());
        }
        setter.set(new JsonPrimitive(result));
    }

    private interface ValueSetter {
        void set(JsonElement value);
    }
}
