package org.bungeni.translators.utility.files;

import org.xml.sax.InputSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import javax.xml.transform.stream.StreamSource;

/**
 * This class supplies several method useful for the management of the File documents
 *
 */
public class FileUtility {

    private static FileUtility instance = null;

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger("org.bungeni.translators.utility.files.FileUtility");

    /**
     * The system line separator string is generated statically,
     * we dont hardcode for different platforms but determine the line separator
     * on the fly
     */
    public static final String LINE_SEPARATOR;

    static {
        StringWriter buf = new StringWriter(4);
        PrintWriter out = new PrintWriter(buf);
        out.println();
        LINE_SEPARATOR = buf.toString();
    }

    /**
     * Private constructor used to create the FileUtility instance
     */
    private FileUtility() {
    }

    /**
     * Get the current instance of the FileUtility class
     * @return the Utility instance
     */
    public static FileUtility getInstance() {
        if (instance == null) {
            instance = new FileUtility();
        }
        return instance;
    }

    /**
     * Write the File at the given path to a String
     * 30th July - fixed to use bufferedReader to prevent encoding problems
     * @param aFilePath the path of the file to retrieve as a String
     * @return the String representation of the file
     * @throws IOException
     */
    public String FileToString(String aFilePath) throws IOException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        StringBuilder textFromFile = new StringBuilder();
        try {
            fileReader = new FileReader(aFilePath);
            bufferedReader = new BufferedReader(fileReader);
            String currentLineFromFile = bufferedReader.readLine();
            int i = 0;
            while (currentLineFromFile != null) {
                if (i == 0) {
                    textFromFile.append(currentLineFromFile);
                } else {
                    textFromFile.append(LINE_SEPARATOR + currentLineFromFile);
                }
                currentLineFromFile = bufferedReader.readLine();
            }
            return textFromFile.toString();
        } catch (IOException ioException) {
            log.error("FileToString: file not found ", ioException);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException ioExceptionIgnore) {
            }
        }
        return textFromFile.toString();
    }

    /**
     * Write the given content to the file at the given path
     * @param aFilePath the path of the file to create
     * @param aFileContent the content of the file to create
     * @throws IOException
     */
    public void StringToFile(String aFilePath, String aFileContent) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(aFilePath));
        out.write(aFileContent);
        out.close();
    }

    /**
     * Make a copy of the file using the NIO channel API
     * @param in source file to copy
     * @param out target copy
     * @throws IOException
     */
    public File copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        copyChannel(inChannel, outChannel);
        return out;
    }

    /**
     * transfer bytes from input channel to output channel
     * @param inChannel
     * @param outChannel
     * @throws IOException
     */
    private void copyChannel(FileChannel inChannel, FileChannel outChannel) throws IOException {
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /**
     * Transfer bytes from FileInputStream to outputChannel
     * @param fis
     * @param out
     * @throws IOException
     */
    public void copyFile(FileInputStream fis, File out) throws IOException {
        FileChannel inChannel = fis.getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        copyChannel(inChannel, outChannel);
    }

    /**
     * Returns a bufferedreader for a file path
     * @param sPath
     * @return
     * @throws FileNotFoundException
     */
    public BufferedReader BufferedFileReader(String sPath) throws FileNotFoundException {
        File fFile = new File(sPath);
        return BufferedFileReader(fFile);
    }

    /**
     * Returns a buffered reader for a file handle
     * @param fFile
     * @return
     * @throws FileNotFoundException
     */
    public BufferedReader BufferedFileReader(File fFile) throws FileNotFoundException {
        BufferedReader bReader = null;
        FileReader fReader = new FileReader(fFile);
        bReader = new BufferedReader(fReader);
        return bReader;
    }

    /**
     * Returns a buffered reader for an inputstream
     * @param iStream
     * @return
     */
    public BufferedReader BufferedFileReader(InputStream iStream) {
        return new BufferedReader(new InputStreamReader(iStream));
    }

    /**
     * Returns a file path as an input source
     * @param sPath
     * @return
     * @throws FileNotFoundException
     */
    public InputSource FileAsInputSource(String sPath) throws FileNotFoundException {
        InputSource iFileSource = null;
        BufferedReader bReader = BufferedFileReader(sPath);
        if (bReader != null) {
            iFileSource = new InputSource(bReader);
        }
        return iFileSource;
    }

    /**
     * Converts an InputStream to an InputSource
     * @param is
     * @return
     */
    public InputSource StreamAsInputSource(InputStream is) {
        InputSource iFileSource = null;
        BufferedReader bReader = BufferedFileReader(is);
        if (bReader != null) {
            iFileSource = new InputSource(bReader);
        }
        return iFileSource;
    }

    /**
     * Takes a File handle and returns the file contents as an InputSource
     * @param fFile
     * @return
     * @throws FileNotFoundException
     */
    public InputSource FileAsInputSource(File fFile) throws FileNotFoundException {
        InputSource iFileSource = null;
        BufferedReader bReader = BufferedFileReader(fFile);
        if (bReader != null) {
            iFileSource = new InputSource(bReader);
        }
        return iFileSource;
    }

    /**
     * Takes a path to a file and returns it as a StreamSource
     * @param sPath
     * @return
     * @throws FileNotFoundException
     */
    public StreamSource FileAsStreamSource(String sPath) throws FileNotFoundException {
        StreamSource sSource = null;
        BufferedReader bReader = BufferedFileReader(sPath);
        sSource = new StreamSource(bReader);
        return sSource;
    }

    /**
     * Takes a File handle to a file and returns it as a StreamSource
     * @param sPath
     * @return
     * @throws FileNotFoundException
     */
    public StreamSource FileAsStreamSource(File fPath) throws FileNotFoundException {
        StreamSource sSource = null;
        BufferedReader bReader = BufferedFileReader(fPath);
        sSource = new StreamSource(bReader);
        return sSource;
    }
}
