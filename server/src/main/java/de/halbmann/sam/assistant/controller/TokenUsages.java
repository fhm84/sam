package de.halbmann.sam.assistant.controller;

import dev.langchain4j.model.output.TokenUsage;

/**
 * Null-safe accessors for LangChain4j token usage. Providers may omit usage data entirely (null
 * {@link TokenUsage}) or report null individual counts — both must not fail event logging after a
 * successful AI call.
 */
public final class TokenUsages {

    private TokenUsages() {}

    public static int inputTokens(TokenUsage usage) {
        return usage != null && usage.inputTokenCount() != null ? usage.inputTokenCount() : 0;
    }

    public static int outputTokens(TokenUsage usage) {
        return usage != null && usage.outputTokenCount() != null ? usage.outputTokenCount() : 0;
    }
}
