package spaghetti;

import java.util.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;
import org.xiph.speex.*;

public class VoiceSystem extends Thread {

    String filename = "foo4.gsm";

    public int buffSize = 1650;

    public String username = "";

    Vector buffer = new Vector();

    Transmitter transmitter;

    Reciever reciever;

    DataInputStream playbackInputStream = null;

    BufferedInputStream recordInputStream = null;

    SourceDataLine playbackLine = null;

    AudioInputStream recordIn = null;

    TargetDataLine targetDataLine;

    DataOutputStream streamToServer = null;

    AudioFormat format = null;

    boolean writeline = false;

    boolean readline = false;

    /**
	 * @param b1 -
	 *                      writeline
	 * @param b2 -
	 *                      readline
	 */
    public VoiceSystem(boolean b1, boolean b2) {
        writeline = b1;
        readline = b2;
        float samplerate = 8000;
        int samplesizeinbits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigendian = false;
        format = new AudioFormat(samplerate, samplesizeinbits, channels, signed, bigendian);
        if (readline) {
            System.out.println("Opening source stream (output)");
            DataLine.Info info2 = new DataLine.Info(SourceDataLine.class, format);
            try {
                playbackLine = (SourceDataLine) AudioSystem.getLine(info2);
                playbackLine.open(format, 10000);
                playbackLine.start();
                System.out.println("Source data line opened");
            } catch (LineUnavailableException e) {
                e.printStackTrace();
                ok = false;
            } catch (Exception e) {
                e.printStackTrace();
                ok = false;
            }
            if (playbackLine == null) System.out.println("cannot create playbackLine");
        }
        if (writeline) {
            System.out.println("Opening input /microphone");
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            targetDataLine = null;
            try {
                targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            } catch (Exception e) {
                ok = false;
                e.printStackTrace();
            }
            try {
                targetDataLine.open(format, 10000);
                targetDataLine.start();
                recordIn = new AudioInputStream(targetDataLine);
                System.out.println("TargetDataLine opened + started.");
            } catch (Exception e) {
                e.printStackTrace();
                ok = false;
            }
            if (recordIn == null) System.out.println("cannot create recordLine");
        }
        System.out.println("after open");
        System.out.println("daemon: Opened audio channels..");
    }

    boolean ok = true;

    boolean running = true;

    boolean opened = false;

    Socket socket;

