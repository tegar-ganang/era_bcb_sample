package xxl.core.io.fat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import xxl.core.io.fat.errors.DirectoryException;
import xxl.core.io.fat.errors.FileDoesntExist;
import xxl.core.io.fat.errors.InitializationException;
import xxl.core.io.fat.errors.NotEnoughMemory;
import xxl.core.io.fat.errors.WrongFATType;
import xxl.core.io.fat.errors.WrongLength;
import xxl.core.io.fat.util.MyMath;
import xxl.core.io.fat.util.StringOperations;
import xxl.core.io.raw.RawAccess;

/**
 * This class represents a device like a floppy, partition, disk or something else.
 * Each device has a BPB (BIOS Parameter Block), a FAT (FileAllocation Table), this 
 * may be FAT12, FAT16, or Fat32, a directory, and if it's a FAT32 a FSI (File System
 * Info) as well as a Backup Boot Sector.
 */
public class FATDevice {

    /**
	 * Object of the BIOS Parameter Block.
	 */
    private BPB bpb = null;

    /**
	 * Object of the file allocation table
	 */
    private FAT fat = null;

    /**
	 * Object of the directory structure.
	 */
    private DIR directory;

    /**
	 * Byte array which can hold one sector. It's used as a tmp variable.
	 */
    private byte[] sectorBuffer;

    /**
	 * The number of root sectors of the root directory.
	 */
    protected int rootDirSectors = 0;

    /**
	 * The first data sector relative to sector with BPB on it.
	 */
    protected long firstDataSector = 0;

    /**
	 * The file name of the file system for this device. Normally this is the
	 * same name as the given to the RawAccess object.
	 */
    private String deviceName;

    /**
	 * Object of raw access to read and write from and to disk.
	 */
    protected RawAccess rawAccess;

    /** 
	 * The extension of a RandomAccessFile needs a file
	 * that exists inside the OS-file system. The file is never used,
	 * just opened read only and immediately closed again. This field
	 * only exists because of the inflexible implementation of RandomAccessFile.
	 */
    private File dummyFile;

    /**
	 * Output stream for messages.
	 */
    protected PrintStream out;

    /**
	 * A map from file names to file information for open files.
	 */
    private HashMap fileMap = new HashMap();

    /**
	 * Constant String that indicates the file mode is read only.
	 */
    public static final String FILE_MODE_READ = "r";

    /**
	 * Constant String that indicates the file mode is read and write.
	 */
    public static final String FILE_MODE_READ_WRITE = "rw";

    /**
	 * Instance of Date.
	 */
    protected static Date date = new Date();

    /**
	 * Instance of GregorianCalendar.
	 */
    protected static GregorianCalendar calendar = new GregorianCalendar();

    /**
	 * Initialize the calendar.
	 */
    static {
        calendar.setTime(date);
    }

    /**
	 * This class is used for the file information of the opened files. It holds the information about the
	 * time and day of the file, number of users, and other things.
	 */
    class FileInfo {

        /**
		 * Object of ExtendedRandomAccessFile that is open.
		 */
        ExtendedRandomAccessFile eraf;

        /**
		 * The last write time of the file.
		 */
        long lastWriteTime;

        /**
		 * The length of the file.
		 */
        long length;

        /**
		 * The number of users of the file.
		 */
        int numOfUsers;

        /**
		 * The year of the last write access, valid range from 1980 to 2107.
		 */
        int writeYear;

        /**
		 * The month of the last write access, valid range from 1 to 12.
		 */
        int writeMonth;

        /**
		 * The day of the last write access, valid range from 1 to 31.
		 */
        int writeDay;

        /**
		 * Indicates if the writeXXX variables are valid.
		 */
        boolean writeIsValid = false;

        /**
		 * The year of the last access, valid range from 1980 to 2107.
		 */
        int accessYear;

        /**
		 * The month of the last access, valid range from 1 to 12.
		 */
        int accessMonth;

        /**
		 * The day of the last access, valid range from 1 to 31.
		 */
        int accessDay;

        /**
		 * Indicates if the accessXXX variables are valid.
		 */
        boolean accessIsValid = false;

        /**
		 * Create a new instance of this object.
		 * @param eraf ExtendedRandomAccessFile object.
		 * @param length the length of the file.
		 * @param writeTime the time of the last write access.
		 */
        FileInfo(ExtendedRandomAccessFile eraf, long length, long writeTime) {
            this.eraf = eraf;
            this.length = length;
            this.lastWriteTime = writeTime;
            numOfUsers = 1;
        }

        /**
		 * Set the last write time.
		 * @param writeTime the last write time.
		 */
        void setWriteTime(long writeTime) {
            this.lastWriteTime = writeTime;
            writeIsValid = true;
        }

        /**
		 * Set the length of the file.
		 * @param length the length of the file.
		 */
        void setLength(long length) {
            this.length = length;
        }

        /**
		 * Get the last write time.
		 * @return the last write time.
		 */
        long getWriteTime() {
            return lastWriteTime;
        }

        /**
		 * Get the length of the file.
		 * @return the length of the file.
		 */
        long getLength() {
            return length;
        }

        /**
		 * Add a new user for the file.
		 */
        void addUser() {
            numOfUsers++;
        }

        /**
		 * Remove one user for the file.
		 */
        void removeUser() {
            numOfUsers--;
        }

        /**
		 * Get the number of users for the file.
		 * @return the number of users.
		 */
        int getNumberOfUsers() {
            return numOfUsers;
        }

