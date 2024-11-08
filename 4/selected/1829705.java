package com.cirnoworks.cas.impl.jsa;

import java.util.WeakHashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import odk.lang.FastMath;
import util.SimpleList;

/**
 * @author Cloudee
 * 
 */
public final class JSASoftMixer implements Runnable {

    final int SIZE = 735;

    final int BUFFERS = 5;

    final int BSIZE = SIZE * 4;

    final int PSIZE = SIZE * BUFFERS;

    final int PBSIZE = PSIZE * 4;

    private final short[] bufL = new short[SIZE];

    private final short[] bufR = new short[SIZE];

    private final byte[] buf = new byte[BSIZE];

    private final SourceDataLine line;

    private int volume = 0xffff;

    private boolean shutdown;

    private Thread thread;

    public void setVolume(float volume) {
        if (volume < 0f) {
            volume = 0f;
        }
        if (volume > 1f) {
            volume = 1f;
        }
        int sv = (int) (volume * 65535f);
        if (this.volume != sv) {
            this.volume = sv;
        }
    }

    class SoundData {

        short[] dataL;

        short[] dataR;

        int pos;

        int size;
    }

    private final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

    private final SimpleList<SoundData> buffer = new SimpleList<JSASoftMixer.SoundData>(new SoundData[16]);

    private final WeakHashMap<byte[], SoundData> cache = new WeakHashMap<byte[], JSASoftMixer.SoundData>();

    public JSASoftMixer() throws LineUnavailableException {
        Mixer mixer = AudioSystem.getMixer(null);
        System.out.println(mixer.getMixerInfo());
        Line.Info[] lis = mixer.getSourceLineInfo();
        SourceDataLine line = null;
        for (Line.Info li : lis) {
            if (li.getLineClass() == SourceDataLine.class) {
                line = (SourceDataLine) mixer.getLine(li);
                System.out.println(mixer.getMaxLines(li));
                break;
            }
        }
        if (line == null) {
            throw new RuntimeException("Can't open sound!");
        }
        line.open(format, PBSIZE);
        line.start();
        this.line = line;
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        while (true) {
            process();
        }
    }

    private void process() {
        long t1 = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            int outL = 0;
            int outR = 0;
            int shink = 0;
            synchronized (buffer) {
                for (int j = 0, max = buffer.size(); j < max; j++) {
                    if (shink > 0) {
                        buffer.set(j - shink, buffer.get(j));
                    }
                    SoundData data = buffer.get(j);
                    if (data.pos >= data.size) {
                        shink++;
                        continue;
                    }
                    outL += (data.dataL[data.pos] * volume) >> 16;
                    outR += (data.dataR[data.pos] * volume) >> 16;
                    data.pos++;
                }
                if (shink > 0) {
                    buffer.trimToSize(buffer.size() - shink);
                }
            }
            bufL[i] = trim(outL);
            bufR[i] = trim(outR);
        }
        merge(SIZE, bufL, bufR, buf);
        long t = System.nanoTime() - t1;
        if (t > 3000000) {
            System.err.println("Sound proecss takes too long:" + t + "ns " + buffer.size() + " sounds");
        }
        synchronized (this) {
            if (shutdown) {
                throw new RuntimeException();
            }
        }
        if (line.available() >= PBSIZE) {
            System.err.println("Audio buffer under run!");
        }
        line.write(buf, 0, BSIZE);
    }

    public void play(byte[] content) {
        long t0 = System.nanoTime();
        SoundData data = getSoundData(content);
        synchronized (buffer) {
            buffer.add(data);
        }
        long t1 = System.nanoTime();
        float t = (t1 - t0) / 1000000f;
        if (t > 1) {
            System.err.println("Player time " + t + "ms");
        }
    }

    protected void finalize() {
        System.out.println("JSAPlayer finalize...");
        line.close();
    }

    public void stop() {
        synchronized (this) {
            shutdown = true;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    public int getChannels() {
        return 1;
    }

    public void destroy() {
        synchronized (this) {
            shutdown = true;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    private int[] RNJ = { 3, 6, 9, 12, 15 };

    private int[] KNJ = { 0, 28672, 32256, 32704, 32760 };

    private short trim(int bj) {
        int sgn = Integer.signum(bj);
        int nj = FastMath.abs(bj);
        int cj = nj & 32767;
        cj = (cj << 2) + (cj << 1) + cj;
        nj = (nj >> 15);
        if (nj > 4) {
            nj = 4;
        }
        nj = KNJ[nj] + (cj >> RNJ[nj]);
        if (nj > 32767) {
            nj = 32767;
        }
        return (short) (sgn * nj);
    }

    private void merge(final int size, final short[] bufL, final short[] bufR, final byte[] buf) {
        for (int i = 0; i < size; i++) {
            int base = i << 2;
            buf[base] = (byte) (bufL[i] & 0xff);
            buf[base + 1] = (byte) (bufL[i] >>> 8);
            buf[base + 2] = (byte) (bufR[i] & 0xff);
            buf[base + 3] = (byte) (bufR[i] >>> 8);
        }
    }

    private SoundData getSoundData(byte[] content) {
        SoundData data = cache.get(content);
        if (data == null) {
            int size = content.length >> 2;
            data = new SoundData();
            data.size = size;
            data.dataL = new short[size];
            data.dataR = new short[size];
            for (int i = 0; i < size; i++) {
                int ss = i << 2;
                data.dataL[i] = (short) ((content[ss] & 0xff) | (content[ss + 1] << 8));
                data.dataR[i] = (short) ((content[ss + 2] & 0xff) | (content[ss + 3] << 8));
            }
            cache.put(content, data);
        }
        SoundData ret = new SoundData();
        ret.dataL = data.dataL;
        ret.dataR = data.dataR;
        ret.size = data.size;
        return ret;
    }
}
