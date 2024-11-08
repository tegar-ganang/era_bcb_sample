package yager.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Ryan Hild (therealfreaker@sourceforge.net)
 */
public final class ResourcePathManager {

    protected static final List directoryPaths = new ArrayList();

    protected static final List archivePaths = new ArrayList();

    static {
        addDirectoryPath("./");
    }

    private ResourcePathManager() {
    }

    public static void addDirectoryPath(String path) {
        if (!path.endsWith("/") && !path.endsWith("\\")) path = path.concat("/");
        directoryPaths.add(path);
    }

    public static void addArchivePath(String path) {
        archivePaths.add(path);
    }

    public static void removeDirectoryPath(String path) {
        directoryPaths.remove(directoryPaths.indexOf(path));
    }

    public static void removeArchivePath(String path) {
        archivePaths.remove(archivePaths.indexOf(path));
    }

    public static String[] getDirectoryPaths() {
        return (String[]) directoryPaths.toArray(new String[0]);
    }

    public static String[] getArchivePaths() {
        return (String[]) archivePaths.toArray(new String[0]);
    }

    public static ReadableByteChannel getChannel(String fileName) {
        String[] directories = (String[]) directoryPaths.toArray(new String[0]);
        for (int i = 0; i < directories.length; ++i) {
            try {
                FileChannel channel = new FileInputStream(directories[i] + fileName).getChannel();
                return channel;
            } catch (IOException e) {
            }
        }
        String[] archives = (String[]) archivePaths.toArray(new String[0]);
        for (int i = 0; i < archives.length; ++i) {
            try {
                ZipFile zipFile = new ZipFile(archives[i]);
                ZipEntry entry = zipFile.getEntry(fileName);
                ReadableByteChannel channel = Channels.newChannel(zipFile.getInputStream(entry));
                return channel;
            } catch (IOException e) {
            }
        }
        return Channels.newChannel(ClassLoader.getSystemResourceAsStream(fileName));
    }
}
