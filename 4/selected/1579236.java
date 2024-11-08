package fr.x9c.cadmium.primitives.graph;

import java.awt.Toolkit;
import java.io.IOException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.util.Beep;

/**
 * This class provides the implementation of sound-related primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Sound {

    /** Size of audio buffer. */
    private static final int BUFFER_SIZE = 1024;

    /** Sample rate of audio stream. */
    private static final float SAMPLE_RATE = 44100.0f;

    /**
     * No instance of this class.
     */
    private Sound() {
    }

    /**
     * Emits a "beep".
     * @param ctxt context
     * @param freq beep frequency - ignored
     * @param duration beep duration - ignored
     * @return <i>unit</i>
     * @throws Fail.Exception if <i>Graphics</i> module has not been initialized
     */
    @Primitive
    public static Value caml_gr_sound(final CodeRunner ctxt, final Value freq, final Value duration) throws Fail.Exception {
        GraphSlot.checkGraph(ctxt);
        if (ctxt.getContext().getParameters().isJavaxSoundUsed()) {
            try {
                final Beep bip = new Beep(Sound.SAMPLE_RATE, freq.asLong(), duration.asLong());
                final DataLine.Info info = new DataLine.Info(SourceDataLine.class, bip.getFormat());
                final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(bip.getFormat());
                line.start();
                final byte[] data = new byte[Sound.BUFFER_SIZE];
                while (bip.available() > 0) {
                    final int read = bip.read(data);
                    int written = line.write(data, 0, read);
                    while (written < read) {
                        written += line.write(data, written, read - written);
                    }
                }
                line.drain();
                line.close();
            } catch (final LineUnavailableException lue) {
                Toolkit.getDefaultToolkit().beep();
            } catch (final IOException ioe) {
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
        return Value.UNIT;
    }
}
