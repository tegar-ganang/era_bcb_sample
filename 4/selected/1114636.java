package org.jboss.netty.handler.codec.http2;

import java.io.File;
import java.io.IOException;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Mixted implementation using both in Memory and in File with a limit of size
 * @author frederic bregier
 *
 */
public class MixteFileUpload implements FileUpload {

    private FileUpload fileUpload = null;

    private long limitSize = 0;

    private long definedSize = 0;

    public MixteFileUpload(String name, String filename, String contentType, String charset, long size, long limitSize) throws NullPointerException, IllegalArgumentException {
        this.limitSize = limitSize;
        if (size > this.limitSize) {
            fileUpload = new DiskFileUpload(name, filename, contentType, charset, size);
        } else {
            fileUpload = new DefaultFileUpload(name, filename, contentType, charset, size);
        }
        definedSize = size;
    }

    public void addContent(ChannelBuffer buffer, boolean last) throws IOException {
        if (fileUpload instanceof DefaultFileUpload) {
            if (fileUpload.length() + buffer.readableBytes() > limitSize) {
                DiskFileUpload diskFileUpload = new DiskFileUpload(fileUpload.getName(), fileUpload.getFilename(), fileUpload.getContentType(), fileUpload.getCharset(), definedSize);
                diskFileUpload.addContent(((DefaultFileUpload) fileUpload).getChannelBuffer(), false);
                fileUpload = diskFileUpload;
            }
        }
        fileUpload.addContent(buffer, last);
    }

    public void delete() {
        fileUpload.delete();
    }

    public byte[] get() throws IOException {
        return fileUpload.get();
    }

    public ChannelBuffer getChannelBuffer() throws IOException {
        return fileUpload.getChannelBuffer();
    }

    public String getCharset() {
        return fileUpload.getCharset();
    }

    public String getContentType() {
        return fileUpload.getContentType();
    }

    public String getFilename() {
        return fileUpload.getFilename();
    }

    public String getString() throws IOException {
        return fileUpload.getString();
    }

    public String getString(String encoding) throws IOException {
        return fileUpload.getString(encoding);
    }

    public boolean isCompleted() {
        return fileUpload.isCompleted();
    }

    public boolean isInMemory() {
        return fileUpload.isInMemory();
    }

    public long length() {
        return fileUpload.length();
    }

    public boolean renameTo(File dest) throws IOException {
        return fileUpload.renameTo(dest);
    }

    public void setCharset(String charset) {
        fileUpload.setCharset(charset);
    }

    public void setContent(ChannelBuffer buffer) throws IOException {
        if (buffer.readableBytes() > limitSize) {
            if (fileUpload instanceof DefaultFileUpload) {
                DiskFileUpload diskFileUpload = new DiskFileUpload(fileUpload.getName(), fileUpload.getFilename(), fileUpload.getContentType(), fileUpload.getCharset(), definedSize);
                fileUpload = diskFileUpload;
            }
        }
        fileUpload.setContent(buffer);
    }

    public void setContent(File file) throws IOException {
        if (file.length() > limitSize) {
            if (fileUpload instanceof DefaultFileUpload) {
                DiskFileUpload diskFileUpload = new DiskFileUpload(fileUpload.getName(), fileUpload.getFilename(), fileUpload.getContentType(), fileUpload.getCharset(), definedSize);
                fileUpload = diskFileUpload;
            }
        }
        fileUpload.setContent(file);
    }

    public void setContentType(String contentType) {
        fileUpload.setContentType(contentType);
    }

    public void setFilename(String filename) {
        fileUpload.setFilename(filename);
    }

    public HttpDataType getHttpDataType() {
        return fileUpload.getHttpDataType();
    }

    public String getName() {
        return fileUpload.getName();
    }

    public int compareTo(HttpData o) {
        return fileUpload.compareTo(o);
    }

    @Override
    public String toString() {
        return "Mixted: " + fileUpload.toString();
    }
}
