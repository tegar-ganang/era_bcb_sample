package ingenias.plugin.wizard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

public class FileCopy {

    public static void main(String[] args) {
        try {
            copy("fromFile.txt", "toFile.txt");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static File copy(InputStream from, String toFileName) throws IOException {
        File toFile = new File(toFileName);
        System.out.println("AbsolutePath toFile: " + toFile.getAbsolutePath());
        if (toFile.isDirectory()) throw new IOException("Target file " + toFileName + " is a directory when a file was expected");
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileOutputStream to = null;
        try {
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
        return toFile;
    }

    private static URL findFileInPlugin(String plugin, String file) throws MalformedURLException, IOException {
        IPluginRegistry registry = Platform.getPluginRegistry();
        IPluginDescriptor descriptor = registry.getPluginDescriptor(plugin);
        URL pluginURL = descriptor.getInstallURL();
        URL jarURL = new URL(pluginURL, file);
        System.err.println("from " + jarURL.toString());
        return jarURL;
    }

    public static IPath copyFile(IProgressMonitor monitor, String fileSystemFullPathOfTargetProject, String fromFile, String toDirectory) throws MalformedURLException, IOException {
        String fromStr, toStr;
        URL path = findFileInPlugin("IDKEditor", fromFile);
        toStr = fileSystemFullPathOfTargetProject + "/" + toDirectory;
        try {
            File toFile = FileCopy.copy(path.openStream(), toStr);
            return new Path(toFile.getPath());
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public static File copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        System.out.println("AbsolutePath fromFile: " + fromFile.getAbsolutePath());
        System.out.println("AbsolutePath toFile: " + toFile.getAbsolutePath());
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
        return toFile;
    }
}
