package ch.laoe.audio.load;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import ch.laoe.clip.ALayer;

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


Class:			ALoadPcmSigned16BitLittleEndian
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	PCM signed 16bit little endian file loader 

History:
Date:			Description:									Autor:
22.11.00		new stream-technique							oli4

***********************************************************/
public class ALoadPcmSigned16BitLittleEndian extends ALoad {

    /**
	* constructor
	*/
    public ALoadPcmSigned16BitLittleEndian() {
        super();
    }

    public ALoad duplicate() {
        return new ALoadPcmSigned16BitLittleEndian();
    }

    public boolean supports(AudioFormat af) {
        if ((af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) && (af.getSampleSizeInBits() == 16) && !af.isBigEndian()) {
            return true;
        } else {
            return false;
        }
    }

    /**
	*	reads from input-stream, writes into layer, maximum length samples,
	*	from offset, returns the number of written samples.
	*/
    public int read(ALayer l, int offset, int length) throws IOException {
        int channels = audioInputStream.getFormat().getChannels();
        int readLength = audioInputStream.read(buffer, 0, length * channels * 2);
        for (int i = 0; i < channels; i++) {
            for (int j = 0; j < readLength / channels / 2; j++) {
                int index = j * channels * 2 + i * 2;
                int data = ((int) buffer[index] & 0x000000FF) | (((int) buffer[index + 1]) << 8);
                l.getChannel(i).setSample(offset + j, data);
            }
        }
        if (readLength >= 0) return readLength / channels / 2; else return readLength;
    }
}
