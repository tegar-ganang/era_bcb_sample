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


Class:			AOFlatSpectrum
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	reduces a spectrumrange to zero with
					sharp edges.

History:
Date:			Description:									Autor:
07.06.02		first draft										oli4

***********************************************************/
public class AOStopSpectrumRange extends AOperation {

    /**
   *	constructor
   */
    public AOStopSpectrumRange() {
        super();
    }

    /**
	*	performs the segments generation
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        for (int i = 0; i < l1; i++) {
            s1.set(o1 + i, 0);
        }
    }
}
