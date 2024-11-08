package net.charabia.ac.update;

import net.charabia.ac.*;
import net.charabia.ac.map.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class DownloaderDialog extends JDialog {

    private Downloader m_downloader;

    private JProgressBar m_progress = new JProgressBar(0, 1000);

    private TransferListener m_transferlistener = new TransferListener();

    private File m_destination;

    private Action m_cancel = new AbstractAction("Cancel") {

        public void actionPerformed(ActionEvent e) {
            m_downloader.cancel();
            dispose();
        }
    };

    public class TransferListener implements Downloader.TransferListener {

        public void transferUpdated(double percent) {
            m_progress.setValue((int) (percent * 1000));
            m_progress.setString(String.valueOf((int) (percent * 100.0)) + "%");
        }

        public void transferComplete(File tmpFile) {
            DownloaderDialog.this.internalTransferComplete(tmpFile);
            DownloaderDialog.this.dispose();
        }

        public void transferFailed() {
            DownloaderDialog.this.transferFailed();
            DownloaderDialog.this.dispose();
        }
    }

    public DownloaderDialog(Dialog parent, Downloader downloader, File destination) {
        super(parent, true);
        init(downloader, destination, "Downloading...");
    }

    public DownloaderDialog(Frame parent, Downloader downloader, File destination, String title) {
        super(parent, true);
        init(downloader, destination, title);
    }

    private void init(Downloader downloader, File destination, String title) {
        m_destination = destination;
        getContentPane().setLayout(new BorderLayout());
        setTitle(title);
        JPanel info = new JPanel(new BorderLayout());
        info.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Progress"));
        getContentPane().add(info);
        info.add(BorderLayout.CENTER, m_progress);
        JPanel control = new JPanel();
        getContentPane().add(BorderLayout.SOUTH, control);
        control.add(new JButton(m_cancel));
        m_downloader = downloader;
        m_downloader.addTransferListener(m_transferlistener);
        m_progress.setStringPainted(true);
    }

    public void transferComplete() {
    }

    private void internalTransferComplete(File tmpfile) {
        System.out.println("transferComplete : " + tmpfile);
        try {
            File old = new File(m_destination.toString() + ".old");
            old.delete();
            File current = m_destination;
            current.renameTo(old);
            FileInputStream fis = new FileInputStream(tmpfile);
            FileOutputStream fos = new FileOutputStream(m_destination);
            BufferedInputStream in = new BufferedInputStream(fis);
            BufferedOutputStream out = new BufferedOutputStream(fos);
            for (int read = in.read(); read != -1; read = in.read()) {
                out.write(read);
            }
            out.flush();
            in.close();
            out.close();
            fis.close();
            fos.close();
            tmpfile.delete();
            setVisible(false);
            transferComplete();
        } catch (Exception exc) {
            exc.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while downloading!", "ACLocator Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void transferFailed() {
        JOptionPane.showMessageDialog(this, "Download failed!\n", "ACLocator Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) throws Exception {
        Downloader dl = new Downloader(new URL("http://www.clarkzoo.org/asheronscall/xml/places2.xml"));
    }
}
