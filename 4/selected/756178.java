package remote;

import infrastructure.exceptions.BaseServerException;
import infrastructure.exceptions.BaseServerRuntimeException;
import infrastructure.exceptions.ServerFataError;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import core.OperationWrapper;

/**
 * This class represents active connection with the server connection 
 * @author sashas
 *
 */
public class ConnectionHandler implements Runnable {

    private SocketChannel channel;

    private RemoteInvocation request;

    public ConnectionHandler(RemoteInvocation request, SocketChannel channel) {
        this.channel = channel;
        this.request = request;
    }

    protected SocketChannel getChannel() {
        return channel;
    }

    protected RemoteInvocation getRequest() {
        return request;
    }

    protected void handleResponce(Serializable responceObject) {
        try {
            writeResponse(responceObject);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).logp(Level.WARNING, this.getClass().getName(), "handleResponce", "error, Could not write responce .. writing error", ex);
            try {
                writeResponse(new ServerFataError("Writing responce failed", ex));
            } catch (Exception fatalEx) {
                Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleResponce", "Error, Could not write error .. closing socket", fatalEx);
                try {
                    getChannel().socket().close();
                } catch (Exception networkEx) {
                    Logger.getLogger(this.getClass().getName()).logp(Level.SEVERE, this.getClass().getName(), "handleResponce", "Error, Could not close socket .. ", networkEx);
                }
            }
        }
    }

    protected void writeResponse(Serializable responceObject) throws IOException {
        ConnectionManager.writeToSocket(responceObject, getChannel());
    }

    public void run() {
        try {
            Object retVal = OperationWrapper.doOperation(this.request);
            if ((null == retVal) || (retVal instanceof Serializable)) {
                handleResponce((Serializable) retVal);
            } else {
                Logger.getLogger(this.getClass().getName()).logp(Level.WARNING, this.getClass().getName(), "run", "Return value of the invocation '" + request + "' is not serializable, value of type '" + retVal.getClass().getSimpleName() + "'");
                handleResponce(new BaseServerRuntimeException("Operation returned non-serializable value"));
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).logp(Level.WARNING, this.getClass().getName(), "run", "Exception, during server op .. ", ex);
            handleResponce(ex);
        }
    }
}
