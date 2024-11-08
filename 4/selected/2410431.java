package avisync.node.debugger;

import avisync.AVIException;
import avisync.model.AVIAudioStreamFormat;
import avisync.model.AVIContext;
import avisync.model.AVIHeader;
import avisync.model.AVIIndex;
import avisync.model.AVIIndexEntry;
import avisync.model.AVIMP3AudioStreamFormat;
import avisync.model.AVIMainHeader;
import avisync.model.AVIPacket;
import avisync.model.AVIStream;
import avisync.model.AVIStreamFormat;
import avisync.model.AVIStreamHeader;
import avisync.model.AVIVideoStreamFormat;
import avisync.node.AVINode;
import avisync.util.AVICharCode;

public class AVIDebugger extends AVINode {

    public static final int DEBUG_HEADER = 1 << 0;

    public static final int DEBUG_INDEX = 1 << 1;

    public static final int DEBUG_PACKETS = 1 << 2;

    public static final int DEBUG_PROGRESS = 1 << 3;

    public static final int DEBUG_ALL = DEBUG_HEADER | DEBUG_INDEX | DEBUG_PACKETS | DEBUG_PROGRESS;

    private AVINode output;

    private int flags;

    private int[] frames = new int[AVIMainHeader.MAX_STREAMS];

    private int progress;

    private long startTime;

    private long stopTime;

    public AVIDebugger() {
    }

    public AVIDebugger(AVIContext context, AVINode output, int flags) {
        setContext(context);
        setOutput(output);
        setFlags(flags);
    }

    public AVINode getOutput() {
        return output;
    }

    public int getFlags() {
        return flags;
    }

