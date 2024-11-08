package net.sourceforge.geeboss.model.midi.roland;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import net.sourceforge.geeboss.GeebossContainer;
import net.sourceforge.geeboss.controller.thread.MidiThread;
import net.sourceforge.geeboss.model.editors.MemoryEditorsManager;
import net.sourceforge.geeboss.model.event.ValueChangeListener;
import net.sourceforge.geeboss.model.midi.StampedMidiMessage;
import net.sourceforge.geeboss.model.midi.memory.MemoryComponent;
import net.sourceforge.geeboss.model.midi.memory.MemoryObject;
import net.sourceforge.geeboss.model.midi.memory.MemoryValue;
import net.sourceforge.geeboss.model.midi.sysex.SysexMemoryMessage;
import net.sourceforge.geeboss.model.patch.Patch;
import net.sourceforge.geeboss.model.stringtable.StringTableManager;
import net.sourceforge.geeboss.view.i18n.I18nUtil;

/**
 * Base class for roland midi devices
 * @author <a href="mailto:fborry@free.fr">Frederic BORRY</a>
 */
public abstract class RolandDevice extends RolandVirtualDevice implements Receiver, ValueChangeListener, Initializable {

    /** Time to wait before sending a schedule memory component update */
    private static long SCHEDULED_REQUEST_DELAY = 500;

    /** Reference to StringTableManager */
    private StringTableManager mStringTableManager;

    /** Reference to the EditorsManager */
    private MemoryEditorsManager mEditorsManager;

    /** Refrence to the midi thread */
    private MidiThread mMidiThread;

    /** Current patch number msb */
    private int mCurrentPatchNumberMsb;

    /**
     * Create a new RolandDevice
     * @param container the GeebossContainer
     */
    public RolandDevice(GeebossContainer container) throws ConfigurationException {
        super(container);
        mMidiThread = container.getMidiThread();
        mMidiThread.setReceiver(this);
        mStringTableManager = new StringTableManager(getStringTablesConfiguration());
        mCurrentPatchNumberMsb = -1;
    }

    /**
     * Returns true if this virtual device is file
     * @return true if this virtual device is file
     */
    public boolean isFile() {
        return false;
    }

    /**
     * Initialize this component
     */
    public void initialize() throws Exception {
        mEditorsManager = new MemoryEditorsManager(getEditorsConfiguration(), mContainer);
    }

    /**
     * Getter for string table manager
     * @return the string table manager
     */
    public StringTableManager getStringTableManager() {
        return mStringTableManager;
    }

    /**
     * Getter for editors manager
     * @return the editors manager
     */
    public MemoryEditorsManager getEditorsManager() {
        return mEditorsManager;
    }

    /**
     * Getter for temporary patch
     * @return the temporary patch for this device
     */
    public abstract Patch getTemporaryPatch();

    /**
     * Get the implementation specific string tables configuration
     * @return the implementation specific string tables configuration
     */
    public abstract Configuration getStringTablesConfiguration() throws ConfigurationException;

    /**
     * Get the implementation specific editors configuration
     * @return the implementation specific editors configuration
     */
    public abstract Configuration getEditorsConfiguration() throws ConfigurationException;

    /**
     * Create a new instance of the implementation specific file device
     * @param container the application container
     * @param file the file associated to the file device instance
     * @return a new instance of the implementation specific file device
     */
    protected abstract RolandFile createFileInstance(GeebossContainer container, File file) throws ConfigurationException;

