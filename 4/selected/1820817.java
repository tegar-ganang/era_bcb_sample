package hoi.birthdaymgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

public class BMgrIO {

    private static final String fname = "data.bm";

    private static final ReentrantLock lock = new ReentrantLock();

    private static String escape(String text) {
        text = text.replace("\\", "\\\\");
        text = text.replace("\b", "\\b");
        text = text.replace("\0", "\\0");
        text = text.replace("\t", "\\t");
        text = text.replace("\n", "\\n");
        text = text.replace("\r", "\\r");
        text = text.replace("|", "\\|");
        return text;
    }

    private static String escapeArray(String... texts) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < texts.length; i++) sb.append(escape(texts[i]) + "|");
        return sb.toString();
    }

    private static String[] unescapeArray(String str) {
        Vector<String> vector = new Vector<String>();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i != str.length() - 1) {
                char d = str.charAt(i + 1);
                switch(d) {
                    case 'b':
                        sb.append('\b');
                        break;
                    case '0':
                        sb.append('\0');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case '|':
                        sb.append('|');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    default:
                        sb.append(("" + c) + d);
                }
                i++;
            } else {
                if (c == '|') {
                    vector.addElement(sb.toString());
                    sb = new StringBuffer();
                } else sb.append(c);
            }
        }
        String[] texts = new String[vector.size()];
        vector.copyInto(texts);
        return texts;
    }

    public static void load(Vector<BMgrRecord> dataVector) {
        lock.lock();
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new InputStreamReader(new FileInputStream(fname), "UTF-8"));
            for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
                if (line != null && !line.trim().equals("") && !line.trim().startsWith("#")) try {
                    BMgrRecord record = new BMgrRecord();
                    record.setContents(unescapeArray(line.trim()));
                    dataVector.add(record);
                } catch (Exception e) {
                    System.err.println(line);
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bReader != null) try {
                bReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lock.unlock();
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

    private static final String BMGR_DIR_PATH = System.getProperty("user.home") + File.separator + "BirthdayMgr";

    private static final String BMGR_FILE_PATTERN = BMGR_DIR_PATH + File.separator + "BirthdayMgr%s.bak";

    private static final String BMGR_FILE_FORMAT = "yyyyMMddHHmmss";

    private static final String BMGR_FILE_REGEX = "BirthdayMgr[0-9]+\\.bak";

    private static void tryBackupData() {
        try {
            File src = new File(fname);
            File dest = new File(String.format(BMGR_FILE_PATTERN, new SimpleDateFormat(BMGR_FILE_FORMAT).format(new Date())));
            if (src.exists() && src.isFile()) copyFile(src, dest);
        } catch (Exception ignore) {
        }
    }

    static {
        try {
            File dir = new File(BMGR_DIR_PATH);
            dir.mkdirs();
            for (File file : dir.listFiles()) {
                if (file.isFile() && file.getName().matches(BMGR_FILE_REGEX)) file.delete();
            }
        } catch (Exception ignore) {
        }
    }

    public static void save(Vector<BMgrRecord> dataVector) {
        BufferedWriter bWriter = null;
        lock.lock();
        try {
            tryBackupData();
            bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fname), "UTF-8"));
            bWriter.write("#一行一条记录, 请不要手动更改(One Line One Record, Please DO NOT Change Manually)!!!");
            bWriter.write(System.getProperty("line.separator"));
            for (BMgrRecord record : dataVector) {
                try {
                    bWriter.write(escapeArray(record.getContents()).trim());
                    bWriter.write(System.getProperty("line.separator"));
                } catch (Exception e) {
                    System.err.println(record);
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bWriter != null) try {
                bWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lock.unlock();
        }
    }
}
