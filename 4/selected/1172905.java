package org.blaps.erazer.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * HEADER
 *   |
 *   |------ METHOD
 *   |          |------ SIZE
 *   |          |------ NAME
 *   |          |------ PATTERN
 *   |          |------ PATTERN
 *   |          |------ PATTERN
 *   |          .
 *   |          .
 *   |          .
 *   |------ METHOD
 *   .          |------ SIZE
 *   .          |------ NAME
 *   .          |------ PATTERN
 *   .          .
 *   .          .
 *
 *
 * HEADER
 * pos      length (byte)          value                        def
 *   0                  4          'ERAZ' [UTF-8] {}            file signature
 *   4                  1          1                            version
 *   5                  4          NNNN                         CRC (computed with methods)
 *   9+         ....                ...                         DATA : list of erase methods
 *
 * DATA - METHOD
 * pos      length (byte)          value                        def
 *   0              4              ZZZZ                        the pattern size (not including name,name size, comment and comment size)
 *   4              1                 N                        the name size (>=0, 0 if no name)
 *   5              N              the name of the method
 *   5+N            2                CC                        the comments size (>=0)
 * 5+N+2           CC             the comment
 * 5+N+2+CC         2                PS                        the fisrt pattern size for this method (>=0, 0 if random)
 * 5+N+2+CC+2      PS              .....                       the first patten bytes
 *
 * @author Administrateur
 */
public class ErazerFile {

    private static final String ERZ_SIGN = "ERAZ";

    private static final int HEADER_SIZE = 9;

    private static final String CHARSET_NAME = "UTF-8";

    private Charset charset = null;

    private Charset getCharset() {
        if (charset == null) {
            charset = Charset.forName(CHARSET_NAME);
        }
        return charset;
    }

    public List<ErazeMethod> load(File file) throws FileNotFoundException, IOException {
        FileInputStream is = null;
        FileChannel channel = null;
        ByteBuffer buffer;
        List<ErazeMethod> methods = new ArrayList<ErazeMethod>(0);
        String header = null;
        int version = 0;
        long crc = 0l;
        try {
            is = new FileInputStream(file);
            channel = is.getChannel();
            buffer = readHeader(channel);
            header = getHeader(buffer);
            version = getVersion(buffer);
            crc = getCRC(buffer);
            verifyFormat(header, version);
            methods = new ArrayList<ErazeMethod>();
            readMethods(channel, methods);
            verifyCRC(crc, methods);
        } finally {
            closeInputStream(is);
        }
        return methods;
    }

    public void save(List<ErazeMethod> methods, File file) throws FileNotFoundException, IOException {
        FileOutputStream os = null;
        FileChannel channel = null;
        try {
            os = new FileOutputStream(file);
            channel = os.getChannel();
            writeHeader(channel, methods);
            writeMethods(channel, methods);
        } finally {
            closeOutputStream(os);
        }
    }

