package org.red5.io.flv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Writer is used to write the contents of a FLV file
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Tiago Jacobs (tiago@imdt.com.br)
 */
public class FLVWriter implements ITagWriter {

    private static Logger log = LoggerFactory.getLogger(FLVWriter.class);

    /**
	 * Length of the flv header in bytes
	 */
    private static final int HEADER_LENGTH = 9;

    /**
	 * Length of the flv tag in bytes
	 */
    private static final int TAG_HEADER_LENGTH = 11;

    /**
	 * Position of the meta data tag in our file.
	 */
    private static final int META_POSITION = 13;

    /**
	 * FLV object
	 */
    private IFLV flv;

    /**
	 * Number of bytes written
	 */
    private volatile long bytesWritten;

    /**
	 * Position in file
	 */
    private int offset;

    /**
	 * Size of tag containing onMetaData.
	 */
    private int fileMetaSize = 0;

    /**
	 * Id of the video codec used.
	 */
    private volatile int videoCodecId = -1;

    /**
	 * Id of the audio codec used.
	 */
    private volatile int audioCodecId = -1;

    /**
	 * Are we appending to an existing file?
	 */
    private boolean append;

    /**
	 * Duration of the file.
	 */
    private int duration;

    /**
	 * need direct access to file to append full duration metadata
	 */
    private RandomAccessFile file;

    private volatile int previousTagSize = 0;

    /**
	 * Creates writer implementation with given file and last tag
	 * 
	 * FLV.java uses this constructor so we have access to the file object
	 * 
	 * @param file
	 *            File output stream
	 * @param append
	 *            true if append to existing file
	 */
    public FLVWriter(File file, boolean append) {
        try {
            log.debug("Writing to: {}", file.getAbsolutePath());
            this.file = new RandomAccessFile(file, "rw");
            this.append = append;
            init();
        } catch (Exception e) {
            log.error("Failed to create FLV writer", e);
        }
    }

    /**
	 * Initialize the writer
	 */
    private void init() {
        if (!append) {
            try {
                writeHeader();
                writeMetadataTag(0, videoCodecId, audioCodecId);
            } catch (IOException e) {
                log.warn("Exception writing header or intermediate meta data", e);
            }
        }
    }

