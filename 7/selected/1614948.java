package Demos;

import telhai.java.gthreads.*;
import telhai.java.gthreads.examples.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * 
 * @author Alex Frid alex.frid@gmail.com; Dima Ruinski
 *
 */
public class GTMDemo extends JFrame implements ActionListener {

    private GraphicalThreadManager GTM;

    private NullThread[] nullthr;

    private RandThread[] randthr;

    private int nullCount;

    private int randCount;

    private JButton addThread;

    private JButton removeThread;

    private JButton removeAllThreads;

    private JTextField nullthrCount;

    private JLabel labelCount;

    private JTextField nullthrDelay;

    private JLabel labelDelay;

    private JTextField nullthrTimes;

    private JLabel labelTimes;

    private JCheckBox nullthrSleep;

    private JTextField removeThrIndex;

    private JLabel labelThrIndex;

    private JButton addObject;

    private JButton removeObject;

    private JButton removeAllObjects;

    private JCheckBox randInt;

    private JCheckBox randLong;

    private JCheckBox randFloat;

    private JCheckBox randDouble;

    private JTextField removeObjIndex;

    private JLabel labelObjIndex;

    private JButton start;

    private JButton stop;

    private JButton reset;

    private JButton restart;

    private JCheckBox autoReset;

    private JTextField speedField;

    private JLabel speedLabel;

    private JButton exit;

    private JTextField errorText;

    public GTMDemo() {
        setTitle("Graphical Thread Manager Interactive Demo");
        setSize(600, 260);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        GTM = new GraphicalThreadManager();
        GTM.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        nullCount = randCount = 0;
        nullthr = new NullThread[GTM.MAX_THREADS];
        randthr = new RandThread[GTM.MAX_OBJECTS];
        addInterface();
        addToolTips();
        setVisible(true);
    }

