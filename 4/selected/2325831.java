package jbuzzer.audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jbuzzer.Debug;
import jbuzzer.io.InputStreamWatcher;

/**
 * 
 * No wrapper around the basic interface of java.applet.AudioClip. Instead the
 * same mimics are performed (as in java.applet.AudioClip) to hide the
 * complexity of javax.sound.sampled instances behind a simple Interface. <br>
 * java.applet.AudioClip offers too few for jbuzzer. Gain Control at least is
 * needed here, and information about length in time and current play position
 * are fancy gadgets.
 * 
 * @author <a href='Achim.Westermann@gmx.de'>Achim Westermann </a>
 */
public class SampleProxy implements ISample, Serializable {

    /**
   * The maximum size for a sound to be loaded to RAM as a clip (vs. stream
   * based access). Currently 1 MB.
   */
    public static final long MAX_CLIP_BYTE_SIZE = 1024 * 1024;

    private transient ISample clip;

    private transient boolean initialized = false;

    private transient List listeners;

    private transient Loader loader;

    /**
   * {@link Clip}and {@link SourceDataLine}both are feed by data from an
   * instance of this type. This is the initial AudioInputStream obtained from
   * the {@link #AudioSystem}.
   */
    protected transient AudioInputStream undecoded;

    /**
   * <p>
   * In case the AudioInputStream obtained from the {@link AudioSystem} does not
   * have to be decoded, this is the same as {@link #undecoded}.
   * </p>
   * <p>
   * Otherwise the method {@link #decode()}will set up this decoded stream.
   * </p>
   */
    protected transient AudioInputStream decoded;

    public boolean isInitialized() {
        return this.initialized;
    }

    /**
   * A copy here, because the internal delegation ISamples are not serialized.
   */
    private URL url;

    /**
   * <p>
   * A copy here, because the internal delegation ISamples are not serialized.
   * </p>
   * <p>
   * The default value corresponds to the default slider position. If
   * deserialized, it will have another value.
   * </p>
   */
    private float volume = 0.5f;

    /**
   * A copy here, because the internal delegation ISamples are not serialized.
   */
    private float pan;

    private long decodedByteSize;

    public SampleProxy(URL url) throws MalformedURLException, IOException, LineUnavailableException, UnsupportedAudioFileException {
        this.url = url;
        this.init();
    }

    /**
   * Triggers the stream size detection if necessary. If that happens,
   * {@link #findStreamSize(AudioInputStream)}will trigger postInit as it may
   * have to do some expensive work that is performed by another Thread (in
   * order not to block the gui).
   */
    private final void init() throws MalformedURLException, IOException, LineUnavailableException, UnsupportedAudioFileException {
        this.undecoded = AudioSystem.getAudioInputStream(this.url);
        this.decoded = SampleProxy.this.decode(this.undecoded);
        this.listeners = new LinkedList();
        this.loader = new Loader(this);
        if (Debug.debug) {
            Debug.getInstance().info(url.toString() + " : undecoded AudioInputStream of type " + this.undecoded.getClass().getName());
            Debug.getInstance().info(url.toString() + " :   decoded AudioInputStream of type " + this.decoded.getClass().getName());
        }
        if (this.decodedByteSize == 0) {
            this.findStreamSize(decoded);
        } else {
            this.postInit();
        }
    }

    /**
   * This method is invoked, when the stream size (decoded) is at hand. This is
   * the case if:
   * <ol>
   * <li>The stream size has been deserialized: Immediately.
   * <li>The stream size may be calculated by
   * init()->findStreamSize(AudioInputStream): Still same Thread.
   * <li>The stream size has been detected by a hard working background Thread:
   * Real callback.
   * </ol>
   * 
   */
    private final void postInit() throws MalformedURLException, IOException, LineUnavailableException, UnsupportedAudioFileException {
        try {
            if (Debug.debug) {
                Debug.getInstance().info(this.getClass().getName() + " trying to load " + url.toExternalForm() + " as a clip.");
            }
            if (this.decodedByteSize > MAX_CLIP_BYTE_SIZE) {
                this.clip = new SampleStreaming();
            } else {
                this.clip = new SampleClip();
            }
        } catch (Exception ex) {
            if (Debug.debug) {
                Debug.getInstance().warn(ex.getMessage());
                Debug.getInstance().info(this.getClass().getName() + " trying to load " + url.toExternalForm() + " as a stream.");
            }
            this.clip = new SampleStreaming();
        }
        linetest(this.clip.getDataLine(), 4);
        this.initialized = true;
        this.fireChange();
    }

