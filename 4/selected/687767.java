package ji;

import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;
import java.io.*;
import java.net.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */
public class JI {

    private static JI instance = null;

    private Hashtable languages = new Hashtable();

    private static String translationsFile = ants.p2p.filesharing.WarriorAnt.workingPath + "translation.ji";

    private static boolean learn = false;

    private static String currentLanguage = null;

    private static final String naiveLanguage = "English";

    private static String languageServer = "http://www.dargens.com/kerjodando/translation.ji";

    public static void setLearning(boolean learningEnabled) {
        learn = learningEnabled;
    }

    public static void setCurrentLanguage(String language) {
        currentLanguage = language;
        String current = naiveLanguage;
        if (currentLanguage != null) {
            current = currentLanguage;
        }
        if (getInstance().languages.get(current) == null) {
            getInstance().languages.put(current, new Hashtable());
            getInstance().storeTranslations();
        }
    }

    public static String[] getAvaiableLanguages() {
        String[] languageIndex = new String[getInstance().languages.size()];
        Enumeration languageNames = getInstance().languages.keys();
        int x = 0;
        while (languageNames.hasMoreElements()) {
            languageIndex[x++] = (String) languageNames.nextElement();
        }
        return languageIndex;
    }

    public static void setTranslationFile(String file) {
        if (file != null) {
            translationsFile = file;
            getInstance().loadTranslations();
        }
    }

    public static void setTranslation(String phrase, String translatedPhrase, String language) {
        String current = naiveLanguage;
        if (language != null) {
            current = language;
            if (getInstance().languages.get(current) == null) {
                getInstance().languages.put(current, new Hashtable());
                getInstance().storeTranslations();
            }
        }
        ((Hashtable) getInstance().languages.get(current)).put(phrase, translatedPhrase);
        if (learn) getInstance().storeTranslations();
    }

    public static ArrayList getNaivePhrases() {
        Enumeration phrases = ((Hashtable) getInstance().languages.get(naiveLanguage)).keys();
        ArrayList rv = new ArrayList();
        while (phrases.hasMoreElements()) {
            rv.add(phrases.nextElement());
        }
        return rv;
    }

    public static JI getInstance() {
        if (instance == null) return instance = new JI(); else return instance;
    }

    public static String i(String phrase) {
        try {
            String current = naiveLanguage;
            if (currentLanguage != null) {
                current = currentLanguage;
            }
            String naive = (String) ((Hashtable) getInstance().languages.get(naiveLanguage)).get(phrase);
            String translated = (String) ((Hashtable) getInstance().languages.get(current)).get(phrase);
            if (naive == null) {
                ((Hashtable) getInstance().languages.get(naiveLanguage)).put(phrase, phrase);
                if (learn) getInstance().storeTranslations();
            }
            if (translated == null) {
                ((Hashtable) getInstance().languages.get(current)).put(phrase, phrase);
                if (learn) getInstance().storeTranslations();
                translated = phrase;
            }
            return translated;
        } catch (Exception e) {
            getInstance().downloadTranslationsAndReload();
            return phrase;
        }
    }

    private JI() {
        this.loadTranslations();
    }

    public void downloadTranslationsAndReload() {
        File languages = new File(this.translationsFile);
        try {
            URL languageURL = new URL(languageServer);
            InputStream is = languageURL.openStream();
            OutputStream os = new FileOutputStream(languages);
            byte[] read = new byte[512000];
            int bytesRead = 0;
            do {
                bytesRead = is.read(read);
                if (bytesRead > 0) {
                    os.write(read, 0, bytesRead);
                }
            } while (bytesRead > 0);
            is.close();
            os.close();
            this.loadTranslations();
        } catch (Exception e) {
            System.err.println("Remote languages file not found!");
            if (languages.exists()) {
                try {
                    XMLDecoder loader = new XMLDecoder(new FileInputStream(languages));
                    this.languages = (Hashtable) loader.readObject();
                    loader.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.languages.put(naiveLanguage, new Hashtable());
                }
            } else this.languages.put(naiveLanguage, new Hashtable());
        }
    }

    private void loadTranslations() {
        File languages = new File(this.translationsFile);
        if (languages.exists()) {
            try {
                XMLDecoder loader = new XMLDecoder(new FileInputStream(languages));
                this.languages = (Hashtable) loader.readObject();
                loader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            downloadTranslationsAndReload();
        }
    }

    private void storeTranslations() {
        File languages = new File(this.translationsFile);
        try {
            XMLEncoder storer = new XMLEncoder(new FileOutputStream(languages));
            storer.writeObject(this.languages);
            storer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
