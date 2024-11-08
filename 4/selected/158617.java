package com.threerings.getdown.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import com.samskivert.io.StreamUtil;
import com.threerings.getdown.util.FileUtil;
import com.threerings.getdown.util.ProgressObserver;
import static com.threerings.getdown.Log.log;

/**
 * Applies a unified patch file to an application directory, providing
 * percentage completion feedback along the way. <em>Note:</em> the
 * patcher is not thread safe. Create a separate patcher instance for each
 * patching action that is desired.
 */
public class Patcher {

    /** A suffix appended to file names to indicate that a file should be newly created. */
    public static final String CREATE = ".create";

    /** A suffix appended to file names to indicate that a file should be patched. */
    public static final String PATCH = ".patch";

    /** A suffix appended to file names to indicate that a file should be deleted. */
    public static final String DELETE = ".delete";

    /**
     * Applies the specified patch file to the application living in the
     * specified application directory. The supplied observer, if
     * non-null, will be notified of progress along the way.
     *
     * <p><em>Note:</em> this method runs on the calling thread, thus the
     * caller may want to make use of a separate thread in conjunction
     * with the patcher so that the user interface is not blocked for the
     * duration of the patch.
     */
    public void patch(File appdir, File patch, ProgressObserver obs) throws IOException {
        _obs = obs;
        _plength = patch.length();
        JarFile file = new JarFile(patch);
        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String path = entry.getName();
            long elength = entry.getCompressedSize();
            if (path.endsWith(CREATE)) {
                path = strip(path, CREATE);
                System.out.println("Creating " + path + "...");
                createFile(file, entry, new File(appdir, path));
            } else if (path.endsWith(PATCH)) {
                path = strip(path, PATCH);
                System.out.println("Patching " + path + "...");
                patchFile(file, entry, appdir, path);
            } else if (path.endsWith(DELETE)) {
                path = strip(path, DELETE);
                System.out.println("Removing " + path + "...");
                File target = new File(appdir, path);
                if (!target.delete()) {
                    System.err.println("Failure deleting '" + target + "'.");
                }
            } else {
                System.err.println("Skipping bogus patch file entry: " + path);
            }
            _complete += elength;
        }
        file.close();
    }

    protected String strip(String path, String suffix) {
        return path.substring(0, path.length() - suffix.length());
    }

    protected void createFile(JarFile file, ZipEntry entry, File target) {
        if (_buffer == null) {
            _buffer = new byte[COPY_BUFFER_SIZE];
        }
        File pdir = target.getParentFile();
        if (!pdir.exists()) {
            if (!pdir.mkdirs()) {
                log.warning("Failed to create parent for '" + target + "'.");
            }
        }
        InputStream in = null;
        FileOutputStream fout = null;
        try {
            in = file.getInputStream(entry);
            fout = new FileOutputStream(target);
            int total = 0, read;
            while ((read = in.read(_buffer)) != -1) {
                total += read;
                fout.write(_buffer, 0, read);
                updateProgress(total);
            }
        } catch (IOException ioe) {
            System.err.println("Error creating '" + target + "': " + ioe);
        } finally {
            StreamUtil.close(in);
            StreamUtil.close(fout);
        }
    }

    protected void patchFile(JarFile file, ZipEntry entry, File appdir, String path) {
        File target = new File(appdir, path);
        File patch = new File(appdir, entry.getName());
        File otarget = new File(appdir, path + ".old");
        JarDiffPatcher patcher = null;
        otarget.delete();
        InputStream in = null;
        FileOutputStream fout = null;
        try {
            StreamUtil.copy(in = file.getInputStream(entry), fout = new FileOutputStream(patch));
            StreamUtil.close(fout);
            fout = null;
            if (!FileUtil.renameTo(target, otarget)) {
                System.err.println("Failed to .oldify '" + target + "'.");
                return;
            }
            final long elength = entry.getCompressedSize();
            ProgressObserver obs = new ProgressObserver() {

                public void progress(int percent) {
                    updateProgress((int) (percent * elength / 100));
                }
            };
            patcher = new JarDiffPatcher();
            fout = new FileOutputStream(target);
            patcher.patchJar(otarget.getPath(), patch.getPath(), fout, obs);
        } catch (IOException ioe) {
            if (patcher == null) {
                System.err.println("Failed to write patch file '" + patch + "': " + ioe);
            } else {
                System.err.println("Error patching '" + target + "': " + ioe);
            }
        } finally {
            StreamUtil.close(fout);
            StreamUtil.close(in);
            if (!patch.delete()) {
                patch.deleteOnExit();
            }
            if (!otarget.delete()) {
                otarget.deleteOnExit();
            }
        }
    }

    protected void updateProgress(int progress) {
        if (_obs != null) {
            _obs.progress((int) (100 * (_complete + progress) / _plength));
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: Patcher appdir patch_file");
            System.exit(-1);
        }
        Patcher patcher = new Patcher();
        try {
            patcher.patch(new File(args[0]), new File(args[1]), null);
        } catch (IOException ioe) {
            System.err.println("Error: " + ioe.getMessage());
            System.exit(-1);
        }
    }

    protected ProgressObserver _obs;

    protected long _complete, _plength;

    protected byte[] _buffer;

    protected static final int COPY_BUFFER_SIZE = 4096;
}
