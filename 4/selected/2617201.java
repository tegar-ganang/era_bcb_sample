package com.bac1ca.project.copier;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author rvv
 */
public class CopyProject extends JFrame {

    private JButton jButton1;

    private JButton jButton2;

    private JButton jButton3;

    private JLabel jLabel1;

    private JLabel jLabel2;

    private JRadioButton jRadioButton1;

    private JRadioButton jRadioButton2;

    private JTextField jTextField1;

    private JTextField jTextField2;

    private static final String JAVA_TYPES = ".java";

    private static final String CPP_TYPES = ".h .cpp .ui .hpp";

    private static final String CPP_IGNORE = "debug .make";

    public CopyProject() {
        initComponents();
    }

    private void initComponents() {
        jButton1 = new JButton();
        jTextField1 = new JTextField();
        jRadioButton1 = new JRadioButton();
        jRadioButton2 = new JRadioButton();
        jButton2 = new JButton();
        jLabel1 = new JLabel();
        jTextField2 = new JTextField();
        jLabel2 = new JLabel();
        jButton3 = new JButton();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2, screenSize.height / 2);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("project copier");
        setResizable(false);
        jLabel1.setText("source");
        jLabel2.setText("target");
        jButton1.setText("Browse");
        jButton1.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent evt) {
                jButton1MousePressed(evt);
            }
        });
        jButton2.setText("Browse");
        jButton2.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent evt) {
                jButton2MousePressed(evt);
            }
        });
        jButton3.setText("done");
        jButton3.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent evt) {
                jButton3MousePressed(evt);
            }
        });
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("java project");
        jRadioButton1.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                jRadioButton1StateChanged(evt);
            }
        });
        jRadioButton2.setText("C++ project");
        jRadioButton2.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                jRadioButton2StateChanged(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton1)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(layout.createSequentialGroup().addComponent(jRadioButton1).addGap(36, 36, 36).addComponent(jRadioButton2))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButton1).addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel1)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButton2).addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabel2)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jButton3).addComponent(jRadioButton2).addComponent(jRadioButton1)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pack();
    }

    private void jRadioButton1StateChanged(ChangeEvent evt) {
        if (jRadioButton1.isSelected()) jRadioButton2.setSelected(false);
    }

    private void jRadioButton2StateChanged(javax.swing.event.ChangeEvent evt) {
        if (jRadioButton2.isSelected()) jRadioButton1.setSelected(false);
    }

    private void jButton1MousePressed(MouseEvent evt) {
        JFileChooser fileopen;
        if (jTextField1.getText().equals("")) fileopen = new JFileChooser(new File(".")); else fileopen = new JFileChooser(new File(jTextField1.getText()));
        fileopen.setFileSelectionMode(WIDTH);
        int ret = fileopen.showDialog(this, "open");
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            jTextField1.setText(file.getPath());
        }
    }

    private void jButton2MousePressed(MouseEvent evt) {
        JFileChooser fileopen;
        if (jTextField2.getText().equals("")) fileopen = new JFileChooser(new File(".")); else fileopen = new JFileChooser(new File(jTextField2.getText()));
        fileopen.setFileSelectionMode(WIDTH);
        int ret = fileopen.showDialog(this, "open");
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            jTextField2.setText(file.getPath());
        }
    }

    private void jButton3MousePressed(MouseEvent evt) {
        String sourcePath = jTextField1.getText();
        String targetPath = jTextField2.getText();
        if (sourcePath.equals("") || targetPath.equals("")) {
            JOptionPane.showMessageDialog(this, "Source path or target path is empty!", null, JOptionPane.WARNING_MESSAGE);
        } else {
            try {
                File sourse = new File(sourcePath);
                if (jRadioButton1.isSelected()) recursionJava(sourse, targetPath); else if (jRadioButton2.isSelected()) recursionCpp(sourse, targetPath);
                JOptionPane.showMessageDialog(this, "Operation completed.", null, JOptionPane.PLAIN_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new CopyProject().setVisible(true);
            }
        });
    }

    private static void recursionJava(File file, String path) throws IOException {
        if (file.listFiles() != null) {
            path += "/" + file.getName() + "/";
            for (File f : file.listFiles()) {
                recursionJava(f, path);
                if (condition(f.getName(), JAVA_TYPES)) {
                    File dir = new File(path);
                    dir.mkdirs();
                    copyFile(f.getPath(), path + f.getName());
                    System.out.println("copy file... \t" + f.getName());
                }
            }
        }
    }

    private static void recursionCpp(File file, String path) throws IOException {
        if (!(condition(file.getName(), CPP_IGNORE)) && file.listFiles() != null) {
            path += "/" + file.getName() + "/";
            for (File f : file.listFiles()) {
                recursionCpp(f, path);
                if (condition(f.getName(), CPP_TYPES)) {
                    File dir = new File(path);
                    dir.mkdirs();
                    copyFile(f.getPath(), path + f.getName());
                    System.out.println("copy file... \t" + f.getName());
                }
            }
        }
    }

    private static void copyFile(String src, String target) throws IOException {
        FileChannel ic = new FileInputStream(src).getChannel();
        FileChannel oc = new FileOutputStream(target).getChannel();
        ic.transferTo(0, ic.size(), oc);
        ic.close();
        oc.close();
    }

    private static boolean condition(String val, String types) {
        for (String type : types.split("[\\s]+")) {
            if (val.endsWith(type)) return true;
        }
        return false;
    }
}