    private void addInterface() {
        Container c = getContentPane();
        Insets zeroMargin = new Insets(0, 0, 0, 0);
        addThread = new JButton("Add Counting Thread");
        addThread.setFont(addThread.getFont().deriveFont((float) 10.0));
        addThread.setMargin(zeroMargin);
        addThread.addActionListener(this);
        c.add(addThread);
        labelCount = new JLabel("Count to:");
        labelCount.setFont(labelCount.getFont().deriveFont((float) 9.0));
        c.add(labelCount);
        labelDelay = new JLabel("Sleep (ms):");
        labelDelay.setFont(labelDelay.getFont().deriveFont((float) 9.0));
        c.add(labelDelay);
        labelTimes = new JLabel("Iterations (0 = forever):");
        labelTimes.setFont(labelTimes.getFont().deriveFont((float) 9.0));
        c.add(labelTimes);
        nullthrCount = new JTextField(new Integer(NullThread.DEFAULT_COUNT).toString());
        c.add(nullthrCount);
        nullthrDelay = new JTextField(new Integer(NullThread.DEFAULT_SLEEP).toString());
        c.add(nullthrDelay);
        nullthrTimes = new JTextField("0");
        c.add(nullthrTimes);
        nullthrSleep = new JCheckBox("Start in sleeping mode:", false);
        nullthrSleep.setFont(nullthrSleep.getFont().deriveFont((float) 9.0));
        nullthrSleep.setHorizontalTextPosition(AbstractButton.LEADING);
        c.add(nullthrSleep);
        removeThread = new JButton("Remove Thread");
        removeThread.setFont(addThread.getFont().deriveFont((float) 10.0));
        removeThread.setMargin(zeroMargin);
        removeThread.setEnabled(false);
        removeThread.addActionListener(this);
        c.add(removeThread);
        labelThrIndex = new JLabel("Thread to remove (0-" + (GTM.MAX_THREADS - 1) + "):");
        labelThrIndex.setFont(labelThrIndex.getFont().deriveFont((float) 9.0));
        c.add(labelThrIndex);
        removeThrIndex = new JTextField();
        c.add(removeThrIndex);
        removeAllThreads = new JButton("Remove All Threads");
        removeAllThreads.setFont(addThread.getFont().deriveFont((float) 10.0));
        removeAllThreads.setMargin(zeroMargin);
        removeAllThreads.setEnabled(false);
        removeAllThreads.addActionListener(this);
        c.add(removeAllThreads);
        addObject = new JButton("Add Random Number Generator");
        addObject.setFont(addObject.getFont().deriveFont((float) 10.0));
        addObject.setMargin(zeroMargin);
        addObject.addActionListener(this);
        c.add(addObject);
        randInt = new JCheckBox("Integer", true);
        randInt.setFont(randInt.getFont().deriveFont((float) 9.0));
        c.add(randInt);
        randLong = new JCheckBox("Long integer", true);
        randLong.setFont(randLong.getFont().deriveFont((float) 9.0));
        c.add(randLong);
        randFloat = new JCheckBox("Single precision floating point", true);
        randFloat.setFont(randFloat.getFont().deriveFont((float) 9.0));
        c.add(randFloat);
        randDouble = new JCheckBox("Double precision floating point", true);
        randDouble.setFont(randDouble.getFont().deriveFont((float) 9.0));
        c.add(randDouble);
        removeObject = new JButton("Remove Generator");
        removeObject.setFont(addObject.getFont().deriveFont((float) 10.0));
        removeObject.setMargin(zeroMargin);
        removeObject.setEnabled(false);
        removeObject.addActionListener(this);
        c.add(removeObject);
        labelObjIndex = new JLabel("Generator to remove (0-" + (GTM.MAX_OBJECTS - 1) + "):");
        labelObjIndex.setFont(labelObjIndex.getFont().deriveFont((float) 9.0));
        c.add(labelObjIndex);
        removeObjIndex = new JTextField();
        c.add(removeObjIndex);
        removeAllObjects = new JButton("Remove All Generators");
        removeAllObjects.setFont(addObject.getFont().deriveFont((float) 10.0));
        removeAllObjects.setMargin(zeroMargin);
        removeAllObjects.setEnabled(false);
        removeAllObjects.addActionListener(this);
        c.add(removeAllObjects);
        speedLabel = new JLabel("Speed (" + GTM.MIN_PPTICK + "-" + GTM.MAX_PPTICK + "):");
        speedLabel.setFont(speedLabel.getFont().deriveFont((float) 9.0));
        c.add(speedLabel);
        speedField = new JTextField(GTM.MIN_PPTICK);
        speedField.addActionListener(this);
        c.add(speedField);
        autoReset = new JCheckBox("Restart automatically", true);
        autoReset.setFont(autoReset.getFont().deriveFont((float) 9.0));
        autoReset.addActionListener(this);
        c.add(autoReset);
        start = new JButton("START");
        start.setFont(start.getFont().deriveFont((float) 14.0));
        start.setMargin(zeroMargin);
        start.setForeground(Color.GREEN.darker());
        start.addActionListener(this);
        c.add(start);
        stop = new JButton("STOP");
        stop.setFont(stop.getFont().deriveFont((float) 14.0));
        stop.setMargin(zeroMargin);
        stop.setForeground(Color.RED.darker());
        stop.setEnabled(false);
        stop.addActionListener(this);
        c.add(stop);
        reset = new JButton("RESET");
        reset.setFont(reset.getFont().deriveFont((float) 14.0));
        reset.setMargin(zeroMargin);
        reset.setForeground(Color.MAGENTA.darker());
        reset.setEnabled(false);
        reset.addActionListener(this);
        c.add(reset);
        restart = new JButton("RESTART");
        restart.setFont(restart.getFont().deriveFont((float) 14.0));
        restart.setMargin(zeroMargin);
        restart.setForeground(Color.CYAN.darker());
        restart.setEnabled(false);
        restart.addActionListener(this);
        c.add(restart);
        exit = new JButton("EXIT");
        exit.setFont(exit.getFont().deriveFont((float) 12.0));
        exit.setMargin(zeroMargin);
        exit.addActionListener(this);
        c.add(exit);
        errorText = new JTextField();
        errorText.setEditable(false);
        errorText.setBackground(Color.WHITE);
        c.add(errorText);
    }

