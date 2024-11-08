package com.snakoid.droidlibrary.classes;

import java.io.*;
import java.net.*;
import java.util.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.*;
import org.xmlpull.v1.XmlSerializer;

public class Tools {

    public static ArrayList<Book> library = new ArrayList<Book>();

    public static ArrayList<Book> booksTemp = new ArrayList<Book>();

    public static String folderPath = "/mnt/sdcard/droidlibrary/";

    public static String configFile = folderPath + "library.xml";

    public static String configFileBackup = folderPath + "library.xml.backup";

    public static String folderThumbsPath = folderPath + "thumbs/";

    private static String requestUrl = "http://snakoid-dev.dyndns.org/api/aws/q-";

    public static boolean libraryFirstShow = true;

    public static String UpFirstLetter(String _txt) {
        String out = "";
        out += _txt.charAt(0);
        out = out.toUpperCase();
        out += _txt.substring(1, _txt.length());
        return out;
    }

    public static boolean CheckConfigFile() {
        CheckAppFolder();
        if (!new File(configFile).exists()) {
            try {
                FileOutputStream fos = new FileOutputStream(Tools.configFile, false);
                XmlSerializer serializer = Xml.newSerializer();
                serializer.setOutput(fos, "utf-8");
                serializer.startDocument("utf-8", Boolean.valueOf(true));
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                serializer.startTag(null, "droidlibrary");
                serializer.endTag(null, "droidlibrary");
                serializer.endDocument();
                serializer.flush();
                fos.close();
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public static boolean CheckAppFolder() {
        if (new File(folderPath).exists() && new File(folderPath).isDirectory()) return true; else {
            new File(folderPath).mkdirs();
            new File(folderThumbsPath).mkdirs();
            return true;
        }
    }

    public static String DownloadFile(String _url, String _out_path) {
        CheckAppFolder();
        String file_name = new File(_url).getName();
        String out_file = _out_path + "/" + file_name;
        if (new File(out_file).exists()) return out_file;
        try {
            if (!new File(_out_path + "/" + file_name).isFile()) {
                URL myFileUrl = new URL(_url);
                HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
                conn.setDoInput(true);
                conn.connect();
                int length = conn.getContentLength();
                byte[] buffer = new byte[length];
                InputStream is = conn.getInputStream();
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out_file, false));
                int readed = 0;
                int current_read = 0;
                while (readed < length) {
                    current_read = is.read(buffer);
                    readed += current_read;
                    bos.write(buffer, 0, current_read);
                }
                is.close();
                bos.close();
                conn.disconnect();
            }
        } catch (IOException e) {
            Log.e("ouned", "Could not write file " + e.getMessage());
            out_file = "";
        }
        return out_file;
    }

    public static String GetRequestUrl(String _code) {
        return requestUrl + _code;
    }

    public static Book GetBookByISBN(String _isbn) {
        for (Book _b : library) {
            if (_b.getISBN().equals(_isbn)) {
                return _b;
            }
        }
        return null;
    }

    public static void AddBook2Library(Book _book) {
        library.add(_book);
        Collections.sort(library);
    }

    public static void AddBooks2Temp(ArrayList<Book> _books) {
        for (Book booknew : _books) {
            booksTemp.add(booknew);
        }
    }

    public static void CreateBackupOfDataFile(String _src, String _dest) {
        try {
            File src = new File(_src);
            File dest = new File(_dest);
            if (new File(_src).exists()) {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
                byte[] read = new byte[128];
                int len = 128;
                while ((len = in.read(read)) > 0) out.write(read, 0, len);
                out.flush();
                out.close();
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean CheckBarcodeScannerIsInstalled(PackageManager _pm) {
        boolean installed = false;
        try {
            PackageManager pm = _pm;
            PackageInfo info = pm.getPackageInfo("com.google.zxing.client.android", 0);
            if (info != null) installed = true;
        } catch (NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }
}
