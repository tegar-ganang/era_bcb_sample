package djlort;

import javax.sound.midi.*;

public class Sender implements Receiver {

    Protocol protocol;

    long first = 0;

    public Sender(Protocol p) {
        protocol = p;
    }

    /** Send midi msg over network in real time
	 * Ignores timeStamp arg, time stamp is taken from system time
	 * */
    public void send(MidiMessage msg, long timeStamp) {
        long now = System.currentTimeMillis();
        if (first == 0) {
            first = now;
        }
        long sysTime = now - first;
        MidiEvent event = new MidiEvent(msg, sysTime);
        String s = eventToString(event);
        if (s == null) return;
        System.out.println("Sending " + s + " to " + protocol);
        protocol.send(s);
    }

    public static String eventToString(MidiEvent event) {
        ShortMessage msg = null;
        if (event.getMessage() instanceof ShortMessage) {
            msg = (ShortMessage) event.getMessage();
        } else return null;
        int volume = -1;
        switch(msg.getCommand()) {
            case ShortMessage.NOTE_ON:
                volume = msg.getData2();
                break;
            case ShortMessage.NOTE_OFF:
                volume = 0;
                break;
            case ShortMessage.CONTROL_CHANGE:
                return "control " + msg.getChannel() + " " + msg.getData1() + " " + msg.getData2() + " " + event.getTick() + "\n";
            default:
                return null;
        }
        int height = msg.getData1();
        String s = "note " + msg.getChannel() + " " + height + " " + volume + " " + event.getTick() + "\n";
        return s;
    }

    public void close() {
    }
}
