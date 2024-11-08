package net.sf.asyncobjects.net.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import net.sf.asyncobjects.AResolver;
import net.sf.asyncobjects.Promise;
import net.sf.asyncobjects.net.nio.NIOSelectorRunner.Attachment;
import net.sf.asyncobjects.vats.Vat;

/**
 * This is a vat that uses NIO services provided by {@link NIOSelectorRunner}.
 * The vat support detaching and ataching to such runners. 
 * Currently detach is not supported for this vat.
 * 
 * @author const
 *
 */
public class NIOSelectorVat extends Vat {

    /**
	 * A constructor
	 * @param runner a runner
	 * @param name a name of vat
	 */
    public NIOSelectorVat(NIOSelectorRunner runner, String name) {
        super(runner, name);
    }

    /**
	 * @see net.sf.asyncobjects.vats.Vat#detachFromRunner(boolean)
	 */
    public void detachFromRunner(boolean waitForIt) throws InterruptedException {
        throw new UnsupportedOperationException("Detach is not supported for NIO vats.");
    }

    /**
	 * register channel with listener
	 * 
	 * @param ch
	 *            a channel used by component
	 * @return a selection key for that component
	 * @throws Exception
	 *             if there is IO problme
	 */
    public SelectionKey registerChannel(SelectableChannel ch) throws Exception {
        return runner().registerChannel(ch);
    }

    /**
	 * @see net.sf.asyncobjects.vats.Vat#runner()
	 */
    @Override
    public NIOSelectorRunner runner() {
        return (NIOSelectorRunner) super.runner();
    }

    /**
	 * wait until socket will became acceptable
	 * 
	 * @param key
	 *            to wait on
	 * @return promise for operation
	 */
    public Promise<Void> waitAcceptable(SelectionKey key) {
        Promise<Void> rc = new Promise<Void>();
        AResolver<Void> resolver = rc.resolver();
        try {
            Attachment a = (Attachment) key.attachment();
            if (a.acceptWaiter != null) throw new IllegalStateException("someone already waits for accept");
            a.acceptWaiter = resolver;
            a.updateOps();
        } catch (Throwable t) {
            resolver.smash(t);
        }
        return rc;
    }

    /**
	 * wait until socket will became readable
	 * 
	 * @param key
	 *            to wait on
	 * @return promise for operation
	 */
    public Promise<Void> waitReadable(SelectionKey key) {
        Promise<Void> rc = new Promise<Void>();
        AResolver<Void> resolver = rc.resolver();
        try {
            Attachment a = (Attachment) key.attachment();
            if (a.readWaiter != null) throw new IllegalStateException("someone already waits for read");
            a.readWaiter = resolver;
            a.updateOps();
        } catch (Throwable t) {
            resolver.smash(t);
        }
        return rc;
    }

    /**
	 * wait until socket will became writable
	 * 
	 * @param key
	 *            to wait on
	 * @return promise for operation
	 */
    public Promise<Void> waitWritable(SelectionKey key) {
        Promise<Void> rc = new Promise<Void>();
        AResolver<Void> resolver = rc.resolver();
        try {
            Attachment a = (Attachment) key.attachment();
            if (a.writeWaiter != null) throw new IllegalStateException("someone already waits for write");
            a.writeWaiter = resolver;
            a.updateOps();
        } catch (Throwable t) {
            resolver.smash(t);
        }
        return rc;
    }

    /**
	 * wait until socket will became ready to finish connection
	 * 
	 * @param key
	 *            to wait on
	 * @return promise for operation
	 */
    public Promise<Void> waitConnectable(SelectionKey key) {
        Promise<Void> rc = new Promise<Void>();
        AResolver<Void> resolver = rc.resolver();
        try {
            Attachment a = (Attachment) key.attachment();
            if (a.connectWaiter != null) throw new IllegalStateException("someone already waits for connect");
            a.connectWaiter = resolver;
            a.updateOps();
        } catch (Throwable t) {
            resolver.smash(t);
        }
        return rc;
    }
}
