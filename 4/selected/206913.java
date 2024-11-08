package cgl.shindig.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cgl.shindig.config.ConfigurationException;
import cgl.shindig.usermanage.util.ResourceLoader;

public class PortalConfig {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(PortalConfig.class);

    /** Name of the default configuration file. */
    private static final String PORTAL_CONFIG_XML = "portal.xml";

    public static PortalConfig install(File dir) throws IOException, ConfigurationException {
        return install(new File(dir, PORTAL_CONFIG_XML), dir);
    }

    public static PortalConfig install(File xml, File dir) throws IOException, ConfigurationException {
        if (!dir.exists()) {
            log.info("Creating directory {}", dir);
            dir.mkdirs();
        }
        if (!xml.exists()) {
            log.info("Installing default configuration to {}", xml);
            OutputStream output = new FileOutputStream(xml);
            try {
                InputStream input = ResourceLoader.open("res://" + PORTAL_CONFIG_XML);
                try {
                    IOUtils.copy(input, output);
                } finally {
                    input.close();
                }
            } finally {
                output.close();
            }
        }
        return create(xml, dir);
    }

    public static PortalConfig create(File dir) throws ConfigurationException {
        return create(new File(dir, PORTAL_CONFIG_XML), dir);
    }

    public static PortalConfig create(File xml, File dir) throws ConfigurationException {
        if (!dir.isDirectory()) {
            throw new ConfigurationException("directory " + dir + " does not exist");
        }
        if (!xml.isFile()) {
            throw new ConfigurationException("configuration file " + xml + " does not exist");
        }
        try {
            return create(new FileInputStream(xml), dir.getPath());
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException("File " + xml + " cannot be found. ", ex);
        }
    }

    public static PortalConfig create(String file, String home) throws ConfigurationException {
        try {
            return create(new FileInputStream(file), home);
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException("File " + file + " cannot be found. ", ex);
        }
    }

    public static PortalConfig create(URI uri, String home) throws ConfigurationException {
        return create(new File(uri), new File(home));
    }

    public static PortalConfig create(InputStream input, String home) throws ConfigurationException {
        try {
            Properties variables = new Properties(System.getProperties());
            PortalConfigParser parser = new PortalConfigParser(variables);
            PortalConfig config = parser.parsePortalConfig(input);
            config.init();
            return config;
        } catch (Exception ex) {
            throw new ConfigurationException("apache common configuration exception", ex);
        }
    }

    private final SecurityConfig sec;

    private final PortalConfigParser parser;

    public PortalConfig(SecurityConfig sec, PortalConfigParser parser) {
        this.sec = sec;
        this.parser = parser;
    }

    public void init() throws ConfigurationException, IllegalStateException {
    }

    public String getAppName() {
        return sec.getAppName();
    }

    public AccessManagerConfig getAccessManagerConfig() {
        return sec.getAccessManagerConfig();
    }

    public LoginModuleConfig getLoginModuleConfig() {
        return sec.getLoginModuleConfig();
    }

    public SecurityConfig getSecurityConfig() {
        return sec;
    }
}
