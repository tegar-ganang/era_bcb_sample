package lrf.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class BBeBOutputStream {

    FileChannel fcha = null;

    MappedByteBuffer mbb = null;

    public BBeBOutputStream(File fn, int xor, int root, int num) throws IOException {
        fcha = new FileInputStream(fn).getChannel();
        mbb = fcha.map(MapMode.READ_WRITE, 0, 0);
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        mbb.put((byte) 'L');
        mbb.put((byte) 0);
        mbb.put((byte) 'R');
        mbb.put((byte) 0);
        mbb.put((byte) 'F');
        mbb.put((byte) 0);
        mbb.put((byte) 0);
        mbb.put((byte) 0);
        mbb.putShort((short) 999);
        mbb.putShort((short) xor);
        mbb.putInt(root);
        mbb.putLong(num);
        mbb.putLong(0);
        mbb.putInt(0);
        mbb.put((byte) 1);
        mbb.put((byte) 0);
        mbb.putShort((short) 170);
        mbb.putShort((short) 0);
        mbb.putShort((short) 600);
        mbb.putShort((short) 800);
        mbb.put((byte) 24);
        mbb.put((byte) 0);
        for (int i = 0; i < 20; i++) mbb.put((byte) 0);
        mbb.putInt(0);
        mbb.putInt(0);
        mbb.putShort((short) 0);
        mbb.putShort((short) 0);
        mbb.putInt(0);
        mbb.putInt(0);
    }

    public int putString(String s) throws IOException {
        mbb.putShort((short) s.length());
        byte str[] = s.getBytes("UTF-16LE");
        mbb.put(str);
        return 2 + str.length;
    }

    public int putInt(int b) throws IOException {
        mbb.putInt(b);
        return 4;
    }

    public int putShort(int b) throws IOException {
        mbb.putShort((short) b);
        return 2;
    }

    public int putByte(int b) throws IOException {
        mbb.put((byte) b);
        return 1;
    }
}
