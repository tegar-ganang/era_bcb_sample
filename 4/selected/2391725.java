package ncclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import ncclient.gui.SettingsWindow;

/**
 *
 * @author  Carl Berglund
 */
public class GUI extends javax.swing.JFrame {

    private DropTarget dropTarget;

    private DropTargetListener dtListener;

    private int acceptableActions = DnDConstants.ACTION_COPY;

    public File[] filer;

    BufferedInputStream filereader;

    BufferedOutputStream filewriter;

    private String ip;

    Socket socket;

    Socket fileSocket;

    private BufferedReader reader;

    private BufferedWriter writer;

    private BufferedReader fileReader;

    private BufferedWriter fileWriter;

    Thread listener;

    Thread fileListener;

    String nickname = "";

    String myNick = "";

    String fileName = "";

    File file;

    String saveDirectory = ".\\";

    BufferedWriter settingsWriter;

    BufferedReader settingsReader;

    /** Creates new form GUI */
    public GUI() {
        initComponents();
        setJMenuBar(jMenuBar1);
        Dimension d = getToolkit().getScreenSize();
        int width = (d.width / 2) - (this.getWidth() / 2);
        int height = (d.height / 2) - (this.getHeight() / 2);
        this.setLocation(width, height);
        dtListener = new DTListener();
        dropTarget = new DropTarget(jTextArea1, acceptableActions, dtListener, true);
        this.checkSettings();
        if (!myNick.equals("")) {
            jTextField1.setText(myNick);
        }
    }

