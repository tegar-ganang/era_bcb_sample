package phex.prefs;

import java.io.*;
import java.util.*;
import phex.common.SortedProperties;
import phex.utils.*;

public class Preferences {

    private Map<String, Setting> settingMap;

    private File prefFile;

    private Properties valueProperties;

    private boolean isSaveRequired;

    public Preferences(File file) {
        prefFile = file;
        settingMap = new HashMap<String, Setting>();
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

    protected void registerSetting(String name, Setting setting) {
        settingMap.put(name, setting);
    }

    public synchronized void saveRequiredNotify() {
        isSaveRequired = true;
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
                NLogger.error(Preferences.class, exp);
            }
            File bakFile = new File(prefFile.getParentFile(), prefFile.getName() + ".bak");
            try {
                inStream = new BufferedInputStream(new FileInputStream(bakFile));
                loadProperties.load(inStream);
            } catch (FileNotFoundException exp2) {
            } catch (IOException exp2) {
                NLogger.error(Preferences.class, exp);
            }
        } finally {
            IOUtil.closeQuietly(inStream);
        }
        valueProperties = loadProperties;
    }

    public synchronized void save() {
        if (!isSaveRequired) {
            NLogger.debug(Preferences.class, "No saving of preferences required.");
            return;
        }
        NLogger.debug(Preferences.class, "Saving preferences to: " + prefFile.getAbsolutePath());
        Properties saveProperties = new SortedProperties();
        for (Setting setting : settingMap.values()) {
            if (setting.isDefault()) {
                continue;
            }
            PreferencesFactory.serializeSetting(setting, saveProperties);
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
            NLogger.error(NLoggerNames.GLOBAL, exp, exp);
        }
    }
}
