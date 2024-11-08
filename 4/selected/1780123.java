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


Class:			AOInsert
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	general insertion, null-samples, another channel

History:
Date:			Description:									Autor:
04.08.00		erster Entwurf									oli4
16.10.00		neuer Stil										oli4
19.12.00		float audio samples							oli4
24.01.01		array-based again...							oli4

***********************************************************/
public class AOInsert extends AOperation {

    public AOInsert() {
        super();
        insertedLength = 0;
    }

    /**
   *  inserts 0's ("length" times) 
   */
    public AOInsert(int length) {
        super();
        insertedLength = length;
    }

    private int insertedLength;

    /**
	*	performs a 0-samples insertion
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        MMArray tmp = new MMArray(s1.getLength() + insertedLength, 0);
        ch1.getChannel().markChange();
        tmp.copy(s1, 0, 0, o1);
        for (int i = o1; i < o1 + insertedLength; i++) {
            tmp.set(i, 0);
        }
        tmp.copy(s1, o1, o1 + insertedLength, tmp.getLength() - o1 - insertedLength);
        AOToolkit.applyZeroCross(tmp, o1);
        AOToolkit.applyZeroCross(tmp, o1 + insertedLength);
        ch1.getChannel().setSamples(tmp);
    }

    /**
	*	performs an insertion of channel2 to channel1
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        MMArray s2 = ch2.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int o2 = ch2.getOffset();
        int l2 = ch2.getLength();
        ch1.getChannel().markChange();
        MMArray tmp = new MMArray(s1.getLength() + l2, 0);
        tmp.copy(s1, 0, 0, o1);
        tmp.copy(s2, o2, o1, l2);
        tmp.copy(s1, o1, o1 + l2, tmp.getLength() - o1 - l2);
        AOToolkit.applyZeroCross(tmp, o1);
        AOToolkit.applyZeroCross(tmp, o1 + l2);
        ch1.getChannel().setSamples(tmp);
    }
}