        /**
		 * Set the last write date.
		 * @param year the year of the last write access, valid range from 1980 to 2107.
		 * @param month the month of the last write access, valid range from 1 to 12.
		 * @param day the day of the last write access, valid range from 1 to 31.
		 */
        void setWriteDate(int year, int month, int day) {
            writeYear = year;
            writeMonth = month;
            writeDay = day;
            writeIsValid = true;
        }

        /**
		 * Set the last access date.
		 * @param year the year of the last access, valid range from 1980 to 2107.
		 * @param month the month of the last access, valid range from 1 to 12.
		 * @param day the day of the last access, valid range from 1 to 31.
		 */
        void setAccessDate(int year, int month, int day) {
            accessYear = year;
            accessMonth = month;
            accessDay = day;
            accessIsValid = true;
        }

        /**
		 * Get the year of the last write access.
		 * @return the year of the last write access.
		 */
        int getWriteYear() {
            return writeYear;
        }

        /**
		 * Get the month of the last write access.
		 * @return the month of the last write access.
		 */
        int getWriteMonth() {
            return writeMonth;
        }

        /**
		 * Get the day of the last write access.
		 * @return the day of the last write access.
		 */
        int getWriteDay() {
            return writeDay;
        }

        /**
		 * Get the year of the last access.
		 * @return the year of the last access.
		 */
        int getAccessYear() {
            return accessYear;
        }

        /**
		 * Get the month of the last access.
		 * @return the month of the last access.
		 */
        int getAccessMonth() {
            return accessMonth;
        }

        /**
		 * Get the day of the last access.
		 * @return the day of the last access.
		 */
        int getAccessDay() {
            return accessDay;
        }

        /**
		 * Return true if the writeXXX variables are valid.
		 * @return true if the writeXXX variables are valid.
		 */
        boolean writeIsValid() {
            return writeIsValid;
        }

        /**
		 * Return true if the accessXXX variables are valid.
		 * @return true if the accessXXX variables are valid.
		 */
        boolean accessIsValid() {
            return accessIsValid;
        }

        /**
		 * Return the ExtendedRandomAccessFile object.
		 * @return the extendedRandomAccessFile object.
		 */
        ExtendedRandomAccessFile getFile() {
            return eraf;
        }
    }

    /**
	 * Create an instance of this object. This constructor will mount the device.
	 * @param deviceName is the name of the file.
	 * @param rawAccess is the file which supports all IO-Operations to the (real) raw device.
	 * @param out Output stream for some messages of the FATDevice. You can use System.out for
	 *	example.
	 * @param dummyFile The extension of a RandomAccessFile needs a file
	 *	that exists inside the OS-file system. The file is never used,
	 *	just opened read only and immediately closed again. This parameter
	 *	only exists because of the inflexible implementation of RandomAccessFile.
	 * @throws InitializationException in case this object couldn't be initialized.
	 */
    public FATDevice(String deviceName, RawAccess rawAccess, PrintStream out, File dummyFile) throws InitializationException {
        this.deviceName = deviceName;
        this.rawAccess = rawAccess;
        this.out = out;
        this.dummyFile = dummyFile;
        boot();
        fileMap = new HashMap();
    }

    /**
	 * Create an instance of this object. This constructor will format the device
	 * with the given fat type.
	 * For a FAT12 file system the length of rawAccess must be smaller than 4084
	 * 512-byte-blocks.
	 * For a FAT16 file system the length of rawAccess must be smaller than 4194304
	 * 512-byte-blocks and bigger than 32680 512-byte-blocks..
	 * For a FAT32 file system the length of rawAccess must be smaller than 0xFFFFFFFF
	 * bytes and bigger than 532480 512-byte-blocks.
	 * @param deviceName is the name of the file.
	 * @param fatType the type of the fat.
	 * @param rawAccess is the file which supports all IO-Operations to the (real) raw device.
	 * @param out Output stream for some messages of the FATDevice. You can use System.out for
	 *	example.
	 * @param dummyFile The extension of a RandomAccessFile needs a file
	 *	that exists inside the OS-file system. The file is never used,
	 *	just opened read only and immediately closed again. This parameter
	 *	only exists because of the inflexible implementation of RandomAccessFile.
	 * @throws WrongLength if the number of blocks of rawAccess are to big for the given fatType.
	 * @throws InitializationException in case this object couldn't be initialized.
	 */
    public FATDevice(String deviceName, byte fatType, RawAccess rawAccess, PrintStream out, File dummyFile) throws WrongLength, InitializationException {
        this.deviceName = deviceName;
        this.rawAccess = rawAccess;
        this.out = out;
        this.dummyFile = dummyFile;
        out.println("Format the device, this may take some minutes.");
        format(fatType);
        fileMap = new HashMap();
    }

    /**
	 * Format the volume with a new file system.
	 * Note: Use this method only to generate a new file system. If there's
	 * already a file system you will lose all data of it.
	 * @param fatType the type of the fat.
	 * @throws WrongLength if the number of sectors of the rawAccess is to big for the given fatType.
	 * @throws InitializationException in case of an initialization error.
	 */
    private void format(byte fatType) throws WrongLength, InitializationException {
        bpb = new BPB(this, rawAccess.getNumSectors(), fatType);
        rootDirSectors = bpb.getRootDirSectors();
        firstDataSector = bpb.getFirstDataSector();
        sectorBuffer = new byte[bpb.BytsPerSec];
        fat = new FAT(this, bpb, fatType);
        fat.initializeFat();
        directory = new DIR(this, bpb);
        out.println("Init the data sector.");
        byte[] dataBlock = new byte[bpb.BytsPerSec];
        for (int i = 0; i < 512; i++) dataBlock[i] = (byte) 0xF6;
        long numDataSectors = bpb.getNumDataSectors();
        long percent;
        long oldPercent = 0;
        for (long i = firstDataSector; i < firstDataSector + numDataSectors; i++) {
            try {
                rawAccess.write(dataBlock, i);
                percent = MyMath.roundDown(((double) i / (firstDataSector + numDataSectors)) * 100);
                if (Math.abs(oldPercent - percent) > 4) {
                    out.print(percent + "%, ");
                    oldPercent = percent;
                }
            } catch (Exception e) {
                fat.markSectorAsBad(i);
            }
        }
        out.println("File system successfully created");
    }

