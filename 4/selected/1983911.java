package org.slasoi.common.messaging.pubsub.inmemory;

import org.apache.log4j.Logger;
import org.jetlang.channels.ChannelSubscription;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.core.Callback;
import org.jetlang.core.Filter;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PubSubManager extends org.slasoi.common.messaging.pubsub.PubSubManager {

    private static final Logger log = Logger.getLogger(PubSubManager.class);

    private static HashMap<String, org.jetlang.channels.MemoryChannel<PubSubMessage>> channels = new HashMap<String, MemoryChannel<PubSubMessage>>();

    private static List<Subscription> subscriptions = new ArrayList<Subscription>();

    private Fiber fiber;

    private String id;

    public PubSubManager(Settings settings) throws MessagingException {
        super(settings);
        id = UUID.randomUUID().toString();
        fiber = FiberFactory.getInstance().createFiber();
        connect();
    }

    @Override
    public void close() throws MessagingException {
        fiber.dispose();
    }

    @Override
    protected void connect() throws MessagingException {
        fiber.start();
    }

    @Override
    public void createChannel(Channel channel) throws MessagingException {
        channels.put(channel.getName(), new org.jetlang.channels.MemoryChannel<PubSubMessage>());
    }

    @Override
    public void deleteChannel(String channel) throws MessagingException {
        channels.remove(channel);
    }

    @Override
    public String getId() throws MessagingException {
        return id;
    }

    @Override
    public List<org.slasoi.common.messaging.pubsub.Subscription> getSubscriptions() throws MessagingException {
        List<org.slasoi.common.messaging.pubsub.Subscription> subs = new ArrayList<org.slasoi.common.messaging.pubsub.Subscription>();
        Iterator<Subscription> itor = getSubscriptions(fiber).iterator();
        while (itor.hasNext()) {
            Subscription subscription = itor.next();
            subs.add(new org.slasoi.common.messaging.pubsub.Subscription(null, subscription.getChannel()));
        }
        return subs;
    }

    private List<Subscription> getSubscriptions(Fiber fiber) {
        List<Subscription> subs = new ArrayList<Subscription>();
        Iterator<Subscription> itor = subscriptions.iterator();
        while (itor.hasNext()) {
            Subscription subscription = itor.next();
            if (subscription.getFiber() == fiber) {
                subs.add(subscription);
            }
        }
        return subs;
    }

    @Override
    public boolean isChannel(String channel) throws MessagingException {
        return channels.containsKey(channel);
    }

    @Override
    public void publish(PubSubMessage message) throws MessagingException {
        message.setFrom(id);
        if (channels.containsKey(message.getChannelName())) {
            channels.get(message.getChannelName()).publish(message);
        } else {
            log.info("No channel named " + message.getChannelName());
        }
    }

    @Override
    public void subscribe(String channel) throws MessagingException {
        if (channels.containsKey(channel)) {
            ChannelSubscription<PubSubMessage> subscription = null;
            if (getFilter()) {
                subscription = new ChannelSubscription<PubSubMessage>(fiber, new PubSubCallback(), new DenySentFromSelfFilter());
            } else {
                subscription = new ChannelSubscription<PubSubMessage>(fiber, new PubSubCallback());
            }
            subscriptions.add(new Subscription(fiber, channel, subscription));
            channels.get(channel).subscribe(subscription);
        } else {
            log.info("Channel " + channel + " does not exist.");
        }
    }

    private boolean getFilter() throws MessagingException {
        String setting = getSetting(Setting.xmpp_publish_echo_enabled);
        if (setting == null || setting.equals(Settings.FALSE)) {
            return true;
        } else if (setting.equals(Settings.TRUE)) {
            return false;
        }
        throw new MessagingException("Malformed " + Setting.xmpp_publish_echo_enabled.toString() + " setting");
    }

    @Override
    public void unsubscribe(String channel) {
        if (channels.containsKey(channel)) {
            try {
                subscriptions.remove(getSubscription(fiber, channel));
                channels.remove(channel);
                MemoryChannel<PubSubMessage> newChannel = new MemoryChannel<PubSubMessage>();
                Iterator<Subscription> itor = subscriptions.iterator();
                while (itor.hasNext()) {
                    Subscription subscription = itor.next();
                    newChannel.subscribe(subscription.getSubscription());
                }
                channels.put(channel, newChannel);
            } catch (MessagingException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.info("No channel named " + channel);
        }
    }

    private class PubSubCallback implements Callback<PubSubMessage> {

        public void onMessage(PubSubMessage message) {
            MessageEvent messageEvent = new MessageEvent(this, message);
            fireMessageEvent(messageEvent);
        }
    }

    private class DenySentFromSelfFilter implements Filter<PubSubMessage> {

        public boolean passes(PubSubMessage message) {
            if (message.getFrom().equals(id)) {
                return false;
            } else {
                return true;
            }
        }
    }

    private static class FiberFactory {

        private PoolFiberFactory factory;

        protected FiberFactory() {
            ExecutorService service = Executors.newCachedThreadPool();
            factory = new PoolFiberFactory(service);
        }

        private static class FiberFactoryHolder {

            private static final FiberFactory INSTANCE = new FiberFactory();
        }

        public static FiberFactory getInstance() {
            return FiberFactoryHolder.INSTANCE;
        }

        public Fiber createFiber() {
            return factory.create();
        }
    }

    private Subscription getSubscription(Fiber fiber, String channel) throws MessagingException {
        Iterator<Subscription> itor = subscriptions.iterator();
        while (itor.hasNext()) {
            Subscription subscription = itor.next();
            if (subscription.getFiber() == fiber && subscription.getChannel().equals(channel)) {
                return subscription;
            }
        }
        throw new MessagingException("No subscription found");
    }

    private class Subscription {

        private Fiber fiber;

        private String channel;

        private ChannelSubscription<PubSubMessage> subscription;

        public void setFiber(Fiber fiber) {
            this.fiber = fiber;
        }

        public Fiber getFiber() {
            return fiber;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getChannel() {
            return channel;
        }

        public void setSubscription(ChannelSubscription<PubSubMessage> subscription) {
            this.subscription = subscription;
        }

        public ChannelSubscription<PubSubMessage> getSubscription() {
            return subscription;
        }

        public Subscription(Fiber fiber, String channel, ChannelSubscription<PubSubMessage> subscription) {
            this.fiber = fiber;
            this.channel = channel;
            this.subscription = subscription;
        }
    }
}
