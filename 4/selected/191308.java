package net.sf.jvibes.sandbox;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileChannelCloseTest {

    public static void main(String[] args) throws IOException {
        FileInputStream in = new FileInputStream("out.dat");
        FileChannel fc = in.getChannel();
        fc.close();
        int n = in.read();
        in.close();
    }
}
