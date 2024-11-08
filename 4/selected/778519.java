package net.sf.buildbox.util;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

public final class FileUtils {

    private FileUtils() {
    }

    /**
     * @deprecated use {@link BbxMiscUtils#copyStream(java.io.InputStream, java.io.OutputStream)} instead
     * @param is -
     * @param os -
     * @throws IOException -
     */
    @Deprecated
    public static void copy(InputStream is, OutputStream os) throws IOException {
        BbxMiscUtils.copyStream(is, os);
    }

    /**
     * @deprecated use {@link org.codehaus.plexus.util.FileUtils#mkdir(String)}  from plexus-utils instead
     * @param dir -
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void mkdirs(File dir) throws IOException {
        org.codehaus.plexus.util.FileUtils.mkdir(dir.getAbsolutePath());
    }

    /**
     * @deprecated use {@link org.codehaus.plexus.util.FileUtils#deleteDirectory(java.io.File)} from plexus-utils instead
     * @param root -
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void deepDelete(File root) throws IOException {
        root.delete();
        if (root.exists()) {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(root);
        }
    }

    /**
     * @deprecated use plexus-utils: {@link org.codehaus.plexus.util.FileUtils#getFileNames(java.io.File, String, String, boolean)}  instead
     * Lists all files (not including directories) into a collection of strings.
     * Each string is pathname relative to dir.
     * @param dir  the root dir for files to be listed
     * @return file listing
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static Collection<String> listFilesRecursive(File dir) {
        try {
            return org.codehaus.plexus.util.FileUtils.getFileNames(dir, null, null, false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @deprecated use plexus-utils: FileUtils#getFileAndDirectoryNames() instead
     * @param dir -
     * @param addDirectories -
     * @return -
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static Collection<String> listFilesRecursive(File dir, boolean addDirectories) {
        try {
            return org.codehaus.plexus.util.FileUtils.getFileAndDirectoryNames(dir, null, null, false, true, true, addDirectories);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @deprecated use {@link org.codehaus.plexus.archiver.zip.ZipArchiver} from library <code>org.codehaus.plexus:plexus-archiver</code> instead.
     * @param dir -
     * @param zipFile -
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void zipDirectory(File dir, File zipFile) throws IOException {
        BbxZipUtils.zipDirectory(dir, zipFile);
    }

    /**
     * @deprecated use {@link org.codehaus.plexus.archiver.zip.ZipArchiver} from library <code>org.codehaus.plexus:plexus-archiver</code> instead.
     * @param dir -
     * @param explicitFiles -
     * @param zipFile -
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void zipDirectory(File dir, Collection<String> explicitFiles, File zipFile) throws IOException {
        BbxZipUtils.zipDirectory(dir, explicitFiles, zipFile);
    }

    /**
     * @param inputFile -
     * @param outputFile -
     * @param keepLastModified -
     * @deprecated use {@link org.codehaus.plexus.util.FileUtils#copyFile(java.io.File, java.io.File)} from plexus-utils instead
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void copyFile(File inputFile, File outputFile, boolean keepLastModified) throws IOException {
        org.codehaus.plexus.util.FileUtils.copyFile(inputFile, outputFile);
        if (keepLastModified) {
            outputFile.setLastModified(inputFile.lastModified());
        }
    }

    /**
     * @deprecated use {@link org.codehaus.plexus.util.FileUtils#copyStreamToFile(org.codehaus.plexus.util.io.InputStreamFacade, java.io.File)} from plexus-utils instead
     * @param is -
     * @param outputFile -
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void copy(InputStream is, File outputFile) throws IOException {
        org.codehaus.plexus.util.FileUtils.copyStreamToFile(new RawInputStreamFacade(is), outputFile);
    }

    /**
     * @deprecated use {@link org.codehaus.plexus.util.FileUtils#copyDirectoryStructure(java.io.File, java.io.File)} from plexus-utils instead
     * @param from -
     * @param to -
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void deepCopy(File from, File to) throws IOException {
        org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(from, to);
    }

    public static void safeMove(File source, File destination, String bakSuffix) throws IOException {
        final File bakFile = new File(destination.getAbsolutePath() + bakSuffix);
        bakFile.delete();
        if (bakFile.exists()) {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(bakFile);
        }
        if (destination.exists()) {
            org.codehaus.plexus.util.FileUtils.rename(destination, bakFile);
        }
        org.codehaus.plexus.util.FileUtils.rename(source, destination);
        bakFile.delete();
        if (bakFile.exists()) {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(bakFile);
        }
    }

    /**
     * @deprecated use {@link org.codehaus.plexus.util.FileUtils#rename(java.io.File, java.io.File)} from plexus-utils instead
     * @param fromFile -
     * @param toFile -
     * @throws java.io.IOException -
     */
    @Deprecated
    public static void renameFile(File fromFile, File toFile) throws IOException {
        org.codehaus.plexus.util.FileUtils.rename(fromFile, toFile);
    }

    /**
     * Converts string representing either absolute or relative path to absolute path.
     *
     * @param base  base for computing relative paths; not used if the reference is absolute.
     * @param reference  file reference in string form
     * @return absolute file
     * @throws java.net.URISyntaxException -
     */
    public static URI absoluteURI(File base, String reference) throws URISyntaxException {
        final URI baseUri = base.getAbsoluteFile().toURI();
        final URI relUri = new URI(reference);
        return baseUri.resolve(relUri);
    }

    /**
     * Tries to compute relative URI.
     * @param base  the reference directory; if null, absolute URI is returned
     * @param file  -
     * @return relative or absolute URI
     */
    public static URI relativeURI(File base, File file) {
        if (base == null) {
            return file.getAbsoluteFile().toURI();
        } else {
            final URI baseUri = base.getAbsoluteFile().toURI();
            return baseUri.relativize(file.toURI());
        }
    }
}
