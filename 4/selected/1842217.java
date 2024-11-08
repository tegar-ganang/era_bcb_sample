package coopnetclient.frames.clientframe.tabs;

import coopnetclient.ErrorHandler;
import coopnetclient.Globals;
import coopnetclient.protocol.out.Protocol;
import coopnetclient.frames.clientframe.TabOrganizer;
import coopnetclient.utils.Settings;
import coopnetclient.utils.Verification;
import coopnetclient.utils.filechooser.FileChooser;
import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class FileTransferRecievePanel extends javax.swing.JPanel {

    private String filename;

    private String sender;

    private String ip;

    private String port;

    private File destfile = null;

    private long totalsize = 0;

    private long firstByteToSend = 0;

    private boolean running = false;

    private boolean resuming = false;

    private boolean wasCancelled = false;

    private ServerSocket serverSocket = null;

    SocketChannel socket = null;

    private int progress = 0;

    private long onePercentInBytes = 0;

    /** Creates new form FileTransfer */
    public FileTransferRecievePanel(String sender, long size, String filename, String ip, String port) {
        initComponents();
        this.filename = filename;
        this.sender = sender;
        this.totalsize = size;
        this.ip = ip;
        this.port = port;
        onePercentInBytes = totalsize / 100;
        lbl_fileValue.setText(filename);
        lbl_senderValue.setText(sender);
        tf_savePath.setText(Settings.getRecieveDestination());
        coopnetclient.utils.Colorizer.colorize(this);
        int i = 0;
        while (size > 1024) {
            size = size / 1024;
            i++;
        }
        lbl_sizeValue.setText(size + " ");
        if (i == 0) {
            lbl_sizeValue.setText(lbl_sizeValue.getText() + " B");
        }
        if (i == 1) {
            lbl_sizeValue.setText(lbl_sizeValue.getText() + " kB");
        }
        if (i == 2) {
            lbl_sizeValue.setText(lbl_sizeValue.getText() + " MB");
        }
        if (i == 3) {
            lbl_sizeValue.setText(lbl_sizeValue.getText() + " GB");
        }
    }

    private File Checkfile(File file, int n) {
        try {
            String path = file.getCanonicalPath();
            String name = file.getName();
            path = path.substring(0, path.length() - name.length());
            int idx = -1;
            idx = name.indexOf(".");
            if (name.charAt(idx - 1) == ')' && name.charAt(idx - 3) == '(') {
                name = name.substring(0, idx - 2) + n + ")" + name.substring(idx);
            } else {
                name = name.substring(0, idx) + "(" + n + ")" + name.substring(idx);
            }
            return new File(path + name);
        } catch (Exception e) {
            e.printStackTrace();
            return file;
        }
    }

    /**
     * Returns if the file can be resumed (size is less than totalsize)
     * and sets the firstByteTosend variable
     */
    private boolean checkResumability() {
        try {
            File checkthis = getDestFile();
            long currentsize = checkthis.length();
            if (totalsize <= currentsize) {
                firstByteToSend = 0;
                JOptionPane.showMessageDialog(Globals.getClientFrame(), "<html>The existing file is larger or equal to the file sent!<br>Probably not the same file!", "Cannot resume file!", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            firstByteToSend = currentsize + 1;
            return true;
        } catch (Exception e) {
            firstByteToSend = 0;
            return false;
        }
    }

    private File getDestFile() throws IOException {
        File dest;
        String fullpath = tf_savePath.getText();
        if (!(new File(fullpath).exists())) {
            new File(fullpath).mkdirs();
        }
        if (!fullpath.endsWith("/") || !fullpath.endsWith("\\")) {
            fullpath = fullpath + "/";
        }
        fullpath += filename;
        dest = new File(fullpath).getCanonicalFile();
        return dest;
    }

    public void setTimeLeft(long time) {
        final int seconds;
        final int minutes;
        final long hours;
        seconds = (int) (time % 60);
        time = time / 60;
        minutes = (int) (time % 60);
        time = time / 60;
        hours = (int) time;
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                lbl_timeLeft.setText(hours + ":" + minutes + ":" + seconds);
            }
        });
    }

    public String getSender() {
        return lbl_senderValue.getText();
    }

    public String getFilename() {
        return lbl_fileValue.getText();
    }

    public void cancelled() {
        wasCancelled = true;
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                lbl_statusValue.setText("Peer cancelled the transfer!");
                btn_refuse.setText("Close");
                btn_accept.setVisible(false);
            }
        });
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
        }
    }

    private void startDownloading() {
        new Thread() {

            long starttime;

            @Override
            public void run() {
                Socket socket = null;
                BufferedInputStream bi = null;
                BufferedOutputStream bo = null;
                try {
                    try {
                        running = true;
                        updateStatusLabel("Transfer starting...");
                        serverSocket = new ServerSocket(Settings.getFiletTansferPort());
                        socket = serverSocket.accept();
                        serverSocket.close();
                    } catch (Exception e) {
                        if (e instanceof java.net.BindException) {
                            serverSocket.close();
                            Protocol.turnTransferAround(sender, filename);
                            Thread.sleep(500);
                            startDownloadingRetry();
                            return;
                        }
                    }
                    bi = new BufferedInputStream(socket.getInputStream());
                    destfile = getDestFile();
                    if (!resuming) {
                        int n = 1;
                        while (!destfile.createNewFile()) {
                            destfile = Checkfile(destfile, n);
                            n++;
                        }
                    } else {
                        if (!destfile.exists()) {
                            destfile.createNewFile();
                        }
                    }
                    bo = new BufferedOutputStream(new FileOutputStream(destfile, resuming));
                    updateStatusLabel("Transferring...");
                    starttime = System.currentTimeMillis();
                    int readedbyte;
                    long recievedBytes = 0;
                    long currenttime;
                    long timeelapsed;
                    long tmpCounter = 0;
                    while (running && (readedbyte = bi.read()) != -1) {
                        bo.write(readedbyte);
                        ++recievedBytes;
                        ++tmpCounter;
                        if (tmpCounter >= onePercentInBytes) {
                            tmpCounter = 0;
                            bo.flush();
                            progress = (int) ((((recievedBytes + firstByteToSend) * 1.0) / (totalsize)) * 100);
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    pgb_progress.setValue(progress);
                                }
                            });
                            currenttime = System.currentTimeMillis();
                            timeelapsed = currenttime - starttime;
                            timeelapsed = timeelapsed / 1000;
                            setTimeLeft((long) (((totalsize - firstByteToSend) - recievedBytes) / (recievedBytes * 1.0) * timeelapsed));
                        }
                    }
                    bo.flush();
                    if ((recievedBytes + firstByteToSend - (resuming ? 1 : 0)) == totalsize) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                lbl_statusValue.setText("Transfer complete!");
                                pgb_progress.setValue(100);
                                btn_accept.setEnabled(true);
                                btn_refuse.setText("Close");
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                lbl_statusValue.setText("Transfer incomplete!");
                                btn_refuse.setText("Close");
                            }
                        });
                    }
                    running = false;
                    bi.close();
                    bo.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!wasCancelled) {
                        updateStatusLabel("Error: " + e.getLocalizedMessage());
                    }
                } finally {
                    try {
                        if (bi != null) {
                            bi.close();
                        }
                        if (bo != null) {
                            bo.close();
                        }
                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }.start();
    }

    public void turnAround() {
        try {
            serverSocket.close();
            serverSocket = null;
        } catch (Exception e) {
        }
        startDownloadingRetry();
    }

    private void updateStatusLabel(final String status) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                lbl_statusValue.setText(status);
            }
        });
    }

    private void startDownloadingRetry() {
        new Thread() {

            long starttime;

            BufferedOutputStream bo = null;

            @Override
            public void run() {
                try {
                    running = true;
                    updateStatusLabel("Retrying...");
                    Thread.sleep(1000);
                    socket = SocketChannel.open();
                    socket.connect(new InetSocketAddress(ip, new Integer(port)));
                    destfile = getDestFile();
                    if (!resuming) {
                        int n = 1;
                        while (!destfile.createNewFile()) {
                            destfile = Checkfile(destfile, n);
                            n++;
                        }
                    } else {
                        if (!destfile.exists()) {
                            destfile.createNewFile();
                        }
                    }
                    bo = new BufferedOutputStream(new FileOutputStream(destfile, resuming));
                    updateStatusLabel("Transferring...");
                    starttime = System.currentTimeMillis();
                    long recievedBytes = 0;
                    long currenttime = 0;
                    long timeelapsed = 0;
                    long tmpCounter = 0;
                    ByteBuffer buffer = ByteBuffer.allocate(1000);
                    buffer.rewind();
                    while (running && ((socket.read(buffer)) != -1)) {
                        buffer.flip();
                        bo.write(buffer.array(), 0, buffer.limit());
                        recievedBytes += buffer.limit();
                        buffer.rewind();
                        tmpCounter += buffer.limit();
                        if (tmpCounter >= onePercentInBytes) {
                            tmpCounter = 0;
                            bo.flush();
                            progress = (int) ((((recievedBytes + firstByteToSend) * 1.0) / (totalsize)) * 100);
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    pgb_progress.setValue(progress);
                                }
                            });
                            currenttime = System.currentTimeMillis();
                            timeelapsed = currenttime - starttime;
                            timeelapsed = timeelapsed / 1000;
                            setTimeLeft((long) (((totalsize - firstByteToSend) - recievedBytes) / (recievedBytes * 1.0) * timeelapsed));
                        }
                    }
                    bo.flush();
                    if ((recievedBytes + firstByteToSend - (resuming ? 1 : 0)) == totalsize) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                lbl_statusValue.setText("Transfer complete!");
                                pgb_progress.setValue(100);
                                btn_accept.setEnabled(true);
                                btn_refuse.setText("Close");
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                lbl_statusValue.setText("Transfer incomplete!");
                                btn_refuse.setText("Close");
                            }
                        });
                    }
                    running = false;
                    bo.close();
                    socket.close();
                    socket = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!wasCancelled) {
                        updateStatusLabel("Error: " + e.getLocalizedMessage());
                    }
                } finally {
                    try {
                        if (bo != null) {
                            bo.close();
                        }
                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }.start();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        lbl_sender = new javax.swing.JLabel();
        lbl_senderValue = new javax.swing.JLabel();
        lbl_file = new javax.swing.JLabel();
        lbl_fileValue = new javax.swing.JLabel();
        lbl_size = new javax.swing.JLabel();
        lbl_sizeValue = new javax.swing.JLabel();
        lbl_progress = new javax.swing.JLabel();
        pgb_progress = new javax.swing.JProgressBar();
        btn_refuse = new javax.swing.JButton();
        lbl_status = new javax.swing.JLabel();
        lbl_statusValue = new javax.swing.JLabel();
        btn_accept = new javax.swing.JButton();
        lbl_savePath = new javax.swing.JLabel();
        btn_browseSavePath = new javax.swing.JButton();
        lbl_note = new javax.swing.JLabel();
        lbl_timeLeft = new javax.swing.JLabel();
        lbl_timeLeftValue = new javax.swing.JLabel();
        cb_Resume = new javax.swing.JCheckBox();
        tf_savePath = new coopnetclient.frames.components.AdvancedJTextField();
        setBorder(javax.swing.BorderFactory.createTitledBorder("Recieve File"));
        setMaximumSize(null);
        lbl_sender.setText("Sender:");
        lbl_senderValue.setText("username");
        lbl_file.setText("Filename:");
        lbl_fileValue.setText("filename");
        lbl_size.setText("Size:");
        lbl_sizeValue.setText("1234 MB");
        lbl_progress.setText("Progress:");
        btn_refuse.setText("Refuse");
        btn_refuse.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_refuseActionPerformed(evt);
            }
        });
        lbl_status.setText("Status:");
        lbl_statusValue.setText("Waiting...");
        btn_accept.setText("Accept");
        btn_accept.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_acceptActionPerformed(evt);
            }
        });
        lbl_savePath.setText("Save to:");
        btn_browseSavePath.setText("Browse");
        btn_browseSavePath.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_browseSavePathActionPerformed(evt);
            }
        });
        lbl_note.setText("<html><b>Note:</b> At least one of you has to have an open port for the transfer to work. Your port is: " + Settings.getFiletTansferPort());
        lbl_timeLeft.setText("??:??:??");
        lbl_timeLeftValue.setText("Time left:");
        cb_Resume.setText("Resume file");
        cb_Resume.setToolTipText("Note: Only the file with the same filename can be resumed !");
        cb_Resume.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cb_ResumeActionPerformed(evt);
            }
        });
        tf_savePath.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tf_savePathActionPerformed(evt);
            }
        });
        tf_savePath.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                tf_savePathFocusLost(evt);
            }
        });
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(lbl_savePath, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(lbl_file, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(lbl_sender, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE).addComponent(lbl_size, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(layout.createSequentialGroup().addComponent(tf_savePath, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btn_browseSavePath)).addComponent(lbl_senderValue, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE).addComponent(lbl_fileValue, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE).addComponent(lbl_sizeValue, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE).addComponent(cb_Resume, javax.swing.GroupLayout.Alignment.LEADING))).addComponent(lbl_note, javax.swing.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE).addComponent(pgb_progress, javax.swing.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(lbl_progress).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 387, Short.MAX_VALUE).addComponent(lbl_timeLeftValue).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbl_timeLeft, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(btn_accept).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 411, Short.MAX_VALUE).addComponent(btn_refuse)).addGroup(layout.createSequentialGroup().addComponent(lbl_status, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbl_statusValue, javax.swing.GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbl_sender).addComponent(lbl_senderValue)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbl_file).addComponent(lbl_fileValue)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbl_size).addComponent(lbl_sizeValue)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbl_savePath).addComponent(btn_browseSavePath).addComponent(tf_savePath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(cb_Resume).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbl_statusValue).addComponent(lbl_status)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lbl_progress).addComponent(lbl_timeLeft).addComponent(lbl_timeLeftValue)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pgb_progress, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btn_refuse).addComponent(btn_accept)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lbl_note).addContainerGap(22, Short.MAX_VALUE)));
        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { btn_accept, btn_browseSavePath, btn_refuse });
    }

    private void btn_acceptActionPerformed(java.awt.event.ActionEvent evt) {
        if (btn_accept.getText().equals("Accept")) {
            startDownloading();
            Protocol.acceptTransfer(sender, filename, firstByteToSend);
            btn_accept.setText("Open file");
            btn_accept.setEnabled(false);
            btn_refuse.setText("Cancel");
            cb_Resume.setEnabled(false);
            tf_savePath.setEnabled(false);
            btn_browseSavePath.setEnabled(false);
        } else {
            if (destfile != null) {
                Desktop desktop = null;
                if (Desktop.isDesktopSupported()) {
                    desktop = Desktop.getDesktop();
                    try {
                        desktop.open(destfile.getCanonicalFile());
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void btn_refuseActionPerformed(java.awt.event.ActionEvent evt) {
        if (btn_refuse.getText().equals("Refuse")) {
            Protocol.refuseTransfer(sender, filename);
            TabOrganizer.closeFileTransferReceivePanel(this);
        } else if (btn_refuse.getText().equals("Cancel")) {
            running = false;
            Protocol.cancelTransfer(sender, filename);
            TabOrganizer.closeFileTransferReceivePanel(this);
        } else {
            TabOrganizer.closeFileTransferReceivePanel(this);
        }
    }

    private void btn_browseSavePathActionPerformed(java.awt.event.ActionEvent evt) {
        new Thread() {

            @Override
            public void run() {
                try {
                    File inputfile = null;
                    FileChooser fc = new FileChooser(FileChooser.DIRECTORIES_ONLY_MODE);
                    int returnVal = fc.choose(Globals.getLastOpenedDir());
                    if (returnVal == FileChooser.SELECT_ACTION) {
                        inputfile = fc.getSelectedFile();
                        if (inputfile != null) {
                            tf_savePath.setText(inputfile.getPath());
                        }
                    }
                } catch (Exception e) {
                    ErrorHandler.handleException(e);
                }
            }
        }.start();
    }

    private void cb_ResumeActionPerformed(java.awt.event.ActionEvent evt) {
        if (!resuming) {
            resuming = checkResumability();
        } else {
            resuming = false;
            firstByteToSend = 0;
        }
        cb_Resume.setSelected(resuming);
    }

    private void tf_savePathActionPerformed(java.awt.event.ActionEvent evt) {
        if (!Verification.verifyDirectory(tf_savePath.getText())) {
            tf_savePath.showErrorMessage("Invalid directory!");
        }
    }

    private void tf_savePathFocusLost(java.awt.event.FocusEvent evt) {
        if (!Verification.verifyDirectory(tf_savePath.getText())) {
            tf_savePath.showErrorMessage("Invalid directory!");
        }
    }

    private javax.swing.JButton btn_accept;

    private javax.swing.JButton btn_browseSavePath;

    private javax.swing.JButton btn_refuse;

    private javax.swing.JCheckBox cb_Resume;

    private javax.swing.JLabel lbl_file;

    private javax.swing.JLabel lbl_fileValue;

    private javax.swing.JLabel lbl_note;

    private javax.swing.JLabel lbl_progress;

    private javax.swing.JLabel lbl_savePath;

    private javax.swing.JLabel lbl_sender;

    private javax.swing.JLabel lbl_senderValue;

    private javax.swing.JLabel lbl_size;

    private javax.swing.JLabel lbl_sizeValue;

    private javax.swing.JLabel lbl_status;

    private javax.swing.JLabel lbl_statusValue;

    private javax.swing.JLabel lbl_timeLeft;

    private javax.swing.JLabel lbl_timeLeftValue;

    private javax.swing.JProgressBar pgb_progress;

    private coopnetclient.frames.components.AdvancedJTextField tf_savePath;
}
