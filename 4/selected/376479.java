package de.mcb.sampler;

import de.mcb.BaseVSTPluginAdapter;
import de.mcb.pitcher.PitchShifter;
import de.mcb.pitcher.Stretcher;
import de.mcb.pitcher.SynthesizeStatus;
import de.mcb.pitcher.PitchedSample;
import de.mcb.util.Tapper;
import de.mcb.util.TapperCallback;
import static de.mcb.sampler.SamplerFX.State.*;
import static de.mcb.util.FluentMap.*;
import de.mcb.util.Sample;
import de.mcb.util.WaveSample;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.*;

/**
 * User: Max
 * Date: 29.11.2009
 */
public class SamplerFX extends BaseVSTPluginAdapter {

    public static enum State {

        STATE_PLAY, STATE_REC
    }

    private static final double DEFAULT_TEMPO = 120.0;

    private static final String basePath = "C://sample_";

    private static final int PLAY = 0;

    private static final int REC = 1;

    private static List<Button> buttons = new ArrayList<Button>();

    private static State state = STATE_PLAY;

    private Button activeBtn;

    private static ConcurrentLinkedQueue<Button> isPlaying = new ConcurrentLinkedQueue<Button>();

    private boolean recording = false;

    private static ByteArrayOutputStream recorder = new ByteArrayOutputStream();

    private static Hashtable<Integer, Button> activeButtonPerChannel = new Hashtable<Integer, Button>();

    private static Tapper tapper = new Tapper();

    private static int blockSize = 512;

    private static PitchSet pitchSet = new PitchSet();

    private static SampleManager sampleManager = new SampleManager();

    public SamplerFX(long wrapper) {
        super(wrapper);
    }

    private PitchedSample prepareShifting(WaveSample sample, double newTempo, int neededLength) {
        float factor = (float) (sample.getTempo() / newTempo);
        log("old tempo = " + sample.getTempo() + ", new tempo = " + newTempo + ", factor = " + factor + ", est = " + (sample.getData().length * factor));
        List<List<float[]>> andat = sample.getPitchAnalysis();
        PitchShifter ps = new PitchShifter(factor, sample.getSampleRate(), sample.getData());
        SynthesizeStatus stat = null;
        PitchedSample pitchedSample = new PitchedSample(andat, sample.getSampleRate(), ps, (int) (sample.getData().length * factor));
        while (((stat == null) || (stat.getLastEnd() < sample.getData().length)) && (pitchedSample.getPitchedLength() < neededLength)) {
            ps.synthesize(andat, true, null);
            stat = ps.getSynthesizeStatus();
            float[] stretched = Stretcher.stretch(factor, Arrays.copyOfRange(stat.getWorkload(), stat.getLastStart(), stat.getLastEnd()));
            pitchedSample.addPitchedPart(stretched);
        }
        return pitchedSample;
    }

    private boolean partlyShifting(PitchedSample pitchedSample) {
        List<List<float[]>> andat = pitchedSample.getPitchAnalysis();
        PitchShifter ps = pitchedSample.getPitchShifter();
        float factor = ps.getPitchShift();
        ps.synthesize(andat, true, null);
        SynthesizeStatus stat = ps.getSynthesizeStatus();
        float[] stretched = Stretcher.stretch(factor, Arrays.copyOfRange(stat.getWorkload(), stat.getLastStart(), stat.getLastEnd()));
        pitchedSample.addPitchedPart(stretched);
        boolean complete = (stat.getLastEnd() == ps.getDataLength());
        if (complete) pitchedSample.isFinalLength();
        return complete;
    }

    private Sample finishShifting(PitchedSample pitchedSample) {
        List<List<float[]>> andat = pitchedSample.getPitchAnalysis();
        PitchShifter ps = pitchedSample.getPitchShifter();
        float factor = ps.getPitchShift();
        SynthesizeStatus stat = ps.getSynthesizeStatus();
        while ((stat == null) || (stat.getLastEnd() < ps.getDataLength())) {
            ps.synthesize(andat, true, null);
            stat = ps.getSynthesizeStatus();
            float[] stretched = Stretcher.stretch(factor, Arrays.copyOfRange(stat.getWorkload(), stat.getLastStart(), stat.getLastEnd()));
            pitchedSample.addPitchedPart(stretched);
        }
        return pitchedSample;
    }

