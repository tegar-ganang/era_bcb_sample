package intf.channel.item;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import net.sf.orcc.runtime.impl.IntfChannel;

public class FileOutputChannel extends IntfChannel {

    private FileOutputStream fileOutputStream;

    public FileOutputChannel(String path) {
        super(path);
        try {
            fileOutputStream = new FileOutputStream(file, false);
            channel = fileOutputStream.getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isOutputShutdown() {
        return !channel.isOpen();
    }

    @Override
    public void writeByte(Byte b) {
        try {
            buffer.put(b);
            if (!buffer.hasRemaining()) {
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (buffer.remaining() != 0) {
                buffer.flip();
                channel.write(buffer);
            }
            super.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
