package org.coos.extender;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import org.coos.util.macro.MacroSubstituteReader;
import org.coos.messaging.COContainer;
import org.coos.messaging.Channel;
import org.coos.messaging.ChannelServer;
import org.coos.messaging.ConnectingException;
import org.coos.messaging.Plugin;
import org.coos.messaging.PluginFactory;
import org.coos.messaging.impl.DefaultChannel;
import org.coos.messaging.plugin.PluginChannel;
import org.coos.messaging.transport.JvmTransport;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Magne Rasmussen for Telespor AS
 */
public final class CoosContainer implements COContainer {

    private static final Log LOG = LogFactory.getLog(CoosContainer.class);

    private final BundleContext context;

    private final Bundle bundle;

    private final String contextDir;

    private final File configDir;

    private final List<Plugin> plugins;

    public CoosContainer(final BundleContext context, final Bundle bundle, final String contextDir, final String configDir) {
        this.context = context;
        this.bundle = bundle;
        this.contextDir = contextDir;
        this.configDir = new File(configDir);
        this.plugins = new ArrayList<Plugin>();
    }

    void start() {
        LOG.info("OSGi plugin container starting [" + this.bundle.getSymbolicName() + "]");
        final List<String> pluginDefs = findPluginDefinitions();
        if (pluginDefs.isEmpty()) {
            LOG.warn("The plugin container has no plugin definitions");
        } else {
            createPlugins(pluginDefs);
        }
        LOG.info("OSGi plugin container started [" + this.bundle.getSymbolicName() + "]");
    }

    void stop() {
        LOG.info("OSGi plugin container stopping [" + this.bundle.getSymbolicName() + "]");
        for (final Plugin plugin : this.plugins) {
            plugin.disconnect();
        }
        LOG.info("OSGi plugin container stopped [" + this.bundle.getSymbolicName() + "]");
    }

    public Class<?> loadClass(final String className) throws ClassNotFoundException {
        try {
            return this.bundle.loadClass(className);
        } catch (final IllegalStateException ex) {
            throw new ClassNotFoundException("BundleContext no longer valid", ex);
        }
    }

    public InputStream getResource(final String resourceName) throws IOException {
        try {
            final URL resourceUrl = getResourceUrl(resourceName);
            if (resourceUrl == null) {
                return null;
            }
            final InputStream stream = resourceUrl.openStream();
            if (resourceName.endsWith(".xml")) {
                return substitute(stream);
            }
            return stream;
        } catch (final IllegalStateException ex) {
            throw new IOException("BundleContext no longer valid", ex);
        }
    }

    public Object getObject(final String name) {
        if (BUNDLE_CONTEXT.equals(name)) {
            return this.context;
        }
        return null;
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
                final String file = path.substring(path.lastIndexOf('/'));
                definitions.add(file);
            }
        }
        return definitions;
    }

    private void createPlugins(final List<String> pluginDefs) {
        for (final String pluginDef : pluginDefs) {
            try {
                LOG.info("Starting plugins defined in " + this.contextDir + pluginDef);
                createPlugins(getResource(pluginDef));
            } catch (final Exception ex) {
                LOG.warn("Failed to load plugins defined in " + pluginDef + ": " + ex.getMessage());
                ex.printStackTrace();
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
            if (channel instanceof PluginChannel && ((DefaultChannel) channel).getTransport() instanceof JvmTransport) {
                final JvmTransport transport = (JvmTransport) ((DefaultChannel) channel).getTransport();
                final ChannelServer channelServer = getChannelServer();
                transport.setChannelServer(channelServer);
            }
        }
        plugin.connect();
    }

    private ChannelServer getChannelServer() {
        final ServiceReference reference = this.context.getServiceReference(ChannelServer.class.getName());
        if (reference == null) {
            throw new RuntimeException("No ChannelServer is registered with the OSGi framework");
        }
        return (ChannelServer) this.context.getService(reference);
    }

    private URL getResourceUrl(final String resourceName) {
        URL resourceUrl = getConfigResourceUrl(resourceName);
        if (resourceUrl == null) {
            resourceUrl = getBundleResourceUrl(resourceName);
        }
        return resourceUrl;
    }

    private URL getConfigResourceUrl(final String resourceName) {
        final File resource = new File(this.configDir, resourceName);
        if (resource.exists() && resource.isFile()) {
            try {
                LOG.info("Container resource [" + getBundleResourceName(resourceName) + "] overridden by [" + resource + "]");
                return resource.toURI().toURL();
            } catch (final MalformedURLException ex) {
            }
        }
        return null;
    }

    private URL getBundleResourceUrl(final String resourceName) {
        return this.bundle.getResource(getBundleResourceName(resourceName));
    }

    private String getBundleResourceName(final String resourceName) {
        if (resourceName != null && resourceName.length() > 0 && resourceName.charAt(0) == '/') {
            return this.contextDir + resourceName;
        }
        return this.contextDir + "/" + resourceName;
    }

    private InputStream substitute(final InputStream stream) throws IOException {
        final InputStreamReader isr = new InputStreamReader(stream);
        final MacroSubstituteReader msr = new MacroSubstituteReader(isr);
        final String substituted = msr.substituteMacros();
        return new ByteArrayInputStream(substituted.getBytes());
    }
}
