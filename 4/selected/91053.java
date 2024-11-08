package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joscar.rvproto.ft.FileTransferHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.ChecksummerImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.StreamInfo;
import java.io.IOException;

public abstract class FileTransferPlumberImpl implements FileTransferPlumber {

    private final StreamInfo stream;

    private final FileTransfer transfer;

    protected FileTransferPlumberImpl(StreamInfo stream, FileTransfer transfer) {
        this.stream = stream;
        this.transfer = transfer;
    }

    public ChecksummerImpl getChecksummer(TransferredFile file, long len) {
        return new ChecksummerImpl(file.getChannel(), len);
    }

    public void sendHeader(FileTransferHeader outHeader) throws IOException {
        outHeader.write(stream.getOutputStream());
    }

    public FileTransferHeader readHeader() throws IOException {
        return FileTransferHeader.readHeader(stream.getInputStream());
    }
}
