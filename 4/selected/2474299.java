package org.jboss.netty.handler.codec.http2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.jboss.netty.buffer.AggregateChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * Default FileUpload implementation that stores file into memory.<br><br>
 *
 * Warning: be aware of the memory limitation.
 *
 * @author frederic bregier
 *
 */
public class DefaultFileUpload implements FileUpload {

    private final String name;

    private String filename = null;

    private ChannelBuffer channelBuffer = null;

    private String contentType = null;

    private String charset = HttpCodecUtil.DEFAULT_CHARSET;

    private long definedSize = 0;

    private long size = 0;

    private boolean completed = false;

    public DefaultFileUpload(String name, String filename, String contentType, String charset, long size) throws NullPointerException, IllegalArgumentException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        name = name.trim();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c > 127) {
                throw new IllegalArgumentException("name contains non-ascii character: " + name);
            }
            switch(c) {
                case '=':
                case ',':
                case ';':
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                case '\f':
                case 0x0b:
                    throw new IllegalArgumentException("name contains one of the following prohibited characters: " + "=,; \\t\\r\\n\\v\\f: " + name);
            }
        }
        this.name = name;
        setFilename(filename);
        setContentType(contentType);
        if (charset != null) {
            setCharset(charset);
        }
        definedSize = size;
    }

    public HttpDataType getHttpDataType() {
        return HttpDataType.FileUpload;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (filename == null) {
            throw new NullPointerException("filename");
        }
        this.filename = filename;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Attribute)) {
            return false;
        }
        Attribute attribute = (Attribute) o;
        return getName().equalsIgnoreCase(attribute.getName());
    }

    public int compareTo(HttpData arg0) {
        if (!(arg0 instanceof FileUpload)) {
            throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + arg0.getHttpDataType());
        }
        return compareTo((FileUpload) arg0);
    }

    public int compareTo(FileUpload o) {
        int v;
        v = getName().compareToIgnoreCase(o.getName());
        if (v != 0) {
            return v;
        }
        return v;
    }

    public void setContent(ChannelBuffer buffer) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        long localsize = buffer.readableBytes();
        if (definedSize > 0 && definedSize < localsize) {
            throw new IOException("Out of size: " + localsize + " > " + definedSize);
        }
        channelBuffer = buffer;
        size = localsize;
        completed = true;
    }

    public void addContent(ChannelBuffer buffer, boolean last) throws IOException {
        if (buffer != null) {
            long localsize = buffer.readableBytes();
            if (definedSize > 0 && definedSize < size + localsize) {
                throw new IOException("Out of size: " + (size + localsize) + " > " + definedSize);
            }
            size += localsize;
            if (channelBuffer == null) {
                channelBuffer = buffer;
            } else {
                channelBuffer = AggregateChannelBuffer.wrappedCheckedBuffer(channelBuffer, buffer);
            }
        }
        if (last) {
            completed = true;
        } else {
            if (buffer == null) {
                throw new NullPointerException("buffer");
            }
        }
    }

    public void setContent(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        long newsize = file.length();
        if (newsize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File too big to be loaded in memory");
        }
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel fileChannel = inputStream.getChannel();
        byte[] array = new byte[(int) newsize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        int read = 0;
        while (read < newsize) {
            read += fileChannel.read(byteBuffer);
        }
        fileChannel.close();
        channelBuffer = ChannelBuffers.wrappedBuffer(byteBuffer);
        size = newsize;
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void delete() {
    }

    public byte[] get() {
        if (channelBuffer == null) {
            return new byte[0];
        }
        byte[] array = new byte[channelBuffer.readableBytes()];
        channelBuffer.getBytes(channelBuffer.readerIndex(), array);
        return array;
    }

    public void setContentType(String contentType) {
        if (contentType == null) {
            throw new NullPointerException("contentType");
        }
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

    public String getString() {
        return getString(HttpCodecUtil.DEFAULT_CHARSET);
    }

    public String getString(String encoding) {
        if (channelBuffer == null) {
            return "";
        }
        if (encoding == null) {
            return getString(HttpCodecUtil.DEFAULT_CHARSET);
        }
        return channelBuffer.toString(encoding);
    }

    /**
     * Utility to go from a In Memory FileUpload
     * to a Disk (or another implementation) FileUpload
     * @return the attached ChannelBuffer containing the actual bytes
     */
    public ChannelBuffer getChannelBuffer() {
        return channelBuffer;
    }

    public boolean isInMemory() {
        return true;
    }

    public long length() {
        return size;
    }

    public boolean renameTo(File dest) throws IOException {
        if (dest == null) {
            throw new NullPointerException("dest");
        }
        if (channelBuffer == null) {
            dest.createNewFile();
            return true;
        }
        int length = channelBuffer.readableBytes();
        FileOutputStream outputStream = new FileOutputStream(dest);
        FileChannel fileChannel = outputStream.getChannel();
        ByteBuffer byteBuffer = channelBuffer.toByteBuffer();
        int written = 0;
        while (written < length) {
            written += fileChannel.write(byteBuffer);
            fileChannel.force(false);
        }
        fileChannel.close();
        return written == length;
    }

    @Override
    public String toString() {
        return "content-disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n" + "Content-Type: " + contentType + (charset != null ? "; charset=" + charset + "\r\n" : "\r\n") + "Content-Length: " + size + "\r\n" + "Completed: " + completed + "\r\nIsInMemory: " + isInMemory();
    }
}
