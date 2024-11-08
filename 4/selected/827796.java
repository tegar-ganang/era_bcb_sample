package lt.ku.ik.recon.presentation.gui.widgets;

import gnu.io.CommPortIdentifier;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;
import lt.ku.ik.recon.logic.model.BaudRate;
import lt.ku.ik.recon.logic.model.CommandFormat;
import lt.ku.ik.recon.logic.model.DataBits;
import lt.ku.ik.recon.logic.model.Parity;
import lt.ku.ik.recon.logic.model.Settings;
import lt.ku.ik.recon.logic.model.StopBits;
import lt.ku.ik.recon.logic.service.SerialPortService;
import lt.ku.ik.recon.logic.service.SettingsService;

/**
 *
 * @author linas
 */
public class SettingsWindowFrame extends SettingsWindowSketch {

    private SettingsService settingsService;

    private SerialPortService serialPortService;

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSerialPortService(SerialPortService serialPortService) {
        this.serialPortService = serialPortService;
    }

    public void setupListeners() {
        ActionListener closeActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        };
        jButtonClose.addActionListener(closeActionListener);
        ActionListener applyActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                apply();
            }
        };
        jButtonApply.addActionListener(applyActionListener);
    }

    public void setValues() {
        Settings settings = settingsService.getSettings();
        HashSet<CommPortIdentifier> devices = serialPortService.getAvailablePorts();
        Iterator deviceIterator = devices.iterator();
        while (deviceIterator.hasNext()) {
            CommPortIdentifier commPortIdentifier = (CommPortIdentifier) deviceIterator.next();
            String port = commPortIdentifier.getName();
            jComboBoxDevice.addItem(port);
        }
        for (BaudRate baudRate : BaudRate.values()) {
            jComboBoxBaudRate.addItem(baudRate);
        }
        for (DataBits dataBits : DataBits.values()) {
            jComboBoxDataBits.addItem(dataBits);
        }
        for (StopBits stopBits : StopBits.values()) {
            jComboBoxStopBits.addItem(stopBits);
        }
        for (Parity parity : Parity.values()) {
            jComboBoxParity.addItem(parity);
        }
        for (CommandFormat commandFormat : CommandFormat.values()) {
            jComboBoxCommandFormat.addItem(commandFormat);
        }
        jComboBoxDevice.setSelectedItem(settings.getDevice());
        jComboBoxBaudRate.setSelectedItem(settings.getBaudRate());
        jComboBoxDataBits.setSelectedItem(settings.getDataBits());
        jComboBoxStopBits.setSelectedItem(settings.getStopBits());
        jComboBoxParity.setSelectedItem(settings.getParity());
        jSpinnerPrecision.setValue(settings.getPrecision());
        jSpinnerChannels.setValue(settings.getChannels());
        jTextFieldStartCommand.setText(String.valueOf(settings.getStartCommand()));
        jTextFieldStopCommand.setText(String.valueOf(settings.getStopCommand()));
        jComboBoxCommandFormat.setSelectedItem(settings.getCommandFormat());
        jComboBoxCommandFormat.setEnabled(false);
    }

    public void apply() {
        Settings settings = settingsService.getSettings();
        settings.setDevice(String.valueOf(jComboBoxDevice.getSelectedItem()));
        settings.setBaudRate((BaudRate) jComboBoxBaudRate.getSelectedItem());
        settings.setDataBits((DataBits) jComboBoxDataBits.getSelectedItem());
        settings.setStopBits((StopBits) jComboBoxStopBits.getSelectedItem());
        settings.setParity((Parity) jComboBoxParity.getSelectedItem());
        settings.setPrecision((Integer) jSpinnerPrecision.getValue());
        settings.setChannels((Integer) jSpinnerChannels.getValue());
        settings.setStartCommand(Integer.valueOf(jTextFieldStartCommand.getText()));
        settings.setStopCommand(Integer.valueOf(jTextFieldStopCommand.getText()));
        settings.setCommandFormat((CommandFormat) jComboBoxCommandFormat.getSelectedItem());
    }
}
