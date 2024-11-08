package com.cbsgmbh.xi.af.as2.jca.configuration;

import java.util.LinkedList;
import javax.resource.ResourceException;
import com.cbsgmbh.xi.af.as2.util.AS2Util;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.Direction;
import com.sap.aii.af.service.cpa.LookupManager;

/**
 * This class encapsulates access to the CPA adapter cache and provides the configuration in a more convenient way to the AS2 JCA Adapter
 */
public class ConfigurationSapImpl extends ConfigurationAbstractImpl implements ConfigurationSettings {

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(ConfigurationSapImpl.class.getName(), TracerCategories.APP_ADAPTER_HTTP);

    private static final String JCA_ADAPTER_STATUS = "adapterStatus";

    private static final String JCA_HTTP_TRANSPROT = "HTTP.TransportProtocol";

    private static final String JCA_HOST = "HTTP.Host";

    private static final String JCA_PORT = "HTTP.Port";

    private static final String JCA_REQUEST_PATH = "HTTP.RequestPath";

    private static final String JCA_HTTP_QUERY = "HTTP.RequestQuery";

    private static final String JCA_HTTP_PROXY = "HTTP.UseProxy";

    private static final String JCA_HTTP_PROXY_HOST = "HTTP.ProxyHost";

    private static final String JCA_HTTP_PROXY_PORT = "HTTP.ProxyPort";

    private static final String JCA_HTTP_PROXY_USER = "HTTP.ProxyUser";

    private static final String JCA_HTTP_PROXY_PASSWORD = "HTTP.ProxyPassword";

    private static final String JCA_HTTP_PASSWORD = "HTTP.SessionPassword";

    private static final String JCA_HTTP_USER = "HTTP.SessionUser";

    private static final String JCA_HTTP_SESSIONAUTH = "HTTP.SessionAuthentication";

    private static final String JCA_CRYPT_USE_ENCRYPTION = "CRYPT.UseEncryption";

    private static final String JCA_CRYPT_USE_SIGNATURE = "CRYPT.UseSignature";

    private static final String JCA_CRYPT_MICALG_OUT_REQ = "CRYPT.Algorithm.Sig.Out.Req";

    private static final String JCA_CRYPT_MICALG_OUT_MDN = "CRYPT.Algorithm.Sig.Out.MDN";

    private static final String JCA_AS2_USE_MDN = "AS2.UseMdn";

    private static final String JCA_RCV_PUBKEY_CERTID_MDN = "CRYPT.Receiver.PubKey.Cert.MDN";

    private static final String JCA_RCV_PUBKEY_CERTID_MDN_VIEW = "CRYPT.Receiver.PubKey.Cert.MDN.View";

    private LookupManager lookupManager = null;

    /**
	 * Creates a XI configuration object by passing a specific channelId. The XI CPA LookupManager
	 * is instantiated and the specific channel object is retrieved.
	 * 
	 * @param channelId channel id
	 * @throws ResourceException
	 */
    @SuppressWarnings("unchecked")
    public ConfigurationSapImpl(String channelId) throws ResourceException {
        final Tracer tracer = baseTracer.entering("ConfigurationSapImpl(String channelId)");
        this.receiverChannels = new LinkedList();
        this.senderChannels = new LinkedList();
        this.lookupManager = LookupManager.getInstance();
        tracer.info("LookupManager.getInstance(): {0}", this.lookupManager);
        try {
            tracer.info("this.lookupManager.getChannelsByAdapterType(" + AS2Util.ADAPTER_NAME + ", " + AS2Util.ADAPTER_NAMESPACE + ")");
            LinkedList allChannels = this.lookupManager.getChannelsByAdapterType(AS2Util.ADAPTER_NAME, AS2Util.ADAPTER_NAMESPACE);
            tracer.info("The XI CPA service returned {0} channels for adapter type {1} with namespace {2}", new Object[] { new Integer(allChannels.size()), AS2Util.ADAPTER_NAME, AS2Util.ADAPTER_NAMESPACE });
            for (int i = 0; i < allChannels.size(); i++) {
                Channel _channel = (Channel) allChannels.get(i);
                tracer.info("Found channel {0} with direction {1}", new Object[] { _channel.getChannelName(), _channel.getDirection() });
                if (_channel.getDirection() == Direction.OUTBOUND) {
                    this.receiverChannels.add(_channel);
                } else if (_channel.getDirection() == Direction.INBOUND) {
                    this.senderChannels.add(_channel);
                }
                tracer.info("Channel with ID {0} for party {1} and service {2} added (direction is {3}).", new Object[] { _channel.getObjectId(), _channel.getParty(), _channel.getService(), _channel.getDirection().toString() });
            }
        } catch (Exception e) {
            tracer.catched(e);
            ResourceException re = new ResourceException("CPA lookup failed: " + e.getMessage());
            tracer.throwing(re);
            throw re;
        }
        this.channelId = channelId;
        tracer.leaving();
    }

