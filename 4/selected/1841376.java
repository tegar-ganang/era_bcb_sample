package org.globaltester.testmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.LinkedList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @version Release 2.4.1
 * @author Holger Funke
 * 
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.globaltester.testmanager";

    private static Activator plugin;

    public static final String VERSION = "Release 2.5.0 (19.04.2010)";

    public static final String EXTENSION_POINT_TEST_EXTENDER = "org.globaltester.testmanager.testextender";

    public static LinkedList<ITestExtender> testExtenders = new LinkedList<ITestExtender>();

    /**
	 * The constructor
	 */
    public Activator() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        checkEnvironment();
        processTestExtenders();
    }

    private void processTestExtenders() {
        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_TEST_EXTENDER);
        if (extensionPoint == null) {
            throw new RuntimeException("unable to resolve extension-point: " + EXTENSION_POINT_TEST_EXTENDER);
        }
        IConfigurationElement[] members = extensionPoint.getConfigurationElements();
        for (int m = 0; m < members.length; m++) {
            IConfigurationElement member = members[m];
            testExtenders.add(new TestExtenderProxy(member));
        }
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
    public static Activator getDefault() {
        return plugin;
    }

    /**
	 * Returns the current date inclusive time in ISO format
	 */
    public static String getIsoDate(String DATE_FORMAT) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        return sdf.format(cal.getTime());
    }

    /**
	 * Returns the current path of GlobalTester_Plugin
	 */
    public static IPath getPluginDir() {
        URL url = plugin.getBundle().getEntry("/");
        IPath pluginDir = null;
        try {
            pluginDir = new Path(FileLocator.toFileURL(url).getPath());
            return pluginDir;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Get current project from workspace
	 * 
	 * @return current project
	 */
    public static IProject getCurrentProject(String workingDirectory) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IPath path = new Path(workingDirectory);
        IProject project = root.getProject(path.segment(path.segmentCount() - 1));
        return project;
    }

    /**
	 * Lookup for property file 'opencard.properties'
	 * 
	 */
    private void checkEnvironment() {
        IPath pluginDir = getPluginDir();
        File propertyFile = new File(System.getProperty("user.dir") + "/opencard.properties");
        if (propertyFile.exists()) {
        } else {
            File internalPropertyFile = new File(pluginDir + "/opencard.properties");
            copy(internalPropertyFile, propertyFile);
        }
        File logoFile = new File(System.getProperty("user.dir") + "/GT_Logo.gif");
        if (!logoFile.exists()) {
            File internalLogoFile = new File(pluginDir + "/stylesheets/reports/GT_Logo.gif");
            copy(internalLogoFile, logoFile);
        }
    }

    /**
	 * Overwrites destination file with source file
	 * 
	 * @param sourceFile
	 * @param destinationFile
	 */
    private void copy(File sourceFile, File destinationFile) {
        try {
            FileChannel in = new FileInputStream(sourceFile).getChannel();
            FileChannel out = new FileOutputStream(destinationFile).getChannel();
            try {
                in.transferTo(0, in.size(), out);
                in.close();
                out.close();
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e) {
        }
    }

    /**
	 * 
	 * @return the working directory currently stored in preferences
	 */
    public static String getWorkingDir() {
        IPreferenceStore store = org.globaltester.Activator.getDefault().getPreferenceStore();
        String workingDirectory = store.getString(org.globaltester.preferences.PreferenceConstants.P_WORKINGDIR);
        return workingDirectory;
    }
}
