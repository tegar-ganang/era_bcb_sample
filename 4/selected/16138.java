package jrackattack.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import jonkoshare.util.VersionInformation;
import jrackattack.JRackAttack;
import jrackattack.event.FXParameterEvent;
import jrackattack.event.PatternParameterEvent;
import jrackattack.event.ProgramParameterEvent;
import jrackattack.event.SoundParameterEvent;
import jrackattack.gui.JRackAttackFrame;
import org.apache.log4j.Logger;

/**
 * Separate Thread to send Midi messages to a configured receiver. This is
 * necessary to have a time source and to avoid a lock for the GUI while
 * playing.
 * 
 * @author Alexander Methke
 */
@VersionInformation(lastChanged = "$LastChangedDate: 2009-09-15 14:42:26 -0400 (Tue, 15 Sep 2009) $", authors = { "Alexander Methke" }, revision = "$LastChangedRevision: 12 $", lastEditor = "$LastChangedBy: onkobu $", id = "$Id")
public class MidiThread extends Thread {

    private static final Logger log = Logger.getLogger(MidiThread.class);

    public static ShortMessage CLOCK_MESSAGE;

    public static ShortMessage SYSTEM_RESET_MESSAGE;

    public static ShortMessage ALIVE_MESSAGE;

    public static enum TestModes {

        CHORD, DRUMS, MELODY
    }

    ;

    protected MidiThread(MidiDevice dIn, MidiDevice dOut) {
        this();
        setMidiInDevice(dIn);
        setMidiOutDevice(dOut);
    }

    protected MidiThread() {
        if (CLOCK_MESSAGE == null) {
            CLOCK_MESSAGE = new ShortMessage();
            SYSTEM_RESET_MESSAGE = new ShortMessage();
            ALIVE_MESSAGE = new ShortMessage();
            try {
                CLOCK_MESSAGE.setMessage(ShortMessage.TIMING_CLOCK);
                SYSTEM_RESET_MESSAGE.setMessage(ShortMessage.SYSTEM_RESET);
                ALIVE_MESSAGE.setMessage(ShortMessage.ACTIVE_SENSING);
            } catch (Exception ex) {
            }
        }
    }

    public static MidiThread getInstance() {
        if (instance == null) {
            instance = new MidiThread();
            instance.start();
            readerInstance = new MidiReader();
            readerInstance.start();
        }
        return instance;
    }

    public static MidiThread getTestInstance(MidiDevice dev, int channel, TestModes tm, int progNo) {
        MidiThread t = new MidiThread();
        t.setMidiOutDevice(dev);
        t.setChannel(channel);
        t.setTestMode(tm);
        t.setProgramNumber(progNo);
        t.initTestMessages();
        return t;
    }

    @Override
    public void run() {
        while (runFlag) {
            try {
                sleep(100);
            } catch (Exception ex) {
            }
            if (runOnce) {
                break;
            }
        }
        cleanUp();
    }

    public void receivedData() {
        receivedData = true;
    }

    public void setRunFlag(boolean state) {
        runFlag = state;
    }

    public boolean isRunning() {
        return runFlag;
    }

    protected void cleanUp() {
        if (getMidiOutDevice() == null) {
            return;
        }
        getMidiOutDevice().close();
    }

    public void setChannel(int ch) {
        channel = ch;
    }

    public int getChannel() {
        return channel;
    }

    public boolean initTestMessages() {
        ShortMessage sm = new ShortMessage();
        try {
            sm.setMessage(ShortMessage.SYSTEM_RESET, 0, 0);
            enqueueEvent(sm);
            if (programNumber != -1) {
                sm = new ShortMessage();
                sm.setMessage(ShortMessage.PROGRAM_CHANGE, programNumber, 0);
                enqueueEvent(sm);
            }
            switch(testMode) {
                case DRUMS:
                    {
                        addDrumEvents(channel);
                    }
                    break;
                case CHORD:
                    {
                        addChordEvents(channel);
                    }
                    break;
                case MELODY:
                    {
                        addMelodyEvents(channel);
                    }
                    break;
            }
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
            return false;
        } catch (MidiUnavailableException e) {
            log.error("error", e);
            return false;
        }
        return true;
    }

    public boolean init(MidiDevice inDev, MidiDevice outDev) {
        setMidiInDevice(inDev);
        setMidiOutDevice(outDev);
        return true;
    }

    public boolean init(MidiDevice inDev, MidiDevice outDev, MidiDevice keyDev) {
        setMidiInDevice(inDev);
        setMidiOutDevice(outDev);
        setKeyInDevice(keyDev);
        return true;
    }

