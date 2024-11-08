package net.sf.bt747.gps.skytraq;

/**
 * Reference: SkyTraq AN0003 Binary Messages Specification.
 * 
 * @author Mario De Weerd
 * 
 */
public class SkytraqTransportMessageModel {

    /** The message ID */
    private byte messageID;

    /** The message payload. */
    private byte[] messageBody;

    /** First byte of transport message. */
    private static final byte startByte0 = (byte) 0xA0;

    /** Second byte of transport message. */
    private static final byte startByte1 = (byte) 0xA1;

    /** Last-1 byte of transport message. */
    private static final byte endByte0 = (byte) 0x0D;

    /** Last byte of transport message. */
    private static final byte endByte1 = (byte) 0x0A;

    public SkytraqTransportMessageModel(byte[] payload) {
        this.messageID = payload[0];
        this.messageBody = new byte[payload.length - 1];
        for (int i = messageBody.length - 1; i >= 0; i--) {
            messageBody[i] = payload[i + 1];
        }
    }

    public SkytraqTransportMessageModel(final byte messageID, final byte[] messageBody) {
        this.messageID = messageID;
        this.messageBody = new byte[messageBody.length];
        for (int i = 0; i < messageBody.length; i++) {
            this.messageBody[i] = messageBody[i];
        }
    }

    /**
     * Get the transport message payload.
     * 
     * @return Transport message payload.
     */
    public byte[] getMessageBody() {
        return messageBody;
    }

    public byte getMessageID() {
        return messageID;
    }

    /**
     * Provide the transport message.
     * 
     * @return The transport message.
     */
    public byte[] getMessage() {
        final int payloadLength = messageBody.length + 1;
        final int transportSize = payloadLength + 8;
        byte[] message = new byte[transportSize];
        message[0] = startByte0;
        message[1] = startByte1;
        message[2] = (byte) (payloadLength >> 8);
        message[3] = (byte) (payloadLength & 0xFF);
        message[4] = messageID;
        int checksum = 0;
        for (int i = 0; i < messageBody.length; i++) {
            message[i + 5] = messageBody[i];
            checksum += messageBody[i];
            checksum &= 0x7FFF;
        }
        message[transportSize - 3] = (byte) (checksum >> 8);
        message[transportSize - 2] = (byte) (checksum & 0xFF);
        message[transportSize - 2] = endByte0;
        message[transportSize - 1] = endByte1;
        return message;
    }

    /**
     * Sets the message. Extracts the payload internally and checks validity
     * of message.
     * 
     * @param message
     * @return true if the message is valid.
     */
    public boolean setMessage(byte[] message) {
        messageBody = null;
        if (message.length < 8) {
            return false;
        }
        if (message[0] != startByte0 || message[1] != startByte1 || message[message.length - 2] != endByte0 || message[message.length - 1] != endByte1) {
            return false;
        }
        int payloadlenght = (message[2] << 8) | (message[3] & 0xFF);
        if (message.length != payloadlenght + 8) {
            return false;
        }
        messageID = message[4];
        final int expected_checksum = (message[message.length - 4] << 8) | (message[message.length - 3] & 0xFF);
        messageBody = new byte[payloadlenght - 1];
        int checksum = 0;
        for (int i = 0; i < payloadlenght - 1; i++) {
            messageBody[i] = message[i + 5];
            checksum += messageBody[i];
            checksum &= 0x7FFF;
        }
        if (checksum != expected_checksum) {
            return false;
        }
        return true;
    }
}
