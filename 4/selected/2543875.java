package code.google.jcustomize;

import java.io.*;

public class Files {

    private static final boolean ROLLBACK = false;

    public static boolean safeRename(File from, File to) {
        if (from == null || to == null) return false;
        if (!from.exists()) return true;
        if (!from.isFile()) return false;
        if (!to.exists()) return renameOrOverwrite(from, to);
        if (!to.isFile()) return false;
        if (!to.canWrite()) return false;
        if (from.getAbsolutePath().equals(to.getAbsolutePath())) return true;
        File dir = to.getParentFile();
        File newFrom = new File(dir, tempFileName(dir));
        if (!renameOrOverwrite(from, newFrom)) return false;
        File newOld = new File(dir, tempFileName(dir));
        if (!renameOrOverwrite(to, newOld)) {
            if (ROLLBACK) renameOrOverwrite(newFrom, from);
            return false;
        }
        if (!renameOrOverwrite(newFrom, to)) {
            if (ROLLBACK) {
                renameOrOverwrite(newOld, to);
                renameOrOverwrite(newFrom, from);
            }
            return false;
        }
        newOld.delete();
        return true;
    }

    private static boolean renameOrOverwrite(File from, File to) {
        if (from.renameTo(to)) return true;
        boolean created = false;
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(from));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(to));
            created = true;
            copyStream(bis, bos, 32 * 1024);
            bos.close();
            bis.close();
            from.delete();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (created) to.delete();
        return false;
    }

    private static String tempFileName(File directory) {
        try {
            File temp = File.createTempFile("sjdbckit", ".tmp", directory);
            String result = temp.getName();
            temp.delete();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        copyStream(is, os, 1);
    }

    public static void copyStream(InputStream is, OutputStream os, int chunk) throws IOException {
        if (chunk < 2) {
            for (int i = -1; (i = is.read()) >= 0; ) os.write(i);
        } else {
            byte[] buf = new byte[chunk];
            int i;
            while ((i = is.read(buf)) > 0) os.write(buf, 0, i);
        }
    }
}
