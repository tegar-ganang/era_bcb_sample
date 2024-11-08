package org.jtell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jtell.config.EventSinkMetadata;
import org.jtell.internal.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * <code>ConcurrentEventSinkRegistry</code> is an implementation of the {@link EventSinkRegistry} interface which is
 * suitable for environments where events will be broadcast from multiple threads. It is most efficient when event sinks
 * are registered or unregistered relatively infrequently.
 * </p>
 * <p>
 * <strong>Thread Safety</strong><br/>
 * Instances of this class are safe for multithreaded access. All internal data structures are accessed in a thread safe
 * manner; however, thread safety ultimately depends upon the characteristics of the registered {@link EventSink}
 * objects and their corresponding application listener objects.
 * </p>
 */
public class ConcurrentEventSinkRegistry implements EventSinkRegistry {

    private static Log LOG = LogFactory.getLog(ConcurrentEventSinkRegistry.class);

    /**
     * <p>
     * Weak map of {@link EventChannelInternal} instances which have registered event sinks to event sink affinity
     * objects.
     * </p>
     */
    private final Map<EventChannelInternal, EventSinkAffinityMultiMap> m_channelEventSinkAffinity = new WeakHashMap<EventChannelInternal, EventSinkAffinityMultiMap>();

    /**
     * <p>
     * Affinity read lock.
     * </p>
     */
    private final Lock m_channelEventSinkAffinityReadLock;

    /**
     * <p>
     * Affinity write lock.
     * </p>
     */
    private final Lock m_channelEventSinkAffinityWriteLock;

    /**
     * <p>
     * Period between invalid event sink flushes, 10 minutes.
     * </p>
     */
    private final Period m_invalidEventSinkFlushPeriod = new Period(10 * 1000 * 60);

    /**
     * <p>
     * Construct a {@link ConcurrentEventSinkRegistry} instance.
     * </p>
     */
    public ConcurrentEventSinkRegistry() {
        super();
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        m_channelEventSinkAffinityReadLock = readWriteLock.readLock();
        m_channelEventSinkAffinityWriteLock = readWriteLock.writeLock();
    }

    public Set<EventSink> findEventSinks(final EventChannelInternal channel, final EventMetadata eventMetadata) {
        flushIfNecessary();
        Set<EventSink> result = Empties.EMPTY_EVENT_SINKS;
        final EventSinkAffinityMultiMap affinity = getEventSinkAffinityForChannel(channel, false);
        if (null != affinity) {
            final Set<EventSink> eventClassEventSinks = affinity.getAllForEventClass(eventMetadata.getEventClassName());
            if (!eventClassEventSinks.isEmpty()) {
                final Set<EventSink> sourceClassEventSinks = affinity.getAllForSourceClass(eventMetadata.getSourceClassName());
                if (!eventClassEventSinks.isEmpty()) {
                    result = new HashSet<EventSink>(Math.min(eventClassEventSinks.size(), sourceClassEventSinks.size()));
                    CollectionUtils.addIntersection(result, eventClassEventSinks, sourceClassEventSinks);
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Returning %d event sink(s) for channel [%s], event class [%s], and source class [%s].", result.size(), channel, eventMetadata.getEventClassName(), eventMetadata.getSourceClassName()));
        }
        return result;
    }

    public void registerEventSink(final EventChannelInternal channel, final EventSink eventSink) {
        Guard.notNull("channel", channel);
        Guard.notNull("eventSink", eventSink);
        flushIfNecessary();
        final EventSinkAffinityMultiMap affinity = getEventSinkAffinityForChannel(channel, true);
        final EventSinkMetadata metadata = eventSink.getEventSinkMetadata();
        affinity.putForEventClass(metadata.getEventClassName(), eventSink);
        affinity.putForSourceClass(metadata.getSourceClassName(), eventSink);
    }

    public void unregisterAllEventSinks(final EventChannelInternal channel) {
        Guard.notNull("channel", channel);
        flushIfNecessary();
        final EventSinkAffinityMultiMap affinity = getEventSinkAffinityForChannel(channel, false);
        if (null != affinity) {
            affinity.clear();
        }
    }

    public void unregisterEventSinksForOwner(final EventChannelInternal channel, final Object owner) {
        Guard.notNull("channel", channel);
        Guard.notNull("owner", owner);
        flushIfNecessary();
        final EventSinkAffinityMultiMap affinity = getEventSinkAffinityForChannel(channel, false);
        if (null != affinity) {
            affinity.removeMatching(new Predicate<EventSink>() {

                public boolean evaluate(final EventSink value) {
                    return owner.equals(value.getOwner());
                }
            });
        }
    }

    /**
     * <p>
     * Flush invalid event sinks from all affinity maps, if the configured time has elapsed since last flush.
     * </p>
     */
    private void flushIfNecessary() {
        if (m_invalidEventSinkFlushPeriod.elapsed()) {
            final long startTimeMillis = System.currentTimeMillis();
            for (final EventSinkAffinityMultiMap affinity : m_channelEventSinkAffinity.values()) {
                affinity.removeMatching(INVALID_EVENT_SINK);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Completed flushing of invalid event sinks in %dms.", System.currentTimeMillis() - startTimeMillis));
            }
        }
    }

    /**
     * <p>
     * Get the event/source class affinity information for a given event channel, optionally creating and registering
     * the object if one does not already exist for the channel.
     * </p>
     *
     * @param channel the event channel.
     * @param createIfNotFound flag indicating whether the affinity map should be created if it does not already exist.
     * @return {@link EventSinkAffinityMultiMap} instance or <code>null</code> if no entry exists for the given channel
     *         and <code>createIfNotFound</code> was <code>false</code>.
     */
    private EventSinkAffinityMultiMap getEventSinkAffinityForChannel(final EventChannelInternal channel, final boolean createIfNotFound) {
        EventSinkAffinityMultiMap result = null;
        m_channelEventSinkAffinityReadLock.lock();
        try {
            if (m_channelEventSinkAffinity.containsKey(channel)) {
                result = m_channelEventSinkAffinity.get(channel);
            }
        } finally {
            m_channelEventSinkAffinityReadLock.unlock();
        }
        if (null == result && createIfNotFound) {
            m_channelEventSinkAffinityWriteLock.lock();
            try {
                if (m_channelEventSinkAffinity.containsKey(channel)) {
                    result = m_channelEventSinkAffinity.get(channel);
                } else {
                    result = new ConcurrentEventSinkAffinityMultiMap();
                    m_channelEventSinkAffinity.put(channel, result);
                }
            } finally {
                m_channelEventSinkAffinityWriteLock.unlock();
            }
        }
        return result;
    }

    /**
     * <p>
     * Predicate used to flush invalid entries from the affinity map.
     * </p>
     */
    private static final Predicate<EventSink> INVALID_EVENT_SINK = new Predicate<EventSink>() {

        public boolean evaluate(EventSink value) {
            return !value.isValid();
        }
    };
}
