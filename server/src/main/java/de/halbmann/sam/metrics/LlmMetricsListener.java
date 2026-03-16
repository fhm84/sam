package de.halbmann.sam.metrics;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Captures LLM token usage and request metrics via the standard langchain4j {@link
 * ChatModelListener} interface. quarkus-langchain4j auto-discovers CDI beans implementing this
 * interface and registers them with every chat model.
 *
 * <p>Metrics published:
 *
 * <ul>
 *   <li>{@code sam.llm.tokens} — counter, tags: type (input|output)
 *   <li>{@code sam.llm.requests} — counter, tags: status (success|failure)
 * </ul>
 */
@ApplicationScoped
public class LlmMetricsListener implements ChatModelListener {

    @Inject
    MeterRegistry registry;

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        registry.counter("sam.llm.requests", "status", "success").increment();

        TokenUsage usage = responseContext.chatResponse().tokenUsage();
        if (usage != null) {
            if (usage.inputTokenCount() != null) {
                registry.counter("sam.llm.tokens", "type", "input").increment(usage.inputTokenCount());
            }
            if (usage.outputTokenCount() != null) {
                registry.counter("sam.llm.tokens", "type", "output").increment(usage.outputTokenCount());
            }
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        registry.counter("sam.llm.requests", "status", "failure").increment();
    }
}
