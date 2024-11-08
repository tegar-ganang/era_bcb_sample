package com.omnividea.media.codec.audio;

import javax.media.*;
import javax.media.protocol.*;
import javax.media.format.*;
import java.awt.*;
import com.sun.media.format.*;

public class NativeDecoder extends com.sun.media.codec.audio.AudioCodec {

    public NativeDecoder() {
        inputFormats = new Format[] { new AudioFormat("FFMPEG_AUDIO") };
    }

    public String getName() {
        return "NULL Fobs Audio Codec";
    }

    public Format[] getSupportedOutputFormats(Format in) {
        if (in == null) return new Format[] { new AudioFormat(AudioFormat.LINEAR) };
        AudioFormat af = (AudioFormat) in;
        return new Format[] { new WavAudioFormat(AudioFormat.LINEAR, af.getSampleRate(), af.getSampleSizeInBits(), af.getChannels(), af.getFrameSizeInBits(), (int) (af.getFrameSizeInBits() * af.getSampleRate() / 8), af.getEndian(), af.getSigned(), (float) af.getFrameRate(), af.getDataType(), new byte[0]) };
    }

    public Format setInputFormat(Format format) {
        if (super.setInputFormat(format) != null) {
            AudioFormat af = (AudioFormat) format;
            outputFormat = new WavAudioFormat(AudioFormat.LINEAR, af.getSampleRate(), af.getSampleSizeInBits(), af.getChannels(), af.getFrameSizeInBits(), (int) (af.getFrameSizeInBits() * af.getSampleRate() / 8), af.getEndian(), af.getSigned(), (float) af.getFrameRate(), af.getDataType(), new byte[0]);
            return format;
        } else {
            return null;
        }
    }

    public void open() {
    }

    public void close() {
    }

    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        if (!checkInputBuffer(inputBuffer)) {
            return BUFFER_PROCESSED_FAILED;
        }
        if (isEOM(inputBuffer)) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        Object outData = outputBuffer.getData();
        outputBuffer.setData(inputBuffer.getData());
        inputBuffer.setData(outData);
        outputBuffer.setLength(inputBuffer.getLength());
        outputBuffer.setFormat(outputFormat);
        outputBuffer.setOffset(inputBuffer.getOffset());
        return BUFFER_PROCESSED_OK;
    }
}
