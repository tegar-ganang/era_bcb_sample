package visitpc.messages;

import java.io.*;
import visitpc.destclient.*;
import java.util.*;
import javax.net.ssl.SSLSocket;

/**
 * This class is responsible for moving messages around using Buffered
 * input/output streams. I tried using an object stream but found it to be
 * inefficient so this class was written to encapsulate the functionality
 * required to move objects over standard input/output streams.
 * 
 * This combines the efficiency of a message based protocol with ease of use of
 * a object server architecture.
 */
public class ObjectMover {

    private static final int VNC_DEST_CLIENT_ID = 1;

    private static final int FILE_TRANSFER_CLIENT_ID = 2;

    private static final int GENERIC_CLIENT_ID = 3;

    private static final int UNDEFINED_CLIENT_ID = 4;

    public static final byte ConnectedMessageID = 1;

    public static final byte ConnectSrcClientToDestClientID = 2;

    public static final byte ConnectFromDestClientID = 3;

    public static final byte DestClientErrorMessageID = 4;

    public static final byte DestClientServerGreetingID = 5;

    public static final byte DestClientServerGreetingResponseID = 6;

    public static final byte DisconnectedMessageID = 7;

    public static final byte DisconnectSrcClientFromDestClientID = 8;

    public static final byte ErrorMessageID = 9;

    public static final byte KeepAliveMessageID = 10;

    public static final byte DataMessageID = 11;

    public static final byte SrcClientServerGreetingID = 12;

    public static final byte ConnectedSrcClientToDestClientID = 14;

    public static final byte ConnectedFromDestClientID = 15;

    public static final byte DisconnectFromDestClientID = 16;

    public static final byte SrcClientServerGreetingResponseV2ID = 17;

    public static final byte SimpleCommandObjectID = 18;

    public static final byte ConnectSrcClientToDestClientV2ID = 19;

    public static final String[] MESSAGE_NAMES = { "ConnectedMessage", "ConnectSrcClientToDestClient", "ConnectFromDestClient", "DestClientErrorMessage", "DestClientServerGreeting", "DestClientServerGreetingResponse", "DisconnectedMessage", "DisconnectSrcClientFromDestClient", "ErrorMessage", "KeepAliveMessage", "DataMessage", "SrcClientServerGreeting", "SrcClientServerGreetingResponse", "ConnectedSrcClientToDestClient", "ConnectedFromDestClient", "DisconnectFromDestClient", "SrcClientServerGreetingResponseV2ID", "SimpleCommandObjectID", "ConnectSrcClientToDestClientV2ID" };

    public synchronized void sendObject(DataOutputStream dos, Object object) throws IOException {
        if (object instanceof ConnectedMessage) {
            sendConnectedMessage(dos, (ConnectedMessage) object);
        } else if (object instanceof ConnectSrcClientToDestClient) {
            sendConnectSrcClientToDestClient(dos, (ConnectSrcClientToDestClient) object);
        } else if (object instanceof ConnectFromDestClient) {
            sendConnectFromDestClient(dos, (ConnectFromDestClient) object);
        } else if (object instanceof DestClientErrorMessage) {
            sendDestClientErrorMessage(dos, (DestClientErrorMessage) object);
        } else if (object instanceof DestClientServerGreeting) {
            sendDestClientServerGreeting(dos, (DestClientServerGreeting) object);
        } else if (object instanceof DestClientServerGreetingResponse) {
            sendDestClientServerGreetingResponse(dos, (DestClientServerGreetingResponse) object);
        } else if (object instanceof DisconnectedMessage) {
            sendDisconnectedMessage(dos, (DisconnectedMessage) object);
        } else if (object instanceof DisconnectSrcClientFromDestClient) {
            sendDisconnectSrcClientFromDestClient(dos, (DisconnectSrcClientFromDestClient) object);
        } else if (object instanceof ErrorMessage) {
            sendErrorMessage(dos, (ErrorMessage) object);
        } else if (object instanceof KeepAliveMessage) {
            sendKeepAliveMessage(dos);
        } else if (object instanceof DataMessage) {
            sendRXDataMessage(dos, (DataMessage) object);
        } else if (object instanceof SrcClientServerGreeting) {
            sendSrcClientServerGreeting(dos, (SrcClientServerGreeting) object);
        } else if (object instanceof ConnectedSrcClientToDestClient) {
            sendConnectedSrcClientToDestClient(dos);
        } else if (object instanceof ConnectedFromDestClient) {
            sendConnectedFromDestClient(dos, (ConnectedFromDestClient) object);
        } else if (object instanceof DisconnectFromDestClient) {
            sendDisconnectFromDestClient(dos, (DisconnectFromDestClient) object);
        } else if (object instanceof SrcClientServerGreetingResponseV2) {
            sendSrcClientServerGreetingResponseV2(dos, (SrcClientServerGreetingResponseV2) object);
        } else if (object instanceof SimpleCommandObject) {
            sendIntCommandObject(dos, (SimpleCommandObject) object);
        } else if (object instanceof ConnectSrcClientToDestClientV2) {
            sendConnectSrcClientToDestClientV2(dos, (ConnectSrcClientToDestClientV2) object);
        } else {
            throw new IOException("SendObject: Request to send unknown object type (" + object + ")");
        }
        dos.flush();
    }

