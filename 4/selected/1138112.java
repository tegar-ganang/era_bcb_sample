package ampt.examples.filters;

import java.util.HashMap;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class FlutterFilter implements Receiver, Transmitter {

    private Receiver receiverOut;

    private HashMap<String, Thread> notesMap;

    public FlutterFilter() {
        notesMap = new HashMap<String, Thread>();
    }

    @Override
    public void send(MidiMessage messageIn, long timeStamp) {
        if (messageIn instanceof ShortMessage) {
            ShortMessage sMsg = (ShortMessage) messageIn;
            int command = sMsg.getCommand();
            int channel = sMsg.getChannel();
            int noteValue = sMsg.getData1();
            String key = channel + "_" + noteValue;
            if (command == ShortMessage.NOTE_ON) {
                if (notesMap.get(key) != null) {
                    Thread thread = notesMap.get(key);
                    thread.interrupt();
                }
                Thread thread = new Thread(new NoteGenerator(sMsg, receiverOut, timeStamp));
                notesMap.put(key, thread);
                thread.start();
            } else if (command == ShortMessage.NOTE_OFF) {
                if (notesMap.get(key) != null) {
                    Thread thread = notesMap.get(key);
                    thread.interrupt();
                }
                receiverOut.send(sMsg, timeStamp);
            } else {
                receiverOut.send(sMsg, timeStamp);
            }
        } else {
            receiverOut.send(messageIn, timeStamp);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void setReceiver(Receiver receiverOut) {
        this.receiverOut = receiverOut;
    }

    @Override
    public Receiver getReceiver() {
        return receiverOut;
    }
}

class NoteGenerator implements Runnable {

    private ShortMessage message;

    private Receiver receiverOut;

    private long timeStamp;

    public NoteGenerator(ShortMessage message, Receiver receiverOut, long timeStamp) {
        this.message = message;
        this.receiverOut = receiverOut;
        this.timeStamp = timeStamp;
    }

    @Override
    public void run() {
        try {
            while (true) {
                receiverOut.send(message, timeStamp);
                Thread.sleep(50);
            }
        } catch (InterruptedException ie) {
            return;
        }
    }
}
