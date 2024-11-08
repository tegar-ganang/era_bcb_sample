package org.kineticsystem.commons.data.demo;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.kineticsystem.commons.data.demo.panels.*;
import org.kineticsystem.commons.data.model.*;
import org.kineticsystem.commons.data.model.swing.*;
import org.kineticsystem.commons.layout.*;
import org.kineticsystem.commons.random.*;
import org.kineticsystem.commons.random.bean.*;
import org.kineticsystem.commons.threads.*;

/**
 * The application main frame.
 * @author Giovanni Remigi
 * $Revision: 169 $
 */
public class MainFrame extends JFrame {

    /** Serial version number. */
    private static final long serialVersionUID = 1L;

    /** The initial size of the list to be randomly modified. */
    private static final int LIST_INITIAL_SIZE = 1000;

    /** A list containing a set of thread working on the same contact list. */
    private LinkedList<RandomContactWriter> writers;

    /** Button used to reload all test data. */
    private JButton loadButton;

    /** Button used to start a thread randomly working on the contact list. */
    private JButton startButton;

    /** Button used to stop all threads randomly working on the contact list. */
    private JButton stopButton;

    /** This is the main list modified by many threads at the same time.  */
    private ActiveList<RandomContact> source;

    private ActiveList<RandomContact> buffer;

    /** A label used to show performance information. */
    private JLabel infoLabel;

    /**
     * A thread used to monitor the performance of the main list being modified
     * by many threads at the same time.
     */
    private PerformanceMonitor performanceMonitor1;

    /**
     * A thread used to monitor the performance of the buffered list build on
     * the top of the main list being modified by many threads at the same time.
     */
    private PerformanceMonitor performanceMonitor2;

    /** 
     * Thread used to collect information from the performance monitors and
     * update a label presenting these information to the user.
     */
    private Thread performanceReader;

    /** Default constructor. */
    public MainFrame() {
        source = new DefaultActiveList<RandomContact>();
        buffer = new DataList<RandomContact>(source);
        infoLabel = new JLabel();
        performanceMonitor1 = new PerformanceMonitor(1000);
        performanceMonitor2 = new PerformanceMonitor(1000);
        performanceMonitor1.start();
        performanceMonitor2.start();
        source.addActiveListListener(performanceMonitor1);
        buffer.addActiveListListener(performanceMonitor2);
        performanceReader = new Thread() {

            int seq = 0;

            public void run() {
                while (!isInterrupted()) {
                    final PerformanceMonitor.Info info1 = performanceMonitor1.getInfo();
                    final PerformanceMonitor.Info info2 = performanceMonitor2.getInfo();
                    Runnable runner = new Runnable() {

                        public void run() {
                            String text = seq++ + ": " + "src events=" + info1.getEvents() + ", " + "dst events=" + info2.getEvents() + ", " + "src changes=" + info1.getChanges() + ", " + "dst changes=" + info2.getChanges();
                            infoLabel.setText(text);
                        }
                    };
                    EventQueue.invokeLater(runner);
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) {
                        interrupt();
                    }
                }
            }
        };
        performanceReader.start();
        writers = new LinkedList<RandomContactWriter>();
        loadButton = new JButton();
        loadButton.setToolTipText("Reload test data.");
        loadButton.setText("Load data");
        loadButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                loadTestData();
            }
        });
        startButton = new JButton();
        startButton.setToolTipText("Execute a new thread.");
        startButton.setText("Start thread (" + writers.size() + ")");
        startButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                RandomContactWriter writer = new RandomContactWriter();
                writers.add(writer);
                writer.setList(source);
                writer.setPriority(Thread.NORM_PRIORITY);
                writer.start();
                startButton.setText("Start (" + writers.size() + ")");
            }
        });
        stopButton = new JButton("Stop threads");
        stopButton.setToolTipText("Stop all running threads.");
        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Iterator<RandomContactWriter> iter = writers.iterator();
                while (iter.hasNext()) {
                    RandomContactWriter writer = iter.next();
                    writer.interrupt();
                    iter.remove();
                }
                startButton.setText("Start (" + writers.size() + ")");
            }
        });
        UniqueListPane listPane = new UniqueListPane(buffer);
        AggregationChartPane chartPane = new AggregationChartPane(buffer);
        ContactFilterPane filterPane = new ContactFilterPane(buffer);
        NavigatorPane navigatorPane = new NavigatorPane(buffer);
        AggregationTablePane tablePane = new AggregationTablePane(buffer);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Filter table", filterPane);
        tabbedPane.add("Unique list", listPane);
        tabbedPane.add("Aggregation table", tablePane);
        tabbedPane.add("Aggregation chart", chartPane);
        tabbedPane.add("Navigator pane", navigatorPane);
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        buttonPane.add(loadButton);
        buttonPane.add(startButton);
        buttonPane.add(stopButton);
        Cell cell = new Cell();
        TetrisLayout mainLayout = new TetrisLayout(3, 1);
        mainLayout.setRowWeight(0, 100);
        mainLayout.setRowWeight(1, 0);
        mainLayout.setRowWeight(2, 0);
        JPanel main = new JPanel();
        main.setLayout(mainLayout);
        main.add(tabbedPane, cell);
        main.add(buttonPane, cell);
        main.add(infoLabel, cell);
        setContentPane(main);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        WindowTerminator terminator = new WindowTerminator(this);
        addWindowListener(terminator);
    }

    /** {@inheritDoc} */
    public void dispose() {
        Iterator<RandomContactWriter> iter = writers.iterator();
        while (iter.hasNext()) {
            RandomContactWriter writer = iter.next();
            writer.interrupt();
            iter.remove();
        }
        performanceMonitor1.interrupt();
        performanceMonitor2.interrupt();
        performanceReader.interrupt();
        super.dispose();
    }

    /** Reload the random test data. */
    private void loadTestData() {
        source.getReadWriteLock().writeLock().lock();
        try {
            source.clear();
        } finally {
            source.getReadWriteLock().writeLock().unlock();
        }
        final RandomContactGenerator contactGen = new RandomContactGenerator();
        MonitoredThread monitoredThread = new MonitoredThread() {

            private int seq = 0;

            public void run() {
                while (!isInterrupted() && (seq < LIST_INITIAL_SIZE)) {
                    RandomContact contact = contactGen.generateContact();
                    source.getReadWriteLock().writeLock().lock();
                    try {
                        source.add(contact);
                        seq++;
                    } finally {
                        source.getReadWriteLock().writeLock().unlock();
                    }
                    try {
                        sleep(1);
                    } catch (InterruptedException ex) {
                        interrupt();
                    }
                }
            }

            public MonitorInfo getInfo() {
                MonitorInfo info = new MonitorInfo();
                info.setValue(100 * seq / LIST_INITIAL_SIZE);
                info.setIndeterminate(false);
                info.setInterruptable(true);
                info.setMessage("Loading test data...");
                return info;
            }
        };
        monitoredThread.start();
        JMonitorWindow mw = new JMonitorWindow();
        mw.setModal(true);
        mw.setLocationRelativeTo(this);
        DefaultMonitor monitor = new DefaultMonitor(monitoredThread);
        monitor.addMonitorListener(mw);
        monitor.addInterrupter(mw);
        monitor.start();
        mw.setVisible(true);
    }
}
