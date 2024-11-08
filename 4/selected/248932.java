package com.corratech.opensuite.deployer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import org.apache.log4j.Logger;
import com.corratech.opensuite.utils.Constants;
import com.corratech.opensuite.utils.CoreComponentUtil;
import com.corratech.opensuite.utils.DeployUtil;
import com.opensuite.bind.endpoint.EndpointProperty;
import com.opensuite.bind.services.endpointmanagement.GetEndpointPropertiesGuestRequest;
import com.opensuite.bind.services.endpointmanagement.GetEndpointPropertiesResponse;
import com.opensuite.bind.services.endpointmanagement.SetEndpointPropertiesGuestRequest;

public class Deployer extends Thread {

    private static Logger log = Logger.getLogger(Deployer.class);

    private File installDir;

    private DeliveryChannel channel;

    public DeliveryChannel getChannel() {
        return channel;
    }

    public Deployer(File installDir) {
        this.installDir = installDir;
        try {
            this.channel = DeployUtil.getCcontext().getDeliveryChannel();
        } catch (MessagingException e) {
            log.error(e);
        }
    }

    @Override
    public void run() {
        super.run();
        Properties properties = DeployUtil.loadProperties(installDir.getPath());
        try {
            syncWithCore(properties);
        } catch (Exception e) {
            log.warn("Some errors occured while synchronizing with Opensuite Core. Nested exception is " + e);
        }
    }

    public static void syncWithCore(Properties properties) throws Exception {
        if (properties == null) {
            return;
        }
        GetEndpointPropertiesGuestRequest getPropertiesRequest = new GetEndpointPropertiesGuestRequest();
        String bcName = properties.getProperty(Constants.BC_NAME);
        getPropertiesRequest.setBcName(bcName);
        GetEndpointPropertiesResponse getResponse = CoreComponentUtil.getInstance().getEndpointPropertiesGuest(getPropertiesRequest);
        List<EndpointProperty> missedProperties = new ArrayList<EndpointProperty>();
        Iterator i = properties.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String value = (String) properties.get(key);
            EndpointProperty missedProperty = new EndpointProperty();
            missedProperty.setKey(key);
            missedProperty.setValue(value);
            boolean found = false;
            for (EndpointProperty endpointProperty : getResponse.getEndpointProperties()) {
                if (key.equals(endpointProperty.getKey()) && value.equals(endpointProperty.getValue())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missedProperties.add(missedProperty);
            }
        }
        if (missedProperties.size() > 0) {
            SetEndpointPropertiesGuestRequest setPropertiesRequest = new SetEndpointPropertiesGuestRequest();
            setPropertiesRequest.setBcName(bcName);
            setPropertiesRequest.getEndpointProperties().addAll(missedProperties);
            CoreComponentUtil.getInstance().setEndpointPropertiesGuest(setPropertiesRequest);
        }
    }
}
