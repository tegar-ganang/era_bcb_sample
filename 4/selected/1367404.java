package org.designerator.media.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import org.designerator.media.MediaPlugin;
import org.designerator.media.image.util.IO;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.dialogs.IDEResourceInfoUtils;

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

    public static void deleteThumbDir(IFolder container) {
        if (container == null) {
            return;
        }
        IPath currentThumbdir = MediaPlugin.thumbdir.append(container.getFullPath());
        File f = currentThumbdir.toFile();
        if (f.exists()) {
            try {
                forceDelete(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (!file.exists()) throw new FileNotFoundException("File does not exist: " + file);
            if (!file.delete()) {
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) return;
        cleanDirectory(directory);
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        } else {
            return;
        }
    }

    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }
        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }
        File files[] = directory.listFiles();
        if (files == null) throw new IOException("Failed to list contents of " + directory);
        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }
        if (null != exception) throw exception; else return;
    }

    public static long getSizeofFile(IFile file) {
        IPath location = file.getLocation();
        if (location == null) {
            if (file.isLinked()) {
                return 0;
            }
            return 0;
        } else {
            File localFile = location.toFile();
            if (localFile.exists()) {
                return (localFile.length());
            }
            return 0;
        }
    }

    public static boolean fillImageList(IContainer container, ArrayList<IFile> images, ArrayList<IContainer> childContainers) {
        IResource[] members;
        boolean isVideo = false;
        try {
            if (container.isAccessible()) {
                members = container.members();
                if (members.length < 1 && container.isLinked()) {
                    IFileInfo fileInfo = null;
                    URI location = container.getLocationURI();
                    if (location != null) {
                        fileInfo = IDEResourceInfoUtils.getFileInfo(location);
                    }
                    if (fileInfo != null && !fileInfo.exists()) {
                        System.out.println("Linked but not online");
                    }
                } else {
                    for (int i = 0; i < members.length; i++) {
                        if (members[i] instanceof IFile) {
                            IFile imageFile = (IFile) members[i];
                            if (IO.isValidImageFile(imageFile)) {
                                images.add(imageFile);
                            } else if (IO.isValidVideoFile(imageFile)) {
                                images.add(imageFile);
                                isVideo = true;
                            }
                        } else if (childContainers != null) {
                            if (members[i] instanceof IContainer) {
                                IContainer iContainer = (IContainer) members[i];
                                if (!iContainer.getName().startsWith(".")) {
                                    childContainers.add(iContainer);
                                }
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return isVideo;
    }

    public static void fillImagesVisitor(IContainer container, final ArrayList<IFile> files) {
        try {
            container.accept(new IResourceProxyVisitor() {

                public boolean visit(IResourceProxy proxy) {
                    if (proxy.getType() == IResource.FILE) {
                        if (IO.isValidImageFile(proxy.getName())) {
                            files.add((IFile) proxy.requestResource());
                        }
                    }
                    return true;
                }
            }, IResource.NONE);
        } catch (CoreException e) {
        }
    }

    public static void fillMediaIContainersVisitor(IContainer container, final ArrayList<IContainer> files) {
        try {
            container.accept(new IResourceProxyVisitor() {

                public boolean visit(IResourceProxy proxy) {
                    if (proxy.getType() == IResource.FOLDER) {
                        files.add((IContainer) proxy.requestResource());
                    }
                    return true;
                }
            }, IResource.NONE);
        } catch (CoreException e) {
        }
    }

    public static void traverseTree(IContainer container, ArrayList<IFile> images) {
        IResource[] members;
        try {
            if (container.isAccessible()) {
                members = container.members();
                for (int i = 0; i < members.length; i++) {
                    if (members[i] instanceof IFile) {
                        IFile imageFile = (IFile) members[i];
                        if (IO.isValidMediaFile(imageFile)) {
                            images.add(imageFile);
                        }
                    } else if (members[i] instanceof IContainer) {
                        IContainer iContainer2 = (IContainer) members[i];
                        if (!iContainer2.getName().startsWith(".")) {
                            traverseTree(iContainer2, images);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public static void traverseContainerTree(IContainer container, ArrayList<IContainer> containers) {
        IResource[] members;
        try {
            if (container.isAccessible()) {
                members = container.members();
                boolean valid = false;
                for (int i = 0; i < members.length; i++) {
                    if (members[i] instanceof IFile) {
                        IFile f = (IFile) members[i];
                        if (IO.isValidMediaFile(f) || IO.isValidVideoLinkFile(f.getName())) {
                            valid = true;
                            break;
                        }
                    }
                }
                if (valid) {
                    containers.add(container);
                }
                for (int i = 0; i < members.length; i++) {
                    if (members[i] instanceof IContainer) {
                        IContainer c = (IContainer) members[i];
                        if (!c.getName().startsWith(".")) {
                            traverseContainerTree(c, containers);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public static void fillTree(File folder, ArrayList<File> files) {
        File[] members = folder.listFiles();
        ;
        if (members != null) {
            for (int i = 0; i < members.length; i++) {
                if (members[i].isFile()) {
                    files.add(members[i]);
                } else if (members[i].isDirectory()) {
                    fillTree(members[i], files);
                }
            }
        }
    }

    public static Image getIconFromProgram(String file) {
        String fileExtension = getFileExtension(file);
        if (fileExtension == null) {
            return null;
        }
        Image image = JFaceResources.getImageRegistry().get(fileExtension);
        if (image == null) {
            Program program = Program.findProgram(fileExtension);
            if (program == null) {
                return null;
            }
            ImageData imageData = program.getImageData();
            if (imageData != null) {
                image = new Image(null, imageData, imageData.getTransparencyMask());
                JFaceResources.getImageRegistry().put(fileExtension, image);
            }
        }
        return image;
    }
}
