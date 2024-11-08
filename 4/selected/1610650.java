package org.coos.extender;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import org.coos.messaging.COOS;
import org.coos.messaging.COOSFactory;
import org.coos.messaging.ChannelServer;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class OSGICoosContainer extends OSGICOContainer {

    private static final Log LOG = LogFactory.getLog(OSGICoosContainer.class);

    private final List<COOS> COOSs;

    public OSGICoosContainer(BundleContext context, Bundle bundle, String contextDir, String configDir, CoosExtender extender) {
        super(context, bundle, contextDir, configDir, extender);
        this.COOSs = Collections.synchronizedList(new ArrayList<COOS>());
    }

    @Override
    public void start() throws Exception {
        LOG.info("OSGI COOS Container starting");
        final List<String> coosDefs = findCoosDefinitions();
        createCOOSs(coosDefs);
    }

    private void createCOOSs(List<String> coosDefs) throws Exception {
        for (final String coosDef : coosDefs) {
            try {
                LOG.info("Starting cooses defined in " + contextDir + coosDef);
                createCOOS(getResource(coosDef));
            } catch (final Exception ex) {
                LOG.warn("Failed to load coos defined in " + coosDef + ": " + ex.getMessage(), ex);
            }
        }
    }

    private void createCOOS(InputStream is) throws Exception {
        COOS coos = null;
        try {
            coos = COOSFactory.createCOOS(is, this);
            coos.start();
        } catch (Exception e) {
            LOG.info("Failed to load COOS(s)", e);
            throw e;
        }
        COOSs.add(coos);
        ChannelServer channelServer = coos.getChannelServer("default");
        context.registerService(ChannelServer.class.getName(), channelServer, new Hashtable());
        LOG.info("ChannelServer registered");
    }

    private List<String> findCoosDefinitions() {
        LOG.info("Searching for coos definitions in " + this.contextDir);
        final List<String> definitions = new ArrayList<String>();
        final Enumeration<URL> entries = this.bundle.findEntries(this.contextDir, "coos*.xml", false);
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
        for (String pluginName : definitions) {
            final String pluginStart = pluginName.substring(0, pluginName.indexOf(".xml"));
            String[] files = filConfigDir.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    if (name.startsWith(pluginStart) && name.endsWith(".xml")) return true;
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

    @Override
    public void stop() throws Exception {
        LOG.info("OSGI COOS container stopping");
        for (COOS coos : COOSs) coos.stop();
    }
}
