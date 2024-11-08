package org.parallelj.tools.maven.ui.workarounds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.parallelj.tools.maven.ui.MavenActivator;

/**
 * This class is a workaround to a maven-eclipse-plugin bug.
 * 
 * Actually, and till version 2.5, the writing in files will be using
 * ISO-8859-1, whereas Eclipse files are supposed to be encoded in UTF-8.
 * 
 * This class provides method to a file, to know if there is a problem with a
 * not compatible sequence in UTF-8, and if a not compatible sequence is found,
 * the file is fully rewritten using the right encoding.
 * 
 */
public class FileEncoder {

    private static final String WRONG_ENCODING = "ISO-8859-1";

    private static final String RIGHT_ENCODING = "UTF-8";

    public static void checkFileEncoding(IFile file) {
        checkFileEncoding(file.getLocation());
    }

    public static void checkFileEncoding(IPath path) {
        try {
            File file = new File(path.toOSString());
            if (file == null || !file.exists()) return;
            Reader rightReader = new InputStreamReader(new FileInputStream(file), RIGHT_ENCODING);
            Reader wrongReader = new InputStreamReader(new FileInputStream(file), WRONG_ENCODING);
            boolean hasRightFailingChar = false;
            boolean hasWrongFailingChar = false;
            while (rightReader.ready() && wrongReader.ready() && !hasWrongFailingChar && !hasRightFailingChar) {
                int rightChar = rightReader.read();
                int wrongChar = wrongReader.read();
                hasRightFailingChar = (rightChar < 1 || rightChar > 255);
                hasWrongFailingChar = (wrongChar < 1 || wrongChar > 255);
            }
            rightReader.close();
            wrongReader.close();
            if (hasRightFailingChar && !hasWrongFailingChar) rewriteFile(path);
        } catch (UnsupportedEncodingException e) {
            MavenActivator.log("An exception has been thrown while rewriting files", e);
        } catch (IOException e) {
            MavenActivator.log("An exception has been thrown while rewriting files", e);
        }
    }

    private static void rewriteFile(IPath path) throws IOException {
        File infile = new File(path.toOSString());
        File outfile = new File(path.toOSString());
        RandomAccessFile inraf = new RandomAccessFile(infile, "r");
        RandomAccessFile outraf = new RandomAccessFile(outfile, "rw");
        FileChannel finc = inraf.getChannel();
        FileChannel foutc = outraf.getChannel();
        MappedByteBuffer inmbb = finc.map(FileChannel.MapMode.READ_ONLY, 0, (int) infile.length());
        Charset inCharset = Charset.forName(WRONG_ENCODING);
        Charset outCharset = Charset.forName(RIGHT_ENCODING);
        CharsetDecoder inDecoder = inCharset.newDecoder();
        CharsetEncoder outEncoder = outCharset.newEncoder();
        CharBuffer cb = inDecoder.decode(inmbb);
        ByteBuffer outbb = outEncoder.encode(cb);
        foutc.write(outbb);
        inraf.close();
        outraf.close();
    }
}
