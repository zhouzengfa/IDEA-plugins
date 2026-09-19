package com.zookeeper.browser;

import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Builds fold ranges for pretty-printed JSON so objects and arrays can collapse.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
final class JsonFold {

    private JsonFold() {
    }

    static void apply(@NotNull FoldingModel foldingModel, @NotNull String text) {
        List<int[]> ranges = ranges(text);
        foldingModel.runBatchFoldingOperation(() -> {
            for (FoldRegion region : foldingModel.getAllFoldRegions()) {
                foldingModel.removeFoldRegion(region);
            }
            for (int[] range : ranges) {
                int start = range[0];
                char open = text.charAt(start);
                String placeholder = open == '[' ? "[...]" : "{...}";
                foldingModel.addFoldRegion(start, range[1], placeholder);
            }
        });
    }

    private static @NotNull List<int[]> ranges(@NotNull String text) {
        List<int[]> ranges = new ArrayList<>();
        Deque<Integer> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{' || c == '[') {
                stack.push(i);
                continue;
            }
            if ((c == '}' || c == ']') && !stack.isEmpty()) {
                int start = stack.pop();
                if (text.substring(start, i + 1).indexOf('\n') >= 0) {
                    ranges.add(new int[]{start, i + 1});
                }
            }
        }
        return ranges;
    }
}
