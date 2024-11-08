package org.red5.io.mp3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IKeyFrameMetaCache;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.Tag;
import org.red5.io.object.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read MP3 files
 */
public class MP3Reader implements ITagReader, IKeyFrameDataAnalyzer {

    /**
	 * Logger
	 */
    protected static Logger log = LoggerFactory.getLogger(MP3Reader.class);

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

    /**
	 * Memory-mapped buffer for file content
	 */
    private MappedByteBuffer mappedFile;

    /**
	 * Source byte buffer
	 */
    private IoBuffer in;

    /**
	 * Last read tag object
	 */
    private ITag tag;

    /**
	 * Previous tag size
	 */
    private int prevSize;

    /**
	 * Current time
	 */
    private double currentTime;

    /**
	 * Frame metadata
	 */
    private KeyFrameMeta frameMeta;

    /**
	 * Positions and time map
	 */
    private HashMap<Integer, Double> posTimeMap;

    private int dataRate;

    /**
	 * File duration
	 */
    private long duration;

    /**
	 * Frame cache
	 */
    private static IKeyFrameMetaCache frameCache;

    /**
	 * Holder for ID3 meta data
	 */
    private MetaData metaData;

    /**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
    private LinkedList<ITag> firstTags = new LinkedList<ITag>();

    MP3Reader() {
    }

    /**
	 * Creates reader from file input stream
	 * @param file file input
	 * 
	 * @throws FileNotFoundException if not found 
	 */
    public MP3Reader(File file) throws FileNotFoundException {
        this.file = file;
        fis = new FileInputStream(file);
        channel = fis.getChannel();
        try {
            mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } catch (IOException e) {
            log.error("MP3Reader {}", e);
        }
        mappedFile.order(ByteOrder.BIG_ENDIAN);
        in = IoBuffer.wrap(mappedFile);
        analyzeKeyFrames();
        firstTags.addFirst(createFileMeta());
        if (in.remaining() > 4) {
            searchNextFrame();
            int pos = in.position();
            MP3Header header = readHeader();
            in.position(pos);
            if (header != null) {
                checkValidHeader(header);
            } else {
                throw new RuntimeException("No initial header found.");
            }
        }
    }

    /**
	 * A MP3 stream never has video.
	 * 
	 * @return always returns <code>false</code>
	 */
    public boolean hasVideo() {
        return false;
    }

    public void setFrameCache(IKeyFrameMetaCache frameCache) {
        MP3Reader.frameCache = frameCache;
    }

    /**
	 * Check if the file can be played back with Flash. Supported sample rates
	 * are 44KHz, 22KHz, 11KHz and 5.5KHz
	 * 
	 * @param header
	 *            Header to check
	 */
    private void checkValidHeader(MP3Header header) {
        switch(header.getSampleRate()) {
            case 48000:
            case 44100:
            case 22050:
            case 11025:
            case 5513:
                break;
            default:
                throw new RuntimeException("Unsupported sample rate: " + header.getSampleRate());
        }
    }

