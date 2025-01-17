package com.limegroup.gnutella.gui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.limewire.io.IOUtils;
import com.limegroup.gnutella.settings.UISettings;

public class ButtonIconController {

    private Icon NULL = new ImageIcon();

    /**
     * A mapping of user-friendly names to the file name
     * of the icon.
     */
    private final Properties BUTTON_NAMES = loadButtonNameMap();

    /**
     * A mapping of the file name of the icon to the icon itself,
     * so we don't load the resource multiple times.
     */
    private final Map<String, Icon> BUTTON_CACHE = new HashMap<String, Icon>();

    /**
     * Wipes out the button icon cache, so we can switch from large to small
     * icons (or vice versa).
     */
    public void wipeButtonIconCache() {
        BUTTON_CACHE.clear();
    }

    /**
     * Retrieves the icon for the specified button name.
     */
    public Icon getIconForButton(String buttonName) {
        String fileName = BUTTON_NAMES.getProperty(buttonName);
        if (fileName == null) return null;
        Icon icon = BUTTON_CACHE.get(fileName);
        if (icon == NULL) return null;
        if (icon != null) return icon;
        try {
            String retrieveName;
            if (UISettings.SMALL_ICONS.getValue()) retrieveName = fileName + "_small"; else retrieveName = fileName + "_large";
            icon = ResourceManager.getThemeImage(retrieveName);
            BUTTON_CACHE.put(fileName, icon);
        } catch (MissingResourceException mre) {
            try {
                icon = ResourceManager.getThemeImage(fileName);
                BUTTON_CACHE.put(fileName, icon);
            } catch (MissingResourceException mre2) {
                BUTTON_CACHE.put(fileName, NULL);
            }
        }
        return icon;
    }

    /**
     * Retrieves the rollover image for the specified button name.
     */
    public Icon getRolloverIconForButton(String buttonName) {
        String fileName = BUTTON_NAMES.getProperty(buttonName);
        if (fileName == null) return null;
        String rolloverName = fileName + "_rollover";
        Icon rollover = BUTTON_CACHE.get(rolloverName);
        if (rollover == NULL) return null;
        if (rollover != null) return rollover;
        Icon icon = BUTTON_CACHE.get(fileName);
        if (icon == NULL || icon == null) {
            BUTTON_CACHE.put(rolloverName, NULL);
            return null;
        }
        rollover = ImageManipulator.brighten(icon);
        if (rollover == null) BUTTON_CACHE.put(rolloverName, NULL); else BUTTON_CACHE.put(rolloverName, rollover);
        return rollover;
    }

    private static Properties loadButtonNameMap() {
        Properties p = new Properties();
        URL url = ResourceManager.getURLResource("icon_mapping.properties");
        InputStream is = null;
        try {
            if (url != null) {
                is = new BufferedInputStream(url.openStream());
                p.load(is);
            }
        } catch (IOException ignored) {
        } finally {
            IOUtils.close(is);
        }
        return p;
    }
}
