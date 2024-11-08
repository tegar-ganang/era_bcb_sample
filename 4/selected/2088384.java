package avisync.io;

import java.io.File;
import java.io.IOException;
import avisync.model.AVIAC3AudioStreamFormat;
import avisync.model.AVIAudioStreamFormat;
import avisync.model.AVIChunkHeader;
import avisync.model.AVIContext;
import avisync.model.AVIHeader;
import avisync.model.AVIIndex;
import avisync.model.AVIIndexEntry;
import avisync.model.AVIJunk;
import avisync.model.AVIMP3AudioStreamFormat;
import avisync.model.AVIMainHeader;
import avisync.model.AVIMovie;
import avisync.model.AVIPCMAudioStreamFormat;
import avisync.model.AVIPacket;
import avisync.model.AVIRectangle;
import avisync.model.AVIStream;
import avisync.model.AVIStreamHeader;
import avisync.model.AVIUnknownStreamFormat;
import avisync.model.AVIVideoStreamFormat;

public class AVIFileWriter {

    private AVIContext context;

    private AVIOutputStream output;

    private AVIFileFixer fixer;

    private int[] streamHeaderPosition = new int[AVIMainHeader.MAX_STREAMS];

    public AVIFileWriter(AVIContext context, File file) throws IOException {
        this.context = context;
        this.output = new AVIOutputStream(file);
        this.fixer = new AVIFileFixer(file);
    }

    public AVIContext getContext() {
        return context;
    }

    public void close() throws IOException {
        fixupHeaderList(context.getHeader());
        output.close();
        fixer.fixup();
    }

    public void writeHeader() throws IOException {
        writeFileHeader();
        writeHeaderList(context.getHeader());
        writeJunkPadding();
        writeMovieHeader(context.getMovie());
    }

    public void writePacket(AVIPacket packet) throws IOException {
        writePacketChunk(packet);
    }

    public void writeTrailer() throws IOException {
        writeMovieTrailer(context.getMovie());
        writeIndex(context.getIndex());
        writeFileTrailer();
    }

    private void writeFileHeader() throws IOException {
        writeListChunkHeader(AVIChunkHeader.RIFF_FILE_CHUNK_ID, AVIChunkHeader.RIFF_FILE_TYPE_CHUNK_ID);
    }

    private void writeFileTrailer() throws IOException {
        writeListChunkTrailer(AVIChunkHeader.RIFF_CHUNK_HEADER_LENGTH);
    }

    private void writeHeaderList(AVIHeader hdrl) throws IOException {
        int chunkPosition = writeListChunkHeader(AVIChunkHeader.RIFF_LIST_CHUNK_ID, AVIHeader.AVI_HEADER_LIST_CHUNK_ID);
        writeMainHeader(hdrl.getMainHeader());
        for (int streamID = 0; streamID < hdrl.getMainHeader().getStreams(); streamID++) {
            markStreamHeaderPosition(streamID);
            writeStreamList(hdrl.getStream(streamID));
        }
        writeListChunkTrailer(chunkPosition);
    }

    private void writeMainHeader(AVIMainHeader avih) throws IOException {
        writeChunkHeader(AVIMainHeader.AVI_MAIN_HEADER_CHUNK_ID, AVIMainHeader.AVI_MAIN_HEADER_CHUNK_LENGTH);
        output.writeInt32(avih.getMicroSecPerFrame());
        output.writeInt32(avih.getMaxBytesPerSec());
        output.writeInt32(avih.getPaddingGranularity());
        output.writeInt32(avih.getFlags());
        output.writeInt32(avih.getTotalFrames());
        output.writeInt32(avih.getInitialFrames());
        output.writeInt32(avih.getStreams());
        output.writeInt32(avih.getSuggestedBufferSize());
        output.writeInt32(avih.getWidth());
        output.writeInt32(avih.getHeight());
        output.writeInt32(0);
        output.writeInt32(0);
        output.writeInt32(0);
        output.writeInt32(0);
    }

    private void writeStreamList(AVIStream strl) throws IOException {
        int chunkPosition = writeListChunkHeader(AVIChunkHeader.RIFF_LIST_CHUNK_ID, AVIStream.AVI_STREAM_LIST_CHUNK_ID);
        writeStreamHeader(strl.getHeader());
        if (strl.getHeader().isAudioType()) {
            writeAudioStreamFormat((AVIAudioStreamFormat) strl.getFormat());
        } else if (strl.getHeader().isVideoType()) {
            writeVideoStreamFormat((AVIVideoStreamFormat) strl.getFormat());
        } else {
            writeUnknownStreamFormat((AVIUnknownStreamFormat) strl.getFormat());
        }
        writeListChunkTrailer(chunkPosition);
    }

