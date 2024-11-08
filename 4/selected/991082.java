package candyfolds.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.Log;

public class PluginHome {

    public static final String MODE_CONFIG_FILE_SUFIX = "-CandyFolds.properties";

    private static File FILE;

    private static File getFile() {
        if (FILE != null) return FILE;
        String jEditSettingsDir = jEdit.getSettingsDirectory();
        if (jEditSettingsDir == null) jEditSettingsDir = jEdit.getJEditHome();
        if (jEditSettingsDir == null) {
            Log.log(Log.ERROR, PluginHome.class, "Couldn't get plugin home directory; using home.");
            jEditSettingsDir = System.getProperty("user.home");
        }
        FILE = new File(jEditSettingsDir + File.separator + "plugins" + File.separator + "CandyFolds");
        if (!FILE.exists()) {
            FILE.mkdirs();
            initializeDistribModeConfigFiles();
        }
        return FILE;
    }

    public static File getModeConfigFile(String modeConfigName) {
        return new File(getFile(), modeConfigName + MODE_CONFIG_FILE_SUFIX);
    }

    private static InputStream getDistribModeConfig(String modeConfigName) {
        return Config.class.getClassLoader().getResourceAsStream("CandyFolds-properties/" + modeConfigName + MODE_CONFIG_FILE_SUFIX);
    }

    static final String[] distribModeConfigNames = { "java", "xml" };

    private static void initializeDistribModeConfigFiles() {
        Log.log(Log.NOTICE, PluginHome.class, "Initializing distrib config files.");
        for (String modeConfigName : distribModeConfigNames) {
            File modeConfigFile = getModeConfigFile(modeConfigName);
            if (modeConfigFile.exists()) continue;
            InputStream is = getDistribModeConfig(modeConfigName);
            if (is == null) throw new AssertionError("distribution configuration for mode not found: " + modeConfigName);
            try {
                is = new BufferedInputStream(is);
                modeConfigFile.createNewFile();
                OutputStream os = new BufferedOutputStream(new FileOutputStream(modeConfigFile));
                for (int r; (r = is.read()) != -1; ) os.write(r);
                is.close();
                os.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
