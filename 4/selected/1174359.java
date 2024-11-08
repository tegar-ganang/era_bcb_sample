package com.sshtools.j2ssh.connection;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.SocketException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import org.lirc.socket.UnixSocket;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.1 $
 */
public abstract class UNIXSocketChannel extends Channel {

    private static Log log = LogFactory.getLog(UNIXSocketChannel.class);

    /**  */
    protected UnixSocket socket = null;

    Thread thread;

    /**
   *
   *
   * @param socket
   *
   * @throws IOException
   */
    public void bindSocket(UnixSocket socket) throws IOException {
        if (state.getValue() == ChannelState.CHANNEL_UNINITIALIZED) {
            this.socket = socket;
        } else {
            throw new IOException("The socket can only be bound to an unitialized channel");
        }
    }

    /**
   *
   *
   * @param msg
   *
   * @throws IOException
   */
    protected void onChannelData(SshMsgChannelData msg) throws IOException {
        try {
            socket.getOutputStream().write(msg.getChannelData());
        } catch (IOException ex) {
        }
    }

    /**
   *
   *
   * @throws IOException
   */
    protected void onChannelEOF() throws IOException {
        try {
            socket.shutdownOutput();
        } catch (IOException ex) {
            log.info("Failed to shutdown Socket OutputStream in response to EOF event: " + ex.getMessage());
        }
    }

    /**
   *
   *
   * @throws IOException
   */
    protected void onChannelClose() throws IOException {
        try {
            socket.close();
        } catch (IOException ex) {
            log.info("Failed to close socket on channel close event: " + ex.getMessage());
        }
    }

    /**
   *
   *
   * @throws IOException
   */
    protected void onChannelOpen() throws IOException {
        if (socket == null) {
            throw new IOException("The socket must be bound to the channel before opening");
        }
        thread = new Thread(new SocketReader());
        thread.start();
    }

    /**
   *
   *
   * @param msg
   *
   * @throws IOException
   */
    protected void onChannelExtData(SshMsgChannelExtendedData msg) throws IOException {
    }

    class SocketReader implements Runnable {

        public void run() {
            byte[] buffer = new byte[getMaximumPacketSize()];
            ByteArrayWriter baw = new ByteArrayWriter();
            try {
                int read = 0;
                while ((read >= 0) && !isClosed()) {
                    try {
                        read = socket.getInputStream().read(buffer);
                    } catch (InterruptedIOException ex1) {
                        read = ex1.bytesTransferred;
                    }
                    synchronized (state) {
                        if (isClosed() || isLocalEOF()) {
                            break;
                        }
                        if (read > 0) {
                            baw.write(buffer, 0, read);
                            sendChannelData(baw.toByteArray());
                            baw.reset();
                        }
                    }
                }
            } catch (IOException ex) {
            }
            try {
                synchronized (state) {
                    if (!isLocalEOF()) {
                        setLocalEOF();
                    }
                    if (isOpen()) {
                        close();
                    }
                }
            } catch (Exception ex) {
                log.info("Failed to send channel EOF message: " + ex.getMessage());
            }
            thread = null;
        }
    }
}
