import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Enumeration;
import java.util.Vector;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import fi.hip.gb.core.JobResult;
import fi.hip.gb.core.WorkResult;
import fi.hip.gb.core.plugin.DispatchPlugin;
import fi.hip.gb.mobile.AgentApi;
import fi.hip.gb.mobile.Observer;
import fi.hip.gb.utils.FileUtils;

/**
 * Builds user interface for listening AIFF, AU and WAV files.
 * 
 * @author Juho Karppinen
 * @version $Id: SoundObserver.java 165 2005-01-31 16:03:10Z jkarppin $
 */
public class SoundObserver implements Runnable, Observer, ChangeListener, LineListener, MetaEventListener {

    private String errStr;

    private JTable table;

    private Vector sounds = new Vector();

    private boolean bump = false;

    private PlaybackMonitor playbackMonitor;

    private Thread thread;

    private Sequencer sequencer;

    private boolean midiEOM, audioEOM;

    private Synthesizer synthesizer;

    private MidiChannel channels[];

    private Object currentSound;

    private String currentName;

    private double duration;

    private boolean paused = false;

    private JSlider gainSlider, seekSlider;

    private JButton btnStart, btnStop;

    public SoundObserver() {
    }

    public void init(AgentApi api) {
    }

    /**
	 * Shows the controls for playing sound files.
	 * 
	 * @see fi.hip.gb.mobile.Observer#showResult(WorkResult)
	 */
    public JComponent showResult(WorkResult results) throws RemoteException {
        JPanel pane = new JPanel();
        _results = results;
        pane.setLayout(new BorderLayout());
        pane.setBorder(new EmptyBorder(5, 5, 5, 5));
        for (Enumeration e = _results.getResults().elements(); e.hasMoreElements(); ) {
            JobResult res = (JobResult) e.nextElement();
            sounds.add(res.getFileURL());
        }
        pane.add(getPlayer());
        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer instanceof Synthesizer) {
                synthesizer = (Synthesizer) sequencer;
                channels = synthesizer.getChannels();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        sequencer.addMetaEventListener(this);
        return pane;
    }

    /**
	 * Saves the content of text area.
	 * 
	 * @see fi.hip.gb.mobile.Observer#saveResult(WorkResult)
	 */
    public String saveResult(WorkResult result) throws RemoteException {
        return "";
    }

    public DispatchPlugin showControls() throws RemoteException {
        return null;
    }

    public void close() {
        if (thread != null && btnStop != null) {
            btnStop.doClick(0);
        }
        if (sequencer != null) {
            sequencer.close();
        }
    }

    public void stateChanged(ChangeEvent e) {
        JSlider slider = (JSlider) e.getSource();
        int value = slider.getValue();
        if (slider.equals(seekSlider)) {
            if (currentSound instanceof Clip) {
                ((Clip) currentSound).setFramePosition(value);
            } else if (currentSound instanceof Sequence) {
                long dur = ((Sequence) currentSound).getMicrosecondLength();
                sequencer.setMicrosecondPosition(value * 1000);
            } else if (currentSound instanceof BufferedInputStream) {
                long dur = sequencer.getMicrosecondLength();
                sequencer.setMicrosecondPosition(value * 1000);
            }
            playbackMonitor.repaint();
            return;
        }
        TitledBorder tb = (TitledBorder) slider.getBorder();
        String s = tb.getTitle();
        if (s.startsWith("Gain")) {
            s = s.substring(0, s.indexOf('=') + 1) + String.valueOf(value);
            if (currentSound != null) {
                setGain();
            }
        }
        tb.setTitle(s);
        slider.repaint();
    }