    private void markStreamHeaderPosition(int streamID) {
        streamHeaderPosition[streamID] = output.getFilePointer();
    }

    private void fixupHeaderList(AVIHeader hdrl) {
        for (int streamID = 0; streamID < hdrl.getStreams(); streamID++) {
            int position = streamHeaderPosition[streamID] + 2 * AVIChunkHeader.RIFF_CHUNK_HEADER_LENGTH + AVIChunkHeader.RIFF_LIST_TYPE_LENGTH;
            AVIStreamHeader strh = hdrl.getStream(streamID).getHeader();
            fixer.addFixup(position + 32, strh.getLength());
            fixer.addFixup(position + 36, strh.getSuggestedBufferSize());
        }
    }

    private void writeStreamHeader(AVIStreamHeader strh) throws IOException {
        writeChunkHeader(AVIStreamHeader.AVI_STREAM_HEADER_CHUNK_ID, AVIStreamHeader.AVI_STREAM_HEADER_CHUNK_LENGTH);
        output.writeInt32(strh.getType());
        output.writeInt32(strh.getHandler());
        output.writeInt32(strh.getFlags());
        output.writeInt16(strh.getPriority());
        output.writeInt16(strh.getLanguage());
        output.writeInt32(strh.getInitialFrames());
        output.writeInt32(strh.getScale());
        output.writeInt32(strh.getRate());
        output.writeInt32(strh.getStart());
        output.writeInt32(strh.getLength());
        output.writeInt32(strh.getSuggestedBufferSize());
        output.writeInt32(strh.getQuality());
        output.writeInt32(strh.getSampleSize());
        AVIRectangle frame = strh.getFrame();
        output.writeInt16(frame.getLeft());
        output.writeInt16(frame.getTop());
        output.writeInt16(frame.getRight());
        output.writeInt16(frame.getBottom());
    }

    private void writeVideoStreamFormat(AVIVideoStreamFormat strf) throws IOException {
        writeChunkHeader(AVIVideoStreamFormat.AVI_VIDEO_STREAM_FORMAT_CHUNK_ID, AVIVideoStreamFormat.AVI_VIDEO_STREAM_FORMAT_CHUNK_LENGTH);
        output.writeInt32(strf.getSize());
        output.writeInt32(strf.getWidth());
        output.writeInt32(strf.getHeight());
        output.writeInt16(strf.getPlanes());
        output.writeInt16(strf.getBitCount());
        output.writeInt32(strf.getCompression());
        output.writeInt32(strf.getSizeImage());
        output.writeInt32(strf.getXPelsPerMeter());
        output.writeInt32(strf.getYPelsPerMeter());
        output.writeInt32(strf.getClrUsed());
        output.writeInt32(strf.getClrImportant());
    }

    private void writeAudioStreamFormat(AVIAudioStreamFormat strf) throws IOException {
        writeChunkHeader(AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_CHUNK_ID, AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_EX_CHUNK_LENGTH + strf.getExtraSize());
        if (strf.getFormatTag() == AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_TAG_MP3) {
            writeMP3AudioStreamFormat((AVIMP3AudioStreamFormat) strf);
        } else if (strf.getFormatTag() == AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_TAG_AC3) {
            writeAC3AudioStreamFormat((AVIAC3AudioStreamFormat) strf);
        } else if (strf.getFormatTag() == AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_TAG_PCM) {
            writePCMAudioStreamFormat((AVIPCMAudioStreamFormat) strf);
        } else {
            throw new IOException();
        }
    }

    private void writeMP3AudioStreamFormat(AVIMP3AudioStreamFormat strf) throws IOException {
        output.writeInt16(strf.getFormatTag());
        output.writeInt16(strf.getChannels());
        output.writeInt32(strf.getSamplesPerSecond());
        output.writeInt32(strf.getAvgBytesPerSec());
        output.writeInt16(strf.getBlockAlign());
        output.writeInt16(strf.getBitsPerSample());
        output.writeInt16(strf.getExtraSize());
        output.writeInt16(strf.getID());
        output.writeInt32(strf.getFlags());
        output.writeInt16(strf.getBlockSize());
        output.writeInt16(strf.getFramesPerBlock());
        output.writeInt16(strf.getCodecDelay());
        if (strf.getExtraSize() != AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_EXTRA_SIZE_MP3) throw new IOException();
    }

