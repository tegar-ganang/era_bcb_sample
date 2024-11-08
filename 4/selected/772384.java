package com.organic.maynard.io;

import java.io.*;
import java.util.*;

/**
 * A collection of static methods and fields useful for working with files on
 * disk.
 */
public class FileTools {

    /** The standard line ending used on unix systems. */
    public static final String LINE_ENDING_UNIX = "\n";

    /** The standard line ending used on windows systems. */
    public static final String LINE_ENDING_WIN = "\r\n";

    /** The standard line ending used on pre OSX mac systems. */
    public static final String LINE_ENDING_MAC = "\r";

    /** The linefeed character. */
    public static final char LF = '\n';

    /** The carriage return character. */
    public static final char CR = '\r';

    /** The character value that indicates end of file. */
    public static final char EOF = (char) -1;

    /** The default character encoding used with many methods in this class. */
    public static final String DEFAULT_ENCODING = "UTF-8";

    private FileTools() {
    }

    /**
	 * Saves the provided lines and corresponding line endings to a file.
	 *
	 * @param file the File to save the data to.
	 * @param encoding the encoding type to save the data as.
	 * @param line a List of lines (Strings) to save.
	 * @param lineEndings a List of lines (Strings) to use as the line endings
	 *        for each line saved.
	 */
    public static void dumpArrayOfLinesToFile(File file, String encoding, List lines, List lineEndings) {
        try {
            File parent = new File(file.getParent());
            parent.mkdirs();
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
            for (int i = 0; i < lines.size(); i++) {
                String line = (String) lines.get(i);
                String lineEnding = (String) lineEndings.get(i);
                if (line != null) {
                    out.write(line);
                }
                if (lineEnding != null) {
                    out.write(lineEnding);
                }
            }
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int BUFFER_SIZE = 8192;

    private static final StringBuffer fileBuf = new StringBuffer();

    private static final StringBuffer lineBuf = new StringBuffer();

    private static final char[] in = new char[BUFFER_SIZE];

    /**
	 * Reads a file and populates the two provided Lists with the lines and their
	 * corresponding line endings.
	 *
	 * @param file the file to read from.
	 * @param encoding the encoding to interpret the file with.
	 * @param lines the List to store the lines read.
	 * @param lineEndings the List to store the line endings read.
	 * @return true if no errors occurred, false if an error occurred.
	 */
    public static boolean readFileToArrayOfLines(File file, String encoding, List lines, List lineEndings) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
            fileBuf.setLength(0);
            while (true) {
                int result = reader.read(in, 0, BUFFER_SIZE);
                if (result == -1) {
                    break;
                } else {
                    fileBuf.append(in, 0, result);
                }
            }
            lineBuf.setLength(0);
            for (int i = 0, limit = fileBuf.length(); i < limit; i++) {
                char c = fileBuf.charAt(i);
                switch(c) {
                    case CR:
                        lines.add(lineBuf.toString());
                        lineBuf.setLength(0);
                        i++;
                        if (i < fileBuf.length()) {
                            char lookahead = fileBuf.charAt(i);
                            switch(lookahead) {
                                case CR:
                                    lineEndings.add(LINE_ENDING_MAC);
                                    lines.add("");
                                    lineEndings.add(LINE_ENDING_MAC);
                                    break;
                                case LF:
                                    lineEndings.add(LINE_ENDING_WIN);
                                    break;
                                default:
                                    lineEndings.add(LINE_ENDING_MAC);
                                    lineBuf.append(lookahead);
                                    if (i == fileBuf.length() - 1) {
                                        lines.add(lineBuf.toString());
                                        lineEndings.add(null);
                                    }
                                    break;
                            }
                        }
                        break;
                    case LF:
                        lines.add(lineBuf.toString());
                        lineBuf.setLength(0);
                        lineEndings.add(LINE_ENDING_UNIX);
                        break;
                    default:
                        lineBuf.append(c);
                        if (i == fileBuf.length() - 1) {
                            lines.add(lineBuf.toString());
                            lineEndings.add(null);
                        }
                        break;
                }
            }
            reader.close();
            return true;
        } catch (FileNotFoundException fnfe) {
            System.out.println("Error: FileTools.readFileToArrayOfLines: " + fnfe.getMessage());
        } catch (Exception e) {
            System.out.println("Error: FileTools.readFileToArrayOfLines: " + e.getMessage());
        }
        return false;
    }

    /**
	 * Reads a file and returns a String.
	 * 
	 * @param file the file to read from.
	 * @return the contents of the file as a String.
	 */
    public static String readFileToString(File file) {
        return readFileToString(file, DEFAULT_ENCODING, LINE_ENDING_UNIX);
    }

    /**
	 * Reads a file and returns a String using the provided line ending for each
	 * line ending in the file.
	 * 
	 * @param file the file to read from.
	 * @param lineEnding the line ending to use for each line ending in the file.
	 * @return the contents of the file as a String.
	 */
    public static String readFileToString(File file, String lineEnding) {
        return readFileToString(file, DEFAULT_ENCODING, lineEnding);
    }

