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
public class FileCopy {

    public static int BUFFER_SIZE = 512;

    private String srcName;

    private String destName;

    /**
	 * 
	 */
    public FileCopy(String[] args) {
        srcName = args[0];
        destName = args[1];
    }

    public void run() {
        FileInputStream src;
        FileOutputStream dest;
        try {
            src = new FileInputStream(srcName);
            dest = new FileOutputStream(destName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        FileChannel srcC = src.getChannel();
        FileChannel destC = dest.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            int i;
            System.out.println(srcC.size());
            while ((i = srcC.read(buf)) > 0) {
                System.out.println(buf.getChar(2));
                buf.flip();
                destC.write(buf);
                buf.compact();
            }
            destC.close();
            dest.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
    }

    public static void main(String[] args) {
        new FileCopy(args).run();
    }
}
