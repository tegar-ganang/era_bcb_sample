package com.spinn3r.flatmap;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;
import static com.spinn3r.flatmap.FlatMapWriter.*;

/**
 *
 */
public abstract class BaseFlatCollection {

    public abstract int size();

    protected abstract TypeHandler getKeyTypeHandler();

    protected abstract byte[] getKeyFromPosition(int pos);

    /**
     * Perform a binary search of the key space in this flat map, return -1 if
     * the key was not found or the position of the key in the set/map if it was
     * found.
     *
     */
    protected int find(Object key) {
        TypeHandler key_type_handler = getKeyTypeHandler();
        byte[] key_data = key_type_handler.toByteArray(key);
        int low = 0;
        int high = size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            byte[] midVal = getKeyFromPosition(mid);
            int cmp = ByteArrayComparator.compare(midVal, key_data);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    protected ByteBuffer getByteBuffer(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        FileChannel channel = in.getChannel();
        MappedByteBuffer bbuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int) channel.size());
        bbuf.load();
        return bbuf;
    }

    /**
     * Given an offset, and a length, fetch the given blocks.
     */
    protected byte[] get(ByteBuffer bbuf, int offset, int length) {
        byte[] buff = new byte[length];
        for (int i = 0; i < length; ++i) {
            buff[i] = bbuf.get(offset + i);
        }
        return buff;
    }
}
