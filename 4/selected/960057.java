package com.timenes.clips.player;

import java.util.List;
import com.timenes.clips.platform.Platform;
import com.timenes.clips.platform.utils.SortableList;

/**
 * @author helge@timenes.com
 * 
 */
public class AbstractPlayer implements TickListener {

    private int channel;

    private int debugLevel = 4;

    private SortableList<NoteEvent> events;

    private int notesPlaying;

    private int tick;

    public AbstractPlayer(int channel) {
        this.channel = channel;
        events = Platform.getUtilsFactory().createSortableList(NoteEvent.class);
    }

    protected void enque(List<NoteEvent> events) {
        for (int i = 0; i < events.size(); i++) {
            enque(events.get(i));
        }
    }

    protected synchronized void enque(NoteEvent event) {
        if (event.getTick() < tick) return;
        int index = events.binarySearch(event);
        if (index < 0) {
            events.add(-index - 1, event);
        } else {
            events.add(index, event);
        }
    }

    public int getChannel() {
        return channel;
    }

    public int getDebugLevel() {
        return debugLevel;
    }

    public int getNotesPlaying() {
        return notesPlaying;
    }

    public int getTick() {
        return tick;
    }

    protected void log(String msg) {
        System.out.println(getClass().getSimpleName() + ": Tick " + tick + ": " + msg);
    }

    protected void log(String prefix, NoteEvent e) {
        log(prefix + "Tick= " + e.getTick() + ", type =  " + e.getType() + ", pitch =  " + e.getPitch() + ", velocity = " + e.getVelocity());
    }

    public void play() {
        notesPlaying = 0;
        tick = 0;
        Ticker.setTickListener(this);
        Ticker.setTempo(120);
        Ticker.start();
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    @Override
    public synchronized void tick(int tickCount) {
        this.tick = tickCount;
        if (events.size() == 0) {
            return;
        }
        if (debugLevel > 4) {
            log("tick(): Next event on " + events.get(0).getTick());
        }
        while (events.size() > 0 && events.get(0).getTick() == tick) {
            NoteEvent e = events.get(0);
            events.remove(0);
            if (debugLevel > 3) {
                log("Triggering event ", e);
            }
            if (e.getType() == NoteEvent.Type.NOTE_ON) {
                Platform.getMidiSystem().noteOn(channel, e.getPitch(), e.getVelocity());
                notesPlaying++;
            }
            if (e.getType() == NoteEvent.Type.NOTE_OFF) {
                Platform.getMidiSystem().noteOff(channel, e.getPitch());
                notesPlaying--;
            }
            if (e.getType() == NoteEvent.Type.PITCH_BEND) {
                Platform.getMidiSystem().pitchBend(channel, e.getPitch());
            }
        }
    }
}
