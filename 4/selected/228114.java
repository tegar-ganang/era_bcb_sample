package sun.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.rmi.MarshalException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.rmi.server.Operation;
import java.rmi.server.RemoteCall;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.security.AccessController;
import sun.rmi.runtime.Log;
import sun.rmi.transport.Connection;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.StreamRemoteCall;
import sun.security.action.GetBooleanAction;

/**
 * NOTE: There is a JDK-internal dependency on the existence of this
 * class's getLiveRef method (as it is inherited by UnicastRef2) in
 * the implementation of javax.management.remote.rmi.RMIConnector.
 **/
public class UnicastRef implements RemoteRef {

    /**
     * Client-side transport log.
     */
    public static final Log clientRefLog = Log.getLog("sun.rmi.client.ref", "transport", Util.logLevel);

    /**
     * Client-side call log.
     */
    public static final Log clientCallLog = Log.getLog("sun.rmi.client.call", "RMI", AccessController.doPrivileged(new GetBooleanAction("sun.rmi.client.logCalls")));

    protected LiveRef ref;

    /**
     * Create a new (empty) Unicast remote reference.
     */
    public UnicastRef() {
    }

    /**
     * Create a new Unicast RemoteRef.
     */
    public UnicastRef(LiveRef liveRef) {
        ref = liveRef;
    }

    /**
     * Returns the current value of this UnicastRef's underlying
     * LiveRef.
     *
     * NOTE: There is a JDK-internal dependency on the existence of
     * this method (as it is inherited by UnicastRef) in the
     * implementation of javax.management.remote.rmi.RMIConnector.
     **/
    public LiveRef getLiveRef() {
        return ref;
    }

    /**
     * Invoke a method. This form of delegating method invocation
     * to the reference allows the reference to take care of
     * setting up the connection to the remote host, marshalling
     * some representation for the method and parameters, then
     * communicating the method invocation to the remote host.
     * This method either returns the result of a method invocation
     * on the remote object which resides on the remote host or
     * throws a RemoteException if the call failed or an
     * application-level exception if the remote invocation throws
     * an exception.
     *
     * @param obj the proxy for the remote object
     * @param method the method to be invoked
     * @param params the parameter list
     * @param opnum  a hash that may be used to represent the method
     * @since 1.2
     */
    public Object invoke(Remote obj, Method method, Object[] params, long opnum) throws Exception {
        if (clientRefLog.isLoggable(Log.VERBOSE)) {
            clientRefLog.log(Log.VERBOSE, "method: " + method);
        }
        if (clientCallLog.isLoggable(Log.VERBOSE)) {
            logClientCall(obj, method);
        }
        Connection conn = ref.getChannel().newConnection();
        RemoteCall call = null;
        boolean reuse = true;
        boolean alreadyFreed = false;
        try {
            if (clientRefLog.isLoggable(Log.VERBOSE)) {
                clientRefLog.log(Log.VERBOSE, "opnum = " + opnum);
            }
            call = new StreamRemoteCall(conn, ref.getObjID(), -1, opnum);
            try {
                ObjectOutput out = call.getOutputStream();
                marshalCustomCallData(out);
                Class<?>[] types = method.getParameterTypes();
                for (int i = 0; i < types.length; i++) {
                    marshalValue(types[i], params[i], out);
                }
            } catch (IOException e) {
                clientRefLog.log(Log.BRIEF, "IOException marshalling arguments: ", e);
                throw new MarshalException("error marshalling arguments", e);
            }
            call.executeCall();
            try {
                Class<?> rtype = method.getReturnType();
                if (rtype == void.class) return null;
                ObjectInput in = call.getInputStream();
                Object returnValue = unmarshalValue(rtype, in);
                alreadyFreed = true;
                clientRefLog.log(Log.BRIEF, "free connection (reuse = true)");
                ref.getChannel().free(conn, true);
                return returnValue;
            } catch (IOException e) {
                clientRefLog.log(Log.BRIEF, "IOException unmarshalling return: ", e);
                throw new UnmarshalException("error unmarshalling return", e);
            } catch (ClassNotFoundException e) {
                clientRefLog.log(Log.BRIEF, "ClassNotFoundException unmarshalling return: ", e);
                throw new UnmarshalException("error unmarshalling return", e);
            } finally {
                try {
                    call.done();
                } catch (IOException e) {
                    reuse = false;
                }
            }
        } catch (RuntimeException e) {
            if ((call == null) || (((StreamRemoteCall) call).getServerException() != e)) {
                reuse = false;
            }
            throw e;
        } catch (RemoteException e) {
            reuse = false;
            throw e;
        } catch (Error e) {
            reuse = false;
            throw e;
        } finally {
            if (!alreadyFreed) {
                if (clientRefLog.isLoggable(Log.BRIEF)) {
                    clientRefLog.log(Log.BRIEF, "free connection (reuse = " + reuse + ")");
                }
                ref.getChannel().free(conn, reuse);
            }
        }
    }

    protected void marshalCustomCallData(ObjectOutput out) throws IOException {
    }

    /**
     * Marshal value to an ObjectOutput sink using RMI's serialization
     * format for parameters or return values.
     */
    protected static void marshalValue(Class<?> type, Object value, ObjectOutput out) throws IOException {
        if (type.isPrimitive()) {
            if (type == int.class) {
                out.writeInt(((Integer) value).intValue());
            } else if (type == boolean.class) {
                out.writeBoolean(((Boolean) value).booleanValue());
            } else if (type == byte.class) {
                out.writeByte(((Byte) value).byteValue());
            } else if (type == char.class) {
                out.writeChar(((Character) value).charValue());
            } else if (type == short.class) {
                out.writeShort(((Short) value).shortValue());
            } else if (type == long.class) {
                out.writeLong(((Long) value).longValue());
            } else if (type == float.class) {
                out.writeFloat(((Float) value).floatValue());
            } else if (type == double.class) {
                out.writeDouble(((Double) value).doubleValue());
            } else {
                throw new Error("Unrecognized primitive type: " + type);
            }
        } else {
            out.writeObject(value);
        }
    }

