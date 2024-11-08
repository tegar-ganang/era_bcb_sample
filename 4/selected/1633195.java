package ast.common.util;

import ast.DocumentController;
import ast.common.error.FileError;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Some file utils for copying, zipping and unzipping.
 *
 * @author Chrissyx
 */
public class FileUtils {

    /**
     * Used buffer size to read and write data.
     */
    private static final byte[] BUFFER = new byte[0xFFFF];

    /**
     * Hidden constructor to prevent instances of this class.
     */
    private FileUtils() {
    }

    /**
     * List files and folders from stated directory with custom file filter NOT
     * to get.
     *
     * @param aDir Folder to list files from
     * @param sFilter Ending to use for filtering, e.g. ".svn"
     * @return Listed files
     */
    public static File[] getFiles(final File aDir, final String sFilter) {
        return aDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(final File aFile) {
                return sFilter == null ? true : !aFile.getPath().endsWith(sFilter);
            }
        });
    }

    /**
     * Copys an existing file to a new one. If the new file already exists, it
     * will be overwritten.
     *
     * @param sFile Existing file to copy
     * @param sNewFile New copied file, will be created or overwritten
     * @throws FileError If copy operation faild due to non-existant source file,
     *                   directories or no write access for target file
     */
    public static void copy(final String sFile, final String sNewFile) throws FileError {
        FileUtils.copy(new File(sFile), new File(sNewFile));
    }

    /**
     * Copys an existing file to a new one. If the new file already exists, it
     * will be overwritten.
     *
     * @param aFile Existing file to copy
     * @param aNewFile New copied file, will be created or overwritten
     * @throws FileError If copy operation faild due to non-existant source file,
     *                   directories or no write access for target file
     */
    public static void copy(final File aFile, final File aNewFile) throws FileError {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            try {
                in = new FileInputStream(aFile);
                out = new FileOutputStream(aNewFile, false);
                in.getChannel().transferTo(0, in.getChannel().size(), out.getChannel());
            } finally {
                if (in != null) in.close();
                if (out != null) out.close();
            }
        } catch (final FileNotFoundException e) {
            throw new FileError("Can't access file!", e);
        } catch (final IOException e) {
            throw new FileError(e);
        }
    }

    /**
     * Moves recursive files from stated directory to the other one from each
     * subfolder.
     *
     * @param aFromDir Folder with files
     * @param aToDir Target directory, must be empty and should have the same
     *               structure
     * @param sFilter Ending to use for filtering, e.g. ".svn"
     */
    public static void copyOver(final File aFromDir, final File aToDir, final String sFilter) {
        final File[] filesFromDir = FileUtils.getFiles(aFromDir, sFilter);
        final File[] filesToDir = FileUtils.getFiles(aToDir, sFilter);
        for (int i = 0; i < filesFromDir.length; i++) if (filesFromDir[i].isDirectory()) FileUtils.copyOver(filesFromDir[i], filesToDir[i], sFilter); else filesFromDir[i].renameTo(new File(aToDir.getAbsolutePath() + File.separator + filesFromDir[i].getName()));
    }

    /**
     * Deletes recursive a directory with its contents and subfolders.
     *
     * @param aDir Folder to delete
     * @param sFilter Ending to use for filtering, e.g. ".svn"
     */
    public static void deleteDir(final File aDir, final String sFilter) {
        if (!aDir.delete()) {
            if (aDir.isDirectory()) for (final File curFile : FileUtils.getFiles(aDir, sFilter)) FileUtils.deleteDir(curFile, sFilter);
            if (!aDir.delete()) DocumentController.getStaticLogger().warning("Can't delete " + aDir.getAbsolutePath() + "!");
        }
    }

    /**
     * Unzips an archive file to given folder.
     *
     * @param sArchive Archive to unzip.
     * @param sDestDir Target directory for files and folders
     * @throws FileError If unzip failed
     */
    public static void unzip(final String sArchive, final String sDestDir) throws FileError {
        ZipFile odfFile;
        try {
            odfFile = new ZipFile(sArchive);
        } catch (final IOException e) {
            throw new FileError("Can't access ODF file " + sArchive + "!", e);
        }
        for (final Enumeration<? extends ZipEntry> i = odfFile.entries(); i.hasMoreElements(); ) {
            final ZipEntry curEntry = i.nextElement();
            final File curFile = new File(sDestDir, curEntry.getName());
            if (curEntry.isDirectory()) curFile.mkdirs(); else {
                new File(curFile.getParent()).mkdirs();
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    try {
                        inputStream = odfFile.getInputStream(curEntry);
                        outputStream = new FileOutputStream(curFile);
                        for (int j; (j = inputStream.read(FileUtils.BUFFER)) != -1; ) outputStream.write(FileUtils.BUFFER, 0, j);
                    } finally {
                        if (inputStream != null) inputStream.close();
                        if (outputStream != null) outputStream.close();
                    }
                } catch (final IOException e) {
                    throw new FileError("Can't unzip entry " + curEntry.getName() + "!", e);
                }
            }
        }
        DocumentController.getStaticLogger().fine(sArchive + " unzipped to " + sDestDir);
    }

    /**
     * Zips stated files to target archive file.
     *
     * @param sFiles Path of files to be zipped, should exist in same folder
     * @param sArchive Zip archive
     * @see ast.common.util.FileUtils#zip(java.io.File[], java.lang.String,
     *                                    java.net.URI, java.lang.String)
     * @throws FileError If zipping failed
     */
    public static void zip(final String[] sFiles, final String sArchive) throws FileError {
        File[] aFiles = new File[sFiles.length];
        for (int i = 0; i < sFiles.length; i++) aFiles[i] = new File(sFiles[i]);
        FileUtils.zip(aFiles, sArchive, aFiles[0].getParentFile().toURI(), "*");
    }

    /**
     * Zips an array of files and folders to target archive file. Uses a file
     * filter and root URI for correct hierarchy within the archive.
     *
     * @param aFiles Files and folders to be zipped
     * @param sArchive Zip archive
     * @param aRootURI The root path to use inside the archive
     * @param sFilter File mask to use by encountering folders, e.g. ".svn"
     * @throws FileError If zipping failed
     */
    public static void zip(final File[] aFiles, final String sArchive, final URI aRootURI, final String sFilter) throws FileError {
        FileUtils.zip(null, aFiles, sArchive, aRootURI, sFilter);
    }

    /**
     * Zips recursive with a given {@link java.util.zip.ZipOutputStream} an array
     * of files and folders to target archive file. Uses a file filter and root
     * URI for correct folder hierarchy within the archive.
     *
     * @param aOutputStream {@link java.util.zip.ZipOutputStream} to use or null
     * @param aFiles Files and folders to be zipped
     * @param sArchive Zip archive
     * @param aRootURI The root path to use inside the archive
     * @param sFilter Ending to use for not zipping files or folders by
     *                encountering (sub-)folders, e.g. ".svn"
     * @throws FileError If zipping failed
     */
    private static void zip(ZipOutputStream aOutputStream, final File[] aFiles, final String sArchive, final URI aRootURI, final String sFilter) throws FileError {
        boolean closeStream = false;
        if (aOutputStream == null) try {
            aOutputStream = new ZipOutputStream(new FileOutputStream(sArchive));
            closeStream = true;
        } catch (final FileNotFoundException e) {
            throw new FileError("Can't create ODF file!", e);
        }
        try {
            try {
                for (final File curFile : aFiles) {
                    aOutputStream.putNextEntry(new ZipEntry(URLDecoder.decode(aRootURI.relativize(curFile.toURI()).toASCIIString(), "UTF-8")));
                    if (curFile.isDirectory()) {
                        aOutputStream.closeEntry();
                        FileUtils.zip(aOutputStream, FileUtils.getFiles(curFile, sFilter), sArchive, aRootURI, sFilter);
                        continue;
                    }
                    final FileInputStream inputStream = new FileInputStream(curFile);
                    for (int i; (i = inputStream.read(FileUtils.BUFFER)) != -1; ) aOutputStream.write(FileUtils.BUFFER, 0, i);
                    inputStream.close();
                    aOutputStream.closeEntry();
                }
            } finally {
                if (closeStream && aOutputStream != null) aOutputStream.close();
            }
        } catch (final IOException e) {
            throw new FileError("Can't zip file to archive!", e);
        }
        if (closeStream) DocumentController.getStaticLogger().fine(aFiles.length + " files and folders zipped as " + sArchive);
    }
}
