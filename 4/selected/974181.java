package rabbit.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;

/** A class that performs a background copy from one stream to another
 */
public class CopyThread extends Thread {

    InputStream in = null;

    OutputStream out = null;

    public CopyThread(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        start();
    }

    public void run() {
        int read;
        byte[] buf = new byte[1024];
        try {
            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
                out.flush();
            }
        } catch (ClosedChannelException e) {
        } catch (SocketException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
