package tristero.tunnel.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Conduit extends Thread {

    InputStream sin;

    OutputStream sout;

    public static void pump(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1];
        int bytes_read = 0;
        while (bytes_read != -1) {
            bytes_read = in.read(buffer);
            if (bytes_read == -1) break;
            out.write(buffer, 0, bytes_read);
        }
    }

    public Conduit(InputStream in, OutputStream out) {
        sin = in;
        sout = out;
    }

    public void run() {
        try {
            Conduit.pump(sin, sout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
