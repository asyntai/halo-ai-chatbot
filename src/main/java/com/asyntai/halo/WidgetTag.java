package com.asyntai.halo;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds the script tag that loads the Asyntai chat widget, and checks the
 * values that go into it.
 */
public final class WidgetTag {

    public static final String DEFAULT_SCRIPT_URL =
        "https://widget.asyntai.com/static/js/chat-widget.js";

    private static final Pattern WIDGET_ID = Pattern.compile("^asyntai_[A-Za-z0-9]+$");

    private WidgetTag() {
    }

    public static boolean isValidWidgetId(String widgetId) {
        return widgetId != null && WIDGET_ID.matcher(widgetId).matches();
    }

    /**
     * Only an https URL on asyntai.com or one of its subdomains is used.
     * Anything else falls back to the default widget URL, so a changed
     * setting can never load a script from another site.
     */
    public static boolean isTrustedScriptUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                || uri.getUserInfo() != null) {
                return false;
            }
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("asyntai.com") || host.endsWith(".asyntai.com");
        } catch (Exception e) {
            return false;
        }
    }

    public static String scriptUrlOrDefault(String url) {
        return isTrustedScriptUrl(url) ? url.trim() : DEFAULT_SCRIPT_URL;
    }

    /**
     * The loader adds the widget script after the page's load event, so a slow
     * or unreachable Asyntai server never delays the page itself.
     */
    public static String build(String widgetId, String scriptUrl) {
        return "<script data-asyntai-loader>(function(){var l=function(){"
            + "if(document.querySelector('script[data-asyntai-id]'))return;"
            + "var s=document.createElement(\"script\");"
            + "s.src=" + jsString(scriptUrlOrDefault(scriptUrl)) + ";s.async=true;"
            + "s.setAttribute(\"data-asyntai-id\"," + jsString(widgetId) + ");"
            + "document.body.appendChild(s)};"
            + "if(document.readyState===\"complete\"){l()}"
            + "else{window.addEventListener(\"load\",l)}})();</script>";
    }

    /**
     * A JavaScript string literal that is also safe inside an HTML script
     * element.
     */
    static String jsString(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\' || c == '<' || c == '>' || c == '&' || c == '\''
                || c < 0x20 || c == 0x2028 || c == 0x2029) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
