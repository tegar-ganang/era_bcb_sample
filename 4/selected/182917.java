package com.sun.star.lib.uno.bridges.java_remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import com.sun.star.lib.util.DisposeListener;
import com.sun.star.lib.util.DisposeNotifier;
import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XInstanceProvider;
import com.sun.star.comp.loader.FactoryHelper;
import com.sun.star.connection.XConnection;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.lang.DisposedException;
import com.sun.star.lib.uno.environments.java.java_environment;
import com.sun.star.lib.uno.environments.remote.IProtocol;
import com.sun.star.lib.uno.environments.remote.IReceiver;
import com.sun.star.lib.uno.environments.remote.Job;
import com.sun.star.lib.uno.environments.remote.Message;
import com.sun.star.lib.uno.environments.remote.ThreadId;
import com.sun.star.lib.uno.environments.remote.ThreadPoolManager;
import com.sun.star.lib.uno.environments.remote.IThreadPool;
import com.sun.star.lib.uno.typedesc.MethodDescription;
import com.sun.star.lib.uno.typedesc.TypeDescription;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.IBridge;
import com.sun.star.uno.IEnvironment;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import com.sun.star.uno.Type;
import com.sun.star.uno.TypeClass;
import com.sun.star.uno.Any;

/**
 * This class implements a remote bridge. Therefor
 * various interfaces are implemented.
 * <p>
 * The protocol to used is passed by name, the bridge
 * then looks for it under <code>com.sun.star.lib.uno.protocols</code>.
 * <p>
 * @version 	$Revision$ $ $Date$
 * @author 	    Kay Ramme
 * @since       UDK1.0
 */
public class java_remote_bridge implements IBridge, IReceiver, RequestHandler, XBridge, XComponent, DisposeNotifier {

    /**
	 * When set to true, enables various debugging output.
	 */
    private static final boolean DEBUG = false;

    private final class MessageDispatcher extends Thread {

        public MessageDispatcher() {
            super("MessageDispatcher");
        }

        public void run() {
            try {
                for (; ; ) {
                    synchronized (this) {
                        if (terminate) {
                            break;
                        }
                    }
                    Message msg = _iProtocol.readMessage();
                    Object obj = null;
                    if (msg.isRequest()) {
                        String oid = msg.getObjectId();
                        Type type = new Type(msg.getType());
                        int fid = msg.getMethod().getIndex();
                        if (fid == MethodDescription.ID_RELEASE) {
                            _java_environment.revokeInterface(oid, type);
                            remRefHolder(type, oid);
                            if (msg.isSynchronous()) {
                                sendReply(false, msg.getThreadId(), null);
                            }
                            continue;
                        }
                        obj = _java_environment.getRegisteredInterface(oid, type);
                        if (obj == null && fid == MethodDescription.ID_QUERY_INTERFACE) {
                            if (_xInstanceProvider == null) {
                                sendReply(true, msg.getThreadId(), new com.sun.star.uno.RuntimeException("unknown OID " + oid));
                                continue;
                            } else {
                                UnoRuntime.setCurrentContext(msg.getCurrentContext());
                                try {
                                    obj = _xInstanceProvider.getInstance(oid);
                                } catch (com.sun.star.uno.RuntimeException e) {
                                    sendReply(true, msg.getThreadId(), e);
                                    continue;
                                } catch (Exception e) {
                                    sendReply(true, msg.getThreadId(), new com.sun.star.uno.RuntimeException(e.toString()));
                                    continue;
                                } finally {
                                    UnoRuntime.setCurrentContext(null);
                                }
                            }
                        }
                    }
                    _iThreadPool.putJob(new Job(obj, java_remote_bridge.this, msg));
                }
            } catch (Throwable e) {
                dispose(new DisposedException(e.toString()));
            }
        }

        public synchronized void terminate() {
            terminate = true;
        }

        private boolean terminate = false;
    }

    protected XConnection _xConnection;

    protected XInstanceProvider _xInstanceProvider;

    protected String _name = "remote";

    private final String protocol;

    protected IProtocol _iProtocol;

    protected IEnvironment _java_environment;

    protected MessageDispatcher _messageDispatcher;

    protected int _life_count = 0;

