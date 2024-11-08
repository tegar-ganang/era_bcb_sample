package com.frinika.toot.javasoundmultiplexed;

import java.util.List;
import java.util.Collections;
import com.frinika.toot.PriorityAudioServer;
import uk.org.toot.audio.server.AudioLine;
import uk.org.toot.audio.server.IOAudioProcess;

/**
 * MultiplexedJavaSoundAudioServer
 * 
 * Provides a Server based on a single javasound DataLine for inout and output.
 * 
 * The widest (most channels) line is used.
 * 
 * The server provides IOAudioProcess that can be mono or stereo based on 1 or 2
 * channels of the target data line.
 * 
 */
public class MultiplexedJavaSoundAudioServer extends PriorityAudioServer {

    private JavaSoundInDevice inDevice;

    private JavaSoundOutDevice outDevice;

    private List<AudioLine> outputs;

    private List<AudioLine> inputs;

    private DeviceManager deviceManager;

    public MultiplexedJavaSoundAudioServer() {
        outputs = new java.util.ArrayList<AudioLine>();
        inputs = new java.util.ArrayList<AudioLine>();
        isRunning = false;
        bufferFrames = calculateBufferFrames();
        System.out.println(" MultiplexBufferSize " + bufferFrames);
        try {
            deviceManager = new DeviceManager(bufferFrames);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float getSampleRate() {
        return 44100.0f;
    }

    public int getSampleSizeInBits() {
        return 16;
    }

    public List<AudioLine> getOutputs() {
        return Collections.<AudioLine>unmodifiableList(outputs);
    }

    public List<AudioLine> getInputs() {
        return Collections.<AudioLine>unmodifiableList(inputs);
    }

    protected void resizeBuffers() {
        try {
            throw new Exception(" NO resizing implmented for the multiplexed server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getInDeviceList() {
        return deviceManager.getInDeviceList();
    }

    public List<String> getOutDeviceList() {
        return deviceManager.getOutDeviceList();
    }

    public void setOutDevice(String name) {
        outDevice = deviceManager.getOutDevice(name);
        outputs.clear();
        for (int i = 0; i < outDevice.getChannels() / 2; i++) {
            int chan[] = { i * 2, i * 2 + 1 };
            outputs.add(new StereoOutConnection(outDevice, chan));
        }
    }

    public void setInDevice(String name) {
        inDevice = deviceManager.getInDevice(name);
        inputs.clear();
        for (int i = 0; i < inDevice.getChannels(); i++) {
            inputs.add(new MonoInConnection(inDevice, i));
        }
        for (int i = 0; i < inDevice.getChannels() / 2; i++) {
            int chan[] = { i * 2, i * 2 + 1 };
            inputs.add(new StereoInConnection(inDevice, chan));
        }
    }

    public int getOutputLatencyFrames() {
        if (syncLine == null) return 0;
        return syncLine.getLatencyFrames();
    }

    public int getInputLatencyFrames() {
        if (inDevice == null) return 0;
        return inDevice.getLatencyFrames();
    }

    public List<String> getAvailableOutputNames() {
        List<String> names = new java.util.ArrayList<String>();
        for (AudioLine line : outputs) {
            names.add(line.getName());
        }
        return names;
    }

    public List<String> getAvailableInputNames() {
        List<String> names = new java.util.ArrayList<String>();
        for (AudioLine line : inputs) {
            names.add(line.getName());
        }
        return names;
    }

    public void start() {
        if (isRunning) return;
        if (inDevice != null) try {
            inDevice.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (outDevice != null) try {
            outDevice.start();
            syncLine = outDevice;
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.start();
    }

    public void stop() {
        if (!isRunning) return;
        if (inDevice != null) try {
            inDevice.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (outDevice != null) try {
            outDevice.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.stop();
    }

    public IOAudioProcess openAudioOutput(String name, String label) throws Exception {
        for (AudioLine line : outputs) {
            if (name.equals(line.getName())) return line;
        }
        return null;
    }

    public void closeAudioOutput(IOAudioProcess output) {
    }

    public IOAudioProcess openAudioInput(String name, String label) throws Exception {
        for (AudioLine line : inputs) {
            if (name.equals(line.getName())) return line;
        }
        return null;
    }

    public void closeAudioInput(IOAudioProcess input) {
    }

    public void setLatencyMilliseconds(float ms) {
        if (ms < getLatencyMilliseconds()) {
            flushInputs();
        }
        super.setLatencyMilliseconds(ms);
    }

    protected void flushInputs() {
        if (inDevice != null) inDevice.flush();
    }

    protected void controlGained() {
        flushInputs();
    }

    public String getConfigKey() {
        return "java_multiplexed";
    }

    public void work() {
        if (inDevice != null) inDevice.fillBuffer();
        if (outDevice != null) outDevice.zeroBuffer();
        super.work();
        if (outDevice != null) outDevice.writeBuffer();
    }
}
