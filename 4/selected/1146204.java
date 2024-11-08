package net.sf.soundcomp.ide.waveeditor.panel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.undo.UndoManager;
import net.sf.soundcomp.ide.ApplicationPanel;
import net.sf.soundcomp.ide.dialog.AudioColorDialog;
import net.sf.soundcomp.ide.dialog.ImageResizeDialog;
import org.xbup.library.audio.swing.XBWavePanel;
import org.xbup.library.audio.wave.XBWave;

/**
 * Audio Panel audio panel.
 *
 * @version 0.1.0 2011/03/19
 * @author SoundComp Project (http://soundcomp.sf.net)
 */
public class AudioPanel extends javax.swing.JPanel implements ApplicationPanel {

    public static int COMPAT_FILEMODE = 1;

    public static int XB_FILEMODE = 2;

    final UndoManager undo;

    private String fileName;

    private String ext;

    private javax.sound.sampled.AudioFileFormat.Type fileType;

    private int fileMode = 0;

    private boolean modified = false;

    PlayThread playThread;

    WavePaintThread wavePaintThread = null;

    private int scrollPosition;

    private double scaleRatio;

    private Object highlight;

    private Color selectionColor;

    private Color toolColor;

    private Color[] defaultColors;

    private InputMethodListener caretListener;

    private int toolMode;

    private XBWavePanel wavePanel;

    private SourceDataLine sourceDataLine;

    private AudioInputStream audioInputStream;

    private AudioFormat targetFormat;

    private DataLine.Info targetDataLineInfo;

    private int dataLinePosition;

