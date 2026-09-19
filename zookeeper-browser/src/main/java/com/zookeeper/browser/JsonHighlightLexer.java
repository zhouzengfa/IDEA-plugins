package com.zookeeper.browser;

import com.intellij.lang.Language;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tokenizes JSON for syntax colors on the right pane.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
final class JsonHighlightLexer extends LexerBase {

    static final IElementType KEY = new IElementType("ZK_JSON_KEY", Language.ANY);
    static final IElementType STRING = new IElementType("ZK_JSON_STRING", Language.ANY);
    static final IElementType NUMBER = new IElementType("ZK_JSON_NUMBER", Language.ANY);
    static final IElementType KEYWORD = new IElementType("ZK_JSON_KEYWORD", Language.ANY);
    static final IElementType BRACES = new IElementType("ZK_JSON_BRACES", Language.ANY);
    static final IElementType BRACKETS = new IElementType("ZK_JSON_BRACKETS", Language.ANY);
    static final IElementType COMMA = new IElementType("ZK_JSON_COMMA", Language.ANY);
    static final IElementType COLON = new IElementType("ZK_JSON_COLON", Language.ANY);
    static final IElementType WHITESPACE = new IElementType("ZK_JSON_WS", Language.ANY);
    static final IElementType BAD = new IElementType("ZK_JSON_BAD", Language.ANY);

    private CharSequence buffer = "";
    private int start;
    private int end;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.start = startOffset;
        this.end = endOffset;
        this.tokenStart = startOffset;
        this.tokenEnd = startOffset;
        advance();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        tokenStart = tokenEnd;
        if (tokenStart >= end) {
            tokenType = null;
            return;
        }
        char c = buffer.charAt(tokenStart);
        if (Character.isWhitespace(c)) {
            tokenEnd = skipWhile(tokenStart, ch -> Character.isWhitespace(ch));
            tokenType = WHITESPACE;
            return;
        }
        if (c == '{') {
            tokenEnd = tokenStart + 1;
            tokenType = BRACES;
            return;
        }
        if (c == '}') {
            tokenEnd = tokenStart + 1;
            tokenType = BRACES;
            return;
        }
        if (c == '[') {
            tokenEnd = tokenStart + 1;
            tokenType = BRACKETS;
            return;
        }
        if (c == ']') {
            tokenEnd = tokenStart + 1;
            tokenType = BRACKETS;
            return;
        }
        if (c == ',') {
            tokenEnd = tokenStart + 1;
            tokenType = COMMA;
            return;
        }
        if (c == ':') {
            tokenEnd = tokenStart + 1;
            tokenType = COLON;
            return;
        }
        if (c == '"') {
            tokenEnd = scanString(tokenStart);
            tokenType = isKeyString(tokenEnd) ? KEY : STRING;
            return;
        }
        if (c == '-' || Character.isDigit(c)) {
            tokenEnd = scanNumber(tokenStart);
            tokenType = NUMBER;
            return;
        }
        if (startsWith("true") || startsWith("false") || startsWith("null")) {
            String word = startsWith("true") ? "true" : startsWith("false") ? "false" : "null";
            tokenEnd = tokenStart + word.length();
            tokenType = KEYWORD;
            return;
        }
        tokenEnd = tokenStart + 1;
        tokenType = BAD;
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return end;
    }

    private int skipWhile(int from, java.util.function.IntPredicate predicate) {
        int i = from;
        while (i < end && predicate.test(buffer.charAt(i))) {
            i++;
        }
        return i;
    }

    private int scanString(int from) {
        int i = from + 1;
        while (i < end) {
            char c = buffer.charAt(i);
            if (c == '\\' && i + 1 < end) {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return end;
    }

    private boolean isKeyString(int afterString) {
        int i = afterString;
        while (i < end && Character.isWhitespace(buffer.charAt(i))) {
            i++;
        }
        return i < end && buffer.charAt(i) == ':';
    }

    private int scanNumber(int from) {
        int i = from;
        if (buffer.charAt(i) == '-') {
            i++;
        }
        while (i < end && (Character.isDigit(buffer.charAt(i)) || buffer.charAt(i) == '.'
                || buffer.charAt(i) == 'e' || buffer.charAt(i) == 'E' || buffer.charAt(i) == '+')) {
            i++;
        }
        return i;
    }

    private boolean startsWith(@NotNull String word) {
        if (tokenStart + word.length() > end) {
            return false;
        }
        return word.contentEquals(buffer.subSequence(tokenStart, tokenStart + word.length()));
    }
}
