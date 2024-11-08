package ch.laoe.operation;

import com.apple.crypto.provider.Debug;
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


Class:			AOCompressExpand
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	compressor / expander

History:
Date:			Description:									Autor:
22.04.01		first draft										oli4
29.07.01		first really debugged version!!!    	oli4
13.09.01		add variable transferfunction	  	oli4

***********************************************************/
public class AOCompressExpand extends AOperation {

    /**
    * fx and fy describes a transfer-function, they must begin with
    * 0, and each position (x,y) is the entry- and exit value, e.g.
    * (30, 60) where this point doubles the sample-value.
    */
    public AOCompressExpand(int sampleRate, int tAttack, int tRelease, MMArray fx, MMArray fy) {
        super();
        this.tAttack = tAttack;
        this.tRelease = tRelease;
        this.fx = fx;
        this.fy = fy;
    }

    private int tAttack;

    private int tRelease;

    private MMArray fx;

    private MMArray fy;

    private float meanAmplitude;

    private void initAmplitude() {
        meanAmplitude = 1;
    }

    private void updateAmplitude(float sample) {
        sample = Math.abs(sample);
        if (sample > meanAmplitude) {
            meanAmplitude = AOToolkit.movingRmsAverage(meanAmplitude, sample, tAttack);
        } else {
            meanAmplitude = AOToolkit.movingRmsAverage(meanAmplitude, sample, tRelease);
        }
    }

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        initAmplitude();
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = o1; i < o1 + l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / l1)) return;
                updateAmplitude(s1.get(i));
                float f = AOToolkit.interpolate1(fx, fy, meanAmplitude);
                s1.set(i, ch1.mixIntensity(i, s1.get(i), s1.get(i) * f));
            }
            LProgressViewer.getInstance().exitSubProgress();
            AOToolkit.applyZeroCross(s1, o1);
            AOToolkit.applyZeroCross(s1, o1 + l1);
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }

    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray t = ch2.getChannel().getSamples();
        ch1.getChannel().markChange();
        initAmplitude();
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = o1; i < o1 + l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / l1)) return;
                updateAmplitude(s1.get(i));
                float f = t.get(ch2.getChannel().limitIndex((int) meanAmplitude)) / meanAmplitude;
                s1.set(i, ch1.mixIntensity(i, s1.get(i), s1.get(i) * f));
            }
            LProgressViewer.getInstance().exitSubProgress();
            AOToolkit.applyZeroCross(s1, o1);
            AOToolkit.applyZeroCross(s1, o1 + l1);
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
