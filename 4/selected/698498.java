package org.nees.archive.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.HashMap;
import java.util.Properties;
import org.nees.archive.inter.ArchiveException;
import org.nees.archive.inter.ArchiveImageInterface;
import org.nees.archive.inter.ArchiveInterface;
import org.nees.archive.inter.ArchiveSegmentImporter;
import org.nees.archive.inter.ArchiveSegmentInterface;
import org.nees.archive.inter.ArchiveItemInterface;
import org.nees.archive.inter.ArchiveProperty;

/**
 * The implementation of the archive interface. Implements an archive in a local
 * file space. Segments are represented as directories in the archive directory.
 * Each segment directory contains a file or files (for numeric data) or a directory
 * (for image data). There are properties files (a property is a name, value pair)
 * at the archive and segment levels.
 * 
 * @author Terry E Weymouth
 * @version $LastChangedRevision:543 $ (Source Revision number)
 */
public class Archive implements ArchiveInterface {

    private static FileFilter notHiddenFilter = new FileFilter() {

        public boolean accept(File f) {
            if (f.isHidden()) return false;
            return true;
        }
    };

    Properties archiveProperties = new Properties();

    int version;

    File archiveBase;

    HashMap segments = new HashMap();

    HashMap deletedSegments = new HashMap();

    Archive() throws ArchiveException {
        this(ArchiveUtility.DEFAULT_ARCHIVE_NAME);
    }

    public static String getVersionString() {
        return "Version information... \n" + " $LastChangedRevision:543 $\n" + " $LastChangedDate:2006-03-13 14:08:34 -0500 (Mon, 13 Mar 2006) $\n" + " $HeadURL:https://svn.nees.org/svn/telepresence/dataturbine-dev/archive/src/org/nees/archive/inter/ArchiveAudioStreamInterface.java $\n" + " $LastChangedBy:weymouth $\n";
    }

    public Archive(String basePath) throws ArchiveException {
        archiveBase = new File(basePath);
        if (!archiveBase.exists()) throw new ArchiveException("Archive does not exist: " + basePath);
        if (!archiveBase.isDirectory()) throw new ArchiveException("Archive base path is not a directory: " + basePath);
        if (!archiveBase.canRead()) throw new ArchiveException("Arcive directory is unreabable: " + basePath);
        File[] segFiles = archiveBase.listFiles(notHiddenFilter);
        System.out.print("Archive (" + archiveBase.getAbsolutePath() + "): ");
        System.out.println(getVersionString());
        ArchiveSegmentInterface seg = null;
        for (int i = 0; i < segFiles.length; i++) {
            seg = null;
            try {
                seg = SegmentFactory.createSegmentFromFile(segFiles[i]);
            } catch (Throwable t) {
                if (t instanceof ArchiveException) throw (ArchiveException) t;
                throw new ArchiveException("Error while creating segment", t);
            }
            if (seg != null) addSegment(seg);
        }
        getProperties();
        updateForVersion();
    }

    private void loadProperties() throws IOException {
        File f = new File(archiveBase, ArchiveUtility.PROPERTIES_FILE_NAME);
        if (f.exists()) {
            FileInputStream in = new FileInputStream(f);
            archiveProperties.load(in);
        }
    }

    private void saveProperties() throws IOException {
        File f = new File(archiveBase, ArchiveUtility.PROPERTIES_FILE_NAME);
        if (!f.exists()) {
            f.createNewFile();
        }
        if (f.exists()) {
            FileOutputStream out = new FileOutputStream(f);
            archiveProperties.store(out, "Archive");
        }
    }

    private void getProperties() throws ArchiveException {
        try {
            loadProperties();
        } catch (FileNotFoundException e) {
            throw new ArchiveException("Archive properties file not found", e);
        } catch (IOException e) {
            throw new ArchiveException("Archive properties file not found", e);
        }
        String versionStr = archiveProperties.getProperty(ArchiveProperty.PROPERTY_VERSION);
        if (versionStr == null) {
            System.out.println("No version number. Setting it to zero");
            setProperty(ArchiveProperty.PROPERTY_VERSION, "0");
            versionStr = archiveProperties.getProperty(ArchiveProperty.PROPERTY_VERSION);
        }
        try {
            version = Integer.parseInt(versionStr);
        } catch (Throwable t) {
            throw new ArchiveException("Error parsing archive property '" + ArchiveProperty.PROPERTY_VERSION + "'", t);
        }
    }

