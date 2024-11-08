package net.sf.xwav.soundrenderer;

/**
 * Component generates a constant output.
 * 
 * This is used to provide constant data to other components so they do not need
 * to implement special handling.
 */
public class ConstantComponent extends SoundComponent {

    private float value;

    public ConstantComponent(float value) {
        super();
        this.value = value;
    }

    @Override
    public void openHook() throws BadParameterException, MissingParameterException {
    }

    @Override
    public void generate(SoundBuffer soundBuffer) throws BadParameterException {
        super.generate(soundBuffer);
        for (int i = 0; i < soundBuffer.getNumberOfChannels(); i++) {
            for (int j = 0; j < soundBuffer.getDataLength(); j++) {
                soundBuffer.getChannelData(i)[j] = value;
            }
        }
    }

    @Override
    public void closeHook() {
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
