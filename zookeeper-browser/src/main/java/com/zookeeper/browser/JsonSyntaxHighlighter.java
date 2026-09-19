package com.zookeeper.browser;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Colors JSON keys, strings, numbers and keywords.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
final class JsonSyntaxHighlighter extends SyntaxHighlighterBase {

    private static final TextAttributesKey KEY =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_KEY", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    private static final TextAttributesKey STRING =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_STRING", DefaultLanguageHighlighterColors.STRING);
    private static final TextAttributesKey NUMBER =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    private static final TextAttributesKey KEYWORD =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    private static final TextAttributesKey BRACES =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_BRACES", DefaultLanguageHighlighterColors.BRACES);
    private static final TextAttributesKey BRACKETS =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    private static final TextAttributesKey COMMA =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_COMMA", DefaultLanguageHighlighterColors.COMMA);
    private static final TextAttributesKey COLON =
            TextAttributesKey.createTextAttributesKey("ZK_JSON_COLON", DefaultLanguageHighlighterColors.DOT);
    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new JsonHighlightLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == JsonHighlightLexer.KEY) {
            return pack(KEY);
        }
        if (tokenType == JsonHighlightLexer.STRING) {
            return pack(STRING);
        }
        if (tokenType == JsonHighlightLexer.NUMBER) {
            return pack(NUMBER);
        }
        if (tokenType == JsonHighlightLexer.KEYWORD) {
            return pack(KEYWORD);
        }
        if (tokenType == JsonHighlightLexer.BRACES) {
            return pack(BRACES);
        }
        if (tokenType == JsonHighlightLexer.BRACKETS) {
            return pack(BRACKETS);
        }
        if (tokenType == JsonHighlightLexer.COMMA) {
            return pack(COMMA);
        }
        if (tokenType == JsonHighlightLexer.COLON) {
            return pack(COLON);
        }
        return EMPTY;
    }
}
