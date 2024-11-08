package keyboardhero;

import java.util.*;
import javax.sound.midi.*;
import javax.sound.midi.MidiDevice.Info;
import keyboardhero.Util.*;

/**
 * Connects to a specified MIDI device and allow for communication between the program and the
 * device.
 */
final class MidiDevicer {

    static final class Device {

        private static int max = 0;

        private static final HashMap<Device, Integer> INSTANCES = new HashMap<Device, Integer>();

        private static boolean changes = false;

        private static boolean refreshInProgress = false;

        final int id;

        final MidiDevice device;

        final MidiDevice.Info info;

        private String name;

        final String configKey;

        final boolean hasInput, hasOutput;

        private boolean inputConnected, songConnected, outputConnected;

        private boolean inputSetConnected, songSetConnected, outputSetConnected;

        private Receiver receiver;

        final boolean isSynthesizer;

        boolean notFound = false;

        final Synthesizer synthesizer;

        private int[] programs = new int[MidiSong.MAX_NUM_OF_CHANNELS];

        private boolean[] channels = new boolean[MidiSong.MAX_NUM_OF_CHANNELS];

        private Device(MidiDevice device, Info info, boolean hasInput, boolean hasOutput) {
            this.name = ((this.info = info) == null ? null : info.getName());
            Integer count = INSTANCES.get(this);
            if (count != null && count != 0) {
                name += " [" + (++count) + "]";
                INSTANCES.put(this, count);
            }
            INSTANCES.put(this, 1);
            int foundDevice = DEVICES.indexOf(this);
            if (foundDevice != -1) {
                DEVICES.get(foundDevice).notFound = false;
                this.id = -1;
                this.device = null;
                this.hasInput = false;
                this.hasOutput = false;
                isSynthesizer = false;
                synthesizer = null;
                configKey = null;
                return;
            }
            changes = true;
            this.id = ++max;
            this.device = device;
            this.hasInput = hasInput;
            this.hasOutput = hasOutput;
            if (device instanceof Synthesizer) {
                synthesizer = (Synthesizer) device;
                isSynthesizer = true;
            } else {
                synthesizer = null;
                isSynthesizer = false;
            }
            for (byte i = 0; i < MidiSong.MAX_NUM_OF_CHANNELS; ++i) {
                programs[i] = 0;
            }
            DEVICES.add(this);
            if (info == null) {
                configKey = "Device_" + name + "|null";
            } else {
                configKey = "Device_" + name + "|" + info.getDescription() + "|" + info.getVendor() + "|" + info.getVersion();
            }
            final String parts[] = Util.getProp(configKey).split("\\|");
            if (parts.length >= 1 && parts[0].length() != 0 && parts[0].charAt(0) == 'y') connectInputDevice(this);
            if (parts.length >= 2 && parts[1].length() != 0 && parts[1].charAt(0) == 'y') connectSongDevice(this);
            if (parts.length >= 3 && parts[2].length() != 0 && parts[2].charAt(0) == 'y') connectOutputDevice(this);
        }

        private boolean open() {
            try {
                if (device != null) device.open();
                if (receiver == null) {
                    receiver = (device == null ? MidiSystem.getReceiver() : device.getReceiver());
                    if (receiver == null) {
                        Util.conditionalError(DEFAULT_DEVICE_UNAVAILABLE, "Err_DefaultDeviceUnavailable", '(' + getName() + ')');
                        return false;
                    }
                }
                return true;
            } catch (MidiUnavailableException e) {
                Util.conditionalError(MIDI_UNAVALIABLE, "Err_MidiUnavailable", '(' + getName() + ") " + e.getLocalizedMessage());
                return false;
            }
        }

        private void close() {
            final StringBuffer buff = new StringBuffer(5);
            buff.append(inputSetConnected ? 'y' : 'n');
            buff.append('|');
            buff.append(songSetConnected ? 'y' : 'n');
            buff.append('|');
            buff.append(outputSetConnected ? 'y' : 'n');
            Util.setProp(configKey, buff.toString());
            if (device != null) device.close();
            receiver = null;
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (o instanceof Device) {
                Device d = (Device) o;
                if (info == null) return d.info == null;
                if (d.info == null) return false;
                return Util.equals(name, d.name) && (Util.equals(info.getVendor(), d.info.getVendor()) && Util.equals(info.getDescription(), d.info.getDescription()) && Util.equals(info.getVersion(), d.info.getVersion()));
            }
            return false;
        }

