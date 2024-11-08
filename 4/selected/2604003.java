package net.sf.xwav.soundrenderer;

/**
 * Component generates output based on sound buffer contents.
 * 
 * This is intended mainly as a simple way of providing test inputs to components
 * for unit testing
 */
public class SoundBufferComponent extends SoundComponent {

    private SoundBuffer source;

    private int[] position;

    public SoundBufferComponent(SoundBuffer source) {
        super();
        this.source = source;
    }

    @Override
    public void openHook() throws BadParameterException, MissingParameterException {
        if (soundDescriptor.getNumberOfChannels() != source.getNumberOfChannels()) throw new BadParameterException("SoundDescriptor number of channels does not match the source SoundBuffer");
        if (soundDescriptor.getNumberOfSamples() > source.getDataLength()) throw new BadParameterException("SoundDescriptor number of samples exceeds the length of the source SoundBuffer");
        position = new int[soundDescriptor.getNumberOfChannels()];
        for (int i = 0; i < soundDescriptor.getNumberOfChannels(); i++) position[i] = 0;
    }

    @Override
    public void generate(SoundBuffer soundBuffer) throws BadParameterException {
        super.generate(soundBuffer);
        for (int channel = 0; channel < soundBuffer.getNumberOfChannels(); channel++) {
            for (int sample = 0; sample < soundBuffer.getDataLength(); sample++) {
                soundBuffer.getChannelData(channel)[sample] = source.getData()[channel][position[channel]];
                position[channel]++;
            }
        }
    }

    @Override
    public void closeHook() {
    }
}