    public Object getObject(SSLSocket sslSocket, DataInputStream dis, int timeoutMS) throws IOException {
        try {
            sslSocket.setSoTimeout(timeoutMS);
            return getObject(dis);
        } finally {
            sslSocket.setSoTimeout(0);
        }
    }

    public Object getObject(DataInputStream dis) throws IOException {
        Object object = null;
        int messageType;
        messageType = dis.readByte();
        switch(messageType) {
            case ObjectMover.ConnectedMessageID:
                object = getConnectedMessage(dis);
                break;
            case ObjectMover.ConnectSrcClientToDestClientID:
                object = getConnectSrcClientToDestClient(dis);
                break;
            case ObjectMover.ConnectFromDestClientID:
                object = getConnectFromDestClient(dis);
                break;
            case ObjectMover.DestClientErrorMessageID:
                object = getDestClientErrorMessage(dis);
                break;
            case ObjectMover.DestClientServerGreetingID:
                object = getDestClientServerGreeting(dis);
                break;
            case ObjectMover.DestClientServerGreetingResponseID:
                object = getDestClientServerGreetingResponse(dis);
                break;
            case ObjectMover.DisconnectedMessageID:
                object = getDisconnectedMessage(dis);
                break;
            case ObjectMover.DisconnectSrcClientFromDestClientID:
                object = getDisconnectSrcClientFromDestClient(dis);
                break;
            case ObjectMover.ErrorMessageID:
                object = getErrorMessage(dis);
                break;
            case ObjectMover.KeepAliveMessageID:
                object = getKeepAliveMessage();
                break;
            case ObjectMover.DataMessageID:
                object = getRXDataMessage(dis);
                break;
            case ObjectMover.SrcClientServerGreetingID:
                object = getSrcClientServerGreeting(dis);
                break;
            case ObjectMover.ConnectedSrcClientToDestClientID:
                object = getConnectedSrcClientToDestClient();
                break;
            case ObjectMover.ConnectedFromDestClientID:
                object = getConnectedFromDestClient(dis);
                break;
            case ObjectMover.DisconnectFromDestClientID:
                object = getDisconnectFromDestClient(dis);
                break;
            case ObjectMover.SrcClientServerGreetingResponseV2ID:
                object = getSrcClientServerGreetingResponseV2(dis);
                break;
            case ObjectMover.SimpleCommandObjectID:
                object = getSimpleCommandObject(dis);
                break;
            case ObjectMover.ConnectSrcClientToDestClientV2ID:
                object = getConnectSrcClientToDestClientV2(dis);
                break;
            default:
                throw new IOException("Recieved invalid message type = " + messageType);
        }
        return object;
    }

    private void sendConnectedMessage(DataOutputStream dos, ConnectedMessage connectedMessage) throws IOException {
        dos.writeByte(ConnectedMessageID);
        dos.writeInt(connectedMessage.message.length());
        dos.write(connectedMessage.message.getBytes());
    }

    private ConnectedMessage getConnectedMessage(DataInputStream dis) throws IOException {
        int messageLength = dis.readInt();
        byte buffer[] = new byte[messageLength];
        dis.readFully(buffer);
        ConnectedMessage connectedMessage = new ConnectedMessage();
        connectedMessage.message = new String(buffer);
        return connectedMessage;
    }

