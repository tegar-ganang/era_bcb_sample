package org.gjt.universe.gui;

import org.gjt.universe.Config;
import com.fooware.net.FtpClient;
import com.fooware.net.FtpInputStream;
import javax.swing.*;
import java.io.*;
import java.util.Vector;
import java.awt.event.*;

public class GUIScriptRepository extends UniverseJDialog {

    private JButton okButton;

    private JButton cancelButton;

    private JPanel listPanel;

    private JPanel buttonPanel;

    private JList list;

    private ListSelectionModel listSelectionModel;

    private FtpClient ftp;

    private String dir;

    public GUIScriptRepository() {
        okButton = new JButton("Ok");
        cancelButton = new JButton("Cancel");
        listPanel = new JPanel();
        buttonPanel = new JPanel();
        dir = Config.getString("script.dir", System.getProperty("user.home"));
        try {
            ftp = new FtpClient();
            ftp.connect("pele.cx");
            ftp.userName("anonymous");
            ftp.password("universe");
            ftp.dataPort();
            ftp.changeWorkingDirectory("/pub/universe/scripts");
            FtpInputStream ftpInput = ftp.nameListStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(ftpInput));
            Vector v = new Vector();
            String line = in.readLine();
            while (line != null) {
                v.addElement(line);
                line = in.readLine();
            }
            ftpInput.close();
            list = new JList(v);
        } catch (IOException e) {
            initFtpError();
        }
        listSelectionModel = list.getSelectionModel();
        listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        listPanel.add(list);
        buttonPanel.add("West", okButton);
        buttonPanel.add("East", cancelButton);
        getContentPane().add("North", new JScrollPane(listPanel));
        getContentPane().add("South", buttonPanel);
        pack();
        setSize(300, 150);
        setVisible(true);
    }

    void ok() {
        Object[] files = list.getSelectedValues();
        for (int i = 0; i < files.length; i++) {
            String filename = (String) files[i];
            File localfile = new File(dir, filename);
            if (localfile.exists()) {
                int val = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "File exists", JOptionPane.YES_NO_OPTION);
                if (val == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            try {
                FileOutputStream localStream = new FileOutputStream(localfile);
                BufferedOutputStream localBuffer = new BufferedOutputStream(localStream, 1024);
                ftp.dataPort();
                FtpInputStream ftpInput = ftp.retrieveStream(filename);
                BufferedInputStream remoteBuffer = new BufferedInputStream(ftpInput, 1024);
                byte[] b = new byte[1024];
                int read;
                read = remoteBuffer.read(b, 0, 1024);
                while (read != -1) {
                    localBuffer.write(b, 0, read);
                    read = remoteBuffer.read(b, 0, 1024);
                }
                ftpInput.close();
                localBuffer.flush();
            } catch (IOException e) {
                transferError(e.getMessage());
            }
        }
        close();
    }

    public void close() {
        try {
            ftp.logout();
        } catch (IOException e) {
        }
        dispose();
    }

    void initFtpError() {
        final JDialog err = new JDialog();
        JLabel message = new JLabel(ftp.getResponse().getMessage());
        JButton argh = new JButton();
        argh.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                err.dispose();
                close();
            }
        });
        err.getRootPane().add(message);
        err.pack();
        err.show();
    }

    void transferError(String s) {
        final JDialog err = new JDialog();
        JLabel message = new JLabel(s);
        JButton argh = new JButton();
        argh.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                err.dispose();
                close();
            }
        });
        err.getRootPane().add(message);
        err.pack();
        err.show();
    }
}
