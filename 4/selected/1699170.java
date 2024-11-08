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


Class:			AOMultiNotch
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	IIR multiple notch filter 

History:
Date:			Description:									Autor:
09.06.01		first draft										oli4

***********************************************************/
public class AOMultiNotch extends AOperation {

    public AOMultiNotch(float[] freq, float q) {
        super();
        this.freq = freq;
        this.q = q;
    }

    private float freq[];

    private float q;

    /**
	*	performs a constant amplification
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1, 0);
        ch1.getChannel().markChange();
        LProgressViewer.getInstance().entrySubProgress(0.7);
        tmp.copy(s1, o1, 0, l1);
        for (int i = 0; i < freq.length; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / freq.length)) return;
            if (freq[i] != 0) {
                AOToolkit.setIirNotch(tmp, 0, l1, freq[i], q, 1);
            }
        }
        for (int i = 0; i < l1; i++) {
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), tmp.get(i)));
        }
        LProgressViewer.getInstance().exitSubProgress();
    }
}
