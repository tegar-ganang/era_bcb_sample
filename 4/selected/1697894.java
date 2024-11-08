package org.weasis.core.api.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DecimalFormat;
import java.util.Properties;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.weasis.core.api.Messages;

public class FileUtil {

    private static final double BASE = 1024, KB = BASE, MB = KB * BASE, GB = MB * BASE;

    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static void safeClose(final Closeable object) {
        try {
            if (object != null) {
                object.close();
            }
        } catch (IOException e) {
        }
    }

    public static void safeClose(ImageInputStream stream) {
        try {
            if (stream != null) {
                stream.flush();
                stream.close();
            }
        } catch (IOException e) {
        }
    }

    public static final void deleteDirectoryContents(final File dir) {
        if ((dir == null) || !dir.isDirectory()) {
            return;
        }
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryContents(f);
                } else {
                    try {
                        f.delete();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public static void safeClose(XMLStreamWriter writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (XMLStreamException e) {
        }
    }

    public static void safeClose(XMLStreamReader xmler) {
        try {
            if (xmler != null) {
                xmler.close();
            }
        } catch (XMLStreamException e) {
        }
    }

    public static boolean isWriteable(File file) {
        if (file.exists()) {
            if (!file.canWrite()) {
                return false;
            }
        } else {
            try {
                String parentDir = file.getParent();
                if (parentDir != null) {
                    File outputDir = new File(file.getParent());
                    if (outputDir.exists() == false) {
                        outputDir.mkdirs();
                    } else {
                        if (outputDir.isDirectory() == false) {
                            outputDir.mkdirs();
                        }
                    }
                }
                file.createNewFile();
            } catch (IOException ioe) {
                return false;
            }
        }
        return true;
    }

    public static String nameWithoutExtension(String fn) {
        if (fn == null) {
            return null;
        }
        int i = fn.lastIndexOf('.');
        if (i > 0) {
            return fn.substring(0, i);
        }
        return fn;
    }

    public static String getExtension(String fn) {
        if (fn == null) {
            return "";
        }
        int i = fn.lastIndexOf('.');
        if (i > 0) {
            return fn.substring(i);
        }
        return "";
    }

    /**
     * @param inputStream
     * @param out
     * @return bytes transferred. O = error, -1 = all bytes has been transferred, other = bytes transferred before
     *         interruption
     */
    public static int writeFile(URL url, File outFilename) {
        InputStream input;
        try {
            input = url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(outFilename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
        return writeFile(input, outputStream);
    }

    /**
     * @param inputStream
     * @param out
     * @return bytes transferred. O = error, -1 = all bytes has been transferred, other = bytes transferred before
     *         interruption
     */
    public static int writeFile(InputStream inputStream, OutputStream out) {
        if (inputStream == null && out == null) {
            return 0;
        }
        try {
            byte[] buf = new byte[4096];
            int offset;
            while ((offset = inputStream.read(buf)) > 0) {
                out.write(buf, 0, offset);
            }
            return -1;
        } catch (InterruptedIOException e) {
            return e.bytesTransferred;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        } finally {
            FileUtil.safeClose(inputStream);
            FileUtil.safeClose(out);
        }
    }

    public static String formatSize(double size) {
        if (size >= GB) {
            return df.format(size / GB) + Messages.getString("FileUtil.gb");
        }
        if (size >= MB) {
            return df.format(size / MB) + Messages.getString("FileUtil.mb");
        }
        if (size >= KB) {
            return df.format(size / KB) + Messages.getString("FileUtil.kb");
        }
        return (int) size + Messages.getString("FileUtil.bytes");
    }

    public static boolean nioWriteFile(FileInputStream inputStream, FileOutputStream out) {
        if (inputStream == null && out == null) {
            return false;
        }
        try {
            FileChannel fci = inputStream.getChannel();
            FileChannel fco = out.getChannel();
            fco.transferFrom(fci, 0, fci.size());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            FileUtil.safeClose(inputStream);
            FileUtil.safeClose(out);
        }
    }

    public static boolean nioWriteFile(InputStream in, OutputStream out, final int bufferSize) {
        if (in == null && out == null) {
            return false;
        }
        try {
            ReadableByteChannel readChannel = Channels.newChannel(in);
            WritableByteChannel writeChannel = Channels.newChannel(out);
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            while (readChannel.read(buffer) != -1) {
                buffer.flip();
                writeChannel.write(buffer);
                buffer.clear();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            FileUtil.safeClose(in);
            FileUtil.safeClose(out);
        }
    }

    public static Properties readProperties(File propsFile, Properties props) {
        Properties p = props == null ? new Properties() : props;
        if (propsFile != null && propsFile.canRead()) {
            FileInputStream fileStream = null;
            try {
                fileStream = new FileInputStream(propsFile);
                p.load(fileStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                FileUtil.safeClose(fileStream);
            }
        }
        return p;
    }

    public static void storeProperties(File propsFile, Properties props, String comments) {
        if (props != null && propsFile != null) {
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(propsFile);
                props.store(fout, comments);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                FileUtil.safeClose(fout);
            }
        }
    }
}
