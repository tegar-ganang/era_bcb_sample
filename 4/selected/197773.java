package org.coos.extender;

import org.coos.messaging.Channel;
import org.coos.messaging.ChannelServer;
import org.coos.messaging.ConnectingException;
import org.coos.messaging.Plugin;
import org.coos.messaging.PluginFactory;
import org.coos.messaging.plugin.PluginChannel;
import org.coos.messaging.transport.JvmTransport;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

public class OSGICoosPluginContainer extends OSGICOContainer {

    private static final Log LOG = LogFactory.getLog(OSGICoosPluginContainer.class);

    private final List<Plugin> plugins;

    public OSGICoosPluginContainer(BundleContext context, Bundle bundle, String contextDir, String configDir, CoosExtender extender) {
        super(context, bundle, contextDir, configDir, extender);
        this.plugins = new ArrayList<Plugin>();
    }

    @Override
    public void start() throws Exception {
        LOG.info("OSGi plugin container starting [" + this.bundle.getSymbolicName() + "]");
        final List<String> pluginDefs = findPluginDefinitions();
        if (pluginDefs.isEmpty()) {
            LOG.warn("The plugin container has no plugin definitions");
            throw new Exception("No plugin definitions");
        } else {
            createPlugins(pluginDefs);
        }
        LOG.info("OSGi plugin container started [" + this.bundle.getSymbolicName() + "]");
    }

    public void stop() throws Exception {
        LOG.info("OSGi plugin container stopping [" + this.bundle.getSymbolicName() + "]");
        for (final Plugin plugin : this.plugins) {
            plugin.disconnect();
        }
        LOG.info("OSGi plugin container stopped [" + this.bundle.getSymbolicName() + "]");
    }

    @SuppressWarnings("unchecked")
    private List<String> findPluginDefinitions() {
        LOG.info("Searching for plugin definitions in " + this.contextDir);
        final List<String> definitions = new ArrayList<String>();
        final Enumeration<URL> entries = this.bundle.findEntries(this.contextDir, "plugin*.xml", false);
        if (entries != null) {
            while (entries.hasMoreElements()) {
                final URL entry = entries.nextElement();
                final String path = entry.getFile();
                final String file = new String(path.substring(path.lastIndexOf('/') + 1));
                definitions.add(file);
            }
        }
        File filConfigDir = new File(this.configDir.getAbsolutePath() + "/" + this.contextDir);
        LOG.info("Searching for plugin definitions in " + filConfigDir);
        final List<String> configDirDefinitions = new ArrayList<String>();
        for (final String pluginName : definitions) {
            final String pluginStart = pluginName.substring(0, pluginName.indexOf(".xml"));
            String[] files = filConfigDir.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    if (name.equals(pluginName) || (name.startsWith(pluginStart + "-") && name.endsWith(".xml"))) return true;
                    return false;
                }
            });
            List<String> res = new ArrayList<String>();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    configDirDefinitions.add(files[i]);
                }
            }
        }
        if (configDirDefinitions.size() > 0) return configDirDefinitions;
        return definitions;
    }

    private void createPlugins(final List<String> pluginDefs) throws Exception {
        for (final String pluginDef : pluginDefs) {
            try {
                LOG.info("Starting plugins defined in " + this.contextDir + "/" + pluginDef);
                createPlugins(getResource(pluginDef));
            } catch (final Exception ex) {
                LOG.warn("Failed to load plugins defined in " + pluginDef + ": " + ex.getMessage());
                throw ex;
            }
        }
    }

    private void createPlugins(final InputStream pluginDef) throws Exception {
        final List<Plugin> connectedPlugins = new ArrayList<Plugin>();
        try {
            final Plugin[] newPlugins = PluginFactory.createPlugins(pluginDef, this);
            for (final Plugin plugin : newPlugins) {
                startPlugin(plugin);
                connectedPlugins.add(plugin);
            }
        } catch (final Exception ex) {
            for (final Plugin plugin : connectedPlugins) {
                plugin.disconnect();
            }
            throw ex;
        }
        this.plugins.addAll(connectedPlugins);
    }

    @SuppressWarnings("unchecked")
    private void startPlugin(final Plugin plugin) throws ConnectingException {
        for (final Channel channel : (Vector<Channel>) plugin.getChannels()) {
            if ((channel instanceof PluginChannel) && (channel.getTransport() instanceof JvmTransport)) {
                final JvmTransport transport = (JvmTransport) channel.getTransport();
                final ChannelServer channelServer = getChannelServer();
                transport.setChannelServer(channelServer);
            }
        }
        plugin.connect();
    }

    private ChannelServer getChannelServer() {
        ServiceTracker tracker = new ServiceTracker(context, ChannelServer.class.getName(), null);
        tracker.open(true);
        Object tracked = null;
        try {
            tracked = getTrackedService(tracker);
            if (tracked == null) throw new RuntimeException("Couldn't find ChannelServer in the OSGi framework");
        } catch (Exception e) {
            throw new RuntimeException("No ChannelServer is registered with the OSGi framework", e);
        }
        return (ChannelServer) tracked;
    }

    @SuppressWarnings("unchecked")
    private <T> T getTrackedService(final ServiceTracker serviceTracker) {
        int max = 150;
        T service = null;
        for (int timeout = 1; (service == null) && (timeout < max); timeout++) {
            Calendar calendar = new GregorianCalendar();
            LOG.info(calendar.get(Calendar.HOUR) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + " : Waited " + Integer.valueOf(timeout) + " seconds for messaging implementation.");
            try {
                service = (T) serviceTracker.waitForService(1000L);
            } catch (InterruptedException e) {
                LOG.warn("InteruptException ignored", e);
            }
        }
        return service;
    }
}
