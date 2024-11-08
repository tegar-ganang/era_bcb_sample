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


Class:			AOInsertMix
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	mixes the original selection with
the new selection. 

History:
Date:			Description:									Autor:
01-11-2010	first draft										oli4

***********************************************************/
public class AOInsertMix extends AOperation {

    public AOInsertMix() {
        super();
    }

    /**
	*	performs an insertion of channel2 to channel1 respecting intensity of channel1
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        MMArray s2 = ch2.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        int o2 = ch2.getOffset();
        int l2 = ch2.getLength();
        int l = Math.min(l1, l2);
        ch1.getChannel().markChange();
        for (int i = 0; i < l; i++) {
            s1.set(i + o1, ch1.mixIntensity(i + o1, s1.get(i + o1), s2.get(i + o2)));
        }
        AOToolkit.applyZeroCross(s1, o1);
        AOToolkit.applyZeroCross(s1, o1 + l);
    }
}