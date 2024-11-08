package p.s.m;

import com.google.protobuf.Message;
import alto.io.u.Objmap;
import alto.sys.Lock;
import alto.sys.lock.Light;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * This abstract base class for {@link Service} is further developed
 * into a server side {@link p.s.q.Queue} or {@link p.s.t.Map}
 * application programming framework.
 * 
 * @see p.s.m.Session
 * @see p.s.t.Store
 * @author jdp
 */
public abstract class Base extends Object implements Service {

    public final int identity;

    public final Object id;

    protected final Lock.Advanced lock;

    protected volatile long version;

    protected final Objmap producers, consumers;

    protected final Object monitor = new Object();

    public Base(Object id) {
        super();
        this.id = id;
        this.identity = System.identityHashCode(this);
        this.lock = new Light();
        this.producers = new Objmap();
        this.consumers = new Objmap();
    }

    protected abstract Message update(Consumer c);

    public Message update(Consumer c, long waitfor) {
        if (0 < waitfor) {
            Message m = this.update(c);
            if (null == m) {
                try {
                    synchronized (this.monitor) {
                        this.monitor.wait(waitfor);
                    }
                    return this.update(c);
                } catch (InterruptedException exc) {
                    throw new RuntimeException(exc);
                }
            } else return m;
        } else throw new IllegalArgumentException(String.valueOf(waitfor));
    }

    public final Object getId() {
        return this.id;
    }

    public final int getServiceIdentity() {
        return this.identity;
    }

    public final long getServiceVersion() {
        return this.version;
    }

    public boolean enterConsumer(Consumer consumer) {
        Object id = consumer.getConsumerIdentity();
        this.lock.lockWriteEnter();
        try {
            if (this.consumers.containsKey(id)) return false; else {
                this.consumers.put(id, consumer);
                return true;
            }
        } finally {
            this.lock.lockWriteExit();
        }
    }

    public boolean exitConsumer(Consumer consumer) {
        Object id = consumer.getConsumerIdentity();
        this.lock.lockWriteEnter();
        try {
            return (null != this.consumers.remove(id));
        } finally {
            this.lock.lockWriteExit();
        }
    }

    public boolean enterProducer(Producer producer) {
        Object id = producer.getProducerIdentity();
        this.lock.lockWriteEnter();
        try {
            if (this.producers.containsKey(id)) return false; else {
                this.producers.put(id, producer);
                return true;
            }
        } finally {
            this.lock.lockWriteExit();
        }
    }

    public boolean exitProducer(Producer producer) {
        Object id = producer.getProducerIdentity();
        this.lock.lockWriteEnter();
        try {
            return (null != this.producers.remove(id));
        } finally {
            this.lock.lockWriteExit();
        }
    }

    public final int lockReadLockCount() {
        return this.lock.lockReadLockCount();
    }

    public final boolean lockReadEnterTry() {
        return this.lock.lockReadEnterTry();
    }

    public final boolean lockReadEnterTry(long millis) throws java.lang.InterruptedException {
        return this.lock.lockReadEnterTry();
    }

    public final void lockReadEnter() {
        this.lock.lockReadEnter();
    }

    public final void lockReadExit() {
        this.lock.lockReadExit();
    }

    public final int lockWriteHoldCount() {
        return this.lock.lockWriteHoldCount();
    }

    public final boolean lockWriteEnterTry() {
        return this.lock.lockWriteEnterTry();
    }

    public final boolean lockWriteEnterTry(long millis) throws java.lang.InterruptedException {
        return this.lock.lockWriteEnterTry(millis);
    }

    public final void lockWriteEnter() {
        this.lock.lockWriteEnter();
    }

    public final void lockWriteExit() {
        this.lock.lockWriteExit();
    }

    protected final byte[] readResource(String resource) {
        InputStream in = this.getClass().getResourceAsStream(resource);
        if (null != in) {
            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] iob = new byte[0x200];
                int read;
                while (0 < (read = in.read(iob, 0, 0x200))) {
                    buf.write(iob, 0, read);
                }
                return buf.toByteArray();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
        return null;
    }
}
