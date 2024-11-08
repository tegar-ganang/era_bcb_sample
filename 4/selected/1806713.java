package ch.laoe.clip;

import java.io.File;
import ch.laoe.audio.Audio;
import ch.laoe.audio.AudioException;
import ch.laoe.operation.AOAmplify;
import ch.laoe.operation.AOMix;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.GPersistence;
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


Class:			AClip
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	clip model.

History:
Date:			Description:									Autor:
04.08.00		erster Entwurf									oli4
10.11.00		attach undostack to AClip					oli4
19.12.00		float audio samples							oli4
28.12.00		stream-based									oli4
24.01.01		array-based	again...							oli4
02.03.02		channel mask introduced						oli4

***********************************************************/
public class AClip extends AContainerModel {

    /**
	* default constructor
	*/
    public AClip() {
        super();
        markChange();
        sampleRate = DEFAULT_SAMPLE_RATE;
        sampleWidth = DEFAULT_SAMPLE_WIDTH;
        comments = GPersistence.createPersistance().getString("clip.defaultComment");
        selection = new AClipSelection(this);
        plotter = new AClipPlotter(this);
    }

    /**
	*	easy constructor
	*/
    public AClip(int layers, int channels) {
        this();
        for (int i = 0; i < layers; i++) add(new ALayer(channels));
        audio = new Audio(this);
        history = new AClipHistory(this);
        history.store(GLanguage.translate("initialState"));
    }

    /**
	*	easy constructor
	*/
    public AClip(int layers, int channels, int samples) {
        this();
        for (int i = 0; i < layers; i++) add(new ALayer(channels, samples));
        audio = new Audio(this);
        history = new AClipHistory(this);
        history.store(GLanguage.translate("initialState"));
    }

    /**
	* copy-constructor
	*/
    public AClip(AClip c) {
        this();
        this.sampleRate = c.sampleRate;
        this.sampleWidth = c.sampleWidth;
        this.comments = c.comments;
        this.name = c.name;
        this.selection = c.selection;
        for (int i = 0; i < c.getNumberOfLayers(); i++) {
            add(new ALayer(c.getLayer(i)));
        }
        audio = new Audio(this);
        history = new AClipHistory(c.history, this);
    }

    /**
	*	open file constructor
	*/
    public AClip(File fileName) throws AudioException {
        this();
        LProgressViewer.getInstance().entrySubProgress(0.6);
        if (LProgressViewer.getInstance().setProgress(0.1)) return;
        audio = new Audio(this);
        audio.open(fileName);
        history = new AClipHistory(this);
        history.store(GLanguage.translate("initialState"));
        LProgressViewer.getInstance().exitSubProgress();
    }

    /**
	 *	destroys the clip.
	 */
    public void destroy() {
        LProgressViewer.getInstance().entrySubProgress(GLanguage.translate("cleanup") + " " + getName());
        LProgressViewer.getInstance().setNote(getName());
        super.destroy();
        if (audio != null) audio.destroy();
        audio = null;
        System.gc();
        LProgressViewer.getInstance().exitSubProgress();
    }

    /**
	 * copies all attributes except the samples
	 * @param c
	 */
    public void copyAllAttributes(AClip c) {
        this.sampleRate = c.sampleRate;
        this.sampleWidth = c.sampleWidth;
        this.comments = c.comments;
        this.name = c.name;
        this.selection = c.selection;
    }

    private Audio audio;

    public Audio getAudio() {
        return audio;
    }

    /**
	 *	unique clip-ID:
	 *	this ID is not used by the clip. 
	 */
    private static int uniqueIdIndex = 0;

    private String uniqueId;

    /**
	 *	change the unique clip-ID, e.g. to give a new reference, when
	 *	data have been changed (history-tracing... etc.).
	 */
    public void markChange() {
        synchronized (this) {
            uniqueIdIndex++;
        }
        uniqueId = "clip" + uniqueIdIndex;
    }

    /**
	 *	get the current channel-ID
	 */
    public String getChangeId() {
        return uniqueId;
    }

