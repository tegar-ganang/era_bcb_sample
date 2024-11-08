package neuralmusic.brain;

import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import neuralmusic.brain.module.BasicDelayed;
import neuralmusic.brain.module.Scheduler;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

public class AudioThreadScheduler extends Scheduler implements AudioScheduler {

    private static final double END_OF_TIME = Double.MAX_VALUE;

    private double currentTime = 0.0;

    double when;

    double T;

    TreeSet<BasicDelayed> treeNormal = new TreeSet<BasicDelayed>();

    TreeSet<AudioEvent> treeAudio = new TreeSet<AudioEvent>();

    OscillatorBank client;

    public AudioThreadScheduler(OscillatorBank bank) {
        proccess = new SlaveProcess();
        client = bank;
    }

    @Override
    public double getCurrentTimeInSecs() {
        return currentTime;
    }

    Thread blockThread;

    private AudioProcess proccess;

    void nudge() {
        if (blockThread != null) {
            blockThread.notify();
        }
    }

    @Override
    public synchronized BasicDelayed take() {
        blockThread = Thread.currentThread();
        BasicDelayed n = null;
        while (blockThread != null) {
            if (!treeNormal.isEmpty()) n = treeNormal.first();
            if (n != null) when = n.getWhen(); else when = END_OF_TIME;
            if (when > currentTime) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            } else {
                blockThread = null;
            }
        }
        treeNormal.remove(n);
        return n;
    }

    @Override
    public void add(BasicDelayed neuron) {
        treeNormal.add(neuron);
    }

    @Override
    public long getDelay(TimeUnit unit, double triggerInSecs) {
        throw new NotImplementedException();
    }

    @Override
    public int size() {
        return treeNormal.size();
    }

    public AudioProcess getAudioProcess() {
        return proccess;
    }

    double getNextAudioTime() {
        if (treeAudio.isEmpty()) return Double.MAX_VALUE;
        AudioEvent ev = treeAudio.first();
        return ev.getWhen();
    }

    class SlaveProcess implements AudioProcess {

        private AudioBuffer nible;

        private float fs;

        private int chunkSize;

        private float dtChunk;

        private float[] nibBuff;

        @Override
        public void close() throws Exception {
        }

        @Override
        public void open() throws Exception {
        }

        int cccc = 0;

        @Override
        public int processAudio(AudioBuffer chunk) {
            try {
                int sampCheck = 0;
                if (nible == null) {
                    fs = chunk.getSampleRate();
                    T = 1.0 / fs;
                    chunkSize = chunk.getSampleCount();
                    nible = new AudioBuffer("nibble", 1, chunkSize, fs);
                    dtChunk = chunkSize / fs;
                    nibBuff = nible.getChannel(0);
                }
                processPendingAudioEvents();
                float chn[] = chunk.getChannel(0);
                double nextAudioTime = getNextAudioTime();
                double dt = nextAudioTime - currentTime;
                double endOfChunkTime = currentTime + dtChunk;
                if (dt > dtChunk) {
                    client.processAudio(chunk);
                    currentTime += dtChunk;
                    sampCheck += chunkSize;
                } else {
                    int ptr = 0;
                    boolean done = false;
                    int nibSize1 = (int) (dt * fs);
                    while (true) {
                        assert (dt >= 0);
                        assert (nibSize1 > 0);
                        assert (nibSize1 < chunkSize);
                        nible.changeSampleCount(nibSize1, false);
                        nible.makeSilence();
                        client.processAudio(nible);
                        nibBuff = nible.getChannel(0);
                        for (int i = 0; i < nibSize1; i++, ptr++) {
                            chn[ptr] += nibBuff[i];
                        }
                        assert (nibBuff == nible.getChannel(0));
                        sampCheck += nibSize1;
                        if (done) {
                            currentTime = endOfChunkTime;
                            break;
                        }
                        currentTime = nextAudioTime;
                        processPendingAudioEvents();
                        nextAudioTime = getNextAudioTime();
                        if (nextAudioTime == Double.MAX_VALUE) {
                            nibSize1 = chunkSize - ptr;
                            assert (nibSize1 != 0);
                            done = true;
                        } else {
                            dt = nextAudioTime - currentTime;
                            nibSize1 = (int) (dt * fs);
                            if (ptr + nibSize1 >= chunkSize) {
                                nibSize1 = chunkSize - ptr;
                                assert (nibSize1 >= 0);
                                done = true;
                            }
                        }
                    }
                }
                if (chunk.getChannelCount() == 2) {
                    System.arraycopy(chn, 0, chunk.getChannel(1), 0, chunkSize);
                }
                cccc++;
                assert (Math.abs(currentTime - cccc * dtChunk) < T);
            } catch (AssertionError e) {
                e.printStackTrace();
            }
            return AUDIO_OK;
        }

        private void processPendingAudioEvents() {
            while (!treeAudio.isEmpty()) {
                AudioEvent ev = treeAudio.first();
                if (ev.getWhen() > currentTime + T) return;
                treeAudio.remove(ev);
                ev.fire(AudioThreadScheduler.this);
            }
        }
    }

    @Override
    public void addAudioEvent(AudioEvent ev) {
        synchronized (treeAudio) {
            treeAudio.add(ev);
        }
    }
}