    /**
	 * This methods reads all important data structures of the
	 * file system from the volume.
	 * @throws InitializationException in case of an initailization error.
	 */
    private void boot() throws InitializationException {
        byte[] b = new byte[512];
        rawAccess.read(b, 0);
        bpb = new BPB(this, b);
        long FATSz = 0;
        rootDirSectors = ((bpb.RootEntCnt * 32) + (bpb.BytsPerSec - 1)) / bpb.BytsPerSec;
        if (bpb.FATSz16 != 0) FATSz = bpb.FATSz16; else FATSz = bpb.FATSz32;
        firstDataSector = bpb.RsvdSecCnt + (bpb.NumFATs * FATSz) + rootDirSectors;
        sectorBuffer = new byte[bpb.BytsPerSec];
        fat = new FAT(this, bpb);
        fat.initializeFat();
        directory = new DIR(this, bpb, bpb.getFatType());
        out.println("\n\nSuccessfully boot");
    }

    /**
	 * This operation will reinitialize the FAT and the root directory.
	 * The FAT will be cleared, only the entries that are marked as bad
	 * entries will remain. The root directory will be cleared, the only
	 * remaining entry is the entry for the root directory. Notice that
	 * this operation can only be performed to a existing device that
	 * was formatted in the past.
	 */
    public void fastFormat() {
        if (fat == null || directory == null) return;
        fat.fastFormat();
        directory = new DIR(this, bpb);
    }

    /**
	 * Return the name of the device. Normally this method is used to handle
	 * the device name with the raw access. See also {@link #getRealDeviceName()}
	 * for further information.
	 * @return name of the device.
	 */
    public String getDeviceName() {
        return StringOperations.extractFileName(deviceName);
    }

    /**
	 * Return the real name of the device. In contrast to {@link #getDeviceName()} this method
	 * returns the place and the name of the file. If you create a RAF-device you have
	 * the opportunity to store the raf-file on an other (real) device. For example:
	 * Your working directory is 'c:\xxl' you create a RAF-device with name 'fat12' on
	 * disk 'd:' in subfolder 'dir' then the real device name is 'g:\dir\fat12' but the
	 * device name that should be used operate on this file system implementation is 'fat12'
	 * only. This device name is returned by the method {@link #getDeviceName()}. 
	 * If you want to get or create a device you have to use this method because devices
	 * are unique by the (real) device name. For all other operations you should use the
	 * {@link #getDeviceName()} method since for the file system implementation it doesn't matter
	 * where the file which simulates the file system is stored.
	 * @return the real name of the device.
	 */
    public String getRealDeviceName() {
        return deviceName;
    }

    /**
	 * Read the sector given by sectorNumber.
	 * @param sectorContent the variable into which the data is read.
	 * @param sectorNumber the number where the sector should be read.
	 * @return true if the operation has succeeded.
	 */
    public boolean readSector(byte sectorContent[], long sectorNumber) {
        try {
            rawAccess.read(sectorContent, sectorNumber);
            return true;
        } catch (Exception e) {
            fat.markSectorAsBad(sectorNumber);
            out.println("FAILURE: Couldn't read from raw device. Sector: " + sectorNumber);
            return false;
        }
    }

    /**
	 * Write one sector to disk.
	 * @param sectorContent the data to write.
	 * @param sectorNumber the number where the sector should be written.
	 */
    public void writeSector(byte[] sectorContent, long sectorNumber) {
        try {
            rawAccess.write(sectorContent, sectorNumber);
        } catch (Exception e) {
            fat.markSectorAsBad(sectorNumber);
        }
    }

    /**
	 * Return the cluster given by clusterNumber as a byte array. The 
	 * clusterNumber is the direct cluster number there is no indirection
	 * with method getFirstSectorNumberOfCluster(clusterNumber).
	 * The method reads bpb.SecPerClus sectors to build the cluster, there for
	 * the clusterNumber should be the first sector number of the cluster
	 * given by getFirstSectorNumberOfCluster(clusterNumber).
	 * @param clusterNumber the number of the cluster that should be returned.
	 * @return the cluster as byte array.
	 */
    public byte[] readCluster(long clusterNumber) {
        byte[] cluster = new byte[bpb.BytsPerSec * bpb.SecPerClus];
        byte[] dataBlock = new byte[512];
        for (int i = 0; i < bpb.SecPerClus; i++) {
            try {
                rawAccess.read(dataBlock, clusterNumber + i);
            } catch (Exception e) {
                out.println(e);
                fat.markClusterAsBad(clusterNumber);
                return null;
            }
            System.arraycopy(dataBlock, 0, cluster, i * bpb.BytsPerSec, bpb.BytsPerSec);
        }
        return cluster;
    }

