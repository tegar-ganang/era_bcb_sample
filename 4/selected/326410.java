package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
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


Class:			AOHarmonicsGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	generate harmonics signal with superposed
					sinus.

History:
Date:			Description:									Autor:
30.05.01		first draft										oli4

***********************************************************/
public class AOHarmonicsGenerator extends AOperation {

    /**
   *	constructor
   */
    public AOHarmonicsGenerator(float[] amplitude, float offset, int basePeriod, boolean add) {
        super();
        this.amplitude = amplitude;
        this.offset = offset;
        this.basePeriod = basePeriod;
        this.add = add;
    }

    private float amplitude[];

    private float offset;

    private int basePeriod;

    private boolean add;

    /**
	*	performs the segments generation
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        MMArray tmp = new MMArray(basePeriod, 0);
        for (int i = 0; i < tmp.getLength(); i++) {
            tmp.set(i, offset);
            for (int j = 0; j < amplitude.length; j++) {
                tmp.set(i, tmp.get(i) + amplitude[j] * (float) Math.sin((double) i / (double) basePeriod * 2 * Math.PI * (j + 1)));
            }
        }
        for (int i = 0; i < l1; i++) {
            float s;
            if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
            if (add) {
                s = s1.get(i + o1) + tmp.get(i % tmp.getLength());
            } else {
                s = tmp.get(i % tmp.getLength());
            }
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s));
        }
    }
}
