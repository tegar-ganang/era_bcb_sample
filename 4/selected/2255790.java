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


Class:			AOReverbAllPass
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	reverbation with flat frequency response.

History:
Date:			Description:									Autor:
29.07.00		erster Entwurf: bin nicht sicher, ob
            der Algorithmus stimmt...					oli4
04.08.00		neuer Stil        							oli4
01.11.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
18.04.01		new operation framework						oli4
21.04.01		add dry and wet parts, useage of toolkit	
																	oli4

***********************************************************/
public class AOReverbAllPass extends AOperation {

    public AOReverbAllPass(int delay, float gain, float dry, float wet, boolean backward) {
        super();
        this.delay = delay;
        this.gain = gain;
        this.dry = dry;
        this.wet = wet;
        this.backward = backward;
    }

    private int delay;

    private float gain;

    private float dry;

    private float wet;

    private boolean backward;

    public void operate(AChannelSelection ch1) {
        MMArray sample = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        float oldRms = AOToolkit.rmsAverage(sample, o1, l1);
        ch1.getChannel().markChange();
        AOFifo delayBuffer = new AOFifo(delay + 1);
        float output;
        MMArray tmp = new MMArray(l1, 0);
        try {
            if (backward) {
                for (int i = 0; i < l1; i++) {
                    tmp.set(l1 - 1 - i, sample.get(i + o1));
                }
            } else {
                tmp.copy(sample, o1, 0, l1);
            }
            for (int i = 0; i < l1; i++) {
                if (delay < delayBuffer.getActualSize()) {
                    output = delayBuffer.pickFromHead(delay - 1) + tmp.get(i) * (-gain);
                    delayBuffer.put(tmp.get(i) + output * gain);
                    tmp.set(i, output);
                } else {
                    output = tmp.get(i) * (-gain);
                    delayBuffer.put(tmp.get(i) + output * gain);
                    tmp.set(i, output);
                }
            }
            float newRms = AOToolkit.rmsAverage(tmp, 0, l1);
            AOToolkit.multiply(tmp, 0, l1, (float) (oldRms / newRms));
            if (backward) {
                for (int i = o1; i < o1 + l1; i++) {
                    float s = sample.get(i) * dry + tmp.get(l1 - 1 - i - o1) * wet;
                    sample.set(i, ch1.mixIntensity(i, sample.get(i), s));
                }
            } else {
                for (int i = o1; i < o1 + l1; i++) {
                    float s = sample.get(i) * dry + tmp.get(i - o1) * wet;
                    sample.set(i, ch1.mixIntensity(i, sample.get(i), s));
                }
            }
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
