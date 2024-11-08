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


Class:			AONoiseGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	generate different noise types

History:
Date:			Description:									Autor:
14.05.01		first draft										oli4

***********************************************************/
public class AONoiseGenerator extends AOperation {

    /**
   *	constructor
   */
    public AONoiseGenerator(float amplitude, float offset, int noiseType, boolean add) {
        super();
        this.amplitude = amplitude;
        this.offset = offset;
        this.noiseType = noiseType;
        this.add = add;
    }

    private float amplitude, offset;

    private int noiseType;

    public static final int WHITE = 1;

    public static final int TRIANGLE = 2;

    public static final int GAUSSIAN = 3;

    private boolean add;

    /**
	*	performs the segments generation
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        for (int i = o1; i < o1 + l1; i++) {
            float d = 0;
            switch(noiseType) {
                case WHITE:
                    d = (float) (offset + (2 * Math.random() - 1) * amplitude);
                    break;
                case TRIANGLE:
                    d = (float) (offset + (Math.random() + Math.random() - 1) * amplitude);
                    break;
                case GAUSSIAN:
                    d = (float) (offset + Math.sqrt(-2 * Math.log(Math.random())) * Math.cos(2 * Math.PI * Math.random()) * amplitude);
                    break;
            }
            if (add) {
                s1.set(i, ch1.mixIntensity(i, s1.get(i), s1.get(i) + d));
            } else {
                s1.set(i, ch1.mixIntensity(i, s1.get(i), d));
            }
        }
    }
}
