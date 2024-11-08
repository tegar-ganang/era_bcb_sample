package deltree.file;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author demangep
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class FileMerge {

    public static int BUFFER_SIZE = 512;

    private String srcName;

    private int maxSize;

    /**
	 * 
	 */
    public FileMerge(String[] args) {
        try {
            srcName = args[0];
            maxSize = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("Usage:\nfilesplt.jar srcname(without _i)");
        }
    }

    public void run() {
        FileInputStream src;
        FileOutputStream dest;
        try {
            dest = new FileOutputStream(srcName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        FileChannel destC = dest.getChannel();
        FileChannel srcC;
        ByteBuffer buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
        try {
            int fileNo = 0;
            while (true) {
                int i = 1;
                String destName = srcName + "_" + fileNo;
                src = new FileInputStream(destName);
                srcC = src.getChannel();
                while ((i > 0)) {
                    i = srcC.read(buf);
                    buf.flip();
                    destC.write(buf);
                    buf.compact();
                }
                srcC.close();
                src.close();
                fileNo++;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
    }

    public static void main(String[] args) {
        new FileMerge(args).run();
    }
}
