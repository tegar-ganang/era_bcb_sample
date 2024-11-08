package com.frinika.sequencer.gui.mixer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Method;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import static com.frinika.localization.CurrentLocale.getMessage;
import com.frinika.project.SynthesizerDescriptor;
import com.frinika.project.mididevices.gui.MidiDevicesPanel;
import com.frinika.synth.importers.soundfont.SoundFontFileFilter;

/**
 * @author Peter Johan Salomonsen
 */
public class MidiDeviceMixerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public MidiChannelMixerSlot[] mixerSlots = new MidiChannelMixerSlot[16];

    public MidiDeviceMixerPanel(final MidiDevicesPanel panel, final SynthWrapper synthWrapper) {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        if (synthWrapper.getRealDevice() instanceof Synthesizer) {
            JButton loadSoundbankButton = new JButton(getMessage("mididevices.loadsoundbank"));
            loadSoundbankButton.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    try {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setDialogTitle("Open soundfont");
                        chooser.setFileFilter(new SoundFontFileFilter());
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            File soundFontFile = chooser.getSelectedFile();
                            Soundbank soundbank = synthWrapper.getSoundbank(soundFontFile);
                            synthWrapper.loadAllInstruments(soundbank);
                            System.out.println("Soundbank loaded");
                            ((SynthesizerDescriptor) panel.getProject().getMidiDeviceDescriptor(synthWrapper)).setSoundBankFileName(soundFontFile.getAbsolutePath());
                        }
                        ;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            add(loadSoundbankButton, gc);
            MidiDevice dev = synthWrapper.getRealDevice();
            try {
                Method method = dev.getClass().getMethod("show");
                JButton showSettingsButton = new JButton(getMessage("mididevices.show"));
                showSettingsButton.addMouseListener(new MouseAdapter() {

                    public void mouseClicked(MouseEvent e) {
                        MidiDevice dev = synthWrapper.getRealDevice();
                        Method method;
                        try {
                            method = dev.getClass().getMethod("show");
                            method.invoke(dev);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                });
                add(showSettingsButton, gc);
            } catch (SecurityException e1) {
            } catch (NoSuchMethodException e1) {
            }
        }
        JButton renameDeviceButton = new JButton(getMessage("mididevices.rename"));
        renameDeviceButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = JOptionPane.showInputDialog(getMessage("mididevices.entername"));
                if (value == null) return;
                panel.getProject().getMidiDeviceDescriptor(synthWrapper).setProjectName(value);
                panel.updateDeviceTabs();
            }
        });
        add(renameDeviceButton, gc);
        gc.gridwidth = GridBagConstraints.REMAINDER;
        JButton removeDeviceButton = new JButton(getMessage("mididevices.removedevice"));
        removeDeviceButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                panel.remove(synthWrapper);
            }
        });
        add(removeDeviceButton, gc);
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.VERTICAL;
        gc.weighty = 1.0;
        for (int n = 0; n < mixerSlots.length; n++) {
            mixerSlots[n] = new MidiChannelMixerSlot(synthWrapper, synthWrapper.getChannels()[n]);
            add(mixerSlots[n], gc);
        }
        synthWrapper.gui = this;
    }
}
