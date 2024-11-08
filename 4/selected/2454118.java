package de.michabrandt.timeview.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The Dialog class represents a whole dialog, including visualization
 * properties.
 */
public class Dialog extends ArrayList<DialogTrack> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(Dialog.class.getName());

    private ArrayList<DialogChannel> channels;

    private ArrayList<DialogTrack> tracks;

    private ArrayList<Pair<String, String>> track_definitions;

    private ArrayList<String> track_labels;

    private Map<String, DialogChannel> channel_by_id;

    private float start;

    private float end;

    public Dialog() {
        this.channels = new ArrayList<DialogChannel>();
        this.tracks = new ArrayList<DialogTrack>();
        this.track_definitions = new ArrayList<Pair<String, String>>();
        this.track_labels = new ArrayList<String>();
        this.channel_by_id = new HashMap<String, DialogChannel>();
        this.start = 0.0f;
        this.end = 0.0f;
    }

    public void defineTrack(String fg_id, String bg_id, String label) {
        this.track_definitions.add(new Pair<String, String>(fg_id, bg_id));
        this.track_labels.add(label);
    }

    public void build() {
        Set<String> assigned_channels = new HashSet<String>();
        for (DialogChannel ch : this.channels) {
            ch.build();
            if (ch.getId() != null) this.channel_by_id.put(ch.getId(), ch);
            this.start = Math.min(ch.getStart(), this.start);
            this.end = Math.max(ch.getEnd(), this.end);
        }
        for (int i = 0; i < this.track_definitions.size(); i++) {
            Pair<String, String> def = this.track_definitions.get(i);
            String fg_id = def.getFirst();
            String bg_id = def.getSecond();
            DialogChannel fg = this.channel_by_id.get(fg_id);
            DialogChannel bg = this.channel_by_id.get(bg_id);
            if (fg != null || bg != null) {
                DialogTrack track = new DialogTrack(fg, bg);
                this.tracks.add(track);
                assigned_channels.add(fg_id);
                assigned_channels.add(bg_id);
            }
        }
        for (DialogChannel ch : this.channels) {
            String ch_id = ch.getId();
            if (ch_id != null && assigned_channels.contains(ch_id)) continue; else this.tracks.add(new DialogTrack(ch));
        }
    }

    /**
     * @return Returns the channels.
     */
    public ArrayList<DialogChannel> getChannels() {
        return channels;
    }

    /**
     * @param channels The channels to set.
     */
    public void setChannels(ArrayList<DialogChannel> channels) {
        this.channels = channels;
    }

    /**
     * @return Returns the tracks.
     */
    public ArrayList<DialogTrack> getTracks() {
        return tracks;
    }

    /**
     * @param tracks The tracks to set.
     */
    public void setTracks(ArrayList<DialogTrack> tracks) {
        this.tracks = tracks;
    }

    @Override
    public String toString() {
        String res = String.valueOf(this.channels.size());
        return res + this.tracks.toString();
    }

    /**
     * @return Returns the end.
     */
    public float getEnd() {
        return end;
    }

    /**
     * @param end The end to set.
     */
    public void setEnd(float end) {
        this.end = end;
    }

    /**
     * @return Returns the start.
     */
    public double getStart() {
        return start;
    }

    /**
     * @param start The start to set.
     */
    public void setStart(float start) {
        this.start = start;
    }

    public float getDuration() {
        return this.end - this.start;
    }
}
