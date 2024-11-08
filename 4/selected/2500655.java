package org.openmobster.core.services;

import java.util.List;
import java.util.ArrayList;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.openmobster.cloud.api.sync.Channel;
import org.openmobster.cloud.api.sync.ChannelInfo;
import org.openmobster.cloud.api.sync.MobileBeanId;
import org.openmobster.core.common.XMLUtilities;
import org.openmobster.core.common.database.HibernateManager;
import org.openmobster.core.common.bus.Bus;
import org.openmobster.core.common.bus.BusListener;
import org.openmobster.core.common.bus.BusMessage;
import org.openmobster.core.services.channel.ChannelManager;
import org.openmobster.core.services.channel.ChannelRegistration;
import org.openmobster.core.services.channel.ChannelBeanMetaData;
import org.openmobster.core.services.event.ChannelEvent;
import org.openmobster.core.services.event.NetworkEvent;
import org.openmobster.core.security.device.DeviceController;

/**
 * MobileObjectMonitor provides infrastructure/management services for the Mobile Object Framework
 * 
 * @author openmobster@gmail.com
 */
public class MobileObjectMonitor implements BusListener {

    private static Logger log = Logger.getLogger(MobileObjectMonitor.class);

    /**
	 * 
	 */
    private Map<String, Channel> registry = null;

    private Map<String, ChannelManager> channelManagers;

    private HibernateManager hibernateManager;

    private DeviceController deviceController;

    public MobileObjectMonitor() {
        this.registry = new HashMap<String, Channel>();
        this.channelManagers = new HashMap<String, ChannelManager>();
    }

    public void start() {
        log.info("--------------------------------------------------");
        log.info("Mobile Object Monitor succesfully started.........");
        log.info("--------------------------------------------------");
    }

    public void stop() {
        this.registry = null;
        if (this.channelManagers != null && !this.channelManagers.isEmpty()) {
            Collection<ChannelManager> channelManagers = this.channelManagers.values();
            for (ChannelManager channelManager : channelManagers) {
                channelManager.stop();
            }
        }
    }

    public HibernateManager getHibernateManager() {
        return hibernateManager;
    }

    public void setHibernateManager(HibernateManager hibernateManager) {
        this.hibernateManager = hibernateManager;
    }

    public DeviceController getDeviceController() {
        return deviceController;
    }

    public void setDeviceController(DeviceController deviceController) {
        this.deviceController = deviceController;
    }

    public void notify(Channel mobileObjectConnector) {
        try {
            Class connectorClazz = mobileObjectConnector.getClass();
            ChannelInfo connectorInfo = (ChannelInfo) connectorClazz.getAnnotation(ChannelInfo.class);
            String channelId = connectorInfo.uri();
            if (channelId.indexOf('/') != -1) {
                log.error("-----------------------------------------------------");
                log.error("ChannelUri: " + channelId + " is invalid!!");
                log.error("-----------------------------------------------------");
                throw new IllegalStateException("A ChannelUri should not contain the '/' character!!");
            }
            String dataObjectStr = connectorInfo.mobileBeanClass();
            Class dataObjectClazz = Thread.currentThread().getContextClassLoader().loadClass(dataObjectStr);
            Field[] declaredFields = dataObjectClazz.getDeclaredFields();
            for (Field field : declaredFields) {
                Annotation id = field.getAnnotation(MobileBeanId.class);
                if (id != null) {
                    Class type = field.getType();
                    if (type != String.class) {
                        log.error("Record Id must be of type <String> only!!!");
                        log.error(mobileObjectConnector + " cannot be registered!!!!");
                        throw new IllegalStateException("Record Id must be of type <String> only!!!");
                    }
                }
            }
            this.registry.put(channelId, mobileObjectConnector);
            ChannelRegistration registration = new ChannelRegistration(channelId, mobileObjectConnector);
            registration.setUpdateCheckInterval(connectorInfo.updateCheckInterval());
            ChannelManager channelManager = ChannelManager.createInstance(this.hibernateManager, this.deviceController, registration);
            channelManager.start();
            Bus.addBusListener(channelId, this);
            this.channelManagers.put(channelId, channelManager);
        } catch (ClassNotFoundException cne) {
            log.error(this, cne);
        }
    }

    public Channel lookup(String channelId) {
        return this.registry.get(channelId);
    }

    @Deprecated
    public Collection<Channel> getConnectors() {
        Collection<Channel> all = this.registry.values();
        return all;
    }

    public void messageIncoming(BusMessage busMessage) {
        String eventState = (String) busMessage.getAttribute(ChannelEvent.event);
        Object event = XMLUtilities.unmarshal(eventState);
        if (event instanceof ChannelEvent) {
            CometService cometService = CometService.getInstance();
            ChannelEvent channelEvent = (ChannelEvent) event;
            cometService.broadcastChannelEvent(channelEvent);
            List<String> updatedChannels = new ArrayList<String>();
            updatedChannels.add(channelEvent.getChannel());
            NetworkEvent networkEvent = new NetworkEvent();
            networkEvent.setUpdatedChannels(updatedChannels);
            List<ChannelBeanMetaData> channelMetaData = (List<ChannelBeanMetaData>) channelEvent.getAttribute(ChannelEvent.metadata);
            networkEvent.setAttribute(ChannelEvent.metadata, channelMetaData);
            cometService.broadcastNetworkEvent(networkEvent);
        }
        busMessage.acknowledge();
    }
}
