package io.hdd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import io.console.Log;

/**
 * this class presented a file IO-handler
 * @author yrow
 * TODO exception-handling and extend the functionalities of this class
 */
public class FileIO {

    /**
	 * the file for IO-access
	 */
    private File file = null;

    /**
	 * the default constructor without parameter of this class
	 */
    public FileIO() {
        ;
    }

    /**
	 * the constructor with parameter of this class
	 * @param defaultFile this file for IO-access
	 */
    public FileIO(File defaultFile) {
        file = defaultFile;
    }

    /**
	 * the constructor with parameter of this class
	 * @param fileName the name of default-file
	 */
    public FileIO(String fileName) {
        setFile(fileName);
    }

    /**
	 * set the absolutly path of the default-file
	 * @param path the absolutly path of file
	 */
    public void setFile(String path) {
        file = new File(path);
    }

    /**
	 * get the name (absolut path) of file
	 * @return the name (absolut path) of file 
	 */
    public String getFileName() {
        return file.getAbsolutePath();
    }

    /**
	 * set the default file
	 * @param defaultFile the file for IO-access
	 */
    public void setFile(File defaultFile) {
        file = defaultFile;
    }

    /**
	 * get the file for IO-access
	 * @return the file for IO-access 
	 */
    public File getFile() {
        return file;
    }

    public String readFile() {
        return "";
    }

    public String readStream(InputStream in) {
        return "";
    }

    /**
	 * write contents in the default file
	 * @param content the contents, you want write in file
	 */
    public void write(String content) {
        ;
    }

    /**
	 * write a InputStream in the default-file
	 * @param in the InputStream, you want write in the file
	 */
    public void write(InputStream in) {
        byte[] buffer = new byte[0xFFFF];
        try {
            FileOutputStream out = new FileOutputStream(file);
            for (int len; (len = in.read(buffer)) != -1; ) out.write(buffer, 0, len);
        } catch (IOException error) {
            Log.print(error);
        }
    }

    /**
	 * copy a InputStream in a other OutputStream
	 * @param in the InputStream, you wanted "copy"
	 * @param out the "copied" InputStream
	 * @throws IOException for exception-handling
	 */
    static void copy(InputStream in, OutputStream out) {
        byte[] buffer = new byte[0xFFFF];
        try {
            for (int len; (len = in.read(buffer)) != -1; ) out.write(buffer, 0, len);
        } catch (IOException error) {
            Log.print(error);
        }
    }

    public boolean exists() {
        return file.exists();
    }

    public void create(boolean overwrite) {
        try {
            if (overwrite) {
                file.createNewFile();
            } else {
                if (exists()) {
                    Log.print("File already exists!");
                } else {
                    file.createNewFile();
                }
            }
        } catch (IOException error) {
            Log.print(error);
        }
    }

    public void create() {
        try {
            if (exists()) {
                Log.print("File already exists!");
            } else {
                file.createNewFile();
            }
        } catch (IOException error) {
            Log.print(error);
        }
    }
}
