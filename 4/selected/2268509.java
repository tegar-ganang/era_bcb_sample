package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

class FileReceiver extends AbstractTransferrer {

    private ReceiveFileController controller;

    private final FileChannel fileChannel;

    public FileReceiver(ReceiveFileController controller, FileChannel fileChannel, long offset, long toDownload) {
        super(controller.getStream(), offset, toDownload);
        this.controller = controller;
        this.fileChannel = fileChannel;
    }

    protected boolean isCancelled() {
        return controller.shouldStop();
    }

    protected boolean waitIfPaused() {
        return controller.waitUntilUnpause();
    }

    protected long transferChunk(ReadableByteChannel readable, WritableByteChannel writable, long transferred, long remaining) throws IOException {
        return fileChannel.transferFrom(readable, offset + transferred, Math.min(1024, remaining));
    }

    protected int getSelectionKey() {
        return SelectionKey.OP_READ;
    }
}
