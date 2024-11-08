package net.sf.jvibes.sandbox.jplot;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;

public class FileCreator {

    private static ByteBuffer bb = ByteBuffer.allocate(900);

    public static void main(String[] args) throws IOException {
        FileOutputStream out = new FileOutputStream("out.dat");
        FileChannel ch = out.getChannel();
        DoubleBuffer db = bb.asDoubleBuffer();
        double t = 0.0;
        for (int i = 0; i < 10000; i++, t += 0.001) {
            put(db, ch, t);
            put(db, ch, 10 * Math.sin(2 * Math.PI / 10 * t));
        }
        out.close();
    }

    private static void put(DoubleBuffer db, FileChannel ch, double t) throws IOException {
        if (!db.hasRemaining()) {
            bb.limit(8 * db.position());
            showStats(db);
            showStats(bb);
            ch.write(bb);
            bb.clear();
            db.clear();
        }
        db.put(t);
    }

    private static void showStats(Buffer bb) {
        System.out.println("capacity: " + bb.capacity());
        System.out.println("limit: " + bb.limit());
        System.out.println("position: " + bb.position());
        System.out.println("mark: " + bb.mark());
    }
}
