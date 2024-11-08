package net.url404.umodular.components;

import java.io.IOException;
import net.url404.umodular.*;

/**
 * Eight track mixer.
 * <p>
 * INPUT 0-8: signal in (SignalComponentConnector)
 * <p>
 * OUTPUT 0: mixed signal (SignalComponentConnector)
 *
 * @author      makela@url404.net
 */
public class MixerComponent extends SoundComponent {

    private SignalComponentConnector outSignal;

    private double mainLevel = 1.0;

    public static final int MIXER_CHANNELS = 8;

    private double mixLevels[] = new double[MixerComponent.MIXER_CHANNELS];

    /** Constructor */
    public MixerComponent() {
        this.outSignal = new SignalComponentConnector(this);
        this.attachOutput(0, this.outSignal);
        this.vendorName = "U4MIX-1";
        for (int i = 0; i < MixerComponent.MIXER_CHANNELS; i++) {
            mixLevels[i] = 1.0;
        }
        this.numInputConnectors = MixerComponent.MIXER_CHANNELS;
    }

    /**
   *
   * Read list of next values. See SoundComponent read() method.
   *
   * @return              Next values
   */
    public double[] read(int portNum) {
        return outputBuf;
    }

    /**
   *
   * Advance implementation. See SoundComponent advance() method.
   * <p>
   * Amplifier multiplies input 0 (signal) with input 1 (level = gain)
   * and returns the value. Nothing else, honestly.
   *
   * @return              Next values
   * @throws IOException  Bad wiring
   */
    public void advance() throws IOException {
        double ch[][] = new double[MixerComponent.MIXER_CHANNELS][0];
        for (int c = 0; c < MixerComponent.MIXER_CHANNELS; c++) {
            if (getInput(c) != null) {
                ch[c] = getInput(c).read();
            }
        }
        for (int i = 0; i < UModularProperties.READ_BUFFER_SIZE; i++) {
            double s = 0.0;
            for (int c = 0; c < MixerComponent.MIXER_CHANNELS; c++) {
                if (getInput(c) != null) {
                    s += ch[c][i] * mixLevels[c];
                }
            }
            s = (s / MixerComponent.MIXER_CHANNELS);
            outputBuf[i] = s * mainLevel;
        }
    }

    /**
   * Set internal amplification level.
   *
   * @param lvl   New level
   */
    public void setLevel(double lvl) {
        mainLevel = lvl;
    }

    /**
   * Get internal amplification level.
   *
   * @return      Level
   */
    public double getLevel() {
        return mainLevel;
    }

    /**
   * Set channel mix level.
   *
   * @param ch    Channel
   * @param lvl   New level
   */
    public void setChannelLevel(int ch, double lvl) {
        if (ch >= 0 && ch < MixerComponent.MIXER_CHANNELS) {
            mixLevels[ch] = lvl;
        }
    }

    /**
   * Get internal amplification level.
   *
   * @param ch    Channel
   * @return      Level
   */
    public double getChannelLevel(int ch) {
        if (ch >= 0 && ch < MixerComponent.MIXER_CHANNELS) {
            return mixLevels[ch];
        }
        return 0.0;
    }

    /**
   * Return a XML string representation of the component.
   */
    public String toXML() {
        String channels = "";
        for (int i = 0; i < MixerComponent.MIXER_CHANNELS; i++) {
            channels = channels + " channel" + i + "=\"" + mixLevels[i] + "\"";
        }
        return "<mixer level=\"" + mainLevel + "\"" + channels + "/>";
    }
}
