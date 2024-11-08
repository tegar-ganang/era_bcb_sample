package dsp.soundinput;

import javax.swing.JDialog;
import dsp.Input;
import dsp.InputFactory;
import dsp.dummy.DummyInput;
import dsp.exception.UserCancelled;

/**
 * @author canti
 *
 */
public class SoundInputFactory implements InputFactory {

    public Input getInput(JDialog dialog) throws UserCancelled {
        SoundInputDialog sic = new SoundInputDialog(dialog);
        sic.setVisible(true);
        if (sic.isOK()) return new SimpleAudioInput(sic.getSampleRate(), sic.getSampleSize(), sic.getChannels(), sic.isSigned(), sic.isBigEndian()); else throw new UserCancelled();
    }

    public String getName() {
        return "Sound Card Input";
    }
}
