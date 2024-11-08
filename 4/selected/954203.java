package magictool.dissim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import magictool.ExpFile;
import magictool.MainFrame;
import magictool.Project;
import magictool.VerticalLayout;

/**
 * DissimilarityFrame is a class that enables the user to select the statistical
 * method for calculating the dissimilarity of the genes
 */
public class DissimilarityFrame extends JInternalFrame implements KeyListener {

    private VerticalLayout verticalLayout1 = new VerticalLayout();

    private JPanel titlePanel = new JPanel();

    private JPanel confirmPanel = new JPanel();

    private JButton discancelButton = new JButton("Cancel");

    private JButton disokButton = new JButton("OK");

    private JLabel dissimStatusLabel = new JLabel();

    private ButtonGroup dissimButtons = new ButtonGroup();

    private ExpFile expMain;

    private TitledBorder titledBorder1;

    private JTextField pField = new JTextField();

    private JLabel fName = new JLabel();

    private JRadioButton lpButton = new JRadioButton();

    private JPanel infoPanel = new JPanel();

    private JLabel nameLabel = new JLabel();

    private JLabel numLabel = new JLabel();

    private JLabel pLabel = new JLabel();

    private FlowLayout flowLayout3 = new FlowLayout();

    private JPanel namePanel = new JPanel();

    private FlowLayout flowLayout2 = new FlowLayout();

    private FlowLayout flowLayout1 = new FlowLayout();

    private JPanel numPanel = new JPanel();

    private VerticalLayout verticalLayout2 = new VerticalLayout();

    private JPanel lpPanel = new JPanel();

    private JLabel numIs = new JLabel();

    private VerticalLayout verticalLayout4 = new VerticalLayout();

    private FlowLayout flowLayout5 = new FlowLayout();

    private JPanel jackPanel = new JPanel();

    private FlowLayout flowLayout4 = new FlowLayout();

    private JRadioButton corrButton = new JRadioButton();

    private JPanel corrPanel = new JPanel();

    private JRadioButton jackButton = new JRadioButton();

    private JPanel outnamePanel = new JPanel();

    private FlowLayout flowLayout7 = new FlowLayout();

    private JTextField outnameField = new JTextField();

    private JLabel jLabel1 = new JLabel();

    private Border border1;

    /**Project associated with the dissimilarity file*/
    protected Project project;

    /**Parent Frame*/
    protected Frame parentFrame;