    /** Creates new form AudioPanel */
    public AudioPanel() {
        initComponents();
        scaleRatio = 1;
        toolMode = 0;
        undo = new UndoManager();
        fileName = "";
        fileType = null;
        highlight = null;
        toolColor = Color.BLACK;
        selectionColor = Color.YELLOW;
        wavePanel = new XBWavePanel();
        wavePanel.addPositionSeekListener(new XBWavePanel.PositionSeekListener() {

            public void positionSeeked(int position) {
                if (sourceDataLine == null) return;
                if (sourceDataLine.isActive()) {
                    performPlay();
                    wavePanel.setCursorPosition(position);
                    performPlay();
                }
            }
        });
        sourceDataLine = null;
        scrollPane.setViewportView(wavePanel);
        scrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent evt) {
                if (sourceDataLine == null) return;
                if ((sourceDataLine.isRunning()) && ((scrollPosition != evt.getValue()) || (evt.getAdjustmentType() == AdjustmentEvent.ADJUSTMENT_LAST))) {
                    wavePanel.seekPosition(evt.getValue());
                }
            }
        });
        playThread = null;
        targetFormat = null;
        targetDataLineInfo = null;
        audioInputStream = null;
    }

    public void performCopy() {
        wavePanel.copy();
    }

    public void performCut() {
        wavePanel.cut();
    }

    public void performDelete() {
        wavePanel.getInputContext().dispatchEvent(new KeyEvent(this, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED));
    }

    public void performPaste() {
        wavePanel.paste();
    }

    public void performSelectAll() {
        wavePanel.selectAll();
    }

    public void performPlay() {
        if (sourceDataLine.isActive()) {
            playThread.stopPlaying();
        } else {
            if (playThread == null) {
                playThread = new PlayThread();
                dataLinePosition = wavePanel.getCursorPosition();
                playThread.start();
            }
        }
    }

    public void performStop() {
        if (sourceDataLine.isActive()) {
            playThread.stopPlaying();
            wavePanel.setCursorPosition(0);
        }
    }

    public void printFile() {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (job.printDialog()) {
            try {
                job.setPrintable(new Printable() {

                    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                        if (pageIndex == 0) return Printable.PAGE_EXISTS;
                        return Printable.NO_SUCH_PAGE;
                    }
                });
                job.print();
            } catch (PrinterException ex) {
                Logger.getLogger(AudioPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setCurrentFont(Font font) {
    }

    public void showColorDialog(AudioColorDialog dlg) {
        ColorPanel colorPanel = dlg.getColorPanel();
        colorPanel.setTextFindColor(getSelectionColor());
        dlg.setVisible(true);
        if (dlg.getOption() == JOptionPane.OK_OPTION) {
            setSelectionColor(colorPanel.getTextFindColor());
        }
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public void setSelectionColor(Color color) {
        selectionColor = color;
        if (highlight != null) {
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        scrollPane = new javax.swing.JScrollPane();
        setAutoscrolls(true);
        setInheritsPopupMenu(true);
        setName("Form");
        setLayout(new java.awt.CardLayout());
        scrollPane.setAutoscrolls(true);
        scrollPane.setName("scrollPane");
        add(scrollPane, "card2");
    }

    private javax.swing.JScrollPane scrollPane;

    /**
     * @return the modified
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    public void setModified(boolean modified) {
    }

    public boolean isEditEnabled() {
        return false;
    }

    public boolean isPasteEnabled() {
        return false;
    }

    public void loadFromFile() {
        XBWave wave = new XBWave();
        wave.loadFromFile(new File(getFileName()));
        wavePanel.setWave(wave);
        targetFormat = wavePanel.getWave().getAudioFormat();
        targetDataLineInfo = new DataLine.Info(SourceDataLine.class, wavePanel.getWave().getAudioFormat());
        audioInputStream = wavePanel.getWave().getAudioInputStream();
        if (!AudioSystem.isLineSupported(targetDataLineInfo)) {
            AudioFormat pcm = new AudioFormat(targetFormat.getSampleRate(), 16, targetFormat.getChannels(), true, false);
            audioInputStream = AudioSystem.getAudioInputStream(pcm, audioInputStream);
            targetFormat = audioInputStream.getFormat();
            targetDataLineInfo = new DataLine.Info(SourceDataLine.class, targetFormat);
        }
        try {
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(targetDataLineInfo);
        } catch (LineUnavailableException ex) {
            Logger.getLogger(AudioPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveToFile() {
        File file = new File(getFileName());
        if (getFileType() == null) {
            wavePanel.getWave().saveToFile(file);
        } else {
            wavePanel.getWave().saveToFile(file, getFileType());
        }
    }

    public void newFile() {
        setFileType(null);
        wavePanel.setWave(null);
        setModified(false);
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public UndoManager getUndo() {
        return undo;
    }

    public void setPopupMenu(JPopupMenu menu) {
        wavePanel.setComponentPopupMenu(menu);
    }

    /**
     * @return the fileMode
     */
    public int getFileMode() {
        return fileMode;
    }

    /**
     * @param fileMode the fileMode to set
     */
    public void setFileMode(int fileMode) {
        this.fileMode = fileMode;
    }

    public void scale(double ratio) {
        scaleRatio = ratio;
    }

    /**
     * @return the ext
     */
    public String getExt() {
        return ext;
    }

    /**
     * @param ext the ext to set
     */
    public void setExt(String ext) {
        this.ext = ext;
    }

    @Override
    public Point getMousePosition() {
        return wavePanel.getMousePosition();
    }

    public void attachCaretListener(MouseMotionListener listener) {
        wavePanel.addMouseMotionListener(listener);
    }

    public void setToolColor(Color toolColor) {
        this.toolColor = toolColor;
    }

    public void showResizeDialog(ImageResizeDialog dlg) {
        if (dlg.getOption() == JOptionPane.OK_OPTION) {
        }
    }

    public String getCurrentPosition() {
        XBWave wave = wavePanel.getWave();
        if (wave == null) return "0:00.00";
        Point point = getMousePosition();
        if (point == null) point = new Point(scrollPane.getHorizontalScrollBar().getValue(), 0);
        float sampleRate = wave.getAudioFormat().getSampleRate();
        float position = point.x / sampleRate;
        String sub = String.valueOf((long) ((position - Math.floor(position)) * 100));
        if (sub.length() < 2) sub = "0" + sub;
        String sec = String.valueOf(((long) position) % 60);
        if (sec.length() < 2) sec = "0" + sec;
        return String.valueOf((long) position / 60) + ":" + sec + "." + sub;
    }

    /**
     * @return the fileType
     */
    public javax.sound.sampled.AudioFileFormat.Type getFileType() {
        return fileType;
    }

    /**
     * @param fileType the fileType to set
     */
    public void setFileType(javax.sound.sampled.AudioFileFormat.Type fileType) {
        this.fileType = fileType;
    }

    public void setDrawMode(XBWavePanel.DrawMode drawMode) {
        wavePanel.setDrawMode(drawMode);
    }

    public void setToolMode(XBWavePanel.ToolMode toolMode) {
        wavePanel.setToolMode(toolMode);
        wavePanel.repaint();
    }

    public String getPanelName() {
        return "Wave";
    }

    class PlayThread extends Thread {

        boolean stopped;

        int bufferPosition;

        @Override
        public void run() {
            if (wavePanel.getWave() == null) return;
            int bufferLength = wavePanel.getWave().chunkSize / 6;
            try {
                sourceDataLine.open(targetFormat, bufferLength);
                sourceDataLine.start();
                if (dataLinePosition < 0) {
                    bufferPosition = 0;
                } else {
                    bufferPosition = dataLinePosition * targetFormat.getFrameSize();
                }
                byte[] buffer = new byte[bufferLength];
                audioInputStream = wavePanel.getWave().getAudioInputStream();
                audioInputStream.skip(bufferPosition);
                bufferLength = audioInputStream.read(buffer, 0, bufferLength);
                int offset = 0;
                stopped = false;
                while ((buffer != null) && (!stopped)) {
                    bufferPosition += bufferLength - offset;
                    sourceDataLine.write(buffer, offset, bufferLength - offset);
                    if (wavePaintThread == null) {
                        wavePaintThread = new WavePaintThread();
                        wavePaintThread.start();
                    }
                    bufferLength = audioInputStream.read(buffer, 0, bufferLength);
                    if (bufferLength < 0) {
                        stopped = true;
                        dataLinePosition = 0;
                    }
                    offset = 0;
                }
                sourceDataLine.drain();
                sourceDataLine.close();
                playThread = null;
            } catch (IOException ex) {
                Logger.getLogger(AudioPanel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (LineUnavailableException ex) {
                Logger.getLogger(AudioPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void stopPlaying() {
            stopped = true;
            sourceDataLine.stop();
            while (playThread != null) try {
                sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(AudioPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private long getFramePosition() {
            return (bufferPosition / targetFormat.getFrameSize());
        }
    }

    class WavePaintThread extends Thread {

        private long lastPosition;

        @Override
        public void run() {
            AudioFormat audioFormat = wavePanel.getWave().getAudioFormat();
            lastPosition = -1;
            while (playThread != null) {
                long position = playThread.getFramePosition();
                if (position != lastPosition) {
                    firePropertyChange("currentWavePosition", position, lastPosition);
                    lastPosition = position;
                    JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
                    if (lastPosition < scrollPane.getWidth()) {
                        scrollPosition = 0;
                    } else if (lastPosition > wavePanel.getWave().getLengthInTicks() - scrollPane.getWidth()) {
                        scrollPosition = wavePanel.getWave().getLengthInTicks() - scrollPane.getWidth();
                    } else {
                        scrollPosition = (int) lastPosition - (scrollPane.getWidth() / 2);
                    }
                    horizontalScrollBar.setValue(scrollPosition);
                    wavePanel.setCursorPosition((int) lastPosition);
                    scrollPane.repaint();
                }
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(AudioPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            wavePaintThread = null;
        }
    }

    /**
     * display formats supported
     *
     * @param li the line information.
     */
    private static void showFormats(Line.Info li) {
        if (li instanceof DataLine.Info) {
            AudioFormat[] afs = ((DataLine.Info) li).getFormats();
            for (AudioFormat af : afs) {
                System.out.println("        " + af.toString());
            }
        }
    }

    public static void main(String[] args) {
        Mixer.Info[] mis = AudioSystem.getMixerInfo();
        for (Mixer.Info mi : mis) {
            Mixer mixer = AudioSystem.getMixer(mi);
            System.out.println("mixer: " + mixer.getClass().getName());
            Line.Info[] lis = mixer.getSourceLineInfo();
            for (Line.Info li : lis) {
                System.out.println("    source line: " + li.toString());
                showFormats(li);
            }
            lis = mixer.getTargetLineInfo();
            for (Line.Info li : lis) {
                System.out.println("    target line: " + li.toString());
                showFormats(li);
            }
            Control[] cs = mixer.getControls();
            for (Control c : cs) {
                System.out.println("    control: " + c.toString());
            }
        }
    }
}
