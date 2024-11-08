package ch.laoe.operation;

import ch.laoe.clip.AChannel;
import ch.laoe.clip.AChannelSelection;

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


Class:			AOMix
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	mixes channel2 to channel1 (changes
					performed in channel1, or mixes channel2 with
					mask ch3 to channel1 (changes performed in 
					channel1. audible is considered.

History:
Date:			Description:									Autor:
26.07.00		erster Entwurf									oli4
04.08.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
24.01.01		array-based again...							oli4
10.03.01		add masking and audible						oli4
02.03.02		change mask-concept to channel-mask		oli4

***********************************************************/
public class AOMix extends AOperation {

    /**
	*	mixes channel2 into channel1, only channel1 is modified
	*/
    public AOMix() {
        super();
        this.volume1 = 1.f;
        this.volume2 = 1.f;
    }

    /**
	*	mixes channel2 into channel1, both channels with a separate
	*	volume. only channel1 is modified
	*/
    public AOMix(float volume1, float volume2) {
        super();
        this.volume1 = volume1;
        this.volume2 = volume2;
    }

    private float volume1, volume2;

    /**
	*	ch2 is added to ch1
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        AChannel s1 = ch1.getChannel();
        AChannel s2 = ch2.getChannel();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        int o2 = ch2.getOffset();
        ch1.getChannel().markChange();
        s1.getMask().prepareResults();
        s2.getMask().prepareResults();
        if (ch2.getChannel().isAudible()) {
            try {
                for (int i = 0; i < l1; i++) {
                    s1.setSample(o1 + i, s1.getMaskedSample(o1 + i) * volume1 + s2.getMaskedSample(o2 + i) * volume2);
                }
            } catch (ArrayIndexOutOfBoundsException oob) {
            }
        }
    }
}
