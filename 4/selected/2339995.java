package net.hawk.digiextractor.digic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Logger;

/**
 * A Factory class that creates a file system object from a file. The file
 * can be either an image file or a device file. There are several detection
 * routines to determine which version of the file system should be used.
 * 
 * @author Hawk
 *
 */
public final class FileSystemFactory {

    /**
	 * The basename of windows physical drives, the drives get enumerated 
	 * starting at 0.
	 */
    private static final String WINDOWS_DRIVE_BASENAME = "\\\\.\\PhysicalDrive%d";

    /**
	 * The basename of linux physical drives, enumeration starts at "a".
	 */
    private static final String LINUX_DRIVE_BASENAME = "/dev/sd%c";

    /** The basename of MAC OS physical drives, first disk is disk0. */
    private static final String MAC_DRIVE_BASENAME = "/dev/disk%d";

    /** The number of devices that will be searched to find a DigiCorder
	 * drive.
	 */
    private static final int DEVICES_TO_TRY = 10;

    /** The Number of Sectors per cluster. */
    private static final int SECTORS_PER_CLUST = 128;

    /**
	 * The count of zeros at the beginning of the S2 TOC.
	 */
    private static final int S2TOC_ZERO_INTS = 8;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FileSystemFactory.class.getName());

    /** The Constant MAX_SEARCH. */
    private static final int MAX_SEARCH = 100;

    /**
	 * Magic value signaling a S1 Image.
	 * 
	 */
    protected static final int S1MAGIC_BYTES = 0xA28CB976;

    /**
	 * Magic value signaling a S2 Image.
	 * 
	 */
    protected static final int S2MAGIC_BYTES = 0x6EB38ADB;

    /** The Constant SPECIAL_INDEX_1. */
    private static final int S1_SPECIAL_INDEX_1 = 48000;

    /** The Constant SPECIAL_VALUE_1. */
    private static final int S1_SPECIAL_VALUE_1 = 2000;

    /** The Constant SPECIAL_INDEX_2. */
    private static final int S1_SPECIAL_INDEX_2 = 48048;

    /** The Constant SPECIAL_VALUE_2. */
    private static final int S1_SPECIAL_VALUE_2 = 2002;

    /** The number of Bytes per sector.*/
    private static final int BYTES_PER_SECTOR = 512;

    /**
	 * A simple struct to contain the version of the filesystem and the
	 * starting sector.
	 * 
	 * @author Hawk
	 *
	 */
    public static class ImageIdStruct {

        /**
		 * The filesystem version.
		 */
        private FileSystemVersion version;

        /**
		 * The startsector.
		 */
        private long startSector;

        /**
		 * Instantiates a new image id struct.
		 */
        protected ImageIdStruct() {
            throw new UnsupportedOperationException();
        }

        /**
		 * Create a new ImageIdStruct containing the version of the image and
		 * the adress of the sector where the partition starts.
		 * 
		 * @param ver the filesystem version.
		 * @param sector the sector address the partition starts on.
		 */
        protected ImageIdStruct(final FileSystemVersion ver, final long sector) {
            version = ver;
            startSector = sector;
        }
    }

    /**
     * Factory is not instantiable.
     */
    private FileSystemFactory() {
    }

    /**
     * Check the given sector for "Magic" byte sequence identifying TSD or
     * TSD_V2 volumes. If the sequence was found, a struct containing the Type
     * of the filesystem and its starting sector are returned.
     * 
     * @param file the fileChannel to read from.
     * @param start the sector to check for byte sequence.
     * @return the struct containing the type of the filesystem and the starting
     * sector.
     * @throws IOException if reading from file or device is was not successful.
     */
    private static ImageIdStruct checkForMagicBytes(final FileChannel file, final long start) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(BYTES_PER_SECTOR);
        file.read(buf, start * BYTES_PER_SECTOR);
        buf.rewind();
        reverse(buf);
        buf.rewind();
        int magic = buf.getInt();
        if (magic == S1MAGIC_BYTES) {
            LOGGER.info("TSD_V1 partition found, start at " + start);
            return new ImageIdStruct(FileSystemVersion.TSD_V1, start);
        } else if (magic == S2MAGIC_BYTES) {
            LOGGER.info("TSD_V2 partition found, start at " + start);
            return new ImageIdStruct(FileSystemVersion.TSD_V2, start);
        }
        return null;
    }

    /**
     * A function to parse the partition tables to find a PVR partition.
     * Also allow a chain of partition tables as used in extended partition
     * tables, should now work on all HD8 receivers.
     * 
     * @param file the file or device to read.
     * @return the ImageIdStruct pointing to the PVR partition if one is found
     * else returns null.
     * @throws IOException if an error occurred.
     */
    private static ImageIdStruct parsePartitionTable(final FileChannel file) throws IOException {
        List<PartitionTableEntry> table = PartitionTable.getPartitions(file);
        ImageIdStruct result = null;
        if (!table.isEmpty()) {
            LOGGER.fine("found Partition Table with " + table.size() + " entries");
            for (PartitionTableEntry e : table) {
                LOGGER.fine(e.toString());
                result = checkForMagicBytes(file, e.getSectorsPrecedingPartition());
                if (result != null) {
                    LOGGER.fine("found magic bytes");
                    return result;
                }
            }
            return null;
        } else {
            return checkForMagicBytes(file, 0);
        }
    }

    /**
     * return a new Image-Object dependend on the version of the image-file
     * or device.
     * @param file image-file or device.
     * @return a new Image-object.
     * @throws IOException in case reading from file fails.
     */
    private static AbstractPVRFileSystem primaryDetection(final File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        ImageIdStruct img = parsePartitionTable(fc);
        fis.close();
        if (img != null) {
            switch(img.version) {
                case TSD_V1:
                    return new PVRV1FileSystem(file);
                case TSD_V2:
                    return new PVRV2FileSystem(file, img.startSector);
                default:
                    LOGGER.severe("Illegal Image type detected");
            }
        }
        return null;
    }

    /**
	 * Try to find the Image by scanning the first clusters. try to determine
	 * which cluster contains the TOC.
	 * 
	 * @param file the file or device to detect
	 * @return the PVR-Image if detection succeeded else returns null.
	 * @throws IOException if an error occurred
	 */
    private static AbstractPVRFileSystem advancedDetection(final File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        ByteBuffer buf = ByteBuffer.allocateDirect(AbstractPVRFileSystem.CLUSTER_SIZE);
        LOGGER.fine("Magic value not found! starting advanced detection");
        for (int i = 1; (i < MAX_SEARCH); i++) {
            buf.rewind();
            fc.read(buf, i * AbstractPVRFileSystem.CLUSTER_SIZE);
            reverse(buf);
            if (isS1TOC(buf)) {
                LOGGER.info("Digicorder S/C/T 1 identified by " + "TOC-heuristic");
                fis.close();
                return new PVRV1FileSystem(file, i);
            }
            for (int j = 0; j < SECTORS_PER_CLUST; ++j) {
                buf.position(BYTES_PER_SECTOR * j);
                if (isS2TOC(buf)) {
                    LOGGER.info("Digicorder S/C/T 2 identified by " + "TOC-heuristic " + i + "/" + j);
                    fis.close();
                    return new PVRV2FileSystem(file, i, j);
                }
            }
        }
        LOGGER.info(file.getName() + " doesn't seem to be a TSD volume");
        fis.close();
        return null;
    }

    /**
	 * Check if the given ByteBuffer contains a S1 Table of Contents structure.
	 * TOC-Cluster is identified by the special entries pointing to the 
	 * name-list.
	 * @param buf the buffer to check.
	 * @return true if buffer contains a S1-Table of Contents.
	 */
    private static boolean isS1TOC(final ByteBuffer buf) {
        return (buf.getShort(S1_SPECIAL_INDEX_1) == S1_SPECIAL_VALUE_1) && (buf.getShort(S1_SPECIAL_INDEX_2) == S1_SPECIAL_VALUE_2);
    }

    /**
	 * Check if the given ByteBuffer contains a S2 Table of Contents structure.
	 * TOC-Cluster is identified by some characteristic sequence of bytes.
	 * 
	 * @param buf the buffer to check.
	 * @return true if buffer contains a S2-Table of Contents.
	 */
    private static boolean isS2TOC(final ByteBuffer buf) {
        boolean retVal = true;
        for (int i = 0; i < S2TOC_ZERO_INTS; i++) {
            if (buf.getInt() != 0) {
                retVal = false;
            }
        }
        if (buf.getInt() == 0) {
            retVal = false;
        }
        if (buf.getInt() != 0) {
            retVal = false;
        }
        if (buf.getInt() != buf.getInt()) {
            retVal = false;
        }
        return retVal;
    }

    /**
	 * Reverse the ByteBuffer.
	 * 
	 * @param buf the Buffer to reverse.
	 * @return the reversed Buffer.
	 */
    private static ByteBuffer reverse(final ByteBuffer buf) {
        byte tmp;
        for (int i = 0; i < buf.capacity() - 1; i += 2) {
            tmp = buf.get(i);
            buf.put(i, buf.get(i + 1));
            buf.put(i + 1, tmp);
        }
        return buf;
    }

    /**
	 * The Enum containing the different supported Operating Systems.
	 */
    private enum OperatingSystem {

        WIN, LINUX, MAC
    }

    ;

    /**
	 * Autodetect the device containing the DigiCorder data. Currently only
	 * works on windows.
	 * 
	 * @return the devicename.
	 */
    public static AbstractPVRFileSystem autodetect() {
        OperatingSystem os;
        String dev;
        if (System.getProperty("os.name").contains("Windows")) {
            dev = WINDOWS_DRIVE_BASENAME;
            os = OperatingSystem.WIN;
        } else if (System.getProperty("os.name").contains("Mac")) {
            dev = MAC_DRIVE_BASENAME;
            os = OperatingSystem.MAC;
        } else {
            dev = LINUX_DRIVE_BASENAME;
            os = OperatingSystem.LINUX;
        }
        String[] deviceNameTable = new String[DEVICES_TO_TRY];
        for (int i = 0; i < DEVICES_TO_TRY; ++i) {
            switch(os) {
                case WIN:
                case MAC:
                    deviceNameTable[i] = String.format(dev, i);
                    break;
                case LINUX:
                    char drv = 'a';
                    drv += i;
                    deviceNameTable[i] = String.format(dev, drv);
                    break;
                default:
                    break;
            }
        }
        LOGGER.fine("Starting primary detection");
        for (String name : deviceNameTable) {
            LOGGER.fine("checking " + name);
            try {
                AbstractPVRFileSystem img = primaryDetection(new File(name));
                if (img != null) {
                    return img;
                }
            } catch (IOException e) {
                LOGGER.fine("Device doesn't seem to exist: " + name);
                LOGGER.fine("Message was: " + e.getMessage());
            }
        }
        LOGGER.warning("Primary detection was not able to find a valid TSD" + " volume, starting advanced detection...");
        for (String name : deviceNameTable) {
            LOGGER.fine("checking " + name);
            try {
                AbstractPVRFileSystem img = advancedDetection(new File(name));
                if (img != null) {
                    return img;
                }
            } catch (IOException e) {
                LOGGER.fine("Device doesn't seem to exist: " + name);
                LOGGER.fine("Message was: " + e.getMessage());
            }
        }
        return null;
    }

    /**
	 * Try to create a new Image from the given file. Run primary detection 
	 * as well as advanced detection.
	 * 
	 * @param file the image-file.
	 * @return the PVR image.
	 * @throws IOException if file does not seem to be a PVR image.
	 */
    public static AbstractPVRFileSystem create(final File file) throws IOException {
        LOGGER.fine("Primary detection");
        AbstractPVRFileSystem img = primaryDetection(file);
        if (img == null) {
            LOGGER.fine("Advanced detection");
            img = advancedDetection(file);
        }
        if (img != null) {
            return img;
        }
        LOGGER.severe("File does not seem to be a TSD image.");
        throw new IOException("detection failed!");
    }
}