    /**
	 * the thread runner - does all time critical line opening and
	 * initializing. change code here to actually support different
	 * formats,etc.
	 *  
	 */
    public void run() {
        if (ok) {
            try {
                socket = new Socket("test.spaghettilearning.com", 9999);
                streamToServer = new DataOutputStream(socket.getOutputStream());
                playbackInputStream = new DataInputStream(socket.getInputStream());
                boolean opened = true;
                if (this.writeline) streamToServer.writeByte(0x00);
                if (this.readline) streamToServer.writeByte(0x01);
                streamToServer.writeByte((byte) username.length());
                streamToServer.write(username.getBytes());
                streamToServer.flush();
            } catch (Exception ex) {
                System.out.println("daemon: hardware problem : " + ex);
            }
            if (this.writeline) {
                transmitter = new Transmitter(this);
                transmitter.start();
                System.out.println("Transmitter started.");
            }
            if (this.readline) {
                reciever = new Reciever(this);
                reciever.start();
                System.out.println("Reciever started.");
            }
            while (running) {
                try {
                    sleep(100);
                } catch (Exception e) {
                }
            }
            this.reciever.running = false;
            this.transmitter.running = false;
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args) {
        VoiceSystem vs = new VoiceSystem(false, false);
        vs.start();
    }

    public void stopTalking() {
        transmitter.running = false;
        try {
            streamToServer.close();
            playbackInputStream.close();
            this.recordIn.close();
            this.recordInputStream.close();
            this.targetDataLine.close();
            this.targetDataLine.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * this class actually gets data from the sound card and transmits it to the
 * server
 * 
 * @author s0evkill
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
class Transmitter extends Thread {

    boolean running = true;

    VoiceSystem v;

    ServerWriter sw;

    int numBytesRead;

    Transmitter(VoiceSystem v) {
        this.v = v;
    }

    short[] convertToShort(byte[] b) {
        short[] ret = new short[b.length / 2];
        short i1 = 0, i2 = 0;
        for (int i = 0; i < b.length / 2; i++) {
            i1 = (short) ((b[i * 2] + 1));
            i2 = (short) ((b[(i * 2)] * 256));
            ret[i] = (short) (i1 + i2);
        }
        return ret;
    }

    int i = 0;

    public void run() {
        sw = new ServerWriter(v);
        sw.start();
        org.tritonus.lowlevel.gsm.Encoder gsmencoder = new org.tritonus.lowlevel.gsm.Encoder();
        SpeexEncoder speexencoder = new SpeexEncoder();
        speexencoder.init(2, 7, 8000, 1);
        int framesize = speexencoder.getFrameSize();
        while (running) {
            try {
                byte[] in = new byte[1280];
                numBytesRead = v.recordIn.read(in);
                System.out.println("Data read.");
                speexencoder.processData(in, 0, in.length);
                System.out.println(speexencoder.getProcessedDataByteSize());
                byte[] speex = new byte[64];
                speexencoder.getProcessedData(speex, 0);
                sw.data.addElement(speex);
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
            }
        }
        try {
            this.v.socket.close();
        } catch (Exception e) {
        }
        sw.running = false;
    }

    void dump(byte[] b) {
        for (int j = 0; j < b.length; j++) {
            System.out.print(" " + (int) b[j]);
        }
        System.out.println("\n");
    }
}

class ServerWriter extends Thread {

    VoiceSystem v;

    Vector data = new Vector();

    boolean running = true;

    ServerWriter(VoiceSystem v) {
        this.v = v;
    }

    int i = 0;

    public void run() {
        while (running) {
            if (data.size() > 0) {
                byte[] b = (byte[]) data.elementAt(0);
                try {
                    if (i < 20) {
                        v.streamToServer.write(b, 0, b.length);
                        v.streamToServer.flush();
                        System.out.println("Data flushed: " + b.length);
                        i++;
                    } else i = 0;
                } catch (Exception e) {
                }
                data.removeElementAt(0);
            } else {
                try {
                    sleep(10);
                } catch (Exception e) {
                }
            }
        }
        try {
            this.v.socket.close();
        } catch (Exception e) {
        }
    }
}

/**
 * this class actually recieves data and sends it to the sound card
 * 
 * @author s0evkill
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
class Reciever extends Thread {

    public boolean running = true;

    VoiceSystem v;

    int numBytesRead;

    byte[] gsmdata;

    Reciever(VoiceSystem v) {
        this.v = v;
    }

    public void run() {
        org.tritonus.lowlevel.gsm.GSMDecoder gsmdecoder = new org.tritonus.lowlevel.gsm.GSMDecoder();
        org.xiph.speex.SpeexDecoder speexdecoder = new SpeexDecoder();
        speexdecoder.init(2, 8000, 1, true);
        Writer w = new Writer(v);
        w.start();
        while (running) {
            try {
                byte[] pcm = new byte[1280];
                byte[] speex = new byte[64];
                byte[] gsmdata = new byte[64];
                numBytesRead = v.playbackInputStream.read(gsmdata);
                System.arraycopy(gsmdata, 0, speex, 0, speex.length);
                speexdecoder.processData(speex, 0, speex.length);
                speexdecoder.getProcessedData(pcm, 0);
                w.data.addElement(pcm);
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
            }
        }
        try {
            this.v.socket.close();
        } catch (Exception e) {
        }
    }
}

class Writer extends Thread {

    VoiceSystem v;

    Vector data = new Vector();

    boolean running = true;

    int i = 0;

    Writer(VoiceSystem v) {
        this.v = v;
    }

    public void run() {
        while (running) {
            if (data.size() > 0) {
                while (data.size() > 0) {
                    byte[] b = (byte[]) data.elementAt(0);
                    v.playbackLine.write(b, 0, b.length);
                    data.removeElementAt(0);
                }
            } else {
                try {
                    sleep(10);
                } catch (Exception e) {
                }
            }
        }
        try {
            this.v.socket.close();
        } catch (Exception e) {
        }
    }
}