    /**
   * <p>
   * Determines the amount of bytes in the given AudioInputStream. The given
   * stream is resetted afterwards. This method is needed, as AudioInputStreams
   * that have to be decoded (AudioSystem.getAudioInputStream(format, stream))
   * most often don't seem to return their correct frame lengths.
   * </p>
   * <p>
   * This method is a performance issues that could perhaps be improved: If
   * stream does not have to be decoded (it is a PCM wav and it's file size is
   * the correct byte size) only the file size might be sufficient.
   * </p>
   * 
   * @throws IOException
   * @throws UnsupportedAudioFileException
   * @throws LineUnavailableException
   * @throws MalformedURLException
   * 
   */
    private void findStreamSize(AudioInputStream decodeStream) throws MalformedURLException, IOException, LineUnavailableException, UnsupportedAudioFileException {
        long frames = decodeStream.getFrameLength();
        int framesize = decodeStream.getFormat().getFrameSize();
        if (frames != AudioSystem.NOT_SPECIFIED && framesize != AudioSystem.NOT_SPECIFIED) {
            decodedByteSize = (int) frames * framesize;
            this.postInit();
        } else {
            new Thread(this.loader).start();
        }
    }

    /**
   * <p>
   * Until now (06/2004) javasound is only able to play AudioInputStreams with a
   * format : PCM_SIGNED.
   * </p>
   * <p>
   * The internal {@link AudioInputStream}is conditionally (if it does not
   * match the format encoding PCM_UNSIGNED) tried to be converted to playable
   * formats.
   * </p>
   * 
   * 
   */
    protected AudioInputStream decode(AudioInputStream stream) throws IOException {
        AudioFormat format = stream.getFormat();
        AudioFormat.Encoding encoding = format.getEncoding();
        AudioInputStream ret;
        if (encoding != AudioFormat.Encoding.PCM_SIGNED) {
            try {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                ret = AudioSystem.getAudioInputStream(format, stream);
            } catch (Exception noConvert) {
                if (Debug.debug) {
                    Debug.getInstance().info(noConvert.getMessage());
                    Debug.getInstance().info("Trying 8 Bit sample-size conversion.");
                }
                format = new AudioFormat(format.getSampleRate(), 8, format.getChannels(), true, false);
                ret = AudioSystem.getAudioInputStream(format, stream);
            }
        } else {
            ret = stream;
        }
        return ret;
    }

    public String getName() {
        String ret = url.getFile();
        StringTokenizer tokens = new StringTokenizer(ret, "/");
        while (tokens.hasMoreTokens()) {
            ret = tokens.nextToken();
        }
        if (this.isInitialized()) {
            return ret;
        } else {
            return new StringBuffer(ret).append(" (" + this.getInitializationPercent() + " %)").toString();
        }
    }

    private Object readResolve() throws ObjectStreamException {
        try {
            this.init();
            if (Debug.debug) {
                Debug.getInstance().info("SampleProxy.readResolve()");
            }
            if (this.decodedByteSize == 0) {
                new Thread(new Loader(this)).start();
            }
            return this;
        } catch (Exception e) {
            if (Debug.debug) {
                Debug.getInstance().error(e.getMessage(), e);
            }
            throw new InvalidObjectException(e.getMessage());
        }
    }

    /**
   * @see java.applet.AudioClip#play()
   */
    public void play() throws LineUnavailableException {
        this.clip.play();
    }

    public void stop() {
        this.clip.stop();
    }

    /**
   * @see java.applet.AudioClip#loop()
   */
    public void loop() {
        this.clip.loop();
    }

    /**
   * Resets the clip to the start. Implies the call to stop, to avoid the
   * <i>MaMaMaMaMax HeHeHeHeHeadroom syndrome </i> which would include a lot of
   * gitters in javasound applications.
   */
    public void reset() {
        this.clip.reset();
    }

    public boolean isPlaying() {
        return clip.isPlaying();
    }

    public URL getUrl() {
        return this.url;
    }

    public void setVolume(float percent) {
        this.clip.setVolume(percent);
        this.volume = percent;
    }

    public float getVolume() {
        return this.volume;
    }

    /**
   * @param pan
   *          Has to be between -1.0 and +1.0.
   */
    public void setPan(float pan) {
        this.clip.setPan(pan);
        this.pan = pan;
    }

    public void close() {
        this.clip.close();
    }

