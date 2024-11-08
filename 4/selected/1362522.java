package moe.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import org.apache.http.entity.FileEntity;

public class FileChannelEntity extends FileEntity {

    public FileChannelEntity(File file, String contentType) {
        super(file, contentType);
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        FileChannel fileChannel = new FileInputStream(this.file).getChannel();
        WritableByteChannel outputChannel = Channels.newChannel(outstream);
        long fileSize = file.length(), position = 0;
        try {
            while (position < fileSize) {
                position += fileChannel.transferTo(position, fileSize, outputChannel);
            }
        } finally {
            fileChannel.close();
        }
    }
}
