package saadadb.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * @author laurentmichel
 * * @version $Id: CopyFile.java 118 2012-01-06 14:33:51Z laurent.mistahl $

 */
public class CopyFile {

    public static void copy(File in, File out) throws Exception {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    public static void copy(String name_prd, String file_out) throws Exception {
        FileInputStream fl = new FileInputStream(name_prd);
        byte b[] = new byte[4096];
        int len = 0;
        FileOutputStream souts = new FileOutputStream(file_out);
        while (len != -1) {
            len = fl.read(b);
            if (len != -1) {
                souts.write(b, 0, len);
            }
        }
        souts.flush();
        souts.close();
    }

    public static void main(String args[]) {
        try {
            (new CopyFile()).copy(args[0], args[1]);
        } catch (Exception e) {
            Messenger.printStackTrace(e);
        }
    }
}
