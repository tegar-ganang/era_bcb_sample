package midimania;

import javax.sound.midi.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 *
 * @author Administrator
 */
public class MidiView extends javax.swing.JPanel implements MouseListener, Receiver, Transmitter, KeyEventDispatcher, KeyListener {

    /** Creates new form MidiView2 */
    ChannelInstance[] asmc;

    MidiMania MT;

    int topMrgin = 5;

    int leftMargin = 10;

    String[] nl;

    int bottomMargin = 10;

    int rightMargin = 250;

    boolean vertical = false;

    Receiver currentreceiver;

    int channelNum = 0;

    int channelviewarrayselection = 0;

    int Delay = -1;

    int program = 0;

    int XOffset = 10;

    int YOffset = 20;

    boolean ListReady = false;

    ArrayList<Integer> Excluding;

    public MidiView(MidiMania mt) {
        initComponents();
        Excluding = new ArrayList<Integer>();
        asmc = new ChannelInstance[16];
        int i = 0;
        while (i < 16) {
            asmc[i] = new ChannelInstance();
            i++;
        }
        MT = mt;
        addMouseListener(this);
    }

    public void close() {
    }

    public int twobytetoint(int fb, int sb) {
        int finalInt = (int) fb;
        int tempint = (int) sb;
        tempint = tempint << 7;
        finalInt = finalInt | tempint;
        return finalInt;
    }

