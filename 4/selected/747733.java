package be.ac.fundp.infonet.econf.producer;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import be.ac.fundp.infonet.econf.*;
import be.ac.fundp.infonet.econf.util.*;
import be.ac.fundp.infonet.econf.resource.*;

/**
 * The archive represents an archive bundled by eConf.
 * @author Stephane NICOLL - Infonet FUNDP
 * @version 0.2
 */
public class Archive {

    /**
     * Logging object.
     */
    private static org.apache.log4j.Category m_logCat = org.apache.log4j.Category.getInstance(Archive.class.getName());

    /**
    * The archive file.
    */
    private File archiveFile = null;

    /**
    * The outputstream used to write to the archive.
    */
    private ZipOutputStream out = null;

    /**
    * List of Slides.
    */
    private Vector roots = null;

    /**
    * Relative path of the sound file.
    */
    private String soundFilePath = null;

    /**
    * Specify whether this archive has some content or not
    */
    private boolean empty = true;

    /**
     * Constructs a new Archive with the specified path.
     * @param archivePath The full path for this archive
     * @throw IllegalStateException If the path is null or not valid
     */
    public Archive(String archivePath) throws IllegalStateException {
        if (archivePath == null) {
            throw new IllegalStateException("null value for the archive path not allowed!");
        }
        archiveFile = new File(archivePath);
        if (archiveFile.exists()) {
            throw new IllegalStateException("Archive file : " + archivePath + " exists already!");
        }
        roots = new Vector();
    }

    /**
    * Initializes this archive to write some content.
    */
    public void init() {
        try {
            if (out == null) if (empty) out = new ZipOutputStream(new FileOutputStream(archiveFile)); else load();
        } catch (IOException ioe) {
            m_logCat.error("Cannot init archive: " + archiveFile, ioe);
        }
    }

    /**
    * Closes the current archive.
    */
    public void close() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (IOException ioe) {
            m_logCat.error("Cannot close archive: " + archiveFile, ioe);
        }
    }

    /**
    * Loads the specified archive. Used when one want to add files to an existing archive.
    */
    private void load() {
        File backFile = null;
        ZipFile zipFile = null;
        Enumeration zippedFiles = null;
        ZipEntry currEntry = null;
        ZipEntry entry = null;
        try {
            String oldName = archiveFile.toString() + ".bak";
            archiveFile.renameTo(new File(oldName));
            backFile = new File(archiveFile.toString() + ".bak");
            zipFile = new ZipFile(backFile.getAbsolutePath());
            zippedFiles = zipFile.entries();
            out = new ZipOutputStream(new FileOutputStream(archiveFile));
            long presentTime = Calendar.getInstance().getTime().getTime();
            out.setMethod(out.DEFLATED);
            while (zippedFiles.hasMoreElements()) {
                currEntry = (ZipEntry) zippedFiles.nextElement();
                BufferedInputStream reader = new BufferedInputStream(zipFile.getInputStream(currEntry));
                int b;
                out.putNextEntry(new ZipEntry(currEntry.getName()));
                while ((b = reader.read()) != -1) out.write(b);
                reader.close();
                out.flush();
                out.closeEntry();
            }
            zipFile.close();
        } catch (Exception e) {
            m_logCat.error("Cannot load zip file", e);
        }
    }

    /**
    * Adds the specified file in the archive in the specified path.
    * @param f
    * The file to add
    * @param archivePath
    * The path in the archive where to store this file
    */
    public void add(File f, String archivePath) {
        if (out == null) init();
        empty = false;
        m_logCat.info("Adding " + f + " in " + archivePath);
        int b = 0;
        File currentFilePath = new File(archivePath);
        try {
            out.putNextEntry(new ZipEntry(archivePath));
            BufferedInputStream cacheIn = new BufferedInputStream(new FileInputStream(f));
            while ((b = cacheIn.read()) != -1) out.write(b);
            cacheIn.close();
            out.closeEntry();
        } catch (ZipException ze) {
            String s = ze.getMessage();
            if (s.indexOf("duplicate entry") == -1) {
                m_logCat.error("I/O error ", ze);
            }
        } catch (FileNotFoundException fe) {
            m_logCat.error("File not found ", fe);
        } catch (IOException e) {
            m_logCat.error("I/O error " + e.getMessage());
        }
    }

    /**
    * Returns the file that contains this archive.
    */
    public File getArchive() {
        return archiveFile;
    }

    /**
    * Returns the list of slides
    */
    public Vector getRootFiles() {
        return roots;
    }

    /**
    * Adds a slide to the slides list.
    * @param root
    * The HTTPRootFile to add
    */
    public void addRoot(Slide root) {
        roots.add(root);
    }

    /**
    * Adds the specified <strong>sound</strong> file in the archive in the
    * specified path and register it with the archive.
    * @param soundFile
    * The sound file to add
    * @param archivePath
    * The path in the archive where to store this file
    */
    public void addSoundFile(File soundFile, String archivePath) {
        add(soundFile, archivePath);
        soundFilePath = archivePath.replace('\\', '/');
    }

    /**
    * Returns the relative sound file path of this archive.
    */
    public String getSoundFilePath() {
        return soundFilePath;
    }

    /**
    * Clones this archive.
    * @param the new path for this archive.
    * @return a clone of this archive with the specified file name
    */
    public Archive clone(String newArchivePath) throws IllegalStateException {
        if (out != null) {
            try {
                out.close();
                out = null;
            } catch (IOException ioe) {
                m_logCat.error("Cannot close archive: " + archiveFile, ioe);
            }
        }
        Archive res = new Archive(newArchivePath);
        res.roots = roots;
        res.empty = empty;
        res.soundFilePath = soundFilePath;
        Utilities.copyFile(archiveFile, res.getArchive());
        return res;
    }

    /**
    * Inits the slide list.
    */
    public void initSlideList(HTTPRootFile f) {
        m_logCat.info("Init slide list with a blank content at http://blank/");
        long msec = f.getRequestedTime().getTime() - SessionManager.getSessionStartTime().getTime();
        m_logCat.info("Blank content has " + msec + "msec");
        Slide s = new Slide("blank.html", msec, "ms");
        addRoot(s);
    }
}
