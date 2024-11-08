import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JList;
import java.io.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.tools.ant.*;

public class AntRunner {

    private static final String TASK_LIST = "TaskList";

    private static final String LOG_FILE = "log.txt";

    private static final String DEFAULT_CONFIG_XML = "config.xml";

    private static final String VERSION = "0.3";

    private static final String COMMENT_CREATED_BY = "<!--\r\ncreated with jantrunner\r\n-->\r\n";

    String ANT_FILES_PATH = null;

    String ANTRUNNER_BATCH_FILE = "antrunner.xml";

    String ANTRUNNER_BATCH_TASK = "batch";

    String STATISTICS_DB = "antrunner.db";

    int message_output_level = Project.MSG_INFO;

    Project antproject;

    Project project;

    org.apache.tools.ant.BuildLogger logger = null;

    String loggerClass;

    String last_descr_build_file = "";

    JFrame frame;

    JPanel panelCmdButtons;

    JButton btnAntBatchRun;

    JList lstBatch;

    JCheckBox chkPoll;

    JTabbedPane tabbedPane;

    Timer timer = null;

    JTextField txtTimerField;

    JTextField loggerTextField;

    JPanel configPanel;

    AntRunnerTab moreTab;

    JSplitPane splitPane;

    private Statistics statistics;

