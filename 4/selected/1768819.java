package ch.laoe.audio.save;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AClip;
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


Class:			ASave
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	parentclass of all save-classes. If you
					find some strange comments, excuse
					our cat "moustique". He also likes computers.

History:
Date:			Description:									Autor:
27.11.00		new stream-technique							oli4

***********************************************************/
public abstract class ASave extends InputStream {

    /**
	* constructor
	*/
    protected ASave() {
    }

    public abstract ASave duplicate();

    public abstract boolean supports(AudioFormat af);

    protected AClip clip;

    protected int channels;

    protected static File file;

    public void setClip(AClip c) {
        clip = c;
        channels = clip.getMaxNumberOfChannels();
    }

    public void setFile(File f) {
        file = f;
    }

    /**
    * returns the indexed channel sample, respecting layer superposing, layer types, channel types.
    * masking and audible is considered. 
    * @param channelIndex
    * @param sampleIndex
    * @return
    */
    protected final float getSample(int channelIndex, int sampleIndex) {
        float s = 0;
        for (int i = 0; i < clip.getNumberOfLayers(); i++) {
            ALayer l = clip.getLayer(i);
            if (l.getType() == ALayer.AUDIO_LAYER) {
                AChannel ch = l.getChannel(channelIndex);
                s += ch.getMaskedSample(sampleIndex);
            }
        }
        return s;
    }

    protected static AudioFileFormat audioFileFormat;

    public void setAudioFileFormat(AudioFileFormat aff) {
        audioFileFormat = aff;
    }

    /**
	*	reads from layer, writes to the file.
	*/
    public int write() throws IOException {
        AudioInputStream ais = new AudioInputStream(this, audioFileFormat.getFormat(), clip.getMaxSampleLength());
        if (AudioSystem.isFileTypeSupported(audioFileFormat.getType(), ais)) {
            AudioSystem.write(ais, audioFileFormat.getType(), file);
        }
        return 0;
    }
}
