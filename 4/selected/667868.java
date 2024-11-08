package eu.davidgamez.mas.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

public class MIDIEvent {

    private long timeStamp;

    private ShortMessage message = new ShortMessage();

    public MIDIEvent(long timeStamp, int command, int data1, int data2, int channel) throws InvalidMidiDataException {
        this.timeStamp = timeStamp;
        message.setMessage(command, channel, data1, data2);
    }

    public MIDIEvent(ShortMessage msg, long timeStamp) {
        this.timeStamp = timeStamp;
        this.message = msg;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getCommand() {
        return message.getCommand();
    }

    public int getData1() {
        return message.getData1();
    }

    public int getData2() {
        return message.getData2();
    }

    public int getChannel() {
        return message.getChannel();
    }

    public ShortMessage getMessage() {
        return message;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