    private JComponent getTable() {
        final String[] names = { "#", "Name" };
        TableModel dataModel = new AbstractTableModel() {

            public int getColumnCount() {
                return names.length;
            }

            public int getRowCount() {
                return sounds.size();
            }

            public Object getValueAt(int row, int col) {
                if (col == 0) {
                    return new Integer(row);
                } else if (col == 1) {
                    URL soundURL = (URL) sounds.get(row);
                    return FileUtils.getFilename(soundURL);
                }
                return null;
            }

            public String getColumnName(int col) {
                return names[col];
            }

            public Class getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }

            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public void setValueAt(Object aValue, int row, int col) {
            }
        };
        JTable newTable = new JTable(dataModel);
        TableColumn col = newTable.getColumn("#");
        col.setMaxWidth(20);
        newTable.sizeColumnsToFit(0);
        newTable.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        return newTable;
    }

    private JComponent getPlayer() {
        JComponent pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 2, 1, 2);
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1.0f;
        c.weighty = 1.0f;
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.gridx = 0;
        c.gridwidth = 2;
        playbackMonitor = new PlaybackMonitor();
        playbackMonitor.setPreferredSize(new Dimension(400, 60));
        pane.add(playbackMonitor, c);
        seekSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
        seekSlider.setEnabled(false);
        seekSlider.addChangeListener(this);
        c.gridy++;
        pane.add(seekSlider, c);
        final ImageIcon playIcon = new ImageIcon("./resources/mediaplayer/Play.gif");
        final ImageIcon pauseIcon = new ImageIcon("./resources/mediaplayer/Pause.gif");
        final ImageIcon stopIcon = new ImageIcon("./resources/mediaplayer/Stop.gif");
        final ImageIcon backwardIcon = new ImageIcon("./resources/mediaplayer/Backward.gif");
        final ImageIcon forwardIcon = new ImageIcon("./resources/mediaplayer/Forward.gif");
        btnStart = new JButton("Start", playIcon);
        btnStart.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (btnStart.getText().equals("Start")) {
                    paused = false;
                    start();
                    btnStart.setText("Pause");
                    btnStart.setIcon(pauseIcon);
                    btnStop.setEnabled(true);
                } else if (btnStart.getText().equals("Pause")) {
                    paused = true;
                    if (currentSound instanceof Clip) {
                        ((Clip) currentSound).stop();
                    } else if (currentSound instanceof Sequence || currentSound instanceof BufferedInputStream) {
                        sequencer.stop();
                    }
                    playbackMonitor.stop();
                    btnStart.setText("Resume");
                    btnStart.setIcon(playIcon);
                } else if (btnStart.getText().equals("Resume")) {
                    paused = false;
                    if (currentSound instanceof Clip) {
                        ((Clip) currentSound).start();
                    } else if (currentSound instanceof Sequence || currentSound instanceof BufferedInputStream) {
                        sequencer.start();
                    }
                    playbackMonitor.start();
                    btnStart.setText("Pause");
                    btnStart.setIcon(pauseIcon);
                }
            }
        });
        c.weightx = 1.0f;
        c.weighty = 0.0f;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        pane.add(btnStart, c);
        btnStop = new JButton("Stop", stopIcon);
        btnStop.setEnabled(false);
        btnStop.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                paused = false;
                stop();
                btnStart.setText("Start");
                btnStart.setIcon(playIcon);
                btnStop.setEnabled(false);
            }
        });
        c.gridx++;
        pane.add(btnStop, c);
        gainSlider = new JSlider(0, 100, 80);
        gainSlider.addChangeListener(this);
        TitledBorder tb = new TitledBorder(new EtchedBorder());
        tb.setTitle("Gain = 80");
        gainSlider.setBorder(tb);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        pane.add(gainSlider, c);
        return pane;
    }

    /**
	 * Load sound into current sound
	 * 
	 * @param soundURL URL for the sound clip
	 * @return boolean true if loading succeeded, false if failed
	 */
    private boolean loadSound(URL soundURL) {
        duration = 0.0;
        currentName = FileUtils.getFilename(soundURL);
        playbackMonitor.repaint();
        try {
            currentSound = AudioSystem.getAudioInputStream(soundURL);
        } catch (Exception e) {
            try {
                currentSound = MidiSystem.getSequence(soundURL);
            } catch (InvalidMidiDataException imde) {
                System.out.println("Unsupported audio file.");
                return false;
            } catch (Exception ex) {
                ex.printStackTrace();
                currentSound = null;
                return false;
            }
        }
        if (currentSound instanceof AudioInputStream) {
            try {
                AudioInputStream stream = (AudioInputStream) currentSound;
                AudioFormat format = stream.getFormat();
                if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                    AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                    stream = AudioSystem.getAudioInputStream(tmp, stream);
                    format = tmp;
                }
                DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.addLineListener(this);
                clip.open(stream);
                currentSound = clip;
                seekSlider.setMaximum((int) stream.getFrameLength());
            } catch (Exception ex) {
                ex.printStackTrace();
                currentSound = null;
                return false;
            }
        } else if (currentSound instanceof Sequence || currentSound instanceof BufferedInputStream) {
            try {
                sequencer.open();
                if (currentSound instanceof Sequence) {
                    sequencer.setSequence((Sequence) currentSound);
                } else {
                    sequencer.setSequence((BufferedInputStream) currentSound);
                }
                seekSlider.setMaximum((int) (sequencer.getMicrosecondLength() / 1000));
            } catch (InvalidMidiDataException imde) {
                System.out.println("Unsupported audio file.");
                currentSound = null;
                return false;
            } catch (Exception ex) {
                ex.printStackTrace();
                currentSound = null;
                return false;
            }
        }
        seekSlider.setValue(0);
        seekSlider.setEnabled(true);
        gainSlider.setEnabled(true);
        duration = getDuration();
        return true;
    }

    /**
	 * Plays current sound. Clip must already be loaded with loadSound(URL)
	 * method
	 */
    private void playSound() {
        playbackMonitor.start();
        setGain();
        midiEOM = audioEOM = bump = false;
        if ((currentSound instanceof Sequence || currentSound instanceof BufferedInputStream) && thread != null) {
            sequencer.start();
            while (!midiEOM && thread != null && !bump) {
                try {
                    Thread.sleep(99);
                } catch (Exception e) {
                    break;
                }
            }
            sequencer.stop();
            sequencer.close();
        } else if (currentSound instanceof Clip) {
            Clip clip = (Clip) currentSound;
            clip.start();
            try {
                Thread.sleep(99);
            } catch (Exception e) {
            }
            while (paused || clip.isActive() && thread != null && !bump) {
                try {
                    Thread.sleep(99);
                } catch (Exception e) {
                    break;
                }
            }
            clip.stop();
            clip.close();
        }
        currentSound = null;
        playbackMonitor.stop();
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("Player");
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }

    public void run() {
        if (loadSound((URL) sounds.get(0)) == true) {
            playSound();
        }
        if (thread != null) {
            btnStop.doClick();
        }
        thread = null;
        currentName = null;
        currentSound = null;
        playbackMonitor.repaint();
    }

    public double getDuration() {
        double dur = 0.0;
        if (currentSound instanceof Sequence) {
            dur = ((Sequence) currentSound).getMicrosecondLength() / 1000000.0;
        } else if (currentSound instanceof BufferedInputStream) {
            dur = sequencer.getMicrosecondLength() / 1000000.0;
        } else if (currentSound instanceof Clip) {
            Clip clip = (Clip) currentSound;
            dur = clip.getBufferSize() / (clip.getFormat().getFrameSize() * clip.getFormat().getFrameRate());
        }
        return dur;
    }

    public double getSeconds() {
        double seconds = 0.0;
        if (currentSound instanceof Clip) {
            Clip clip = (Clip) currentSound;
            seconds = clip.getFramePosition() / clip.getFormat().getFrameRate();
        } else if ((currentSound instanceof Sequence) || (currentSound instanceof BufferedInputStream)) {
            try {
                seconds = sequencer.getMicrosecondPosition() / 1000000.0;
            } catch (IllegalStateException e) {
                System.out.println("TEMP: IllegalStateException " + "on sequencer.getMicrosecondPosition(): " + e);
            }
        }
        return seconds;
    }

    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP && !paused) {
            audioEOM = true;
        }
    }

    public void meta(MetaMessage message) {
        if (message.getType() == 47) {
            midiEOM = true;
        }
    }

    private void reportStatus(String msg) {
        if (msg != null) {
            errStr = msg;
            System.out.println(msg);
            playbackMonitor.repaint();
        }
    }

    public void setGain() {
        double value = gainSlider.getValue() / 100.0;
        if (currentSound instanceof Clip) {
            try {
                Clip clip = (Clip) currentSound;
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(value == 0.0 ? 0.0001 : value) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (currentSound instanceof Sequence || currentSound instanceof BufferedInputStream) {
            for (int i = 0; i < channels.length; i++) {
                channels[i].controlChange(7, (int) (value * 127.0));
            }
        }
    }

    /**
	 * Displays current sound and time elapsed.
	 */
    public class PlaybackMonitor extends JPanel implements Runnable {

        String welcomeStr = " ";

        Thread pbThread;

        Color black = new Color(20, 20, 20);

        Color jfcBlue = new Color(204, 204, 255);

        Color jfcDarkBlue = jfcBlue.darker();

        Font font24 = new Font("serif", Font.BOLD, 24);

        Font font28 = new Font("serif", Font.BOLD, 28);

        Font font42 = new Font("serif", Font.BOLD, 42);

        FontMetrics fm28, fm42;

        public PlaybackMonitor() {
            fm28 = getFontMetrics(font28);
            fm42 = getFontMetrics(font42);
        }

        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            Dimension d = getSize();
            g2.setBackground(black);
            g2.clearRect(0, 0, d.width, d.height);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(jfcBlue);
            if (errStr != null) {
                g2.setFont(new Font("serif", Font.BOLD, 18));
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.drawString("ERROR", 5, 20);
                AttributedString as = new AttributedString(errStr);
                Font font12 = new Font("serif", Font.PLAIN, 12);
                as.addAttribute(TextAttribute.FONT, font12, 0, errStr.length());
                AttributedCharacterIterator aci = as.getIterator();
                FontRenderContext frc = g2.getFontRenderContext();
                LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
                float x = 5, y = 25;
                lbm.setPosition(0);
                while (lbm.getPosition() < errStr.length()) {
                    TextLayout tl = lbm.nextLayout(d.width - x - 5);
                    if (!tl.isLeftToRight()) {
                        x = d.width - tl.getAdvance();
                    }
                    tl.draw(g2, x, y += tl.getAscent());
                    y += tl.getDescent() + tl.getLeading();
                }
            } else if (currentName == null) {
                FontRenderContext frc = g2.getFontRenderContext();
                TextLayout tl = new TextLayout(welcomeStr, font28, frc);
                float x = (float) (d.width / 2 - tl.getBounds().getWidth() / 2);
                tl.draw(g2, x, d.height / 2);
            } else {
                g2.setFont(font24);
                g2.drawString(currentName, 5, fm28.getHeight() - 5);
                if (duration <= 0.0) {
                } else {
                    double seconds = getSeconds();
                    if (midiEOM || audioEOM) {
                        seconds = duration;
                    }
                    if (seconds > 0.0) {
                        g2.setFont(font42);
                        String s = String.valueOf(seconds);
                        s = s.substring(0, s.indexOf('.') + 2);
                        int strW = (int) fm42.getStringBounds(s, g2).getWidth();
                        g2.drawString(s, d.width - strW - 9, fm42.getAscent());
                        int num = 30;
                        int progress = (int) (seconds / duration * num);
                        double ww = ((double) (d.width - 10) / (double) num);
                        double hh = (int) (d.height * 0.25);
                        double x = 0.0;
                        for (; x < progress; x += 1.0) {
                            g2.fill(new Rectangle2D.Double(x * ww + 5, d.height - hh - 5, ww - 1, hh));
                        }
                        g2.setColor(jfcDarkBlue);
                        for (; x < num; x += 1.0) {
                            g2.fill(new Rectangle2D.Double(x * ww + 5, d.height - hh - 5, ww - 1, hh));
                        }
                    }
                }
            }
        }

        public void start() {
            pbThread = new Thread(this);
            pbThread.setName("PlaybackMonitor");
            pbThread.start();
        }

        public void stop() {
            if (pbThread != null) {
                pbThread.interrupt();
            }
            pbThread = null;
        }

        public void run() {
            while (pbThread != null) {
                try {
                    Thread.sleep(99);
                } catch (Exception e) {
                    break;
                }
                repaint();
            }
            pbThread = null;
        }
    }

    /** result to be shown */
    private WorkResult _results;
}
