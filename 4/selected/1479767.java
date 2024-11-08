package es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands;

/**
 *
 * @author tarasco
 */
public class CommandConfigAnalogCfm extends Command {

    private ECGChannels m_Channels;

    private byte m_Frequency;

    public CommandConfigAnalogCfm(byte[] packet) {
        super(packet);
        m_Channels = ECGChannels.values()[packet[4]];
        m_Frequency = packet[5];
    }

    public ECGChannels getChannels() {
        return m_Channels;
    }

    public byte getFrequency() {
        return m_Frequency;
    }
}
