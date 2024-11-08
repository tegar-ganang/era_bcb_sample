package de.mse.mogwai.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author msertic
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MogwaiChart extends JFrame {

    private JTextArea m_sourceText;

    private JPanel m_resultPanel;

    private String m_dotexe;

    private Logger m_logger;

    public MogwaiChart() throws IOException {
        super("Mogwai Chart");
        this.setSize(800, 600);
        this.m_logger = Logger.getLogger(MogwaiChart.class.getName());
        this.initialize();
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        String def = null;
        if (File.separatorChar == '/') {
            def = "/usr/bin/dot";
        } else {
            def = "c:\\Programme\\ATT\\Graphviz\\bin\\dot.exe";
        }
        String baseDir = Preferences.userRoot().get("/MogwaiChart/DotExe", def);
        JFileChooser chooser = new JFileChooser();
        if ((baseDir != null) && (baseDir.length() > 0)) {
            chooser.setSelectedFile(new File(baseDir));
        }
        if (chooser.showDialog(this, "Select DOT binary") == JFileChooser.APPROVE_OPTION) {
            this.m_dotexe = chooser.getSelectedFile().toString();
            Preferences.userRoot().get("/MogwaiChart/DotExe", this.m_dotexe);
        }
        this.m_logger.info("Using dot.exe -> " + this.m_dotexe);
    }

    protected void initialize() throws IOException {
        JMenuBar menu = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add("Load chart file...").addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadFromFile();
            }
        });
        file.add("Save chart file...").addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });
        file.addSeparator();
        file.add("Quit").addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        menu.add(file);
        this.setJMenuBar(menu);
        final JTabbedPane tab = new JTabbedPane();
        JPanel sourceTab = new JPanel(new BorderLayout());
        this.m_sourceText = new JTextArea();
        sourceTab.add(new JScrollPane(this.m_sourceText));
        tab.add("Source", sourceTab);
        this.m_resultPanel = new JPanel(new BorderLayout());
        this.m_resultPanel.setBackground(Color.white);
        this.m_resultPanel.setBorder(null);
        tab.add("Result", new JScrollPane(this.m_resultPanel));
        tab.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent arg0) {
                if (tab.getSelectedIndex() == 1) {
                    updateFlowTab();
                }
            }
        });
        this.getContentPane().add(tab);
        StringBuffer graph = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(MogwaiChart.class.getClassLoader().getResourceAsStream("defaultgraph.txt")));
        while (br.ready()) {
            String line = br.readLine();
            graph.append(line);
            graph.append("\n");
        }
        br.close();
        this.m_sourceText.setText(graph.toString());
    }

    protected void loadFromFile() {
        try {
            String baseDir = Preferences.userRoot().get("/MogwaiChart/LastFile", "");
            JFileChooser chooser = new JFileChooser();
            if ((baseDir != null) && (baseDir.length() > 0)) {
                chooser.setSelectedFile(new File(baseDir));
            }
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File sourceFile = chooser.getSelectedFile();
                Preferences.userRoot().put("/MogwaiChart/LastFile", sourceFile.toString());
                StringBuffer graph = new StringBuffer();
                BufferedReader br = new BufferedReader(new FileReader(sourceFile));
                while (br.ready()) {
                    String line = br.readLine();
                    graph.append(line);
                    graph.append("\n");
                }
                br.close();
                this.m_sourceText.setText(graph.toString());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void saveToFile() {
        try {
            String baseDir = Preferences.userRoot().get("/MogwaiChart/LastFile", "");
            JFileChooser chooser = new JFileChooser();
            if ((baseDir != null) && (baseDir.length() > 0)) {
                chooser.setSelectedFile(new File(baseDir));
            }
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File sourceFile = chooser.getSelectedFile();
                Preferences.userRoot().put("/MogwaiChart/LastFile", sourceFile.toString());
                PrintWriter pw = new PrintWriter(new FileWriter(sourceFile));
                pw.print(this.m_sourceText.getText());
                pw.close();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void updateFlowTab() {
        try {
            File tempfile = File.createTempFile("MogwaiChart", ".dot");
            PrintWriter pw = new PrintWriter(new FileWriter(tempfile));
            pw.println(this.m_sourceText.getText());
            pw.flush();
            pw.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(MogwaiChart.class.getClassLoader().getResourceAsStream("imagelist.txt")));
            while (br.ready()) {
                String line = br.readLine();
                this.m_logger.info("Copy resource to temp directory -> " + line);
                byte buffer[] = new byte[8192];
                BufferedInputStream bis = new BufferedInputStream(MogwaiChart.class.getClassLoader().getResourceAsStream(line));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(tempfile.getParent(), line)));
                while (bis.available() > 0) {
                    int read = bis.read(buffer);
                    bos.write(buffer, 0, read);
                }
                bis.close();
                bos.close();
            }
            br.close();
            File tempfile2 = File.createTempFile("MogwaiChart", ".gif");
            String args[] = new String[4];
            args[0] = this.m_dotexe;
            args[1] = "-o" + tempfile2;
            args[2] = "-Tgif";
            args[3] = tempfile.toString();
            this.m_logger.info("Invoking dot");
            final Process p = Runtime.getRuntime().exec(args, null, new File(tempfile.getParent()));
            Thread runner = new Thread() {

                public void run() {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            m_logger.info("Out->" + line);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            runner.start();
            Thread runner2 = new Thread() {

                public void run() {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            m_logger.info("Error->" + line);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            runner2.start();
            p.waitFor();
            while (runner.isAlive()) ;
            while (runner2.isAlive()) ;
            this.m_logger.info("Loading graph...");
            ImageIcon icon = new ImageIcon(tempfile2.toString());
            JLabel displayLabel = new JLabel(icon);
            displayLabel.setTransferHandler(new ImageSelection());
            this.m_logger.info("Copying graph to clipboard...");
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            displayLabel.getTransferHandler().exportToClipboard(displayLabel, board, TransferHandler.COPY);
            this.m_resultPanel.removeAll();
            this.m_resultPanel.add(displayLabel);
            tempfile.delete();
            tempfile2.delete();
            this.m_logger.info("Done...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        MogwaiChart chart = new MogwaiChart();
        chart.setVisible(true);
    }
}