        private static final boolean startRefresh() {
            if (refreshInProgress) return false;
            INSTANCES.clear();
            for (Device device : DEVICES) {
                device.notFound = true;
            }
            return true;
        }

        private static final void endRefresh() {
            Iterator<Device> iterator = DEVICES.iterator();
            while (iterator.hasNext()) {
                Device device = iterator.next();
                if (device.notFound) {
                    device.close();
                    iterator.remove();
                    changes = true;
                }
            }
            refreshInProgress = false;
        }

        final boolean isInputConnected() {
            return inputConnected;
        }

        final boolean isSongConnected() {
            return songConnected;
        }

        final boolean isOutputConnected() {
            return outputConnected;
        }

        final String getName() {
            return name;
        }
    }

    static final byte[] CONTROLLERS = new byte[] { 3, 9, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 80, 81, 82, 83, 85, 86, 87, 88, 89, 90, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119 };

    static final int REFRESH_TIME = 20;

    private static final Vector<Device> DEVICES = new Vector<Device>();

    static final List<Device> DEVICELIST = Collections.unmodifiableList(DEVICES);

    private static final MidiSequencer SEQUENCER = MidiSequencer.getInstance();

    private static final Status MIDI_UNAVALIABLE = new Status("Err_MidiUnavailable", Status.ERROR, 500);

    private static final Status DEFAULT_DEVICE_UNAVAILABLE = new Status("Err_DefaultDeviceUnavailable", Status.ERROR, 450);

    private static final Status NO_INPUTE_DEVICE = new Status("Wrn_NoInputDeviceConnected", Status.WARNING, 150);

    private static final byte[] CHANNELS = new byte[MidiSong.MAX_NUM_OF_CHANNELS];

    static {
        resetChannels();
    }

    private static final int[] PROGRAMS = new int[MidiSong.MAX_NUM_OF_CHANNELS];

    static {
        for (byte i = 0; i < PROGRAMS.length; ++i) {
            PROGRAMS[i] = -1;
        }
    }

    static final Receiver SONG_RECEIVER = new Receiver() {

        public void send(MidiMessage message, long timeStamp) {
            for (final Device device : MidiDevicer.DEVICES) {
                if (device.isSongConnected()) {
                    device.receiver.send(message, timeStamp);
                }
            }
        }

        public void close() {
        }
    };

