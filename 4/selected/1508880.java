package org.peertrust.modeler.policysystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class PolicysystemPlugin extends AbstractUIPlugin {

    private static PolicysystemPlugin plugin;

    private static final String LOG_PROPERTIES_FILE = "logger.properties";

    private static final String RDFS_FILE = "/model/schema.rdfs";

    private static final String RDF_FILE = "/model/empty.rdf";

    public static final String IMG_KEY_POLICY = "IMG_KEY_POLICY";

    public static final String IMG_PATH_POLICY = "/icons/policy.gif";

    public static final String IMG_KEY_OVERRIDDEN_POLICY = "IMG_KEY_OVERRIDDEN_POLICY";

    public static final String IMG_PATH_OVERRIDDEN_POLICY = "/icons/overridden_policy.gif";

    public static final String IMG_KEY_ORULE = "IMG_KEY_ORULE";

    public static final String IMG_PATH_ORULE = "/icons/orule.gif";

    public static final String IMG_KEY_FILTER = "IMG_KEY_FILTER";

    public static final String IMG_PATH_FILTER = "/icons/filter.gif";

    public static final String IMG_KEY_RESOURCE = "IMG_KEY_REOURCE";

    public static final String IMG_PATH_RESOURCE = "/icons/resource.gif";

    /**
	 * The constructor.
	 */
    public PolicysystemPlugin() {
        plugin = this;
    }

    /**
	 * This method is called upon plug-in activation
	 */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        try {
            configure();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * This method is called when the plug-in is stopped
	 */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
	 * Returns the shared instance.
	 */
    public static PolicysystemPlugin getDefault() {
        return plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        PolicysystemPlugin.getDefault().getWorkbench();
        return AbstractUIPlugin.imageDescriptorFromPlugin("org.peertrust.modeler.policysystem", path);
    }

    private void configure() {
        File configFile = new File(Platform.getInstanceLocation().getURL().getPath(), LOG_PROPERTIES_FILE);
        ;
        if (configFile.exists() == false) {
            BasicConfigurator.configure();
        } else {
            try {
                Properties props = new Properties();
                FileInputStream iStream = new FileInputStream(configFile);
                props.load(iStream);
                PropertyConfigurator.configure(props);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void showMessage(final String message) {
        plugin.getWorkbench().getDisplay().syncExec(new Runnable() {

            public void run() {
                Shell shell = plugin.getWorkbench().getDisplay().getActiveShell();
                MessageDialog.openInformation(shell, "Message", message);
            }
        });
    }

    public boolean askQuestion(final String message) {
        Shell shell = plugin.getWorkbench().getDisplay().getActiveShell();
        return MessageDialog.openConfirm(shell, "Message", message);
    }

    public void showException(String message, Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(message);
        pw.print("\n=======================================\n");
        th.printStackTrace(pw);
        this.showMessage(sw.getBuffer().toString());
    }

    public void copyModelfilesTo(File rdfschemaFile, File rdfModelFile) {
        try {
            URL schemaURL = this.getBundle().getEntry(RDFS_FILE);
            InputStream iStream = schemaURL.openStream();
            FileOutputStream fOut = new FileOutputStream(rdfschemaFile);
            byte bytes[] = new byte[512];
            int read = -1;
            while ((read = iStream.read(bytes, 0, 512)) != -1) {
                fOut.write(bytes, 0, read);
            }
        } catch (Exception e) {
            showException("Exception while Copying rdfs schema", e);
        }
        try {
            URL modelURL = this.getBundle().getEntry(RDF_FILE);
            InputStream iStream = modelURL.openStream();
            FileOutputStream fOut = new FileOutputStream(rdfModelFile);
            byte bytes[] = new byte[512];
            int read = -1;
            while ((read = iStream.read(bytes, 0, 512)) != -1) {
                fOut.write(bytes, 0, read);
            }
        } catch (Exception e) {
            showException("Exception while Copying rdf model file", e);
        }
    }

    protected void initializeImageRegistry(ImageRegistry reg) {
        String couples[][] = { { IMG_KEY_FILTER, IMG_PATH_FILTER }, { IMG_KEY_ORULE, IMG_PATH_ORULE }, { IMG_KEY_OVERRIDDEN_POLICY, IMG_PATH_OVERRIDDEN_POLICY }, { IMG_KEY_POLICY, IMG_PATH_POLICY }, { IMG_KEY_RESOURCE, IMG_PATH_RESOURCE } };
        String curCouple[];
        for (int i = 0; i < couples.length; i++) {
            curCouple = couples[i];
            URL url = getBundle().getEntry(curCouple[1]);
            System.out.println("loading:" + curCouple[0] + "=" + curCouple[1] + " url=" + url);
            ImageDescriptor desc = ImageDescriptor.createFromURL(url);
            reg.put(curCouple[0], desc);
        }
        return;
    }
}
