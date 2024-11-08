package org.granite.gravity.gae;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.granite.gravity.Channel;
import org.granite.gravity.Subscription;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import flex.messaging.messages.AsyncMessage;

/**
 * Adapted from Greg Wilkins code (Jetty).
 * 
 * @author William DRAI
 */
public class GAETopic {

    private final GAETopicId id;

    private final GAEServiceAdapter serviceAdapter;

    private static final String TOPIC_PREFIX = "org.granite.gravity.gae.topic.";

    private static MemcacheService gaeCache = MemcacheServiceFactory.getMemcacheService();

    private ConcurrentMap<String, GAETopic> children = new ConcurrentHashMap<String, GAETopic>();

    private GAETopic wild;

    private GAETopic wildWild;

    public GAETopic(String topicId, GAEServiceAdapter serviceAdapter) {
        this.id = new GAETopicId(topicId);
        this.serviceAdapter = serviceAdapter;
    }

    public String getId() {
        return id.toString();
    }

    public GAETopicId getTopicId() {
        return id;
    }

    public GAETopic getChild(GAETopicId topicId) {
        String next = topicId.getSegment(id.depth());
        if (next == null) return null;
        GAETopic topic = children.get(next);
        if (topic == null || topic.getTopicId().depth() == topicId.depth()) {
            return topic;
        }
        return topic.getChild(topicId);
    }

    public void addChild(GAETopic topic) {
        GAETopicId child = topic.getTopicId();
        if (!id.isParentOf(child)) throw new IllegalArgumentException(id + " not parent of " + child);
        String next = child.getSegment(id.depth());
        if ((child.depth() - id.depth()) == 1) {
            GAETopic old = children.putIfAbsent(next, topic);
            if (old != null) throw new IllegalArgumentException("Already Exists");
            if (GAETopicId.WILD.equals(next)) wild = topic; else if (GAETopicId.WILDWILD.equals(next)) wildWild = topic;
        } else {
            GAETopic branch = serviceAdapter.getTopic((id.depth() == 0 ? "/" : (id.toString() + "/")) + next, true);
            branch.addChild(topic);
        }
    }

    private void removeExpiredSubscriptions(Map<String, Subscription> subscriptions) {
        List<Object> channelIds = new ArrayList<Object>(subscriptions.size());
        for (Subscription sub : subscriptions.values()) channelIds.add(GAEGravity.CHANNEL_PREFIX + sub.getChannel().getId());
        Map<Object, Object> channels = gaeCache.getAll(channelIds);
        for (Iterator<Map.Entry<String, Subscription>> ime = subscriptions.entrySet().iterator(); ime.hasNext(); ) {
            Map.Entry<String, Subscription> me = ime.next();
            if (!channels.containsKey(GAEGravity.CHANNEL_PREFIX + me.getValue().getChannel().getId())) ime.remove();
        }
    }

    public void subscribe(Channel channel, String destination, String subscriptionId, String selector, boolean noLocal) {
        synchronized (this) {
            Subscription subscription = channel.addSubscription(destination, getId(), subscriptionId, noLocal);
            subscription.setSelector(selector);
            @SuppressWarnings("unchecked") Map<String, Subscription> subscriptions = (Map<String, Subscription>) gaeCache.get(TOPIC_PREFIX + getId());
            if (subscriptions == null) subscriptions = new HashMap<String, Subscription>(); else removeExpiredSubscriptions(subscriptions);
            subscriptions.put(subscriptionId, subscription);
            gaeCache.put(TOPIC_PREFIX + getId(), subscriptions);
        }
    }

    public void unsubscribe(Channel channel, String subscriptionId) {
        synchronized (this) {
            @SuppressWarnings("unchecked") Map<String, Subscription> subscriptions = (Map<String, Subscription>) gaeCache.get(TOPIC_PREFIX + getId());
            if (subscriptions != null) {
                subscriptions.remove(subscriptionId);
                removeExpiredSubscriptions(subscriptions);
            }
            gaeCache.put(TOPIC_PREFIX + getId(), subscriptions);
            channel.removeSubscription(subscriptionId);
        }
    }

    public void publish(GAETopicId to, Channel fromChannel, AsyncMessage msg) {
        int tail = to.depth() - id.depth();
        switch(tail) {
            case 0:
                @SuppressWarnings("unchecked") Map<String, Subscription> subscriptions = (Map<String, Subscription>) gaeCache.get(TOPIC_PREFIX + getId());
                if (subscriptions != null) {
                    for (Subscription subscription : subscriptions.values()) {
                        AsyncMessage m = msg.clone();
                        subscription.deliver(fromChannel, m);
                    }
                }
                break;
            case 1:
                if (wild != null) {
                    @SuppressWarnings("unchecked") Map<String, Subscription> subs = (Map<String, Subscription>) gaeCache.get(TOPIC_PREFIX + wild.getId());
                    for (Subscription subscription : subs.values()) {
                        AsyncMessage m = msg.clone();
                        subscription.deliver(fromChannel, m);
                    }
                }
            default:
                {
                    if (wildWild != null) {
                        @SuppressWarnings("unchecked") Map<String, Subscription> subs = (Map<String, Subscription>) gaeCache.get(TOPIC_PREFIX + wildWild.getId());
                        for (Subscription subscription : subs.values()) {
                            AsyncMessage m = msg.clone();
                            subscription.deliver(fromChannel, m);
                        }
                    }
                    String next = to.getSegment(id.depth());
                    GAETopic topic = children.get(next);
                    if (topic != null) topic.publish(to, fromChannel, msg);
                }
        }
    }

    @Override
    public String toString() {
        return id.toString() + " {" + children.values() + "}";
    }
}
