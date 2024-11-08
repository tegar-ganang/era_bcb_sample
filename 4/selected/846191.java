package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.rvproto.ft.FileTransferHeader;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

public class TransferredFileImpl implements TransferredFile {

    private static final Logger LOGGER = Logger.getLogger(TransferredFileImpl.class.getName());

    @Nullable
    private final RandomAccessFile raf;

    private final long size;

    private final File file;

    private final String name;

    private final long lastmod;

    private ByteBlock macFileInfo = FileTransferHeader.MACFILEINFO_DEFAULT;

    protected TransferredFileImpl(@Nullable RandomAccessFile raf, long size, File file, String name, long lastmod) {
        this.raf = raf;
        this.size = size;
        this.file = file;
        this.name = name;
        this.lastmod = lastmod;
    }

    public TransferredFileImpl(File file, String name, String fileMode) throws IOException {
        this.file = file;
        this.name = name;
        raf = new RandomAccessFile(file, fileMode);
        size = raf.length();
        lastmod = file.lastModified();
    }

    public long getSize() {
        return size;
    }

    public void close() throws IOException {
        if (raf == null) {
            LOGGER.fine("Couldn't close " + file + " because there's no " + "RandomAccessFile set");
        } else {
            LOGGER.fine("Closing RandomAccessFile for " + file);
            raf.close();
        }
    }

    public String getTransferredName() {
        return name;
    }

    public File getRealFile() {
        return file;
    }

    public long getLastModifiedMillis() {
        return lastmod;
    }

    public FileChannel getChannel() {
        if (raf == null) {
            throw new IllegalStateException("This file does not have a " + "RandomAccessFile. It was probably created for unit testing.");
        }
        return raf.getChannel();
    }

    public ByteBlock getMacFileInfo() {
        return macFileInfo;
    }

    public void setMacFileInfo(ByteBlock macFileInfo) {
        this.macFileInfo = macFileInfo;
    }
}