    private void addToolTips() {
        start.setToolTipText("Starts the Graphical Thread Manager.");
        stop.setToolTipText("Pauses the Graphical Thread Manager. " + "All threads and objects will stay in their positions, but the timeline will stop.");
        reset.setToolTipText("Resets the Graphical Thread Manager. Clears all the timelines and thread messages.");
        restart.setToolTipText("Restarts the Graphical Thread Manager (RESET+START)");
        exit.setToolTipText("Exits this Interactive Demo.");
        speedField.setToolTipText("The speed at which the timeline of the Graphical Thread Manager moves.");
        speedLabel.setToolTipText("The speed at which the timeline of the Graphical Thread Manager moves.");
        autoReset.setToolTipText("Selects whether the timeline should start over when it reaches the end.");
        addObject.setToolTipText("Adds a random number generator to the monitored objects. " + "Use the checkboxes below to select which types of numbers to generate.");
        removeObject.setToolTipText("Remove one of the random number generators. " + "Type the index of the generator to remove in the text field below.");
        addThread.setToolTipText("Adds a simple counting thread to the threads monitored by the " + "Graphical Thread Manager.");
        removeThread.setToolTipText("Remove one of the counting threads. " + "Type the index of the thread to remove in the text field below.");
        nullthrDelay.setToolTipText("The length of the sleep period between iterations of the counting thread " + "(in milliseconds)");
        labelDelay.setToolTipText("The length of the sleep period between iterations of the counting thread " + "(in milliseconds)");
        errorText.setToolTipText("Error messages are displayed here.");
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        errorText.setText("");
        if (src == exit) {
            System.exit(0);
        } else if (src == start) {
            start.setEnabled(false);
            stop.setEnabled(true);
            reset.setEnabled(true);
            restart.setEnabled(true);
            GTM.start();
        } else if (src == stop) {
            stop.setEnabled(false);
            start.setEnabled(true);
            reset.setEnabled(true);
            restart.setEnabled(true);
            GTM.stop();
        } else if (src == reset) {
            reset.setEnabled(false);
            start.setEnabled(true);
            stop.setEnabled(false);
            restart.setEnabled(false);
            GTM.resetHard();
        } else if (src == restart) {
            GTM.restartHard();
        } else if (src == autoReset) {
            if (autoReset.isSelected()) GTM.setAutoReset(true); else GTM.setAutoReset(false);
        } else if (src == speedField) {
            int speed;
            try {
                speed = Integer.parseInt(speedField.getText());
                if (speed < GTM.MIN_PPTICK || speed > GTM.MAX_PPTICK) errorText.setText("Error: speed value out of range."); else GTM.setPixelsPerTick(speed);
            } catch (NumberFormatException x) {
                errorText.setText("Error: illegal numeric format for speed.");
            }
        } else if (src == addThread) {
            int count, delay, times, numThreads;
            try {
                count = Integer.parseInt(nullthrCount.getText());
                delay = Integer.parseInt(nullthrDelay.getText());
                times = Integer.parseInt(nullthrTimes.getText());
                numThreads = GTM.getNumOfThreads();
                nullthr[numThreads] = new NullThread(count, delay, times, new Integer(++nullCount).toString());
                nullthr[numThreads].setInitSleep(nullthrSleep.isSelected());
                nullthr[numThreads].setMessages(true);
                nullthr[numThreads].setPriority(Thread.MIN_PRIORITY);
                GTM.addThread(nullthr[numThreads]);
                nullthr[numThreads].start();
                if (++numThreads == GTM.MAX_THREADS) addThread.setEnabled(false);
                removeThread.setEnabled(true);
                removeAllThreads.setEnabled(true);
            } catch (NumberFormatException x) {
                errorText.setText("Error: illegal numeric format in thread values.");
            }
        } else if (src == removeThread) {
            int index, numThreads;
            try {
                index = Integer.parseInt(removeThrIndex.getText());
                numThreads = GTM.getNumOfThreads();
                if (index < 0 || index > numThreads - 1) errorText.setText("Error: thread index out of bounds."); else {
                    GTM.removeThread(index);
                    if (--numThreads == 0) {
                        removeThread.setEnabled(false);
                        removeAllThreads.setEnabled(false);
                    }
                    addThread.setEnabled(true);
                    nullthr[index].kill();
                    int i;
                    for (i = index; i < numThreads; ++i) nullthr[i] = nullthr[i + 1];
                    nullthr[i] = null;
                }
            } catch (NumberFormatException x) {
                errorText.setText("Error: illegal numeric format in thread index.");
            }
        } else if (src == removeAllThreads) {
            int numThreads = GTM.getNumOfThreads();
            GTM.removeAllThreads();
            addThread.setEnabled(true);
            removeThread.setEnabled(false);
            removeAllThreads.setEnabled(false);
            for (int i = 0; i < numThreads; ++i) {
                nullthr[i].kill();
                nullthr[i] = null;
            }
        } else if (src == addObject) {
            int key = 0;
            int numThreads = GTM.getNumOfObjects();
            if (randInt.isSelected()) key += RandThread.R_I;
            if (randLong.isSelected()) key += RandThread.R_L;
            if (randFloat.isSelected()) key += RandThread.R_F;
            if (randDouble.isSelected()) key += RandThread.R_D;
            randthr[numThreads] = new RandThread(key, new Integer(++randCount).toString());
            randthr[numThreads].setPriority(Thread.MIN_PRIORITY);
            randthr[numThreads].start();
            GTM.addObject(randthr[numThreads]);
            if (++numThreads == GTM.MAX_OBJECTS) addObject.setEnabled(false);
            removeObject.setEnabled(true);
            removeAllObjects.setEnabled(true);
        } else if (src == removeObject) {
            int index, numThreads;
            try {
                index = Integer.parseInt(removeObjIndex.getText());
                numThreads = GTM.getNumOfObjects();
                if (index < 0 || index > GTM.getNumOfObjects() - 1) errorText.setText("Error: object index out of bounds."); else {
                    GTM.removeObject(index);
                    if (--numThreads == 0) {
                        removeObject.setEnabled(false);
                        removeAllObjects.setEnabled(false);
                    }
                    addObject.setEnabled(true);
                    randthr[index].kill();
                    int i;
                    for (i = index; i < numThreads; ++i) randthr[i] = randthr[i + 1];
                    randthr[i] = null;
                }
            } catch (NumberFormatException x) {
                errorText.setText("Error: illegal numeric format in object index.");
            }
        } else if (src == removeAllObjects) {
            int numThreads = GTM.getNumOfObjects();
            GTM.removeAllObjects();
            addObject.setEnabled(true);
            removeObject.setEnabled(false);
            removeAllObjects.setEnabled(false);
            for (int i = 0; i < numThreads; ++i) {
                randthr[i].kill();
                randthr[i] = null;
            }
        }
    }

