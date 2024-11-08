package net.sourceforge.pinemup.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.JOptionPane;
import net.sourceforge.pinemup.core.I18N;
import net.sourceforge.pinemup.core.UserSettings;

class WebdavConnection extends ServerConnection {

    private String protocol;

    public WebdavConnection(boolean sslEnabled) {
        if (sslEnabled) {
            protocol = "https";
        } else {
            protocol = "http";
        }
    }

    public WebdavConnection() {
        protocol = "http";
    }

    private void setDefaultAuthenticator() {
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(UserSettings.getInstance().getServerUser(), UserSettings.getInstance().getServerPasswdString().toCharArray());
            }
        });
    }

    public void importNotesFromServer() {
        boolean downloaded = true;
        try {
            makeBackupFile();
            File f = new File(UserSettings.getInstance().getNotesFile());
            FileOutputStream fos = new FileOutputStream(f);
            String urlString = protocol + "://" + UserSettings.getInstance().getServerAddress() + UserSettings.getInstance().getServerDir() + f.getName();
            setDefaultAuthenticator();
            URL url = new URL(urlString);
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            InputStream is = urlc.getInputStream();
            int nextByte = is.read();
            while (nextByte != -1) {
                fos.write(nextByte);
                nextByte = is.read();
            }
            fos.close();
            if (urlc.getResponseCode() != HttpURLConnection.HTTP_OK) {
                downloaded = false;
            }
        } catch (SSLHandshakeException e) {
            JOptionPane.showMessageDialog(null, I18N.getInstance().getString("error.sslcertificateerror"), I18N.getInstance().getString("error.title"), JOptionPane.ERROR_MESSAGE);
            downloaded = false;
        } catch (Exception e) {
            downloaded = false;
        }
        if (downloaded) {
            deleteBackupFile();
            JOptionPane.showMessageDialog(null, I18N.getInstance().getString("info.notesfiledownloaded"), I18N.getInstance().getString("info.title"), JOptionPane.INFORMATION_MESSAGE);
        } else {
            restoreFileFromBackup();
            JOptionPane.showMessageDialog(null, I18N.getInstance().getString("error.notesfilenotdownloaded"), I18N.getInstance().getString("error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportNotesToServer() {
        boolean uploaded = true;
        try {
            File f = new File(UserSettings.getInstance().getNotesFile());
            FileInputStream fis = new FileInputStream(f);
            String urlString = protocol + "://" + UserSettings.getInstance().getServerAddress() + UserSettings.getInstance().getServerDir() + f.getName();
            setDefaultAuthenticator();
            URL url = new URL(urlString);
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setDoOutput(true);
            urlc.setRequestMethod("PUT");
            OutputStream os = urlc.getOutputStream();
            int nextByte = fis.read();
            while (nextByte != -1) {
                os.write(nextByte);
                nextByte = fis.read();
            }
            fis.close();
            os.close();
            if (urlc.getResponseCode() != HttpURLConnection.HTTP_CREATED && urlc.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                uploaded = false;
            }
        } catch (SSLHandshakeException e) {
            JOptionPane.showMessageDialog(null, I18N.getInstance().getString("error.sslcertificateerror"), I18N.getInstance().getString("error.title"), JOptionPane.ERROR_MESSAGE);
            uploaded = false;
        } catch (Exception e) {
            uploaded = false;
        }
        if (uploaded) {
            JOptionPane.showMessageDialog(null, I18N.getInstance().getString("info.notesfileuploaded"), I18N.getInstance().getString("info.title"), JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, I18N.getInstance().getString("error.notesfilenotuploaded"), I18N.getInstance().getString("error.title"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
