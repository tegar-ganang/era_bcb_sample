package at.ac.tuwien.law.yaplaf.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import at.ac.tuwien.law.yaplaf.Yaplaf;

/**
 * Klasse fuer das GUI - Fenster
 * 
 * Hier wird das GUI - Fenster erzeugt, die Klassen zum Einlesen des Inputs und
 * Konfigurieren vom yaplaf-config.xml, sowie die Methoden zum Starten und
 * Abbrechen des zentralen Prozesses ueber Yaplaf.java definiert
 * 
 * @author TU Wien, Daten- & Informatikrecht, YAPLAF++, Iliyana Zagralova, Mario
 *         Budischek
 * 
 */
public class YFrame extends JFrame implements ActionListener, PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    private static ResourceBundle messages = ResourceBundle.getBundle("at.ac.tuwien.law.yaplaf.gui.messages");

    protected static Logger logger = Logger.getLogger(YFrame.class);

    private JButton btn_start = new JButton(messages.getString("buttonStart"));

    private JButton btn_cancel = new JButton(messages.getString("buttonStop"));

    private JProgressBar progressbar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);

    final JFileChooser filechooser = new JFileChooser();

    private JLabel lbl_choose = new JLabel(messages.getString("browseFile"));

    private JTextField txt_chosen = new JTextField();

    private JButton btn_browse = new JButton("Browse");

    private JButton btn_report = new JButton("Report");

    private JLabel lbl_status = new JLabel();

    private YInputReader ystart;

    private boolean running = false;

    String filt;

    MainPanel intern;

    /**
	 * Konstruktor fuer diese Klasse
	 * 
	 * Erzeugt und fuegt alle Komponenten ein: progressbar - der
	 * Fortschrittsbalken, filechooser - der Dateiauswahldialog, start, cancel -
	 * Start bzw. Abbrechen - Buttons
	 */
    YFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container pane = this.getContentPane();
        pane.setPreferredSize(new Dimension(1024, 768));
        SpringLayout layout = new SpringLayout();
        pane.setLayout(layout);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width / 2 - pane.getPreferredSize().width / 2), d.height / 2 - pane.getPreferredSize().height / 2);
        this.setTitle("YAPLAF++");
        btn_start.setActionCommand("start");
        btn_start.addActionListener(this);
        btn_cancel.setActionCommand("cancel");
        btn_cancel.addActionListener(this);
        btn_browse.setActionCommand("browse");
        btn_browse.addActionListener(this);
        btn_report.setActionCommand("report");
        btn_report.addActionListener(this);
        txt_chosen.setEditable(false);
        txt_chosen.setBackground(Color.white);
        txt_chosen.setBorder(BorderFactory.createLoweredBevelBorder());
        ExtensionFilter ext = new ExtensionFilter();
        filechooser.setFileFilter(ext);
        filechooser.setAcceptAllFileFilterUsed(false);
        filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        pane.add(btn_start);
        pane.add(btn_cancel);
        pane.add(progressbar);
        pane.add(lbl_choose);
        pane.add(txt_chosen);
        pane.add(btn_browse);
        pane.add(lbl_status);
        pane.add(btn_report);
        btn_browse.setPreferredSize(new Dimension(100, 30));
        btn_start.setEnabled(false);
        layout.putConstraint(SpringLayout.WEST, lbl_choose, 20, SpringLayout.WEST, pane);
        layout.putConstraint(SpringLayout.WEST, txt_chosen, 0, SpringLayout.WEST, lbl_choose);
        layout.putConstraint(SpringLayout.WEST, btn_browse, 20, SpringLayout.EAST, txt_chosen);
        layout.putConstraint(SpringLayout.WEST, progressbar, 0, SpringLayout.WEST, lbl_choose);
        layout.putConstraint(SpringLayout.WEST, lbl_status, 0, SpringLayout.WEST, lbl_choose);
        layout.putConstraint(SpringLayout.WEST, btn_start, 20, SpringLayout.WEST, pane);
        layout.putConstraint(SpringLayout.WEST, btn_cancel, 10, SpringLayout.EAST, btn_start);
        layout.putConstraint(SpringLayout.WEST, btn_report, 0, SpringLayout.WEST, btn_browse);
        layout.putConstraint(SpringLayout.NORTH, lbl_choose, 10, SpringLayout.NORTH, pane);
        layout.putConstraint(SpringLayout.NORTH, txt_chosen, 5, SpringLayout.SOUTH, lbl_choose);
        layout.putConstraint(SpringLayout.NORTH, btn_browse, 0, SpringLayout.NORTH, txt_chosen);
        layout.putConstraint(SpringLayout.NORTH, progressbar, 20, SpringLayout.SOUTH, txt_chosen);
        layout.putConstraint(SpringLayout.NORTH, lbl_status, 0, SpringLayout.SOUTH, progressbar);
        layout.putConstraint(SpringLayout.NORTH, btn_start, 20, SpringLayout.SOUTH, lbl_status);
        layout.putConstraint(SpringLayout.NORTH, btn_cancel, 0, SpringLayout.NORTH, btn_start);
        layout.putConstraint(SpringLayout.NORTH, btn_report, 0, SpringLayout.NORTH, btn_start);
        layout.putConstraint(SpringLayout.EAST, pane, 20, SpringLayout.EAST, progressbar);
        layout.putConstraint(SpringLayout.EAST, progressbar, 0, SpringLayout.EAST, btn_browse);
        layout.putConstraint(SpringLayout.EAST, lbl_choose, 0, SpringLayout.EAST, progressbar);
        layout.putConstraint(SpringLayout.EAST, lbl_status, 0, SpringLayout.EAST, progressbar);
        layout.putConstraint(SpringLayout.HEIGHT, btn_start, 0, SpringLayout.HEIGHT, btn_browse);
        layout.putConstraint(SpringLayout.HEIGHT, btn_cancel, 0, SpringLayout.HEIGHT, btn_browse);
        layout.putConstraint(SpringLayout.HEIGHT, txt_chosen, 0, SpringLayout.HEIGHT, btn_browse);
        layout.putConstraint(SpringLayout.HEIGHT, btn_cancel, 0, SpringLayout.HEIGHT, btn_browse);
        layout.putConstraint(SpringLayout.HEIGHT, btn_report, 0, SpringLayout.HEIGHT, btn_browse);
        layout.putConstraint(SpringLayout.HEIGHT, progressbar, 0, SpringLayout.HEIGHT, btn_browse);
        layout.putConstraint(SpringLayout.WIDTH, btn_start, 0, SpringLayout.WIDTH, btn_browse);
        layout.putConstraint(SpringLayout.WIDTH, btn_cancel, 0, SpringLayout.WIDTH, btn_browse);
        layout.putConstraint(SpringLayout.WIDTH, btn_report, 0, SpringLayout.WIDTH, btn_browse);
        intern = new MainPanel();
        intern.setVisible(false);
        pane.add(intern);
        layout.putConstraint(SpringLayout.NORTH, intern, 40, SpringLayout.SOUTH, btn_start);
        layout.putConstraint(SpringLayout.WEST, intern, 20, SpringLayout.WEST, pane);
        layout.putConstraint(SpringLayout.SOUTH, intern, -20, SpringLayout.SOUTH, pane);
        layout.putConstraint(SpringLayout.EAST, intern, 0, SpringLayout.EAST, btn_browse);
    }

    /**
	 * @param e
	 *            wenn e = "start" initialisiert den Fortschrittsbalken, liest
	 *            das Input, und startet den zentralen Prozess ueber
	 *            Yaplaf.java, wenn e = "cancel" bricht den Prozess ab, falls
	 *            bereits gestartet wenn e = "browse" wird eine filesuche
	 *            gestartet
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("start")) {
            try {
                File file = filechooser.getSelectedFile();
                btn_start.setEnabled(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                progressbar.setVisible(true);
                progressbar.setString(messages.getString("startApplication"));
                progressbar.setStringPainted(true);
                ystart = new YInputReader();
                ystart.read(file);
                ystart.addPropertyChangeListener(this);
                ystart.execute();
                logger.info("Yaplaf++ was started....");
                running = true;
                btn_report.setEnabled(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, messages.getString("browseFile"), "", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getActionCommand().equals("report")) {
            try {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler bewertung.pdf");
            } catch (Exception e1) {
                logger.error("file not found");
            }
        } else if (e.getActionCommand().equals("cancel")) {
            if (!running) {
                return;
            }
            String[] optionen = { messages.getString("optionYes"), messages.getString("optionNo") };
            int select = JOptionPane.showOptionDialog(null, messages.getString("abortQuestion"), messages.getString("abortTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionen, optionen[0]);
            if (select == JOptionPane.YES_OPTION) {
                ystart.cancel();
                progressbar.setString(messages.getString("abortApplication"));
                progressbar.setVisible(false);
                progressbar.setValue(0);
                btn_report.setEnabled(false);
                logger.info("Yaplaf++ was cancelled....");
            }
        } else if (e.getActionCommand().equals("browse")) {
            int returnVal = filechooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                txt_chosen.setText(filechooser.getSelectedFile().toString());
                btn_start.setEnabled(true);
            } else if (returnVal == JFileChooser.CANCEL_OPTION) {
                txt_chosen.setText("");
            } else {
                logger.error("couldnt open file");
            }
        } else {
            logger.error("Unknown action event");
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            if (!ystart.isDone() && !ystart.isCancelled()) {
                progressbar.setValue((Integer) evt.getNewValue());
                progressbar.setString(messages.getString("processInput"));
                logger.info("set progress: " + (Integer) evt.getNewValue());
            }
        }
    }

    class YInputReader extends SwingWorker<Void, Void> {

        private Yaplaf yap;

        @Override
        protected Void doInBackground() throws Exception {
            this.yap = new Yaplaf("yaplaf-config.xml");
            this.yap.start();
            this.yap.setMainPanel(intern);
            int progress = 0;
            setProgress(0);
            while (progress < 100) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    setProgress(0);
                    return null;
                }
                progress = (int) (this.yap.getProgress() * 100);
                setProgress(progress);
            }
            return null;
        }

        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            progressbar.setValue(100);
            btn_start.setEnabled(true);
            setCursor(null);
            progressbar.setString("process Done");
            if (isCancelled()) {
                return;
            }
            running = false;
        }

        /**
		 * Bricht den Prozess und den Fotrschrittsbalken ab
		 */
        public void cancel() {
            this.yap.stop();
            this.cancel(true);
            running = false;
        }

        /**
		 * Liest das Input - Archiv ein, leist das config.xml und setzt
		 * yaplaf-config.xml, falls submissions.xml, extrachiert das Archiv
		 * 
		 * @param file
		 *            , der Inputpfad
		 */
        public void read(File file) {
            try {
                String filePath = file.getPath();
                String configPath = UnzipInput.check(filePath, "config.xml");
                if (configPath != null) {
                    logger.info("config.xml found:" + configPath);
                    ZipFile zipFile = new ZipFile(filePath);
                    ZipEntry zipEntry = zipFile.getEntry(configPath);
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();
                    DefaultHandler handler = new ConfigHandler();
                    InputStream is = zipFile.getInputStream(zipEntry);
                    saxParser.parse(new InputSource(is), handler);
                    is.close();
                    logger.info(zipEntry.getName() + " parsed");
                    if (((ConfigHandler) handler).getAssignmentType().equals("online")) {
                        String submPath = UnzipInput.check(filePath, "submissions.xml");
                        if (submPath != null) {
                            String destDir = filePath.substring(0, filePath.lastIndexOf("."));
                            int idx;
                            if ((idx = submPath.lastIndexOf("/")) != -1) {
                                submPath = submPath.substring(idx + 1);
                            }
                            ((ConfigHandler) handler).setInputPathProps(destDir + File.separator + submPath);
                            logger.info("Path set to " + destDir + File.separator + submPath);
                            UnzipInput.unzip(filePath, destDir);
                        } else {
                            logger.error(file.getName() + "does not contain submissions.xml");
                        }
                    } else {
                        ((ConfigHandler) handler).setInputPathProps(filePath);
                        logger.info("Path set to " + filePath);
                    }
                    ((ConfigHandler) handler).setYaplafConfig("yaplaf-config.xml");
                    logger.info("yaplaf-config.xml modified");
                } else {
                    logger.error("config.xml not found. yaplaf-config.xml cannot be modified.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * Klasse fuer das Extrahieren von Archiv-Dateien
 * 
 */
class UnzipInput {

    protected static Logger logger = Logger.getLogger(UnzipInput.class);

    private static final byte[] buffer = new byte[0xFFFF];

    /**
	 * @param inputFile
	 *            , die Archivdatei
	 * @param fileName
	 *            , der Eintrag nachdem gesucht werden soll
	 * @return Pfad zum Eintrag, falls enthalten, sonst null
	 */
    public static String check(String inputFile, String fileName) {
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(inputFile);
            Enumeration<? extends ZipEntry> zipEntryEnum = zipFile.entries();
            while (zipEntryEnum.hasMoreElements()) {
                ZipEntry zipEntry = zipEntryEnum.nextElement();
                String entryName = zipEntry.getName();
                if (entryName.equals(fileName) || entryName.endsWith("/" + fileName)) {
                    return entryName;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * @param zipFile
	 *            , die Datei zum Extrahieren
	 * @param destDir
	 *            , das Verzeichnis, wo die Datei extrahiert wird
	 */
    public static void unzip(String zipFile, String destDir) {
        try {
            ZipFile zip = new ZipFile(zipFile);
            Enumeration<? extends ZipEntry> zipEntryEnum = zip.entries();
            while (zipEntryEnum.hasMoreElements()) {
                ZipEntry zipEntry = zipEntryEnum.nextElement();
                String name = zipEntry.getName();
                if (!name.equals("config.xml") && !name.endsWith("/config.xml")) {
                    logger.info(name);
                    extractEntry(zip, zipEntry, destDir);
                    logger.info("unpacked");
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("ZipFile not found!");
        } catch (IOException e) {
            logger.error("Unzip error");
        }
    }

    /**
	 * @param zipFile
	 *            , die Datei zum Extrahieren
	 * @param zipEntry
	 *            , der Eintrag im Archiv
	 * @param destDir
	 *            , das Verzeichnis, wo die Datei extrahiert wird
	 * @throws IOException
	 */
    private static void extractEntry(ZipFile zipFile, ZipEntry zipEntry, String destDir) throws IOException {
        String fileName = zipEntry.getName();
        int idx;
        if ((idx = fileName.lastIndexOf("/")) != -1) {
            fileName = fileName.substring(idx + 1);
        }
        File file = new File(destDir, fileName);
        logger.info(file.getPath());
        new File(file.getParent()).mkdirs();
        InputStream is = null;
        OutputStream os = null;
        try {
            is = zipFile.getInputStream(zipEntry);
            os = new FileOutputStream(file);
            for (int len; (len = is.read(buffer)) != -1; ) os.write(buffer, 0, len);
        } finally {
            os.close();
            is.close();
        }
    }
}
