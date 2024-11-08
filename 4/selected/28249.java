package test;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class MyReceiver implements Receiver {

    Receiver receiver;

    public MyReceiver() {
        try {
            receiver = MidiSystem.getReceiver();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void send(MidiMessage msg, long time) {
        System.out.println("-------------------");
        System.out.println("Length: " + msg.getLength());
        System.out.println("Status: " + msg.getStatus());
        System.out.println("Time: " + (int) time / 1000);
        System.out.println("-------------------");
        this.receiver.send(msg, time);
        if (msg instanceof ShortMessage) {
            ShortMessage msg2 = (ShortMessage) msg;
            System.out.println("Channel: " + msg2.getChannel());
            System.out.println("Command: " + msg2.getCommand());
            System.out.println("Data1: " + msg2.getData1());
            System.out.println("Data2: " + msg2.getData2());
            System.out.println("Length: " + msg2.getLength());
            System.out.println("Status: " + msg2.getStatus());
            if (msg2.getStatus() == ShortMessage.NOTE_ON) {
                System.out.print("NOTE ON");
            }
        }
    }

    public void close() {
        this.receiver.close();
        System.out.println("Closing");
    }

    public static MidiDevice getMidiDevice(String strDeviceName) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            System.out.println(aInfos[i].getName());
            if (aInfos[i].getName().equals(strDeviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                    return device;
                } catch (MidiUnavailableException e) {
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            MyReceiver myrec = new MyReceiver();
            MidiDevice device = getMidiDevice("USB audioeszk�z");
            device.open();
            device.getTransmitter().setReceiver(myrec);
            Thread.sleep(10000000);
            device.close();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
