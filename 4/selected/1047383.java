package net.mogray.infinitypfm.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import net.mogray.infinitypfm.core.conf.MM;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.apache.commons.io.IOUtil;

/**
 * @author wggray
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class FileHandler {

    /**
	 *  
	 */
    public FileHandler() {
        super();
    }

    private BufferedReader in = null;

    private PrintWriter out = null;

    private boolean bOpenMode = false;

    private boolean bNewMode = false;

    public void FileOpenRead(String sPath) throws FileNotFoundException {
        if (!bNewMode && !bOpenMode) {
            in = new BufferedReader(new FileReader(sPath));
        }
    }

    public void FileOpenWrite(String sFile) throws IOException {
        if (!bNewMode && !bOpenMode) {
            out = new PrintWriter(new BufferedWriter(new FileWriter(sFile)));
        }
    }

    public void FileClose() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
        if (out != null) {
            out.close();
            out = null;
        }
    }

    public String getDirectory() {
        DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
        return dialog.open();
    }

    public File[] getFileList(String sDir, String starts, String ends) {
        final String sW = starts;
        final String eW = ends;
        FileFilter ff = new FileFilter() {

            public boolean accept(File file) {
                if (sW == "" && eW == "") {
                    return true;
                } else if (file.getName().endsWith(eW) && file.getName().startsWith(sW)) {
                    return true;
                } else if (file.getName().endsWith(eW) && sW == "") {
                    return true;
                } else if (file.getName().startsWith(sW) && eW == "") {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File fDir = new File(sDir);
        File[] resultList = fDir.listFiles(ff);
        return resultList;
    }

    public String LineInput() throws IOException {
        if (in != null) {
            return in.readLine();
        } else {
            return null;
        }
    }

    public String getFileAsString() throws IOException {
        return IOUtil.toString(in);
    }

    public void FileNew(String sFile) throws IOException {
        if (!bNewMode && !bOpenMode) {
            out = new PrintWriter(new BufferedWriter(new FileWriter(sFile)));
        }
    }

    public void WriteLine(String sLine) {
        out.println(sLine);
    }

    public void copyFile(String source_name, String dest_name) throws IOException {
        File source_file = new File(source_name);
        File destination_file = new File(dest_name);
        FileInputStream source = null;
        FileOutputStream destination = null;
        byte[] buffer;
        int bytes_read;
        try {
            if (!source_file.exists() || !source_file.isFile()) throw new FileCopyException(MM.PHRASES.getPhrase("25") + " " + source_name);
            if (!source_file.canRead()) throw new FileCopyException(MM.PHRASES.getPhrase("26") + " " + MM.PHRASES.getPhrase("27") + ": " + source_name);
            if (destination_file.exists()) {
                if (destination_file.isFile()) {
                    DataInputStream in = new DataInputStream(System.in);
                    String response;
                    if (!destination_file.canWrite()) throw new FileCopyException(MM.PHRASES.getPhrase("28") + " " + MM.PHRASES.getPhrase("29") + ": " + dest_name);
                    System.out.print(MM.PHRASES.getPhrase("19") + dest_name + MM.PHRASES.getPhrase("30") + ": ");
                    System.out.flush();
                    response = in.readLine();
                    if (!response.equals("Y") && !response.equals("y")) throw new FileCopyException(MM.PHRASES.getPhrase("31"));
                } else throw new FileCopyException(MM.PHRASES.getPhrase("28") + " " + MM.PHRASES.getPhrase("32") + ": " + dest_name);
            } else {
                File parentdir = parent(destination_file);
                if (!parentdir.exists()) throw new FileCopyException(MM.PHRASES.getPhrase("28") + " " + MM.PHRASES.getPhrase("33") + ": " + dest_name);
                if (!parentdir.canWrite()) throw new FileCopyException(MM.PHRASES.getPhrase("28") + " " + MM.PHRASES.getPhrase("34") + ": " + dest_name);
            }
            source = new FileInputStream(source_file);
            destination = new FileOutputStream(destination_file);
            buffer = new byte[1024];
            while (true) {
                bytes_read = source.read(buffer);
                if (bytes_read == -1) break;
                destination.write(buffer, 0, bytes_read);
            }
        } finally {
            if (source != null) try {
                source.close();
            } catch (IOException e) {
                ;
            }
            if (destination != null) try {
                destination.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    private static File parent(File f) {
        String dirname = f.getParent();
        if (dirname == null) {
            if (f.isAbsolute()) return new File(File.separator); else return new File(System.getProperty("user.dir"));
        }
        return new File(dirname);
    }

    public void copyFile2(String src, String dest) throws IOException {
        FileWriter fw = null;
        FileReader fr = null;
        BufferedReader br = null;
        BufferedWriter bw = null;
        File source = null;
        try {
            fr = new FileReader(src);
            fw = new FileWriter(dest);
            br = new BufferedReader(fr);
            bw = new BufferedWriter(fw);
            source = new File(src);
            int fileLength = (int) source.length();
            char charBuff[] = new char[fileLength];
            while (br.read(charBuff, 0, fileLength) != -1) bw.write(charBuff, 0, fileLength);
        } catch (FileNotFoundException fnfe) {
            throw new FileCopyException(src + " " + MM.PHRASES.getPhrase("35"));
        } catch (IOException ioe) {
            throw new FileCopyException(MM.PHRASES.getPhrase("36"));
        } finally {
            try {
                if (br != null) br.close();
                if (bw != null) bw.close();
            } catch (IOException ioe) {
            }
        }
    }

    public String getFile() {
        FileDialog f = new FileDialog(Display.getCurrent().getActiveShell());
        return f.open();
    }

    class FileCopyException extends IOException {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        public FileCopyException(String msg) {
            super(msg);
        }
    }
}