    /**
	 * Write a cluster to disk. The cluster is written at the given clusterNumber.
	 * @param clusterNumber the number of the cluster at which the given cluster should be written.
	 * @param clusterContent the data to write.
	 */
    public void writeCluster(long clusterNumber, byte[] clusterContent) {
        byte[] dataBlock = new byte[bpb.BytsPerSec];
        for (int i = 0; i < bpb.SecPerClus; i++) {
            System.arraycopy(clusterContent, i * bpb.BytsPerSec, dataBlock, 0, bpb.BytsPerSec);
            try {
                rawAccess.write(dataBlock, clusterNumber + i);
            } catch (Exception e) {
                fat.markClusterAsBad(clusterNumber);
            }
        }
    }

    /**
	 * Return the content of the fat at the given clusterNumber.
	 * @param clusterNumber the index in the fat.
	 * @return the fat at the given clusterNumber  
	 */
    public long getFatContent(long clusterNumber) {
        return fat.getFatContent(clusterNumber);
    }

    /**
	 * Return the content of given fat fatBuffer at index clusterNumber.
	 * @param clusterNumber the index in the fat.
	 * @param fatBuffer the whole fat as a byte array.
	 * @return the content at the index clusterNumber.
	 */
    public long getFatContent(long clusterNumber, byte[] fatBuffer) {
        return fat.getFatContent(clusterNumber, fatBuffer);
    }

    /**
	 * Check if the given clusterNumbers points to an EOC_MARK.
	 * 
     * @param clusterNumber the index in the fat.
	 * @return true is the clusterNumber equals an EOC_MARK otherwise false.
	 */
    protected boolean isLastCluster(long clusterNumber) {
        return fat.isLastCluster(clusterNumber);
    }

    /**
	 * Return the fat type.
	 * @return the fat type.
	 */
    protected byte getFatType() {
        return fat.getFatType();
    }

    /**
	 * Extends the size of a file. All new clusters that are allocated will be automatically
	 * stored in the fat started at lastClusterNumber.
	 * @param numOfClusters the number of clusters.
	 * @param lastClusterNumber the last cluster number of the file.
	 * @return a extended List.
	 * @throws NotEnoughMemory in case there is not enough memory to support
	 * this operation.
	 */
    protected List extendFileSize(long numOfClusters, long lastClusterNumber) throws NotEnoughMemory {
        return fat.getFreeClusters(numOfClusters, lastClusterNumber);
    }

    /**
	 * Mark all cluster numbers stored at the freeClustersList as free.
	 * @param freeClusterList list of cluster numbers that should be
	 * marked as free.
	 */
    public void addFreeClusters(List freeClusterList) {
        fat.addFreeClusters(freeClusterList);
    }

    /**
	 * Mark cluster numbers as free. The cluster number startClusterNumber will be set to EOC_MARK
	 * all next clusters belonging to the cluster chain will be marked as free until the EOC_MARK
	 * is reached.
	 * @param startClusterNumber the cluster number to start with.
	 */
    public void addFreeClustersMarkFirstAsEOC(long startClusterNumber) {
        fat.addFreeClustersMarkFirstAsEOC(startClusterNumber);
    }

    /**
	 * Mark cluster numbers as free beginning with the given startClusterNumber.
	 * @param startClusterNumber the cluster number to start with.
	 */
    public void addFreeClusters(long startClusterNumber) {
        fat.addFreeClusters(startClusterNumber);
    }

    /**
	 * Get a list of size numOfFreeClusters with free cluster numbers as entries.
	 * @param numOfFreeClusters the number of free clusters.
	 * @return list with free cluster numbers as entries. The list
	 * is empty, if there are not enough free clusters.
	 * @throws NotEnoughMemory in case there is not enough memory to support
	 * this operation.
	 */
    public List getFreeClusters(long numOfFreeClusters) throws NotEnoughMemory {
        return fat.getFreeClusters(numOfFreeClusters);
    }

    /**
	 * Return a list with numOfFreeClusters free cluster numbers. All this
	 * numbers are automatically stored at the fat started by the given 
	 * lastClusterNumber.
	 * @param numOfFreeClusters the number of free clusters.
	 * @param lastClusterNumber clusterNumber of the last cluster.
	 * @return a list with numOfFreeClusters free cluster numbers.
	 * @throws NotEnoughMemory in case there is not enough memory to support
	 * this operation.
	 */
    public List getFreeClusters(long numOfFreeClusters, long lastClusterNumber) throws NotEnoughMemory {
        return fat.getFreeClusters(numOfFreeClusters, lastClusterNumber);
    }

    /**
	 * Set the EOC_MARK at clusterNumber in the fat.
	 * @param clusterNumber the index where the EOC_MARK should be set.
	 */
    public void setFatEocMark(long clusterNumber) {
        fat.setFatEocMark(clusterNumber);
    }

    /**
	 * Set the given content at clusterNumber in the fat.
	 * @param clusterNumber the index in the fat.
	 * @param content the data to set.
	 */
    public void setFatContent(long clusterNumber, long content) {
        fat.setFatContent(clusterNumber, content);
    }

    /**
	 * Return the FAT object.
	 * @return the FAT object.
	 */
    protected FAT getFAT() {
        return fat;
    }

    /**
	 * Return the cluster number stored in the directory entry of the given fileName.
	 * @param fileName the name of the file.
	 * @return the cluster number
	 * @throws FileDoesntExist in case the file given by fileName doesn't exist.
	 */
    protected long getStartClusterNumber(String fileName) throws FileDoesntExist {
        return directory.getClusterNumber(fileName);
    }

    /**
	 * Return the file length stored at the directory entry for the given fileName.
	 * @param fileName the name of the file.
	 * @return the length of the file.
	 */
    protected long length(String fileName) {
        if (fileMap.get(fileName) != null) return ((FileInfo) fileMap.get(fileName)).getLength();
        return directory.length(fileName);
    }

