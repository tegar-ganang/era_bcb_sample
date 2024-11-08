package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import java.io.IOException;
import java.util.List;

public class OutgoingFileTransferPlumberImpl extends FileTransferPlumberImpl implements OutgoingFileTransferPlumber {

    private final OutgoingFileTransfer transfer;

    private final SendFileController controller;

    protected OutgoingFileTransferPlumberImpl(OutgoingFileTransfer transfer, SendFileController controller) {
        super(controller.getStream(), transfer);
        this.transfer = transfer;
        this.controller = controller;
    }

    public Transferrer createTransferrer(TransferredFile file, long startedAt, long toDownload) {
        return new FileSender(controller, file.getChannel(), startedAt, toDownload);
    }

    public List<TransferredFile> getFilesToTransfer() throws IOException {
        return transfer.getFiles();
    }
}