    public void validate() {
        super.validate();
        addThread.setBounds(10, 10, 150, 20);
        labelCount.setBounds(10, 30, 50, 20);
        labelDelay.setBounds(10, 50, 60, 20);
        labelTimes.setBounds(10, 70, 120, 20);
        nullthrCount.setBounds(60, 30, 100, 20);
        nullthrDelay.setBounds(70, 50, 90, 20);
        nullthrTimes.setBounds(130, 70, 30, 20);
        nullthrSleep.setBounds(10, 90, 150, 20);
        removeThread.setBounds(10, 120, 150, 20);
        labelThrIndex.setBounds(10, 140, 120, 20);
        removeThrIndex.setBounds(130, 140, 30, 20);
        removeAllThreads.setBounds(10, 170, 150, 20);
        addObject.setBounds(180, 10, 200, 20);
        randInt.setBounds(180, 30, 200, 20);
        randLong.setBounds(180, 50, 200, 20);
        randFloat.setBounds(180, 70, 200, 20);
        randDouble.setBounds(180, 90, 200, 20);
        removeObject.setBounds(180, 120, 200, 20);
        labelObjIndex.setBounds(180, 140, 150, 20);
        removeObjIndex.setBounds(330, 140, 50, 20);
        removeAllObjects.setBounds(180, 170, 200, 20);
        speedLabel.setBounds(420, 10, 100, 20);
        speedField.setBounds(520, 10, 40, 20);
        autoReset.setBounds(420, 40, 140, 20);
        start.setBounds(420, 80, 80, 30);
        stop.setBounds(500, 80, 80, 30);
        reset.setBounds(420, 120, 80, 30);
        restart.setBounds(500, 120, 80, 30);
        exit.setBounds(470, 170, 70, 20);
        errorText.setBounds(10, 200, 400, 20);
    }

    public static void main(String[] args) {
        GTMDemo demo = new GTMDemo();
    }
}