    private void initComponents() {
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jEditorPane1 = new javax.swing.JEditorPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jTextField3 = new javax.swing.JTextField();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel4 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jMenu1.setText("File");
        jMenuItem1.setText("Settings");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuItem2.setText("Quit");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenuBar1.add(jMenu1);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("P2PChat");
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Messages"));
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jEditorPane1.setEditable(false);
        jScrollPane1.setViewportView(jEditorPane1);
        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane1));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE));
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Send message"));
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextArea1KeyPressed(evt);
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextArea1KeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(jTextArea1);
        jButton1.setText("Send");
        jButton1.setEnabled(false);
        jButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel3Layout.createSequentialGroup().add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 365, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jButton1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE).addContainerGap()));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel3Layout.createSequentialGroup().add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE).add(jButton1)).addContainerGap()));
        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Send file"));
        jButton4.setText("Open");
        jButton4.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        jButton5.setText("Send");
        jButton5.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton5.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        jTextField3.setEditable(false);
        jLabel4.setText("Idle");
        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup().add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jTextField3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE).add(jLabel4).add(org.jdesktop.layout.GroupLayout.TRAILING, jProgressBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jButton4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jButton5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 61, Short.MAX_VALUE)).addContainerGap()));
        jPanel5Layout.setVerticalGroup(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel5Layout.createSequentialGroup().add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jButton4).add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(8, 8, 8).add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(jPanel5Layout.createSequentialGroup().add(jLabel4).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jProgressBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(jButton5))));
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)));
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Connection"));
        jButton2.setText("Connect");
        jButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jTextField2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });
        jTextField2.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField2KeyReleased(evt);
            }
        });
        jLabel3.setText("Adress");
        jButton3.setText("jButton3");
        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup().addContainerGap(378, Short.MAX_VALUE).add(jLabel1).add(73, 73, 73)).add(jPanel4Layout.createSequentialGroup().add(jLabel3).addContainerGap()).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup().add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jTextField1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.TRAILING, jTextField2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false).add(jPanel4Layout.createSequentialGroup().add(jButton3).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).add(org.jdesktop.layout.GroupLayout.TRAILING, jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 101, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))));
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel4Layout.createSequentialGroup().add(jLabel3).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel4Layout.createSequentialGroup().add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(162, 162, 162).add(jLabel1)).add(jPanel4Layout.createSequentialGroup().add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(16, 16, 16).add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jButton3).add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))));
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 138, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap(14, Short.MAX_VALUE)));
        pack();
    }

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (!fileName.equals("")) {
                fileWriter.write("file " + file.length() + " " + jTextField3.getText() + "\n");
                fileWriter.flush();
                addMessage(myNick, "Sending file: " + file.getPath() + " @ " + (file.length() / 10) + " kB");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser jFileChooser1 = new JFileChooser();
        String key;
        if (jFileChooser1.showOpenDialog(null) == jFileChooser1.APPROVE_OPTION) {
            file = jFileChooser1.getSelectedFile();
            jTextField3.setText(file.getName());
            fileName = file.getPath();
            System.out.println(fileName);
        }
    }

    private void jTextField2KeyReleased(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == evt.VK_ENTER) {
            jButton2ActionPerformed(null);
        }
    }

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {
        jButton2ActionPerformed(null);
    }

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jTextArea1KeyReleased(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == evt.VK_ENTER && jButton1.isEnabled()) {
            jTextArea1.setText("");
        }
    }

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void jTextArea1KeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == evt.VK_ENTER && jButton1.isEnabled()) {
            jButton1ActionPerformed(null);
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        if (!jTextArea1.getText().equals("")) {
            sendMessage(jTextField1.getText(), jTextField2.getText(), jTextArea1.getText());
        }
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        if (jButton2.getText().equals("Connect")) {
            if (myNick.equals("")) {
                JOptionPane.showMessageDialog(null, "No nickname is set.", "ERROR", JOptionPane.WARNING_MESSAGE);
            } else {
                connect();
            }
        } else if (jButton2.getText().equals("Disconnect")) {
            try {
                writer.write("bye\n");
                writer.flush();
                setDisconnection();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Could not send the message", "ERROR", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new GUI().setVisible(true);
            }
        });
    }

    public void addMessage(String from, String text) {
        if (!jEditorPane1.getText().equals("")) {
            jEditorPane1.setText(jEditorPane1.getText() + "\n");
        }
        jEditorPane1.setText(jEditorPane1.getText() + from + " s�ger:\n    " + text);
        jEditorPane1.setCaretPosition(jEditorPane1.getText().length());
    }

    public void setConnection(Socket socket) {
        this.socket = socket;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "reader and writer error", "ERROR", JOptionPane.WARNING_MESSAGE);
        }
        jButton2.setText("Disconnect");
        jTextField2.setEditable(false);
        jTextField1.setEditable(false);
        jButton1.setEnabled(true);
        setTitle("P2PChat: CONNECTED to " + socket.getInetAddress().toString().split("/")[1]);
        jTextField2.setText(socket.getInetAddress().toString().split("/")[1]);
    }

    public void setDisconnection() {
        try {
            setTitle("P2PChat");
            jButton2.setText("Connect");
            jTextField2.setEditable(true);
            jTextField2.setText("");
            jButton1.setEnabled(false);
            jEditorPane1.setText("");
            writer.close();
            socket.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Disconnection error", "ERROR", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void connect() {
        try {
            socket = new Socket(jTextField2.getText(), 1337);
            fileSocket = new Socket(jTextField2.getText(), 7331);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            fileReader = new BufferedReader(new InputStreamReader(fileSocket.getInputStream()));
            fileWriter = new BufferedWriter(new OutputStreamWriter(fileSocket.getOutputStream()));
            writer.write("info " + jTextField1.getText() + "\n");
            writer.flush();
            jTextField2.setEditable(false);
            jTextField2.setText("Waiting for other side to reply...");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Could not connect to " + jTextField2.getText(), "ERROR", JOptionPane.WARNING_MESSAGE);
        }
    }

    public class DTListener implements DropTargetListener {

        public void drop(DropTargetDropEvent evt) {
            evt.acceptDrop(acceptableActions);
            Transferable tmp = evt.getTransferable();
            List files;
            String str;
            try {
                files = (List) tmp.getTransferData(DataFlavor.javaFileListFlavor);
                filer = new File[files.size()];
                for (int i = 0; i < files.size(); i++) {
                    filer[i] = (File) files.get(i);
                    jEditorPane1.setText(jEditorPane1.getText() + files.get(i).toString() + "\n");
                }
            } catch (UnsupportedFlavorException e) {
                jLabel1.setText("UnsupportedFlavorException: " + e);
            } catch (IOException e) {
                jLabel1.setText("IOException: " + e);
            }
            jTextArea1.setBackground(Color.WHITE);
        }

        public void dragEnter(DropTargetDragEvent evt) {
        }

        public void dragOver(DropTargetDragEvent evt) {
            jTextArea1.setBackground(Color.GRAY);
        }

        public void dropActionChanged(DropTargetDragEvent evt) {
            jTextArea1.setBackground(Color.WHITE);
        }

        public void dragExit(DropTargetEvent evt) {
            jTextArea1.setBackground(Color.WHITE);
        }
    }

    public void sendMessage(String from, String to, String message) {
        try {
            if (!jTextArea1.getText().equals("")) {
                writer.write("message " + message + "\n");
                jTextArea1.setText("");
                writer.flush();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        if (!jEditorPane1.getText().equals("")) {
            jEditorPane1.setText(jEditorPane1.getText() + "\n");
        }
        jEditorPane1.setText(jEditorPane1.getText() + from + " s�ger:\n    " + message);
    }

    public void sendFile() {
        Thread t = new Thread(new FileTransfer(fileSocket, this, 0, fileName, file.length()));
        t.start();
        jLabel4.setText("Sending");
        System.out.println("Skickar");
    }

    public void setBar(long v) {
        int value = Integer.parseInt(Long.toString(v));
        jProgressBar1.setValue(value);
    }

    public void setSaveDirectory(String directory) {
        saveDirectory = directory;
        System.out.println(saveDirectory);
    }

    public void checkSettings() {
        try {
            File file = new File("settings.ini");
            if (file.exists() && file.canRead() && file.canWrite()) {
                settingsWriter = new BufferedWriter(new FileWriter(file, true));
                settingsReader = new BufferedReader(new FileReader(file));
                String line;
                String[] array;
                while (settingsReader.ready()) {
                    line = settingsReader.readLine();
                    array = line.split("=", 2);
                    if (array[0].equals("nickname") && !array[1].equals("")) {
                        myNick = array[1];
                    }
                }
                System.out.println(myNick);
            } else {
                settingsWriter = new BufferedWriter(new FileWriter(file, true));
                settingsReader = new BufferedReader(new FileReader(file));
                settingsWriter.write("save_directory=.\\\n");
                settingsWriter.write("nickname=\n");
                settingsWriter.flush();
            }
            settingsReader.close();
            settingsWriter.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void setFileLabel(String s) {
        jLabel4.setText(s);
    }

    public void fileFinished() {
        jLabel4.setText("Idle");
        jTextField3.setText("");
        jProgressBar1.setValue(0);
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButton2;

    private javax.swing.JButton jButton3;

    private javax.swing.JButton jButton4;

    private javax.swing.JButton jButton5;

    private javax.swing.JEditorPane jEditorPane1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JMenuItem jMenuItem1;

    private javax.swing.JMenuItem jMenuItem2;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JProgressBar jProgressBar1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JTextArea jTextArea1;

    private javax.swing.JTextField jTextField1;

    private javax.swing.JTextField jTextField2;

    private javax.swing.JTextField jTextField3;
}
