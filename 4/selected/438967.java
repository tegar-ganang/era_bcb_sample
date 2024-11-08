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


Class:			AOVibrato
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	vibrato effect, a kind of sinus-resampling.

History:
Date:			Description:									Autor:
25.12.2001	first draft 									oli4

***********************************************************/
public class AOVibrato extends AOperation {

    public AOVibrato(int modulationDelay, int modulationPeriod, int modulationShape) {
        super();
        this.modulationDelay = modulationDelay;
        this.modulationPeriod = modulationPeriod;
        this.modulationShape = modulationShape;
    }

    private float modulationDelay, modulationPeriod;

    private int modulationShape;

    public static final int SINUS = 1;

    public static final int TRIANGLE = 2;

    public static final int SAW = 3;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1, 0);
        ch1.getChannel().markChange();
        LProgressViewer.getInstance().entrySubProgress(0.7);
        for (int i = 0; i < l1; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
            float iMod = 0;
            switch(modulationShape) {
                case SINUS:
                    iMod += (float) Math.sin((float) (i % (int) modulationPeriod) / modulationPeriod * 2 * Math.PI) * modulationDelay;
                    break;
                case TRIANGLE:
                    iMod += (Math.abs(((float) (i % (int) modulationPeriod) / modulationPeriod * 4) - 2) - 1) * modulationDelay;
                    break;
                case SAW:
                    iMod += (((float) (i % (int) modulationPeriod) / modulationPeriod * 2) - 1) * modulationDelay;
                    break;
            }
            double m = o1 + i + iMod;
            if (m < 0) m = 0; else if (m >= s1.getLength()) m = s1.getLength() - 1;
            tmp.set(i, AOToolkit.interpolate0(s1, (float) m));
        }
        LProgressViewer.getInstance().exitSubProgress();
        for (int i = 0; i < tmp.getLength(); i++) {
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), tmp.get(i)));
        }
    }
}
