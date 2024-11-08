package edu.ucsd.ncmir.wibshare;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JApplet;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.MutableComboBoxModel;

/**
 *
 * @author spl
 */
public class WIBShare extends JApplet implements Runnable, ClipboardOwner {

    private static final String DEFAULT_SERVER = "tirebiter.ucsd.edu";

    private static final String URL_BASE = "http://%s/WebImageBrowser/cgi-bin/start.pl?";

    private static final String SEND_BASE = "http://%s/WebImageBrowser/cgi-bin/send.cgi";

    private static final String DIRPATH_BASE = "http://%s/cgi-bin/diragent?dirpath=";

    public static String _urlbase;

    public static String _sendbase;

    public static String _dirpathbase;

    private MutableComboBoxModel _model;

    @Override
    public void init() {
        try {
            String server = this.getParameter("server");
            if (server == null) server = WIBShare.DEFAULT_SERVER;
            WIBShare._urlbase = String.format(WIBShare.URL_BASE, server);
            WIBShare._sendbase = String.format(WIBShare.SEND_BASE, server);
            WIBShare._dirpathbase = String.format(WIBShare.DIRPATH_BASE, server);
            java.awt.EventQueue.invokeAndWait(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        this._model = new DefaultComboBoxModel();
        this.initComponents();
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        pane = new javax.swing.JLayeredPane();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        search = new javax.swing.JButton();
        send = new javax.swing.JButton();
        view = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        copy_url = new javax.swing.JButton();
        path = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        delete_email = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        email_list = new javax.swing.JComboBox();
        setStub(null);
        getContentPane().setLayout(new java.awt.GridLayout(1, 1));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/ucsd/ncmir/wibshare/images/wibshare.png")));
        jLabel1.setLabelFor(this);
        jLabel1.setBounds(0, 0, 640, 430);
        pane.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jPanel1.setOpaque(false);
        jPanel1.setLayout(null);
        jPanel1.setBounds(20, 130, 600, 30);
        pane.add(jPanel1, javax.swing.JLayeredPane.PALETTE_LAYER);
        jPanel2.setOpaque(false);
        jPanel2.setLayout(null);
        search.setText("Search");
        search.setFocusPainted(false);
        search.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                search(evt);
            }
        });
        jPanel2.add(search);
        search.setBounds(520, 0, 100, 30);
        send.setText("Send");
        send.setEnabled(false);
        send.setFocusPainted(false);
        send.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                send(evt);
            }
        });
        jPanel2.add(send);
        send.setBounds(0, 0, 100, 30);
        view.setText("View");
        view.setEnabled(false);
        view.setFocusPainted(false);
        view.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                view(evt);
            }
        });
        jPanel2.add(view);
        view.setBounds(250, 0, 100, 30);
        jPanel2.setBounds(10, 10, 620, 30);
        pane.add(jPanel2, javax.swing.JLayeredPane.PALETTE_LAYER);
        jPanel3.setOpaque(false);
        jPanel3.setLayout(null);
        copy_url.setText("Copy URL");
        copy_url.setEnabled(false);
        copy_url.setFocusPainted(false);
        copy_url.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyToClipboard(evt);
            }
        });
        jPanel3.add(copy_url);
        copy_url.setBounds(520, 0, 100, 30);
        path.setOpaque(true);
        jPanel3.add(path);
        path.setBounds(0, 0, 510, 30);
        jPanel3.setBounds(10, 50, 620, 30);
        pane.add(jPanel3, javax.swing.JLayeredPane.PALETTE_LAYER);
        jPanel4.setOpaque(false);
        jPanel4.setLayout(null);
        delete_email.setText("Del Email");
        delete_email.setEnabled(false);
        delete_email.setFocusPainted(false);
        delete_email.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteEmail(evt);
            }
        });
        jPanel4.add(delete_email);
        delete_email.setBounds(520, 0, 100, 30);
        jButton4.setText("Add Email");
        jButton4.setFocusPainted(false);
        jButton4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEmail(evt);
            }
        });
        jPanel4.add(jButton4);
        jButton4.setBounds(0, 0, 100, 30);
        email_list.setModel(this._model);
        jPanel4.add(email_list);
        email_list.setBounds(110, 0, 400, 30);
        jPanel4.setBounds(10, 90, 620, 30);
        pane.add(jPanel4, javax.swing.JLayeredPane.PALETTE_LAYER);
        getContentPane().add(pane);
    }

    private WIBFile _selected_file = null;

    private void search(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser(new WIBDirectory("/"), new WIBFileSystemView());
        chooser.setFileFilter(new WIBFilter());
        chooser.setFileView(new WIBFileView());
        chooser.setDialogTitle("WIBSharable Files");
        chooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setApproveButtonText("Select");
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) try {
            this._selected_file = (WIBFile) chooser.getSelectedFile();
            this.path.setText(this._selected_file.getCanonicalPath());
            this.copy_url.setEnabled(true);
            this.view.setEnabled(true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void copyToClipboard(java.awt.event.ActionEvent evt) {
        String url = this.getURL();
        if (url != null) {
            Transferable transferable = new StringSelection(url);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, this);
        }
    }

    private String getURL() {
        String url = null;
        String ident = null;
        String type = this._selected_file.getType();
        if (type.equals("ZFY")) ident = "imagePath"; else if (type.equals("PPM")) ident = "flatID"; else if (type.equals("IMOD")) ident = "volumeID"; else if (type.equals("A75")) ident = "volumeID"; else if (type.equals("PNZ")) ident = "pnzID"; else if (type.equals("TIFF")) ident = "flatID"; else if (type.equals("PFF")) ident = "pffID";
        if (ident != null) url = WIBShare._urlbase + ident + "=file:" + this._selected_file.toString();
        return url;
    }

    private static final Pattern VALIDATE = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$");

    private void addEmail(java.awt.event.ActionEvent evt) {
        String email = JOptionPane.showInputDialog(this, "Enter new email address.", "WIBShare Add Email", JOptionPane.PLAIN_MESSAGE);
        if ((email != null) && VALIDATE.matcher(email.toUpperCase()).matches()) {
            String[] list = new String[this._model.getSize() + 1];
            for (int i = 0; i < this._model.getSize(); i++) list[i] = (String) this._model.getElementAt(i);
            list[this._model.getSize()] = email;
            for (String address : list) this._model.removeElement(address);
            Arrays.sort(list);
            for (String address : list) this._model.addElement(address);
            this.email_list.setSelectedItem(email);
            this.delete_email.setEnabled(true);
            this.send.setEnabled(true);
        } else if (email != null) JOptionPane.showMessageDialog(this, "<html><center>" + "Sorry, but<br>" + "\"" + email + "\"<br>" + "doesn't seem to be a valid " + "email address." + "</center></html>", "WIBShare Email Address Error", JOptionPane.ERROR_MESSAGE);
    }

    private void deleteEmail(java.awt.event.ActionEvent evt) {
        this._model.removeElement(this.email_list.getSelectedItem());
        if (this._model.getSize() == 0) this.delete_email.setEnabled(false); else this.send.setEnabled(false);
    }

    private void send(java.awt.event.ActionEvent evt) {
        String url = this.getURL();
        if (url != null) {
            String tinyurl = "";
            try {
                URLConnection conn = new URL("http://tinyurl.com/api-create.php?url=" + url).openConnection();
                conn.setDoInput(true);
                conn.connect();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                tinyurl = br.readLine();
            } catch (SocketTimeoutException ste) {
                ste.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            String addresses = (String) this._model.getElementAt(0);
            for (int i = 1; i < this._model.getSize(); i++) addresses += "," + (String) this._model.getElementAt(i);
            SendDialog dialog = new SendDialog("Web Image Browser view", "To view, click the URL below\n\n" + url + "\n\n" + ((tinyurl != null) ? ("or\n\n" + tinyurl) : ""), addresses);
            dialog.setVisible(true);
        } else JOptionPane.showMessageDialog(this, "<html><center>" + "Sorry, but the file in<br>" + "\"" + this.path.getText() + "\"<br>" + "doesn't seem to be a valid " + "WIBShare-able file." + "</center></html>", "WIBShare Error", JOptionPane.ERROR_MESSAGE);
    }

    private void view(java.awt.event.ActionEvent evt) {
        new BrowserThread(this.getURL()).start();
    }

    private class BrowserThread extends Thread {

        private String _url;

        BrowserThread(String url) {
            super();
            this._url = url;
        }

        @Override
        public void run() {
            WebBrowserLauncher.openURL(this._url);
        }
    }

    private javax.swing.JButton copy_url;

    private javax.swing.JButton delete_email;

    private javax.swing.JComboBox email_list;

    private javax.swing.JButton jButton4;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JLayeredPane pane;

    private javax.swing.JLabel path;

    private javax.swing.JButton search;

    private javax.swing.JButton send;

    private javax.swing.JButton view;

    public static void main(String[] argv) {
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
