package pckt.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class StorageHandler {

    private static final String TAG = "pcktTest_StorageHandler";

    private static final boolean D = true;

    private String name, state;

    private boolean readable, writeable;

    private File file;

    private FileWriter fileWriter;

    private FileReader fileReader;

    public StorageHandler(String name, Context context) {
        this.name = name;
        fileWriter = null;
        fileReader = null;
        state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            readable = writeable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            readable = true;
            writeable = false;
        } else {
            readable = writeable = false;
        }
        String path;
        path = File.separator;
        path = path.concat("Android").concat(File.separator).concat("data").concat(File.separator).concat("pckt.Test").concat(File.separator).concat("files").concat(File.separator);
        file = new File(Environment.getExternalStorageDirectory() + path, this.name);
    }

    /**
	 * Open the file for writing
	 * @return True in success. False otherwise
	 */
    public boolean openWrite() {
        if (!writeable) {
            if (D) Log.i(TAG, "The file is not availiable for writing.");
            return false;
        }
        try {
            fileWriter = new FileWriter(file);
        } catch (Exception e) {
            if (D) Log.i(TAG, "Excpetion: couldn't open the file writer.");
            if (D) Log.e(TAG, "Excpetion: couldn't open the file writer.", e);
            return false;
        }
        return true;
    }

    /**
	 * Open the file for reading
	 * @return True in success. False otherwise
	 */
    public boolean openRead() {
        if (!readable) {
            if (D) Log.i(TAG, "The file is not availiable for reading.");
            return false;
        }
        try {
            fileReader = new FileReader(file);
        } catch (Exception e) {
            if (D) Log.i(TAG, "Excpetion: couldn't open the file reader.");
            if (D) Log.e(TAG, "Excpetion: couldn't open the file reader.", e);
            return false;
        }
        return true;
    }

    /**
	 * Write a string to the file
	 * @param str The string to be written
	 * @return True on success. False otherwise
	 */
    public boolean write(String str) {
        if (fileWriter == null) openWrite();
        try {
            fileWriter.write(str);
        } catch (Exception e) {
            if (D) Log.i(TAG, "Excpetion: couldn't write to the file.");
            if (D) Log.e(TAG, "Excpetion: couldn't write to the file.", e);
            return false;
        }
        return true;
    }

    /**
	 * Read a string from the file
	 * @return The string read
	 */
    public String read() {
        if (fileReader == null) {
            if (openRead()) return null;
        }
        char[] buffer = new char[1024];
        int size = 0;
        try {
            size = fileReader.read(buffer);
        } catch (Exception e) {
            if (D) Log.i(TAG, "Excpetion: couldn't read the file.");
            if (D) Log.e(TAG, "Excpetion: couldn't read the file.", e);
            return null;
        }
        if (size == -1) {
            return null;
        }
        String str = new String(buffer, 0, size);
        return (str);
    }

    public boolean close() {
        try {
            if (fileWriter != null) fileWriter.close();
            fileWriter = null;
            if (fileReader != null) fileReader.close();
            fileReader = null;
        } catch (IOException e) {
            if (D) Log.i(TAG, "Excpetion: couldn't close the fileWriter/fileReader.");
            if (D) Log.e(TAG, "Excpetion: couldn't close the fileWriter/fileReader.", e);
            return false;
        }
        return true;
    }
}
