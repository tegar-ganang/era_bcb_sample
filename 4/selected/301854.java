package org.red5.io.flv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.BufferType;
import org.red5.io.IKeyFrameMetaCache;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Reader is used to read the contents of a FLV file.
 * NOTE: This class is not implemented as threading-safe. The caller
 * should make sure the threading-safety.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class FLVReader implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

    /**
	 * Logger
	 */
    private static Logger log = LoggerFactory.getLogger(FLVReader.class);

    /**
	 * File
	 */
    private File file;

    /**
	 * File input stream
	 */
    private FileInputStream fis;

    /**
	 * File channel
	 */
    private FileChannel channel;

    private long channelSize;

    /**
	 * Keyframe metadata
	 */
    private KeyFrameMeta keyframeMeta;

    /**
	 * Input byte buffer
	 */
    private IoBuffer in;

    /** Set to true to generate metadata automatically before the first tag. */
    private boolean generateMetadata;

    /** Position of first video tag. */
    private long firstVideoTag = -1;

    /** Position of first audio tag. */
    private long firstAudioTag = -1;

    /** metadata sent flag */
    private boolean metadataSent = false;

    /** Duration in milliseconds. */
    private long duration;

    /** Mapping between file position and timestamp in ms. */
    private HashMap<Long, Long> posTimeMap;

    /** Buffer type / style to use **/
    private static BufferType bufferType = BufferType.AUTO;

    private static int bufferSize = 1024;

    /** Use load buffer */
    private boolean useLoadBuf;

    /** Cache for keyframe informations. */
    private static IKeyFrameMetaCache keyframeCache;

    /** The header of this FLV file. */
    private FLVHeader header;

    /** Constructs a new FLVReader. */
    FLVReader() {
    }

    /**
	 * Creates FLV reader from file input stream.
	 *
	 * @param f         File
	 * @throws IOException on error
	 */
    public FLVReader(File f) throws IOException {
        this(f, false);
    }

    /**
	 * Creates FLV reader from file input stream, sets up metadata generation flag.
	 *
	 * @param f                    File input stream
	 * @param generateMetadata     <code>true</code> if metadata generation required, <code>false</code> otherwise
	 * @throws IOException on error
	 */
    public FLVReader(File f, boolean generateMetadata) throws IOException {
        if (null == f) {
            log.warn("Reader was passed a null file");
            log.debug("{}", org.apache.commons.lang.builder.ToStringBuilder.reflectionToString(this));
        }
        this.file = f;
        this.fis = new FileInputStream(f);
        this.generateMetadata = generateMetadata;
        channel = fis.getChannel();
        channelSize = channel.size();
        in = null;
        fillBuffer();
        postInitialize();
    }

    /**
	 * Creates FLV reader from file channel.
	 *
	 * @param channel
	 * @throws IOException on error
	 */
    public FLVReader(FileChannel channel) throws IOException {
        if (null == channel) {
            log.warn("Reader was passed a null channel");
            log.debug("{}", org.apache.commons.lang.builder.ToStringBuilder.reflectionToString(this));
        }
        if (!channel.isOpen()) {
            log.warn("Reader was passed a closed channel");
            return;
        }
        this.channel = channel;
        channelSize = channel.size();
        if (channel.position() > 0) {
            log.debug("Channel position: {}", channel.position());
            channel.position(0);
        }
        fillBuffer();
        postInitialize();
    }

    /**
	 * Accepts mapped file bytes to construct internal members.
	 *
	 * @param generateMetadata         <code>true</code> if metadata generation required, <code>false</code> otherwise
	 * @param buffer                   IoBuffer
	 */
    public FLVReader(IoBuffer buffer, boolean generateMetadata) {
        this.generateMetadata = generateMetadata;
        in = buffer;
        postInitialize();
    }

    public void setKeyFrameCache(IKeyFrameMetaCache keyframeCache) {
        FLVReader.keyframeCache = keyframeCache;
    }

    /**
	 * Get the remaining bytes that could be read from a file or ByteBuffer.
	 *
	 * @return          Number of remaining bytes
	 */
    private long getRemainingBytes() {
        if (!useLoadBuf) {
            return in.remaining();
        }
        try {
            return channelSize - channel.position() + in.remaining();
        } catch (Exception e) {
            log.error("Error getRemainingBytes", e);
            return 0;
        }
    }

    /**
	 * Get the total readable bytes in a file or ByteBuffer.
	 *
	 * @return          Total readable bytes
	 */
    public long getTotalBytes() {
        if (!useLoadBuf) {
            return in.capacity();
        }
        try {
            return channelSize;
        } catch (Exception e) {
            log.error("Error getTotalBytes", e);
            return 0;
        }
    }

    /**
	 * Get the current position in a file or ByteBuffer.
	 *
	 * @return           Current position in a file
	 */
    private long getCurrentPosition() {
        long pos;
        if (!useLoadBuf) {
            return in.position();
        }
        try {
            if (in != null) {
                pos = (channel.position() - in.remaining());
            } else {
                pos = channel.position();
            }
            return pos;
        } catch (Exception e) {
            log.error("Error getCurrentPosition", e);
            return 0;
        }
    }

    /**
	 * Modifies current position.
	 *
	 * @param pos  Current position in file
	 */
    private void setCurrentPosition(long pos) {
        if (pos == Long.MAX_VALUE) {
            pos = file.length();
        }
        if (!useLoadBuf) {
            in.position((int) pos);
            return;
        }
        try {
            if (pos >= (channel.position() - in.limit()) && pos < channel.position()) {
                in.position((int) (pos - (channel.position() - in.limit())));
            } else {
                channel.position(pos);
                fillBuffer(bufferSize, true);
            }
        } catch (Exception e) {
            log.error("Error setCurrentPosition", e);
        }
    }

    /**
	 * Loads whole buffer from file channel, with no reloading (that is, appending).
	 */
    private void fillBuffer() {
        fillBuffer(bufferSize, false);
    }

    /**
	 * Loads data from channel to buffer.
	 *
	 * @param amount         Amount of data to load with no reloading
	 */
    private void fillBuffer(long amount) {
        fillBuffer(amount, false);
    }

    /**
	 * Load enough bytes from channel to buffer.
	 * After the loading process, the caller can make sure the amount
	 * in buffer is of size 'amount' if we haven't reached the end of channel.
	 *
	 * @param amount The amount of bytes in buffer after returning,
	 * no larger than bufferSize
	 * @param reload Whether to reload or append
	 */
    private void fillBuffer(long amount, boolean reload) {
        try {
            if (amount > bufferSize) {
                amount = bufferSize;
            }
            if (channelSize - channel.position() < amount) {
                amount = channelSize - channel.position();
            }
            if (in == null) {
                switch(bufferType) {
                    case HEAP:
                        in = IoBuffer.allocate(bufferSize, false);
                        break;
                    case DIRECT:
                        in = IoBuffer.allocate(bufferSize, true);
                        break;
                    default:
                        in = IoBuffer.allocate(bufferSize);
                }
                channel.read(in.buf());
                in.flip();
                useLoadBuf = true;
            }
            if (!useLoadBuf) {
                return;
            }
            if (reload || in.remaining() < amount) {
                if (!reload) {
                    in.compact();
                } else {
                    in.clear();
                }
                channel.read(in.buf());
                in.flip();
            }
        } catch (Exception e) {
            log.error("Error fillBuffer", e);
        }
    }

    /**
	 * Post-initialization hook, reads keyframe metadata and decodes header (if any).
	 */
    private void postInitialize() {
        if (getRemainingBytes() >= 9) {
            decodeHeader();
        }
        if (file != null) {
        }
    }

    /** {@inheritDoc} */
    public boolean hasVideo() {
        KeyFrameMeta meta = analyzeKeyFrames();
        if (meta == null) {
            return false;
        }
        return (!meta.audioOnly && meta.positions.length > 0);
    }

    /**
	 * Getter for buffer type (auto, direct or heap).
	 *
	 * @return Value for property 'bufferType'
	 */
    public static String getBufferType() {
        switch(bufferType) {
            case AUTO:
                return "auto";
            case DIRECT:
                return "direct";
            case HEAP:
                return "heap";
            default:
                return null;
        }
    }

    /**
	 * Setter for buffer type.
	 *
	 * @param bufferType Value to set for property 'bufferType'
	 */
    public static void setBufferType(String bufferType) {
        int bufferTypeHash = bufferType.hashCode();
        switch(bufferTypeHash) {
            case 3198444:
                FLVReader.bufferType = BufferType.HEAP;
                break;
            case -1331586071:
                FLVReader.bufferType = BufferType.DIRECT;
                break;
            case 3005871:
            default:
                FLVReader.bufferType = BufferType.AUTO;
        }
    }

    /**
	 * Getter for buffer size.
	 *
	 * @return Value for property 'bufferSize'
	 */
    public static int getBufferSize() {
        return bufferSize;
    }

    /**
	 * Setter for property 'bufferSize'.
	 *
	 * @param bufferSize Value to set for property 'bufferSize'
	 */
    public static void setBufferSize(int bufferSize) {
        if (bufferSize < 1024) {
            bufferSize = 1024;
        }
        FLVReader.bufferSize = bufferSize;
    }

    /**
	 * Returns the file buffer.
	 * 
	 * @return  File contents as byte buffer
	 */
    public IoBuffer getFileData() {
        return null;
    }

    /** {@inheritDoc} */
    public void decodeHeader() {
        fillBuffer(9);
        header = new FLVHeader();
        in.skip(3);
        header.setVersion(in.get());
        header.setTypeFlags(in.get());
        header.setDataOffset(in.getInt());
    }

    /** {@inheritDoc}
	 */
    public IStreamableFile getFile() {
        return null;
    }

    /** {@inheritDoc}
	 */
    public int getOffset() {
        return 0;
    }

    /** {@inheritDoc}
	 */
    public long getBytesRead() {
        return getCurrentPosition();
    }

    /** {@inheritDoc} */
    public long getDuration() {
        return duration;
    }

    public int getVideoCodecId() {
        KeyFrameMeta meta = analyzeKeyFrames();
        if (meta == null) {
            return -1;
        }
        long old = getCurrentPosition();
        setCurrentPosition(firstVideoTag);
        readTagHeader();
        fillBuffer(1);
        byte frametype = in.get();
        setCurrentPosition(old);
        return frametype & MASK_VIDEO_CODEC;
    }

    public int getAudioCodecId() {
        KeyFrameMeta meta = analyzeKeyFrames();
        if (meta == null) {
            return -1;
        }
        long old = getCurrentPosition();
        setCurrentPosition(firstAudioTag);
        readTagHeader();
        fillBuffer(1);
        byte frametype = in.get();
        setCurrentPosition(old);
        return frametype & MASK_SOUND_FORMAT;
    }

    /** {@inheritDoc}
	 */
    public boolean hasMoreTags() {
        return getRemainingBytes() > 4;
    }

    /**
	 * Create tag for metadata event.
	 *
	 * @return         Metadata event tag
	 */
    private ITag createFileMeta() {
        IoBuffer buf = IoBuffer.allocate(1024);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onMetaData");
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put("duration", duration / 1000.0);
        if (firstVideoTag != -1) {
            long old = getCurrentPosition();
            setCurrentPosition(firstVideoTag);
            readTagHeader();
            fillBuffer(1);
            byte frametype = in.get();
            props.put("videocodecid", frametype & MASK_VIDEO_CODEC);
            setCurrentPosition(old);
        }
        if (firstAudioTag != -1) {
            long old = getCurrentPosition();
            setCurrentPosition(firstAudioTag);
            readTagHeader();
            fillBuffer(1);
            byte frametype = in.get();
            props.put("audiocodecid", (frametype & MASK_SOUND_FORMAT) >> 4);
            setCurrentPosition(old);
        }
        props.put("canSeekToEnd", true);
        out.writeMap(props, new Serializer());
        buf.flip();
        ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
        result.setBody(buf);
        metadataSent = true;
        out = null;
        return result;
    }

    /** {@inheritDoc}
	 */
    public synchronized ITag readTag() {
        long oldPos = getCurrentPosition();
        ITag tag = readTagHeader();
        if (!metadataSent && tag.getDataType() != TYPE_METADATA && generateMetadata) {
            setCurrentPosition(oldPos);
            KeyFrameMeta meta = analyzeKeyFrames();
            if (meta != null) {
                return createFileMeta();
            }
        }
        int bodySize = tag.getBodySize();
        IoBuffer body = IoBuffer.allocate(bodySize, false);
        long newPosition = getCurrentPosition() + bodySize;
        if (newPosition <= getTotalBytes()) {
            int limit;
            while (getCurrentPosition() < newPosition) {
                fillBuffer(newPosition - getCurrentPosition());
                if (getCurrentPosition() + in.remaining() > newPosition) {
                    limit = in.limit();
                    in.limit((int) (newPosition - getCurrentPosition()) + in.position());
                    body.put(in);
                    in.limit(limit);
                } else {
                    body.put(in);
                }
            }
            body.flip();
            tag.setBody(body);
        }
        if (tag.getDataType() == TYPE_METADATA) {
            metadataSent = true;
        }
        return tag;
    }

    /** {@inheritDoc}
	 */
    public void close() {
        log.debug("Reader close");
        if (in != null) {
            in.free();
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

    /**
	 * Key frames analysis may be used as a utility method so
	 * synchronize it.
	 *
	 * @return             Keyframe metadata
	 */
    public synchronized KeyFrameMeta analyzeKeyFrames() {
        if (keyframeMeta != null) {
            return keyframeMeta;
        }
        if (keyframeCache != null) {
            keyframeMeta = keyframeCache.loadKeyFrameMeta(file);
            if (keyframeMeta != null) {
                duration = keyframeMeta.duration;
                posTimeMap = new HashMap<Long, Long>();
                for (int i = 0; i < keyframeMeta.positions.length; i++) {
                    posTimeMap.put(keyframeMeta.positions[i], (long) keyframeMeta.timestamps[i]);
                }
                return keyframeMeta;
            }
        }
        List<Long> positionList = new ArrayList<Long>();
        List<Integer> timestampList = new ArrayList<Integer>();
        List<Long> audioPositionList = new ArrayList<Long>();
        List<Integer> audioTimestampList = new ArrayList<Integer>();
        long origPos = getCurrentPosition();
        setCurrentPosition(9);
        boolean audioOnly = true;
        while (this.hasMoreTags()) {
            long pos = getCurrentPosition();
            ITag tmpTag = this.readTagHeader();
            duration = tmpTag.getTimestamp();
            if (tmpTag.getDataType() == IoConstants.TYPE_VIDEO) {
                if (audioOnly) {
                    audioOnly = false;
                    audioPositionList.clear();
                    audioTimestampList.clear();
                }
                if (firstVideoTag == -1) {
                    firstVideoTag = pos;
                }
                fillBuffer(1);
                byte frametype = in.get();
                if (((frametype & MASK_VIDEO_FRAMETYPE) >> 4) == FLAG_FRAMETYPE_KEYFRAME) {
                    positionList.add(pos);
                    timestampList.add(tmpTag.getTimestamp());
                }
            } else if (tmpTag.getDataType() == IoConstants.TYPE_AUDIO) {
                if (firstAudioTag == -1) {
                    firstAudioTag = pos;
                }
                if (audioOnly) {
                    audioPositionList.add(pos);
                    audioTimestampList.add(tmpTag.getTimestamp());
                }
            }
            long newPosition = pos + tmpTag.getBodySize() + 15;
            if (newPosition >= getTotalBytes()) {
                log.error("New position exceeds limit");
                break;
            } else {
                setCurrentPosition(newPosition);
            }
        }
        setCurrentPosition(origPos);
        keyframeMeta = new KeyFrameMeta();
        keyframeMeta.duration = duration;
        posTimeMap = new HashMap<Long, Long>();
        if (audioOnly) {
            positionList = audioPositionList;
            timestampList = audioTimestampList;
        }
        keyframeMeta.audioOnly = audioOnly;
        keyframeMeta.positions = new long[positionList.size()];
        keyframeMeta.timestamps = new int[timestampList.size()];
        for (int i = 0; i < keyframeMeta.positions.length; i++) {
            keyframeMeta.positions[i] = positionList.get(i);
            keyframeMeta.timestamps[i] = timestampList.get(i);
            posTimeMap.put((long) positionList.get(i), (long) timestampList.get(i));
        }
        if (keyframeCache != null) {
            keyframeCache.saveKeyFrameMeta(file, keyframeMeta);
        }
        return keyframeMeta;
    }

    /**
	 * Put the current position to pos.
	 * The caller must ensure the pos is a valid one
	 * (eg. not sit in the middle of a frame).
	 *
	 * @param pos         New position in file. Pass <code>Long.MAX_VALUE</code> to seek to end of file.
	 */
    public void position(long pos) {
        setCurrentPosition(pos);
    }

    /**
	 * Read only header part of a tag.
	 *
	 * @return              Tag header
	 */
    private ITag readTagHeader() {
        fillBuffer(15);
        int previousTagSize = in.getInt();
        byte dataType = in.get();
        if (dataType != 8 && dataType != 9 && dataType != 18) {
            log.debug("Invalid data type detected, skipping crap-bytes");
            in.skip(51);
            dataType = in.get();
        }
        int bodySize = IOUtils.readUnsignedMediumInt(in);
        int timestamp = IOUtils.readExtendedMediumInt(in);
        if (log.isDebugEnabled()) {
        } else {
            in.skip(3);
        }
        return new Tag(dataType, timestamp, bodySize, null, previousTagSize);
    }

    public static long getDuration(File flvFile) {
        RandomAccessFile flv = null;
        try {
            flv = new RandomAccessFile(flvFile, "r");
            long flvLength = flv.length();
            if (flvLength < 13) {
                return 0;
            }
            flv.seek(flvLength - 4);
            byte[] buf = new byte[4];
            flv.read(buf);
            long lastTagSize = 0;
            for (int i = 0; i < 4; i++) {
                lastTagSize += (buf[i] & 0x0ff) << ((3 - i) * 8);
            }
            if (lastTagSize == 0) {
                return 0;
            }
            flv.seek(flvLength - lastTagSize);
            flv.read(buf);
            long duration = 0;
            for (int i = 0; i < 3; i++) {
                duration += (buf[i] & 0x0ff) << ((2 - i) * 8);
            }
            duration += (buf[3] & 0x0ff) << 24;
            return duration;
        } catch (IOException e) {
            return 0;
        } finally {
            try {
                if (flv != null) {
                    flv.close();
                }
            } catch (IOException e) {
            }
            flv = null;
        }
    }
}
