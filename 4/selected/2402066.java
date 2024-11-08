package com.onionnetworks.io;

import java.io.*;
import com.onionnetworks.util.*;

public class LazyRenameRAF extends FilterRAF {

    File destFile;

    public LazyRenameRAF(RAF raf) throws IOException {
        super(raf);
        if (getMode().equals("r")) {
            throw new IllegalStateException("LazyRenameRAFs are only useful " + "in read/write mode.");
        }
    }

    /**
     * If the current location and the newFile are different, it is guarenteed
     * that the file will be moved to some new location, even if it isn't the
     * final destination.  This is to allow the safe setting of deleteOnExit
     * for locations that are intended to be temporary.
     */
    public synchronized void renameTo(File newFile) throws IOException {
        this.destFile = newFile;
        if (getMode().equals("r")) {
            _raf.renameTo(destFile);
        } else {
            File newTemp = FileUtil.createTempFile(destFile);
            _raf.renameTo(newTemp);
        }
    }

    public synchronized void setReadOnly() throws IOException {
        _raf.setReadOnly();
        if (destFile != null) {
            _raf.renameTo(destFile);
        }
    }
}
