package org.mcisb.util.io;

import java.beans.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import org.mcisb.util.*;
import org.mcisb.util.task.*;

/**
 * @author Neil Swainston
 *
 */
public class FileServer extends AbstractTask {

    /**
	 * 
	 */
    private final InterruptThread interruptThread = new InterruptThread();

    /**
	 * 
	 */
    private final int port;

    /**
	 * 
	 */
    private boolean stopped = false;

    /**
	 * 
	 * @param port
	 */
    public FileServer(final int port) {
        this.port = port;
    }

    @Override
    protected Serializable doTask() throws IOException {
        interruptThread.start();
        final ServerSocketChannel channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        final ServerSocket serverSocket = channel.socket();
        while (!stopped) {
            FileServerRunnable runnable = new FileServerRunnable(serverSocket.getChannel().accept().socket());
            runnable.addPropertyChangeListener(this);
            new Thread(runnable).start();
        }
        return null;
    }

    /**
	 * 
	 */
    public void stop() {
        interruptThread.allowInterruption();
        stopped = true;
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals(Task.PROGRESS)) {
            int progress = ((Integer) e.getNewValue()).intValue();
            if (progress == Task.CANCELLED || progress == Task.FINISHED || progress == Task.ERROR) {
                Object source = e.getSource();
                if (source instanceof AbstractTask) {
                    AbstractTask task = (AbstractTask) source;
                    task.removePropertyChangeListener(this);
                    if (progress == Task.ERROR) {
                        exception = task.getException();
                    }
                }
            }
        }
        firePropertyChange(e);
    }
}