    public void open() {
        try {
            tapper.registerHandler(new TapperCallback() {

                public void tapIs(int count) {
                    if (count == 4) {
                        new Thread() {

                            public void run() {
                                long a = System.currentTimeMillis();
                                double bpm = tapper.getBPM();
                                tapper.reset();
                                pitchSet.clear();
                                for (Button btn : buttons) {
                                    WaveSample sample = btn.getBaseSample();
                                    if (sample != null) {
                                        pitchSet.addPitch(btn, new PitchStatus(prepareShifting(sample, bpm, blockSize)));
                                    }
                                }
                                for (Button btn : pitchSet.buttons()) {
                                    btn.setPreparedSample(pitchSet.getStatus(btn).getPitchedSample());
                                }
                                long b = System.currentTimeMillis();
                                boolean allComplete = false;
                                while (!allComplete) {
                                    boolean completeStatus = true;
                                    for (Button btn : pitchSet.buttons()) {
                                        try {
                                            PitchStatus ps = pitchSet.getStatus(btn);
                                            if (ps != null && !ps.isCompleted()) {
                                                boolean completed = partlyShifting(ps.getPitchedSample());
                                                if (!completed) completeStatus = false; else {
                                                    ps.setCompleted(true);
                                                }
                                            }
                                        } catch (Exception e) {
                                            log("failed to shift partly for " + btn);
                                        }
                                    }
                                    allComplete = completeStatus;
                                }
                                pitchSet.conversionComplete();
                                long c = System.currentTimeMillis();
                                log("all complete! prepare = " + (b - a) + "ms, complete = " + (c - b) + "ms");
                            }
                        }.start();
                    }
                }
            });
            buttons.add(new Button("PLAY", 0));
            buttons.add(new Button("REC", 1));
            final Button btn1 = new Button("BTN1", 2, 1, false);
            btn1.setOwnBpm(true);
            buttons.add(btn1);
            final Button btn2 = new Button("BTN2", 3, 2, true);
            buttons.add(btn2);
            final Button btn3 = new Button("BTN3", 4, 2, true);
            buttons.add(btn3);
            final Button btn4 = new Button("BTN4", 5, 2, true);
            buttons.add(btn4);
            final Button btn5 = new Button("BTN5", 6, 2, true);
            buttons.add(btn5);
            buttons.add(new Button("BTN6", 7, 2, true));
            buttons.add(new Button("BTN7", 8, 2, true));
            buttons.add(new Button("BTN8", 9, 2, true));
            sampleManager.loadMultipleSamples(Map(o(btn1, "C://bassline.wav"), o(btn2, "C://Beat_A.wav"), o(btn3, "C://Beat_B.wav"), o(btn4, "C://Beat_C.wav"), o(btn5, "C://Beat_D.wav")));
            btn1.getBaseSample().setTempo(110);
            btn2.getBaseSample().setTempo(110);
            btn3.getBaseSample().setTempo(110);
            btn4.getBaseSample().setTempo(110);
            btn5.getBaseSample().setTempo(110);
            log("finished loading samples");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String s) {
        System.out.println(s);
    }

    private void tap() {
        log("tap");
        tapper.tap();
        log("tempo = " + tapper.getBPM() + ", " + tapper.getTap());
    }

    private void setState(State newState) {
        state = newState;
        log("new state: " + state);
    }

    private boolean isPlaying(Button btn) {
        return (isPlaying.contains(btn));
    }

    private void stopPlaying(Button btn) {
        log("stop playing " + btn);
        isPlaying.remove(btn);
        btn.resetSample();
        log("isPlaying.size = " + isPlaying.size());
    }

    private void startPlaying(Button btn) {
        List<Button> stopPlaying = new ArrayList<Button>();
        for (Button oldBtn : isPlaying) {
            if (oldBtn.getChannel() == btn.getChannel()) {
                if (!oldBtn.isLooping() || btn.isLooping()) {
                    log(oldBtn + " was on same channel, stop playing it");
                    stopPlaying.add(oldBtn);
                } else {
                    log(oldBtn + " was muted");
                }
            }
        }
        for (Button b : stopPlaying) stopPlaying(b);
        boolean remove;
        for (Button b : pitchSet.buttons()) {
            remove = false;
            if (btn.isOwnBpm() && b != btn) {
                remove = true;
            } else if (!btn.isOwnBpm() && b.isOwnBpm()) {
                remove = true;
            }
            if (remove) {
                pitchSet.remove(b);
                b.setPreparedSample(null);
            } else {
                b.setPitchedSample(b.getPreparedSample());
            }
        }
        pitchSet.buttonChosen();
        isPlaying.add(btn);
    }

    private String getFilename(Button btn) {
        String name = basePath + btn.getName() + ".SAM";
        int i = 1;
        while (new File(name).exists()) {
            name = basePath + btn.getName() + "-" + i + ".SAM";
            i++;
        }
        return name;
    }

    private void stopRecording(final Button btn) {
        log("stop recording " + btn);
        recording = false;
        final byte[] data = recorder.toByteArray();
        btn.setBaseSample(sampleManager.loadSample(data, getSampleRate()));
        recorder.reset();
        new Thread() {

            public void run() {
                String filename = getFilename(btn);
                try {
                    sampleManager.saveSample(data, filename, tapper.getBPM(), 1.0, SamplerFX.this.getSampleRate(), null);
                } catch (Exception e) {
                    log("failed to write file " + filename);
                    e.printStackTrace();
                }
                log("wrote file " + filename);
            }
        }.start();
    }

    private void startRecording(Button btn) {
        log("start recording " + btn);
        recording = true;
    }

    public void setParameter(final int param, final float value) {
        if (value > 0) log("param: " + System.currentTimeMillis());
        new Thread() {

            public void run() {
                Button btn = getButton(param);
                if (btn == null) {
                    log("unrecognised button " + param);
                    return;
                }
                if (value == 0) {
                    switch(state) {
                        case STATE_PLAY:
                            switch(btn.getParam()) {
                                case PLAY:
                                    break;
                                case REC:
                                    setState(STATE_REC);
                                    break;
                                default:
                                    activeBtn = btn;
                                    if (isPlaying(activeBtn)) stopPlaying(btn); else startPlaying(btn);
                                    break;
                            }
                            break;
                        case STATE_REC:
                            switch(btn.getParam()) {
                                case REC:
                                    if (recording) {
                                        stopRecording(activeBtn);
                                    }
                                    break;
                                case PLAY:
                                    if (recording) {
                                        stopRecording(activeBtn);
                                        startPlaying(activeBtn);
                                    }
                                    setState(STATE_PLAY);
                                    break;
                                default:
                                    if (!recording) {
                                        startRecording(btn);
                                    }
                                    activeBtn = btn;
                                    break;
                            }
                            break;
                    }
                } else {
                    switch(state) {
                        case STATE_PLAY:
                            switch(btn.getParam()) {
                                case PLAY:
                                    tap();
                                    break;
                            }
                            break;
                        case STATE_REC:
                            switch(btn.getParam()) {
                                case REC:
                                    if (!recording) {
                                        tap();
                                    }
                                    break;
                                default:
                                    if (recording && activeBtn.equals(btn)) {
                                        tap();
                                    }
                                    break;
                            }
                            break;
                    }
                }
            }
        }.start();
    }

    public void processReplacing(float[][] in, float[][] out, int sampleFrames) {
        blockSize = sampleFrames;
        if (recording) {
            try {
                for (float f : in[0]) {
                    int i = (int) (f * Integer.MAX_VALUE);
                    recorder.write(intToByteArray(i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < sampleFrames; i++) {
            out[0][i] = 0;
            for (Button btn : buttons) {
                if ((btn.getBaseSample() != null) && (isPlaying.contains(btn)) && (!btn.isLooping()) && (btn.valuesRemaining() == 0)) {
                    stopPlaying(btn);
                }
            }
            activeButtonPerChannel.clear();
            for (Button btn : isPlaying) {
                if (!activeButtonPerChannel.containsKey(btn.getChannel())) {
                    activeButtonPerChannel.put(btn.getChannel(), btn);
                } else {
                    Button chb = activeButtonPerChannel.get(btn.getChannel());
                    if (!btn.isLooping() && chb.isLooping()) activeButtonPerChannel.put(btn.getChannel(), btn);
                }
            }
            for (Button btn : activeButtonPerChannel.values()) {
                if (btn.getBaseSample() != null) {
                    float f = btn.nextValue();
                    out[0][i] += f;
                }
            }
            out[0][i] += in[0][i];
            if (out[0][i] > 1) out[0][i] = 1; else if (out[0][i] < -1) out[0][i] = -1;
        }
    }

    private Button getButton(int param) {
        for (Button btn : buttons) if (btn.getParam() == param) return btn;
        return null;
    }

    private byte[] intToByteArray(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16) };
    }
}
