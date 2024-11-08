package com.rbnb.api;

final class PlugInHandle extends com.rbnb.api.ClientHandle implements com.rbnb.api.PlugIn {

    /**
     * read synchronization object.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 11/14/2002
     */
    private Door readLock = null;

    /**
     * the <code>RequestOptions</code> object read from the server.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.2
     * @version 06/11/2003
     */
    private RequestOptions ro = null;

    /**
     * write synchronization object.
     * <p>
     *
     * @author Ian Brown
     *
     * @since V2.0
     * @version 11/14/2002
     */
    private Door writeLock = null;

    PlugInHandle() {
        super();
        try {
            readLock = new Door(Door.STANDARD);
            writeLock = new Door(Door.STANDARD);
        } catch (java.lang.InterruptedException e) {
        }
    }

    PlugInHandle(InputStream isI, DataInputStream disI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        this();
        read(isI, disI);
    }

    PlugInHandle(String nameI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        super(nameI);
        try {
            readLock = new Door(Door.STANDARD);
            writeLock = new Door(Door.STANDARD);
        } catch (java.lang.InterruptedException e) {
        }
    }

    public long bytesTransferred() {
        return getACO().bytesTransferred();
    }

    public final void addChild(Rmap childI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.IOException, java.lang.InterruptedException {
        try {
            writeLock.setIdentification(getFullName() + "_handle_write");
            writeLock.lock("PlugInHandle.addChild");
            getACO().addChild(childI);
        } finally {
            writeLock.unlock();
            synchronized (this) {
                notify();
            }
        }
    }

    public final Object fetch(long timeOutI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.IOException, java.lang.InterruptedException {
        Object requestR = null;
        try {
            readLock.setIdentification(getFullName() + "_handle_read");
            readLock.lock("PlugInHandle.fetch");
            do {
                requestR = getACO().fetch(timeOutI);
                if (requestR instanceof RequestOptions) {
                    ro = (RequestOptions) requestR;
                }
            } while ((requestR != null) && (requestR instanceof RequestOptions));
        } finally {
            readLock.unlock();
            synchronized (this) {
                notify();
            }
        }
        return (requestR);
    }

    public final void fillRequestOptions(RequestOptions roO) {
        if (ro != null) {
            roO.copy(ro);
            ro = null;
        }
    }

    Rmap newInstance() throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        return (PlugInIO.newInstance(this));
    }

    public final void register(Rmap rmapI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        try {
            readLock.setIdentification(getFullName() + "_handle_read");
            writeLock.setIdentification(getFullName() + "_handle_write");
            synchronized (this) {
                while (readLock.isLocked() || writeLock.isLocked()) {
                    wait(TimerPeriod.NORMAL_WAIT);
                }
                readLock.lock("PlugInHandle.register");
                writeLock.lock("PlugInHandle.register");
            }
            getACO().register(rmapI);
        } finally {
            writeLock.unlock();
            readLock.unlock();
            synchronized (this) {
                notify();
            }
        }
    }

    public final void reRegister(Rmap rmapI) throws com.rbnb.api.AddressException, com.rbnb.api.SerializeException, java.io.EOFException, java.io.InterruptedIOException, java.io.IOException, java.lang.InterruptedException {
        try {
            readLock.setIdentification(getFullName() + "_handle_read");
            writeLock.setIdentification(getFullName() + "_handle_write");
            synchronized (this) {
                while (readLock.isLocked() || writeLock.isLocked()) {
                    wait(TimerPeriod.NORMAL_WAIT);
                }
                readLock.lock("PlugInHandle.reRegister");
                writeLock.lock("PlugInHandle.reRegister");
            }
            getACO().reRegister(rmapI);
        } finally {
            writeLock.unlock();
            readLock.unlock();
            synchronized (this) {
                notify();
            }
        }
    }

    public Object clone() {
        Object o = new PlugInHandle();
        cloned(o);
        return o;
    }

    /** Copies all the fields of the object to the given object
	 */
    protected void cloned(Object o) {
        super.cloned(o);
        PlugInHandle clonedR = (PlugInHandle) o;
        clonedR.readLock = readLock;
        clonedR.ro = ro;
        clonedR.writeLock = writeLock;
    }
}
