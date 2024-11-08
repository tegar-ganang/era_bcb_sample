package com.volantis.mcs.accessors.xml;

import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.localization.LocalizationFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Provides the ability to both read and write the contents of a zip file
 * archive in a "random access" fashion.
 * <p>
 * This is required because Java provides "serial" read and write access and 
 * "random access" for read only.
 * <p>
 * One can also think of this as a very simple read/write filesystem like 
 * interface to a zip file.
 * <p>
 * Note that this class caches all modifications until the {@link #save} 
 * method is called. 
 * <p>
 * NOTE: this class is NOT safe for multithreading. It assumes that it is only
 * accessed by one thread.
 * 
 * @todo this class could use some logging.
 */
public class ZipArchive {

    /**
     * The copyright statement.
     */
    private static String mark = "(c) Volantis Systems Ltd 2003.";

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(ZipArchive.class);

    /**
     * The zip file we are managing.
     */
    private File archiveFile;

    /**
     * The map of name -> archive entries for the individual files present
     * in the zip file.
     */
    private Map archiveEntries;

    /**
     * Create an instance of this class, which manages the zip archive file of
     * the name provided.
     * <p>
     * If the zip file does not exist, it will be created when {@link #save}
     * is called.
     * 
     * @param archiveFileName the name of the zip archive file to open.
     * @throws IOException If the file cannot be read for some reason.
     */
    public ZipArchive(String archiveFileName) throws IOException {
        this.archiveEntries = new HashMap();
        this.archiveFile = new File(archiveFileName);
        if (logger.isDebugEnabled()) {
            logger.debug("Loading archive file: " + archiveFile + ", " + new Date(archiveFile.lastModified()));
        }
        if (archiveFile.exists() && archiveFile.length() > 0) {
            ZipFile zipFile = new ZipFile(archiveFile);
            try {
                Enumeration zipEntries = zipFile.entries();
                while (zipEntries.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
                    if (!zipEntry.isDirectory()) {
                        InputStream input = zipFile.getInputStream(zipEntry);
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[2048];
                        int readBytes = input.read(buffer);
                        while (readBytes != -1) {
                            output.write(buffer, 0, readBytes);
                            readBytes = input.read(buffer);
                        }
                        ZipArchiveEntry archiveEntry = new ZipArchiveEntry(zipEntry.getName(), output.toByteArray(), zipEntry.getTime());
                        output.close();
                        input.close();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Loading archive entry: " + new Date(archiveEntry.getTime()) + " " + archiveEntry.getName());
                        }
                        archiveEntries.put(archiveEntry.getName(), archiveEntry);
                    }
                }
            } finally {
                zipFile.close();
            }
        }
    }

    /**
     * Constructs a new archive with the same entries as the specified archive.
     * 
     * @param archive the archive who's entries are to be placed in this 
     *      archive.
     */
    public ZipArchive(ZipArchive archive) {
        this.archiveEntries = new HashMap(archive.archiveEntries);
        this.archiveFile = archive.archiveFile;
    }

    /**
     * Tests to see if the file exists in the archive.
     * @param name
     * @return boolean flag
     */
    public boolean exists(String name) {
        return archiveEntries.containsKey(name);
    }

    /**
     * Retrieve an InputStream to read the content of a file in the 
     * zip archive.
     * 
     * @param name the name of the file in the zip archive.
     * @return an input stream from the created file, or null if the file
     *      does not exist in the zip archive.
     */
    public InputStream getInputFrom(String name) {
        InputStream input = null;
        ZipArchiveEntry entry = (ZipArchiveEntry) archiveEntries.get(name);
        if (entry != null) {
            input = new ByteArrayInputStream(entry.getContent());
        }
        return input;
    }

    /**
     * Retrieve an OutputStream to write the content of an file in 
     * the zip archive.
     * <p>
     * If the file does not exist, it will be created.
     * 
     * @param name the name of the file in the zip archive.
     * @return an output stream to the existing file.
     */
    public OutputStream getOutputTo(final String name) {
        return new ByteArrayOutputStream() {

            public synchronized void close() throws IOException {
                ZipArchiveEntry entry = (ZipArchiveEntry) archiveEntries.get(name);
                if (entry != null) {
                    archiveEntries.remove(name);
                }
                ZipArchiveEntry newEntry = new ZipArchiveEntry(name, toByteArray());
                archiveEntries.put(name, newEntry);
                super.close();
            }
        };
    }

    /**
     * Delete a file from the zip archive.
     * 
     * @param name the name of the file in the zip archive.
     * @return true if the file was deleted, or false it the file does not
     *      exist in the zip archive.
     */
    public boolean delete(String name) {
        return archiveEntries.remove(name) != null;
    }

    /**
     * Rename a file in the zip archive.
     * 
     * @param from the file to rename.
     * @param to the new name for the file.
     * @return true if the file was renamed, or false if not (i.e. the from 
     *      file did not exist or the to file already exists).
     */
    public boolean rename(String from, String to) {
        boolean success = false;
        ZipArchiveEntry toEntry = (ZipArchiveEntry) archiveEntries.get(to);
        if (toEntry == null) {
            ZipArchiveEntry fromEntry = (ZipArchiveEntry) archiveEntries.get(from);
            if (fromEntry != null) {
                archiveEntries.remove(from);
                toEntry = new ZipArchiveEntry(to, fromEntry.getContent());
                archiveEntries.put(to, toEntry);
                success = true;
            }
        }
        return success;
    }

    /**
     * Set the name of the ZipArchive file.
     * @param filename the full path of the proposed ZipArchive file name.
     * @throws IllegalArgumentException if the proposed filename would
     * create a ZipArchive that is not writeable.
     */
    public void setArchiveFilename(String filename) {
        File proposedFile = new File(filename);
        if (proposedFile.isDirectory()) {
            throw new IllegalArgumentException("File " + filename + " is a directory.");
        }
        if (proposedFile.exists()) {
            if (!proposedFile.canWrite()) {
                throw new IllegalArgumentException("Cannot write to file: " + filename);
            }
        } else {
            File proposedDir = proposedFile.getParentFile();
            if (!proposedDir.canWrite()) {
                throw new IllegalArgumentException("Cannot write to directory" + ": " + proposedDir.getAbsolutePath());
            }
        }
        archiveFile = proposedFile;
    }

    /**
     * Create a new version of the zip archive, with all the changes made via 
     * other methods since the last successful save operation. 
     * <p>
     * This will attempt to ensure that the operation is atomic by creating 
     * the new zip archive in a temporary file and then renaming it to the
     * proper name once it is complete.
     * <p>
     * If the zip file did not exist when the archive was created, this will
     * fail if at least one file was not added to the archive.
     *  
     * @throws IOException if there was a problem during the file operations.
     */
    public void save() throws IOException {
        File parentDirectory = archiveFile.getCanonicalFile().getParentFile();
        File tempFile = File.createTempFile("archive", null, parentDirectory);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile));
        try {
            List nameList = new ArrayList(archiveEntries.keySet());
            Collections.sort(nameList);
            Iterator names = nameList.iterator();
            while (names.hasNext()) {
                String name = (String) names.next();
                ZipArchiveEntry entry = (ZipArchiveEntry) archiveEntries.get(name);
                if (entry != null) {
                    ZipEntry zipEntry = new ZipEntry(entry.getName());
                    zipEntry.setTime(entry.getTime());
                    zos.putNextEntry(zipEntry);
                    zos.write(entry.getContent());
                    zos.closeEntry();
                }
            }
        } finally {
            zos.flush();
            zos.close();
        }
        if (archiveFile.exists()) {
            File backupArchiveFile = new File(archiveFile + ".bak");
            if (backupArchiveFile.exists()) {
                if (!backupArchiveFile.delete()) {
                    throw new IOException("Unable to delete old backup file " + backupArchiveFile);
                }
            }
            System.gc();
            if (!archiveFile.renameTo(backupArchiveFile)) {
                throw new IOException("Unable to rename archive file " + archiveFile + " to backup file " + backupArchiveFile);
            }
            if (!tempFile.renameTo(archiveFile)) {
                if (backupArchiveFile.renameTo(archiveFile)) {
                    throw new IOException("Unable to rename temporary file " + tempFile + " to new archive file " + archiveFile + ", restored old archive file.");
                } else {
                    throw new IOException("Unable to rename temporary file " + tempFile + " to archive file " + archiveFile + ", and unable to restore old archive file from " + "backup file " + backupArchiveFile + ". " + "Please record this message and contact technical " + "support.");
                }
            } else {
                backupArchiveFile.delete();
            }
        } else {
            if (!tempFile.renameTo(archiveFile)) {
                throw new IOException("Unable to rename temporary file " + tempFile + " to new archive file " + archiveFile);
            }
        }
    }

    /**
     * An entry in the files map. Vaguely corresponds to a ZipEntry, but it
     * is immutable and contains the content of the file as well.
     */
    static class ZipArchiveEntry {

        /**
         * The name of the archive entry
         */
        private String name;

        /**
         * The creation time of the archive entry
         */
        private long time;

        /**
         * The content of the archive entry.
         */
        private byte[] content;

        /**
         * Construct an entry for a new file.
         * 
         * @param name the name of the file.
         * @param content the content of the file.
         */
        ZipArchiveEntry(String name, byte[] content) {
            this.name = name;
            this.content = content;
            this.time = System.currentTimeMillis();
        }

        /**
         * Construct an entry for an existing file.
         * 
         * @param name the name of the file.
         * @param content the content of the file.
         * @param time the creation time of the file.
         */
        ZipArchiveEntry(String name, byte[] content, long time) {
            this.name = name;
            this.content = content;
            this.time = time;
        }

        /**
         * Get the name of this archive entry.
         * @return the name of this archive entry.
         */
        String getName() {
            return name;
        }

        /**
         * Get the creation time of this archive entry.
         * @return the creation time of this archive entry.
         */
        long getTime() {
            return time;
        }

        /**
         * Get the content of this archive entry
         * @return Get the content of this archive entry
         */
        byte[] getContent() {
            byte[] result = new byte[content.length];
            for (int i = 0; i < content.length; i++) {
                result[i] = content[i];
            }
            return result;
        }
    }
}
