package lt.ku.ik.recon.data.content;

import gnu.io.SerialPort;
import lt.ku.ik.recon.logic.model.BaudRate;
import lt.ku.ik.recon.logic.model.CommandFormat;
import lt.ku.ik.recon.logic.model.DataBits;
import lt.ku.ik.recon.logic.model.Parity;
import lt.ku.ik.recon.logic.model.Settings;
import lt.ku.ik.recon.logic.model.StopBits;

/**
 *
 * @author linas
 */
public class SettingsDao {

    Settings settings = new Settings();

    public SettingsDao() {
        settings.setDevice(null);
        settings.setBaudRate(BaudRate.BAUD_115200);
        settings.setDataBits(DataBits.DATABITS_8);
        settings.setStopBits(StopBits.STOPBITS_1);
        settings.setParity(Parity.PARITY_NONE);
        settings.setPrecision(11);
        settings.setChannels(2);
        settings.setStartCommand(115);
        settings.setStopCommand(255);
        settings.setCommandFormat(CommandFormat.DEC);
        settings.setResolutionOfGraph(8000);
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings.setDevice(settings.getDevice());
        this.settings.setBaudRate(settings.getBaudRate());
        this.settings.setDataBits(settings.getDataBits());
        this.settings.setStopBits(settings.getStopBits());
        this.settings.setParity(settings.getParity());
        this.settings.setPrecision(settings.getPrecision());
        this.settings.setChannels(settings.getChannels());
        this.settings.setStartCommand(settings.getStartCommand());
        this.settings.setStopCommand(settings.getStopCommand());
        this.settings.setCommandFormat(settings.getCommandFormat());
        this.settings.setResolutionOfGraph(settings.getResolutionOfGraph());
    }
}
