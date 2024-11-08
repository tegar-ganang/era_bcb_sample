package org.objectstyle.cayenne.remote.service;

import java.io.Serializable;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.event.EventBridge;
import org.objectstyle.cayenne.remote.BaseConnection;
import org.objectstyle.cayenne.remote.ClientMessage;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.util.Util;

/**
 * A ClientConnection that connects to a DataChannel. Used as an emulator of a remote
 * service. Emulation includes serialization/deserialization of objects.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class LocalConnection extends BaseConnection {

    public static final int NO_SERIALIZATION = 0;

    public static final int JAVA_SERIALIZATION = 1;

    public static final int HESSIAN_SERIALIZATION = 2;

    protected DataChannel channel;

    protected int serializationPolicy;

    /**
     * Creates LocalConnector with specified handler and no serialization.
     */
    public LocalConnection(DataChannel handler) {
        this(handler, NO_SERIALIZATION);
    }

    /**
     * Creates a LocalConnector with specified handler and serialization policy. Valid
     * policies are defined as final static int field in this class.
     */
    public LocalConnection(DataChannel handler, int serializationPolicy) {
        this.channel = handler;
        this.serializationPolicy = serializationPolicy == JAVA_SERIALIZATION || serializationPolicy == HESSIAN_SERIALIZATION ? serializationPolicy : NO_SERIALIZATION;
    }

    public boolean isSerializingMessages() {
        return serializationPolicy == JAVA_SERIALIZATION || serializationPolicy == HESSIAN_SERIALIZATION;
    }

    /**
     * Returns wrapped DataChannel.
     */
    public DataChannel getChannel() {
        return channel;
    }

    /**
     * Returns null.
     */
    public EventBridge getServerEventBridge() {
        return null;
    }

    /**
     * Does nothing.
     */
    protected void beforeSendMessage(ClientMessage message) {
    }

    /**
     * Dispatches a message to an internal handler.
     */
    protected Object doSendMessage(ClientMessage message) throws CayenneRuntimeException {
        ClientMessage processedMessage;
        try {
            switch(serializationPolicy) {
                case HESSIAN_SERIALIZATION:
                    processedMessage = (ClientMessage) HessianUtil.cloneViaClientServerSerialization(message, channel.getEntityResolver());
                    break;
                case JAVA_SERIALIZATION:
                    processedMessage = (ClientMessage) Util.cloneViaSerialization(message);
                    break;
                default:
                    processedMessage = message;
            }
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error serializing message", ex);
        }
        Serializable result = (Serializable) DispatchHelper.dispatch(channel, processedMessage);
        try {
            switch(serializationPolicy) {
                case HESSIAN_SERIALIZATION:
                    return HessianUtil.cloneViaServerClientSerialization(result, channel.getEntityResolver());
                case JAVA_SERIALIZATION:
                    return Util.cloneViaSerialization(result);
                default:
                    return result;
            }
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error deserializing result", ex);
        }
    }
}