    /**
	 * This methods reads specific channel data based on the channelId that was passed within the constructor.
	 * All necessary parameters are read, depending on the message direction
	 * 
	 * @return void
	 */
    public void init() throws CPAException {
        Tracer tracer = baseTracer.entering("init()");
        Channel channel = (Channel) this.lookupManager.getCPAObject(CPAObjectType.CHANNEL, this.channelId);
        tracer.info("Channel channel created : {0} with channelId : {1} for party: {2}", new Object[] { channel.getChannelName(), this.channelId, channel.getParty() });
        if (channel.getValueAsString(JCA_ADAPTER_STATUS) != null) {
            this.adapterStatus = channel.getValueAsString(JCA_ADAPTER_STATUS);
            tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_ADAPTER_STATUS, this.adapterStatus });
        } else {
            tracer.error("Channel parameter reading error : key {0}", JCA_ADAPTER_STATUS);
        }
        if (channel.getTransProt().toLowerCase() != null) {
            this.protocol = channel.getTransProt().toLowerCase();
            tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_TRANSPROT, this.protocol });
        } else {
            tracer.error("Channel parameter reading error : key {0}", JCA_HTTP_TRANSPROT);
        }
        if (channel.getDirection().toString().equals(Direction.OUTBOUND.toString())) {
            if (channel.getValueAsString(JCA_HOST) != null) {
                this.host = channel.getValueAsString(JCA_HOST).toLowerCase();
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HOST, this.host });
            } else {
                tracer.error("Channel parameter reading error : key {0}", JCA_HOST);
            }
            if (channel.getValueAsString(JCA_PORT) != null) {
                this.port = new Integer(channel.getValueAsInt(JCA_PORT));
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_PORT, this.port });
            } else {
                tracer.error("Channel parameter reading error : key {0}", JCA_PORT);
            }
            if (channel.getValueAsString(JCA_REQUEST_PATH) != null) {
                this.path = channel.getValueAsString(JCA_REQUEST_PATH);
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_REQUEST_PATH, this.path });
            } else {
                tracer.error("Channel parameter reading error : key {0}", JCA_REQUEST_PATH);
            }
            if (channel.getValueAsString(JCA_HTTP_QUERY) != null) {
                this.query = channel.getValueAsString(JCA_HTTP_QUERY);
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_QUERY, this.query });
            } else {
                tracer.info("Channel parameter reading : key {0} is not set", JCA_HTTP_QUERY);
            }
            if (channel.getValueAsString(JCA_HTTP_SESSIONAUTH) != null) {
                this.authentication = channel.getValueAsString(JCA_HTTP_SESSIONAUTH).toLowerCase();
                tracer.info("Channel parameter read : key = {0}", new Object[] { JCA_HTTP_SESSIONAUTH, this.authentication });
                if (this.authentication.equalsIgnoreCase("basic")) {
                    if (channel.getValueAsString(JCA_HTTP_USER) != null) {
                        this.user = channel.getValueAsString(JCA_HTTP_USER);
                        tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_USER, this.user });
                    } else {
                        tracer.error("Channel parameter reading error : key {0} is not set", JCA_HTTP_USER);
                    }
                    if (channel.getValueAsString(JCA_HTTP_PASSWORD) != null) {
                        this.password = channel.getValueAsString(JCA_HTTP_PASSWORD);
                        tracer.info("Channel parameter read : key {0}", JCA_HTTP_PASSWORD);
                    } else {
                        tracer.info("Channel parameter reading error : key {0} is not set", JCA_HTTP_PASSWORD);
                    }
                }
            } else {
                tracer.error("Channel parameter reading error : key {0}", JCA_HTTP_SESSIONAUTH);
            }
            this.useProxy = new Boolean(channel.getValueAsBoolean(JCA_HTTP_PROXY));
            if (this.useProxy.booleanValue()) {
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_PROXY, this.useProxy });
                if (channel.getValueAsString(JCA_HTTP_PROXY_HOST) != null) {
                    this.proxyHost = channel.getValueAsString(JCA_HTTP_PROXY_HOST);
                    tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_PROXY_HOST, this.proxyHost });
                } else {
                    tracer.error("Channel parameter reading error: key {0} is not set", JCA_HTTP_PROXY_HOST);
                }
                if (channel.getValueAsString(JCA_HTTP_PROXY_PORT) != null) {
                    this.proxyPort = new Integer(channel.getValueAsInt(JCA_HTTP_PROXY_PORT));
                    tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_PROXY_PORT, this.proxyPort });
                } else {
                    tracer.error("Channel parameter reading error: key {0} is not set", JCA_HTTP_PROXY_PORT);
                }
                if (channel.getValueAsString(JCA_HTTP_PROXY_USER) != null) {
                    this.proxyUser = channel.getValueAsString(JCA_HTTP_PROXY_USER);
                    tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_PROXY_USER, this.proxyUser });
                } else {
                    tracer.error("Channel parameter reading error: key {0} is not set", JCA_HTTP_PROXY_USER);
                }
                if (channel.getValueAsString(JCA_HTTP_PROXY_PASSWORD) != null) {
                    this.proxyPassword = channel.getValueAsString(JCA_HTTP_PROXY_PASSWORD);
                    tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_HTTP_PROXY_PASSWORD, this.proxyPassword });
                } else {
                    tracer.error("Channel parameter reading error: key {0} is not set", JCA_HTTP_PROXY_PASSWORD);
                }
            } else {
                tracer.info("Channel parameter read : no proxy settings defined");
            }
            if (channel.getValueAsString(JCA_CRYPT_USE_ENCRYPTION) != null) {
                this.useEncryption = channel.getValueAsBoolean(JCA_CRYPT_USE_ENCRYPTION);
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_CRYPT_USE_ENCRYPTION, new Boolean(this.useEncryption) });
            } else {
                tracer.error("Channel parameter read : key = {0} encryption not found", JCA_CRYPT_USE_ENCRYPTION);
            }
            if (channel.getValueAsString(JCA_CRYPT_USE_SIGNATURE) != null) {
                this.useSignature = channel.getValueAsBoolean(JCA_CRYPT_USE_SIGNATURE);
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_CRYPT_USE_SIGNATURE, new Boolean(this.useSignature) });
            } else {
                tracer.error("Channel parameter read : key = {0} signature not found", JCA_CRYPT_USE_SIGNATURE);
            }
            if (channel.getValueAsString(JCA_CRYPT_MICALG_OUT_MDN) != null) {
                this.micAlgMdn = channel.getValueAsString(JCA_CRYPT_MICALG_OUT_MDN);
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_CRYPT_MICALG_OUT_MDN, this.micAlgMdn });
            } else {
                tracer.error("Channel parameter read : key = {0} micAlgMdn not found", JCA_CRYPT_MICALG_OUT_MDN);
            }
            if (channel.getValueAsString(JCA_CRYPT_MICALG_OUT_REQ) != null) {
                this.micAlgReq = channel.getValueAsString(JCA_CRYPT_MICALG_OUT_REQ);
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_CRYPT_MICALG_OUT_REQ, this.micAlgReq });
            } else {
                tracer.error("Channel parameter read : key = {0} micAlgReq not found", JCA_CRYPT_MICALG_OUT_REQ);
            }
            if (channel.getValueAsString(JCA_AS2_USE_MDN) != null) {
                this.mdnRequired = channel.getValueAsBoolean(JCA_AS2_USE_MDN);
                tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { JCA_AS2_USE_MDN, new Boolean(this.mdnRequired) });
            } else {
                tracer.error("Channel parameter read : key = {0} signature not found", JCA_AS2_USE_MDN);
            }
        }
        if (this.useSignature && this.mdnRequired) {
            this.viewCertificateForMdnSignature = channel.getValueAsString(JCA_RCV_PUBKEY_CERTID_MDN_VIEW);
            if ((this.viewCertificateForMdnSignature == null) || (this.viewCertificateForMdnSignature.trim().equals(""))) {
                tracer.error("Channel parameter reading error : key {0}", JCA_RCV_PUBKEY_CERTID_MDN_VIEW);
                String errorMessage = "View certificate for MDN signature entry could not be retrieved from channel.";
                CPAException ce = new CPAException(errorMessage);
                tracer.throwing(ce);
                throw ce;
            }
            tracer.info("View MDN certificate parameter retrieved from channel. viewCertificateForMdnSignature: {0}", this.viewCertificateForMdnSignature);
            this.certificateForMdnSignature = channel.getValueAsString(JCA_RCV_PUBKEY_CERTID_MDN);
            if ((this.certificateForMdnSignature == null) || (this.certificateForMdnSignature.trim().equals(""))) {
                tracer.error("Channel parameter reading error : key {0}", JCA_RCV_PUBKEY_CERTID_MDN);
                String errorMessage = "Certificate for MDN signature entry could not be retrieved from channel.";
                CPAException ce = new CPAException(errorMessage);
                tracer.throwing(ce);
                throw ce;
            }
            tracer.info("MDN certificate parameter retrieved from channel. certificateForMdnSignature: {0}", this.certificateForMdnSignature);
        }
        tracer.leaving();
    }

    /**
     * String concatenation of all configuration parameters - for internal tests only
     * @return String String representation of concatenated configuration parameters
     */
    @Override
    public String toString() {
        String hash = "";
        if (this.protocol != null) {
            hash = hash + ";protocol=" + this.protocol;
        } else {
            hash = hash + ";protocol=<null>";
        }
        if (this.host != null) {
            hash = hash + ";host=" + this.host;
        } else {
            hash = hash + ";host=<null>";
        }
        if (this.port != null) {
            hash = hash + ";port=" + this.port;
        } else {
            hash = hash + ";port=<null>";
        }
        if (this.path != null) {
            hash = hash + ";path=" + this.path;
        } else {
            hash = hash + ";path=<null>";
        }
        if (this.query != null) {
            hash = hash + ";query=" + this.query;
        } else {
            hash = hash + ";query=<null>";
        }
        if (this.useProxy != null) {
            hash = hash + ";useProxy=" + this.useProxy.toString();
        } else {
            hash = hash + ";useProxy=<null>";
        }
        if (this.proxyHost != null) {
            hash = hash + ";proxyHost=" + this.proxyHost;
        } else {
            hash = hash + ";proxyHost=<null>";
        }
        if (this.proxyPort != null) {
            hash = hash + ";proxyPort=" + this.proxyPort.intValue();
        } else {
            hash = hash + ";proxyPort=<null>";
        }
        if (this.authentication != null) {
            hash = hash + ";authentication=" + this.authentication;
        } else {
            hash = hash + ";authentication=<null>";
        }
        if (this.user != null) {
            hash = hash + ";user=" + this.user;
        } else {
            hash = hash + ";user=<null>";
        }
        if (this.password != null) {
            hash = hash + ";password=" + this.password;
        } else {
            hash = hash + ";password=<null>";
        }
        if (this.adapterStatus != null) {
            hash = hash + ";adapterStatus=" + this.adapterStatus;
        } else {
            hash = hash + ";adapterStatus=<null>";
        }
        if (hash == null) {
            hash = "ConfigurationSapImpl is empty";
        }
        return hash;
    }
}
