package uk.org.toot.audio.server;

import java.util.List;
import java.util.Collections;
import javax.sound.sampled.*;
import uk.org.toot.audio.core.*;
import com.frinika.toot.PriorityAudioServer;

/**
 * JavaSoundAudioServer extends BasicAudioServer with JavaSound-style byte[]
 * buffer provision and management and JavaSound audio I/O provision.
 */
public class JavaSoundAudioServer extends PriorityAudioServer {

    private byte[] sharedByteBuffer;

    private int sampleSizeInBits = 16;

    private float sampleRate = 44100;

    private AudioFormat format;

    private List<JavaSoundAudioOutput> outputs;

    private List<JavaSoundAudioInput> inputs;

    private int lineBufferBytes = 32768;

    public JavaSoundAudioServer() {
        bufferFrames = calculateBufferFrames();
        outputs = new java.util.ArrayList<JavaSoundAudioOutput>();
        inputs = new java.util.ArrayList<JavaSoundAudioInput>();
    }

    protected void checkFormat() {
        if (format != null) return;
        format = new AudioFormat(getSampleRate(), getSampleSizeInBits(), 2, true, false);
        lineBufferBytes = format.getFrameSize() * (int) getSampleRate() / 5;
        sharedByteBuffer = createByteBuffer();
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public void setSampleSizeInBits(int sampleSizeInBits) {
        if (format != null) {
            throw new IllegalStateException("too late, format already set");
        }
        this.sampleSizeInBits = sampleSizeInBits;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(float sampleRate) {
        if (format != null) {
            throw new IllegalStateException("too late, format already set");
        }
        this.sampleRate = sampleRate;
    }

    public List<AudioLine> getOutputs() {
        return Collections.<AudioLine>unmodifiableList(outputs);
    }

    public List<AudioLine> getInputs() {
        return Collections.<AudioLine>unmodifiableList(inputs);
    }

    protected void resizeBuffers(int bufferFrames) {
        super.resizeBuffers(bufferFrames);
        sharedByteBuffer = createByteBuffer();
    }

    protected byte[] createByteBuffer() {
        checkFormat();
        byte[] ret = new byte[_createAudioBuffer("hack").getByteArrayBufferSize(format)];
        return ret;
    }

    public int getOutputLatencyFrames() {
        if (syncLine == null) return 0;
        return syncLine.getLatencyFrames();
    }

    public int getInputLatencyFrames() {
        if (inputs.size() == 0) return 0;
        return inputs.get(0).getLatencyFrames();
    }

    public List<String> getAvailableOutputNames() {
        List<String> names = new java.util.ArrayList<String>();
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        Mixer mixer;
        Line.Info[] lines;
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getName().startsWith("Port ")) continue;
            mixer = AudioSystem.getMixer(infos[i]);
            lines = mixer.getSourceLineInfo();
            if (lines.length > 0) {
                names.add(infos[i].getName());
            }
        }
        return names;
    }