    public void send(MidiMessage message, long timeStamp) {
        ShortMessage sm;
        try {
            sm = (ShortMessage) message;
        } catch (ClassCastException e) {
            return;
        }
        int channelnum = sm.getChannel();
        ChannelInstance dasmc = asmc[channelnum];
        if (sm.getCommand() == ShortMessage.NOTE_ON) {
            dasmc.note = sm.getData1();
            dasmc.velocity = sm.getData2();
            PaintChannel(sm.getChannel(), getGraphics());
        } else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
            dasmc.note = 0;
            dasmc.velocity = 0;
            PaintChannel(sm.getChannel(), getGraphics());
        } else if (sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
            dasmc.Instrument = sm.getData1();
            PaintChannel(sm.getChannel(), getGraphics());
        } else if (sm.getCommand() == ShortMessage.CONTROL_CHANGE) {
            if (sm.getData1() == 7) {
                asmc[sm.getChannel()].channelVelocity = sm.getData2();
                PaintChannel(sm.getChannel(), getGraphics());
            } else if (sm.getData1() == 121) {
                reset(sm.getChannel());
            }
        } else if (sm.getCommand() == ShortMessage.PITCH_BEND) {
            asmc[sm.getChannel()].pitchbend = twobytetoint(sm.getData1(), sm.getData2());
            PaintChannel(sm.getChannel(), getGraphics());
        } else if (sm.getStatus() == sm.SYSTEM_RESET) {
            reset();
        }
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paintComponent(g);
        int i = 0;
        while (i < 16) {
            PaintChannel(i, g2d);
            i++;
        }
    }

    public int ExtractMidiChannel(byte b) {
        byte cb = 0x0F;
        byte db = (byte) (b & cb);
        switch(db) {
            case 0x00:
                {
                    return 0;
                }
            case 0x01:
                {
                    return 1;
                }
            case 0x02:
                {
                    return 2;
                }
            case 0x03:
                {
                    return 3;
                }
            case 0x04:
                {
                    return 4;
                }
            case 0x05:
                {
                    return 5;
                }
            case 0x06:
                {
                    return 6;
                }
            case 0x07:
                {
                    return 7;
                }
            case 0x08:
                {
                    return 8;
                }
            case 0x09:
                {
                    return 9;
                }
            case 0x0A:
                {
                    return 10;
                }
            case 0x0B:
                {
                    return 11;
                }
            case 0x0C:
                {
                    return 12;
                }
            case 0x0D:
                {
                    return 13;
                }
            case 0x0E:
                {
                    return 14;
                }
            case 0x0F:
                {
                    return 15;
                }
            default:
                return -1;
        }
    }

    public class ChannelInstance {

        int channelVelocity = 127;

        int note;

        int velocity;

        int channel;

        int Instrument = 0;

        int pitchbend = 8192;
    }

    public void PaintChannel(int i, Graphics g2) {
        Graphics2D g = (Graphics2D) g2;
        if (vertical) {
            if (g == null) {
                return;
            }
            int x = leftMargin;
            int heightpersmc = (getHeight() - topMrgin - bottomMargin) / 16;
            int widthpersmc = getWidth() - leftMargin - rightMargin;
            int y = (i * heightpersmc) + topMrgin;
            ChannelInstance csmc = asmc[i];
            Color c = new Color(0, 0, 255);
            g.clearRect(x, y, getWidth(), heightpersmc);
            g.setColor(c);
            g.fillRect(x, y, csmc.note, heightpersmc);
            g.setColor(Color.RED);
            g.fillRect(x, y, csmc.velocity, heightpersmc / 2);
            g.setColor(Color.BLACK);
            g.drawString(i + " " + MidiManiaUtils.DefaultInstrumentList[csmc.Instrument], x, y + 12);
        } else {
            if (g == null) {
                return;
            }
            int lwidth = (getWidth() - leftMargin - rightMargin);
            int widthpermc = lwidth / 16;
            int tx = leftMargin + (i * widthpermc);
            int heightpermc = getHeight() - (topMrgin);
            Color c;
            g.setColor(getBackground());
            g.fillRect(tx, topMrgin, widthpermc, heightpermc + bottomMargin);
            g.setColor(Color.MAGENTA);
            g.fillRect(tx, topMrgin + heightpermc - (asmc[i].channelVelocity * heightpermc / 127), widthpermc, (asmc[i].channelVelocity * heightpermc / 127));
            c = Color.RED;
            g.setColor(c);
            g.fillRect(tx, topMrgin + heightpermc - (asmc[i].velocity * heightpermc / 127), widthpermc / 4 * 3, (asmc[i].velocity * heightpermc / 127));
            c = Color.BLUE;
            g.setColor(c);
            g.fillRect(tx, topMrgin + heightpermc - (asmc[i].note * heightpermc / 127), widthpermc / 2, (asmc[i].note * heightpermc / 127));
            c = Color.yellow;
            g.setColor(c);
            g.fillRect(tx, topMrgin + heightpermc - (asmc[i].pitchbend / 128 * heightpermc / 127), widthpermc / 4, (asmc[i].pitchbend / 128 * heightpermc / 127));
            AffineTransform af = g.getTransform();
            g.rotate(90 * 0.0174532, tx, topMrgin * 3);
            g.setColor(Color.BLACK);
            ChannelInstance csmc = asmc[i];
            g.drawString((i + 1) + " " + MidiManiaUtils.DefaultInstrumentList[csmc.Instrument], tx, topMrgin);
            g.setTransform(af);
            if (channelNum == i) {
                g.drawRect(tx, topMrgin, widthpermc - 1, heightpermc);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent evt) {
        requestFocusInWindow();
        int tx = evt.getX();
        int ty = evt.getY();
        tx = tx - leftMargin;
        ty = ty - topMrgin;
        if (tx < 0 || ty < 0) {
            return;
        }
        int cwitch = getWidth() - leftMargin - rightMargin;
        int cheight = getHeight() - topMrgin - bottomMargin;
        if (tx > cwitch || ty > cheight) {
            return;
        }
        int lwidth = (getWidth() - leftMargin - rightMargin);
        int widthpermc = lwidth / 16;
        int cn = tx / widthpermc;
        setChannel(cn);
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        requestFocusInWindow();
    }

    public void mouseExited(MouseEvent e) {
    }

    public String getModuleName() {
        return "Midi Keyboard";
    }

    public boolean dispatchKeyEvent(KeyEvent e) {
        if (jToggleButton2.isSelected()) {
            if (e.getID() == e.KEY_PRESSED) {
                keyPressed(e);
            }
            if (e.getID() == e.KEY_RELEASED) {
                keyReleased(e);
            }
            return true;
        }
        return false;
    }

    public void setChannel(int channel) {
        int oc = channelNum;
        channelNum = channel;
        PaintChannel(oc, getGraphics());
        PaintChannel(channel, getGraphics());
        ListReady = false;
        jComboBox1.setSelectedIndex(asmc[channel].Instrument);
        ListReady = true;
        Reanalize();
    }

    public Receiver getReceiver() {
        return currentreceiver;
    }

    public void setReceiver(Receiver receiver) {
        currentreceiver = receiver;
    }

    public void keyPressed(KeyEvent e) {
        if (true) {
            int i = 0;
            while (i < Excluding.size()) {
                if ((Integer) Excluding.get(i) == e.getKeyCode()) {
                    return;
                }
                i++;
            }
        }
        try {
            ShortMessage SM = new ShortMessage();
            SM.setMessage(ShortMessage.NOTE_ON, channelNum, 0, 0);
            int KeyCode = e.getKeyCode();
            if (KeyCode == KeyEvent.VK_Q) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 14), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_SPACE) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 121, 0);
                } else {
                    SM.setMessage(SM.SYSTEM_RESET);
                }
            }
            if (KeyCode == KeyEvent.VK_W) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 13), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_E) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 12), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_R) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 11), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_T) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 10), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_Y) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 9), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_U) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 8), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_I) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 7), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_O) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 6), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_P) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 5), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_A) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 4), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_S) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 3), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_D) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 2), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_F) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 - ((Integer) jSpinner1.getValue() * 1), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_G) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60, jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_H) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 1), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_J) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 2), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_K) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 3), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_L) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 4), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_SEMICOLON) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 5), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_Z) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 6), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_X) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 7), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_C) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 8), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_V) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 9), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_B) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 10), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_N) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 11), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_M) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 12), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_COMMA) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 13), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_PERIOD) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 14), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_SLASH) {
                if (e.isControlDown()) {
                    SM.setMessage(ShortMessage.CHANNEL_PRESSURE, channelNum, getCharValue(KeyCode, (Integer) jSpinner1.getValue()), 0);
                } else if (e.isAltDown()) {
                    int value = getCharValue(KeyCode, 546);
                    byte[] ab = separete7bit(value);
                    SM.setMessage(ShortMessage.PITCH_BEND, channelNum, ab[0], ab[1]);
                } else {
                    SM.setMessage(ShortMessage.NOTE_ON, channelNum, 60 + ((Integer) jSpinner1.getValue() * 15), jSlider1.getValue());
                }
            }
            if (KeyCode == KeyEvent.VK_UP) {
                jSlider1.setValue(jSlider1.getValue() + 5);
                return;
            }
            if (KeyCode == KeyEvent.VK_DOWN) {
                jSlider1.setValue(jSlider1.getValue() - 5);
                return;
            }
            if (KeyCode == KeyEvent.VK_PAGE_UP) {
                if (program != 127) {
                    program++;
                    jComboBox1.setSelectedIndex(program);
                }
                return;
            }
            if (KeyCode == KeyEvent.VK_PAGE_DOWN) {
                if (program != 0) {
                    program--;
                    jComboBox1.setSelectedIndex(program);
                }
                return;
            }
            if (KeyCode == KeyEvent.VK_LEFT) {
                if (channelNum > 0) {
                    setChannel(channelNum - 1);
                }
                Reanalize();
                return;
            }
            if (KeyCode == KeyEvent.VK_RIGHT) {
                if (channelNum < 16 - 1) {
                    setChannel(channelNum + 1);
                }
                Reanalize();
                return;
            }
            if (KeyCode == KeyEvent.VK_BACK_SPACE) {
                SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 123, 0);
            }
            if (KeyCode == KeyEvent.VK_1) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 0);
                } else {
                    smoothPitchWheel(1);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_2) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 13);
                } else {
                    smoothPitchWheel(2);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_3) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 38);
                } else {
                    smoothPitchWheel(3);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_4) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 51);
                } else {
                    smoothPitchWheel(4);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_5) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 64);
                } else {
                    smoothPitchWheel(5);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_6) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 76);
                } else {
                    smoothPitchWheel(6);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_7) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 89);
                } else {
                    smoothPitchWheel(7);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_8) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 102);
                } else {
                    smoothPitchWheel(8);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_9) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 114);
                } else {
                    smoothPitchWheel(9);
                    return;
                }
            }
            if (KeyCode == KeyEvent.VK_0) {
                if (e.isShiftDown()) {
                    SM.setMessage(ShortMessage.CONTROL_CHANGE, channelNum, 7, 127);
                } else {
                    smoothPitchWheel(10);
                    return;
                }
            }
            Reanalize();
            currentreceiver.send(SM, -1);
            if (SM.getCommand() == ShortMessage.NOTE_ON) {
                Excluding.add(KeyCode);
            }
        } catch (InvalidMidiDataException ex) {
            ErrorLog("Invalid");
        }
    }

    private void reset(int Channel) {
        asmc[Channel] = new ChannelInstance();
    }

    private void reset() {
        int i = 0;
        while (i < asmc.length) {
            asmc[i] = new ChannelInstance();
            i++;
        }
    }

    private void smoothPitchWheel(int data) {
        final int dt = data * 1638;
        final int distance = dt - asmc[channelNum].pitchbend;
        Thread t = new Thread() {

            public void run() {
                int i = 0;
                int current = asmc[channelNum].pitchbend;
                while (i < 10) {
                    try {
                        sleep(10);
                        ShortMessage sm = new ShortMessage();
                        current = current + (distance / 10);
                        byte[] sep = separete7bit(current);
                        sm.setMessage(sm.PITCH_BEND, channelNum, sep[0], sep[1]);
                        currentreceiver.send(sm, Delay);
                    } catch (InterruptedException ex) {
                    } catch (InvalidMidiDataException ex) {
                        MT.NewMessage(ex.toString());
                    }
                    i++;
                }
            }
        };
        t.run();
    }

    private byte[] separete7bit(int source) {
        int fb = source & 0x0000007f;
        int sb = source & 0x00003f80;
        sb = sb >> 7;
        byte[] ab = new byte[2];
        ab[0] = (byte) fb;
        ab[1] = (byte) sb;
        return ab;
    }

    private int getCharValue(int KeyCode, int multiply) {
        int centeredmultiply = multiply * 30 / 2;
        if (KeyCode == KeyEvent.VK_Q) {
            return centeredmultiply - (multiply * 14);
        }
        if (KeyCode == KeyEvent.VK_W) {
            return centeredmultiply - (multiply * 13);
        }
        if (KeyCode == KeyEvent.VK_E) {
            return centeredmultiply - (multiply * 12);
        }
        if (KeyCode == KeyEvent.VK_R) {
            return centeredmultiply - (multiply * 11);
        }
        if (KeyCode == KeyEvent.VK_T) {
            return centeredmultiply - (multiply * 10);
        }
        if (KeyCode == KeyEvent.VK_Y) {
            return centeredmultiply - (multiply * 9);
        }
        if (KeyCode == KeyEvent.VK_U) {
            return centeredmultiply - (multiply * 8);
        }
        if (KeyCode == KeyEvent.VK_I) {
            return centeredmultiply - (multiply * 7);
        }
        if (KeyCode == KeyEvent.VK_O) {
            return centeredmultiply - (multiply * 6);
        }
        if (KeyCode == KeyEvent.VK_P) {
            return centeredmultiply - (multiply * 5);
        }
        if (KeyCode == KeyEvent.VK_A) {
            return centeredmultiply - (multiply * 4);
        }
        if (KeyCode == KeyEvent.VK_S) {
            return centeredmultiply - (multiply * 3);
        }
        if (KeyCode == KeyEvent.VK_D) {
            return centeredmultiply - (multiply * 2);
        }
        if (KeyCode == KeyEvent.VK_F) {
            return centeredmultiply - (multiply * 1);
        }
        if (KeyCode == KeyEvent.VK_G) {
            return centeredmultiply;
        }
        if (KeyCode == KeyEvent.VK_H) {
            return centeredmultiply + (multiply * 1);
        }
        if (KeyCode == KeyEvent.VK_J) {
            return centeredmultiply + (multiply * 2);
        }
        if (KeyCode == KeyEvent.VK_K) {
            return centeredmultiply + (multiply * 3);
        }
        if (KeyCode == KeyEvent.VK_L) {
            return centeredmultiply + (multiply * 4);
        }
        if (KeyCode == KeyEvent.VK_SEMICOLON) {
            return centeredmultiply + (multiply * 5);
        }
        if (KeyCode == KeyEvent.VK_Z) {
            return centeredmultiply + (multiply * 6);
        }
        if (KeyCode == KeyEvent.VK_X) {
            return centeredmultiply + (multiply * 7);
        }
        if (KeyCode == KeyEvent.VK_C) {
            return centeredmultiply + (multiply * 8);
        }
        if (KeyCode == KeyEvent.VK_V) {
            return centeredmultiply + (multiply * 9);
        }
        if (KeyCode == KeyEvent.VK_B) {
            return centeredmultiply + (multiply * 10);
        }
        if (KeyCode == KeyEvent.VK_N) {
            return centeredmultiply + (multiply * 11);
        }
        if (KeyCode == KeyEvent.VK_M) {
            return centeredmultiply + (multiply * 12);
        }
        if (KeyCode == KeyEvent.VK_COMMA) {
            return centeredmultiply + (multiply * 13);
        }
        if (KeyCode == KeyEvent.VK_PERIOD) {
            return centeredmultiply + (multiply * 14);
        }
        if (KeyCode == KeyEvent.VK_SLASH) {
            return centeredmultiply + (multiply * 15);
        }
        return 0;
    }

    public void Reanalize() {
        repaint();
    }

    public void keyReleased(KeyEvent e) {
        if (!jToggleButton1.isSelected()) {
            try {
                ShortMessage SM = new ShortMessage();
                SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 0, 0);
                int KeyCode = e.getKeyCode();
                if (KeyCode == KeyEvent.VK_Q) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 14), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_W) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 13), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_E) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 12), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_R) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 11), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_T) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 10), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_Y) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 9), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_U) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 8), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_I) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 7), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_O) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 6), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_P) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 5), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_A) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 4), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_S) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 3), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_D) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 2), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_F) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 - ((Integer) jSpinner1.getValue() * 1), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_G) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60, jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_H) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 1), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_J) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 2), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_K) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 3), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_L) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 4), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_SEMICOLON) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 5), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_Z) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 6), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_X) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 7), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_C) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 8), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_V) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 9), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_B) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 10), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_N) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 11), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_M) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 12), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_COMMA) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 13), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_PERIOD) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 14), jSlider1.getValue());
                }
                if (KeyCode == KeyEvent.VK_SLASH) {
                    SM.setMessage(ShortMessage.NOTE_OFF, channelNum, 60 + ((Integer) jSpinner1.getValue() * 15), jSlider1.getValue());
                }
                currentreceiver.send(SM, 0);
            } catch (InvalidMidiDataException ex) {
                ErrorLog("Invalid");
            }
        }
        int i = 0;
        while (i < Excluding.size()) {
            if ((Integer) Excluding.get(i) == e.getKeyCode()) {
                Excluding.remove(i);
            }
            i++;
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void ErrorLog(String s) {
        MT.NewMessage(s);
    }

    public void LoadComboBoxItem() {
        jComboBox1.removeAllItems();
        int i = 0;
        while (i < 127) {
            jComboBox1.addItem(MidiManiaUtils.DefaultInstrumentList[i]);
            i++;
        }
        ListReady = true;
    }

    public void ready(MidiMania MT) {
        LoadComboBoxItem();
        MT.getMidiManiaMidiSystem().getMidiMessageJunction().addTransmitter(this, "Midi Keyboard");
        MT.getMidiManiaMidiSystem().getMidiMessageJunction().addReceiver(this, "Midi View");
        KeyboardFocusManager DKFM = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        DKFM.addKeyEventDispatcher(this);
        addKeyListener(this);
        addMouseListener(MT.getMidiManiaGUI());
    }

    private void jComboBox1ActionPerformed2(java.awt.event.ActionEvent evt) {
        if (!ListReady) {
            return;
        }
        program = jComboBox1.getSelectedIndex();
        try {
            ShortMessage sm = new ShortMessage();
            sm.setMessage(ShortMessage.PROGRAM_CHANGE, channelNum, jComboBox1.getSelectedIndex(), 0);
            currentreceiver.send(sm, -1);
        } catch (InvalidMidiDataException e) {
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jSlider1 = new javax.swing.JSlider();
        jPanel1 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox();
        jToggleButton1 = new javax.swing.JToggleButton();
        jSpinner1 = new javax.swing.JSpinner();
        jButton1 = new javax.swing.JButton();
        jToggleButton2 = new javax.swing.JToggleButton();
        jSlider1.setMaximum(127);
        jSlider1.setOrientation(javax.swing.JSlider.VERTICAL);
        jSlider1.setSnapToTicks(true);
        jSlider1.setValue(127);
        jSlider1.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                VolumeSliderChanged(evt);
            }
        });
        jPanel1.setLayout(new java.awt.GridLayout(0, 1, 0, 3));
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });
        jPanel1.add(jComboBox1);
        jToggleButton1.setText("Continues");
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContinuesButton(evt);
            }
        });
        jPanel1.add(jToggleButton1);
        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(2, -5, 5, 1));
        jPanel1.add(jSpinner1);
        jButton1.setText("Reset");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetButton(evt);
            }
        });
        jPanel1.add(jButton1);
        jToggleButton2.setText("Capture Focus");
        jPanel1.add(jToggleButton2);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap(389, Short.MAX_VALUE).addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGap(11, 11, 11).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jSlider1, 0, 0, Short.MAX_VALUE)).addContainerGap()));
    }

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {
        jComboBox1ActionPerformed2(evt);
    }

    private void ContinuesButton(java.awt.event.ActionEvent evt) {
    }

    private void VolumeSliderChanged(javax.swing.event.ChangeEvent evt) {
    }

    private void ResetButton(java.awt.event.ActionEvent evt) {
        ShortMessage sm = new ShortMessage();
        try {
            sm.setMessage(ShortMessage.SYSTEM_RESET);
            currentreceiver.send(sm, Delay);
        } catch (InvalidMidiDataException e) {
            MT.NewMessage(e.toString());
        }
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JComboBox jComboBox1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JSlider jSlider1;

    private javax.swing.JSpinner jSpinner1;

    private javax.swing.JToggleButton jToggleButton1;

    private javax.swing.JToggleButton jToggleButton2;
}
