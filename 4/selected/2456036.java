package com.teletalk.jserver.tcp.messaging;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import org.apache.log4j.Level;
import com.teletalk.jserver.tcp.TcpEndPoint;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.messaging.command.ConnectRequest;
import com.teletalk.jserver.tcp.messaging.command.ConnectResponse;
import com.teletalk.jserver.tcp.messaging.command.EndPointCheckCommand;
import com.teletalk.jserver.util.ClassLoaderObjectInputStream;
import com.teletalk.jserver.util.NoHeadersClassLoaderObjectInputStream;
import com.teletalk.jserver.util.NoHeadersObjectOutputStream;
import com.teletalk.jserver.util.OutputStreamer;
import com.teletalk.jserver.util.SpillOverByteArrayOutputStream;
import com.teletalk.jserver.util.Streamable;
import com.teletalk.jserver.util.StringUtils;

/**
 * MessagingEndPoint implements a communication endpoint, representing a connection to a remote messaging system. 
 * This class implements both client side and server side logic and handles that actual sending and receiving of messages. <br>
 * <br>
 * MessagingEndPoint contains the nested class {@link MessageWriter}), which contains the message dispatch 
 * logic that is specific to the different types of messages (object, byte array and input stream). For object messages, this implementation 
 * performs the serialization of the message into a byte array, which is performed in-memory in order to be able to calculate the size of 
 * the message body and set the corresponding header field.
 *  
 * @author Tobias L�fstrand
 * 
 * @since 1.2
 */
public class MessagingEndPoint extends TcpEndPoint {

    private MessagingEndPointInputStream endPointInputStream;

    private ClassLoaderObjectInputStream classLoaderObjectInputStream;

    private OutputStream endPointOutputStream;

    private final AbstractMessagingManager messagingManager;

    private Destination destination;

    private long lastReadyTime = -1;

    private static final int BYTE_BUFFER_INITIAL_SIZE = 8192;

    private static final int BYTE_BUFFER_SPILL_OVER_LIMIT = 16 * 1024 * 1024;

    protected ByteArrayOutputStream headerStreamableSerializerByteStream;

    protected EndPointOutputStreamer headerStreamableSerializerStream;

    protected SpillOverByteArrayOutputStream objectSerializerByteStream;

    protected NoHeadersObjectOutputStream objectSerializerObjectStream;

    protected SpillOverByteArrayOutputStream streamableSerializerByteStream;

    protected EndPointOutputStreamer streamableSerializerStream;

    private static final int BODY_READ_BUFFER_LIMIT = 8192;

    private final int streamBufferSize;

    private final byte[] streamBuffer;

    private boolean disconnectHeaderReceived = false;

    /** @since 2.1 (20050429) */
    private boolean firstEndPointInGroup = false;

    /**
	 * Creates a new MessagingEndPoint.
	 * 
	 * @param messagingManager reference to a parent MessagingManager.
	 */
    public MessagingEndPoint(MessagingManager messagingManager) {
        this(messagingManager, "MessagingEndPoint");
    }

    /**
	 * Creates a new MessagingEndPoint.
	 * 
	 * @param messagingManager reference to a parent MessagingManager.
	 */
    public MessagingEndPoint(MessagingManager messagingManager, int streamBufferSize) {
        this(messagingManager, "MessagingEndPoint", streamBufferSize);
    }

    /**
	 * Creates a new MessagingEndPoint.
	 * 
	 * @param messagingManager reference to a parent MessagingManager.
	 * @param name the name of this MessagingEndPoint.
	 */
    public MessagingEndPoint(MessagingManager messagingManager, String name) {
        this(messagingManager, name, 8192);
    }

    /**
	 * Creates a new MessagingEndPoint.
	 * 
	 * @param messagingManager reference to a parent MessagingManager.
	 * @param name the name of this MessagingEndPoint.
	 */
    public MessagingEndPoint(MessagingManager messagingManager, String name, int streamBufferSize) {
        super(messagingManager, name);
        this.messagingManager = messagingManager;
        this.destination = null;
        this.streamBufferSize = streamBufferSize;
        this.streamBuffer = new byte[this.streamBufferSize];
    }

    /**
    * Gets the {@link Destination} associated with this endpoint.
    * 
    * @return the {@link Destination} associated with this endpoint.
    */
    public Destination getDestination() {
        return this.destination;
    }

    /**
    * Returns the time (millisecond value since January 1, 1970 UTC) when this endpoint last was made ready.
    * 
    * @return the time (millisecond value since January 1, 1970 UTC) when this endpoint last was made ready.
    */
    public long getLastReadyTime() {
        return lastReadyTime;
    }

    /**
    * Sets the time (millisecond value since January 1, 1970 UTC) when this endpoint last was made ready.
    * 
    * @param lastReadyTime the time (millisecond value since January 1, 1970 UTC) when this endpoint last was made ready.
    */
    public void setLastReadyTime(long lastReadyTime) {
        this.lastReadyTime = lastReadyTime;
    }