    private void updateForVersion() throws ArchiveException {
    }

    public void updateSegmentsFromDir() {
        File[] segFiles = archiveBase.listFiles(notHiddenFilter);
        for (int i = 0; i < segFiles.length; i++) {
            try {
                if (null == getSegmentByName(segFiles[i].getName())) addSegment(new ImageSequenceSegmentImpl(this, segFiles[i].getName(), segFiles[i].getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public File getBaseDir() {
        return archiveBase;
    }

    private void addSegment(ArchiveSegmentInterface seg) throws ArchiveException {
        ArchiveSegmentInterface test = (ArchiveSegmentInterface) segments.get(seg.getName());
        if ((test != null) && (test.equals(seg))) {
            if (!test.isDeleted()) throw new ArchiveException("Attempt to overwrite existing segment = " + seg.getName() + ". Delete segment first.");
            purge(test);
        }
        segments.put(seg.getName(), seg);
    }

    public ArchiveSegmentInterface getSegmentByName(String name) {
        return (ArchiveSegmentInterface) segments.get(name);
    }

    public ArchiveSegmentInterface getDeletedSegmentByName(String name) {
        return (ArchiveSegmentInterface) deletedSegments.get(name);
    }

    public Iterator getSegmentsIterator() {
        return segments.values().iterator();
    }

    public Vector getSegmentsVector() {
        return new Vector(segments.values());
    }

    public ArchiveSegmentInterface[] getSegmentsArray() {
        Vector v = getSegmentsVector();
        return (ArchiveSegmentInterface[]) v.toArray(new ArchiveSegmentInterface[v.size()]);
    }

    public File recursivlyFindFirstGE(File file, long time) {
        File[] fileList = file.listFiles(notHiddenFilter);
        long test;
        if (fileList == null) {
            try {
                test = ArchiveUtility.makeTimeFromFilename(file);
            } catch (Exception ignore) {
                System.out.println("Parse error: what file is this (GE)" + file.getName());
                return null;
            }
            if (test >= time) return file;
            return null;
        }
        fileList = ArchiveUtility.sortFiles(fileList);
        try {
            File mark = ArchiveUtility.recursivlyFindGreatest(fileList[fileList.length - 1]);
            test = ArchiveUtility.makeTimeFromFilename(mark);
            if (test < time) {
                return null;
            }
        } catch (Exception ignore) {
            System.out.println("Parse error: what file is this (GE)" + file.getName());
            return null;
        }
        File target;
        for (int i = 0; i < fileList.length; i++) {
            target = recursivlyFindFirstGE(fileList[i], time);
            if (target != null) return target;
        }
        System.out.println("Failed at oops (GE)! " + file.getName());
        return null;
    }

    public File recursivlyFindFirstLE(File file, long time) {
        File[] fileList = file.listFiles(notHiddenFilter);
        long test;
        if (fileList == null) {
            try {
                test = ArchiveUtility.makeTimeFromFilename(file);
            } catch (Exception ignore) {
                System.out.println("Parse error: what file is this (LE)" + file.getName());
                return null;
            }
            if (test <= time) return file;
            return null;
        }
        fileList = ArchiveUtility.sortFiles(fileList);
        try {
            File mark = ArchiveUtility.recursivlyFindLeast(fileList[0]);
            test = ArchiveUtility.makeTimeFromFilename(mark);
            if (test > time) {
                return null;
            }
        } catch (Exception ignore) {
            System.out.println("Parse error: what file is this (LE)" + file.getName());
            return null;
        }
        File target;
        for (int i = (fileList.length - 1); i > -1; i--) {
            target = recursivlyFindFirstGE(fileList[i], time);
            if (target != null) return target;
        }
        System.out.println("Failed at oops (LE)! " + file.getName());
        return null;
    }

    public String nextDefaultSegmentName() {
        int index = 1;
        String baseName = "Segment";
        String testName = baseName + ((index < 9) ? ("0" + index) : ("" + index));
        File test = new File(archiveBase, testName);
        while (test.exists()) {
            index++;
            testName = baseName + ((index < 10) ? ("0" + index) : ("" + index));
            test = new File(archiveBase, testName);
        }
        return testName;
    }

    public ArchiveSegmentInterface mergeOrderedSegments(String name, ArchiveSegmentInterface seg1, ArchiveSegmentInterface seg2) throws ArchiveException, FileNotFoundException, IOException {
        ArchiveImageInterface[] array1 = (ArchiveImageInterface[]) seg1.getSortedArray(seg1.getStartTime(), seg1.getEndTime());
        ArchiveImageInterface[] array2 = (ArchiveImageInterface[]) seg2.getSortedArray(seg2.getStartTime(), seg2.getEndTime());
        int totalFilesToCopy = array1.length + array2.length;
        File segmentDir = new File(archiveBase, name);
        int index = 0;
        while (segmentDir.exists()) {
            index++;
            segmentDir = new File(archiveBase, name + "_" + ((index < 10) ? ("0" + index) : ("" + index)));
        }
        String baseDir = segmentDir.getAbsolutePath();
        System.out.println("Merging " + array2.length + " files to segment " + segmentDir.getName());
        for (int i = 0; i < array2.length; i++) {
            File targetFile = ArchiveUtility.makePathFromTime(baseDir, array2[i].getTime());
            if (!targetFile.getParentFile().exists()) targetFile.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(targetFile);
            InputStream in = array2[i].getImageInputStream();
            copy(in, out);
            out.close();
            in.close();
        }
        System.out.println("Merging " + array1.length + " files to segment " + segmentDir.getName());
        for (int i = 0; i < array1.length; i++) {
            File testFile = ArchiveUtility.makePathFromTime(baseDir, array1[i].getTime());
            if (!testFile.getParentFile().exists()) testFile.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(testFile);
            InputStream in = array1[i].getImageInputStream();
            copy(in, out);
            out.close();
            in.close();
        }
        String server1 = seg1.getProperty(ArchiveProperty.PROPERTY_KEY_Server);
        String channel1 = seg1.getProperty(ArchiveProperty.PROPERTY_KEY_Channel);
        String server2 = seg2.getProperty(ArchiveProperty.PROPERTY_KEY_Server);
        String channel2 = seg2.getProperty(ArchiveProperty.PROPERTY_KEY_Channel);
        String server = "[merge]" + server1 + "+" + server2;
        if ((server1 != null) && (server1.equals(server2))) server = server1;
        String channel = "[merge]" + channel1 + "+" + channel2;
        if ((channel1 != null) && (channel1.equals(channel2))) channel = channel1;
        String segName = segmentDir.getName();
        ImageSequenceSegmentImpl seg = new ImageSequenceSegmentImpl(this, segName, segmentDir.getAbsolutePath());
        seg.setProperty(ArchiveProperty.PROPERTY_KEY_Server, server);
        seg.setProperty(ArchiveProperty.PROPERTY_KEY_Channel, channel);
        seg.setProperty(ArchiveProperty.PROPERTY_KEY_Date_Created, ArchiveUtility.DATE_FORMAT.format(new Date(seg.getStartTime())));
        addSegment(seg);
        return getSegmentByName(segName);
    }

    public ArchiveSegmentInterface makeNewCopyOfSegment(String fromName, String toName) throws ArchiveException {
        ArchiveSegmentInterface seg = getSegmentByName(fromName);
        if (seg == null) return null;
        return makeNewCopyOfSegment(fromName, toName, seg.getStartTimeAsDouble(), seg.getEndTimeAsDouble());
    }

    public ArchiveSegmentInterface makeNewCopyOfSegment(String fromName, String toName, double startTimeD, double endTimeD) throws ArchiveException {
        String segName = "";
        try {
            ArchiveSegmentInterface seg = getSegmentByName(fromName);
            if (seg == null) return null;
            File segmentDir = new File(archiveBase, toName);
            int index = 0;
            while (segmentDir.exists()) {
                index++;
                segmentDir = new File(archiveBase, toName + "_" + ((index < 10) ? ("0" + index) : ("" + index)));
            }
            long startTime = (long) (startTimeD * 1000.0);
            long endTime = (long) (endTimeD * 1000.0);
            ArchiveItemInterface[] array = seg.getSortedArray(startTime, endTime);
            String baseDir = segmentDir.getAbsolutePath();
            for (int i = 0; i < array.length; i++) {
                File testFile = ArchiveUtility.makePathFromTime(baseDir, array[i].getTime());
                if (!testFile.getParentFile().exists()) testFile.getParentFile().mkdirs();
                FileOutputStream out;
                out = new FileOutputStream(testFile);
                InputStream in = ((ArchiveImageInterface) array[i]).getImageInputStream();
                copy(in, out);
                out.close();
                in.close();
            }
            String server = seg.getProperty(ArchiveProperty.PROPERTY_KEY_Server);
            String channel = seg.getProperty(ArchiveProperty.PROPERTY_KEY_Channel);
            segName = segmentDir.getName();
            ImageSequenceSegmentImpl copyedSeg = new ImageSequenceSegmentImpl(this, segName, segmentDir.getAbsolutePath());
            copyedSeg.setProperty(ArchiveProperty.PROPERTY_KEY_Server, server);
            copyedSeg.setProperty(ArchiveProperty.PROPERTY_KEY_Channel, channel);
            copyedSeg.setProperty(ArchiveProperty.PROPERTY_KEY_Date_Created, ArchiveUtility.DATE_FORMAT.format(new Date(copyedSeg.getStartTime())));
            addSegment(copyedSeg);
        } catch (FileNotFoundException e) {
            throw new ArchiveException(e);
        } catch (IOException e) {
            throw new ArchiveException(e);
        }
        return getSegmentByName(segName);
    }

    private static final int BUFFER_SIZE = 10000;

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] data = new byte[BUFFER_SIZE];
        int read = 0;
        while (read >= 0) {
            read = in.read(data);
            if (read > 0) out.write(data, 0, read);
        }
    }

    public ArchiveSegmentInterface importSegment(ArchiveSegmentImporter in) {
        return null;
    }

    public String getProperty(String propertyKey) {
        return archiveProperties.getProperty(propertyKey);
    }

    public void setProperty(String propertyKey, String propertyValue) {
        archiveProperties.setProperty(propertyKey, propertyValue);
        try {
            saveProperties();
        } catch (IOException e) {
        }
    }

    public void removeSegment(String name, boolean reallyRemove) throws ArchiveException {
        ArchiveSegmentInterface seg = getSegmentByName(name);
        if (seg == null) return;
        segments.remove(name);
        deletedSegments.put(seg.getName(), seg);
        if (reallyRemove) purge(seg.getName());
    }

    public void purge(String string) throws ArchiveException {
        ArchiveSegmentInterface seg = getDeletedSegmentByName(string);
        if (seg == null) return;
        File base = ((ImageSequenceSegmentImpl) seg).theBaseDir;
        recursivelyDelete(base);
        deletedSegments.remove(seg.getName());
    }

    private void recursivelyDelete(File base) throws ArchiveException {
        if (!base.exists()) throw new ArchiveException("Attempt to delete not existent archive file = " + base.getAbsolutePath());
        if (!inArchive(base)) throw new ArchiveException("Attempt to delete file not in archive = " + base.getAbsolutePath());
        File[] files = base.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                recursivelyDelete(files[i]);
            }
        }
        if (!base.delete()) throw new ArchiveException("Could not delete file " + base.getAbsolutePath());
    }

    private boolean inArchive(File base) {
        while (base.getParentFile() != null) {
            if (base.getParentFile().getAbsolutePath().equals(archiveBase.getAbsolutePath())) return true;
            base = base.getParentFile();
        }
        return false;
    }

    public ArchiveSegmentInterface mergeSegments(String name, ArchiveSegmentInterface seg1, ArchiveSegmentInterface seg2) throws ArchiveException {
        return null;
    }

    public void removeSegment(String name) throws ArchiveException {
    }

    public void purge() {
    }

    public void purge(ArchiveSegmentInterface seg) throws ArchiveException {
    }
}
