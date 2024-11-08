package com.frinika.sequencer.model;

import com.frinika.audio.toot.AudioPeakMonitor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.sound.sampled.AudioFormat;
import javax.swing.Icon;
import rasmus.midi.provider.RasmusSynthesizer;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.mixer.MixControls;
import uk.org.toot.audio.server.AudioServer;
import uk.org.toot.audio.server.IOAudioProcess;
import com.frinika.project.FrinikaAudioSystem;
import com.frinika.global.FrinikaConfig;
import com.frinika.project.ProjectContainer;
import com.frinika.sequencer.FrinikaSequencer;
import com.frinika.sequencer.SequencerListener;
import com.frinika.audio.io.AudioWriter;
import static com.frinika.localization.CurrentLocale.getMessage;

public class AudioLane extends Lane implements RecordableLane, SequencerListener {

    transient AudioProcess audioInProcess = null;

    protected transient AudioProcess audioInsert = null;

    static Icon icon = new javax.swing.ImageIcon(RasmusSynthesizer.class.getResource("/icons/audiolane.png"));

    public static int stripNo = 1;

    /**
	 * Audio Process to be connected to the project mixer
	 */
    transient AudioProcess audioProcess;

    transient AudioPeakMonitor peakMonitor;

    transient boolean armed = false;

    transient boolean isRecording = false;

    transient boolean hasRecorded = false;

    transient AudioWriter writer = null;

    private transient long recordStartTimeInMicros;

    private transient FrinikaSequencer sequencer;

    private transient MixControls mixerControls = null;

    transient int stripInt = -1;

    private static final long serialVersionUID = 1L;

    protected transient File clipFile;

    static int nameCount = 0;

    public AudioLane(ProjectContainer project) {
        super("Audio " + nameCount++, project);
        attachAudioProcessToMixer();
    }

    public void dispose() {
        project.getSequencer().removeSequencerListener(this);
        writer.discard();
    }

    public void removeFromModel() {
        project.removeStrip(stripInt + "");
        super.removeFromModel();
    }

    private void attachAudioProcessToMixer() {
        peakMonitor = new AudioPeakMonitor();
        audioProcess = new AudioProcess() {

            public void close() {
            }

            public void open() {
            }

            public int processAudio(AudioBuffer buffer) {
                if (armed) {
                    audioInProcess.processAudio(buffer);
                    peakMonitor.processAudio(buffer);
                    if (audioInsert != null) audioInsert.processAudio(buffer);
                    if (isRecording) {
                        writer.processAudio(buffer);
                        hasRecorded = true;
                    }
                    if (FrinikaConfig.getDirectMonitoring()) {
                        buffer.makeSilence();
                    }
                } else {
                    if (project.getSequencer().isRunning()) {
                        buffer.setChannelFormat(ChannelFormat.STEREO);
                        buffer.makeSilence();
                        for (Part part : getParts()) {
                            if (((AudioPart) part).getAudioProcess() != null) ((AudioPart) part).getAudioProcess().processAudio(buffer);
                        }
                        peakMonitor.processAudio(buffer);
                    } else {
                        buffer.makeSilence();
                    }
                }
                buffer.setMetaInfo(channelLabel);
                return AUDIO_OK;
            }
        };
        try {
            mixerControls = project.addMixerInput(audioProcess, (stripInt = stripNo++) + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        sequencer = project.getSequencer();
        sequencer.addSequencerListener(this);
    }

    public void restoreFromClone(EditHistoryRecordable object) {
        System.out.println("AudioLane restroeFromClone");
    }

    public Selectable deepCopy(Selectable parent) {
        return null;
    }

    public void deepMove(long tick) {
    }

    public boolean isRecording() {
        return armed;
    }

    public boolean isMute() {
        return mixerControls.isMute();
    }

    public boolean isSolo() {
        return mixerControls.isSolo();
    }

    public void setRecording(boolean b) {
        if (b && audioInProcess == null) {
            armed = false;
            project.message(getMessage("recording.please_select_audio_input"));
            return;
        }
        armed = b;
    }

    public void setMute(boolean b) {
        mixerControls.getMuteControl().setValue(b);
    }

    public void setSolo(boolean b) {
        mixerControls.getSoloControl().setValue(b);
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        attachAudioProcessToMixer();
    }

    public AudioProcess getAudioInDevice() {
        return audioInProcess;
    }

    public void setAudioInDevice(AudioProcess handle) {
        audioInProcess = handle;
        if (writer != null) writer.close();
        writer = newAudioWriter();
    }

    public double getMonitorValue() {
        return peakMonitor.getPeak();
    }

    /**
	 * 
	 * Creates a new audio file handle to save a clip.
	 * 
	 */
    public AudioWriter newAudioWriter() {
        clipFile = newFilename();
        AudioFormat format = new AudioFormat(FrinikaConfig.sampleRate, 16, ((IOAudioProcess) audioInProcess).getChannelFormat().getCount(), true, false);
        try {
            return new AudioWriter(clipFile, format);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File newFilename() {
        ProjectContainer proj = getProject();
        File audioDir = proj.getAudioDirectory();
        String audioFileName = getName() + ".wav";
        File clipFile = new File(audioDir, audioFileName);
        int cnt = 1;
        while (clipFile.exists()) {
            audioFileName = getName() + "_" + (cnt++) + ".wav";
            clipFile = new File(audioDir, audioFileName);
        }
        return clipFile;
    }

    public void beforeStart() {
    }

    public void start() {
        isRecording = project.getSequencer().isRecording();
        if (isRecording) {
            recordStartTimeInMicros = sequencer.getMicrosecondPosition();
        }
    }

    public void stop() {
        isRecording = false;
        if (hasRecorded) {
            project.getEditHistoryContainer().mark(getMessage("sequencer.audiolane.record"));
            writer.close();
            hasRecorded = false;
            AudioServer server = project.getAudioServer();
            int latencyInframes = project.getAudioServer().getTotalLatencyFrames();
            System.out.println(" latency in frames is " + latencyInframes);
            double latencyInMicros = latencyInframes * 1000000.0 / server.getSampleRate();
            recordStartTimeInMicros -= latencyInMicros;
            AudioPart part;
            try {
                part = new AudioPart(this, writer.getFile(), recordStartTimeInMicros);
                part.onLoad();
                writer = newAudioWriter();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            project.getEditHistoryContainer().notifyEditHistoryListeners();
        }
    }

    public MixControls getMixerControls() {
        return mixerControls;
    }

    /**
	 * 
	 */
    @Override
    public Part createPart() {
        try {
            throw new Exception(" Attempt to create an AudiPart");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }
}
