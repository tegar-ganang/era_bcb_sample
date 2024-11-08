package net.sf.wwusmart.pixeltocontour;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

/**
 * Gui with options to select a source and a destination folder using Javas
 * JFileChooser. Also has options to show a preview window and it renders the
 * progress od a running conversion task.
 * 
 * @author Armin
 * @version $Rev: 30 $
 */
public class Gui extends javax.swing.JFrame {

    /** Source directory. */
    private File src;

    /** Destination directory. */
    private File dest;

    /** Current {@code value} value for the JProgressBar. */
    private int progressValue;

    /** Ref to the class doing the conversion work. */
    private final PixelToContour converter = new PixelToContour();

    /** "Abort" String. */
    private static final String ABORT = "Abort";

    /** "Run" String.*/
    private static final String RUN = "Run";

    /** "Done." String. */
    private static final String DONE = "Done.";

    /** PriviewDialog referece. */
    public PreviewDialog preview;

    /** Creates new form Gui */
    public Gui() {
        initComponents();
        setSize(getSize().width * 2, getSize().height);
        setLocationRelativeTo(null);
        this.preview = new PreviewDialog(this);
    }

    /**
     * Sets the currently dispayed progress String to the given text. And
     * increases the value of the progress by 1.
     *
     * @param text A String.
     */
    public void nextProgress(String text) {
        this.progress.setValue(progressValue++);
        this.progress.setString(text);
        progress.repaint();
    }

    /**
     * Sets up the progress bar with the given minimum and maximum value and
     * the given text. Value is not changed.
     * @param min A positive integer (probably 0).
     * @param max A positive integer &gt; min.
     * @param text A String.
     */
    public void setProgress(int min, int max, String text) {
        this.progress.setMinimum(min);
        this.progress.setMaximum(max);
        this.progress.setString(text);
        progress.repaint();
    }