    /**
	 * Check if the file given by fileName exists.
	 * @param fileName the name of the file.
	 * @return true if the file exists otherwise false.
	 */
    protected boolean fileExists(String fileName) {
        return directory.exists(fileName);
    }

    /**
	 * Check if the file given by fileName is a directory.
	 * @param fileName the name of the file.
	 * @return true if the file is a directory otherwise false.
	 */
    public boolean isDirectory(String fileName) {
        return directory.isDirectory(fileName);
    }

    /**
	 * Tests whether the file denoted by this fileName is a normal
	 * file.
	 * 
	 * @param fileName the name of the file.
	 * @return true if and only if the file denoted by this
	 * fileName exists and is a normal file; false otherwise.
	 */
    public boolean isFile(String fileName) {
        return directory.isFile(fileName);
    }

    /**
	 * Return true if the file given by fileName is marked as hidden.
	 * 
	 * @param fileName the name of the file.
	 * @return true if the file given by fileName is marked as hidden; false otherwise.
	 */
    public boolean isHidden(String fileName) {
        return directory.isHidden(fileName);
    }

    /**
	 * Return the last write time of the file given
	 * by the fileName. The returned value is the number of milliseconds
	 * since January 1, 1970, 00:00:00 GMT.
	 * @param fileName the name of the file.
	 * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
	 * until the last write time.
	 */
    public long getWriteTime(String fileName) {
        if (fileMap.get(fileName) != null) return ((FileInfo) fileMap.get(fileName)).getWriteTime();
        return directory.getLastWriteTime(fileName);
    }

    /**
	 * Return the creation time of the file given
	 * by the fileName. The returned value is the number of milliseconds
	 * since January 1, 1970, 00:00:00 GMT.
	 * @param fileName the name of the file.
	 * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT,
	 * until the creation time.
	 */
    public long getCreationTime(String fileName) {
        return directory.getCreationTime(fileName);
    }

    /**
	 * Return the attribute stored at the directory entry given by fileName.
	 * The returned attributes are the varaibles ATTR_XXX of the DIR-class.
	 * @param fileName the name of the file.
	 * @return the attribute of the directory entry given by fileName.
	 */
    public byte getAttribute(String fileName) {
        return directory.getAttribute(fileName);
    }

    /**
	 * Set a new length at the directory entry given by fileName.
	 * @param fileName the name of the file.
	 * @param fileLength the new length of the file.
	 * @return true if the operation was successful, otherwise false.
	 */
    public boolean writeLength(String fileName, long fileLength) {
        return writeLength(fileName, fileLength, false);
    }

    /**
	 * Set a new length at the directory entry given by fileName.
	 * @param fileName the name of the file.
	 * @param fileLength the new length of the file.
	 * @param writeThrough if set the length is written to disk
	 * otherwise the length is stored in a buffer
	 * @return true if the operation was successful, otherwise false.
	 */
    public boolean writeLength(String fileName, long fileLength, boolean writeThrough) {
        if (!writeThrough && fileMap.get(fileName) != null) {
            ((FileInfo) fileMap.get(fileName)).setLength(fileLength);
            return true;
        } else return directory.writeLength(fileName, fileLength);
    }

    /**
	 * Create file and return the start cluster number.
	 * @param fileName the name of the file.
	 * @return the cluster number stored at the directory entry given by the new file name.
	 * @throws DirectoryException in case the entry couldn't be created.
	 */
    protected long createFile(String fileName) throws DirectoryException {
        return directory.createFile(fileName);
    }

    /**
	 * Creates the file and return the start cluster number.
	 * @param fileName the name of the file.
	 * @param length the initial length of the file.
	 * @return the cluster number stored at the directory entry given by the new file fileName.
	 * @throws DirectoryException in case the entry couldn't be created.
	 */
    protected long createFile(String fileName, long length) throws DirectoryException {
        return directory.createFile(fileName, length);
    }

    /**
	 * Deletes the file or directory denoted by this abstract pathname.  If
	 * this pathname denotes a directory, then the directory must be empty in
	 * order to be deleted.
	 * 
	 * @param fileName the name of the file.
	 * @return true if and only if the file or directory is successfully
	 * deleted; false otherwise.
	 */
    public boolean delete(String fileName) {
        return directory.delete(fileName);
    }

    /**
	 * Extends the size of the file fileName with numOfClusters clusters.
	 * @param fileName the name of the file.
	 * @param numOfClusters the number of clusters the file should be extended.
	 * @return the clusterNumber of the first new allocated cluster,
	 * @throws DirectoryException in case the file size couldn't be extended.
	 */
    protected List extendFileSize(String fileName, long numOfClusters) throws DirectoryException {
        List freeClusters = fat.getFreeClusters(numOfClusters);
        long startClusterNumber = ((Long) freeClusters.get(0)).longValue();
        directory.setClusterNumber(fileName, startClusterNumber);
        return freeClusters;
    }

    /**
	 * Returns an array of strings naming the files and directories in the
	 * directory denoted by this fileName.
	 * If this fileName does not denote a directory, then this method returns
	 * null. Otherwise an array of strings is returned, one for each file or
	 * directory in the directory. Names denoting the directory itself and
	 * the directory's parent directory are not included in the result.
	 * Each string is a file name rather than a complete path.
	 * There is no guarantee that the name strings in the resulting array
	 * will appear in any specific order; they are not, in particular,
	 * guaranteed to appear in alphabetical order.
	 * @param fileName the name of the file.
	 * @return an array of strings naming the files and directories in the
	 * directory denoted by this pathname. The array will be empty if the
	 * directory is empty. Returns null if this pathname does not denote a
	 * directory, or if an I/O error occurs.
	 */
    protected String[] list(String fileName) {
        return directory.list(fileName);
    }