    static void refreshDevices() {
        if (Device.startRefresh()) {
            new Device(null, null, true, true);
            for (final MidiDevice.Info deviceInfo : MidiSystem.getMidiDeviceInfo()) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
                    new Device(device, deviceInfo, (device.getMaxTransmitters() != 0), (device.getMaxReceivers() != 0));
                } catch (MidiUnavailableException e) {
                }
            }
            Device.endRefresh();
            if (Device.changes) {
                Device.changes = false;
                checkForInputDevice();
                DialogSettings.DeviceList.refreshDevices();
            }
        }
    }

    static void checkForInputDevice() {
        for (Device device : DEVICES) {
            if (device.isInputConnected()) {
                KeyboardHero.removeStatus(NO_INPUTE_DEVICE);
                return;
            }
        }
        KeyboardHero.addStatus(NO_INPUTE_DEVICE);
    }

    static void resetStatuses() {
        KeyboardHero.removeStatus(MIDI_UNAVALIABLE);
        KeyboardHero.removeStatus(DEFAULT_DEVICE_UNAVAILABLE);
    }

    static void connectInputDevice(final Device device) {
        device.inputSetConnected = true;
        try {
            (device.device == null ? MidiSystem.getTransmitter() : device.device.getTransmitter()).setReceiver(new Receiver() {

                @SuppressWarnings("null")
                public void send(MidiMessage message, long timeStamp) {
                    SEQUENCER.messageReceived(message, device.id);
                    ShortMessage shortMessage = null;
                    boolean isShortMessage = false;
                    int channel = 0;
                    if (message instanceof ShortMessage) {
                        shortMessage = (ShortMessage) message;
                        isShortMessage = true;
                        channel = shortMessage.getChannel();
                        device.channels[channel] = true;
                        if (shortMessage.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                            device.programs[channel] = shortMessage.getData1();
                        }
                    }
                    for (final Device outputDevice : MidiDevicer.DEVICES) {
                        if (outputDevice.isOutputConnected()) {
                            if (outputDevice.isSongConnected() && isShortMessage) {
                                ShortMessage newMessage = new ShortMessage();
                                final int newChannel = CHANNELS[channel];
                                if (device.programs[channel] != PROGRAMS[newChannel]) {
                                    try {
                                        newMessage.setMessage(ShortMessage.PROGRAM_CHANGE, newChannel, device.programs[channel], 0);
                                        PROGRAMS[newChannel] = device.programs[channel];
                                        outputDevice.receiver.send(newMessage, timeStamp);
                                    } catch (InvalidMidiDataException e) {
                                        if (Util.getDebugLevel() > 90) e.printStackTrace();
                                    }
                                    newMessage = new ShortMessage();
                                }
                                try {
                                    newMessage.setMessage(shortMessage.getCommand(), newChannel, shortMessage.getData1(), shortMessage.getData2());
                                } catch (InvalidMidiDataException e) {
                                    newMessage = shortMessage;
                                }
                                try {
                                    outputDevice.receiver.send(newMessage, timeStamp);
                                } catch (IllegalStateException e) {
                                    if (device.open() && outputDevice.open()) {
                                        try {
                                            outputDevice.receiver.send(newMessage, timeStamp);
                                        } catch (IllegalStateException e2) {
                                            KeyboardHero.addStatus(MIDI_UNAVALIABLE);
                                        }
                                    }
                                }
                            } else {
                                try {
                                    outputDevice.receiver.send(message, timeStamp);
                                } catch (IllegalStateException e) {
                                    if (outputDevice.open()) {
                                        try {
                                            outputDevice.receiver.send(message, timeStamp);
                                        } catch (IllegalStateException e2) {
                                            KeyboardHero.addStatus(MIDI_UNAVALIABLE);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                public void close() {
                }
            });
            for (int i = 0; i < device.channels.length; ++i) {
                device.channels[i] = false;
            }
            if (device.device != null) device.device.open();
            device.inputConnected = true;
            checkForInputDevice();
        } catch (MidiUnavailableException e) {
            Util.conditionalError(MIDI_UNAVALIABLE, "Err_MidiUnavailable", '(' + device.getName() + ") " + e.getLocalizedMessage());
        } catch (NullPointerException e) {
            Util.conditionalError(DEFAULT_DEVICE_UNAVAILABLE, "Err_DefaultDeviceUnavailable", '(' + device.getName() + ')');
        }
    }

    static void disconnectInputDevice(Device device) {
        device.inputSetConnected = false;
        try {
            (device.device == null ? MidiSystem.getTransmitter() : device.device.getTransmitter()).setReceiver(null);
            device.inputConnected = false;
            if (device.device != null && !(device.songConnected || device.outputConnected)) {
                device.close();
            }
            checkForInputDevice();
        } catch (MidiUnavailableException e) {
            Util.conditionalError(MIDI_UNAVALIABLE, "Err_MidiUnavailable", '(' + device.getName() + ") " + e.getLocalizedMessage());
        } catch (NullPointerException e) {
            Util.conditionalError(DEFAULT_DEVICE_UNAVAILABLE, "Err_DefaultDeviceUnavailable", '(' + device.getName() + ')');
        }
    }

    static void connectSongDevice(Device device) {
        device.songSetConnected = true;
        if (device.open()) device.songConnected = true;
    }

    static void disconnectSongDevice(Device device) {
        device.songSetConnected = false;
        device.songConnected = false;
        if (device.device != null && !(device.inputConnected || device.outputConnected)) {
            device.close();
        }
    }

    static void connectOutputDevice(Device device) {
        device.outputSetConnected = true;
        if (device.open()) device.outputConnected = true;
    }

    static void disconnectOutputDevice(Device device) {
        device.outputSetConnected = false;
        device.outputConnected = false;
        if (!(device.inputConnected || device.songConnected)) {
            device.close();
        }
    }

    static void channelsChanged(final boolean[] usedChannels) {
        usedChannels[9] = true;
        boolean[] channels = new boolean[MidiSong.MAX_NUM_OF_CHANNELS];
        for (final Device device : DEVICES) {
            if (device.inputConnected) {
                for (int i = 0; i < device.channels.length; ++i) {
                    if (device.channels[i]) {
                        channels[i] = true;
                    }
                }
            }
        }
        ArrayList<Byte> freeChannelsList = new ArrayList<Byte>();
        for (byte i = 0; i < usedChannels.length; ++i) {
            if (!usedChannels[i]) freeChannelsList.add(i);
        }
        if (freeChannelsList.size() == 0) {
            for (byte k = 0; k < usedChannels.length; ++k) {
                CHANNELS[k] = k;
            }
            return;
        }
        byte[] freeChannels = Util.toArray(freeChannelsList);
        int j = 0;
        for (int i = 0; i < channels.length; ++i) {
            if (channels[i]) {
                if (i == 9) continue;
                CHANNELS[i] = freeChannels[j++ % freeChannels.length];
            }
        }
        for (int i = 0; i < channels.length; ++i) {
            if (i == 9) continue;
            if (!channels[i]) {
                CHANNELS[i] = freeChannels[j++ % freeChannels.length];
            }
        }
        if (Util.getDebugLevel() > 60) {
            for (int i = 0; i < CHANNELS.length; i++) {
                Util.debug(i + " -> " + CHANNELS[i]);
            }
            Util.debug("--------------------------------");
        }
    }

    static void resetChannels() {
        for (byte i = 0; i < CHANNELS.length; ++i) {
            CHANNELS[i] = i;
        }
    }

    static void closure() {
        for (final Device device : DEVICES) {
            device.close();
        }
    }

    /**
	 * Creates a string containing the most important information about the game. This method is
	 * used only for debugging and testing purposes.
	 * 
	 * @return the created string.
	 */
    static String getString() {
        return "MidiDevicer()";
    }

    /**
	 * This method serves security purposes. Provides an integrity string that will be checked by
	 * the {@link Connection#integrityCheck()} method; thus the application can only be altered if
	 * the source is known. Every class in the {@link keyboardhero} package has an integrity string.
	 * 
	 * @return the string of this class used for integrity checking.
	 */
    static String getIntegrityString() {
        return "Jam2,ay$sfgp23has_";
    }

    /**
	 * The tester object of this class. It provides a debugging menu and unit tests for this class.
	 * Its only purpose is debugging or testing.
	 */
    static final Tester TESTER = new Tester("MidiDevicer", new String[] { "getString()" }) {

        void menu(int choice) throws Exception {
            switch(choice) {
                case 5:
                    System.out.println(getString());
                    break;
                default:
                    baseMenu(choice);
                    break;
            }
        }

        void runUnitTests() throws Exception {
            higherTestStart("MidiDevicer");
            testEq("getIntegrityString()", "Jam2,ay$sfgp23has_", MidiDevicer.getIntegrityString());
            higherTestEnd();
        }

        boolean isAutoSandbox() {
            return true;
        }

        void sandbox() throws Throwable {
        }
    };

    /**
	 * Starts the class's developing menu. If this build is a developer's one it starts the
	 * application in a normal way with the exception that it starts the debugging tool for this
	 * class as well; otherwise exits with an error message.
	 * 
	 * @param args
	 *            the arguments given to the program.
	 * @see KeyboardHero#startApp()
	 */
    public static void main(String[] args) {
        Tester.mainer(args, TESTER);
    }

    static void initialize() {
        refreshDevices();
    }
}
