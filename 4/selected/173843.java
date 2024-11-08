package com.sun.corba.se.impl.legacy.connection;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.CompletionStatus;
import com.sun.corba.se.pept.transport.Acceptor;
import com.sun.corba.se.pept.transport.ContactInfo;
import com.sun.corba.se.spi.ior.IOR;
import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;
import com.sun.corba.se.spi.orb.ORB;
import com.sun.corba.se.spi.transport.CorbaConnection;
import com.sun.corba.se.spi.transport.CorbaContactInfo;
import com.sun.corba.se.spi.transport.SocketInfo;
import com.sun.corba.se.impl.orbutil.ORBUtility;
import com.sun.corba.se.impl.transport.SocketOrChannelContactInfoImpl;
import com.sun.corba.se.impl.transport.SocketOrChannelConnectionImpl;

/**
 * @author Harold Carr
 */
public class SocketFactoryConnectionImpl extends SocketOrChannelConnectionImpl {

    public SocketFactoryConnectionImpl(ORB orb, CorbaContactInfo contactInfo, boolean useSelectThreadToWait, boolean useWorkerThread) {
        super(orb, useSelectThreadToWait, useWorkerThread);
        this.contactInfo = contactInfo;
        boolean isBlocking = !useSelectThreadToWait;
        SocketInfo socketInfo = ((SocketFactoryContactInfoImpl) contactInfo).socketInfo;
        try {
            socket = orb.getORBData().getLegacySocketFactory().createSocket(socketInfo);
            socketChannel = socket.getChannel();
            if (socketChannel != null) {
                socketChannel.configureBlocking(isBlocking);
            } else {
                setUseSelectThreadToWait(false);
            }
            if (orb.transportDebugFlag) {
                dprint(".initialize: connection created: " + socket);
            }
        } catch (GetEndPointInfoAgainException ex) {
            throw wrapper.connectFailure(ex, socketInfo.getType(), socketInfo.getHost(), Integer.toString(socketInfo.getPort()));
        } catch (Exception ex) {
            throw wrapper.connectFailure(ex, socketInfo.getType(), socketInfo.getHost(), Integer.toString(socketInfo.getPort()));
        }
        state = OPENING;
    }

    public String toString() {
        synchronized (stateEvent) {
            return "SocketFactoryConnectionImpl[" + " " + (socketChannel == null ? socket.toString() : socketChannel.toString()) + " " + getStateString(state) + " " + shouldUseSelectThreadToWait() + " " + shouldUseWorkerThreadForEvent() + "]";
        }
    }

    public void dprint(String msg) {
        ORBUtility.dprint("SocketFactoryConnectionImpl", msg);
    }
}
