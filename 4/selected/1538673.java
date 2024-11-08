package corny.FritzPhoneBook.ftp;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import sun.net.TelnetInputStream;
import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpLoginException;
import corny.Utils.gui.PasswordDialog;
import corny.Utils.gui.ProgressBarDialog.ProgressListener;
import corny.Utils.keychain.Keychain;

public class FTPHandler {

    private static final String KEYCHAIN_ITEM_NAME = "Fritz!Box FTP Password";

    private static final String KEYCHAIN_USER_NAME = "ftpuser";

    private static FtpClient client;

    private static char[] password;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            public void run() {
                disconnect();
            }
        }));
    }

    private static char[] getPassword(boolean forceReEnter) {
        if (!forceReEnter && Keychain.isPasswordSet(KEYCHAIN_ITEM_NAME, KEYCHAIN_USER_NAME)) {
            password = Keychain.getPassword(KEYCHAIN_ITEM_NAME, KEYCHAIN_USER_NAME);
        }
        if (password == null || forceReEnter) {
            PasswordDialog dialog = new PasswordDialog(null, "Fritz!Box Passwort", "Fritz!Box Passwort eingeben:", false);
            dialog.setVisible(true);
            if (!dialog.wasCanceled()) {
                password = dialog.getPassword();
                if (Keychain.isPasswordSet(KEYCHAIN_ITEM_NAME, KEYCHAIN_USER_NAME)) {
                    Keychain.changePassword(KEYCHAIN_ITEM_NAME, KEYCHAIN_USER_NAME, password);
                } else {
                    Keychain.addPassword(KEYCHAIN_ITEM_NAME, KEYCHAIN_USER_NAME, password);
                }
            }
        }
        return password;
    }

    /**
	 * Öffnet den Server
	 * 
	 * @throws IOException
	 */
    private static synchronized boolean connect() {
        if (client == null) {
            client = new FtpClient();
        }
        try {
            if (!client.serverIsOpen()) {
                client.openServer("fritz.box");
                if (!login(false)) {
                    client.closeServer();
                    return false;
                }
                client.binary();
            }
            client.cd("/");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static boolean login(boolean forceEnter) throws IOException {
        try {
            client.login(KEYCHAIN_USER_NAME, new String(getPassword(forceEnter)));
        } catch (NullPointerException e) {
            return false;
        } catch (FtpLoginException e) {
            password = null;
            return login(true);
        }
        return true;
    }

    /**
	 * Schließt dern Server
	 * 
	 * @throws IOException
	 */
    private static synchronized void disconnect() {
        if (client.serverIsOpen()) {
            try {
                client.closeServer();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Lädt eine Datei in den Ornder "Bilder" der Fritz!Box hoch. Die Fritz!Box
	 * muss unter fritz.box erreichbar sein. Ist eine Datei mit dem Namen
	 * fileName schon vorhanden, so wird ein Laufindex an den Namen angehängt.
	 * Diese Methode öffnet den Server automatisch, schließt ihn aber nicht.
	 * 
	 * @param in
	 *            InputStream, aus dem die Daten gelesen werden
	 * @param fileName
	 *            Vorgeschlagener Dateiname (wenn keine Erweiterung angegeben
	 *            wird, wird ".jpg" angehängt
	 * @param length
	 *            Länge der Eingabedaten
	 * @param listener
	 *            ProgressListener
	 * @return URL der hochgeladenen Datei
	 * @throws IOException
	 *             Kann verschiedene Ursachen haben
	 */
    public static synchronized String uploadPicture(InputStream in, String fileName, long length, final ProgressListener listener) throws IOException {
        if (!fileNameHasExtension(fileName)) {
            fileName = fileName + ".png";
        }
        String nameWithoutExt, extension;
        int num = 0;
        int dotIndex = fileName.lastIndexOf('.');
        nameWithoutExt = fileName.substring(0, dotIndex);
        extension = fileName.substring(dotIndex);
        String completeName = nameWithoutExt + extension;
        while (existsFile(client, completeName)) {
            completeName = nameWithoutExt + (++num) + extension;
        }
        if (!connect()) {
            return null;
        }
        client.cd("Bilder");
        if (listener != null) {
            listener.progressStarted(100);
        }
        final double incrementStep = (double) length / 409600;
        OutputStream out = client.put(completeName);
        byte c[] = new byte[4096];
        int read;
        long loopCounter = 0;
        double progress = incrementStep;
        while ((read = in.read(c)) != -1) {
            out.write(c, 0, read);
            if (++loopCounter > progress) {
                progress += incrementStep;
                if (listener != null) {
                    listener.progressIncrease();
                }
            }
        }
        in.close();
        out.close();
        if (listener != null) {
            listener.progressEnded();
        }
        return "file:///var/InternerSpeicher/Bilder/" + completeName;
    }

    private static boolean fileNameHasExtension(String fileName) {
        String fileNameLowerCase = fileName.toLowerCase();
        return fileNameLowerCase.endsWith(".jpg") || fileNameLowerCase.endsWith(".jpeg") || fileNameLowerCase.endsWith(".png");
    }

    /**
	 * Lädt die angegebene Datei von der Fritz!Box und versucht, sie in ein Bild
	 * umzuwandeln. Diese Methode öffnet den Server automatisch und schließt in
	 * auch.
	 * 
	 * @param path
	 *            Pfad zur Bilddatei
	 * @return Bild
	 * @throws IOException
	 *             kann diverse Ursachen haben.
	 */
    public static synchronized BufferedImage getImage(String path) throws IOException {
        if (!connect()) {
            return null;
        }
        if (path.startsWith("file:///var/InternerSpeicher/")) {
            path = path.substring("file:///var/InternerSpeicher/".length());
        }
        while (path.contains("/")) {
            client.cd(path.substring(0, path.indexOf("/")));
            path = path.substring(path.indexOf("/") + 1);
        }
        TelnetInputStream in = client.get(path);
        BufferedImage ret = ImageIO.read(in);
        client.closeServer();
        return ret;
    }

    public static synchronized List<String> getImageFilesNames() throws IOException {
        if (!connect()) {
            return new ArrayList<String>(0);
        }
        client.cd("Bilder");
        return getContent(client.nameList("*"));
    }

    private static synchronized boolean existsFile(FtpClient client, String filename) throws IOException {
        try {
            client.nameList(filename);
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    private static synchronized List<String> getContent(TelnetInputStream in) throws IOException {
        List<String> ret = new LinkedList<String>();
        int c = 0;
        StringBuffer line = new StringBuffer();
        while ((c = in.read()) != -1) {
            if (c == 10) {
                ret.add(line.toString());
                line.delete(0, line.length());
            } else {
                line.append((char) c);
            }
        }
        return ret;
    }
}
