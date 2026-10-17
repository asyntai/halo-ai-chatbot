package com.asyntai.halo;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

/**
 * Adds the widget loader to the head of every theme page.
 *
 * <p>Halo runs head processors for every page that a theme renders, so the
 * chatbot works with any theme and the site owner edits no template. The
 * console and the user center are not theme pages, so they stay clean.</p>
 *
 * <p>Settings, in the plugin's ConfigMap:</p>
 * <ul>
 *   <li>{@code basic.enabled} and {@code basic.widgetId}, from the settings form</li>
 *   <li>{@code connection.scriptUrl}, saved by the "Connect Asyntai" tab</li>
 * </ul>
 */
@Component
public class ChatbotHeadProcessor implements TemplateHeadProcessor {

    private final ReactiveSettingFetcher settingFetcher;

    public ChatbotHeadProcessor(ReactiveSettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler) {
        Mono<JsonNode> basic = settingFetcher.get("basic").defaultIfEmpty(missing());
        Mono<JsonNode> connection = settingFetcher.get("connection").defaultIfEmpty(missing());
        return Mono.zip(basic, connection)
            .map(t -> tagFor(t.getT1(), t.getT2()))
            .filter(tag -> !tag.isEmpty())
            .doOnNext(tag -> model.add(context.getModelFactory().createText(tag)))
            .onErrorResume(e -> Mono.empty())
            .then();
    }

    /**
     * The loader tag for these settings, or an empty string when the chatbot
     * is off or no valid widget ID is set.
     */
    static String tagFor(JsonNode basic, JsonNode connection) {
        JsonNode enabled = basic.path("enabled");
        if (isOff(enabled)) {
            return "";
        }
        String widgetId = basic.path("widgetId").asText("").trim();
        if (!WidgetTag.isValidWidgetId(widgetId)) {
            return "";
        }
        String scriptUrl = connection.path("scriptUrl").asText("");
        return WidgetTag.build(widgetId, scriptUrl);
    }

    private static boolean isOff(JsonNode enabled) {
        if (enabled.isMissingNode() || enabled.isNull()) {
            return false;
        }
        if (enabled.isBoolean()) {
            return !enabled.booleanValue();
        }
        String text = enabled.asText("").trim();
        return text.equalsIgnoreCase("false") || text.equals("0");
    }

    private static JsonNode missing() {
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }
}
