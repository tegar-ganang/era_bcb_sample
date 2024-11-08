package src.utilities;

import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.util.regex.*;

/**
 * Various input/output utilities.
 */
public class IOUtils {

    /** Trims {@code (/tmp/a/file.txt, /tmp)} to {@code a/file.txt} */
    public static final String trimPath(String path, String parent) {
        if (path.startsWith(parent)) {
            path = path.substring(parent.length());
            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }
        }
        return path;
    }

    public static final boolean copyWithRsync(File src, File dst, String outPrefix) {
        boolean worked = false;
        File resourcesFile = new File(src.getAbsolutePath() + File.separator + "resources.txt");
        if (resourcesFile.exists() && resourcesFile.isFile()) {
            System.out.printf("Reading files to copy from resources file (%s) ...\n", resourcesFile.getAbsolutePath());
            try {
                dst.mkdirs();
                String cmd = String.format("rsync -uav %s %s --include-from=%s", src.getAbsolutePath() + File.separator, dst.getAbsolutePath() + File.separator, resourcesFile.getAbsolutePath());
                System.out.println("Copying files: " + cmd);
                Process rsync = Runtime.getRuntime().exec(cmd);
                InputStreamRunner stdoutReader = new InputStreamRunner(rsync.getInputStream(), System.out, outPrefix);
                InputStreamRunner stderrReader = new InputStreamRunner(rsync.getErrorStream(), System.err, outPrefix);
                new Thread(stdoutReader).start();
                new Thread(stderrReader).start();
                try {
                    rsync.waitFor();
                    worked = rsync.exitValue() == 0;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.err.println("Please check whether rsync is installed!");
                } finally {
                    stdoutReader.youCanStopNow();
                    stderrReader.youCanStopNow();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.printf("Resources file (%s) does not exist, not copying additional files.\n" + "\tYou can add files and directories to copy with «+ file» or exclude them with «- file», " + "one per line.\n" + "\tSee also: http://www.samba.org/ftp/rsync/rsync.html, INCLUDE/EXCLUDE PATTERN RULES \n", resourcesFile.getAbsolutePath());
        }
        return worked;
    }

    /** Loops on the input stream, constantly trying to fetch something and put it to the output stream. */
    private static class InputStreamRunner implements Runnable {

        boolean stop = false;

        private final InputStream _in;

        private final OutputStream _out;

        private final String _prefix;

        InputStreamRunner(InputStream in, OutputStream out, String prefix) {
            _in = in;
            _out = out;
            _prefix = prefix;
            assert _in != null;
            assert _out != null;
            assert _prefix != null;
        }

        public void run() {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(_in));
            PrintStream o = new PrintStream(_out);
            while (!stop) {
                try {
                    while ((line = br.readLine()) != null) {
                        o.println(_prefix + line);
                    }
                } catch (IOException e) {
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void youCanStopNow() {
            stop = true;
        }
    }

    public static boolean copyFile(InputStream is, OutputStream os) {
        boolean ok = true;
        try {
            byte[] buffer = new byte[0xFFFF];
            for (int len; (len = is.read(buffer)) != -1; ) os.write(buffer, 0, len);
        } catch (IOException e) {
            System.err.println(e);
            ok = false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
        return ok;
    }

    /**
	 * See #copy(File, File, java.io.FileFilter). Copies only one file/directory.
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public static void copy(final File src, final File dest) throws IOException {
        copy(src, dest, new java.io.FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return false;
            }
        });
    }

    /**
	 * Copies files and directories. To copy recursively edit the FileFilter to return true for directories.
	 * @since wiki2xhtml 3.4
	 * 
	 * @param src
	 * @param target
	 * @param fileFilter
	 */
    public static void copy(final File src, final File target, final java.io.FileFilter fileFilter) throws IOException {
        if (src.isFile()) {
            File dest;
            if (target.exists() && target.isDirectory()) {
                dest = new File(target.getAbsolutePath() + File.separator + target.getName());
            } else {
                dest = target;
            }
            copyFile(src, dest);
        } else if (src.isDirectory()) {
            if (!target.exists()) target.mkdirs();
            for (File f : src.listFiles(fileFilter)) {
                copy(f, new File(target.getAbsolutePath() + File.separator + f.getName()), fileFilter);
            }
        } else {
            throw new IOException(src.getAbsolutePath() + " is neither file nor directory.");
        }
    }

    /**
	 * Copies a file
	 *
	 * @param in Input file
	 * @param out Output file
	 * @return 0 with no errors, otherwise -1
	 */
    private static boolean copyFile(File in, File out) {
        boolean ok = true;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(in);
            os = new FileOutputStream(out);
            byte[] buffer = new byte[0xFFFF];
            for (int len; (len = is.read(buffer)) != -1; ) os.write(buffer, 0, len);
        } catch (IOException e) {
            System.err.println(e);
            ok = false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }
        return ok;
    }

    /**
	 * Removes an extension from a file
	 *
	 * @param fExt -
	 *            File to remove extension from
	 * @return File without an extension (from last point on)
	 */
    public static File removeFileExtension(File fExt) {
        File noExtension = null;
        String filename = fExt.getName();
        int dotPos = filename.lastIndexOf('.');
        if (dotPos <= 0) {
            noExtension = fExt;
        } else {
            noExtension = new File(fExt.getParentFile(), filename.substring(0, dotPos));
        }
        return noExtension;
    }

    /**
	 * Returns the file extension.
	 */
    public static String getFileExtensionS(String filename) {
        String ext = new String();
        int dotPos = filename.lastIndexOf('.');
        if (dotPos > 0) {
            ext = filename.substring(dotPos, filename.length());
        }
        return ext;
    }

    /**
	 * Adds an extension to a File.
	 *
	 * @param file File to add the extension
	 * @param ext Extension, with or without .
	 * @return File with extension added to filename, if necessary
	 */
    public static File addFileExtension(File file, String ext) {
        if (file == null) {
            return null;
        }
        File withExtension = null;
        String filename = file.getName();
        if (filename.endsWith(ext)) {
            return file;
        } else {
            if (ext.startsWith(".") && filename.endsWith(".")) filename = filename + ext.substring(1, ext.length()); else {
                if (ext.startsWith(".") || filename.endsWith(".")) filename = filename + ext; else filename = filename + '.' + ext;
            }
            withExtension = new File(file.getParentFile(), filename);
        }
        return withExtension;
    }

    /**
	 * @param extension available extension
	 * @param directory the working directory (null for default)
	 * @param title the title (null for default)
	 * @param append append selected extension
	 * @return The selected file or null
	 */
    public static File openInFileDialog(String extension, File directory, String title, boolean append) {
        String[] s = new String[1];
        s[0] = extension;
        if (append) {
            return addFileExtension(openInFileDialog(s, directory, title, append), extension);
        } else {
            return openInFileDialog(s, directory, title, append);
        }
    }

    /**
	 * @param extensions available extensions
	 * @param directory the working directory (null for default)
	 * @param title the title (null for default)
	 * @param append append selected extension
	 * @return The selected file or null
	 */
    public static File openInFileDialog(String[] extensions, File directory, String title, boolean append) {
        JFileChooser jfc = new JFileChooser();
        if (title == null) {
            title = new String("Open File");
        }
        jfc.setDialogTitle(title);
        if (directory != null && directory.exists() && directory.isDirectory()) {
            jfc.setCurrentDirectory(directory);
        }
        for (int i = 0; i < extensions.length; i++) {
            final String ext = extensions[i];
            jfc.setFileFilter(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(ext);
                }

                public String getDescription() {
                    if (ext.startsWith(".")) {
                        return new StringBuffer("*" + ext).toString();
                    } else {
                        return new StringBuffer("*." + ext).toString();
                    }
                }
            });
        }
        int returnValue = jfc.showOpenDialog(null);
        File datei;
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            datei = jfc.getSelectedFile();
            String ext = new String();
            String filter = jfc.getFileFilter().getDescription();
            Pattern pat;
            Matcher mat;
            boolean found = false;
            if (append) {
                for (int i = 0; i < extensions.length; i++) {
                    pat = Pattern.compile(extensions[i]);
                    mat = pat.matcher(filter);
                    if (mat.find()) {
                        found = true;
                        ext = extensions[i];
                        break;
                    }
                }
                System.out.println(found);
                if (found) {
                    datei = addFileExtension(datei, ext);
                }
            }
            return datei;
        } else {
            return null;
        }
    }

    /**
	 * @param append Append the selected file name extension
	 * @return selected file
	 */
    public static File openInFileDialog(boolean append) {
        String[] s = { ".txt", ".html" };
        return openInFileDialog(s, null, null, append);
    }

    /**
	 * @param extension - availible extension
	 * @param directory - the working directory (null for default
	 * @param title - the title (null for default)
	 * @param append - append selected extension
	 * @return The selected file or null
	 */
    public static File openOutFileDialog(String extension, File directory, String title, boolean append) {
        String[] s = new String[1];
        s[0] = extension;
        if (append) {
            File f = openOutFileDialog(s, directory, title, append);
            if (f != null && !f.exists()) {
                f = addFileExtension(f, extension);
            }
            return f;
        } else {
            return openOutFileDialog(s, directory, title, append);
        }
    }

    /**
	 * @param extensions - available extensions
	 * @param directory - the working directory (null for default
	 * @param title - the title (null for default)
	 * @param append - append selected extension
	 * @return The selected file or null
	 */
    public static File openOutFileDialog(String[] extensions, File directory, String title, boolean append) {
        JFileChooser jfc = new JFileChooser();
        if (title == null) {
            title = new String("Save File");
        }
        jfc.setDialogTitle("Save File");
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);
        if (directory != null && directory.exists() && directory.isDirectory()) {
            jfc.setCurrentDirectory(directory);
        }
        for (int i = 0; i < extensions.length; i++) {
            final String ext = extensions[i];
            jfc.setFileFilter(new FileFilter() {

                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(ext);
                }

                public String getDescription() {
                    if (ext.startsWith(".")) {
                        return new StringBuffer("*" + ext).toString();
                    } else {
                        return new StringBuffer("*." + ext).toString();
                    }
                }
            });
        }
        int returnVal = jfc.showSaveDialog(null);
        File datei;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            datei = jfc.getSelectedFile();
            String ext = new String();
            String filter = jfc.getFileFilter().getDescription();
            Pattern pat;
            Matcher mat;
            boolean found = false;
            if (append) {
                for (int i = 0; i < extensions.length; i++) {
                    pat = Pattern.compile(extensions[i]);
                    mat = pat.matcher(filter);
                    if (mat.find()) {
                        found = true;
                        ext = extensions[i];
                        break;
                    }
                }
                if (found) {
                    datei = addFileExtension(datei, ext);
                }
            }
            return datei;
        } else {
            return null;
        }
    }

    /**
	 * @param append - Append selected Extension or not
	 * @return selected file
	 */
    public static File openOutFileDialog(boolean append, File directory) {
        String[] s = { ".html", ".txt" };
        return openOutFileDialog(s, directory, null, append);
    }

    /**
	 * @return The short path name and eventually removes a part of the path
	 * (usually java.class.dir)
	 */
    public static String getShortPath(File f, String remove) {
        StringBuffer shortPath = new StringBuffer();
        try {
            shortPath = new StringBuffer(f.getCanonicalPath());
        } catch (IOException e) {
            System.err.println(e);
        }
        if (remove.length() > 0) {
            if (shortPath.toString().startsWith(remove)) {
                shortPath.delete(0, remove.length());
            }
        }
        return shortPath.toString();
    }

    /**
	 * Checks whether a file is binary by looking for null-Bytes.
	 *
	 * @param f - input file to check
	 * @return true, if it is a binary file
	 */
    public static boolean binaryCheck(File f) {
        boolean bin = false;
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
            byte[] b = new byte[4096];
            int nBytes;
            tryLoop: do {
                nBytes = bis.read(b, 0, b.length);
                for (int i = 0; i < nBytes; i++) {
                    if (b[i] == 0) {
                        bin = true;
                        break tryLoop;
                    }
                }
            } while (nBytes > 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bin;
    }
}
