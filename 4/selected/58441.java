package strudle.gui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Valce
 */
@SuppressWarnings("serial")
public class SoundViewerComponent extends JComponent implements Observer, ChangeListener {

    public double starttime;

    private double duration;

    public double zoomX = 1.0;

    public double zoomY = 1.0;

    public int zoomXPower = 0;

    private String filename = null;

    private int channels = 0;

    private int samplesize = 0;

    private int framesize = 0;

    private double framepersec = 0;

    private double maxsamplevalue = 0;

    private Image img = null;

    private TimeRuler ruler = null;

    private JScrollBar scrollbar = null;

    private PaintThread painter = null;

    private Viewport views[] = null;

    private class Viewport extends Rectangle {

        public double transformX(double X) {
            return x + (X - starttime) * zoomX * width / duration;
        }

        public double transformY(double Y) {
            return y + ((-Y * zoomY / maxsamplevalue) + 1) * height / 2.0;
        }

        public double getPercent(double percent) {
            return percent * maxsamplevalue / 100.0;
        }
    }

    private class TimeRuler extends JComponent {

        public int step = 10;

        public double substep = 0.01;

        public double calcOptimalSubStep(Graphics g) {
            if (g == null) g = getGraphics();
            FontMetrics fm = g.getFontMetrics();
            Viewport v = views[0];
            double substep = 0.0001;
            int print_width = fm.stringWidth(getTimeString(0));
            while (v.transformX(substep * step) - v.x < print_width + 20) substep += 0.0001;
            return substep;
        }

        public String getTimeString(double time) {
            int hour = (int) (time / 3600);
            time -= hour * 3600;
            int minute = (int) (time / 60);
            time -= minute * 60;
            int second = (int) time;
            int mill = (int) ((time - (int) time) * 1000);
            return String.format("%02d:%02d:%02d.%03d", hour, minute, second, mill);
        }

        protected void paintComponent(Graphics g) {
            Insets insets = getInsets();
            Rectangle client = new Rectangle(insets.left, insets.top, getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
            int maxy = (int) client.getMaxY() - 1;
            g.setColor(getBackground());
            g.fillRect(client.x, client.y, client.width, client.height);
            g.setColor(SystemColor.controlShadow);
            g.drawLine(client.x, maxy, (int) client.getMaxX(), maxy);
            if (substep > 0) {
                boolean print_time = true;
                int stepCounter = step;
                substep = calcOptimalSubStep(g);
                double endtime = starttime + duration / zoomX;
                for (double time = starttime; time < endtime; time += substep, time = Math.round(time * 10000) / 10000.0) {
                    int x = (int) views[0].transformX(time);
                    int y = maxy;
                    if (stepCounter++ < step) {
                        g.setColor(SystemColor.controlText);
                        g.drawLine(x, y - 2, x, y);
                        g.setColor(SystemColor.window);
                        g.drawLine(x + 1, y - 2, x + 1, y);
                    } else {
                        stepCounter = 1;
                        g.setColor(SystemColor.window);
                        g.drawLine(x + 1, y - 6, x + 1, y);
                        g.setColor(SystemColor.controlText);
                        g.drawLine(x, y - 6, x, y);
                        print_time = true;
                    }
                    if (print_time) {
                        print_time = false;
                        g.setColor(SystemColor.controlText);
                        g.drawString(getTimeString(time), x + 2, y - 4);
                    }
                }
            }
        }
    }

    private class PaintThread extends Observable implements Runnable {

        private Thread th = null;

        private boolean stop = false;

        public boolean isAlive() {
            return th != null && th.isAlive();
        }

