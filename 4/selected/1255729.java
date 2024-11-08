package com.googlecode.acpj.internal.channels;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.googlecode.acpj.channels.BufferedChannel;
import com.googlecode.acpj.channels.Channel;
import com.googlecode.acpj.channels.ChannelFactory;
import com.googlecode.acpj.channels.ChannelMonitor;
import com.googlecode.acpj.channels.MonitoredChannel;
import com.googlecode.acpj.channels.PortArity;
import com.googlecode.acpj.internal.config.Configuration;

/**
 * <p>
 * Internal - implementation of the factory for 
 * {@link com.googlecode.acpj.channels.Channel} and {@link com.googlecode.acpj.channels.BufferedChannel}.
 * </p>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public class DefaultChannelFactory extends ChannelFactory {

    public static boolean monitorChannels = Configuration.getChannelMonitorStatus();

    public static Set<WeakReference<SimpleChannel<?>>> channels = new HashSet<WeakReference<SimpleChannel<?>>>();

    @Override
    public <T> Channel<T> createAnyToAnyChannel() {
        return createChannel(null, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createAnyToAnyChannel(int capacity) {
        return createChannel(null, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createAnyToAnyChannel(String name) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createAnyToAnyChannel(String name, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createAnyToAnyChannel(String name, int readPortLimit, int writePortLimit) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, readPortLimit, PortArity.ANY, writePortLimit);
    }

    @Override
    public <T> BufferedChannel<T> createAnyToAnyChannel(String name, int readPortLimit, int writePortLimit, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, readPortLimit, PortArity.ANY, writePortLimit, capacity);
    }

    @Override
    public <T> Channel<T> createAnyToOneChannel() {
        return createChannel(null, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createAnyToOneChannel(int capacity) {
        return createChannel(null, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createAnyToOneChannel(String name) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createAnyToOneChannel(String name, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ANY, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createAnyToOneChannel(String name, int readPortLimit, int writePortLimit) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, readPortLimit, PortArity.ANY, writePortLimit);
    }

    @Override
    public <T> BufferedChannel<T> createAnyToOneChannel(String name, int readPortLimit, int writePortLimit, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, readPortLimit, PortArity.ANY, writePortLimit, capacity);
    }

    @Override
    public <T> Channel<T> createOneToAnyChannel() {
        return createChannel(null, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createOneToAnyChannel(int capacity) {
        return createChannel(null, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createOneToAnyChannel(String name) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createOneToAnyChannel(String name, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createOneToAnyChannel(String name, int readPortLimit, int writePortLimit) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, readPortLimit, PortArity.ONE, writePortLimit);
    }

    @Override
    public <T> BufferedChannel<T> createOneToAnyChannel(String name, int readPortLimit, int writePortLimit, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ANY, readPortLimit, PortArity.ONE, writePortLimit, capacity);
    }

    @Override
    public <T> Channel<T> createOneToOneChannel() {
        return createChannel(null, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createOneToOneChannel(int capacity) {
        return createChannel(null, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createOneToOneChannel(String name) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED);
    }

    @Override
    public <T> BufferedChannel<T> createOneToOneChannel(String name, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, PORT_LIMIT_UNLIMITED, PortArity.ONE, PORT_LIMIT_UNLIMITED, capacity);
    }

    @Override
    public <T> Channel<T> createOneToOneChannel(String name, int readPortLimit, int writePortLimit) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, readPortLimit, PortArity.ONE, writePortLimit);
    }

    @Override
    public <T> BufferedChannel<T> createOneToOneChannel(String name, int readPortLimit, int writePortLimit, int capacity) throws IllegalArgumentException {
        return createChannel(name, PortArity.ONE, readPortLimit, PortArity.ONE, writePortLimit, capacity);
    }

    @Override
    public <T> Channel<T> createChannel(String name, PortArity readPortArity, int readPortLimit, PortArity writePortArity, int writePortLimit) throws IllegalArgumentException {
        SimpleChannel<T> channel = new SimpleChannel<T>(name, readPortArity, readPortLimit, writePortArity, writePortLimit, 0);
        if (monitorChannels) {
            channels.add(new WeakReference<SimpleChannel<?>>(channel));
        }
        return channel;
    }

    @Override
    public <T> BufferedChannel<T> createChannel(String name, PortArity readPortArity, int readPortLimit, PortArity writePortArity, int writePortLimit, int capacity) throws IllegalArgumentException {
        SimpleChannel<T> channel = new SimpleChannel<T>(name, readPortArity, readPortLimit, writePortArity, writePortLimit, capacity);
        if (monitorChannels) {
            channels.add(new WeakReference<SimpleChannel<?>>(channel));
        }
        return channel;
    }

    private class SimpleChannelMonitor implements ChannelMonitor {

        public Iterator<MonitoredChannel> getChannels() {
            Set<MonitoredChannel> monitored = new HashSet<MonitoredChannel>();
            for (Iterator<WeakReference<SimpleChannel<?>>> iterator = channels.iterator(); iterator.hasNext(); ) {
                WeakReference<SimpleChannel<?>> ref = iterator.next();
                if (ref.get() != null) {
                    monitored.add(new MonitoredChannelImpl(ref.get()));
                }
            }
            return monitored.iterator();
        }
    }

    @Override
    @SuppressWarnings("synthetic-access")
    public ChannelMonitor getChannelMonitor() {
        if (monitorChannels) {
            return new SimpleChannelMonitor();
        }
        return null;
    }
}
