package com.rbnb.plot;

import com.rbnb.utility.HostAndPortDialog;
import com.rbnb.utility.JInfoDialog;
import com.rbnb.utility.RBNBProcess;
import com.rbnb.utility.RBNBProcessInterface;
import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;

public class RBNBPlotMain extends Applet implements ActionListener, Runnable, Printable {

    public JFrame frame = null;

    boolean applicationRun = false;

    private RunModeCubby rmc = null;

    private JButton startButton = null;

    private LayoutCubby loc = null;

    private RBNBInterface rbnbInterface = null;

    private PlotsContainer pc = null;

    private ConfigCubby cc = null;

    private JRadioButtonMenuItem plot = null;

    private JRadioButtonMenuItem table = null;

    private JRadioButtonMenuItem rbCascade = null;

    private JRadioButtonMenuItem rbTile = null;

    private JRadioButtonMenuItem rbManual = null;

    private Cursor pointer = new Cursor(Cursor.DEFAULT_CURSOR);

    private Cursor hand = new Cursor(Cursor.HAND_CURSOR);

    private Cursor wait = new Cursor(Cursor.WAIT_CURSOR);

    private Thread runner;

    private UserControl uc = null;

    private Environment environment = new Environment();

    private RBNBProcessInterface target = null;

    public RBNBPlotMain() {
        applicationRun = false;
    }

    public RBNBPlotMain(String[] args) {
        this(args, null);
    }

    public RBNBPlotMain(String[] args, RBNBProcessInterface targetI) {
        target = targetI;
        int idx, idx1, idx2, idx3, idx4;
        environment.SCROLLGRIDLINES = true;
        for (idx = 0; idx < args.length; idx++) {
            if (args[idx].charAt(0) != '-') {
                System.err.println("Illegal command line argument:" + args[idx]);
                RBNBProcess.exit(-1, target);
            }
            if (args[idx].length() == 2) {
                idx1 = idx + 1;
                idx2 = 0;
            } else {
                idx1 = idx;
                idx2 = 2;
            }
            switch(args[idx].charAt(1)) {
                case 'r':
                    idx3 = args[idx1].substring(idx2).indexOf(':');
                    if (idx3 == -1) {
                        environment.HOST = args[idx1].substring(idx2);
                    } else {
                        if (idx2 < idx3) {
                            environment.HOST = args[idx1].substring(idx2, idx2 + idx3);
                        } else {
                            environment.HOST = "localhost";
                        }
                        environment.PORT = (new Integer(args[idx1].substring(idx2 + idx3 + 1))).intValue();
                    }
                    idx = idx1;
                    break;
                case 'S':
                    environment.STATICMODE = false;
                    break;
                case 'k':
                    environment.KILLRBNB = true;
                    break;
                case 'p':
                    idx3 = args[idx1].substring(idx2).indexOf(',');
                    try {
                        environment.POSITION_X = Integer.parseInt(args[idx1].substring(idx2, idx2 + idx3));
                        environment.POSITION_Y = Integer.parseInt(args[idx1].substring(idx2 + idx3 + 1));
                    } catch (NumberFormatException e) {
                    }
                    idx = idx1;
                    if (environment.POSITION_X < 0 || environment.POSITION_Y < 0) {
                        System.err.println("-p format incorrect.  Use x,y.");
                        System.err.println("RBNBPlot aborting.");
                        RBNBProcess.exit(-3, target);
                    }
                    break;
                case 's':
                    idx3 = args[idx1].substring(idx2).indexOf(',');
                    idx4 = args[idx1].substring(idx2).lastIndexOf(',');
                    double min = 1, max = 0;
                    int div = 0;
                    try {
                        min = (new Double(args[idx1].substring(idx2, idx2 + idx3))).doubleValue();
                        max = (new Double(args[idx1].substring(idx2 + idx3 + 1, idx2 + idx4))).doubleValue();
                        div = Integer.parseInt(args[idx1].substring(idx2 + idx4 + 1));
                    } catch (NumberFormatException e) {
                    }
                    if (div < 0 || min >= max) {
                        System.err.println("-s format incorrect.  Use min,max,div.");
                        System.err.println("RBNBPlot aborting.");
                        RBNBProcess.exit(-3, target);
                    }
                    environment.SCALE_MIN = min;
                    environment.SCALE_MAX = max;
                    environment.SCALE_DIV = div;
                    idx = idx1;
                    break;
                case 'd':
                    environment.DURATION = new Time((new Double(args[idx1].substring(idx2))).doubleValue());
                    idx = idx1;
                    break;
                case 'D':
                    environment.MAXDURATION = new Time((new Double(args[idx1].substring(idx2))).doubleValue());
                    idx = idx1;
                    break;
                case 'g':
                    environment.SCROLLGRIDLINES = false;
                    break;
                case 'u':
                    environment.TIME_LABEL = " " + args[idx1].substring(idx2);
                    idx = idx1;
                    break;
                case 'w':
                    environment.RTWAIT = Integer.parseInt(args[idx1].substring(idx2));
                    idx = idx1;
                    break;
                case 'c':
                    environment.SHOWALLCHANNELS = true;
                    try {
                        if (!args[idx1].substring(idx2).startsWith("-")) {
                            environment.CHANSPERDG = Integer.parseInt(args[idx1].substring(idx2));
                            idx = idx1;
                        }
                    } catch (Exception e) {
                    }
                    break;
                case 'n':
                    environment.STREAMING = true;
                    break;
                case 'N':
                    environment.STREAMING = true;
                    environment.SLAVEMODE = true;
                    break;
                case 'e':
                    environment.EXPORT = "jdbc:odbc:rbnb";
                    if (idx1 < args.length) {
                        String temp = args[idx1].substring(idx2);
                        if ((temp.charAt(0) != '-') && (temp != null)) {
                            environment.EXPORT = temp;
                            idx = idx1;
                        }
                    }
                    System.err.println("export to: " + environment.EXPORT);
                    break;
                case 'i':
                    environment.FOURBYTEASINTEGER = true;
                    break;
                default:
                    System.err.println("Unrecognized switch: " + args[idx]);
                    RBNBProcess.exit(-3, target);
            }
        }
        applicationRun = true;
    }

