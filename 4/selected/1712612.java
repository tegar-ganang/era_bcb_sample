package demo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JComponent;

public class VirtualKeyboard12 extends JComponent implements Receiver, Transmitter {

    private static final long serialVersionUID = 1L;

    private char[] virtualKeys = "zsxdcvgbhnjmq2w3er5t6y7ui9o0p".toCharArray();

    private boolean[] keyDown = new boolean[virtualKeys.length];

    private int lowestKey = 36;

    private Receiver recv = null;

    private int velocity = 80;

    private int channel = 0;

    private boolean[] noteDown = new boolean[128];

    private int midiNoteDown = -1;

    public int getMidiNote(int x, int y) {
        int w = getWidth();
        int h = getHeight();
        float nw = w / 75f;
        int wn = (int) (x / nw);
        int oct = wn / 7;
        int n = oct * 12;
        int nb = wn % 7;
        if (nb == 1) {
            n += 2;
        }
        if (nb == 2) {
            n += 4;
        }
        if (nb == 3) {
            n += 5;
        }
        if (nb == 4) {
            n += 7;
        }
        if (nb == 5) {
            n += 9;
        }
        if (nb == 6) {
            n += 11;
        }
        if (y < h * 4.0 / 7.0) {
            int xb = x - (int) (oct * 7 * nw);
            float cx = 0;
            float black_note_width = nw * 0.7f;
            for (int b = 0; b < 12; b++) {
                boolean a = (b == 1 || b == 3 | b == 6 | b == 8 | b == 10);
                if (!a) {
                    cx += nw;
                } else {
                    float cstart = cx - (black_note_width / 2);
                    float cend = cstart + black_note_width;
                    if (xb > cstart && xb < cend) {
                        return oct * 12 + b;
                    }
                }
            }
        }
        if (n < 0) {
            n = 0;
        }
        if (n > 127) {
            n = 127;
        }
        return n;
    }

    private void allKeyboardKeyOff() {
        for (int i = 0; i < keyDown.length; i++) {
            if (keyDown[i]) {
                if ((i + lowestKey) < 128) {
                    ShortMessage sm = new ShortMessage();
                    try {
                        sm.setMessage(ShortMessage.NOTE_OFF, channel, (i + lowestKey), 0);
                        if (recv != null) {
                            recv.send(sm, -1);
                        }
                        send(sm, -1);
                    } catch (InvalidMidiDataException e1) {
                        e1.printStackTrace();
                    }
                    keyDown[i] = false;
                }
            }
        }
    }

    public void setChannel(int c) {
        channel = c;
    }

    public void setVelocity(int v) {
        velocity = v;
    }

    private Point pressPoint;

    long tLast;

    final ShortMessage pressMess = new ShortMessage();

    final ShortMessage vMess = new ShortMessage();

    void updateControllers(Point p2) {
        float dx, dy, dxdt, dydt;
        long tNow = System.nanoTime();
        if (p2 == pressPoint) {
            dx = dy = dxdt = dydt = 0;
        } else {
            long tDiff = tNow - tLast;
            dy = (int) (p2.getY() - pressPoint.getY());
            dx = (int) (p2.getX() - pressPoint.getX());
            dxdt = (float) dx / tDiff;
            dydt = (float) dy / tDiff;
        }
        dxdt = dxdt * 1000000;
        int vel = (int) Math.min(127, Math.abs(dx));
        int pressure = (int) Math.min(127, Math.abs(dy));
        System.out.println(" vel= " + vel + " pressure= " + dy);
        try {
            pressMess.setMessage(ShortMessage.CONTROL_CHANGE, channel, 109, pressure);
            vMess.setMessage(ShortMessage.CONTROL_CHANGE, channel, 108, vel);
            if (recv != null) {
                recv.send(pressMess, -1);
                recv.send(vMess, -1);
            }
        } catch (InvalidMidiDataException ex) {
            Logger.getLogger(VirtualKeyboard12.class.getName()).log(Level.SEVERE, null, ex);
        }
        tLast = tNow;
    }

