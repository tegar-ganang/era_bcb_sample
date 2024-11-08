package com.ctext.pdfextractor;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;

/**
 * The GUI for the Autshumato PTE (PDF Text Extractor)
 * @author W. Fourie
 */
public class GUIForm extends javax.swing.JFrame {

    private File readFile;

    private File writeFile;

    private static String ERROR_EXTRACT = "ERROR Extracting Text: ";

    private static String NO_FILE_SELECTED = "No File Selected";

    private static String SELECT_FILE = "Select a file to extract...";

    private static String MUST_SELECT_FILE = "You must select a file to be extracted !";

    private static String PRESS_EXTRACT = "Press the Exctract button to start the extraction...";

    private static String EXTRACTING = "Extracting...";

    private static String EXTARCT_DONE = "Extraction Completed !";

    private static Color SUCCEED = Color.GREEN;

    private static Color INFO = new Color(212, 208, 200);

    private static Color WARNING = Color.RED;

    /** Creates new form GUIForm */
    public GUIForm() {
        initComponents();
        inputField.setText(NO_FILE_SELECTED);
        outputField.setText(NO_FILE_SELECTED);
        progressLabel.setBackground(INFO);
        progressLabel.setText(SELECT_FILE);
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        inputField = new javax.swing.JTextField();
        inputButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        outputField = new javax.swing.JTextField();
        outputButton = new javax.swing.JButton();
        extractButton = new javax.swing.JButton();
        quitButton = new javax.swing.JButton();
        progressLabel = new javax.swing.JTextField();
        extractImagesChckbx = new javax.swing.JCheckBox();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Autshumato PDF Text Extractor");
        setIconImages(null);
        setResizable(false);
        jLabel1.setText("Input File:");
        inputField.setText("inputField");
        inputField.setToolTipText("The PDF from which the text should be extracted.");
        inputButton.setText("...");
        inputButton.setToolTipText("Browse for a PDF file.");
        inputButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputButtonActionPerformed(evt);
            }
        });
        jLabel2.setText("Output File:");
        outputField.setText("outputField");
        outputField.setToolTipText("The file in which the extracted text is saved.");
        outputButton.setText("...");
        outputButton.setToolTipText("Specify the text output file.");
        outputButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputButtonActionPerformed(evt);
            }
        });
        extractButton.setText("Extract");
        extractButton.setToolTipText("Starts the text extraction.");
        extractButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extractButtonActionPerformed(evt);
            }
        });
        quitButton.setText("Quit");
        quitButton.setToolTipText("");
        quitButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitButtonActionPerformed(evt);
            }
        });
        progressLabel.setEditable(false);
        progressLabel.setText("progressLabel");
        extractImagesChckbx.setText("Extract Images");
        extractImagesChckbx.setToolTipText("If this option is enabled every page in the PDF document will be extracted as a PNG picture file.");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(progressLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(extractImagesChckbx).addContainerGap()).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel1).addComponent(jLabel2).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(outputField).addComponent(inputField, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(inputButton).addComponent(outputButton)))).addGap(33, 33, 33)).addGroup(layout.createSequentialGroup().addComponent(extractButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 263, Short.MAX_VALUE).addComponent(quitButton).addContainerGap())))));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(inputField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(inputButton)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(outputField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(outputButton)).addGap(18, 18, 18).addComponent(extractImagesChckbx).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(extractButton).addComponent(quitButton)).addGap(18, 18, 18).addComponent(progressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        pack();
    }

    private void inputButtonActionPerformed(java.awt.event.ActionEvent evt) {
        selectPDFFile();
    }

    private void quitButtonActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void outputButtonActionPerformed(java.awt.event.ActionEvent evt) {
        selectTXTFile();
    }

    private void extractButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if ((!inputField.getText().equals(NO_FILE_SELECTED)) && (!outputField.getText().equals(NO_FILE_SELECTED))) {
            progressLabel.setBackground(INFO);
            progressLabel.setText(EXTRACTING);
            progressLabel.validate();
            try {
                writeFile = new File(outputField.getText());
                Extractor extractor = new Extractor(readFile, writeFile);
                if (extractImagesChckbx.isSelected()) extractor.extractImages(readFile, writeFile);
                progressLabel.setBackground(SUCCEED);
                progressLabel.setText(EXTARCT_DONE);
            } catch (IOException iox) {
                System.out.println(ERROR_EXTRACT);
                iox.printStackTrace();
                progressLabel.setBackground(WARNING);
                progressLabel.setText(ERROR_EXTRACT + iox.getMessage());
            }
        } else {
            progressLabel.setBackground(WARNING);
            progressLabel.setText(MUST_SELECT_FILE);
        }
    }

    /**
     * Open a JFileChooser to select the PDF document from which the text is to
     * be extracted.
     */
    private void selectPDFFile() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(new PDFFilter());
        jfc.setMultiSelectionEnabled(false);
        int ret = jfc.showDialog(this, "Open");
        if (ret == 0) {
            readFile = jfc.getSelectedFile();
            inputField.setText(readFile.getAbsolutePath());
            String outString = readFile.getAbsolutePath();
            outString = outString.substring(0, outString.length() - 3);
            outString = outString + "txt";
            outputField.setText(outString);
            progressLabel.setBackground(INFO);
            progressLabel.setText(PRESS_EXTRACT);
        } else {
            System.out.println(NO_FILE_SELECTED);
            progressLabel.setBackground(WARNING);
            progressLabel.setText(SELECT_FILE);
        }
    }

    /**
     * Open a JFileChooser to specify where to save the text document containing
     * the text extracted from the PDF document.
     */
    private void selectTXTFile() {
        JFileChooser jfc = new JFileChooser();
        File curDir;
        if (readFile != null) curDir = new File(readFile.getParent()); else curDir = new File(System.getProperty("user.home"));
        jfc.setCurrentDirectory(curDir);
        jfc.setFileFilter(new TXTFilter());
        jfc.setMultiSelectionEnabled(false);
        int ret = jfc.showDialog(this, "Save");
        if (ret == 0) {
            outputField.setText(jfc.getSelectedFile().getAbsolutePath() + ".txt");
        }
    }

    private javax.swing.JButton extractButton;

    private javax.swing.JCheckBox extractImagesChckbx;

    private javax.swing.JButton inputButton;

    private javax.swing.JTextField inputField;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JButton outputButton;

    private javax.swing.JTextField outputField;

    private javax.swing.JTextField progressLabel;

    private javax.swing.JButton quitButton;
}
