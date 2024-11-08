package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers.TransferredFile;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import java.io.IOException;
import java.io.File;
import java.nio.channels.FileChannel;

class MockTransferredFile implements TransferredFile {

    private int size = MOCK_SIZE;

    public static final int MOCK_SIZE = 100;

    public void setSize(int size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void close() throws IOException {
    }

    public String getTransferredName() {
        return "transferred-file";
    }

    public File getRealFile() {
        throw new UnsupportedOperationException();
    }

    public long getLastModifiedMillis() {
        return 500;
    }

    public FileChannel getChannel() {
        throw new UnsupportedOperationException();
    }

    public ByteBlock getMacFileInfo() {
        return FileTransferHeader.MACFILEINFO_DEFAULT;
    }
}
