package edu.virginia.cs.storagedesk.storageserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import edu.virginia.cs.storagedesk.common.Config;
import edu.virginia.cs.storagedesk.common.ISCSI;
import edu.virginia.cs.storagedesk.database.Mapping;

public class Replica {

    private static Logger logger = Logger.getLogger(Replica.class);

    private String machineID;

    private int replicaID;

    private int virtualChunkID;

    private int physicalChunkID;

    private long volumeID;

    private long maskSize;

    private File datFile;

    private RandomAccessFile raf;

    private FileChannel data;

    private ByteBuffer bytes = null;

    public Replica(Mapping map) {
        super();
        this.machineID = map.getMachineID();
        this.replicaID = map.getReplicaID();
        this.virtualChunkID = map.getVirtualChunkID();
        this.physicalChunkID = map.getPhyscialChunkID();
        this.volumeID = map.getVolumeID();
        datFile = new File(this.volumeID + File.separator + this.virtualChunkID + File.separator + this.machineID + "-" + this.physicalChunkID + ".dat");
        if (Config.STORAGEMACHINE_CHUNK_SIZE % ISCSI.DEFAULT_DISK_BLOCK_SIZE != 0) {
            logger.error("Chunk size must be a multiple of disk block size!");
            return;
        }
        this.maskSize = (long) (Config.STORAGEMACHINE_CHUNK_SIZE / ISCSI.DEFAULT_DISK_BLOCK_SIZE);
        try {
            logger.debug("File name is " + datFile.getName());
            if (datFile.exists() == false) {
                new File(this.volumeID + "").mkdir();
                new File(this.volumeID + File.separator + this.virtualChunkID).mkdir();
                datFile.createNewFile();
                logger.info("Create a new file");
            }
            if (datFile.length() != this.maskSize) {
                RandomAccessFile f = new RandomAccessFile(this.datFile, "rw");
                f.setLength(this.maskSize);
                f.close();
                logger.info("Set the file to size " + this.maskSize);
            }
            this.raf = new RandomAccessFile(this.datFile, "rwd");
            this.data = this.raf.getChannel();
            logger.info("Mask for [" + datFile.getName() + "] inits");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public boolean read(long cursor, int length) {
        cursor = cursor / ISCSI.DEFAULT_DISK_BLOCK_SIZE;
        length = (int) (length / ISCSI.DEFAULT_DISK_BLOCK_SIZE);
        try {
            bytes = ByteBuffer.allocate(length);
            int numBytes = data.read(bytes, cursor);
            logger.info("Read " + numBytes + " bytes");
            for (int i = 0; i < numBytes; i++) {
                if (bytes.get(i) != 0xf) {
                    return false;
                }
            }
        } catch (IOException ex) {
            logger.error("Read IOException happened at position " + cursor + ", length " + length);
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public void write(long cursor, int length) {
        cursor = cursor / ISCSI.DEFAULT_DISK_BLOCK_SIZE;
        length = (int) (length / ISCSI.DEFAULT_DISK_BLOCK_SIZE);
        try {
            bytes = ByteBuffer.allocate(length);
            for (int i = 0; i < length; i++) {
                bytes.put((byte) 0xf);
            }
            int numBytes = data.write(bytes, cursor);
            data.force(true);
            logger.info("Write " + numBytes + " bytes");
        } catch (IOException ex) {
            logger.error("Read IOException happened at position " + cursor + ", length " + length);
            ex.printStackTrace();
        }
    }

    public String getMachineID() {
        return machineID;
    }

    public int getReplicaID() {
        return replicaID;
    }

    public int getVirtualChunkID() {
        return virtualChunkID;
    }

    public int getPhysicalChunkID() {
        return physicalChunkID;
    }

    public long getVolumeId() {
        return volumeID;
    }
}
