package example.audio;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.aspectix.formi.FormiException;

/**
 * Sender fragment of the radio example object
 * 
 * Initial version dates back to 3.2.2003
 * 
 * @author Michael Kirstein
 * @author Franz J. Hauck
 */
public class RadioSenderFImpl extends RadioFImpl implements Radio {

    /**
	 * Logger
	 */
    protected static Logger log = Logger.getLogger("example");

    /**
	 * Packet size of transmission
	 */
    protected static int transferSize = 512;

    /**
	 * The serial version UID
	 */
    private static final long serialVersionUID = 5018913839146691133L;

    /**
	 * Constructor of radio sender fragment
	 */
    public RadioSenderFImpl(String addr, int port) throws FormiException, UnknownHostException, IOException {
        super(new RadioFMgr(addr, port));
    }

    protected class Ticker implements Runnable {

        Thread tickerThread;

        Object barrier;

        long waitMs;

        int waitNs;

        boolean stopFlag = false;

        Ticker(float time, Object waitPoint) {
            waitMs = (long) (time * 1000);
            waitNs = ((int) (time * 1000000)) % 1000;
            barrier = waitPoint;
            tickerThread = new Thread(this, "Ticker");
            log.finer("Ticker: wait time is " + waitMs + "ms+" + waitNs + "ns");
            tickerThread.start();
        }

        void stop() {
            stopFlag = true;
        }

        ;

        public void run() {
            try {
                while (!stopFlag) {
                    Thread.sleep(waitMs, waitNs);
                    synchronized (barrier) {
                        barrier.notify();
                    }
                }
            } catch (InterruptedException e) {
                ;
            }
        }
    }

    /**
	 * This method sends a filed audio stream to the clients via UDP multicast sockets.
	 * 
	 * @param filename Name of the audio file
	 */
    @Override
    public synchronized void send(String filename) throws UnsupportedAudioFileException, IOException {
        Object barrier = new Object();
        float tickerTime = transferSize / (getFormat().getSampleRate() * getFormat().getSampleSizeInBits() / 8 * getFormat().getChannels());
        AudioInputStream orgStream, sendStream;
        AudioFormat orgFormat;
        byte[] data = new byte[transferSize];
        Ticker tickerThread = new Ticker(tickerTime, barrier);
        int dataRead;
        log.info("Now broadcasting " + filename);
        try {
            orgStream = AudioSystem.getAudioInputStream(new File(filename));
            orgFormat = orgStream.getFormat();
            log.fine("Input Format is: " + orgFormat);
            log.fine("Network Format is: " + getFormat());
            if (!orgFormat.matches(getFormat())) {
                if (!AudioSystem.isConversionSupported(getFormat(), orgStream.getFormat())) {
                    log.severe("Conversion is not supported");
                    throw new IllegalArgumentException();
                }
                sendStream = AudioSystem.getAudioInputStream(getFormat(), orgStream);
            } else {
                sendStream = orgStream;
            }
            while (sendStream.available() > 0) {
                synchronized (barrier) {
                    dataRead = sendStream.read(data);
                    DatagramPacket packet = new DatagramPacket(data, dataRead, fmgr.getAddr(), fmgr.getPort());
                    barrier.wait();
                    servicePort.send(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tickerThread.stop();
        }
    }
}
