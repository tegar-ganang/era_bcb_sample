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


Class:			AOAutoVolume
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	auto volume adapts the volume to a equal
					value.

History:
Date:			Description:									Autor:
08.05.01		first draft										oli4
28.01.02		add backward-mode and calibrate mean
				before starting								oli4

***********************************************************/
public class AOAutoVolume extends AOperation {

    public AOAutoVolume(int sampleRate, float dry, float wet, int tAttack, int tRelease, boolean backward) {
        super();
        this.dry = dry;
        this.wet = wet;
        this.tAttack = tAttack;
        this.tRelease = tRelease;
        this.backward = backward;
    }

    private float dry, wet;

    private int tAttack;

    private int tRelease;

    private boolean backward;

    private float meanAmplitude;

    private void updateAmplitude(float sample) {
        sample = Math.abs(sample);
        if (sample > meanAmplitude) {
            meanAmplitude = AOToolkit.movingRmsAverage(meanAmplitude, sample, tAttack);
        } else {
            meanAmplitude = AOToolkit.movingRmsAverage(meanAmplitude, sample, tRelease);
        }
    }

    /**
	*	performs a variable amplification, where the variable is
	*	channel2
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        float oldRms = AOToolkit.rmsAverage(s1, o1, l1);
        ch1.getChannel().markChange();
        meanAmplitude = 1;
        if (backward) {
            for (int i = l1 - 1; i >= Math.max(0, l1 - tAttack - tRelease); i--) {
                updateAmplitude(s1.get(i + o1));
            }
        } else {
            for (int i = 0; i < Math.min(l1, tAttack + tRelease); i++) {
                updateAmplitude(s1.get(i + o1));
            }
        }
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            if (backward) {
                for (int i = l1 - 1; i >= 0; i--) {
                    if (LProgressViewer.getInstance().setProgress((l1 - i) * 1.0 / l1)) return;
                    updateAmplitude(s1.get(i + o1));
                    s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), (1000 / meanAmplitude * s1.get(i + o1)) * wet + s1.get(i + o1) * dry));
                }
            } else {
                for (int i = 0; i < l1; i++) {
                    if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                    updateAmplitude(s1.get(i + o1));
                    s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), (1000 / meanAmplitude * s1.get(i + o1)) * wet + s1.get(i + o1) * dry));
                }
            }
            float newRms = AOToolkit.rmsAverage(s1, o1, l1);
            AOToolkit.multiply(s1, o1, l1, (float) (oldRms / newRms));
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
