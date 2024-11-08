package vivace.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JPanel;
import vivace.exception.NoChannelAssignedException;
import vivace.model.Action;
import vivace.model.App;

public class Keyboard extends JPanel implements Observer {

    private static final long serialVersionUID = 4074154835728075879L;

    public static final Color KEY_PRESSED_COLOR = new Color(204, 204, 255);

    public static final int ORIENTATION_HORIZONTAL = 1;

    public static final int ORIENTATION_VERTICAL = 2;

    public static enum KeyColor {

        WHITE, BLACK
    }

    ;

    public static final int WHITE_KEY_HEIGHT = 60, WHITE_KEY_WIDTH = 16;

    public static final int BLACK_KEY_HEIGHT = 30, BLACK_KEY_WIDTH = 8;

    /**
	 * The height of a note in the piano roll. Applies to the midi events in the Piano Roll as
	 * well as the bars in the Track Content view.
	 */
    public static final int BIGNOTE_HEIGHT = WHITE_KEY_WIDTH - BLACK_KEY_WIDTH / 2;

    /**
	 * The height of a note in the piano roll. Applies to the midi events in the Piano Roll as
	 * well as the bars in the Track Content view.
	 */
    public static final int SMALLNOTE_HEIGHT = BLACK_KEY_WIDTH;

    private int orientation;

    private Key theKey;

    private MidiChannel channel;

    private int channel_no;

    private Receiver rcv;

    private int lowNote, highNote;

    private Vector<Key> keys = new Vector<Key>();

    private Vector<Key> blackKeys = new Vector<Key>();

    private Vector<Key> whiteKeys = new Vector<Key>();

    /**
	 * Constructor
	 * @param lowNote The lowest note on the keyboard
	 * @param highNote The highest note on the keyboard
	 * @param orientation The orientation
	 */
    public Keyboard(int lowNote, int highNote, int orientation) {
        this.lowNote = lowNote;
        this.highNote = highNote;
        this.orientation = orientation;
        initialize();
        addMouseListener(new KeyMouseListener());
        setBackground(Color.WHITE);
    }

    /**
	 * Empty constructor that creates a horisontal keyboard spanning from C1 to C7
	 */
    public Keyboard() {
        this(24, 96, Keyboard.ORIENTATION_HORIZONTAL);
    }

    private void initialize() {
        if (App.hasProjects()) {
            App.addProjectObserver(this, App.Source.ALL);
            setChannel();
            setReceiver();
        } else {
            channel_no = 0;
            try {
                channel = MidiSystem.getSynthesizer().getChannels()[channel_no];
            } catch (MidiUnavailableException e) {
            }
        }
        int width = 0, height = 0;
        if (orientation == ORIENTATION_HORIZONTAL) {
            int currentX = 0;
            Key key;
            for (int i = lowNote; i <= highNote; i++) {
                switch(i % 12) {
                    case 1:
                    case 3:
                    case 6:
                    case 8:
                    case 10:
                        key = new Key((currentX - BLACK_KEY_WIDTH / 2), 0, BLACK_KEY_WIDTH, BLACK_KEY_HEIGHT, i, KeyColor.BLACK);
                        blackKeys.add(key);
                        break;
                    default:
                        key = new Key(currentX, 0, WHITE_KEY_WIDTH, WHITE_KEY_HEIGHT, i, KeyColor.WHITE);
                        whiteKeys.add(key);
                        currentX += WHITE_KEY_WIDTH;
                        break;
                }
                keys.add(key);
            }
            width = currentX;
            height = WHITE_KEY_HEIGHT;
        } else if (orientation == ORIENTATION_VERTICAL) {
            int currentY = 0;
            Key key;
            for (int i = highNote; i >= lowNote; i--) {
                switch(i % 12) {
                    case 1:
                    case 3:
                    case 6:
                    case 8:
                    case 10:
                        key = new Key(0, (currentY - BLACK_KEY_WIDTH / 2), BLACK_KEY_HEIGHT, BLACK_KEY_WIDTH, i, KeyColor.BLACK);
                        blackKeys.add(key);
                        break;
                    default:
                        key = new Key(0, currentY, WHITE_KEY_HEIGHT, WHITE_KEY_WIDTH, i, KeyColor.WHITE);
                        whiteKeys.add(key);
                        currentY += WHITE_KEY_WIDTH;
                        break;
                }
                keys.add(key);
            }
            width = WHITE_KEY_HEIGHT;
            height = currentY;
        }
        setPreferredSize(new Dimension(width, height));
    }

