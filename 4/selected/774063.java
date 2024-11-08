package be.lassi.control.midi;

import java.util.Formatter;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

/**
 * Writes a string representation of a <code>MidiMessage</code> object.
 */
public class MidiMessagePrinter {

    public void append(final StringBuilder b, final MidiMessage message) {
        if (message instanceof ShortMessage) {
            ShortMessage shortMessage = (ShortMessage) message;
            append(b, shortMessage);
        } else if (message instanceof SysexMessage) {
            SysexMessage sysexMessage = (SysexMessage) message;
            append(b, sysexMessage);
        } else {
            String name = message.getClass().getName();
            int index = name.lastIndexOf('.');
            name = name.substring(index + 1);
            b.append(name);
        }
    }

    private void append(final StringBuilder b, final ShortMessage message) {
        int command = message.getCommand();
        int channel = message.getChannel();
        if (command == ShortMessage.NOTE_ON || command == ShortMessage.NOTE_OFF) {
            b.append("Note ");
            if (command == ShortMessage.NOTE_ON) {
                b.append("On");
            } else {
                b.append("Off");
            }
            b.append(" (channel=");
            b.append(channel);
            b.append(", note=");
            b.append(message.getData1());
            b.append(", velocity=");
            b.append(message.getData2());
            b.append(")");
        } else {
            String string = "Unknown Command";
            if (command == ShortMessage.CONTROL_CHANGE) {
                string = "Control Change";
            } else if (command == ShortMessage.POLY_PRESSURE) {
                string = "Polyphonic Key Pressure (Aftertouch)";
            } else if (command == ShortMessage.PROGRAM_CHANGE) {
                string = "Program Change";
            } else if (command == ShortMessage.CHANNEL_PRESSURE) {
                string = "Channel Pressure (Aftertouch)";
            } else if (command == ShortMessage.PITCH_BEND) {
                string = "Pitch Bend";
            }
            b.append(string);
            b.append(" (channel=");
            b.append(channel);
            b.append(", data1=");
            b.append(message.getData1());
            b.append(", data2=");
            b.append(message.getData2());
            b.append(")");
        }
    }

    private void append(final StringBuilder b, final SysexMessage message) {
        byte[] bytes = message.getMessage();
        b.append("SysexMessage (");
        if (bytes.length >= 8) {
            int messageId = bytes[7] * 128;
            messageId += bytes[8];
            b.append(messageId);
            b.append("  ");
            byte[] stringBytes = new byte[bytes.length - 8 - 2];
            for (int i = 0; i < stringBytes.length; i++) {
                stringBytes[i] = bytes[i + 8 + 1];
            }
            String string = new String(stringBytes);
            b.append(string);
            for (int i = string.length(); i < 35; i++) {
                b.append(" ");
            }
        }
        Formatter formatter = new Formatter(b);
        for (int i = 0; i < message.getLength(); i++) {
            formatter.format("%02X", bytes[i]);
            if (i < message.getLength() - 1) {
                b.append(", ");
            }
        }
        b.append(")");
    }
}
