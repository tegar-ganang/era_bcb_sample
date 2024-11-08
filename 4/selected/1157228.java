package net.sourceforge.sevents.mainmodule;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Arrays;
import net.sourceforge.sevents.scripthandle.Log;
import net.sourceforge.sevents.scripthandle.StringLibrary;
import net.sourceforge.sevents.util.FileUtils;
import net.sourceforge.sevents.util.SSettings;

/**
 *
 * @author Rinat Bikov <becase@altlinux.org>
 */
public class ModuleExecutor {

    /**
     * List of modules.
     */
    private LinkedList<String> modules = null;

    /**
     * Read list of modules
     */
    private void readModuleNames() {
        modules = new LinkedList<String>();
        String libDir = SSettings.getString(FileUtils.writeLibDirName);
        String libExt = SSettings.getString(FileUtils.writeLibExtension);
        boolean isWinSys = false;
        try {
            isWinSys = System.getProperty("os.name").split(" ")[0].toLowerCase().equals("windows");
        } catch (Exception e) {
        }
        String[] files = FileUtils.getFiles(libDir);
        Arrays.sort(files);
        for (String file : files) {
            String ext = StringLibrary.getFileExtention(file);
            if ((isWinSys && ext.equalsIgnoreCase(libExt)) || (!isWinSys && ext.equals(libExt))) {
                modules.add(file);
            }
        }
    }

    /**
     * Run modules
     */
    public void runModules() {
        if (modules == null) {
            readModuleNames();
        }
        if (!LibLoader.isRunning()) {
            try {
                LibLoader.startProcess();
            } catch (IOException ioe) {
                Log.war("Cannot run libLoader", Log.WARNING);
                ioe.printStackTrace(System.err);
                return;
            }
        }
        for (String module : modules) {
            try {
                LibLoader.loadLibrary(module);
                LibLoader.setFunction("run", null);
                LibLoader.runThread(SSettings.getInt(FileUtils.writeMaxProcDelay, FileUtils.defaultMaxProcDelay));
                if (LibLoader.isRunning()) {
                    LibLoader.unloadLibrary();
                }
            } catch (IOException ioe) {
                Log.out("runModules: " + ioe.getClass().getName() + " " + ioe.getMessage(), Log.WARNING);
                try {
                    LibLoader.stopProcess();
                } catch (IOException ex) {
                    Log.out("stopProcess: " + ex.getMessage(), Log.WARNING);
                }
                try {
                    LibLoader.startProcess();
                } catch (IOException ex) {
                    Log.out("startProcess: " + ex.getMessage(), Log.WARNING);
                }
            }
        }
    }
}
