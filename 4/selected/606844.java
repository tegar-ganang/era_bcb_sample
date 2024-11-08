package com.bix.util.blizfiles.mpq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * This class encapsulates a MPQ file and all associated functionality.
 * 
 * @author squid
 */
public class MPQ {

    private long archiveOffset;

    private MPQHeader header;

    public MPQ(String filename) throws IOException, MPQException {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        this.header = new MPQHeader();
        this.archiveOffset = 0;
        while (header.read(fc) == true) {
            if (header.isValid()) {
                long offset = this.header.getHashTableOffset() + (this.header.getHashTableOffsetHigh() << 32) + this.archiveOffset;
                fc.position(offset);
                MPQHashTable ht = new MPQHashTable();
                ht.read(fc);
                break;
            }
            this.archiveOffset += 512;
            fc.position(this.archiveOffset);
        }
        fis.close();
    }

    public boolean isValid() {
        return this.header.isValid();
    }

    public String toString() {
        String s = "";
        s += "Header[" + this.header + "]";
        return s;
    }
}