    /**
	 * Add the given info to the log file.
	 */
    public void addToLog(String info, int loglevel) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(LOG_FILE, true));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
            Date dt = new java.util.Date();
            String s = formatter.format(dt);
            s = s + " " + System.getenv("COMPUTERNAME") + " " + info;
            out.write(s);
            if (project != null) project.log(s); else System.out.println(s);
            out.newLine();
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
	 * execute given program and wait for process
	 */
    public String exec(String path) {
        return exec(path, true);
    }

    /**
	 * execute given program
	 * 
	 * @param path
	 * @param wait
	 *            if true wait for process to exit
	 * @return
	 */
    public String exec(String path, boolean wait) {
        String out = "";
        try {
            String line;
            Process p = Runtime.getRuntime().exec(path);
            if (wait) {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = input.readLine()) != null) {
                    if (out != "") out += "\r\n";
                    out += line;
                    System.out.println(line);
                    line = err.readLine();
                    if (line != null) System.err.println(line);
                }
                input.close();
            }
        } catch (Exception err) {
            System.err.println("ERROR");
            err.printStackTrace();
        }
        return out;
    }

    /**
	 * edit the given file with the default system editor
	 * 
	 * @param filename
	 */
    public void editFile(String filename) {
        boolean startNotepad = false;
        try {
            Desktop desktop = null;
            if (Desktop.isDesktopSupported()) {
                desktop = Desktop.getDesktop();
                File f = new File(filename);
                desktop.edit(f.getCanonicalFile());
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
            startNotepad = true;
        }
        if (startNotepad) {
            try {
                String s = "notepad.exe " + new File(filename).getCanonicalPath();
                exec(s);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
	 * rename the given file (append date)
	 * 
	 * @param filepath
	 */
    void renameYYYYMMDD(String filepath) {
        File file = new File(filepath);
        String filename = file.getName();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        Date dt = new java.util.Date(file.lastModified());
        String dateString = formatter.format(dt);
        file.renameTo(new File(file.getParent(), filename.substring(0, filename.lastIndexOf('.') + 1) + dateString + filename.substring(filename.lastIndexOf('.'))));
    }

    /**
	 * This method returns the selected radio button in a button group public
	 * 
	 * @param group
	 * @return
	 */
    JRadioButton getSelection(ButtonGroup group) {
        for (Enumeration e = group.getElements(); e.hasMoreElements(); ) {
            JRadioButton b = (JRadioButton) e.nextElement();
            if (b.getModel() == group.getSelection()) {
                return b;
            }
        }
        return null;
    }

    /**
	 * write the given text to file
	 * 
	 * @param filename
	 * @param text
	 */
    void writeFile(String filename, String text) {
        try {
            File file = new File(filename);
            Writer output = new BufferedWriter(new FileWriter(file));
            output.write(text);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * read in the given text file
	 * 
	 * @param filename
	 * @return
	 */
    public String readFile(String filename) {
        String text = "";
        try {
            File file = new File(filename);
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                text += str + "\r\n";
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    /**
	 * copy the given text file
	 * 
	 * @param src
	 * @param dest
	 */
    void copyFileAscii(String src, String dest) {
        try {
            File inputFile = new File(src);
            File outputFile = new File(dest);
            FileReader in = new FileReader(inputFile);
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }

    String Hex2Ascii(String s) {
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < s.length(); i += 2) {
            int c = Integer.parseInt(s.substring(i, Math.min(i + 2, s.length())), 16);
            ascii.append(c == 0 ? '?' : (char) c);
        }
        return ascii.toString();
    }

    /**
	 * Execute the selected targets of the given AntRunnerComponent in a thread. Change the background
	 * color of the given button depending on the status
	 * @param antRunnerComponent
	 * @param button
	 */
    public void executeSelectedTargets(AntRunnerComponent antRunnerComponent, JButton button) {
        ExecuteThread t = new ExecuteThread(this, antRunnerComponent, button);
        t.start();
    }

    /**
	 * Change the color of the given button to green (ok = true), otherwise to
	 * red.
	 */
    void setButtonBgColor(JButton button, boolean ok) {
        if (ok) button.setBackground(Color.GREEN); else button.setBackground(Color.RED);
    }

    /**
	 * Set the color of the buton to default system color.
	 * 
	 * @param button
	 */
    void clearButtonBgColor(JButton button) {
        button.setBackground((Color) Toolkit.getDefaultToolkit().getDesktopProperty("control"));
    }

    /**
	 * Execute the ant target of the given build file.
	 * 
	 * @param ant
	 *            build filename
	 * @param ant
	 *            target
	 * @return true if successfully
	 */
    public boolean executeAntTarget(String filename, String target, BuildListener listener) {
        boolean ret = true;
        Date dateStart = new Date();
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File buildFile = new File(filename);
        project = new Project();
        project.setUserProperty("ant.file", buildFile.getAbsolutePath());
        if (logger instanceof DefaultLogger) {
            logger.setErrorPrintStream(System.err);
            logger.setOutputPrintStream(System.out);
            logger.setMessageOutputLevel(message_output_level);
        }
        project.addBuildListener(logger);
        if (listener != null) project.addBuildListener(listener);
        try {
            project.fireBuildStarted();
            project.init();
            ProjectHelper helper = ProjectHelper.getProjectHelper();
            project.addReference("ant.projectHelper", helper);
            helper.parse(project, buildFile);
            project.executeTarget(target);
            project.fireBuildFinished(null);
            Date dateEnd = new Date();
            SimpleDateFormat formatNew = new SimpleDateFormat("s.SSS");
            Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            double duration = (dateEnd.getTime() - dateStart.getTime()) / 1000;
            statistics.addValues(formatter.format(dateEnd), filename, target, duration);
        } catch (Exception e) {
            project.fireBuildFinished(e);
            project = null;
            ret = false;
        }
        frame.setCursor(Cursor.getDefaultCursor());
        return ret;
    }

    public boolean executeAntTarget(String filename, String target) {
        return executeAntTarget(filename, target, null);
    }

    public Statistics getStatistics() {
        return statistics;
    }

    /**
	 * Returns the ant description of the given target.
	 * 
	 * @param file
	 * @param target
	 * @return
	 */
    String getAntDescription(String file, String target) {
        String s = "";
        try {
            File buildFile = new File(ANT_FILES_PATH, file);
            if (last_descr_build_file == "" || last_descr_build_file != file) {
                ProjectHelper helper = ProjectHelper.getProjectHelper();
                antproject = new Project();
                antproject.init();
                antproject.addReference("ant.projectHelper", helper);
                last_descr_build_file = file;
                helper.parse(antproject, buildFile);
            }
            Hashtable hash = antproject.getTargets();
            s = ((Target) hash.get(target)).getDescription();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return s;
    }

    /**
	 * global action handler
	 */
    ActionListener buttonHandler = new ActionListener() {

        public void actionPerformed(ActionEvent event) {
            if (event.getSource() instanceof AntRunnerButton) {
                AntRunnerButton btn = (AntRunnerButton) event.getSource();
                clearButtonBgColor(btn);
                boolean passed = executeAntTarget(new File(btn.file).getAbsolutePath(), btn.target);
                setButtonBgColor((JButton) btn, passed);
            }
        }
    };

    private void writeBatchFile() {
        String targets = "";
        for (int i = 0; i < ((DefaultListModel) lstBatch.getModel()).size(); i++) {
            String s = ((DefaultListModel) lstBatch.getModel()).get(i).toString();
            String file = s.substring(0, s.indexOf(";"));
            String target = s.substring(s.indexOf(";") + 1);
            if (i > 0) targets += "\r\n";
            targets += "  <ant antfile=\"" + file + "\" target=\"" + target + "\" />";
        }
        writeAntTaskfile(ANTRUNNER_BATCH_FILE, targets, ANTRUNNER_BATCH_TASK, "");
    }

    /**
	 * button handler for ant batch list
	 */
    ActionListener antBatchHandler = new ActionListener() {

        public void actionPerformed(ActionEvent event) {
            int idx = 0;
            if (event.getActionCommand() == "Run Batch") {
                writeBatchFile();
                setButtonBgColor((JButton) event.getSource(), executeAntTarget(ANTRUNNER_BATCH_FILE, ANTRUNNER_BATCH_TASK));
            } else if (event.getActionCommand() == "Up") {
                idx = lstBatch.getSelectedIndex();
                if (idx > 0) {
                    String old = ((DefaultListModel) lstBatch.getModel()).get(idx).toString();
                    ((DefaultListModel) lstBatch.getModel()).removeElementAt(idx);
                    ((DefaultListModel) lstBatch.getModel()).insertElementAt(old, idx - 1);
                    lstBatch.setSelectedIndex(idx - 1);
                }
            } else if (event.getActionCommand() == "Down") {
                idx = lstBatch.getSelectedIndex();
                if (idx < ((DefaultListModel) lstBatch.getModel()).size() - 1) {
                    String old = ((DefaultListModel) lstBatch.getModel()).get(idx).toString();
                    ((DefaultListModel) lstBatch.getModel()).removeElementAt(idx);
                    ((DefaultListModel) lstBatch.getModel()).insertElementAt(old, idx + 1);
                    lstBatch.setSelectedIndex(idx + 1);
                }
            } else if (event.getActionCommand() == "Remove") {
                idx = lstBatch.getSelectedIndex();
                if (idx >= 0) ((DefaultListModel) lstBatch.getModel()).remove(idx);
                if (idx > ((DefaultListModel) lstBatch.getModel()).size() - 1) idx = ((DefaultListModel) lstBatch.getModel()).size() - 1;
                if (((DefaultListModel) lstBatch.getModel()).size() > 0) lstBatch.setSelectedIndex(idx);
            }
        }
    };

    MouseMotionAdapter mouseMotionHandler = new MouseMotionAdapter() {

        public void mouseMoved(MouseEvent e) {
            if (e.getSource() instanceof AntTaskList) {
                System.out.println(((AntTaskList) e.getSource()).getFilename());
            }
            if (e.getSource() instanceof JList) {
                int idx = ((JList) e.getSource()).locationToIndex(e.getPoint());
                if (idx >= 0 && idx < ((DefaultListModel) ((JList) e.getSource()).getModel()).size()) {
                    String s = ((DefaultListModel) ((JList) e.getSource()).getModel()).get(idx).toString();
                    String filename = s.substring(0, s.indexOf(";"));
                    String task = s.substring(s.indexOf(";") + 1);
                    String description = getAntDescription(filename, task);
                    if (description != null) ((JList) e.getSource()).setToolTipText(description);
                }
            }
        }
    };

    /**
	 * Mouse handler: right click -> edit file
	 */
    MouseAdapter mouseHandler = new MouseAdapter() {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1 && (e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                JList lst = ((JList) e.getSource());
                if (lst == lstBatch) {
                    writeBatchFile();
                    editFile(ANTRUNNER_BATCH_FILE);
                }
            }
        }

        public void mouseMoved(MouseEvent e) {
            System.out.println(e.toString());
        }
    };

    ActionListener toolsHandler = new ActionListener() {

        public void actionPerformed(ActionEvent event) {
            if (event.getSource() == chkPoll) {
            }
        }
    };

    class MyTimerTask extends TimerTask {

        private String filename;

        private String target;

        private long delay;

        MyTimerTask(String filename, String target, long delay) {
            this.filename = filename;
            this.target = target;
            this.delay = delay;
        }

        public void run() {
            executeAntTarget(filename, target);
            setUpdateTimer(filename, target, delay);
        }
    }

    void setUpdateTimer(String file, String target, long delay_sec) {
        if (chkPoll != null) if (chkPoll.getModel().isSelected()) {
            if (timer == null) timer = new Timer(true);
            txtTimerField.setText(file + ";" + target + " (" + String.valueOf(delay_sec) + "sec)");
            timer.schedule(new MyTimerTask(file, target, delay_sec), delay_sec * 1000);
        } else {
            if (timer != null) timer.cancel();
            timer = null;
        }
    }

    String replace(String str, String pattern, String replace) {
        int start = 0;
        int index = 0;
        StringBuffer result = new StringBuffer();
        while ((index = str.indexOf(pattern, start)) >= 0) {
            result.append(str.substring(start, index));
            result.append(replace);
            start = index + pattern.length();
        }
        result.append(str.substring(start));
        return result.toString();
    }

    public JList getBatchList() {
        return lstBatch;
    }

    void writeAntTaskfile(String filename, String task, String taskname, String additional) {
        String antxml = "<project name=\"Run" + taskname + "\" default=\"" + taskname + "\" >\r\n" + COMMENT_CREATED_BY + additional + "<target name=\"" + taskname + "\">\r\n" + task + "\r\n</target>\r\n</project>";
        writeFile(filename, antxml);
    }

    private void readSingleBuildFile(String buildfile) {
        new AntRunnerTab(this, tabbedPane, new File(buildfile).getName(), TASK_LIST, buildfile, "");
    }

    /**
	 * Hide batch list, "+" tab and tools tab.
	 */
    private void showSimpleGui() {
        configPanel.setVisible(false);
    }

    private void readConfig(String xmlFile) {
        try {
            File file = new File(xmlFile);
            if (file.exists()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(xmlFile);
                NodeList nodes;
                if (doc.getElementsByTagName("buttons").item(0) != null) {
                    nodes = doc.getElementsByTagName("buttons").item(0).getChildNodes();
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Node node = nodes.item(i);
                        if (node instanceof Element) {
                            Element child = (Element) node;
                            JButton btn = new AntRunnerButton(child.getAttribute("name"), child.getAttribute("file"), child.getAttribute("execute"), child.getAttribute("description"));
                            btn.addActionListener(buttonHandler);
                            panelCmdButtons.add(btn);
                        }
                    }
                } else {
                    panelCmdButtons.setVisible(false);
                }
                nodes = doc.getElementsByTagName("options");
                if (nodes.getLength() > 0) {
                    Node node = nodes.item(0);
                    if (node instanceof Element) {
                        Element child = (Element) node;
                        loggerClass = child.getAttribute("logger");
                    }
                }
                nodes = doc.getElementsByTagName("tabs").item(0).getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    if (node instanceof Element) {
                        Element child = (Element) node;
                        new AntRunnerTab(this, tabbedPane, child.getAttribute("name"), child.getAttribute("type"), child.getAttribute("file"), child.getAttribute("source"));
                    }
                }
                nodes = doc.getElementsByTagName("timer");
                if (nodes.getLength() > 0) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Node node = nodes.item(i);
                        if (node instanceof Element) {
                            Element child = (Element) node;
                            setUpdateTimer(child.getAttribute("file"), child.getAttribute("execute"), Long.parseLong(child.getAttribute("interval")));
                        }
                    }
                } else {
                    chkPoll.setSelected(false);
                    chkPoll.setEnabled(false);
                }
            } else {
                System.err.print("File " + xmlFile + " not found!");
            }
        } catch (Exception e) {
            System.err.print(e.toString());
            System.exit(1);
        }
        try {
            if (loggerClass == null) loggerClass = "org.apache.tools.ant.DefaultLogger";
            Class c = Class.forName(loggerClass);
            logger = (BuildLogger) c.newInstance();
            loggerTextField.setText(logger.getClass().getCanonicalName());
        } catch (Exception e) {
            System.err.print(e.toString());
            System.exit(1);
        }
    }

    /**
	 * Check if given filename is an ant build file.
	 * @param filename
	 * @return true - ant file was supplied.
	 */
    public boolean checkAntFile(String filename) {
        File buildFile = new File(filename);
        Project p = new Project();
        p.setUserProperty("ant.file", buildFile.getAbsolutePath());
        boolean ant_file = true;
        try {
            p.init();
            ProjectHelper helper = ProjectHelper.getProjectHelper();
            p.addReference("ant.projectHelper", helper);
            helper.parse(p, buildFile);
        } catch (BuildException e) {
            ant_file = false;
        }
        return ant_file;
    }

    /**
	 * Launch the application.
	 */
    private static String[] parms;

    public static void main(String[] args) {
        parms = args;
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    AntRunner window = new AntRunner();
                    if (parms.length == 0) {
                        File f = new File(DEFAULT_CONFIG_XML);
                        if (!f.exists()) {
                            JFileChooser fc = new JFileChooser();
                            if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(null)) {
                                f = fc.getSelectedFile();
                            }
                        }
                        window.readConfig(f.getAbsolutePath());
                        window.AddAdvGui();
                    } else if (window.checkAntFile(parms[0])) {
                        window.readSingleBuildFile(parms[0]);
                    }
                    window.frame.pack();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
	 * Create the application.
	 */
    public AntRunner() {
        initialize();
        try {
            statistics = new Statistics(STATISTICS_DB);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void AddAdvGui() {
        moreTab = new AntRunnerTab(this, tabbedPane, "+", TASK_LIST, null, "");
        moreTab.setTransferHandler(new AntRunnerFileTransferHandler(moreTab.getTaskList()));
        tabbedPane.addTab("Config", configPanel);
    }

    /**
	 * Initialize the contents of the frame.
	 */
    private void initialize() {
        frame = new JFrame(String.format("jantrunner %s", VERSION));
        frame.getContentPane().setLayout(new BorderLayout(5, 5));
        panelCmdButtons = new JPanel();
        panelCmdButtons.setLayout(new FlowLayout());
        frame.getContentPane().add(panelCmdButtons, BorderLayout.NORTH);
        splitPane = new JSplitPane();
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        JPanel panelFrm = new JPanel(new BorderLayout());
        splitPane.setLeftComponent(panelFrm);
        JPanel panelAntBatch = new JPanel(new BorderLayout());
        lstBatch = new JList(new DefaultListModel());
        lstBatch.addMouseListener(mouseHandler);
        splitPane.setRightComponent(panelAntBatch);
        panelAntBatch.add(new JLabel("Batch"), BorderLayout.NORTH);
        panelAntBatch.add(lstBatch, BorderLayout.CENTER);
        btnAntBatchRun = new JButton("Run Batch");
        btnAntBatchRun.addActionListener(antBatchHandler);
        JButton btnAntBatchRemove = new JButton("Remove");
        btnAntBatchRemove.addActionListener(antBatchHandler);
        JButton btnAntBatchUp = new JButton("Up");
        btnAntBatchUp.addActionListener(antBatchHandler);
        JButton btnAntBatchDown = new JButton("Down");
        btnAntBatchDown.addActionListener(antBatchHandler);
        JPanel panelBatchButtons = new JPanel(new FlowLayout());
        panelBatchButtons.add(btnAntBatchRemove);
        panelBatchButtons.add(btnAntBatchUp);
        panelBatchButtons.add(btnAntBatchDown);
        panelBatchButtons.add(btnAntBatchRun);
        panelAntBatch.add(panelBatchButtons, BorderLayout.SOUTH);
        tabbedPane = new JTabbedPane();
        panelFrm.add(tabbedPane, BorderLayout.CENTER);
        configPanel = new JPanel(new BorderLayout());
        JPanel toolsChildPanel = new JPanel(new FlowLayout());
        configPanel.add(toolsChildPanel, BorderLayout.CENTER);
        chkPoll = new JCheckBox("Timer");
        chkPoll.setSelected(true);
        chkPoll.addActionListener(toolsHandler);
        toolsChildPanel.add(chkPoll);
        txtTimerField = new JTextField();
        txtTimerField.setEditable(false);
        toolsChildPanel.add(txtTimerField);
        toolsChildPanel.add(new JLabel("Logger: "));
        loggerTextField = new JTextField();
        loggerTextField.setEditable(false);
        toolsChildPanel.add(loggerTextField);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