    private void writeAC3AudioStreamFormat(AVIAC3AudioStreamFormat strf) throws IOException {
        output.writeInt16(strf.getFormatTag());
        output.writeInt16(strf.getChannels());
        output.writeInt32(strf.getSamplesPerSecond());
        output.writeInt32(strf.getAvgBytesPerSec());
        output.writeInt16(strf.getBlockAlign());
        output.writeInt16(strf.getBitsPerSample());
        output.writeInt16(strf.getExtraSize());
        if (strf.getExtraSize() != AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_EXTRA_SIZE_AC3) throw new IOException();
    }

    private void writePCMAudioStreamFormat(AVIPCMAudioStreamFormat strf) throws IOException {
        output.writeInt16(strf.getFormatTag());
        output.writeInt16(strf.getChannels());
        output.writeInt32(strf.getSamplesPerSecond());
        output.writeInt32(strf.getAvgBytesPerSec());
        output.writeInt16(strf.getBlockAlign());
        output.writeInt16(strf.getBitsPerSample());
        output.writeInt16(strf.getExtraSize());
        if (strf.getExtraSize() != AVIAudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_EXTRA_SIZE_PCM) throw new IOException();
    }

    private void writeUnknownStreamFormat(AVIUnknownStreamFormat strf) throws IOException {
        writeChunkHeader(AVIUnknownStreamFormat.AVI_UNKNOWN_STREAM_FORMAT_CHUNK_ID, strf.getLength());
        output.writeBytes(strf.getBuffer());
    }

    private void writeJunkPadding() throws IOException {
        int offset = output.getFilePointer() + AVIChunkHeader.RIFF_CHUNK_HEADER_LENGTH;
        int padding = (offset + AVIJunk.AVI_JUNK_PADDING_ALIGNMENT - 1) & -AVIJunk.AVI_JUNK_PADDING_ALIGNMENT;
        if (padding < AVIJunk.AVI_JUNK_PADDING_MINIMUM_LENGTH) padding = AVIJunk.AVI_JUNK_PADDING_MINIMUM_LENGTH;
        int chunkLength = padding - offset;
        writeChunkHeader(AVIJunk.AVI_JUNK_CHUNK_ID, chunkLength);
        while (--chunkLength >= 0) output.writeByte(0);
    }

    private void writeMovieHeader(AVIMovie movi) throws IOException {
        int movieChunkOffset = writeListChunkHeader(AVIChunkHeader.RIFF_LIST_CHUNK_ID, AVIMovie.AVI_MOVIE_LIST_CHUNK_ID);
        getContext().getMovie().setChunkOffset(movieChunkOffset);
    }

    private void writeMovieTrailer(AVIMovie movi) throws IOException {
        int movieChunkLength = writeListChunkTrailer(getContext().getMovie().getChunkOffset());
        getContext().getMovie().setChunkLength(movieChunkLength);
    }

    private void writePacketChunk(AVIPacket packet) throws IOException {
        writeChunkHeader(packet.getChunkID(), packet.getLength());
        output.writeBytes(packet.getData());
        if (packet.getPadding() != 0) output.writeByte(0);
    }

    private void writeIndex(AVIIndex idx1) throws IOException {
        writeChunkHeader(AVIIndex.AVI_INDEX_CHUNK_ID, AVIIndexEntry.AVI_INDEX_ENTRY_LENGTH * idx1.getLength());
        for (int frameNumber = 0; frameNumber < idx1.getLength(); frameNumber++) writeIndexEntry(idx1.getEntry(frameNumber));
    }

    private void writeIndexEntry(AVIIndexEntry entry) throws IOException {
        output.writeInt32(entry.getChunkID());
        output.writeInt32(entry.getFlags());
        output.writeInt32(entry.getChunkOffset());
        output.writeInt32(entry.getChunkLength());
    }

    private void writeChunkHeader(int chunkID, int chunkLength) throws IOException {
        output.writeInt32(chunkID);
        output.writeInt32(chunkLength);
    }

    private int writeListChunkHeader(int chunkID, int chunkListTypeID) throws IOException {
        int chunkOffset = output.getFilePointer() + AVIChunkHeader.RIFF_CHUNK_HEADER_LENGTH;
        output.writeInt32(chunkID);
        output.writeInt32(0);
        output.writeInt32(chunkListTypeID);
        return chunkOffset;
    }

    private int writeListChunkTrailer(int chunkOffset) throws IOException {
        int chunkLength = output.getFilePointer() - chunkOffset;
        fixer.addFixup(chunkOffset - AVIChunkHeader.RIFF_CHUNK_HEADER_LENGTH + 4, chunkLength);
        int chunkPadding = chunkLength & 1;
        if (chunkPadding != 0) output.writeByte(0);
        return chunkLength;
    }
}
