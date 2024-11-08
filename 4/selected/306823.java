package it.freax.fpm.util;

import it.freax.fpm.util.exceptions.ConfigurationReadException;
import it.freax.fpm.util.exceptions.ExtensionDecodingException;
import java.io.*;
import java.util.Properties;
import java.util.Scanner;
import org.apache.commons.io.IOUtils;

public final class Constants {

    /**
	 * Home directory dell'utente.<br/>
	 * In *nix è /home/USER/ In Windows è C:\Users\USER\ (Vista, 2k8, 7) o
	 * C:\Documents and Settings\USER\ (2k, XP, 2k3)
	 */
    public static final String USER_HOME = System.getProperty("user.home");

    /**
	 * Directory di esecuzione del programma java, la jvm risolve il percorso
	 * dei class file o del jar file che sta eseguendo e torna la directory root
	 * che li contiene.<br/>
	 * Es.: java -jar /home/joe/fpm/fpmMain.jar -> /home/joe/fpm
	 */
    public static final String USER_DIR = System.getProperty("user.dir");

    /**
	 * Nome del sistema operativo in uso.<br/>
	 * Windows 7, Windows XP, Linux, Solaris, Mac OS X ...
	 */
    public static final String OS_NAME = System.getProperty("os.name");

    /**
	 * Separatore di file (e directory), In Windows '\', in *nix '/'.
	 */
    public static final String FS = System.getProperty("file.separator");

    /**
	 * Separatore di linea, In Windows '\r\n', in *nix '\n' in MacOS '\r'.
	 */
    public static final String LS = System.getProperty("line.separator");

    /**
	 * Nazionalità (e linguaggio) dell'utente corrente
	 */
    public static final String COUNTRY = System.getProperty("user.country");

    /**
	 * Nome del file di configurazione principale di fpm.
	 */
    public static final String MAIN_CONF_FILE = "fpm.conf";

    /**
	 * Nome del file di log principale di fpm.
	 */
    public static final String MAIN_LOG_FILE = "fpm.log";

    /**
	 * Pattern di default per il logger di fpm.
	 */
    public static final String DEFAULT_LOG_PATTERN = "%-8r [%t] %-5p %c - %m%n";

    /**
	 * Nome della directory contenente i file di configurazione.
	 */
    public static final String CONF_DIR = "conf";

    /**
	 * Nome della directory delle grammatiche di ANTLR
	 */
    public static final String GRAMMARS_DIR = "grammars";

    /**
	 * Nome della directory dei file fisici di fpm (configurazione, log, ...).
	 */
    public static final String FPM_DIR = "fpm";

    /**
	 * Tag di sostituzione che sta a indicare un percorso relativo (interno al
	 * jar che contiene il software eseguito).
	 */
    public static final String REL_P = "${REL}";

    /**
	 * Tag di sostituzione che sta a indicare un paese (o una lingua) per il
	 * sistema di internazionalizzazione.
	 */
    public static final String COUNTRY_P = "${COUNTRY}";

    /**
	 * Tag di sostituzione che sta a indicare il percorso precedente nella lista
	 * di percorsi.
	 */
    public static final String PREV_P = "${PREV}";

    /**
	 * Tag di sostituzione che sta a indicare il percorso della directory di
	 * output per i file generati dalle grammatiche ANTLR
	 */
    public static final String ANTLR_OUT_P = "${ANTLR-Output}";

    /**
	 * Separatore per la lista di percorsi da utilizzare per la ricerca del
	 * MAIN_CONF_FILE.
	 */
    public static final String SEP_P = ":";

    /**
	 * Segnaposto per la directory home dell'utente corrente.
	 */
    public static final String HOME_P = "~";

    /**
	 * Package per le classi generate dal motore ANTLR
	 */
    public static final String ENGINE_PACKAGE = "it.freax.fpm.core.solver.parsers.";

    /**
	 * Delimitatore per la variabile Entry Point all'interno dei file di
	 * grammatica.<br>
	 * Sitassi aggiunta per fpm.<br>
	 * Il delimitatore viene usato nel modo:
	 * {@literal @@EP::ENTRY_POINT[reverse(@@EP::)]}<br>
	 * Esempio:<br>
	 * {@literal @@EP::compilationUnit::PE@@}
	 * 
	 * @author kLeZ-hAcK<br>
	 */
    public static final String ENTRY_POINT_DEL = "@@EP::";

    /**
	 * Lista di percorsi da utilizzare per la ricerca del MAIN_CONF_FILE.<br/>
	 * In maniera gerarchica si trova il percorso di sistema o il percorso
	 * relativo (una risorsa all'interno del jar di fpm, reset to default).<br/>
	 * Se in presenza di un reset to default, il file di configurazione va
	 * scritto sul disco nella directory di sistema.
	 */
    public static final String CONF_PATHS = getSystemConfDir() + SEP_P + REL_P + FS + CONF_DIR + FS;

    private static Constants singleton = null;

    private static boolean hasLoaded = false;

    private String confPath;

    private Properties fpmConf;

    private Properties localizedStrings;

