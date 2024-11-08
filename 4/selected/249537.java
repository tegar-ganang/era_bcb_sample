package com.jme3.audio.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioStream;
import com.jme3.audio.SeekableStream;
import com.jme3.util.BufferUtils;
import de.jarnbjo.ogg.EndOfOggStreamException;
import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.ogg.PhysicalOggStream;
import de.jarnbjo.vorbis.IdentificationHeader;
import de.jarnbjo.vorbis.VorbisStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OGGLoader implements AssetLoader {

    private PhysicalOggStream oggStream;

    private LogicalOggStream loStream;

    private VorbisStream vorbisStream;

    private IdentificationHeader streamHdr;

    private static class JOggInputStream extends InputStream {

        private boolean endOfStream = false;

        protected final VorbisStream vs;

        public JOggInputStream(VorbisStream vs) {
            this.vs = vs;
        }

        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public int read(byte[] buf) throws IOException {
            return read(buf, 0, buf.length);
        }

        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {
            if (endOfStream) return -1;
            int bytesRead = 0, cnt = 0;
            assert length % 2 == 0;
            while (bytesRead < length) {
                if ((cnt = vs.readPcm(buf, offset + bytesRead, length - bytesRead)) <= 0) {
                    System.out.println("Read " + cnt + " bytes");
                    System.out.println("offset " + offset);
                    System.out.println("bytesRead " + bytesRead);
                    System.out.println("buf length " + length);
                    for (int i = 0; i < bytesRead; i++) {
                        System.out.print(buf[i]);
                    }
                    System.out.println("");
                    System.out.println("EOS");
                    endOfStream = true;
                    break;
                }
                bytesRead += cnt;
            }
            swapBytes(buf, offset, bytesRead);
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            vs.close();
        }
    }

    private static class SeekableJOggInputStream extends JOggInputStream implements SeekableStream {

        private LogicalOggStream los;

        private float duration;

        public SeekableJOggInputStream(VorbisStream vs, LogicalOggStream los, float duration) {
            super(vs);
            this.los = los;
            this.duration = duration;
        }

        public void setTime(float time) {
            System.out.println("--setTime--)");
            System.out.println("max granule : " + los.getMaximumGranulePosition());
            System.out.println("current granule : " + los.getTime());
            System.out.println("asked Time : " + time);
            System.out.println("new granule : " + (time / duration * los.getMaximumGranulePosition()));
            System.out.println("new granule2 : " + (time * vs.getIdentificationHeader().getSampleRate()));
            try {
                los.setTime((long) (time * vs.getIdentificationHeader().getSampleRate()));
            } catch (IOException ex) {
                Logger.getLogger(OGGLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Returns the total of expected OGG bytes. 
     * 
     * @param dataBytesTotal The number of bytes in the input
     * @return If the computed number of bytes is less than the number
     * of bytes in the input, it is returned, otherwise the number 
     * of bytes in the input is returned.
     */
    private int getOggTotalBytes(int dataBytesTotal) {
        int numSamples;
        if (oggStream instanceof CachedOggStream) {
            CachedOggStream cachedOggStream = (CachedOggStream) oggStream;
            numSamples = (int) cachedOggStream.getLastOggPage().getAbsoluteGranulePosition();
        } else {
            UncachedOggStream uncachedOggStream = (UncachedOggStream) oggStream;
            numSamples = (int) uncachedOggStream.getLastOggPage().getAbsoluteGranulePosition();
        }
        int totalBytes = numSamples * streamHdr.getChannels() * 2;
        return Math.min(totalBytes, dataBytesTotal);
    }

    private float computeStreamDuration() {
        if (oggStream instanceof UncachedOggStream) return -1;
        int bytesPerSec = 2 * streamHdr.getChannels() * streamHdr.getSampleRate();
        int totalBytes = getOggTotalBytes(Integer.MAX_VALUE);
        return (float) totalBytes / bytesPerSec;
    }

    private ByteBuffer readToBuffer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int read = 0;
        try {
            while ((read = vorbisStream.readPcm(buf, 0, buf.length)) > 0) {
                baos.write(buf, 0, read);
            }
        } catch (EndOfOggStreamException ex) {
        }
        byte[] dataBytes = baos.toByteArray();
        swapBytes(dataBytes, 0, dataBytes.length);
        int bytesToCopy = getOggTotalBytes(dataBytes.length);
        ByteBuffer data = BufferUtils.createByteBuffer(bytesToCopy);
        data.put(dataBytes, 0, bytesToCopy).flip();
        vorbisStream.close();
        loStream.close();
        oggStream.close();
        return data;
    }

    private static void swapBytes(byte[] b, int off, int len) {
        byte tempByte;
        for (int i = off; i < (off + len); i += 2) {
            tempByte = b[i];
            b[i] = b[i + 1];
            b[i + 1] = tempByte;
        }
    }

    private InputStream readToStream(boolean seekable, float streamDuration) {
        if (seekable) {
            return new SeekableJOggInputStream(vorbisStream, loStream, streamDuration);
        } else {
            return new JOggInputStream(vorbisStream);
        }
    }

    private AudioData load(InputStream in, boolean readStream, boolean streamCache) throws IOException {
        if (readStream && streamCache) {
            oggStream = new CachedOggStream(in);
        } else {
            oggStream = new UncachedOggStream(in);
        }
        Collection<LogicalOggStream> streams = oggStream.getLogicalStreams();
        loStream = streams.iterator().next();
        vorbisStream = new VorbisStream(loStream);
        streamHdr = vorbisStream.getIdentificationHeader();
        if (!readStream) {
            AudioBuffer audioBuffer = new AudioBuffer();
            audioBuffer.setupFormat(streamHdr.getChannels(), 16, streamHdr.getSampleRate());
            audioBuffer.updateData(readToBuffer());
            return audioBuffer;
        } else {
            AudioStream audioStream = new AudioStream();
            audioStream.setupFormat(streamHdr.getChannels(), 16, streamHdr.getSampleRate());
            float streamDuration = computeStreamDuration();
            audioStream.updateData(readToStream(oggStream.isSeekable(), streamDuration), streamDuration);
            return audioStream;
        }
    }

    public Object load(AssetInfo info) throws IOException {
        if (!(info.getKey() instanceof AudioKey)) {
            throw new IllegalArgumentException("Audio assets must be loaded using an AudioKey");
        }
        AudioKey key = (AudioKey) info.getKey();
        boolean readStream = key.isStream();
        boolean streamCache = key.useStreamCache();
        InputStream in = null;
        try {
            in = info.openStream();
            AudioData data = load(in, readStream, streamCache);
            if (data instanceof AudioStream) {
                in = null;
            }
            return data;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
