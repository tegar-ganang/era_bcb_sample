package com.sshtools.j2ssh.agent;

import com.sshtools.j2ssh.connection.InvalidChannelException;
import com.sshtools.j2ssh.connection.SocketChannel;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import java.io.IOException;
import java.net.InetAddress;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.11 $
 */
public class AgentSocketChannel extends SocketChannel {

    /**  */
    public static final String AGENT_FORWARDING_CHANNEL = "auth-agent";

    private boolean isForwarding;

    /**
     * Creates a new AgentSocketChannel object.
     *
     * @param isForwarding
     */
    public AgentSocketChannel(boolean isForwarding) {
        this.isForwarding = isForwarding;
    }

    /**
     *
     *
     * @return
     */
    public String getChannelType() {
        return AGENT_FORWARDING_CHANNEL;
    }

    protected void onChannelRequest(String requestType, boolean wantReply, byte[] requestData) throws java.io.IOException {
        if (wantReply) {
            connection.sendChannelRequestFailure(this);
        }
    }

    /**
     *
     *
     * @return
     */
    protected int getMaximumPacketSize() {
        return 32678;
    }

    public byte[] getChannelOpenData() {
        return null;
    }

    /**
     *
     *
     * @return
     */
    protected int getMinimumWindowSpace() {
        return 1024;
    }

    /**
     *
     *
     * @throws com.sshtools.j2ssh.connection.InvalidChannelException DOCUMENT
     *         ME!
     * @throws InvalidChannelException
     */
    protected void onChannelOpen() throws com.sshtools.j2ssh.connection.InvalidChannelException {
        try {
            if (isForwarding) {
                SshAgentForwardingNotice msg = new SshAgentForwardingNotice(InetAddress.getLocalHost().getHostName(), InetAddress.getLocalHost().getHostAddress(), socket.getPort());
                ByteArrayWriter baw = new ByteArrayWriter();
                baw.writeBinaryString(msg.toByteArray());
                sendChannelData(baw.toByteArray());
            }
            super.onChannelOpen();
        } catch (IOException ex) {
            throw new InvalidChannelException(ex.getMessage());
        }
    }

    /**
     *
     *
     * @return
     */
    protected int getMaximumWindowSpace() {
        return 32768;
    }

    /**
     *
     *
     * @return
     */
    public byte[] getChannelConfirmationData() {
        return null;
    }
}
