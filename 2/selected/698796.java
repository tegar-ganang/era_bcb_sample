package Core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JProgressBar;

/**
 *
 * @author H3R3T1C
 */
public class Downloader extends javax.swing.JFrame {

    BufferedOutputStream fOut = null;

    InputStream is = null;

    public Downloader(final File file, String name, final String url) {
        initComponents();
        jLabel1.setText(name);
        new Thread(new Runnable() {

            public void run() {
                download(file, url);
            }
        }).start();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jButton1 = new javax.swing.JButton();
        bar = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Downloader");
        setResizable(false);
        jButton1.setText("Cancel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        bar.setStringPainted(true);
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("file name");
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("%");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE).addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING).addComponent(bar, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE).addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(bar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton1).addContainerGap()));
        pack();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            is.close();
            fOut.close();
            this.dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
    * @param args the command line arguments
    */
    public void download(File file, String uri) {
        URL url = null;
        String bts = "";
        try {
            file.createNewFile();
            url = new URL(uri);
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            bar.setMaximum(conn.getContentLength());
            bts = "" + conn.getContentLength();
            fOut = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[32 * 1024];
            int bytesRead = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                bar.setValue(bar.getValue() + bytesRead);
                jLabel2.setText(bar.getValue() + " bytes of " + bts + " bytes");
                fOut.write(buffer, 0, bytesRead);
            }
            is.close();
            fOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private javax.swing.JProgressBar bar;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;
}