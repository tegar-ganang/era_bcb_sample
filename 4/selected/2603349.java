package de.uni_leipzig.lots.webfrontend.app;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alexander Kiel
 * @version $Id: AbstractHomeDirConfig.java,v 1.6 2007/10/23 06:30:19 mai99bxd Exp $
 */
public abstract class AbstractHomeDirConfig {

    private static final Logger logger = Logger.getLogger(AbstractHomeDirConfig.class.getName());

    @NotNull
    private HomeLocator homeLocator;

    @NotNull
    private String configFilename;

    @Nullable
    private String comment;

    @Nullable
    private Properties properties;

    /**
     * Timestamp of the last modification done by us.
     */
    private long lastModifiedByUs = -1;

    protected AbstractHomeDirConfig() {
    }

    public void setHomeLocator(@NotNull HomeLocator homeLocator) {
        this.homeLocator = homeLocator;
    }

    public void setConfigFilename(@NotNull String configFilename) {
        this.configFilename = configFilename;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    @Nullable
    protected String getProperty(@NotNull String key) {
        String value = loadProperties().getProperty(key);
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, getClass().getName(), "getProperty", "use property (" + key + ", " + value + ")");
        }
        return value;
    }

    @NotNull
    protected String getProperty(@NotNull String key, @NotNull String defaultValue) {
        String value = loadProperties().getProperty(key, defaultValue);
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, getClass().getName(), "getProperty", "use property (" + key + ", " + value + ")");
        }
        return value;
    }

    protected void setProperty(@NotNull String key, @NotNull String value) {
        Properties properties = loadProperties();
        properties.setProperty(key, value);
        storeProperties(properties);
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, getClass().getName(), "setProperty", "store property (" + key + ", " + value + ")");
        }
    }

    @NotNull
    private Properties loadProperties() {
        File file = new File(homeLocator.getHomeDir(), configFilename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("IOException while creating \"" + file.getAbsolutePath() + "\".", e);
            }
        }
        if (!file.canRead() || !file.canWrite()) {
            throw new RuntimeException("Cannot read and write from file: " + file.getAbsolutePath());
        }
        if (lastModifiedByUs < file.lastModified()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("File \"" + file + "\" is newer on disk. Read it ...");
            }
            Properties properties = new Properties();
            try {
                FileInputStream in = new FileInputStream(file);
                try {
                    properties.loadFromXML(in);
                } catch (InvalidPropertiesFormatException e) {
                    FileOutputStream out = new FileOutputStream(file);
                    try {
                        properties.storeToXML(out, comment);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("IOException while reading from \"" + file.getAbsolutePath() + "\".", e);
            }
            this.lastModifiedByUs = file.lastModified();
            this.properties = properties;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("... read done.");
            }
        }
        assert this.properties != null;
        return this.properties;
    }

    private void storeProperties(@NotNull Properties properties) {
        File file = new File(homeLocator.getHomeDir(), configFilename);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Store properties to file \"" + file + "\" ...");
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            try {
                properties.storeToXML(out, comment, "UTF-8");
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException while writing to \"" + file.getAbsolutePath() + "\".", e);
        }
        this.lastModifiedByUs = file.lastModified();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("... store done.");
        }
    }
}