    public void init() {
        if (applicationRun) createFrame(); else {
            String parameter = null;
            parameter = getParameter("host");
            if (parameter != null) environment.HOST = parameter;
            parameter = getParameter("port");
            if (parameter != null) environment.PORT = Integer.parseInt(parameter);
            parameter = getParameter("staticmode");
            if (parameter != null && parameter.equals("true")) environment.STATICMODE = true;
            parameter = getParameter("killrbnb");
            if (parameter != null && parameter.equals("true")) environment.KILLRBNB = true;
            parameter = getParameter("position");
            if (parameter != null) {
                int idx = parameter.indexOf(',');
                try {
                    environment.POSITION_X = Integer.parseInt(parameter.substring(0, idx));
                    environment.POSITION_Y = Integer.parseInt(parameter.substring(idx + 1));
                } catch (NumberFormatException e) {
                }
                if (environment.POSITION_X < 0 || environment.POSITION_Y < 0) {
                    System.err.println("position format incorrect.  Use x,y.");
                    System.err.println("RBNBPlot aborting.");
                    RBNBProcess.exit(-3, target);
                }
            }
            parameter = getParameter("scaling");
            if (parameter != null) {
                int idx1 = parameter.indexOf(',');
                int idx2 = parameter.lastIndexOf(',');
                double min = 1, max = 0;
                int div = 0;
                try {
                    min = (new Double(parameter.substring(0, idx1))).doubleValue();
                    max = (new Double(parameter.substring(idx1 + 1, idx2))).doubleValue();
                    div = Integer.parseInt(parameter.substring(idx2 + 1));
                } catch (NumberFormatException e) {
                }
                if (div < 0 || min >= max) {
                    System.err.println("-s format incorrect.  Use min,max,div.");
                    System.err.println("RBNBPlot aborting.");
                    RBNBProcess.exit(-3, target);
                }
                environment.SCALE_MIN = min;
                environment.SCALE_MAX = max;
                environment.SCALE_DIV = div;
            }
            parameter = getParameter("duration");
            if (parameter != null) environment.DURATION = new Time((new Double(parameter).doubleValue()));
            parameter = getParameter("timelabel");
            if (parameter != null) environment.TIME_LABEL = " " + parameter;
            parameter = getParameter("rtwait");
            if (parameter != null) environment.RTWAIT = Integer.parseInt(parameter);
            parameter = getParameter("showallchannels");
            if (parameter != null && parameter.equals("true")) environment.SHOWALLCHANNELS = true;
            parameter = getParameter("streaming");
            if (parameter != null && parameter.equals("false")) environment.STREAMING = false;
            setLayout(new BorderLayout());
            startButton = new JButton("Start Plot");
            startButton.addActionListener(this);
            add(startButton, BorderLayout.CENTER);
            setVisible(true);
            setCursor(hand);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == plot) {
            loc.set(LayoutCubby.PlotMode);
            rbCascade.setEnabled(true);
            rbManual.setEnabled(true);
            return;
        } else if (e.getSource() == table) {
            loc.set(LayoutCubby.TableMode);
            rbCascade.setEnabled(false);
            rbManual.setEnabled(false);
            rbTile.setSelected(true);
            return;
        } else if (e.getSource() == rbCascade) {
            pc.setAuto(false);
            pc.cascade();
            return;
        } else if (e.getSource() == rbTile) {
            pc.setAuto(true);
            pc.tile(false);
            return;
        } else if (e.getSource() == rbManual) {
            pc.setAuto(false);
            return;
        }
        if (e.getSource() instanceof JButton) {
            setCursor(wait);
            if (frame == null) createFrame();
        } else if (e.getSource() instanceof JMenuItem) {
            JMenuItem mi = (JMenuItem) e.getSource();
            String arg = mi.getText();
            if (arg.equals("Exit")) {
                destroy();
                quitApp();
            } else if (arg.equals("Save Config")) {
                Properties config = new Properties();
                pc.getConfig(config);
                loc.set(LayoutCubby.SaveConfig);
                cc.setHash(config);
                cc.getChannel();
                config = (Properties) cc.getHash();
                try {
                    JFileChooser chooser = new JFileChooser(".");
                    chooser.setSelectedFile(new File("rbnbPlotConfig"));
                    chooser.setDialogTitle("Save Configuration");
                    int returnVal = chooser.showSaveDialog(frame);
                    if (returnVal != JFileChooser.APPROVE_OPTION) {
                        throw new Exception("File not selected");
                    }
                    String fileName = chooser.getSelectedFile().getAbsolutePath();
                    System.err.println("Save config to file " + fileName);
                    FileOutputStream fos = new FileOutputStream(fileName);
                    config.store(fos, "rbnbPlot Configuration File");
                    fos.close();
                } catch (Exception fe) {
                    System.err.println("Exception, configuration not saved.");
                    fe.printStackTrace();
                }
            } else if (arg.equals("Load Config")) {
                Properties config = new Properties();
                try {
                    JFileChooser chooser = new JFileChooser(".");
                    chooser.setSelectedFile(new File("rbnbPlotConfig"));
                    chooser.setDialogTitle("Load Configuration");
                    int returnVal = chooser.showOpenDialog(frame);
                    if (returnVal != JFileChooser.APPROVE_OPTION) {
                        throw new Exception("File not selected");
                    }
                    File loadFile = chooser.getSelectedFile();
                    if (!loadFile.exists()) {
                        throw new Exception("Specified config file does not exist.");
                    }
                    String fileName = loadFile.getAbsolutePath();
                    System.err.println("Load config from file " + fileName);
                    FileInputStream fis = new FileInputStream(fileName);
                    config.load(fis);
                } catch (Exception fe) {
                    System.err.println("Exception, configuration not loaded.");
                    fe.printStackTrace();
                    return;
                }
                loc.set(LayoutCubby.LoadConfig);
                cc.setHash(config);
                cc.getChannel();
                if (config == null) {
                    String[] aboutInfo = new String[2];
                    aboutInfo[0] = new String("Error reading configuration file.");
                    aboutInfo[1] = new String("Load aborted.");
                    JInfoDialog id = new JInfoDialog(frame, true, "Error", aboutInfo);
                    id.show();
                    id.dispose();
                } else {
                    if (config.containsKey("mode") && Integer.parseInt((String) config.get("mode")) == LayoutCubby.PlotMode) {
                        plot.setSelected(true);
                    } else {
                        table.setSelected(true);
                    }
                    pc.setConfig(config);
                    pc.setDisplayMode(Integer.parseInt((String) config.get("mode")));
                    pc.setDisplayGroup(Integer.parseInt((String) config.get("dg.current")));
                }
            } else if (arg.equals("Export to Clipboard")) {
                loc.set(LayoutCubby.ExportToCB);
            } else if (arg.equals("Export to DataTurbine")) {
                loc.set(LayoutCubby.ExportToDT);
            } else if (arg.equals("Export to Matlab")) {
                loc.set(LayoutCubby.ExportToMatlab);
            } else if (arg.equals("Print")) {
                printScreen();
            } else if (arg.equals("Open RBNB")) {
                HostAndPortDialog hapd = new HostAndPortDialog(frame, true, "RBNB", "Specify RBNB Connection", environment.HOST, environment.PORT, applicationRun);
                hapd.show();
                if (hapd.state == HostAndPortDialog.OK) {
                    environment.HOST = new String(hapd.machine);
                    environment.PORT = hapd.port;
                    loc.set(LayoutCubby.OpenRBNB);
                    frame.setCursor(wait);
                    frame.setTitle("rbnbPlot by Creare " + Environment.VERSION + " (connecting to " + environment.HOST + ":" + environment.PORT + "...)");
                    if (runner == null || !runner.isAlive()) {
                        runner = new Thread(this);
                        runner.start();
                    }
                }
                hapd.dispose();
            } else if (arg.equals("Refresh")) {
                loc.set(LayoutCubby.RefreshRBNB);
            } else if (arg.equals("Close RBNB")) {
                loc.set(LayoutCubby.CloseRBNB);
                frame.setTitle("rbnbPlot by Creare (no connection)");
            } else if (arg.equals("About")) {
                System.err.println("rbnbPlot by Creare, version " + Environment.VERSION);
                String[] aboutInfo = new String[3];
                aboutInfo[0] = new String("rbnbPlot by Creare, Development Version");
                aboutInfo[1] = new String("Copyright 1998-2005 Creare, Inc.");
                aboutInfo[2] = new String("All Rights Reserved");
                JInfoDialog id = new JInfoDialog(frame, true, "About", aboutInfo);
            } else if (arg.equals("OnLine Documentation")) {
                if (applicationRun) {
                    Runtime rt = Runtime.getRuntime();
                    try {
                        Process p = rt.exec("C:\\u\\SDP\\Product\\RBNB\\V1.0\\browser.bat http://outlet.creare.com/rbnb");
                    } catch (IOException ioe) {
                        System.err.println("cannot create process!");
                        ioe.printStackTrace();
                    }
                }
            }
        }
    }

