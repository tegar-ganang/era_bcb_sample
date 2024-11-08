package net.sf.p2pim;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import net.sf.component.config.ConfigHelper;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class P2PActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "net.sf.p2pim";

    private static P2PActivator plugin;

    /**
	 * The constructor
	 */
    public P2PActivator() {
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        if (!new File(ConfigHelper.getDataHome() + "/data/embed/faces/01.jpg").exists()) {
            final File dstFile = new File(ConfigHelper.getDataHome() + "/data/embed/faces/");
            dstFile.mkdirs();
            final String src = FileLocator.toFileURL(BundleUtility.find(P2PActivator.PLUGIN_ID, "icons/faces/")).getPath();
            File[] listFiles = new File(src).listFiles(new FileFilter() {

                public boolean accept(File arg0) {
                    return arg0.getName().endsWith("jpg");
                }
            });
            for (File f : listFiles) {
                copy(f, new File(dstFile, f.getName()));
            }
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
    public static P2PActivator getDefault() {
        return plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /** Fast & simple file copy. */
    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
