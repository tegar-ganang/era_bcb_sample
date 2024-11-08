package org.designerator.explorer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class ExplorerUtil {

    private static ImageRegistry image_registry;

    private static Clipboard clipboard;

    public static String getFileExtension(String name) {
        int index = name.lastIndexOf('.');
        if (index == -1) return null;
        if (index == (name.length() - 1)) return "";
        return name.substring(index + 1);
    }

    public static String addFileExtension(String extension, String name) {
        return name + "." + extension;
    }

    public static String removeFileExtension(String name) {
        int index = name.lastIndexOf('.');
        if (index == -1) return null;
        return name.substring(0, index);
    }

    public static Properties getProperties(String path) {
        File file = new File(path, "thumb.properties");
        Properties state = new Properties();
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                state.load(fis);
                fis.close();
            } catch (IOException e) {
                System.out.println("error reading props: " + e);
            }
        } else {
            try {
                if (file.createNewFile()) {
                    FileInputStream fis = new FileInputStream(file);
                    state.load(fis);
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return state;
    }

    public static void saveProperties(Properties state, String path) {
        if (state != null) {
            try {
                File file = new File(path, "thumb.properties");
                FileOutputStream fos = new FileOutputStream(file);
                state.store(fos, "");
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static URL newURL(String url_name) {
        try {
            return new URL(url_name);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL " + url_name, e);
        }
    }

    public static ImageRegistry getImageRegistry() {
        if (image_registry == null) {
            image_registry = new ImageRegistry();
            image_registry.put("folder", ImageDescriptor.createFromURL(newURL("file:icons/folder.gif")));
            image_registry.put("file", ImageDescriptor.createFromURL(newURL("file:icons/file.gif")));
        }
        return image_registry;
    }

    public static Clipboard getClipboard() {
        if (clipboard == null) {
            clipboard = new Clipboard(Display.getCurrent());
        }
        return clipboard;
    }

    public static void fileCopy(String from_name, String to_name) throws IOException {
        File fromFile = new File(from_name);
        File toFile = new File(to_name);
        if (fromFile.equals(toFile)) abort("cannot copy on itself: " + from_name);
        if (!fromFile.exists()) abort("no such currentSourcepartName file: " + from_name);
        if (!fromFile.isFile()) abort("can't copy directory: " + from_name);
        if (!fromFile.canRead()) abort("currentSourcepartName file is unreadable: " + from_name);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) abort("destination file is unwriteable: " + to_name);
        } else {
            String parent = toFile.getParent();
            if (parent == null) abort("destination directory doesn't exist: " + parent);
            File dir = new File(parent);
            if (!dir.exists()) abort("destination directory doesn't exist: " + parent);
            if (dir.isFile()) abort("destination is not a directory: " + parent);
            if (!dir.canWrite()) abort("destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
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
    }

    private static void abort(String msg) throws IOException {
        throw new IOException("FileCopy: " + msg);
    }

    public static ISelection getCurrentWorkbenchSelection() {
        ISelection sel;
        sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
        if (!(sel instanceof StructuredSelection)) sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection("org.eclipse.ui.views.ResourceNavigator");
        if (sel == null) sel = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
        return sel;
    }

    public static void main(String[] args) {
        System.out.println(removeFileExtension("file.jpg"));
        System.out.println(addFileExtension("jpg", "car.png"));
    }
}
