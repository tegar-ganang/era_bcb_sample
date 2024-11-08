package tootmidi;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.server.AudioClient;
import uk.org.toot.audio.server.AudioServer;
import uk.org.toot.audio.server.IOAudioProcess;
import uk.org.toot.audio.server.MultiIOJavaSoundAudioServer;
import uk.org.toot.control.CompoundControl;
import uk.org.toot.swingui.controlui.CompoundControlPanel;
import uk.org.toot.swingui.controlui.ControlPanelFactory;
import uk.org.toot.swingui.synthui.MultiControlPanel;
import uk.org.toot.synth.SynthChannel;
import uk.org.toot.synth.synths.plucked.FourStringBassGuitarControls;
import uk.org.toot.synth.synths.plucked.PluckedSynth;
import uk.org.toot.synth.synths.plucked.PluckedSynthControls;

/**
 *
 * @author pjl
 */
public class TootMidiTestSimple {

    public static void main(String args[]) {
        try {
            final AudioServer audioServer = new MultiIOJavaSoundAudioServer();
            List<String> list = audioServer.getAvailableOutputNames();
            Object a[] = new Object[list.size()];
            a = list.toArray(a);
            Object selectedValue = JOptionPane.showInputDialog(null, "Select audio output", "Please", JOptionPane.INFORMATION_MESSAGE, null, a, a[0]);
            final IOAudioProcess outProcess = audioServer.openAudioOutput((String) selectedValue, "output");
            audioServer.start();
            PluckedSynthControls controls = new FourStringBassGuitarControls();
            final PluckedSynth synth = new PluckedSynth(controls);
            synth.open();
            CompoundControlPanel panel = new CompoundControlPanel(controls, 1, null, new ControlPanelFactory() {

                @Override
                protected boolean canEdit() {
                    return true;
                }
            }, true, true);
            JFrame frame = new JFrame();
            frame.setContentPane(panel);
            frame.pack();
            frame.setVisible(true);
            SynthChannel chan = synth.getChannel(0);
            final AudioBuffer buff = audioServer.createAudioBuffer("BUFF");
            buff.setRealTime(true);
            AudioClient client = new AudioClient() {

                public void setEnabled(boolean arg0) {
                }

                public void work(int arg0) {
                    synth.processAudio(buff);
                    buff.setChannelFormat(ChannelFormat.STEREO);
                    outProcess.processAudio(buff);
                }
            };
            audioServer.setClient(client);
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice dev = null;
            for (int i = 0; i < infos.length; i++) {
                System.out.println(infos[i].getName());
                if (infos[i].getName().equals("Virtual Piano")) {
                    MidiDevice.Info rtinfo = infos[i];
                    dev = MidiSystem.getMidiDevice(rtinfo);
                    if (dev.getMaxTransmitters() != 0) {
                        break;
                    }
                    dev = null;
                }
            }
            if (dev == null) {
                return;
            }
            dev.open();
            dev.getTransmitter().setReceiver(new Receiver() {

                public void send(MidiMessage message, long timeStamp) {
                    System.out.println(" HELLO ");
                    synth.transport(message, timeStamp);
                }

                public void close() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(TootMidiTestSimple.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