    private AClipHistory history;

    /**
	*	get history
	*/
    public AClipHistory getHistory() {
        return history;
    }

    /**
	*	get selection
	*/
    public AClipSelection getSelection() {
        return (AClipSelection) selection;
    }

    /**
	*	get view
	*/
    public AClipPlotter getPlotter() {
        return (AClipPlotter) plotter;
    }

    public void setPlotType(int type) {
        for (int i = 0; i < getNumberOfLayers(); i++) {
            getLayer(i).setPlotType(type);
        }
    }

    private static int nameCounter;

    /**
	*	set the default name of the layer
	*/
    public void setDefaultName() {
        setDefaultName("clip", nameCounter++);
    }

    private String comments;

    /**
	*	get the comment of this clip
	*/
    public String getComments() {
        return comments;
    }

    /**
	*	set the comment of this clip
	*/
    public void setComments(String n) {
        comments = n;
    }

    public void prepareMask() {
        for (int i = 0; i < getNumberOfLayers(); i++) {
            getLayer(i).prepareMask();
        }
    }

    public ALayer getLayer(int index) {
        return (ALayer) get(index);
    }

    public void mergeDownLayer(int index) {
        if ((getNumberOfElements() > 1) && (index > 0)) {
            ALayer l0 = getLayer(index - 1);
            ALayer l1 = getLayer(index);
            ALayerSelection s0 = l0.createSelection();
            ALayerSelection s1 = l1.createSelection();
            AClipSelection c = new AClipSelection(this);
            c.addLayerSelection(s0);
            c.addLayerSelection(s1);
            c.operateLayer0WithLayer1(new AOMix());
            remove(index);
            for (int i = 0; i < l0.getNumberOfChannels(); i++) {
                l0.getChannel(i).getMask().clear();
            }
        }
    }

    public void mergeAllLayers() {
        while (getNumberOfElements() > 1) {
            mergeDownLayer(1);
        }
    }

    public ALayer getSelectedLayer() {
        return (ALayer) getSelected();
    }

    public int getNumberOfLayers() {
        return getNumberOfElements();
    }

    public int getMaxNumberOfChannels() {
        return getMaxNumberOfSubElements();
    }

    /**
	*	return the biggest channel size of the whole clip
	*/
    public int getMaxSampleLength() {
        int m = 0;
        for (int i = 0; i < getNumberOfElements(); i++) {
            int s = getLayer(i).getMaxSampleLength();
            if (s > m) m = s;
        }
        return m;
    }

    /**
	*	returns the biggest sample value (absolute value)
	*	of this clip
	*/
    public float getMaxSampleValue() {
        float m = 0;
        for (int i = 0; i < getNumberOfElements(); i++) {
            float s = getLayer(i).getMaxSampleValue();
            if (s > m) m = s;
        }
        return m;
    }

    /**
	*	returns the biggest sample value (absolute value)
	*	of this clip in the given x-range
	*/
    public float getMaxSampleValue(int offset, int length) {
        float m = 0;
        for (int i = 0; i < getNumberOfElements(); i++) {
            float s = getLayer(i).getMaxSampleValue(offset, length);
            if (s > m) m = s;
        }
        return m;
    }

    private float sampleRate;

    private static final float DEFAULT_SAMPLE_RATE = 8000.f;

    public float getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(float s) {
        if (s > 48000.f) sampleRate = 48000.f; else if (s < 1000.f) sampleRate = 1000.f; else sampleRate = s;
        audio.changeSampleRate(s);
    }

    private int sampleWidth;

    private static final int DEFAULT_SAMPLE_WIDTH = 16;

    public int getSampleWidth() {
        return sampleWidth;
    }

    public void setSampleWidth(int sw) {
        if (sw < 1) sampleWidth = 1; else if (sw > 32) sampleWidth = 32; else sampleWidth = sw;
    }

    private boolean bigEndian;

    public boolean isBigEndian() {
        return false;
    }

    public void setBigEndian(boolean b) {
        bigEndian = b;
    }
}
