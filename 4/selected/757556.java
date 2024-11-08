package q.zik.basic.generator;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import q.lang.ParameterListener;
import q.zik.core.machine.definition.GeneratorDefinition;
import q.zik.core.machine.instance.Generator;
import q.zik.core.machine.instance.GeneratorTrack;
import q.zik.core.parameter.definition.MachineAttributeDefinition;
import q.zik.core.parameter.definition.MachineParameterDefinition;
import q.zik.core.parameter.definition.RangeDefinition.NoteDefinition;
import q.zik.core.parameter.instance.MachineParameter;
import q.zik.lang.Gain;
import q.zik.lang.Note;
import q.zik.lang.Sample;

/**
 * Machine definition.
 * 
 * @author ruyantq
 */
public class QTrackerDefinition extends GeneratorDefinition {

    public static class TrackDefinition extends GeneratorTrackDefinition<Generator<QTrackerDefinition>> {

        protected final NoteDefinition note = addNoteParameter("Note", "Note of the sample, relatively to the reference note. A higher note will play the sample at a higher speed.");

        protected final MachineParameterDefinition<Gain> volume = addGainParameter("Volume", "volume", -40, 0, 40, -3);

        protected final MachineParameterDefinition<Boolean> reverse = addBooleanParameter("Reverse", "Reverse the sample.", Boolean.FALSE);

        @Override
        public GeneratorTrack<?, ?> buildInstance(final Generator<QTrackerDefinition> _machine) {
            return new Track(this, _machine);
        }
    }

    public static class Track extends GeneratorTrack<TrackDefinition, Generator<QTrackerDefinition>> implements ParameterListener<Note> {

        private final MachineParameter<Note> noteParam;

        private final MachineParameter<Note> refParam;

        private final MachineParameter<Boolean> reverse;

        private final MachineParameter<Gain> volParam;

        private short[] buffer;

        private boolean stereo;

        private float count;

        private float ratio;

        public Track(final TrackDefinition _definition, final Generator<QTrackerDefinition> _machine) {
            super(_definition, _machine);
            noteParam = getParameter(_definition.note);
            refParam = getMachine().getParameter(getMachine().getDefinition().note);
            reverse = getParameter(_definition.reverse);
            volParam = getParameter(_definition.volume);
            noteParam.addListener(this);
        }

        @Override
        protected Sample generate(final int _time) {
            if (buffer != null) {
                if (count + (stereo ? 3 : 1) >= buffer.length) {
                    buffer = null;
                    return Sample.SILENCE;
                } else {
                    int intCount = Math.round(count);
                    float decimal = count - intCount;
                    if (reverse.getValue().booleanValue()) {
                        intCount = buffer.length - intCount - (stereo ? 4 : 2);
                        decimal = 1 - decimal;
                    }
                    if (stereo) {
                        final long left = Sample.interpolateLong(decimal, buffer[intCount], buffer[intCount + 2]);
                        final long right = Sample.interpolateLong(decimal, buffer[intCount + 1], buffer[intCount + 3]);
                        count += 2 * ratio;
                        return new Sample(left, right).mult(volParam.getValue().getRatio());
                    } else {
                        final long sample = Sample.interpolateLong(decimal, buffer[intCount], buffer[intCount + 1]);
                        count += 1 * ratio;
                        return new Sample(sample).mult(volParam.getValue().getRatio());
                    }
                }
            }
            return Sample.SILENCE;
        }

        @Override
        public void valueChanged(final ParameterChangeEvent<? extends Note> event) {
            final Note noteValue = noteParam.getValue();
            if (noteValue.intValue() >= 0) {
                try {
                    final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getMachine().getAttribute(getMachine().getDefinition().file).getValue());
                    final byte[] bytes = new byte[(int) audioInputStream.getFrameLength() * audioInputStream.getFormat().getFrameSize()];
                    audioInputStream.read(bytes);
                    final ShortBuffer shortbuffer = ByteBuffer.wrap(bytes).asShortBuffer();
                    buffer = new short[shortbuffer.capacity()];
                    for (int i = 0; i < buffer.length; i++) {
                        buffer[i] = shortbuffer.get();
                    }
                    stereo = audioInputStream.getFormat().getChannels() == 2;
                    ratio = refParam.getValue().getPeriodInSample() / noteValue.getPeriodInSample();
                    count = 0;
                } catch (final UnsupportedAudioFileException e) {
                    logError(e);
                } catch (final IOException e) {
                    logError(e);
                }
            } else {
                buffer = null;
            }
        }
    }

    protected final MachineAttributeDefinition<File> file = addFileAttribute("Sample File", "Sample for playback.", "Audio File", new String[] { "wav", "aif" });

    protected final NoteDefinition note = addNoteValueParameter("Reference", "Reference note value of the sample. Can be adjusted for fine tuning.", 0d, 128d, 512, 0d);

    /**
     * Default constructor.
     */
    public QTrackerDefinition() {
        super("QTracker", "Simple tracker. Playback samples.", new TrackDefinition());
    }

    @Override
    public Generator<QTrackerDefinition> buildInstance() {
        return new Generator<QTrackerDefinition>(this);
    }
}
