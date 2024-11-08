package sun.rmi.transport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.rmi.RemoteException;
import java.rmi.MarshalException;
import java.rmi.UnmarshalException;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteCall;
import sun.rmi.runtime.Log;
import sun.rmi.server.UnicastRef;
import sun.rmi.transport.tcp.TCPEndpoint;

/**
 * Stream-based implementation of the RemoteCall interface.
 *
 * @author Ann Wollrath
 */
public class StreamRemoteCall implements RemoteCall {

    private ConnectionInputStream in = null;

    private ConnectionOutputStream out = null;

    private Connection conn;

    private boolean resultStarted = false;

    private Exception serverException = null;

    public StreamRemoteCall(Connection c) {
        conn = c;
    }

    public StreamRemoteCall(Connection c, ObjID id, int op, long hash) throws RemoteException {
        try {
            conn = c;
            Transport.transportLog.log(Log.VERBOSE, "write remote call header...");
            conn.getOutputStream().write(TransportConstants.Call);
            getOutputStream();
            id.write(out);
            out.writeInt(op);
            out.writeLong(hash);
        } catch (IOException e) {
            throw new MarshalException("Error marshaling call header", e);
        }
    }

    /**
     * Return the connection associated with this call.
     */
    public Connection getConnection() {
        return conn;
    }

    /**
     * Return the output stream the stub/skeleton should put arguments/results
     * into.
     */
    public ObjectOutput getOutputStream() throws IOException {
        return getOutputStream(false);
    }

    private ObjectOutput getOutputStream(boolean resultStream) throws IOException {
        if (out == null) {
            Transport.transportLog.log(Log.VERBOSE, "getting output stream");
            out = new ConnectionOutputStream(conn, resultStream);
        }
        return out;
    }

    /**
     * Release the outputStream  Currently, will not complain if the
     * output stream is released more than once.
     */
    public void releaseOutputStream() throws IOException {
        try {
            if (out != null) {
                try {
                    out.flush();
                } finally {
                    out.done();
                }
            }
            conn.releaseOutputStream();
        } finally {
            out = null;
        }
    }

    /**
     * Get the InputStream the stub/skeleton should get results/arguments
     * from.
     */
    public ObjectInput getInputStream() throws IOException {
        if (in == null) {
            Transport.transportLog.log(Log.VERBOSE, "getting input stream");
            in = new ConnectionInputStream(conn.getInputStream());
        }
        return in;
    }

    /**
     * Release the input stream, this would allow some transports to release
     * the channel early.
     */
    public void releaseInputStream() throws IOException {
        try {
            if (in != null) {
                try {
                    in.done();
                } catch (RuntimeException e) {
                }
                in.registerRefs();
                in.done(conn);
            }
            conn.releaseInputStream();
        } finally {
            in = null;
        }
    }

    /**
     * Returns an output stream (may put out header information
     * relating to the success of the call).
     * @param success If true, indicates normal return, else indicates
     * exceptional return.
     * @exception StreamCorruptedException If result stream previously
     * acquired
     * @exception IOException For any other problem with I/O.
     */
    public ObjectOutput getResultStream(boolean success) throws IOException {
        if (resultStarted) throw new StreamCorruptedException("result already in progress"); else resultStarted = true;
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeByte(TransportConstants.Return);
        getOutputStream(true);
        if (success) out.writeByte(TransportConstants.NormalReturn); else out.writeByte(TransportConstants.ExceptionalReturn);
        out.writeID();
        return out;
    }

    /**
     * Do whatever it takes to execute the call.
     */
    public void executeCall() throws Exception {
        byte returnType;
        DGCAckHandler ackHandler = null;
        try {
            if (out != null) {
                ackHandler = out.getDGCAckHandler();
            }
            releaseOutputStream();
            DataInputStream rd = new DataInputStream(conn.getInputStream());
            byte op = rd.readByte();
            if (op != TransportConstants.Return) {
                if (Transport.transportLog.isLoggable(Log.BRIEF)) {
                    Transport.transportLog.log(Log.BRIEF, "transport return code invalid: " + op);
                }
                throw new UnmarshalException("Transport return code invalid");
            }
            getInputStream();
            returnType = in.readByte();
            in.readID();
        } catch (UnmarshalException e) {
            throw e;
        } catch (IOException e) {
            throw new UnmarshalException("Error unmarshaling return header", e);
        } finally {
            if (ackHandler != null) {
                ackHandler.release();
            }
        }
        switch(returnType) {
            case TransportConstants.NormalReturn:
                break;
            case TransportConstants.ExceptionalReturn:
                Object ex;
                try {
                    ex = in.readObject();
                } catch (Exception e) {
                    throw new UnmarshalException("Error unmarshaling return", e);
                }
                if (ex instanceof Exception) {
                    exceptionReceivedFromServer((Exception) ex);
                } else {
                    throw new UnmarshalException("Return type not Exception");
                }
            default:
                if (Transport.transportLog.isLoggable(Log.BRIEF)) {
                    Transport.transportLog.log(Log.BRIEF, "return code invalid: " + returnType);
                }
                throw new UnmarshalException("Return code invalid");
        }
    }

    /**
     * Routine that causes the stack traces of remote exceptions to be
     * filled in with the current stack trace on the client.  Detail
     * exceptions are filled in iteratively.
     */
    protected void exceptionReceivedFromServer(Exception ex) throws Exception {
        serverException = ex;
        StackTraceElement[] serverTrace = ex.getStackTrace();
        StackTraceElement[] clientTrace = (new Throwable()).getStackTrace();
        StackTraceElement[] combinedTrace = new StackTraceElement[serverTrace.length + clientTrace.length];
        System.arraycopy(serverTrace, 0, combinedTrace, 0, serverTrace.length);
        System.arraycopy(clientTrace, 0, combinedTrace, serverTrace.length, clientTrace.length);
        ex.setStackTrace(combinedTrace);
        if (UnicastRef.clientCallLog.isLoggable(Log.BRIEF)) {
            TCPEndpoint ep = (TCPEndpoint) conn.getChannel().getEndpoint();
            UnicastRef.clientCallLog.log(Log.BRIEF, "outbound call " + "received exception: [" + ep.getHost() + ":" + ep.getPort() + "] exception: ", ex);
        }
        throw ex;
    }

    public Exception getServerException() {
        return serverException;
    }

    public void done() throws IOException {
        releaseInputStream();
    }
}
