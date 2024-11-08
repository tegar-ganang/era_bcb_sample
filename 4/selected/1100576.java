package net.sf.traser.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.PolicyOperator;
import org.apache.rampart.RampartMessageData;
import org.apache.rampart.policy.model.RampartConfig;

/**
 * @author karnokd, 2007.11.27.
 * @version $Revision 1.0$
 */
public class CommunicatorImpl implements Communicator, ConfigInitializable {

    /**
	 * The rule to apply when searching for endpoint configuration properties.
	 * The associated values to the configuration attribute values
	 * <li>0: Local-Only</li>
	 * <li>1: Store-Only</li>
	 * <li>2: Local-First</li>
	 * <li>3: Store-First</li>
	 * 
	 */
    private int rule;

    /**
	 * The communication store object, if defined.
	 */
    private CommunicatorStore store;

    /**
	 * The policy file in binary immutable format.
	 */
    private byte[] policy;

    /**
	 * The optional path to the client repository.
	 */
    private String repository;

    /**
	 * The path to the axis2 client configuration file.
	 */
    private String axisconfig;

    /**
	 * The traser endpoint map configuration containing
	 * endpoint url as the key and TraserEndpoint object as a value.
	 */
    private Map<String, TraserEndpoint> endpointMap;

    @Override
    public OMElement query(String targetURL, OMElement payload) {
        ServiceClient client = prepareRemoteCall(targetURL);
        try {
            return client.sendReceive(payload);
        } catch (AxisFault ex) {
            if (ex.getMessage().indexOf("null") > 0) {
                return null;
            }
            throw new RuntimeException("Failed to query " + targetURL, ex);
        } finally {
            try {
                client.cleanup();
            } catch (AxisFault ex) {
            }
        }
    }

    @Override
    public void send(String targetURL, OMElement payload) {
        ServiceClient client = prepareRemoteCall(targetURL);
        try {
            client.sendRobust(payload);
        } catch (AxisFault ex) {
            throw new RuntimeException("Failed to send payload to " + targetURL, ex);
        } finally {
            try {
                client.cleanup();
            } catch (AxisFault ex) {
            }
        }
    }

