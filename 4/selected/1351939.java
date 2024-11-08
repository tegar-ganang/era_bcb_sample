package net.hawk.digiextractor.digic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import net.hawk.digiextractor.GUI.Configuration;
import net.hawk.digiextractor.util.DebugWriter;

/**
 * The base class for the different PVR file systems. Has some methods that are
 * used for all the different file systems.
 * 
 * @author Hawk
 *
 */
public abstract class AbstractPVRFileSystem {

    /** The Constant INT_MASK. */
    private static final long INT_MASK = 0xFFFFFFFFL;

    /**
	 * Number of pointers per cluster.
	 * (SIZEOF cluster / SIZEOF int)
	 * 65536 / 4
	 */
    private static final int POINTERCOUNT = 16384;

    /**
	 * The value used for deleted cluster entries in the cluster allocation
	 * table.
	 */
    private static final int DELETED_TAG = 0x7fffffff;

    /**
	 * The size of an int in bytes.
	 */
    private static final int INT_SIZE = 4;

    /**
	 * Error message when reading beyond end of Image.
	 *
	 */
    private static final String BEYOND_END_MSG = "Cluster 0x%X is not part of the Image, skipping";

    /**
	 * Size of Cluster.
	 * 
	 */
    protected static final int CLUSTER_SIZE = 0x10000;

    /**
	 * The offset of the pointer to the table of contents in the first sector.
	 */
    protected static final int TOC_POINTER_POS = 32;

    /**
	 * The name of this class.
	 */
    private static final String CLASS_NAME = AbstractPVRFileSystem.class.getName();

    /**
	 * The logger for this class.
	 */
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    /**
	 * File Channel for Image file.
	 */
    private final transient FileChannel imageFileChannel;

    /**
	 * FileInputStream for image file, used for reading from Image.
	 */
    private final transient FileInputStream imageInputStream;

    /**
	 * A ByteBuffer as buffer during reads from file channel.
	 */
    private final transient ByteBuffer byteBuffer = ByteBuffer.allocateDirect(CLUSTER_SIZE);

    /**
	 * A Byte Buffer used as a helper to do byte swapping.
	 */
    private final transient ByteBuffer reverseBuffer = ByteBuffer.allocateDirect(CLUSTER_SIZE);

    /**
	 * The name of the file system file, might be a device file.
	 */
    private final transient String fileName;

    /**
	 * The total Cluster Count.
	 */
    private transient long maximumClusters;

    /**
	 * The offset (in byte) needed for newer Technisat receivers.
	 * For example HD8-S
	 * 
	 */
    private transient long offset;

    /**
	 * Instantiates a new abstract digicorder image.
	 * 
	 * @param image the image
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public AbstractPVRFileSystem(final File image) throws IOException {
        fileName = image.getAbsolutePath();
        offset = 0;
        maximumClusters = image.length() / CLUSTER_SIZE;
        if (image.length() == 0) {
            maximumClusters = Long.MAX_VALUE;
        }
        imageInputStream = new FileInputStream(image);
        imageFileChannel = imageInputStream.getChannel();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        reverseBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
	 * Reads a cluster (64Kb) into the images ByteBuffer.
	 * 
	 * @param cluster the cluster to be read.
	 * @param reverse if true odd and even bytes will be swapped.
	 * 
	 * @return ByteBuffer containing the contents of the specified cluster
	 * 
	 * @throws IOException when reading from image fails.
	 */
    public final ByteBuffer readClusterIntoBuffer(final long cluster, final boolean reverse) throws IOException {
        LOGGER.entering(CLASS_NAME, "readClusterIntoBuffer", cluster);
        reverseBuffer.clear();
        byteBuffer.clear();
        ByteBuffer input;
        if (reverse) {
            input = reverseBuffer;
        } else {
            input = byteBuffer;
        }
        if (imageFileChannel.read(input, (long) cluster * CLUSTER_SIZE + offset) < CLUSTER_SIZE) {
            LOGGER.severe("No more data!, filling with zeros! Cluster: " + Long.toHexString(cluster));
            for (int i = 0; i < CLUSTER_SIZE; i++) {
                input.put(i, (byte) 0x00);
            }
        }
        if (reverse) {
            reverseBuffer.rewind();
            while (reverseBuffer.remaining() > 0) {
                byteBuffer.putShort(reverseBuffer.getShort());
            }
        }
        byteBuffer.rewind();
        LOGGER.exiting(CLASS_NAME, "readClusterIntoBuffer");
        return byteBuffer;
    }