    private void createFrame() {
        loc = new LayoutCubby();
        RBNBCubby rbc = new RBNBCubby();
        PosDurCubby pdc = new PosDurCubby();
        rmc = new RunModeCubby(environment.STATICMODE);
        cc = new ConfigCubby();
        setFont(Environment.FONT12);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        JMenuBar mb = new JMenuBar();
        mb.setFont(Environment.FONT12);
        JMenu file = new JMenu("File");
        file.setFont(Environment.FONT12);
        JMenuItem open = new JMenuItem("Open RBNB");
        open.setFont(Environment.FONT12);
        open.addActionListener(this);
        file.add(open);
        JMenuItem close = new JMenuItem("Close RBNB");
        close.setFont(Environment.FONT12);
        close.addActionListener(this);
        file.add(close);
        file.addSeparator();
        JMenuItem load = new JMenuItem("Load Config");
        load.setFont(Environment.FONT12);
        load.setEnabled(true);
        load.addActionListener(this);
        file.add(load);
        JMenuItem save = new JMenuItem("Save Config");
        save.setFont(Environment.FONT12);
        save.setEnabled(true);
        save.addActionListener(this);
        file.add(save);
        file.addSeparator();
        JMenuItem export = new JMenuItem("Export to Clipboard");
        export.setFont(Environment.FONT12);
        export.setEnabled(false);
        export.addActionListener(this);
        file.add(export);
        JMenuItem copyToDT = new JMenuItem("Export to DataTurbine");
        copyToDT.setFont(Environment.FONT12);
        copyToDT.setEnabled(true);
        copyToDT.addActionListener(this);
        file.add(copyToDT);
        JMenuItem exportMatlab = new JMenuItem("Export to Matlab");
        exportMatlab.setFont(Environment.FONT12);
        exportMatlab.setEnabled(true);
        exportMatlab.addActionListener(this);
        file.add(exportMatlab);
        JMenuItem print = new JMenuItem("Print");
        print.setFont(Environment.FONT12);
        print.setEnabled(true);
        print.addActionListener(this);
        file.add(print);
        file.addSeparator();
        JMenuItem exit = new JMenuItem("Exit");
        exit.setFont(Environment.FONT12);
        exit.addActionListener(this);
        file.add(exit);
        JMenu mode = new JMenu("Mode");
        mode.setFont(Environment.FONT12);
        plot = new JRadioButtonMenuItem("Plot");
        plot.setFont(Environment.FONT12);
        plot.setSelected(true);
        table = new JRadioButtonMenuItem("Table");
        table.setFont(Environment.FONT12);
        ButtonGroup group = new ButtonGroup();
        group.add(plot);
        group.add(table);
        mode.add(plot);
        mode.add(table);
        plot.addActionListener(this);
        table.addActionListener(this);
        JMenu window = new JMenu("Window");
        window.setFont(Environment.FONT12);
        rbCascade = new JRadioButtonMenuItem("Cascade");
        rbCascade.setFont(Environment.FONT12);
        rbTile = new JRadioButtonMenuItem("Tile", true);
        rbTile.setFont(Environment.FONT12);
        rbManual = new JRadioButtonMenuItem("Manual");
        rbManual.setFont(Environment.FONT12);
        ButtonGroup windowgroup = new ButtonGroup();
        windowgroup.add(rbCascade);
        windowgroup.add(rbTile);
        windowgroup.add(rbManual);
        window.add(rbCascade);
        window.add(rbTile);
        window.add(rbManual);
        rbCascade.addActionListener(this);
        rbTile.addActionListener(this);
        rbManual.addActionListener(this);
        JMenu help = new JMenu("Help");
        help.setFont(Environment.FONT12);
        JMenuItem about = new JMenuItem("About");
        about.setFont(Environment.FONT12);
        about.addActionListener(this);
        help.add(about);
        mb.add(file);
        mb.add(mode);
        mb.add(window);
        mb.add(help);
        frame = new LWFrame("rbnbPlot by Creare");
        if (environment.HOST == null) frame.setTitle("rbnbPlot by Creare (no connection)"); else {
            frame.setTitle("rbnbPlot by Creare (connecting to " + environment.HOST + ":" + environment.PORT + "...)");
            if (runner == null || !runner.isAlive()) {
                runner = new Thread(this);
                runner.start();
            }
        }
        frame.addNotify();
        frame.setLocation(getFrameLocation());
        frame.setSize(getFrameSize());
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new CloseClass());
        frame.setJMenuBar(mb);
        frame.getContentPane().setLayout(new BorderLayout());
        pc = new PlotsContainer(rbc, pdc, environment);
        rbnbInterface = new RBNBInterface(this, pc, rmc, loc, rbc, pdc, cc, environment);
        uc = new UserControl(frame, rmc, loc, rbc, pdc, environment);
        Thread rbnbThread = new Thread(rbnbInterface, "rbnbThread");
        rbnbThread.start();
        frame.getContentPane().add(uc, BorderLayout.NORTH);
        frame.getContentPane().add(pc, BorderLayout.CENTER);
        frame.validate();
        frame.show();
    }

    public void destroy() {
        if (rmc != null) {
            rmc.set(RunModeDefs.quit, true);
            rmc = null;
        }
        if (uc != null) {
            uc.clearChannelDialog();
            uc = null;
        }
        if (frame != null) {
            frame.setVisible(false);
            frame = null;
        }
    }

    public void quitApp() {
        destroy();
        if (applicationRun) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.err.println("thread sleep error: " + e);
            }
            RBNBProcess.exit(0, target);
        } else {
            setCursor(hand);
        }
    }

    public void printScreen() {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(this);
        if (pj.printDialog()) {
            try {
                pj.print();
            } catch (Exception e) {
                System.err.println("Printing exception " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public int print(Graphics g, PageFormat format, int pageNumber) {
        if (pageNumber != 0) return Printable.NO_SUCH_PAGE;
        g.translate((int) format.getImageableX(), (int) format.getImageableY());
        Dimension frameSize = frame.getSize();
        double frameAspect = frameSize.getWidth() / frameSize.getHeight();
        double pageAspect = format.getImageableWidth() / format.getImageableHeight();
        if (frameAspect > pageAspect) {
            frame.setSize((int) format.getImageableWidth(), (int) (format.getImageableWidth() / frameAspect));
        } else {
            frame.setSize((int) (format.getImageableHeight() * frameAspect), (int) format.getImageableHeight());
        }
        frame.invalidate();
        frame.validate();
        frame.repaint();
        frame.printAll(g);
        frame.setSize(frameSize);
        frame.invalidate();
        frame.validate();
        frame.repaint();
        return Printable.PAGE_EXISTS;
    }

    public void run() {
        int count = 0;
        Boolean status = null;
        while ((status = loc.getStatus()) == null) {
            if (count++ > 600) {
                status = new Boolean(false);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }
        if (status.booleanValue() == true) {
            frame.setTitle("rbnbPlot by Creare (connected to " + environment.HOST + ":" + environment.PORT + ")");
            if (uc == null) try {
                Thread.currentThread().sleep(1000);
            } catch (Exception e) {
            }
            uc.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Channels"));
        } else {
            String[] aboutInfo = new String[2];
            aboutInfo[0] = new String("Failed to connect to");
            aboutInfo[1] = new String(environment.HOST + ":" + environment.PORT);
            JInfoDialog id = new JInfoDialog(frame, true, "Error", aboutInfo);
            frame.setTitle("rbnbPlot by Creare (no connection)");
        }
        frame.setCursor(pointer);
    }

    class SlaveItemListener implements ItemListener {

        JMenu mode = null;

        Environment env = null;

        boolean streamingStart = false;

        SlaveItemListener(JMenu m, Environment e) {
            mode = m;
            env = e;
            streamingStart = env.STREAMING;
        }

        public void itemStateChanged(ItemEvent e) {
            JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
            env.SLAVEMODE = cbmi.getState();
            if (env.SLAVEMODE) {
                env.STREAMING = true;
                rmc.set(RunModeDefs.realTime, true);
            } else env.STREAMING = streamingStart;
        }
    }

    class CloseClass extends WindowAdapter {

        public void windowClosing(WindowEvent evt) {
            destroy();
            quitApp();
        }
    }

    class LWFrame extends JFrame {

        public LWFrame(String title) {
            super(title);
            java.net.URL url = this.getClass().getResource("/images/whirligig.gif");
            if (url != null) setIconImage(new javax.swing.ImageIcon(url).getImage());
        }

        public void update(Graphics g) {
            paint(g);
        }
    }

    public void start() {
    }

    public static void main(String[] args) {
        new AppletFrame(new RBNBPlotMain(args), 300, 300, false);
    }

    private Dimension getFrameSize() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension sz = tk.getScreenSize();
        sz.width /= 2;
        sz.height = sz.height * 3 / 4;
        return (sz);
    }

    private Point getFrameLocation() {
        if (environment.POSITION_X == -1 || environment.POSITION_Y == -1) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension sz = tk.getScreenSize();
            return (new Point(sz.width / 4, sz.height / 8));
        } else return (new Point(environment.POSITION_X, environment.POSITION_Y));
    }
}