    private void setReceiver() {
        try {
            rcv = App.Project.getSequencer().getReceiver();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void setChannel() {
        try {
            int t = App.UI.getTrackSelection().iterator().next();
            channel_no = App.Project.getTrackChannel(t);
        } catch (NoSuchElementException e) {
            channel_no = 0;
        } catch (NoChannelAssignedException e) {
            channel_no = 0;
        }
        channel = App.Project.getSynthesizer().getChannels()[channel_no];
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof Action) {
            Action a = (Action) arg;
            switch(a) {
                case TRACK_SELECTION_CHANGED:
                    setChannel();
                    break;
                case DEVICE_CHANGED:
                    setReceiver();
                    if (DumpReceiver.getInstance() != null) {
                        DumpReceiver.getInstance().deleteObserver(this);
                        DumpReceiver.getInstance().addObserver(this);
                    }
                    break;
            }
        } else if (arg instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) arg;
            if (sm.getData1() < 0 || sm.getData1() > 127) {
                return;
            } else if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                keys.get(127 - sm.getData1()).on();
                repaint();
            } else if (sm.getCommand() == ShortMessage.NOTE_OFF || sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0) {
                keys.get(127 - sm.getData1()).off();
                repaint();
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);
        g2.fill(new Rectangle(0, 0, getPreferredSize().width, getPreferredSize().height));
        for (Key key : whiteKeys) {
            if (key.isNoteOn()) {
                g2.setColor(KEY_PRESSED_COLOR);
                g2.fill(key);
            }
            g2.setColor(Color.BLACK);
            g2.draw(key);
        }
        for (Key key : blackKeys) {
            if (key.isNoteOn()) {
                g2.setColor(KEY_PRESSED_COLOR);
            } else {
                g2.setColor(Color.BLACK);
            }
            g2.fill(key);
            g2.setColor(Color.BLACK);
            g2.draw(key);
            g2.setColor(Color.WHITE);
            if (orientation == ORIENTATION_HORIZONTAL) {
                g2.fillRect(key.x + 2, BLACK_KEY_HEIGHT - 2, BLACK_KEY_WIDTH - 2, 1);
            } else {
                g2.fillRect(BLACK_KEY_HEIGHT - 2, key.y + 2, 1, BLACK_KEY_WIDTH - 2);
            }
        }
    }

    private class KeyMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            theKey = getKey(e.getPoint());
            if (theKey != null) {
                theKey.on();
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (theKey != null) {
                theKey.off();
                repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (theKey != null) {
                theKey.off();
                repaint();
                theKey = null;
            }
        }

        /**
		 * Find the key with the highest z-value on the specified point
		 * @param point
		 * @return
		 */
        public Key getKey(Point point) {
            Vector<Key> tmp = new Vector<Key>();
            tmp.addAll(blackKeys);
            tmp.addAll(whiteKeys);
            for (Key key : tmp) {
                if (key.contains(point)) {
                    return key;
                }
            }
            return null;
        }
    }

    /**
	 * Private class modelling a keyboard key, with information
	 * about its state (ON/OFF), it's color (WHITE/BLACK) and it's note value
	 */
    private class Key extends Rectangle {

        private static final long serialVersionUID = -2263234943956943170L;

        private static final int ON = 0, OFF = 1, VELOCITY = 127;

        private KeyColor keyColor;

        private int note;

        private int state = OFF;

        /**
		 * Constructor
		 * @param x
		 * @param y
		 * @param width
		 * @param height
		 * @param note
		 * @param keyColor
		 */
        public Key(int x, int y, int width, int height, int note, KeyColor keyColor) {
            super(x, y, width, height);
            this.note = note;
            this.keyColor = keyColor;
        }

        /**
		 * Returns whether or not the key is in ON-state
		 * @return
		 */
        public boolean isNoteOn() {
            return state == ON;
        }

        /**
		 * Returns whether or not the key is a white key
		 * @return
		 */
        public boolean isWhiteKey() {
            return keyColor == KeyColor.WHITE;
        }

        /**
		 * Sends a NOTE_OFF event to the receiver
		 * @return
		 */
        public void off() {
            setState(OFF);
            if (channel != null) {
                channel.noteOff(note, VELOCITY);
            }
            ShortMessage sm = new ShortMessage();
            try {
                sm.setMessage(ShortMessage.NOTE_OFF, channel_no, note, VELOCITY);
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
            if (App.hasProjects()) {
                rcv.send(sm, -1);
            }
        }

        /**
		 * Sends a NOTE_ON message to the receiver
		 */
        public void on() {
            setState(ON);
            if (channel != null) {
                channel.noteOn(note, VELOCITY);
            }
            ShortMessage sm = new ShortMessage();
            try {
                sm.setMessage(ShortMessage.NOTE_ON, channel_no, note, VELOCITY);
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
            if (App.hasProjects()) {
                rcv.send(sm, -1);
            }
        }

        /**
		 * Sets the state of the key
		 * @param state
		 */
        public void setState(int state) {
            this.state = state;
        }
    }
}
