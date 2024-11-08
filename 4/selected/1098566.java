package ch.laoe.operation;

import java.util.ArrayList;
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


Class:			AODelayEcho
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	delay and echo without feedback and finite
               number of echoes.

History:
Date:			Description:									Autor:
29.07.00		erster Entwurf									oli4
03.08.00		neuer Stil        							oli4
02.11.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
07.05.01		add dry and wet								oli4

***********************************************************/
public class AODelayEcho extends AOperation {

    /**
	 * @param delayShape unit = delay
	 * @param gainShape unit = gain
	 * @param delay
	 * @param gain
	 * @param dry
	 * @param wet
	 * @param continueWet continue applying wet part outside selection (selection defines dry source only)
	 */
    public AODelayEcho(MMArray delayShape, MMArray gainShape, float delay, float gain, float dry, float wet, boolean continueWet) {
        super();
        this.delayShape = delayShape;
        this.gainShape = gainShape;
        this.delay = delay;
        this.gain = gain;
        this.dry = dry;
        this.wet = wet;
        this.continueWet = continueWet;
    }

    public AODelayEcho(float dry, float wet, boolean continueWet) {
        super();
        this.dry = dry;
        this.wet = wet;
        this.continueWet = continueWet;
    }

    private MMArray delayShape;

    private MMArray gainShape;

    private float delay, gain;

    private float dry, wet;

    private boolean continueWet;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1, 0);
        ch1.getChannel().markChange();
        LProgressViewer.getInstance().entrySubProgress(0.7);
        for (int i = 0; i < l1; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
            for (int j = 0; j < delayShape.getLength(); j++) {
                int d = (int) (delay * delayShape.get(j));
                float g = gain * gainShape.get(j);
                if (continueWet || i + d < l1) {
                    tmp.set(i + d, tmp.get(i + d) + s1.get(i + o1) * g);
                }
            }
        }
        LProgressViewer.getInstance().exitSubProgress();
        for (int i = 0; i < tmp.getLength(); i++) {
            float s = dry * s1.get(i + o1) + wet * tmp.get(i);
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s));
        }
        AOToolkit.applyZeroCross(s1, o1);
        AOToolkit.applyZeroCross(s1, o1 + tmp.getLength());
    }

    /**
	 * multi-echo using a user-defined room
	 *	@param ch1 samples
	 *	@param ch2 room
	 */
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray sample = ch1.getChannel().getSamples();
        MMArray room = ch2.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1, 0);
        ch1.getChannel().markChange();
        class Echo {

            public Echo(int d, float g) {
                delay = d;
                gain = g;
            }

            public int delay;

            public float gain;
        }
        ArrayList<Echo> echoList = new ArrayList<Echo>(333);
        for (int i = 0; i < room.getLength(); i++) {
            if (room.get(i) != 0) {
                echoList.add(new Echo(i, room.get(i)));
            }
        }
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = 0; i < l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                for (int j = 0; j < echoList.size(); j++) {
                    int d = echoList.get(j).delay;
                    float g = echoList.get(j).gain;
                    if (continueWet || i + d < l1) {
                        tmp.set(i + d, tmp.get(i + d) + sample.get(i + o1) * g);
                    }
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
            for (int i = 0; i < tmp.getLength(); i++) {
                float s = dry * sample.get(i + o1) + wet * tmp.get(i);
                sample.set(i + o1, ch1.mixIntensity(i + o1, sample.get(i + o1), s));
            }
            AOToolkit.applyZeroCross(sample, o1);
            AOToolkit.applyZeroCross(sample, o1 + tmp.getLength());
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
