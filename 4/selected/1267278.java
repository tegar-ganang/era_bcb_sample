package net.sourceforge.entrainer.eeg.nia;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import net.sourceforge.entrainer.eeg.core.AbstractEEGDevice;
import net.sourceforge.entrainer.eeg.core.EEGChannelState;
import net.sourceforge.entrainer.eeg.core.EEGDevice;
import net.sourceforge.entrainer.eeg.core.EEGException;
import net.sourceforge.entrainer.eeg.core.EEGRuntimeException;
import net.sourceforge.entrainer.eeg.core.EntrainerEEGFactory;
import net.sourceforge.entrainer.util.Utils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ch.ntb.usb.Device;
import ch.ntb.usb.USB;
import ch.ntb.usb.USBException;

/**
 * Implementation of {@link EEGDevice} for the OCZ NIA. Do not instantiate
 * directly; use {@link EntrainerEEGFactory} and set the property:<br>
 * <br>
 * device.class=net.sourceforge.entrainer.eeg.nia.NIA<br>
 * <br>
 * in the device.properties file included in the distribution. Using this class
 * directly will tie one's implementation to this class and to the NIA.
 * 
 * @author burton
 * @see EntrainerEEGFactory
 * @see AbstractEEGDevice
 * @see EEGDevice
 */
public class NIA extends AbstractEEGDevice {

    private static final long serialVersionUID = -2190714925262661731L;

    private static Logger log = LogManager.getLogger(NIA.class);

    public static final short NIA_VENDOR = (short) 0x1234;

    public static final short NIA_DEVICE = (short) 0x0;

    public static final int NIA_ENDPOINT_1 = 0x81;

    public static final int NIA_ENDPOINT_2 = 0x1;

    private Device niaDevice;

    private NIASignalProcessor signalProcessor = new NIASignalProcessor();

    private ExecutorService threadService = Executors.newCachedThreadPool();

    private double[] currentFrequencies = new double[45];

    private ReentrantLock lock = new ReentrantLock();

    public NIA() {
        super("OCZ Neural Impulse Actuator (TM)");
        setStatusOfDevice("NIA device initialized");
    }

    public void openDevice(long millisBetweenReads) throws EEGException {
        if (millisBetweenReads <= 0) {
            throw new EEGException("millisBetweenReads must be > 0");
        }
        setMillisBetweenReads(millisBetweenReads);
        if (getNiaDevice() == null) {
            setNiaDevice(USB.getDevice(NIA_VENDOR, NIA_DEVICE));
        }
        if (getNiaDevice().isOpen()) {
            setOpen(true);
            return;
        }
        try {
            openNia();
        } catch (USBException e) {
            setStatusOfDevice("Could not open NIA");
            throwEEGException(getStatusOfDevice(), e);
        }
        startNiaReadThreads();
    }

    public void closeDevice() throws EEGException {
        if (getNiaDevice() == null || !getNiaDevice().isOpen()) {
            setOpen(false);
            return;
        }
        try {
            closeNia();
        } catch (USBException e) {
            setStatusOfDevice("Could not close NIA");
            throwEEGException(getStatusOfDevice(), e);
        }
    }

    public void calibrate() throws EEGException {
        setCalibrating(true);
        setStatusOfDevice("NIA is calibrating");
        setCalibrating(false);
        setStatusOfDevice("NIA is calibrated");
    }

    private void startNiaReadThreads() {
        readThread();
        statusThread();
        notificationThread();
        processSignalThread();
    }

    private void notificationThread() {
        Runnable notifyRunnable = new Runnable() {

            public void run() {
                while (isOpen()) {
                    Utils.snooze(getMillisBetweenReads());
                    if (isOpen()) {
                        notifyEEGReadListeners();
                    }
                }
            }
        };
        threadService.execute(notifyRunnable);
    }

    private void statusThread() {
        Runnable statusRunnable = new Runnable() {

            public void run() {
                setStatusOfDevice("NIA reading started...");
                while (isOpen() && !signalProcessor.isFull()) {
                    Utils.snooze(200);
                }
                setStatusOfDevice("NIA output available");
            }
        };
        threadService.execute(statusRunnable);
    }

    private void readThread() {
        Runnable readRunnable = new Runnable() {

            public void run() {
                while (isOpen()) {
                    Utils.snooze(0, 10);
                    if (isOpen()) {
                        readNia();
                    }
                }
            }
        };
        threadService.execute(readRunnable);
    }

    private void processSignalThread() {
        Runnable dspRunnable = new Runnable() {

            public void run() {
                while (isOpen()) {
                    Utils.snooze(getMillisBetweenReads());
                    if (isOpen()) {
                        if (signalProcessor.isFull()) {
                            applyProcessedSignal(signalProcessor.getFilteredData());
                        }
                    }
                }
            }
        };
        threadService.execute(dspRunnable);
    }

    private void readNia() {
        Runnable readRunnable = new Runnable() {

            public void run() {
                byte[] buf = new byte[55];
                try {
                    if (isOpen()) {
                        int numBytes = getNiaDevice().readBulk(NIA_ENDPOINT_1, buf, buf.length, 2000, false);
                        processData(buf, numBytes);
                    }
                } catch (USBException e) {
                    setStatusOfDevice("Could not read NIA");
                    throwRuntimeEEGException(getStatusOfDevice(), e);
                }
            }
        };
        threadService.execute(readRunnable);
    }

    private void processData(final byte[] data, int numBytes) {
        lock.lock();
        try {
            int numSamples = getNumberOfSamples(data);
            double[] samples = new double[numSamples];
            for (int b = 0; b < numSamples; b++) {
                double d = getSample(data, b);
                samples[b] = d;
            }
            signalProcessor.addBytes(samples);
        } finally {
            lock.unlock();
        }
    }

    private void applyProcessedSignal(double[] filteredData) {
        for (int i = 0; i < currentFrequencies.length; i++) {
            currentFrequencies[i] = Math.abs(filteredData[i]);
        }
        setChannels();
    }

    private void setChannels() {
        for (EEGChannelState state : getChannelStates()) {
            setChannelValue(state);
        }
    }

    private void setChannelValue(EEGChannelState state) {
        setChannel(state.getFrequencyType(), getChannelStrength(state.getRangeFrom()));
    }

    private double getChannelStrength(double rangeFrom) {
        return currentFrequencies[(int) rangeFrom - 1];
    }

    private int getNumberOfSamples(byte[] data) {
        return data[54];
    }

    @SuppressWarnings("unused")
    private int getPacketTimer(byte[] data) {
        return data[53] * 256 + data[52] - getNumberOfSamples(data);
    }

    private double getSample(byte[] data, int sampleNumber) {
        return data[sampleNumber * 3 + 2] * 65536 + data[sampleNumber * 3 + 1] * 256 + data[sampleNumber * 3];
    }

    private void openNia() throws USBException {
        getNiaDevice().open(1, 0, -1);
        setOpen(true);
        setStatusOfDevice("NIA device is open");
    }

    private void closeNia() throws USBException {
        getNiaDevice().close();
        setOpen(false);
        signalProcessor.clear();
        currentFrequencies = new double[45];
        setStatusOfDevice("NIA device is closed");
    }

    protected Device getNiaDevice() {
        return niaDevice;
    }

    protected void setNiaDevice(Device niaDevice) {
        this.niaDevice = niaDevice;
    }

    private void throwEEGException(String message, Throwable cause) throws EEGException {
        log.error(message, cause);
        throw new EEGException(message, cause);
    }

    private void throwRuntimeEEGException(String message, Throwable cause) {
        log.error(message, cause);
        throw new EEGRuntimeException(message, cause);
    }
}
