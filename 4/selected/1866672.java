package com.azureus.plugins.aztsearch;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

/**
 * The entry point for the plugin as provided by the Vuze plugin framework.
 * 
 * @version 0.4
 * @author Dalmazio Brisinda

 * <p>
 * Acknowledgements: Daniele Borghi (ades83@users.sourceforge.net)
 * 
 * <p>
 * This software is licensed under the 
 * <a href="http://creativecommons.org/licenses/GPL/2.0/">CC-GNU GPL.</a>
 */
public class TSMainViewPlugin implements UnloadablePlugin {

    private static final String VIEWID = "aztsearchViewID";

    private static PluginInterface pluginInterface;

    private static LoggerChannel loggerChannel;

    private TSViewListener viewListener;

    private UISWTInstance swtInstance;

    private boolean autoOpenPlugin;

    /** Unload the plugin. */
    public void unload() throws PluginException {
        if (swtInstance != null) swtInstance.removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
    }

    /**
	 * Initialize the plugin.
	 * 
	 * <p>
	 * Acknowledgements: Daniele Borghi (ades83@users.sourceforge.net)
	 * 
	 * @param pi the instance of the PluginInterface class passed by the Vuze
	 * plugin framework for use by this plugin.
	 */
    public void initialize(PluginInterface pi) throws PluginException {
        pluginInterface = pi;
        pi.addListener(new PluginListener() {

            public void initializationComplete() {
            }

            public void closedownInitiated() {
            }

            public void closedownComplete() {
            }
        });
        loggerChannel = pi.getLogger().getChannel("aztsearch");
        initPluginViewModel(pi);
        if (!initPluginConfigModel(pi)) return;
        pi.getUIManager().addUIListener(new UIManagerListener() {

            public void UIAttached(UIInstance instance) {
                if (instance instanceof UISWTInstance) {
                    swtInstance = ((UISWTInstance) instance);
                    viewListener = new TSViewListener();
                    if (viewListener != null) {
                        swtInstance.addView(UISWTInstance.VIEW_MAIN, VIEWID, viewListener);
                        if (autoOpenPlugin) {
                            swtInstance.openMainView(VIEWID, viewListener, null);
                        }
                    }
                }
            }

            public void UIDetached(UIInstance instance) {
                if (instance instanceof UISWTInstance) swtInstance = null;
            }
        });
    }

    /** 
	 * Provides access to the main PluginInterface instance in order to access
	 * various utility functions.
	 * 
	 * @return the PluginInterface instance.
	 */
    public static PluginInterface getPluginInterface() {
        return pluginInterface;
    }

    /** 
	 * Provides access to the LoggerChannel instance in order to access various
	 * logging utility functions.
	 * 
	 * <p>
	 * Acknowledgements: Daniele Borghi (ades83@users.sourceforge.net)
	 * 
	 * @return the LoggerChannel instance.
	 */
    public static LoggerChannel getLoggerChannel() {
        return loggerChannel;
    }

    /**
	 * Initialize the plugin configuration model.
	 * 
	 * <p>
	 * Acknowledgements: Daniele Borghi (ades83@users.sourceforge.net)
	 * 
	 * @param pi the instance of the PluginInterface class passed by the Vuze
	 * plugin framework for use by this plugin.
	 * 
	 * @return true if the plugin is enabled in the plugin configuration model,
	 * otherwise return false.
	 */
    private boolean initPluginConfigModel(PluginInterface pi) {
        BasicPluginConfigModel configModel = pi.getUIManager().createBasicPluginConfigModel("plugins", "aztsearch.pluginConfig.name");
        final BooleanParameter enableCheckbox = configModel.addBooleanParameter2("aztsearch.pluginConfig.enableCheckbox", "aztsearch.pluginConfig.enableCheckbox", true);
        final BooleanParameter autoOpenCheckbox = configModel.addBooleanParameter2("aztsearch.pluginConfig.autoOpenCheckbox", "aztsearch.pluginConfig.autoOpenCheckbox", true);
        @SuppressWarnings("unused") final ActionParameter updateButton = configModel.addActionParameter2("aztsearch.pluginConfig.updateButtonLabel", "aztsearch.pluginConfig.updateButton");
        if (!(enableCheckbox.getValue())) {
            loggerChannel.log("Torrent Search plugin disabled.");
            return false;
        }
        autoOpenPlugin = autoOpenCheckbox.getValue();
        return true;
    }

    /**
	 * Initialize the plugin view model. Here we just add the logger channel to
	 * a view so we can see any results of any logging to this channel.
	 * 
	 * <p>
	 * Acknowledgements: Daniele Borghi (ades83@users.sourceforge.net) 
	 * 
	 * @param pi the instance of the PluginInterface class passed by the Vuze
	 * plugin framework for use by this plugin.
	 */
    private void initPluginViewModel(PluginInterface pi) {
        final BasicPluginViewModel viewModel = pi.getUIManager().createBasicPluginViewModel("Torrent Search Log");
        viewModel.getActivity().setVisible(false);
        viewModel.getProgress().setVisible(false);
        viewModel.attachLoggerChannel(loggerChannel);
    }
}
