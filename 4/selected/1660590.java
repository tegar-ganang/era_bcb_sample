package org.susan.java.io;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class TranslationApp {

    public static void main(String args[]) throws Exception {
        File infile = new File("D:/test.txt");
        File outfile = new File("D:/data2.txt");
        RandomAccessFile inraf = new RandomAccessFile(infile, "r");
        RandomAccessFile outraf = new RandomAccessFile(outfile, "rw");
        FileChannel finc = inraf.getChannel();
        FileChannel foutc = outraf.getChannel();
        MappedByteBuffer inmbb = finc.map(FileChannel.MapMode.READ_ONLY, 0, (int) infile.length());
        Charset inCharset = Charset.forName("ISO8859-1");
        Charset outCharset = Charset.forName("utf8");
        CharsetDecoder decoder = inCharset.newDecoder();
        CharsetEncoder encoder = outCharset.newEncoder();
        CharBuffer cb = decoder.decode(inmbb);
        ByteBuffer outbb = encoder.encode(cb);
        foutc.write(outbb);
        inraf.close();
        outraf.close();
    }
}
