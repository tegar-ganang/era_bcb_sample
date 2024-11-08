package edu.iupmime.ginasys.client;

import java.io.FileNotFoundException;
import java.io.PipedOutputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;

/**
 * Record input audio stream.
 *
 * @author Cedric Chantepie (cedric.chantepie@iupmime.univ-lemans.fr)
 */
public final class Record implements Runnable {

    /**
     */
    private AudioFormat format = null;

    /**
     */
    private TargetDataLine line = null;

    /**
     */
    private OutputStream out = null;

    /**
     */
    private Socket dest = null;

    /**
     */
    private String host = null;

    /**
     */
    private int port = -1;

    /**
     */
    private boolean transmit = false;

    /**
     * CLI start point.
     */
    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        Record r = new Record(host, port);
        r.start();
        boolean t = r.getTransmit();
        BufferedReader rd = null;
        try {
            InputStreamReader isr = new InputStreamReader(System.in);
            rd = new BufferedReader(isr);
            while (true) {
                if (t) {
                    System.out.println("Transmitting ...");
                } else {
                    System.out.println("... Waiting");
                }
                rd.readLine();
                r.setTransmit(t = !t);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Constructor recording data and sending it
     * to destination specified by |dsthost| and |dstport|.
     *
     * @param dsthost Host name of destination
     * @param dstport Port of destination
     */
    public Record(String dsthost, int dstport) {
        this.host = dsthost;
        this.port = dstport;
    }

    /**
     * Returns true if transmission is activated.
     */
    public synchronized boolean getTransmit() {
        return this.transmit;
    }

    /**
     * Set whether transmission is activated.
     *
     * @param transmit true if transmission should be activated, false otherwise.
     */
    public synchronized void setTransmit(boolean transmit) {
        this.transmit = transmit;
    }

    /**
     * Initialize recording.
     */
    private void init() {
        try {
            this.format = getFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            this.line = (TargetDataLine) AudioSystem.getLine(info);
            this.line.open(format);
            this.line.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Start recording
     */
    public void start() {
        if (this.dest != null) {
            return;
        }
        if (this.line == null) {
            this.init();
        }
        try {
            this.dest = new Socket(this.host, this.port);
            OutputStream destOut = dest.getOutputStream();
            PipedInputStream inpipe = new PipedInputStream();
            PipedOutputStream outpipe = new PipedOutputStream(inpipe);
            this.out = outpipe;
            Transmission transmission = new Transmission(inpipe, destOut);
            Thread txThread = new Thread(transmission);
            Thread captureThread = new Thread(this);
            txThread.start();
            captureThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        int bufferSize = (int) this.format.getSampleRate() * format.getFrameSize();
        byte buffer[] = new byte[bufferSize];
        try {
            while (true) {
                int count = this.line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    this.out.write(buffer, 0, count);
                }
            }
        } catch (IOException e) {
            System.err.println("I/O problems: " + e);
            System.exit(-1);
        } finally {
            this.release();
        }
    }

    /**
     * Returns required audio format.
     */
    private static AudioFormat getFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    /**
     * Release underlying resources.
     */
    private void release() {
        if (this.out == null) {
            return;
        }
        try {
            this.out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void finalize() throws Throwable {
        this.release();
    }

    /**
     * Update an ouput stream writing on it data
     * read from specified input stream.
     * It used given buffer size for reading data and
     * close and flush all stream.
     *
     * @param is Input stream from which data will be read.
     * @param os Output stream to which data will be written.
     * @param bufferSize Size of buffer to be used to read input data.
     */
    private static void updateStream(InputStream is, OutputStream os, int bufferSize) {
        byte[] buffer = new byte[bufferSize];
        try {
            int read = -1;
            if ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                os.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Transmission
     *
     * @author Cedric Chantepie (cedric.chantepie@iupmime.univ-lemans.fr)
     */
    private final class Transmission implements Runnable {

        /**
	 */
        private InputStream in = null;

        /**
	 */
        private OutputStream out = null;

        /**
	 * Transmit data from |in| to |out|.
	 *
	 * @param in Input stream
	 * @param out Output stream
	 */
        public Transmission(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        /**
	 * {@inheritDoc}
	 */
        public void finalize() throws Throwable {
            if (this.in != null) {
                try {
                    this.in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (this.out != null) {
                try {
                    this.out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
	 * {@inheritDoc}
	 */
        public void run() {
            byte[] nullBuff = new byte[8 * 1024];
            try {
                while (true) {
                    if (transmit) {
                        Record.updateStream(this.in, this.out, 8 * 1024);
                    } else {
                        this.in.read(nullBuff, 0, nullBuff.length);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Connection error");
                System.exit(1);
            }
        }
    }
}