    /**
    * Checks if a disconnect header has been received by this endpoint.
    * 
    * @since 1.3.1 build 694.
    */
    public boolean isDisconnectHeaderReceived() {
        return disconnectHeaderReceived;
    }

    /**
    * Checks if this endpoint is the first to be connected in the group.
    * 
    * @since 2.1 (20050429)
    */
    public boolean isFirstEndPointInGroup() {
        return firstEndPointInGroup;
    }

    /**
    * Gets the MessagingEndPointInputStream used to read data from the socket of this endpoint.
    * 
    * @return the MessagingEndPointInputStream used to read data from the socket of this endpoint.
    */
    protected MessagingEndPointInputStream getMessagingEndPointInputStream() {
        return this.endPointInputStream;
    }

    /**
	 * Initializes the input stream (the field {@link #inputStream}) used to read data from the socket. All other 
	 * input streams reading data from the socket should be connected to that stream.<br>
	 * <br>
	 * This implementation creates an instance of the class {@link MessagingEndPointInputStream} connected to a 
	 * <code>BufferedInputStream</code>, which in turn is connected to the socket input stream.
	 * 
	 * @throws IOException if an error occurs while creating the stream.
	 */
    protected void initInputStream() throws IOException {
        this.endPointInputStream = new MessagingEndPointInputStream(new BufferedInputStream(getSocket().getInputStream()));
        super.inputStream = this.endPointInputStream;
    }

    /**
    * Called to initialize the <code>java.io.ObjectInputStream</code> object (used in the method {@link #readObject()}) of this MessagingEndPoint.
    * 
    * @since 1.3
    */
    protected void initObjectInputStream() throws IOException {
        this.initObjectInputStream(true);
    }

    /**
	 * Called to initialize the <code>java.io.ObjectInputStream</code> object (used in the method {@link #readObject()}) of this MessagingEndPoint.
	 * 
	 * @since 2.1.2 (20060308)
	 */
    protected void initObjectInputStream(final boolean readHeaders) throws IOException {
        if (readHeaders) this.classLoaderObjectInputStream = new ClassLoaderObjectInputStream(super.inputStream); else this.classLoaderObjectInputStream = new NoHeadersClassLoaderObjectInputStream(super.inputStream);
        super.objectReader = this.classLoaderObjectInputStream;
    }

    /**
	 * Initializes the output stream (the field {@link #outputStream}) used to write data to the socket. All other 
    * output streams writing data to the socket should be connected to that stream.
    * 
    * @throws IOException if an error occurs while creating the stream.
    * 
	 * @since 1.3.1
	 */
    protected void initOutputStream() throws IOException {
        this.endPointOutputStream = getSocket().getOutputStream();
        super.outputStream = this.endPointOutputStream;
    }

    /**
    * Internal endpoint initialization method.
    */
    private void initEndPoint() {
        try {
            super.setObjectStreamResetInterval(1);
            this.headerStreamableSerializerByteStream = new ByteArrayOutputStream(BYTE_BUFFER_INITIAL_SIZE);
            this.headerStreamableSerializerStream = new EndPointOutputStreamer(this.headerStreamableSerializerByteStream);
            this.objectSerializerByteStream = new SpillOverByteArrayOutputStream(BYTE_BUFFER_INITIAL_SIZE, BYTE_BUFFER_SPILL_OVER_LIMIT);
            this.objectSerializerObjectStream = new NoHeadersObjectOutputStream(objectSerializerByteStream);
            this.objectSerializerObjectStream.reset();
            this.streamableSerializerByteStream = new SpillOverByteArrayOutputStream(BYTE_BUFFER_INITIAL_SIZE, BYTE_BUFFER_SPILL_OVER_LIMIT);
            this.streamableSerializerStream = new EndPointOutputStreamer(this.streamableSerializerByteStream);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while creating object message body serialization streams. Error is: " + e + ".");
        }
    }

