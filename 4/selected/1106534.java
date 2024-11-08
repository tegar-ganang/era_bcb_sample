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


Class:			AClipSelection
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	selection model.

History:
Date:			Description:									Autor:
10.01.01		first draft										oli4
16.05.01		add start/endOperation						oli4

***********************************************************/
public class AClipSelection extends ASelection {

    /**
	* constructor
	*/
    public AClipSelection(AClip c) {
        super(c);
        layerSelection = new ArrayList<ALayerSelection>();
    }

    /**
	* copy-constructor
	*/
    public AClipSelection(AClipSelection s) {
        this((AClip) s.model);
        this.name = s.name;
    }

    public AClip getClip() {
        return (AClip) model;
    }

    private static int nameCounter;

    /**
	*	set the default name of the layer
	*/
    public void setDefaultName() {
        setDefaultName("clipSelection", nameCounter++);
    }

    private ArrayList<ALayerSelection> layerSelection;

    /**
	*	add a channel selection
	*/
    public void addLayerSelection(ALayerSelection s) {
        layerSelection.add(s);
    }

    public ALayerSelection getLayerSelection(int index) {
        return layerSelection.get(index);
    }

    public int getNumberOfLayerSelections() {
        return layerSelection.size();
    }

    /**
	*	returns true if anything is selected
	*/
    public boolean isSelected() {
        for (int i = 0; i < getNumberOfLayerSelections(); i++) {
            if (getLayerSelection(i).isSelected()) {
                return true;
            }
        }
        return false;
    }

    /**
	*	operate one-channel operations, each selected channel of each selected
	*	layer. overwrite channels.
	*/
    public void operateEachChannel(AOperation o) {
        LProgressViewer.getInstance().entrySubProgress(0.7, "clip");
        o.startOperation();
        for (int i = 0; i < getNumberOfLayerSelections(); i++) {
            LProgressViewer.getInstance().entrySubProgress(1.0 / getNumberOfLayerSelections(), "layer " + i);
            for (int j = 0; j < getLayerSelection(i).getNumberOfChannelSelections(); j++) {
                LProgressViewer.getInstance().entrySubProgress(1.0 / getLayerSelection(i).getNumberOfChannelSelections(), "channel " + j);
                if (getLayerSelection(i).getChannelSelection(j).isSelected()) {
                    o.operate(getLayerSelection(i).getChannelSelection(j));
                }
                LProgressViewer.getInstance().exitSubProgress();
            }
            LProgressViewer.getInstance().exitSubProgress();
        }
        o.endOperation();
        System.gc();
        LProgressViewer.getInstance().exitSubProgress();
    }

    /**
	*	operate two-channel operations, layer0 with layer1, channel by
	*	channel, write result to layer0
	*/
    public void operateLayer0WithLayer1(AOperation o) {
        int n0 = getLayerSelection(0).getNumberOfChannelSelections();
        int n1 = getLayerSelection(1).getNumberOfChannelSelections();
        int n = Math.min(n0, n1);
        LProgressViewer.getInstance().entrySubProgress(0.7, "clip");
        o.startOperation();
        for (int i = 0; i < n; i++) {
            if (LProgressViewer.getInstance().setProgress(1.0 * (i + 1) / n)) return;
            if (getLayerSelection(0).getChannelSelection(i).isSelected()) o.operate(getLayerSelection(0).getChannelSelection(i), getLayerSelection(1).getChannelSelection(i));
        }
        o.endOperation();
        System.gc();
        LProgressViewer.getInstance().exitSubProgress();
    }

    /**
	*	operate three-channel operations, layer0 with layer1 and layer2, channel by
	*	channel, write result to layer0
	*/
    public void operateLayer0WithLayer1And2(AOperation o) {
        int n0 = getLayerSelection(0).getNumberOfChannelSelections();
        int n1 = getLayerSelection(1).getNumberOfChannelSelections();
        int n2 = getLayerSelection(1).getNumberOfChannelSelections();
        int n = Math.min(Math.min(n0, n1), n2);
        LProgressViewer.getInstance().entrySubProgress(0.7, "clip");
        o.startOperation();
        for (int i = 0; i < n; i++) {
            if (LProgressViewer.getInstance().setProgress(1.0 * (i + 1) / n)) return;
            if (getLayerSelection(0).getChannelSelection(i).isSelected()) {
                o.operate(getLayerSelection(0).getChannelSelection(i), getLayerSelection(1).getChannelSelection(i), getLayerSelection(2).getChannelSelection(i));
            }
        }
        o.endOperation();
        System.gc();
        LProgressViewer.getInstance().exitSubProgress();
    }
}
