package hoi.addrbook.data;

import hoi.addrbook.AddrBookInfo;
import hoi.addrbook.VersionCtrl;
import hoi.addrbook.util.Localization;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

public class AddrBookProps extends LinkedHashMap<String, ContactProps> {

    private static final long serialVersionUID = 2738957830618139780L;

    private static final String ADDRBOOK_DIR_PATH = System.getProperty("user.home") + File.separator + "AddrBooker";

    private static final String ADDRBOOK_FILE_PATH = ADDRBOOK_DIR_PATH + File.separator + "AddrBooker.abk";

    private static final String ADDRBOOK_FILE_PATH2 = ADDRBOOK_DIR_PATH + File.separator + "AddrBooker%s.abk";

    static {
        thisInit();
    }

    public Vector<String> getClassifys() {
        Vector<String> vector = new Vector<String>();
        for (ContactProps contact : this.values()) {
            String classify = contact.getProperty(ContactProps.CLASSIFY).trim();
            if (!vector.contains(classify)) vector.add(classify);
        }
        return vector;
    }

    private static void thisInit() {
        new File(ADDRBOOK_DIR_PATH).mkdirs();
        File file = new File(ADDRBOOK_FILE_PATH);
        if (file.exists()) {
            if (file.isDirectory()) {
                file.delete();
                save(new AddrBookProps(), ADDRBOOK_FILE_PATH);
            } else {
                copyFile(file, new File(String.format(ADDRBOOK_FILE_PATH2, new SimpleDateFormat("yyyyMMddHH").format(new Date()))));
            }
        } else {
            save(new AddrBookProps(), ADDRBOOK_FILE_PATH);
        }
    }

    private static boolean copyFile(File src, File dest) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            for (int c = fis.read(); c != -1; c = fis.read()) fos.write(c);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String quote(String str) {
        if (str == null) return "";
        str = str.replace("\r", "");
        return str.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String unquote(String str) {
        if (str == null) return "";
        String ret = "";
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\\') {
                int j = i + 1;
                if (j < str.length()) {
                    if (str.charAt(j) == '\\') {
                        ret += "\\";
                        i += 1;
                    } else if (str.charAt(j) == 'n') {
                        ret += "\n";
                        i += 1;
                    } else {
                        ret += str.charAt(i);
                    }
                } else {
                    ret += str.charAt(i);
                }
            } else {
                ret += str.charAt(i);
            }
        }
        return ret;
    }

    public static AddrBookProps load(String path) {
        BufferedReader bReader = null;
        try {
            AddrBookProps addrbook = new AddrBookProps();
            ContactProps contact = new ContactProps();
            bReader = new BufferedReader(new FileReader(path));
            int line_cnt = 0;
            for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
                line_cnt += 1;
                line = line.trim();
                if (line.startsWith("#")) {
                    ;
                } else if (line.startsWith("@")) {
                    ;
                } else if (line.startsWith("+")) {
                    String name = contact.getProperty(ContactProps.NAME, "");
                    if (addrbook.containsKey(name)) {
                        System.err.println(String.format("Line(%d), %s, ignored!!!", line_cnt, String.format("DUPLICATE KEY_NAME(%s)", name)));
                    } else {
                        addrbook.put(name, contact);
                    }
                    contact = new ContactProps();
                } else if (line.matches("^\\s*\\[" + ContactProps.KEY_REX + "\\].*?:.*$")) {
                    String[] items = line.split(":", 2);
                    String key = items[0].trim();
                    String value = items[1].trim();
                    key = key.substring(key.indexOf("[") + 1, key.indexOf("]"));
                    contact.setProperty(key, unquote(value));
                } else {
                    if (line.trim().equals("")) {
                        ;
                    } else {
                        System.err.println(String.format("Line(%d), %s, ignored!!!", line_cnt, "CAN NOT DISTINGUISH"));
                    }
                }
            }
            return addrbook;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new AddrBookProps();
        } catch (IOException e) {
            e.printStackTrace();
            return new AddrBookProps();
        } finally {
            if (bReader != null) try {
                bReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static AddrBookProps load() {
        return load(ADDRBOOK_FILE_PATH);
    }

    public static boolean save(AddrBookProps addrbook, String path) {
        BufferedWriter bWriter = null;
        try {
            bWriter = new BufferedWriter(new FileWriter(path)) {

                public void write(String str) throws IOException {
                    str += System.getProperty("line.separator");
                    this.write(str, 0, str.length());
                }
            };
            bWriter.write(String.format("@Project Home Page: %s", AddrBookInfo.HOME_WEBSITE));
            bWriter.write(String.format("@AddrBooker Version: %s", VersionCtrl.FULL_VERSION));
            bWriter.write(String.format("@Data Save Datetime: %s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
            int cnt = 0, size = addrbook.size();
            for (String _key : addrbook.keySet()) {
                cnt += 1;
                bWriter.write("");
                bWriter.write(String.format("#%d/%d", cnt, size));
                ContactProps contact = addrbook.get(_key);
                for (String key : ContactProps.KEYS) {
                    bWriter.write(String.format("[%s]%s: %s", key, Localization.getLocalString(key), quote(contact.getProperty(key))));
                }
                bWriter.write("+");
            }
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (bWriter != null) try {
                bWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean save(AddrBookProps addrbook) {
        return save(addrbook, ADDRBOOK_FILE_PATH);
    }

    public static void main(String[] args) {
        AddrBookProps.save(new AddrBookProps());
        AddrBookProps.save(new AddrBookProps());
    }
}
