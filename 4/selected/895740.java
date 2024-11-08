package org.red5.io.m4a.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.impl.Tag;
import org.red5.io.mp4.MP4Atom;
import org.red5.io.mp4.MP4DataStream;
import org.red5.io.mp4.MP4Descriptor;
import org.red5.io.mp4.MP4Frame;
import org.red5.io.mp4.impl.MP4Reader;
import org.red5.io.object.Serializer;
import org.red5.io.utils.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Reader is used to read the contents of a M4A file.
 * NOTE: This class is not implemented as threading-safe. The caller
 * should make sure the threading-safety.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class M4AReader implements IoConstants, ITagReader {

    /**
     * Logger
     */
    private static Logger log = LoggerFactory.getLogger(M4AReader.class);

    /**
     * File
     */
    private File file;

    /**
     * Input stream
     */
    private MP4DataStream fis;

    /**
     * File channel
     */
    private FileChannel channel;

    /**
     * Memory-mapped buffer for file content
     */
    private MappedByteBuffer mappedFile;

    /**
     * Input byte buffer
     */
    private IoBuffer in;

    private String audioCodecId = "mp4a";

    private byte[] audioDecoderBytes;

    /** Duration in milliseconds. */
    private long duration;

    private int timeScale;

    private double audioTimeScale;

    private int audioChannels;

    private int audioCodecType = 1;

    private String formattedDuration;

    private long moovOffset;

    private long mdatOffset;

    private Vector<MP4Atom.Record> audioSamplesToChunks;

    private Vector<Integer> audioSamples;

    private Vector<Long> audioChunkOffsets;

    private int audioSampleDuration = 1024;

    private int currentFrame = 1;

    private int prevFrameSize = 0;

    private List<MP4Frame> frames = new ArrayList<MP4Frame>();

    /**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
    private LinkedList<ITag> firstTags = new LinkedList<ITag>();

    /** Constructs a new M4AReader. */
    M4AReader() {
    }

    /**
     * Creates M4A reader from file input stream, sets up metadata generation flag.
	 *
     * @param f                    File input stream
     */
    public M4AReader(File f) throws IOException {
        if (null == f) {
            log.warn("Reader was passed a null file");
            log.debug("{}", ToStringBuilder.reflectionToString(this));
        }
        this.file = f;
        this.fis = new MP4DataStream(new FileInputStream(f));
        channel = fis.getChannel();
        try {
            mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } catch (IOException e) {
            log.error("M4AReader {}", e);
        }
        in = IoBuffer.wrap(mappedFile);
        decodeHeader();
        analyzeFrames();
        firstTags.add(createFileMeta());
        createPreStreamingTags();
    }

    /**
	 * Accepts mapped file bytes to construct internal members.
	 *
     * @param buffer                   Byte buffer
	 */
    public M4AReader(IoBuffer buffer) throws IOException {
        in = buffer;
        decodeHeader();
        analyzeFrames();
        firstTags.add(createFileMeta());
        createPreStreamingTags();
    }

    /**
	 * This handles the moov atom being at the beginning or end of the file, so the mdat may also
	 * be before or after the moov atom.
	 */
    public void decodeHeader() {
        try {
            MP4Atom type = MP4Atom.createAtom(fis);
            log.debug("Type {}", MP4Atom.intToType(type.getType()));
            int topAtoms = 0;
            while (topAtoms < 2) {
                MP4Atom atom = MP4Atom.createAtom(fis);
                switch(atom.getType()) {
                    case 1836019574:
                        topAtoms++;
                        MP4Atom moov = atom;
                        log.debug("Type {}", MP4Atom.intToType(moov.getType()));
                        log.debug("moov children: {}", moov.getChildren());
                        moovOffset = fis.getOffset() - moov.getSize();
                        MP4Atom mvhd = moov.lookup(MP4Atom.typeToInt("mvhd"), 0);
                        if (mvhd != null) {
                            log.debug("Movie header atom found");
                            timeScale = mvhd.getTimeScale();
                            duration = mvhd.getDuration();
                            log.debug("Time scale {} Duration {}", timeScale, duration);
                        }
                        MP4Atom trak = moov.lookup(MP4Atom.typeToInt("trak"), 0);
                        if (trak != null) {
                            log.debug("Track atom found");
                            log.debug("trak children: {}", trak.getChildren());
                            MP4Atom edts = trak.lookup(MP4Atom.typeToInt("edts"), 0);
                            if (edts != null) {
                                log.debug("Edit atom found");
                                log.debug("edts children: {}", edts.getChildren());
                            }
                            MP4Atom mdia = trak.lookup(MP4Atom.typeToInt("mdia"), 0);
                            if (mdia != null) {
                                log.debug("Media atom found");
                                int scale = 0;
                                MP4Atom mdhd = mdia.lookup(MP4Atom.typeToInt("mdhd"), 0);
                                if (mdhd != null) {
                                    log.debug("Media data header atom found");
                                    scale = mdhd.getTimeScale();
                                    log.debug("Time scale {}", scale);
                                }
                                MP4Atom hdlr = mdia.lookup(MP4Atom.typeToInt("hdlr"), 0);
                                if (hdlr != null) {
                                    log.debug("Handler ref atom found");
                                    log.debug("Handler type: {}", MP4Atom.intToType(hdlr.getHandlerType()));
                                    String hdlrType = MP4Atom.intToType(hdlr.getHandlerType());
                                    if ("soun".equals(hdlrType)) {
                                        if (scale > 0) {
                                            audioTimeScale = scale * 1.0;
                                            log.debug("Audio time scale: {}", audioTimeScale);
                                        }
                                    }
                                }
                                MP4Atom minf = mdia.lookup(MP4Atom.typeToInt("minf"), 0);
                                if (minf != null) {
                                    log.debug("Media info atom found");
                                    MP4Atom smhd = minf.lookup(MP4Atom.typeToInt("smhd"), 0);
                                    if (smhd != null) {
                                        log.debug("Sound header atom found");
                                        MP4Atom dinf = minf.lookup(MP4Atom.typeToInt("dinf"), 0);
                                        if (dinf != null) {
                                            log.debug("Data info atom found");
                                            log.debug("Sound dinf children: {}", dinf.getChildren());
                                            MP4Atom dref = dinf.lookup(MP4Atom.typeToInt("dref"), 0);
                                            if (dref != null) {
                                                log.debug("Data reference atom found");
                                            }
                                        }
                                        MP4Atom stbl = minf.lookup(MP4Atom.typeToInt("stbl"), 0);
                                        if (stbl != null) {
                                            log.debug("Sample table atom found");
                                            log.debug("Sound stbl children: {}", stbl.getChildren());
                                            MP4Atom stsd = stbl.lookup(MP4Atom.typeToInt("stsd"), 0);
                                            if (stsd != null) {
                                                log.debug("Sample description atom found");
                                                MP4Atom mp4a = stsd.getChildren().get(0);
                                                setAudioCodecId(MP4Atom.intToType(mp4a.getType()));
                                                log.debug("Sample size: {}", mp4a.getSampleSize());
                                                int ats = mp4a.getTimeScale();
                                                if (ats > 0) {
                                                    audioTimeScale = ats * 1.0;
                                                }
                                                audioChannels = mp4a.getChannelCount();
                                                log.debug("Sample rate (audio time scale): {}", audioTimeScale);
                                                log.debug("Channels: {}", audioChannels);
                                                if (mp4a.getChildren().size() > 0) {
                                                    log.debug("Elementary stream descriptor atom found");
                                                    MP4Atom esds = mp4a.getChildren().get(0);
                                                    log.debug("{}", ToStringBuilder.reflectionToString(esds));
                                                    MP4Descriptor descriptor = esds.getEsd_descriptor();
                                                    log.debug("{}", ToStringBuilder.reflectionToString(descriptor));
                                                    if (descriptor != null) {
                                                        Vector<MP4Descriptor> children = descriptor.getChildren();
                                                        for (int e = 0; e < children.size(); e++) {
                                                            MP4Descriptor descr = children.get(e);
                                                            log.debug("{}", ToStringBuilder.reflectionToString(descr));
                                                            if (descr.getChildren().size() > 0) {
                                                                Vector<MP4Descriptor> children2 = descr.getChildren();
                                                                for (int e2 = 0; e2 < children2.size(); e2++) {
                                                                    MP4Descriptor descr2 = children2.get(e2);
                                                                    log.debug("{}", ToStringBuilder.reflectionToString(descr2));
                                                                    if (descr2.getType() == MP4Descriptor.MP4DecSpecificInfoDescriptorTag) {
                                                                        audioDecoderBytes = descr2.getDSID();
                                                                        switch(audioDecoderBytes[0]) {
                                                                            case 0x12:
                                                                            default:
                                                                                audioCodecType = 1;
                                                                                break;
                                                                            case 0x0a:
                                                                                audioCodecType = 0;
                                                                                break;
                                                                            case 0x11:
                                                                            case 0x13:
                                                                                audioCodecType = 2;
                                                                                break;
                                                                        }
                                                                        e = 99;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            MP4Atom stsc = stbl.lookup(MP4Atom.typeToInt("stsc"), 0);
                                            if (stsc != null) {
                                                log.debug("Sample to chunk atom found");
                                                audioSamplesToChunks = stsc.getRecords();
                                                log.debug("Record count: {}", audioSamplesToChunks.size());
                                                MP4Atom.Record rec = audioSamplesToChunks.firstElement();
                                                log.debug("Record data: Description index={} Samples per chunk={}", rec.getSampleDescriptionIndex(), rec.getSamplesPerChunk());
                                            }
                                            MP4Atom stsz = stbl.lookup(MP4Atom.typeToInt("stsz"), 0);
                                            if (stsz != null) {
                                                log.debug("Sample size atom found");
                                                audioSamples = stsz.getSamples();
                                                log.debug("Sample size: {}", stsz.getSampleSize());
                                                log.debug("Sample count: {}", audioSamples.size());
                                            }
                                            MP4Atom stco = stbl.lookup(MP4Atom.typeToInt("stco"), 0);
                                            if (stco != null) {
                                                log.debug("Chunk offset atom found");
                                                audioChunkOffsets = stco.getChunks();
                                                log.debug("Chunk count: {}", audioChunkOffsets.size());
                                            }
                                            MP4Atom stts = stbl.lookup(MP4Atom.typeToInt("stts"), 0);
                                            if (stts != null) {
                                                log.debug("Time to sample atom found");
                                                Vector<MP4Atom.TimeSampleRecord> records = stts.getTimeToSamplesRecords();
                                                log.debug("Record count: {}", records.size());
                                                MP4Atom.TimeSampleRecord rec = records.firstElement();
                                                log.debug("Record data: Consecutive samples={} Duration={}", rec.getConsecutiveSamples(), rec.getSampleDuration());
                                                if (records.size() > 1) {
                                                    log.warn("Audio samples have differing durations, audio playback may fail");
                                                }
                                                audioSampleDuration = rec.getSampleDuration();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        double clipTime = ((double) duration / (double) timeScale);
                        log.debug("Clip time: {}", clipTime);
                        int minutes = (int) (clipTime / 60);
                        if (minutes > 0) {
                            sb.append(minutes);
                            sb.append('.');
                        }
                        NumberFormat df = DecimalFormat.getInstance();
                        df.setMaximumFractionDigits(2);
                        sb.append(df.format((clipTime % 60)));
                        formattedDuration = sb.toString();
                        log.debug("Time: {}", formattedDuration);
                        break;
                    case 1835295092:
                        topAtoms++;
                        long dataSize = 0L;
                        MP4Atom mdat = atom;
                        dataSize = mdat.getSize();
                        log.debug("{}", ToStringBuilder.reflectionToString(mdat));
                        mdatOffset = fis.getOffset() - dataSize;
                        log.debug("File size: {} mdat size: {}", file.length(), dataSize);
                        break;
                    case 1718773093:
                    case 2003395685:
                        break;
                    default:
                        log.warn("Unexpected atom: {}", MP4Atom.intToType(atom.getType()));
                }
            }
            moovOffset += 8;
            mdatOffset += 8;
            log.debug("Offsets moov: {} mdat: {}", moovOffset, mdatOffset);
        } catch (IOException e) {
            log.error("Exception decoding header / atoms", e);
        }
    }

    public long getTotalBytes() {
        try {
            return channel.size();
        } catch (Exception e) {
            log.error("Error getTotalBytes", e);
            return 0;
        }
    }

    /** {@inheritDoc} */
    public boolean hasVideo() {
        return false;
    }

    /**
	 * Returns the file buffer.
	 * 
	 * @return  File contents as byte buffer
	 */
    public IoBuffer getFileData() {
        return null;
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
        return in.position();
    }

    /** {@inheritDoc} */
    public long getDuration() {
        return duration;
    }

    public String getAudioCodecId() {
        return audioCodecId;
    }

    /** {@inheritDoc}
	 */
    public boolean hasMoreTags() {
        return currentFrame < frames.size();
    }

    /**
     * Create tag for metadata event.
	 *
     * @return         Metadata event tag
     */
    ITag createFileMeta() {
        log.debug("Creating onMetaData");
        IoBuffer buf = IoBuffer.allocate(1024);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onMetaData");
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put("duration", ((double) duration / (double) timeScale));
        props.put("audiocodecid", audioCodecId);
        props.put("aacaot", audioCodecType);
        props.put("audiosamplerate", audioTimeScale);
        props.put("audiochannels", audioChannels);
        props.put("moovposition", moovOffset);
        props.put("canSeekToEnd", false);
        out.writeMap(props, new Serializer());
        buf.flip();
        duration = Math.round(duration * 1000d);
        ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
        result.setBody(buf);
        return result;
    }

    /**
	 * Tag sequence
	 * MetaData, Audio config, remaining audio  
	 * 
	 * Packet prefixes:
	 * af 00 ...   06 = Audio extra data (first audio packet)
	 * af 01          = Audio frame
	 * 
	 * Audio extra data(s): 
	 * af 00                = Prefix
	 * 11 90 4f 14          = AAC Main   = aottype 0
	 * 12 10                = AAC LC     = aottype 1
	 * 13 90 56 e5 a5 48 00 = HE-AAC SBR = aottype 2
	 * 06                   = Suffix
	 * 
	 * Still not absolutely certain about this order or the bytes - need to verify later
	 */
    private void createPreStreamingTags() {
        log.debug("Creating pre-streaming tags");
        IoBuffer body = IoBuffer.allocate(7);
        body.setAutoExpand(true);
        body.put(new byte[] { (byte) 0xaf, (byte) 0 });
        if (audioDecoderBytes != null) {
            log.debug("Audio decoder bytes: {}", HexDump.byteArrayToHexString(audioDecoderBytes));
            body.put(audioDecoderBytes);
        } else {
            body.put(MP4Reader.AUDIO_CONFIG_FRAME_AAC_LC);
        }
        body.put((byte) 0x06);
        ITag tag = new Tag(IoConstants.TYPE_AUDIO, 0, body.position(), null, prevFrameSize);
        body.flip();
        tag.setBody(body);
        firstTags.add(tag);
    }

    /**
	 * Packages media data for return to providers.
	 *
	 */
    public synchronized ITag readTag() {
        if (!firstTags.isEmpty()) {
            log.debug("Returning pre-tag");
            return firstTags.removeFirst();
        }
        MP4Frame frame = frames.get(currentFrame);
        log.debug("Playback {}", frame);
        int sampleSize = frame.getSize();
        int time = (int) Math.round(frame.getTime() * 1000.0);
        long samplePos = frame.getOffset();
        byte type = frame.getType();
        ByteBuffer data = ByteBuffer.allocate(sampleSize + 2);
        try {
            data.put(MP4Reader.PREFIX_AUDIO_FRAME);
            channel.position(samplePos);
            channel.read(data);
        } catch (IOException e) {
            log.error("Error on channel position / read", e);
        }
        IoBuffer payload = IoBuffer.wrap(data.array());
        ITag tag = new Tag(type, time, payload.limit(), payload, prevFrameSize);
        currentFrame++;
        prevFrameSize = tag.getBodySize();
        return tag;
    }

    /**
     * Performs frame analysis and generates metadata for use in seeking. All the frames
     * are analyzed and sorted together based on time and offset.
     */
    public void analyzeFrames() {
        log.debug("Analyzing frames");
        int sample = 1;
        Long pos = null;
        for (int i = 0; i < audioSamplesToChunks.size(); i++) {
            MP4Atom.Record record = audioSamplesToChunks.get(i);
            int firstChunk = record.getFirstChunk();
            int lastChunk = audioChunkOffsets.size();
            if (i < audioSamplesToChunks.size() - 1) {
                MP4Atom.Record nextRecord = audioSamplesToChunks.get(i + 1);
                lastChunk = nextRecord.getFirstChunk() - 1;
            }
            for (int chunk = firstChunk; chunk <= lastChunk; chunk++) {
                int sampleCount = record.getSamplesPerChunk();
                pos = audioChunkOffsets.elementAt(chunk - 1);
                while (sampleCount > 0) {
                    double ts = (audioSampleDuration * (sample - 1)) / audioTimeScale;
                    int size = (audioSamples.get(sample - 1)).intValue();
                    MP4Frame frame = new MP4Frame();
                    frame.setOffset(pos);
                    frame.setSize(size);
                    frame.setTime(ts);
                    frame.setType(TYPE_AUDIO);
                    frames.add(frame);
                    log.debug("Sample #{} {}", sample, frame);
                    pos += size;
                    sampleCount--;
                    sample++;
                }
            }
        }
        Collections.sort(frames);
        log.debug("Frames count: {}", frames.size());
    }

    /**
	 * Put the current position to pos. The caller must ensure the pos is a valid one.
	 *
	 * @param pos position to move to in file / channel
	 */
    public void position(long pos) {
        log.debug("position: {}", pos);
        currentFrame = getFrame(pos);
        log.debug("Setting current sample: {}", currentFrame);
    }

    /**
	 * Search through the frames by offset / position to find the sample.
	 * 
	 * @param pos
	 * @return
	 */
    private int getFrame(long pos) {
        int sample = 1;
        int len = frames.size();
        MP4Frame frame = null;
        for (int f = 0; f < len; f++) {
            frame = frames.get(f);
            if (pos == frame.getOffset()) {
                sample = f;
                break;
            }
        }
        return sample;
    }

    /** {@inheritDoc}
	 */
    public void close() {
        log.debug("Close");
        if (in != null) {
            in.free();
            in = null;
        }
        if (channel != null) {
            try {
                channel.close();
                fis.close();
                fis = null;
            } catch (IOException e) {
                log.error("Channel close {}", e);
            } finally {
                if (frames != null) {
                    frames.clear();
                    frames = null;
                }
            }
        }
    }

    public void setAudioCodecId(String audioCodecId) {
        this.audioCodecId = audioCodecId;
    }

    public ITag readTagHeader() {
        return null;
    }
}
