package org.gudy.azureus2.update;

import java.io.*;
import java.util.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class CorePatchChecker implements Plugin, UpdatableComponent, UpdateCheckInstanceListener {

    private static final LogIDs LOGID = LogIDs.CORE;

    public static final boolean TESTING = false;

    protected PluginInterface plugin_interface;

    private Map<UpdateCheckInstance, Update> my_updates = new HashMap<UpdateCheckInstance, Update>(1);

    public void initialize(PluginInterface _plugin_interface) throws PluginException {
        plugin_interface = _plugin_interface;
        plugin_interface.getPluginProperties().setProperty("plugin.version", "1.0");
        plugin_interface.getPluginProperties().setProperty("plugin.name", "Core Patcher (level=" + CorePatchLevel.getCurrentPatchLevel() + ")");
        if (TESTING || !Constants.isCVSVersion()) {
            if (TESTING) {
                System.out.println("CorePatchChecker: TESTING !!!!");
            }
            plugin_interface.getUpdateManager().registerUpdatableComponent(this, false);
        }
    }

    public String getName() {
        return ("Core Patch Checker");
    }

    public int getMaximumCheckTime() {
        return (0);
    }

    public void checkForUpdate(UpdateChecker checker) {
        try {
            UpdateCheckInstance inst = checker.getCheckInstance();
            inst.addListener(this);
            my_updates.put(inst, checker.addUpdate("Core Patch Checker", new String[0], "", new ResourceDownloader[0], Update.RESTART_REQUIRED_MAYBE));
        } finally {
            checker.completed();
        }
    }

    public void cancelled(UpdateCheckInstance instance) {
        Update update = my_updates.remove(instance);
        if (update != null) {
            update.cancel();
        }
    }

    public void complete(final UpdateCheckInstance instance) {
        Update my_update = my_updates.remove(instance);
        if (my_update != null) {
            my_update.complete(true);
        }
        Update[] updates = instance.getUpdates();
        final PluginInterface updater_plugin = plugin_interface.getPluginManager().getPluginInterfaceByClass(UpdaterUpdateChecker.class);
        for (int i = 0; i < updates.length; i++) {
            final Update update = updates[i];
            Object user_object = update.getUserObject();
            if (user_object != null && user_object == updater_plugin) {
                if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Core Patcher: updater update found"));
                update.setRestartRequired(Update.RESTART_REQUIRED_MAYBE);
                update.addListener(new UpdateListener() {

                    public void complete(Update update) {
                        if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Core Patcher: updater update complete"));
                        patch(instance, update, updater_plugin);
                    }

                    public void cancelled(Update update) {
                    }
                });
            }
        }
    }

    protected void patch(UpdateCheckInstance instance, Update updater_update, PluginInterface updater_plugin) {
        try {
            ResourceDownloader rd_log = updater_update.getDownloaders()[0];
            File[] files = new File(updater_plugin.getPluginDirectoryName()).listFiles();
            if (files == null) {
                if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Core Patcher: no files in plugin dir!!!"));
                return;
            }
            String patch_prefix = "Azureus2_" + Constants.getBaseVersion() + "_P";
            int highest_p = -1;
            File highest_p_file = null;
            for (int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                if (name.startsWith(patch_prefix) && name.endsWith(".pat")) {
                    if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Core Patcher: found patch file '" + name + "'"));
                    try {
                        int this_p = Integer.parseInt(name.substring(patch_prefix.length(), name.indexOf(".pat")));
                        if (this_p > highest_p) {
                            highest_p = this_p;
                            highest_p_file = files[i];
                        }
                    } catch (Throwable e) {
                        Debug.printStackTrace(e);
                    }
                }
            }
            if (CorePatchLevel.getCurrentPatchLevel() >= highest_p) {
                if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Core Patcher: no applicable patch found (highest = " + highest_p + ")"));
                if (updater_update.getRestartRequired() == Update.RESTART_REQUIRED_MAYBE) {
                    updater_update.setRestartRequired(Update.RESTART_REQUIRED_NO);
                }
            } else {
                rd_log.reportActivity("Applying patch '" + highest_p_file.getName() + "'");
                if (Logger.isEnabled()) Logger.log(new LogEvent(LOGID, "Core Patcher: applying patch '" + highest_p_file.toString() + "'"));
                InputStream pis = new FileInputStream(highest_p_file);
                patchAzureus2(instance, pis, "P" + highest_p, plugin_interface.getLogger().getChannel("CorePatcher"));
                Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_INFORMATION, "Patch " + highest_p_file.getName() + " ready to be applied"));
                String done_file = highest_p_file.toString();
                done_file = done_file.substring(0, done_file.length() - 1) + "x";
                highest_p_file.renameTo(new File(done_file));
                updater_update.setRestartRequired(Update.RESTART_REQUIRED_YES);
            }
        } catch (Throwable e) {
            Debug.printStackTrace(e);
            Logger.log(new LogAlert(LogAlert.UNREPEATABLE, "Core Patcher failed", e));
        }
    }

    public static void patchAzureus2(UpdateCheckInstance instance, InputStream pis, String resource_tag, LoggerChannel log) throws Exception {
        String resource_name = "Azureus2_" + resource_tag + ".jar";
        UpdateInstaller installer = instance.createInstaller();
        File tmp = AETemporaryFileHandler.createTempFile();
        OutputStream os = new FileOutputStream(tmp);
        String az2_jar;
        if (Constants.isOSX) {
            az2_jar = installer.getInstallDir() + "/" + SystemProperties.getApplicationName() + ".app/Contents/Resources/Java/";
        } else {
            az2_jar = installer.getInstallDir() + File.separator;
        }
        az2_jar += "Azureus2.jar";
        InputStream is = new FileInputStream(az2_jar);
        new UpdateJarPatcher(is, pis, os, log);
        is.close();
        pis.close();
        os.close();
        installer.addResource(resource_name, new FileInputStream(tmp));
        tmp.delete();
        installer.addMoveAction(resource_name, az2_jar);
    }
}