    /**
	 * Client side initialization method run after attempts has been made to connect this endpoint. This method is responsible for performing handshaking procedures 
	 * if a connection has been successfully established. This is accomplished by sending a {@link ConnectRequest} and 
	 * waiting for a {@link ConnectResponse}. If this endpoint represents the first connection created to a 
	 * certain remote messaging system, the unique id used to identify this client in the remote messaging system will be determined during handshaking.
	 *  
	 * @param connectionSuccessful boolean flag indicating if connection was successful.
	 */
    protected boolean initClientSide(boolean connectionSuccessful) {
        HashMap remoteMessagingSystemMetaData;
        MessagingEndPointInitData endPointData = null;
        try {
            Socket socket = null;
            int previousSoTimeout = -1;
            if (connectionData != null) {
                endPointData = (MessagingEndPointInitData) connectionData.getCustomData();
                this.destination = (endPointData != null) ? endPointData.getDestination() : null;
                socket = connectionSuccessful ? connectionData.getSocket() : null;
                if (socket != null) {
                    previousSoTimeout = socket.getSoTimeout();
                    socket.setSoTimeout(30000);
                }
                if (endPointData == null) {
                    logError("Invalid initialization of endpoint! Custom data didn't contiain reference to MessagingEndPointInitData!");
                    return false;
                }
                if (connectionSuccessful) {
                    this.initEndPoint();
                    try {
                        initInputStream();
                        initOutputStream();
                        initObjectOutputStream(true);
                        initObjectInputStream();
                    } catch (Exception e) {
                        logError("An error occurred while intializing endpoint. Couldn't create streams.", e);
                        connectionSuccessful = false;
                    }
                    if (connectionSuccessful) {
                        ConnectResponse response;
                        this.firstEndPointInGroup = endPointData.isFirstEndPoint();
                        if (this.firstEndPointInGroup) {
                            this.destination.setDestinationId(super.getRemoteAddress().getAddressAsLong());
                            if (super.isDebugMode()) logDebug("Sending connect request on primary endpoint.");
                            super.writeObject(new ConnectRequest(this.destination.getDestinationId(), this.messagingManager.initializeDestinationMetaData(this.destination), true));
                            super.objectWriter.flush();
                            if (super.isDebugMode()) logDebug("Waiting for connect response on primary endpoint.");
                            response = (ConnectResponse) super.readObject();
                            this.destination.setProtocolVersion(response.getProtocolVersion());
                            remoteMessagingSystemMetaData = response.getMessagingSystemMetaData();
                            if (super.isDebugMode()) logDebug("Connect response received on primary endpoint. Remote messaging system meta data: " + remoteMessagingSystemMetaData + ", protocol version: " + response.getProtocolVersion() + ".");
                            this.destination.setDestinationMetaData(remoteMessagingSystemMetaData);
                            this.messagingManager.destinationMetaDataInitialized(this.destination);
                            this.destination.setClientId(response.getClientId());
                        } else {
                            if (super.isDebugMode()) logDebug("Sending connect request on secondary endpoint.");
                            super.writeObject(new ConnectRequest(destination.getClientId(), null, false));
                            super.objectWriter.flush();
                            if (super.isDebugMode()) logDebug("Waiting for connect response on secondary endpoint.");
                            response = (ConnectResponse) super.readObject();
                            if (super.isDebugMode()) logDebug("Connect response received on secondary endpoint.");
                            if (this.destination.getProtocolVersion() >= 6) {
                                connectionSuccessful = response.isSecondaryResponseSuccess();
                                if (!connectionSuccessful) logError("Error connecting secondary endpoint!");
                            }
                        }
                    }
                }
                if ((this.destination.getProtocolVersion() >= 4) && connectionSuccessful) {
                    this.endPointInputStream.read();
                }
            } else {
                connectionSuccessful = false;
            }
            if (connectionSuccessful && (socket != null)) socket.setSoTimeout(previousSoTimeout);
        } catch (Exception e) {
            logError("Fatal error occured during client side initialization!", e);
            connectionSuccessful = false;
        } finally {
            if ((endPointData != null) && endPointData.isFirstEndPoint()) {
                messagingManager.firstClientSideEndPointEstablishedLink(this, this.destination, connectionSuccessful);
            }
        }
        return connectionSuccessful;
    }

