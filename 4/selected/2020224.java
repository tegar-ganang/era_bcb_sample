package org.sw.asp.reader;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import org.sw.asp.JSndFileFormat;
import org.sw.asp.JSndInfo;
import org.sw.asp.JSndReader;
import org.sw.utils.tools;

/**
 *
 * @author Rui Dong
 */
public class JSndWaveReader implements JSndReader {

    DataInputStream dis;

    private JSndInfo info;

    byte[] cache = new byte[MAX_CACHE_LENGTH];

    int pos;

    /** Creates a new instance of JSndWaveReader */
    public JSndWaveReader() {
    }

    /**
     * open a sound file
     *
     * @param filename sndfile name
     * @return true if open file no error
     */
    public boolean open(String filename) throws IOException {
        try {
            dis = new DataInputStream(new FileInputStream(filename));
            info = new JSndInfo();
            if (dis == null) return false;
            byte[] bInfo = new byte[44];
            dis.read(bInfo, 0, 44);
            String id = new String(bInfo, 0, 4);
            String typ = new String(bInfo, 8, 4);
            String fmt = new String(bInfo, 12, 4);
            if (!id.equals("RIFF") || !typ.equals("WAVE") || !fmt.startsWith("fmt")) return false;
            info.setChannels(tools.bytesToInt(bInfo, 22, 2));
            info.setSamplerate(tools.bytesToInt(bInfo, 24, 4));
            int af = tools.bytesToInt(bInfo, 20, 2);
            int size = tools.bytesToInt(bInfo, 16, 4);
            int bps = tools.bytesToInt(bInfo, 34, 2);
            info.setBitsPerSamples(bps);
            int ssize = tools.bytesToInt(bInfo, 40, 4);
            info.setFrames(ssize / (bps >> 3) / info.getChannels());
            int fm = JSndFileFormat.JSND_FORMAT_WAV;
            int sm = tools.bytesToInt(bInfo, 20, 2);
            info.setBlockalign(tools.bytesToInt(bInfo, 32, 2));
            if (sm == 16) {
                switch(bps) {
                    case 8:
                        fm |= JSndFileFormat.JSND_FORMAT_PCM_S8;
                        break;
                    case 16:
                        fm |= JSndFileFormat.JSND_FORMAT_PCM_16;
                        break;
                    case 24:
                        fm |= JSndFileFormat.JSND_FORMAT_PCM_24;
                        break;
                    case 32:
                        fm |= JSndFileFormat.JSND_FORMAT_PCM_32;
                        break;
                    default:
                        break;
                }
                info.setFormat(sm);
            }
            info.setSections((double) info.getFrames() / info.getSamplerate());
            return true;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * read signals by frames in float
     *
     * @param len wanted signals length
     * @return readed frames
     */
    public float[] read_frames_float(int len) throws IOException {
        return read_items_float(len * info.getChannels());
    }

    /**
     * read items in float
     *
     * @param len wanted items length
     * @return readed items
     */
    public float[] read_items_float(int len) throws IOException {
        float[] r = new float[len];
        if (info.getBitsPerSamples() == 16) {
            int[] rr = read_items_int(len);
            for (int i = 0; i < len; i++) r[i] = rr[i] / 65536f - 1f;
        } else {
            for (int i = 0; i < len; i++) r[i] = dis.readFloat();
        }
        return r;
    }

    /**
     * read signals by frames in int
     *
     * @param len wanted signals length
     * @return readed frames
     */
    public int[] read_frames_int(int len) throws IOException {
        return read_items_int(len * info.getChannels());
    }

    /**
     * read items in int
     *
     * @param len wanted items length
     * @return readed items
     */
    public int[] read_items_int(int len) throws IOException {
        int byteps = info.getBitsPerSamples() >> 3;
        int rlen = len * byteps;
        byte[] r = new byte[rlen];
        dis.read(r);
        int[] rs = new int[len];
        for (int i = 0; i < len; i++) rs[i] = tools.bytesToInt(r, i * byteps, byteps);
        return rs;
    }

    /**
     * close a sound file
     *
     * @return true if close no error
     */
    public boolean close() throws IOException {
        dis.close();
        return true;
    }

    /**
     * seek file from start
     *
     * @param pos want to seek frames count
     */
    public void seek_start(int pos) throws IOException {
        dis.reset();
        dis.skipBytes(pos * (info.getBitsPerSamples() >> 3));
        this.pos = pos;
    }

    /**
     * seek file from current position
     *
     * @param pos want to seek frames count
     */
    public void seek_current(int pos) throws IOException {
        dis.skipBytes(pos * (info.getBitsPerSamples() >> 3));
        this.pos += pos;
    }

    /**
     * seek file from end the file
     *
     * @param pos want to seek frames count
     */
    public void seek_end(int pos) throws IOException {
    }

    public JSndInfo getInfo() {
        return info;
    }
}