    /**
     * Sets the progress String to DONE and the run/abort button String to RUN.
     */
    public void convertDone() {
        this.progress.setString(DONE);
        this.runBtn.setText(RUN);
        progress.repaint();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        GridBagConstraints gridBagConstraints;
        fc = new JFileChooser();
        srcPnl = new JPanel();
        srcLbl = new JLabel();
        srcBtn = new JButton();
        destPnl = new JPanel();
        destLbl = new JLabel();
        destBtn = new JButton();
        progressPnl = new JPanel();
        progress = new JProgressBar();
        controls = new JPanel();
        settingsPnl = new JPanel();
        invertCB = new JCheckBox();
        buttonsPnl = new JPanel();
        runBtn = new JButton();
        previewBtn = new JButton();
        exitBtn = new JButton();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Pixel image to c Contour tracing");
        setResizable(false);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        srcPnl.setBorder(BorderFactory.createTitledBorder("Sourcefolder (.gif,.jp(e)g,.bmp,.png)"));
        srcPnl.setLayout(new GridBagLayout());
        srcLbl.setText("select a folder");
        srcLbl.setBorder(BorderFactory.createEtchedBorder());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        srcPnl.add(srcLbl, gridBagConstraints);
        srcBtn.setText("...");
        srcBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                srcBtnAction(evt);
            }
        });
        srcPnl.add(srcBtn, new GridBagConstraints());
        getContentPane().add(srcPnl);
        destPnl.setBorder(BorderFactory.createTitledBorder("Destinationfolder (.c)"));
        destPnl.setLayout(new GridBagLayout());
        destLbl.setText("select a folder");
        destLbl.setBorder(BorderFactory.createEtchedBorder());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        destPnl.add(destLbl, gridBagConstraints);
        destBtn.setText("...");
        destBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                destBtnAction(evt);
            }
        });
        destPnl.add(destBtn, new GridBagConstraints());
        getContentPane().add(destPnl);
        progressPnl.setLayout(new BoxLayout(progressPnl, BoxLayout.LINE_AXIS));
        progress.setString("");
        progress.setStringPainted(true);
        progressPnl.add(progress);
        getContentPane().add(progressPnl);
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        settingsPnl.setLayout(new FlowLayout(FlowLayout.LEFT));
        invertCB.setSelected(true);
        invertCB.setText("invert?");
        settingsPnl.add(invertCB);
        controls.add(settingsPnl);
        buttonsPnl.setLayout(new FlowLayout(FlowLayout.RIGHT));
        runBtn.setText("Run");
        runBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                runBtnAction(evt);
            }
        });
        buttonsPnl.add(runBtn);
        previewBtn.setText("Preview");
        previewBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                previewBtnAction(evt);
            }
        });
        buttonsPnl.add(previewBtn);
        exitBtn.setText("Exit");
        exitBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                exitBtnAction(evt);
            }
        });
        buttonsPnl.add(exitBtn);
        controls.add(buttonsPnl);
        getContentPane().add(controls);
        pack();
    }

    /**
     * Shows an file opend dialog for opening folders. And sets the selected
     * folder to this classes src field if the dialog was closed with an
     * approve option.
     * @param evt ignored.
     */
    private void srcBtnAction(ActionEvent evt) {
        int ret = fc.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) return;
        src = fc.getSelectedFile();
        this.srcLbl.setText(src.getPath());
    }

    /**
     * Shows an file save dialog for opening folders. And sets the selected
     * folder to this classes dest field if the dialog was closed with an
     * approve option.
     * @param evt ignored.
     */
    private void destBtnAction(ActionEvent evt) {
        int ret = fc.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) return;
        dest = fc.getSelectedFile();
        this.destLbl.setText(dest.getPath());
    }

    /**
     *  Exits the program with value 0.
     * @param evt ignored.
     */
    private void exitBtnAction(ActionEvent evt) {
        System.exit(0);
    }

    /**
     * Checks if the input data is valid by calling {@link #foldersValid()}. If
     * this returns true resets the progress bar and starts the convertion in
     * a seperate thread.
     * @param evt ignored.
     */
    private void runBtnAction(ActionEvent evt) {
        if (runBtn.getText().equals(ABORT)) {
            converter.abort();
            runBtn.setText(RUN);
            return;
        }
        if (!foldersValid()) return;
        this.progressValue = 0;
        this.progress.setValue(this.progressValue);
        this.runBtn.setText(ABORT);
        final Gui guiref = this;
        Thread worker = new Thread(new Runnable() {

            public void run() {
                converter.convert(src, dest, invertCB.isSelected(), guiref);
                guiref.convertDone();
            }
        });
        worker.start();
    }

    /**
     * Sets the preview dialog to visible.
     * @param evt ignored.
     */
    private void previewBtnAction(ActionEvent evt) {
        this.preview.setVisible(true);
    }

    private JPanel buttonsPnl;

    private JPanel controls;

    private JButton destBtn;

    private JLabel destLbl;

    private JPanel destPnl;

    private JButton exitBtn;

    private JFileChooser fc;

    private JCheckBox invertCB;

    private JButton previewBtn;

    private JProgressBar progress;

    private JPanel progressPnl;

    private JButton runBtn;

    private JPanel settingsPnl;

    private JButton srcBtn;

    private JLabel srcLbl;

    private JPanel srcPnl;

    /**
     * Checks the set src and dest folder for existance and if the needed
     * permissions are available to perform the conversion.
     * Shows a Warning Message dialog to the user if the test is not passed.
     * @return ture - if everything if fine.<br/>false - else.
     */
    private boolean foldersValid() {
        if (src == null || dest == null || !src.isDirectory() || !dest.isDirectory() || !src.canRead() || !dest.canWrite()) {
            JOptionPane.showMessageDialog(this, "Source or destination folder not set\n" + "or not a folder\n" + "or source not readable\n" + "or dest not writeable!", "Bad folder", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
}
