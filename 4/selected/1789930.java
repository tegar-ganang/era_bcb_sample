package xwh.jPiano;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.swing.JFrame;
import javax.swing.JTable;

public class MyJavaPiano extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final int PROGRAM = 192;

    public static final int NOTEON = 144;

    public static final int NOTEOFF = 128;

    public static final int SUSTAIN = 64;

    public static final int REVERB = 91;

    public static final int ON = 0, OFF = 1;

    public static final Color jfcBlue = new Color(204, 204, 255);

    public static final Color pink = new Color(255, 175, 175);

    public static Sequencer sequencer;

    public static Sequence sequence;

    public static Synthesizer synthesizer;

    public static Instrument instruments[];

    public static ChannelData channels[];

    public static ChannelData cc_left;

    public static ChannelData cc_right;

    public static JTable table;

    public static boolean record = false;

    public static Track track;

    public static long startTime;

    public static RecordFrame recordFrame;

    public static Controls controls;

    public static int flag = 0;

    public static int flag_index = 0;

    public static int v8_left = 0;

    public static int v8_right = 0;

    public static int velocity_left = 70;

    public static int velocity_right = 85;

    public static int device_left = 0;

    public static int device_right = 0;

    public static int delay = 0;

    public static Toolkit tk = Toolkit.getDefaultToolkit();

    public static Piano piano = null;

    public static Keyboard keyboard = null;

    public static MyJavaPiano jPiano = null;

    public static void main(String[] args) {
        SettingManage.parseXMLFile("xml/MyJavaPianoSetting.xml");
        jPiano = new MyJavaPiano();
        jPiano.setTitle("MyJavaPiano");
        SettingManage.setting();
    }

    private int w = 850;

    private int h = 486;

    public MyJavaPiano() {
        piano = new Piano();
        keyboard = new Keyboard();
        setLayout(new FlowLayout());
        controls = new Controls(this);
        this.add(controls);
        this.add(keyboard);
        this.add(piano);
        this.setJMenuBar(new Menu().getJMenuBar());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(screenSize.width / 2 - w / 2, screenSize.height / 2 - h / 2);
        this.setSize(w, h);
        this.setVisible(true);
        this.setResizable(false);
        this.open();
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                DeviceManage.midiDevice_right.close();
                close();
                if (!tk.getLockingKeyState(KeyEvent.VK_NUM_LOCK)) {
                    tk.setLockingKeyState(KeyEvent.VK_NUM_LOCK, true);
                }
                System.exit(0);
            }
        });
        this.addKeyListener(new PianoKeyListener_Left());
        this.addKeyListener(new PianoKeyListener_Right());
        this.addKeyListener(new SpecialKeyListener());
        this.setFocusable(true);
        this.requestFocus();
        this.addMouseListener(frameGetFocusListener);
    }

    public void open() {
        try {
            if (synthesizer == null) {
                if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
                    System.out.println("getSynthesizer() failed!");
                    return;
                }
            }
            synthesizer.open();
            sequencer = MidiSystem.getSequencer();
            sequence = new Sequence(Sequence.PPQ, 10);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        Soundbank sb = synthesizer.getDefaultSoundbank();
        if (sb != null) {
            instruments = synthesizer.getDefaultSoundbank().getInstruments();
            synthesizer.loadInstrument(instruments[0]);
        }
        MidiChannel midiChannels[] = synthesizer.getChannels();
        channels = new ChannelData[midiChannels.length];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new ChannelData(midiChannels[i], i);
        }
        cc_left = channels[0];
        cc_right = channels[1];
    }

    public void close() {
        if (synthesizer != null) {
            synthesizer.close();
        }
        if (sequencer != null) {
            sequencer.close();
        }
        sequencer = null;
        synthesizer = null;
        instruments = null;
        channels = null;
        if (recordFrame != null) {
            recordFrame.dispose();
            recordFrame = null;
        }
    }

    RequestFocusListener frameGetFocusListener = new RequestFocusListener();

    class RequestFocusListener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            MyJavaPiano.this.requestFocus();
        }
    }
}
