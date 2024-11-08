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


Class:			AODistort
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	distors samples

History:
Date:			Description:									Autor:
29.07.00		erster Entwurf          					oli4
03.08.00		neuer Stil        							oli4
24.08.00		free amplitude distortion					oli4
26.01.01		array-based again...							oli4
19.08.01		variable distortion						oli4

***********************************************************/
public class AODistort extends AOperation {

    /**
	*	constructor for free amplitude distortion
	*/
    public AODistort() {
        super();
    }

    /**
	*	constructor for clamping and noise gate
	*/
    public AODistort(float threshold, float clamping, int type) {
        super();
        this.threshold = threshold;
        this.clamping = clamping;
        this.type = type;
    }

    private float threshold;

    private float clamping;

    private int type;

    public static final int CLAMPING_TYPE = 1;

    public static final int NOISE_GATING_TYPE = 2;

    /**
	*	clamping or noise gate
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        LProgressViewer.getInstance().entrySubProgress(0.7);
        switch(type) {
            case CLAMPING_TYPE:
                for (int i = o1; i < (o1 + l1); i++) {
                    float s = s1.get(i);
                    if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / l1)) return;
                    if (s1.get(i) > threshold) s = clamping; else if (s1.get(i) < -threshold) s = -clamping;
                    s1.set(i, ch1.mixIntensity(i, s1.get(i), s));
                }
                break;
            case NOISE_GATING_TYPE:
                for (int i = o1; i < (o1 + l1); i++) {
                    float s = s1.get(i);
                    if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / l1)) return;
                    if ((s1.get(i) < threshold) && (s1.get(i) > 0)) s = clamping; else if ((s1.get(i) > -threshold) && (s1.get(i) < 0)) s = -clamping;
                    s1.set(i, ch1.mixIntensity(i, s1.get(i), s));
                }
                break;
        }
        LProgressViewer.getInstance().exitSubProgress();
    }

    /**
	*	variable distortion, ch1 will be distorted in function of
   	*	the transferfunction ch2 (x=input, unit of x=1, y=output, unit of y=1)
      * neutral curve: value[index] = index
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray f = ch2.getChannel().getSamples();
        ch1.getChannel().markChange();
        LProgressViewer.getInstance().entrySubProgress(0.7);
        float s;
        for (int i = 0; i < l1; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
            float in = s1.get(i + o1);
            if (in >= 0) {
                s = AOToolkit.interpolate3(f, in);
            } else {
                s = -AOToolkit.interpolate3(f, -in);
            }
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s));
        }
        LProgressViewer.getInstance().exitSubProgress();
    }
}