    private boolean closeOutputStream(OutputStream os) {
        if (os != null) {
            try {
                os.close();
                return true;
            } catch (IOException ex) {
                Logger.getLogger(ErazerFile.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    private boolean closeInputStream(InputStream is) {
        if (is != null) {
            try {
                is.close();
                return true;
            } catch (IOException ex) {
                Logger.getLogger(ErazerFile.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    private long getCRC(ByteBuffer headerBuffer) {
        return (long) headerBuffer.getInt(5);
    }

    private long getCRC(List<ErazeMethod> methods) {
        byte[] randomBytes = { (byte) 0x00, (byte) 0x00 };
        byte[] sizeByte = new byte[2];
        int patternLength;
        CRC32 crc = new CRC32();
        for (ErazeMethod method : methods) {
            for (PatternDefinition pattern : method.getPatterns()) {
                if (pattern.isRandom()) {
                    crc.update(randomBytes);
                } else {
                    patternLength = pattern.getPattern().length;
                    sizeByte[1] = (byte) ((patternLength >> 8) & 0xFF);
                    sizeByte[0] = (byte) ((patternLength) & 0xFF);
                    crc.update(sizeByte);
                    crc.update(pattern.getPattern());
                }
            }
        }
        return crc.getValue();
    }

    private void initPattern(ByteBuffer buffer, List<PatternDefinition> patterns) throws IOException {
        Integer patternSize;
        byte[] pattern = null;
        if (buffer.remaining() > 0) {
            patternSize = getUShort(buffer);
            if (patternSize != null && patternSize >= 0) {
                if (patternSize == 0) {
                    pattern = new byte[patternSize];
                } else if (patternSize > 0) {
                    pattern = getBytes(buffer, patternSize);
                }
            }
            if (pattern != null) {
                patterns.add(new PatternDefinition(pattern));
                initPattern(buffer, patterns);
            }
        }
    }

    private ByteBuffer prepareHeader(List<ErazeMethod> methods) {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.put(ERZ_SIGN.getBytes(getCharset()));
        header.put((byte) 0x01);
        header.putInt((int) getCRC(methods));
        header.flip();
        return header;
    }

    private String getHeader(ByteBuffer headerBuffer) {
        headerBuffer.position(0);
        return getString(headerBuffer, ERZ_SIGN.length());
    }

    private Long readUInt(FileChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        int nb = channel.read(buffer);
        if (nb == 4) {
            buffer.rewind();
            return (long) (buffer.getInt(0) & 0xFFFFFFFF);
        }
        return null;
    }

    private Integer readUShort(FileChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        int nb = channel.read(buffer);
        if (nb == 2) {
            buffer.rewind();
            return getUShort(buffer);
        }
        return null;
    }

    private Integer getUShort(ByteBuffer buffer) {
        return (int) (buffer.getShort(0) & 0xFFFF);
    }

    private Short readUByte(FileChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        int nb = channel.read(buffer);
        if (nb == 1) {
            buffer.rewind();
            return (short) (buffer.get(0) & 0xFF);
        }
        return null;
    }

    private byte[] readBytes(FileChannel channel, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        int nb = channel.read(buffer);
        if (nb == length) {
            buffer.rewind();
            return getBytes(buffer, nb);
        }
        return null;
    }

    private byte[] getBytes(ByteBuffer buffer, int size) {
        byte[] b = new byte[size];
        buffer.get(b);
        return b;
    }

    private String readString(FileChannel channel, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        int nb = channel.read(buffer);
        if (nb == length) {
            buffer.rewind();
            return getString(buffer, length);
        }
        return null;
    }

    private String getString(ByteBuffer buffer, int length) {
        if (buffer == null) {
            return "";
        }
        return new String(getBytes(buffer, length), getCharset());
    }

    private ErazeMethod getMethod(FileChannel channel) throws IOException {
        ErazeMethod method = null;
        Long methodSize = readUInt(channel);
        Short nameSize;
        Integer commentSize;
        String name;
        String comment;
        ByteBuffer patternBuffer;
        if (methodSize != null) {
            nameSize = readUByte(channel);
            if (nameSize != null) {
                if (nameSize > 0) {
                    name = readString(channel, nameSize);
                } else {
                    name = "";
                }
                commentSize = readUShort(channel);
                if (commentSize != null) {
                    if (commentSize > 0) {
                        comment = readString(channel, commentSize);
                    } else {
                        comment = "";
                    }
                    method = new ErazeMethod(name);
                    method.setComment(comment);
                    if (methodSize > 0) {
                        patternBuffer = ByteBuffer.allocate(methodSize.intValue());
                        patternBuffer.rewind();
                        initPattern(patternBuffer, method.getPatterns());
                    }
                }
            }
        } else {
        }
        return method;
    }

    private int getMethodSize(ErazeMethod method) {
        int size = 0;
        for (PatternDefinition pattern : method.getPatterns()) {
            size += 2;
            if (!pattern.isRandom()) {
                size += pattern.getPattern().length;
            }
        }
        return size;
    }

    private String getString(String s, int maxLength) {
        if (s == null) {
            return "";
        }
        if (s.length() > maxLength) {
            return s.substring(0, maxLength);
        }
        return s;
    }

    private int getVersion(ByteBuffer headerBuffer) {
        return (int) (headerBuffer.get(4) & 0xFF);
    }

    private ByteBuffer readHeader(FileChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        channel.read(buffer);
        buffer.flip();
        return buffer;
    }

    private void readMethods(FileChannel channel, List<ErazeMethod> methods) throws IOException {
        channel.position(HEADER_SIZE);
        ErazeMethod method;
        while ((method = getMethod(channel)) != null) {
            methods.add(method);
        }
    }

    private void verifyCRC(long crc, List<ErazeMethod> methods) {
    }

    private void verifyFormat(String header, int version) throws IOException {
        boolean bh = ERZ_SIGN.equals(header);
        boolean bv = (version == 1);
        String message = null;
        if (!bh) {
            message = "Invalid header file " + header;
        } else if (!bv) {
            message = "Incompatible format version " + version;
        }
        if (message != null) {
            throw new IOException(message);
        }
    }

    private void writeHeader(FileChannel channel, List<ErazeMethod> methods) throws IOException {
        channel.position(0l);
        channel.write(prepareHeader(methods));
    }

    private void writeMethod(FileChannel channel, ErazeMethod method) throws IOException {
        String name = getString(method.getName(), 255);
        String comment = getString(method.getComment(), 65535);
        int methodSize = getMethodSize(method);
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + name.length() + 2 + comment.length() + methodSize);
        buffer.putInt(methodSize);
        buffer.put((byte) (name.length() & 0xFF));
        if (name.length() > 0) {
            buffer.put(name.getBytes(getCharset()));
        }
        buffer.putShort((short) (comment.length() & 0xFFFF));
        if (comment.length() > 0) {
            buffer.put(comment.getBytes(getCharset()));
        }
        for (PatternDefinition pattern : method.getPatterns()) {
            if (pattern.isRandom()) {
                buffer.putShort((short) 0x0);
            } else {
                buffer.putShort((short) (pattern.getPattern().length & 0xFFFF));
                buffer.put(pattern.getPattern());
            }
        }
        buffer.flip();
        channel.write(buffer);
    }

    private void writeMethods(FileChannel channel, List<ErazeMethod> methods) throws IOException {
        for (ErazeMethod method : methods) {
            writeMethod(channel, method);
        }
    }
}
