package com.google.code.yawwwserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;

/**
 * Klasa jest odpowiedzialna za wczytanie całego pliku z podanej ścieżki. Nie ma
 * żadnych instancji tej klasy, istnieje tylko jedna metoda statyczna.
 *
 * @author Michał Brzeziński-Spiczak
 * @version 1.0
 *
 */
public class FileReader {

    private String header = "";

    private File file;

    private Configuration readerConf;

    public static final HashMap<String, String> TYPES = new HashMap<String, String>();

    /**
     * konstruktor FileReader analizujšcy cieżkę i wczytujšcy nagłówek dla
     * żšdanego pliku, a w razie błędów wywułujšcy odpowiedniš stronę błedu
     *
     * @param filePath -
     *                ścieżka do pliku
     * @param config -
     *                ścieżka do folderu konfiguracyjnego serwera ze stronami
     *                błędów
     * @throws IOException
     */
    public FileReader(String filePath, Configuration aConfiguration) throws IOException {
        file = new File(URLDecoder.decode(filePath, "UTF-8")).getCanonicalFile();
        readerConf = aConfiguration;
        if (file.isDirectory()) {
            File indexFile = new File(file, "index.php");
            File indexFile_1 = new File(file, "index.html");
            if (indexFile.exists() && !indexFile.isDirectory()) {
                file = indexFile;
            } else if (indexFile_1.exists() && !indexFile_1.isDirectory()) {
                file = indexFile_1;
            } else {
                if (!readerConf.getOption("showFolders").equals("Yes")) {
                    makeErrorPage(503, "Permision denied");
                } else {
                    FileOutputStream out = new FileOutputStream(readerConf.getOption("wwwPath") + "/temp/temp.php");
                    File[] files = file.listFiles();
                    makeHeader(200, -1, new Date(System.currentTimeMillis()).toString(), "text/html");
                    String title = "Index of " + file;
                    out.write(("<html><head><title>" + title + "</title></head><body><h3>Index of " + file + "</h3><p>\n").getBytes());
                    for (int i = 0; i < files.length; i++) {
                        file = files[i];
                        String filename = file.getName();
                        String description = "";
                        if (file.isDirectory()) {
                            description = "&lt;DIR&gt;";
                        }
                        out.write(("<a href=\"" + file.getPath().substring(readerConf.getOption("wwwPath").length()) + "\">" + filename + "</a> " + description + "<br>\n").getBytes());
                    }
                    out.write(("</p><hr><p>yawwwserwer</p></body><html>").getBytes());
                    file = new File(URLDecoder.decode(readerConf.getOption("wwwPath") + "/temp/temp.php", "UTF-8")).getCanonicalFile();
                }
            }
        } else if (!file.exists()) {
            makeErrorPage(404, "File Not Found.");
        } else if (getExtension() == ".exe" || getExtension().contains(".py")) {
            FileOutputStream out = new FileOutputStream(readerConf.getOption("wwwPath") + "/temp/temp.php");
            out.write((runCommand(filePath)).getBytes());
            file = new File(URLDecoder.decode(readerConf.getOption("wwwPath") + "/temp/temp.php", "UTF-8")).getCanonicalFile();
        } else {
            System.out.println(getExtension());
            makeHeader(200, file.length(), new Date(file.lastModified()).toString(), TYPES.get(getExtension()).toString());
        }
        System.out.println(file);
    }

    /**
     * metoda generuje nagłówek HTTP dla danego zapytania
     *
     * @param code -
     *                kod błędu
     * @param contentLength
     *                długość pliku wywołania
     * @param lastModified -
     *                data ostatniej modyfikacji
     * @param type -
     *                typ pliku
     */
    private void makeHeader(int code, long contentLength, String lastModified, String type) {
        header = ("HTTP/1.0 " + code + " OK\r\n" + "Date: " + new Date().toString() + "\r\n" + "Server: yawww/1.0\r\n" + "Content-Type:" + type + "\r\n" + "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n" + ((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") + "Last-modified: " + lastModified + "\r\n" + "\r\n");
    }

    /**
     * metoda generująca stronę błędu
     *
     * @param code -
     *                kod błedu
     * @param desc -
     *                opis błedu
     */
    private void makeErrorPage(int code, String desc) {
        makeHeader(code, 0, new Date().toString(), "text/html");
        try {
            file = new File(URLDecoder.decode(readerConf.getOption("errorPath") + "/temp/" + code + ".php", "UTF-8")).getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * metoda zwracająca nagłówek w postaci łańcucha tekstowego
     *
     * @return nagłówek
     */
    public String getHeader() {
        return header;
    }

    /**
     * metoda zwaracająca plik z odpowiednimi danymi do wyświetlenia
     *
     * @return plik
     */
    public File getFile() {
        return file;
    }

    /**
     * metoda wyznaczajaca rozszerzenie zadanego pliku
     *
     * @return
     */
    public String getExtension() {
        String extension = "";
        String filename = file.getName();
        int dotPos = filename.lastIndexOf(".");
        if (dotPos >= 0) {
            extension = filename.substring(dotPos);
        }
        return extension.toLowerCase();
    }

    /**
     * hashmapa przechowujaca rozszerzenia i typecontent
     */
    static {
        String image = "image/";
        TYPES.put(".gif", image + "gif");
        TYPES.put(".bmp", image + "bmp");
        TYPES.put(".jpg", image + "jpeg");
        TYPES.put(".jpeg", image + "jpeg");
        TYPES.put(".png", image + "png");
        String text = "text/";
        TYPES.put(".html", text + "html");
        TYPES.put(".htm", text + "html");
        TYPES.put(".txt", text + "plain");
    }

    /**
     * Wykonuję podaną komendę
     *
     * @param command
     *                komenda
     * @param conf
     *                aktualna konfiguracja serwera
     * @return String zawierający ekran zwrócony przez komendę
     */
    private String runCommand(String command) {
        makeHeader(200, -1, new Date(System.currentTimeMillis()).toString(), "text/html");
        if (readerConf.getOption("Scripting").equals("Yes")) {
            StringBuffer result = new StringBuffer();
            try {
                Runtime rt = Runtime.getRuntime();
                Process prcs = rt.exec(command);
                InputStreamReader isr = new InputStreamReader(prcs.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
                return "<pre>" + result.toString() + "</pre>";
            } catch (IOException ioe) {
                System.out.println(ioe);
                return "Błędne wykonanie skryptu";
            }
        } else return "Scripting is disabled on this server (sam nie wiem czemu po angielsku)";
    }
}