    public void emitVolumeChange(Object src, int sndNo, int vol) {
        emitParamChange(src, sndNo, RackAttack.VOLUME, vol);
    }

    public void emitPanChange(Object src, int sndNo, int pan) {
        emitParamChange(src, sndNo, RackAttack.PAN, pan);
    }

    public void emitNoteOn(int note) {
        assertValidMidiValue(note, "note pitch");
        ShortMessage sm = new ShortMessage();
        try {
            sm.setMessage(ShortMessage.NOTE_ON, note, 100);
            enqueueEvent(sm);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void emitNoteOff(int note) {
        assertValidMidiValue(note, "note pitch");
        ShortMessage sm = new ShortMessage();
        try {
            sm.setMessage(ShortMessage.NOTE_OFF, note, 100);
            enqueueEvent(sm);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void emitFxChange(Object src, int fx, byte[] ah_al, int value) {
        assertValidFXNumber(fx);
        assertValidMidiValue(value, "sys ex value");
        SysexMessage sms = new SysexMessage();
        byte[] bytes = new byte[14];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.FX_CHANGE;
        bytes[5] = RackAttack.EDIT_BUFFER;
        bytes[6] = (byte) (fx & 0xFF);
        bytes[7] = ah_al[0];
        bytes[8] = ah_al[1];
        bytes[9] = (byte) (value & 0x7F);
        bytes[10] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] + bytes[8] + bytes[9] & 0x7F);
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[12] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[13] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
            JRackAttackFrame.getInstance().internalFxChange(new FXParameterEvent(src, fx, ah_al, value));
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void emitGlobalChange(Object src, byte[] ah_al, int value) {
        assertValidMidiValue(value, "sys ex value");
        SysexMessage sms = new SysexMessage();
        byte[] bytes = new byte[12];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.GLOBAL_CHANGE;
        bytes[5] = ah_al[0];
        bytes[6] = ah_al[1];
        bytes[7] = (byte) (value & 0xFF);
        bytes[8] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] & 0x7F);
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void emitProgramChange(Object src, int prg, byte[] ah_al, int value) {
        assertValidProgramNumber(prg);
        assertValidMidiValue(value, "sys ex value");
        SysexMessage sms = new SysexMessage();
        byte[] bytes = new byte[14];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.PROGRAM_CHANGE;
        bytes[5] = RackAttack.EDIT_BUFFER;
        bytes[6] = (byte) (prg & 0xFF);
        bytes[7] = ah_al[0];
        bytes[8] = ah_al[1];
        bytes[9] = (byte) (value & 0xFF);
        bytes[10] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] + bytes[8] + bytes[9] & 0x7F);
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[12] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[13] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
            JRackAttackFrame.getInstance().internalProgramChange(new ProgramParameterEvent(src, prg, ah_al, value));
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void emitPatternChange(Object src, int snd, byte[] ah_al, int value) {
        assertValidPatternNumber(snd);
        assertValidMidiValue(value, "sys ex value");
        SysexMessage sms = new SysexMessage();
        byte[] bytes = new byte[14];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.PATTERN_CHANGE;
        bytes[5] = RackAttack.EDIT_BUFFER;
        bytes[6] = (byte) (snd & 0xFF);
        bytes[7] = ah_al[0];
        bytes[8] = ah_al[1];
        bytes[9] = (byte) (value & 0xFF);
        bytes[10] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] + bytes[8] + bytes[9] & 0x7F);
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[12] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[13] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
            JRackAttackFrame.getInstance().internalPatternChange(new PatternParameterEvent(src, snd, ah_al, value));
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void emitParamChange(Object src, int snd, byte[] ah_al, int value) {
        assertValidSoundNumber(snd);
        assertValidMidiValue(value, "sys ex value");
        SysexMessage sms = new SysexMessage();
        byte[] bytes = new byte[14];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.SOUND_CHANGE;
        bytes[5] = RackAttack.EDIT_BUFFER;
        bytes[6] = (byte) (snd & 0xFF);
        bytes[7] = ah_al[0];
        bytes[8] = ah_al[1];
        bytes[9] = (byte) (value & 0xFF);
        bytes[10] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] + bytes[8] + bytes[9] & 0x7F);
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[12] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[13] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
            JRackAttackFrame.getInstance().internalParamChange(new SoundParameterEvent(src, snd, ah_al, value));
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public SoundParameter[] loadAllSoundParameter() {
        return null;
    }

    public void storeSoundParameter(SoundParameter sp, boolean buffer) {
        try {
            enqueueEvent(sp.createMessage(buffer));
        } catch (MidiUnavailableException ex) {
            log.error("error", ex);
        }
    }

    public void loadFXParameter(int fx, boolean buffer) {
        assertValidFXNumber(fx);
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[11];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.FX_REQUEST;
        if (buffer) {
            bytes[5] = RackAttack.EDIT_BUFFER;
        } else {
            bytes[5] = RackAttack.ASSEMBLY_BUFFER;
        }
        bytes[6] = (byte) (fx & 0xFF);
        bytes[7] = (byte) (bytes[4] + bytes[5] + bytes[6] & 0x7F);
        bytes[8] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[9] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
        log(" Waiting for midi in events");
        for (int i = 0; i < 5; i++) {
            if (receivedData) {
                break;
            }
            try {
                sleep(500);
            } catch (InterruptedException e) {
            }
        }
        if (!receivedData) {
            JRackAttackFrame.getInstance().handleError("msg.no_data_received");
        } else {
            receivedData = false;
        }
    }

    public void loadGlobalParameter() {
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[10];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.GLOBAL_REQUEST;
        bytes[5] = 0x00;
        bytes[6] = (byte) (bytes[4] + bytes[5] & 0x7F);
        bytes[7] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[8] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
        log(" Waiting for midi in events");
        for (int i = 0; i < 5; i++) {
            if (receivedData) {
                break;
            }
            try {
                sleep(500);
            } catch (InterruptedException e) {
            }
        }
        if (!receivedData) {
            JRackAttackFrame.getInstance().handleError("msg.no_data_received");
        } else {
            receivedData = false;
        }
    }

    public void resetEditBuffer() {
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[12];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.MOV_COMMAND;
        bytes[5] = RackAttack.MCMD_INIT_BUFFER;
        bytes[6] = RackAttack.EDIT_BUFFER;
        bytes[7] = 0;
        bytes[8] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] & 0x7F);
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    /**
	 * Copies the current program from memory into edit buffer.
	 */
    public void recallProgramToEdit() {
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[12];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.MOV_COMMAND;
        bytes[5] = RackAttack.MCMD_RECALL_CURRENT_PROGRAM;
        bytes[6] = RackAttack.MCMD_MOV1_RECALL_CURRENT_PROGRAM;
        bytes[7] = 0;
        bytes[8] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] & 0x7F);
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    /**
	 * Copies the current program from memory into edit buffer.
	 */
    public void loadProgramToEdit(int pNumber) {
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[12];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.MOV_COMMAND;
        bytes[5] = RackAttack.MCMD_COPY_PROGRAM_TO_EDIT;
        bytes[6] = RackAttack.EDIT_BUFFER;
        bytes[7] = (byte) (pNumber & 0x7f);
        bytes[8] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] & 0x7F);
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void dumpProgramFromEdit(int pNumber) {
        readerInstance.setNextProgramNumber(pNumber);
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[12];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.MOV_COMMAND;
        bytes[5] = RackAttack.MCMD_DUMP_PROGRAM_FROM_EDIT;
        bytes[6] = RackAttack.EDIT_BUFFER;
        bytes[7] = 0;
        bytes[8] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] & 0x7F);
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void storeProgramFromEdit(int pNumber) {
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[12];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.MOV_COMMAND;
        bytes[5] = RackAttack.MCMD_COPY_PROGRAM_FROM_EDIT;
        bytes[6] = RackAttack.EDIT_BUFFER;
        bytes[7] = (byte) (pNumber & 0x7f);
        bytes[8] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] & 0x7F);
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    /**
	 * Loads the given sound from it's program memory to the edit buffer. This
	 * is a sort of reset.
	 */
    public void loadSoundFromProgramToEdit(int snd) {
        assertValidSoundNumber(snd);
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[12];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.MOV_COMMAND;
        bytes[5] = RackAttack.MCMD_RECALL_SOUND;
        bytes[6] = RackAttack.MCMD_MOV1_RECALL_SOUND;
        bytes[7] = (byte) (snd & 0xFF);
        bytes[8] = (byte) (bytes[4] + bytes[5] + bytes[6] + bytes[7] & 0x7F);
        bytes[9] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[11] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    public void loadSoundParameter(int snd, boolean buffer) {
        assertValidSoundNumber(snd);
        SysexMessage sms = new SysexMessage();
        byte[] bytes = null;
        bytes = new byte[11];
        bytes[0] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[1] = RackAttack.WALDORF_MANUFACTURER_BYTE;
        bytes[2] = RackAttack.RACK_ATTACK_MODEL_BYTE;
        bytes[3] = 0x00;
        bytes[4] = RackAttack.SOUND_REQUEST;
        if (buffer) {
            bytes[5] = RackAttack.EDIT_BUFFER;
        } else {
            bytes[5] = RackAttack.ASSEMBLY_BUFFER;
        }
        bytes[6] = (byte) (snd & 0xFF);
        bytes[7] = (byte) (bytes[4] + bytes[5] + bytes[6] & 0x7F);
        bytes[8] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        bytes[9] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
        bytes[10] = (byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE;
        try {
            sms.setMessage(bytes, bytes.length);
            enqueueEvent(sms);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
        log(" Waiting for midi in events");
        for (int i = 0; i < 5; i++) {
            if (receivedData) {
                break;
            }
            try {
                sleep(500);
            } catch (InterruptedException e) {
            }
        }
        if (!receivedData) {
            JRackAttackFrame.getInstance().handleError("msg.no_data_received");
        } else {
            receivedData = false;
        }
    }

    protected void assertValidPatternNumber(int patNo) {
        assertValidSoundNumber(patNo);
    }

    protected void assertValidSoundNumber(int sndNo) {
        if (sndNo < 0 || sndNo > 23) {
            throw new IllegalArgumentException("Sound number may range only from 1-24, " + sndNo + " is therefore invalid");
        }
    }

    protected void assertValidProgramNumber(int prgNo) {
        if (prgNo < 0 || prgNo > 49) {
            throw new IllegalArgumentException("Program number may range only from 1-50, " + prgNo + " is therefore invalid");
        }
    }

    protected void assertValidFXNumber(int fxNo) {
        if (fxNo < 0 || fxNo > 3) {
            throw new IllegalArgumentException("Fx number may range only from 1-4, " + fxNo + " is therefore invalid");
        }
    }

    protected void assertValidMidiValue(int val, String valueName) {
        if (val < 0 || val > 128) {
            throw new IllegalArgumentException("Value for " + valueName + " may range only from 0..127, " + val + " is therefore invalid");
        }
    }

    private void addDrumEvents(int channel) {
        NoteMessage sm = null;
        try {
            for (int i = 0; i < 30; i++) {
                sm = new NoteMessage();
                sm.setMessage(ShortMessage.NOTE_ON, channel, 40 + i, 100);
                sm.setDuration(250);
                enqueueEvent(sm);
                enqueueEvent(new PauseMessage(50));
                sm = new NoteMessage();
                sm.setMessage(ShortMessage.NOTE_OFF, channel, 40 + i, 100);
                enqueueEvent(sm);
            }
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    private void addChordEvents(int channel) {
        ChordMessage sm = null;
        try {
            sm = new ChordMessage(new int[] { 64, 67, 70 }, channel, 1000, ShortMessage.NOTE_ON);
            enqueueEvent(sm);
            sm = new ChordMessage(sm, ShortMessage.NOTE_OFF);
            enqueueEvent(sm);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    private void addMelodyEvents(int channel) {
        try {
            NoteMessage sm = null;
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_ON, channel, 64, 100);
            sm.setDuration(750);
            enqueueEvent(sm);
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_OFF, channel, 64, 100);
            enqueueEvent(sm);
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_ON, channel, 70, 100);
            sm.setDuration(500);
            enqueueEvent(sm);
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_OFF, channel, 70, 100);
            enqueueEvent(sm);
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_ON, channel, 70, 100);
            sm.setDuration(250);
            enqueueEvent(sm);
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_OFF, channel, 70, 100);
            enqueueEvent(sm);
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_ON, channel, 67, 100);
            sm.setDuration(1000);
            enqueueEvent(sm);
            sm = new NoteMessage();
            sm.setMessage(ShortMessage.NOTE_OFF, channel, 67, 100);
            enqueueEvent(sm);
        } catch (InvalidMidiDataException e) {
            log.error("error", e);
        } catch (MidiUnavailableException e) {
            log.error("error", e);
        }
    }

    protected void setMidiInDevice(MidiDevice dev) {
        readerInstance.setMidiInDevice(dev);
    }

    public MidiDevice getMidiInDevice() {
        return readerInstance.getMidiInDevice();
    }

    public MidiDevice getMidiOutDevice() {
        return outDevice;
    }

    protected void setMidiOutDevice(MidiDevice dev) {
        if (outDevice != null && dev != outDevice && outDevice.isOpen()) {
            outDevice.close();
        }
        outDevice = dev;
        if (outDevice != null && !outDevice.isOpen()) {
            try {
                outDevice.open();
            } catch (MidiUnavailableException e) {
                log.error("error", e);
            }
        }
    }

    protected synchronized void enqueueEvent(MidiMessage msg) throws MidiUnavailableException {
        MidiDevice dev = getMidiOutDevice();
        if (JRackAttack.spoolMidi()) {
            log("sending " + MidiReader.formatMidiMessage(msg));
        }
        if (dev == null) {
            return;
        }
        if (receiver == null) {
            receiver = dev.getReceiver();
        }
        if (msg instanceof ChordMessage) {
            ChordMessage cMesg = (ChordMessage) msg;
            receiver.send(cMesg.getNote1(), 0);
            receiver.send(cMesg.getNote2(), 0);
            receiver.send(cMesg.getNote3(), 0);
            if (cMesg.getNote4() != null) {
                receiver.send(cMesg.getNote1(), -1);
                log("as note message");
            }
            if ((cMesg.getNote1().getStatus() & 0xF0) == ShortMessage.NOTE_ON) {
                log("sleeping message duration (" + cMesg.getDuration() + " mS)");
                try {
                    sleep(cMesg.getDuration());
                } catch (InterruptedException e) {
                }
            }
        } else if (msg instanceof PauseMessage) {
            log("pause message (" + ((DurationMessage) msg).getDuration() + " mS)");
            try {
                sleep(((DurationMessage) msg).getDuration());
            } catch (InterruptedException e) {
            }
        } else if (msg instanceof DurationMessage) {
            log("duration message (" + ((DurationMessage) msg).getDuration() + " mS)");
            receiver.send(msg, -1);
            if ((msg.getStatus() & 0xF0) == ShortMessage.NOTE_ON) {
                try {
                    sleep(((DurationMessage) msg).getDuration());
                } catch (InterruptedException e) {
                }
            }
        } else {
            log("raw midi message");
            receiver.send(msg, -1);
        }
    }

    /**
	 * Getter for property testMode.
	 * 
	 * @return Value of property testMode.
	 */
    public TestModes getTestMode() {
        return this.testMode;
    }

    /**
	 * Setter for property testMode.
	 * 
	 * @param testMode
	 *            New value of property testMode.
	 */
    public void setTestMode(TestModes testMode) {
        this.testMode = testMode;
    }

    /**
	 * Getter for property programNumber.
	 * 
	 * @return Value of property programNumber.
	 */
    public int getProgramNumber() {
        return this.programNumber;
    }

    /**
	 * Setter for property programNumber.
	 * 
	 * @param programNumber
	 *            New value of property programNumber.
	 */
    public void setProgramNumber(int programNumber) {
        this.programNumber = programNumber;
    }

    /**
	 * Getter for property runOnce.
	 * 
	 * @return Value of property runOnce.
	 */
    public boolean isRunOnce() {
        return this.runOnce;
    }

    /**
	 * Setter for property runOnce.
	 * 
	 * @param runOnce
	 *            New value of property runOnce.
	 */
    public void setRunOnce(boolean runOnce) {
        this.runOnce = runOnce;
    }

    /**
	 * Getter for property keyInDevice.
	 * 
	 * @return Value of property keyInDevice.
	 */
    public MidiDevice getKeyInDevice() {
        return this.keyInDevice;
    }

    /**
	 * Setter for property keyInDevice.
	 * 
	 * @param keyInDevice
	 *            New value of property keyInDevice.
	 */
    public void setKeyInDevice(MidiDevice keyInDevice) {
        readerInstance.setKeyInDevice(keyInDevice);
        this.keyInDevice = keyInDevice;
    }

    protected void log(String msg) {
        log.debug(msg + " (hc:" + hashCode() + ")");
    }

    private static MidiThread instance;

    private static MidiReader readerInstance;

    private boolean runFlag = true;

    private int channel;

    private MidiDevice outDevice;

    /**
	 * Holds value of property testMode.
	 */
    private TestModes testMode;

    /**
	 * Holds value of property programNumber.
	 */
    private int programNumber;

    /**
	 * Holds value of property runOnce.
	 */
    private boolean runOnce = false;

    /**
	 * Holds value of property keyInDevice.
	 */
    private MidiDevice keyInDevice;

    private boolean receivedData;

    private Receiver receiver;
}
