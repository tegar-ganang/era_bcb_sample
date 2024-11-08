package com.frinika.tootX;

import com.frinika.project.FrinikaAudioSystem;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JFrame;
import com.frinika.priority.Priority;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.server.AudioClient;
import uk.org.toot.audio.server.AudioServer;
import uk.org.toot.audio.server.IOAudioProcess;

public class LatencyTester extends Observable {

    AudioBuffer outbuf;

    AudioBuffer inbuf;

    AudioServer server;

    IOAudioProcess out;

    IOAudioProcess in;

    AudioProcess pulse;

    Analysis analysis;

    int NOSIGNAL = -1;

    int period;

    int latency = 0;

    boolean first = false;

    boolean reset = true;

    AudioClient client = new AudioClient() {

        public void work(int bufSize) {
            if (first) {
                Priority.setPriorityRR(90);
                first = false;
            }
            in.processAudio(inbuf);
            analysis.processAudio(inbuf);
            pulse.processAudio(outbuf);
            out.processAudio(outbuf);
            if (latency != analysis.latency || reset) {
                latency = analysis.latency;
                LatencyTester.this.setChanged();
                notifyObservers();
                reset = false;
            }
        }

        public void setEnabled(boolean b) {
        }
    };

    protected int getLatency() {
        return latency;
    }

    public void reset() {
        reset = true;
    }

    public void start(JFrame frame) {
        server = FrinikaAudioSystem.getAudioServer();
        try {
            out = FrinikaAudioSystem.audioOutputDialog(frame, "Select output for latency test");
            in = FrinikaAudioSystem.audioInputDialog(frame, "Select input for latency test");
        } catch (Exception e) {
            e.printStackTrace();
        }
        period = (int) (FrinikaAudioSystem.getSampleRate()) / 2;
        pulse = new Pulse();
        analysis = new Analysis();
        outbuf = server.createAudioBuffer("latenyOut");
        inbuf = server.createAudioBuffer("latencyIn");
        System.out.println(" bffer  size = " + outbuf.getSampleCount() + " " + inbuf.getSampleCount());
        assert (false);
        server.start();
    }

    public void stop() {
        assert (false);
    }

    class Pulse implements AudioProcess {

        long count = 0;

        public int processAudio(AudioBuffer buf) {
            int n = buf.getSampleCount();
            float buff[] = buf.getChannel(0);
            for (int i = 0; i < n; i++, count++) {
                if (count % period == 0) buff[i] = 0.2f; else buff[i] = 0.0f;
            }
            return AUDIO_OK;
        }

        public void open() {
        }

        public void close() {
        }
    }

    ;

    class Analysis implements AudioProcess {

        long count = 0;

        int latency = NOSIGNAL;

        int latch;

        float threshold;

        Analysis() {
            latch = 0;
            threshold = 0.2f;
        }

        public int processAudio(AudioBuffer buf) {
            int n = buf.getSampleCount();
            float buff[] = buf.getChannel(0);
            for (int i = 0; i < n; i++, count++) {
                if (latch-- < 0) {
                    if (buff[i] > threshold) {
                        latency = (int) (count % period);
                        latch = 100;
                    }
                    if (latch < -period) latency = NOSIGNAL;
                }
            }
            return AUDIO_OK;
        }

        public void open() {
        }

        public void close() {
        }
    }

    public float getLatencyInMillis() {
        return (float) (latency / server.getSampleRate() * 1000.0);
    }

    public static void main(String args[]) throws Exception {
        final LatencyTester l = new LatencyTester();
        l.addObserver(new Observer() {

            public void update(Observable o, Object arg) {
                System.out.println(" latency is " + l.getLatencyInMillis() + "mS");
            }
        });
        l.start(null);
        Thread.sleep(100000);
    }

    public int getLatencyInSamples() {
        return latency;
    }
}
