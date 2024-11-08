package com.sshtools.j2ssh.connection;

import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.transport.MessageNotAvailableException;
import com.sshtools.j2ssh.transport.MessageStoreEOFException;
import com.sshtools.j2ssh.transport.SshMessageStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.16 $
 */
public abstract class IOChannel extends Channel {

    private static Log log = LogFactory.getLog(IOChannel.class);

    /**  */
    private SshMessageStore incoming = new SshMessageStore();

    /**  */
    protected ChannelInputStream in;

    /**  */
    protected ChannelOutputStream out;

    /**  */
    protected InputStream boundInputStream = null;

    /**  */
    protected OutputStream boundOutputStream = null;

    /**  */
    protected IOStreamConnector ios = null;

    /**
     *
     *
     * @param connection
     * @param localChannelId
     * @param senderChannelId
     * @param initialWindowSize
     * @param maximumPacketSize
     *
     * @throws IOException
     */
    protected void init(ConnectionProtocol connection, long localChannelId, long senderChannelId, long initialWindowSize, long maximumPacketSize) throws IOException {
        this.in = new ChannelInputStream(incoming);
        this.out = new ChannelOutputStream(this);
        super.init(connection, localChannelId, senderChannelId, initialWindowSize, maximumPacketSize);
    }

    /**
     *
     *
     * @throws IOException
     */
    protected void open() throws IOException {
        super.open();
        if (boundOutputStream != null) {
            sendOutstandingMessages();
        }
        if ((boundInputStream != null) && (ios == null)) {
            ios.setCloseInput(false);
            ios.setCloseOutput(false);
            ios.connect(boundInputStream, out);
        }
    }

    /**
     *
     *
     * @return
     */
    public ChannelInputStream getInputStream() {
        return in;
    }

    /**
     *
     *
     * @return
     */
    public ChannelOutputStream getOutputStream() {
        return out;
    }

    /**
     *
     *
     * @param msg
     *
     * @throws IOException
     */
    protected void onChannelData(SshMsgChannelData msg) throws IOException {
        synchronized (incoming) {
            if (boundOutputStream != null) {
                try {
                    boundOutputStream.write(msg.getChannelData());
                } catch (IOException ex) {
                    log.info("Could not route data to the bound OutputStream; Closing channel.");
                    log.info(ex.getMessage());
                    close();
                }
            } else {
                incoming.addMessage(msg);
            }
        }
    }

    /**
     *
     *
     * @throws IOException
     */
    public void setLocalEOF() throws IOException {
        super.setLocalEOF();
        if (!out.isClosed()) {
            out.close();
        }
    }

    /**
     *
     *
     * @throws IOException
     */
    protected void onChannelEOF() throws IOException {
        if (!in.isClosed()) {
            in.close();
        }
    }

    /**
     *
     *
     * @throws IOException
     */
    protected void onChannelClose() throws IOException {
        if (!in.isClosed()) {
            in.close();
        }
        if (!out.isClosed()) {
            out.close();
        }
        if (ios != null) {
            ios.close();
        }
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

    public void bindOutputStream(OutputStream boundOutputStream) throws IOException {
        synchronized (incoming) {
            this.boundOutputStream = boundOutputStream;
            if (state.getValue() == ChannelState.CHANNEL_OPEN) {
                sendOutstandingMessages();
            }
        }
    }

    /**
     *
     *
     * @param boundInputStream
     *
     * @throws IOException
     */
    public void bindInputStream(InputStream boundInputStream) throws IOException {
        this.boundInputStream = boundInputStream;
        this.ios = new IOStreamConnector();
        if (state.getValue() == ChannelState.CHANNEL_OPEN) {
            ios.setCloseInput(false);
            ios.setCloseOutput(false);
            ios.connect(boundInputStream, out);
        }
    }

    private void sendOutstandingMessages() throws IOException {
        if ((boundInputStream != null) && (boundOutputStream != null) && incoming.hasMessages()) {
            while (true) {
                try {
                    SshMsgChannelData msg = (SshMsgChannelData) incoming.peekMessage(SshMsgChannelData.SSH_MSG_CHANNEL_DATA);
                    incoming.removeMessage(msg);
                    try {
                        boundOutputStream.write(msg.getChannelData());
                    } catch (IOException ex1) {
                        close();
                    }
                } catch (MessageStoreEOFException ex) {
                    break;
                } catch (MessageNotAvailableException ex) {
                    break;
                } catch (InterruptedException ex) {
                    throw new IOException("The thread was interrupted");
                }
            }
        }
    }
}