    /**
	 * Server side initialization method. This method is responsible for performing handshaking procedures 
	 * if a connection has been successfully established. This is accomplished by receiving a {@link ConnectRequest} and 
	 * replying with a {@link ConnectResponse}. If this endpoint represents the first connection from a 
	 * certain client, the unique id used to identify this client in the remote messaging system will be determined during handshaking.
	 */
    protected boolean initServerSide() {
        HashMap remoteMessagingSystemMetaData;
        boolean connectionSuccessful = true;
        try {
            Socket socket = super.getSocket();
            int previousSoTimeout = socket.getSoTimeout();
            socket.setSoTimeout(30000);
            this.initEndPoint();
            try {
                initInputStream();
                initOutputStream();
                initObjectInputStream();
                initObjectOutputStream(true);
            } catch (Exception e) {
                logError("An error occurred while intializing endpoint. Couldn't create streams.", e);
                return false;
            }
            if (super.isDebugMode()) logDebug("Waiting for connect request.");
            ConnectRequest request = (ConnectRequest) super.readObject();
            this.firstEndPointInGroup = request.isFirstConnectRequest();
            long remoteClientId;
            if (firstEndPointInGroup) remoteClientId = super.getRemoteAddress().getAddressAsLong(); else remoteClientId = request.getClientId();
            TcpEndPointIdentifier identityAddress = TcpEndPointIdentifier.parseTcpEndPointIdentifier(remoteClientId);
            setEndPointIdentifier(identityAddress);
            byte requestedProtocolVersion = request.getProtocolVersion();
            if (this.firstEndPointInGroup) {
                this.destination = this.messagingManager.getDestination(identityAddress, true, false);
                this.destination.setProtocolVersion((requestedProtocolVersion <= MessagingManager.MESSAGING_PROTOCOL_VERSION) ? requestedProtocolVersion : MessagingManager.MESSAGING_PROTOCOL_VERSION);
                long localClientId = request.getClientId();
                remoteMessagingSystemMetaData = request.getMessagingSystemMetaData();
                if (super.isDebugMode()) logDebug("Connect request received. Remote client id : " + remoteClientId + ". Local client id : " + localClientId + ". Remote messaging system meta data: " + remoteMessagingSystemMetaData + ", protocol version: " + this.destination.getProtocolVersion() + ".");
                this.destination.setDestinationId(remoteClientId);
                this.destination.setClientId(localClientId);
                this.destination.setDestinationMetaData(remoteMessagingSystemMetaData);
                this.messagingManager.destinationMetaDataInitialized(this.destination);
                MessagingEndPoint.this.registerEndPoint();
                if (super.isDebugMode()) logDebug("Sending connect response.");
                super.writeObject(new ConnectResponse(remoteClientId, this.messagingManager.initializeDestinationMetaData(this.destination), this.destination.getProtocolVersion()));
                super.objectWriter.flush();
            } else {
                synchronized (messagingManager.getEndpointGroupsLock()) {
                    this.destination = this.messagingManager.getDestination(identityAddress, false, false);
                    if (this.destination != null) {
                        if (super.isDebugMode()) logDebug("Connect request received. Remote client id : " + remoteClientId + ".");
                        MessagingEndPoint.this.registerEndPoint();
                    } else {
                        logError("Unable to obtain destination for secondary endpoint when handling connect request from system at " + this.getEndPointIdentifier() + " received. Remote client id : " + remoteClientId + ".");
                        connectionSuccessful = false;
                    }
                }
                if (super.isDebugMode()) logDebug("Sending connect response.");
                super.writeObject(new ConnectResponse(connectionSuccessful));
                super.objectWriter.flush();
            }
            if ((this.destination != null) && (this.destination.getProtocolVersion() >= 4)) {
                this.endPointInputStream.read();
            }
            socket.setSoTimeout(previousSoTimeout);
        } catch (Exception e) {
            logError("Fatal error occured during server side initialization!", e);
            connectionSuccessful = false;
        }
        return connectionSuccessful;
    }

    /**
	 * Method to create a Socket for this MessagingEndPoint.
	 * 
	 * @return a newly created Socket object.
	 */
    protected Socket createClientSocket(final TcpEndPointIdentifier address) {
        MessagingEndPointInitData endPointData = (MessagingEndPointInitData) super.connectionData.getCustomData();
        TcpEndPointIdentifier bindAddress = messagingManager.getBindAddressForRemoteAddress(address);
        if ((endPointData != null) && endPointData.isFirstEndPoint()) {
            Destination destiation = endPointData.getDestination();
            if (destiation.isError()) {
                if (bindAddress != null) {
                    return messagingManager.createSocket(address, bindAddress, messagingManager.getConnectTimeOut(), 1, true);
                } else {
                    return messagingManager.createSocket(address, messagingManager.getConnectTimeOut(), 1, true);
                }
            }
        }
        if (bindAddress != null) {
            return messagingManager.createSocket(address, bindAddress);
        } else {
            return messagingManager.createSocket(address);
        }
    }

    /**
	 * Sends a message to the remote messaging system which this endpoint is connected to. 
	 * 
	 * @param header the header of the message to dispatch.
	 * @param messageDispatchImpl the implementation to be used for dispatching the message body.
	 * 
	 * @throws MessageDispatchFailedException if message dispatch failed.
	 */
    public synchronized void dispatchMessage(final MessageHeader header, final MessageWriter messageDispatchImpl) throws MessageDispatchFailedException {
        MessageDispatchFailedException messageDispatchFailedException = null;
        try {
            messageDispatchImpl.writeMessage(header, this, this.endPointOutputStream);
            this.endPointOutputStream.flush();
        } catch (MessageDispatchFailedException mdfe) {
            throw (MessageDispatchFailedException) mdfe.fillInStackTrace();
        } catch (IOException ioe) {
            if (super.isDebugMode()) {
                logError("Fatal communication error (" + ioe + ") while sending message (header: " + header + ")! Destroying endpoint!", ioe);
            } else {
                logError("Fatal communication error (" + ioe + ") while sending message (header: " + header + ")! Destroying endpoint!");
            }
            messageDispatchFailedException = new MessageDispatchFailedException("Fatal communication error (" + ioe + ") while sending message!", true);
            super.disconnect();
        } catch (Exception e) {
            logError("Fatal error while sending message (header: " + header + ")! Destroying endpoint!", e);
            messageDispatchFailedException = new MessageDispatchFailedException("Fatal error (" + e + ") while sending message!", true);
            super.disconnect();
        } catch (Throwable t) {
            logError("Fatal error while sending message (header: " + header + ")! Destroying endpoint!", t);
            super.disconnect();
            if (t instanceof Error) {
                throw (Error) t.fillInStackTrace();
            }
        }
        if (messageDispatchFailedException != null) throw messageDispatchFailedException;
    }

