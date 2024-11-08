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


Class:			AOSinusSweepGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	generate sinus sweep signal

History:
Date:			Description:									Autor:
28.04.01		first draft										oli4

***********************************************************/
public class AOSinusSweepGenerator extends AOperation {

    /**
   *	constructor
   */
    public AOSinusSweepGenerator(float amplitude, float offset, int startPeriod, int endPeriod, boolean add) {
        super();
        this.amplitude = amplitude;
        this.offset = offset;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.add = add;
    }

    private float amplitude, offset;

    private int startPeriod, endPeriod;

    private boolean add;

    /**
	*	performs the segments generation
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        float p = startPeriod;
        double rad = 0;
        double exp = Math.pow((double) endPeriod / startPeriod, 1. / l1);
        ch1.getChannel().markChange();
        for (int i = 0; i < l1; i++) {
            p *= exp;
            rad += 1. / p * 2 * Math.PI;
            float s = 0;
            if (add) {
                s = s1.get(o1 + i);
            }
            s += offset + amplitude * (float) Math.sin(rad);
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s));
        }
    }
}
