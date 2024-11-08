package ch.laoe.clip;

import java.awt.Point;
import ch.laoe.operation.AOMix;
import ch.laoe.plugin.GPSpectrogramSelect;
import ch.laoe.ui.GEditableArea;
import ch.laoe.ui.GLanguage;

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


Class:			ALayer
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	layer model.

History:
Date:			Description:									Autor:
25.07.00		erster Entwurf									oli4
02.08.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
28.12.00		stream-based									oli4
24.01.01		array-based	again...							oli4

***********************************************************/
public class ALayer extends AContainerModel {

    /**
	* constructor
	*/
    public ALayer() {
        super();
        type = AUDIO_LAYER;
        plotType = SAMPLE_CURVE_TYPE;
        selection = new ALayerSelection(this);
        plotter = new ALayerPlotter(this);
    }

    /**
	*	easy constructor
	*/
    public ALayer(int channels) {
        super();
        type = AUDIO_LAYER;
        plotType = SAMPLE_CURVE_TYPE;
        selection = new ALayerSelection(this);
        plotter = new ALayerPlotter(this);
        for (int i = 0; i < channels; i++) {
            add(new AChannel());
        }
        setStandardChannelNames();
    }

    /**
	*	easy constructor
	*/
    public ALayer(int channels, int samples) {
        super();
        type = AUDIO_LAYER;
        plotType = SAMPLE_CURVE_TYPE;
        selection = new ALayerSelection(this);
        plotter = new ALayerPlotter(this);
        for (int i = 0; i < channels; i++) {
            add(new AChannel(samples));
        }
        setStandardChannelNames();
    }

    /**
	* copy-constructor
	*/
    public ALayer(ALayer l) {
        super();
        type = l.type;
        plotType = SAMPLE_CURVE_TYPE;
        selection = new ALayerSelection(this);
        plotter = new ALayerPlotter(this);
        getPlotter().setColor(l.getPlotter().getColor());
        this.name = GLanguage.translate("copyOf") + " " + l.name;
        for (int i = 0; i < l.getNumberOfChannels(); i++) {
            add(new AChannel(l.getChannel(i)));
        }
    }

    public void destroy() {
        super.destroy();
    }

    private void setStandardChannelNames() {
        int channels = getNumberOfChannels();
        if (channels == 1) {
            getChannel(0).setName(GLanguage.translate("mono"));
        } else if (channels == 2) {
            getChannel(0).setName(GLanguage.translate("left"));
            getChannel(1).setName(GLanguage.translate("right"));
        }
    }

    /**
	* returns the clip which contains this layer
	*/
    public AClip getParentClip() {
        return (AClip) getParent();
    }