    public List<String> getAvailableInputNames() {
        List<String> names = new java.util.ArrayList<String>();
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        Mixer mixer;
        Line.Info[] lines;
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getName().startsWith("Port ")) continue;
            mixer = AudioSystem.getMixer(infos[i]);
            lines = mixer.getTargetLineInfo();
            if (lines.length > 0) {
                names.add(infos[i].getName());
            }
        }
        return names;
    }

    protected Mixer.Info inputForName(String name) {
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        Mixer mixer;
        Line.Info[] lines;
        for (int i = 0; i < infos.length; i++) {
            mixer = AudioSystem.getMixer(infos[i]);
            lines = mixer.getTargetLineInfo();
            if (lines.length > 0) {
                if (infos[i].getName().indexOf(name) >= 0) {
                    return infos[i];
                }
            }
        }
        System.out.println("Oops, no input named " + name);
        return null;
    }

    protected Mixer.Info outputForName(String name) {
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        Mixer mixer;
        Line.Info[] lines;
        for (int i = 0; i < infos.length; i++) {
            mixer = AudioSystem.getMixer(infos[i]);
            lines = mixer.getSourceLineInfo();
            if (lines.length > 0) {
                if (infos[i].getName().indexOf(name) >= 0) {
                    return infos[i];
                }
            }
        }
        System.out.println("Oops, no output named " + name);
        return null;
    }

    protected void startImpl() {
        for (JavaSoundAudioLine input : inputs) {
            try {
                input.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (JavaSoundAudioLine output : outputs) {
            try {
                output.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.startImpl();
    }

    protected void stopImpl() {
        super.stopImpl();
        for (JavaSoundAudioLine output : outputs) {
            try {
                output.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (JavaSoundAudioLine input : inputs) {
            try {
                input.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public IOAudioProcess openAudioOutput(String name, String label) throws Exception {
        JavaSoundAudioOutput output;
        boolean wasRunning = isRunning;
        checkFormat();
        if (isRunning) stop();
        if (name == null) {
            name = getAvailableOutputNames().get(0);
            System.out.println(label + " null name specified, using " + name);
        }
        try {
            output = new JavaSoundAudioOutput(format, outputForName(name), label);
            output.open();
            outputs.add(output);
        } finally {
            if (wasRunning) {
                start();
            } else {
                checkStart();
            }
        }
        return output;
    }

    public void closeAudioOutput(IOAudioProcess output) {
        if (!(output instanceof JavaSoundAudioOutput)) {
            throw new IllegalArgumentException(output.getName() + " is not a JavaSoundAudioOutput");
        }
        JavaSoundAudioOutput jsoutput = (JavaSoundAudioOutput) output;
        if (jsoutput.isActive()) {
            jsoutput.stop();
        }
        jsoutput.close();
        outputs.remove(output);
    }

    public IOAudioProcess openAudioInput(String name, String label) throws Exception {
        JavaSoundAudioInput input;
        checkFormat();
        if (name == null) {
            name = getAvailableInputNames().get(0);
            System.out.println(label + " null name specified, using " + name);
        }
        input = new JavaSoundAudioInput(format, inputForName(name), label, name);
        input.open();
        inputs.add(input);
        if (isRunning) input.start();
        return input;
    }

    public void closeAudioInput(IOAudioProcess input) {
        if (!(input instanceof JavaSoundAudioInput)) {
            throw new IllegalArgumentException(input.getName() + " is not a JavaSoundAudioInput");
        }
        JavaSoundAudioInput jsinput = (JavaSoundAudioInput) input;
        if (jsinput.isActive()) {
            jsinput.stop();
        }
        jsinput.close();
        inputs.remove(input);
    }

    public void setLatencyMilliseconds(float ms) {
        if (ms < getLatencyMilliseconds()) {
            minimiseInputLatency();
        }
        super.setLatencyMilliseconds(ms);
    }

    protected void minimiseInputLatency() {
        for (JavaSoundAudioInput input : inputs) {
            input.flush();
        }
    }

    protected void controlGained() {
        minimiseInputLatency();
        super.controlGained();
    }

    protected abstract class JavaSoundAudioLine implements AudioLine {

        protected AudioFormat format;

        protected Mixer.Info mixerInfo;

        protected String label;

        protected int latencyFrames = -1;

        protected ChannelFormat channelFormat;

        public JavaSoundAudioLine(AudioFormat format, Mixer.Info info, String label) {
            this.format = format;
            mixerInfo = info;
            this.label = label;
            switch(format.getChannels()) {
                case 1:
                    channelFormat = ChannelFormat.MONO;
                    break;
                case 2:
                    channelFormat = ChannelFormat.STEREO;
                    break;
            }
        }

        public String getName() {
            return label;
        }

        public ChannelFormat getChannelFormat() {
            return channelFormat;
        }

        public int getLatencyFrames() {
            return latencyFrames;
        }

        public abstract void start() throws Exception;

        public abstract void stop() throws Exception;

        public abstract boolean isActive();
    }

    protected class JavaSoundAudioOutput extends JavaSoundAudioLine {

        protected SourceDataLine lineOut;

        protected DataLine.Info infoOut;

        protected long framesWritten = 0;

        public JavaSoundAudioOutput(AudioFormat format, Mixer.Info info, String label) throws LineUnavailableException {
            super(format, info, label);
            infoOut = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.getMixer(mixerInfo).isLineSupported(infoOut)) {
                throw new LineUnavailableException(info + " does not support " + infoOut);
            }
            if (syncLine == null) syncLine = this;
        }

        public void open() throws Exception {
            if (lineOut != null && lineOut.isOpen()) return;
            lineOut = (SourceDataLine) AudioSystem.getMixer(mixerInfo).getLine(infoOut);
            lineOut.open(format, lineBufferBytes);
            if (syncLine == this) {
                if (lineOut.getBufferSize() != lineBufferBytes) {
                    System.out.println("JavaSound Line buffer: " + lineBufferBytes + " bytes requested, " + lineOut.getBufferSize() + " bytes returned.");
                    lineBufferBytes = lineOut.getBufferSize();
                }
                maximumLatencyMilliseconds = 1000 * lineBufferBytes / format.getFrameSize() / format.getSampleRate();
                maximumLatencyMilliseconds -= 10;
            }
        }

        public void start() throws Exception {
            framesWritten = lineOut.getLongFramePosition();
            lineOut.start();
        }

        public void stop() {
            lineOut.stop();
            lineOut.flush();
        }

        public void close() {
            if (lineOut != null && lineOut.isOpen()) lineOut.close();
        }

        public int processAudio(AudioBuffer buffer) {
            if (!buffer.isRealTime()) return AUDIO_OK;
            int nbytes = buffer.convertToByteArray(sharedByteBuffer, 0, format);
            if (lineOut.available() > sharedByteBuffer.length) {
                lineOut.write(sharedByteBuffer, 0, nbytes);
                framesWritten += nbytes / format.getFrameSize();
            }
            long framePos = lineOut.getLongFramePosition();
            latencyFrames = (int) (framesWritten - framePos);
            if (latencyFrames < 0) {
                latencyFrames = 0;
            }
            return AUDIO_OK;
        }

        public boolean isActive() {
            if (lineOut == null) return false;
            return lineOut.isActive();
        }
    }

    protected class JavaSoundAudioInput extends JavaSoundAudioLine {

        protected TargetDataLine lineIn;

        protected DataLine.Info infoIn;

        protected AudioBuffer.MetaInfo metaInfo;

        protected long framesRead = 0;

        private boolean doFlush = false;

        public JavaSoundAudioInput(AudioFormat format, Mixer.Info info, String label, String location) throws LineUnavailableException {
            super(format, info, label);
            infoIn = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.getMixer(mixerInfo).isLineSupported(infoIn)) {
                throw new LineUnavailableException(mixerInfo + " does not support " + infoIn);
            }
            metaInfo = new AudioBuffer.MetaInfo(label, location);
        }

        public void open() throws Exception {
            if (lineIn != null && lineIn.isOpen()) return;
            lineIn = (TargetDataLine) AudioSystem.getMixer(mixerInfo).getLine(infoIn);
            lineIn.open(format, lineBufferBytes);
        }

        public void start() throws Exception {
            framesRead = lineIn.getLongFramePosition();
            lineIn.start();
        }

        public void stop() {
            lineIn.stop();
            lineIn.flush();
        }

        public void close() {
            if (lineIn != null && lineIn.isOpen()) lineIn.close();
        }

        public void flush() {
            doFlush = true;
        }

        public int processAudio(AudioBuffer buffer) {
            buffer.setMetaInfo(metaInfo);
            if (!buffer.isRealTime()) return AUDIO_DISCONNECT;
            buffer.setChannelFormat(channelFormat);
            int avail = lineIn.available();
            if (avail < sharedByteBuffer.length) {
                buffer.makeSilence();
            } else {
                latencyFrames = (int) (lineIn.getLongFramePosition() - framesRead);
                lineIn.read(sharedByteBuffer, 0, sharedByteBuffer.length);
                buffer.initFromByteArray(sharedByteBuffer, 0, sharedByteBuffer.length, format);
                framesRead += sharedByteBuffer.length / format.getFrameSize();
                if (doFlush) {
                    lineIn.flush();
                    long fp = lineIn.getLongFramePosition();
                    framesRead = fp;
                    latencyFrames = 0;
                    doFlush = false;
                }
            }
            return AUDIO_OK;
        }

        public boolean isActive() {
            if (lineIn == null) return false;
            return lineIn.isActive();
        }
    }

    public String getConfigKey() {
        return "javasound";
    }
}
