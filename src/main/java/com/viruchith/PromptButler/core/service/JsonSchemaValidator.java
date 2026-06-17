package com.viruchith.PromptButler.core.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Locale;
import java.util.Objects;

/**
 * Strict structural validation for prompt store / import JSON payloads.
 */
public final class JsonSchemaValidator {

    public static final long MAX_IMPORT_BYTES = 10L * 1024L * 1024L;

    public void validatePromptStoreJson(String json) {
        Objects.requireNonNull(json, "json");
        JsonElement root = JsonParser.parseString(json);
        validateRootObject(root, true);
    }

    public void validatePromptStoreJsonElement(JsonElement root) {
        validateRootObject(root, true);
    }

    private static void validateRootObject(JsonElement root, boolean requireTemplates) {
        if (root == null || !root.isJsonObject()) {
            throw new IllegalArgumentException("Root must be a JSON object");
        }
        JsonObject o = root.getAsJsonObject();
        for (String key : o.keySet()) {
            if (!"templates".equals(key) && !"version".equals(key)) {
                throw new IllegalArgumentException("Unknown root field: " + key);
            }
        }
        if (o.has("version") && !o.get("version").isJsonPrimitive()) {
            throw new IllegalArgumentException("version must be a primitive");
        }
        if (o.has("version")) {
            JsonPrimitive pv = o.getAsJsonPrimitive("version");
            if (!pv.isNumber()) {
                throw new IllegalArgumentException("version must be a number");
            }
        }
        if (!o.has("templates")) {
            if (requireTemplates) {
                throw new IllegalArgumentException("Missing templates array");
            }
            return;
        }
        JsonElement te = o.get("templates");
        if (te == null || !te.isJsonArray()) {
            throw new IllegalArgumentException("templates must be an array");
        }
        JsonArray arr = te.getAsJsonArray();
        for (JsonElement el : arr) {
            validateTemplateObject(el);
        }
    }

    private static void validateTemplateObject(JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            throw new IllegalArgumentException("Each template must be an object");
        }
        JsonObject t = el.getAsJsonObject();
        requireStringField(t, "id");
        requireStringField(t, "title");
        requireStringField(t, "body");
        if (!t.has("tags")) {
            throw new IllegalArgumentException("Missing tags array");
        }
        JsonElement tags = t.get("tags");
        if (!tags.isJsonArray()) {
            throw new IllegalArgumentException("tags must be an array");
        }
        for (JsonElement tagEl : tags.getAsJsonArray()) {
            if (tagEl == null || !tagEl.isJsonPrimitive() || !tagEl.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Each tag must be a string");
            }
            if (tagEl.getAsString().contains("\u0000")) {
                throw new IllegalArgumentException("Invalid tag string");
            }
        }
        for (String key : t.keySet()) {
            String k = key.toLowerCase(Locale.ROOT);
            if (!("id".equals(k) || "title".equals(k) || "body".equals(k) || "tags".equals(k))) {
                throw new IllegalArgumentException("Unknown field on template: " + key);
            }
        }
    }

    private static void requireStringField(JsonObject o, String field) {
        if (!o.has(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        JsonElement e = o.get(field);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Field must be string: " + field);
        }
    }
}
