package nl.huub.van.amelsvoort.sound.jsound;

import nl.huub.van.amelsvoort.Globals;
import nl.huub.van.amelsvoort.game.cvar_t;
import nl.huub.van.amelsvoort.qcommon.Cvar;
import javax.sound.sampled.*;

/**
 * SND_JAVA
 */
public class SND_JAVA extends Globals {

    static boolean snd_inited = false;

    static cvar_t sndbits;

    static cvar_t sndspeed;

    static cvar_t sndchannels;

    static class dma_t {

        int channels;

        int samples;

        int submission_chunk;

        int samplebits;

        int speed;

        byte[] buffer;
    }

    static SND_DMA.dma_t dma = new dma_t();

    static class SoundThread extends Thread {

        byte[] b;

        SourceDataLine l;

        int pos = 0;

        boolean running = false;

        public SoundThread(byte[] buffer, SourceDataLine line) {
            b = buffer;
            l = line;
        }

        public void run() {
            running = true;
            while (running) {
                line.write(b, pos, 512);
                pos = (pos + 512) % b.length;
            }
        }

        public synchronized void stopLoop() {
            running = false;
        }

        public int getSamplePos() {
            return pos >> 1;
        }
    }

    static SoundThread thread;

    static SourceDataLine line;

    static AudioFormat format;

    static boolean SNDDMA_Init() {
        if (snd_inited) return true;
        if (sndbits == null) {
            sndbits = Cvar.Get("sndbits", "16", CVAR_ARCHIVE);
            sndspeed = Cvar.Get("sndspeed", "0", CVAR_ARCHIVE);
            sndchannels = Cvar.Get("sndchannels", "1", CVAR_ARCHIVE);
        }
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050, 16, 1, 2, 22050, false);
        DataLine.Info dinfo = new DataLine.Info(SourceDataLine.class, format);
        try {
            line = (SourceDataLine) AudioSystem.getLine(dinfo);
        } catch (LineUnavailableException e4) {
            return false;
        }
        dma.buffer = new byte[65536];
        dma.channels = format.getChannels();
        dma.samplebits = format.getSampleSizeInBits();
        dma.samples = dma.buffer.length / format.getFrameSize();
        dma.speed = (int) format.getSampleRate();
        dma.submission_chunk = 1;
        try {
            line.open(format, 4096);
        } catch (LineUnavailableException e5) {
            return false;
        }
        line.start();
        thread = new SoundThread(dma.buffer, line);
        thread.start();
        snd_inited = true;
        return true;
    }

    static int SNDDMA_GetDMAPos() {
        return thread.getSamplePos();
    }

    static void SNDDMA_Shutdown() {
        thread.stopLoop();
        line.stop();
        line.flush();
        line.close();
        line = null;
        snd_inited = false;
    }

    public static void SNDDMA_Submit() {
    }

    static void SNDDMA_BeginPainting() {
    }
}
