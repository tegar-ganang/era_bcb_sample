package gnu.saw.client.graphicsmode;

import java.awt.GraphicsEnvironment;
import gnu.saw.SAW;
import gnu.saw.client.session.SAWClientSession;
import gnu.saw.terminal.SAWTerminal;

public class SAWGraphicsModeClientSession {

    private volatile boolean finished;

    private Thread readerThread;

    private Thread writerThread;

    private SAWClientSession session;

    private SAWGraphicsModeClientReader reader;

    private SAWGraphicsModeClientWriter writer;

    public SAWGraphicsModeClientSession(SAWClientSession session) {
        this.session = session;
        this.reader = new SAWGraphicsModeClientReader(this);
        this.writer = new SAWGraphicsModeClientWriter(this);
        this.reader.setWriter(writer);
        this.writer.setReader(reader);
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public SAWClientSession getSession() {
        return session;
    }

    public boolean verifySession() {
        boolean headless = true;
        try {
            headless = GraphicsEnvironment.isHeadless();
        } catch (Exception e) {
        }
        try {
            if (headless) {
                session.getConnection().getGraphicsControlDataOutputStream().write(SAW.SAW_GRAPHICS_MODE_SESSION_UNSTARTABLE);
                session.getConnection().getGraphicsControlDataOutputStream().flush();
                SAWTerminal.print("\nSAW>SAWGRAPHICSMODE:Graphics mode cannot start on client!\nSAW>");
                if (session.getConnection().getGraphicsControlDataInputStream().read() == SAW.SAW_GRAPHICS_MODE_SESSION_UNSTARTABLE) {
                    SAWTerminal.print("\nSAW>SAWGRAPHICSMODE:Graphics mode cannot start on server!\nSAW>");
                }
            } else {
                session.getConnection().getGraphicsControlDataOutputStream().write(SAW.SAW_GRAPHICS_MODE_SESSION_STARTABLE);
                session.getConnection().getGraphicsControlDataOutputStream().flush();
                if (session.getConnection().getGraphicsControlDataInputStream().read() == SAW.SAW_GRAPHICS_MODE_SESSION_STARTABLE) {
                    return true;
                } else {
                    SAWTerminal.print("\nSAW>SAWGRAPHICSMODE:Graphics mode cannot start on server!\nSAW>");
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void startSession() {
        writer.setStopped(false);
        reader.setStopped(false);
        writerThread = new Thread(writer, "SAWGraphicsModeClientWriter");
        writerThread.setPriority(Thread.NORM_PRIORITY);
        readerThread = new Thread(reader, "SAWGraphicsModeClientReader");
        readerThread.setPriority(Thread.NORM_PRIORITY);
        if (writer.isReadOnly()) {
            SAWTerminal.print("\nSAW>SAWGRAPHICSMODE:Starting graphics mode in view mode...\nSAW>");
        } else {
            SAWTerminal.print("\nSAW>SAWGRAPHICSMODE:Starting graphics mode in control mode...\nSAW>");
        }
        writerThread.start();
        readerThread.start();
    }

    public boolean isStopped() {
        return reader.isStopped() || writer.isStopped();
    }

    public void setStopped(boolean stopped) {
        writer.setStopped(stopped);
    }

    public boolean isReadOnly() {
        return writer.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        writer.setReadOnly(readOnly);
    }

    public void waitSession() {
        synchronized (this) {
            while (!isStopped()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void tryStopThreads() {
        setStopped(true);
        if (session.getClipboardTransferThread().isAlive()) {
            session.getClipboardTransferThread().interrupt();
        }
    }

    public void waitThreads() {
        try {
            session.getClipboardTransferThread().join();
            readerThread.join();
            writerThread.join();
            reader.dispose();
            writer.dispose();
        } catch (InterruptedException e) {
        }
    }

    public void endSession() {
        try {
            session.getConnection().getGraphicsControlDataOutputStream().write(SAW.SAW_GRAPHICS_MODE_SESSION_ENDED);
            session.getConnection().getGraphicsControlDataOutputStream().flush();
            SAWTerminal.print("\nSAW>SAWGRAPHICSMODE:Stopping graphics mode...\nSAW>");
            session.getConnection().getGraphicsControlDataInputStream().read();
            synchronized (session.getGraphicsClient()) {
                SAWTerminal.print("\nSAW>SAWGRAPHICSMODE:Graphics mode stopped!\nSAW>");
                finished = true;
            }
        } catch (Exception e) {
        }
        finished = true;
    }
}
