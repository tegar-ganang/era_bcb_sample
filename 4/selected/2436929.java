package view.visualization;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import model.datapath.AbstractDataPathElement;
import model.datapath.DataPath;
import view.datapath.element.Factory;

public class PercentageVisualization extends JComponent implements Visualization {

    private static final long serialVersionUID = 1L;

    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();

    private static final NumberFormat SMALL_PERCENT_FORMAT = new DecimalFormat("0.#####E0##%");

    private static final double MIN_SMALL = 0.0001;

    private static final NumberFormat BITS_FORMAT = NumberFormat.getIntegerInstance();

    static {
        PERCENT_FORMAT.setMinimumFractionDigits(3);
        BITS_FORMAT.setGroupingUsed(true);
    }

    private JButton pauseButton;

    private PercentPanel[] counters = new PercentPanel[0];

    private GridBagConstraints gbc;

    private Thread writeThread, ui_updateThread;

    private boolean paused = true;

    public PercentageVisualization() {
        ui_updateThread = new UI_update();
        setName("Percentage Visualization");
        JPanel top = new JPanel();
        addButtons(top);
        setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        add(top, gbc);
        ui_updateThread.start();
    }

    private void addButtons(JPanel panel) {
        pauseButton = new JButton("Start");
        pauseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setPaused(!paused);
            }
        });
        panel.add(pauseButton);
        setPaused(true);
    }

    private synchronized void setPaused(boolean paused) {
        if (this.paused == paused) return;
        this.paused = paused;
        if (paused) {
            pauseButton.setText("Resume");
        } else {
            pauseButton.setText("Pause");
            if (writeThread != null) synchronized (writeThread) {
                writeThread.notify();
            }
        }
    }

    public JComponent getComponent() {
        return this;
    }

    private class DataCollector extends AbstractDataPathElement<Boolean, Boolean> {

        private long correct, incorrect;

        private LinkedBlockingQueue<Boolean> comparison = new LinkedBlockingQueue<Boolean>();

        public void write(Boolean data) {
            if (data.equals(comparison.poll())) {
                correct++;
            } else {
                incorrect++;
            }
        }

        @Override
        public void wroteLast() {
        }
    }

    private class WriteThread extends Thread {

        private Random r = new Random();

        @Override
        public void run() {
            try {
                while (paused) synchronized (this) {
                    wait();
                }
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    while (paused) synchronized (this) {
                        wait();
                    }
                    boolean b = r.nextBoolean();
                    for (PercentPanel counter : counters) {
                        counter.sink.comparison.put(b);
                        counter.datapath.write(b);
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private class UI_update extends Thread {

        @Override
        public void run() {
            try {
                setPriority(Thread.MIN_PRIORITY);
                while (!interrupted()) {
                    for (PercentPanel counter : counters) {
                        update(counter);
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        private void update(PercentPanel counter) throws InterruptedException {
            long c = counter.sink.correct;
            long i = counter.sink.incorrect;
            long t = c + i;
            counter.streamLength.setText(BITS_FORMAT.format(t) + " bits");
            double faults = (t == 0 ? 0 : ((double) i) / t);
            Color color = new Color(((float) faults), ((float) (1 - faults)), 0);
            if (faults > MIN_SMALL || faults == 0) {
                counter.faultPercentage.setText(PERCENT_FORMAT.format(faults));
            } else {
                counter.faultPercentage.setText(SMALL_PERCENT_FORMAT.format(faults));
            }
            counter.faultPercentage.setForeground(color);
            Thread.sleep(100);
        }
    }

    @SuppressWarnings("unchecked")
    public void setFactories(Factory<DataPath> factory1, Factory<DataPath> factory2) {
        if (factory2 == null) {
            setFactories(new Factory[] { factory1 });
        } else {
            setFactories(new Factory[] { factory1, factory2 });
        }
    }

    public void setFactories(Factory<DataPath>... factories) {
        if (writeThread != null) {
            writeThread.interrupt();
        }
        for (PercentPanel counter : counters) {
            remove(counter);
        }
        List<PercentPanel> newCounters = new LinkedList<PercentPanel>();
        for (Factory<DataPath> factory : factories) {
            if (factory == null) continue;
            newCounters.add(new PercentPanel(factory));
        }
        counters = newCounters.toArray(new PercentPanel[newCounters.size()]);
        for (PercentPanel counter : counters) {
            add(counter, gbc);
        }
        writeThread = new WriteThread();
        writeThread.start();
    }

    private class PercentPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private JLabel streamLength, faultPercentage;

        private DataPath datapath;

        private DataCollector sink;

        public PercentPanel(Factory<DataPath> factory) {
            datapath = factory.create();
            sink = new DataCollector();
            datapath.setNext(sink);
            JPanel bitCounter = new JPanel();
            JPanel percentageCounter = new JPanel();
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            initBits(bitCounter);
            initPercentage(percentageCounter);
            add(bitCounter, c);
            add(percentageCounter, c);
        }

        private void initBits(JPanel panel) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            streamLength = new JLabel("0");
            Font f = streamLength.getFont().deriveFont(Font.BOLD, 30);
            streamLength.setFont(f);
            panel.add(new JLabel("Bits transmitted:"));
            panel.add(streamLength);
        }

        private void initPercentage(JPanel panel) {
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            faultPercentage = new JLabel(PERCENT_FORMAT.format(0));
            Font f = faultPercentage.getFont().deriveFont(Font.BOLD, 30);
            faultPercentage.setFont(f);
            faultPercentage.setForeground(Color.GREEN);
            panel.add(new JLabel("Percentage faults:"));
            panel.add(faultPercentage);
        }
    }
}
