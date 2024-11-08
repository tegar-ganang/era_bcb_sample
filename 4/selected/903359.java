package org.nuplay;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JLabel;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;

public class NuPlaySend implements Receiver, Runnable {

    private Socket client_socket;

    private Integer channel;

    private JLabel error_label;

    private Receiver receiver;

    private DataOutputStream stream_out;

    private BufferedReader stream_in;

    private OSCByteArrayToJavaConverter converter = new OSCByteArrayToJavaConverter();

    private boolean running;

    private NuPlayMidiClient client;

    public NuPlaySend(NuPlayMidiClient client, Integer in_channel, InetAddress address, JLabel error_label) {
        this.client = client;
        this.error_label = error_label;
        this.channel = in_channel;
        try {
            client_socket = new Socket(address, 3333);
            stream_out = new DataOutputStream(client_socket.getOutputStream());
            stream_in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            Thread thread = new Thread(this);
            thread.start();
            running = true;
        } catch (Exception e) {
            client.disconnect("NuPlay@Unity3D not running!");
            e.printStackTrace();
        }
    }

    public void setMidiOut(Receiver in_receiver) {
        receiver = in_receiver;
    }

    public void run() {
        System.out.println("run!!!");
        byte[] buffer;
        while (running) {
            try {
                if (stream_in.ready()) {
                    String new_packet = stream_in.readLine();
                    if (new_packet.charAt(new_packet.length() - 1) != '*') {
                        new_packet += stream_in.readLine();
                    }
                    System.out.println(new_packet);
                    buffer = new_packet.getBytes();
                    OSCMessage message = (OSCMessage) converter.convert(buffer, buffer.length - 1);
                    Object[] data = message.getArguments();
                    if (((String) data[0]).equals("midi")) {
                        if (receiver != null) {
                            ShortMessage midi_message = new ShortMessage();
                            try {
                                if (data[1].equals("note_on")) {
                                    midi_message.setMessage(ShortMessage.NOTE_ON, ((Integer) data[2]).intValue() - 1, ((Integer) data[3]).intValue(), ((Integer) data[4]).intValue());
                                }
                                if (data[1].equals("note_off")) {
                                    midi_message.setMessage(ShortMessage.NOTE_OFF, ((Integer) data[2]).intValue() - 1, ((Integer) data[3]).intValue(), ((Integer) data[4]).intValue());
                                }
                                if (data[1].equals("control_change")) {
                                    midi_message.setMessage(ShortMessage.CONTROL_CHANGE, ((Integer) data[2]).intValue() - 1, ((Integer) data[3]).intValue(), ((Integer) data[4]).intValue());
                                }
                                if (data[1].equals("pitch_bend")) {
                                    midi_message.setMessage(ShortMessage.PITCH_BEND, ((Integer) data[2]).intValue() - 1, ((Integer) data[3]).intValue(), ((Integer) data[4]).intValue());
                                }
                            } catch (InvalidMidiDataException e) {
                                e.printStackTrace();
                            }
                            receiver.send((MidiMessage) midi_message, -1);
                        }
                    }
                }
            } catch (IOException e) {
                client.disconnect("NuPlay@Unity3D disconnected!");
                e.printStackTrace();
            }
        }
    }

    public void close() {
        running = false;
        try {
            stream_in.close();
            stream_out.close();
            client_socket.close();
            receiver.close();
            stream_in = null;
            stream_out = null;
            client_socket = null;
            receiver = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(MidiMessage message, long timeStamp) {
        OSCMessage osc_message = new OSCMessage();
        osc_message.setAddress("/nuplay/channel_" + this.channel);
        osc_message.addArgument(new String("midi"));
        NuPlayMidiMessage midi_message = new NuPlayMidiMessage(message.getMessage());
        switch(midi_message.getCommand()) {
            case NuPlayMidiMessage.NOTE_ON:
                error_label.setText("note on :: n=" + midi_message.getData1() + ", v=" + midi_message.getData2());
                osc_message.addArgument(new String("note_on"));
                osc_message.addArgument(new Integer(midi_message.getChannel()));
                osc_message.addArgument(new Integer(midi_message.getData1()));
                osc_message.addArgument(new Integer(midi_message.getData2()));
                osc_message.addArgument(new String(""));
                break;
            case NuPlayMidiMessage.NOTE_OFF:
                error_label.setText("note off :: n=" + midi_message.getData1() + ", v=" + midi_message.getData2());
                osc_message.addArgument(new String("note_off"));
                osc_message.addArgument(new Integer(midi_message.getChannel()));
                osc_message.addArgument(new Integer(midi_message.getData1()));
                osc_message.addArgument(new Integer(midi_message.getData2()));
                osc_message.addArgument(new String(""));
                break;
            case NuPlayMidiMessage.CONTROL_CHANGE:
                error_label.setText("control change :: c=" + midi_message.getData1() + ", d=" + midi_message.getData2());
                osc_message.addArgument(new String("control_change"));
                osc_message.addArgument(new Integer(midi_message.getChannel()));
                osc_message.addArgument(new Integer(midi_message.getData1()));
                osc_message.addArgument(new Integer(midi_message.getData2()));
                osc_message.addArgument(new String(""));
                break;
        }
        if (client_socket.isConnected()) {
            if (stream_out != null) {
                try {
                    stream_out.write(osc_message.getByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                    client.disconnect("NuPlay@Unity3D disconnected!");
                }
            }
        }
    }
}
