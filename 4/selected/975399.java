package net.sf.mzmine.main.mzmineclient;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.desktop.impl.helpsystem.HelpImpl;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.main.MZminePreferences;
import net.sf.mzmine.project.impl.ProjectManagerImpl;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * Main client class
 */
public class MZmineClient extends MZmineCore implements Runnable {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Vector<MZmineModule> moduleSet;

    private static MZmineClient client = new MZmineClient();

    private MZmineClient() {
    }

    public static MZmineClient getInstance() {
        return client;
    }

    /**
	 * Main method
	 */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(client);
    }

    /**
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        Document configuration = null;
        MainWindow desktop = null;
        logger.finest("Checking for old temporary files...");
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File remainingTmpFiles[] = tempDir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.matches("mzmine.*\\.scans");
                }
            });
            for (File remainingTmpFile : remainingTmpFiles) {
                if (!remainingTmpFile.canWrite()) continue;
                RandomAccessFile rac = new RandomAccessFile(remainingTmpFile, "rw");
                FileLock lock = rac.getChannel().tryLock();
                rac.close();
                if (lock != null) {
                    logger.finest("Removing unused file " + remainingTmpFile);
                    remainingTmpFile.delete();
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while checking for old temporary files", e);
        }
        SAXReader reader = new SAXReader();
        try {
            configuration = reader.read(CONFIG_FILE);
        } catch (DocumentException e1) {
            if (CONFIG_FILE.exists()) {
                logger.log(Level.WARNING, "Error parsing the configuration file " + CONFIG_FILE + ", loading default configuration", e1);
            }
            try {
                configuration = reader.read(DEFAULT_CONFIG_FILE);
            } catch (DocumentException e2) {
                logger.log(Level.SEVERE, "Error parsing the default configuration file " + DEFAULT_CONFIG_FILE, e2);
                System.exit(1);
            }
        }
        Element configRoot = configuration.getRootElement();
        logger.info("Starting MZmine 2");
        logger.info("Loading core classes..");
        MZmineCore.preferences = new MZminePreferences();
        TaskControllerImpl taskController = new TaskControllerImpl();
        projectManager = new ProjectManagerImpl();
        desktop = new MainWindow();
        help = new HelpImpl();
        MZmineCore.taskController = taskController;
        MZmineCore.desktop = desktop;
        logger.finer("Initializing core classes..");
        projectManager.initModule();
        desktop.initModule();
        taskController.initModule();
        logger.finer("Loading modules");
        moduleSet = new Vector<MZmineModule>();
        Iterator<Element> modIter = configRoot.element(MODULES_ELEMENT_NAME).elementIterator(MODULE_ELEMENT_NAME);
        while (modIter.hasNext()) {
            Element moduleElement = modIter.next();
            String className = moduleElement.attributeValue(CLASS_ATTRIBUTE_NAME);
            loadModule(className);
        }
        MZmineCore.initializedModules = moduleSet.toArray(new MZmineModule[0]);
        try {
            if (CONFIG_FILE.exists()) loadConfiguration(CONFIG_FILE);
        } catch (DocumentException e) {
            logger.log(Level.WARNING, "Error while loading module configuration", e);
        }
        ShutDownHook shutDownHook = new ShutDownHook();
        Runtime.getRuntime().addShutdownHook(shutDownHook);
        logger.finest("Showing main window");
        desktop.setVisible(true);
        desktop.setStatusBarText("Welcome to MZmine 2!");
    }

    public MZmineModule loadModule(String moduleClassName) {
        try {
            logger.finest("Loading module " + moduleClassName);
            Class moduleClass = Class.forName(moduleClassName);
            MZmineModule moduleInstance = (MZmineModule) moduleClass.newInstance();
            moduleInstance.initModule();
            moduleSet.add(moduleInstance);
            return moduleInstance;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not load module " + moduleClassName, e);
            return null;
        }
    }
}
