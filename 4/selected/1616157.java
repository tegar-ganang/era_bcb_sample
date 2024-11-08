package net.kano.joustsim.oscar.oscar.service.icbm.dim;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

public class FileAttachment extends Attachment {

    private final File file;

    public FileAttachment(File file, String id, long length) {
        super(id, length);
        this.file = file;
    }

    public FileAttachment(File file, String id) {
        super(id, file.length());
        this.file = file;
    }

    public WritableByteChannel openForWriting() throws IOException {
        return new FileOutputStream(file).getChannel();
    }

    @Nullable
    public SelectableChannel getSelectableForWriting() {
        return null;
    }

    public ReadableByteChannel openForReading() throws FileNotFoundException {
        return new FileInputStream(file).getChannel();
    }

    public File getFile() {
        return file;
    }
}
