package strudle;

import javax.sound.sampled.AudioFormat;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

public class WaveCanvas extends FigureCanvas {

    private WavePaintListener pl;

    int curX = 0;

    int selectionCur = 0;

    private int waveLength;

    AudioFormat af;

    Polyline lines;

    RectangleFigure selectionRec;

    private int[] selection = new int[2];

    private int accuracy;

    private int canvasLength;

    public WaveCanvas(Composite parent, int style, AudioFormat af) {
        super(parent, style);
        pl = new WavePaintListener();
        pl.setFrameRate(af.getFrameRate());
        addPaintListener(pl);
        this.af = af;
    }

    public void addSelection(int[] sel) {
    }

    public WavePaintListener getPaintListener() {
        return pl;
    }

    public int setSelectionStart(int start) {
        return selection[0] = start * this.getFramesPerPixel();
    }

    public int setSelectionEnd(int end) {
        return selection[1] = end * this.getFramesPerPixel();
    }

    public void setSelectionCur(int x) {
        if (x <= canvasLength) {
            selectionCur = x;
        } else {
            selectionCur = canvasLength;
        }
    }

    public void setCurX(int x) {
        if (x <= canvasLength) curX = x;
    }

    public int getCurX() {
        return curX;
    }

    void setCanvasAxes() {
        int maxY = getSize().y;
        System.out.println("maxy: " + maxY);
        int maxX = (int) (getAudioDataLength() / af.getFrameRate() * accuracy);
        int halfY = (int) maxY / 2;
        Polyline asse = new Polyline();
        asse.addPoint(new Point(0, 0));
        asse.addPoint(new Point(0, maxY));
        for (int i = 0; i <= (maxX / accuracy); i++) {
            asse.addPoint(new Point((int) (i * accuracy), halfY));
            asse.addPoint(new Point((int) (i * accuracy), halfY - 7));
            asse.addPoint(new Point((int) (i * accuracy), halfY));
            Label lbl = new Label();
            lbl.setText(i + " s");
            lbl.setLocation(new Point((int) (i * accuracy + 3), halfY + 7));
            lbl.setEnabled(true);
            lbl.setSize(30, 30);
            setContents(lbl);
        }
        System.out.println("Maxx= " + maxX);
        asse.addPoint(new Point(maxX, halfY));
        asse.addPoint(new Point(maxX, halfY - 7));
        setContents(asse);
        this.canvasLength = maxX;
        System.out.println("Il valore di canvasLength � " + canvasLength);
        System.out.println("frame/pixel: " + this.waveLength / this.canvasLength);
    }

    public void setClipLength(long l, int accuracy) {
        this.accuracy = accuracy;
        this.waveLength = (int) (l);
        System.out.println("Il valore di wavelength � " + waveLength);
        setCanvasAxes();
    }

    public void createWaveform(int[] audioData, int accuracy) {
        this.accuracy = accuracy;
        int w = audioData.length / accuracy;
        int h = getSize().y;
        int frames_per_pixel = audioData.length / af.getFrameSize() / w;
        this.waveLength = (audioData.length) / accuracy;
        System.out.println(w + " e anche dim " + this.waveLength);
        byte my_byte = 0;
        lines = new Polyline();
        lines.addPoint(new Point(0, 0));
        int numChannels = af.getChannels();
        for (double x = 0; x < w && audioData != null; x++) {
            int idx = (int) (frames_per_pixel * numChannels * x);
            if (af.getSampleSizeInBits() == 8) {
                my_byte = (byte) audioData[idx];
            } else {
                my_byte = (byte) (128 * audioData[idx] / 32768);
            }
            double y_new = (double) (h * (128 - my_byte) / 256);
            lines.addPoint(new Point(x, y_new));
        }
        setContents(lines);
    }

    public void incrementSelection(int x) {
        if (waveLength - selection[0] >= x) selection[0] += x;
    }

    public void decrementCurX(int x) {
        if (curX - x >= 0) curX -= x;
    }

    public void setSelection(int start, int end) {
        selectionRec = new RectangleFigure();
        Rectangle rect = new Rectangle();
        rect.x = start;
        rect.y = 0;
        rect.width = end - start;
        rect.height = getSize().y;
        selectionRec.setBackgroundColor(new Color(getDisplay(), 123, 166, 200));
        selectionRec.setBounds(rect);
    }

    int getFramesPerPixel() {
        return this.waveLength / this.canvasLength;
    }

    public int getAudioDataLength() {
        return waveLength;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public int getSelectionStart() {
        return selection[0];
    }

    public int getSelectionEnd() {
        return selection[1];
    }

    int getCanvasLength() {
        return this.canvasLength;
    }
}
