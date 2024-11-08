package uk.org.toot.midi.sequence;

import javax.sound.midi.Track;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.MidiMessage;
import static uk.org.toot.midi.message.MetaMsg.*;
import static uk.org.toot.midi.message.ChannelMsg.*;

/**
 * Some Midi utility functions.
 * @author Steve Taylor
 */
public class Midi {

    /** Convert from microseconds per quarter note to beats per minute and vice versa */
    public static float convertTempo(float value) {
        if (value <= 0) {
            value = 0.1f;
        }
        return 60000000.0f / value;
    }

    /** Return position in form mm:ss:?? bug: m:s need zero prefix padding */
    public static String timePosition(long us) {
        float sec = (us) / 1000000L;
        int mm = (int) sec / 60;
        int ss = (int) sec - 60 * mm;
        int t = (int) (10 * (sec - (ss + 60 * mm)));
        String sep1 = ss < 10 ? ":0" : ":";
        return mm + sep1 + ss + "." + t;
    }

    public static Sequence unMix(Sequence mix) throws InvalidMidiDataException {
        if (mix.getTracks().length > 1) return mix;
        System.out.println("UnMixing because only " + mix.getTracks().length + " track");
        Sequence unmixed = new Sequence(mix.getDivisionType(), mix.getResolution());
        Track ctrlTrack = unmixed.createTrack();
        Track chanTrack[] = new Track[16];
        Track[] mixT = mix.getTracks();
        for (int i = 0; i < mixT.length; i++) {
            Track trk = mixT[i];
            int chan = -1;
            int chanPrefix = -1;
            MidiEvent heldEvent = null;
            for (int v = 0; v < trk.size(); v++) {
                MidiEvent event = trk.get(v);
                MidiMessage msg = event.getMessage();
                if (isChannel(msg)) {
                    if (getCommand(msg) != 0xF0) {
                        chan = getChannel(msg);
                        chanPrefix = -1;
                    }
                } else {
                    chan = chanPrefix;
                }
                if (isMeta(msg)) {
                    switch(getType(msg)) {
                        case CHANNEL_PREFIX:
                            chan = chanPrefix = getData(msg)[0] & 0x0F;
                            break;
                        case PORT_PREFIX:
                            heldEvent = event;
                            continue;
                    }
                }
                if (chan == -1) {
                    ctrlTrack.add(event);
                } else {
                    if (chanTrack[chan] == null) chanTrack[chan] = unmixed.createTrack();
                    if (heldEvent != null) {
                        chanTrack[chan].add(heldEvent);
                        heldEvent = null;
                    }
                    chanTrack[chan].add(event);
                }
            }
        }
        return unmixed;
    }
}
