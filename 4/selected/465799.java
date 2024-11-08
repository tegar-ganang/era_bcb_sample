package dsp.voc;

import javax.swing.JDialog;
import dsp.Input;
import dsp.InputFactory;
import dsp.exception.UserCancelled;

/**
 * @author Sir Costy
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class VocInputFactory implements InputFactory {

    /**
	 * 
	 */
    public Input getInput(JDialog dialog) throws UserCancelled {
        VocInputDialog sic = new VocInputDialog(dialog);
        sic.setResizable(false);
        sic.setVisible(true);
        if (sic.isOK()) return new VocInput(sic.getFile(), sic.getSel_Channel(), sic.getChannels(), sic.getSampleRate(), sic.getSampleSize()); else throw new UserCancelled();
    }

    public String getName() {
        return "File Input (.voc)";
    }
}
