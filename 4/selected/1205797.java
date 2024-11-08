package io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

public class IsoFS {

    private RandomAccessFile pointer;

    private int PathTableSize;

    private int LocationOfPathTable;

    private MappedByteBuffer pvd;

    public IsoFS(String name) {
        try {
            pointer = new RandomAccessFile(name, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            pvd = pointer.getChannel().map(MapMode.READ_ONLY, 16 << 11, 2048);
            PathTableSize = pvd.getInt(136);
            System.out.println("PathTableSize " + PathTableSize);
        } catch (IOException e) {
        }
    }
}
