package ch.laoe.clip;

import java.util.ArrayList;
import ch.laoe.operation.AOperation;
import ch.laoe.ui.LProgressViewer;

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


Class:			ALayerSelection
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	groups multiple selections

History:
Date:			Description:									Autor:
25.07.00		erster Entwurf									oli4
03.08.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
16.05.01		add start/endOperation						oli4

***********************************************************/
public class ALayer2DSelection {

    /**
	* constructor
	*/
    public ALayer2DSelection(ALayer l) {
        channelSelection = new ArrayList<AChannel2DSelection>();
    }

    private ArrayList<AChannel2DSelection> channelSelection;

    /**
	*	add a channel selection
	*/
    public void addChannelSelection(AChannel2DSelection s) {
        channelSelection.add(s);
    }

    public AChannel2DSelection getChannelSelection(int index) {
        return channelSelection.get(index);
    }

    public int getNumberOfChannelSelections() {
        return channelSelection.size();
    }

    /**
	*	returns true if anything is selected
	*/
    public boolean isSelected() {
        for (int i = 0; i < getNumberOfChannelSelections(); i++) {
            if (getChannelSelection(i).isSelected()) {
                return true;
            }
        }
        return false;
    }

    /**
	*	operate one-channel operations, each selected channel. overwrite channels.
	*/
    public void operateEachChannel(AOperation o) {
        LProgressViewer.getInstance().entrySubProgress(0.5, "layer");
        o.startOperation();
        for (int j = 0; j < getNumberOfChannelSelections(); j++) {
            if (getChannelSelection(j).isSelected()) {
                if (LProgressViewer.getInstance().setProgress(1.0 * (j + 1) / getNumberOfChannelSelections())) return;
                LProgressViewer.getInstance().entrySubProgress(0.3, "channel " + j);
                o.operate(getChannelSelection(j));
                LProgressViewer.getInstance().exitSubProgress();
            }
        }
        o.endOperation();
        System.gc();
        LProgressViewer.getInstance().exitSubProgress();
    }
}