    public void setOutput(AVINode output) {
        this.output = output;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void init() throws AVIException {
        if (getOutput() != null) getOutput().init();
        if ((getFlags() & DEBUG_HEADER) != 0) printHeader(getContext().getHeader());
        if ((getFlags() & DEBUG_PROGRESS) != 0) initProgress();
    }

    public void destroy() throws AVIException {
        if ((getFlags() & DEBUG_PROGRESS) != 0) destroyProgress();
        if ((getFlags() & DEBUG_INDEX) != 0) printIndex(getContext().getIndex());
        if (getOutput() != null) getOutput().destroy();
    }

    public void sendPacket(AVIPacket packet) throws AVIException {
        if ((getFlags() & DEBUG_PACKETS) != 0) printPacket(packet);
        if ((getFlags() & DEBUG_PROGRESS) != 0) printProgress(packet);
        if (getOutput() != null) getOutput().sendPacket(packet);
    }

    private void printHeader(AVIHeader header) {
        printMainHeader(header.getMainHeader());
        for (int streamID = 0; streamID < header.getStreams(); streamID++) printStreamList(header.getStream(streamID));
    }

    private void printMainHeader(AVIMainHeader mainHeader) {
        double duration = mainHeader.getTotalFrames() * (mainHeader.getMicroSecPerFrame() / 1000000.0);
        info("AVI Main Header:");
        info("  MicroSecPerFrame: " + mainHeader.getMicroSecPerFrame() + " usec " + "(" + (1000000.0 / mainHeader.getMicroSecPerFrame()) + " frames/second)");
        info("  MaxBytesPerSec: " + mainHeader.getMaxBytesPerSec() + " bytes");
        info("  PaddingGranularity: " + mainHeader.getPaddingGranularity() + " bytes");
        info("  Flags: 0x" + Integer.toHexString(mainHeader.getFlags()) + " ( " + ((mainHeader.getFlags() & AVIMainHeader.AVI_FLAG_HASINDEX) != 0 ? "HASINDEX " : "") + ((mainHeader.getFlags() & AVIMainHeader.AVI_FLAG_MUSTUSEINDEX) != 0 ? "MUSTUSEINDEX " : "") + ((mainHeader.getFlags() & AVIMainHeader.AVI_FLAG_ISINTERLEAVED) != 0 ? "ISINTERLEAVED " : "") + ((mainHeader.getFlags() & AVIMainHeader.AVI_FLAG_WASCAPTUREFILE) != 0 ? "WASCAPTUREFILE " : "") + ((mainHeader.getFlags() & AVIMainHeader.AVI_FLAG_COPYRIGHTED) != 0 ? "COPYRIGHTED " : "") + ")");
        info("  TotalFrames: " + mainHeader.getTotalFrames() + " frames (" + formatTime(duration) + ")");
        info("  InitialFrames: " + mainHeader.getInitialFrames() + " frames");
        info("  Streams: " + mainHeader.getStreams() + " streams");
        info("  SuggestedBufferSize: " + mainHeader.getSuggestedBufferSize() + " bytes");
        info("  Width: " + mainHeader.getWidth() + " pixels");
        info("  Height: " + mainHeader.getHeight() + " pixels");
        info();
    }

    private void printStreamList(AVIStream stream) {
        info("AVI Stream Header:");
        printStreamHeader(stream.getHeader());
        printStreamFormat(stream.getFormat());
    }

    private void printStreamHeader(AVIStreamHeader streamHeader) {
        double rate = (double) streamHeader.getRate() / (double) streamHeader.getScale();
        double duration = (double) streamHeader.getLength() * (double) streamHeader.getScale() / (double) streamHeader.getRate();
        info("  Type: " + AVICharCode.createFourCC(streamHeader.getType()));
        if (streamHeader.getHandler() != 0) info("  Handler: " + AVICharCode.createFourCC(streamHeader.getHandler()));
        info("  Flags: 0x" + Integer.toHexString(streamHeader.getFlags()));
        info("  Priority: " + streamHeader.getPriority());
        info("  Language: " + streamHeader.getLanguage());
        info("  InitialFrames: " + streamHeader.getInitialFrames() + " frames");
        info("  Rate: " + rate + " frames/second (" + streamHeader.getRate() + "/" + streamHeader.getScale() + ")");
        info("  Start: " + streamHeader.getStart() + " frames");
        info("  Length: " + streamHeader.getLength() + " frames " + "(" + formatTime(duration) + ")");
        info("  SuggestedBufferSize: " + streamHeader.getSuggestedBufferSize() + " bytes");
        info("  Quality: " + streamHeader.getQuality());
        info("  SampleSize: " + streamHeader.getSampleSize() + " bytes (" + (streamHeader.getSampleSize() == 0 ? "VBR" : "CBR") + ")");
        if (streamHeader.isVideoType()) info("  Frame: [" + streamHeader.getFrame().getLeft() + " " + streamHeader.getFrame().getTop() + " " + streamHeader.getFrame().getRight() + " " + streamHeader.getFrame().getBottom() + "]");
    }

    private void printStreamFormat(AVIStreamFormat streamFormat) {
        if (streamFormat instanceof AVIVideoStreamFormat) printVideoStreamFormat((AVIVideoStreamFormat) streamFormat);
        if (streamFormat instanceof AVIAudioStreamFormat) printAudioStreamFormat((AVIAudioStreamFormat) streamFormat);
    }

    private void printVideoStreamFormat(AVIVideoStreamFormat streamFormat) {
        info("  Video:");
        info("    Size: " + streamFormat.getSize() + " bytes");
        info("    Width: " + streamFormat.getWidth() + " pixels");
        info("    Height: " + streamFormat.getHeight() + " pixels");
        info("    Planes: " + streamFormat.getPlanes() + " planes");
        info("    BitCount: " + streamFormat.getBitCount() + " bits");
        info("    Compression: " + AVICharCode.createFourCC(streamFormat.getCompression()));
        info("    SizeImage: " + streamFormat.getSizeImage() + " bytes");
        info("    PelsPerMeter: " + streamFormat.getXPelsPerMeter() + " x " + streamFormat.getYPelsPerMeter() + " pixels/meter");
        info("    ClrUsed: " + streamFormat.getClrUsed());
        info("    ClrImportant: " + streamFormat.getClrImportant());
        info();
    }

    private void printAudioStreamFormat(AVIAudioStreamFormat streamFormat) {
        double bitRate = 8.0 * streamFormat.getAvgBytesPerSec() / 1000.0;
        info("  Audio:");
        info("    FormatTag: 0x" + Integer.toHexString(streamFormat.getFormatTag()) + " (" + (streamFormat.getFormatTag() == AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_TAG_MP3 ? "MP3" : streamFormat.getFormatTag() == AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_TAG_AC3 ? "AC3" : streamFormat.getFormatTag() == AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_TAG_PCM ? "PCM" : "Unknown") + ")");
        info("    Channels: " + streamFormat.getChannels() + " channels");
        info("    SamplesPerSecond: " + streamFormat.getSamplesPerSecond() + " samples/second");
        info("    AvgBytesPerSec: " + streamFormat.getAvgBytesPerSec() + " bytes/second (" + bitRate + " Kbps)");
        info("    BlockAlign: " + streamFormat.getBlockAlign() + " samples");
        info("    BitsPerSample: " + streamFormat.getBitsPerSample() + " bits");
        info("    ExtraSize: " + streamFormat.getExtraSize() + " bytes");
        if (streamFormat instanceof AVIMP3AudioStreamFormat) {
            AVIMP3AudioStreamFormat mp3f = (AVIMP3AudioStreamFormat) streamFormat;
            double blockRate = (double) streamFormat.getAvgBytesPerSec() / (double) mp3f.getBlockSize();
            double frameRate = (double) mp3f.getFramesPerBlock() * blockRate;
            info("    MP3 ID: " + mp3f.getID());
            info("    MP3 Flags: 0x" + Integer.toHexString(mp3f.getFlags()));
            info("    MP3 BlockSize: " + mp3f.getBlockSize() + " bytes " + "(" + blockRate + " blocks/second)");
            info("    MP3 FramesPerBlock: " + mp3f.getFramesPerBlock() + " frames (" + frameRate + " frames/second)");
            info("    MP3 CodecDelay: " + mp3f.getCodecDelay() + " samples");
        }
        info();
    }

    private void printIndex(AVIIndex index) {
        info("AVI Index");
        for (int chunkNumber = 0; chunkNumber < index.getLength(); chunkNumber++) printIndexEntry(index.getEntry(chunkNumber));
    }

    private void printIndexEntry(AVIIndexEntry entry) {
        info(String.format("  %4s %04x %08x %d", AVICharCode.createFourCC(entry.getChunkID()), entry.getFlags(), entry.getChunkOffset(), entry.getChunkLength()));
    }

    private void printPacket(AVIPacket packet) {
        info(String.format("Packet: %4s %04x %d", AVICharCode.createFourCC(packet.getChunkID()), packet.getFlags(), packet.getLength()));
    }

    private void printProgress(AVIPacket packet) {
        updateProgress(packet);
        if (++progress >= 10000) {
            AVIHeader header = getContext().getHeader();
            StringBuffer buffer = new StringBuffer();
            for (int streamID = 0; streamID < header.getStreams(); streamID++) {
                AVIStreamHeader streamHeader = header.getStream(streamID).getHeader();
                int streamFrames = frames[streamID];
                double streamTime = streamFrames * (double) streamHeader.getScale() / (double) streamHeader.getRate();
                if (streamID != 0) buffer.append("  ");
                buffer.append(streamHeader.isVideoType() ? "V" : streamHeader.isAudioType() ? "A" : "S");
                buffer.append(": ");
                buffer.append(String.format("%-6d", streamFrames)).append(" (").append(formatTime(streamTime)).append(")");
            }
            info(buffer.toString());
            progress = 0;
        }
    }

    private void initProgress() {
        info("AVI Packets:");
        progress = 0;
        for (int streamID = 0; streamID < AVIMainHeader.MAX_STREAMS; streamID++) frames[streamID] = 0;
        startTime = System.currentTimeMillis();
    }

    private void destroyProgress() {
        stopTime = System.currentTimeMillis();
        AVIHeader header = getContext().getHeader();
        double mainDuration = header.getMainHeader().getTotalFrames() * (header.getMainHeader().getMicroSecPerFrame() / 1000000.0);
        for (int streamID = 0; streamID < header.getStreams(); streamID++) {
            AVIStreamHeader streamHeader = header.getStream(streamID).getHeader();
            double streamDuration = streamHeader.getLength() * (double) streamHeader.getScale() / (double) streamHeader.getRate();
            if (Math.abs(mainDuration - streamDuration) >= 1.0) warning("Stream length " + formatTime(streamDuration) + " should be " + formatTime(mainDuration));
        }
        info();
        info("Elapsed Time: " + formatTime((stopTime - startTime) / 1000.0));
    }

    private void updateProgress(AVIPacket packet) {
        int streamID = packet.getStreamID();
        int sampleSize = getContext().getHeader().getStream(streamID).getHeader().getSampleSize();
        frames[streamID] += (sampleSize != 0 ? packet.getLength() / sampleSize : 1);
    }

    private String formatTime(double ms) {
        long time = (long) (1000 * ms);
        int milliseconds = (int) (time % 1000);
        int seconds = (int) ((time / 1000) % 60);
        int minutes = (int) ((time / 60000) % 60);
        int hours = (int) ((time / 3600000) % 24);
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }
}
