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


Class:			AOConvolution
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	convolution operation

History:
Date:			Description:									Autor:
24.06.01		first draft										oli4
17.09.01		layer-kernel added							oli4

***********************************************************/
public class AOConvolution extends AOperation {

    public AOConvolution(MMArray kernel) {
        this.kernel = kernel;
    }

    public AOConvolution() {
    }

    private MMArray kernel;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1, 0);
        ch1.getChannel().markChange();
        LProgressViewer.getInstance().entrySubProgress(0.7);
        for (int i = 0; i < l1; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
            tmp.set(i, AOToolkit.convolve(s1, i + o1, kernel, kernel.getLength()));
        }
        LProgressViewer.getInstance().exitSubProgress();
        for (int i = 0; i < l1; i++) {
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), tmp.get(i)));
        }
        AOToolkit.applyZeroCross(s1, o1);
        AOToolkit.applyZeroCross(s1, o1 + l1);
    }

    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        kernel = ch2.getChannel().getSamples();
        operate(ch1);
    }
}
