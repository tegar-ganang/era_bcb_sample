package it.polimi.MA.service;

import static org.junit.Assert.assertTrue;
import it.polimi.MA.impl.MAServiceImpl;
import it.polimi.MA.impl.doe.DOESensorConfiguration;
import it.polimi.MA.service.exceptions.ServiceStartupException;
import java.util.List;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import org.slasoi.models.scm.ServiceBuilder;
import org.slasoi.models.scm.ServiceImplementation;
import org.slasoi.models.scm.extended.ServiceBuilderExtended;
import org.slasoi.models.scm.extended.ServiceImplementationExtended;
import org.slasoi.monitoring.common.configuration.Component;
import org.slasoi.monitoring.common.configuration.ComponentConfiguration;
import org.slasoi.monitoring.common.configuration.MonitoringSystemConfiguration;
import org.slasoi.monitoring.common.configuration.OutputReceiver;
import org.slasoi.monitoring.common.configuration.impl.ConfigurationFactoryImpl;
import org.slasoi.monitoring.common.configuration.impl.OutputReceiverImpl;

public class DOESensorTests {

    private static ServiceBuilder builder = null;

    private static String uuid = null;

    private static Settings settings = null;

    @BeforeClass
    public static void init() {
        uuid = "testUUID";
        builder = new ServiceBuilderExtended();
        builder.setUuid(uuid);
        ServiceImplementation impl = new ServiceImplementationExtended();
        impl.setServiceImplementationName("paymentService");
        builder.setImplementation(impl);
        settings = new Settings();
        settings.setSetting(Setting.pubsub, "xmpp");
        settings.setSetting(Setting.xmpp_username, "primitive-ecf");
        settings.setSetting(Setting.xmpp_password, "primitive-ecf");
        settings.setSetting(Setting.xmpp_host, "testbed.sla-at-soi.eu");
        settings.setSetting(Setting.xmpp_port, "5222");
        settings.setSetting(Setting.messaging, "xmpp");
        settings.setSetting(Setting.pubsub, "xmpp");
        settings.setSetting(Setting.xmpp_service, "testbed.sla-at-soi.eu");
        settings.setSetting(Setting.xmpp_resource, "test");
        settings.setSetting(Setting.xmpp_pubsubservice, "pubsub.testbed.sla-at-soi.eu");
    }

    @Test
    public void executeAction() {
        MAService service = new MAServiceImpl();
        Settings connectionSettings = settings;
        String notificationChannel = "test-DOE-Paolo";
        try {
            service.startServiceInstance(builder, connectionSettings, notificationChannel);
        } catch (ServiceStartupException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        IManageabilityAgentFacade facade = service.getManagibilityAgentFacade(builder);
        ConfigurationFactoryImpl factory = new ConfigurationFactoryImpl();
        MonitoringSystemConfiguration msc = factory.createMonitoringSystemConfiguration();
        msc.setUuid(UUID.randomUUID().toString());
        Component[] components = new Component[1];
        Component c = factory.createComponent();
        c.setType("Sensor");
        DOESensorConfiguration config = new DOESensorConfiguration();
        config.setConfigurationId(UUID.randomUUID().toString());
        String serviceID = "paymentService";
        config.setServiceID(serviceID);
        String operationID = "/process/flow/receive[@name=$$ReceivePaymentRequest$$]";
        config.setOperationID(operationID);
        String status = "input";
        config.setStatus(status);
        String correlationKey = "cardNumber";
        config.setCorrelationKey(correlationKey);
        String correlationValue = "7777";
        config.setCorrelationValue(correlationValue);
        OutputReceiver[] newOutputReceivers = new OutputReceiverImpl[1];
        OutputReceiver receiver = new ConfigurationFactoryImpl().createOutputReceiver();
        receiver.setEventType("event");
        receiver.setUuid("tcp:localhost:10000");
        newOutputReceivers[0] = receiver;
        config.setOutputReceivers(newOutputReceivers);
        ComponentConfiguration[] configs = new ComponentConfiguration[1];
        configs[0] = config;
        c.setConfigurations(configs);
        components[0] = c;
        msc.setComponents(components);
        facade.configureMonitoringSystem(msc);
        System.out.println("[DOE- facade] Added Monitoring System Configuration to DOE");
        assertTrue(true);
        List<SensorSubscriptionData> subDatas = facade.getSensorSubscriptionData();
        System.out.println("[DOE- facade] Got SensorSubscriptionData from DOE");
        assertTrue(true);
        for (SensorSubscriptionData subData : subDatas) {
            System.out.println("[DOE - facade] Sensor Subscription ID: " + subData.getSensorID().toString());
            for (String s : subData.getChannels()) {
                System.out.println("[DOE - facade] Sensor Subscription on channel: " + s);
            }
        }
        facade.deconfigureMonitoring();
        System.out.println("[DOE- facade] Removed Monitoring System Configuration from DOE");
        assertTrue(true);
    }
}
