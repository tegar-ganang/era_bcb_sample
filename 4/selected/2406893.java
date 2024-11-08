package com.memoire.vainstall;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.UIManager;

/**
 * @version      $Id: VAGlobals.java,v 1.29 2005/10/14 09:35:17 deniger Exp $
 * @author       Axel von Arnim
 */
public class VAGlobals {

    public static final boolean DEBUG = "yes".equals(System.getProperty("DEBUG"));

    public static final String NAME = "VAInstall";

    public static final String VERSION = "0.23a";

    public static final String AUTHOR = "Axel von Arnim";

    public static final String COPYRIGHT = "2000-2002";

    public static final String LICENSE = "GPL2";

    public static final String HTTP = "http://www.ifrance.com/vonarnim/vainstall";

    public static final String EMAIL = "vonarnim@club-internet.fr";

    public static final Class BASE_CLASS = new VAGlobals().getClass();

    public static final int UNKNOWN = 0;

    public static final int INSTALL = 1;

    public static final int UNINSTALL = 2;

    public static final int UPDATE = 3;

    public static int OPERATION = UNKNOWN;

    public static String UI_MODE;

    public static boolean UI_BLUESCREEN;

    public static Color UI_BLUESCREEN_COLOR;

    public static String IMAGE;

    public static boolean USE_FULL_JAVA_PATH;

    public static boolean SHORTCUTS_IN_INSTALLDIR;

    public static String DEST_PATH;

    public static String APP_NAME;

    public static String APP_VERSION;

    public static String JNI_DLL_FILE;

    public static String LINK_SECTION_NAME;

    public static String LINK_SECTION_ICON;

    public static String LINK_ENTRY_NAME;

    public static String LINK_ENTRY_ICON;

    public static boolean CREATE_UNINSTALL_SHORTCUT;

    /**
   *  Supported languages
   */
    public static String[][] languages = { { "danish", "Danish", "da", "DK" }, { "german", "Deutsch", "de", "DE" }, { "english", "English", "en", "UK" }, { "french", "Fran�ais", "fr", "FR" }, { "italian", "Italian", "it", "IT" }, { "japanese", "Japanese", "ja", "JP" } };

    private static String[] allSupportedLanguages;

    private static String[] supportedLanguages;

    /**
   *  The locale that is used now
   */
    private static Locale locale;

    /**
   *  The language that is used now
   */
    private static String currentLanguage;

    /**
   *  Cached resources
   */
    private static Hashtable resourceList = null;

    public static void printDebug(String msg) {
        if (DEBUG) System.err.println("DEBUG: " + msg);
    }

    public static String i18n(String key) {
        String s = null;
        try {
            s = getResource("com.memoire.vainstall.Language", key);
        } catch (MissingResourceException e) {
        }
        if (s == null) try {
            s = getResource("com.memoire.vainstall.gui.Language", key);
        } catch (MissingResourceException e) {
        }
        if (s == null) try {
            s = getResource("com.memoire.vainstall.xui.Language", key);
        } catch (MissingResourceException e) {
        }
        if (s == null) try {
            s = getResource("com.memoire.vainstall.tui.Language", key);
        } catch (MissingResourceException e) {
        }
        if (s == null) s = key;
        return s;
    }

    public static String i18n(String key, Object[] params) {
        String s = i18n(key);
        if (s != null && params != null) {
            s = MessageFormat.format(s, params);
        }
        return s;
    }

    /**
   *  Get a String resource from a resourcebundle.
   *  @param baseName String The name of the resource class ex. 'com.memorie.vainstall.Language'
   *  @param key String The keyword to find
   *  @return String
   */
    public static String getResource(String baseName, String key) throws MissingResourceException {
        if ((locale == null) || DEBUG) {
            locale = new Locale("en", "UK");
        }
        if (resourceList == null) {
            resourceList = new java.util.Hashtable();
        }
        if (resourceList.contains(baseName) == false) {
            ResourceBundle resource = ResourceBundle.getBundle(baseName, locale);
            resourceList.put(baseName, resource);
            return resource.getString(key);
        }
        return ((ResourceBundle) resourceList.get(baseName)).getString(key);
    }

    /**
   *  Get a 'int' resource from a resourcebundle.
   *  @param baseName String The name of the resource class ex. 'com.memorie.vainstall.Language'
   *  @param key String The keyword to find
   *  @return int
   */
    public static int getResourceInt(String baseName, String key) throws java.util.MissingResourceException {
        if (locale == null) {
            locale = new Locale("en", "UK");
        }
        if (resourceList == null) {
            resourceList = new java.util.Hashtable();
        }
        if (resourceList.contains(baseName) == false) {
            ResourceBundle resource = ResourceBundle.getBundle(baseName, locale);
            resourceList.put(baseName, resource);
            Integer value = (Integer) resource.getObject(key);
            return value.intValue();
        }
        Integer value = (Integer) ((ResourceBundle) resourceList.get(baseName)).getObject(key);
        return value.intValue();
    }

