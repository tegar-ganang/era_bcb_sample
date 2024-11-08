package org.red5.io.flv.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagWriter;
import org.red5.io.flv.IFLV;
import org.red5.io.utils.IOUtils;

/**
 * A Writer is used to write the contents of a FLV file
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class FLVWriter implements ITagWriter {

    private static Log log = LogFactory.getLog(FLVWriter.class.getName());

    private FileOutputStream fos = null;

    private WritableByteChannel channel;

    private ByteBuffer out;

    private ITag lastTag = null;

    private IFLV flv = null;

    private long bytesWritten = 0;

    private int offset = 0;

    public FLVWriter(FileOutputStream fos) {
        this(fos, null);
    }

    /**
	 * WriterImpl Constructor
	 *
	 * @param fos
	 */
    public FLVWriter(FileOutputStream fos, ITag lastTag) {
        this.fos = fos;
        this.lastTag = lastTag;
        if (lastTag != null) {
            offset = lastTag.getTimestamp();
        }
        channel = this.fos.getChannel();
        out = ByteBuffer.allocate(1024);
        out.setAutoExpand(true);
    }

    /**
	 * Writes the header bytes
	 *
	 * @throws IOException
	 */
    public void writeHeader() throws IOException {
        out.put((byte) 0x46);
        out.put((byte) 0x4C);
        out.put((byte) 0x56);
        out.put((byte) 0x01);
        out.put((byte) 0x05);
        out.putInt(0x09);
        out.flip();
        channel.write(out.buf());
    }

    public IStreamableFile getFile() {
        return flv;
    }

    public void setFLV(IFLV flv) {
        this.flv = flv;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public boolean writeTag(ITag tag) throws IOException {
        out.clear();
        out.putInt((lastTag == null) ? 0 : (lastTag.getBodySize() + 11));
        out.put(tag.getDataType());
        IOUtils.writeMediumInt(out, tag.getBodySize());
        IOUtils.writeMediumInt(out, tag.getTimestamp() + offset);
        out.putInt(0x00);
        out.flip();
        bytesWritten += channel.write(out.buf());
        ByteBuffer bodyBuf = tag.getBody();
        bytesWritten += channel.write(bodyBuf.buf());
        lastTag = tag;
        return false;
    }

    public boolean writeTag(byte type, ByteBuffer data) throws IOException {
        return false;
    }

    public void close() {
        if (out != null) {
            out.release();
            out = null;
        }
        try {
            channel.close();
            fos.close();
        } catch (IOException e) {
            log.error("FLVWriter :: close ::>\n", e);
        }
    }

    public boolean writeStream(byte[] b) {
        return false;
    }
}