    private final Vector _listeners = new Vector();

    protected IThreadPool _iThreadPool;

    private boolean disposed = false;

    /**
	 * This method is for testing only.
	 */
    int getLifeCount() {
        return _life_count;
    }

    /**
	 * This method is for testing only.
	 */
    IProtocol getProtocol() {
        return _iProtocol;
    }

    private static final class RefHolder {

        public RefHolder(Type type, Object object) {
            this.type = type;
            this.object = object;
        }

        public Type getType() {
            return type;
        }

        public void acquire() {
            ++count;
        }

        public boolean release() {
            return --count == 0;
        }

        private final Type type;

        private final Object object;

        private int count = 1;
    }

    private final HashMap refHolders = new HashMap();

    private boolean hasRefHolder(String oid, Type type) {
        synchronized (refHolders) {
            LinkedList l = (LinkedList) refHolders.get(oid);
            if (l != null) {
                for (Iterator i = l.iterator(); i.hasNext(); ) {
                    RefHolder rh = (RefHolder) i.next();
                    if (type.isSupertypeOf(rh.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    final void addRefHolder(Object obj, Type type, String oid) {
        synchronized (refHolders) {
            LinkedList l = (LinkedList) refHolders.get(oid);
            if (l == null) {
                l = new LinkedList();
                refHolders.put(oid, l);
            }
            boolean found = false;
            for (Iterator i = l.iterator(); !found && i.hasNext(); ) {
                RefHolder rh = (RefHolder) i.next();
                if (rh.getType().equals(type)) {
                    found = true;
                    rh.acquire();
                }
            }
            if (!found) {
                l.add(new RefHolder(type, obj));
            }
        }
        acquire();
    }

    final void remRefHolder(Type type, String oid) {
        synchronized (refHolders) {
            LinkedList l = (LinkedList) refHolders.get(oid);
            if (l != null) {
                for (Iterator i = l.iterator(); i.hasNext(); ) {
                    RefHolder rh = (RefHolder) i.next();
                    if (rh.getType().equals(type)) {
                        try {
                            if (rh.release()) {
                                l.remove(rh);
                                if (l.isEmpty()) {
                                    refHolders.remove(oid);
                                }
                            }
                        } finally {
                            release();
                        }
                        break;
                    }
                }
            }
        }
    }

    final void freeHolders() {
        synchronized (refHolders) {
            for (Iterator i1 = refHolders.entrySet().iterator(); i1.hasNext(); ) {
                Map.Entry e = (Map.Entry) i1.next();
                String oid = (String) e.getKey();
                LinkedList l = (LinkedList) e.getValue();
                for (Iterator i2 = l.iterator(); i2.hasNext(); ) {
                    RefHolder rh = (RefHolder) i2.next();
                    for (boolean done = false; !done; ) {
                        done = rh.release();
                        _java_environment.revokeInterface(oid, rh.getType());
                        release();
                    }
                }
            }
            refHolders.clear();
        }
    }

    public java_remote_bridge(IEnvironment java_environment, IEnvironment remote_environment, Object[] args) throws Exception {
        _java_environment = java_environment;
        String proto = (String) args[0];
        _xConnection = (XConnection) args[1];
        _xInstanceProvider = (XInstanceProvider) args[2];
        if (args.length > 3) {
            _name = (String) args[3];
        }
        String attr;
        int i = proto.indexOf(',');
        if (i >= 0) {
            protocol = proto.substring(0, i);
            attr = proto.substring(i + 1);
        } else {
            protocol = proto;
            attr = null;
        }
        _iProtocol = (IProtocol) Class.forName("com.sun.star.lib.uno.protocols." + protocol + "." + protocol).getConstructor(new Class[] { IBridge.class, String.class, InputStream.class, OutputStream.class }).newInstance(new Object[] { this, attr, new XConnectionInputStream_Adapter(_xConnection), new XConnectionOutputStream_Adapter(_xConnection) });
        proxyFactory = new ProxyFactory(this, this);
        _iThreadPool = ThreadPoolManager.create();
        _messageDispatcher = new MessageDispatcher();
        _messageDispatcher.start();
        _iProtocol.init();
    }

    private void notifyListeners() {
        EventObject eventObject = new EventObject(this);
        Enumeration elements = _listeners.elements();
        while (elements.hasMoreElements()) {
            XEventListener xEventListener = (XEventListener) elements.nextElement();
            try {
                xEventListener.disposing(eventObject);
            } catch (com.sun.star.uno.RuntimeException runtimeException) {
            }
        }
    }

    /**
	 * Constructs a new bridge.
	 * <p>
	 * This method is not part of the provided <code>api</code>
	 * and should only be used by the UNO runtime.
	 * <p>
	 * @deprecated as of UDK 1.0
	 * <p>
	 * @param  args               the custom parameters: arg[0] == protocol_name, arg[1] == xConnection, arg[2] == xInstanceProvider
	 */
    public java_remote_bridge(Object args[]) throws Exception {
        this(UnoRuntime.getEnvironment("java", null), UnoRuntime.getEnvironment("remote", null), args);
    }

    public Object mapInterfaceTo(Object object, Type type) {
        checkDisposed();
        if (object == null) {
            return null;
        } else {
            String[] oid = new String[1];
            object = _java_environment.registerInterface(object, oid, type);
            if (!proxyFactory.isProxy(object)) {
                addRefHolder(object, type, oid[0]);
            }
            return oid[0];
        }
    }

    /**
	 * Maps an object from destination environment to the source environment.
	 * <p>
	 * @return     the object in the source environment
	 * @param      object     the object to map
	 * @param      type       the interface under which is to be mapped
	 * @see                   com.sun.star.uno.IBridge#mapInterfaceFrom
	 */
    public Object mapInterfaceFrom(Object oId, Type type) {
        checkDisposed();
        acquire();
        String oid = (String) oId;
        Object object = _java_environment.getRegisteredInterface(oid, type);
        if (object == null) {
            object = _java_environment.registerInterface(proxyFactory.create(oid, type), new String[] { oid }, type);
        } else if (!hasRefHolder(oid, type)) {
            sendInternalRequest(oid, type, "release", null);
        }
        return object;
    }

    /**
	 * Gives the source environment.
	 * <p>
	 * @return   the source environment of this bridge
	 * @see      com.sun.star.uno.IBridge#getSourceEnvironment
	 */
    public IEnvironment getSourceEnvironment() {
        return _java_environment;
    }

    /**
	 * Gives the destination environment.
	 * <p>
	 * @return   the destination environment of this bridge
	 * @see      com.sun.star.uno.IBridge#getTargetEnvironment
	 */
    public IEnvironment getTargetEnvironment() {
        return null;
    }

    /**
	 * Increases the life count.
	 * <p>
	 * @see com.sun.star.uno.IBridge#acquire
	 */
    public synchronized void acquire() {
        ++_life_count;
        if (DEBUG) System.err.println("##### " + getClass().getName() + ".acquire:" + _life_count);
    }

    /**
	 * Decreases the life count.
	 * If the life count drops to zero, the bridge disposes itself.
	 * <p>
	 * @see com.sun.star.uno.IBridge#release
	 */
    public void release() {
        boolean dispose;
        synchronized (this) {
            --_life_count;
            dispose = _life_count <= 0;
        }
        if (dispose) {
            dispose(new com.sun.star.uno.RuntimeException("end of life"));
        }
    }

    public void dispose() {
        dispose(new com.sun.star.uno.RuntimeException("user dispose"));
    }

    private void dispose(Throwable throwable) {
        synchronized (this) {
            if (disposed) {
                return;
            }
            disposed = true;
        }
        notifyListeners();
        for (Iterator i = disposeListeners.iterator(); i.hasNext(); ) {
            ((DisposeListener) i.next()).notifyDispose(this);
        }
        try {
            _messageDispatcher.terminate();
            _xConnection.close();
            if (Thread.currentThread() != _messageDispatcher && _messageDispatcher.isAlive()) {
                if (System.getProperty("os.name", "").toLowerCase().equals("linux") && System.getProperty("java.version", "").startsWith("1.3.") && (System.getProperty("java.vendor", "").toLowerCase().indexOf("sun") != -1 || System.getProperty("java.vendor", "").toLowerCase().indexOf("blackdown") != -1)) {
                    _messageDispatcher.suspend();
                    _messageDispatcher.resume();
                }
                _messageDispatcher.join(1000);
                if (_messageDispatcher.isAlive()) {
                    _messageDispatcher.interrupt();
                    _messageDispatcher.join();
                }
            }
            _iThreadPool.dispose(throwable);
            freeHolders();
            ((java_environment) _java_environment).revokeAllProxies();
            if (DEBUG) {
                if (_life_count != 0) {
                    System.err.println(getClass().getName() + ".dispose - life count (proxies left):" + _life_count);
                }
                _java_environment.list();
            }
            _xConnection = null;
            _java_environment = null;
            _messageDispatcher = null;
        } catch (InterruptedException e) {
            System.err.println(getClass().getName() + ".dispose - InterruptedException:" + e);
        } catch (com.sun.star.io.IOException e) {
            System.err.println(getClass().getName() + ".dispose - IOException:" + e);
        }
    }

    public Object getInstance(String instanceName) {
        Type t = new Type(XInterface.class);
        return sendInternalRequest(instanceName, t, "queryInterface", new Object[] { t });
    }

    /**
	 * Gives the name of this bridge
	 * <p>
	 * @return  the name of this bridge
	 * @see     com.sun.star.bridge.XBridge#getName
	 */
    public String getName() {
        return _name;
    }

    /**
	 * Gives a description of the connection type and protocol used
	 * <p>
	 * @return  connection type and protocol
	 * @see     com.sun.star.bridge.XBridge#getDescription
	 */
    public String getDescription() {
        return protocol + "," + _xConnection.getDescription();
    }

    public void sendReply(boolean exception, ThreadId threadId, Object result) {
        if (DEBUG) {
            System.err.println("##### " + getClass().getName() + ".sendReply: " + exception + " " + result);
        }
        checkDisposed();
        try {
            _iProtocol.writeReply(exception, threadId, result);
        } catch (IOException e) {
            dispose(e);
            throw new DisposedException("unexpected " + e);
        } catch (RuntimeException e) {
            dispose(e);
            throw e;
        } catch (Error e) {
            dispose(e);
            throw e;
        }
    }

    public Object sendRequest(String oid, Type type, String operation, Object[] params) throws Throwable {
        Object result = null;
        checkDisposed();
        boolean goThroughThreadPool = false;
        ThreadId threadId = _iThreadPool.getThreadId();
        Object handle = _iThreadPool.attach(threadId);
        try {
            boolean sync;
            try {
                sync = _iProtocol.writeRequest(oid, TypeDescription.getTypeDescription(type), operation, threadId, params);
            } catch (IOException e) {
                DisposedException d = new DisposedException(e.toString());
                dispose(d);
                throw d;
            }
            if (sync && Thread.currentThread() != _messageDispatcher) {
                result = _iThreadPool.enter(handle, threadId);
            }
        } finally {
            _iThreadPool.detach(handle, threadId);
            if (operation.equals("release")) release();
        }
        if (DEBUG) System.err.println("##### " + getClass().getName() + ".sendRequest left:" + result);
        if (operation.equals("queryInterface") && result instanceof Any) {
            Any a = (Any) result;
            if (a.getType().getTypeClass() == TypeClass.INTERFACE) {
                result = a.getObject();
            } else {
                result = null;
            }
        }
        return result;
    }

    private Object sendInternalRequest(String oid, Type type, String operation, Object[] arguments) {
        try {
            return sendRequest(oid, type, operation, arguments);
        } catch (Error e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected " + e);
        }
    }

    public void addEventListener(XEventListener xEventListener) {
        _listeners.addElement(xEventListener);
    }

    public void removeEventListener(XEventListener xEventListener) {
        _listeners.removeElement(xEventListener);
    }

    public void addDisposeListener(DisposeListener listener) {
        synchronized (this) {
            if (!disposed) {
                disposeListeners.add(listener);
                return;
            }
        }
        listener.notifyDispose(this);
    }

    private synchronized void checkDisposed() {
        if (disposed) {
            throw new DisposedException("java_remote_bridge " + this + " is disposed");
        }
    }

    private final ProxyFactory proxyFactory;

    private final ArrayList disposeListeners = new ArrayList();
}
