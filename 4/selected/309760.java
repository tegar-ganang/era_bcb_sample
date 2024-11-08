package de.sciss.eisenkraut.timeline;

import java.util.List;
import de.sciss.eisenkraut.io.AudioTrail;
import de.sciss.eisenkraut.net.OSCRouter;
import de.sciss.eisenkraut.net.OSCRouterWrapper;
import de.sciss.eisenkraut.net.RoutedOSCMessage;
import de.sciss.eisenkraut.session.Session;
import de.sciss.eisenkraut.session.SessionCollection;
import de.sciss.app.AbstractApplication;
import de.sciss.common.ProcessingThread;
import de.sciss.timebased.Trail;
import de.sciss.util.Flag;

public class AudioTracks extends SessionCollection implements OSCRouter {

    private static final String OSC_AUDIO = "audio";

    private final OSCRouterWrapper osc;

    private final Session doc;

    private AudioTrail trail;

    public AudioTracks(Session doc) {
        super();
        this.doc = doc;
        osc = new OSCRouterWrapper(doc, this);
    }

    public void setTrail(AudioTrail t) {
        trail = t;
    }

    public Trail getTrail() {
        return trail;
    }

    public boolean isSelected(AudioTrack t) {
        return doc.selectedTracks.contains(t);
    }

    public static boolean checkSyncedAudio(List tis, boolean changesTimeline, ProcessingThread context, Flag hasSelectedAudio) {
        Track.Info ti;
        hasSelectedAudio.set(false);
        for (int i = 0; i < tis.size(); i++) {
            ti = (Track.Info) tis.get(i);
            if (changesTimeline && !ti.getChannelSync()) {
                if (context != null) context.setException(new IllegalStateException(AbstractApplication.getApplication().getResourceString("errAudioWillLooseSync")));
                return false;
            }
            if ((ti.trail instanceof AudioTrail) && ti.selected) {
                hasSelectedAudio.set(true);
            }
        }
        return true;
    }

    public String oscGetPathComponent() {
        return OSC_AUDIO;
    }

    public void oscRoute(RoutedOSCMessage rom) {
        osc.oscRoute(rom);
    }

    public void oscAddRouter(OSCRouter subRouter) {
        osc.oscAddRouter(subRouter);
    }

    public void oscRemoveRouter(OSCRouter subRouter) {
        osc.oscRemoveRouter(subRouter);
    }

    public Object oscQuery_count() {
        return new Integer(size());
    }
}
