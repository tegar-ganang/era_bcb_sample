package net.sf.mogbox.pol.ffxi;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mogbox.pol.ffxi.loader.UnsupportedFormatException;
import net.sf.mogbox.renderer.engine.Sound;
import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec.ID;
import com.xuggle.xuggler.IStreamCoder.Direction;

public class ATRAC3Decoder implements Decoder {

    private static final Constructor<?> IMPLEMENTATION_CONSTRUCTOR;

    private static Logger log = Logger.getLogger(ATRAC3Decoder.class.getName());

    static {
        Constructor<?> constructor = null;
        try {
            File xuggler = new File(System.getenv("XUGGLE_HOME"), "share/java/jars").getCanonicalFile();
            File[] files = xuggler.listFiles(new FileFilter() {

                @Override
                public boolean accept(File file) {
                    return file.isFile() && file.canRead() && file.getName().endsWith(".jar");
                }
            });
            if (files != null) {
                URL[] urls = new URL[files.length];
                for (int i = 0; i < files.length; i++) urls[i] = files[i].toURI().toURL();
                ClassLoader loader = new URLClassLoader(urls, ATRAC3Decoder.class.getClassLoader());
                Class<?> cls = loader.loadClass(ATRAC3Decoder.class.getName() + "$ATRAC3DecoderImpl");
                if (Decoder.class.isAssignableFrom(cls)) constructor = cls.getConstructor(Sound.class, ReadableByteChannel.class);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, null, e);
        }
        IMPLEMENTATION_CONSTRUCTOR = constructor;
    }

    private Decoder decoder;

    public ATRAC3Decoder(FFXISound sound, ReadableByteChannel in) {
        if (IMPLEMENTATION_CONSTRUCTOR == null) throw new UnsupportedFormatException("Unable to load Xuggler ATRAC3 decoder.");
        try {
            Object o = IMPLEMENTATION_CONSTRUCTOR.newInstance(sound, in);
            if (o instanceof Decoder) decoder = (Decoder) o;
        } catch (Exception t) {
            throw new UnsupportedFormatException(t);
        }
        if (decoder == null) throw new UnsupportedFormatException("Unable to load Xuggler ATRAC3 decoder.");
    }

    @Override
    public int getFrameSize() {
        return decoder.getFrameSize();
    }

    @Override
    public int getSamplesPerFrame() {
        return decoder.getSamplesPerFrame();
    }

    @Override
    public int getLoopOffset() {
        return decoder.getLoopOffset();
    }

    @Override
    public void reset(boolean loop) throws IOException {
        decoder.reset(loop);
    }

    @Override
    public void decode(ReadableByteChannel in, ByteBuffer out) throws IOException {
        decoder.decode(in, out);
    }

    @SuppressWarnings("unused")
    private static class ATRAC3DecoderImpl implements Decoder {

        private Sound sound;

        private int frameSize;

        private int loopFrame;

        private int loopFrameOffset;

        private byte[] key;

        private boolean loop;

        private IStreamCoder decoder;

        private IPacket packet;

        private IAudioSamples samples;

        public ATRAC3DecoderImpl(Sound sound, ReadableByteChannel in) throws IOException {
            this.sound = sound;
            frameSize = 192 * sound.getChannels();
            loopFrame = sound.getLoopPoint() / 1024;
            loopFrameOffset = sound.getLoopPoint() % 1024;
            decoder = IStreamCoder.make(Direction.DECODING);
            decoder.setSampleRate(sound.getSampleRate());
            decoder.setChannels(sound.getChannels());
            decoder.setCodec(ID.CODEC_ID_ATRAC3);
            decoder.setBitRate(frameSize * 8 * 44100 / 1024);
            decoder.setProperty("block_align", frameSize);
            IBuffer extraData = IBuffer.make(decoder, 14);
            ByteBuffer extraDataBuffer = extraData.getByteBuffer(0, 14);
            extraDataBuffer.putShort((short) 1);
            extraDataBuffer.putInt(sound.getSampleRate());
            extraDataBuffer.putInt(0);
            extraDataBuffer.putInt(1);
            decoder.setExtraData(extraData, 0, 14, true);
            if (decoder.open() < 0) throw new RuntimeException("Error opening ATRAC3 decoder.");
            packet = IPacket.make();
            IBuffer data = IBuffer.make(packet, frameSize);
            packet.setData(data);
            samples = IAudioSamples.make(1024, 2);
            ByteBuffer buffer = packet.getByteBuffer();
            int read;
            do {
                read = in.read(buffer);
            } while (read >= 0 && buffer.hasRemaining());
            if (buffer.hasRemaining()) throw new UnsupportedFormatException("Could not read ATRAC3 audio key.");
            buffer.flip();
            if (buffer.order() == ByteOrder.LITTLE_ENDIAN) {
                for (int i = 0; i < sound.getChannels(); i++) buffer.putInt(i * 192, buffer.getInt(i * 192) ^ 0x9F4E02A0);
            } else {
                for (int i = 0; i < sound.getChannels(); i++) buffer.putInt(i * 192, buffer.getInt(i * 192) ^ 0xA0024E9F);
            }
            buffer.get(key = new byte[frameSize]);
        }

        @Override
        public int getFrameSize() {
            return frameSize;
        }

        @Override
        public int getSamplesPerFrame() {
            return 1024;
        }

        @Override
        public int getLoopOffset() {
            return (loopFrame + 1) * frameSize;
        }

        @Override
        public void reset(boolean loop) {
            this.loop = loop;
        }

        @Override
        public void decode(ReadableByteChannel in, ByteBuffer out) throws IOException {
            ByteBuffer buffer = packet.getByteBuffer();
            int read;
            do {
                read = in.read(buffer);
            } while (read >= 0 && buffer.hasRemaining());
            if (buffer.hasRemaining()) throw new RuntimeException("Unexpected end of stream.");
            buffer.flip();
            if ((buffer.get(0) & 0xFC) != 0xA0) {
                for (int i = 0; i < key.length; i++) buffer.put(i, (byte) (buffer.get(i) ^ key[i]));
            }
            int offset = 0;
            while (offset < packet.getSize()) {
                int decoded = decoder.decodeAudio(samples, packet, offset);
                if (decoded < 0) throw new RuntimeException("Error decoding ATRAC3 stream.");
                offset += decoded;
                if (!samples.isComplete()) throw new RuntimeException("Error decoding ATRAC3 stream.");
                byte[] data;
                if (loop) {
                    loop = false;
                    int skip = loopFrameOffset * 2 * decoder.getChannels();
                    data = samples.getData().getByteArray(skip, samples.getSize() - skip);
                } else {
                    data = samples.getData().getByteArray(0, samples.getSize());
                }
                out.put(data);
            }
        }
    }
}
