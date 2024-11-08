package ch.laoe.plugin;

import ch.laoe.clip.AChannel;
import ch.laoe.clip.AChannelMarker;
import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.AClip;
import ch.laoe.clip.ALayer;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.Laoe;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			GPSplit
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	plugin to split a clip infunction of the markers. 

History:
Date:			Description:									Autor:
12.05.2003	first draft										oli4

***********************************************************/
public class GPSplitToNew extends GPlugin {

    public GPSplitToNew(GPluginHandler ph) {
        super(ph);
    }

    public String getName() {
        return "splitToNew";
    }

    public void start() {
        super.start();
        LProgressViewer.getInstance().entrySubProgress(getName());
        LProgressViewer.getInstance().entrySubProgress(0.7);
        ALayer l = getSelectedLayer();
        AChannelMarker m = l.getSelectedChannel().getMarker();
        int n = m.getNumberOfMarkers();
        int lastMarker = 0;
        for (int i = 0; i < n + 1; i++) {
            if (LProgressViewer.getInstance().setProgress(1.0 * i / (n + 1))) return;
            int currentMarker;
            if (i < n) {
                currentMarker = m.getMarkerX(i);
            } else {
                currentMarker = l.getSelectedChannel().getSampleLength();
            }
            AClip nc = new AClip(1, 0, 0);
            nc.copyAllAttributes(getFocussedClip());
            for (int j = 0; j < l.getNumberOfChannels(); j++) {
                AChannelSelection chs = new AChannelSelection(l.getChannel(j), lastMarker, currentMarker - lastMarker);
                nc.getLayer(0).add(new AChannel(chs));
            }
            nc.getAudio().setLoopEndPointer(nc.getLayer(0).getMaxSampleLength());
            nc.setDefaultName();
            Laoe.getInstance().addClipFrame(nc);
            autoScaleFocussedClip();
            lastMarker = currentMarker;
        }
        LProgressViewer.getInstance().exitSubProgress();
        LProgressViewer.getInstance().exitSubProgress();
    }
}
