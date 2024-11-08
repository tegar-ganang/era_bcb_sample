package com.tcurtil.jforker.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

/**
 * ==============<br/>
 * /!\ WARING /!\<br/>
 * ==============<br/>
 * This stream gobbler is stoppable only if you called
 * the setSoTimeout method with a non 0 value on the
 * Socket
 * @author <a href="mailto:thierry_curtil@users.sourceforge.net">Thierry Curtil</a>
 */
public class StoppableStreamGobbler extends Thread {

    private InputStream is;

    private OutputStream os;

    private boolean stop = false;

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    StoppableStreamGobbler(InputStream is, OutputStream redirect) {
        this.is = is;
        this.os = redirect;
    }

    public void run() {
        try {
            byte[] buffer = new byte[Config.getStreamGobblerBufferSize()];
            int read;
            while (true) {
                read = 0;
                try {
                    read = is.read(buffer);
                } catch (SocketTimeoutException ste) {
                }
                if (read == -1) break;
                os.write(buffer, 0, read);
                if (stop) break;
            }
            os.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
