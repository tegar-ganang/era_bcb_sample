package net.sourceforge.oradoc.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class FileUtils {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(FileUtils.class);

    private FileUtils() {
    }

    public static String readFile(File f) throws IOException {
        return readFile(new FileInputStream(f));
    }

    public static String readFile(InputStream is) throws IOException {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                }
                is = null;
            }
        }
    }

    /**
	 * Speichert einen Stream in einen File
	 * 
	 * @param filename
	 *          , Name (mit Pfad) unter welchem die Datei angelegt werden soll
	 * @param in
	 * @param targetEncoding
	 * @throws IOException
	 */
    public static void saveFile(String filename, Reader in, String targetEncoding) throws IOException {
        OutputStreamWriter out = null;
        try {
            File file = new File(filename);
            if (targetEncoding == null) {
                out = new OutputStreamWriter(new FileOutputStream(file));
            } else {
                out = new OutputStreamWriter(new FileOutputStream(file), targetEncoding);
            }
            char[] buffer = new char[1024];
            int count = 0;
            while ((count = in.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
            out.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.error(ex);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    logger.error(ex);
                }
            }
        }
    }

    public static File findFile(File path, String filename) {
        return findFile(path, filename, new String[] { ".svn" });
    }

    public static File findFile(File path, String filename, String[] filters) {
        if (path == null || !path.exists()) {
            return null;
        }
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                File currentfile = files[i];
                if (isFiltered(currentfile.getName(), filters)) {
                    continue;
                }
                if (currentfile.isDirectory()) {
                    File result = findFile(currentfile, filename);
                    if (result != null) {
                        return result;
                    }
                } else {
                    String currentName = currentfile.getName();
                    if (currentName.equalsIgnoreCase(filename)) {
                        return currentfile;
                    }
                }
            }
        } else {
            String currentName = path.getName();
            if (currentName.equalsIgnoreCase(filename)) {
                return path;
            }
        }
        return null;
    }

    private static boolean isFiltered(String name, String[] filters) {
        for (int i = 0; i < filters.length; i++) {
            if (name.startsWith(filters[i])) {
                return true;
            }
        }
        return false;
    }

    public static String extractFilenameWithExt(String path) {
        int index = path.lastIndexOf('\\');
        if (index == -1) index = path.lastIndexOf('/');
        if (index == -1) return path; else return path.substring(index + 1, path.length());
    }

    public static String extractPath(String filename) {
        int index = filename.lastIndexOf('\\');
        if (index == -1) index = filename.lastIndexOf('/');
        if (index == -1) return filename; else return filename.substring(0, index + 1);
    }

    public static String extractExtension(String filename) {
        if (filename == null) return null;
        int index = filename.lastIndexOf(".");
        if (index != -1) return filename.substring(index); else return null;
    }

    public static String extractFilenameWithoutExt(String filename) {
        if (filename == null) return null;
        int index = filename.lastIndexOf('\\');
        if (index == -1) {
            index = filename.lastIndexOf('/');
        }
        if (index != -1) {
            filename = filename.substring(index + 1, filename.length());
        }
        index = filename.lastIndexOf(".");
        if (index != -1) return filename.substring(0, index); else return null;
    }

    public static void deepCopy(String filename, String destination, boolean includeHidden) throws IOException {
        if (filename == null || filename.length() == 0) {
            throw new IllegalArgumentException("deepCopy: filename is null or empty!");
        }
        if (destination == null || destination.length() == 0) {
            throw new IllegalArgumentException("deepCopy: destination is null or empty!");
        }
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filename);
        }
        if (!file.canRead()) {
            throw new IOException("Can't read file: " + filename);
        }
        if (file.isHidden() && !includeHidden) {
            return;
        }
        if (!destination.endsWith(File.separator)) {
            destination = destination + File.separator;
        }
        File destDir = new File(destination);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        if (file.isDirectory()) {
            File newDestDir = new File(destination + file.getName());
            newDestDir.mkdirs();
            File[] files = file.listFiles();
            Iterator<File> it = Arrays.asList(files).iterator();
            while (it.hasNext()) {
                File subFile = it.next();
                String subFilename = subFile.getAbsolutePath();
                FileUtils.deepCopy(subFilename, newDestDir.getAbsolutePath(), includeHidden);
            }
        } else {
            if (!file.isHidden() || (file.isHidden() && includeHidden)) {
                FileUtils.copy(file.getAbsolutePath(), destination + file.getName());
            }
        }
    }

    public static void copy(String srcFilename, String destFilename) throws IOException {
        int bytes_read = 0;
        byte[] buffer = new byte[512];
        FileInputStream fin = null;
        FileOutputStream fout = null;
        try {
            fin = new FileInputStream(srcFilename);
            try {
                fout = new FileOutputStream(destFilename);
                while ((bytes_read = fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, bytes_read);
                }
            } finally {
                try {
                    if (fout != null) {
                        fout.close();
                        fout = null;
                    }
                } catch (IOException e) {
                }
            }
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                    fin = null;
                }
            } catch (IOException e) {
            }
        }
    }
}