    /**
 * Sets up the frame for the user to choose a dissimilarity method and select appropriate
 * parameters
 * @param expMain exp file that creates the dissimilarity file
 * @param p project associated with the dissimilarity file
 * @param parentFrame parent frame
 */
    public DissimilarityFrame(ExpFile expMain, Project p, Frame parentFrame) {
        this.project = p;
        this.parentFrame = parentFrame;
        try {
            jbInit();
            this.expMain = expMain;
            setFileInfo(expMain);
            this.addKeyListenerRecursively(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 2), "Select Method To Create Dissimilarity");
        border1 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.white, Color.white, new Color(148, 145, 140), new Color(103, 101, 98)), BorderFactory.createEmptyBorder(3, 3, 3, 3));
        this.getContentPane().setLayout(verticalLayout1);
        this.setClosable(true);
        this.setResizable(true);
        this.setPreferredSize(new Dimension(560, 350));
        this.setOpaque(true);
        this.setBackground(new Color(204, 204, 204));
        discancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                discancelButton_actionPerformed(e);
            }
        });
        disokButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                disokButton_actionPerformed(e);
            }
        });
        this.getRootPane().setDefaultButton(disokButton);
        dissimStatusLabel.setBackground(Color.lightGray);
        dissimStatusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        dissimStatusLabel.setMaximumSize(new Dimension(45, 21));
        dissimStatusLabel.setMinimumSize(new Dimension(4, 4));
        dissimStatusLabel.setOpaque(true);
        dissimStatusLabel.setPreferredSize(new Dimension(45, 21));
        dissimStatusLabel.setToolTipText("");
        dissimStatusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        titlePanel.setBorder(titledBorder1);
        titlePanel.setLayout(verticalLayout4);
        pField.setEnabled(false);
        pField.setPreferredSize(new Dimension(35, 21));
        pField.setText("2");
        lpButton.setText("l^p");
        lpButton.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                lpButton_itemStateChanged(e);
            }
        });
        infoPanel.setLayout(verticalLayout2);
        infoPanel.setBorder(BorderFactory.createEtchedBorder());
        nameLabel.setText("Exp File:");
        numLabel.setFont(new java.awt.Font("Dialog", 1, 12));
        numLabel.setText("Number of genes:");
        pLabel.setText("p=");
        flowLayout3.setAlignment(FlowLayout.LEFT);
        namePanel.setLayout(flowLayout1);
        flowLayout2.setAlignment(FlowLayout.LEFT);
        flowLayout2.setVgap(0);
        flowLayout1.setAlignment(FlowLayout.LEFT);
        flowLayout1.setVgap(0);
        numPanel.setLayout(flowLayout2);
        verticalLayout2.setVgap(0);
        lpPanel.setLayout(flowLayout3);
        flowLayout5.setAlignment(FlowLayout.LEFT);
        jackPanel.setLayout(flowLayout5);
        flowLayout4.setAlignment(FlowLayout.LEFT);
        corrButton.setSelected(true);
        corrButton.setText("1 - correlation");
        corrButton.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                corrButton_itemStateChanged(e);
            }
        });
        corrPanel.setLayout(flowLayout4);
        jackButton.setText("1 - (jackknife correlation)");
        jackButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jackButton_actionPerformed(e);
            }
        });
        outnamePanel.setLayout(flowLayout7);
        flowLayout7.setAlignment(FlowLayout.LEFT);
        flowLayout7.setVgap(3);
        outnameField.setPreferredSize(new Dimension(370, 21));
        outnamePanel.setBorder(BorderFactory.createEtchedBorder());
        jLabel1.setBorder(border1);
        jLabel1.setText("Output File");
        this.getContentPane().add(titlePanel, null);
        namePanel.add(nameLabel, null);
        namePanel.add(fName, null);
        infoPanel.add(numPanel, null);
        infoPanel.add(namePanel, null);
        numPanel.add(numLabel, null);
        numPanel.add(numIs, null);
        lpPanel.add(lpButton, null);
        lpPanel.add(pLabel, null);
        lpPanel.add(pField, null);
        titlePanel.add(infoPanel, null);
        outnamePanel.add(jLabel1, null);
        outnamePanel.add(outnameField, null);
        jackPanel.add(jackButton, null);
        titlePanel.add(corrPanel, null);
        corrPanel.add(corrButton, null);
        titlePanel.add(lpPanel, null);
        titlePanel.add(jackPanel, null);
        titlePanel.add(outnamePanel, null);
        confirmPanel.add(disokButton, null);
        confirmPanel.add(discancelButton, null);
        this.getContentPane().add(confirmPanel, null);
        this.getContentPane().add(dissimStatusLabel, null);
        dissimButtons.add(jackButton);
        dissimButtons.add(corrButton);
        dissimButtons.add(lpButton);
        pField.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                try {
                    Document doc = e.getDocument();
                    String docText = doc.getText(0, doc.getLength());
                    outnameField.setText(expMain.getName() + "lp" + docText + ".dis");
                } catch (Exception e1) {
                }
            }

            public void insertUpdate(DocumentEvent e) {
                try {
                    Document doc = e.getDocument();
                    String docText = doc.getText(0, doc.getLength());
                    outnameField.setText(expMain.getName() + "lp" + docText + ".dis");
                } catch (Exception e1) {
                }
            }

            public void removeUpdate(DocumentEvent e) {
                try {
                    Document doc = e.getDocument();
                    String docText = doc.getText(0, doc.getLength());
                    outnameField.setText(expMain.getName() + "lp" + docText + ".dis");
                } catch (Exception e1) {
                }
            }
        });
    }

    private void discancelButton_actionPerformed(ActionEvent e) {
        this.dispose();
    }

    private void disokButton_actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {

            public void run() {
                Dissimilarity sm = null;
                String outfile = project.getPath() + expMain.getName() + File.separator + outnameField.getText().trim();
                if (!outfile.endsWith(".dis")) outfile += ".dis";
                if (corrButton.isSelected()) {
                    if (outfileIsValid(outfile)) {
                        dissimStatusLabel.setText("Creating 1-Correlation Matrix - Window will close when complete.");
                        sm = new Dissimilarity(expMain.getPath(), outfile, 0, null, getDesktopPane());
                    }
                } else if (lpButton.isSelected()) {
                    if (pIsValid(pField.getText().trim())) {
                        if (outfileIsValid(outfile)) {
                            dissimStatusLabel.setText("Creating lp Matrix - Window will close when complete.");
                            sm = new Dissimilarity(expMain.getPath(), outfile, 1, pField.getText().trim(), getDesktopPane());
                        }
                    } else {
                        dissimStatusLabel.setText("Invalid Value Entered For p");
                    }
                } else if (jackButton.isSelected()) {
                    if (outfileIsValid(outfile)) {
                        sm = new Dissimilarity(expMain.getPath(), outfile, 2, null, getDesktopPane());
                    }
                }
                if (sm != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    sm.setProject(project);
                    sm.start();
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            dispose();
                        }
                    });
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        thread.start();
    }

    private void setFileInfo(ExpFile expMain) {
        fName.setText(expMain.getName() + (expMain.getName().endsWith(".exp") ? "" : ".exp"));
        numIs.setText(Integer.toString(expMain.numGenes()));
        String name = expMain.getName();
        StringBuffer tempName = new StringBuffer(name);
        if (name.endsWith(".exp")) tempName.replace(tempName.length() - 4, tempName.length(), ".dis"); else tempName.append(".dis");
        outnameField.setText(tempName.toString());
    }

    private boolean outfileIsValid(String outfile) {
        boolean goodFile;
        outfile.trim();
        File outFile = new File(outfile);
        if (outFile.isDirectory()) {
            JOptionPane.showMessageDialog(parentFrame, "The output file path is a directory.  Please add a file name.", "Directory Found", JOptionPane.OK_OPTION);
            return false;
        } else if (outFile.exists()) {
            int result = JOptionPane.showConfirmDialog(parentFrame, "The file " + outFile.getPath() + " already exists.  Overwrite this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                outFile.delete();
                return true;
            } else return false;
        }
        return true;
    }

    private boolean pIsValid(String p) {
        if (p.equalsIgnoreCase("i")) {
            return true;
        } else {
            try {
                Integer.parseInt(p);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private void outButton_actionPerformed(ActionEvent e) {
        MainFrame.fileLoader.setApproveButtonText("Select");
        MainFrame.fileLoader.setFileFilter(MainFrame.fileLoader.disFilter);
        MainFrame.fileLoader.setDialogTitle("Select Output Path...");
        MainFrame.fileLoader.setSelectedFile(null);
        int result = MainFrame.fileLoader.showOpenDialog(null);
        File fileobj = MainFrame.fileLoader.getSelectedFile();
        if (result == JFileChooser.APPROVE_OPTION) {
            String outfilestring = fileobj.getPath();
            if (outfilestring.lastIndexOf(".") == -1) outfilestring = outfilestring.concat(".dis");
            outnameField.setText(outfilestring);
        }
    }

    private void corrButton_itemStateChanged(ItemEvent e) {
        if (corrButton.isSelected()) outnameField.setText(expMain.getName() + "c.dis");
    }

    private void lpButton_itemStateChanged(ItemEvent e) {
        if (lpButton.isSelected()) {
            pField.setEnabled(true);
            outnameField.setText(expMain.getName() + "lp" + pField.getText() + ".dis");
        } else pField.setEnabled(false);
    }

    private void jackButton_actionPerformed(ActionEvent e) {
        if (jackButton.isSelected()) outnameField.setText(expMain.getName() + "j.dis");
    }

    private void pField_actionPerformed(ActionEvent e) {
        outnameField.setText(expMain.getName() + "lp" + pField.getText() + ".dis");
    }

    private void addKeyListenerRecursively(Component c) {
        c.removeKeyListener(this);
        c.addKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            Component[] children = cont.getComponents();
            for (int i = 0; i < children.length; i++) {
                addKeyListenerRecursively(children[i]);
            }
        }
    }

    /**
     * Closes the frame when user press control + 'w'
     * @param e key event
     */
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK).getKeyCode() && e.isControlDown()) {
            this.dispose();
        }
    }

    /**
     * Not implemented in this frame
     * @param e key event
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Not implemented in this frame
     * @param e key event
     */
    public void keyTyped(KeyEvent e) {
    }
}
