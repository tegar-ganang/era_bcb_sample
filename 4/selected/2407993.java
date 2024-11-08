package com.scholardesk.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * File Utility class for dealing with Files in memory.  May also want to
 * look into Apache's IO Commons for future utilities.
 * 
 * @author Christopher Dunavant
 * @author Joe Liversedge
 */
public class FileUtil {

    /**
	 * Converts the contents of a File to a String.
	 * 
	 * @param _file a file which already exists and can be read.
	 * 
	 * @return string that contains the contents of the file.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static String fileToString(File _file) throws FileNotFoundException, IOException {
        StringWriter writer = new StringWriter();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(_file)));
        char buf[] = new char[1024];
        int len = 0;
        while ((len = reader.read(buf, 0, 1024)) != -1) writer.write(buf, 0, len);
        return writer.toString();
    }

    /**
	 * Returns the basename of a file name.
	 * 
	 * @param _filename a complete file name with path.
	 * 
	 * @return file basename.
	 */
    public static String basename(String _filename) {
        int slash = _filename.lastIndexOf("\\");
        if (slash != -1) _filename = _filename.substring(slash + 1);
        slash = _filename.lastIndexOf("/");
        if (slash != -1) _filename = _filename.substring(slash + 1);
        slash = _filename.lastIndexOf(":");
        if (slash != -1) _filename = _filename.substring(slash + 1);
        return _filename;
    }

    /**
	 * Returns the filepath for a file name.
	 * 
	 * @param _filename a complete file name with path.
	 * 
	 * @return file's full path.
	 */
    public static String filepath(String _filename) {
        int slash = _filename.lastIndexOf("\\");
        if (slash != -1) _filename = _filename.substring(0, slash);
        slash = _filename.lastIndexOf("/");
        if (slash != -1) _filename = _filename.substring(0, slash);
        return _filename;
    }

    /**
	 * Delete a file from the file system.
	 * 
	 * @param _filename name of file to delete.
	 * 
	 * @return {@code true} if file is deleted {@code false} if the file could not be deleted.
	 */
    public static boolean delete(String _filename) {
        File _file = new java.io.File(_filename);
        return _file.delete();
    }

    /**
	 * Write the contents of a file to an output stream.
	 * 
	 * @param _filename name and full path of file. 
	 * @param _stream output stream to write contents of file to.
	 */
    public static void writeToStream(String _filename, OutputStream _stream) {
        File _file = new java.io.File(_filename);
        writeToStream(_file, _stream);
    }

    /**
	 * Write the contents of a file to an output stream.
	 * 
	 * @param _file {@link java.io.File} instance.
	 * @param _stream output stream to write contents of file to.
	 * 
	 * @throws RuntimeException if an IOException is encountered.
	 */
    public static void writeToStream(File _file, OutputStream _stream) {
        BufferedInputStream _buf = null;
        try {
            FileInputStream input = new FileInputStream(_file);
            _buf = new BufferedInputStream(input);
            int readBytes = 0;
            while ((readBytes = _buf.read()) != -1) {
                _stream.write(readBytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (_stream != null) _stream.close();
                if (_buf != null) _buf.close();
            } catch (IOException e) {
            }
        }
    }
}
