package org.zoolib;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class ZUtil_IO {

    private ZUtil_IO() {
    }

    public static final int sReadCount(DataInput iDI) throws IOException {
        int count = iDI.readUnsignedByte();
        if (count == 255) count = iDI.readInt();
        return count;
    }

    public static void sWriteCount(DataOutput iDO, int iCount) throws IOException {
        if (iCount < 255) {
            iDO.writeByte(iCount);
        } else {
            iDO.writeByte(255);
            iDO.writeInt(iCount);
        }
    }

    public static final void sWriteString(DataOutput iDO, String iString) throws IOException {
        byte byteBuffer[] = sUTF8FromString(iString);
        sWriteCount(iDO, byteBuffer.length);
        iDO.write(byteBuffer);
    }

    private static final String sReadUTF8(DataInput iDI, int iCount) throws IOException {
        byte byteBuffer[] = new byte[iCount];
        iDI.readFully(byteBuffer);
        return sStringFromUTF8(byteBuffer);
    }

    public static final String sReadString(DataInput iDI) throws IOException {
        int stringLength = ZUtil_IO.sReadCount(iDI);
        return ZUtil_IO.sReadUTF8(iDI, stringLength);
    }

    public static final String sStringFromUTF8(byte iBytes[]) {
        int sourceCount = iBytes.length;
        int sourceIndex = 0;
        char destBuffer[] = new char[sourceCount];
        int destIndex = 0;
        for (; ; ) {
            if (sourceIndex == sourceCount) break;
            int byte1 = iBytes[sourceIndex++] & 0xFF;
            int test = byte1 >> 4;
            if (test < 8) {
                destBuffer[destIndex++] = (char) byte1;
            } else {
                if (sourceIndex == sourceCount) break;
                int byte2 = iBytes[sourceIndex++];
                if (test == 12 || test == 13) {
                    destBuffer[destIndex++] = (char) (((byte1 & 0x1F) << 6) | (byte2 & 0x3F));
                } else if (test == 14) {
                    if (sourceIndex == sourceCount) break;
                    int byte3 = iBytes[sourceIndex++];
                    destBuffer[destIndex++] = (char) (((byte1 & 0x0F) << 12) | ((byte2 & 0x3F) << 6) | (byte3 & 0x3F));
                } else {
                    break;
                }
            }
        }
        return new String(destBuffer, 0, destIndex);
    }

    public static final byte[] sUTF8FromString(String iString) {
        int sourceCount = iString.length();
        int destCount = 0;
        for (int sourceIndex = 0; sourceIndex < sourceCount; ++sourceIndex) {
            int c = iString.charAt(sourceIndex);
            if ((c >= 0x0000) && (c <= 0x007F)) ++destCount; else if (c > 0x07FF) destCount += 3; else destCount += 2;
        }
        byte destBuffer[] = new byte[destCount];
        for (int sourceIndex = 0, destIndex = 0; sourceIndex < sourceCount; ++sourceIndex) {
            int c = iString.charAt(sourceIndex);
            if ((c >= 0x0000) && (c <= 0x007F)) {
                destBuffer[destIndex++] = (byte) c;
            } else if (c > 0x07FF) {
                destBuffer[destIndex++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                destBuffer[destIndex++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                destBuffer[destIndex++] = (byte) (0x80 | (c & 0x3F));
            } else {
                destBuffer[destIndex++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                destBuffer[destIndex++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return destBuffer;
    }

    public static final void sWrite(OutputStream iOS, String iString) throws IOException {
        byte buffer[] = new byte[iString.length()];
        for (int x = 0, length = iString.length(); x < length; ++x) buffer[x] = (byte) iString.charAt(x);
        iOS.write(buffer);
    }

    public static final int sSuckUntilLF(InputStream iIS) throws IOException {
        int count = 0;
        byte buf[] = new byte[1];
        boolean priorWasCR = false;
        for (; ; ) {
            if (0 >= iIS.read(buf)) {
                break;
            }
            byte curByte = buf[0];
            if (curByte == 10) {
                break;
            } else if (curByte == 13) {
                if (priorWasCR) ++count;
                priorWasCR = true;
            } else {
                if (priorWasCR) {
                    ++count;
                }
                priorWasCR = false;
                ++count;
            }
        }
        return count;
    }

    public static final void sCopyAll(InputStream iIS, OutputStream iOS) {
        byte[] buffer = new byte[4096];
        for (; ; ) {
            int readLength = 0;
            try {
                if (0 == iIS.available()) {
                    readLength = iIS.read(buffer, 0, 1);
                    if (readLength == -1) break;
                }
                int available = iIS.available();
                if (available > 0) {
                    if (available > buffer.length - readLength) available = buffer.length - readLength;
                    int nextChunk = iIS.read(buffer, readLength, available);
                    if (nextChunk > 0) readLength += nextChunk;
                }
            } catch (Exception ex) {
                break;
            }
            try {
                iOS.write(buffer, 0, readLength);
            } catch (Exception ex) {
                break;
            }
        }
        try {
            iOS.close();
        } catch (Exception ex) {
        }
        try {
            iIS.close();
        } catch (Exception ex) {
        }
    }

    public static final long sCopy(InputStream iIS, OutputStream iOS, long iCount) {
        long countWritten = 0;
        final byte[] buffer = new byte[4096];
        while (iCount != 0) {
            int readLength = 0;
            try {
                if (0 == iIS.available()) {
                    readLength = iIS.read(buffer, 0, 1);
                    if (readLength == -1) break;
                }
                int available = iIS.available();
                if (available > 0) {
                    if (available > buffer.length - readLength) available = buffer.length - readLength;
                    if (available > iCount - readLength) available = (int) (iCount - readLength);
                    int nextChunk = iIS.read(buffer, readLength, available);
                    if (nextChunk > 0) readLength += nextChunk;
                }
                iCount -= readLength;
            } catch (Exception ex) {
                break;
            }
            try {
                iOS.write(buffer, 0, readLength);
            } catch (Exception ex) {
                break;
            }
            countWritten += readLength;
        }
        return countWritten;
    }
}