    private static String getSystemConfDir() {
        String sysConf = "";
        if (OS_NAME.toLowerCase().contains("windows")) {
            sysConf = (!USER_DIR.endsWith(FS) ? USER_DIR + FS : USER_DIR) + CONF_DIR + FS;
        } else {
            sysConf = FS + "etc" + FS + FPM_DIR + FS;
            if (!new File(sysConf).canWrite()) {
                sysConf = USER_HOME + FS + "." + FPM_DIR + FS + CONF_DIR + FS;
            }
        }
        return sysConf;
    }

    private static MapEntry<String, Properties> loadFpmConf() throws ConfigurationReadException {
        MapEntry<String, Properties> ret = null;
        Scanner sc = new Scanner(CONF_PATHS).useDelimiter(SEP_P);
        String prev = "";
        while (sc.hasNext() && !hasLoaded) {
            Properties fpmConf = null;
            boolean relative = false;
            String path = sc.next();
            if (path.startsWith(PREV_P)) {
                path = path.replace(PREV_P, prev.substring(0, prev.length() - 1));
            } else if (path.startsWith(REL_P)) {
                path = path.replace(REL_P + FS, "");
                relative = true;
            } else if (path.contains(HOME_P)) {
                path = path.replace(HOME_P, USER_HOME);
            }
            prev = path;
            path = path.concat(MAIN_CONF_FILE);
            try {
                InputStream is = null;
                if (relative) {
                    is = ClassLoader.getSystemResourceAsStream(path);
                    path = getSystemConfDir();
                    Strings.getOne().createPath(path);
                    path += MAIN_CONF_FILE;
                    FileOutputStream os = new FileOutputStream(path);
                    IOUtils.copy(is, os);
                    os.flush();
                    os.close();
                    os = null;
                } else {
                    is = new FileInputStream(path);
                }
                fpmConf = new Properties();
                fpmConf.load(is);
                if (fpmConf.isEmpty()) {
                    throw new ConfigurationReadException();
                }
                ret = new MapEntry<String, Properties>(path, fpmConf);
                hasLoaded = true;
            } catch (FileNotFoundException e) {
                fpmConf = null;
                singleton = null;
                hasLoaded = false;
            } catch (IOException e) {
                throw new ConfigurationReadException();
            }
        }
        return ret;
    }

    private static Properties loadLocalizedStrings(String path, String pattern) throws ExtensionDecodingException {
        Properties ret = null;
        Strings s = Strings.getOne();
        InputStream is = null;
        String locRes = pattern.replace(COUNTRY_P, COUNTRY);
        String fullpath = s.concatPaths(path, locRes);
        try {
            if (s.isRelativePath(path)) {
                is = ClassLoader.getSystemResourceAsStream(fullpath);
            } else {
                is = new FileInputStream(fullpath);
            }
            ret = new Properties();
            ret.load(is);
        } catch (IOException e) {
            is = ClassLoader.getSystemResourceAsStream(CONF_DIR + FS + pattern.replace(COUNTRY_P, "default"));
            ret = new Properties();
            try {
                ret.load(is);
            } catch (Throwable t) {
            }
        }
        return ret;
    }

    public static Constants getOne() throws ConfigurationReadException {
        if (!hasLoaded && (singleton == null)) {
            MapEntry<String, Properties> entry = loadFpmConf();
            String locFilesPath = entry.getValue().getProperty("localized.files.path");
            String locFilesPattern = entry.getValue().getProperty("localized.files.pattern");
            Properties localizedStrings;
            try {
                localizedStrings = loadLocalizedStrings(locFilesPath, locFilesPattern);
            } catch (ExtensionDecodingException e) {
                throw new ConfigurationReadException((Exception) e.fillInStackTrace());
            }
            singleton = new Constants();
            singleton.setDefaultConfPath(entry.getKey());
            singleton.setFpmConf(entry.getValue());
            singleton.setLocalizedStrings(localizedStrings);
        }
        return singleton;
    }

    public static Constants getOneReset() throws ConfigurationReadException {
        hasLoaded = false;
        singleton = null;
        return getOne();
    }

    private void setDefaultConfPath(String confPath) {
        this.confPath = confPath;
    }

    public String getDefaultConfPath() {
        return confPath;
    }

    private void setFpmConf(Properties fpmConf) {
        this.fpmConf = fpmConf;
    }

    public Properties getFpmConf() {
        return fpmConf;
    }

    public String getDefaultFpmPath() {
        return getDefaultConfPath().replace(CONF_DIR, "").replace(FS + FS, FS);
    }

    public String getConstant(String name) {
        return getFpmConf().getProperty(name);
    }

    public String getConstant(String name, String defaultProp) {
        return getProperty(getFpmConf(), name, defaultProp);
    }

    private void setLocalizedStrings(Properties localizedStrings) {
        this.localizedStrings = localizedStrings;
    }

    public Properties getLocalizedStrings() {
        return localizedStrings;
    }

    public String getLocalizedString(String name) {
        return getLocalizedStrings().getProperty(name);
    }

    public String getLocalizedString(String name, String defaultProp) {
        return getProperty(getLocalizedStrings(), name, defaultProp);
    }

    public static String getProperty(Properties props, String defaultProp, String propName) {
        String ret = defaultProp;
        if (props.containsKey(propName)) {
            ret = props.getProperty(propName);
        }
        return ret;
    }
}