    private void sendConnectSrcClientToDestClient(DataOutputStream dos, ConnectSrcClientToDestClient connectSrcClientToDestClient) throws IOException {
        dos.writeByte(ConnectSrcClientToDestClientID);
        dos.writeInt(connectSrcClientToDestClient.username.length());
        dos.write(connectSrcClientToDestClient.username.getBytes());
        dos.writeInt(connectSrcClientToDestClient.password.length());
        dos.write(connectSrcClientToDestClient.password.getBytes());
        dos.writeInt(connectSrcClientToDestClient.pcName.length());
        dos.write(connectSrcClientToDestClient.pcName.getBytes());
    }

    private ConnectSrcClientToDestClient getConnectSrcClientToDestClient(DataInputStream dis) throws IOException {
        byte buffer[];
        ConnectSrcClientToDestClient connectSrcClientToDestClient = new ConnectSrcClientToDestClient();
        int usernameLength = dis.readInt();
        buffer = new byte[usernameLength];
        dis.readFully(buffer);
        connectSrcClientToDestClient.username = new String(buffer);
        int passwordLength = dis.readInt();
        buffer = new byte[passwordLength];
        dis.readFully(buffer);
        connectSrcClientToDestClient.password = new String(buffer);
        int pcNameLength = dis.readInt();
        buffer = new byte[pcNameLength];
        dis.readFully(buffer);
        connectSrcClientToDestClient.pcName = new String(buffer);
        return connectSrcClientToDestClient;
    }

    private void sendConnectSrcClientToDestClientV2(DataOutputStream dos, ConnectSrcClientToDestClientV2 connectSrcClientToDestClientV2) throws IOException {
        dos.writeByte(ConnectSrcClientToDestClientV2ID);
        dos.writeInt(connectSrcClientToDestClientV2.username.length());
        dos.write(connectSrcClientToDestClientV2.username.getBytes());
        dos.writeInt(connectSrcClientToDestClientV2.password.length());
        dos.write(connectSrcClientToDestClientV2.password.getBytes());
        dos.writeInt(connectSrcClientToDestClientV2.pcName.length());
        dos.write(connectSrcClientToDestClientV2.pcName.getBytes());
        if (connectSrcClientToDestClientV2.destClientType == DestClientConfig.DEST_CLIENT_TYPES.VNC) {
            dos.writeInt(ObjectMover.VNC_DEST_CLIENT_ID);
        } else if (connectSrcClientToDestClientV2.destClientType == DestClientConfig.DEST_CLIENT_TYPES.FILE_TRANSFER) {
            dos.writeInt(ObjectMover.FILE_TRANSFER_CLIENT_ID);
        } else if (connectSrcClientToDestClientV2.destClientType == DestClientConfig.DEST_CLIENT_TYPES.GENERIC) {
            dos.writeInt(ObjectMover.GENERIC_CLIENT_ID);
        } else {
            dos.writeInt(ObjectMover.UNDEFINED_CLIENT_ID);
        }
    }

    private ConnectSrcClientToDestClientV2 getConnectSrcClientToDestClientV2(DataInputStream dis) throws IOException {
        byte buffer[];
        ConnectSrcClientToDestClientV2 connectSrcClientToDestClientV2 = new ConnectSrcClientToDestClientV2();
        int usernameLength = dis.readInt();
        buffer = new byte[usernameLength];
        dis.readFully(buffer);
        connectSrcClientToDestClientV2.username = new String(buffer);
        int passwordLength = dis.readInt();
        buffer = new byte[passwordLength];
        dis.readFully(buffer);
        connectSrcClientToDestClientV2.password = new String(buffer);
        int pcNameLength = dis.readInt();
        buffer = new byte[pcNameLength];
        dis.readFully(buffer);
        connectSrcClientToDestClientV2.pcName = new String(buffer);
        int clientType = dis.readInt();
        switch(clientType) {
            case ObjectMover.VNC_DEST_CLIENT_ID:
                connectSrcClientToDestClientV2.destClientType = DestClientConfig.DEST_CLIENT_TYPES.VNC;
                break;
            case ObjectMover.FILE_TRANSFER_CLIENT_ID:
                connectSrcClientToDestClientV2.destClientType = DestClientConfig.DEST_CLIENT_TYPES.FILE_TRANSFER;
                break;
            case ObjectMover.GENERIC_CLIENT_ID:
                connectSrcClientToDestClientV2.destClientType = DestClientConfig.DEST_CLIENT_TYPES.GENERIC;
                break;
            default:
                connectSrcClientToDestClientV2.destClientType = DestClientConfig.DEST_CLIENT_TYPES.UNDEFINED;
                break;
        }
        return connectSrcClientToDestClientV2;
    }

