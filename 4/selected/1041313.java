package org.xmatthew.spy2servers.jmx;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import org.apache.log4j.Logger;
import org.springframework.jmx.export.MBeanExporter;
import org.xmatthew.spy2servers.core.AbstractComponent;
import org.xmatthew.spy2servers.core.AlertComponent;
import org.xmatthew.spy2servers.core.Component;
import org.xmatthew.spy2servers.core.MessageAlertChannel;
import org.xmatthew.spy2servers.core.MessageAlertChannelActiveAwareComponent;
import org.xmatthew.spy2servers.core.SpyComponent;
import org.xmatthew.spy2servers.core.SpyServerCoreConstant;
import org.xmatthew.spy2servers.util.Assert;
import org.xmatthew.spy2servers.util.CollectionUtils;
import org.xmatthew.spy2servers.util.ComponentUtils;

/**
 * @author Matthew Xie
 *
 */
public class JmxServiceComponent extends AbstractComponent implements MessageAlertChannelActiveAwareComponent {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(JmxServiceComponent.class);

    private MBeanExporter mbeanExporter;

    private SpyComponentView sypComponentView;

    private AlertComponentView alertComponentView;

    private ChannelAwareComponentView channelAwareComponentView;

    private int port = 1099;

    public void stop() {
        try {
            if (mbeanExporter != null) {
                mbeanExporter.destroy();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        status = ST_STOP;
        statusName = ST_STOP_NAME;
    }

    @SuppressWarnings("unchecked")
    public void startup() {
        status = ST_RUN;
        statusName = ST_RUN_NAME;
        if (getContext() == null) {
            return;
        }
        List<Component> components = getContext().getComponents();
        if (CollectionUtils.isBlankCollection(components)) {
            return;
        }
        mbeanExporter = new MBeanExporter();
        String mbeanNamePrefix = SpyServerCoreConstant.MAIN_PACKAGE + ":type=Component";
        String spyCompoentMBeanNamePrefix = mbeanNamePrefix + ",class=SpyComopent,";
        String alertCompoentMBeanNamePrefix = mbeanNamePrefix + ",class=AlertComopent,";
        String channelAwareCompoentMBeanNamePrefix = mbeanNamePrefix + ",class=ChannelAwareComopent,";
        Map mbeans = new HashMap(components.size());
        boolean isNeedJMXStart = false;
        String mbeanComponentName;
        for (Component component : components) {
            if (component instanceof SpyComponent) {
                isNeedJMXStart = true;
                sypComponentView = new SpyComponentView((SpyComponent) component);
                mbeanComponentName = spyCompoentMBeanNamePrefix + "name=" + ComponentUtils.getComponentName(component);
                mbeanComponentName = getUniMBeanName(mbeans, mbeanComponentName);
                mbeans.put(mbeanComponentName, sypComponentView);
            }
            if (component instanceof AlertComponent) {
                isNeedJMXStart = true;
                alertComponentView = new AlertComponentView((AlertComponent) component);
                mbeanComponentName = alertCompoentMBeanNamePrefix + "name=" + ComponentUtils.getComponentName(component);
                mbeanComponentName = getUniMBeanName(mbeans, mbeanComponentName);
                mbeans.put(mbeanComponentName, alertComponentView);
            }
            if (component instanceof MessageAlertChannelActiveAwareComponent) {
                isNeedJMXStart = true;
                channelAwareComponentView = new ChannelAwareComponentView((MessageAlertChannelActiveAwareComponent) component);
                mbeanComponentName = channelAwareCompoentMBeanNamePrefix + "name=" + ComponentUtils.getComponentName(component);
                mbeanComponentName = getUniMBeanName(mbeans, mbeanComponentName);
                mbeans.put(mbeanComponentName, channelAwareComponentView);
            }
        }
        if (isNeedJMXStart) {
            ManagementContext managementContext = null;
            if (port > 0) {
                managementContext = new ManagementContext(1099);
                MBeanServer beanServer = managementContext.getMBeanServer();
                mbeanExporter.setServer(beanServer);
            }
            mbeanExporter.setBeans(mbeans);
            try {
                if (managementContext != null) managementContext.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mbeanExporter.afterPropertiesSet();
        }
    }

    protected String getUniMBeanName(Map mbeans, String name) {
        String uniName = name;
        int order = 1;
        while (mbeans.containsKey(uniName)) {
            uniName = name + order;
        }
        return uniName;
    }

    public void onMessageAlertChannelActive(MessageAlertChannel channel) {
        if (channels == null) {
            channels = new LinkedList<MessageAlertChannel>();
        }
        Assert.notNull(channel, "messageAlertChannel is null");
        channels.add(channel);
        LOGGER.debug("message channel active.");
    }

    private List<MessageAlertChannel> channels;

    public List<MessageAlertChannel> getChannels() {
        return channels;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }
}
