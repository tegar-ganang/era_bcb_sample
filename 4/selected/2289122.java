package org.nl.applet.print;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.nl.printer.ImprimePDF;

/**
 *
 * @author  usuario
 */
public class PrintReportes extends javax.swing.JApplet {

    /**
   * Initializes the applet PrintReport
   */
    public void init() {
        try {
            this.urlService = getParameter("urlService");
            java.awt.EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    initComponents();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton1.setText("print");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        comunicacionServer();
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JPanel jPanel1;

    private String urlService;

    private void comunicacionServer() {
        int in = 0;
        long tamFile = 0;
        URL pagina = this.getCodeBase();
        String protocolo = pagina.getProtocol();
        String servidor = pagina.getHost();
        int puerto = pagina.getPort();
        String servlet = "";
        if (this.urlService != null && !this.urlService.equals("")) servlet = this.urlService; else servlet = "/PruebaApplet/ServletEnviaReportes";
        URL direccion = null;
        URLConnection conexion = null;
        try {
            direccion = new URL(protocolo, servidor, puerto, servlet);
            conexion = direccion.openConnection();
            System.out.println(" Url del urlService " + direccion.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        conexion.setUseCaches(false);
        conexion.setRequestProperty("Accept-Language", "es");
        BufferedInputStream buffer = null;
        try {
            DataInputStream dataIn = new DataInputStream(conexion.getInputStream());
            buffer = new BufferedInputStream(conexion.getInputStream());
            int numArchivosRecibir = dataIn.readInt();
            System.out.println(" Numero de archivos a recibir " + numArchivosRecibir);
            String nameFiles[] = new String[numArchivosRecibir];
            File f = null;
            File files[] = new File[numArchivosRecibir];
            FileOutputStream ouputStream = null;
            for (int k = 0; k < numArchivosRecibir; k++) {
                f = this.fileTemp(dataIn.readUTF(), null);
                tamFile = dataIn.readLong();
                nameFiles[k] = f.getAbsolutePath();
                files[k] = f;
                System.out.println(" file no. " + k + " " + nameFiles[k] + " tama�o recibido " + tamFile);
                ouputStream = new FileOutputStream(f);
                for (int j = 0; j < tamFile; j++) {
                    ouputStream.write(buffer.read());
                }
                ouputStream.close();
                System.out.println(" Tama�o del archivo " + f.getName() + " : " + f.length());
            }
            dataIn.close();
            buffer.close();
            dataIn = null;
            buffer = null;
            ouputStream = null;
            ImprimePDF printPdf = new ImprimePDF();
            printPdf.imprimirPdfs(nameFiles);
            for (File fi : files) {
                fi.delete();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private File fileTemp(String name, String suffix) {
        try {
            return File.createTempFile(name, suffix);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
