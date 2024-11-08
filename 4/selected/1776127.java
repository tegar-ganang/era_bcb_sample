package com.sshtools.j2ssh.forwarding;

import com.sshtools.j2ssh.io.ByteArrayWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.11 $
 */
public class ForwardingChannelImpl implements ForwardingChannel {

    private static Log log = LogFactory.getLog(ForwardingChannelImpl.class);

    private String forwardType;

    private String originatingHost;

    private int originatingPort;

    private String hostToConnectOrBind;

    private int portToConnectOrBind;

    private String name;

    /**
     * Creates a new ForwardingChannelImpl object.
     *
     * @param forwardType
     * @param hostToConnectOrBind
     * @param portToConnectOrBind
     * @param originatingHost
     * @param originatingPort
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingChannelImpl(String forwardType, String name, String hostToConnectOrBind, int portToConnectOrBind, String originatingHost, int originatingPort) throws ForwardingConfigurationException {
        if (!forwardType.equals(LOCAL_FORWARDING_CHANNEL) && !forwardType.equals(REMOTE_FORWARDING_CHANNEL) && !forwardType.equals(X11_FORWARDING_CHANNEL)) {
            throw new ForwardingConfigurationException("The forwarding type is invalid");
        }
        this.forwardType = forwardType;
        this.hostToConnectOrBind = hostToConnectOrBind;
        this.portToConnectOrBind = portToConnectOrBind;
        this.originatingHost = originatingHost;
        this.originatingPort = originatingPort;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     *
     *
     * @return
     */
    public String getHostToConnectOrBind() {
        return hostToConnectOrBind;
    }

    /**
     *
     *
     * @return
     */
    public int getPortToConnectOrBind() {
        return portToConnectOrBind;
    }

    /**
     *
     *
     * @return
     */
    public byte[] getChannelOpenData() {
        try {
            ByteArrayWriter baw = new ByteArrayWriter();
            baw.writeString(hostToConnectOrBind);
            baw.writeInt(portToConnectOrBind);
            baw.writeString(originatingHost);
            baw.writeInt(originatingPort);
            return baw.toByteArray();
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     *
     *
     * @return
     */
    public byte[] getChannelConfirmationData() {
        return null;
    }

    /**
     *
     *
     * @return
     */
    public String getChannelType() {
        return forwardType;
    }

    /**
     *
     *
     * @return
     */
    public String getOriginatingHost() {
        return originatingHost;
    }

    /**
     *
     *
     * @return
     */
    public int getOriginatingPort() {
        return originatingPort;
    }
}
