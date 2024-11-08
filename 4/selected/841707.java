package com.googlecode.acpj.internal.channels;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicLong;
import com.googlecode.acpj.actors.ActorFactory;
import com.googlecode.acpj.channels.BufferedChannel;
import com.googlecode.acpj.channels.ChannelException;
import com.googlecode.acpj.channels.ChannelPoisonedException;
import com.googlecode.acpj.channels.Port;
import com.googlecode.acpj.channels.PortArity;
import com.googlecode.acpj.channels.ReadPort;
import com.googlecode.acpj.channels.WritePort;

/**
 * <p>
 * Internal - Common implementation for both the 
 * {@link com.googlecode.acpj.channels.BufferedChannel} and
 * {@link com.googlecode.acpj.channels.Channel} interfaces.
 * </p>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public class SimpleChannel<T> implements BufferedChannel<T>, SimpleMonitoredChannel<T> {

    class Item<V> {

        V data = null;
    }

    private static AtomicLong serialNumberGenerator = new AtomicLong(0);

    private long id = serialNumberGenerator.getAndIncrement();

    private String name = null;

    private int capacity = 1;

    private BlockingQueue<Item<T>> values = null;

    private PortArity readArity = null;

    private int readPortLimit = 0;

    private Set<ReadPort<T>> readPorts = null;

    private PortArity writeArity = null;

    private int writePortLimit = 0;

    private Set<WritePort<T>> writePorts = null;

    private boolean poisoned = false;

    public SimpleChannel(String name, PortArity readPortArity, int readPortLimit, PortArity writePortArity, int writePortLimit, int capacity) {
        this.name = name;
        if (readPortArity == null) {
            throw new IllegalArgumentException("Read port arity may not be null.");
        }
        this.readArity = readPortArity;
        if (readPortLimit == 0) {
            throw new IllegalArgumentException("Read port limit may not be zero.");
        }
        this.readPortLimit = readPortLimit;
        this.readPorts = new HashSet<ReadPort<T>>(this.readArity == PortArity.ONE ? 1 : 10);
        if (writePortArity == null) {
            throw new IllegalArgumentException("Write port arity may not be null.");
        }
        this.writeArity = writePortArity;
        if (writePortLimit == 0) {
            throw new IllegalArgumentException("Write port limit may not be zero.");
        }
        this.writePortLimit = writePortLimit;
        this.writePorts = new HashSet<WritePort<T>>(this.writeArity == PortArity.ONE ? 1 : 10);
        this.capacity = capacity;
        if (this.capacity == 0) {
            this.values = new SynchronousQueue<Item<T>>(true);
        } else {
            this.values = new LinkedBlockingQueue<Item<T>>(this.capacity == BUFFER_CAPACITY_UNLIMITED ? Integer.MAX_VALUE : this.capacity);
        }
    }

    public T readValue() throws IllegalStateException, ChannelException {
        if (isPoisoned()) {
            throw new ChannelPoisonedException();
        }
        T value = null;
        try {
            Item<T> temp = this.values.take();
            value = temp.data;
        } catch (InterruptedException e) {
            throw new ChannelException(e);
        }
        if (isPoisoned()) {
            throw new ChannelPoisonedException();
        }
        return value;
    }

    public void writeValue(T value) throws IllegalStateException, ChannelException {
        if (isPoisoned()) {
            throw new ChannelPoisonedException();
        }
        if (value == null) {
            throw new IllegalArgumentException("Value may not be null.");
        }
        try {
            Item<T> temp = new Item<T>();
            temp.data = value;
            this.values.put(temp);
        } catch (InterruptedException e) {
            throw new ChannelException(e);
        }
        if (isPoisoned()) {
            throw new ChannelPoisonedException();
        }
    }

    public ReadPort<T> getReadPort(boolean claimed) throws IllegalStateException, ChannelException {
        if (isPoisoned()) {
            throw new ChannelPoisonedException();
        }
        if (this.readArity == PortArity.ONE) {
            if (this.readPorts.size() == 0) {
                ReadPort<T> newPort = new SimpleReadPort<T>(this, this.readPortLimit, claimed ? ActorFactory.getInstance().getCurrentActor() : null);
                if (this.readPorts.add(newPort) == true) {
                    return newPort;
                }
            } else {
                throw new ChannelException("Port arity invalid for write port.");
            }
        } else {
            ReadPort<T> newPort = new SimpleReadPort<T>(this, this.readPortLimit, claimed ? ActorFactory.getInstance().getCurrentActor() : null);
            if (this.readPorts.add(newPort) == true) {
                return newPort;
            }
        }
        throw new ChannelException("Could not create ReadPort");
    }

    public PortArity getReadPortArity() {
        return this.readArity;
    }

    public long getLocalId() {
        return this.id;
    }

    public String getName() {
        if (this.name == null) {
            return String.format("channel:/%d", this.id);
        } else {
            return String.format("channel:/%s/%d", this.name, this.id);
        }
    }

    public int getBufferCapacity() {
        return this.capacity;
    }

    public int size() {
        return this.values.size();
    }

    public WritePort<T> getWritePort(boolean claimed) throws IllegalStateException, ChannelException {
        if (isPoisoned()) {
            throw new ChannelPoisonedException();
        }
        if (this.writeArity == PortArity.ONE) {
            if (this.writePorts.size() == 0) {
                WritePort<T> newPort = new SimpleWritePort<T>(this, this.writePortLimit, claimed ? ActorFactory.getInstance().getCurrentActor() : null);
                if (this.writePorts.add(newPort) == true) {
                    return newPort;
                }
            } else {
                throw new ChannelException("Port arity invalid for write port.");
            }
        } else {
            WritePort<T> newPort = new SimpleWritePort<T>(this, this.writePortLimit, claimed ? ActorFactory.getInstance().getCurrentActor() : null);
            if (this.writePorts.add(newPort) == true) {
                return newPort;
            }
        }
        throw new ChannelException("Could not create WritePort");
    }

    public void closePort(Port<T> port) {
        if (port instanceof ReadPort<?>) {
            this.readPorts.remove(port);
        } else {
            this.writePorts.remove(port);
        }
    }

    public PortArity getWritePortArity() {
        return this.writeArity;
    }

    public void poison() throws IllegalStateException, ChannelException {
        this.poisoned = true;
        shutdown();
    }

    public boolean isPoisoned() throws IllegalStateException {
        return this.poisoned;
    }

    private void shutdown() {
        this.values.clear();
        try {
            this.values.put(new Item<T>());
        } catch (InterruptedException e) {
        }
        this.readPorts.clear();
        this.writePorts.clear();
    }

    @Override
    public int hashCode() {
        return (int) this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleChannel<?>)) {
            return false;
        }
        return this.id == ((SimpleChannel<?>) obj).id;
    }

    @Override
    public String toString() {
        return getName();
    }

    public Iterator<ReadPort<T>> getReadPorts() {
        return this.readPorts.iterator();
    }

    public Iterator<WritePort<T>> getWritePorts() {
        return this.writePorts.iterator();
    }
}
