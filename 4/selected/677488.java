package example.audiosample;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.aspectix.formi.FormiFragment;
import org.aspectix.formi.FormiView;
import org.aspectix.formi.FragImplFactory;

/**
 * @author Michael Kirstein
 * The fragment implementation for the audio sample
 * 
 */
public class Radio extends FormiFragment implements Runnable, RadioInterface {

    /**
	 * 
	 */
    private static final long serialVersionUID = -1441858477074991226L;

    public static final String COMM_ADDR = "224.0.0.1";

    /**
     * @author Michael Kirstein
     * the ticker for sending a audio file. 
     * Feb 3, 2004
     */
    class Ticker implements Runnable {

        Thread m_waitThread;

        Object m_WaitPoint;

        long m_waitTime_ms;

        int m_waitTime_ns;

        boolean m_stop = false;

        Ticker(float time, Object waitPoint) {
            m_waitTime_ms = (long) (time * Math.pow(10, 3));
            m_waitTime_ns = ((int) (time * Math.pow(10, 6))) % 1000000;
            m_WaitPoint = waitPoint;
            m_waitThread = new Thread(this, "WaitThread");
            System.out.println("Wait ms: " + m_waitTime_ms + " Wait ns: " + m_waitTime_ns);
            m_waitThread.start();
        }

        void stop() {
            m_stop = true;
        }

        ;

        public void run() {
            try {
                while (m_stop != true) {
                    Thread.sleep(m_waitTime_ms, m_waitTime_ns);
                    synchronized (m_WaitPoint) {
                        m_WaitPoint.notify();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private FormiView m_View;

    public static int port = 6666;

    public static InetAddress memberGroup = null;

    public static int timeout = 1000;

    public static int transferSize = 512;

    MulticastSocket servicePort;

    AudioInputStream recive;

    Thread m_reciveThread;

    Collection<PipedOutputStream> m_streams;

    static {
        try {
            memberGroup = InetAddress.getByName(COMM_ADDR);
        } catch (Exception e) {
        }
    }

    public AudioFormat getFormat() throws IOException {
        AudioFormat format = new AudioFormat((float) 8000.0, 16, 1, true, false);
        return format;
    }

    public Radio() {
        super(null, new Object[] { COMM_ADDR });
        initCom();
    }

    public Radio(FragImplFactory fragImplFactory, Object[] comm) {
        super(fragImplFactory, comm);
        initCom();
    }

    private void initCom() {
        try {
            servicePort = new MulticastSocket(port);
            servicePort.joinGroup(memberGroup);
            servicePort.setSoTimeout(timeout);
            servicePort.setReceiveBufferSize(65536);
            servicePort.setSendBufferSize(65536);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeCom() {
        try {
            servicePort.leaveGroup(memberGroup);
        } catch (IOException e) {
            e.printStackTrace();
        }
        servicePort.close();
    }

    public void send(String aFilename) throws UnsupportedAudioFileException, IOException {
        Object wp = new Object();
        double time = transferSize / (getFormat().getSampleRate() * getFormat().getSampleSizeInBits() / 8 * getFormat().getChannels());
        try {
            System.out.println("Now Sending " + aFilename);
            AudioFormat orgFormat;
            byte[] data = new byte[transferSize];
            File mySong = new File(aFilename);
            time = time * 1.01;
            Ticker wait = new Ticker((float) time, wp);
            try {
                int dataRead;
                AudioInputStream orgStream, sendStream;
                orgStream = AudioSystem.getAudioInputStream(mySong);
                orgFormat = orgStream.getFormat();
                System.out.println("Input Format is: " + orgFormat);
                System.out.println("Network Format is: " + getFormat());
                if (!orgFormat.matches(getFormat())) {
                    System.out.println("Audio formats differ. Trying conversion.");
                    if (!AudioSystem.isConversionSupported(getFormat(), orgStream.getFormat())) {
                        System.out.println("Conversion is not supported");
                        return;
                    }
                    sendStream = AudioSystem.getAudioInputStream(getFormat(), orgStream);
                } else {
                    sendStream = orgStream;
                }
                while (sendStream.available() > 0) {
                    synchronized (wp) {
                        dataRead = sendStream.read(data);
                        DatagramPacket beat = new DatagramPacket(data, dataRead, memberGroup, port);
                        wp.wait();
                        servicePort.send(beat);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                wait.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        byte[] data = new byte[2048];
        DatagramPacket remoteObject;
        Iterator<PipedOutputStream> pos;
        PipedOutputStream cur;
        remoteObject = new DatagramPacket(data, data.length);
        try {
            do {
                try {
                    servicePort.receive(remoteObject);
                    synchronized (m_streams) {
                        pos = m_streams.iterator();
                        while (pos.hasNext()) {
                            cur = pos.next();
                            try {
                                cur.write(remoteObject.getData(), 0, remoteObject.getLength());
                            } catch (IOException ex) {
                                m_streams.remove(cur);
                            }
                        }
                    }
                } catch (java.net.SocketTimeoutException e) {
                }
            } while (!m_streams.isEmpty());
        } catch (Exception e) {
            System.out.println("Exception caut:'" + e.getMessage() + "'");
            e.printStackTrace();
        }
        System.out.println("Recive Thread stoped");
    }

    public AudioInputStream getAudioStream() throws IOException {
        PipedOutputStream outStream;
        PipedInputStream inStream = null;
        if (m_streams == null) {
            m_streams = new ArrayList<PipedOutputStream>();
        }
        synchronized (m_streams) {
            outStream = new PipedOutputStream();
            inStream = new AudioInputPipe(outStream);
            m_streams.add(outStream);
            if (m_reciveThread == null) {
                m_reciveThread = new Thread(this, "ReciveThread");
                m_reciveThread.start();
            }
        }
        AudioInputStream audioInput = new AudioInputStream(inStream, getFormat(), AudioSystem.NOT_SPECIFIED);
        return audioInput;
    }

    public void initObject(FormiView myView) {
        m_View = myView;
        initCom();
    }

    public void initFragment(FormiView myView) {
        m_View = myView;
        initCom();
    }

    public void dereferenced(int noFrags) {
        System.out.println("Referenced by " + noFrags);
        if (noFrags == 0) {
            System.out.println("No References left -> Removing myself");
            m_View.removeView();
        }
    }

    public void shutdown() {
        closeCom();
    }

    public void referenced(int noFrags) {
    }

    public String getCommunicationCredentials() {
        return COMM_ADDR;
    }
}
