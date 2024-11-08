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


Class:			AOTriangleGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	generate triangle signal

History:
Date:			Description:									Autor:
28.04.01		first draft										oli4

***********************************************************/
public class AOTriangleGenerator extends AOperation {

    /**
   *	constructor
   */
    public AOTriangleGenerator(float amplitude, float offset, int period, float dutyCycle, boolean add) {
        super();
        this.amplitude = amplitude;
        this.offset = offset;
        this.period = period;
        this.dutyCycle = dutyCycle;
        this.add = add;
    }

    private float amplitude, offset, dutyCycle;

    private int period;

    private boolean add;

    /**
	*	performs the segments generation
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        MMArray x = new MMArray(3, 0);
        MMArray y = new MMArray(3, 0);
        x.set(0, 0);
        x.set(1, period * dutyCycle);
        x.set(2, period);
        y.set(0, offset - amplitude);
        y.set(1, offset + amplitude);
        y.set(2, offset - amplitude);
        MMArray tmp = new MMArray(period, 0);
        for (int i = 0; i < tmp.getLength(); i++) {
            tmp.set(i, AOToolkit.interpolate1(x, y, i));
        }
        for (int i = 0; i < l1; i++) {
            float s = s1.get(o1 + i);
            if (add) {
                s += tmp.get(i % tmp.getLength());
            } else {
                s = tmp.get(i % tmp.getLength());
            }
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s));
        }
    }
}
