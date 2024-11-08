package org.gang.util;

import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Some Gang Intelligent code
 * Please visit http://gang.dinfo.ru for more info
 *
 * @author Vitaly
 * @since 19.04.2007 11:11:56
 */
public class GFile {

    public static void write(String fileName, String fileData) throws FileNotFoundException, IOException {
        write(fileName, fileData.getBytes());
    }

    public static void write(String fileName, byte fileData[]) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(new File(fileName));
        ByteBuffer bb = ByteBuffer.wrap(fileData);
        out.getChannel().write(bb);
        out.close();
    }
}
