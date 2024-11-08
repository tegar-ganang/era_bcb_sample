package com.jmex.audio.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;
import com.jme.util.geom.BufferUtils;
import com.jmex.audio.AudioBuffer;
import com.jmex.audio.AudioTrack.Format;
import com.jmex.audio.openal.OpenALStreamedAudioPlayer;
import com.jmex.audio.stream.AudioInputStream;
import com.jmex.audio.stream.OggInputStream;
import com.jmex.audio.stream.WavInputStream;

/**
 * Utility class for loading audio files.  For use by the underlying AudioSystem code.
 * @author Joshua Slack
 * @version $Id: AudioLoader.java 4833 2010-02-23 07:12:46Z christoph.luder $
 */
public class AudioLoader {

    private static final Logger logger = Logger.getLogger(AudioLoader.class.getName());

    public static void fillBuffer(AudioBuffer buffer, URL file) throws IOException {
        if (file == null) return;
        InputStream is = file.openStream();
        Format type = AudioInputStream.sniffFormat(is);
        is.close();
        if (Format.WAV.equals(type)) {
            loadWAV(buffer, file);
        } else if (Format.OGG.equals(type)) {
            loadOGG(buffer, file);
        } else {
            throw new IllegalArgumentException("Given url is not a recognized audio type. Must be OGG or RIFF/WAV: " + file);
        }
    }

    public static AudioInputStream openStream(URL resource) throws IOException {
        InputStream is = resource.openStream();
        Format type = AudioInputStream.sniffFormat(is);
        is.close();
        if (Format.WAV.equals(type)) {
            return new WavInputStream(resource);
        } else if (Format.OGG.equals(type)) {
            float length = -1;
            try {
                VorbisFile vf;
                if (!resource.getProtocol().equals("file")) {
                    vf = new VorbisFile(resource.openStream(), null, 0);
                } else {
                    vf = new VorbisFile(URLDecoder.decode(new File(resource.getFile()).getPath(), "UTF-8"));
                }
                length = vf.time_total(-1);
            } catch (JOrbisException e) {
                logger.log(Level.WARNING, "Error creating VorbisFile", e);
            }
            return new OggInputStream(resource, length);
        } else {
            throw new IOException("Given url is not a recognized audio type. Must be OGG or RIFF/WAV: " + resource);
        }
    }

    private static void loadOGG(AudioBuffer buffer, URL file) throws IOException {
        OggInputStream oggInput = new OggInputStream(file, -1);
        ByteBuffer data = read(oggInput);
        int channels = oggInput.getChannelCount();
        int bitRate = oggInput.getBitRate();
        int depth = oggInput.getDepth();
        int bytes = data.limit();
        float time = bytes / (bitRate * channels * depth * .125f);
        buffer.setup(data, channels, bitRate, time, depth);
        logger.log(Level.INFO, "ogg loaded - time: {0} channels: {1} rate: {2} depth: {3} bytes: {4}", new Object[] { time, channels, bitRate, depth, bytes });
        data.clear();
        oggInput.close();
    }

    private static void loadWAV(AudioBuffer buffer, URL file) throws IOException {
        WavInputStream wavInput = new WavInputStream(file);
        ByteBuffer data = read(wavInput);
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            ShortBuffer tmp2 = data.duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            while (tmp2.hasRemaining()) data.putShort(tmp2.get());
            data.rewind();
        }
        int channels = wavInput.getChannelCount();
        int bitRate = wavInput.getBitRate();
        int depth = wavInput.getDepth();
        int bytes = data.limit();
        float time = bytes / (bitRate * channels * depth * .125f);
        buffer.setup(data, channels, bitRate, time, depth);
        logger.log(Level.INFO, "wav loaded - time: {0} channels: {1} rate: {2} depth: {3} bytes: {4}", new Object[] { time, channels, bitRate, depth, bytes });
        data.clear();
        wavInput.close();
    }

    private static ByteBuffer read(AudioInputStream input) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream(1024 * 256);
        byte copyBuffer[] = new byte[1024 * 4];
        int bytesRead;
        do {
            bytesRead = input.read(copyBuffer, 0, copyBuffer.length);
            if (bytesRead > 0) {
                byteOut.write(copyBuffer, 0, bytesRead);
            }
        } while (bytesRead > 0);
        int bytes = byteOut.size();
        ByteBuffer data = BufferUtils.createByteBuffer(bytes);
        data.put(byteOut.toByteArray());
        data.flip();
        return data;
    }
}