    private static void linetest(Line l, int indent) {
        if (Debug.debug) {
            StringBuffer msg = new StringBuffer();
            msg.append(l.getLineInfo().toString() + " :");
            Control[] controls = l.getControls();
            String spaces = spaces(indent);
            msg.append(spaces + "Controls:");
            for (int i = 0; i < controls.length; i++) {
                msg.append(spaces + "  " + controls[i].toString());
                msg.append(spaces + "  type: " + controls[i].getType().toString());
            }
            Debug.getInstance().info(msg.toString());
        }
    }

    private static String spaces(int howmuch) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < howmuch; i++) ret.append(' ');
        return ret.toString();
    }

    /**
   * @return
   */
    public DataLine getDataLine() {
        return clip.getDataLine();
    }

    public final void addChangeListener(ChangeListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
            this.fireChange();
        }
    }

    public final boolean removeChangeListener(ChangeListener listener) {
        return this.listeners.remove(listener);
    }

    private final void fireChange() {
        Iterator it = this.listeners.iterator();
        ChangeListener change;
        ChangeEvent event = new ChangeEvent(this);
        while (it.hasNext()) {
            change = (ChangeListener) it.next();
            change.stateChanged(event);
        }
    }

    public final int getInitializationPercent() {
        if (this.isInitialized()) {
            return 100;
        } else if (this.loader != null) {
            return (int) ((100.0d) * this.loader.getProgress());
        } else return 100;
    }

    abstract class ASample implements ISample {

        public final boolean isInitialized() {
            return SampleProxy.this.isInitialized();
        }

        protected transient DataLine clip;

        /**
     * The clip control extracted to this class.
     */
        protected FloatControl gainControl = null;

        /**
     * The clip control extracted to this class.
     */
        protected FloatControl panControl = null;

        /**
     * We cannot use {@link javax.sound.sampled.DataLine#isRunning()}as it only
     * returns true, when the first data has been written to or read from the
     * line. A streaming thread needs to test before writing and would terminate
     * the loop before playing.
     */
        protected boolean isPlaying = false;

        public ASample() throws UnsupportedAudioFileException, IOException {
        }

        /**
     * @return The {@link java.net.URL}describing the sample's location.
     */
        public URL getUrl() {
            return url;
        }

        public boolean isPlaying() {
            boolean result = this.clip.isActive();
            return result;
        }

        public String getName() {
            return SampleProxy.this.getName();
        }

        public DataLine getDataLine() {
            return this.clip;
        }

        public void setPan(float pan) {
            try {
                panControl.setValue(pan);
            } catch (Exception ex) {
                if (Debug.debug) {
                    Debug.getInstance().error(ex.getMessage(), ex);
                }
            }
        }

        public void setVolume(float percent) {
            if (percent < 0.0 || percent > 1.0) {
                throw new IllegalArgumentException("Argument percent has to be between 0.0 and 1.0. Is: " + percent);
            }
            try {
                double range = amplitude(gainControl.getMaximum()) - amplitude(gainControl.getMinimum());
                double scaled = percent * range;
                scaled += amplitude(gainControl.getMinimum());
                gainControl.setValue(db(scaled));
                if (Debug.debug) {
                    Debug.getInstance().info(this.getClass().getName() + ".setVolume(" + percent + "): scaled to db is: " + db(scaled));
                }
            } catch (Exception ex) {
                if (Debug.debug) {
                    Debug.getInstance().error(ex.getMessage(), ex);
                }
            }
        }

        private double amplitude(float db) {
            return Math.pow(10.0, db / 20.0);
        }

        private float db(double amplitude) {
            return (float) (Math.log(amplitude == 0.0 ? 0.0001 : amplitude) / Math.log(10.0) * 20.0);
        }

        public float getVolume() {
            if (Debug.debug) {
                Debug.getInstance().info(this.getClass().getName() + ".getVolume()");
            }
            float db = gainControl.getValue() + gainControl.getMinimum();
            float range = gainControl.getMaximum() - gainControl.getMinimum();
            db /= range;
            if (Debug.debug) {
                Debug.getInstance().info(this.getClass().getName() + ".getVolume()" + db);
            }
            return db;
        }

        /**
     * @return The potentially decoded AudioInputStream.
     * 
     * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann </a>
     */
        public AudioInputStream getStream() {
            return decoded;
        }

        public final void addChangeListener(ChangeListener listener) {
            SampleProxy.this.addChangeListener(listener);
        }

        public final boolean removeChangeListener(ChangeListener listener) {
            return SampleProxy.this.removeChangeListener(listener);
        }
    }

    class SampleStreaming extends ASample {

        private InputStreamWatcher posStat;

        public SampleStreaming() throws MalformedURLException, IOException, LineUnavailableException, UnsupportedAudioFileException {
            super();
            this.init();
        }

        private void init() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
            AudioInputStream stream = this.getStream();
            AudioFormat decodeFormat = stream.getFormat();
            this.posStat = new InputStreamWatcher(stream, decodedByteSize);
            SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, decodeFormat);
            this.clip = (SourceDataLine) AudioSystem.getLine(info);
            ((SourceDataLine) this.clip).open();
            this.gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            this.panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
        }

        public void loop() {
        }

        public void play() throws LineUnavailableException {
            if (Debug.debug) {
                Debug.getInstance().info(this.getName() + ".play(): streamPos: " + this.posStat.getPosition() + " total: " + decodedByteSize);
            }
            if (this.posStat.getPosition() >= decodedByteSize) {
                this.reset();
            }
            final AudioInputStream stream = this.getStream();
            this.isPlaying = true;
            this.clip.start();
            this.setVolume(SampleProxy.this.volume);
            new Thread(new Runnable() {

                public void run() {
                    int numRead = 0;
                    long total = decodedByteSize;
                    int frameScalar = 0;
                    AudioFormat decoded = stream.getFormat();
                    double percent = 0.0;
                    DecimalFormat twoDigits = new DecimalFormat("0.00");
                    byte[] buf = new byte[SampleStreaming.this.clip.getBufferSize()];
                    try {
                        if (Debug.debug) {
                            Debug.getInstance().info("Play: Starting data transport.");
                        }
                        outer: while (true) {
                            synchronized (SampleStreaming.this) {
                                numRead = SampleStreaming.this.posStat.read(buf, 0, buf.length);
                                frameScalar++;
                                if (Debug.debug) {
                                    if (frameScalar % 100 == 0) {
                                        percent = 100.0d * (SampleStreaming.this.posStat.getRelativePosition());
                                        Debug.getInstance().info("Read position: " + SampleStreaming.this.posStat.getPosition() + " of: " + total + ", " + twoDigits.format(percent) + " %");
                                    }
                                }
                            }
                            if (numRead < 0) {
                                if (Debug.debug) {
                                    Debug.getInstance().info("Play: Stopping transport (0 bytes read).");
                                    SampleStreaming.this.isPlaying = false;
                                    Debug.getInstance().info("Read: " + SampleStreaming.this.posStat.getPosition() + " bytes, decodedSize: " + decodedByteSize);
                                }
                                break outer;
                            }
                            if (!SampleStreaming.this.isPlaying()) {
                                synchronized (SampleStreaming.this) {
                                    if (Debug.debug) {
                                        Debug.getInstance().info("Play: Thread was stopped.");
                                        Debug.getInstance().info("Play: Stopping data transport.");
                                    }
                                }
                                break outer;
                            }
                            int offset = 0;
                            synchronized (SampleStreaming.this) {
                                while (offset < numRead) {
                                    offset += ((SourceDataLine) SampleStreaming.this.clip).write(buf, offset, numRead - offset);
                                }
                            }
                        }
                    } catch (IOException ioex) {
                        if (Debug.debug) {
                            Debug.getInstance().error(ioex.getMessage(), ioex);
                        }
                    }
                    if (SampleStreaming.this.isPlaying()) {
                        SampleStreaming.this.clip.drain();
                    }
                }
            }).start();
        }

        /**
     * @see jbuzzer.audio.SampleProxy.ASample#isPlaying()
     */
        @Override
        public boolean isPlaying() {
            return this.isPlaying;
        }

        public synchronized void reset() {
            if (Debug.debug) {
                Debug.getInstance().info("reset()");
            }
            try {
                SampleProxy.this.undecoded = AudioSystem.getAudioInputStream(SampleProxy.this.url);
                SampleProxy.this.decoded = SampleProxy.this.decode(SampleProxy.this.undecoded);
                this.posStat = new InputStreamWatcher(SampleProxy.this.decoded, decodedByteSize);
            } catch (Exception e) {
                if (Debug.debug) {
                    Debug.getInstance().error("reset(): Failure.", e);
                }
            }
        }

        public synchronized void stop() {
            this.isPlaying = false;
            this.clip.stop();
        }

        public synchronized void close() {
            this.stop();
            this.clip.flush();
            this.clip.close();
        }
    }

    public class SampleClip extends ASample {

        public SampleClip() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
            super();
            AudioInputStream stream = this.getStream();
            AudioFormat decodeFormat = stream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, decodeFormat, (int) decodedByteSize);
            this.clip = (Clip) AudioSystem.getLine(info);
            ((Clip) this.clip).open(stream);
            this.gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            this.panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
            this.setVolume(SampleProxy.this.volume);
        }

        public void loop() {
            ((Clip) this.clip).loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
        }

        public void play() {
            this.reset();
            this.clip.start();
            this.isPlaying = true;
        }

        public void reset() {
            if (this.clip != null) {
                ((Clip) this.clip).setMicrosecondPosition(0);
            }
        }

        public void stop() {
            this.clip.stop();
            this.isPlaying = false;
        }

        public synchronized void close() {
            this.clip.close();
            this.isPlaying = false;
        }
    }

    static class Loader implements Runnable {

        private SampleProxy client;

        private InputStreamWatcher watcher;

        private static int loaders = 0;

        Loader(SampleProxy prxy) throws FileNotFoundException {
            this.client = prxy;
            this.watcher = new InputStreamWatcher(new File(this.client.getUrl().getFile()));
        }

        public double getProgress() {
            return this.watcher.getRelativePosition();
        }

        public void run() {
            while (loaders >= 2) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            loaders++;
            if (Debug.debug) {
                Debug.getInstance().info(this.toString() + " starts scanning " + this.client.url.toString());
            }
            AudioInputStream stream;
            long ret = 0;
            try {
                stream = AudioSystem.getAudioInputStream(this.watcher);
                stream = this.client.decode(stream);
                long read = 0;
                byte[] buf = new byte[131072];
                int steps = 0;
                double percent = 0.0;
                double oldPercent = 0.0;
                while ((read = stream.read(buf)) >= 0) {
                    ret += read;
                    steps++;
                    percent = watcher.getRelativePosition();
                    if (percent - oldPercent > .01) {
                        oldPercent = percent;
                        this.client.fireChange();
                    }
                    if (steps % (Math.max(40 / loaders, 1)) == 0) {
                        Thread.sleep(100);
                    }
                }
            } catch (Throwable e) {
                if (Debug.debug) {
                    Debug.getInstance().error("Error message: " + String.valueOf(e.getMessage()) + " Therfore the byte size wil be incorrect and we cannot seek.", e);
                    ret = 1024 * 16384;
                }
            } finally {
                if (ret == 0) {
                    ret = 1024 * 16384;
                }
                if (Debug.debug) {
                    Debug.getInstance().info(this.toString() + " scanned " + ret + " bytes in " + this.client.url.toString());
                    if (ret == 0) {
                        Debug.getInstance().warn("This cannot be correct. Defaulting to 2MB default. Seeking may be incorrect.");
                    }
                }
                client.decodedByteSize = ret;
                loaders--;
                if (Debug.debug) {
                    Debug.getInstance().info("Estimated size of " + this.client.url.toString() + " : " + client.decodedByteSize);
                }
                try {
                    client.postInit();
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (LineUnavailableException e1) {
                    e1.printStackTrace();
                } catch (UnsupportedAudioFileException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            URL baseurl = new File("C:/data/media/sound/own/").toURL();
            System.out.println("Trying a clip:");
            URL snd2 = new URL(baseurl, "samples/drum/combined/intro2.wav");
            SampleProxy test = new SampleProxy(snd2);
            System.out.println("play");
            test.play();
            Thread.sleep(6000);
            System.out.println("reset");
            test.reset();
            System.out.println("play");
            test.play();
            Thread.sleep(1000);
            System.out.println("stop");
            test.stop();
            Thread.sleep(4000);
            System.out.println("play");
            test.play();
            Thread.sleep(4000);
            System.out.println("Trying a stream:");
            URL snd1 = new URL(baseurl, "burn.wav");
            test = new SampleProxy(snd1);
            System.out.println("play");
            test.setVolume(1.0f);
            test.play();
            Thread.sleep(12000);
            System.out.println("reset");
            test.reset();
            System.out.println("did a reset");
            Thread.sleep(6000);
            System.out.println("play");
            test.play();
            Thread.sleep(6000);
            System.out.println("stop");
            test.stop();
            Thread.sleep(6000);
            System.out.println("play");
            test.play();
            Thread.sleep(6000);
            System.out.println("stop");
            test.stop();
            Thread.sleep(6000);
            System.out.println("play");
            test.play();
            Thread.sleep(6000);
        } catch (Throwable f) {
            f.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
}
