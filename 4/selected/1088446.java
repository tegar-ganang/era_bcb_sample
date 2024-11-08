package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;

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


Class:			AOPitchGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	pitch generator.

History:
Date:			Description:									Autor:
25.05.01		first draft										oli4

***********************************************************/
public class AOPitchGenerator extends AOperation {

    public AOPitchGenerator(MMArray baseSignal) {
        super();
        this.baseSignal = baseSignal;
        smoothBaseSignal();
    }

    public AOPitchGenerator(MMArray baseSignal, float constPitch) {
        this(baseSignal);
        this.constPitch = constPitch;
    }

    private MMArray baseSignal;

    private float constPitch;

    private void smoothBaseSignal() {
        if (baseSignal.getLength() > 50) {
            AOToolkit.applyZeroCross(baseSignal, 0);
        }
    }

    /**
	 *	constant pitch generation
	 *	@param ch1	channel, where the pitch signal is applied
	 */
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        float pitchIndex = 0;
        ch1.getChannel().markChange();
        try {
            for (int i = 0; i < l1; i++) {
                float s = AOToolkit.interpolate3(baseSignal, pitchIndex);
                s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s));
                pitchIndex += constPitch;
            }
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }

    /**
	 *	variable pitch generation
	 *	@param ch1	channel, where the pitch signal is applied
	 *	@param ch2	channel, where the pitch value is defined
	 */
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray s2 = ch2.getChannel().getSamples();
        float pitchIndex = 0;
        ch1.getChannel().markChange();
        try {
            for (int i = 0; i < l1; i++) {
                float s = AOToolkit.interpolate3(baseSignal, pitchIndex);
                s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s));
                pitchIndex += s2.get(i + o1);
            }
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
