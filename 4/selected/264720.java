package com.google.code.sagetvaddons.sagealert.plugin;

import gkusnick.sagetv.api.API;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import sage.SageTVPlugin;
import sage.SageTVPluginRegistry;
import com.google.code.sagetvaddons.sagealert.server.DataStore;
import com.google.code.sagetvaddons.sagealert.shared.Client;

/**
 * @author dbattams
 *
 */
public final class Plugin implements SageTVPlugin {

    private static Logger LOG = null;

    public static final String PLUGIN_ID = "sa2";

    public static final File RES_DIR = new File("plugins/sagealert");

    public static final String OPT_PREFIX = "sagealert/";

    public static final String OPT_IGNORE_REPEAT_SYS_MSGS = OPT_PREFIX + "IgnoreRepeatSysMsgs";

    public static final String OPT_IGNORE_REPEAT_SYS_MSGS_DEFAULT = "true";

    public static Plugin INSTANCE = null;

    /**
	 * 
	 */
    public Plugin(SageTVPluginRegistry reg) {
        synchronized (Plugin.class) {
            if (LOG == null) {
                PropertyConfigurator.configure(new File(RES_DIR, "sagealert.log4j.properties").getAbsolutePath());
                LOG = Logger.getLogger(Plugin.class);
            }
            INSTANCE = this;
        }
    }

    public void destroy() {
    }

    public String getConfigHelpText(String arg0) {
        String helpTxt;
        if (OPT_IGNORE_REPEAT_SYS_MSGS.equals(arg0)) helpTxt = "If true, repeated system messages, as determined by the core, will be ignored and not processed by SageAlert."; else helpTxt = "No help available.";
        return helpTxt;
    }

    public String getConfigLabel(String arg0) {
        String lbl;
        if (OPT_IGNORE_REPEAT_SYS_MSGS.equals(arg0)) lbl = "Ignore Repeated System Messages"; else lbl = "No label available";
        return lbl;
    }

    public String[] getConfigOptions(String arg0) {
        return null;
    }

    public String[] getConfigSettings() {
        return new String[] { OPT_IGNORE_REPEAT_SYS_MSGS };
    }

    public int getConfigType(String arg0) {
        if (OPT_IGNORE_REPEAT_SYS_MSGS.equals(arg0)) return SageTVPlugin.CONFIG_BOOL;
        throw new RuntimeException("Unknown option! [" + arg0 + "]");
    }

    public String getConfigValue(String arg0) {
        String defaultVal = null;
        if (OPT_IGNORE_REPEAT_SYS_MSGS.equals(arg0)) defaultVal = OPT_IGNORE_REPEAT_SYS_MSGS_DEFAULT;
        return API.apiNullUI.configuration.GetServerProperty(arg0, defaultVal);
    }

    public String[] getConfigValues(String arg0) {
        return null;
    }

    public void resetConfig() {
        setConfigValue(OPT_IGNORE_REPEAT_SYS_MSGS, OPT_IGNORE_REPEAT_SYS_MSGS_DEFAULT);
    }

    public void setConfigValue(String arg0, String arg1) {
        API.apiNullUI.configuration.SetServerProperty(arg0, arg1);
    }

    public void setConfigValues(String arg0, String[] arg1) {
    }

    public void start() {
        LOG.info("Deploying SageAlert v2.x into Jetty plugin...");
        try {
            FileUtils.copyFileToDirectory(new File(RES_DIR, "SageAlert.war"), new File("jetty/webapps"), true);
            FileUtils.copyFileToDirectory(new File(RES_DIR, "SageAlert.context.xml"), new File("jetty/contexts"), false);
            LOG.info("Deployment successful!");
        } catch (IOException e) {
            LOG.fatal("Deployment failed!", e);
            throw new RuntimeException(e);
        }
        LOG.info("Registering all connected UI contexts...");
        DataStore ds = DataStore.getInstance();
        for (String clntIp : (String[]) ArrayUtils.addAll(API.apiNullUI.global.GetConnectedClients(), API.apiNullUI.global.GetUIContextNames())) {
            Client c = ds.getClient(clntIp);
            ds.registerClient(c.getId());
            LOG.info("Client id '" + c.getId() + "' registered!");
        }
    }

    public void stop() {
        LOG.info("Undeploying SageAlert from Jetty plugin...");
        if (!new File("jetty/webapps/SageAlert.war").delete()) LOG.warn("Unable to delete SageAlert war file; you may need to restart SageTV to correct this error!");
        if (!new File("jetty/contexts/SageAlert.context.xml").delete()) LOG.error("Unable to delete SageAlert context file; you will need to stop SageTV and delete this file manually!");
        LOG.info("Undeployment completed!");
    }

    @SuppressWarnings("rawtypes")
    public void sageEvent(String arg0, Map arg1) {
    }
}