    public VirtualKeyboard12() {
        super();
        setFocusable(true);
        addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                updateControllers(e.getPoint());
            }
        });
        addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                grabFocus();
                pressPoint = e.getPoint();
                Point p = pressPoint;
                tLast = System.nanoTime();
                midiNoteDown = getMidiNote(p.x, p.y);
                ShortMessage sm = new ShortMessage();
                try {
                    sm.setMessage(ShortMessage.NOTE_ON, channel, getMidiNote(p.x, p.y), velocity);
                    if (recv != null) {
                        recv.send(sm, -1);
                    }
                    send(sm, -1);
                } catch (InvalidMidiDataException e1) {
                    e1.printStackTrace();
                }
                updateControllers(pressPoint);
            }

            public void mouseReleased(MouseEvent e) {
                if (midiNoteDown == -1) {
                    return;
                }
                ShortMessage sm = new ShortMessage();
                try {
                    sm.setMessage(ShortMessage.NOTE_OFF, channel, midiNoteDown, 0);
                    if (recv != null) {
                        recv.send(sm, -1);
                    }
                    send(sm, -1);
                } catch (InvalidMidiDataException e1) {
                    e1.printStackTrace();
                }
                midiNoteDown = -1;
            }
        });
        addKeyListener(new KeyListener() {

            private long timeup;

            final long tttol = 10;

            public void keyPressed(KeyEvent e) {
                if (e.getWhen() - timeup < tttol) {
                    timeup = 0;
                    return;
                }
                System.out.println("KEY Press " + e.getWhen());
                char lc = Character.toLowerCase(e.getKeyChar());
                for (int i = 0; i < virtualKeys.length; i++) {
                    if (virtualKeys[i] == lc) {
                        if (!keyDown[i]) {
                            if ((i + lowestKey) < 128) {
                                ShortMessage sm = new ShortMessage();
                                try {
                                    sm.setMessage(ShortMessage.NOTE_ON, channel, (i + lowestKey), velocity);
                                    if (recv != null) {
                                        recv.send(sm, -1);
                                    }
                                    send(sm, -1);
                                } catch (InvalidMidiDataException e1) {
                                    e1.printStackTrace();
                                }
                                keyDown[i] = true;
                            }
                        }
                        return;
                    }
                }
            }

            public void keyReleased(final KeyEvent e) {
                timeup = e.getWhen();
                java.awt.EventQueue.invokeLater(new Runnable() {

                    public void run() {
                        if (0 != timeup) {
                            System.out.println("Key " + e.getKeyCode() + " released " + e.getWhen());
                            char lc = Character.toLowerCase(e.getKeyChar());
                            for (int i = 0; i < virtualKeys.length; i++) {
                                if (virtualKeys[i] == lc) {
                                    if (keyDown[i]) {
                                        if ((i + lowestKey) < 128) {
                                            ShortMessage sm = new ShortMessage();
                                            try {
                                                sm.setMessage(ShortMessage.NOTE_OFF, channel, (i + lowestKey), 0);
                                                if (recv != null) {
                                                    recv.send(sm, -1);
                                                }
                                                send(sm, -1);
                                            } catch (InvalidMidiDataException e1) {
                                                e1.printStackTrace();
                                            }
                                            keyDown[i] = false;
                                        }
                                    }
                                    return;
                                }
                            }
                        }
                    }
                });
            }

            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '-') {
                    allKeyboardKeyOff();
                    lowestKey -= 12;
                    if (lowestKey < 0) {
                        lowestKey = 0;
                    }
                    repaint();
                }
                if (e.getKeyChar() == '+') {
                    allKeyboardKeyOff();
                    lowestKey += 12;
                    if (lowestKey > 120) {
                        lowestKey = 120;
                    }
                    repaint();
                }
            }
        });
        addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                repaint();
            }

            public void focusLost(FocusEvent e) {
                repaint();
                allKeyboardKeyOff();
            }
        });
    }

    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int w = getWidth();
        int h = getHeight();
        float nw = w / 75f;
        float cx = 0;
        Rectangle2D rect = new Rectangle2D.Double();
        for (int i = 0; i < 128; i++) {
            int b = i % 12;
            boolean a = (b == 1 || b == 3 | b == 6 | b == 8 | b == 10);
            if (!a) {
                rect.setRect(cx, 0, nw, h);
                if (noteDown[i]) {
                    g2.setColor(new Color(0.8f, 0.8f, 0.95f));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fill(rect);
                g2.setColor(Color.BLACK);
                g2.draw(rect);
                if (hasFocus() && (i >= lowestKey)) {
                    if (i >= lowestKey) {
                        if (i - lowestKey < virtualKeys.length) {
                            g2.setColor(Color.GRAY);
                            char k = virtualKeys[i - lowestKey];
                            g2.drawString("" + k, cx + 2, h - 4);
                        }
                    }
                }
                cx += nw;
            }
        }
        cx = 0;
        float black_note_width = nw * 0.7f;
        for (int i = 0; i < 128; i++) {
            int b = i % 12;
            boolean a = (b == 1 || b == 3 | b == 6 | b == 8 | b == 10);
            if (!a) {
                cx += nw;
            } else {
                rect.setRect(cx - (black_note_width / 2), 0, black_note_width, h * 4.0 / 7.0);
                if (noteDown[i]) {
                    g2.setColor(new Color(0.8f, 0.8f, 0.95f));
                } else {
                    g2.setColor(Color.BLACK);
                }
                g2.fill(rect);
                g2.setColor(Color.BLACK);
                g2.draw(rect);
                if (hasFocus() && (i >= lowestKey)) {
                    if (i >= lowestKey) {
                        if (i - lowestKey < virtualKeys.length) {
                            g2.setColor(Color.LIGHT_GRAY);
                            char k = virtualKeys[i - lowestKey];
                            g2.drawString("" + k, cx - (black_note_width / 2) + 1, (h * 4.0f / 7.0f) - 3);
                        }
                    }
                }
            }
        }
    }

    public void close() {
    }

    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) message;
            if (sm.getChannel() == channel) {
                if (sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF) {
                    noteDown[sm.getData1()] = sm.getCommand() == ShortMessage.NOTE_ON && sm.getData1() > 0;
                    repaint();
                }
            }
        }
    }

    public Receiver getReceiver() {
        return recv;
    }

    public void setReceiver(Receiver receiver) {
        recv = receiver;
    }
}
