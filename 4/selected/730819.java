package org.slasoi.common.messaging.pubsub;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import static org.junit.Assert.*;

public abstract class PubSubManagerTestCommon {

    private static Logger log = Logger.getLogger(PubSubManagerTestCommon.class);

    String msgBody1;

    String msgBody2;

    String msgBody3;

    String receivedMsgChannelName;

    boolean msg1Received;

    int msg1ReceivedCounter;

    int psm1MsgCount;

    int psm2MsgCount;

    boolean msg2Received;

    PubSubManager pubSubManager1;

    PubSubManager pubSubManager2;

    @Test
    public void testPublish1() throws MessagingException, InterruptedException, IOException {
        msgBody1 = "<bla>Some message body 1</bla>";
        msg1ReceivedCounter = 0;
        msgBody2 = "Some message body 2";
        msgBody3 = "Some message body 3";
        receivedMsgChannelName = "";
        msg1Received = false;
        msg2Received = false;
        final String channelName1 = "testChannel-testPublish1-1";
        final String channelName2 = "testChannel-testPublish1-2";
        Properties props1 = loadPubSubManager1Properties();
        Properties props2 = loadPubSubManager2Properties();
        pubSubManager1 = PubSubFactory.createPubSubManager(props1);
        pubSubManager2 = PubSubFactory.createPubSubManager(props2);
        pubSubManager1.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                PubSubMessage message = messageEvent.getMessage();
                if (message.getPayload().equals(msgBody1)) {
                    fail("Message 1 received that should not be received.");
                }
            }
        });
        pubSubManager2.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                PubSubMessage message = messageEvent.getMessage();
                String messageText = message.getPayload();
                if (messageText.equals(msgBody1)) {
                    msg1ReceivedCounter++;
                    msg1Received = true;
                    assertEquals("Message1 channel name.", message.getChannelName(), channelName1);
                }
                if (messageText.equals(msgBody2)) {
                    msg2Received = true;
                }
                if (messageText.equals(msgBody3)) {
                    fail("Message3 received.");
                }
            }
        });
        pubSubManager1.createChannel(new Channel(channelName1));
        assertTrue("Channel 1 created.", pubSubManager1.isChannel(channelName1));
        pubSubManager1.createChannel(new Channel(channelName2));
        assertTrue(pubSubManager1.isChannel(channelName2));
        pubSubManager2.subscribe(channelName1);
        pubSubManager1.publish(new PubSubMessage(channelName1, msgBody1));
        pubSubManager1.publish(new PubSubMessage(channelName2, msgBody1));
        pubSubManager2.publish(new PubSubMessage(channelName1, msgBody2));
        Thread.sleep(200);
        pubSubManager2.unsubscribe(channelName1);
        pubSubManager1.publish(new PubSubMessage(channelName1, msgBody3));
        Thread.sleep(200);
        pubSubManager1.deleteChannel(channelName1);
        assertFalse(pubSubManager1.isChannel(channelName1));
        pubSubManager1.deleteChannel(channelName2);
        assertFalse(pubSubManager1.isChannel(channelName2));
        pubSubManager1.close();
        pubSubManager2.close();
        assertTrue("Message 1 received", msg1Received);
        assertEquals(msg1ReceivedCounter, 1);
        if (pubSubManager2.getSetting(Setting.xmpp_publish_echo_enabled).equals(Settings.TRUE)) {
            assertTrue("Message 2 received", msg2Received);
            pubSubManager1.setSetting(Setting.xmpp_publish_echo_enabled, Settings.TRUE);
        } else {
            assertFalse("Message 2 received", msg2Received);
        }
    }

    @Test
    public void testPublish2() throws IOException, MessagingException, InterruptedException {
        String channelName1 = "channel-test_asd3sdaf_-003";
        String channelName2 = "channel-test_asd3sdaf_-004";
        final String payload1 = "message1";
        msg1ReceivedCounter = 0;
        msg1Received = false;
        msg2Received = false;
        Properties props1 = loadPubSubManager1Properties();
        Properties props2 = loadPubSubManager2Properties();
        pubSubManager1 = PubSubFactory.createPubSubManager(props1);
        pubSubManager2 = PubSubFactory.createPubSubManager(props2);
        pubSubManager1.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                msg1ReceivedCounter++;
                if (messageEvent.getMessage().getPayload().equals(payload1)) {
                    log.debug("pubsubmanager1 received: " + messageEvent.getMessage().getPayload());
                    msg1Received = true;
                }
            }
        }, new String[] { channelName1 });
        pubSubManager2.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                if (messageEvent.getMessage().getPayload().equals(payload1)) {
                    log.debug("pubsubmanager2 received: " + messageEvent.getMessage().getPayload());
                    msg2Received = true;
                }
            }
        });
        pubSubManager1.createAndSubscribe(new Channel(channelName1));
        assertTrue(pubSubManager1.isChannel(channelName1));
        pubSubManager1.createChannel(new Channel(channelName2));
        pubSubManager1.subscribe(channelName2);
        assertTrue(pubSubManager1.isChannel(channelName2));
        pubSubManager2.publish(new PubSubMessage(channelName1, payload1));
        pubSubManager2.publish(new PubSubMessage(channelName2, payload1));
        Thread.sleep(200);
        pubSubManager1.deleteChannel(channelName1);
        pubSubManager1.deleteChannel(channelName2);
        pubSubManager1.close();
        pubSubManager2.close();
        assertTrue("Msg 1 received", msg1Received);
        assertEquals("Messages received", 1, msg1ReceivedCounter);
        assertFalse("Msg 2 not received", msg2Received);
    }

    @Test
    public void testPublishWithoutEcho() throws IOException, MessagingException, InterruptedException {
        String channelName1 = "channel_test_g3d443f_002";
        final String payload1 = "message1";
        final String payload2 = "message2";
        psm1MsgCount = 0;
        psm2MsgCount = 0;
        msg1Received = false;
        msg2Received = false;
        Properties props1 = loadPubSubManager1Properties();
        Properties props2 = loadPubSubManager2Properties();
        props1.setProperty("xmpp_publish_echo_enabled", "false");
        props2.setProperty("xmpp_publish_echo_enabled", "false");
        pubSubManager1 = PubSubFactory.createPubSubManager(props1);
        pubSubManager2 = PubSubFactory.createPubSubManager(props2);
        pubSubManager1.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                psm1MsgCount++;
                log.debug("pubsubmanager1 received: " + messageEvent.getMessage().getPayload());
                if (messageEvent.getMessage().getPayload().equals(payload1)) {
                    msg1Received = true;
                    try {
                        pubSubManager1.publish(new PubSubMessage(messageEvent.getMessage().getChannelName(), payload2));
                    } catch (MessagingException e) {
                        fail();
                    }
                }
            }
        });
        pubSubManager2.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                psm2MsgCount++;
                log.debug("pubsubmanager2 received: " + messageEvent.getMessage().getPayload());
                if (messageEvent.getMessage().getPayload().equals(payload2)) {
                    msg2Received = true;
                }
            }
        });
        pubSubManager1.createChannel(new Channel(channelName1));
        pubSubManager1.subscribe(channelName1);
        pubSubManager2.subscribe(channelName1);
        pubSubManager2.publish(new PubSubMessage(channelName1, payload1));
        Thread.sleep(200);
        pubSubManager1.unsubscribe(channelName1);
        pubSubManager2.unsubscribe(channelName1);
        pubSubManager1.deleteChannel(channelName1);
        pubSubManager1.close();
        pubSubManager2.close();
        assertTrue("Msg 1 received", msg1Received);
        assertTrue("Msg 2 received", msg2Received);
        assertEquals("Number of messages psm1 received", 1, psm1MsgCount);
        assertEquals("Number of messages psm2 received", 1, psm2MsgCount);
    }

    @Test
    public void testPublishWithEcho() throws IOException, MessagingException, InterruptedException {
        String channelName1 = "channel_test_g3d443f_002";
        final String payload1 = "message1";
        final String payload2 = "message2";
        psm1MsgCount = 0;
        psm2MsgCount = 0;
        msg1Received = false;
        msg2Received = false;
        Properties props1 = loadPubSubManager1Properties();
        Properties props2 = loadPubSubManager2Properties();
        props1.setProperty("xmpp_publish_echo_enabled", "true");
        props2.setProperty("xmpp_publish_echo_enabled", "true");
        pubSubManager1 = PubSubFactory.createPubSubManager(props1);
        pubSubManager2 = PubSubFactory.createPubSubManager(props2);
        pubSubManager1.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                psm1MsgCount++;
                log.debug("pubsubmanager1 received: " + messageEvent.getMessage().getPayload());
                if (messageEvent.getMessage().getPayload().equals(payload1)) {
                    msg1Received = true;
                    try {
                        pubSubManager1.publish(new PubSubMessage(messageEvent.getMessage().getChannelName(), payload2));
                    } catch (MessagingException e) {
                        fail();
                    }
                }
            }
        });
        pubSubManager2.addMessageListener(new MessageListener() {

            public void processMessage(MessageEvent messageEvent) {
                psm2MsgCount++;
                log.debug("pubsubmanager2 received: " + messageEvent.getMessage().getPayload());
                if (messageEvent.getMessage().getPayload().equals(payload2)) {
                    msg2Received = true;
                }
            }
        });
        pubSubManager1.createChannel(new Channel(channelName1));
        pubSubManager1.subscribe(channelName1);
        pubSubManager2.subscribe(channelName1);
        pubSubManager2.publish(new PubSubMessage(channelName1, payload1));
        Thread.sleep(200);
        pubSubManager1.unsubscribe(channelName1);
        pubSubManager2.unsubscribe(channelName1);
        pubSubManager1.deleteChannel(channelName1);
        pubSubManager1.close();
        pubSubManager2.close();
        assertTrue("Msg 1 received", msg1Received);
        assertTrue("Msg 2 received", msg2Received);
        assertEquals("Number of messages psm1 received", 2, psm1MsgCount);
        assertEquals("Number of messages psm2 received", 2, psm2MsgCount);
    }

    @Test
    public void testSubscriptions() throws IOException, MessagingException {
        final String channelName1 = "testChannel-testSubscriptions";
        Properties props1 = loadPubSubManager1Properties();
        pubSubManager1 = PubSubFactory.createPubSubManager(props1);
        pubSubManager1.createAndSubscribe(new Channel(channelName1));
        assertTrue(pubSubManager1.isChannel(channelName1));
        List<Subscription> subscriptions = pubSubManager1.getSubscriptions();
        boolean subscriptionExists = false;
        for (Subscription subscription : subscriptions) {
            if (subscription.getName().endsWith(channelName1)) {
                subscriptionExists = true;
            }
        }
        assertTrue("Subscription exists.", subscriptionExists);
        pubSubManager1.deleteChannel(channelName1);
        pubSubManager1.close();
    }

    abstract Properties loadPubSubManager1Properties() throws IOException;

    abstract Properties loadPubSubManager2Properties() throws IOException;

    Properties loadPropertiesFile(String filePath) throws IOException {
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(filePath);
        props.load(fis);
        fis.close();
        return props;
    }
}
