package edu.berkeley.cs.db.yfilter.icdedemo;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;

public class Framework extends JFrame {

    JTextArea textarea;

    ICDEDemo demo;

    FrameworkMenuBar menubar;

    JButton run;

    JButton pause;

    JButton cycle;

    JButton step;

    JButton query;

    JButton doc;

    JScrollPane scrollPanel;

    XMLViewer xmlviewer;

    QueryViewer queryviewer;

    SystemMonitor sm;

    PerformanceMonitor pm;

    WorkloadMonitor wm;

    /**
   *  View all queries in the system.
   */
    JButton allqueries;

    XMLViewer allqueryviewer;

    public Framework() {
        super("YFilter Demo");
        textarea = new JTextArea("YFilter Demo\n", 30, 25);
        textarea.setLineWrap(true);
        textarea.setMargin(new Insets(5, 5, 5, 5));
        textarea.setEditable(false);
        textarea.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPanel = new JScrollPane(textarea);
        menubar = new FrameworkMenuBar(this);
        setJMenuBar(menubar);
        run = new JButton("Run");
        run.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                runDemo();
            }
        });
        run.setEnabled(false);
        pause = new JButton("Pause");
        pause.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pauseDemo();
            }
        });
        pause.setEnabled(false);
        cycle = new JButton("One cycle");
        cycle.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cycleDemo();
            }
        });
        cycle.setEnabled(false);
        step = new JButton("Step Element");
        step.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                stepDemo();
            }
        });
        step.setEnabled(false);
        query = new JButton("View matched queries");
        query.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                queryviewer.show();
            }
        });
        query.setEnabled(false);
        allqueryviewer = new XMLViewer("View all queries");
        WindowListener aqvl = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                allqueryviewer.hide();
            }
        };
        allqueryviewer.addWindowListener(aqvl);
        allqueries = new JButton("View all queries");
        allqueries.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                allqueryviewer.show(demo.getCurrentQueries());
            }
        });
        allqueries.setEnabled(false);
        xmlviewer = new XMLViewer("View XML");
        WindowListener xvl = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                xmlviewer.hide();
            }
        };
        xmlviewer.addWindowListener(xvl);
        doc = new JButton("View XML");
        doc.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                xmlviewer.show(demo.getCurrentXML());
            }
        });
        doc.setEnabled(false);
        JPanel controlPanel = new JPanel();
        TitledBorder controlTitle = BorderFactory.createTitledBorder("Controls");
        controlTitle.setTitleJustification(TitledBorder.CENTER);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.setBorder(controlTitle);
        controlPanel.add(run);
        controlPanel.add(pause);
        controlPanel.add(cycle);
        controlPanel.add(step);
        controlPanel.setEnabled(false);
        JPanel resultPanel = new JPanel();
        TitledBorder resultTitle = BorderFactory.createTitledBorder("Results");
        resultTitle.setTitleJustification(TitledBorder.CENTER);
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.X_AXIS));
        resultPanel.setBorder(resultTitle);
        resultPanel.add(query);
        resultPanel.add(allqueries);
        resultPanel.add(doc);
        resultPanel.setEnabled(false);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(controlPanel, BorderLayout.EAST);
        buttonPanel.add(resultPanel, BorderLayout.WEST);
        buttonPanel.setPreferredSize(new Dimension(700, 100));
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setPreferredSize(new Dimension(800, 600));
        pane.add(buttonPanel, BorderLayout.NORTH);
        pane.add(scrollPanel, BorderLayout.SOUTH);
        setContentPane(pane);
    }

    public void showSystemMonitor() {
        sm.show();
    }

    public void showPerformanceMonitor() {
        pm.show();
    }

    public void showWorkloadMonitor() {
        wm.show();
    }

    public void setXML(String xml) {
        xmlviewer.setXML(xml);
    }

    public void searchXML(String elementName) {
        xmlviewer.search(elementName);
    }

    public void write(String text) {
        synchronized (textarea) {
            textarea.append(text);
            textarea.setCaretPosition(textarea.getDocument().getLength());
        }
    }

    public void writeln(String text) {
        synchronized (textarea) {
            textarea.append(text + "\n");
            textarea.setCaretPosition(textarea.getDocument().getLength());
        }
    }

    public void clearText() {
        synchronized (textarea) {
            textarea.setText("");
            textarea.setCaretPosition(0);
        }
    }

    private void bulkLoad() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Bulk Load Queries from file (optional). ");
        TextFileFilter filter = new TextFileFilter();
        chooser.setFileFilter(filter);
        int returnVal = chooser.showDialog(this, "Load Queries (optional). ");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            writeln(chooser.getSelectedFile().getName() + " is selected to bulk load");
            demo.enqueueQueries(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void setDemo(String dtdfilename) {
        demo = new ICDEDemo(dtdfilename, this);
        textarea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        pm = new PerformanceMonitor(demo.getFilteringtime(), demo.getThroughput());
        WindowListener pml = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                pm.hide();
            }
        };
        pm.addWindowListener(pml);
        sm = new SystemMonitor();
        WindowListener sml = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                sm.hide();
            }
        };
        sm.addWindowListener(sml);
        wm = new WorkloadMonitor(demo.getEXfilter());
        WindowListener wml = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                wm.hide();
            }
        };
        wm.addWindowListener(wml);
        queryviewer = new QueryViewer(demo);
        WindowListener qvl = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                queryviewer.hide();
            }
        };
        queryviewer.addWindowListener(qvl);
        queryviewer.waitForDisplay();
        Thread initThread = new Thread() {

            public void run() {
                bulkLoad();
                demo.start();
                writeln("Demo is ready to be run.");
                menubar.enableRun();
                menubar.setModeEnabled(true);
                menubar.setParamEnabled(true);
                menubar.setMonitorEnabled(true);
                run.setEnabled(true);
                cycle.setEnabled(true);
                step.setEnabled(true);
                query.setEnabled(true);
                allqueries.setEnabled(true);
                doc.setEnabled(true);
                textarea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        };
        initThread.start();
    }

    public void selectDTDFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a DTD file");
        DTDFileFilter filter = new DTDFileFilter();
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            writeln(chooser.getSelectedFile().getName() + " is selected");
            menubar.disableSelect();
            setDemo(chooser.getSelectedFile().getAbsolutePath());
            writeln(chooser.getSelectedFile().getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(null, "Please select a DTD file", "Alert", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void runDemo() {
        demo.runAgain();
        writeln("Running Demo ...");
        queryviewer.waitForDisplay();
        menubar.enablePause();
        menubar.disableRun();
        run.setEnabled(false);
        pause.setEnabled(true);
        cycle.setEnabled(false);
        step.setEnabled(false);
    }

    public void pauseDemo() {
        demo.pause();
        writeln("Demo has been paused ...");
        menubar.enableRun();
        menubar.disablePause();
        run.setEnabled(true);
        pause.setEnabled(false);
        if (!demo.isBatchMode()) {
            cycle.setEnabled(true);
            step.setEnabled(true);
        }
    }

    public void cycleDemo() {
        writeln("Stepping Demo one cycle ...");
        queryviewer.waitForDisplay();
        menubar.disableRun();
        run.setEnabled(false);
        demo.cycle();
        menubar.enableRun();
        run.setEnabled(true);
    }

    public void stepDemo() {
        queryviewer.waitForDisplay();
        menubar.disableRun();
        run.setEnabled(false);
        demo.step();
        menubar.enableRun();
        run.setEnabled(true);
    }

    public void changeQueryRate() {
        String strQueryRate = JOptionPane.showInputDialog(this, "Please input new query rate");
        if (strQueryRate == null) {
            return;
        }
        try {
            double queryRate = Double.parseDouble(strQueryRate);
            demo.setQueryRate(queryRate);
            sm.changeQueryRate(queryRate);
            writeln("Query rate is changed to " + queryRate + " queries per ms");
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid query rate", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changeQueryBufferSize() {
        String strBufferSize = JOptionPane.showInputDialog(this, "Please input new query buffer size");
        if (strBufferSize == null) {
            return;
        }
        try {
            int bufferSize = Integer.parseInt(strBufferSize);
            if (bufferSize <= 0) {
                throw new NumberFormatException();
            }
            demo.changeQueryBufferSize(bufferSize);
            sm.changeQueryBufferSize(bufferSize);
            writeln("Query Buffer size is changed to " + bufferSize);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid query buffer size", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changeXMLBufferSize() {
        String strBufferSize = JOptionPane.showInputDialog(this, "Please input new xml buffer size");
        if (strBufferSize == null) {
            return;
        }
        try {
            int bufferSize = Integer.parseInt(strBufferSize);
            if (bufferSize <= 0) {
                throw new NumberFormatException();
            }
            demo.changeXMLBufferSize(bufferSize);
            sm.changeXMLBufferSize(bufferSize);
            writeln("XML Buffer size is changed to " + bufferSize);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid xml buffer size", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changeDynamicUpdate() {
        if (demo.isDynamicUpdateEnabled()) {
            demo.setDynamicUpdateEnabled(false);
            menubar.setDynamicUpdate(false);
            writeln("Dynamic Update is disallowed");
        } else {
            demo.setDynamicUpdateEnabled(true);
            menubar.setDynamicUpdate(true);
            writeln("Dynamic Update is allowed");
        }
    }

    public void updateXMLInBuffer(int no) {
        sm.changeXMLInBuffer(no);
    }

    public void updateFrame() {
        pm.update();
        wm.update();
        queryviewer.update(demo.getMatchedQueries());
        allqueryviewer.setXML(demo.getCurrentQueries());
    }

    public void changeBatchMode() {
        if (demo.isBatchMode()) {
            demo.setBatchMode(false);
            menubar.setBatchMode(false);
            cycle.setEnabled(true);
            step.setEnabled(true);
            writeln("Switched to One-XML Mode");
        } else {
            JFileChooser fc = new JFileChooser(".");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                demo.changeDirectory(fc.getSelectedFile().getAbsolutePath());
            } else {
                JOptionPane.showMessageDialog(this, "Did not select directory", "Cancelled", JOptionPane.ERROR_MESSAGE);
                return;
            }
            demo.setBatchMode(true);
            menubar.setBatchMode(true);
            cycle.setEnabled(false);
            step.setEnabled(false);
            writeln("Switched to Batch Mode");
        }
    }

    public void changeQueryDepth() {
        String str = JOptionPane.showInputDialog(this, "Please input new Query Depth for Query Generator");
        if (str == null) {
            return;
        }
        try {
            int value = Integer.parseInt(str);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            demo.m_maxDepth = value;
            sm.changeQueryMaxDepth(value);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changeWildcard() {
        String str = JOptionPane.showInputDialog(this, "Please input new wildcard probability for Query Generator");
        if (str == null) {
            return;
        }
        try {
            double value = Double.parseDouble(str);
            if (value < 0 || value > 1) {
                throw new NumberFormatException();
            }
            demo.m_wildcard = value;
            sm.changeQueryWildcard(value);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changeDSlash() {
        String str = JOptionPane.showInputDialog(this, "Please input new dSlash for Query Generator");
        if (str == null) {
            return;
        }
        try {
            double value = Double.parseDouble(str);
            if (value < 0 || value > 1) {
                throw new NumberFormatException();
            }
            demo.m_dSlash = value;
            sm.changeQueryDSlash(value);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changePredProb() {
        String str = JOptionPane.showInputDialog(this, "Please input new predicate Probability for Query Generator");
        if (str == null) {
            return;
        }
        try {
            double value = Double.parseDouble(str);
            if (value < 0 || value > 1) {
                throw new NumberFormatException();
            }
            demo.m_predProb = value;
            sm.changeQueryPredicate(value);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changeLevelDist() {
        String str = JOptionPane.showInputDialog(this, "Please input new levelDIst for Query Generator");
        if (str == null) {
            return;
        }
        char value = str.charAt(0);
        demo.m_levelDist = value;
        sm.changeQueryLevelDist(value);
    }

    public void changeNestedPath() {
        String str = JOptionPane.showInputDialog(this, "Please input new nested path for Query Generator");
        if (str == null) {
            return;
        }
        try {
            double value = Double.parseDouble(str);
            if (value < 0 || value > 1) {
                throw new NumberFormatException();
            }
            demo.m_nestedPath = value;
            sm.changeQueryNestedPath(value);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void changeXMLLevel() {
        String str = JOptionPane.showInputDialog(this, "Please input new Query Depth for Query Generator");
        if (str == null) {
            return;
        }
        try {
            int value = Integer.parseInt(str);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            demo.m_maxLevel = value;
            sm.changeXMLMaxLevel(value);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid input", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            ICDEDemo.m_yfilter_home = args[0];
        }
        JFrame frame = new Framework();
        WindowListener l = new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        };
        frame.addWindowListener(l);
        frame.pack();
        frame.setVisible(true);
    }
}
