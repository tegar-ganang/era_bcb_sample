package it.unina.seclab.jafimon.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Classe di utilit� per la manipolazione di oggetti di tipo <code>java.io.File</code>
 *  
 * @author Mauro Iorio
 *
 */
public class FileTools {

    /**
	 * Converts the contents of a file into a CharSequence
	 * suitable for use by the regex package.
	 * 
	 * @throws IOException 
	 */
    public static CharSequence fromFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        ByteBuffer bbuf = ByteBuffer.allocate((int) fc.size());
        fc.read(bbuf);
        bbuf.position(0);
        CharBuffer cbuf = Charset.forName("8859_1").newDecoder().decode(bbuf);
        return cbuf;
    }

    /**
	 * Converts the contents of a file into a CharSequence
	 * suitable for use by the regex package.
	 * 
	 * @throws IOException 
	 */
    public static CharSequence fromFile(String filename) throws IOException {
        File f = new File(filename);
        return fromFile(f);
    }

    /**
     * Converts the contents of a StringBuffer into an encoded
     * ByteBuffer and writes it to file
     * 
     * @throws IOException 
     */
    public static void toFile(File file, StringBuffer sb) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        FileChannel fc = fos.getChannel();
        CharBuffer cb = CharBuffer.allocate(sb.length());
        cb.put(sb.toString());
        cb.position(0);
        ByteBuffer bbf = Charset.forName("8859_1").newEncoder().encode(cb);
        fc.write(bbf);
    }
}
