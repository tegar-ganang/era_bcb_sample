package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import memory.Memory;

public class Loader {

    private static boolean binaryLoaded = false;

    private static boolean biosPresent = false;

    public static void loadBinaryFile(String Filename, boolean check, ByteBuffer mem, int address) {
        final File binary;
        FileInputStream stream;
        MappedByteBuffer bin;
        binary = new File(Filename);
        try {
            stream = new FileInputStream(binary);
        } catch (FileNotFoundException e) {
            Error.errornum = Error.FILE_NOT_FOUND;
            return;
        }
        try {
            bin = stream.getChannel().map(MapMode.READ_ONLY, 0, binary.length());
            bin.order(ByteOrder.LITTLE_ENDIAN);
        } catch (IOException e) {
            Error.errornum = Error.ioError;
            System.err.println("Error");
            return;
        }
        System.out.println("Writing to Address " + Integer.toHexString(address));
        if (check) {
            if (BinChecker.isUnscrambled(bin)) {
                System.out.println("Binary is Unscrambled");
                System.out.println("Address " + address);
                bin.rewind();
                mem.position(address);
                mem.put(bin);
            } else {
                System.out.println("Binary is Scrambled");
            }
        } else {
            bin.rewind();
            mem.position(address);
            mem.put(bin);
        }
        System.out.println("Binary Loaded  " + Filename);
    }

    private static final void printArray(byte[] array, int end) {
        for (int i = 0; i < end; i++) System.out.print(array[i]);
    }

    private static final void readFile(String name, ByteBuffer mem, int address) {
        FileInputStream stream;
        try {
            stream = new FileInputStream(name);
        } catch (FileNotFoundException e) {
            Error.errornum = Error.FILE_NOT_FOUND;
            return;
        }
        try {
            stream.getChannel().read(mem, address);
            stream.close();
        } catch (IOException e) {
            Error.errornum = Error.ioError;
            return;
        }
    }

    public static void LoadBiosFiles(Config c) {
        readFile(c.bios, Memory.bios, 0);
        if (Error.errornum != 0) return;
        readFile(c.flash, Memory.flash, 0);
    }
}
