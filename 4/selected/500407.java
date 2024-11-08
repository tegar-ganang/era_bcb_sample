package vavi.sound.mfi.vavi;

import java.util.NoSuchElementException;
import java.util.logging.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.NoteMessage;
import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;

/**
 * VaviNoteMessage.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030821 nsano initial version <br>
 *          0.01 030826 nsano implements {@link MidiConvertible} <br>
 */
public class VaviNoteMessage extends NoteMessage implements MidiConvertible, MfiConvertible {

    /** MFi */
    public VaviNoteMessage(int delta, int status, int data) {
        super(delta, status, data);
    }

    /** MFi2 note = 1 �̏ꍇ */
    public VaviNoteMessage(int delta, int status, int data1, int data2) {
        super(delta, status, data1, data2);
    }

    /** for {@link MfiConvertible}, note = 1 */
    protected VaviNoteMessage() {
        super();
    }

    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        int voice = getVoice();
        int velocity = getVelocity() * 2;
        int length = getGateTime();
        int channel = voice + 4 * context.getMfiTrackNumber();
        int pitch = getNote();
        channel = context.retrieveChannel(channel);
        pitch = context.retrievePitch(channel, pitch);
        MidiEvent[] events = new MidiEvent[2];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
        events[0] = new MidiEvent(shortMessage, context.getCurrent());
        shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
        events[1] = new MidiEvent(shortMessage, context.getCurrent() + length);
        return events;
    }

    /**
     * TODO ���^�C�����A���O�̓��{�C�X�A���L�[�� NoteMessage ��
     *      �Q�[�g�^�C����菬�����ꍇ�͒��O�� NoteMessage ����̌p�����Ƃ���
     * TODO ���̉��܂ŗ]�T����������L�΂��āA����������؂�H(������)
     */
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context) throws InvalidMfiDataException {
        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int command = shortMessage.getCommand();
        int data1 = shortMessage.getData1();
        int data2 = shortMessage.getData2();
        if (command == ShortMessage.NOTE_OFF || (command == ShortMessage.NOTE_ON && data2 == 0)) {
            if (!context.isNoteOffEventUsed()) {
                Debug.println("[" + context.getMidiEventIndex() + "] no pair of ON for: " + channel + "ch, " + data1);
            }
            return null;
        } else {
            MidiEvent noteOffEvent = null;
            try {
                noteOffEvent = context.getNoteOffMidiEvent();
            } catch (NoSuchElementException e) {
                Debug.println(Level.WARNING, "[" + context.getMidiEventIndex() + "] no pair of OFF for: " + channel + "ch, " + data1);
                return null;
            }
            int track = context.retrieveMfiTrack(channel);
            int voice = context.retrieveVoice(channel);
            double scale = context.getScale();
            long currentTick = midiEvent.getTick();
            long noteOffTick = noteOffEvent.getTick();
            int length = (int) Math.round((noteOffTick - currentTick) / scale);
            if (length == 0) {
                Debug.println(Level.WARNING, "length is 0 ~ 1, " + MidiUtil.paramString(shortMessage) + ", " + ((noteOffTick - currentTick) / scale));
            } else if (length < 0) {
                Debug.println(Level.WARNING, "length < 0, " + MidiUtil.paramString(shortMessage) + ", " + ((noteOffTick - currentTick) / scale));
            }
            int delta = context.getDelta(context.retrieveMfiTrack(channel));
            int onLength = (length + 254) / 255;
            MfiEvent[] mfiEvents = new MfiEvent[1];
            for (int i = 0; i < Math.max(onLength, 1); i++) {
                NoteMessage mfiMessage = new VaviNoteMessage();
                mfiMessage.setDelta(i == 0 ? delta : 0);
                mfiMessage.setVoice(voice);
                mfiMessage.setNote(context.retrievePitch(channel, data1));
                mfiMessage.setGateTime(i == onLength - 1 ? length % 255 : 255);
                mfiMessage.setVelocity(data2 / 2);
                if (length >= 255) {
                    Debug.println(channel + "ch, " + mfiMessage.getNote() + ", " + mfiMessage.getDelta() + ":[" + i + "]:" + (i == onLength - 1 ? length % 255 : 255) + "/" + length);
                }
                mfiEvents[i] = new MfiEvent(mfiMessage, 0l);
                if (i == 0) {
                    context.setPreviousTick(track, midiEvent.getTick());
                    break;
                } else {
                }
            }
            return mfiEvents;
        }
    }
}
