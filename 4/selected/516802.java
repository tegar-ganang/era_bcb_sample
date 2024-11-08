package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
import ch.laoe.ui.Debug;
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


Class:			AOMultiReverb
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	reverbation with multiple gaussian distributed
					feedback 

History:
Date:			Description:									Autor:
20.06.01		first draft										oli4
11.08.01		multireverb with user-definable room		oli4

***********************************************************/
public class AOMultiReverb extends AOperation {

    /**
	 * @param delay
	 * @param gain
	 * @param delayShape unit = 1 delay
	 * @param gainShape unit = 1 gain
	 * @param dry
	 * @param wet
	 * @param negFeedback
	 * @param backward
	 */
    public AOMultiReverb(int delay, float gain, MMArray delayShape, MMArray gainShape, float dry, float wet, boolean negFeedback, boolean backward) {
        super();
        this.delay = delay;
        this.gain = gain;
        this.delayShape = delayShape;
        this.gainShape = gainShape;
        this.dry = dry;
        this.wet = wet;
        this.negFeedback = negFeedback;
        this.backward = backward;
    }

    /**
	 * @param dry
	 * @param wet
	 * @param backward
	 */
    public AOMultiReverb(float dry, float wet, boolean backward) {
        super();
        this.delay = 1;
        this.gain = 1;
        this.dry = dry;
        this.wet = wet;
        this.negFeedback = false;
        this.backward = backward;
    }

    private int delay;

    private float gain;

    private float dry;

    private float wet;

    private boolean negFeedback;

    private boolean backward;

    MMArray delayShape = null;

    MMArray gainShape = null;

    /**
	 *	multi-echo using the shapes from constructor
	 */
    public void operate(AChannelSelection ch1) {
        MMArray sample = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray sample2 = new MMArray(l1, 0);
        MMArray sample3 = new MMArray(l1, 0);
        float oldRms = AOToolkit.rmsAverage(sample, o1, l1);
        ch1.getChannel().markChange();
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int k = 0; k < delayShape.getLength(); k++) {
                if (LProgressViewer.getInstance().setProgress((k + 1) * 1.0 / delayShape.getLength())) return;
                if (backward) {
                    for (int i = 0; i < sample2.getLength(); i++) {
                        sample2.set(l1 - 1 - i, sample.get(i + o1));
                    }
                } else {
                    sample2.copy(sample, o1, 0, sample2.getLength());
                }
                int d = (int) (delay * delayShape.get(k));
                float g = gain * gainShape.get(k);
                for (int i = 0; i < l1; i++) {
                    if ((i + d < l1) && (i + d >= 0)) {
                        if (negFeedback) {
                            sample2.set(i + d, sample2.get(i + d) - sample2.get(i) * g);
                        } else {
                            sample2.set(i + d, sample2.get(i + d) + sample2.get(i) * g);
                        }
                    }
                }
                if (backward) {
                    for (int i = 0; i < sample2.getLength(); i++) {
                        sample2.set(l1 - 1 - i, sample2.get(l1 - 1 - i) - sample.get(i + o1));
                    }
                } else {
                    for (int i = 0; i < sample2.getLength(); i++) {
                        sample2.set(i, sample2.get(i) - sample.get(i + o1));
                    }
                }
                for (int i = 0; i < l1; i++) {
                    sample3.set(i, sample3.get(i) + sample2.get(i));
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
            float newRms = AOToolkit.rmsAverage(sample3, 0, l1);
            AOToolkit.multiply(sample3, 0, l1, (float) (oldRms / newRms));
            if (backward) {
                for (int i = 0; i < l1; i++) {
                    float s = dry * sample.get(i + o1) + wet * sample3.get(l1 - 1 - i);
                    sample.set(i + o1, ch1.mixIntensity(i + o1, sample.get(i + o1), s));
                }
            } else {
                for (int i = 0; i < l1; i++) {
                    float s = dry * sample.get(i + o1) + wet * sample3.get(i);
                    sample.set(i + o1, ch1.mixIntensity(i + o1, sample.get(i + o1), s));
                }
            }
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }

    /**
      * multi-echo using a user-defined room
      *	@param ch1 samples
      *@param ch2 room
      */
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray shape = ch2.getChannel().getSamples();
        int shapeLength = 0;
        for (int i = 0; i < shape.getLength(); i++) {
            if (shape.get(i) != 0) {
                shapeLength++;
            }
        }
        this.delayShape = new MMArray(shapeLength, 0);
        this.gainShape = new MMArray(shapeLength, 0);
        int index = 0;
        for (int i = 0; i < shape.getLength(); i++) {
            if (shape.get(i) != 0) {
                delayShape.set(index, i);
                gainShape.set(index, shape.get(i));
                index++;
            }
        }
        operate(ch1);
    }
}