    /**
	 * Writes the header bytes
	 * 
	 * @throws IOException
	 *             Any I/O exception
	 */
    public void writeHeader() throws IOException {
        FLVHeader flvHeader = new FLVHeader();
        flvHeader.setFlagAudio(true);
        flvHeader.setFlagVideo(true);
        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4);
        flvHeader.write(header);
        file.setLength(HEADER_LENGTH + 4);
        if (header.hasArray()) {
            log.debug("Header bytebuffer has a backing array");
            file.write(header.array());
        } else {
            log.debug("Header bytebuffer does not have a backing array");
            byte[] tmp = new byte[HEADER_LENGTH + 4];
            header.get(tmp);
            file.write(tmp);
        }
        bytesWritten = file.length();
        header.clear();
    }

    /**
	 * {@inheritDoc}
	 */
    public IStreamableFile getFile() {
        return flv;
    }

    /**
	 * Sets the base file.
	 * 
	 * @param file
	 *            source flv
	 */
    public void setFile(File file) {
        try {
            this.file = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            log.warn("File could not be set", e);
        }
    }

    /**
	 * Setter for FLV object
	 * 
	 * @param flv
	 *            FLV source
	 * 
	 */
    public void setFLV(IFLV flv) {
        this.flv = flv;
    }

    /**
	 * {@inheritDoc}
	 */
    public int getOffset() {
        return offset;
    }

    /**
	 * Setter for offset
	 * 
	 * @param offset
	 *            Value to set for offset
	 */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
	 * {@inheritDoc}
	 */
    public long getBytesWritten() {
        return bytesWritten;
    }

    /**
	 * {@inheritDoc}
	 */
    public synchronized boolean writeTag(ITag tag) throws IOException {
        long prevBytesWritten = bytesWritten;
        int bodySize = tag.getBodySize();
        if (bodySize > 0) {
            if (file != null) {
                int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
                ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
                byte dataType = tag.getDataType();
                if (previousTagSize != tag.getPreviousTagSize() && dataType != ITag.TYPE_METADATA) {
                    tag.setPreviousTagSize(previousTagSize);
                }
                int timestamp = tag.getTimestamp() + offset;
                byte[] bodyBuf = new byte[bodySize];
                tag.getBody().get(bodyBuf);
                if (dataType == ITag.TYPE_AUDIO && audioCodecId == -1) {
                    int id = bodyBuf[0] & 0xff;
                    audioCodecId = (id & ITag.MASK_SOUND_FORMAT) >> 4;
                } else if (dataType == ITag.TYPE_VIDEO && videoCodecId == -1) {
                    int id = bodyBuf[0] & 0xff;
                    videoCodecId = id & ITag.MASK_VIDEO_CODEC;
                }
                tagBuffer.put(dataType);
                IOUtils.writeMediumInt(tagBuffer, bodySize);
                IOUtils.writeExtendedMediumInt(tagBuffer, timestamp);
                IOUtils.writeMediumInt(tagBuffer, 0);
                tagBuffer.put(bodyBuf);
                previousTagSize = TAG_HEADER_LENGTH + bodySize;
                tagBuffer.putInt(previousTagSize);
                tagBuffer.flip();
                file.write(tagBuffer.array());
                bytesWritten = file.length();
                tagBuffer.clear();
                duration = Math.max(duration, timestamp);
                if ((bytesWritten - prevBytesWritten) != (previousTagSize + 4)) {
                    log.debug("Not all of the bytes appear to have been written, prev-current: {}", (bytesWritten - prevBytesWritten));
                }
            } else {
                throw new IOException("FLV write channel has been closed and cannot be written to", new ClosedChannelException());
            }
        } else {
            log.debug("Empty tag skipped: {}", tag);
            return false;
        }
        return true;
    }

    /**
	 * {@inheritDoc}
	 */
    public boolean writeTag(byte type, IoBuffer data) throws IOException {
        return false;
    }

    /**
	 * {@inheritDoc}
	 */
    public void close() {
        log.debug("close");
        try {
            file.seek(0);
            FLVHeader flvHeader = new FLVHeader();
            flvHeader.setFlagAudio(audioCodecId != -1 ? true : false);
            flvHeader.setFlagVideo(videoCodecId != -1 ? true : false);
            ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH + 4);
            flvHeader.write(header);
            file.write(header.array());
            header.clear();
            log.debug("In the metadata writing (close) method - duration:{}", duration);
            file.seek(META_POSITION);
            writeMetadataTag(duration * 0.001, videoCodecId, audioCodecId);
        } catch (IOException e) {
            log.error("IO error on close", e);
        } finally {
            try {
                if (file != null) {
                    file.close();
                }
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }

    /** {@inheritDoc} */
    public boolean writeStream(byte[] b) {
        return false;
    }

    /**
	 * Write "onMetaData" tag to the file.
	 * 
	 * @param duration
	 *            Duration to write in milliseconds.
	 * @param videoCodecId
	 *            Id of the video codec used while recording.
	 * @param audioCodecId
	 *            Id of the audio codec used while recording.
	 * @throws IOException
	 *             if the tag could not be written
	 */
    private void writeMetadataTag(double duration, Integer videoCodecId, Integer audioCodecId) throws IOException {
        log.debug("writeMetadataTag - duration: {} video codec: {} audio codec: {}", new Object[] { duration, videoCodecId, audioCodecId });
        IoBuffer buf = IoBuffer.allocate(192);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onMetaData");
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("creationdate", GregorianCalendar.getInstance().getTime().toString());
        params.put("duration", duration);
        if (videoCodecId != null) {
            params.put("videocodecid", videoCodecId.intValue());
        }
        if (audioCodecId != null) {
            params.put("audiocodecid", audioCodecId.intValue());
        }
        params.put("canSeekToEnd", true);
        out.writeMap(params, new Serializer());
        buf.flip();
        if (fileMetaSize == 0) {
            fileMetaSize = buf.limit();
        }
        log.debug("Metadata size: {}", fileMetaSize);
        ITag onMetaData = new Tag(ITag.TYPE_METADATA, 0, fileMetaSize, buf, 0);
        writeTag(onMetaData);
    }

    public void testFLV() {
        log.debug("testFLV");
        try {
            ITagReader reader = null;
            if (flv != null) {
                reader = flv.getReader();
            }
            if (reader == null) {
                file.seek(0);
                reader = new FLVReader(file.getChannel());
            }
            log.trace("reader: {}", reader);
            log.debug("Has more tags: {}", reader.hasMoreTags());
            ITag tag = null;
            while (reader.hasMoreTags()) {
                tag = reader.readTag();
                log.debug("\n{}", tag);
            }
        } catch (IOException e) {
            log.warn("", e);
        }
    }
}