    /**
	 * Load a sysex file and create a new instance of the device from this file
	 * @param sysexFile the file to load
	 * @return a new instance of the device loaded from the file
	 * @throws IllegalArgumentException if the file format was incorrect
	 */
    public RolandFile loadFromFile(File sysexFile) throws IllegalArgumentException {
        RolandFile result;
        try {
            result = createFileInstance(mContainer, sysexFile);
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException("Could not instanciate device.", e);
        }
        try {
            FileInputStream stream = new FileInputStream(sysexFile);
            FileChannel channel = stream.getChannel();
            ByteBuffer buffer = null;
            try {
                buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                int index = 0;
                while (buffer.hasRemaining()) {
                    if (buffer.get(index) == ((byte) SysexMessage.SYSTEM_EXCLUSIVE)) {
                        boolean f7Found = false;
                        index++;
                        int frameSize = 1;
                        while (!f7Found && buffer.hasRemaining()) {
                            if (buffer.get(index) == ((byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE)) {
                                f7Found = true;
                            }
                            frameSize++;
                            index++;
                        }
                        if (f7Found) {
                            byte[] frameData = new byte[frameSize];
                            buffer.get(frameData);
                            result.loadSysexData(frameData);
                        } else {
                            Locale language = mContainer.getSettings().getLocale();
                            throw new IllegalArgumentException(I18nUtil.getI18nString("error.missing.f7", language));
                        }
                    } else {
                        Locale language = mContainer.getSettings().getLocale();
                        throw new IllegalArgumentException(I18nUtil.getI18nString("error.missing.f0", language));
                    }
                }
            } finally {
                buffer = null;
                System.gc();
                stream.close();
                channel.close();
            }
        } catch (FileNotFoundException e) {
            Locale language = mContainer.getSettings().getLocale();
            throw new IllegalArgumentException(I18nUtil.getI18nString("error.sysex.fnf", language, sysexFile.getName()), e);
        } catch (IOException e) {
            Locale language = mContainer.getSettings().getLocale();
            throw new IllegalArgumentException(I18nUtil.getI18nString("error.sysex.io", language, sysexFile.getName()), e);
        }
        return result;
    }

    /**
     * Send a sysex message to the device
     * @param message the message to send
     */
    public void sendSysexMessage(byte[] message) {
        sendSysexMessage(message, false, 0);
    }

    /**
     * Send a sysex message to the device
     * @param message the message to send
     * @param waitForAck if true wait for data from the device before sending another message
     * @param delay the number of ms to wait before sending the message (if another message is sent before that delay, it will discard this one)
     */
    public void sendSysexMessage(byte[] message, boolean waitForAck, long delay) {
        byte[] data = buildSysexFrame(message);
        SysexMessage sysexMessage = new SysexMessage();
        try {
            sysexMessage.setMessage(data, data.length);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException("Invalid content for sysex message.", e);
        }
        mMidiThread.send(new StampedMidiMessage(sysexMessage, -1, waitForAck), delay);
    }

    /**
     * Send a midi message to the device
     * @param status the status for that short message
     * @param data1 the data1 for that short message
     * @param data2 the data2 for that short message
     */
    public void sendShortMessage(int status, int data1, int data2) {
        ShortMessage shortMessage = new ShortMessage();
        try {
            shortMessage.setMessage(status, data1, data2);
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException("Invalid content for short message.", e);
        }
        mMidiThread.send(new StampedMidiMessage(shortMessage, -1, false), 0);
    }

    /**
     * Send a patch change message to the device
     * @param bankIndex the index of the bank
     * @param patchIndex the index of the patch in the bank
     */
    public void sendPatchChangeRequest(int bankIndex, int patchIndex) {
        int[] patchMsbAndProgramChange = patchNumberToMidi(bankIndex, patchIndex);
        sendShortMessage(ShortMessage.CONTROL_CHANGE, 0x00, patchMsbAndProgramChange[0]);
        sendShortMessage(ShortMessage.CONTROL_CHANGE, 0x20, 0x00);
        sendShortMessage(ShortMessage.PROGRAM_CHANGE, patchMsbAndProgramChange[1], 0x00);
    }

    /**
     * Send a data set 1 command
     * @param address the address
     * @param data data bytes
     */
    public void dataSet1(long address, byte[] data) {
        sendSysexMessage(buildDataSet1Command(address, data));
    }

    /**
     * Send a request data 1 command
     * @param address the address
     * @param size the size
     * @param delay the number of ms to wait before sending the request (if another message is sent before that delay, it will discard this one)
     */
    public void scheduleRequestData1(long address, long size, long delay) {
        sendSysexMessage(buildRequestData1Command(address, size), true, delay);
    }

    /**
     * Send a request data 1 command
     * @param address the address
     * @param size the size
     */
    public void requestData1(long address, long size) {
        sendSysexMessage(buildRequestData1Command(address, size), true, 0);
    }

    /**
     * Send the data associated to a memory component to the device
     * @param component the component to send
     */
    public void sendMemoryComponent(MemoryComponent component) {
        if (component.isValue()) {
            sendMemoryValue((MemoryValue) component);
        } else {
            sendMemoryObject((MemoryObject) component);
        }
    }

    /**
     * Send the data associated to a memory object to the device
     * @param object the object to send
     */
    public void sendMemoryObject(MemoryObject object) {
        sendMemoryMessages(sysexMemoryObject(object));
    }

    /**
     * Send memory obejcts that have been modified
     */
    public void sendModifiedMemoryObjects() {
        for (MemoryObject modifiedObject : mModifiedObjects) {
            sendMemoryObject(modifiedObject);
        }
        mModifiedObjects.clear();
    }

    /**
     * Send the data associated to a memory value to the device
     * @param value the value to send
     */
    public void sendMemoryValue(MemoryValue value) {
        SysexMemoryMessage message = new SysexMemoryMessage(value.getAbsoluteAddress(), (int) value.getSize());
        value.toSysex(Collections.singletonList(message));
        sendMemoryMessage(message);
    }

    /**
     * Request the data associated to a memory component from the device
     * @param component the component we want the data for
     */
    public void requestMemoryComponent(MemoryComponent component) {
        requestData1(component.getAbsoluteAddress(), component.getSize());
    }

    /**
     * Request the data associated to a memory component from the device
     * @param component the component we want the data for
     */
    public void scheduleRequestMemoryComponent(MemoryComponent component) {
        scheduleRequestData1(component.getAbsoluteAddress(), component.getSize(), SCHEDULED_REQUEST_DELAY);
    }

    /**
     * Send memory messages to the device
     * @param messages the messages to send
     */
    public void sendMemoryMessages(List<SysexMemoryMessage> messages) {
        for (SysexMemoryMessage message : messages) {
            sendMemoryMessage(message);
        }
    }

    /**
     * Send the provided sysex memory message to the device
     * @param message the message to senb
     */
    public void sendMemoryMessage(SysexMemoryMessage message) {
        byte[] data = new byte[message.getSize()];
        List<Byte> buffer = message.getData();
        for (int index = 0; index < data.length; index++) {
            data[index] = buffer.get(index).byteValue();
        }
        dataSet1(message.getAddress(), data);
    }

    /**
     * Receive data from the midi thread
     */
    public void send(MidiMessage message, long timeStamp) {
        if (message.getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            byte[] data = message.getMessage();
            loadSysexData(data);
        } else if (message.getStatus() == ShortMessage.CONTROL_CHANGE) {
            ShortMessage controlChange = (ShortMessage) message;
            if (controlChange.getData1() == 0x00) {
                mCurrentPatchNumberMsb = controlChange.getData2();
            }
        } else if (message.getStatus() == ShortMessage.PROGRAM_CHANGE) {
            ShortMessage programChange = (ShortMessage) message;
            if (mCurrentPatchNumberMsb >= 0 && mCurrentPatchNumberMsb <= 3) {
                int[] bankAndPatchNumber = midiToPatchNumber(mCurrentPatchNumberMsb, programChange.getData1());
                getEventManager().selectPatch(bankAndPatchNumber[0], bankAndPatchNumber[1]);
            }
        }
    }

    /**
     * Convert midi patch change information to bank and patch number 
     * @param patchNumberMsb the patch number MSB value
     * @param programChangeValue the program change value (i.e. patch number LSB)
     * @return an two interger array containing the bank number and the patch number
     */
    public static int[] midiToPatchNumber(int patchNumberMsb, int programChangeValue) {
        int[] result = new int[2];
        int patchNumber = (patchNumberMsb * 100) + programChangeValue;
        result[0] = (patchNumber / PATCH_IN_BANK) + 1;
        result[1] = (patchNumber % PATCH_IN_BANK) + 1;
        return result;
    }

    /**
     * Convert bank and patch number to patchNumber MSB and program change value for a midi patch change message 
     * @param bankNumber the bank number
     * @param patchNumber the patch number
     * @return an two interger array containing the patchNumber MSB and program change value
     */
    public static int[] patchNumberToMidi(int bankNumber, int patchNumber) {
        int[] result = new int[2];
        int absolutePatchNumber = ((bankNumber - 1) * PATCH_IN_BANK) + (patchNumber - 1);
        result[0] = absolutePatchNumber / 100;
        result[1] = absolutePatchNumber % 100;
        return result;
    }

    /**
     * Receive a close command fromt the midi thread
     */
    public void close() {
    }

    /**
     * Notify the listener that the provided value (or object) has changed 
     * @param value the changed value (or object)
     */
    public void valueChanged(MemoryComponent value) {
        sendMemoryComponent(value);
    }
}
