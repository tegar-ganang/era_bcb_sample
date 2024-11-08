package ch.laoe.clip;

import ch.laoe.plugin.GPSpectrogramSelect;
import ch.laoe.ui.GEditableArea;
import ch.laoe.ui.GGraphicObjects;
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


Class:			AChannel
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	channel model.

History:
Date:			Description:									Autor:
26.07.00		erster Entwurf									oli4
02.08.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
28.12.00		stream-based									oli4
02.01.01		separate operation and view stream		oli4
24.01.01		array-based again...							oli4
02.03.02		change mask-concept to channel-mask		oli4
02.07.02		add markers										oli4

***********************************************************/
public class AChannel extends AModel {

    /**
	* constructor
	*/
    public AChannel() {
        super();
        markChange();
        audible = true;
        selection = new AChannelSelection(this);
        setPlotType(ALayer.SAMPLE_CURVE_TYPE);
        mask = new AChannelMask(this);
        marker = new AChannelMarker(this);
        samples = new MMArray(10, 0);
        graphicObjects = new GGraphicObjects();
        graphicObjects.setChannel(this);
    }

    /**
	* constructor
	*/
    public AChannel(int length) {
        super();
        markChange();
        audible = true;
        selection = new AChannelSelection(this);
        setPlotType(ALayer.SAMPLE_CURVE_TYPE);
        mask = new AChannelMask(this);
        marker = new AChannelMarker(this);
        samples = new MMArray(length, 0);
        graphicObjects = new GGraphicObjects();
        graphicObjects.setChannel(this);
    }

    /**
	* constructor
	*/
    public AChannel(AChannelSelection chs) {
        super();
        markChange();
        audible = true;
        selection = new AChannelSelection(this);
        setPlotType(ALayer.SAMPLE_CURVE_TYPE);
        mask = new AChannelMask(this);
        marker = new AChannelMarker(this);
        samples = new MMArray(chs.getLength(), 0);
        samples.copy(chs.getChannel().samples, chs.getOffset(), 0, samples.getLength());
        graphicObjects = new GGraphicObjects();
        graphicObjects.setChannel(this);
    }

    /**
	* copy-constructor
	*/
    public AChannel(AChannel ch) {
        super();
        markChange();
        audible = true;
        selection = new AChannelSelection(ch.getSelection());
        ((AChannelSelection) selection).setChannel(this);
        setPlotType(ALayer.SAMPLE_CURVE_TYPE);
        mask = new AChannelMask(this);
        marker = new AChannelMarker(ch.marker);
        samples = new MMArray(ch.getSampleLength(), 0);
        samples.copy(ch.samples, 0, 0, ch.samples.getLength());
        graphicObjects = new GGraphicObjects();
        graphicObjects.setChannel(this);
        name = GLanguage.translate("copyOf") + " " + ch.name;
    }

    public void destroy() {
        super.destroy();
    }

    /**
	* returns the layer which contains this channel
	*/
    public ALayer getParentLayer() {
        return (ALayer) getParent();
    }

    /**
	* returns the clip which contains this channel
	*/
    public AClip getParentClip() {
        return (AClip) getParent().getParent();
    }

    /**
	 *	unique channel-ID:
	 *	this ID is not used by the clip. 
	 */
    private static long uniqueIdIndex = 0;

    private String uniqueId;

    /**
	 *	change the unique channel-ID, e.g. to give a new reference, when
	 *	data have been changed (history-tracing... etc.).
	 */
    public void markChange() {
        synchronized (this) {
            uniqueIdIndex++;
        }
        uniqueId = "channel" + uniqueIdIndex;
    }

    /**
	 *	get the current channel-ID
	 */
    String getChangeId() {
        return uniqueId;
    }

    /**
	 *	set any new channel-ID
	 */
    void setChangeId(String id) {
        uniqueId = id;
    }

    private MMArray samples;

    public MMArray getSamples() {
        return samples;
    }

    public void setSamples(MMArray s) {
        samples = s;
    }

    /**
    * get the sample at index, with limitating the index.
    */
    public float getSample(int index) {
        return samples.get(index);
    }

    public void prepareMask() {
        mask.prepareResults();
    }

    /**
    * get the sample at index, with volume-mask considered.
    */
    public float getMaskedSample(int index) {
        if (isAudible()) {
            if (mask.isEnabled()) {
                return samples.get(index) * mask.getSample(index);
            } else {
                return samples.get(index);
            }
        } else {
            return 0;
        }
    }

