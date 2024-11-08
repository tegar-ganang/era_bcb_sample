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
public class ALayerSelection extends ASelection {

    /**
	* constructor
	*/
    public ALayerSelection(ALayer l) {
        super(l);
        channelSelection = new ArrayList<AChannelSelection>();
    }

    /**
	* copy-constructor
	*/
    public ALayerSelection(ALayerSelection s) {
        this((ALayer) s.model);
        this.name = s.name;
        for (int i = 0; i < s.getNumberOfChannelSelections(); i++) {
            addChannelSelection(new AChannelSelection(s.getChannelSelection(i)));
        }
    }

    public ALayer getLayer() {
        return (ALayer) model;
    }

    public void setLayer(ALayer l) {
        model = l;
    }

    private static int nameCounter;

    /**
	*	set the default name of the layer
	*/
    public void setDefaultName() {
        setDefaultName("layerSelection", nameCounter++);
    }

    private ArrayList<AChannelSelection> channelSelection;

    /**
	*	add a channel selection
	*/
    public void addChannelSelection(AChannelSelection s) {
        channelSelection.add(s);
    }

    public AChannelSelection getChannelSelection(int index) {
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
	*	return the biggest channel-selection size 
	*/
    public int getMaxLength() {
        int max = 0;
        for (int i = 0; i < getNumberOfChannelSelections(); i++) {
            int l = getChannelSelection(i).getLength();
            if (l > max) {
                max = l;
            }
        }
        return max;
    }

    /**
	*	return the lowest selected index of all channels
	*/
    public int getLowestSelectedIndex() {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < getNumberOfChannelSelections(); i++) {
            int l = getChannelSelection(i).getOffset();
            if (l < min) {
                min = l;
            }
        }
        return min;
    }

    /**
	*	return the highest selected index of all channels
	*/
    public int getHighestSelectedIndex() {
        int max = 0;
        for (int i = 0; i < getNumberOfChannelSelections(); i++) {
            AChannelSelection chs = getChannelSelection(i);
            int l = chs.getOffset() + chs.getLength();
            if (l > max) {
                max = l;
            }
        }
        return max;
    }

    /**
	 *	clear the intensityof all channel-selections
	 */
    public void clearIntensity() {
        for (int i = 0; i < getNumberOfChannelSelections(); i++) {
            getChannelSelection(i).clearIntensity();
        }
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

    /**
	*	operate channel 0 and 1
	*/
    public void operateChannel0WithChannel1(AOperation o) {
        o.startOperation();
        if (getChannelSelection(0).isSelected()) {
            o.operate(getChannelSelection(0), getChannelSelection(1));
        }
        o.endOperation();
        System.gc();
    }

    /**
	*	operate channel 0 and 1 and 2
	*/
    public void operateChannel0WithChannel1WithChannel2(AOperation o) {
        o.startOperation();
        if (getChannelSelection(0).isSelected()) {
            o.operate(getChannelSelection(0), getChannelSelection(1), getChannelSelection(2));
        }
        o.endOperation();
        System.gc();
    }
}