    /**
	 * Sends a message header to the remote messaging system which this endpoint is connected to. 
	 * 
	 * @since 1.3.1
	 */
    protected void dispatchHeader(final MessageHeader header) throws IOException {
        header.setSenderId(this.destination.getClientId());
        header.setProtocolVersion(this.destination.getProtocolVersion());
        if (this.destination.getProtocolVersion() >= 4) {
            this.headerStreamableSerializerStream.writeInt(0);
            header.write(this.headerStreamableSerializerStream);
            this.headerStreamableSerializerStream.flush();
            int headerLength = (int) this.headerStreamableSerializerByteStream.size();
            headerLength = headerLength - 4;
            byte[] headerBytes = this.headerStreamableSerializerByteStream.toByteArray();
            headerBytes[0] = (byte) ((headerLength >>> 24));
            headerBytes[1] = (byte) ((headerLength >>> 16));
            headerBytes[2] = (byte) ((headerLength >>> 8));
            headerBytes[3] = (byte) ((headerLength >>> 0));
            this.endPointOutputStream.write(headerBytes);
            this.resetHeaderStreamableSerializer();
        } else {
            this.serializeObject(header);
            this.objectSerializerByteStream.writeTo(this.endPointOutputStream);
        }
    }

    /**
	 * Serializes an object to the internal byte stream.
    * 
    * @throws IOException if an i/o error occurs.
	 */
    public long serializeObject(final Object obj) throws IOException {
        if ((obj instanceof Streamable) && (this.destination.getProtocolVersion() >= 4)) {
            return this.serializeStreamable((Streamable) obj);
        } else {
            this.objectSerializerObjectStream.writeObject(obj);
            this.objectSerializerObjectStream.flush();
            long objectSize = this.objectSerializerByteStream.size();
            if (this.objectSerializerByteStream.hasSpilledOver() && super.isDebugMode()) {
                super.logDebug("Large object message body (" + objectSize + ") - serialization buffer spilled over to file: " + this.objectSerializerByteStream.getSpillOverFile() + ".");
            }
            return objectSize;
        }
    }

    /**
    * Serializes a streamable to the internal byte stream.
    * 
    * @throws IOException if an i/o error occurs.
    */
    public long serializeStreamable(final Streamable streamable) throws IOException {
        streamable.write(this.streamableSerializerStream);
        this.streamableSerializerStream.flush();
        return this.streamableSerializerByteStream.size();
    }

    /**
	 * Resets the serializer (byte array output stream and object output stream) used to serialize request objects.
    * 
    * @throws IOException if an i/o error occurs.
	 */
    public final void resetObjectSerializer(final boolean resetObjectSerializerByteStream, final boolean resetObjectSerializerObjectStream) throws IOException {
        if (resetObjectSerializerByteStream && (this.objectSerializerByteStream != null)) {
            if (this.objectSerializerByteStream.size() > (2 * BYTE_BUFFER_INITIAL_SIZE)) {
                this.objectSerializerByteStream.reset();
                this.objectSerializerByteStream = new SpillOverByteArrayOutputStream(BYTE_BUFFER_INITIAL_SIZE, BYTE_BUFFER_SPILL_OVER_LIMIT);
                this.objectSerializerObjectStream = null;
            } else {
                this.objectSerializerByteStream.reset();
            }
        }
        if (resetObjectSerializerObjectStream || (this.objectSerializerObjectStream == null)) {
            if (super.isUsingAlternativeResetMethod()) {
                this.objectSerializerObjectStream = null;
            }
            if (this.objectSerializerObjectStream == null) {
                this.objectSerializerObjectStream = new NoHeadersObjectOutputStream(this.objectSerializerByteStream);
            }
            this.objectSerializerObjectStream.reset();
        }
    }

    /**
    * Resets the streamable serializer.
    * 
    * @throws IOException if an i/o error occurs.
    */
    public final void resetStreamableSerializer() throws IOException {
        if (this.streamableSerializerByteStream != null) {
            if (this.streamableSerializerByteStream.size() > (2 * BYTE_BUFFER_INITIAL_SIZE)) {
                this.streamableSerializerByteStream.reset();
                this.streamableSerializerByteStream = new SpillOverByteArrayOutputStream(BYTE_BUFFER_INITIAL_SIZE, BYTE_BUFFER_SPILL_OVER_LIMIT);
                this.streamableSerializerStream = null;
            } else {
                this.streamableSerializerByteStream.reset();
            }
        }
        if (this.streamableSerializerStream == null) {
            this.streamableSerializerStream = new EndPointOutputStreamer(this.streamableSerializerByteStream);
        } else {
            this.streamableSerializerStream.setContextObjectOutputStream(null);
        }
    }