    /**
	 * Creates the directory named by this pathname.
	 * @param directoryName the name of the directory.
	 * @return true if and only if the directory was
	 * created; false otherwise.
	 */
    protected boolean makeDirectory(String directoryName) {
        try {
            directory.createDirectory(directoryName);
        } catch (DirectoryException e) {
            return false;
        }
        return true;
    }

    /**
	 * Renames the file denoted by fileName to newName. If the entry named by fileName
	 * is an existing file, it will be deleted and a new file/directory given by newName
	 * will be created (with all subdirectories if they don't exist). It is not possible
	 * to change the device name. In case the destination file/directory already exists, the
	 * old file will not be deleted and the renaming operation will not processed. In case the
	 * entry named by this pathname is a directory the directory is only deleted if it is
	 * empty.
	 * @param fileName the name of the file which should be renamed.
	 * @param newName the new name for the named file.
	 * @return true if and only if the renaming succeeded; false
	 * otherwise.
	 */
    protected boolean renameTo(String fileName, String newName) {
        if (fileExists(StringOperations.removeDeviceName(newName))) return false;
        FileInfo fileInfo = (FileInfo) fileMap.get(fileName);
        if (fileInfo != null && fileInfo.getNumberOfUsers() > 1) {
            out.println("source file:" + fileName + " is in use. The renaming will not be done.");
            return false;
        }
        return directory.renameTo(fileName, newName);
    }

    /**
	 * Returns an array of strings naming the files in the root
	 * directory.
	 * There is no guarantee that the name strings in the resulting array
	 * will appear in any specific order; they are not, in particular,
	 * guaranteed to appear in alphabetical order.
	 *
	 * @return  An array of strings naming the files and directories in the
	 * root directory.  The array will be empty if the directory is empty.
	 * Returns null if an I/O error occurs.
	 *
	 */
    protected String[] listRoots() {
        return directory.listRoots();
    }

    /**
	 * Sets the last-modified time of the file or directory named by fileName.
	 * @param fileName the name of the file.
	 * @param time the new last-modified time, measured in milliseconds since
	 * the epoch (00:00:00 GMT, January 1, 1970).
	 * @return true if and only if the operation succeeded; false otherwise.
	 */
    protected boolean setLastWriteTime(String fileName, long time) {
        return setLastWriteTime(fileName, time, false);
    }

    /**
	 * Sets the last-modified time of the file or directory named by fileName.
	 * @param fileName the name of the file.
	 * @param time the new last-modified time, measured in milliseconds since
	 * the epoch (00:00:00 GMT, January 1, 1970).
	 * @param writeThrough indicates if this modification should be written to
	 * disk immediately or later. If the value is true the modification is written
	 * immediately.
	 * @return true if and only if the operation succeeded; false otherwise.
	 */
    protected boolean setLastWriteTime(String fileName, long time, boolean writeThrough) {
        if (!writeThrough && fileMap.get(fileName) != null) {
            ((FileInfo) fileMap.get(fileName)).setWriteTime(time);
            return true;
        }
        return directory.setLastWriteTime(fileName, time);
    }

    /**
	 * Set the last write date of the given fileName.
	 * @param fileName the name of the file.
	 * @param year the year of the last write date, valid range from 1980 to 2107.
	 * @param month the month of the last write date, valid range from 1 to 12.
	 * @param day the day of the last write date, valid range from 1 to 31.
	 * @return true if the last date could be set; false otherwise.
	 */
    protected boolean setLastWriteDate(String fileName, int year, int month, int day) {
        return setLastWriteDate(fileName, year, month, day, false);
    }

    /**
	 * Set the last write date of the given fileName.
	 * @param fileName the name of the file.
	 * @param year the year of the last write date, valid range from 1980 to 2107.
	 * @param month the month of the last write date, valid range from 1 to 12.
	 * @param day the day of the last write date, valid range from 1 to 31.
	 * @param writeThrough indicates if this modification should be written to
	 * disk immediately or later. If the value is true the modification is written
	 * immediately.
	 * @return true if the last date could be set; false otherwise.
	 */
    protected boolean setLastWriteDate(String fileName, int year, int month, int day, boolean writeThrough) {
        if (!writeThrough && fileMap.get(fileName) != null) {
            ((FileInfo) fileMap.get(fileName)).setWriteDate(year, month, day);
            return true;
        }
        return directory.setLastWriteDate(fileName, year, month, day);
    }

    /**
	 * Set the last access date to the given fileName.
	 * @param fileName the name of the file.
	 * @param year the last access year, valid range from 1980 to 2107.
	 * @param month the last access month, valid range from 1 to 12.
	 * @param day the last access day, valid range from 1 to 31.
	 * @return true if and only if the operation is successfully
	 * done; false otherwise.
	 */
    protected boolean setLastAccessDate(String fileName, int year, int month, int day) {
        return setLastAccessDate(fileName, year, month, day, false);
    }