    /**
	 * Read multiple clusters.
	 * 
	 * @param pointerToCAT the pointer to cat
	 * @param length the length
	 * 
	 * @return the byte buffer
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public final ByteBuffer readMultipleClusters(final long pointerToCAT, final long length) throws IOException {
        LOGGER.entering(CLASS_NAME, "readMultipleClusters", new Object[] { pointerToCAT, length });
        final Collection<Long> clusters = readCAT(pointerToCAT, length);
        final ByteBuffer result = ByteBuffer.allocateDirect((int) length * CLUSTER_SIZE);
        for (long i : clusters) {
            if (result.remaining() < CLUSTER_SIZE) {
                LOGGER.severe("not enugh space in buffer!");
                break;
            }
            result.put(readClusterIntoBuffer(i, (getImageVersion().equals(FileSystemVersion.TSD_V1))));
        }
        result.rewind();
        return result;
    }

    /**
	 * This Method is used to read cluster allocation tables. The tables are
	 * stored in the following way: The Address of the first cluster is stored
	 * in the Table of contents, it points to a cluster containing pointers to
	 * the Clusters containing the Data
	 * 
	 * @param address
	 *            int The Address of the Cluster containing the Table
	 * @param len
	 *            int The count of Clusters that belong to the table
	 * @return List<Integer> a list containing the Addresses of the clusters
	 *         belonging to the Video or Info BLock
	 * @throws IOException
	 *             If something goes wrong while reading from file, throw an
	 *             IOException
	 */
    public final List<Long> readCAT(final long address, final long len) throws IOException {
        LOGGER.entering(CLASS_NAME, "readCAT", new Object[] { address, len });
        final List<Long> pointersToTables = new ArrayList<Long>((int) len / POINTERCOUNT);
        readClusterIntoBuffer(address, true);
        byteBuffer.rewind();
        long entry;
        do {
            entry = byteBuffer.getInt() & INT_MASK;
            if (entry != 0) {
                if ((entry <= maximumClusters) && (entry > 0)) {
                    LOGGER.finest("cluster containing pointers: " + Long.toHexString(entry));
                    pointersToTables.add(entry);
                } else {
                    LOGGER.warning(String.format(BEYOND_END_MSG, entry));
                }
            }
        } while ((entry != 0) && (byteBuffer.remaining() >= INT_SIZE));
        LOGGER.finer("first level pointers: " + pointersToTables.toString());
        final List<Long> pointersToData = new ArrayList<Long>((int) len);
        LOGGER.fine("reading pointer tables");
        int count = 0;
        for (long i : pointersToTables) {
            readClusterIntoBuffer(i, true);
            byteBuffer.rewind();
            LOGGER.finest("reading table from cluster " + Long.toHexString(i));
            do {
                entry = byteBuffer.getInt() & INT_MASK;
                if (entry != 0) {
                    if ((entry <= maximumClusters) || (entry == DELETED_TAG)) {
                        pointersToData.add(entry);
                    } else {
                        LOGGER.warning(String.format(BEYOND_END_MSG, entry));
                    }
                }
                count++;
            } while ((byteBuffer.remaining() > 0) && (count < len));
        }
        LOGGER.exiting(CLASS_NAME, "readCAT", pointersToData.toString());
        return pointersToData;
    }

    /**
	 * get the Filename of the Image.
	 *  
	 * @return the name of the opened file
	 */
    public final String getFilename() {
        return fileName;
    }

    /**
	 * Close.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public final void close() throws IOException {
        LOGGER.entering(CLASS_NAME, "close");
        imageInputStream.close();
    }

    /**
	 * Gets the image channel.
	 * 
	 * @return the image channel
	 */
    public final FileChannel getImageChannel() {
        return imageFileChannel;
    }

    /**
	 * Gets the image version.
	 * 
	 * @return the image version
	 */
    public abstract FileSystemVersion getImageVersion();

    /**
	 * Read toc.
	 * 
	 * @return List<AbstractFile> a list containing all file entries.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public abstract List<AbstractFile> readTOC() throws IOException;

    /**
	 * Read info block.
	 * 
	 * @param pointer the pointer
	 * @param count the count
	 * 
	 * @return the byte buffer
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    public abstract ByteBuffer readInfoBlock(long pointer, long count) throws IOException;

    /**
	 * Read name list.
	 * 
	 * @param line the line in the table of contents that points to the cluster
	 * containing the list of names.
	 * 
	 * @return an Array containing all the names.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    protected final List<AbstractNameEntry> readNameList(final AbstractTableOfContentsLine line) throws IOException {
        LOGGER.entering(CLASS_NAME, "readNameList");
        final ByteBuffer nlData = readMultipleClusters(line.getPointer(), line.getSize() + 1);
        if (getImageVersion().equals(FileSystemVersion.TSD_V2)) {
            nlData.order(ByteOrder.LITTLE_ENDIAN);
        }
        if (Configuration.getInstance().getDump()) {
            DebugWriter.getInstance().dumpBuffer(nlData, "Names.dat");
        }
        List<AbstractNameEntry> namelist = NameListFactory.parseNameList(nlData, getImageVersion());
        LOGGER.exiting(CLASS_NAME, "readNameList", namelist);
        return namelist;
    }

    /**
	 * Gets the maximum clusters.
	 * 
	 * @return the maximum clusters
	 */
    public final long getMaximumClusters() {
        return maximumClusters;
    }

    /**
	 * Sets the maximum clusters.
	 * 
	 * @param max the new maximum clusters
	 */
    public final void setMaximumClusters(final long max) {
        this.maximumClusters = max;
    }

    /**
	 * Gets the offset.
	 * 
	 * @return the offset
	 */
    public final long getOffset() {
        return offset;
    }

    /**
	 * Sets the offset.
	 * 
	 * @param offs the new offset
	 */
    public final void setOffset(final long offs) {
        offset = offs;
    }
}
