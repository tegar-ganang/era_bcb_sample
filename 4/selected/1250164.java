package com.frinika.sequencer.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import javax.sound.sampled.AudioFileFormat;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.server.AudioServer;
import com.frinika.global.FrinikaConfig;
import com.frinika.project.ProjectContainer;
import com.frinika.project.gui.ProjectFrame;
import com.frinika.sequencer.gui.ItemPanel;
import com.frinika.sequencer.gui.menu.AudioAnalysisAction;
import com.frinika.sequencer.gui.partview.PartView;
import com.frinika.audio.io.AudioReader;
import com.frinika.audio.io.BufferedRandomAccessFile;
import com.frinika.sequencer.model.audio.AudioStreamVoice;
import com.frinika.sequencer.model.audio.EnvelopedAudioReader;
import com.frinika.audio.io.AudioReaderFactory;
import com.frinika.audio.io.VanillaRandomAccessFile;

public class AudioPart extends Part implements AudioReaderFactory {

    private static final long serialVersionUID = 1L;

    private String audioFileName;

    private String audioDir;

    /**
	 * Start time relative to sequencer zero time (in microseconds)
	 */
    double realStartTimeInMicros = 0;

    transient AudioStreamVoice outputProcess = null;

    transient int nChannel;

    private transient Image thumbNailImage = null;

    private transient AudioReader thumbNailIn = null;

    private transient int buffSize;

    private transient ThumbNailRunnable thumbNailRunnable;

    static AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;

    transient EnvelopedAudioReader audioPlayerIn;

    Envelope envelope;

    public AudioPart(AudioLane lane) {
        super(lane);
        init();
    }

    public AudioPart() {
        init();
    }

    private void init() {
        realStartTimeInMicros = 0;
        outputProcess = null;
        nChannel = 1;
        buffSize = 100000;
    }

