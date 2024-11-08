package org.red5.io.flv.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.FLVHeader;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.utils.IOUtils;

/**
 * A Reader is used to read the contents of a FLV file
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class FLVReader implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

    private static Log log = LogFactory.getLog(FLVReader.class.getName());

    private FileInputStream fis = null;

    private FileChannel channel = null;

    private MappedByteBuffer mappedFile = null;

    private KeyFrameMeta keyframeMeta = null;

    private ByteBuffer in = null;

    /** Set to true to generate metadata automatically before the first tag. */
    private boolean generateMetadata = false;

    /** Position of first video tag. */
    private int firstVideoTag = -1;

    /** Position of first audio tag. */
    private int firstAudioTag = -1;

    /** Current tag. */
    private int tagPosition = 0;

    /** Duration in milliseconds. */
    private long duration = 0;

    /** Mapping between file position and timestamp in ms. */
    private HashMap<Long, Long> posTimeMap = null;

    /** Mapping between file position and tag number. */
    private HashMap<Long, Integer> posTagMap = null;

    public FLVReader(FileInputStream f) {
        this(f, false);
    }

    public FLVReader(FileInputStream f, boolean generateMetadata) {
        this.fis = f;
        this.generateMetadata = generateMetadata;
        channel = fis.getChannel();
        try {
            mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            mappedFile.order(ByteOrder.BIG_ENDIAN);
        } catch (IOException e) {
            log.error("FLVReader :: FLVReader ::>\n", e);
        }
        in = ByteBuffer.wrap(mappedFile);
        log.debug("FLVReader 1 - Buffer size: " + in.capacity() + " position: " + in.position() + " remaining: " + in.remaining());
        if (in.remaining() >= 9) {
            decodeHeader();
        }
        keyframeMeta = analyzeKeyFrames();
    }

    /**
	 * Accepts mapped file bytes to construct internal members.
	 * 
	 * @param mappedFile
	 * @param generateMetadata
	 */
    public FLVReader(ByteBuffer buffer, boolean generateMetadata) {
        this.generateMetadata = generateMetadata;
        in = buffer;
        log.debug("FLVReader 2 - Buffer size: " + in.capacity() + " position: " + in.position() + " remaining: " + in.remaining());
        if (in.remaining() >= 9) {
            decodeHeader();
        }
        keyframeMeta = analyzeKeyFrames();
    }

    /**
	 * Returns the file buffer.
	 * 
	 * @return file bytes
	 */
    public ByteBuffer getFileData() {
        return in.asReadOnlyBuffer();
    }

    public void decodeHeader() {
        FLVHeader header = new FLVHeader();
        in.skip(3);
        header.setVersion(in.get());
        header.setTypeFlags(in.get());
        header.setDataOffset(in.getInt());
        log.debug("Header: " + header.toString());
    }

    public IStreamableFile getFile() {
        return null;
    }

    public int getOffset() {
        return 0;
    }

    public synchronized long getBytesRead() {
        return in.position();
    }

    public synchronized long getDuration() {
        return duration;
    }

    public synchronized boolean hasMoreTags() {
        return in.remaining() > 4;
    }

    private ITag createFileMeta() {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onMetaData");
        out.writeStartMap(3);
        out.writePropertyName("duration");
        out.writeNumber(duration / 1000.0);
        if (firstVideoTag != -1) {
            int old = in.position();
            in.position(firstVideoTag);
            readTagHeader();
            byte frametype = in.get();
            out.writePropertyName("videocodecid");
            out.writeNumber(frametype & MASK_VIDEO_CODEC);
            in.position(old);
        }
        if (firstAudioTag != -1) {
            int old = in.position();
            in.position(firstAudioTag);
            readTagHeader();
            byte frametype = in.get();
            out.writePropertyName("audiocodecid");
            out.writeNumber((frametype & MASK_SOUND_FORMAT) >> 4);
            in.position(old);
        }
        out.writePropertyName("canSeekToEnd");
        out.writeBoolean(true);
        out.markEndMap();
        buf.flip();
        ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
        result.setBody(buf);
        return result;
    }

    public synchronized ITag readTag() {
        int oldPos = in.position();
        ITag tag = readTagHeader();
        if (tagPosition == 0 && tag.getDataType() != TYPE_METADATA && generateMetadata) {
            in.position(oldPos);
            KeyFrameMeta meta = analyzeKeyFrames();
            tagPosition++;
            if (meta != null) {
                return createFileMeta();
            }
        }
        ByteBuffer body = ByteBuffer.allocate(tag.getBodySize());
        final int limit = in.limit();
        int newPosition = in.position() + tag.getBodySize();
        if (newPosition <= limit) {
            in.limit(newPosition);
            body.put(in);
            body.flip();
            in.limit(limit);
            tag.setBody(body);
            tagPosition++;
        }
        return tag;
    }

    public synchronized void close() {
        log.debug("Reader close");
        if (mappedFile != null) {
            mappedFile.clear();
            mappedFile = null;
        }
        if (in != null) {
            in.release();
            in = null;
        }
        if (channel != null) {
            try {
                channel.close();
                fis.close();
            } catch (IOException e) {
                log.error("FLVReader :: close ::>\n", e);
            }
        }
    }

    public synchronized KeyFrameMeta analyzeKeyFrames() {
        if (keyframeMeta != null) {
            return keyframeMeta;
        }
        List<Integer> positionList = new ArrayList<Integer>();
        List<Integer> timestampList = new ArrayList<Integer>();
        int origPos = in.position();
        in.position(9);
        posTagMap = new HashMap<Long, Integer>();
        int idx = 0;
        while (this.hasMoreTags()) {
            int pos = in.position();
            posTagMap.put((long) pos, idx++);
            ITag tmpTag = this.readTagHeader();
            duration = tmpTag.getTimestamp();
            if (tmpTag.getDataType() == IoConstants.TYPE_VIDEO) {
                if (firstVideoTag == -1) {
                    firstVideoTag = pos;
                }
                byte frametype = in.get();
                if (((frametype & MASK_VIDEO_FRAMETYPE) >> 4) == FLAG_FRAMETYPE_KEYFRAME) {
                    positionList.add(pos);
                    timestampList.add(tmpTag.getTimestamp());
                }
            } else if (tmpTag.getDataType() == IoConstants.TYPE_AUDIO) {
                if (firstAudioTag == -1) {
                    firstAudioTag = pos;
                }
            }
            int newPosition = (pos + tmpTag.getBodySize() + 15);
            if (newPosition >= in.limit()) {
                log.info("New position exceeds limit");
                if (log.isDebugEnabled()) {
                    log.debug("-----");
                    log.debug("Keyframe analysis");
                    log.debug(" data type=" + tmpTag.getDataType() + " bodysize=" + tmpTag.getBodySize());
                    log.debug(" remaining=" + in.remaining() + " limit=" + in.limit() + " new pos=" + newPosition);
                    log.debug(" pos=" + pos);
                    log.debug("-----");
                }
                break;
            } else {
                in.position(newPosition);
            }
        }
        in.position(origPos);
        keyframeMeta = new KeyFrameMeta();
        posTimeMap = new HashMap<Long, Long>();
        keyframeMeta.positions = new int[positionList.size()];
        keyframeMeta.timestamps = new int[timestampList.size()];
        for (int i = 0; i < keyframeMeta.positions.length; i++) {
            keyframeMeta.positions[i] = positionList.get(i);
            keyframeMeta.timestamps[i] = timestampList.get(i);
            posTimeMap.put((long) positionList.get(i), (long) timestampList.get(i));
        }
        return keyframeMeta;
    }

    public synchronized void position(long pos) {
        in.position((int) pos);
        analyzeKeyFrames();
        Integer tag = posTagMap.get(pos);
        if (tag == null) {
            return;
        }
        tagPosition = tag;
    }

    /**
	 * Read only header part of a tag
	 *
	 * @return
	 */
    private ITag readTagHeader() {
        int previousTagSize = in.getInt();
        byte dataType = in.get();
        int bodySize = IOUtils.readUnsignedMediumInt(in);
        int timestamp = IOUtils.readUnsignedMediumInt(in);
        in.getInt();
        return new Tag(dataType, timestamp, bodySize, null, previousTagSize);
    }
}
