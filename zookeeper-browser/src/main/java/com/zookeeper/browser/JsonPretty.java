package com.zookeeper.browser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

/**
 * Expands compact JSON for the right-hand content pane.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
public final class JsonPretty {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private JsonPretty() {
    }

    public static @NotNull String expand(@NotNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return text;
        }
        char first = trimmed.charAt(0);
        if (first != '{' && first != '[') {
            return text;
        }
        try {
            JsonElement element = JsonParser.parseString(trimmed);
            if (!element.isJsonObject() && !element.isJsonArray()) {
                return text;
            }
            return GSON.toJson(element);
        } catch (JsonSyntaxException e) {
            return text;
        }
    }
}
