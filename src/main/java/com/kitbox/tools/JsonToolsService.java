package com.kitbox.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * JSON 工具：格式化、压缩、校验、字符串转义/去转义。
 * 解析为严格模式（拒绝非法 JSON），错误信息尽量带行列位置。
 */
public final class JsonToolsService {

    private static final Gson ESCAPE_GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** JsonElement 的类型适配器：读取时尊重 JsonReader 的严格/宽松标志（线程安全）。 */
    private static final com.google.gson.TypeAdapter<JsonElement> ELEMENT_ADAPTER =
            new Gson().getAdapter(JsonElement.class);

    private JsonToolsService() {
    }

    /** 格式化：indent 为缩进空格数（建议 2 或 4）。 */
    public static String format(String json, int indent) {
        JsonElement element = parseStrict(json);
        return write(element, Math.max(0, indent));
    }

    /** 压缩：去除所有非必要空白。 */
    public static String compress(String json) {
        return write(parseStrict(json), -1);
    }

    /**
     * 校验：合法返回 null；非法返回带行列位置的错误描述。
     */
    public static String validate(String json) {
        try {
            parseStrict(json);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** 转义为 JSON 字符串字面量（含首尾引号）。 */
    public static String escape(String text) {
        return ESCAPE_GSON.toJson(text);
    }

    /** 去转义：输入是 JSON 字符串字面量（带引号），或裸内容（按含转义序列处理）。 */
    public static String unescape(String text) {
        String input = text.trim();
        if (input.length() < 2 || !input.startsWith("\"") || !input.endsWith("\"")) {
            input = "\"" + input + "\"";
        }
        try {
            return JsonParser.parseString(input).getAsString();
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new IllegalArgumentException("不是合法的 JSON 字符串字面量：" + e.getMessage());
        }
    }

    private static JsonElement parseStrict(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入 JSON 内容");
        }
        // 注意：JsonParser.parseReader / Gson.fromJson 都会临时把 reader 设为宽松模式，
        // 严格校验必须取 JsonElement 的适配器直接读，它尊重 reader 的 lenient 标志。
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(false);
        try {
            JsonElement element = ELEMENT_ADAPTER.read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("JSON 文档后面还有多余内容");
            }
            return element;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("JSON 语法错误：" + cleanMessage(e.getMessage()));
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON 语法错误：" + cleanMessage(e.getMessage()));
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("JSON 语法错误：" + cleanMessage(e.getMessage()));
        }
    }

    /** Gson 错误消息常带异常类名前缀，去掉让它更可读。 */
    private static String cleanMessage(String message) {
        if (message == null) {
            return "未知错误";
        }
        int idx = message.lastIndexOf(": ");
        return idx >= 0 ? message.substring(idx + 2) : message;
    }

    /** indent < 0 表示压缩输出。 */
    private static String write(JsonElement element, int indent) {
        StringWriter out = new StringWriter();
        try {
            JsonWriter writer = new JsonWriter(out);
            writer.setIndent(indent < 0 ? "" : spaces(indent));
            writer.setHtmlSafe(false);
            writer.setSerializeNulls(true);
            new GsonBuilder().disableHtmlEscaping().create().toJson(element, writer);
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("输出 JSON 失败", e);
        }
        return out.toString();
    }

    /** Java 8 无 String.repeat。 */
    private static String spaces(int count) {
        StringBuilder sb = new StringBuilder(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
