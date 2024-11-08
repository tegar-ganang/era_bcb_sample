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


Class:			AOCut
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	cut a part (defined by a selection) of a 
					channel. 

History:
Date:			Description:									Autor:
04.08.00		erster Entwurf									oli4
16.10.00		neuer Stil										oli4
19.12.00		float audio samples							oli4
24.01.01		array-based again...							oli4

***********************************************************/
public class AOCut extends AOperation {

    /**
   *  cuts the "selections"
   */
    public AOCut() {
        super();
    }

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(Math.max(2, s1.getLength() - l1), 0);
        ch1.getChannel().markChange();
        try {
            tmp.copy(s1, 0, 0, o1);
            tmp.copy(s1, o1 + l1, o1, tmp.getLength() - o1);
            AOToolkit.applyZeroCross(tmp, o1);
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
        ch1.getChannel().setSamples(tmp);
    }
}
