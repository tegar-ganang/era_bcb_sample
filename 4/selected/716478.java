package org.nees.tivo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.HashMap;
import java.util.Properties;
import org.nees.rbnb.ArchiveUtility;

class Archive implements ArchiveInterface {

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

    public static final String DEFAULT_ARCHIVE_NAME = "ArchiveStore";

    public static final String PROPERTIES_FILE_NAME = ".properties";

    public static final String PROPERTY_VERSION = "version";

    Archive() throws ArchiveException {
        this(DEFAULT_ARCHIVE_NAME);
    }

    private String getCVSVersionString() {
        return "  CVS information... \n" + "  $Revision: 153 $\n" + "  $Date: 2007-09-24 13:10:37 -0700 (Mon, 24 Sep 2007) $\n" + "  $RCSfile: Archive.java,v $ \n";
    }

    Archive(String basePath) throws ArchiveException {
        System.out.print("Archive ");
        System.out.println(getCVSVersionString());
        archiveBase = new File(basePath);
        if (!archiveBase.exists()) throw new ArchiveException("Archive does not exist: " + basePath);
        if (!archiveBase.isDirectory()) throw new ArchiveException("Archive base path is not a directory: " + basePath);
        if (!archiveBase.canRead()) throw new ArchiveException("Arcive directory is unreabable: " + basePath);
        File[] segFiles = archiveBase.listFiles(notHiddenFilter);
        for (int i = 0; i < segFiles.length; i++) {
            addSegment(new SegmentImpl(segFiles[i].getName(), segFiles[i].getAbsolutePath()));
        }
        getProperties();
        updateForVersion();
    }

    private void loadProperties() throws IOException {
        File f = new File(archiveBase, PROPERTIES_FILE_NAME);
        if (f.exists()) {
            FileInputStream in = new FileInputStream(f);
            archiveProperties.load(in);
        }
    }

    private void saveProperties() throws IOException {
        File f = new File(archiveBase, PROPERTIES_FILE_NAME);
        if (!f.exists()) {
            f.createNewFile();
        }
        if (f.exists()) {
            FileOutputStream out = new FileOutputStream(f);
            archiveProperties.store(out, "Archive");
        }
    }

    private void getProperties() throws ArchiveException {
        boolean noFile = false;
        try {
            loadProperties();
        } catch (FileNotFoundException e) {
            noFile = true;
            e.printStackTrace();
        } catch (IOException e) {
            noFile = true;
            e.printStackTrace();
        }
        if (noFile) {
            System.out.println("Warning: the properties file (" + PROPERTIES_FILE_NAME + ") was not found. Set version as zero (0).");
            setProperty(PROPERTY_VERSION, "0");
        }
        String versionStr = archiveProperties.getProperty(PROPERTY_VERSION);
        if (versionStr == null) {
            System.out.println("No version number. Setting it to zero");
            setProperty(PROPERTY_VERSION, "0");
            versionStr = archiveProperties.getProperty(PROPERTY_VERSION);
        }
        version = Integer.parseInt(versionStr);
    }

    private void updateForVersion() throws ArchiveException {
    }

