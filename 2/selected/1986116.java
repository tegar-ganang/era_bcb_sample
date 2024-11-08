package jmash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 *
 * @author  Alessandro
 */
public class Bug extends javax.swing.JInternalFrame {

    /** Creates new form Bug */
    public Bug(Object classe) {
        initComponents();
        setBorder(Utils.getDefaultBorder());
        if (classe != null) txtClasse.setText(classe.getClass().toString()); else txtClasse.setText("Segnalazione generica");
        txtClasse.setVisible(false);
        txt.setRequestFocusEnabled(true);
        txt.grabFocus();
        txt.requestFocus();
        txt.setSelectionStart(0);
        txt.setSelectionEnd(txt.getText().length());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        txtClasse = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        txt = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Segnalazione bug");
        txtClasse.setEditable(false);
        getContentPane().add(txtClasse, java.awt.BorderLayout.NORTH);
        txt.setColumns(20);
        txt.setLineWrap(true);
        txt.setRows(5);
        txt.setText("Inserisci qui il testo della segnalazione");
        jScrollPane1.setViewportView(txt);
        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);
        jButton1.setText("Invia segnalazione");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1);
        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
        pack();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.txt.getText().length() == 0) return;
        String data;
        try {
            data = URLEncoder.encode("classe", "UTF-8") + "=" + URLEncoder.encode(this.txtClasse.getText(), "UTF-8");
            data += "&" + URLEncoder.encode("testo", "UTF-8") + "=" + URLEncoder.encode(this.txt.getText(), "UTF-8");
            URL url;
            String u = Main.config.getRemoteRoot() + "/bug.asp?" + data;
            if (!u.startsWith("http://")) u = "http://" + u;
            url = new URL(u);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String str = "";
            while ((line = rd.readLine()) != null) {
                str += line;
            }
            rd.close();
            new Info("Invio eseguito con successo!").startModal(this);
            doDefaultCloseAction();
        } catch (Exception ex) {
            Utils.showException(ex, "Errore in upload", this);
        }
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTextArea txt;

    private javax.swing.JTextField txtClasse;
}