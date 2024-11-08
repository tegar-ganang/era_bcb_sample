package gnu.saw.server.graphicsmode;

import gnu.saw.SAW;
import gnu.saw.server.session.SAWServerSession;

public class SAWGraphicsModeServerSession {

    private Thread readerThread;

    private Thread writerThread;

    private SAWServerSession session;

    private SAWGraphicsModeServerReader reader;

    private SAWGraphicsModeServerWriter writer;

    public SAWGraphicsModeServerSession(SAWServerSession session) {
        this.session = session;
        this.reader = new SAWGraphicsModeServerReader(this);
        this.writer = new SAWGraphicsModeServerWriter(this);
        this.reader.setWriter(writer);
    }

    public SAWServerSession getSession() {
        return session;
    }

    public boolean verifySession() {
        boolean viewProviderInitialized = false;
        boolean controlProviderInitialized = false;
        try {
            viewProviderInitialized = session.getViewProvider().isScreenCaptureInitialized() || session.getViewProvider().initializeScreenCapture();
            controlProviderInitialized = session.getControlProvider().isInputControlInitialized() || session.getControlProvider().initializeInputControl();
        } catch (Exception e) {
        }
        try {
            if (viewProviderInitialized && controlProviderInitialized) {
                session.getConnection().getGraphicsControlDataOutputStream().write(SAW.SAW_GRAPHICS_MODE_SESSION_STARTABLE);
                session.getConnection().getGraphicsControlDataOutputStream().flush();
                if (session.getConnection().getGraphicsControlDataInputStream().read() == SAW.SAW_GRAPHICS_MODE_SESSION_STARTABLE) {
                    return true;
                }
            } else {
                session.getConnection().getGraphicsControlDataOutputStream().write(SAW.SAW_GRAPHICS_MODE_SESSION_UNSTARTABLE);
                session.getConnection().getGraphicsControlDataOutputStream().flush();
                session.getConnection().getGraphicsControlDataInputStream().read();
                return false;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void startSession() {
        reader.setStopped(false);
        writer.setStopped(false);
        readerThread = new Thread(reader, "SAWGraphicsModeServerReader");
        readerThread.setPriority(Thread.NORM_PRIORITY);
        writerThread = new Thread(writer, "SAWGraphicsModeServerWriter");
        writerThread.setPriority(Thread.NORM_PRIORITY);
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
        return reader.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        reader.setReadOnly(readOnly);
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
            session.getConnection().getGraphicsControlDataInputStream().read();
        } catch (Exception e) {
        }
    }
}
