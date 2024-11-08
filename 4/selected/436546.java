package phex.prefs.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phex.common.collections.SortedProperties;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.utils.FileUtils;
import phex.utils.IOUtil;

public class Preferences {

    private static final Logger logger = LoggerFactory.getLogger(Preferences.class);

    private PhexEventService eventService;

    private Map<String, Setting<?>> settingMap;

    private File prefFile;

    private Properties valueProperties;

    private boolean isSaveRequired;

    public Preferences(File file) {
        this(file, null);
    }

    public Preferences(File file, PhexEventService eventService) {
        prefFile = file;
        this.eventService = eventService;
        settingMap = new HashMap<String, Setting<?>>();
        valueProperties = new Properties();
    }

    public void setEventService(PhexEventService eventService) {
        this.eventService = eventService;
    }

    protected String getLoadedProperty(String name) {
        return valueProperties.getProperty(name);
    }

    protected List<String> getPrefixedPropertyNames(String prefix) {
        List<String> found = new ArrayList<String>();
        Set<Object> keys = valueProperties.keySet();
        for (Object keyObj : keys) {
            String key = (String) keyObj;
            if (key.startsWith(prefix)) {
                found.add(key);
            }
        }
        return found;
    }

    protected void registerSetting(String name, Setting<?> setting) {
        settingMap.put(name, setting);
    }

    public synchronized void saveRequiredNotify() {
        isSaveRequired = true;
    }

    protected void fireSettingChanged(SettingChangedEvent<?> event) {
        saveRequiredNotify();
        if (eventService != null) {
            eventService.publish(PhexEventTopics.Prefs_Changed, event);
        }
    }

    public synchronized void load() {
        Properties loadProperties = new Properties();
        InputStream inStream = null;
        try {
            inStream = new BufferedInputStream(new FileInputStream(prefFile));
            loadProperties.load(inStream);
        } catch (IOException exp) {
            IOUtil.closeQuietly(inStream);
            if (!(exp instanceof FileNotFoundException)) {
                logger.error(exp.toString(), exp);
            }
            File bakFile = new File(prefFile.getParentFile(), prefFile.getName() + ".bak");
            try {
                inStream = new BufferedInputStream(new FileInputStream(bakFile));
                loadProperties.load(inStream);
            } catch (FileNotFoundException exp2) {
            } catch (IOException exp2) {
                logger.error(exp.toString(), exp);
            }
        } finally {
            IOUtil.closeQuietly(inStream);
        }
        valueProperties = loadProperties;
    }

    public synchronized void save() {
        if (!isSaveRequired) {
            logger.debug("No saving of preferences required.");
            return;
        }
        logger.debug("Saving preferences to: " + prefFile.getAbsolutePath());
        Properties saveProperties = new SortedProperties();
        for (Setting<?> setting : settingMap.values()) {
            if (setting.isDefault() && !setting.isAlwaysSaved()) {
                continue;
            }
            PreferencesCodec.serializeSetting(setting, saveProperties);
        }
        File bakFile = new File(prefFile.getParentFile(), prefFile.getName() + ".bak");
        try {
            if (prefFile.exists()) {
                FileUtils.copyFile(prefFile, bakFile);
            }
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(prefFile));
                saveProperties.store(os, "Phex Preferences");
            } finally {
                IOUtil.closeQuietly(os);
            }
        } catch (IOException exp) {
            logger.error(exp.toString(), exp);
        }
    }
}
