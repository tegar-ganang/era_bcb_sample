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

Desctription:	FFT 

History:
Date:			Description:									Autor:
02.08.01		first draft										oli4

***********************************************************/
public class AOFft extends AOperation {

    public AOFft() {
    }

    /**
	*	performs FFT of the whole channel, all channels must have the same size!
   *  ch1 is t-domain and will change to magnitude in f-domain
   *  ch2 is unused, and will change to phase in f-domain
	*/
    public final void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray td = ch1.getChannel().getSamples();
        ch1.getChannel().markChange();
        ch2.getChannel().markChange();
        try {
            int l = 1 << (int) Math.ceil(Math.log(td.getLength()) / Math.log(2));
            MMArray re = new MMArray(l, 0);
            MMArray im = new MMArray(l, 0);
            for (int i = 0; i < l; i++) {
                if (i < td.getLength()) {
                    re.set(i, td.get(i));
                    im.set(i, 0);
                } else {
                    re.set(i, 0);
                    im.set(i, 0);
                }
            }
            AOToolkit.complexFft(re, im);
            MMArray mag = new MMArray(l / 2, 0);
            MMArray phas = new MMArray(l / 2, 0);
            for (int i = 0; i < mag.getLength(); i++) {
                mag.set(i, AOToolkit.cartesianToMagnitude(re.get(i), im.get(i)));
                phas.set(i, AOToolkit.cartesianToPhase(re.get(i), im.get(i)));
            }
            ch1.getChannel().setSamples(mag);
            ch2.getChannel().setSamples(phas);
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }
}
