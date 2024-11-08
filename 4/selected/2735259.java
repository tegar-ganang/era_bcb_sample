package com.volantis.mps;

import com.volantis.mcs.runtime.Volantis;
import com.volantis.mcs.runtime.configuration.MpsChannelConfiguration;
import com.volantis.mcs.runtime.configuration.MpsPluginConfiguration;
import com.volantis.mcs.testtools.application.AppContext;
import com.volantis.mcs.testtools.application.AppExecutor;
import com.volantis.mcs.testtools.application.AppManager;
import com.volantis.mps.channels.LogicaSMSChannelAdapter;
import com.volantis.mps.channels.SMTPChannelAdapter;
import com.volantis.mps.recipient.MessageRecipient;
import com.volantis.mps.recipient.MessageRecipientInfo;
import com.volantis.testtools.config.ConfigValueChannel;
import com.volantis.testtools.config.ConfigValueChannelMms;
import com.volantis.testtools.config.ConfigValueChannelSms;
import com.volantis.testtools.config.ConfigValueChannelSmtp;
import com.volantis.testtools.config.MPSAppConfigurator;
import com.volantis.testtools.config.MPSPluginConfigBuilder;
import com.volantis.testtools.config.MPSPluginConfigValue;
import com.volantis.testtools.config.PluginConfigFileBuilder;
import com.volantis.testtools.stubs.ServletContextStub;
import junit.framework.TestCase;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A test case to ensure that we can read our config info from MCS's Volantis 
 * bean.
 */
public class ConfigurationTestCase extends TestCase {

    public ConfigurationTestCase(String s) {
        super(s);
    }

    private Volantis volantis;

    private ServletContextStub servletContext;

    /**
     * A "dummy" instance of {@link MessageRecipientInfo} to use for testing.
     */
    public static class TestMessageRecipient implements MessageRecipientInfo {

        public String resolveDeviceName(MessageRecipient recipient) {
            return null;
        }

        public String resolveChannelName(MessageRecipient recipient) {
            return null;
        }

        public String resolveCharacterEncoding(MessageRecipient recipient) {
            return null;
        }
    }

    public void testDummy() {
    }

    /**
     * Test that the entire "full" value of the MPS configuration can be 
     * found, all at the same time.
     * 
     * @todo refactor this into multiple tests ala MCS
     */
    public void notestMPSConfig() throws Exception {
        final MPSPluginConfigValue mps = createMpsValue();
        MPSAppConfigurator appConf = new MPSAppConfigurator();
        appConf.setPluginConfigValue(mps);
        volantis = new Volantis();
        servletContext = new ServletContextStub();
        AppManager am = new AppManager(volantis, servletContext);
        am.setAppConf(appConf);
        PluginConfigFileBuilder builder = new MPSPluginConfigBuilder();
        am.registerPluginConfigFileBuilder(builder, mps.getClass());
        am.useAppWith(new AppExecutor() {

            public void execute(AppContext context) {
                checkConfiguration(volantis, mps);
            }
        });
    }

    private MPSPluginConfigValue createMpsValue() {
        MPSPluginConfigValue mps;
        mps = new MPSPluginConfigValue();
        mps.internalBaseUrl = "mps internal base url";
        mps.messageRecipientInfo = TestMessageRecipient.class.getName();
        ConfigValueChannelSms sms = new ConfigValueChannelSms();
        sms.channelClass = "com.volantis.mps.channels.LogicaSMSChannelAdapter";
        sms.name = "demo sms";
        sms.address = "sms address";
        sms.port = new Integer(80);
        sms.userName = "sms user";
        sms.password = "sms password";
        sms.bindtype = "sms bindtype";
        sms.serviceType = "sms service type";
        sms.serviceAddress = "sms service address";
        sms.supportsMulti = Boolean.FALSE;
        mps.channels.add(sms);
        ConfigValueChannelSmtp smtp = new ConfigValueChannelSmtp();
        smtp.channelClass = "com.volantis.mps.channels.SMTPChannelAdapter";
        smtp.name = "demo smtp";
        smtp.hostName = "smtp hostname";
        smtp.authorisationEnabled = Boolean.TRUE;
        smtp.userName = "smtp user";
        smtp.password = "smtp password";
        mps.channels.add(smtp);
        return mps;
    }

    private void checkConfiguration(Volantis volantisBean, MPSPluginConfigValue mps) {
        MpsPluginConfiguration config = (MpsPluginConfiguration) volantisBean.getApplicationPluginConfiguration("MPS");
        String ibu = config.getInternalBaseUrl();
        String mri = config.getMessageRecipientInfo();
        assertEquals("internal-base-url", mps.internalBaseUrl, ibu);
        assertEquals("message-recipient-info", mps.messageRecipientInfo, mri);
        Map channelTable = new HashMap();
        Iterator channelsIterator = config.getChannelsIterator();
        while (channelsIterator.hasNext()) {
            MpsChannelConfiguration channelConfig = (MpsChannelConfiguration) channelsIterator.next();
            channelTable.put(channelConfig.getName(), channelConfig);
        }
        Iterator channels = mps.channels.iterator();
        while (channels.hasNext()) {
            ConfigValueChannel channel = (ConfigValueChannel) channels.next();
            MpsChannelConfiguration channelConfig = (MpsChannelConfiguration) channelTable.get(channel.name);
            Map attrs = channelConfig.getArguments();
            assertNotNull("Channel Config for " + channel.name, channelConfig);
            assertEquals(channel.name, channelConfig.getName());
            assertEquals(channel.channelClass, channelConfig.getClassName());
            if (channel instanceof ConfigValueChannelSms) {
                ConfigValueChannelSms sms = (ConfigValueChannelSms) channel;
                assertEquals(sms.address, attrs.get(LogicaSMSChannelAdapter.ADDRESS));
                assertEquals(valueOf(sms.port), attrs.get(LogicaSMSChannelAdapter.PORT));
                assertEquals(sms.userName, attrs.get(LogicaSMSChannelAdapter.USERNAME));
                assertEquals(sms.password, attrs.get(LogicaSMSChannelAdapter.PASSWORD));
                assertEquals(sms.bindtype, attrs.get(LogicaSMSChannelAdapter.BINDTYPE));
                assertEquals(sms.serviceType, attrs.get(LogicaSMSChannelAdapter.SERVICE_TYPE));
                assertEquals(sms.serviceAddress, attrs.get(LogicaSMSChannelAdapter.SERVICE_ADDRESS));
                assertEquals(valueOf(sms.supportsMulti), attrs.get(LogicaSMSChannelAdapter.SUPPORTS_MULTI));
            } else if (channel instanceof ConfigValueChannelSmtp) {
                ConfigValueChannelSmtp smtp = (ConfigValueChannelSmtp) channel;
                assertEquals(smtp.hostName, attrs.get(SMTPChannelAdapter.HOST_NAME));
                assertEquals(valueOf(smtp.authorisationEnabled), attrs.get(SMTPChannelAdapter.REQUIRES_AUTH));
                assertEquals(smtp.userName, attrs.get(SMTPChannelAdapter.USER_NAME));
                assertEquals(smtp.password, attrs.get(SMTPChannelAdapter.PASSWORD));
            }
        }
    }

    /**
     * Similar to String.valueOf(), but returns null instead of "null", if the
     * value is null.
     * 
     * @param value to turn to a string, or null
     * @return string value of param, or null if it was null.
     */
    private static String valueOf(Object value) {
        if (value == null) {
            return null;
        } else {
            return value.toString();
        }
    }
}