        public void start() {
            if (th != null) {
                stop = true;
                try {
                    while (th.isAlive()) Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stop = false;
            th = new Thread(this);
            th.start();
        }

        public void drawLevel(Graphics g, Viewport v, double level, Color color) {
            g.setColor(color);
            int y = (int) v.transformY(level);
            g.drawLine(v.x, y, v.width - 1, y);
        }

        public void run() {
            Graphics g = img.getGraphics();
            Rectangle client = new Rectangle(0, 0, img.getWidth(null), img.getHeight(null));
            g.setColor(Color.white);
            g.fillRect(client.x, client.y, client.width, client.height);
            if (views == null) return;
            for (int index = 0; index < views.length; index++) {
                Viewport v = views[index];
                if (v == null) views[index] = v = new Viewport();
                int height = client.height / channels;
                v.setRect(0, index * height, client.width, height);
                if (index > 0) {
                    g.setColor(Color.black);
                    g.drawLine(v.x, v.y, v.width - 1, v.y);
                }
                drawLevel(g, v, 0, Color.blue);
                drawLevel(g, v, v.getPercent(50), Color.lightGray);
                drawLevel(g, v, v.getPercent(-50), Color.lightGray);
            }
            if (filename == null) return;
            try {
                g.setColor(Color.black);
                AudioInputStream audio = AudioSystem.getAudioInputStream(new FileInputStream(filename));
                long frames = audio.getFrameLength();
                int stepFrames = (int) (frames / zoomX / (client.width * 4));
                if (stepFrames < 1) stepFrames = 1;
                byte buf0[] = new byte[framesize];
                byte buf[] = new byte[framesize];
                int iChannel;
                double time = starttime;
                double time0 = -1;
                double endtime = starttime + duration / zoomX;
                audio.skip((long) (time * framepersec));
                while (audio.read(buf) != -1 && time < endtime && !stop) {
                    iChannel = channels;
                    while (iChannel-- > 0) {
                        Viewport v = views[iChannel];
                        g.drawLine((int) v.transformX(time0), (int) v.transformY(extractSample(buf0, iChannel)), (int) v.transformX(time), (int) v.transformY(extractSample(buf, iChannel)));
                    }
                    audio.skip((stepFrames - 1) * framesize);
                    time0 = time;
                    System.arraycopy(buf, 0, buf0, 0, buf.length);
                    time += stepFrames / framepersec;
                }
                audio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
            setChanged();
            notifyObservers();
        }
    }

    public SoundViewerComponent() {
        initialize();
    }

    public SoundViewerComponent(String filename) {
        openAudioFile(filename);
    }

    protected void initialize() {
        starttime = 0;
        zoomXPower = 0;
        zoomX = 1;
        zoomY = 1;
        painter = new PaintThread();
        painter.addObserver(this);
        add(getScrollBar());
        add(getTimeRuler());
        if (filename != null) {
            try {
                AudioInputStream audio = AudioSystem.getAudioInputStream(new FileInputStream(filename));
                AudioFormat format = audio.getFormat();
                channels = format.getChannels();
                samplesize = format.getSampleSizeInBits();
                framesize = format.getFrameSize();
                framepersec = format.getSampleRate();
                duration = audio.getFrameLength() / framepersec;
                audio.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            channels = 2;
            samplesize = 16;
            framesize = 4;
            framepersec = 44100;
            duration = 44100 / framepersec;
        }
        maxsamplevalue = Math.pow(2, samplesize - 1);
        views = new Viewport[channels];
        for (int i = 0; i < channels; i++) views[i] = new Viewport();
        enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        invalidate();
    }

    public void openAudioFile(String filename) {
        this.filename = filename;
        initialize();
    }

    public int extractSample(byte frame[], int channel) {
        switch(samplesize) {
            case 8:
                return (frame[channel] & 0xFF) - 127;
            case 16:
                return (frame[channel * 2 + 1] << 8) + (frame[channel * 2] & 0xFF);
            case 24:
                return (frame[channel * 3 + 2] << 16) + (frame[channel * 3 + 1] << 8) + (frame[channel * 3] & 0xFF);
            case 32:
                return (frame[channel * 4 + 3] << 24) + (frame[channel * 3 + 2] << 16) + (frame[channel * 3 + 1] << 8) + (frame[channel * 3] & 0xFF);
        }
        return 0;
    }

    public TimeRuler getTimeRuler() {
        if (ruler == null) ruler = new TimeRuler();
        return ruler;
    }

    public boolean isShowRuler() {
        return getTimeRuler().isVisible();
    }

    public void setShowRuler(boolean value) {
        getTimeRuler().setVisible(value);
    }

    public JScrollBar getScrollBar() {
        if (scrollbar == null) {
            scrollbar = new JScrollBar();
            scrollbar.setOrientation(SwingConstants.HORIZONTAL);
            scrollbar.getModel().addChangeListener(this);
        }
        return scrollbar;
    }

    public boolean isShowScrollbar() {
        return getScrollBar().isVisible();
    }

    public void setShowScrollbar(boolean value) {
        if (isShowScrollbar() != value) {
            getScrollBar().setVisible(value);
            if (painter != null) painter.start();
        }
    }

    public Insets getInsets() {
        Insets insets = super.getInsets();
        if (ruler.isVisible()) insets.top += ruler.getHeight();
        if (scrollbar.isVisible()) insets.bottom += scrollbar.getHeight();
        return insets;
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        update(width, height);
    }

    public String getFilename() {
        return filename;
    }

    public int getChannels() {
        return channels;
    }

    public double getDuration() {
        return duration;
    }

    public double getSampleRate() {
        return framepersec;
    }

    public boolean isFocusable() {
        return true;
    }

    public void update() {
        update(getWidth(), getHeight());
    }

    public void update(Graphics g) {
    }

    public void update(Observable o, Object arg) {
        repaint();
    }

    public void update(int width, int height) {
        Insets insets = super.getInsets();
        int Width = width - (insets.left + insets.right);
        int Height = height - (insets.top + insets.bottom);
        if (scrollbar.isVisible()) {
            scrollbar.setBounds(insets.left, insets.top + Height - 18, Width, 18);
            Height -= scrollbar.getHeight();
        }
        if (ruler.isVisible()) {
            ruler.setBounds(insets.left, insets.top, Width, 18);
            Height -= ruler.getHeight();
        }
        if (img == null || (img != null && (img.getWidth(null) != Width || img.getHeight(null) != Height))) {
            try {
                img = new BufferedImage(Width, Height, BufferedImage.TYPE_USHORT_555_RGB);
                MediaTracker mt = new MediaTracker(this);
                mt.addImage(img, 1);
                mt.waitForAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (zoomX >= 1) scrollbar.setMaximum((int) (zoomX / 100));
        setShowScrollbar(zoomXPower > 0);
        painter.start();
    }

    public void paintComponent(Graphics g) {
        g.drawImage(img, 0, ruler.isVisible() ? ruler.getHeight() : 0, null);
    }

    protected void processFocusEvent(FocusEvent e) {
        switch(e.getID()) {
            case FocusEvent.FOCUS_GAINED:
                break;
            case FocusEvent.FOCUS_LOST:
                break;
        }
        super.processFocusEvent(e);
    }

    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
    }

    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
    }

    protected void processMouseWheelEvent(MouseWheelEvent e) {
        int zoomOldPower = zoomXPower;
        zoomXPower -= e.getWheelRotation();
        if (zoomXPower < 0) zoomXPower = 0;
        if (zoomXPower != zoomOldPower) {
            zoomX = Math.pow(2, zoomXPower);
            update();
        }
        super.processMouseWheelEvent(e);
    }

    public void stateChanged(ChangeEvent e) {
        starttime = scrollbar.getValue() * duration / zoomX / 100;
        update();
    }
}
