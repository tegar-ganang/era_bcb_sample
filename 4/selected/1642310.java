package sun.security.smartcardio;

import java.nio.*;
import java.security.AccessController;
import javax.smartcardio.*;
import static sun.security.smartcardio.PCSC.*;
import sun.security.action.GetPropertyAction;

/**
 * CardChannel implementation.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
final class ChannelImpl extends CardChannel {

    private final CardImpl card;

    private final int channel;

    private volatile boolean isClosed;

    ChannelImpl(CardImpl card, int channel) {
        this.card = card;
        this.channel = channel;
    }

    void checkClosed() {
        card.checkState();
        if (isClosed) {
            throw new IllegalStateException("Logical channel has been closed");
        }
    }

    public Card getCard() {
        return card;
    }

    public int getChannelNumber() {
        checkClosed();
        return channel;
    }

    private static void checkManageChannel(byte[] b) {
        if (b.length < 4) {
            throw new IllegalArgumentException("Command APDU must be at least 4 bytes long");
        }
        if ((b[0] >= 0) && (b[1] == 0x70)) {
            throw new IllegalArgumentException("Manage channel command not allowed, use openLogicalChannel()");
        }
    }

    public ResponseAPDU transmit(CommandAPDU command) throws CardException {
        checkClosed();
        card.checkExclusive();
        byte[] commandBytes = command.getBytes();
        byte[] responseBytes = doTransmit(commandBytes);
        return new ResponseAPDU(responseBytes);
    }

    public int transmit(ByteBuffer command, ByteBuffer response) throws CardException {
        checkClosed();
        card.checkExclusive();
        if ((command == null) || (response == null)) {
            throw new NullPointerException();
        }
        if (response.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (command == response) {
            throw new IllegalArgumentException("command and response must not be the same object");
        }
        if (response.remaining() < 258) {
            throw new IllegalArgumentException("Insufficient space in response buffer");
        }
        byte[] commandBytes = new byte[command.remaining()];
        command.get(commandBytes);
        byte[] responseBytes = doTransmit(commandBytes);
        response.put(responseBytes);
        return responseBytes.length;
    }

    private static final boolean t0GetResponse = getBooleanProperty("sun.security.smartcardio.t0GetResponse", true);

    private static final boolean t1GetResponse = getBooleanProperty("sun.security.smartcardio.t1GetResponse", true);

    private static final boolean t1StripLe = getBooleanProperty("sun.security.smartcardio.t1StripLe", false);

    private static boolean getBooleanProperty(String name, boolean def) {
        String val = AccessController.doPrivileged(new GetPropertyAction(name));
        if (val == null) {
            return def;
        }
        if (val.equalsIgnoreCase("true")) {
            return true;
        } else if (val.equalsIgnoreCase("false")) {
            return false;
        } else {
            throw new IllegalArgumentException(name + " must be either 'true' or 'false'");
        }
    }

    private byte[] concat(byte[] b1, byte[] b2, int n2) {
        int n1 = b1.length;
        if ((n1 == 0) && (n2 == b2.length)) {
            return b2;
        }
        byte[] res = new byte[n1 + n2];
        System.arraycopy(b1, 0, res, 0, n1);
        System.arraycopy(b2, 0, res, n1, n2);
        return res;
    }

    private static final byte[] B0 = new byte[0];

    private byte[] doTransmit(byte[] command) throws CardException {
        try {
            checkManageChannel(command);
            setChannel(command);
            int n = command.length;
            boolean t0 = card.protocol == SCARD_PROTOCOL_T0;
            boolean t1 = card.protocol == SCARD_PROTOCOL_T1;
            if (t0 && (n >= 7) && (command[4] == 0)) {
                throw new CardException("Extended length forms not supported for T=0");
            }
            if ((t0 || (t1 && t1StripLe)) && (n >= 7)) {
                int lc = command[4] & 0xff;
                if (lc != 0) {
                    if (n == lc + 6) {
                        n--;
                    }
                } else {
                    lc = ((command[5] & 0xff) << 8) | (command[6] & 0xff);
                    if (n == lc + 9) {
                        n -= 2;
                    }
                }
            }
            boolean getresponse = (t0 && t0GetResponse) || (t1 && t1GetResponse);
            int k = 0;
            byte[] result = B0;
            while (true) {
                if (++k >= 32) {
                    throw new CardException("Could not obtain response");
                }
                byte[] response = SCardTransmit(card.cardId, card.protocol, command, 0, n);
                int rn = response.length;
                if (getresponse && (rn >= 2)) {
                    if ((rn == 2) && (response[0] == 0x6c)) {
                        command[n - 1] = response[1];
                        continue;
                    }
                    if (response[rn - 2] == 0x61) {
                        if (rn > 2) {
                            result = concat(result, response, rn - 2);
                        }
                        command[1] = (byte) 0xC0;
                        command[2] = 0;
                        command[3] = 0;
                        command[4] = response[rn - 1];
                        n = 5;
                        continue;
                    }
                }
                result = concat(result, response, rn);
                break;
            }
            return result;
        } catch (PCSCException e) {
            card.handleError(e);
            throw new CardException(e);
        }
    }

    private static int getSW(byte[] res) throws CardException {
        if (res.length < 2) {
            throw new CardException("Invalid response length: " + res.length);
        }
        int sw1 = res[res.length - 2] & 0xff;
        int sw2 = res[res.length - 1] & 0xff;
        return (sw1 << 8) | sw2;
    }

    private static boolean isOK(byte[] res) throws CardException {
        return (res.length == 2) && (getSW(res) == 0x9000);
    }

    private void setChannel(byte[] com) {
        int cla = com[0];
        if (cla < 0) {
            return;
        }
        if ((cla & 0xe0) == 0x20) {
            return;
        }
        if (channel <= 3) {
            com[0] &= 0xbc;
            com[0] |= channel;
        } else if (channel <= 19) {
            com[0] &= 0xb0;
            com[0] |= 0x40;
            com[0] |= (channel - 4);
        } else {
            throw new RuntimeException("Unsupported channel number: " + channel);
        }
    }

    public void close() throws CardException {
        if (getChannelNumber() == 0) {
            throw new IllegalStateException("Cannot close basic logical channel");
        }
        if (isClosed) {
            return;
        }
        card.checkExclusive();
        try {
            byte[] com = new byte[] { 0x00, 0x70, (byte) 0x80, 0 };
            com[3] = (byte) getChannelNumber();
            setChannel(com);
            byte[] res = SCardTransmit(card.cardId, card.protocol, com, 0, com.length);
            if (isOK(res) == false) {
                throw new CardException("close() failed: " + PCSC.toString(res));
            }
        } catch (PCSCException e) {
            card.handleError(e);
            throw new CardException("Could not close channel", e);
        } finally {
            isClosed = true;
        }
    }

    public String toString() {
        return "PC/SC channel " + channel;
    }
}
