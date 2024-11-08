package io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

public class IsoFileSystem {

    private RandomAccessFile pointer;

    private int PathTableSize;

    private int LocationOfPathTable;

    private int SectorSize;

    private static final int PathTableSizeInfo = 136;

    private static final int SectorSizeInfo = 130;

    private static final int PathTableLocationInfo = 148;

    private MappedByteBuffer pvd;

    public IsoFileSystem(String name) {
        try {
            pointer = new RandomAccessFile(name, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            pvd = pointer.getChannel().map(MapMode.READ_ONLY, 16 << 11, 2048);
            PathTableSize = pvd.getInt(PathTableSizeInfo);
            System.out.println("PathTableSize " + PathTableSize);
            LocationOfPathTable = pvd.getInt(PathTableLocationInfo);
            System.out.println(LocationOfPathTable);
            pointer.seek((LocationOfPathTable << 11));
            System.out.println(pointer.readUnsignedByte());
            System.out.print(pvd.getShort(SectorSizeInfo));
        } catch (IOException e) {
        }
    }
}