    public static void copyStream(InputStream _in, OutputStream _out) throws IOException {
        copyStream(_in, _out, null);
    }

    public static void copyStream(InputStream _in, OutputStream _out, byte[] _buffer) throws IOException {
        int read = 0;
        byte[] buf = _buffer == null ? new byte[32768] : _buffer;
        final int bufSize = buf.length;
        while ((read = _in.read(buf, 0, bufSize)) >= 0) {
            if (read == 0) {
                Thread.yield();
            } else {
                _out.write(buf, 0, read);
            }
        }
        _out.flush();
    }

    /**
   *  Set the language as defined in the documentation.
   *  We translate the name to a 'controlled' Java Locale.
   *  @param language String The language as defined in the documentation
   *                         english, french, danish etc.
   */
    public static void setLanguage(String language) {
        if (resourceList != null) {
            resourceList.clear();
        }
        if (language.toLowerCase().indexOf("choose") != -1) {
            if (language.equalsIgnoreCase("choose") == true) {
                setLanguage("default");
                return;
            }
            Vector languageList = new Vector();
            Locale l = Locale.getDefault();
            for (int i = 0; i < languages.length; i++) {
                if (l.getLanguage().equals(languages[i][2]) == true) {
                    currentLanguage = languages[i][0];
                }
            }
            StringTokenizer nizer = new StringTokenizer(language, ",");
            while (nizer.hasMoreTokens() == true) {
                String element = nizer.nextToken();
                element = element.trim().toLowerCase();
                for (int i = 0; i < languages.length; i++) {
                    if (element.equals(languages[i][0]) == true) {
                        if (element.equals(currentLanguage) == true) {
                            languageList.insertElementAt(languages[i][0], 0);
                        } else {
                            languageList.addElement(languages[i][0]);
                        }
                    }
                }
            }
            supportedLanguages = new String[languageList.size()];
            for (int i = 0; i < languageList.size(); i++) {
                for (int j = 0; j < languages.length; j++) {
                    if (languageList.elementAt(i).equals(languages[j][0]) == true) {
                        supportedLanguages[i] = languages[j][1];
                    }
                }
            }
            setLanguage("default");
            return;
        }
        if (language.equals("default") == true) {
            Locale l = Locale.getDefault();
            String shortLanguage = l.getLanguage();
            for (int i = 0; i < languages.length; i++) {
                if (shortLanguage.equals(languages[i][2]) == true) {
                    currentLanguage = languages[i][0];
                    locale = new Locale(languages[i][2], languages[i][3]);
                }
            }
        } else {
            for (int i = 0; i < languages.length; i++) {
                if (language.equals(languages[i][0]) == true) {
                    currentLanguage = languages[i][0];
                    locale = new Locale(languages[i][2], languages[i][3]);
                }
            }
        }
        UIManager.put("OptionPane.yesButtonText", getResource("com.memoire.vainstall.Language", "Common_OptionPane.yesButtonText"));
        UIManager.put("OptionPane.noButtonText", getResource("com.memoire.vainstall.Language", "Common_OptionPane.noButtonText"));
        UIManager.put("OptionPane.okButtonText", getResource("com.memoire.vainstall.Language", "Common_OptionPane.okButtonText"));
        UIManager.put("OptionPane.cancelButtonText", getResource("com.memoire.vainstall.Language", "Common_OptionPane.cancelButtonText"));
    }

    public static String[] getSupportedLanguages() {
        if (supportedLanguages == null) {
            supportedLanguages = new String[languages.length];
            for (int i = 0; i < languages.length; i++) {
                supportedLanguages[i] = languages[i][1];
            }
        }
        return supportedLanguages;
    }

    public static String[] getAllSupportedLanguages() {
        if (allSupportedLanguages == null) {
            allSupportedLanguages = new String[languages.length];
            for (int i = 0; i < languages.length; i++) {
                allSupportedLanguages[i] = languages[i][1];
            }
        }
        return allSupportedLanguages;
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
   * Return an index that corresponds to the value of getSupportedLanguages
   * or getAllSupportedLanguages that matches the default locale.
   * If an exact match for the locale is not present,the value returned is
   * the first language in the list that matches the language of the default
   * locale.
   *
   */
    public static int suggestLanguage() {
        Locale syslocale = Locale.getDefault();
        String syslang = syslocale.getLanguage();
        String syscountry = syslocale.getCountry();
        int langonly = -1;
        for (int idx = 0; idx < languages.length; idx++) {
            if (syslang.equals(languages[idx][2])) {
                if (syscountry.equals(languages[idx][3])) {
                    return idx;
                }
                if (langonly == -1) langonly = idx;
            }
        }
        if (langonly == -1) langonly = 0;
        return langonly;
    }
}
