package org.esb.hive;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IPacket;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.esb.av.Coder;
import org.esb.av.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioProcessUnit implements Serializable, ProcessUnit {

    private Coder _dec = null;

    private Coder _enc = null;

    private int _source_stream;

    private int _target_stream;

    private long _pu_id;

    private List<Packet> _input_packets;

    private List<Packet> _output_packets;

    private static final long serialVersionUID = 00001L;

    private static final Logger _log = LoggerFactory.getLogger(AudioProcessUnit.class);

    private int incount = 0;

    private int outcount = 0;

    private double _rateCompensate = 0.0;

    private int _esteminatedPacketCount = 0;

    private long _decoded_bytes = 0;

    private long _encoded_bytes = 0;

    private long _resampled_bytes = 0;

    public AudioProcessUnit() {
        super();
        _output_packets = new ArrayList<Packet>();
    }

    public void setDecoder(Coder dec) {
        _dec = dec;
    }

    public void setEncoder(Coder enc) {
        _enc = enc;
    }

    public void setInputPackets(List<Packet> input) {
        _input_packets = input;
    }

    public int getSourceStream() {
        return _source_stream;
    }

    public int getTargetStream() {
        return _target_stream;
    }

    public void setSourceStream(int d) {
        _source_stream = d;
    }

    public void setTargetStream(int d) {
        _target_stream = d;
    }

    public long getPuId() {
        return _pu_id;
    }

    public void setPuId(long id) {
        _pu_id = id;
    }

    public long getStartTimeStamp() {
        long result = 0;
        if (_input_packets.size() > 0) result = _input_packets.get(0).getDts();
        return result;
    }

    public long getEndTimeStamp() {
        long result = 0;
        if (_input_packets.size() > 0) result = _input_packets.get(_input_packets.size() - 1).getDts();
        return result;
    }

    public int getPacketCount() {
        return _input_packets.size();
    }

    public void clear() {
    }

    public void process() {
        int channels = _dec.getStreamCoder().getChannels() > 2 ? 2 : _dec.getStreamCoder().getChannels();
        _dec.getStreamCoder().setProperty("request_channel_layout", channels);
        _dec.getStreamCoder().setProperty("request_channels", channels);
        _dec.getStreamCoder().setChannels(2);
        _dec.open();
        _enc.open();
        long frame_size = _enc.getStreamCoder().getPropertyAsLong("frame_size");
        if (_dec.getStreamCoder().getChannels() > 2) System.exit(1);
        IAudioResampler resampler = IAudioResampler.make(_enc.getStreamCoder().getChannels(), channels, _enc.getStreamCoder().getSampleRate(), _dec.getStreamCoder().getSampleRate(), _enc.getStreamCoder().getSampleFormat(), _dec.getStreamCoder().getSampleFormat());
        IAudioSamples insamples = IAudioSamples.make(_dec.getStreamCoder().getAudioFrameSize(), channels, _dec.getStreamCoder().getSampleFormat());
        IAudioSamples outsamples = IAudioSamples.make(_enc.getStreamCoder().getAudioFrameSize(), _enc.getStreamCoder().getChannels(), _enc.getStreamCoder().getSampleFormat());
        IPacket outpacket = IPacket.make();
        int samples_offset = (int) Math.floor(_rateCompensate);
        if (samples_offset > 0) {
            samples_offset += 64;
            IAudioSamples dummysamples = IAudioSamples.make(IBuffer.make(null, new byte[samples_offset], 0, samples_offset), _enc.getStreamCoder().getChannels(), _enc.getStreamCoder().getSampleFormat());
            dummysamples.setComplete(true, samples_offset, _enc.getStreamCoder().getSampleRate(), _enc.getStreamCoder().getChannels(), _enc.getStreamCoder().getSampleFormat(), 0);
            int error = _enc.getStreamCoder().encodeAudio(outpacket, dummysamples, 0);
            _encoded_bytes += error;
            if (error < 0) {
                _log.error("encoding audio");
            }
        }
        for (Packet p : _input_packets) {
            if ((_decoded_bytes += _dec.getStreamCoder().decodeAudio(insamples, p.getPacket(), 0)) < 0) {
                _log.error("decoding audio");
            }
            if (!insamples.isComplete()) {
                continue;
            }
            _resampled_bytes += resampler.resample(outsamples, insamples, 0);
            for (int consumed = 0; consumed < outsamples.getNumSamples(); ) {
                int result = _enc.getStreamCoder().encodeAudio(outpacket, outsamples, consumed);
                _encoded_bytes += result;
                if (result < 0) {
                    _log.error("encoding audio");
                } else consumed += result;
                if (outpacket.isComplete()) {
                    outpacket.setDuration(frame_size);
                    _output_packets.add(new Packet(outpacket));
                }
            }
        }
        boolean encode = true;
        while (encode) {
            _encoded_bytes += _enc.getStreamCoder().encodeAudio(outpacket, null, 0);
            if (outpacket.isComplete()) {
                _output_packets.add(new Packet(outpacket));
            } else {
                encode = false;
            }
        }
        for (int a = 0; false && a < 3; a++) {
            outsamples.put(new byte[5000], 0, 0, 5000);
            int result = _enc.getStreamCoder().encodeAudio(outpacket, outsamples, 0);
            if (result < 0) {
                _log.error("encoding audio");
            }
            if (outpacket.isComplete()) {
                _log.debug("AudioPacket completed" + outpacket.toString());
                outpacket.setDuration(frame_size);
                _output_packets.add(new Packet(outpacket));
            }
        }
        _dec.close();
        _enc.close();
        _input_packets.clear();
        if (_esteminatedPacketCount > 0 && _esteminatedPacketCount != _output_packets.size()) {
            _log.warn("PacketCount " + _output_packets.size() + " diff from expeceted PacketCount " + _esteminatedPacketCount + " for ProccessUnit#" + getPuId());
        }
        resampler.delete();
        insamples.delete();
        outsamples.delete();
        outpacket.delete();
    }

    public List<Packet> getOutputPackets() {
        return _output_packets;
    }

    public void setRateCompensateBase(double val) {
        _rateCompensate = val;
    }

    public void setEsteminatedPacketCount(int c) {
        _esteminatedPacketCount = c;
    }

    public int getEstimatedPacketCount() {
        return _esteminatedPacketCount;
    }

    private void dumpHex(IBuffer b, int s) {
        int size = s;
        byte[] buffer = b.getByteArray(0, size);
        int len, i, j, c;
        for (i = 0; i < size; i += 16) {
            len = size - i;
            if (len > 16) len = 16;
            System.out.printf("%08x ", i);
            for (j = 0; j < 16; j++) {
                if (j < len) System.out.printf(" %02x", buffer[i + j]); else System.out.printf("   ");
            }
            System.out.printf(" ");
            for (j = 0; j < len; j++) {
                c = buffer[i + j];
                if (c < ' ' || c > '~') c = '.';
                System.out.printf("%c", c);
            }
            System.out.printf("\n");
        }
    }

    public Coder getDecoder() {
        return _dec;
    }

    public Coder getEncoder() {
        return _enc;
    }
}