    private void sendConnectFromDestClient(DataOutputStream dos, ConnectFromDestClient connectFromDestClient) throws IOException {
        dos.writeByte(ConnectFromDestClientID);
        dos.writeInt(connectFromDestClient.sessionID);
        dos.writeInt(connectFromDestClient.destPort);
        dos.writeBoolean(connectFromDestClient.useCRC32);
        dos.writeInt(connectFromDestClient.destAddress.length());
        if (connectFromDestClient.destAddress.length() > 0) {
            dos.write(connectFromDestClient.destAddress.getBytes());
        }
    }

    private ConnectFromDestClient getConnectFromDestClient(DataInputStream dis) throws IOException {
        ConnectFromDestClient connectFromDestClient = new ConnectFromDestClient();
        connectFromDestClient.sessionID = dis.readInt();
        connectFromDestClient.destPort = dis.readInt();
        connectFromDestClient.useCRC32 = dis.readBoolean();
        int destAddressStringLength = dis.readInt();
        if (destAddressStringLength > 0) {
            byte buffer[] = new byte[destAddressStringLength];
            dis.readFully(buffer);
            connectFromDestClient.destAddress = new String(buffer);
        }
        return connectFromDestClient;
    }

    private void sendDestClientErrorMessage(DataOutputStream dos, DestClientErrorMessage destClientErrorMessage) throws IOException {
        dos.writeByte(DestClientErrorMessageID);
        dos.writeInt(destClientErrorMessage.message.length());
        dos.write(destClientErrorMessage.message.getBytes());
    }

    private DestClientErrorMessage getDestClientErrorMessage(DataInputStream dis) throws IOException {
        int messageLength = dis.readInt();
        byte buffer[] = new byte[messageLength];
        dis.readFully(buffer);
        DestClientErrorMessage destClientErrorMessage = new DestClientErrorMessage();
        destClientErrorMessage.message = new String(buffer);
        return destClientErrorMessage;
    }

    private void sendDestClientServerGreeting(DataOutputStream dos, DestClientServerGreeting destClientServerGreeting) throws IOException {
        dos.writeByte(DestClientServerGreetingID);
        dos.writeInt(destClientServerGreeting.username.length());
        dos.write(destClientServerGreeting.username.getBytes());
        dos.writeInt(destClientServerGreeting.password.length());
        dos.write(destClientServerGreeting.password.getBytes());
        dos.writeInt(destClientServerGreeting.pcName.length());
        dos.write(destClientServerGreeting.pcName.getBytes());
        if (destClientServerGreeting.destClientType.equals(DestClientConfig.DEST_CLIENT_TYPES.GENERIC)) {
            dos.writeByte(1);
        } else if (destClientServerGreeting.destClientType.equals(DestClientConfig.DEST_CLIENT_TYPES.VNC)) {
            dos.writeByte(2);
        } else if (destClientServerGreeting.destClientType.equals(DestClientConfig.DEST_CLIENT_TYPES.FILE_TRANSFER)) {
            dos.writeByte(3);
        } else {
            throw new IOException("SendDestClientServerGreeting: Unknown destClientServerGreeting.destClientType (" + destClientServerGreeting.destClientType + ")");
        }
    }