    public void updateSegmentsFromDir() {
        File[] segFiles = archiveBase.listFiles(notHiddenFilter);
        for (int i = 0; i < segFiles.length; i++) {
            try {
                if (null == getSegmentByName(segFiles[i].getName())) addSegment(new SegmentImpl(segFiles[i].getName(), segFiles[i].getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public File getBaseDir() {
        return archiveBase;
    }

    private void addSegment(SegmentImpl seg) throws ArchiveException {
        ArchiveSegmentInterface test = (ArchiveSegmentInterface) segments.get(seg.getName());
        if ((test != null) && (test instanceof SegmentImpl)) {
            if (!((SegmentImpl) test).isDeleted()) throw new ArchiveException("Attempt to overwrite existing segment = " + seg.getName() + ". Delete segment first.");
            purge(test.getName());
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
        String server1 = seg1.getProperty(seg1.PROPERTY_KEY_Server);
        String channel1 = seg1.getProperty(seg1.PROPERTY_KEY_Channel);
        String server2 = seg2.getProperty(seg2.PROPERTY_KEY_Server);
        String channel2 = seg2.getProperty(seg2.PROPERTY_KEY_Channel);
        String server = "[merge]" + server1 + "+" + server2;
        if ((server1 != null) && (server1.equals(server2))) server = server1;
        String channel = "[merge]" + channel1 + "+" + channel2;
        if ((channel1 != null) && (channel1.equals(channel2))) channel = channel1;
        String segName = segmentDir.getName();
        SegmentImpl seg = new SegmentImpl(segName, segmentDir.getAbsolutePath());
        seg.setProperty(seg.PROPERTY_KEY_Server, server);
        seg.setProperty(seg.PROPERTY_KEY_Channel, channel);
        seg.setProperty(seg.PROPERTY_KEY_Date_Created, ImageRepositoryViewer.DATE_FORMAT.format(new Date(seg.getStartTime())));
        addSegment(seg);
        return getSegmentByName(segName);
    }

    public ArchiveSegmentInterface makeNewCopyOfSegment(String fromName, String toName) throws ArchiveException, FileNotFoundException, IOException {
        ArchiveSegmentInterface seg = getSegmentByName(fromName);
        if (seg == null) return null;
        return makeNewCopyOfSegment(fromName, toName, seg.getStartTimeAsDouble(), seg.getEndTimeAsDouble());
    }

    public ArchiveSegmentInterface makeNewCopyOfSegment(String fromName, String toName, double startTimeD, double endTimeD) throws ArchiveException, FileNotFoundException, IOException {
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
            FileOutputStream out = new FileOutputStream(testFile);
            InputStream in = ((ArchiveImageInterface) array[i]).getImageInputStream();
            copy(in, out);
            out.close();
            in.close();
        }
        String server = seg.getProperty(seg.PROPERTY_KEY_Server);
        String channel = seg.getProperty(seg.PROPERTY_KEY_Channel);
        String segName = segmentDir.getName();
        SegmentImpl copyedSeg = new SegmentImpl(segName, segmentDir.getAbsolutePath());
        copyedSeg.setProperty(copyedSeg.PROPERTY_KEY_Server, server);
        copyedSeg.setProperty(copyedSeg.PROPERTY_KEY_Channel, channel);
        copyedSeg.setProperty(copyedSeg.PROPERTY_KEY_Date_Created, ImageRepositoryViewer.DATE_FORMAT.format(new Date(copyedSeg.getStartTime())));
        addSegment(copyedSeg);
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

    private void purge(String string) throws ArchiveException {
        ArchiveSegmentInterface seg = getDeletedSegmentByName(string);
        if (seg == null) return;
        File base = ((SegmentImpl) seg).theBaseDir;
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

    private class ImageCoverImpl implements ArchiveImageInterface {

        private String mimeType;

        private File file;

        private double time;

        ImageCoverImpl(File f) throws ArchiveException {
            file = f;
            try {
                time = ((double) (ArchiveUtility.makeTimeFromFilename(file))) / 1000.0;
                if (time == 0.0) throw new ArchiveException("Corupt Archive? - can not get time from file name = " + file.toString());
            } catch (Exception e) {
                throw new ArchiveException("Corupt Archive? - non-archvive name = " + file.toString());
            }
        }

        /**
         * @see org.nees.tivo.ArchiveImageCover#compareTo(org.nees.tivo.ArchiveImageCover)
         */
        public int compareTo(ArchiveImageInterface test) {
            double testTime = test.getTimeAsDouble();
            if (testTime > time) return -1;
            if (time > testTime) return 1;
            return 0;
        }

        /**
         * @see org.nees.tivo.ArchiveImageCover#compareTo(java.lang.Object)
         */
        public int compareTo(Object t) {
            if (!(t instanceof ArchiveImageInterface)) throw new ClassCastException("Test object is not of class " + "org.nees.tivo.ArchiveImageCover; found " + t.getClass().getName() + " instead.");
            return compareTo((ArchiveImageInterface) t);
        }

        public long getTime() {
            return (long) (time * 1000.0);
        }

        public InputStream getImageInputStream() {
            InputStream ret = null;
            try {
                ret = (InputStream) (new FileInputStream(file));
            } catch (FileNotFoundException e) {
                System.out.println("File not found: " + file.getAbsolutePath());
            }
            return ret;
        }

        public String getFilePath() {
            return file.getAbsolutePath();
        }

        /**
         * The MIME type of this object is always "image/jpg".
         * 
         * @see getMime
         * @see org.nees.tivo.ArchiveItemInterface#setMime(java.lang.String)
         */
        public String getMime() {
            return "image/jpg";
        }

        /**
         * @see org.nees.tivo.ArchiveItemInterface#equals(org.nees.tivo.ArchiveItemInterface)
         */
        public boolean equals(ArchiveItemInterface test) {
            if (!(test instanceof ImageCoverImpl)) return false;
            ImageCoverImpl t = (ImageCoverImpl) test;
            return equals(t);
        }

        /**
         * @see org.nees.tivo.ArchiveItemInterface#equals(org.nees.tivo.ArchiveItemInterface)
         * @see #eqauls(ArchiveItemInterface)
         */
        public boolean equals(ImageCoverImpl test) {
            return file.getAbsolutePath().equals(test.file.getAbsolutePath());
        }

        public double getTimeAsDouble() {
            return time;
        }

        public long getDuration() {
            return 0;
        }

        public double getDurationAsDouble() {
            return 0.0;
        }

        public boolean hasMultipleItems() {
            return false;
        }
    }

    private class SegmentImpl implements ArchiveSegmentInterface {

        String theBasePath, theName;

        File theBaseDir = null;

        double startTime = 0;

        double endTime = 0;

        Properties properties = new Properties();

        public SegmentImpl(String name, String base) throws ArchiveException {
            theBasePath = base;
            theName = name;
            theBaseDir = new File(theBasePath);
            if ((theBaseDir == null) || !theBaseDir.exists()) throw new ArchiveException("Invalid segment - no base directory: " + theBasePath);
            if ((theBaseDir.listFiles(notHiddenFilter) == null) || (theBaseDir.listFiles(notHiddenFilter).length == 0)) throw new ArchiveException("Invalid segment - base dir is empty: " + theBasePath);
            File prop = new File(theBaseDir, Archive.PROPERTIES_FILE_NAME);
            if (!prop.exists()) {
                try {
                    prop.createNewFile();
                } catch (IOException e) {
                    throw new ArchiveException("Properties file " + PROPERTIES_FILE_NAME + " unavailable " + "for segment = " + name + "; IOException " + e.toString());
                }
            }
            try {
                loadProperties();
                saveProperties();
            } catch (IOException e1) {
                throw new ArchiveException("Properties file " + PROPERTIES_FILE_NAME + " unavailable " + "for segment = " + name + "; IOException " + e1.toString());
            }
            getStartTimeAsDouble();
            getEndTimeAsDouble();
        }

        private void loadProperties() throws IOException {
            File f = new File(theBaseDir, PROPERTIES_FILE_NAME);
            FileInputStream in = new FileInputStream(f);
            properties.load(in);
        }

        private void saveProperties() throws IOException {
            File f = new File(theBaseDir, PROPERTIES_FILE_NAME);
            FileOutputStream out = new FileOutputStream(f);
            properties.store(out, getName());
        }

        public boolean isDeleted() {
            return (getDeletedSegmentByName(getName()) != null);
        }

        public void setName(String name) {
            theName = name;
        }

        public String getName() {
            return theName;
        }

        public String toString() {
            return getName();
        }

        public double getStartTimeAsDouble() {
            if (startTime == 0.0) {
                long time = 0;
                File bottom = ArchiveUtility.recursivlyFindLeast(theBaseDir);
                try {
                    time = ArchiveUtility.makeTimeFromFilename(bottom);
                } catch (ParseException e) {
                    System.out.println("GetStartTime parse error: " + bottom.getAbsolutePath());
                    new ArchiveException("Invalid Archive Segment - can not get startTime");
                }
                startTime = ((double) time) / 1000.0;
            }
            return startTime;
        }

        public double getEndTimeAsDouble() {
            if (endTime == 0) {
                long time = 0;
                File top = ArchiveUtility.recursivlyFindGreatest(theBaseDir);
                try {
                    time = ArchiveUtility.makeTimeFromFilename(top);
                } catch (ParseException e) {
                    System.out.println("GetEndTime parse error: " + top.getAbsolutePath());
                    new ArchiveException("Invalid Archive Segment - can not get endTime");
                }
                endTime = ((double) time) / 1000.0;
            }
            return endTime;
        }

        public ArchiveItemInterface[] getSortedArray(long startTime, long endTime) {
            File[] f = ArchiveUtility.getSortedFileArray(theBasePath, startTime, endTime);
            ArchiveImageInterface[] ret = new ArchiveImageInterface[f.length];
            for (int i = 0; i < f.length; i++) {
                try {
                    ret[i] = (ArchiveImageInterface) new ImageCoverImpl(f[i]);
                } catch (ArchiveException e) {
                    return null;
                }
            }
            return ret;
        }

        public ArchiveItemInterface getAtOrAfter(long time) {
            return getAtOrAfter(time, getStartTime(), getEndTime());
        }

        public ArchiveItemInterface getAtOrAfter(long time, long startTime, long endTime) {
            if (time < startTime) time = startTime;
            if (time > endTime) return null;
            File f = recursivlyFindFirstGE(new File(theBasePath), time);
            if (f == null) return null;
            try {
                return (ArchiveImageInterface) new ImageCoverImpl(f);
            } catch (ArchiveException e) {
                return null;
            }
        }

        public ArchiveItemInterface getAtOrBefore(long time) {
            return getAtOrBefore(time, getStartTime(), getEndTime());
        }

        public ArchiveItemInterface getAtOrBefore(long time, long startTime, long endTime) {
            if (time < startTime) return null;
            if (time > endTime) time = startTime;
            File f = recursivlyFindFirstLE(new File(theBasePath), time);
            if (f == null) return null;
            try {
                return (ArchiveImageInterface) new ImageCoverImpl(f);
            } catch (ArchiveException e) {
                return null;
            }
        }

        /**
         * @see org.nees.tivo.ArchiveSegmentInterface#compareTo(java.lang.Object)
         */
        public int compareTo(Object t) {
            if (!(t instanceof ArchiveSegmentInterface)) throw new ClassCastException("CompareTo test object is not of class " + "org.nees.tivo.ArchiveSegmentInterface; found " + t.getClass().getName() + " instead.");
            return compareTo((ArchiveSegmentInterface) t);
        }

        /**
         * @see org.nees.tivo.ArchiveSegmentInterface#compareTo(org.nees.tivo.ArchiveSegmentInterface)
         */
        public int compareTo(ArchiveSegmentInterface test) {
            return getName().compareTo(test.getName());
        }

        public void setStartTime(double theTime) {
            startTime = theTime;
        }

        public long getDuration() {
            return getStartTime() - getEndTime();
        }

        public double getDurationAsDouble() {
            return getStartTimeAsDouble() - getEndTimeAsDouble();
        }

        public boolean equals(ArchiveSegmentInterface test) {
            if (!(test instanceof SegmentImpl)) return false;
            SegmentImpl t = (SegmentImpl) test;
            return equals(t);
        }

        public boolean equals(SegmentImpl test) {
            return getName().equals(test.getName());
        }

        public long getStartTime() {
            return (long) (startTime * 1000.0);
        }

        public long getEndTime() {
            return (long) (endTime * 1000.0);
        }

        public String getProperty(String propertyKey) {
            return properties.getProperty(propertyKey);
        }

        public void setProperty(String propertyKey, String propertyValue) {
            if (propertyValue != null) properties.setProperty(propertyKey, propertyValue);
            try {
                saveProperties();
            } catch (IOException e) {
            }
        }
    }
}
