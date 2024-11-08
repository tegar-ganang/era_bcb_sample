package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
import ch.laoe.ui.Debug;

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


Class:			AOFft
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	IFFT

History:
Date:			Description:									Autor:
02.08.01		first draft										oli4

***********************************************************/
public class AOIfft extends AOperation {

    public AOIfft() {
    }

    /**
	*	performs FFT of the whole channel, all channels must have the same size!
   *  ch1 is f-domain magnitude and will change to t-domain
   *  ch2 is f-domain phase and will change to unused
	*/
    public final void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray mag = ch1.getChannel().getSamples();
        MMArray phas = ch2.getChannel().getSamples();
        ch1.getChannel().markChange();
        int l = 1 << (int) Math.ceil(Math.log(mag.getLength()) / Math.log(2));
        MMArray re = new MMArray(2 * l, 0);
        MMArray im = new MMArray(2 * l, 0);
        try {
            for (int i = 0; i < l; i++) {
                float m = 0;
                float p = 0;
                if (i < mag.getLength()) {
                    m = mag.get(i);
                }
                if (i < phas.getLength()) {
                    p = phas.get(i);
                }
                re.set(i, AOToolkit.polarToX(m, p));
                im.set(i, AOToolkit.polarToY(m, p));
            }
            AOToolkit.complexIfft(re, im);
            ch1.getChannel().setSamples(re);
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }
}