    private DestClientServerGreeting getDestClientServerGreeting(DataInputStream dis) throws IOException {
        byte buffer[];
        DestClientServerGreeting destClientServerGreeting = new DestClientServerGreeting();
        int usernameLength = dis.readInt();
        buffer = new byte[usernameLength];
        dis.readFully(buffer);
        destClientServerGreeting.username = new String(buffer);
        int passwordLength = dis.readInt();
        buffer = new byte[passwordLength];
        dis.readFully(buffer);
        destClientServerGreeting.password = new String(buffer);
        int pcNameLength = dis.readInt();
        buffer = new byte[pcNameLength];
        dis.readFully(buffer);
        destClientServerGreeting.pcName = new String(buffer);
        int destClientType = dis.readByte();
        if (destClientType == 1) {
            destClientServerGreeting.destClientType = DestClientConfig.DEST_CLIENT_TYPES.GENERIC;
        } else if (destClientType == 2) {
            destClientServerGreeting.destClientType = DestClientConfig.DEST_CLIENT_TYPES.VNC;
        } else if (destClientType == 3) {
            destClientServerGreeting.destClientType = DestClientConfig.DEST_CLIENT_TYPES.FILE_TRANSFER;
        } else {
            throw new IOException("DestClientServerGreeting: Unknown destClientServerGreeting.destClientType (" + destClientServerGreeting.destClientType + ")");
        }
        return destClientServerGreeting;
    }

    private void sendDestClientServerGreetingResponse(DataOutputStream dos, DestClientServerGreetingResponse destClientServerGreetingResponse) throws IOException {
        dos.writeByte(DestClientServerGreetingResponseID);
        dos.writeBoolean(destClientServerGreetingResponse.pcNameAlreadyUsed);
    }

    private DestClientServerGreetingResponse getDestClientServerGreetingResponse(DataInputStream dis) throws IOException {
        boolean pcNameAlreadyUsed = dis.readBoolean();
        DestClientServerGreetingResponse destClientServerGreetingResponse = new DestClientServerGreetingResponse();
        destClientServerGreetingResponse.pcNameAlreadyUsed = pcNameAlreadyUsed;
        return destClientServerGreetingResponse;
    }

    private void sendDisconnectedMessage(DataOutputStream dos, DisconnectedMessage disconnectedMessage) throws IOException {
        dos.writeByte(DisconnectedMessageID);
        dos.writeInt(disconnectedMessage.message.length());
        dos.write(disconnectedMessage.message.getBytes());
    }

    private DisconnectedMessage getDisconnectedMessage(DataInputStream dis) throws IOException {
        int messageLength = dis.readInt();
        byte buffer[] = new byte[messageLength];
        dis.readFully(buffer);
        DisconnectedMessage disconnectedMessage = new DisconnectedMessage();
        disconnectedMessage.message = new String(buffer);
        return disconnectedMessage;
    }

    private void sendDisconnectSrcClientFromDestClient(DataOutputStream dos, DisconnectSrcClientFromDestClient disconnectSrcClientFromDestClient) throws IOException {
        dos.writeByte(DisconnectSrcClientFromDestClientID);
        dos.writeInt(disconnectSrcClientFromDestClient.username.length());
        dos.write(disconnectSrcClientFromDestClient.username.getBytes());
        dos.writeInt(disconnectSrcClientFromDestClient.password.length());
        dos.write(disconnectSrcClientFromDestClient.password.getBytes());
        dos.writeInt(disconnectSrcClientFromDestClient.pcName.length());
        dos.write(disconnectSrcClientFromDestClient.pcName.getBytes());
    }

    private DisconnectSrcClientFromDestClient getDisconnectSrcClientFromDestClient(DataInputStream dis) throws IOException {
        byte buffer[];
        DisconnectSrcClientFromDestClient disconnectSrcClientFromDestClient = new DisconnectSrcClientFromDestClient();
        int usernameLength = dis.readInt();
        buffer = new byte[usernameLength];
        dis.readFully(buffer);
        disconnectSrcClientFromDestClient.username = new String(buffer);
        int passwordLength = dis.readInt();
        buffer = new byte[passwordLength];
        dis.readFully(buffer);
        disconnectSrcClientFromDestClient.password = new String(buffer);
        int pcNameLength = dis.readInt();
        buffer = new byte[pcNameLength];
        dis.readFully(buffer);
        disconnectSrcClientFromDestClient.pcName = new String(buffer);
        return disconnectSrcClientFromDestClient;
    }

    private void sendErrorMessage(DataOutputStream dos, ErrorMessage errorMessage) throws IOException {
        dos.writeByte(ErrorMessageID);
        dos.writeInt(errorMessage.message.length());
        dos.write(errorMessage.message.getBytes());
    }

