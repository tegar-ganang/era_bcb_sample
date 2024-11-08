package org.taak.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import org.taak.error.IOError;
import org.taak.error.TypeError;

public class CopyUtil {

    /**
     * Copy from source to destination.  Source and destination maybe
     * a filename, File, URL, InputStream, Reader/Writer or RandomAccessFile.
     */
    public static void copy(Object arg1, Object arg2) {
        Writer writer = null;
        Reader reader = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            if (arg2 instanceof Writer) {
                writer = (Writer) arg2;
                if (arg1 instanceof Reader) {
                    reader = (Reader) arg1;
                    copy(reader, writer);
                } else if (arg1 instanceof String) {
                    reader = new FileReader(new File((String) arg1));
                    copy(reader, writer);
                } else if (arg1 instanceof File) {
                    reader = new FileReader((File) arg1);
                    copy(reader, writer);
                } else if (arg1 instanceof URL) {
                    copy(((URL) arg1).openStream(), writer);
                } else if (arg1 instanceof InputStream) {
                    reader = new InputStreamReader((InputStream) arg1);
                    copy(reader, writer);
                } else if (arg1 instanceof RandomAccessFile) {
                    copy((RandomAccessFile) arg1, writer);
                } else {
                    throw new TypeError("Invalid first argument to copy()");
                }
            } else if (arg2 instanceof OutputStream) {
                outStream = (OutputStream) arg2;
                if (arg1 instanceof Reader) {
                    copy((Reader) arg1, new OutputStreamWriter(outStream));
                } else if (arg1 instanceof String) {
                    inStream = new FileInputStream(new File((String) arg1));
                    copy(inStream, outStream);
                } else if (arg1 instanceof File) {
                    inStream = new FileInputStream((File) arg1);
                    copy(inStream, outStream);
                } else if (arg1 instanceof URL) {
                    copy(((URL) arg1).openStream(), outStream);
                } else if (arg1 instanceof InputStream) {
                    copy((InputStream) arg1, outStream);
                } else if (arg1 instanceof RandomAccessFile) {
                    copy((RandomAccessFile) arg1, outStream);
                } else {
                    throw new TypeError("Invalid first argument to copy()");
                }
            } else if (arg2 instanceof RandomAccessFile) {
                RandomAccessFile out = (RandomAccessFile) arg2;
                if (arg1 instanceof Reader) {
                    copy((Reader) arg1, out);
                } else if (arg1 instanceof String) {
                    inStream = new FileInputStream(new File((String) arg1));
                    copy(inStream, out);
                } else if (arg1 instanceof File) {
                    inStream = new FileInputStream((File) arg1);
                    copy(inStream, out);
                } else if (arg1 instanceof URL) {
                    copy(((URL) arg1).openStream(), out);
                } else if (arg1 instanceof InputStream) {
                    copy((InputStream) arg1, out);
                } else if (arg1 instanceof RandomAccessFile) {
                    copy((RandomAccessFile) arg1, out);
                } else {
                    throw new TypeError("Invalid first argument to copy()");
                }
            } else if (arg2 instanceof File || arg2 instanceof String) {
                File outFile = null;
                if (arg2 instanceof File) {
                    outFile = (File) arg2;
                } else {
                    outFile = new File((String) arg2);
                }
                outStream = new FileOutputStream(outFile);
                if (arg1 instanceof Reader) {
                    copy((Reader) arg1, new OutputStreamWriter(outStream));
                } else if (arg1 instanceof String) {
                    inStream = new FileInputStream(new File((String) arg1));
                    copy(inStream, outStream);
                } else if (arg1 instanceof File) {
                    inStream = new FileInputStream((File) arg1);
                    copy(inStream, outStream);
                } else if (arg1 instanceof URL) {
                    copy(((URL) arg1).openStream(), outStream);
                } else if (arg1 instanceof InputStream) {
                    copy((InputStream) arg1, outStream);
                } else if (arg1 instanceof RandomAccessFile) {
                    copy((RandomAccessFile) arg1, outStream);
                } else {
                    throw new TypeError("Invalid first argument to copy()");
                }
            } else {
                throw new TypeError("Invalid second argument to copy()");
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage(), e);
        }
    }

    /**
     * Copy from a Reader to a Writer.
     * @param reader
     * @param writer
     */
    public static void copy(Reader reader, Writer writer) {
        try {
            char[] buf = new char[1024];
            for (; ; ) {
                int n = reader.read(buf);
                if (n < 0) {
                    break;
                }
                writer.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage(), e);
        }
    }

    /**
     * Copy from an InputStream to an OutputStream.
     * @param in
     * @param out
     */
    public static void copy(InputStream in, OutputStream out) {
        try {
            byte[] buf = new byte[1024];
            for (; ; ) {
                int n = in.read(buf);
                if (n < 0) {
                    break;
                }
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage(), e);
        }
    }

    /**
     * Copy from a RandomAccessFile to a Writer.
     * @param in
     * @param writer
     */
    public static void copy(RandomAccessFile in, Writer writer) {
        try {
            byte[] buf = new byte[1024];
            for (; ; ) {
                int n = in.read(buf);
                if (n < 0) {
                    break;
                }
                writer.write(new String(buf, 0, n));
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage(), e);
        }
    }

    /**
     * Copy from a RandomAccessFile to an OutputStream.
     * @param in
     * @param out
     */
    public static void copy(RandomAccessFile in, OutputStream out) {
        try {
            byte[] buf = new byte[1024];
            for (; ; ) {
                int n = in.read(buf);
                if (n < 0) {
                    break;
                }
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage(), e);
        }
    }

    /**
     * Copy from a RandomAccessFile to a RandomAccessFile.
     * @param in
     * @param out
     */
    public static void copy(RandomAccessFile in, RandomAccessFile out) {
        try {
            byte[] buf = new byte[1024];
            for (; ; ) {
                int n = in.read(buf);
                if (n < 0) {
                    break;
                }
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage(), e);
        }
    }

    /**
     * Copy from a reader to a RandomAccessFile.
     * @param reader
     * @param out
     */
    public static void copy(Reader reader, RandomAccessFile out) {
        try {
            char[] buf = new char[1024];
            for (; ; ) {
                int n = reader.read(buf);
                if (n < 0) {
                    break;
                }
                out.writeChars(new String(buf, 0, n));
            }
        } catch (IOException e) {
            throw new IOError(e.getMessage(), e);
        }
    }
}
