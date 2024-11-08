package be.ac.fundp.infonet.econf.installer;

import be.ac.fundp.infonet.econf.util.Utilities;
import java.io.*;
import java.util.*;

/**
 * Initializes eConf configuration
 * @version 0.1
 */
public class ConfigInitializator {

    /**
     * Temporary directory.
     */
    private String tmp_dir = null;

    /**
     * eConf home directory.
     */
    private String econf_dir = null;

    private static final String log4j_path = File.separator + "conf" + File.separator + "log4j-config.xml";

    private static final String config_path = File.separator + "conf" + File.separator + "econf-config.xml";

    private static final String ECONF_HOME = "@@ECONF_HOME@@";

    private static final String TEMP_DIR = "@@TEMP_PATH@@";

    /**
     * Inits the config initialization.
     * @param temporaryDir   the temp directory (without ending slashe)
     * @param eConfHome      the econf installation directory (without ending slashe)
     */
    public ConfigInitializator(String temporaryDir, String econfHome) {
        this.tmp_dir = temporaryDir.substring(0, temporaryDir.length() - 1);
        this.econf_dir = econfHome.substring(0, econfHome.length() - 1);
        this.tmp_dir = this.tmp_dir.replace('\\', '/');
        this.econf_dir = this.econf_dir.replace('\\', '/');
    }

    public void initConfig() {
        updateConfig(econf_dir + log4j_path);
        updateConfig(econf_dir + config_path);
    }

    private void updateConfig(String filePath) {
        try {
            String config = loadFile(filePath);
            config = Utilities.replace(config, ECONF_HOME, econf_dir, true);
            config = Utilities.replace(config, TEMP_DIR, tmp_dir, true);
            saveFile(config, filePath);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Loads the specified file.
     */
    private String loadFile(String path) throws IOException, FileNotFoundException {
        BufferedReader in = new BufferedReader(new FileReader(path));
        StringWriter out = new StringWriter();
        int b;
        while ((b = in.read()) != -1) out.write(b);
        out.flush();
        out.close();
        in.close();
        return out.toString();
    }

    /**
     * Writes the specified content to the file.
     */
    private void saveFile(String content, String path) throws IOException {
        File f = new File(path);
        if (f.exists()) f.delete();
        f.createNewFile();
        FileWriter out = new FileWriter(f);
        StringReader in = new StringReader(content);
        int b;
        while ((b = in.read()) != -1) out.write(b);
        out.flush();
        out.close();
        in.close();
    }
}