    /**
	 * Creates a new AudioPart. To use the AudioPart call onLoad() (normally
	 * done be the AudioLane).
	 * 
	 * @param lane
	 *            add part to this lane. Can be null for a detached part.
	 * @param clipFile
	 *            .wav file of audio
	 * @param startTimeInMicros
	 *            postition first sample in micros
	 * 
	 */
    public AudioPart(Lane lane, File clipFile, double startTimeInMicros) {
        super(lane);
        init();
        audioDir = clipFile.getParent();
        audioFileName = clipFile.getName();
        realStartTimeInMicros = startTimeInMicros;
        if (!clipFile.exists()) {
            try {
                System.err.println(" Missing audio file " + clipFile.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            clipFile = null;
        }
        createFileHandles(clipFile);
    }

    private void createFileHandles(File clipFile) {
        AudioServer audioServer = lane.getProject().getAudioServer();
        boolean newEnvelope = envelope == null;
        if (newEnvelope) {
            envelope = new Envelope();
        }
        double lengthInMicros = 0;
        if (!(clipFile == null)) {
            try {
                RandomAccessFile raf = new RandomAccessFile(clipFile, "r");
                BufferedRandomAccessFile braf = new BufferedRandomAccessFile(raf, buffSize, lane.getProject().getAudioFileManager());
                audioPlayerIn = new EnvelopedAudioReader(braf, FrinikaConfig.sampleRate);
                RandomAccessFile rafG = new RandomAccessFile(clipFile, "r");
                thumbNailIn = new AudioReader(new VanillaRandomAccessFile(rafG), FrinikaConfig.sampleRate);
                if (audioPlayerIn.getFormat().getSampleRate() != FrinikaConfig.sampleRate) {
                    try {
                        throw new Exception(" unsupport format " + audioPlayerIn.getFormat());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                lengthInMicros = audioPlayerIn.getLengthInFrames() / audioPlayerIn.getFormat().getSampleRate() * 1000000.0;
                System.out.println("audioPart:" + clipFile + " " + lengthInMicros / 1000000.0 + " secs");
                outputProcess = new AudioStreamVoice(audioServer, lane.getProject().getSequencer(), audioPlayerIn, (long) realStartTimeInMicros);
                nChannel = audioPlayerIn.getFormat().getChannels();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        envelope.setMaxTime(lengthInMicros);
        if (newEnvelope) {
            envelope.setTOn(0.0);
            envelope.setTOff(lengthInMicros);
        }
        refreshEnvelope();
    }

    public void refreshEnvelope() {
        if (envelope == null) return;
        envelope.validate();
        if (audioPlayerIn == null) return;
        audioPlayerIn.setEvelope(envelope);
        thumbNailIn.setBoundsInMicros(envelope.tOn, envelope.tOff);
    }

    public File getAudioFile() {
        return new File(audioDir, audioFileName);
    }

    @Override
    protected void moveItemsBy(long deltaTick) {
        assert (false);
    }

    @Override
    public void addToModel() {
        super.addToModel();
        System.out.println(" Adding " + this + " to model ");
        try {
            onLoad();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            removeFromModel();
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        AudioPart clone = new AudioPart();
        clone.audioDir = audioDir;
        clone.audioFileName = audioFileName;
        clone.realStartTimeInMicros = realStartTimeInMicros;
        clone.envelope = (Envelope) envelope.clone();
        File clipFile = new File(audioDir, audioFileName);
        return clone;
    }

    public void restoreFromClone(EditHistoryRecordable object) {
        AudioPart clone = (AudioPart) object;
        audioDir = clone.audioDir;
        audioFileName = clone.audioFileName;
        realStartTimeInMicros = clone.realStartTimeInMicros;
        envelope = clone.envelope;
        File clipFile = new File(audioDir, audioFileName);
    }

    @Override
    public void copyBy(double tick, Lane dst) {
        AudioPart clone;
        try {
            clone = new AudioPart((AudioLane) dst, new File(audioDir, audioFileName), (long) realStartTimeInMicros);
            clone.envelope = (Envelope) envelope.clone();
            clone.onLoad();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Selectable deepCopy(Selectable parent) {
        AudioPart clone = new AudioPart((AudioLane) parent);
        clone.audioDir = audioDir;
        clone.audioFileName = audioFileName;
        clone.realStartTimeInMicros = realStartTimeInMicros;
        clone.envelope = (Envelope) envelope.clone();
        if (parent == null) {
            clone.lane = lane;
        }
        clone.color = color;
        return clone;
    }

    public void deepMove(long dTick) {
        double tick1 = lane.getProject().tickAtMicros(realStartTimeInMicros);
        realStartTimeInMicros = (long) lane.getProject().microsAtTick(tick1 + dTick);
    }

    /**
	 * 
	 * @return start of the part in ticks
	 */
    public long getStartTick() {
        return (long) lane.getProject().getTempoList().getTickAtTime((realStartTimeInMicros + envelope.tOn) / 1000000.0);
    }

    /**
	 * 
	 * @return end tick of the part
	 */
    public long getEndTick() {
        return (long) lane.getProject().getTempoList().getTickAtTime((realStartTimeInMicros + envelope.tOff) / 1000000.0);
    }

    /**
	 * 
	 * @return length of the part in secs
	 */
    public double getDurationInSecs() {
        return (envelope.tOff - envelope.tOn) / 1000000.0;
    }

    /**
	 * 
	 * @return start of the part in secs
	 */
    public double getStartInSecs() {
        return (realStartTimeInMicros + envelope.tOn) / 1000000.0;
    }

    /**
	 * 
	 * @return end  of the part in secs
	 */
    public double getEndInSecs() {
        return (realStartTimeInMicros + envelope.tOff) / 1000000.0;
    }

    public void commitEventsRemove() {
    }

    public void commitEventsAdd() {
        refreshEnvelope();
    }

    public void onLoad() throws FileNotFoundException {
        File clipFile = new File(audioDir, audioFileName);
        if (!clipFile.exists()) {
            try {
                System.err.println(" Missing audio file " + clipFile.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            clipFile = null;
        }
        createFileHandles(clipFile);
    }

    /**
	 * A non realtime Reader for a raw view at the data.
	 * 
	 * @return
	 * @throws IOException
	 */
    public AudioReader createAudioReader() throws IOException {
        File clipFile = new File(audioDir, audioFileName);
        VanillaRandomAccessFile rafG = new VanillaRandomAccessFile(new RandomAccessFile(clipFile, "r"));
        AudioReader reader = new AudioReader(rafG, FrinikaConfig.sampleRate);
        reader.setBoundsInMicros(envelope.tOn, envelope.tOff);
        return reader;
    }

    private void reconstructThumbNail(Rectangle rect, PartView panel) {
        if (thumbNailRunnable == null) {
            thumbNailRunnable = new ThumbNailRunnable();
            Thread t = new Thread(thumbNailRunnable);
            t.setPriority(Thread.MIN_PRIORITY);
            thumbNailRunnable.setThread(t);
            t.start();
        }
        thumbNailRunnable.reconstruct(rect, panel);
    }

    transient Rectangle lastRect = null;

    public void drawThumbNail(Graphics2D g, Rectangle rect, PartView panel) {
        if (outputProcess == null || thumbNailIn == null) return;
        if (thumbNailImage == null || lastRect == null || rect.width != lastRect.width || rect.height != lastRect.height) {
            lastRect = (Rectangle) rect.clone();
            reconstructThumbNail(lastRect, panel);
        }
        g.setXORMode(Color.WHITE);
        g.drawImage(thumbNailImage, rect.x, rect.y, null);
        g.setPaintMode();
    }

    public void moveContentsBy(double dTick, Lane dstLane) {
        double tick1 = lane.getProject().tickAtMicros(realStartTimeInMicros);
        realStartTimeInMicros = (long) lane.getProject().microsAtTick(tick1 + dTick);
        if (dstLane != lane) {
            lane.getParts().remove(this);
            dstLane.getParts().add(this);
            lane = dstLane;
        }
        outputProcess.setRealStartTime((long) realStartTimeInMicros);
    }

    public String toString() {
        String ret = hashCode() + ":" + audioDir + "/" + audioFileName + "|" + realStartTimeInMicros;
        return ret;
    }

    /**
	 * @return output process to play audio
	 */
    public AudioProcess getAudioProcess() {
        return outputProcess;
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        init();
        in.defaultReadObject();
    }

    class ThumbNailRunnable implements Runnable {

        Rectangle rect;

        PartView panel;

        Thread runThread;

        Graphics2D gg;

        public void reconstruct(Rectangle rect, PartView panel) {
            this.rect = rect;
            this.panel = panel;
            if (runThread.isInterrupted()) {
                return;
            }
            runThread.interrupt();
        }

        public void setThread(Thread t) {
            runThread = t;
        }

        synchronized boolean buildThumbNail() {
            thumbNailImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_BYTE_BINARY);
            gg = (Graphics2D) thumbNailImage.getGraphics();
            int y = rect.height / 2;
            gg.drawString("...", 0, 5);
            panel.setDirty();
            double x = getStartInSecs();
            double w = getDurationInSecs();
            try {
                thumbNailIn.seekFrame(0, false);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            int nChannel = thumbNailIn.getFormat().getChannels();
            long nFrame = thumbNailIn.getLengthInFrames();
            ProjectContainer project = lane.getProject();
            double sampleToScreen = panel.userToScreen / FrinikaConfig.sampleRate;
            int chunkSize = 1024;
            AudioBuffer buff = new AudioBuffer("thumbnail", nChannel, chunkSize, 44100.0f);
            int nRead = 0;
            double valMax = 0;
            double valMin = 0;
            double scale = rect.height / 2.0;
            gg.setColor(Color.white);
            int midY = rect.height / 2;
            int pix = 0;
            int ii = 0;
            int cc = 0;
            try {
                thumbNailIn.seekTimeInMicros(envelope.tOn, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (nRead < nFrame) {
                if (runThread.isInterrupted()) {
                    return false;
                }
                int nn = chunkSize;
                if (nRead + chunkSize > nFrame) nn = (int) (nFrame - nRead);
                buff.makeSilence();
                thumbNailIn.processAudio(buff);
                nRead += nn;
                if (nChannel == 2) {
                    float left[] = buff.getChannel(0);
                    float right[] = buff.getChannel(1);
                    for (int i = 0; i < nn; i++, ii++) {
                        float sampleL = left[i];
                        float sampleR = right[i];
                        valMin = Math.min(Math.min(valMin, sampleL), sampleR);
                        valMax = Math.max(Math.max(valMax, sampleL), sampleR);
                        int pixNow = (int) (ii * sampleToScreen);
                        if (pixNow > pix) {
                            gg.drawLine(pix, (int) (midY + valMin * scale), pix, (int) (midY + valMax * scale));
                            pix = pixNow;
                            valMax = valMin = 0;
                        }
                    }
                } else {
                    float left[] = buff.getChannel(0);
                    for (int i = 0; i < nn; i++, ii++) {
                        float sampleL = left[i];
                        valMin = Math.min(valMin, sampleL);
                        valMax = Math.max(valMax, sampleL);
                        int pixNow = (int) (ii * sampleToScreen);
                        if (pixNow > pix) {
                            gg.drawLine(pix, (int) (midY + valMin * scale), pix, (int) (midY + valMax * scale));
                            pix = pixNow;
                            valMax = valMin = 0;
                        }
                    }
                }
            }
            Rectangle2D brect = gg.getFontMetrics().getStringBounds(audioFileName, gg);
            gg.setColor(Color.BLACK);
            gg.fillRect(1, 0, (int) brect.getWidth(), (int) brect.getHeight());
            gg.setColor(Color.WHITE);
            gg.drawString(audioFileName, 1, (int) brect.getHeight());
            panel.setDirty();
            panel.repaint();
            return true;
        }

        public synchronized void run() {
            assert (runThread == Thread.currentThread());
            while (true) {
                try {
                    wait();
                } catch (InterruptedException e1) {
                    while (!buildThumbNail()) Thread.interrupted();
                }
            }
        }
    }

    public void setStartTick(long tick) {
        envelope.setTOn(lane.getProject().microsAtTick(tick) - realStartTimeInMicros);
    }

    /**
	 * 
	 * @param tick
	 *            new end tick for display purpose only
	 */
    public void setEndTick(long tick) {
        envelope.setTOff(lane.getProject().microsAtTick(tick) - realStartTimeInMicros);
    }

    static double minT = 1;

    static int danc = 1;

    static Rectangle anc = new Rectangle(0, 0, 3 * danc, 3 * danc);

    public final class Envelope implements Serializable {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        double tOn;

        double tRise;

        double gain;

        double tOff;

        double tFall;

        transient double maxTime;

        public void validate() {
            assert (tOn <= tOff);
        }

        public Envelope() {
            gain = 1.0;
        }

        public double getGain() {
            return gain;
        }

        public void setGain(double gain) {
            System.out.println(" SET GAIN  " + gain);
            if (gain > 1.0) {
                this.gain = 1.0;
            } else if (gain < 0.0) {
                this.gain = 0.0;
            } else {
                this.gain = gain;
            }
        }

        public double getTOff() {
            return tOff;
        }

        public void setTOff(double off1) {
            tOff = Math.min(off1, maxTime);
        }

        public double getTOn() {
            return tOn;
        }

        public void setTOn(double on1) {
            tOn = Math.max(0, on1);
        }

        public Object clone() {
            Envelope clone = new Envelope();
            clone.tOn = tOn;
            clone.tOff = tOff;
            clone.gain = gain;
            clone.tRise = tRise;
            clone.tFall = tFall;
            clone.maxTime = maxTime;
            return clone;
        }

        @SuppressWarnings("unchecked")
        private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
            in.defaultReadObject();
        }

        public double getMaxTime() {
            return maxTime;
        }

        public void setMaxTime(double maxTime) {
            this.maxTime = maxTime;
        }

        public void draw(Graphics2D g, Rectangle rect, PartView view) {
            double xt = tOff - tOn;
            int x1 = rect.x;
            int y1 = rect.y + rect.height;
            int x4 = rect.x + rect.width;
            int y4 = y1;
            int x2 = (int) (x1 + tRise / xt * rect.width);
            int y2 = (int) (rect.y + rect.height * (1.0 - gain));
            int x3 = (int) (x4 - tFall / xt * rect.width);
            int y3 = y2;
            Stroke stroke = g.getStroke();
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.BLUE);
            g.drawLine(x1, y1, x2, y2);
            g.drawLine(x2, y2, x3, y3);
            g.drawLine(x3, y3, x4, y4);
            g.setStroke(stroke);
            g.setColor(Color.YELLOW);
            anc.setLocation(x2 - danc, y2 - danc);
            g.fill(anc);
            anc.setLocation(x3 - danc, y3 - danc);
            g.fill(anc);
        }

        public int getHoverState(Point p, Rectangle rect) {
            int tol = 4;
            double xt = tOff - tOn;
            int y2 = (int) (rect.y + (1.0 - gain) * rect.height);
            if (Math.abs(p.y - y2) > tol) return -1;
            int x1 = rect.x;
            int y1 = rect.y + rect.height;
            int x4 = rect.x + rect.width;
            int y4 = y1;
            int x2 = (int) (x1 + tRise / xt * rect.width);
            if (Math.abs(p.x - x2 - tol) < tol) return ItemPanel.OVER_ENVELOPE_LEFT;
            int x3 = (int) (x4 - tFall / xt * rect.width);
            int y3 = y2;
            if (Math.abs(p.x - x3 + tol) < tol) return ItemPanel.OVER_ENVELOPE_RIGHT;
            return ItemPanel.OVER_ENVELOPE_GAIN;
        }

        public void setTOffRel(double fact) {
            if (fact < 0.0 || fact > 1.0) return;
            tFall = (1.0 - fact) * (tOff - tOn);
        }

        public void setTOnRel(double fact) {
            if (fact < 0.0 || fact > 1.0) return;
            tRise = fact * (tOff - tOn);
        }

        public double getTFall() {
            return tFall;
        }

        public double getTRise() {
            return tRise;
        }

        public void setTFall(double fall) {
            tFall = fall;
        }

        public void setTRise(double rise) {
            tRise = rise;
        }
    }

    public void drawEnvelope(Graphics2D g, Rectangle rect, PartView view) {
        envelope.draw(g, rect, view);
    }

    public int getHoverState(Point p, Rectangle rect) {
        return envelope.getHoverState(p, rect);
    }

    public Envelope getEvelope() {
        return envelope;
    }

    /**
	 * To be extended by subclasses.
	 * 
	 * @param popup
	 */
    @Override
    protected void initContextMenu(final ProjectFrame frame, JPopupMenu popup) {
        JMenuItem item = new JMenuItem(new AudioAnalysisAction(frame));
        popup.add(item);
    }

    public void setStartInSecs(double start) {
        envelope.setTOn(start * 1000000.0 - realStartTimeInMicros);
    }

    public void setEndInSecs(double end) {
        envelope.setTOff(1000000.0 * end - realStartTimeInMicros);
    }
}
