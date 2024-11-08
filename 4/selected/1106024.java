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


Class:			AORampGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	generate a ramp signal

History:
Date:			Description:									Autor:
13.09.01		first draft										oli4

***********************************************************/
public class AORampGenerator extends AOperation {

    /**
   *	constructor
   */
    public AORampGenerator(float startAmplitude, float endAmplitude, boolean add, boolean normalized) {
        super();
        this.startAmplitude = startAmplitude;
        this.endAmplitude = endAmplitude;
        this.add = add;
        this.normalized = normalized;
    }

    private float startAmplitude, endAmplitude;

    private boolean add, normalized;

    /**
	*	performs the segments generation
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        MMArray x = new MMArray(2, 0);
        MMArray y = new MMArray(2, 0);
        x.set(0, 0);
        x.set(1, l1);
        if (normalized) {
            y.set(0, o1);
            y.set(1, o1 + l1);
        } else {
            y.set(0, startAmplitude);
            y.set(1, endAmplitude);
        }
        if (add) {
            for (int i = 0; i < l1; i++) {
                s1.set(o1 + i, ch1.mixIntensity(i + o1, s1.get(o1 + i), s1.get(o1 + i) + AOToolkit.interpolate1(x, y, i)));
            }
        } else {
            for (int i = 0; i < l1; i++) {
                s1.set(o1 + i, ch1.mixIntensity(i + o1, s1.get(o1 + i), AOToolkit.interpolate1(x, y, i)));
            }
        }
    }
}
