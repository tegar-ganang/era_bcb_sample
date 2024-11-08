package nl.joppla.ejb.entity.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import nl.joppla.ejb.entity.document.StyledDocument;
import oracle.apps.xdo.XDOException;
import oracle.apps.xdo.common.pdf.util.PDFDocMerger;
import oracle.apps.xdo.template.pdf.book.PDFOverlay;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

public class DocUtils {

    private static Logger logger = Logger.getLogger(DocUtils.class);

    public DocUtils() {
    }

    /**
     * Test method for writing files to a arbitrary location (C:/java/debug)
     * 
     * @param os outputstream
     * @param extension file extension
     */
    public void writeOutputStream(OutputStream os, String extension) {
        try {
            File file = new File("c:/java/debug/file" + RandomStringUtils.randomAlphabetic(3) + extension);
            file.createNewFile();
            ((ByteArrayOutputStream) os).toByteArray();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(((ByteArrayOutputStream) os).toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Test method for writing files to a arbitrary location (C:/java/debug)
     * 
     * @param is inputStream
     * @param extension file extension
     */
    public void writeInputStream(InputStream is, String extension) {
        try {
            FileOutputStream fos = new FileOutputStream("c:/java/debug/file" + RandomStringUtils.randomAlphabetic(3) + extension);
            byte[] buf = new byte[256];
            int read = 0;
            while ((read = is.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Test method for writing files to a arbitrary location (C:/java/debug)
     * 
     * @param filename filename
     * @param content char[]
     */
    public void writeContent(String filename, char[] content) {
        try {
            File file = new File("c:/java/debug/" + filename);
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(new String(content).getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Overlay different streams
     * @param mainDoc the main document
     * @param overlay the overlay document
     * @return the overlaid outputstream
     */
    public OutputStream overlayStreams(OutputStream mainDoc, OutputStream overlay) {
        ByteArrayOutputStream merged = new ByteArrayOutputStream();
        try {
            InputStream mainDocIn = this.toInputStream(mainDoc);
            InputStream overlayIn = this.toInputStream(overlay);
            PDFOverlay weaver = new PDFOverlay(mainDocIn, overlayIn, merged);
            weaver.setInterweave(true);
            weaver.process();
        } catch (IOException ioe) {
            logger.error(ioe);
        } catch (XDOException xdoe) {
            logger.error(xdoe);
        } catch (Throwable e) {
            logger.error(e);
        } finally {
            if (merged.size() == 0) {
                merged = (ByteArrayOutputStream) mainDoc;
            }
        }
        return merged;
    }

    /**
     * Merges OutputStreams
     * 
     * @param docList list containing outputstreams
     * @return singe outputstream
     */
    public OutputStream mergeStreams(List<StyledDocument> docList) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream[] streams = new InputStream[docList.size()];
        for (int i = 0; i < streams.length; i++) {
            streams[i] = toInputStream(docList.get(i).getStream());
        }
        PDFDocMerger merger = new PDFDocMerger(streams, baos);
        merger.setConfig(PropertyHandler.getPDFMergerProperties());
        try {
            merger.process();
        } catch (XDOException xdoe) {
            logger.error(xdoe);
        }
        return baos;
    }

    /**
     * Merges byte[]]
     * @param docList list containing outputstreams
     * @return singe inputstream
     */
    public InputStream mergeByteArrStreams(List<StyledDocument> docList) {
        OutputStream out = null;
        ByteArrayOutputStream baos = null;
        byte[] arr = null;
        byte[] total = new byte[0];
        for (StyledDocument document : docList) {
            if (document != null) {
                out = document.getStream();
                baos = (ByteArrayOutputStream) out;
                if (baos != null) {
                    arr = baos.toByteArray();
                    if (total.length == 0) {
                        total = arr;
                    } else {
                        byte[] temp = new byte[total.length + arr.length];
                        System.arraycopy(total, 0, temp, 0, total.length);
                        System.arraycopy(arr, 0, temp, total.length, arr.length);
                        total = temp;
                        temp = null;
                    }
                }
            }
            System.out.println("************* lengte : " + total.length + "*****************");
        }
        return new ByteArrayInputStream(total);
    }

    /**
     * Convert ByteArrayOutputStream to ByteArrayInputStream
     * @param out ByteArrayOupptuStream
     * @return ButeArrayInputStream
     */
    public InputStream toInputStream(OutputStream out) {
        if (out != null) {
            return new ByteArrayInputStream(((ByteArrayOutputStream) out).toByteArray());
        }
        return new ByteArrayInputStream(new byte[0]);
    }

    /**
     * Gets a ByteArrayOutputstream based on a char[]
     * The method relies on two temporal objects, an outputstream and a string.
     * Therefore the size of this char[] is limited.
     * 
     * @param content the char[] to transfrom to
     * 
     * @return an inputstream containing the content
     * 
     * @throws IOException on out writer error
     */
    public InputStream getInputstream(char[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new String(content).getBytes("ISO-8859-1"));
        return new ByteArrayInputStream(out.toByteArray());
    }
}
