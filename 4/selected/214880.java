package com.dcivision.dms.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.dcivision.dms.client.parser.IndexParser;

public class DmsDocumentAutoOCR extends Thread {

    public static final String REVISION = "$Revision: 1.3.2.1 $";

    private static final Log log = LogFactory.getLog(DmsDocumentAutoOCR.class);

    private static Object lock = new Object();

    private Hashtable hConfTable = null;

    private static java.util.Properties props = new java.util.Properties();

    private static final String separator = ".";

    public DmsDocumentAutoOCR(Hashtable confTable, int pos) {
        hConfTable = confTable;
    }

    public void run() {
        log.debug("DMS Auto OCR Client Running...");
        String strList[][] = null;
        String timeInterval = (String) hConfTable.get(IndexParser.INTERVAL);
        if (timeInterval != null) {
            Integer interval = new Integer(timeInterval.trim());
            int sleepingInterval = interval.intValue() * 1000;
            do synchronized (lock) {
                try {
                    String sourceDir = (String) hConfTable.get(IndexParser.OCR_SOURCE_DIR);
                    File sourceDirectory = new File(sourceDir);
                    File listFileSource[] = sourceDirectory.listFiles();
                    if (listFileSource != null) {
                        if (listFileSource.length > 0) {
                            log.debug(listFileSource.length + " new files detected, trying to pass them to OCR engine");
                            for (int i = 0; i < listFileSource.length; i++) {
                                if (listFileSource[i].isFile()) {
                                    uploadFile(listFileSource[i]);
                                }
                            }
                        }
                    }
                    Thread.sleep(sleepingInterval);
                } catch (InterruptedException ie) {
                    log.error(ie, ie);
                } catch (Exception e) {
                    log.error(e, e);
                }
            } while (true);
        }
    }

    public void uploadFile(File inputFile) throws IOException {
        String fileName = inputFile.getName();
        String xmlFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".xml";
        String ocrIndexTemplate = (String) hConfTable.get(IndexParser.OCR_INDEX_TEMPLATE);
        String destinationDir = (String) hConfTable.get(IndexParser.OCR_DESTINATION_DIR);
        moveFile(inputFile, destinationDir);
        copyFile(new File(ocrIndexTemplate), new File(destinationDir + File.separator + xmlFileName));
    }

    public void moveFile(File src, String destinationDir) {
        String fileName = "";
        try {
            fileName = src.getName();
            src.renameTo(new File(destinationDir + File.separator + fileName));
            src.delete();
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public static void copyFile(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            log.error(e, e);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    public static void main(String args[]) throws Exception {
        Hashtable[] aryConfTable = configInit();
        int numInstant = aryConfTable.length;
        DmsDocumentAutoOCR[] cat = new DmsDocumentAutoOCR[numInstant];
        for (int i = 0; i < numInstant; i++) {
            Hashtable tmpTable = aryConfTable[i];
            cat[i] = new DmsDocumentAutoOCR(tmpTable, i);
            cat[i].start();
            Thread.sleep(25);
        }
    }

    /** configInit method
  * @return Value of total number of locations
  */
    public static Hashtable[] configInit() throws Exception {
        File config = new File(System.getProperty("DMS_HOME") + "/autoocr.conf");
        java.io.FileInputStream fis = new java.io.FileInputStream(config);
        props.load(fis);
        Integer numInstant = new Integer(props.getProperty(IndexParser.MULTIPLE_INSTANCE));
        Hashtable[] tmpConfTable = new Hashtable[numInstant.intValue()];
        for (int i = 0; i < numInstant.intValue(); i++) {
            Hashtable tmpTable = new Hashtable();
            tmpTable.put(IndexParser.INTERVAL, getConfigProperty(IndexParser.INTERVAL, i));
            tmpTable.put(IndexParser.OCR_SOURCE_DIR, getConfigProperty(IndexParser.OCR_SOURCE_DIR, i));
            tmpTable.put(IndexParser.OCR_DESTINATION_DIR, getConfigProperty(IndexParser.OCR_DESTINATION_DIR, i));
            tmpTable.put(IndexParser.OCR_INDEX_TEMPLATE, getConfigProperty(IndexParser.OCR_INDEX_TEMPLATE, i));
            tmpConfTable[i] = tmpTable;
        }
        return tmpConfTable;
    }

    protected static String getConfigProperty(String property, int pos) {
        String value = "";
        String pro = property.concat(separator).concat(new Integer(++pos).toString());
        value = props.getProperty(pro);
        if (value == null || "".equals(value)) {
            value = props.getProperty(property);
        }
        return value;
    }
}
