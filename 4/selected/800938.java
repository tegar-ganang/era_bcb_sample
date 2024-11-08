package org.llama.jmex.terra.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.llama.jmex.terra.MapStore;

public class MapFileUtils {

    public static ByteBuffer readFile(MapStore store, File file, int x, int y) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        try {
            int len = new DataInputStream(fin).readInt();
            ByteBuffer buf = store.createMap(x, y, len);
            buf.rewind();
            int read;
            while (len > 0) {
                read = fin.getChannel().read(buf);
                if (read == -1) {
                    throw new IOException("File is too short: " + len + " bytes left");
                }
                Thread.yield();
                len -= read;
            }
            return buf;
        } catch (IOException e) {
            throw e;
        } finally {
            fin.close();
        }
    }

    public static void saveFile(MapStore store, File file, ByteBuffer buf) throws IOException {
        FileOutputStream fout = new FileOutputStream(file);
        buf.rewind();
        try {
            int len = buf.limit();
            new DataOutputStream(fout).writeInt(len);
            while (len > 0) {
                len -= fout.getChannel().write(buf);
                Thread.yield();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            fout.close();
        }
    }

    public static String getFileName(String filename, int x, int y) {
        return filename + x + "_" + y;
    }
}