    /**
	 * Set the last access date to the given directory entry.
	 * @param fileName the name of the file.
	 * @param year the last access year, valid range from 1980 to 2107.
	 * @param month the last access month, valid range from 1 to 12.
	 * @param day the last access day, valid range from 1 to 31.
	 * @param writeThrough indicates if this modification should be written to
	 * disk immediately or later. If the value is true the modification is written
	 * immediately.
	 * @return true if and only if the operation is successfully
	 * done; false otherwise.
	 */
    protected boolean setLastAccessDate(String fileName, int year, int month, int day, boolean writeThrough) {
        if (!writeThrough && fileMap.get(fileName) != null) {
            ((FileInfo) fileMap.get(fileName)).setAccessDate(year, month, day);
            return true;
        }
        return directory.setLastAccessDate(fileName, year, month, day);
    }

    /**
	 * Return the number of sectors per cluster.
	 * @return the number of sectors per cluster.
	 */
    protected int getSecPerClus() {
        return bpb.SecPerClus;
    }

    /**
	 * Return the number of bytes per sector.
	 * @return the number of bytes per sector.
	 */
    protected int getBytsPerSec() {
        return bpb.BytsPerSec;
    }

    /**
	 * Return the number of sectors of the root directory.
	 * The returned value is only valid for FAT12 or FAT16 FAT'S.
	 * @return the number of sectors of the root directory.
	 */
    public long getNumRootDirSectors() {
        return bpb.getRootDirSectors();
    }

    /**
	 * Return the first root directory sector number.
	 * @return the first root directory sector number.
	 */
    protected long getFirstRootDirSector() {
        return bpb.getFirstRootDirSecNum();
    }

    /**
	 * Return the BPB object.
	 * @return the BPB object.
	 */
    protected BPB getBPB() {
        return bpb;
    }

    /**
	 * Calculates the first sector number for the given cluster number.
	 * Important: variable firstDataSector must be initialized.
	 * @param clusterNumber the number of the cluster.
	 * @return the first sector number for the cluster with the given cluster number.
	 */
    public long getFirstSectorNumberOfCluster(long clusterNumber) {
        return ((clusterNumber - 2) * bpb.SecPerClus) + firstDataSector;
    }

    /**
	 * This method can be used to exchange the data of a file from the original file system
	 * to this file system based on raw-access. The directory path to the file directoryName
	 * must exist, otherwise this operation could not be done.
	 * @param file the source file.
	 * @param directoryName the name of the destination file.
	 * @throws DirectoryException in case the copy process couldn't be done.
	 * @throws FileNotFoundException in case the file couldn't be found.
	 * @throws IOException in case an I/O error with file occurred.
	 * @throws InitializationException in case the ExtendedRandomAccessFile couldn't be initialized.
	 */
    public void copyFileToRAW(File file, String directoryName) throws DirectoryException, FileNotFoundException, IOException, InitializationException {
        directoryName = StringOperations.removeDeviceName(directoryName);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        createFile(directoryName, raf.length());
        ExtendedRandomAccessFile eraf = getRandomAccessFile(directoryName, "rw");
        int read;
        for (; ; ) {
            read = raf.read(sectorBuffer);
            if (read == -1) break;
            eraf.write(sectorBuffer, 0, read);
        }
        raf.close();
        eraf.close();
    }

    /**
	 * This method can be used to exchange the data of a file from this file system based on
	 * raw-access to the original file system.
	 * @param rawDirectoryName the name of the source file.
	 * @param originalFile the destination file.
	 * @throws DirectoryException in case the copy process couldn't be done.
	 * @throws IOException in case an I/O error with file occurred.
	 * @throws InitializationException in case the ExtendedRandomAccessFile couldn't be initialized.
	 */
    public void copyFileToOriginalFileSystem(String rawDirectoryName, File originalFile) throws DirectoryException, IOException, InitializationException {
        String directoryName = StringOperations.removeDeviceName(rawDirectoryName);
        RandomAccessFile raf = new RandomAccessFile(originalFile, "rw");
        ExtendedRandomAccessFile eraf = getRandomAccessFile(directoryName, "r");
        int read;
        for (; ; ) {
            read = eraf.read(sectorBuffer);
            if (read == -1) break;
            raf.write(sectorBuffer, 0, read);
        }
        raf.close();
        eraf.close();
    }

    /**
	 * Return an instance of ExtendedFile.
	 * 
	 * @param pathname the given pathname.
	 * @return an instance of ExtendedFile with the given pathname.
	 */
    public ExtendedFile getFile(String pathname) {
        ExtendedFile ef = new ExtendedFile(this, pathname);
        return ef;
    }

    /**
	 * Return an instance of ExtendedFile.
	 * @param parent is the path to the parent directory.
	 * @param child is the name of the file or directory.
	 * @return an instance of ExtendedFile with the given parent and 
	 * child name merged to a pathname.
	 */
    public ExtendedFile getFile(String parent, String child) {
        ExtendedFile ef = new ExtendedFile(this, parent, child);
        return ef;
    }

    /**
	 * The mode argument must either be equal to "r" or "rw", indicating
	 * that the file is to be opened for input only or for both input 
	 * and output, respectively. The write methods on this object will
	 * always throw an IOException if the file is opened with a mode of
	 * "r". If the mode is "rw" and the file does not exist, then an 
	 * attempt is made to create it. An IOException is thrown if the 
	 * name argument refers to a directory.
	 * 
	 * @param fileName the name of the file.
	 * @param mode indicates if the file is to be opened for input only
	 * or for both input and output
	 * @return an instance of ExtendedRandomAccessFile with the given fileName and mode, or
	 * null in case there is already an opened ExtendedRandomAccessFile with an other mode.
	 * @throws FileNotFoundException if the file exists but is a directory
	 * rather than a regular file, or cannot be opened or created for any other reason.
	 * @throws DirectoryException in case the ExtendedRandomAccessFile couldn't be initialized.
	 */
    public ExtendedRandomAccessFile getRandomAccessFile(String fileName, String mode) throws FileNotFoundException, DirectoryException {
        FileInfo fileInfo = (FileInfo) fileMap.get(fileName);
        if (fileInfo != null) {
            if (fileInfo.getFile().getMode().equals(FILE_MODE_READ) && mode.equals(FILE_MODE_READ_WRITE)) return null;
            fileInfo.addUser();
            return fileInfo.getFile();
        }
        ExtendedRandomAccessFile eraf = new ExtendedRandomAccessFile(this, fileName, mode, dummyFile);
        try {
            fileMap.put(fileName, new FileInfo(eraf, eraf.length(), 0));
        } catch (IOException e) {
            throw new DirectoryException("ExtendedRandomAccessFile is not accessible.");
        }
        return eraf;
    }

