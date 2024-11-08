package org.gudy.azureus2.pluginsimpl.local.deprecate;

import java.util.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.StringListImpl;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

/**
 * @author Allan Crooks
 *
 */
public class PluginDeprecation {

    private static final String CONFIG_KEY = "PluginDeprecationWarnings";

    private static final String FORUM_STABLE_LINK = "http://forum.vuze.com/forum.jspa?forumID=3";

    private static final String FORUM_BETA_LINK = "http://forum.vuze.com/forum.jspa?forumID=4";

    private static final int IGNORE = 0;

    private static final int NOTIFY_ONCE = 1;

    private static final int NOTIFY_EVERY = 2;

    private static final int NOTIFY_AND_DIE = 3;

    private static final int DIE = 4;

    private static BasicPluginViewModel model = null;

    private static LoggerChannel channel = null;

    private static Map behaviour_mapping = new HashMap();

    private static Set persistent_warnings = Collections.synchronizedSet(new HashSet());

    private static Set instance_warnings = Collections.synchronizedSet(new HashSet());

    private static void register(String identifier, int stable_behaviour, int beta_behaviour) {
        behaviour_mapping.put(identifier, new Integer(Constants.isCVSVersion() ? beta_behaviour : stable_behaviour));
    }

    static {
        register("property listener", IGNORE, NOTIFY_EVERY);
        register("createPluginView", IGNORE, NOTIFY_EVERY);
        register("getSWTManager", IGNORE, IGNORE);
        register("openTorrentFile", IGNORE, NOTIFY_EVERY);
        register("openTorrentURL", IGNORE, NOTIFY_EVERY);
        register("setDisabled", IGNORE, NOTIFY_ONCE);
        register("isDisabled", IGNORE, NOTIFY_ONCE);
        register("isBuiltIn", IGNORE, NOTIFY_ONCE);
        register("isMandatory", IGNORE, NOTIFY_ONCE);
        register("isOperational", IGNORE, NOTIFY_ONCE);
        register("isShared", IGNORE, NOTIFY_ONCE);
        register("unload", IGNORE, NOTIFY_ONCE);
        register("reload", IGNORE, NOTIFY_ONCE);
        register("uninstall", IGNORE, NOTIFY_ONCE);
        register("isUnloadable", IGNORE, NOTIFY_ONCE);
        persistent_warnings.addAll(Arrays.asList(COConfigurationManager.getStringListParameter(CONFIG_KEY).toArray()));
    }

    public static void call(String identifier, Object context) {
        call(identifier, context.getClass().getName());
    }

    public static void call(String identifier, String context) {
        Integer behaviour = (Integer) behaviour_mapping.get(identifier);
        if (behaviour == null) {
            throw new IllegalArgumentException("unknown deprecated call identifier: " + identifier);
        }
        int b = behaviour.intValue();
        if (b == IGNORE) {
            return;
        }
        boolean persistent_notify = b == NOTIFY_ONCE;
        boolean notify = b != DIE;
        boolean raise_error = (b == NOTIFY_AND_DIE || b == DIE);
        String persistent_id = context + ":" + identifier;
        if (notify && !instance_warnings.contains(context) && (!persistent_notify || !persistent_warnings.contains(persistent_id))) {
            instance_warnings.add(context);
            if (!persistent_notify && persistent_warnings.remove(persistent_id)) {
                COConfigurationManager.setParameter(CONFIG_KEY, new StringListImpl(persistent_warnings));
            }
            synchronized (PluginDeprecation.class) {
                if (model == null) {
                    final PluginInterface pi = PluginInitializer.getDefaultInterface();
                    model = pi.getUIManager().createBasicPluginViewModel(MessageText.getString("PluginDeprecation.view"));
                    model.getStatus().setVisible(false);
                    model.getProgress().setVisible(false);
                    model.getActivity().setVisible(false);
                    model.getLogArea().appendText(MessageText.getString("PluginDeprecation.log.start", new String[] { Constants.isCVSVersion() ? FORUM_BETA_LINK : FORUM_STABLE_LINK }));
                    UIManagerListener uiml = new UIManagerListener() {

                        public void UIAttached(UIInstance inst) {
                            if (inst instanceof UISWTInstance) {
                                ((UISWTInstance) inst).openView(model);
                                pi.getUIManager().removeUIListener(this);
                            }
                        }

                        public void UIDetached(UIInstance inst) {
                        }
                    };
                    pi.getUIManager().addUIListener(uiml);
                }
                String log_details = MessageText.getString("PluginDeprecation.log.details", new String[] { identifier, context, Debug.getStackTrace(false, false) });
                model.getLogArea().appendText(log_details);
                if (channel == null) {
                    channel = PluginInitializer.getDefaultInterface().getLogger().getChannel("PluginDeprecation");
                }
                channel.logAlert(LoggerChannel.LT_WARNING, MessageText.getString("PluginDeprecation.alert"));
                Debug.out(new PluginDeprecationException("Deprecated plugin call - " + persistent_id).fillInStackTrace());
            }
            if (persistent_notify) {
                persistent_warnings.add(persistent_id);
                COConfigurationManager.setParameter(CONFIG_KEY, new StringListImpl(persistent_warnings));
            }
        }
        if (raise_error) {
            throw new PluginDeprecationException(persistent_id);
        }
    }
}
