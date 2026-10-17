package com.asyntai.halo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.Test;

class WidgetTagTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String text) throws Exception {
        return MAPPER.readTree(text);
    }

    @Test
    void widgetIdFormat() {
        assertTrue(WidgetTag.isValidWidgetId("asyntai_1a2b3c4d5e6f"));
        assertFalse(WidgetTag.isValidWidgetId(""));
        assertFalse(WidgetTag.isValidWidgetId(null));
        assertFalse(WidgetTag.isValidWidgetId("asyntai_"));
        assertFalse(WidgetTag.isValidWidgetId("asyntai_abc\"><script>"));
        assertFalse(WidgetTag.isValidWidgetId("other_123"));
        assertFalse(WidgetTag.isValidWidgetId("asyntai_abc\n"));
    }

    @Test
    void onlyHttpsAsyntaiScriptsAreTrusted() {
        assertTrue(WidgetTag.isTrustedScriptUrl("https://widget.asyntai.com/static/js/chat-widget.js"));
        assertTrue(WidgetTag.isTrustedScriptUrl("https://asyntai.com/static/js/chat-widget.js"));
        assertFalse(WidgetTag.isTrustedScriptUrl("http://widget.asyntai.com/x.js"));
        assertFalse(WidgetTag.isTrustedScriptUrl("https://evil.com/x.js"));
        assertFalse(WidgetTag.isTrustedScriptUrl("https://asyntai.com.evil.com/x.js"));
        assertFalse(WidgetTag.isTrustedScriptUrl("https://evilasyntai.com/x.js"));
        assertFalse(WidgetTag.isTrustedScriptUrl("https://user@evil.com/x.js"));
        assertFalse(WidgetTag.isTrustedScriptUrl("javascript:alert(1)"));
        assertFalse(WidgetTag.isTrustedScriptUrl(""));
        assertFalse(WidgetTag.isTrustedScriptUrl(null));
        assertEquals(WidgetTag.DEFAULT_SCRIPT_URL, WidgetTag.scriptUrlOrDefault("https://evil.com/x.js"));
    }

    @Test
    void tagLoadsTheWidgetAfterTheLoadEvent() {
        String tag = WidgetTag.build("asyntai_abc123", "");
        assertTrue(tag.startsWith("<script data-asyntai-loader>"));
        assertTrue(tag.endsWith("</script>"));
        assertTrue(tag.contains("\"https://widget.asyntai.com/static/js/chat-widget.js\""));
        assertTrue(tag.contains("\"data-asyntai-id\",\"asyntai_abc123\""));
        assertTrue(tag.contains("window.addEventListener(\"load\",l)"));
        assertEquals(1, tag.split("</script>", -1).length - 1);
    }

    @Test
    void jsStringCannotCloseTheScript() {
        String out = WidgetTag.jsString("</script><img onerror=x>'\"\\ ");
        assertFalse(out.contains("<"));
        assertFalse(out.contains(">"));
        assertFalse(out.substring(1, out.length() - 1).contains("\""));
        assertFalse(out.contains("'"));
        assertTrue(out.contains("\\u003c"));
        assertTrue(out.contains("\\u2028"));
    }

    @Test
    void settingsDecideTheTag() throws Exception {
        JsonNode none = MissingNode.getInstance();
        assertEquals("", ChatbotHeadProcessor.tagFor(none, none));
        assertEquals("", ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\"\"}"), none));
        assertEquals("", ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\"bad id\"}"), none));
        assertTrue(ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\"asyntai_x1\"}"), none)
            .contains("asyntai_x1"));
        assertTrue(ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\" asyntai_x1 \",\"enabled\":true}"), none)
            .contains("asyntai_x1"));
        assertEquals("", ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\"asyntai_x1\",\"enabled\":false}"), none));
        assertEquals("", ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\"asyntai_x1\",\"enabled\":\"false\"}"), none));
        String custom = ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\"asyntai_x1\"}"),
            json("{\"scriptUrl\":\"https://beta.asyntai.com/w.js\"}"));
        assertTrue(custom.contains("https://beta.asyntai.com/w.js"));
        String evil = ChatbotHeadProcessor.tagFor(json("{\"widgetId\":\"asyntai_x1\"}"),
            json("{\"scriptUrl\":\"https://evil.com/w.js\"}"));
        assertFalse(evil.contains("evil.com"));
        assertTrue(evil.contains(WidgetTag.DEFAULT_SCRIPT_URL));
    }
}