    private ErrorMessage getErrorMessage(DataInputStream dis) throws IOException {
        int messageLength = dis.readInt();
        byte buffer[] = new byte[messageLength];
        dis.readFully(buffer);
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.message = new String(buffer);
        return errorMessage;
    }

    private void sendKeepAliveMessage(DataOutputStream dos) throws IOException {
        dos.writeByte(KeepAliveMessageID);
    }

    private KeepAliveMessage getKeepAliveMessage() {
        return new KeepAliveMessage();
    }

    private void sendRXDataMessage(DataOutputStream dos, DataMessage dataMessage) throws IOException {
        dos.writeByte(DataMessageID);
        dos.writeInt(dataMessage.sessionID);
        dos.writeInt(dataMessage.buffer.length);
        if (dataMessage instanceof CRC32DataMessage) {
            dos.writeBoolean(true);
            dos.writeLong(((CRC32DataMessage) dataMessage).crc32);
        } else {
            dos.writeBoolean(false);
        }
        dos.write(dataMessage.buffer);
    }

    private DataMessage getRXDataMessage(DataInputStream dis) throws IOException {
        int sessionID = dis.readInt();
        int bufferLength = dis.readInt();
        boolean isCRC32DataMessage = dis.readBoolean();
        if (isCRC32DataMessage) {
            CRC32DataMessage crc32DataMessage = new CRC32DataMessage();
            crc32DataMessage.crc32 = dis.readLong();
            crc32DataMessage.buffer = new byte[bufferLength];
            crc32DataMessage.sessionID = sessionID;
            dis.readFully(crc32DataMessage.buffer);
            return crc32DataMessage;
        } else {
            DataMessage dataMessage = new DataMessage();
            dataMessage.buffer = new byte[bufferLength];
            dataMessage.sessionID = sessionID;
            dis.readFully(dataMessage.buffer);
            return dataMessage;
        }
    }

    private void sendSrcClientServerGreeting(DataOutputStream dos, SrcClientServerGreeting srcClientServerGreeting) throws IOException {
        dos.writeByte(SrcClientServerGreetingID);
        dos.writeInt(srcClientServerGreeting.username.length());
        dos.write(srcClientServerGreeting.username.getBytes());
        dos.writeInt(srcClientServerGreeting.password.length());
        dos.write(srcClientServerGreeting.password.getBytes());
    }

    private SrcClientServerGreeting getSrcClientServerGreeting(DataInputStream dis) throws IOException {
        byte buffer[];
        SrcClientServerGreeting srcClientServerGreeting = new SrcClientServerGreeting();
        int usernameLength = dis.readInt();
        buffer = new byte[usernameLength];
        dis.readFully(buffer);
        srcClientServerGreeting.username = new String(buffer);
        int passwordLength = dis.readInt();
        buffer = new byte[passwordLength];
        dis.readFully(buffer);
        srcClientServerGreeting.password = new String(buffer);
        return srcClientServerGreeting;
    }

    private void sendConnectedSrcClientToDestClient(DataOutputStream dos) throws IOException {
        dos.writeByte(ConnectedSrcClientToDestClientID);
    }

    private ConnectedSrcClientToDestClient getConnectedSrcClientToDestClient() {
        return new ConnectedSrcClientToDestClient();
    }

    private void sendConnectedFromDestClient(DataOutputStream dos, ConnectedFromDestClient connectedFromDestClient) throws IOException {
        dos.writeByte(ConnectedFromDestClientID);
        dos.writeInt(connectedFromDestClient.sessionID);
    }

    private ConnectedFromDestClient getConnectedFromDestClient(DataInputStream dis) throws IOException {
        ConnectedFromDestClient connectedFromDestClient = new ConnectedFromDestClient();
        connectedFromDestClient.sessionID = dis.readInt();
        return connectedFromDestClient;
    }

    private void sendDisconnectFromDestClient(DataOutputStream dos, DisconnectFromDestClient disconnectFromDestClient) throws IOException {
        dos.writeByte(DisconnectFromDestClientID);
        dos.writeInt(disconnectFromDestClient.sessionID);
    }

