package net.sourceforge.keepassj2me;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * Localized resources
 * @author unknown
 * @author Stepan Strelets
 */
public class L10n {

    private static final String DEFAULT_LOCALE = "en-US";

    private Hashtable values;

    private String locale;

    private static L10n instance;

    private static Hashtable locales;

    /**
	 * Gets an instance of L10nResources to access resources with
	 * the specified locale.
	 *
	 * @param locale name of locale.
	 * @return L10nResources instance.
	 */
    public static L10n getL10n(String locale) {
        if (instance == null) instance = new L10n(locale); else if (!instance.locale.equals(locale)) instance.setLocale(locale);
        return instance;
    }

    /**
	 * Creates an instance of L10nResources to access resources
	 * with the specified locale.
	 *
	 * @param locale name of locale.
	 * //@return L10nResources instance.
	 */
    private L10n(String locale) {
        this.setLocale(locale);
    }

    /**
	 * Sets the locale of the L10nResources instance.
	 *
	 * @param locale name of the locale.
	 */
    public void setLocale(String locale) {
        if (locale == null || locale.length() == 0) {
            this.locale = System.getProperty("microedition.locale");
            if (this.locale == null) {
                this.locale = L10n.DEFAULT_LOCALE;
            }
        } else {
            this.locale = locale;
        }
        this.values = loadProperties("/langs/" + this.locale);
    }

    /**
	 * Get locales
	 * @return hash with locales id's and names
	 */
    public static Hashtable getLocales() {
        if (locales == null) locales = loadProperties("/langs/ls");
        return locales;
    }

    /**
	 * Gets the value for the specified key. For every parameter
	 * on the params argument there must be an entry in the key
	 * value in the format {x} where x is the index of the value
	 * to be replaced on the formated string.
	 *
	 * @param key resource key.
	 * @param params parameters to be formated.
	 * @return the formated value.
	 */
    public String getString(String key, String[] params) {
        if (this.values != null) {
            String result = (String) this.values.get(key);
            if (result != null) {
                StringBuffer buffer = new StringBuffer(result);
                for (int i = 0; i < params.length; i++) {
                    int index = result.indexOf("{" + i + "}");
                    if (index != -1) {
                        buffer.delete(index, index + String.valueOf(i).length() + 2);
                        buffer.insert(index, params[i]);
                        result = buffer.toString();
                        buffer.delete(0, buffer.length());
                        buffer.append(result);
                    } else {
                        buffer.append(" /i" + i + "=" + params[i]);
                    }
                }
                return result;
            }
        }
        return key;
    }

    /**
	 * Gets the value for the specified key.
	 *
	 * @param key resource key.
	 * @return String values associated to the
	 * key.
	 */
    public String getString(String key) {
        if (this.values != null) {
            String result = (String) this.values.get(key);
            if (result != null) {
                return result;
            }
        }
        return key;
    }

    /**
	 * Gets the binary data associated to the path
	 * set as value of the specified key.
	 *
	 * @param key resource key.
	 * @return the bytes of the binary data or null
	 * if key has no associated value.
	 * @throws IOException - If any error occurs.
	 */
    public byte[] getData(String key) {
        byte[] result = null;
        String value = getString(key);
        try {
            if (value != null) {
                InputStream stream = this.getClass().getResourceAsStream(value);
                if (stream != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    try {
                        byte[] chunk = new byte[100];
                        int read = -1;
                        do {
                            read = stream.read(chunk);
                            if (read > 0) {
                                buffer.write(chunk, 0, read);
                            }
                        } while (read != -1);
                    } finally {
                        stream.close();
                    }
                    result = buffer.toByteArray();
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    private static Hashtable loadProperties(String path) {
        Hashtable values = new Hashtable();
        InputStream is = values.getClass().getResourceAsStream(path);
        if (is != null) {
            try {
                byte buf[] = new byte[is.available()];
                is.read(buf);
                is.close();
                String res = new String(buf, "UTF-8");
                String pair;
                String key;
                String value;
                do {
                    int i = res.indexOf("\n");
                    if (i > 0) {
                        pair = res.substring(0, i).trim();
                        if ((i + 1) < res.length()) res = res.substring(i + 1); else res = "";
                    } else {
                        pair = res.trim();
                        res = "";
                    }
                    if (pair.length() > 0) {
                        i = pair.indexOf("=");
                        if (i > 0) {
                            key = pair.substring(0, i);
                            if ((i + 1) < pair.length()) value = pair.substring(i + 1); else value = "";
                            values.put(key, value);
                        }
                    }
                } while (res.length() > 0);
            } catch (IOException e) {
            }
        }
        return values;
    }
}
