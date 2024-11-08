package de.sciss.eisenkraut.timeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.sciss.eisenkraut.io.AudioTrail;
import de.sciss.eisenkraut.session.AbstractSessionObject;
import de.sciss.timebased.Trail;

/**
 *  A simple implementation of the <code>Transmitter</code>
 *  interface that does not yet make assumptions
 *  about the data structure but provides some
 *  common means useful for all transmitters.
 *  It provides the basic mechanism for XML import and
 *  export, it handles all methods except
 *  <code>getTrackEditor</code>.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.70, 07-Dec-07
 */
public abstract class Track extends AbstractSessionObject {

    /**
	 *  Constructs a new empty transmitter.
	 *  Basic initialization is achieved by
	 *  adding a preexisting file to the track editor,
	 *  calling <code>setName</code> etc. methods.
	 */
    protected Track() {
        super();
    }

    public abstract Trail getTrail();

    public void clear(Object source) {
        getTrail().clear(source);
    }

    public static List getInfos(List selectedTracks, List allTracks) {
        Track track;
        Trail trail;
        Track.Info ti;
        int chan;
        final Map mapInfos = new HashMap();
        final List collInfos = new ArrayList();
        for (int i = 0; i < allTracks.size(); i++) {
            track = (Track) allTracks.get(i);
            trail = track.getTrail();
            ti = (Track.Info) mapInfos.get(trail.getClass());
            if (ti == null) {
                ti = new Info(trail);
                mapInfos.put(ti.trail.getClass(), ti);
                collInfos.add(ti);
            }
            if (track instanceof AudioTrack) {
                chan = ((AudioTrack) track).getChannelIndex();
            } else {
                chan = 0;
            }
            if (selectedTracks.contains(track)) {
                ti.selected = true;
                ti.trackMap[chan] = true;
                ti.numTracks++;
            }
        }
        return collInfos;
    }

    public static class Info {

        public final Trail trail;

        public boolean selected = false;

        public final boolean[] trackMap;

        public final int numChannels;

        public int numTracks = 0;

        protected Info(Trail trail) {
            this.trail = trail;
            if (trail instanceof AudioTrail) {
                numChannels = ((AudioTrail) trail).getChannelNum();
            } else {
                numChannels = 1;
            }
            trackMap = new boolean[numChannels];
        }

        public boolean getChannelSync() {
            if (numChannels == 0) return true;
            final boolean first = trackMap[0];
            for (int i = 1; i < numChannels; i++) {
                if (trackMap[i] != first) return false;
            }
            return true;
        }

        public int[] createChannelMap(int numCh2, int offset, boolean skipUnused) {
            final int[] chanMap = new int[numCh2];
            int i, j;
            for (i = 0, j = offset; (i < this.numChannels) && (j < numCh2); i++) {
                if (trackMap[i]) {
                    chanMap[j++] = i;
                } else if (skipUnused) {
                    chanMap[j++] = -1;
                }
            }
            while (j < numCh2) {
                chanMap[j++] = -1;
            }
            return chanMap;
        }
    }
}
