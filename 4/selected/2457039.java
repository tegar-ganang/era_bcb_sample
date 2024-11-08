package net.io;

import java.io.IOException;
import java.nio.channels.*;
import java.util.HashMap;

/**
 * A pool of Common Dispatchers
 *
 * @author    <a href="mailto:vsuman@gmail.com">Viorel Suman</a>
 * @version   $Id: CommonDispatchPool.java,v 1.1 2007/03/14 14:11:11 viorel_suman Exp $
 */
public final class CommonDispatchPool {

    private final CommonDispatcher[] pool;

    private int last = 0;

    /**
	 * Constructor for the CommonDispatchPool object
	 *
	 * @param  name         the name of this pool
	 * @param  size         the number of CommonDispatchers in this pool
	 * @throws IOException  if an IOException occures
	 */
    public CommonDispatchPool(String name, int size) throws IOException {
        pool = new CommonDispatcher[size];
        for (int i = 0; i < size; i++) {
            pool[i] = new CommonDispatcher(name + " [" + i + "]");
        }
    }

    /**
	 * Register a listener to be notified about "OP_READ" events
	 *
	 * @param listener                 listener to be registered
	 * @throws ClosedChannelException  if the listener's channel is closed
	 */
    public void regRead(SocketChannelListener listener) throws ClosedChannelException {
        register(listener, SelectionKey.OP_READ);
    }

    /**
	 * Unregister listener for "OP_READ" events from this dispatcher
	 *
	 * @param listener  listener to be unregistered
	 */
    public void unRegRead(SocketChannelListener listener) {
        unregister(listener, SelectionKey.OP_READ);
    }

    /**
	 * Register a listener to be notified about "OP_WRITE" events
	 *
	 * @param listener                 listener to be registered
	 * @throws ClosedChannelException  if the listener's channel is closed
	 */
    public void regWrite(SocketChannelListener listener) throws ClosedChannelException {
        register(listener, SelectionKey.OP_WRITE);
    }

    /**
	 * Unregister listener for "OP_READ" events from this dispatcher
	 *
	 * @param listener  listener to be unregistered
	 */
    public void unRegWrite(SocketChannelListener listener) {
        unregister(listener, SelectionKey.OP_WRITE);
    }

    /**
	 * Register a listener to be notified about "OP_CONNECT" events
	 *
	 * @param listener                 listener to be registered
	 * @throws ClosedChannelException  if the listener's channel is closed
	 */
    public void regConnect(SocketChannelListener listener) throws ClosedChannelException {
        register(listener, SelectionKey.OP_CONNECT);
    }

    /**
	 * Unregister listener for "OP_CONNECT" events from this dispatcher
	 *
	 * @param listener  listener to be unregistered
	 */
    public void unRegConnect(SocketChannelListener listener) {
        unregister(listener, SelectionKey.OP_CONNECT);
    }

    /**
	 * Register a listener to be notified about "OP_ACCEPT" events
	 *
	 * @param listener                 listener to be registered
	 * @throws ClosedChannelException  if the listener's channel is closed
	 */
    public void regAccept(ServerSocketChannelListener listener) throws ClosedChannelException {
        register(listener, SelectionKey.OP_ACCEPT);
    }

    /**
	 * Unregister listener for "OP_ACCEPT" events from this dispatcher
	 *
	 * @param listener  listener to be unregistered
	 */
    public void unRegAccept(ServerSocketChannelListener listener) {
        unregister(listener, SelectionKey.OP_ACCEPT);
    }

    private class Operations {

        private int readOp = -1, writeOp = -1, connOp = -1, accOp = -1;
    }

    private HashMap regs = new HashMap();

    private void register(SelectableChannelListener listener, int ops) throws ClosedChannelException {
        synchronized (regs) {
            Operations rOps = (Operations) regs.get(listener);
            if (rOps == null) {
                rOps = new Operations();
                regs.put(listener, rOps);
            }
            switch(ops) {
                case SelectionKey.OP_READ:
                    if (rOps.readOp >= 0) return;
                    rOps.readOp = last;
                    break;
                case SelectionKey.OP_WRITE:
                    if (rOps.writeOp >= 0) return;
                    rOps.writeOp = last;
                    break;
                case SelectionKey.OP_CONNECT:
                    if (rOps.connOp >= 0) return;
                    rOps.connOp = last;
                    break;
                case SelectionKey.OP_ACCEPT:
                    if (rOps.accOp >= 0) return;
                    rOps.accOp = last;
                    break;
            }
            pool[last].register(listener, ops);
            last = (last + 1) % pool.length;
        }
    }

    private void unregister(SelectableChannelListener listener, int ops) {
        synchronized (regs) {
            Operations rOps = (Operations) regs.get(listener);
            if (rOps == null) return;
            int index = -1;
            switch(ops) {
                case SelectionKey.OP_READ:
                    if (rOps.readOp < 0) return;
                    index = rOps.readOp;
                    rOps.readOp = -1;
                    break;
                case SelectionKey.OP_WRITE:
                    if (rOps.writeOp < 0) return;
                    index = rOps.writeOp;
                    rOps.writeOp = -1;
                    break;
                case SelectionKey.OP_CONNECT:
                    if (rOps.connOp < 0) return;
                    index = rOps.connOp;
                    rOps.connOp = -1;
                    break;
                case SelectionKey.OP_ACCEPT:
                    if (rOps.accOp < 0) return;
                    index = rOps.accOp;
                    rOps.accOp = -1;
                    break;
            }
            if (rOps.readOp < 0 && rOps.writeOp < 0 && rOps.connOp < 0 && rOps.accOp < 0) regs.remove(listener);
            if (index >= 0) pool[index].unregister(listener, ops);
        }
        return;
    }
}
