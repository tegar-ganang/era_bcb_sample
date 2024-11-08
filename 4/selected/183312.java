package midi;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import com.bric.swing.ColorPicker;
import com.bric.swing.ColorPickerPanel;
import com.visutools.nav.bislider.BiSlider;

public class LEDFrame extends JFrame {

    private LEDInfo li;

    private MidiApp midiApp;

    private PhidgetController pc;

    /**
	 * Create the frame
	 * @param midiApp 
	 * @param li 
	 * @param pc 
	 */
    public LEDFrame(MidiApp midiApp, LEDInfo li, PhidgetController pc) {
        super();
        try {
            UIManager.setLookAndFeel("com.shfarr.ui.plaf.fh.FhLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.li = li;
        this.midiApp = midiApp;
        this.pc = pc;
        setBounds((1280 - (3 + 175 + 3 + 175 + 3 + 400 + 3 + 400 + 3)) / 2, 0, 3 + 175 + 3 + 175 + 3 + 400 + 3 + 400 + 3, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        createSliderPanel();
        createValuePanel();
        createDollPanel();
        createTransitionPanel();
        createMixPanel();
        initLevels();
    }

    Set<Integer>[] levels;

    private void initLevels() {
        int i;
        levels = new Set[9];
        for (i = 0; i < 9; i++) {
            levels[i] = new TreeSet<Integer>();
        }
        levels[0].add(17);
        levels[0].add(18);
        levels[1].add(15);
        levels[1].add(16);
        levels[1].add(19);
        levels[1].add(20);
        levels[2].add(13);
        levels[2].add(14);
        levels[2].add(21);
        levels[2].add(27);
        levels[3].add(6);
        levels[3].add(7);
        levels[3].add(22);
        levels[3].add(28);
        levels[4].add(5);
        levels[4].add(8);
        levels[4].add(23);
        levels[4].add(29);
        levels[5].add(4);
        levels[5].add(9);
        levels[5].add(24);
        levels[5].add(30);
        levels[6].add(3);
        levels[6].add(10);
        levels[6].add(25);
        levels[6].add(31);
        levels[7].add(2);
        levels[7].add(11);
        levels[7].add(26);
        levels[7].add(32);
        levels[8].add(1);
        levels[8].add(12);
    }

    private void createTransitionPanel() {
        JPanel transitionPanel;
        JComboBox R, G, B;
        JLabel rR, gG, bB, vel;
        int i;
        JSlider slVel;
        JLabel valor;
        transitionPanel = new JPanel();
        transitionPanel.setBounds(3 + 175 + 3 + 175 + 3 + 400 + 3, 3, 400, 180);
        transitionPanel.setLayout(null);
        transitionPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        rR = new JLabel("R");
        rR.setBounds(60, 30, 20, 20);
        gG = new JLabel("G");
        gG.setBounds(160, 30, 20, 20);
        bB = new JLabel("B");
        bB.setBounds(260, 30, 20, 20);
        R = new JComboBox();
        G = new JComboBox();
        B = new JComboBox();
        R.setBounds(80, 30, 40, 20);
        G.setBounds(180, 30, 40, 20);
        B.setBounds(280, 30, 40, 20);
        for (i = 0; i < 64; i++) {
            R.addItem(i + "");
            G.addItem(i + "");
            B.addItem(i + "");
        }
        R.setSelectedIndex(li.getChannelR());
        G.setSelectedIndex(li.getChannelG());
        B.setSelectedIndex(li.getChannelB());
        transitionPanel.add(rR);
        transitionPanel.add(gG);
        transitionPanel.add(bB);
        transitionPanel.add(R);
        transitionPanel.add(G);
        transitionPanel.add(B);
        vel = new JLabel("TRANSITION VELOCITY");
        vel.setBounds(80, 70, 200, 30);
        transitionPanel.add(vel);
        slVel = new JSlider();
        slVel.setBounds(76, 100, 249, 20);
        slVel.setMinimum(0);
        slVel.setMaximum(500);
        slVel.setValue((int) (li.getTransition() * 100));
        transitionPanel.add(slVel);
        valor = new JLabel("2.5s");
        valor.setBounds(300, 115, 20, 20);
        valor.setText(li.getTransition() + "s");
        transitionPanel.add(valor);
        getContentPane().add(transitionPanel);
    }

    private void createMixPanel() {
        JPanel mixPanel;
        JTabbedPane pane;
        int i;
        String[] label;
        JPanel panel;
        JLabel serialLabel;
        JTextField serial;
        JButton[] but;
        ColorPicker cp;
        mixPanel = new JPanel();
        mixPanel.setBounds(3 + 175 + 3 + 175 + 3 + 400 + 3, 3 + 180 + 3, 400, 780 - 180 - 33);
        mixPanel.setLayout(null);
        mixPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        pane = new JTabbedPane();
        pane.setBounds(10, 10, 350, 30);
        label = new String[] { "0", "1", "2", "3", "4", "5" };
        for (i = 0; i < label.length; i++) {
            panel = new JPanel();
            pane.addTab(label[i], panel);
        }
        pane.setSelectedIndex(0);
        mixPanel.add(pane);
        serialLabel = new JLabel("RFID ID");
        serialLabel.setBounds(10, 50, 75, 30);
        mixPanel.add(serialLabel);
        serial = new JTextField();
        serial.setBounds(80, 50, 100, 30);
        mixPanel.add(serial);
        but = new JButton[64];
        for (i = 0; i < 64; i++) {
            but[i] = new JButton("" + i);
            but[i].setBounds(80 + (i % 8) * 30, 100 + (i / 8) * 30, 20, 20);
            if (li.getLri()[0].getActive()[i]) {
                but[i].setBackground(Color.RED);
            }
            mixPanel.add(but[i]);
        }
        cp = new ColorPicker();
        cp.setBounds(80, 350, 200, 160);
        cp.setRGB(li.getLri()[0].getColor().getRed(), li.getLri()[0].getColor().getGreen(), li.getLri()[0].getColor().getBlue());
        mixPanel.add(cp);
        getContentPane().add(mixPanel);
    }

    JTextField[] led;

    private void createDollPanel() {
        JPanel dollPanel;
        int i;
        JLabel izq, der;
        dollPanel = new JPanel();
        dollPanel.setBounds(3 + 175 + 3 + 175 + 3, 3, 400, 30 + 8 * 90);
        dollPanel.setLayout(null);
        dollPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        led = new JTextField[32];
        for (i = 0; i < 32; i++) {
            led[i] = new JTextField((i + 1) + "");
            led[i].setBackground(Color.RED);
            led[i].setForeground(Color.RED);
            led[i].setHorizontalAlignment(JTextField.CENTER);
        }
        for (i = 0; i < 12; i++) {
            led[i].setBounds(25 + i * 30, 180, 20, 20);
        }
        for (i = 12; i < 20; i++) {
            led[i].setBounds(175 + 30 * (i % 2), 180 + ((((i + 1) / 2) - 5) * 30 - 30 * (i % 2)), 20, 20);
        }
        for (i = 20; i < 26; i++) {
            led[i].setBounds(25 + 4 * 30 - (i - 20) * 15, 330 + (i - 20) * 30, 20, 20);
        }
        for (i = 26; i < 32; i++) {
            led[i].setBounds(25 + 7 * 30 + (i - 26) * 15, 330 + (i - 26) * 30, 20, 20);
        }
        for (i = 0; i < 32; i++) {
            dollPanel.add(led[i]);
        }
        izq = new JLabel(">");
        izq.setBounds(25 + 4 * 30 + 10, 180 + ((((16 + 1) / 2) - 5) * 30 - 30 * (16 % 2)), 20, 20);
        dollPanel.add(izq);
        der = new JLabel("<");
        der.setBounds(25 + 7 * 30, 180 + ((((16 + 1) / 2) - 5) * 30 - 30 * (16 % 2)), 20, 20);
        dollPanel.add(der);
        getContentPane().add(dollPanel);
    }

    JLabel actualValue, value, sus, vel, pow, susA, velA, powA;

    JSlider sustain, velocity, power;

    private void createValuePanel() {
        JPanel valuePanel;
        valuePanel = new JPanel();
        valuePanel.setBounds(3 + 175 + 3, 3, 175, 30 + 8 * 90);
        valuePanel.setLayout(null);
        valuePanel.setBorder(BorderFactory.createLineBorder(Color.black));
        actualValue = new JLabel("ACTUAL VALUE");
        actualValue.setBounds(50, 100, 100, 50);
        valuePanel.add(actualValue);
        value = new JLabel(0 + "");
        value.setBounds(70, 130, 100, 50);
        value.setFont(actualValue.getFont().deriveFont((float) 24.00));
        valuePanel.add(value);
        sus = new JLabel("SUSTAIN");
        sus.setBounds(12, 180, 100, 50);
        valuePanel.add(sus);
        vel = new JLabel("VELOCITY");
        vel.setBounds(65, 180, 100, 50);
        valuePanel.add(vel);
        pow = new JLabel("POWER");
        pow.setBounds(125, 180, 100, 50);
        valuePanel.add(pow);
        sustain = new JSlider();
        sustain.setOrientation(JSlider.VERTICAL);
        sustain.setBounds(30, 220, 25, 300);
        sustain.setMinimum(0);
        sustain.setMaximum(500);
        sustain.setValue((int) (li.getSustain() * 100));
        sustain.addChangeListener(new SustainListener(this));
        valuePanel.add(sustain);
        velocity = new JSlider();
        velocity.setOrientation(JSlider.VERTICAL);
        velocity.setBounds(80, 220, 25, 300);
        velocity.setMinimum(0);
        velocity.setMaximum(200);
        velocity.setValue((int) (li.getVelocity() * 100));
        velocity.addChangeListener(new VelocityListener(this));
        valuePanel.add(velocity);
        power = new JSlider();
        power.setOrientation(JSlider.VERTICAL);
        power.setBounds(130, 220, 25, 300);
        power.setMinimum(0);
        power.setMaximum(500);
        power.setValue(li.getPower());
        power.addChangeListener(new PowerListener(this));
        valuePanel.add(power);
        susA = new JLabel("1.0s");
        susA.setBounds(25, 500, 100, 50);
        susA.setText(li.getSustain() + "s");
        valuePanel.add(susA);
        velA = new JLabel("1.0s");
        velA.setBounds(77, 500, 100, 50);
        velA.setText(li.getVelocity() + "s");
        valuePanel.add(velA);
        powA = new JLabel("250");
        powA.setBounds(127, 500, 100, 50);
        powA.setText(li.getPower() + "");
        valuePanel.add(powA);
        getContentPane().add(valuePanel);
    }

    BiSlider[] sl, slLectura;

    JSlider[] pot;

    JLabel[] num, dato;

    JCheckBox[] invert;

    private void createSliderPanel() {
        JPanel sliderPanel;
        int i;
        Font font;
        sliderPanel = new JPanel();
        sliderPanel.setBounds(3, 3, 175, 30 + 8 * 90);
        sliderPanel.setLayout(null);
        sliderPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        num = new JLabel[8];
        sl = new BiSlider[8];
        slLectura = new BiSlider[8];
        pot = new JSlider[8];
        dato = new JLabel[8];
        invert = new JCheckBox[8];
        for (i = 0; i < 8; i++) {
            num[i] = new JLabel("" + i);
            num[i].setBounds(15, 15 + i * 90, 145, 15);
            sliderPanel.add(num[i]);
            font = num[i].getFont();
            slLectura[i] = createSlider(i, 0, 0);
            slLectura[i].setBounds(15, 27 + i * 90, 145, 25);
            slLectura[i].setFont(font);
            sliderPanel.add(slLectura[i]);
            sl[i] = createSlider(-1, li.getLsi()[i].getMin(), li.getLsi()[i].getMax());
            sl[i].setBounds(15, 52 + i * 90, 145, 25);
            sl[i].setFont(font);
            sliderPanel.add(sl[i]);
            pot[i] = new JSlider();
            pot[i].setBounds(31, 83 + i * 90, 115, 25);
            pot[i].setMinimum(0);
            pot[i].setMaximum(200);
            pot[i].setValue((int) (li.getLsi()[i].getMultiplier() * 100));
            pot[i].addChangeListener(new LEDSliderListener(this, i));
            sliderPanel.add(pot[i]);
            dato[i] = new JLabel(1.00 + "");
            dato[i].setBounds(145, 79 + i * 90, 25, 25);
            dato[i].setText(li.getLsi()[i].getMultiplier() + "");
            sliderPanel.add(dato[i]);
            invert[i] = new JCheckBox();
            invert[i].setBounds(130, 15 + i * 90, 20, 10);
            invert[i].setSelected(li.getLsi()[i].isInvert());
            invert[i].addChangeListener(new LEDInvertListener(this, i));
            sliderPanel.add(invert[i]);
        }
        getContentPane().add(sliderPanel);
    }

    private BiSlider createSlider(int index, double min, double max) {
        BiSlider sensitivity;
        sensitivity = new BiSlider(BiSlider.HSB);
        sensitivity.setVisible(true);
        sensitivity.setMinimumValue(0);
        sensitivity.setMaximumValue(1000);
        sensitivity.setSegmentSize(50);
        sensitivity.setMinimumColor(new Color(0xE2, 0xE6, 0xD6));
        sensitivity.setMaximumColor(Color.RED);
        sensitivity.setPrecise(true);
        sensitivity.setBackground(new Color(0xE2, 0xE6, 0xD6));
        sensitivity.setColoredValues(min, max);
        return sensitivity;
    }

    int values[] = new int[8];

    double aggregate = 0;

    double[] signalShutdown = new double[32];

    double[] signalStart = new double[32];

    double LATENCY = 2000;

    double[] valueLED = new double[32];

    int MAX_LED = 100;

    public void checkSensor() {
        long time;
        int i, level, iter, l;
        Iterator<Integer> it;
        for (i = 0; i < 8; i++) {
            values[i] = midiApp.getValue(i);
            slLectura[i].setMinimumColoredValue(0);
            slLectura[i].setMaximumColoredValue(values[i]);
            li.getLsi()[i].setMin(sl[i].getMinimumColoredValue());
            li.getLsi()[i].setMax(sl[i].getMaximumColoredValue());
            if ((li.getLsi()[i].getMin() <= values[i] && values[i] <= li.getLsi()[i].getMax()) ^ li.getLsi()[i].isInvert()) {
                aggregate = aggregate + li.getLsi()[i].getMultiplier();
            }
        }
        aggregate = aggregate - li.getVelocity();
        if (aggregate < 0) {
            aggregate = 0;
        } else if (aggregate > li.getPower() * 18) {
            aggregate = li.getPower() * 18;
        }
        value.setText(Math.round(aggregate) + "");
        level = (int) (aggregate / li.getPower());
        for (iter = 0; iter <= Math.min(level - 1, levels.length - 1); iter++) {
            it = levels[iter].iterator();
            while (it.hasNext()) {
                l = it.next() - 1;
                signalShutdown[l] = 0;
                if (signalStart[l] > 0) {
                    time = System.currentTimeMillis();
                    if ((time - signalStart[l]) <= LATENCY || valueLED[l] < MAX_LED) {
                        valueLED[l] = Math.min(valueLED[l] + MAX_LED * (midiApp.getRefreshRate() / LATENCY), MAX_LED);
                        led[l].setForeground(new Color((int) (255 * valueLED[l] / MAX_LED), 0, 0));
                        pc.sendLED(l, valueLED[l]);
                    } else {
                        signalStart[l] = 0;
                    }
                } else {
                    signalStart[l] = System.currentTimeMillis();
                }
            }
        }
        for (iter = level; iter < 9; iter++) {
            it = levels[iter].iterator();
            while (it.hasNext()) {
                l = it.next() - 1;
                signalStart[l] = 0;
                if (signalShutdown[l] > 0) {
                    time = System.currentTimeMillis();
                    if ((time - signalShutdown[l]) <= LATENCY || valueLED[l] >= 0) {
                        valueLED[l] = Math.max(valueLED[l] - MAX_LED * (midiApp.getRefreshRate() / LATENCY), 0);
                        led[l].setForeground(new Color((int) (255 * valueLED[l] / MAX_LED), 0, 0));
                        pc.sendLED(l, valueLED[l]);
                    } else {
                        signalShutdown[l] = 0;
                    }
                } else {
                    signalShutdown[l] = System.currentTimeMillis();
                }
            }
        }
    }

    public void updateSlider(int index) {
        li.getLsi()[index].setMultiplier((double) pot[index].getValue() / 100);
        dato[index].setText(li.getLsi()[index].getMultiplier() + "");
    }

    public void updateInvert(int index) {
        li.getLsi()[index].setInvert(invert[index].isSelected());
    }

    public void updateVelocity() {
        li.setVelocity((double) velocity.getValue() / 100);
        velA.setText(li.getVelocity() + "s");
    }

    public void updatePower() {
        li.setPower(power.getValue());
        powA.setText(li.getPower() + "");
    }

    public void updateSustain() {
        li.setSustain((double) sustain.getValue() / 100);
        susA.setText(li.getSustain() + "s");
    }
}