    /**
	 * Creates file metadata object
	 * 
	 * @return Tag
	 */
    private ITag createFileMeta() {
        IoBuffer buf = IoBuffer.allocate(1024);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onMetaData");
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put("duration", frameMeta.timestamps[frameMeta.timestamps.length - 1] / 1000.0);
        props.put("audiocodecid", IoConstants.FLAG_FORMAT_MP3);
        if (dataRate > 0) {
            props.put("audiodatarate", dataRate);
        }
        props.put("canSeekToEnd", true);
        if (metaData != null) {
            props.put("artist", metaData.getArtist());
            props.put("album", metaData.getAlbum());
            props.put("songName", metaData.getSongName());
            props.put("genre", metaData.getGenre());
            props.put("year", metaData.getYear());
            props.put("track", metaData.getTrack());
            props.put("comment", metaData.getComment());
            if (metaData.hasCoverImage()) {
                Map<Object, Object> covr = new HashMap<Object, Object>(1);
                covr.put("covr", new Object[] { metaData.getCovr() });
                props.put("tags", covr);
            }
            metaData = null;
        }
        out.writeMap(props, new Serializer());
        buf.flip();
        ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, prevSize);
        result.setBody(buf);
        return result;
    }

    /** Search for next frame sync word. Sync word identifies valid frame. */
    public void searchNextFrame() {
        while (in.remaining() > 1) {
            int ch = in.get() & 0xff;
            if (ch != 0xff) {
                continue;
            }
            if ((in.get() & 0xe0) == 0xe0) {
                in.position(in.position() - 2);
                return;
            }
        }
    }

    /** {@inheritDoc} */
    public IStreamableFile getFile() {
        return null;
    }

    /** {@inheritDoc} */
    public int getOffset() {
        return 0;
    }

    /** {@inheritDoc} */
    public long getBytesRead() {
        return in.position();
    }

    /** {@inheritDoc} */
    public long getDuration() {
        return duration;
    }

    /**
	 * Get the total readable bytes in a file or ByteBuffer.
	 * 
	 * @return Total readable bytes
	 */
    public long getTotalBytes() {
        return in.capacity();
    }

    /** {@inheritDoc} */
    public boolean hasMoreTags() {
        MP3Header header = null;
        while (header == null && in.remaining() > 4) {
            try {
                header = new MP3Header(in.getInt());
            } catch (IOException e) {
                log.error("MP3Reader :: hasMoreTags ::>\n", e);
                break;
            } catch (Exception e) {
                searchNextFrame();
            }
        }
        if (header == null) {
            return false;
        }
        if (header.frameSize() == 0) {
            return false;
        }
        if (in.position() + header.frameSize() - 4 > in.limit()) {
            in.position(in.limit());
            return false;
        }
        in.position(in.position() - 4);
        return true;
    }

    private MP3Header readHeader() {
        MP3Header header = null;
        while (header == null && in.remaining() > 4) {
            try {
                header = new MP3Header(in.getInt());
            } catch (IOException e) {
                log.error("MP3Reader :: readTag ::>\n", e);
                break;
            } catch (Exception e) {
                searchNextFrame();
            }
        }
        return header;
    }

    /** {@inheritDoc} */
    public synchronized ITag readTag() {
        if (!firstTags.isEmpty()) {
            return firstTags.removeFirst();
        }
        MP3Header header = readHeader();
        if (header == null) {
            return null;
        }
        int frameSize = header.frameSize();
        if (frameSize == 0) {
            return null;
        }
        if (in.position() + frameSize - 4 > in.limit()) {
            in.position(in.limit());
            return null;
        }
        tag = new Tag(IoConstants.TYPE_AUDIO, (int) currentTime, frameSize + 1, null, prevSize);
        prevSize = frameSize + 1;
        currentTime += header.frameDuration();
        IoBuffer body = IoBuffer.allocate(tag.getBodySize());
        body.setAutoExpand(true);
        byte tagType = (IoConstants.FLAG_FORMAT_MP3 << 4) | (IoConstants.FLAG_SIZE_16_BIT << 1);
        switch(header.getSampleRate()) {
            case 44100:
                tagType |= IoConstants.FLAG_RATE_44_KHZ << 2;
                break;
            case 22050:
                tagType |= IoConstants.FLAG_RATE_22_KHZ << 2;
                break;
            case 11025:
                tagType |= IoConstants.FLAG_RATE_11_KHZ << 2;
                break;
            default:
                tagType |= IoConstants.FLAG_RATE_5_5_KHZ << 2;
        }
        tagType |= (header.isStereo() ? IoConstants.FLAG_TYPE_STEREO : IoConstants.FLAG_TYPE_MONO);
        body.put(tagType);
        final int limit = in.limit();
        body.putInt(header.getData());
        in.limit(in.position() + frameSize - 4);
        body.put(in);
        body.flip();
        in.limit(limit);
        tag.setBody(body);
        return tag;
    }

    /** {@inheritDoc} */
    public void close() {
        if (posTimeMap != null) {
            posTimeMap.clear();
        }
        mappedFile.clear();
        if (in != null) {
            in.free();
            in = null;
        }
        try {
            fis.close();
            channel.close();
        } catch (IOException e) {
            log.error("MP3Reader :: close ::>\n", e);
        }
    }

    /** {@inheritDoc} */
    public void decodeHeader() {
    }

    /** {@inheritDoc} */
    public void position(long pos) {
        if (pos == Long.MAX_VALUE) {
            in.position(in.limit());
            currentTime = duration;
            return;
        }
        in.position((int) pos);
        searchNextFrame();
        analyzeKeyFrames();
        Double time = posTimeMap.get(in.position());
        if (time != null) {
            currentTime = time;
        } else {
            currentTime = 0;
        }
    }

    /** {@inheritDoc} */
    public synchronized KeyFrameMeta analyzeKeyFrames() {
        if (frameMeta != null) {
            return frameMeta;
        }
        if (frameCache != null) {
            frameMeta = frameCache.loadKeyFrameMeta(file);
            if (frameMeta != null && frameMeta.duration > 0) {
                duration = frameMeta.duration;
                frameMeta.audioOnly = true;
                posTimeMap = new HashMap<Integer, Double>();
                for (int i = 0; i < frameMeta.positions.length; i++) {
                    posTimeMap.put((int) frameMeta.positions[i], (double) frameMeta.timestamps[i]);
                }
                return frameMeta;
            }
        }
        List<Integer> positionList = new ArrayList<Integer>();
        List<Double> timestampList = new ArrayList<Double>();
        dataRate = 0;
        long rate = 0;
        int count = 0;
        int origPos = in.position();
        double time = 0;
        in.position(0);
        searchNextFrame();
        while (this.hasMoreTags()) {
            MP3Header header = readHeader();
            if (header == null) {
                break;
            }
            if (header.frameSize() == 0) {
                break;
            }
            int pos = in.position() - 4;
            if (pos + header.frameSize() > in.limit()) {
                break;
            }
            positionList.add(pos);
            timestampList.add(time);
            rate += header.getBitRate() / 1000;
            time += header.frameDuration();
            in.position(pos + header.frameSize());
            count++;
        }
        in.position(origPos);
        duration = (long) time;
        dataRate = (int) (rate / count);
        posTimeMap = new HashMap<Integer, Double>();
        frameMeta = new KeyFrameMeta();
        frameMeta.duration = duration;
        frameMeta.positions = new long[positionList.size()];
        frameMeta.timestamps = new int[timestampList.size()];
        frameMeta.audioOnly = true;
        for (int i = 0; i < frameMeta.positions.length; i++) {
            frameMeta.positions[i] = positionList.get(i);
            frameMeta.timestamps[i] = timestampList.get(i).intValue();
            posTimeMap.put(positionList.get(i), timestampList.get(i));
        }
        if (frameCache != null) {
            frameCache.saveKeyFrameMeta(file, frameMeta);
        }
        return frameMeta;
    }

    /**
	 * Simple holder for id3 meta data
	 */
    static class MetaData {

        String album = "";

        String artist = "";

        String genre = "";

        String songName = "";

        String track = "";

        String year = "";

        String comment = "";

        byte[] covr = null;

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getSongName() {
            return songName;
        }

        public void setSongName(String songName) {
            this.songName = songName;
        }

        public String getTrack() {
            return track;
        }

        public void setTrack(String track) {
            this.track = track;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public byte[] getCovr() {
            return covr;
        }

        public void setCovr(byte[] covr) {
            this.covr = covr;
            log.debug("Cover image array size: {}", covr.length);
        }

        public boolean hasCoverImage() {
            return covr != null;
        }
    }
}