    private DisconnectFromDestClient getDisconnectFromDestClient(DataInputStream dis) throws IOException {
        DisconnectFromDestClient disconnectFromDestClient = new DisconnectFromDestClient();
        disconnectFromDestClient.sessionID = dis.readInt();
        return disconnectFromDestClient;
    }

    private void sendSrcClientServerGreetingResponseV2(DataOutputStream dos, SrcClientServerGreetingResponseV2 srcClientServerGreetingResponse) throws IOException {
        dos.writeByte(SrcClientServerGreetingResponseV2ID);
        dos.writeDouble(srcClientServerGreetingResponse.serverVersion);
        dos.writeInt(srcClientServerGreetingResponse.destClientsDetails.size());
        for (DestClientDetails destClientDetails : srcClientServerGreetingResponse.destClientsDetails) {
            dos.writeInt(destClientDetails.dnsName.length());
            if (destClientDetails.dnsName.length() > 0) {
                dos.writeBytes(destClientDetails.dnsName);
            }
            dos.writeInt(destClientDetails.pcName.length());
            if (destClientDetails.pcName.length() > 0) {
                dos.writeBytes(destClientDetails.pcName);
            }
            if (destClientDetails.destClientType == DestClientConfig.DEST_CLIENT_TYPES.GENERIC) {
                dos.writeInt(1);
            } else if (destClientDetails.destClientType == DestClientConfig.DEST_CLIENT_TYPES.VNC) {
                dos.writeInt(2);
            } else if (destClientDetails.destClientType == DestClientConfig.DEST_CLIENT_TYPES.FILE_TRANSFER) {
                dos.writeInt(3);
            } else {
                throw new IOException("sendSrcClientServerGreetingResponse: Unknown destClientDetails.destClientType (" + destClientDetails.destClientType + ")");
            }
        }
    }

    private SrcClientServerGreetingResponseV2 getSrcClientServerGreetingResponseV2(DataInputStream dis) throws IOException {
        DestClientDetails destClientDetails;
        SrcClientServerGreetingResponseV2 srcClientServerGreetingResponseV2 = new SrcClientServerGreetingResponseV2();
        srcClientServerGreetingResponseV2.serverVersion = dis.readDouble();
        srcClientServerGreetingResponseV2.destClientsDetails = new Vector<DestClientDetails>();
        int destClientDetailsLength = dis.readInt();
        for (int i = 0; i < destClientDetailsLength; i++) {
            destClientDetails = new DestClientDetails();
            byte buffer[];
            destClientDetails.dnsName = "";
            int dnsNameLength = dis.readInt();
            if (dnsNameLength > 0) {
                buffer = new byte[dnsNameLength];
                dis.readFully(buffer);
                destClientDetails.dnsName = new String(buffer);
            }
            destClientDetails.pcName = "";
            int pcNameLength = dis.readInt();
            if (pcNameLength > 0) {
                buffer = new byte[pcNameLength];
                dis.readFully(buffer);
                destClientDetails.pcName = new String(buffer);
            }
            int destClientType = dis.readInt();
            if (destClientType == 1) {
                destClientDetails.destClientType = DestClientConfig.DEST_CLIENT_TYPES.GENERIC;
            } else if (destClientType == 2) {
                destClientDetails.destClientType = DestClientConfig.DEST_CLIENT_TYPES.VNC;
            } else if (destClientType == 3) {
                destClientDetails.destClientType = DestClientConfig.DEST_CLIENT_TYPES.FILE_TRANSFER;
            } else {
                throw new IOException("SrcClientServerGreetingResponse: Unknown destClientDetails.destClientType (" + destClientDetails.destClientType + ")");
            }
            srcClientServerGreetingResponseV2.destClientsDetails.add(destClientDetails);
        }
        return srcClientServerGreetingResponseV2;
    }

    private void sendIntCommandObject(DataOutputStream dos, SimpleCommandObject simpleCommandObject) throws IOException {
        dos.writeByte(SimpleCommandObjectID);
        dos.writeInt(simpleCommandObject.command);
    }

    private SimpleCommandObject getSimpleCommandObject(DataInputStream dis) throws IOException {
        SimpleCommandObject simpleCommandObject = new SimpleCommandObject();
        simpleCommandObject.command = dis.readInt();
        return simpleCommandObject;
    }
}
