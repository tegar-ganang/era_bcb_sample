package gnu.saw.client.console;

import gnu.saw.client.connection.SAWClientConnection;
import gnu.saw.client.session.SAWClientSession;
import gnu.saw.terminal.SAWTerminal;

public class SAWClientServerReader implements Runnable {

    private static final int resultBufferSize = 1024;

    private volatile boolean stopped;

    private int readChars;

    private final char[] resultBuffer = new char[resultBufferSize];

    private SAWClientSession session;

    private SAWClientConnection connection;

    public SAWClientServerReader(SAWClientSession session) {
        this.session = session;
        this.connection = session.getConnection();
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
                readChars = connection.getResultReader().read(resultBuffer, 0, resultBufferSize);
                if (readChars > 0 && !stopped) {
                    SAWTerminal.write(resultBuffer, 0, readChars);
                    SAWTerminal.flush();
                } else {
                    stopped = true;
                    break;
                }
            } catch (Exception e) {
                stopped = true;
                break;
            }
        }
        synchronized (session) {
            session.notify();
        }
    }
}
