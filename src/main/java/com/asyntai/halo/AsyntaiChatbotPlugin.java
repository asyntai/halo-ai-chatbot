package com.asyntai.halo;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * Asyntai AI Chatbot for Halo.
 *
 * <p>The plugin adds the Asyntai chat widget to every page of the site. The
 * work happens in {@link ChatbotHeadProcessor}; this class only marks the
 * plugin's life cycle.</p>
 */
@Component
public class AsyntaiChatbotPlugin extends BasePlugin {

    public AsyntaiChatbotPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }
}
