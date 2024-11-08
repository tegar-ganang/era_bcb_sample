package com.sshtools.j2ssh.forwarding;

import com.sshtools.j2ssh.connection.IOChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class ForwardingIOChannel extends IOChannel implements ForwardingChannel {

    private static Log log = LogFactory.getLog(ForwardingIOChannel.class);

    private ForwardingChannelImpl channel;

    /**
     * Creates a new ForwardingIOChannel object.
     *
     * @param forwardType
     * @param hostToConnectOrBind
     * @param portToConnectOrBind
     * @param originatingHost
     * @param originatingPort
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingIOChannel(String forwardType, String name, String hostToConnectOrBind, int portToConnectOrBind, String originatingHost, int originatingPort) throws ForwardingConfigurationException {
        if (!forwardType.equals(LOCAL_FORWARDING_CHANNEL) && !forwardType.equals(REMOTE_FORWARDING_CHANNEL) && !forwardType.equals(X11_FORWARDING_CHANNEL)) {
            throw new ForwardingConfigurationException("The forwarding type is invalid");
        }
        channel = new ForwardingChannelImpl(forwardType, name, hostToConnectOrBind, portToConnectOrBind, originatingHost, originatingPort);
    }

    /**
     *
     *
     * @return
     */
    public byte[] getChannelOpenData() {
        return channel.getChannelOpenData();
    }

    /**
     *
     *
     * @return
     */
    public byte[] getChannelConfirmationData() {
        return channel.getChannelConfirmationData();
    }

    public String getName() {
        return channel.getName();
    }

    /**
     *
     *
     * @return
     */
    public String getChannelType() {
        return channel.getChannelType();
    }

    /**
     *
     *
     * @return
     */
    protected int getMinimumWindowSpace() {
        return 32768;
    }

    /**
     *
     *
     * @return
     */
    protected int getMaximumWindowSpace() {
        return 131072;
    }

    /**
     *
     *
     * @return
     */
    protected int getMaximumPacketSize() {
        return 32768;
    }

    /**
     *
     *
     * @return
     */
    public String getOriginatingHost() {
        return channel.getOriginatingHost();
    }

    /**
     *
     *
     * @return
     */
    public int getOriginatingPort() {
        return channel.getOriginatingPort();
    }

    /**
     *
     *
     * @return
     */
    public String getHostToConnectOrBind() {
        return channel.getHostToConnectOrBind();
    }

    /**
     *
     *
     * @return
     */
    public int getPortToConnectOrBind() {
        return channel.getPortToConnectOrBind();
    }

    /**
     *
     *
     * @param request
     * @param wantReply
     * @param requestData
     *
     * @throws IOException
     */
    protected void onChannelRequest(String request, boolean wantReply, byte[] requestData) throws IOException {
        connection.sendChannelRequestFailure(this);
    }

    /**
     *
     *
     * @throws IOException
     */
    protected void onChannelOpen() throws IOException {
    }
}
