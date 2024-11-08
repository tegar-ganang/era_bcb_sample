package geovista.sound;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.TexturePaint;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.logging.Logger;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SonicRampSwatch extends JPanel implements MouseListener, ChangeListener {

    public static final transient boolean OFF = false;

    public static final transient boolean ON = true;

    private boolean anchored;

    private Color swatchColor;

    private final transient SonicRampPicker parent;

    private final transient boolean isEnd;

    private transient ImageIcon iconBlack;

    private transient ImageIcon iconWhite;

    private transient TexturePaint texPaint;

    private ChannelData cc;

    private Synthesizer synthesizer;

    private Instrument instruments[];

    private boolean noteState;

    JTable table;

    private transient int keyNum;

    protected static final Logger logger = Logger.getLogger(SonicRampSwatch.class.getName());

    public SonicRampSwatch(SonicRampPicker parent, boolean anchored, boolean end) {
        makeImage();
        this.parent = parent;
        swatchColor = Color.black;
        setBackground(swatchColor);
        addMouseListener(this);
        setAnchored(anchored);
        isEnd = end;
        initSound();
    }

    @SuppressWarnings("unused")
    private void initSound_old() {
        try {
            if (synthesizer == null) {
                if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
                    logger.severe("getSynthesizer() failed!");
                    return;
                }
            }
            synthesizer.open();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        Soundbank sb = synthesizer.getDefaultSoundbank();
        if (sb != null) {
            instruments = synthesizer.getDefaultSoundbank().getInstruments();
            synthesizer.loadInstrument(instruments[0]);
        }
        MidiChannel[] midiChannels = synthesizer.getChannels();
        cc = new ChannelData(midiChannels[0], 0);
        setInstrument(27);
    }

    private void initSound() {
        try {
            if (synthesizer == null) {
                if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
                    logger.severe("getSynthesizer() failed!");
                    return;
                }
            }
            synthesizer.open();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        Soundbank sb = SonicRampPicker.sb;
        if (sb == null) {
            SonicRampPicker.openSoundbank();
        }
        if (sb != null) {
            instruments = sb.getInstruments();
            synthesizer.loadInstrument(instruments[0]);
        }
        MidiChannel[] midiChannels = synthesizer.getChannels();
        cc = new ChannelData(midiChannels[0], 0);
    }

    public void makeImage() {
        Class cl = this.getClass();
        URL urlGif = cl.getResource("resources/SonicAnchor16.gif");
        ImageIcon icon = new ImageIcon(urlGif, "Anchors the color in a ramp");
        iconBlack = icon;
        URL urlGif2 = cl.getResource("resources/SonicAnchor16.gif");
        ImageIcon icon2 = new ImageIcon(urlGif2, "Anchors the color in a ramp");
        iconWhite = icon2;
    }

    public void setTexPaint(TexturePaint texPaint) {
        this.texPaint = texPaint;
    }

    public void setSwatchColor(Color newColor) {
        swatchColor = newColor;
        setBackground(newColor);
    }

    public Color getSwatchColor() {
        return swatchColor;
    }

    public void setAnchored(boolean anchor) {
        anchored = anchor;
        if (anchor || isEnd) {
            setBorder(BorderFactory.createLoweredBevelBorder());
        } else {
            setBorder(BorderFactory.createRaisedBevelBorder());
        }
    }

    public boolean getAnchored() {
        return anchored;
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        setNoteState(SonicRampSwatch.ON);
        on();
    }

    public void mouseExited(MouseEvent e) {
        setNoteState(SonicRampSwatch.OFF);
        off();
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
        } else if (e.getClickCount() == 1) {
            if (isEnd) {
                return;
            }
            if (anchored) {
                setAnchored(false);
                parent.swatchChanged();
            } else {
                setAnchored(true);
            }
        }
    }

    public void stateChanged(ChangeEvent e) {
        JSlider slid = (JSlider) e.getSource();
        int val = slid.getValue();
        setKeyNum(val);
    }

    public void setKeyNum(int keyNum) {
        logger.finest("setting key num, num = " + keyNum);
        this.keyNum = keyNum;
    }

    public void setInstrument(int insturment) {
        logger.finest("setting insturment,= " + insturment);
        boolean currState = noteState;
        if (noteState == SonicRampSwatch.ON) {
            off();
        }
        cc.channel.programChange(insturment);
        if (currState == SonicRampSwatch.ON) {
            on();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        if (texPaint != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(texPaint);
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (getAnchored()) {
            int midX = getWidth() / 2;
            int midY = getHeight() / 2;
            Color c = getBackground();
            int colorValue = c.getRed() + c.getBlue() + c.getGreen();
            Image ico = null;
            if (colorValue > 200) {
                ico = iconBlack.getImage();
            } else {
                ico = iconWhite.getImage();
            }
            midX = midX - (ico.getWidth(this) / 2);
            midY = midY - (ico.getHeight(this) / 2);
            g.drawImage(ico, midX, midY, this);
        }
    }

    public boolean isNoteOn() {
        return noteState == ON;
    }

    public void on() {
        setNoteState(ON);
        cc.channel.noteOn(keyNum, cc.velocity);
        makeTexPaint();
        this.repaint();
    }

    public void off() {
        setNoteState(OFF);
        cc.channel.noteOff(keyNum, cc.velocity);
        texPaint = null;
        this.repaint();
    }

    public void open() {
        try {
            if (synthesizer == null) {
                if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
                    logger.severe("getSynthesizer() failed!");
                    return;
                }
            }
            synthesizer.open();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        Soundbank sb = synthesizer.getDefaultSoundbank();
        if (sb != null) {
            instruments = synthesizer.getDefaultSoundbank().getInstruments();
            synthesizer.loadInstrument(instruments[0]);
        }
        ListSelectionModel lsm = table.getSelectionModel();
        lsm.setSelectionInterval(0, 0);
        lsm = table.getColumnModel().getSelectionModel();
        lsm.setSelectionInterval(0, 0);
    }

    public void setNoteState(boolean state) {
        noteState = state;
    }

    /**
	 * Main method for testing.
	 */
    public static void main(String[] args) {
        JFrame app = new JFrame();
        app.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        SonicRampPicker pick = new SonicRampPicker();
        SonicRampSwatch swat = new SonicRampSwatch(pick, true, false);
        app.getContentPane().setLayout(new BoxLayout(app.getContentPane(), BoxLayout.X_AXIS));
        app.getContentPane().add(swat);
        JSlider slider = new JSlider();
        slider.setValue(0);
        slider.addChangeListener(swat);
        app.getContentPane().add(slider);
        app.pack();
        app.setVisible(true);
    }

    private void makeTexPaint() {
        int texSize = 4;
        Rectangle2D.Float rect = new Rectangle2D.Float(0, 0, texSize, texSize);
        BufferedImage buff = new BufferedImage(texSize, texSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buff.createGraphics();
        Color trans = new Color(255, 255, 255);
        g2.setColor(trans);
        g2.fillRect(0, 0, texSize, texSize);
        g2.setColor(Color.blue);
        g2.drawLine(0, 0, 32, 32);
        texPaint = new TexturePaint(buff, rect);
    }

    /**
	 * Stores MidiChannel information.
	 */
    class ChannelData {

        MidiChannel channel;

        boolean solo;

        boolean mono;

        boolean mute;

        boolean sustain;

        int velocity;

        int pressure;

        int bend;

        int reverb;

        int row;

        int col;

        int num;

        public ChannelData(MidiChannel channel, int num) {
            this.channel = channel;
            this.num = num;
            velocity = pressure = bend = reverb = 64;
        }
    }
}
