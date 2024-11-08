package org.nuplay;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.swing.JLabel;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;

public class NuPlayArduinoSend implements Runnable {

    private Socket client_socket;

    private DataOutputStream stream_out;

    private BufferedReader stream_in;

    private OSCByteArrayToJavaConverter converter = new OSCByteArrayToJavaConverter();

    private BufferedInputStream in;

    private Integer channel;

    private JLabel error_label;

    private Thread thread;

    private ByteBuffer buffer;

    private int status;

    private int counter;

    private boolean running;

    private NuPlayArduinoClient client;

    public NuPlayArduinoSend(NuPlayArduinoClient client, InputStream in, Integer in_channel, InetAddress address, JLabel error_label) {
        this.client = client;
        this.in = new BufferedInputStream(in);
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
            client.disconnect("connection refused");
            e.printStackTrace();
        }
    }

    public static byte[] copy(byte[] src, int offset, int length) {
        byte[] new_buffer = new byte[length];
        int j = 0;
        for (int i = offset; i < offset + length; i++) {
            new_buffer[j] = src[i];
            j++;
        }
        return new_buffer;
    }

    public void run() {
        while (running) {
            int status = -1;
            NuPlayMidiMessage message = null;
            while (in != null) {
                try {
                    byte b = (byte) in.read();
                    if (b != -1) {
                        switch(b & 0xFF) {
                            case 0xF0:
                                status = 1;
                                buffer = ByteBuffer.allocate(1024);
                                buffer.put(b);
                                break;
                            case 0xF7:
                                status = -1;
                                message = new NuPlayMidiMessage(copy(buffer.array(), 0, buffer.position()));
                                send(message, 0);
                                buffer = ByteBuffer.allocate(3);
                                counter = 0;
                                break;
                            default:
                                if (buffer == null) {
                                    buffer = ByteBuffer.allocate(3);
                                    counter = 0;
                                }
                                buffer.put(b);
                                counter++;
                                if ((status == -1) && (counter == 3)) {
                                    message = new NuPlayMidiMessage(copy(buffer.array(), 0, buffer.position()));
                                    send(message, 0);
                                    buffer = ByteBuffer.allocate(3);
                                    counter = 0;
                                }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() {
        running = false;
        try {
            stream_in.close();
            stream_out.close();
            client_socket.close();
            stream_in = null;
            stream_out = null;
            client_socket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                error_label.setText("Error : " + e.getMessage());
            }
            in = null;
        }
    }

    public void send(NuPlayMidiMessage message, long timeStamp) {
        OSCMessage osc_message = new OSCMessage();
        osc_message.setAddress("/nuplay/channel_" + this.channel);
        osc_message.addArgument(new String("midi"));
        NuPlayMidiMessage midi_message = new NuPlayMidiMessage(message.getMessage());
        switch(midi_message.getCommand()) {
            case NuPlayMidiMessage.PITCH_BEND:
                error_label.setText("pitch bend :: d1=" + midi_message.getData1() + " , d2=" + midi_message.getData2());
                osc_message.addArgument(new String("pitch_bend"));
                osc_message.addArgument(new Integer(midi_message.getChannel()));
                osc_message.addArgument(new Integer(midi_message.getData1()));
                osc_message.addArgument(new Integer(midi_message.getData2()));
                osc_message.addArgument(new String(""));
                break;
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
            case NuPlayMidiMessage.SYSEX_START:
                error_label.setText("sysex :: data=" + message.getStringMessage());
                osc_message.addArgument(new String("sys_ex"));
                osc_message.addArgument(new Integer(midi_message.getChannel()));
                osc_message.addArgument(new Integer(-1));
                osc_message.addArgument(new Integer(-1));
                System.out.println(message.getStringMessage());
                osc_message.addArgument(message.getStringMessage());
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
                    this.client.disconnect("NuPlay not active?");
                    e.printStackTrace();
                }
            }
        }
    }
}