    private ServiceClient prepareRemoteCall(String targetURL) {
        TraserEndpoint endpoint = null;
        if (rule == 0 || rule == 2) {
            endpoint = endpointMap.get(targetURL);
        }
        if (endpoint == null && (rule == 1 || rule == 3)) {
            endpoint = store.getTraserEndpoint(targetURL);
        }
        if (endpoint == null && rule == 3) {
            endpoint = endpointMap.get(targetURL);
        }
        if (endpoint == null) {
            throw new RuntimeException("Could not determine endpoint properties for " + targetURL);
        }
        try {
            MessageContext msgCtx = MessageContext.getCurrentMessageContext();
            ConfigurationContext configContext = null;
            if (msgCtx != null) {
                configContext = msgCtx.getConfigurationContext();
            } else {
                configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repository, axisconfig);
            }
            ServiceClient client = new ServiceClient(configContext, null);
            Options opts = new Options();
            opts.setAction("urn:service");
            opts.setTo(new EndpointReference(endpoint.getUrl()));
            opts.setTransportInProtocol(Constants.TRANSPORT_HTTP);
            if (endpoint.isSecure()) {
                Policy policy = PolicyEngine.getPolicy(new ByteArrayInputStream(this.policy));
                if (policy != null) {
                    RampartConfig rampart = locateRampartConfig(policy);
                    if (rampart != null) {
                        rampart.setEncryptionUser(endpoint.getAlias());
                    }
                    opts.setProperty(RampartMessageData.KEY_RAMPART_POLICY, policy);
                } else {
                    throw new RuntimeException("The Policy object was not created by PolicyEngine.");
                }
            }
            client.setOptions(opts);
            client.engageModule("addressing");
            if (endpoint.isSecure()) {
                client.engageModule("rampart");
            }
            return client;
        } catch (AxisFault ex) {
            throw new RuntimeException("Could not initialize the ServiceClient instance ", ex);
        }
    }

    /**
	 * Locate the Rampart configuration on a Policy object.
	 * @param policy the Policy object
	 * @return the RampartConfig or null if not found
	 */
    private static RampartConfig locateRampartConfig(PolicyOperator policy) {
        RampartConfig result = null;
        List<?> components = policy.getPolicyComponents();
        for (Object o : components) {
            if (o instanceof PolicyOperator) {
                result = locateRampartConfig((PolicyOperator) o);
                if (result != null) {
                    break;
                }
            } else if (o instanceof RampartConfig) {
                result = (RampartConfig) o;
                break;
            }
        }
        return result;
    }

    @Override
    public void finish() {
    }

    @Override
    public void init(OMElement configuration, ConfigManager mgr) {
        endpointMap = new ConcurrentHashMap<String, TraserEndpoint>();
        String rule = configuration.getAttributeValue(RULE);
        if (LOCAL_ONLY.equals(rule)) {
            this.rule = 0;
        } else if (STORE_ONLY.equals(rule)) {
            this.rule = 1;
        } else if (LOCAL_FIRST.equals(rule)) {
            this.rule = 2;
        } else if (STORE_FIRST.equals(rule)) {
            this.rule = 3;
        } else {
            throw new RuntimeException("The rule attribute contains an invalid value: " + rule);
        }
        String policyFile = configuration.getAttributeValue(POLICY);
        InputStream policyStream = ResourceLocator.findAsStream(policyFile);
        try {
            if (policyStream == null) {
                throw new RuntimeException("The policy file " + policyFile + " was not found.");
            }
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
            int read = 0;
            while ((read = policyStream.read(buffer)) >= 0) {
                if (read > 0) {
                    bout.write(buffer, 0, read);
                }
            }
            this.policy = bout.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Problem while loading the policy file " + policyFile, ex);
        } finally {
            if (policyStream != null) {
                try {
                    policyStream.close();
                } catch (IOException ex) {
                }
            }
        }
        this.repository = configuration.getAttributeValue(REPOSITORY);
        this.axisconfig = repository + "/client.axis2.xml";
        OMElement storeConfig = configuration.getFirstChildWithName(STORE);
        if (storeConfig != null) {
            Object storeObj = mgr.initInterface(storeConfig);
            if (storeObj instanceof CommunicatorStore) {
                store = (CommunicatorStore) storeObj;
            } else {
                throw new RuntimeException("The supplied Store object does not implement the CommunicatorStore interface");
            }
        } else {
            if (this.rule > 0) {
                throw new RuntimeException("The Store configuration must be present if the rule attribute is not LOCAL_ONLY!");
            }
        }
        Iterator<?> targets = configuration.getChildrenWithName(TARGET);
        while (targets.hasNext()) {
            OMElement target = (OMElement) targets.next();
            TraserEndpoint endpoint = new TraserEndpoint(target);
            endpointMap.put(endpoint.getUrl(), endpoint);
        }
    }

    /**
	 * The QName of the Store element.
	 */
    private static final QName STORE = new QName("Store");

    /**
	 * The QName of the rule attribute.
	 */
    private static final QName RULE = new QName("rule");

    /**
	 * The QName of the policy attribute.
	 */
    private static final QName POLICY = new QName("policy");

    /**
	 * The QName of the repository attribute.
	 */
    private static final QName REPOSITORY = new QName("repository");

    /**
	 * The QName of the Target element.
	 */
    private static final QName TARGET = new QName("Target");

    /**
	 * Constant for the rule attribute value.
	 * Search for endpoint configuration locally, then in the store.
	 */
    private static final String LOCAL_FIRST = "Local-First";

    /**
	 * Constant for the rule attribute value.
	 * Search for endpoint configuration in store, then locally.
	 */
    private static final String STORE_FIRST = "Store-First";

    /**
	 * Constant for the rule attribute value.
	 * Search the endpoint configuration locally only.
	 */
    private static final String LOCAL_ONLY = "Local-Only";

    /**
	 * Constant for the rule attribute value.
	 * Search the endpoint configuration in store only.
	 */
    private static final String STORE_ONLY = "Store-Only";
}