    /**
     * Unmarshal value from an ObjectInput source using RMI's serialization
     * format for parameters or return values.
     */
    protected static Object unmarshalValue(Class<?> type, ObjectInput in) throws IOException, ClassNotFoundException {
        if (type.isPrimitive()) {
            if (type == int.class) {
                return Integer.valueOf(in.readInt());
            } else if (type == boolean.class) {
                return Boolean.valueOf(in.readBoolean());
            } else if (type == byte.class) {
                return Byte.valueOf(in.readByte());
            } else if (type == char.class) {
                return Character.valueOf(in.readChar());
            } else if (type == short.class) {
                return Short.valueOf(in.readShort());
            } else if (type == long.class) {
                return Long.valueOf(in.readLong());
            } else if (type == float.class) {
                return Float.valueOf(in.readFloat());
            } else if (type == double.class) {
                return Double.valueOf(in.readDouble());
            } else {
                throw new Error("Unrecognized primitive type: " + type);
            }
        } else {
            return in.readObject();
        }
    }

    /**
     * Create an appropriate call object for a new call on this object.
     * Passing operation array and index, allows the stubs generator to
     * assign the operation indexes and interpret them. The RemoteRef
     * may need the operation to encode in for the call.
     */
    public RemoteCall newCall(RemoteObject obj, Operation[] ops, int opnum, long hash) throws RemoteException {
        clientRefLog.log(Log.BRIEF, "get connection");
        Connection conn = ref.getChannel().newConnection();
        try {
            clientRefLog.log(Log.VERBOSE, "create call context");
            if (clientCallLog.isLoggable(Log.VERBOSE)) {
                logClientCall(obj, ops[opnum]);
            }
            RemoteCall call = new StreamRemoteCall(conn, ref.getObjID(), opnum, hash);
            try {
                marshalCustomCallData(call.getOutputStream());
            } catch (IOException e) {
                throw new MarshalException("error marshaling " + "custom call data");
            }
            return call;
        } catch (RemoteException e) {
            ref.getChannel().free(conn, false);
            throw e;
        }
    }

    /**
     * Invoke makes the remote call present in the RemoteCall object.
     *
     * Invoke will raise any "user" exceptions which
     * should pass through and not be caught by the stub.  If any
     * exception is raised during the remote invocation, invoke should
     * take care of cleaning up the connection before raising the
     * "user" or remote exception.
     */
    public void invoke(RemoteCall call) throws Exception {
        try {
            clientRefLog.log(Log.VERBOSE, "execute call");
            call.executeCall();
        } catch (RemoteException e) {
            clientRefLog.log(Log.BRIEF, "exception: ", e);
            free(call, false);
            throw e;
        } catch (Error e) {
            clientRefLog.log(Log.BRIEF, "error: ", e);
            free(call, false);
            throw e;
        } catch (RuntimeException e) {
            clientRefLog.log(Log.BRIEF, "exception: ", e);
            free(call, false);
            throw e;
        } catch (Exception e) {
            clientRefLog.log(Log.BRIEF, "exception: ", e);
            free(call, true);
            throw e;
        }
    }

    /**
     * Private method to free a connection.
     */
    private void free(RemoteCall call, boolean reuse) throws RemoteException {
        Connection conn = ((StreamRemoteCall) call).getConnection();
        ref.getChannel().free(conn, reuse);
    }

    /**
     * Done should only be called if the invoke returns successfully
     * (non-exceptionally) to the stub. It allows the remote reference to
     * clean up (or reuse) the connection.
     */
    public void done(RemoteCall call) throws RemoteException {
        clientRefLog.log(Log.BRIEF, "free connection (reuse = true)");
        free(call, true);
        try {
            call.done();
        } catch (IOException e) {
        }
    }

    /**
     * Log the details of an outgoing call.  The method parameter is either of
     * type java.lang.reflect.Method or java.rmi.server.Operation.
     */
    void logClientCall(Object obj, Object method) {
        clientCallLog.log(Log.VERBOSE, "outbound call: " + ref + " : " + obj.getClass().getName() + ref.getObjID().toString() + ": " + method);
    }

    /**
     * Returns the class of the ref type to be serialized
     */
    public String getRefClass(ObjectOutput out) {
        return "UnicastRef";
    }

    /**
     * Write out external representation for remote ref.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        ref.write(out, false);
    }

    /**
     * Read in external representation for remote ref.
     * @exception ClassNotFoundException If the class for an object
     * being restored cannot be found.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ref = LiveRef.read(in, false);
    }

    /**
     * Method from object, forward from RemoteObject
     */
    public String remoteToString() {
        return Util.getUnqualifiedName(getClass()) + " [liveRef: " + ref + "]";
    }

    /**
     * default implementation of hashCode for remote objects
     */
    public int remoteHashCode() {
        return ref.hashCode();
    }

    /** default implementation of equals for remote objects
     */
    public boolean remoteEquals(RemoteRef sub) {
        if (sub instanceof UnicastRef) return ref.remoteEquals(((UnicastRef) sub).ref);
        return false;
    }
}
