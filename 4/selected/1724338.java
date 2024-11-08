package neuralmusic.midiIO;

import java.util.List;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import neuralmusic.brain.module.BrainException;
import neuralmusic.brain.module.Connection;

/**
 * 
 * @author pjl
 *
 *  Constructs a Receiver that is conncets to the input of a brain
 *
 */
public class MidiInToBrain {

    Receiver recv;

    public MidiInToBrain(final Receiver thru, final List<Connection> inputs) {
        recv = new Receiver() {

            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (thru != null) thru.send(message, -1);
                if (message instanceof ShortMessage) {
                    ShortMessage shm = (ShortMessage) message;
                    if (shm.getChannel() == 9) {
                        if (shm.getData2() != 0) {
                        }
                    }
                }
                if (message instanceof ShortMessage) {
                    ShortMessage shm = (ShortMessage) message;
                    int cmd = shm.getCommand();
                    if (cmd == ShortMessage.NOTE_ON || cmd == ShortMessage.NOTE_OFF) {
                        int pitch = shm.getData1();
                        int vel = shm.getData2();
                        pitch = pitch - 36;
                        if (pitch >= inputs.size() || pitch < 0) {
                            return;
                        }
                        try {
                            inputs.get(pitch).excite(vel / 128.0f);
                        } catch (BrainException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void close() {
            }
        };
    }

    public Receiver getReceiver() {
        return recv;
    }
}