    /**
	 * Close the file with name fileName.
	 * @param fileName the name of the file.
	 */
    public void close(String fileName) {
        FileInfo fileInfo = (FileInfo) fileMap.get(fileName);
        if (fileInfo.getNumberOfUsers() <= 1) {
            if (fileInfo.writeIsValid()) {
                setLastWriteTime(fileName, fileInfo.getWriteTime(), true);
                setLastWriteDate(fileName, fileInfo.getWriteYear(), fileInfo.getWriteMonth(), fileInfo.getWriteDay(), true);
                setLastAccessDate(fileName, fileInfo.getAccessYear(), fileInfo.getAccessMonth(), fileInfo.getAccessDay(), true);
            } else if (fileInfo.accessIsValid()) {
                setLastAccessDate(fileName, fileInfo.getAccessYear(), fileInfo.getAccessMonth(), fileInfo.getAccessDay(), true);
            }
            fileMap.remove(fileName);
        } else fileInfo.removeUser();
    }

    /**
	 * Write the given value as a byte at the index calculated by filePointer 
	 * and sectorNumber. 
	 * 
	 * @param fileName the name of the file.
	 * @param value the data.
	 * @param filePointer the current position in a file.
	 * @param sectorNumber the current sector number the filePointer points in.
	 */
    public void writeByte(String fileName, int value, long filePointer, long sectorNumber) {
        byte b[] = new byte[512];
        readSector(b, sectorNumber);
        b[(int) (filePointer % bpb.BytsPerSec)] = (byte) value;
        writeSector(b, sectorNumber);
        setLastWriteTime(fileName, date.getTime());
        setLastWriteDate(fileName, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        setLastAccessDate(fileName, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
	 * Read a byte from the data region of a file given by filePointer and sectorNumber.
	 * @param filePointer the current position in a file.
	 * @param sectorNumber the current sector number the filePointer points in.
	 * @return the read byte
	 */
    public byte readByte(long filePointer, long sectorNumber) {
        byte b[] = new byte[512];
        readSector(b, sectorNumber);
        return b[(int) (filePointer - (filePointer / bpb.BytsPerSec) * bpb.BytsPerSec)];
    }

    /**
	 * Unmount the device. This method is called by the file system, never
	 * call this method directly.
	 */
    protected void unmount() {
        Iterator fileIterator = fileMap.values().iterator();
        while (fileIterator.hasNext()) {
            Object file = ((FileInfo) fileIterator.next()).getFile();
            try {
                ((ExtendedRandomAccessFile) file).close();
            } catch (Exception e) {
                out.println(e);
            }
        }
        fat.unmount();
        rawAccess.close();
    }

    /**
	 * Return the total number of free bytes.
	 * @return the total number of free bytes.
	 */
    public long getNumberOfFreeBytes() {
        return fat.getNumberOfFreeBytes();
    }

    /**
	 * Return the information about the BPB as String.
	 * @return information about the BPB.
	 */
    public String getBPBInfo() {
        return bpb.toString();
    }

    /**
	 * Read the BPB-sector.
	 * @return the BPB-Sector as byte array.
	 */
    public byte[] getBPBSector() {
        byte b[] = new byte[512];
        if (readSector(b, 0)) return b; else return null;
    }

    /**
	 * Return the FAT with the given fatNumber.
	 * @param fatNumber the number of the FAT.
	 * @return the FAT indicated by fatNumber.
	 */
    public byte[] getFATSectors(int fatNumber) {
        return fat.getFAT(fatNumber);
    }

    /**
	 * Return the start sector number for the given fatNumber.
	 * @param fatNumber the number of the FAT.
	 * @return the start sector number.
	 */
    public long getFATSectorNumber(int fatNumber) {
        return fat.getFATSectorNumber(fatNumber);
    }

    /**
	 * Return the FSI-sector.
	 * @return the FSI-sector as byte array.
	 * @throws WrongFATType in case the actual FAT has no FSI.
	 */
    public byte[] getFSI() throws WrongFATType {
        byte b[] = new byte[512];
        if (readSector(b, bpb.getFSInfoSectorNumber())) return b; else return null;
    }

    /**
	 * Return the sector number of the FSI-sector.
	 * @return the sector number of the FSI-sector.
	 * @throws WrongFATType in case the actual FAT has no FSI.
	 */
    public long getFSISectorNumber() throws WrongFATType {
        return bpb.getFSInfoSectorNumber();
    }

    /**
	 * Return the root directory as byte array.
	 * @return the root directory as byte array.
	 */
    public byte[] getRootDir() {
        return directory.getRootDir();
    }

    /**
	 * Return the first sector number of the root directory.
	 * @return the first sector number of the root directory.
	 */
    public long getRootSectorNumber() {
        return bpb.getFirstRootDirSecNum();
    }
}