    /**
    * Resets the header streamable serializer.
    * 
    * @throws IOException if an i/o error occurs.
    */
    public final void resetHeaderStreamableSerializer() throws IOException {
        if (this.headerStreamableSerializerByteStream != null) {
            if (this.headerStreamableSerializerByteStream.size() > (2 * BYTE_BUFFER_INITIAL_SIZE)) {
                this.headerStreamableSerializerByteStream = new ByteArrayOutputStream(BYTE_BUFFER_INITIAL_SIZE);
                this.headerStreamableSerializerStream = null;
            } else {
                this.headerStreamableSerializerByteStream.reset();
            }
        }
        if (this.headerStreamableSerializerStream == null) {
            this.headerStreamableSerializerStream = new EndPointOutputStreamer(this.headerStreamableSerializerByteStream);
        } else {
            this.headerStreamableSerializerStream.setContextObjectOutputStream(null);
        }
    }

    /**
	 * Writes a streamed body to the output stream of this endpoint.
	 * 
	 * @param header the message header containing the body data length, i.e. the number of bytes to read from the input stream.
	 * @param messageBodyInputStream the stream to read body data from.
	 * 
	 * @throws IOException if an i/o error occurs.
	 */
    public void writeStreamBody(final MessageHeader header, final InputStream messageBodyInputStream) throws IOException {
        final long dataLength = header.getBodyLength();
        BufferedInputStream bufferedMessageBodyInputStream;
        if (messageBodyInputStream instanceof BufferedInputStream) {
            bufferedMessageBodyInputStream = (BufferedInputStream) messageBodyInputStream;
        } else {
            bufferedMessageBodyInputStream = new BufferedInputStream(messageBodyInputStream);
        }
        if (dataLength > 0) {
            int readBytes = 0;
            long dataLeftToWrite = dataLength;
            int dataTransferCount;
            while (dataLeftToWrite > 0) {
                dataTransferCount = (dataLeftToWrite >= streamBufferSize) ? streamBufferSize : (int) dataLeftToWrite;
                readBytes = bufferedMessageBodyInputStream.read(streamBuffer, 0, dataTransferCount);
                if (readBytes < 0) {
                    throw new IOException("Error occurred while reading data from input stream! Got unexpected end of file! Data left to write: " + dataLeftToWrite + ", data transfer count: " + dataTransferCount + ".");
                }
                this.endPointOutputStream.write(streamBuffer, 0, readBytes);
                dataLeftToWrite -= readBytes;
            }
        }
    }

    /**
    * Method for receiving responses.
    */
    private final void receiveMessages() {
        MessageHeader header = null;
        Message message = null;
        while (isConnected()) {
            try {
                if (this.destination.getProtocolVersion() >= 4) {
                    int headerLength = this.endPointInputStream.readInt();
                    this.endPointInputStream.setStream(this.readAsByteArrayInputStream(headerLength));
                    header = new MessageHeader();
                    header.setProtocolVersion(this.destination.getProtocolVersion());
                    header.read(this.endPointInputStream);
                    this.endPointInputStream.setStream(null);
                    this.endPointInputStream.setContextObjectInputStream(null);
                } else {
                    header = (MessageHeader) super.readObject();
                }
                if ((header != null) && isConnected()) {
                    if (header.getHeaderType() == MessageHeader.META_DATA_UPDATE_HEADER) {
                        HashMap oldDestinationMetaData = this.destination.getDestinationMetaData();
                        this.destination.setDestinationMetaData(header.getMessagingSystemMetaData());
                        this.messagingManager.destinationMetaDataUpdated(this.destination, oldDestinationMetaData);
                    } else if (header.getHeaderType() == MessageHeader.DISCONNECT_HEADER) {
                        this.disconnectHeaderReceived = true;
                        if (super.isDebugMode()) logDebug("Received disconnect header.");
                        Thread.yield();
                        super.disconnect();
                    } else if (header.getHeaderType() != MessageHeader.ENDPOINT_CHECK_HEADER) {
                        if (header.getMessagingSystemMetaData() != null) {
                            HashMap oldDestinationMetaData = this.destination.getDestinationMetaData();
                            this.destination.updateDestinationMetaData(header.getMessagingSystemMetaData());
                            this.messagingManager.destinationMetaDataUpdated(this.destination, oldDestinationMetaData);
                        }
                        if ((header.getBodyLength() > 0) && (header.getBodyLength() < BODY_READ_BUFFER_LIMIT)) {
                            this.endPointInputStream.setStream(this.readAsByteArrayInputStream((int) header.getBodyLength()));
                        }
                        message = this.messagingManager.createMessage(header, this);
                        if (super.isDebugMode()) logDebug("Received message with header " + header.toString() + ".");
                        this.endPointInputStream.setCurrentMessage(message);
                        this.messagingManager.messageReceived(message);
                        this.waitForMessageReadCompletion(message);
                        this.endPointInputStream.setStream(null);
                        this.endPointInputStream.setContextObjectInputStream(null);
                    }
                }
            } catch (InvalidClassException ice) {
                if (this.isDebugMode()) {
                    logWarning("Error while receiving command (header: " + ((header != null) ? header.toString() : "<error>") + "): " + ice + ".", ice);
                } else {
                    logWarning("Error while receiving command (header: " + ((header != null) ? header.toString() : "<error>") + "): " + ice + ".");
                }
            } catch (InvalidObjectException ivoe) {
                if (this.isDebugMode()) {
                    logWarning("Error while receiving command (header: " + ((header != null) ? header.toString() : "<error>") + "): " + ivoe + ".", ivoe);
                } else {
                    logWarning("Error while receiving command (header: " + ((header != null) ? header.toString() : "<error>") + "): " + ivoe + ".");
                }
            } catch (ClassNotFoundException cnfe) {
                if (this.isDebugMode()) {
                    logWarning("Error while receiving command (header: " + ((header != null) ? header.toString() : "<error>") + "): " + cnfe + ".", cnfe);
                } else {
                    logWarning("Error while receiving command (header: " + ((header != null) ? header.toString() : "<error>") + "): " + cnfe + ".");
                }
            } catch (InterruptedException ie) {
                if (isConnected()) {
                    logWarning("Interrupted during blocking operation! Destroying endpoint!", ie);
                }
            } catch (IOException ioe) {
                if (isConnected()) {
                    if (this.isDebugMode()) {
                        logError("Fatal communication error (" + ioe + ") while receiving message! Destroying endpoint!", ioe);
                    } else {
                        logError("Fatal communication error (" + ioe + ") while receiving message! Destroying endpoint!");
                    }
                }
                return;
            } catch (Exception e) {
                if (isConnected()) {
                    logError("Fatal error while receiving message! Destroying endpoint!", e);
                }
                return;
            } finally {
                header = null;
                message = null;
                this.endPointInputStream.resetCurrentMessage();
            }
        }
    }

