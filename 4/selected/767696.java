package gnu.saw.tunnel.stream;

import gnu.saw.tunnel.session.SAWTunnelSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SAWTunnelStreamRedirector implements Runnable {

    private static final int redirectorBufferSize = 64 * 1024;

    private volatile boolean stopped;

    private volatile int readed;

    private final byte[] redirectorBuffer = new byte[redirectorBufferSize];

    private InputStream source;

    private OutputStream destination;

    private SAWTunnelSession session;

    public SAWTunnelStreamRedirector(InputStream source, OutputStream destination, SAWTunnelSession session) {
        this.source = source;
        this.destination = destination;
        this.session = session;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public void run() {
        while (!stopped) {
            try {
                readed = source.read(redirectorBuffer, 0, redirectorBufferSize);
                destination.write(redirectorBuffer, 0, readed);
                destination.flush();
            } catch (IOException e) {
                stopped = true;
                break;
            }
        }
        synchronized (session) {
            session.notify();
        }
    }
}