    /**
    * mark change, when the layer has been modified, and the channel data must be reconsidered. 
    */
    public void markChange() {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            getChannel(i).markChange();
        }
    }

    public void add(AModel e) {
        super.add(e);
        setType(type);
        ((AChannel) e).setPlotType(plotType);
    }

    public void insert(AModel e, int index) {
        super.insert(e, index);
        setType(type);
        ((AChannel) e).setPlotType(plotType);
    }

    /**
	*	get view
	*/
    public ALayerPlotter getPlotter() {
        return (ALayerPlotter) plotter;
    }

    private int plotType;

    public static final int SAMPLE_CURVE_TYPE = 1;

    public static final int SPECTROGRAM_TYPE = 2;

    public void setPlotType(int type) {
        plotType = type;
        for (int i = 0; i < getNumberOfChannels(); i++) {
            getChannel(i).setPlotType(type);
        }
    }

    public int getPlotType() {
        return plotType;
    }

    private static int nameCounter;

    /**
	*	set the default name of the track
	*/
    public void setDefaultName() {
        setDefaultName("layer", nameCounter++);
    }

    private int type;

    public static final int SOLO_AUDIO_LAYER = 0;

    public static final int AUDIO_LAYER = 1;

    public static final int PARAMETER_LAYER = 2;

    /**
	*	set the layer type
	*/
    public void setType(int t) {
        type = t;
    }

    /**
	*	returns the layer type
	*/
    public int getType() {
        return type;
    }

    public AChannel getChannel(int index) {
        return (AChannel) get(index);
    }

    public void mergeDownChannel(int index) {
        if ((getNumberOfElements() > 1) && (index > 0)) {
            AChannelSelection ch1 = getChannel(index - 1).createSelection();
            AChannelSelection ch2 = getChannel(index).createSelection();
            ALayerSelection l = new ALayerSelection(this);
            l.addChannelSelection(ch1);
            l.addChannelSelection(ch2);
            l.operateChannel0WithChannel1(new AOMix());
            remove(index);
        }
    }

    public void mergeAllChannels() {
        while (getNumberOfElements() > 1) {
            mergeDownChannel(1);
        }
    }

    public AChannel getSelectedChannel() {
        return (AChannel) getSelected();
    }

    public int getNumberOfChannels() {
        return getNumberOfElements();
    }

    public AChannel getChannel(Point p) {
        try {
            return getChannel(getPlotter().getInsideChannelIndex(p));
        } catch (Exception e) {
            return null;
        }
    }

    /**
	*	return the biggest channel size of the whole clip
	*/
    public int getMaxSampleLength() {
        int m = 0;
        for (int i = 0; i < getNumberOfElements(); i++) {
            int s = getChannel(i).getSampleLength();
            if (s > m) m = s;
        }
        return m;
    }

    /**
	*	returns the biggest sample value (absolute value)
	*	of this layer
	*/
    public float getMaxSampleValue() {
        float m = 0;
        for (int i = 0; i < getNumberOfElements(); i++) {
            float s = getChannel(i).getMaxSampleValue();
            if (s > m) m = s;
        }
        return m;
    }

    /**
	*	returns the biggest sample value (absolute value)
	*	of this layer in the given range
	*/
    public float getMaxSampleValue(int offset, int length) {
        float m = 0;
        for (int i = 0; i < getNumberOfElements(); i++) {
            float s = getChannel(i).getMaxSampleValue(offset, length);
            if (s > m) m = s;
        }
        return m;
    }

    public void prepareMask() {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            getChannel(i).prepareMask();
        }
    }

    /**
	*	return true, if masking is used in one of the channels
	*/
    public boolean isMaskEnabled() {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            if (getChannel(i).isMaskEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
	*	set a selection
	*/
    public void setSelection(ALayerSelection s) {
        int n = Math.min(getNumberOfChannels(), s.getNumberOfChannelSelections());
        s.setLayer(this);
        for (int i = 0; i < n; i++) {
            getChannel(i).setSelection(s.getChannelSelection(i));
            s.getChannelSelection(i).setChannel(getChannel(i));
        }
        selection = s;
    }

    /**
	*	builds dynamically a layer-selection containing only selected channels
	*/
    public ALayerSelection getSelection() {
        ALayerSelection s = new ALayerSelection(this);
        for (int i = 0; i < getNumberOfChannels(); i++) {
            AChannelSelection chS = getChannel(i).getSelection();
            if (chS.isSelected()) {
                s.addChannelSelection(chS);
            }
        }
        if (!s.isSelected()) {
            return createSelection();
        }
        return s;
    }

    public ALayer2DSelection get2DSelection() {
        ALayer2DSelection s = new ALayer2DSelection(this);
        for (int i = 0; i < getNumberOfChannels(); i++) {
            AChannel2DSelection chS = getChannel(i).get2DSelection();
            if (chS.isSelected()) {
                s.addChannelSelection(chS);
            }
        }
        return s;
    }

    /**
	*	create a selection which selects the whole layer, and all channels
	*/
    public ALayerSelection createSelection() {
        ALayerSelection s = new ALayerSelection(this);
        for (int i = 0; i < getNumberOfChannels(); i++) {
            s.addChannelSelection(getChannel(i).createSelection());
        }
        return s;
    }

    /**
	*	modify all channel-selections
	*/
    public void modifySelection(int offset, int length) {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            getChannel(i).modifySelection(offset, length);
        }
    }

    /**
	*	set the selection to select the full layer, and all channels
	*/
    public void setFullSelection() {
        ALayerSelection s = new ALayerSelection(this);
        for (int i = 0; i < getNumberOfChannels(); i++) {
            getChannel(i).setFullSelection();
            s.addChannelSelection(getChannel(i).getSelection());
        }
        selection = s;
    }

    /**
	*	set the selection to select nothing of the layer
	*/
    public void setEmptySelection() {
        ALayerSelection s = new ALayerSelection(this);
        for (int i = 0; i < getNumberOfChannels(); i++) {
            getChannel(i).setEmptySelection();
            s.addChannelSelection(getChannel(i).getSelection());
        }
        selection = s;
    }

    public void wideCopySelection(AChannelSelection orig) {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            AChannel ch = getChannel(i);
            ((AChannelSelection) ch.getSelection()).copy(orig);
        }
    }

    public void wideCopyMask(AChannelMask orig) {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            AChannel ch = getChannel(i);
            ((AChannelMask) ch.getMask()).copy(orig);
        }
    }

    public void wideCopySpectrogramSelection(GEditableArea orig) {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            AChannel ch = getChannel(i);
            GPSpectrogramSelect.Cookie c = ((GPSpectrogramSelect.Cookie) ch.getCookies().getCookie("spectrogramSelect"));
            if (c != null) {
                c.area.copy(orig);
            }
        }
    }
}
