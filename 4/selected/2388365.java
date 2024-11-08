package ch.laoe.audio.play;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import ch.laoe.audio.Audio;
import ch.laoe.audio.AudioException;
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


Class:			APlay
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	parentclass of all playback-classes. If you
					find some strange programming style, excuse
					our cat "moustique". He also likes computers.

History:
Date:			Description:									Autor:
02.12.00		new stream-technique							oli4

***********************************************************/
public abstract class APlay extends InputStream {

    public abstract APlay duplicate();

    public abstract boolean supports(AudioFormat af);

    protected AClip clip;

    protected int channels;

    public void setClip(AClip c) {
        clip = c;
        channels = clip.getMaxNumberOfChannels();
    }

    /**
	 * returns the indexed channel sample, respecting layer superposing, layer types, channel types.
    * masking and audible is considered. 
	 * @param channelIndex
	 * @param sampleIndex
	 * @return
	 */
    public final float getSample(int channelIndex, int sampleIndex) {
        float s = 0;
        for (int i = 0; i < clip.getNumberOfLayers(); i++) {
            ALayer l = clip.getLayer(i);
            switch(l.getType()) {
                case ALayer.AUDIO_LAYER:
                    s += l.getChannel(channelIndex).getMaskedSample(sampleIndex);
                    break;
                case ALayer.SOLO_AUDIO_LAYER:
                    s = l.getChannel(channelIndex).getMaskedSample(sampleIndex);
                    return s;
            }
        }
        return s;
    }

    protected byte data[];

    protected int dataBufferLength;

    private AudioFormat audioFormat;

    public void setAudioFormat(AudioFormat af) {
        audioFormat = af;
    }

    private AudioInputStream audioInputStream;

    private SourceDataLine line;

    public void start() throws AudioException {
        try {
            audioInputStream = new AudioInputStream(this, audioFormat, Long.MAX_VALUE);
            line = (SourceDataLine) AudioSystem.getSourceDataLine(audioFormat);
            line.open();
            line.start();
            createControls();
        } catch (LineUnavailableException lue) {
            throw new AudioException("missingAudioResource");
        } catch (IllegalArgumentException lue) {
            throw new AudioException("unsupportedAudioFormat");
        }
    }

    public void flush() {
        line.flush();
    }

    public void stop() {
        try {
            audioInputStream.close();
        } catch (IOException ioe) {
        } finally {
            line.stop();
        }
    }

    private void printControls() {
        Control c[] = line.getControls();
        System.out.println("controls: ");
        for (int i = 0; i < c.length; i++) {
            System.out.println(c[i]);
        }
    }

    private FloatControl sampleRateControl;

    private void createControls() {
        if (line.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
            sampleRateControl = (FloatControl) line.getControl(FloatControl.Type.SAMPLE_RATE);
        } else {
            sampleRateControl = null;
        }
    }

    public void changeSampleRate(float sr) {
        if (sampleRateControl != null) {
            sampleRateControl.setValue(sr);
        }
    }

    /**
	*	reads from layer, writes to the line.
	*/
    public int write(int length) throws IOException {
        int factor = audioFormat.getSampleSizeInBits() / 8 * audioFormat.getChannels();
        int n = audioInputStream.read(data, 0, Math.min(length * factor, data.length));
        if (n < 0) return -1;
        line.write(data, 0, n);
        return n / factor;
    }

    public void goTo(int sample) {
    }

    /**
	 *	returns the actual sample position that is really playing now!
	 */
    public int getActualPosition() {
        return (int) (line.getMicrosecondPosition() * audioFormat.getSampleRate() / 1e6);
    }
}
