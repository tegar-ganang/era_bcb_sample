package netgest.bo.xwc.framework.localization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import netgest.bo.runtime.EboContext;
import netgest.bo.system.boApplication;
import netgest.bo.system.boSessionUser;

public class XUIMessagesLocalization {

    private static UTF8Control control = new UTF8Control();

    public static String getApplicationLanguage() {
        boApplication bo = boApplication.currentContext().getApplication();
        String ret = bo.getApplicationLanguage();
        return ret;
    }

    public static String getUserLanguage() {
        String ret;
        ret = getApplicationLanguage();
        try {
            if (boApplication.currentContext() != null) {
                EboContext ctx = boApplication.currentContext().getEboContext();
                if (ctx != null) {
                    boSessionUser boUser = ctx.getSysUser();
                    if (boUser != null) {
                        if (boUser.getLanguage() != null && boUser.getLanguage() != "") {
                            ret = boUser.getLanguage();
                        }
                    }
                }
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String getMessage(String lang, Locale local, String bundle, String key, Object... args) {
        Locale language;
        if (lang != null && lang != "") {
            if (lang.charAt(2) == '_') {
                String s1 = lang.substring(0, 2);
                String s2 = lang.substring(3, 5);
                language = new Locale(s1, s2);
            } else {
                language = new Locale(lang);
            }
            return getMessage(language, bundle, key, args);
        }
        return getMessage(local, bundle, key, args);
    }

    static ThreadLocal<Locale> threadLocal = new ThreadLocal<Locale>() {

        protected Locale initialValue() {
            return Locale.getDefault();
        }

        ;
    };

    private static final Hashtable<Locale, Hashtable<String, ResourceBundle>> resourceBundles = new Hashtable<Locale, Hashtable<String, ResourceBundle>>();

    public static String getMessage(Locale locale, String bundle, String key) {
        return getMessage(locale, bundle, key, (Object[]) null);
    }

    public static String getMessage(Locale locale, String bundle, String key, Object... args) {
        String lang = locale.getLanguage();
        if (!lang.equalsIgnoreCase(getUserLanguage())) {
            locale = new Locale(getUserLanguage());
        }
        lang = locale.getLanguage();
        if (lang.length() > 2) if (lang.charAt(2) == '_') {
            String string1 = lang.substring(0, 2);
            String string2 = lang.substring(3, 5);
            locale = new Locale(string1, string2);
        }
        String localizedMessage = null;
        Hashtable<String, ResourceBundle> localeBundles;
        ResourceBundle resourceBundle;
        try {
            resourceBundle = null;
            localeBundles = resourceBundles.get(locale);
            if (localeBundles != null) {
                resourceBundle = localeBundles.get(bundle);
            }
            if (resourceBundle == null) {
                resourceBundle = ResourceBundle.getBundle(bundle, locale, control);
                if (resourceBundle != null) {
                    if (localeBundles == null) {
                        localeBundles = new Hashtable<String, ResourceBundle>();
                        resourceBundles.put(locale, localeBundles);
                    }
                    localeBundles.put(bundle, resourceBundle);
                }
            }
            if (resourceBundle != null) {
                localizedMessage = resourceBundle.getString(key);
                if (args != null && args.length > 0) {
                    Formatter formatter = new Formatter();
                    formatter.format(localizedMessage, args);
                    localizedMessage = formatter.toString();
                }
            }
        } catch (java.util.MissingResourceException e) {
        }
        if (localizedMessage == null) {
            localizedMessage = bundle + "_" + locale.getLanguage() + "[" + key + "]";
            if (args != null && args.length > 0) {
                boolean first = true;
                localizedMessage += " (";
                for (Object arg : args) {
                    if (!first) localizedMessage += ", ";
                    first = false;
                    localizedMessage += arg;
                }
                localizedMessage += ")";
            }
        }
        return localizedMessage;
    }

    public static String getMessage(String bundle, String id) {
        return getMessage(getThreadCurrentLocale(), bundle, id, (Object[]) null);
    }

    public static String getMessage(String bundle, String id, Object... args) {
        return getMessage(getThreadCurrentLocale(), bundle, id, args);
    }

    public static void setThreadCurrentLocale(Locale local) {
        threadLocal.set(local);
    }

    public static Locale getThreadCurrentLocale() {
        return threadLocal.get();
    }

    public static class UTF8Control extends Control {

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
