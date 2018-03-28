package com.wawa.common.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 增加泛型支持
 * Created by Administrator on 2017/11/10.
 */
public abstract class JSONUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonFactory JSONFACTORY = new JsonFactory();

    /**
     * 转换Java Bean 为 json
     */
    public static String beanToJson(Object o) {
        StringWriter sw = new StringWriter(300);
        JsonGenerator gen = null;
        try {
            gen = JSONFACTORY.createGenerator(sw);
            MAPPER.writeValue(gen, o);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("JSON转换失败", e);
        } finally {
            if (gen != null) try {
                gen.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static String beanToJson(JSONUtil.ToJson o) {
        return o.toJsonString();
    }

    /**
     * 转换Java Bean 为 HashMap
     */
    public static Map<String, Object> beanToMap(Object o) {
        try {
            return (Map) MAPPER.readValue(beanToJson(o), HashMap.class);
        } catch (IOException e) {
            throw new RuntimeException("转换失败", e);
        }
    }


    /**
     * 转换Json String 为 HashMap
     */
    public static Map<String, Object> jsonToMap(String json) {
        try {
            return (Map) MAPPER.readValue(json, HashMap.class);
        } catch (IOException e) {
            throw new RuntimeException("转换失败", e);
        }
    }


    /**
     * 转换Json String 为 JavaBean
     */
    public static <T> T jsonToBean(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
            //return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T jsonToBean(String json, JavaType type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }


    public static void validateJSON(String json) throws IOException {
        try (JsonParser parser = JSONFACTORY.createParser(json)) {
            while (parser.nextToken() != null) {
            }
        }
    }

    public interface ToJson {
        String toJsonString();
    }


}
