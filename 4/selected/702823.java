package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
import ch.laoe.ui.Debug;

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


Class:			AOPitchShift
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	pitch shift operation

History:
Date:			Description:									Autor:
10.06.01		first draft (f-domain method)          oli4
01.08.01		time-domain block-resampling method    oli4
23.12.01		introduce smooth transition curve		oli4
18.06.02		reduce volume-flicker						oli4
06.05.2003	variable pitch shift added					oli4

***********************************************************/
public class AOPitchShift extends AOperation {

    /**
	 */
    public AOPitchShift(int bufferLength, float transitionFactor) {
        this(1.f, bufferLength, transitionFactor);
    }

    /**
	 * @param shiftFactor 1:neutral, <1:lowrer pitch, >1:higher pitch
	 * @param bufferLength lehgth of processing blocks
	 * @param transitionFactor length of transition from one block to the other 1=bufferlength
	 */
    public AOPitchShift(float shiftFactor, int bufferLength, float transitionFactor) {
        this.shiftFactor = shiftFactor;
        buffer = new MMArray(bufferLength, 0);
        transitionLength = (int) (bufferLength * transitionFactor);
    }

    private float shiftFactor;

    private int transitionLength;

    private MMArray buffer;

    /**
	*	performs a variable pitch shift, where the variable is
	*	channel2
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        performPitchShift(ch1, ch2);
    }

    /**
	*	performs a constant amplification
	*/
    public final void operate(AChannelSelection ch1) {
        performPitchShift(ch1, null);
    }

    /**
   *	performs a constant amplification
   */
    private void performPitchShift(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        try {
            MMArray tmp = new MMArray(l1, 0);
            tmp.copy(s1, o1, 0, tmp.getLength());
            int stepWidth = buffer.getLength() - transitionLength;
            int stepBegin = o1;
            while (stepBegin < o1 + l1) {
                for (int i = 0; i < buffer.getLength(); i++) {
                    float index;
                    if (ch2 != null) {
                        index = stepBegin - o1 + (float) i * ch2.getChannel().getSamples().get(stepBegin);
                    } else {
                        index = stepBegin - o1 + (float) i * shiftFactor;
                    }
                    if (index < l1) {
                        buffer.set(i, AOToolkit.interpolate3(tmp, index));
                    } else {
                        buffer.set(i, AOToolkit.interpolate3(tmp, i));
                    }
                }
                for (int i = 0; i < buffer.getLength(); i++) {
                    if (stepBegin + i < o1 + l1) {
                        float s;
                        if (i < transitionLength) {
                            float f3 = ((float) i) / transitionLength;
                            f3 *= f3;
                            float f4 = ((float) (transitionLength - i)) / transitionLength;
                            f4 *= f4;
                            s = s1.get(stepBegin + i) * f4 + buffer.get(i) * f3;
                        } else {
                            s = buffer.get(i);
                        }
                        s1.set(stepBegin + i, ch1.mixIntensity(stepBegin + i, s1.get(stepBegin + i), s));
                    }
                }
                stepBegin += stepWidth;
            }
            AOToolkit.applyZeroCross(s1, o1);
            AOToolkit.applyZeroCross(s1, o1 + l1);
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }
}
