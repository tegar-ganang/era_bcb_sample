package org.personalsmartspace.sre.nio.channels;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.io.IOException;
import java.util.Collections;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.spi.AbstractSelectableChannel;

public class SelectorImpl extends AbstractSelector {

    private Set<SelectionKey> keys;

    private Set<SelectionKey> selected;

    /**
	 * A dummy object whose monitor regulates access to both our
	 * selectThread and unhandledWakeup fields.
	 */
    private Object selectThreadMutex = new Object();

    /**
	 * Any thread that's currently blocked in a select operation.
	 */
    private Thread selectThread;

    /**
	 * Indicates whether we have an unhandled wakeup call. This can
	 * be due to either wakeup() triggering a thread interruption while
	 * a thread was blocked in a select operation (in which case we need
	 * to reset this thread's interrupt status after interrupting the
	 * select), or else that no thread was on a select operation at the
	 * time that wakeup() was called, in which case the following select()
	 * operation should return immediately with nothing selected.
	 */
    private boolean unhandledWakeup;

    public SelectorImpl(SelectorProvider provider) {
        super(provider);
        keys = new HashSet<SelectionKey>();
        selected = new HashSet<SelectionKey>();
    }

    protected void finalize() throws Throwable {
        close();
    }

    protected final void implCloseSelector() throws IOException {
        wakeup();
        synchronized (keys) {
            synchronized (selected) {
                synchronized (cancelledKeys()) {
                }
            }
        }
    }

    public final Set<SelectionKey> keys() {
        if (!isOpen()) throw new ClosedSelectorException();
        return Collections.unmodifiableSet(keys);
    }

    public final int selectNow() throws IOException {
        return select(1);
    }

    public final int select() throws IOException {
        return select(0);
    }

    private final int[] getFDsAsArray(int ops) {
        int[] result;
        int counter = 0;
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
            SelectionKeyImpl key = (SelectionKeyImpl) it.next();
            if ((key.interestOps() & ops) != 0) {
                counter++;
            }
        }
        result = new int[counter];
        counter = 0;
        it = keys.iterator();
        while (it.hasNext()) {
            SelectionKeyImpl key = (SelectionKeyImpl) it.next();
            if ((key.interestOps() & ops) != 0) {
                result[counter] = key.getNativeFD();
                counter++;
            }
        }
        return result;
    }

    public synchronized int select(long timeout) throws IOException {
        if (!isOpen()) throw new ClosedSelectorException();
        synchronized (keys) {
            synchronized (selected) {
                deregisterCancelledKeys();
                int[] read = getFDsAsArray(SelectionKey.OP_READ | SelectionKey.OP_ACCEPT);
                int[] write = getFDsAsArray(SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
                int[] except = new int[0];
                synchronized (selectThreadMutex) {
                    if (unhandledWakeup) {
                        unhandledWakeup = false;
                        return 0;
                    } else {
                        selectThread = Thread.currentThread();
                    }
                }
                int result = 0;
                try {
                    begin();
                    result = VMSelector.select(read, write, except, timeout);
                } finally {
                    end();
                }
                synchronized (selectThreadMutex) {
                    if (unhandledWakeup) {
                        unhandledWakeup = false;
                        Thread.interrupted();
                    }
                    selectThread = null;
                }
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    int ops = 0;
                    SelectionKeyImpl key = (SelectionKeyImpl) it.next();
                    if (selected.contains(key)) {
                        ops = key.readyOps();
                    }
                    for (int i = 0; i < read.length; i++) {
                        if (key.getNativeFD() == read[i]) {
                            if (key.channel() instanceof ServerSocketChannelImpl) {
                                ops = ops | SelectionKey.OP_ACCEPT;
                            } else {
                                ops = ops | SelectionKey.OP_READ;
                            }
                        }
                    }
                    for (int i = 0; i < write.length; i++) {
                        if (key.getNativeFD() == write[i]) {
                            ops = ops | SelectionKey.OP_WRITE;
                        }
                    }
                    if (!selected.contains(key)) {
                        selected.add(key);
                    }
                    key.readyOps(key.interestOps() & ops);
                }
                deregisterCancelledKeys();
                return result;
            }
        }
    }

    public final Set<SelectionKey> selectedKeys() {
        if (!isOpen()) throw new ClosedSelectorException();
        return selected;
    }

    public final Selector wakeup() {
        synchronized (selectThreadMutex) {
            unhandledWakeup = true;
            if (selectThread != null) selectThread.interrupt();
        }
        return this;
    }

    private final void deregisterCancelledKeys() {
        Set<SelectionKey> ckeys = cancelledKeys();
        synchronized (ckeys) {
            Iterator<SelectionKey> it = ckeys.iterator();
            while (it.hasNext()) {
                keys.remove((SelectionKeyImpl) it.next());
                it.remove();
            }
        }
    }

    protected SelectionKey register(SelectableChannel ch, int ops, Object att) {
        return register((AbstractSelectableChannel) ch, ops, att);
    }

    protected final SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        SelectionKeyImpl result;
        if (ch instanceof SocketChannelImpl) result = new SocketChannelSelectionKey(ch, this); else if (ch instanceof ServerSocketChannelImpl) result = new ServerSocketChannelSelectionKey(ch, this); else throw new InternalError("No known channel type");
        synchronized (keys) {
            keys.add(result);
        }
        result.interestOps(ops);
        result.attach(att);
        return result;
    }
}
