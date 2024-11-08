package com.cookeroo.streams;

import java.io.File;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import com.cookeroo.io.DecoderInputStream;
import com.cookeroo.media.AudioRecorder;
import com.cookeroo.media.SampleRateConverter;
import com.cookeroo.media.SpeexDecoder;
import com.wowza.wms.application.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.live.*;

/**
 * @author Thomas Quintana
 */
public class ClientMediaBridge extends MediaStreamLive {

    private ByteBuffer audioBuffer = null;

    private int audioBytes = 0;

    private SpeexDecoder decoder = null;

    public ClientMediaBridge() {
        super();
        this.decoder = new SpeexDecoder(1, 16000, 1);
        this.decoder.start();
    }

    public void init(MediaStreamMap parent, int src, WMSProperties properties) {
        super.init(parent, src, properties);
    }

    public void startPublishing() {
        super.startPublishing();
        DecoderInputStream decoderStream = new DecoderInputStream(this.decoder);
        AudioFormat format = new AudioFormat(this.decoder.getSampleRate(), this.decoder.getSampleSize(), this.decoder.getChannels(), true, false);
        AudioRecorder recorder = new AudioRecorder(new File("/home/thomas/test.wav"), AudioFileFormat.Type.WAVE, SampleRateConverter.convert(8000.0f, new AudioInputStream(decoderStream, format, AudioSystem.NOT_SPECIFIED)));
    }

    public void stopPublishing() {
        super.stopPublishing();
        this.decoder.stop();
        this.decoder.close();
    }

    public void addAudioData(byte[] data, int offset, int size) {
        super.addAudioData(data, offset, size);
        if ((data[offset] & 0x01) == 0) if ((data[offset] & 0x02) == 0x02) if ((data[offset] & 0xF0) == 0xB0) {
            if (this.audioBuffer == null) this.audioBuffer = ByteBuffer.allocate(super.getAudioSize() - 1);
            this.audioBuffer.put(data, offset + 1, size - 1);
            this.audioBytes += size;
        }
        if (this.audioBytes == super.getAudioSize()) {
            this.decoder.decode(this.audioBuffer.array());
            this.audioBuffer = null;
            this.audioBytes = 0;
        }
    }
}
