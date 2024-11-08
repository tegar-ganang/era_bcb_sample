package org.wtc.eclipse.platform.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.osgi.framework.Bundle;
import org.wtc.eclipse.platform.PlatformActivator;

/**
 */
public class FileUtil {

    /**
     */
    public static void copyFile(File from, File to) throws IOException {
        assert (from != null);
        assert (to != null);
        if (!to.exists()) {
            File parentDir = to.getParentFile();
            if (!parentDir.exists()) parentDir.mkdirs();
            to.createNewFile();
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            try {
                out = new FileOutputStream(to);
                FileChannel ic = in.getChannel();
                try {
                    FileChannel oc = out.getChannel();
                    try {
                        oc.transferFrom(ic, 0, from.length());
                    } finally {
                        if (oc != null) {
                            oc.close();
                        }
                    }
                } finally {
                    if (ic != null) {
                        ic.close();
                    }
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Copies the contents of the source File to the destination File. If the args are
     * directories, performs a recurive copy.
     *
     * @param   from  Source file. If a directory, contents will be copied recursively.
     * @param   to    Destination file. If source is a directory, must be a directory.
     * @throws  IOException  Thrown if an error is encountered.
     */
    public static void copyFiles(final File from, final File to) throws IOException {
        copyFiles(from, to, (String) null);
    }

    /**
     * Copies the contents of the source File to the destination File. If the args are
     * directories, performs a recurive copy. If the pattern regexp is specified, only
     * files that match the pattern will be copied.
     *
     * @param   from           Source file. If a directory, contents will be copied
     *                         recursively.
     * @param   to             Destination file. If source is a directory, must be a
     *                         directory.
     * @param   regexpPattern  Optional regular expression pattern. If specified, source
     *                         file names must match to be copied and empty directories
     *                         will not be copied.
     * @return  True if any files were copied.
     * @throws  IOException  Thrown if an error is encountered.
     */
    public static boolean copyFiles(final File from, final File to, final String regexp) throws IOException {
        assert (from != null);
        assert (to != null);
        Pattern pattern = null;
        if (regexp != null) {
            pattern = Pattern.compile(regexp);
        }
        return copyFiles(from, to, pattern);
    }

    private static boolean copyFiles(final File from, final File to, final Pattern regexp) throws IOException {
        if (from.isDirectory()) {
            if (!to.exists()) {
                to.mkdirs();
            } else {
                assert (to.isDirectory());
            }
            final File[] fromChildren = from.listFiles();
            boolean copiedOneChild = false;
            if (fromChildren != null) {
                for (File nextFrom : fromChildren) {
                    final File nextTo = new File(to, nextFrom.getName());
                    if (copyFiles(nextFrom, nextTo, regexp)) {
                        copiedOneChild = true;
                    }
                }
            }
            if ((regexp != null) && !copiedOneChild) {
                to.delete();
            }
            return copiedOneChild;
        } else {
            if ((regexp == null) || regexp.matcher(from.getName()).matches()) {
                copyFile(from, to);
                return true;
            }
            return false;
        }
    }

    /**
     */
    public static void copyFileToProject(File from, IFile to) throws IOException, CoreException {
        copyFile(from, to.getLocation().toFile());
        to.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
    }

    /**
     * Deletes the specified file. If a directory, recursively deletes all contents.
     *
     * @param   file  File or directory to delete.
     * @return  True if successfully deleted.
     * @throws  IOException  Thrown if an error is encountered.
     */
    public static boolean deleteFile(final File file) throws IOException {
        assert (file != null);
        if (file.isDirectory()) {
            final File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFile(child);
                }
            }
        }
        return file.delete();
    }

    /**
     * Return the contents of the given source as a string.
     */
    public static String getFileContents(IFile sourceFile) throws CoreException {
        assert (sourceFile != null);
        String contents = "";
        IPath fullPath = sourceFile.getFullPath();
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        try {
            bufferManager.connect(fullPath, LocationKind.NORMALIZE, null);
            ITextFileBuffer buff = bufferManager.getTextFileBuffer(fullPath, LocationKind.NORMALIZE);
            assert (buff != null);
            ISchedulingRule rule = buff.computeCommitRule();
            Job.getJobManager().beginRule(rule, new NullProgressMonitor());
            try {
                IDocument doc = buff.getDocument();
                contents = doc.get();
            } finally {
                Job.getJobManager().endRule(rule);
            }
        } finally {
            bufferManager.disconnect(fullPath, LocationKind.NORMALIZE, null);
        }
        return contents;
    }

    /**
     */
    public static File getFileFromBundle(Bundle bundle, String pluginRelativePath) {
        File file = null;
        URL fileDir = FileLocator.find(bundle, new Path(pluginRelativePath), null);
        assert (fileDir != null) : "Couldn't find path in plugin bundle: " + pluginRelativePath;
        try {
            URL localURL = FileLocator.toFileURL(fileDir);
            file = new File(localURL.toURI());
        } catch (Exception e) {
            PlatformActivator.logException(e);
            assert (false) : e.getMessage();
        }
        return file;
    }

    /**
     */
    public static File getFileFromPlugin(Plugin plugin, String pluginRelativePath) {
        Bundle bundle = plugin.getBundle();
        return getFileFromBundle(bundle, pluginRelativePath);
    }

    /**
     * Determine if the given file's content type matches the given content type id. If
     * the file does not exist, or an error is encountered trying to get its content type,
     * this will return false.
     */
    public static boolean isContentType(IFile file, String contentTypeId) {
        assert (file != null);
        ISchedulingRule rule = null;
        try {
            rule = ResourcesPlugin.getWorkspace().getRuleFactory().deleteRule(file);
            Job.getJobManager().beginRule(rule, null);
            if (!file.exists()) return false;
            IContentDescription contentDesc = null;
            try {
                contentDesc = file.getContentDescription();
            } catch (CoreException e) {
                contentDesc = null;
            }
            if (contentDesc != null) {
                IContentType contentType = contentDesc.getContentType();
                if (contentType != null) {
                    if (contentType.getId().equals(contentTypeId)) return true;
                }
            }
        } finally {
            if (rule != null) {
                Job.getJobManager().endRule(rule);
            }
        }
        return false;
    }
}
