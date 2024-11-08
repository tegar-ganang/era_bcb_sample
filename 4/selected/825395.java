package org.apache.axis2.clustering.tribes;

import org.apache.axis2.clustering.ClusteringConstants;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.MembershipScheme;
import org.apache.axis2.clustering.MembershipListener;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.util.Utils;
import org.apache.catalina.tribes.ManagedChannel;
import org.apache.catalina.tribes.ChannelInterceptor;
import org.apache.catalina.tribes.group.interceptors.OrderInterceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import org.apache.catalina.tribes.group.interceptors.NonBlockingCoordinator;
import org.apache.catalina.tribes.transport.ReceiverBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.net.SocketException;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation of the multicast based membership scheme. In this scheme, membership is discovered
 * using multicasts
 */
public class MulticastBasedMembershipScheme implements MembershipScheme {

    private static final Log log = LogFactory.getLog(MulticastBasedMembershipScheme.class);

    /**
     * The Tribes channel
     */
    private ManagedChannel channel;

    private Map<String, Parameter> parameters;

    /**
     * The domain to which this node belongs to
     */
    private byte[] domain;

    /**
     * The mode in which this member operates such as "loadBalance" or "application"
     */
    private OperationMode mode;

    private boolean atmostOnceMessageSemantics;

    private boolean preserverMsgOrder;

    public MulticastBasedMembershipScheme(ManagedChannel channel, OperationMode mode, Map<String, Parameter> parameters, byte[] domain, boolean atmostOnceMessageSemantics, boolean preserverMsgOrder) {
        this.channel = channel;
        this.mode = mode;
        this.parameters = parameters;
        this.domain = domain;
        this.atmostOnceMessageSemantics = atmostOnceMessageSemantics;
        this.preserverMsgOrder = preserverMsgOrder;
    }

    public void init() throws ClusteringFault {
        addInterceptors();
        configureMulticastParameters();
    }

    public void joinGroup() throws ClusteringFault {
    }

    private void configureMulticastParameters() throws ClusteringFault {
        Properties mcastProps = channel.getMembershipService().getProperties();
        Parameter mcastAddress = getParameter(TribesConstants.MCAST_ADDRESS);
        if (mcastAddress != null) {
            mcastProps.setProperty(TribesConstants.MCAST_ADDRESS, ((String) mcastAddress.getValue()).trim());
        }
        Parameter mcastBindAddress = getParameter(TribesConstants.MCAST_BIND_ADDRESS);
        if (mcastBindAddress != null) {
            mcastProps.setProperty(TribesConstants.MCAST_BIND_ADDRESS, ((String) mcastBindAddress.getValue()).trim());
        }
        Parameter mcastPort = getParameter(TribesConstants.MCAST_PORT);
        if (mcastPort != null) {
            mcastProps.setProperty(TribesConstants.MCAST_PORT, ((String) mcastPort.getValue()).trim());
        }
        Parameter mcastFrequency = getParameter(TribesConstants.MCAST_FREQUENCY);
        if (mcastFrequency != null) {
            mcastProps.setProperty(TribesConstants.MCAST_FREQUENCY, ((String) mcastFrequency.getValue()).trim());
        }
        Parameter mcastMemberDropTime = getParameter(TribesConstants.MEMBER_DROP_TIME);
        if (mcastMemberDropTime != null) {
            mcastProps.setProperty(TribesConstants.MEMBER_DROP_TIME, ((String) mcastMemberDropTime.getValue()).trim());
        }
        ReceiverBase receiver = (ReceiverBase) channel.getChannelReceiver();
        Parameter tcpListenHost = getParameter(TribesConstants.LOCAL_MEMBER_HOST);
        if (tcpListenHost != null) {
            String host = ((String) tcpListenHost.getValue()).trim();
            mcastProps.setProperty(TribesConstants.TCP_LISTEN_HOST, host);
            mcastProps.setProperty(TribesConstants.BIND_ADDRESS, host);
            receiver.setAddress(host);
        } else {
            String host;
            try {
                host = Utils.getIpAddress();
            } catch (SocketException e) {
                String msg = "Could not get local IP address";
                log.error(msg, e);
                throw new ClusteringFault(msg, e);
            }
            mcastProps.setProperty(TribesConstants.TCP_LISTEN_HOST, host);
            mcastProps.setProperty(TribesConstants.BIND_ADDRESS, host);
            receiver.setAddress(host);
        }
        String localIP = System.getProperty(ClusteringConstants.LOCAL_IP_ADDRESS);
        if (localIP != null) {
            receiver.setAddress(localIP);
        }
        Parameter tcpListenPort = getParameter(TribesConstants.LOCAL_MEMBER_PORT);
        if (tcpListenPort != null) {
            String port = ((String) tcpListenPort.getValue()).trim();
            mcastProps.setProperty(TribesConstants.TCP_LISTEN_PORT, port);
            receiver.setPort(Integer.parseInt(port));
        }
        mcastProps.setProperty(TribesConstants.MCAST_CLUSTER_DOMAIN, new String(domain));
    }

    /**
     * Add ChannelInterceptors. The order of the interceptors that are added will depend on the
     * membership management scheme
     */
    private void addInterceptors() {
        if (log.isDebugEnabled()) {
            log.debug("Adding Interceptors...");
        }
        TcpFailureDetector tcpFailureDetector = new TcpFailureDetector();
        tcpFailureDetector.setConnectTimeout(30000);
        channel.addInterceptor(tcpFailureDetector);
        if (log.isDebugEnabled()) {
            log.debug("Added TCP Failure Detector");
        }
        channel.getMembershipService().setDomain(domain);
        mode.addInterceptors(channel);
        if (atmostOnceMessageSemantics) {
            AtMostOnceInterceptor atMostOnceInterceptor = new AtMostOnceInterceptor();
            atMostOnceInterceptor.setOptionFlag(TribesConstants.AT_MOST_ONCE_OPTION);
            channel.addInterceptor(atMostOnceInterceptor);
            if (log.isDebugEnabled()) {
                log.debug("Added At-most-once Interceptor");
            }
        }
        if (preserverMsgOrder) {
            OrderInterceptor orderInterceptor = new OrderInterceptor();
            orderInterceptor.setOptionFlag(TribesConstants.MSG_ORDER_OPTION);
            channel.addInterceptor(orderInterceptor);
            if (log.isDebugEnabled()) {
                log.debug("Added Message Order Interceptor");
            }
        }
    }

    public Parameter getParameter(String name) {
        return parameters.get(name);
    }
}
