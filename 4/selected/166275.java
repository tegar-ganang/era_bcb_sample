package tools;

import core.Card;
import core.Lesson;
import gui.WordsListPanel;
import gui.mainframe;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import util.LessonFileType;
import util.LessonLoader;
import util.LessonSaver;
import util.ToolsInterface;

public class CompressLesson extends javax.swing.JFrame implements ToolsInterface {

    /** Creates new form mainFrame */
    public CompressLesson() {
        initComponents();
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        SourcePath = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        DestPath = new javax.swing.JTextField();
        SourceBT = new javax.swing.JButton();
        DestBT = new javax.swing.JButton();
        ImportBT = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel1.setLabelFor(SourcePath);
        jLabel1.setText("Source :");
        jLabel2.setLabelFor(SourcePath);
        jLabel2.setText("Destination :");
        SourceBT.setText("Browse");
        SourceBT.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SourceBTActionPerformed(evt);
            }
        });
        DestBT.setText("Browse");
        DestBT.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DestBTActionPerformed(evt);
            }
        });
        ImportBT.setText("Compress");
        ImportBT.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ImportBTActionPerformed(evt);
            }
        });
        jTable1.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Word", "Meaning", "Pronounciation" }) {

            boolean[] canEdit = new boolean[] { false, false, false };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(23, 23, 23).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1).add(org.jdesktop.layout.GroupLayout.LEADING, ImportBT).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(DestPath, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 277, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(DestBT)).add(org.jdesktop.layout.GroupLayout.LEADING, jLabel2).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(SourcePath, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 277, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(SourceBT)).add(org.jdesktop.layout.GroupLayout.LEADING, jLabel1)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jLabel1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(SourcePath, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(SourceBT)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jLabel2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(DestPath, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(DestBT)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(ImportBT).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)));
        pack();
    }

    private void ImportBTActionPerformed(java.awt.event.ActionEvent evt) {
        if (SourcePath.getText().equals(DestPath.getText())) {
            JOptionPane.showMessageDialog(this, "Source and destination are the same!", "Error", JOptionPane.ERROR);
            return;
        }
        LessonLoader lloader = new LessonLoader();
        Lesson lesson = lloader.load(new File(SourcePath.getText()));
        LessonSaver lsaver = new LessonSaver();
        lsaver.save(lesson, new File(DestPath.getText()));
        File fdir;
        fdir = new File(DestPath.getText() + "pron");
        fdir.mkdir();
        Lesson newlesson = lloader.load(new File(DestPath.getText()));
        Card[] crds = lesson.getAllCardsForSave();
        Card[] newcrds = newlesson.getAllCardsForSave();
        for (int i = 0; i < crds.length; i++) {
            newcrds[i].setExtraExamples(crds[i].getExtraExamples(new File(SourcePath.getText())), new File(DestPath.getText()));
            try {
                copyFile(new File(SourcePath.getText() + "pron/" + newcrds[i].getWord() + ".wav"), new File(fdir.getPath() + "/" + newcrds[i].getWord() + ".wav"));
            } catch (IOException ex) {
            }
            ((DefaultTableModel) jTable1.getModel()).addRow(new Object[] { crds[i].getWord(), crds[i].getMeaning(), crds[i].getPronounce() });
        }
    }

    private void DestBTActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new LessonFileType());
        if (fc.showSaveDialog(CompressLesson.this) == JFileChooser.APPROVE_OPTION) {
            File FileName;
            File f = fc.getSelectedFile();
            if (f.getPath().substring(f.getPath().lastIndexOf(".") + 1).toLowerCase().equals("mlf")) FileName = f; else FileName = new File(f.getPath() + ".mlf");
            DestPath.setText(FileName.getPath());
        }
    }

    private void SourceBTActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new LessonFileType());
        if (fc.showOpenDialog(CompressLesson.this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            SourcePath.setText(f.getPath());
        }
    }

    private javax.swing.JButton DestBT;

    private javax.swing.JTextField DestPath;

    private javax.swing.JButton ImportBT;

    private javax.swing.JButton SourceBT;

    private javax.swing.JTextField SourcePath;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTable jTable1;

    public void setLesson(Lesson lesson) {
    }

    public void setMainframe(mainframe mf) {
    }

    public void setWordsListPanel(WordsListPanel WLP) {
    }

    public String getMenuName() {
        return "Compress Old Lessons";
    }

    public void run() {
        this.setVisible(true);
    }

    public static void copyFile(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    public void runOnce() {
    }
}
