package br.unb.unbiquitous.ubiquitos.me.uos.connectivity.messages;

import java.util.Hashtable;

public class ServiceCall extends Message {

    /** enum to specify the type of data transmission from the called service*/
    public static class ServiceType {

        public static final ServiceType DISCRETE = new ServiceType("DISCRETE");

        public static final ServiceType STREAM = new ServiceType("STREAM");

        public final String name;

        private ServiceType(String name) {
            this.name = name;
        }

        public static ServiceType valueOf(String type) {
            if (type.equals(DISCRETE.name)) {
                return DISCRETE;
            } else if (type.equals(STREAM.name)) {
                return STREAM;
            } else {
                return null;
            }
        }

        public String toString() {
            return name;
        }
    }

    private String driver;

    private String service;

    private Hashtable parameters;

    private String instanceId;

    private ServiceType serviceType;

    private int channels;

    private String[] channelIDs;

    private String channelType;

    private String securityType;

    public ServiceCall() {
        setType(Message.Type.SERVICE_CALL_REQUEST);
        setServiceType(ServiceType.DISCRETE);
        setChannels(1);
    }

    public ServiceCall(String driver, String service) {
        this();
        this.driver = driver;
        this.service = service;
    }

    public ServiceCall(String driver, String service, String instanceId) {
        this(driver, service);
        this.instanceId = instanceId;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Hashtable getParameters() {
        return parameters;
    }

    public void setParameters(Hashtable parameters) {
        this.parameters = parameters;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public String[] getChannelIDs() {
        return channelIDs;
    }

    public void setChannelIDs(String[] channelIDs) {
        this.channelIDs = channelIDs;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public ServiceCall addParameter(String key, String value) {
        if (parameters == null) {
            parameters = new Hashtable();
        }
        parameters.put(key, value);
        return this;
    }

    public String getParameter(String key) {
        if (parameters != null) {
            return (String) parameters.get(key);
        }
        return null;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ServiceCall)) {
            return false;
        }
        ServiceCall temp = (ServiceCall) obj;
        if (!(this.driver == temp.driver || (this.driver != null && this.driver.equals(temp.driver)))) {
            return false;
        }
        if (!(this.instanceId == temp.instanceId || (this.instanceId != null && this.instanceId.equals(temp.instanceId)))) {
            return false;
        }
        if (!(this.parameters == temp.parameters || (this.parameters != null && this.parameters.equals(temp.parameters)))) {
            return false;
        }
        if (!(this.service == temp.service || (this.service != null && this.service.equals(temp.service)))) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        if (this.driver != null) {
            hash += this.driver.hashCode();
        }
        if (this.instanceId != null) {
            hash += this.instanceId.hashCode();
        }
        if (this.parameters != null) {
            hash += this.parameters.hashCode();
        }
        if (this.service != null) {
            hash += this.service.hashCode();
        }
        if (hash != 0) {
            return hash;
        }
        return super.hashCode();
    }

    /**
	 * @return the securityType
	 */
    public String getSecurityType() {
        return securityType;
    }

    /**
	 * @param securityType the securityType to set
	 */
    public void setSecurityType(String securityType) {
        this.securityType = securityType;
    }
}
