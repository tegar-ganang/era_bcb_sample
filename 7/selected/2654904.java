package librfid.rfid;

import librfid.exceptions.*;
import librfid.utils.*;

public class HexCommand {

    private static byte[] bytes;

    private int offset = 0;

    public HexCommand(byte... ibytes) {
        bytes = ibytes;
        int new_size = bytes.length + 3;
        if (new_size > 0xFF) {
            new_size = new_size / 0xFF;
            int mod = new_size % 0xFF;
            if (mod != 0) new_size += 1;
            offset = new_size;
        }
        bytes = ByteUtils.cons((byte) new_size, bytes);
        bytes = ByteUtils.concat(bytes, CRC16.crc16(bytes));
    }

    public static HexCommand fromByteArray(byte[] data) throws RFIDException {
        int actual_size = data.length;
        int data_size = ByteUtils.unsignedByteToInt(data[0]);
        byte[] crc = ByteUtils.range(actual_size - 2, -1, data);
        data = ByteUtils.range(0, actual_size - 3, data);
        if (actual_size > 0xFF) {
            byte[] data_size_bytes = new byte[data_size];
            for (int i = 0; i < data_size; i++) data_size_bytes[i] = data[i + 1];
            data_size = ByteUtils.mergeBytes(data_size_bytes);
        }
        data = ByteUtils.range(1, -1, data);
        if (data_size != actual_size) throw new CommandException("size mismatch, given: " + data_size + ", string was: " + actual_size);
        HexCommand hc = new HexCommand(data);
        byte[] calc_crc = hc.crc16();
        if (!(ByteUtils.sameContents(crc, calc_crc))) throw new CRCException("checksum (crc16) mismatch, given: " + ByteUtils.byteArrayToHex(crc) + ", calculated: " + ByteUtils.byteArrayToHex(calc_crc));
        return hc;
    }

    public static HexCommand fromString(String s) throws RFIDException {
        byte[] data = s.getBytes();
        int actual_size = data.length;
        int data_size = ByteUtils.unsignedByteToInt(data[0]);
        byte[] crc = ByteUtils.range(actual_size - 2, -1, data);
        if (actual_size > 0xFF) {
            byte[] data_size_bytes = new byte[data_size];
            for (int i = 0; i < data_size; i++) data_size_bytes[i] = data[i + 1];
            data_size = ByteUtils.mergeBytes(data_size_bytes);
        }
        if (data_size != actual_size) throw new CommandException("size mismatch, given: " + data_size + ", string was: " + actual_size);
        data = ByteUtils.range(1, data.length - 1, data);
        HexCommand hc = new HexCommand(data);
        byte[] calc_crc = CRC16.crc16(data);
        if (!(ByteUtils.sameContents(crc, calc_crc))) throw new CRCException("checksum (crc16) mismatch, given: " + ByteUtils.byteArrayToHex(crc) + ", calculated: " + ByteUtils.byteArrayToHex(calc_crc));
        return hc;
    }

    public String toString() {
        return new String(bytes);
    }

    public byte[] toByteArray() {
        return bytes;
    }

    public String[] toStringArray() {
        String[] result = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = ByteUtils.byteToHex(bytes[i]);
        }
        return result;
    }

    public int length() {
        return bytes.length;
    }

    public int size() {
        return this.length();
    }

    public byte[] crc16() {
        return ByteUtils.range(bytes.length - 2, -1, bytes);
    }

    public byte at(int i) {
        return bytes[i];
    }

    public byte comAdr() {
        return bytes[1 + offset];
    }

    public byte controlByte() {
        return bytes[2 + offset];
    }

    public boolean isHostCommand() {
        return (bytes[3 + offset] == (byte) 0xB0);
    }

    public byte status() {
        int host_command_offset = 0;
        if (this.isHostCommand()) host_command_offset = 1;
        byte st = bytes[3 + host_command_offset + offset];
        return st;
    }

    public boolean hasStatus(byte s) {
        return (this.status() == s);
    }

    public byte[] protocolData() {
        int host_command_offset = 0;
        if (this.isHostCommand()) host_command_offset = 1;
        byte[] pd = ByteUtils.range(4 + host_command_offset + offset, this.length() - 3, bytes);
        return pd;
    }
}
