package org.esb.av;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Coder implements Serializable {

    private volatile IStreamCoder _coder = null;

    private static final long serialVersionUID = 00001L;

    private Properties _props = null;

    private static Collection<String> _keys = null;

    private IRational _framerate = IRational.make();

    private static Logger _log = LoggerFactory.getLogger(Coder.class);

    private int _out_stream_index = 0;

    private static final Object _open_close_lock = new Object();

    private static final Object _global_read_write_lock = new Object();

    public Coder(IStreamCoder coder) {
        _coder = coder;
    }

    public IStreamCoder getStreamCoder() {
        return _coder;
    }

    public int open() {
        synchronized (_open_close_lock) {
            return _coder.open();
        }
    }

    public void close() {
        synchronized (_open_close_lock) {
            _coder.close();
        }
    }

    public void setOutStreamIndex(int idx) {
        _out_stream_index = idx;
    }

    public int setOutStreamIndex() {
        return _out_stream_index;
    }

    public void setFrameRate(int a, int b) {
        _framerate.setNumerator(a);
        _framerate.setDenominator(b);
    }

    public void setFrameRate(IRational fr) {
        _framerate = fr;
    }

    public IRational getFrameRate() {
        return _framerate;
    }

    public int decodeVideo(IVideoPicture pic, Packet pkt) {
        return _coder.decodeVideo(pic, pkt.getPacket(), 0);
    }

    public int encodeVideo(Packet pkt, IVideoPicture pic) {
        return _coder.encodeVideo(pkt.getPacket(), pic, 0);
    }

    public int decodeAudio(IAudioSamples smp, Packet pkt) {
        return _coder.decodeAudio(smp, pkt.getPacket(), 0);
    }

    public int encodeAudio(Packet pkt, IAudioSamples smp) {
        return _coder.encodeAudio(pkt.getPacket(), smp, 0);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        synchronized (_global_read_write_lock) {
            out.writeInt(_coder.getDirection().swigValue());
            out.writeInt(_coder.getCodecID().swigValue());
            out.writeInt(_coder.getFlags());
            out.writeInt(_coder.getPixelType().swigValue());
            out.writeInt(_coder.getWidth());
            out.writeInt(_coder.getHeight());
            out.writeInt(_coder.getTimeBase().getNumerator());
            out.writeInt(_coder.getTimeBase().getDenominator());
            out.writeInt(_framerate.getNumerator());
            out.writeInt(_framerate.getDenominator());
            out.writeInt(_coder.getNumPicturesInGroupOfPictures());
            out.writeInt(_coder.getBitRate());
            out.writeInt(_coder.getBitRateTolerance());
            out.writeInt(_coder.getChannels());
            out.writeInt(_coder.getSampleRate());
            out.writeInt(_coder.getSampleFormat().swigValue());
            if (_keys == null) {
                _keys = _coder.getPropertyNames();
            }
            _props = new Properties();
            ;
            for (String key : _keys) {
                if (_coder.getPropertyAsString(key) != null) _props.setProperty(key, _coder.getPropertyAsString(key));
            }
            out.writeObject(_props);
            out.writeInt(_coder.getExtraDataSize());
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        synchronized (_global_read_write_lock) {
            int direction = in.readInt();
            _coder = IStreamCoder.make(IStreamCoder.Direction.swigToEnum(direction));
            _coder.setCodec(ICodec.ID.swigToEnum(in.readInt()));
            _coder.setFlags(in.readInt());
            _coder.setPixelType(IPixelFormat.Type.swigToEnum(in.readInt()));
            _coder.setWidth(in.readInt());
            _coder.setHeight(in.readInt());
            IRational tb = IRational.make(in.readInt(), in.readInt());
            _coder.setTimeBase(tb);
            _framerate = IRational.make(in.readInt(), in.readInt());
            _coder.setNumPicturesInGroupOfPictures(in.readInt());
            _coder.setBitRate(in.readInt());
            _coder.setBitRateTolerance(in.readInt());
            _coder.setChannels(in.readInt());
            _coder.setSampleRate(in.readInt());
            _coder.setSampleFormat(IAudioSamples.Format.swigToEnum(in.readInt()));
            Properties props = (Properties) in.readObject();
            Iterator keys = props.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                _coder.setProperty(key, props.getProperty(key));
            }
            int extradatasize = in.readInt();
        }
    }

    private void readObjectNoData() throws ObjectStreamException {
    }
}