    /**
	 * Reads a file and returns it as a String.
	 * 
	 * @param file the File to read from.
	 * @param encoding the encoding to interpret the file with.
	 * @param lineEnding the line ending String to isert into the String at the
	 *        end of each line of the file.
	 */
    public static String readFileToString(File file, String encoding, String lineEnding) {
        StringBuffer text = new StringBuffer("");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader buffer = new BufferedReader(new InputStreamReader(fileInputStream, encoding));
            boolean eof = false;
            while (!eof) {
                String theLine = buffer.readLine();
                if (theLine == null) {
                    eof = true;
                } else {
                    text.append(theLine).append(lineEnding);
                }
            }
            fileInputStream.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return text.toString();
    }

    /**
	 * Saves the text to the file using the default encoding. Makes parent directories
	 * as necessary. Handles IOExceptions by printing a stack trace.
	 * 
	 * @param file the File to save the String to.
	 * @param text the text to save into the file.
	 */
    public static void dumpStringToFile(File file, String text) {
        try {
            dumpStringToFile(file, text, DEFAULT_ENCODING);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
	 * Saves the text to the file using the encoding provided. Makes parent directories
	 * as necessary.
	 * 
	 * @param file the File to save the String to.
	 * @param text the text to save into the file.
	 * @param encoding the encoding to use for the file.
	 */
    public static void dumpStringToFile(File file, String text, String encoding) throws IOException {
        File parent = new File(file.getParent());
        parent.mkdirs();
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
        out.write(text);
        out.flush();
        out.close();
    }

    /**
	 * Copies the "from" file to the "to" file. If the file is a directory then
	 * all descendants of the file are also copied.
	 * 
	 * @param from the file/dir to copy from.
	 * @param to the file/dir to copy to.
	 */
    public static void copy(File from, File to) throws IOException {
        Vector fromFileList = new Vector();
        Vector toFileList = new Vector();
        fromFileList.add(from);
        toFileList.add(to);
        String rootTo = "";
        String rootFrom = "";
        if (from.isDirectory()) {
            rootTo = to.getAbsolutePath();
            rootFrom = from.getAbsolutePath();
        }
        int i = 0;
        while (true) {
            if (i >= fromFileList.size()) {
                break;
            }
            File fromFile = (File) fromFileList.get(i);
            File toFile = (File) toFileList.get(i);
            i++;
            if (fromFile.isDirectory()) {
                toFile.mkdirs();
                File[] files = fromFile.listFiles();
                for (int j = 0; j < files.length; j++) {
                    File newFromFile = files[j];
                    fromFileList.add(newFromFile);
                    String newFromPath = newFromFile.getAbsolutePath();
                    String newPath = newFromPath.substring(rootFrom.length(), newFromPath.length());
                    File newToFile = new File(rootTo + newPath);
                    toFileList.add(newToFile);
                }
            } else if (fromFile.isFile()) {
                FileInputStream sFrom = new FileInputStream(fromFile);
                FileOutputStream sTo = new FileOutputStream(toFile);
                copy(sFrom, sTo);
                sFrom.close();
                sTo.flush();
                sTo.close();
            } else {
            }
        }
    }

    /**
	 * Copies the data from the input stream to the output stream.
	 * 
	 * @param in the InputStream to read from.
	 * @param out the OutputStream to write to.
	 */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[256];
                while (true) {
                    int bytesread = in.read(buffer);
                    if (bytesread == -1) {
                        break;
                    }
                    out.write(buffer, 0, bytesread);
                }
            }
        }
    }

    /**
	 * Writes the provided Object to a file with the provided filename.
	 * 
	 * @param obj the Object to save.
	 * @param filename the path of the file to save to.
	 * @return true if the file was saved successfully or false if it failed for
	 *         some reason.
	 */
    public static boolean writeObjectToFile(Object obj, String filename) {
        try {
            File file = new File(filename);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean dirsCreated = parent.mkdirs();
                if (!dirsCreated) {
                    System.out.println("ERROR: Unable to create parent directories for: " + file.getPath());
                    return false;
                }
            }
            ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
            stream.writeObject(obj);
            stream.close();
            return true;
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
            return false;
        }
    }

    /**
	 * Reads in a serialized java object from the filename provided.
	 * 
	 * @param filename the path to the file to read from.
	 * @return the Object contained in the serialized java file or null
	 *         if an exception occurred.
	 */
    public static Object ReadObjectFromFile(String filename) {
        Object obj = null;
        try {
            ObjectInputStream stream = new ObjectInputStream(new FileInputStream(filename));
            obj = stream.readObject();
            stream.close();
        } catch (OptionalDataException ode) {
            System.out.println("Exception: " + ode);
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Exception: " + cnfe);
        } catch (FileNotFoundException fnfe) {
            System.out.println("Exception: " + fnfe);
        } catch (StreamCorruptedException sce) {
            System.out.println("Exception: " + sce);
        } catch (IOException ioe) {
            System.out.println("Exception: " + ioe);
        }
        return obj;
    }
}