    /**
    * Waits for message read completion.
    */
    private void waitForMessageReadCompletion(final Message message) throws InterruptedException, IOException {
        if (message.getHeader().getBodyLength() > 0) {
            long lastReadProgress = this.endPointInputStream.getReadProgress();
            final long messageReadTimeout = this.messagingManager.getMessageReadTimeout();
            while (!message.waitForReadCompletion(messageReadTimeout)) {
                if (lastReadProgress == this.endPointInputStream.getReadProgress()) {
                    String messageHandlerThreadStack = "";
                    Thread messageHandlerThread = message.getMessageHandlerThread();
                    if (messageHandlerThread != null) {
                        messageHandlerThreadStack = " Message handler thread stack: " + StringUtils.toString(messageHandlerThread.getStackTrace());
                    }
                    logError("Inputstream read timeout! Bytes read: " + this.endPointInputStream.getReadCount() + ", total bytes to read: " + this.endPointInputStream.getMaxReadCount() + ". Header: " + message.getHeader() + "." + messageHandlerThreadStack);
                    break;
                }
                lastReadProgress = this.endPointInputStream.getReadProgress();
            }
        }
        this.endPointInputStream.skipAll();
    }

    /**
    * Creates the actual mode implementation, handling the setup and execution of the client or server side 
    * endpoint logic.
    * 
    * @since 2.0.1 (20041126)
    */
    protected TcpEndPointModeImpl createTcpEndPointModeImpl(final boolean serverSide) {
        if (serverSide) return new MessagingEndPointServerSideModeImpl(); else return super.createTcpEndPointModeImpl(serverSide);
    }

    /**
	 * Method implemeting client side behaviour for this MessagingEndPoint.
	 */
    protected void runClientSideImpl() {
        if (isConnected() && isLinkEstablished()) {
            receiveMessages();
        }
    }

    /**
	 * Method implemeting client side behaviour for this MessagingEndPoint.
	 */
    protected void runServerSideImpl() {
        runClientSideImpl();
    }

    /**
	 * Checks this MessagingEndPoint for errors.
	 * 
	 * @return <code>true</code> if there were no errors, otherwise <code>false</code>.
	 */
    public boolean check() {
        if (super.check()) {
            synchronized (this) {
                if (isLinkEstablished()) {
                    try {
                        if (this.destination.getProtocolVersion() >= 4) {
                            this.dispatchHeader(new EndPointCheckCommand());
                        } else {
                            this.dispatchHeader(new EndPointCheckCommand());
                            this.resetObjectSerializer(true, true);
                        }
                        return true;
                    } catch (Exception e) {
                        if (super.isDebugMode()) log(Level.DEBUG, "Exception occurred while sending StatusCommand during check.", e);
                        return false;
                    }
                } else return true;
            }
        }
        return false;
    }

