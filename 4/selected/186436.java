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


Class:			AOAutoCropSilence
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	cut all silent parts, which mplitude is lower 
					than slienceLimit for a longer time [samples]
					than tMinSilence. 

History:
Date:			Description:									Autor:
29.10.00		erster Entwurf									oli4
19.12.00		float audio samples							oli4
26.01.01		array-based again...							oli4

***********************************************************/
public class AOAutoCropSilence extends AOperation {

    public AOAutoCropSilence(float silenceLimit, int tMinSilence) {
        super();
        this.silenceLimit = silenceLimit;
        this.tMinSilence = tMinSilence;
    }

    private float silenceLimit;

    private int tMinSilence;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        int ns = o1;
        int i = o1;
        int ss = 0;
        final int NOISE = 1;
        final int BEGIN_OF_SILENCE = 2;
        final int SILENCE = 3;
        int state = NOISE;
        try {
            while (i < o1 + l1) {
                switch(state) {
                    case NOISE:
                        s1.set(ns++, s1.get(i));
                        if (Math.abs(s1.get(i)) <= silenceLimit) {
                            ss = 1;
                            state = BEGIN_OF_SILENCE;
                        }
                        break;
                    case BEGIN_OF_SILENCE:
                        s1.set(ns++, s1.get(i));
                        if (Math.abs(s1.get(i)) <= silenceLimit) {
                            if (ss++ >= tMinSilence) {
                                state = SILENCE;
                            }
                        } else {
                            state = NOISE;
                        }
                        break;
                    case SILENCE:
                        if (Math.abs(s1.get(i)) > silenceLimit) {
                            s1.set(ns++, s1.get(i));
                            state = NOISE;
                        }
                        break;
                }
                i++;
            }
            MMArray tmp = new MMArray(s1.getLength() + ns - 1 - l1, 0);
            tmp.copy(s1, 0, 0, o1);
            tmp.copy(s1, o1, o1, ns - o1);
            tmp.copy(s1, ns, ns, tmp.getLength() - ns);
            ch1.getChannel().setSamples(tmp);
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
