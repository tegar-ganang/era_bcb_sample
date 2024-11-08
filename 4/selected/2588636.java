package ch.laoe.audio.load;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.FormatConversionProvider;
import ch.laoe.audio.AudioException;
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


Class:			ALoadFactory
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	factory to build loaders.

History:
Date:			Description:									Autor:
03.07.01		first draft										oli4
18.07.02		encoded file support							oli4

***********************************************************/
public class ALoadFactory {

    private static ArrayList<ALoad> classList;

    static {
        preloadClasses();
    }

    private static void preloadClasses() {
        classList = new ArrayList<ALoad>();
        classList.add(new ALoadPcmUnsigned8Bit());
        classList.add(new ALoadPcmSigned8Bit());
        classList.add(new ALoadPcmUnsigned16BitLittleEndian());
        classList.add(new ALoadPcmUnsigned16BitBigEndian());
        classList.add(new ALoadPcmSigned16BitLittleEndian());
        classList.add(new ALoadPcmSigned16BitBigEndian());
        classList.add(new ALoadUlaw8Bit());
    }

    public static final ALoad create(File f) throws AudioException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            if (f.getName().toLowerCase().endsWith(".mp3")) {
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, ais.getFormat().getSampleRate(), 16, ais.getFormat().getChannels(), ais.getFormat().getChannels() * 2, ais.getFormat().getSampleRate(), false);
                ais = AudioSystem.getAudioInputStream(decodedFormat, ais);
            } else if (f.getName().toLowerCase().endsWith(".ogg")) {
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, ais.getFormat().getSampleRate(), 16, ais.getFormat().getChannels(), ais.getFormat().getChannels() * 2, ais.getFormat().getSampleRate(), false);
                try {
                    ais = AudioSystem.getAudioInputStream(decodedFormat, ais);
                } catch (IllegalArgumentException iae) {
                    try {
                        ais = ((FormatConversionProvider) (Class.forName("javazoom.spi.vorbis.sampled.convert.VorbisFormatConversionProvider").newInstance())).getAudioInputStream(decodedFormat, ais);
                    } catch (Exception cnfe) {
                        cnfe.printStackTrace();
                    }
                }
            }
            AudioFormat af = ais.getFormat();
            Debug.println(3, "audioformat = " + af.toString());
            int sl = (int) (ais.getFrameLength() * af.getFrameSize() / af.getChannels() / (af.getSampleSizeInBits() >> 3));
            if (sl < 0) {
                sl = 1;
            }
            Debug.println(3, "sample length = " + sl);
            for (int i = 0; i < classList.size(); i++) {
                ALoad l = classList.get(i);
                if (l.supports(af)) {
                    l = l.duplicate();
                    l.setAudioInputStream(ais, sl);
                    l.setFile(f);
                    return l;
                }
            }
            Debug.println(3, "unsupported audioformat = " + af.toString());
            throw new AudioException("unsupportedAudioFormat");
        } catch (UnsupportedAudioFileException uafe) {
            Debug.printStackTrace(5, uafe);
            throw new AudioException("unsupportedAudioFormat");
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
            throw new AudioException("unsupportedAudioFormat");
        }
    }
}
