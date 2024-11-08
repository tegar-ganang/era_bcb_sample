package org.red5.io.mp4;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.Tag;
import org.red5.io.mp4.MP4Atom.CompositionTimeSampleRecord;
import org.red5.io.object.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This reader is used to read the contents of an MP4 file.
 * 
 * NOTE: This class is not implemented as thread-safe, the caller
 * should ensure the thread-safety.
 * <p>
 * New NetStream notifications
 * <br />
 * Two new notifications facilitate the implementation of the playback components:
 * <ul>
 * <li>NetStream.Play.FileStructureInvalid: This event is sent if the player detects 
 * an MP4 with an invalid file structure. Flash Player cannot play files that have 
 * invalid file structures.</li>
 * <li>NetStream.Play.NoSupportedTrackFound: This event is sent if the player does not 
 * detect any supported tracks. If there aren't any supported video, audio or data 
 * tracks found, Flash Player does not play the file.</li>
 * </ul>
 * </p>
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class MP4Reader implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

    /**
	 * Logger
	 */
    private static Logger log = LoggerFactory.getLogger(MP4Reader.class);

    /** Audio packet prefix */
    public static final byte[] PREFIX_AUDIO_FRAME = new byte[] { (byte) 0xaf, (byte) 0x01 };

    /** Audio config aac main */
    public static final byte[] AUDIO_CONFIG_FRAME_AAC_MAIN = new byte[] { (byte) 0x0a, (byte) 0x10 };

    /** Audio config aac lc */
    public static final byte[] AUDIO_CONFIG_FRAME_AAC_LC = new byte[] { (byte) 0x12, (byte) 0x10 };

    /** Audio config sbr */
    public static final byte[] AUDIO_CONFIG_FRAME_SBR = new byte[] { (byte) 0x13, (byte) 0x90, (byte) 0x56, (byte) 0xe5, (byte) 0xa5, (byte) 0x48, (byte) 0x00 };

    /** Video packet prefix for the decoder frame */
    public static final byte[] PREFIX_VIDEO_CONFIG_FRAME = new byte[] { (byte) 0x17, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    /** Video packet prefix for key frames */
    public static final byte[] PREFIX_VIDEO_KEYFRAME = new byte[] { (byte) 0x17, (byte) 0x01 };

    /** Video packet prefix for standard frames (interframe) */
    public static final byte[] PREFIX_VIDEO_FRAME = new byte[] { (byte) 0x27, (byte) 0x01 };

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

    /** Mapping between file position and timestamp in ms. */
    private HashMap<Integer, Long> timePosMap;

    private HashMap<Integer, Long> samplePosMap;

    /** Whether or not the clip contains a video track */
    private boolean hasVideo = false;

    /** Whether or not the clip contains an audio track */
    private boolean hasAudio = false;

    private String videoCodecId = "avc1";

    private String audioCodecId = "mp4a";

    private byte[] audioDecoderBytes;

    private byte[] videoDecoderBytes;

    private long duration;

    private int timeScale;

    private int width;

    private int height;

    private double audioTimeScale;

    private int audioChannels;

    private int audioCodecType = 1;

    private int videoSampleCount;

    private double fps;

    private double videoTimeScale;

    private int avcLevel;

    private int avcProfile;

    private String formattedDuration;

    private long moovOffset;

    private long mdatOffset;

    private Vector<MP4Atom.Record> videoSamplesToChunks;

    private Vector<MP4Atom.Record> audioSamplesToChunks;

    private Vector<Integer> syncSamples;

    private Vector<Integer> videoSamples;

    private Vector<Integer> audioSamples;

    private Vector<Long> videoChunkOffsets;

    private Vector<Long> audioChunkOffsets;

    private int videoSampleDuration = 125;

    private int audioSampleDuration = 1024;

    private int currentFrame = 0;

    private int prevFrameSize = 0;

    private int prevVideoTS = -1;

    private List<MP4Frame> frames = new ArrayList<MP4Frame>();

    private long audioCount;

    private long videoCount;

    private Vector<MP4Atom.CompositionTimeSampleRecord> compositionTimes;

    /**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
    private LinkedList<ITag> firstTags = new LinkedList<ITag>();

    /**
	 * Container for seek points in the video. These are the time stamps
	 * for the key frames.
	 */
    private LinkedList<Integer> seekPoints;

    /** Constructs a new MP4Reader. */
    MP4Reader() {
    }

    /**
	 * Creates MP4 reader from file input stream, sets up metadata generation flag.
	 *
	 * @param f                    File input stream
	 */
    public MP4Reader(File f) throws IOException {
        if (null == f) {
            log.warn("Reader was passed a null file");
            log.debug("{}", ToStringBuilder.reflectionToString(this));
        }
        this.file = f;
        this.fis = new MP4DataStream(new FileInputStream(f));
        channel = fis.getChannel();
        decodeHeader();
        analyzeFrames();
        firstTags.add(createFileMeta());
        createPreStreamingTags(0, false);
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
                        int loops = 0;
                        int tracks = 0;
                        do {
                            MP4Atom trak = moov.lookup(MP4Atom.typeToInt("trak"), loops);
                            if (trak != null) {
                                log.debug("Track atom found");
                                log.debug("trak children: {}", trak.getChildren());
                                MP4Atom tkhd = trak.lookup(MP4Atom.typeToInt("tkhd"), 0);
                                if (tkhd != null) {
                                    log.debug("Track header atom found");
                                    log.debug("tkhd children: {}", tkhd.getChildren());
                                    if (tkhd.getWidth() > 0) {
                                        width = tkhd.getWidth();
                                        height = tkhd.getHeight();
                                        log.debug("Width {} x Height {}", width, height);
                                    }
                                }
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
                                        if ("vide".equals(hdlrType)) {
                                            hasVideo = true;
                                            if (scale > 0) {
                                                videoTimeScale = scale * 1.0;
                                                log.debug("Video time scale: {}", videoTimeScale);
                                            }
                                        } else if ("soun".equals(hdlrType)) {
                                            hasAudio = true;
                                            if (scale > 0) {
                                                audioTimeScale = scale * 1.0;
                                                log.debug("Audio time scale: {}", audioTimeScale);
                                            }
                                        }
                                        tracks++;
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
                                                        log.info("Audio samples have differing durations, audio playback may fail");
                                                    }
                                                    audioSampleDuration = rec.getSampleDuration();
                                                }
                                            }
                                        }
                                        MP4Atom vmhd = minf.lookup(MP4Atom.typeToInt("vmhd"), 0);
                                        if (vmhd != null) {
                                            log.debug("Video header atom found");
                                            MP4Atom dinf = minf.lookup(MP4Atom.typeToInt("dinf"), 0);
                                            if (dinf != null) {
                                                log.debug("Data info atom found");
                                                log.debug("Video dinf children: {}", dinf.getChildren());
                                                MP4Atom dref = dinf.lookup(MP4Atom.typeToInt("dref"), 0);
                                                if (dref != null) {
                                                    log.debug("Data reference atom found");
                                                }
                                            }
                                            MP4Atom stbl = minf.lookup(MP4Atom.typeToInt("stbl"), 0);
                                            if (stbl != null) {
                                                log.debug("Sample table atom found");
                                                log.debug("Video stbl children: {}", stbl.getChildren());
                                                MP4Atom stsd = stbl.lookup(MP4Atom.typeToInt("stsd"), 0);
                                                if (stsd != null) {
                                                    log.debug("Sample description atom found");
                                                    log.debug("Sample description (video) stsd children: {}", stsd.getChildren());
                                                    MP4Atom avc1 = stsd.lookup(MP4Atom.typeToInt("avc1"), 0);
                                                    if (avc1 != null) {
                                                        log.debug("AVC1 children: {}", avc1.getChildren());
                                                        setVideoCodecId(MP4Atom.intToType(avc1.getType()));
                                                        MP4Atom codecChild = avc1.lookup(MP4Atom.typeToInt("avcC"), 0);
                                                        if (codecChild != null) {
                                                            avcLevel = codecChild.getAvcLevel();
                                                            log.debug("AVC level: {}", avcLevel);
                                                            avcProfile = codecChild.getAvcProfile();
                                                            log.debug("AVC Profile: {}", avcProfile);
                                                            log.debug("AVCC size: {}", codecChild.getSize());
                                                            videoDecoderBytes = codecChild.getVideoConfigBytes();
                                                            log.debug("Video config bytes: {}", ToStringBuilder.reflectionToString(videoDecoderBytes));
                                                        } else {
                                                            MP4Atom pasp = avc1.lookup(MP4Atom.typeToInt("pasp"), 0);
                                                            if (pasp != null) {
                                                                log.debug("PASP children: {}", pasp.getChildren());
                                                                codecChild = pasp.lookup(MP4Atom.typeToInt("avcC"), 0);
                                                                if (codecChild != null) {
                                                                    avcLevel = codecChild.getAvcLevel();
                                                                    log.debug("AVC level: {}", avcLevel);
                                                                    avcProfile = codecChild.getAvcProfile();
                                                                    log.debug("AVC Profile: {}", avcProfile);
                                                                    log.debug("AVCC size: {}", codecChild.getSize());
                                                                    videoDecoderBytes = codecChild.getVideoConfigBytes();
                                                                    log.debug("Video config bytes: {}", ToStringBuilder.reflectionToString(videoDecoderBytes));
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        MP4Atom mp4v = stsd.lookup(MP4Atom.typeToInt("mp4v"), 0);
                                                        if (mp4v != null) {
                                                            log.debug("MP4V children: {}", mp4v.getChildren());
                                                            setVideoCodecId(MP4Atom.intToType(mp4v.getType()));
                                                            MP4Atom codecChild = mp4v.lookup(MP4Atom.typeToInt("esds"), 0);
                                                            if (codecChild != null) {
                                                                MP4Descriptor descriptor = codecChild.getEsd_descriptor();
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
                                                                                    videoDecoderBytes = new byte[descr2.getDSID().length - 8];
                                                                                    System.arraycopy(descr2.getDSID(), 8, videoDecoderBytes, 0, videoDecoderBytes.length);
                                                                                    log.debug("Video config bytes: {}", ToStringBuilder.reflectionToString(videoDecoderBytes));
                                                                                    e = 99;
                                                                                    break;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    log.debug("{}", ToStringBuilder.reflectionToString(avc1));
                                                }
                                                MP4Atom stsc = stbl.lookup(MP4Atom.typeToInt("stsc"), 0);
                                                if (stsc != null) {
                                                    log.debug("Sample to chunk atom found");
                                                    videoSamplesToChunks = stsc.getRecords();
                                                    log.debug("Record count: {}", videoSamplesToChunks.size());
                                                    MP4Atom.Record rec = videoSamplesToChunks.firstElement();
                                                    log.debug("Record data: Description index={} Samples per chunk={}", rec.getSampleDescriptionIndex(), rec.getSamplesPerChunk());
                                                }
                                                MP4Atom stsz = stbl.lookup(MP4Atom.typeToInt("stsz"), 0);
                                                if (stsz != null) {
                                                    log.debug("Sample size atom found");
                                                    videoSamples = stsz.getSamples();
                                                    log.debug("Sample size: {}", stsz.getSampleSize());
                                                    videoSampleCount = videoSamples.size();
                                                    log.debug("Sample count: {}", videoSampleCount);
                                                }
                                                MP4Atom stco = stbl.lookup(MP4Atom.typeToInt("stco"), 0);
                                                if (stco != null) {
                                                    log.debug("Chunk offset atom found");
                                                    videoChunkOffsets = stco.getChunks();
                                                    log.debug("Chunk count: {}", videoChunkOffsets.size());
                                                }
                                                MP4Atom stss = stbl.lookup(MP4Atom.typeToInt("stss"), 0);
                                                if (stss != null) {
                                                    log.debug("Sync sample atom found");
                                                    syncSamples = stss.getSyncSamples();
                                                    log.debug("Keyframes: {}", syncSamples.size());
                                                }
                                                MP4Atom stts = stbl.lookup(MP4Atom.typeToInt("stts"), 0);
                                                if (stts != null) {
                                                    log.debug("Time to sample atom found");
                                                    Vector<MP4Atom.TimeSampleRecord> records = stts.getTimeToSamplesRecords();
                                                    log.debug("Record count: {}", records.size());
                                                    MP4Atom.TimeSampleRecord rec = records.firstElement();
                                                    log.debug("Record data: Consecutive samples={} Duration={}", rec.getConsecutiveSamples(), rec.getSampleDuration());
                                                    if (records.size() > 1) {
                                                        log.info("Video samples have differing durations, video playback may fail");
                                                    }
                                                    videoSampleDuration = rec.getSampleDuration();
                                                }
                                                MP4Atom ctts = stbl.lookup(MP4Atom.typeToInt("ctts"), 0);
                                                if (ctts != null) {
                                                    log.debug("Composition time to sample atom found");
                                                    compositionTimes = ctts.getCompositionTimeToSamplesRecords();
                                                    log.debug("Record count: {}", compositionTimes.size());
                                                    if (log.isTraceEnabled()) {
                                                        for (MP4Atom.CompositionTimeSampleRecord rec : compositionTimes) {
                                                            double offset = rec.getSampleOffset();
                                                            if (scale > 0d) {
                                                                offset = (offset / (double) scale) * 1000.0;
                                                                rec.setSampleOffset((int) offset);
                                                            }
                                                            log.trace("Record data: Consecutive samples={} Offset={}", rec.getConsecutiveSamples(), rec.getSampleOffset());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            loops++;
                        } while (loops < 3);
                        log.trace("Busted out of track loop with {} tracks after {} loops", tracks, loops);
                        fps = (videoSampleCount * timeScale) / (double) duration;
                        log.debug("FPS calc: ({} * {}) / {}", new Object[] { videoSampleCount, timeScale, duration });
                        log.debug("FPS: {}", fps);
                        StringBuilder sb = new StringBuilder();
                        double videoTime = ((double) duration / (double) timeScale);
                        log.debug("Video time: {}", videoTime);
                        int minutes = (int) (videoTime / 60);
                        if (minutes > 0) {
                            sb.append(minutes);
                            sb.append('.');
                        }
                        NumberFormat df = DecimalFormat.getInstance();
                        df.setMaximumFractionDigits(2);
                        sb.append(df.format((videoTime % 60)));
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

    /**
	 * Get the total readable bytes in a file or IoBuffer.
	 *
	 * @return          Total readable bytes
	 */
    public long getTotalBytes() {
        try {
            return channel.size();
        } catch (Exception e) {
            log.error("Error getTotalBytes", e);
        }
        if (file != null) {
            return file.length();
        } else {
            return 0;
        }
    }

    /**
	 * Get the current position in a file or IoBuffer.
	 *
	 * @return           Current position in a file
	 */
    private long getCurrentPosition() {
        try {
            if (channel.position() == channel.size()) {
                log.debug("Reached end of file, going back to data offset");
                channel.position(mdatOffset);
            }
            return channel.position();
        } catch (Exception e) {
            log.error("Error getCurrentPosition", e);
            return 0;
        }
    }

    /** {@inheritDoc} */
    public boolean hasVideo() {
        return hasVideo;
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
        return getCurrentPosition();
    }

    /** {@inheritDoc} */
    public long getDuration() {
        return duration;
    }

    public String getVideoCodecId() {
        return videoCodecId;
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
	 * Info from http://www.kaourantin.net/2007/08/what-just-happened-to-video-on-web_20.html
	 * <pre>
		duration - Obvious. But unlike for FLV files this field will always be present.
		videocodecid - For H.264 we report 'avc1'.
	    audiocodecid - For AAC we report 'mp4a', for MP3 we report '.mp3'.
	    avcprofile - 66, 77, 88, 100, 110, 122 or 144 which corresponds to the H.264 profiles.
	    avclevel - A number between 10 and 51. Consult this list to find out more.
	    aottype - Either 0, 1 or 2. This corresponds to AAC Main, AAC LC and SBR audio types.
	    moovposition - The offset in bytes of the moov atom in a file.
	    trackinfo - An array of objects containing various infomation about all the tracks in a file
	      ex.
	    	trackinfo[0].length: 7081
	    	trackinfo[0].timescale: 600
	    	trackinfo[0].sampledescription.sampletype: avc1
	    	trackinfo[0].language: und
	    	trackinfo[1].length: 525312
	    	trackinfo[1].timescale: 44100
	    	trackinfo[1].sampledescription.sampletype: mp4a
	    	trackinfo[1].language: und
	    
	    chapters - As mentioned above information about chapters in audiobooks.
	    seekpoints - As mentioned above times you can directly feed into NetStream.seek();
	    videoframerate - The frame rate of the video if a monotone frame rate is used. 
	    		Most videos will have a monotone frame rate.
	    audiosamplerate - The original sampling rate of the audio track.
	    audiochannels - The original number of channels of the audio track.
	    tags - As mentioned above ID3 like tag information.
	 * </pre>
	 * Info from 
	 * <pre>
		width: Display width in pixels.
		height: Display height in pixels.
		duration: Duration in seconds.
		avcprofile: AVC profile number such as 55, 77, 100 etc.
		avclevel: AVC IDC level number such as 10, 11, 20, 21 etc.
		aacaot: AAC audio object type; 0, 1 or 2 are supported.
		videoframerate: Frame rate of the video in this MP4.
		seekpoints: Array that lists the available keyframes in a file as time stamps in milliseconds. 
				This is optional as the MP4 file might not contain this information. Generally speaking, 
				most MP4 files will include this by default.
		videocodecid: Usually a string such as "avc1" or "VP6F."
		audiocodecid: Usually a string such as ".mp3" or "mp4a."
		progressivedownloadinfo: Object that provides information from the "pdin" atom. This is optional 
				and many files will not have this field.
		trackinfo: Object that provides information on all the tracks in the MP4 file, including their 
				sample description ID.
		tags: Array of key value pairs representing the information present in the "ilst" atom, which is 
				the equivalent of ID3 tags for MP4 files. These tags are mostly used by iTunes. 
	 * </pre>
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
        props.put("width", width);
        props.put("height", height);
        props.put("videocodecid", videoCodecId);
        props.put("avcprofile", avcProfile);
        props.put("avclevel", avcLevel);
        props.put("videoframerate", fps);
        props.put("audiocodecid", audioCodecId);
        props.put("aacaot", audioCodecType);
        props.put("audiosamplerate", audioTimeScale);
        props.put("audiochannels", audioChannels);
        props.put("moovposition", moovOffset);
        if (seekPoints != null) {
            props.put("seekpoints", seekPoints);
        }
        List<Map<String, Object>> arr = new ArrayList<Map<String, Object>>(2);
        if (hasAudio) {
            Map<String, Object> audioMap = new HashMap<String, Object>(4);
            audioMap.put("timescale", audioTimeScale);
            audioMap.put("language", "und");
            List<Map<String, String>> desc = new ArrayList<Map<String, String>>(1);
            audioMap.put("sampledescription", desc);
            Map<String, String> sampleMap = new HashMap<String, String>(1);
            sampleMap.put("sampletype", audioCodecId);
            desc.add(sampleMap);
            if (audioSamples != null) {
                audioMap.put("length_property", audioSampleDuration * audioSamples.size());
                audioSamples.clear();
                audioSamples = null;
            }
            arr.add(audioMap);
        }
        if (hasVideo) {
            Map<String, Object> videoMap = new HashMap<String, Object>(3);
            videoMap.put("timescale", videoTimeScale);
            videoMap.put("language", "und");
            List<Map<String, String>> desc = new ArrayList<Map<String, String>>(1);
            videoMap.put("sampledescription", desc);
            Map<String, String> sampleMap = new HashMap<String, String>(1);
            sampleMap.put("sampletype", videoCodecId);
            desc.add(sampleMap);
            if (videoSamples != null) {
                videoMap.put("length_property", videoSampleDuration * videoSamples.size());
                videoSamples.clear();
                videoSamples = null;
            }
            arr.add(videoMap);
        }
        props.put("trackinfo", arr);
        props.put("canSeekToEnd", (seekPoints != null));
        out.writeMap(props, new Serializer());
        buf.flip();
        duration = Math.round(duration * 1000d);
        ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
        result.setBody(buf);
        return result;
    }

    /**
	 * Tag sequence
	 * MetaData, Video config, Audio config, remaining audio and video 
	 * 
	 * Packet prefixes:
	 * 17 00 00 00 00 = Video extra data (first video packet)
	 * 17 01 00 00 00 = Video keyframe
	 * 27 01 00 00 00 = Video interframe
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
    private void createPreStreamingTags(int timestamp, boolean clear) {
        log.debug("Creating pre-streaming tags");
        if (clear) {
            firstTags.clear();
        }
        ITag tag = null;
        IoBuffer body = null;
        if (hasVideo) {
            body = IoBuffer.allocate(41);
            body.setAutoExpand(true);
            body.put(PREFIX_VIDEO_CONFIG_FRAME);
            if (videoDecoderBytes != null) {
                body.put(videoDecoderBytes);
            }
            tag = new Tag(IoConstants.TYPE_VIDEO, timestamp, body.position(), null, 0);
            body.flip();
            tag.setBody(body);
            firstTags.add(tag);
        }
        if (hasAudio) {
            body = IoBuffer.allocate(7);
            body.setAutoExpand(true);
            body.put(new byte[] { (byte) 0xaf, (byte) 0 });
            if (audioDecoderBytes != null) {
                body.put(audioDecoderBytes);
            } else {
                body.put(AUDIO_CONFIG_FRAME_AAC_LC);
            }
            body.put((byte) 0x06);
            tag = new Tag(IoConstants.TYPE_AUDIO, timestamp, body.position(), null, tag.getBodySize());
            body.flip();
            tag.setBody(body);
            firstTags.add(tag);
        }
    }

    /**
	 * Packages media data for return to providers
	 */
    public synchronized ITag readTag() {
        if (!firstTags.isEmpty()) {
            return firstTags.removeFirst();
        }
        MP4Frame frame = frames.get(currentFrame);
        log.debug("Playback #{} {}", currentFrame, frame);
        int sampleSize = frame.getSize();
        int time = (int) Math.round(frame.getTime() * 1000.0);
        long samplePos = frame.getOffset();
        byte type = frame.getType();
        int pad = 5;
        if (type == TYPE_AUDIO) {
            pad = 2;
        }
        ByteBuffer data = ByteBuffer.allocate(sampleSize + pad);
        try {
            if (type == TYPE_VIDEO) {
                if (frame.isKeyFrame()) {
                    data.put(PREFIX_VIDEO_KEYFRAME);
                } else {
                    data.put(PREFIX_VIDEO_FRAME);
                }
                int timeOffset = prevVideoTS != -1 ? time - prevVideoTS : 0;
                data.put((byte) ((timeOffset >>> 16) & 0xff));
                data.put((byte) ((timeOffset >>> 8) & 0xff));
                data.put((byte) (timeOffset & 0xff));
                if (log.isTraceEnabled()) {
                    byte[] prefix = new byte[5];
                    int p = data.position();
                    data.position(0);
                    data.get(prefix);
                    data.position(p);
                    log.trace("{}", prefix);
                }
                videoCount++;
                prevVideoTS = time;
            } else {
                data.put(PREFIX_AUDIO_FRAME);
                audioCount++;
            }
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
        timePosMap = new HashMap<Integer, Long>();
        samplePosMap = new HashMap<Integer, Long>();
        int sample = 1;
        Long pos = null;
        if (videoSamplesToChunks != null) {
            int compositeIndex = 0;
            CompositionTimeSampleRecord compositeTimeEntry = null;
            if (compositionTimes != null && !compositionTimes.isEmpty()) {
                compositeTimeEntry = compositionTimes.remove(0);
            }
            for (int i = 0; i < videoSamplesToChunks.size(); i++) {
                MP4Atom.Record record = videoSamplesToChunks.get(i);
                int firstChunk = record.getFirstChunk();
                int lastChunk = videoChunkOffsets.size();
                if (i < videoSamplesToChunks.size() - 1) {
                    MP4Atom.Record nextRecord = videoSamplesToChunks.get(i + 1);
                    lastChunk = nextRecord.getFirstChunk() - 1;
                }
                for (int chunk = firstChunk; chunk <= lastChunk; chunk++) {
                    int sampleCount = record.getSamplesPerChunk();
                    pos = videoChunkOffsets.elementAt(chunk - 1);
                    while (sampleCount > 0) {
                        samplePosMap.put(sample, pos);
                        double ts = (videoSampleDuration * (sample - 1)) / videoTimeScale;
                        boolean keyframe = false;
                        if (syncSamples != null) {
                            keyframe = syncSamples.contains(sample);
                            if (seekPoints == null) {
                                seekPoints = new LinkedList<Integer>();
                            }
                            int keyframeTs = (int) Math.round(ts * 1000.0);
                            seekPoints.add(keyframeTs);
                            timePosMap.put(keyframeTs, pos);
                        }
                        int size = (videoSamples.get(sample - 1)).intValue();
                        MP4Frame frame = new MP4Frame();
                        frame.setKeyFrame(keyframe);
                        frame.setOffset(pos);
                        frame.setSize(size);
                        frame.setTime(ts);
                        frame.setType(TYPE_VIDEO);
                        if (compositeTimeEntry != null) {
                            int consecutiveSamples = compositeTimeEntry.getConsecutiveSamples();
                            frame.setTimeOffset(compositeTimeEntry.getSampleOffset());
                            compositeIndex++;
                            if (compositeIndex - consecutiveSamples == 0) {
                                if (!compositionTimes.isEmpty()) {
                                    compositeTimeEntry = compositionTimes.remove(0);
                                }
                                compositeIndex = 0;
                            }
                        }
                        frames.add(frame);
                        pos += size;
                        sampleCount--;
                        sample++;
                    }
                }
            }
            log.debug("Sample position map (video): {}", samplePosMap);
        }
        if (audioSamplesToChunks != null) {
            sample = 1;
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
                        pos += size;
                        sampleCount--;
                        sample++;
                    }
                }
            }
        }
        Collections.sort(frames);
        log.debug("Frames count: {}", frames.size());
        if (audioSamplesToChunks != null) {
            audioChunkOffsets.clear();
            audioChunkOffsets = null;
            audioSamplesToChunks.clear();
            audioSamplesToChunks = null;
        }
        if (videoSamplesToChunks != null) {
            videoChunkOffsets.clear();
            videoChunkOffsets = null;
            videoSamplesToChunks.clear();
            videoSamplesToChunks = null;
        }
        if (syncSamples != null) {
            syncSamples.clear();
            syncSamples = null;
        }
    }

    /**
	 * Put the current position to pos. The caller must ensure the pos is a valid one.
	 *
	 * @param pos position to move to in file / channel
	 */
    public void position(long pos) {
        log.debug("Position: {}", pos);
        log.debug("Current frame: {}", currentFrame);
        int len = frames.size();
        MP4Frame frame = null;
        for (int f = 0; f < len; f++) {
            frame = frames.get(f);
            long offset = frame.getOffset();
            if (pos == offset || (offset > pos && frame.isKeyFrame())) {
                if (!frame.isKeyFrame()) {
                    log.debug("Frame #{} was not a key frame, so trying again..", f);
                    continue;
                }
                log.info("Frame #{} found for seek: {}", f, frame);
                createPreStreamingTags((int) (frame.getTime() * 1000), true);
                currentFrame = f;
                break;
            }
            prevVideoTS = (int) (frame.getTime() * 1000);
        }
        log.debug("Setting current frame: {}", currentFrame);
    }

    /** {@inheritDoc}
	 */
    public void close() {
        log.debug("Close");
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

    public void setVideoCodecId(String videoCodecId) {
        this.videoCodecId = videoCodecId;
    }

    public void setAudioCodecId(String audioCodecId) {
        this.audioCodecId = audioCodecId;
    }

    public ITag readTagHeader() {
        return null;
    }

    public KeyFrameMeta analyzeKeyFrames() {
        KeyFrameMeta result = new KeyFrameMeta();
        result.audioOnly = hasAudio && !hasVideo;
        result.duration = duration;
        result.positions = new long[seekPoints.size()];
        result.timestamps = new int[seekPoints.size()];
        for (int idx = 0; idx < seekPoints.size(); idx++) {
            final Integer ts = seekPoints.get(idx);
            result.positions[idx] = timePosMap.get(ts);
            result.timestamps[idx] = ts;
        }
        return result;
    }
}
