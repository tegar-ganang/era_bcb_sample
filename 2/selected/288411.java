package org.gvsig.xmlschema.utils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Clase con m�todos de utilidad en el protocolo WMS
 *
 * @authors Laura D�az, jaume dominguez faus
 */
public class DownloadUtilities {

    private static String characters;

    static boolean canceled;

    static final long latency = 500;

    /**
     * <b>key</b>: URL, <b>value</b>: path to the downloaded file.
     */
    private static Hashtable downloadedFiles;

    static Exception downloadException;

    private static final String tempDirectoryPath = System.getProperty("java.io.tmpdir") + "/tmp-andami";

    static {
        characters = "";
        for (int j = 32; j <= 127; j++) {
            characters += (char) j;
        }
        characters += "�������������������������������������������ǡ�����\n\r\f\t��";
    }

    /**
	 * Checks a File and tries to figure if the file is a text or a binary file.<br>
	 * Keep in mind that binary files are those that contains at least one
	 * non-printable character.
	 *
	 * @param file
	 * @return <b>true</b> when the file is <b>pretty problably</b> text,
	 * <b>false</b> if the file <b>is</b> binary.
	 */
    public static boolean isTextFile(File file) {
        return isTextFile(file, 1024);
    }

    /**
	 * Checks a File and tries to figure if the file is a text or a binary file.<br>
	 * Keep in mind that binary files are those that contains at least one
	 * non-printable character.
	 *
	 * @param file
	 * @param byteAmount, number of bytes to check.
	 * @return <b>true</b> when the file is <b>pretty problably</b> text,
	 * <b>false</b> if the file <b>is</b> binary.
	 */
    public static boolean isTextFile(File file, int byteAmount) {
        int umbral = byteAmount;
        try {
            FileReader fr = new FileReader(file);
            for (int i = 0; i < umbral; i++) {
                int c = fr.read();
                if (c == -1) {
                    return true;
                }
                char ch = (char) c;
                if (characters.indexOf(ch) == -1) {
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
	 * Checks a byte array and tells if it contains only text or contains
	 * any binary data.
	 *
	 * @param file
	 * @return <b>true</b> when the data is <b>only</b> text, <b>false</b> otherwise.
	 * @deprecated
	 */
    public static boolean isTextData(byte[] data) {
        char[] charData = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            charData[i] = (char) data[i];
        }
        for (int i = 0; i < data.length; i++) {
            int c = charData[i];
            if (c == -1) {
                return true;
            }
            char ch = (char) c;
            if (characters.indexOf(ch) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Copia el contenido de un InputStream en un OutputStream
	 *
	 * @param in InputStream
	 * @param out OutputStream
	 */
    public static void serializar(InputStream in, OutputStream out) {
        byte[] buffer = new byte[102400];
        int n;
        try {
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Elimina del xml la declaraci�n del DTD
	 *
	 * @param bytes bytes del fichero XML de respuesta a getCapabilities
	 * @param startTag Tag raiz del xml respuesta a getCapabilities
	 *
	 * @return bytes del fichero XML sin la declaraci�n del DTD
	 */
    public static byte[] eliminarDTD(byte[] bytes, String startTag) {
        String text = new String(bytes);
        int index1 = text.indexOf("?>") + 2;
        int index2;
        try {
            index2 = findBeginIndex(bytes, startTag);
        } catch (NoSuchObjectException e) {
            return bytes;
        }
        byte[] buffer = new byte[bytes.length - (index2 - index1)];
        System.arraycopy(bytes, 0, buffer, 0, index1);
        System.arraycopy(bytes, index2, buffer, index1, bytes.length - index2);
        return buffer;
    }

    /**
	 * Obtiene el �ndice del comienzo del xml
	 *
	 * @param bytes bytes del fichero XML en el que se busca
	 * @param tagRaiz Tag raiz del xml respuesta a getCapabilities
	 *
	 * @return �ndice donde empieza el tag raiz
	 *
	 * @throws NoSuchObjectException Si no se encuentra el tag
	 */
    private static int findBeginIndex(byte[] bytes, String tagRaiz) throws NoSuchObjectException {
        try {
            int nodo = 0;
            int ret = -1;
            int i = 0;
            while (true) {
                switch(nodo) {
                    case 0:
                        if (bytes[i] == '<') {
                            ret = i;
                            nodo = 1;
                        }
                        break;
                    case 1:
                        if (bytes[i] == ' ') {
                        } else if (bytes[i] == tagRaiz.charAt(0)) {
                            nodo = 2;
                        } else {
                            nodo = 0;
                        }
                        break;
                    case 2:
                        String aux = new String(bytes, i, 18);
                        if (aux.equalsIgnoreCase(tagRaiz.substring(1))) {
                            return ret;
                        }
                        nodo = 0;
                        break;
                }
                i++;
            }
        } catch (Exception e) {
            throw new NoSuchObjectException("No se pudo parsear el xml");
        }
    }

    /**
	 * Converts the contents of a Vector to a comma separated list
	 *
	 * */
    public static String Vector2CS(Vector v) {
        String str = new String();
        if (v != null) {
            int i;
            for (i = 0; i < v.size(); i++) {
                str = str + v.elementAt(i);
                if (i < v.size() - 1) str = str + ",";
            }
        }
        return str;
    }

    public static boolean isValidVersion(String version) {
        if (version.trim().length() == 5) {
            if ((version.charAt(1) == '.') && (version.charAt(3) == '.')) {
                char x = version.charAt(0);
                char y = version.charAt(2);
                char z = version.charAt(4);
                if ((Character.isDigit(x)) && (Character.isDigit(y)) && (Character.isDigit(z))) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
	 * Crea un fichero temporal con un nombre concreto y unos datos pasados por
	 * par�metro.
	 * @param fileName Nombre de fichero
	 * @param data datos a guardar en el fichero
	 */
    public static void createTemp(String fileName, String data) throws IOException {
        File f = new File(fileName);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        dos.writeBytes(data);
        dos.close();
        f.deleteOnExit();
    }

    /**
	 * Checks if a String is a number or not
	 *
	 * @param String, s
	 * @return boolean, true if s is a number
	 */
    public static boolean isNumber(String s) {
        try {
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
	 * Parses the String containing different items [character] separated and
	 * creates a vector with them.
	 * @param str String contains item1[c]item2[c]item3...
	 * @param c is the string value for separating the items
	 * @return Vector containing all the items
	 */
    public static Vector createVector(String str, String c) {
        StringTokenizer tokens = new StringTokenizer(str, c);
        Vector v = new Vector();
        try {
            while (tokens.hasMoreTokens()) {
                v.addElement(tokens.nextToken());
            }
            return v;
        } catch (Exception e) {
            return new Vector();
        }
    }

    /**
	 * @param dimensions
	 * @return
	 */
    public static String Vector2URLParamString(Vector v) {
        if (v == null) return "";
        String s = "";
        for (int i = 0; i < v.size(); i++) {
            s += v.get(i);
            if (i < v.size() - 1) s += "&";
        }
        return s;
    }

    /**
     * Returns the content of this URL as a file from the file system.<br>
     * <p>
     * If the URL has been already downloaded in this session and notified
     * to the system using the static <b>Utilities.addDownloadedURL(URL)</b>
     * method, it can be restored faster from the file system avoiding to
     * download it again.
     * </p>
     * @param url
     * @return File containing this URL's content or null if no file was found.
     */
    private static File getPreviousDownloadedURL(URL url) {
        File f = null;
        if (downloadedFiles != null && downloadedFiles.containsKey(url)) {
            String filePath = (String) downloadedFiles.get(url);
            f = new File(filePath);
            if (!f.exists()) return null;
        }
        return f;
    }

    /**
     * Adds an URL to the table of downloaded files for further uses. If the URL
     * already exists in the table its filePath value is updated to the new one and
     * the old file itself is removed from the file system.
     *
     * @param url
     * @param filePath
     */
    static void addDownloadedURL(URL url, String filePath) {
        if (downloadedFiles == null) downloadedFiles = new Hashtable();
        String fileName = (String) downloadedFiles.put(url, filePath);
    }

    /**
     * Downloads an URL into a temporary file that is removed the next time the
     * tempFileManager class is called, which means the next time gvSIG is launched.
     *
     * @param url
     * @param name
     * @return
     * @throws IOException
     * @throws ServerErrorResponseException
     * @throws ConnectException
     * @throws UnknownHostException
     */
    public static synchronized File downloadFile(URL url, String name) throws IOException, ConnectException, UnknownHostException {
        File f = null;
        if ((f = getPreviousDownloadedURL(url)) == null) {
            File tempDirectory = new File(tempDirectoryPath);
            if (!tempDirectory.exists()) tempDirectory.mkdir();
            f = new File(tempDirectoryPath + "/" + name + System.currentTimeMillis());
            Thread downloader = new Thread(new Downloader(url, f));
            downloader.start();
            while (!canceled && downloader.isAlive()) {
                try {
                    Thread.sleep(latency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (canceled) return null;
            downloader = null;
            if (DownloadUtilities.downloadException != null) {
                Exception e = DownloadUtilities.downloadException;
                if (e instanceof FileNotFoundException) throw (IOException) e; else if (e instanceof IOException) throw (IOException) e; else if (e instanceof ConnectException) throw (ConnectException) e; else if (e instanceof UnknownHostException) throw (UnknownHostException) e;
            }
        } else {
            System.out.println(url.toString() + " cached at '" + f.getAbsolutePath() + "'");
        }
        return f;
    }

    /**
     * Cleans every temporal file previously downloaded.
     */
    public static void cleanUpTempFiles() {
        try {
            File tempDirectory = new File(tempDirectoryPath);
            File[] files = tempDirectory.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) deleteDirectory(files[i]);
                    files[i].delete();
                }
            }
            tempDirectory.delete();
        } catch (Exception e) {
        }
    }

    /**
     * Recursive directory delete.
     * @param f
     */
    private static void deleteDirectory(File f) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) deleteDirectory(files[i]);
            files[i].delete();
        }
    }

    /**
     * Remove an URL from the system cache. The file will remain in the file
     * system for further eventual uses.
     * @param request
     */
    public static void removeURL(URL url) {
        if (downloadedFiles != null && downloadedFiles.containsKey(url)) downloadedFiles.remove(url);
    }
}

final class Monitor implements Runnable {

    public void run() {
        try {
            Thread.sleep(DownloadUtilities.latency);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DownloadUtilities.canceled = true;
    }
}

final class Downloader implements Runnable {

    private URL url;

    private File dstFile;

    public Downloader(URL url, File dstFile) {
        this.url = url;
        this.dstFile = dstFile;
        DownloadUtilities.downloadException = null;
    }

    public void run() {
        System.out.println("downloading '" + url.toString() + "' to: " + dstFile.getAbsolutePath());
        DataOutputStream dos;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dstFile)));
            byte[] buffer = new byte[1024 * 4];
            DataInputStream is = new DataInputStream(url.openStream());
            long readed = 0;
            for (int i = is.read(buffer); !DownloadUtilities.canceled && i > 0; i = is.read(buffer)) {
                dos.write(buffer, 0, i);
                readed += i;
            }
            dos.close();
            is.close();
            is = null;
            dos = null;
            if (DownloadUtilities.canceled) {
                System.err.println("[RemoteClients] '" + url + "' CANCELED.");
                dstFile.delete();
                dstFile = null;
            } else {
                DownloadUtilities.addDownloadedURL(url, dstFile.getAbsolutePath());
            }
        } catch (Exception e) {
            DownloadUtilities.downloadException = e;
        }
    }
}
