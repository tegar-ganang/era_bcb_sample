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


Class:			AOFlipBytes
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	flips the bytes

History:
Date:			Description:									Autor:
17.11.00		first draft										oli4
19.12.00		float audio samples							oli4
26.01.01		array-based again...							oli4

***********************************************************/
public class AOFlipBytes extends AOperation {

    /**
	*	maps the bytes at a new byte-position (0..3 or EMPTY)
	*/
    public AOFlipBytes(int newByte0Location, int newByte1Location, int newByte2Location, int newByte3Location) {
        super();
        this.newByte0Location = newByte0Location;
        this.newByte1Location = newByte1Location;
        this.newByte2Location = newByte2Location;
        this.newByte3Location = newByte3Location;
    }

    private int newByte0Location;

    private int newByte1Location;

    private int newByte2Location;

    private int newByte3Location;

    public static final int EMPTY = 4;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        for (int i = o1; i < (o1 + l1); i++) {
            try {
                int newSample = 0;
                if (newByte0Location < EMPTY) newSample |= ((int) s1.get(i) & 0x000000FF) << (newByte0Location * 8);
                if (newByte1Location < EMPTY) newSample |= (((int) s1.get(i) >> 8) & 0x000000FF) << (newByte1Location * 8);
                if (newByte2Location < EMPTY) newSample |= (((int) s1.get(i) >> 16) & 0x000000FF) << (newByte2Location * 8);
                if (newByte3Location < EMPTY) newSample |= (((int) s1.get(i) >> 24) & 0x000000FF) << (newByte3Location * 8);
                s1.set(i, ch1.mixIntensity(i, s1.get(i), (float) newSample));
            } catch (ArrayIndexOutOfBoundsException oob) {
            }
        }
    }
}
