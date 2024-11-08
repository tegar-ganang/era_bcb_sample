package reports.acquisitions;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author yogesh
 */
public class BatchDupeCheckIF extends javax.swing.JInternalFrame {

    File createFile = null;

    /** Creates new form BatchDupeCheckIF */
    public BatchDupeCheckIF() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jPanel1 = new javax.swing.JPanel();
        lbExcelsheetToPerformeDupeCheck = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        bnBrowse = new javax.swing.JButton();
        bnOutputBrowse = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        bnPerformDupeCheck = new javax.swing.JButton();
        bnCancel = new javax.swing.JButton();
        bnClose = new javax.swing.JButton();
        setTitle("Batch Dupe Check Utility");
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));
        jPanel1.setLayout(new java.awt.GridBagLayout());
        lbExcelsheetToPerformeDupeCheck.setText("ExcelsheetToPerformeDupeCheck");
        jPanel1.add(lbExcelsheetToPerformeDupeCheck, new java.awt.GridBagConstraints());
        jLabel2.setText("Output report");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel2, gridBagConstraints);
        jTextField1.setText("                                         ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 11, 0);
        jPanel1.add(jTextField1, gridBagConstraints);
        jTextField2.setText("                                         ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        jPanel1.add(jTextField2, gridBagConstraints);
        bnBrowse.setText("...");
        bnBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnBrowseActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 9, 0, 0);
        jPanel1.add(bnBrowse, gridBagConstraints);
        bnOutputBrowse.setText("...");
        bnOutputBrowse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnOutputBrowseActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(bnOutputBrowse, gridBagConstraints);
        getContentPane().add(jPanel1);
        bnPerformDupeCheck.setText("Perform Dupe Check");
        bnPerformDupeCheck.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnPerformDupeCheckActionPerformed(evt);
            }
        });
        jPanel2.add(bnPerformDupeCheck);
        bnCancel.setText("Cancel");
        bnCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnCancelActionPerformed(evt);
            }
        });
        jPanel2.add(bnCancel);
        bnClose.setText("Close");
        bnClose.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnCloseActionPerformed(evt);
            }
        });
        jPanel2.add(bnClose);
        getContentPane().add(jPanel2);
        pack();
    }

    private void bnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void bnCancelActionPerformed(java.awt.event.ActionEvent evt) {
        int i = JOptionPane.showConfirmDialog(this, "are tou sure want to cancel this operation", "message", JOptionPane.YES_NO_OPTION);
        if (i == 0) {
            this.dispose();
        }
    }

    private void bnBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return ".xls";
            }

            @Override
            public boolean accept(File file) {
                boolean status = false;
                try {
                    if (file.isDirectory()) {
                        return true;
                    }
                    String fileName = file.getName().toLowerCase();
                    status = fileName.endsWith(".xls");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return status;
            }
        });
        int i = chooser.showOpenDialog(this);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (i == JFileChooser.APPROVE_OPTION) {
            String file = chooser.getSelectedFile().toString();
            StringTokenizer str = new StringTokenizer(file, ".");
            if (str.countTokens() <= 2) {
                str.nextToken();
                String s1 = str.nextToken(".");
                if (s1.equalsIgnoreCase("xls") || s1.equalsIgnoreCase("xlsx")) {
                    createFile = new File(chooser.getSelectedFile().toString());
                    if (createFile.exists()) {
                    } else {
                        JOptionPane.showMessageDialog(this, "file not found", "error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "The given file is not in given format", "check", JOptionPane.CANCEL_OPTION);
                }
            } else {
                JOptionPane.showMessageDialog(this, "The given file is not correct ", "check", JOptionPane.YES_OPTION);
            }
        }
    }

    private void bnOutputBrowseActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return ".csv";
            }

            @Override
            public boolean accept(File file) {
                boolean status = false;
                try {
                    String fileName = file.getName().toLowerCase();
                    status = fileName.endsWith(".csv");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return status;
            }
        });
        int i = chooser.showSaveDialog(this);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (i == JFileChooser.APPROVE_OPTION) {
            String file = chooser.getSelectedFile().toString();
            StringTokenizer str = new StringTokenizer(file, ".");
            if (str.countTokens() <= 2) {
                if (str.countTokens() == 1) {
                    createFile = new File(chooser.getSelectedFile().toString() + ".csv");
                    if (createFile.exists()) {
                        int cnt = JOptionPane.showConfirmDialog(this, "This file already exists ! Are you sure \n you want to over write it.", "check", JOptionPane.OK_CANCEL_OPTION);
                        if (cnt == 0) {
                            jTextField2.setText(createFile.toString());
                            System.out.println("override");
                        } else {
                            createFile = null;
                            jTextField2.setText("");
                        }
                    } else {
                        jTextField2.setText(createFile.toString());
                    }
                } else {
                    str.nextToken();
                    String s1 = str.nextToken(".");
                    if (s1.equalsIgnoreCase("csv")) {
                        createFile = new File(chooser.getSelectedFile().toString());
                        if (createFile.exists()) {
                            int cnt = JOptionPane.showConfirmDialog(this, "This file already exists ! Are you sure \n you want to over write it.", "check", JOptionPane.OK_CANCEL_OPTION);
                            if (cnt == 0) {
                                jTextField2.setText(createFile.toString());
                                System.out.println("override");
                            } else {
                                createFile = null;
                                jTextField2.setText("");
                            }
                        } else {
                            jTextField2.setText(createFile.toString());
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "The given file is not in .csv format \n Please create .csv extension.", "check", JOptionPane.CANCEL_OPTION);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "The given file name is not correct \n Please create a new file.", "check", JOptionPane.YES_OPTION);
            }
        } else {
            jTextField2.setText("");
        }
    }

    private void bnPerformDupeCheckActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (!createFile.exists()) {
                createFile.createNewFile();
            }
            String inputfiles = jTextField1.getText();
            String tl = "TITLE";
            String at = "AUTHOR";
            String pl = "PUBLISHER";
            String Yop = "YEAR OF PUBLICATION";
            String Pop = "PLACE OF PUBLICATION";
            String en = "EDITION";
            String Isbn = "ISBN";
            int tl1 = 0, at1 = 0, pl1 = 0, Yop1 = 0, pop1 = 0, en1 = 0, Isbn1 = 0;
            File fl = new File(inputfiles);
            if (!inputfiles.isEmpty() && fl.exists()) {
                POIFSFileSystem poifs = new POIFSFileSystem(new FileInputStream(fl));
                HSSFWorkbook hswb = new HSSFWorkbook(poifs);
                HSSFSheet sheet = hswb.getSheetAt(0);
                HSSFRow row = sheet.getRow(0);
                int i = row.getPhysicalNumberOfCells();
                for (int j = 0; j < i; j++) {
                    HSSFCell cell = row.getCell((short) j);
                    String sn = cell.getStringCellValue();
                    sn = sn.toUpperCase();
                    if (sn.equals(tl)) {
                        tl1 = j;
                    }
                    if (sn.equals(at)) {
                        at1 = j;
                    }
                    if (sn.equals(pl)) {
                        pl1 = j;
                    }
                    if (sn.equals(Yop)) {
                        Yop1 = j;
                    }
                    if (sn.equals(Pop)) {
                        pop1 = j;
                    }
                    if (sn.equals(en)) {
                        en1 = j;
                    }
                    if (sn.equals(Isbn)) {
                        Isbn1 = j;
                    }
                }
                System.out.println(tl1 + "Title:\n" + at1 + "author:\n" + pl1 + "Publisher:\n" + Yop1 + "Year of Publication:\n" + pop1 + "place of publication:\n" + en1 + "edition:\n" + Isbn1 + "ISBN:\n");
                for (Iterator<org.apache.poi.ss.usermodel.Row> rit = sheet.rowIterator(); rit.hasNext(); ) {
                    for (Iterator<Cell> cit = row.cellIterator(); cit.hasNext(); ) {
                        Cell cell = cit.next();
                        cell.getColumnIndex();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private javax.swing.JButton bnBrowse;

    private javax.swing.JButton bnCancel;

    private javax.swing.JButton bnClose;

    private javax.swing.JButton bnOutputBrowse;

    private javax.swing.JButton bnPerformDupeCheck;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JTextField jTextField1;

    private javax.swing.JTextField jTextField2;

    private javax.swing.JLabel lbExcelsheetToPerformeDupeCheck;
}