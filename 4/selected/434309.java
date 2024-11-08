package gnu.saw.server.console;

import gnu.saw.server.connection.SAWServerConnection;
import gnu.saw.server.session.SAWServerSession;

public class SAWServerShellOutputWriter implements Runnable {

    private static final int resultBufferSize = 256;

    private volatile boolean stopped;

    private int readChars;

    private final char[] resultBuffer = new char[resultBufferSize];

    private SAWServerConnection connection;

    private SAWServerSession session;

    public SAWServerShellOutputWriter(SAWServerSession session) {
        this.session = session;
        this.connection = session.getConnection();
        this.stopped = false;
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
                if (session.getShellOutputReader().ready()) {
                    readChars = session.getShellOutputReader().read(resultBuffer, 0, resultBufferSize);
                    if (readChars > 0 && !stopped) {
                        connection.getResultWriter().write(resultBuffer, 0, readChars);
                        connection.getResultWriter().flush();
                    } else {
                        stopped = true;
                        break;
                    }
                } else {
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                stopped = true;
                break;
            }
        }
        try {
            synchronized (session.getShell()) {
                session.getShell().notify();
            }
        } catch (Exception e) {
        }
    }
}
