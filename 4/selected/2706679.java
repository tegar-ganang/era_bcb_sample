package com.frinika.sequencer.converter;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import com.frinika.sequencer.midi.message.TempoMessage;

/**
 * Tools for converting Midi Sequence objects
 * 
 * @author Peter Johan Salomonsen
 */
public class MidiSequenceConverter {

    /** 
     * Convert a Midi Sequence from Frinika singleTrack sequences to MultiTrack. All tracks are split up
     * so that there is only one channel represented per track. Tracks are mapped to channel
     * in ascending channel order, and the initial track is left as a mastertrack (containing tempo events etc.)
     */
    public static Sequence splitChannelsToMultiTrack(Sequence sequence) {
        System.out.println("Scanning sequence with " + sequence.getTracks().length + " tracks.");
        Track track = sequence.getTracks()[0];
        boolean[] channelsUsed = new boolean[16];
        for (int n = 0; n < track.size(); n++) {
            MidiEvent event = track.get(n);
            if (event.getMessage() instanceof ShortMessage) {
                ShortMessage message = (ShortMessage) event.getMessage();
                channelsUsed[message.getChannel()] = true;
            }
        }
        System.out.print("Channels used: ");
        for (int n = 0; n < channelsUsed.length; n++) {
            if (channelsUsed[n]) System.out.print(n + " ");
        }
        System.out.println();
        Integer[] channelToTrackMapping = new Integer[16];
        int tracksCreated = 0;
        for (int n = 0; n < channelsUsed.length; n++) {
            if (channelsUsed[n]) {
                sequence.createTrack();
                channelToTrackMapping[n] = tracksCreated++;
            }
        }
        System.out.println("Created " + tracksCreated + " new tracks.");
        for (int n = 0; n < track.size(); n++) {
            MidiEvent event = track.get(n);
            if (event.getMessage() instanceof ShortMessage) {
                ShortMessage message = (ShortMessage) event.getMessage();
                sequence.getTracks()[channelToTrackMapping[message.getChannel()] + 1].add(event);
                track.remove(event);
                n--;
            }
        }
        System.out.println("Events moved into new tracks. Initial track kept as mastertrack for tempo change etc.");
        return sequence;
    }

    /**
     * Find the first tempo meta message and return the tempo value
     * @return
     */
    public static float findFirstTempo(Sequence sequence) throws Exception {
        for (Track track : sequence.getTracks()) {
            for (int n = 0; n < track.size(); n++) {
                MidiEvent event = track.get(n);
                if (event.getMessage() instanceof MetaMessage && ((MetaMessage) event.getMessage()).getType() == 0x51) {
                    float tempo = new TempoMessage((MetaMessage) event.getMessage()).getBpm();
                    System.out.println("Found tempomessage " + tempo + " bpm");
                    return tempo;
                }
            }
        }
        throw new Exception("No tempo message found");
    }
}
