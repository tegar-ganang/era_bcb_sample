package intf.channel.item;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import net.sf.orcc.runtime.impl.IntfChannel;

public class FileInputChannel extends IntfChannel {

    private FileInputStream fileInputStream;

    public FileInputChannel(String path) {
        super(path);
        try {
            fileInputStream = new FileInputStream(file);
            channel = fileInputStream.getChannel();
            buffer.flip();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isInputShutdown() {
        try {
            long position = channel.position();
            long size = channel.size();
            int remains = buffer.remaining();
            return (position >= size) & (remains == 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.isInputShutdown();
    }

    @Override
    public Byte readByte() {
        try {
            if (buffer.remaining() == 0) {
                buffer.limit(buffer.capacity());
                channel.read(buffer);
                buffer.flip();
            }
            return buffer.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.readByte();
    }

    @Override
    public void close() {
        super.close();
        try {
            if (fileInputStream != null) fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