    /**
	 * Clean up method for this MessagingEndPoint.
	 */
    protected void cleanUp() {
        super.cleanUp();
        this.destination = null;
        this.disconnectHeaderReceived = false;
        this.firstEndPointInGroup = false;
        ;
    }

    /**
	 * Destroys this MessagingEndPoint. This method is called when the MessagingEndPoint can no longer be reused and is to be killed. 
	 */
    protected void destroy() {
        super.destroy();
        this.destination = null;
        this.disconnectHeaderReceived = false;
        this.firstEndPointInGroup = false;
        ;
    }

    /**
    * Disconnects this MessagingEndPoint by dispatching a disconnection header and closing the connection.
    */
    public synchronized void disconnect() {
        try {
            if (super.isConnected()) {
                super.setConnected(false);
                MessageHeader disconnectHeader = new MessageHeader();
                disconnectHeader.setHeaderType(MessageHeader.DISCONNECT_HEADER);
                this.dispatchHeader(disconnectHeader);
                Thread.yield();
            }
        } catch (Exception e) {
            logError("Error while dispatching disconnect message!", e);
        }
        super.disconnect();
    }

    /**
    * Reads form the input stream of the endpoint into a ByteArrayInputStream.
    */
    private ByteArrayInputStream readAsByteArrayInputStream(final int length) throws IOException {
        byte[] bodyBytes = new byte[length];
        for (int read = 0; read < length; ) {
            read += this.endPointInputStream.read(bodyBytes, read, (length - read));
        }
        return new ByteArrayInputStream(bodyBytes);
    }

    /**
    * This method is not allowed in this implementation, and thus, an IOException will be thrown if invoked. 
    */
    public String readLine() throws IOException {
        throw new IOException("Operation not allowed!");
    }

    /**
    * This method is not allowed in this implementation, and thus, an IOException will be thrown if invoked. 
    */
    public void writeLine(String str) throws IOException {
        throw new IOException("Operation not allowed!");
    }

    /**
    * Reads an Object from the InputStream of the Socket, using the specified ClassLoader to resolve 
    * the class of the read object.
    * 
    * @param classLoader the ClassLoader that is to be used to resolve the class of the read object.
    * 
    * @return an Object read from the stream.
    * 
    * @exception IOException if there was an error reading from the socket.
    * @exception ClassNotFoundException if no class was found for the object that was read from the socket.
    * 
    * @since 1.3
    */
    public final Object readObject(ClassLoader classLoader) throws IOException, ClassNotFoundException {
        Object object;
        synchronized (super.readerLock) {
            this.classLoaderObjectInputStream.setClassLoader(classLoader);
            object = this.classLoaderObjectInputStream.readObject();
            this.classLoaderObjectInputStream.setClassLoader(null);
        }
        return object;
    }

    /**
    * This method is not allowed in this implementation, and thus, an IOException will be thrown if invoked. 
    */
    public void writeObject(Object obj) throws IOException {
        throw new IOException("Operation not allowed!");
    }

    /**
    * Called to indicate that an error occurred while reading body data.
    * 
    * @param t the error that occurred.
    * 
    * @since 2.1.2 (20060308)
    */
    protected void bodyReadErrorOccurred(final Throwable t) throws IOException {
        if (super.objectReader != null) {
            super.logInfo("Body read error occurred - reinitializing object reader.");
            super.objectReader = null;
            this.initObjectInputStream(false);
        }
    }

    /**
    * Custom server side mode implementation that lets the method {@link #initServerSide()} take care of endpoint registration.
    */
    public final class MessagingEndPointServerSideModeImpl extends TcpEndPointServerSideModeImpl {

        /**
       * Server side mode implementation worker method. This method handles setup and 
       * execution of the endpoint logic.
       */
        public void work() {
            if (isDebugMode()) logDebug("Initializing connection from " + this.getEndPointIdentifier() + "...");
            setConnected(true);
            MessagingEndPoint.this.notifyEndPointConnected();
            if (initServerSide()) {
                setLinkEstablished(true);
                MessagingEndPoint.this.notifyEndPointLinkEstablished();
                if (isDebugMode()) logDebug("Link established.");
                runServerSideImpl();
            } else {
                if (isDebugMode()) logDebug("Failed to establish link.");
                setLinkEstablished(false);
            }
        }
    }

    private static class EndPointOutputStreamer extends DataOutputStream implements OutputStreamer {

        private ObjectOutputStream currentContextObjectOutputStream = null;

        public EndPointOutputStreamer(final OutputStream outputStream) {
            super(outputStream);
        }

        public ObjectOutputStream getContextObjectOutputStream() throws IOException {
            if (this.currentContextObjectOutputStream == null) this.currentContextObjectOutputStream = new NoHeadersObjectOutputStream(super.out);
            return this.currentContextObjectOutputStream;
        }

        public void setContextObjectOutputStream(ObjectOutputStream contextObjectOutputStream) throws IOException {
            this.currentContextObjectOutputStream = contextObjectOutputStream;
        }
    }
}
