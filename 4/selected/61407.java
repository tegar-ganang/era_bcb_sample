package net.sourceforge.ivi.core.iviSimIfc;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class iviSimPluginShmIfc {

    /****************************************************************
     * iviSimPluginShmIfc()
     ****************************************************************/
    public iviSimPluginShmIfc(File shm_file) {
        try {
            d_file = new RandomAccessFile(shm_file, "r");
            FileChannel fc = d_file.getChannel();
            d_mapping = fc.map(FileChannel.MapMode.READ_ONLY, 0, MapSizeIdx + 4);
            if (readInt(HostBigEndianIdx) != 0) {
                d_isBigEndian = true;
            } else {
                d_isBigEndian = false;
            }
            int mapSize = readInt(MapSizeIdx);
            d_mapping = fc.map(FileChannel.MapMode.READ_ONLY, 0, mapSize);
        } catch (IOException e) {
            System.out.println("Failed to open file \"" + shm_file.getAbsolutePath() + "\": " + e.getMessage());
        }
    }

    /****************************************************************
     * getWaveFilename()
     ****************************************************************/
    public String getWaveFilename() {
        String name = readString(readInt(WaveFilenamePtrIdx));
        System.out.println("waveFilename=" + name);
        return readString(readInt(WaveFilenamePtrIdx));
    }

    /****************************************************************
     * getSimTime()
     ****************************************************************/
    public long getSimTime() {
        return readLong(SimTimeIdx);
    }

    /****************************************************************
     * getSimTimeResUnit()
     ****************************************************************/
    public int getSimTimeResUnit() {
        return readInt(SimResUnitIdx);
    }

    /****************************************************************
     * getSimTimeResExp()
     ****************************************************************/
    public int getSimTimeResExp() {
        return readInt(SimResExpIdx);
    }

    /****************************************************************
     * readInt
     ****************************************************************/
    private int readInt(int off) {
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            int tmp_i = d_mapping.get(i + off);
            if (d_isBigEndian) {
                ret |= ((tmp_i & 0xFF) << 8 * (3 - i));
            } else {
                ret |= ((tmp_i & 0xFF) << 8 * i);
            }
        }
        return ret;
    }

    /****************************************************************
     * readLong
     ****************************************************************/
    private long readLong(int off) {
        long ret = 0;
        for (int i = 0; i < 8; i++) {
            int tmp_i = d_mapping.get(i + off);
            if (d_isBigEndian) {
                ret |= ((tmp_i & 0xFF) << 8 * (7 - i));
            } else {
                ret |= ((tmp_i & 0xFF) << 8 * i);
            }
        }
        return ret;
    }

    /****************************************************************
     * readString
     ****************************************************************/
    private String readString(int off) {
        StringBuffer sb = new StringBuffer();
        int idx = off;
        while (idx < d_mapping.limit() && d_mapping.get(idx) != 0) {
            sb.append((char) d_mapping.get(idx));
            idx++;
        }
        return sb.toString();
    }

    /****************************************************************
     * Private Constants
     ****************************************************************/
    private static final int HostBigEndianIdx = 0;

    private static final int WaveFilenamePtrIdx = (HostBigEndianIdx + 4);

    private static final int DfioDataPtrIdx = (HostBigEndianIdx + 4);

    private static final int SimResUnitIdx = (DfioDataPtrIdx + 4);

    private static final int SimResExpIdx = (SimResUnitIdx + 4);

    private static final int SimTimeIdx = (SimResExpIdx + 4);

    private static final int MapSizeIdx = (SimTimeIdx + 8);

    /****************************************************************
     * Private Data
     ****************************************************************/
    private MappedByteBuffer d_mapping;

    private RandomAccessFile d_file;

    private boolean d_isBigEndian;
}
