package ch.laoe.audio.load;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import ch.laoe.clip.ALayer;
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


Class:			ALoad
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	parentclass of all load-classes.

History:
Date:			Description:									Autor:
22.11.00		new stream-technique							oli4

***********************************************************/
public abstract class ALoad {

    /**
	* constructor
	*/
    protected ALoad() {
        buffer = new byte[bufferLength];
    }

    public abstract ALoad duplicate();

    protected void setAudioInputStream(AudioInputStream ais, int sampleLength) {
        audioInputStream = ais;
        this.sampleLength = sampleLength;
    }

    protected static File file;

    protected static AudioInputStream audioInputStream;

    protected int sampleLength;

    public void setFile(File f) {
        file = f;
    }

    protected byte buffer[];

    private static final int bufferLength = 16000;

    /**
	*	reads from input-stream, writes into layer, maximum length samples,
	*	from offset, returns the number of written samples.
	*/
    public abstract int read(ALayer l, int offset, int length) throws IOException;

    public void close() {
        try {
            audioInputStream.close();
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
        }
    }

    public abstract boolean supports(AudioFormat af);

    public int getSampleWidth() {
        return audioInputStream.getFormat().getSampleSizeInBits();
    }

    public float getSampleRate() {
        return audioInputStream.getFormat().getSampleRate();
    }

    public int getChannels() {
        return audioInputStream.getFormat().getChannels();
    }

    public int getSampleLength() {
        return sampleLength;
    }
}
