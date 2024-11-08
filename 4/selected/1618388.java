package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joscar.rvcmd.SegmentedFilename;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.FileMapper;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.IncomingFileTransfer;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class IncomingFileTransferPlumberImpl extends FileTransferPlumberImpl implements IncomingFileTransferPlumber {

    private final IncomingFileTransfer transfer;

    private ReceiveFileController controller;

    public IncomingFileTransferPlumberImpl(IncomingFileTransfer transfer, ReceiveFileController controller) {
        super(controller.getStream(), transfer);
        this.transfer = transfer;
        this.controller = controller;
    }

    public TransferredFile getNativeFile(SegmentedFilename segName) throws IOException {
        return getNativeFile(segName, FileTransferHeader.MACFILEINFO_DEFAULT);
    }

    public TransferredFile getNativeFile(SegmentedFilename segName, ByteBlock macFileInfo) throws IOException {
        List<String> parts = segName.getSegments();
        File destFile;
        FileMapper fileMapper = transfer.getFileMapper();
        if (parts.size() > 0) {
            destFile = fileMapper.getDestinationFile(segName);
        } else {
            destFile = fileMapper.getUnspecifiedFilename();
        }
        TransferredFileImpl tfile = new TransferredFileImpl(destFile, segName.toNativeFilename(), "rw");
        tfile.setMacFileInfo(macFileInfo);
        return tfile;
    }

    public Transferrer createTransferrer(TransferredFile file, long startedAt, long toDownload) {
        return new FileReceiver(controller, file.getChannel(), startedAt, toDownload);
    }

    public boolean shouldAttemptResume(TransferredFile file) {
        return file.getRealFile().exists();
    }
}
