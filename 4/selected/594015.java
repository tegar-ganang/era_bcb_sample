package net.genaud.vicaya.launch;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertyReader {

    private static Log log = LogFactory.getLog(PropertyReader.class);

    private Properties systemProp = null;

    private Properties readbaseConfigProp = null;

    private Properties writebaseConfigProp = null;

    private ConfigBean configBean = null;

    private String readbasePath = null;

    private String writebasePath = null;

    private String tomcatPath = null;

    private String userdir = null;

    private String userhome = null;

    private String slash = null;

    public PropertyReader() {
        configBean = new ConfigBean();
        systemProp = System.getProperties();
        userdir = systemProp.getProperty("user.dir");
        userhome = systemProp.getProperty("user.home");
        slash = systemProp.getProperty("file.separator");
    }

    public void init() {
        initBasePaths();
        initConfigBean();
    }

    public ConfigBean getConfigPropertyBean() {
        return configBean;
    }

    /**
	 * These three directories, tomcat, readbase, and writebase
	 * contain all the configuration files. We can set any
	 * or all of these paths from the command line:
	 * 
	 *   example:   -Dtomcat=/path/to/tomcat
	 * 
	 * If readbase is not set it will be assumed based userdir/vicaya
	 * The other two can be set in the readbase/config.properties
	 * 
	 * This method makes prioritiezed guesses at the values,
	 * and checks their validity (does the directory exist
	 * with correct permissions, etc) otherwise throws
	 * a runtime exception.
	 */
    private void initBasePaths() {
        readbasePath = pathRelativeToAbsolute(systemProp.getProperty("readbase"), userdir);
        if (readbasePath != null && !isFileValid(readbasePath, true, false, false, true)) {
            throw new RuntimeException("System.property.readbase is invalid (" + readbasePath + ")");
        }
        if (readbasePath == null) {
            readbasePath = userdir + slash + "vicaya";
            if (!isFileValid(readbasePath, true, false, false, true)) {
                readbasePath = null;
            }
        }
        if (readbasePath == null) {
            readbasePath = userdir;
            if (!isFileValid(readbasePath, true, false, false, true)) {
                readbasePath = null;
            }
        }
        if (readbasePath == null) {
            throw new RuntimeException("Can not locate a suitable readbase directory");
        }
        readbaseConfigProp = loadProperties(readbasePath + slash + "config.properties");
        writebasePath = pathRelativeToAbsolute(systemProp.getProperty("writebase"), userdir);
        if (writebasePath != null && !isFileValid(writebasePath, true, true, false, true)) {
            throw new RuntimeException("System.property.writebase is invalid (" + writebasePath + ")");
        }
        if (writebasePath == null) {
            writebasePath = pathRelativeToAbsolute(readbaseConfigProp.getProperty("writebase"), readbasePath);
            if (!isFileValid(writebasePath, true, true, false, true)) {
                writebasePath = null;
            }
        }
        if (writebasePath == null) {
            writebasePath = userhome + slash + ".vicaya";
            if (!isFileValid(writebasePath, true, true, false, true)) {
                writebasePath = null;
            }
        }
        if (writebasePath == null) {
            throw new RuntimeException("Can not locate a suitable writebase directory");
        }
        writebaseConfigProp = loadProperties(writebasePath + slash + "config.properties");
    }

    /**
	 * init all properties except home
	 *
	 */
    private void initConfigBean() {
        tomcatPath = getConfigProperty("tomcat", readbasePath + slash + "tomcat");
        String logFile = getConfigProperty("log", writebasePath + slash + "logs" + slash + "serverlog.txt");
        String apps = getConfigProperty("apps", writebasePath + slash + "webapps");
        String splashFile = getConfigProperty("splash", readbasePath + slash + "images" + slash + "splash.png");
        String middleIconFile = getConfigProperty("middleicon", readbasePath + slash + "images" + slash + "middleicon.gif");
        String trayIconFile = getConfigProperty("trayicon", readbasePath + slash + "images" + slash + "trayicon.gif");
        boolean verbose = false;
        String strVerbose = getConfigProperty("verbose", "false");
        if (!"false".equalsIgnoreCase(strVerbose)) {
            verbose = true;
        }
        boolean lockconfig = false;
        String strLockconfig = getConfigProperty("lockconfig", "false");
        if (!"false".equalsIgnoreCase(strLockconfig)) {
            lockconfig = true;
        }
        String strRunPort = getConfigProperty("runport", "8108");
        int runport = 8108;
        try {
            runport = Integer.parseInt(strRunPort);
        } catch (Exception e) {
            runport = 8108;
        }
        String strEndPort = getConfigProperty("endport", "8109");
        int endport = 8109;
        try {
            endport = Integer.parseInt(strEndPort);
        } catch (Exception e) {
            endport = 8109;
        }
        String startPage = getConfigProperty("startpage", "http://localhost:" + runport + "/index.html");
        String title = getConfigProperty("title", "Vicaya");
        String tooltip = getConfigProperty("tooltip", "Vicaya Tooltip");
        PropertyReader.replace(tomcatPath, "{catalina.home}", "");
        PropertyReader.replace(tomcatPath, "{tomcat}", "");
        Vector subPairs = generatePairs();
        configBean.setVerbose(verbose);
        configBean.setLockconfig(lockconfig);
        configBean.setReadbase(readbasePath);
        configBean.setWritebase(writebasePath);
        configBean.setTomcat(pathSubstitution(tomcatPath, subPairs));
        System.out.println("\n\n\n tomcat: " + configBean.getTomcat() + " \n\n\n");
        configBean.setApps(pathSubstitution(apps, subPairs));
        configBean.setLogFile(pathSubstitution(logFile, subPairs));
        configBean.setMiddleIconFile(pathSubstitution(middleIconFile, subPairs));
        configBean.setSplashFile(pathSubstitution(splashFile, subPairs));
        configBean.setTrayIconFile(pathSubstitution(trayIconFile, subPairs));
        configBean.setStartPage(startPage);
        configBean.setTitle(title);
        configBean.setTooltip(tooltip);
        configBean.setEndport(endport);
        configBean.setRunport(runport);
    }

    static String replace(String input, String s, String r) {
        String[] sr = { s, r };
        return PropertyReader.replace(input, sr);
    }

    static String replace(String input, String[] searchPair) {
        if (input == null) return null;
        for (int in = input.indexOf(searchPair[0]); in >= 0; in = input.indexOf(searchPair[0])) {
            input = input.substring(0, in) + searchPair[1] + input.substring(in + searchPair[0].length());
        }
        return input;
    }

    /**
     * helper method. Check to see if a file exists,
     * if it has acceptable r/w permissions,
     * and/or is a file, directory, or not (or both?)
     * 
     * @param path
     * @param mustBeReadable
     * @param mustBeWriteable
     * @param mustBeFile
     * @param mustBeDirectory
     * @return
     */
    static boolean isFileValid(String path, boolean mustBeReadable, boolean mustBeWriteable, boolean mustBeFile, boolean mustBeDirectory) {
        if (path == null) return false;
        File f = new File(path);
        if (!f.exists()) return false; else if (mustBeReadable && !f.canRead()) return false; else if (mustBeWriteable && !f.canWrite()) return false; else if (mustBeFile && !f.isFile()) return false; else if (mustBeDirectory && !f.isDirectory()) return false; else return true;
    }

    static String pathRelativeToAbsolute(String inPath, String basePath) {
        if (inPath == null) return null; else if (isAbsolutePath(inPath)) return inPath; else if (!isAbsolutePath(basePath)) return null; else if (inPath.length() <= 0) return basePath; else if (inPath.equals(".")) return basePath;
        if (basePath.endsWith("/") || basePath.endsWith("\\")) {
            return basePath + inPath;
        }
        return basePath + File.separator + inPath;
    }

    static void addPairToVector(Vector v, String key, String val) {
        String[] s = { key, val };
        v.add(s);
    }

    private Vector generatePairs() {
        Vector pairs = new Vector();
        addPairToVector(pairs, "{user.dir}", userdir);
        addPairToVector(pairs, "{user.home}", userhome);
        addPairToVector(pairs, "{vicaya}", readbasePath);
        addPairToVector(pairs, "{catalina.home}", tomcatPath);
        addPairToVector(pairs, "{catalina.base}", writebasePath);
        return pairs;
    }

    static String pathSubstitution(String inPath, Vector pairs) {
        String bak = inPath + "";
        String[] sr;
        for (int i = 0; i < pairs.size(); i++) {
            inPath = PropertyReader.replace(inPath, (String[]) pairs.get(i));
        }
        if (!bak.equals(inPath)) System.out.println(bak + " -----> " + inPath);
        return inPath;
    }

    static boolean isAbsolutePath(String path) {
        if (path == null || path.length() <= 0) return false; else if (path.substring(0, 1).equals("/")) return true; else if (path.length() > 1 && path.matches("[a-zA-Z]:.*")) return true; else if (path.length() > 4 && path.substring(0, 5).equalsIgnoreCase("file:")) return true;
        return false;
    }

    /**
     * returns a value based on key against various resources with the following priority
     * 1. system value  : -Dkey="value"
     * 2. writebase value : user.home/.vicaya/config.properties
     * 3. readbase value  : CDROM/vicaya/config.properties
     * 4. defaultVal : second argument 
     */
    private String getConfigProperty(String propKey, String defaultVal) {
        if (propKey == null) return null;
        String systemVal = systemProp.getProperty(propKey);
        String writebaseVal = writebaseConfigProp.getProperty(propKey);
        String readbaseVal = readbaseConfigProp.getProperty(propKey);
        if (systemVal != null) {
            return systemVal;
        } else if (writebaseVal != null) {
            return writebaseVal;
        } else if (readbaseVal != null) {
            return readbaseVal;
        } else {
            return defaultVal;
        }
    }

    static Properties loadProperties(String propFilePath) {
        Properties props = new Properties();
        if (propFilePath == null) return props;
        if (!propFilePath.endsWith(".properties")) propFilePath += ".properties";
        try {
            loadPropertiesFromFile(props, propFilePath);
        } catch (FileNotFoundException fnfe) {
            log.error("Could not find property file '" + propFilePath + "' : " + fnfe.getMessage(), fnfe);
        } catch (Exception e) {
            log.error("Error while reading property file '" + propFilePath + "' : " + e.getMessage(), e);
        }
        return props;
    }

    static String configToString(ConfigBean config, boolean commented) {
        StringBuffer s = new StringBuffer();
        String comment = "# ";
        if (!commented) {
            comment = "";
        }
        s.append("# Vicaya config.properties\n");
        s.append("#\n");
        s.append("# readbase (the data typically a readonly media) and\n");
        s.append("# writebase (the configuration runtime on writable media)\n");
        s.append("# can not be configured without contradiction. All other\n");
        s.append("# properties can be modified with the following priority:\n");
        s.append("# writebase/config.properties\n");
        s.append("# readbase/config.properties\n");
        s.append("# default value (somewhat interdependent)\n");
        s.append("#\n");
        s.append("# last use: readbase  = " + config.getReadbase() + "\n");
        s.append("# last use: writebase = " + config.getWritebase() + "\n");
        s.append("#\n");
        s.append("\n");
        s.append(comment + "lockconfig = " + config.isLockconfig() + "\n");
        s.append(comment + "verbose    = " + config.isVerbose() + "\n");
        s.append(comment + "tomcat     = " + config.getTomcat() + "\n");
        s.append(comment + "apps       = " + config.getApps() + "\n");
        s.append(comment + "startpage  = " + config.getStartPage() + "\n");
        s.append(comment + "runport    = " + config.getRunport() + "\n");
        s.append(comment + "endport    = " + config.getEndport() + "\n");
        s.append(comment + "log        = " + config.getLogFile() + "\n");
        s.append(comment + "middleicon = " + config.getMiddleIconFile() + "\n");
        s.append(comment + "splash     = " + config.getSplashFile() + "\n");
        s.append(comment + "title      = " + config.getTitle() + "\n");
        s.append(comment + "tooltip    = " + config.getTooltip() + "\n");
        s.append(comment + "trayicon   = " + config.getTrayIconFile() + "\n");
        return s.toString();
    }

    static void loadPropertiesFromFile(Properties props, String propFilePath) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(propFilePath));
        while (in.ready()) {
            String line = in.readLine();
            String[] split = line.split("=");
            if (split == null || split.length != 2) continue;
            String key = split[0].trim();
            String val = split[1].trim();
            props.setProperty(key, val);
        }
    }
}