    /**
    * set the sample at index, with limitating the index
    */
    public void setSample(int index, float value) {
        samples.set(index, value);
    }

    public int limitIndex(int index) {
        if (index < 0) {
            return 0;
        } else if (index >= samples.getLength()) {
            return samples.getLength() - 1;
        } else {
            return index;
        }
    }

    private int oldType = -1;

    public void setPlotType(int type) {
        if (type != oldType) {
            if (plotter != null) plotter.destroy();
            switch(type) {
                case ALayer.SAMPLE_CURVE_TYPE:
                    plotter = new AChannelPlotterSampleCurve(this, getPlotter());
                    break;
                case ALayer.SPECTROGRAM_TYPE:
                    plotter = new AChannelPlotterSpectrogram(this, getPlotter());
                    break;
            }
        }
        oldType = type;
    }

    /**
	*	get plotter
	*/
    public AChannelPlotter getPlotter() {
        return (AChannelPlotter) plotter;
    }

    private static int nameCounter;

    /**
	*	set the default name of the layer
	*/
    public void setDefaultName() {
        setDefaultName("channel", nameCounter++);
    }

    private boolean audible;

    /**
	*	set audible
	*/
    public void setAudible(boolean a) {
        audible = a;
    }

    /**
	*	returns audible
	*/
    public boolean isAudible() {
        return audible;
    }

    /**
	*	return the sample length
	*/
    public int getSampleLength() {
        return samples.getLength();
    }

    /**
	*	return the maximum absolute sample value
	*/
    public float getMaxSampleValue() {
        return getMaxSampleValue(0, getSampleLength());
    }

    /**
	*	return the maximum absolute sample value in the given range
	*/
    public float getMaxSampleValue(int offset, int length) {
        float m = 0.f;
        int start = limitIndex(offset);
        int end = limitIndex(offset + length);
        for (int i = start; i < end; i++) {
            float s = Math.abs(samples.get(i));
            if (s > m) m = s;
        }
        return m;
    }

    private AChannelMask mask;

    /**
	*	return the mask of this channel
	*/
    public AChannelMask getMask() {
        return mask;
    }

    /**
	*	return true, if masking is used (if mask-points are defined)
	*/
    public boolean isMaskEnabled() {
        return mask.isEnabled();
    }

    private AChannelMarker marker;

    /**
	*	return the markers of this channel
	*/
    public AChannelMarker getMarker() {
        return marker;
    }

    public void setMarker(AChannelMarker m) {
        marker = m;
        marker.setChannel(this);
    }

    /**
	*	set a selection
	*/
    public void setSelection(AChannelSelection s) {
        selection = s;
    }

    /**
	*	modify a selection
	*/
    public void modifySelection(int offset, int length) {
        if (selection != null) {
            ((AChannelSelection) selection).setOffset(offset);
            ((AChannelSelection) selection).setLength(length);
        } else {
            selection = new AChannelSelection(this, offset, length);
        }
    }

    /**
	*	get a Selection
	*/
    public AChannelSelection getSelection() {
        return (AChannelSelection) selection;
    }

    /**
	*	get a Selection, return full selection if not selected
	*/
    public AChannelSelection getNonEmptySelection() {
        if (!selection.isSelected()) {
            return createSelection();
        } else {
            return getSelection();
        }
    }

    /**
	*	create an independent selection which selects the whole channel
	*/
    public AChannelSelection createSelection() {
        AChannelSelection s = new AChannelSelection(this, 0, getSampleLength());
        return s;
    }

    /**
	*	set the selection to select the full channel
	*/
    public void setFullSelection() {
        setSelection(createSelection());
    }

    /**
	*	set the selection to select the range from left to right marker relative to x.
	*/
    public void setMarkedSelection(int x) {
        AChannelMarker m = getMarker();
        int o = m.searchLeftMarker(x);
        int l = m.searchRightMarker(x) - o;
        modifySelection(o, l);
    }

    /**
	*	set the selection to select nothing of the channel
	*/
    public void setEmptySelection() {
        setSelection(new AChannelSelection(this, 0, 0));
    }

    public AChannel2DSelection get2DSelection() {
        return new AChannel2DSelection(this, ((GPSpectrogramSelect.Cookie) getCookies().getCookie("spectrogramSelect")).area);
    }

    private GGraphicObjects graphicObjects;

    public GGraphicObjects getGraphicObjects() {
        return graphicObjects;
    }
}
