package org.jpedal.io;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import org.jpedal.exception.PdfException;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_boolean;

/**
 * provides access to the file using Random access class to
 * read bytes and strings from a pdf file. Pdf file is a mix of
 * character and binary streams
 */
public class PdfFileReader {

    /**list of cached objects to delete*/
    protected Map cachedObjects = new HashMap();

    boolean isFDF = false;

    protected RandomAccessBuffer pdf_datafile = null;

    /**currentGeneration used by decryption*/
    protected int currentGeneration = 0;

    /**file length*/
    protected long eof = 0;

    /**location from the reference table of each
	 * object in the file
	 */
    protected Vector_Int offset = new Vector_Int(2000);

    /**flag to show if compressed*/
    protected Vector_boolean isCompressed = new Vector_boolean(2000);

    /**generation of each object*/
    private Vector_Int generation = new Vector_Int(2000);

    /**
	 * version of move pointer which takes object name
	 * and converts before calling main routine
	 */
    protected final long movePointer(String pages_id) {
        long pointer = getOffset(pages_id);
        return movePointer(pointer);
    }

    /**
	 * get pdf type in file (found at start of file)
	 */
    public final String getType() {
        String pdf_type = "";
        try {
            movePointer(0);
            pdf_type = pdf_datafile.readLine();
            int pos = pdf_type.indexOf("%PDF");
            if (pos != -1) pdf_type = pdf_type.substring(pos + 5);
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " in reading type");
        }
        return pdf_type;
    }

    /**
	 * open pdf file<br> Only files allowed (not http)
	 * so we can handle Random Access of pdf
	 */
    public final void openPdfFile(String filename) throws PdfException {
        isFDF = filename.toLowerCase().endsWith(".fdf");
        try {
            pdf_datafile = new RandomAccessFileBuffer(filename, "r");
            eof = pdf_datafile.length();
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " accessing file");
            throw new PdfException("Exception " + e + " accessing file");
        }
    }

    /**
		 * open pdf file using a byte stream
		 */
    public final void openPdfFile(byte[] data) throws PdfException {
        try {
            pdf_datafile = new RandomAccessDataBuffer(data);
            eof = pdf_datafile.length();
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " accessing file");
            throw new PdfException("Exception " + e + " accessing file");
        }
        LogWriter.writeMethod("{openPdfFile} EOF=" + eof, 0);
    }

    /**
	 * returns current location pointer and sets to new value
	 */
    protected final long movePointer(long pointer) {
        long old_pointer = 0;
        try {
            if (pointer > pdf_datafile.length()) {
                LogWriter.writeLog("Attempting to access ref outside file");
            } else {
                old_pointer = getPointer();
                pdf_datafile.seek(pointer);
            }
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " moving pointer to  " + pointer + " in file. EOF =" + eof);
        }
        return old_pointer;
    }

    /**
	 * gets pointer to current location in the file
	 */
    protected final long getPointer() {
        long old_pointer = 0;
        try {
            old_pointer = pdf_datafile.getFilePointer();
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " getting pointer in file");
        }
        return old_pointer;
    }

    /**
	 * close the file
	 */
    public final void closePdfFile() {
        try {
            if (pdf_datafile != null) {
                pdf_datafile.close();
                pdf_datafile = null;
            }
            if (cachedObjects != null) {
                Iterator files = cachedObjects.keySet().iterator();
                while (files.hasNext()) {
                    String fileName = (String) files.next();
                    File file = new File(fileName);
                    file.delete();
                    if (file.exists()) LogWriter.writeLog("Unable to delete temp file " + fileName);
                }
            }
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " closing file");
        }
    }

    /**
	 * place object details in queue
	 */
    protected final void storeObjectOffset(int current_number, int current_offset, int current_generation, boolean isEntryCompressed) {
        int existing_generation = 0;
        int offsetNumber = 0;
        if (current_number < generation.getCapacity()) {
            existing_generation = generation.elementAt(current_number);
            offsetNumber = offset.elementAt(current_number);
        }
        if ((existing_generation < current_generation) | (offsetNumber == 0)) {
            offset.setElementAt(current_offset, current_number);
            generation.setElementAt(current_generation, current_number);
            isCompressed.setElementAt(isEntryCompressed, current_number);
        } else {
        }
    }

    /**
	 * returns stream in which compressed object will be found
	 * (actually reuses getOffset internally)
	 */
    protected final int getCompressedStreamObject(String value) {
        int currentID = 0;
        if (value.endsWith("R") == true) {
            StringTokenizer values = new StringTokenizer(value);
            currentID = Integer.parseInt(values.nextToken());
            currentGeneration = Integer.parseInt(values.nextToken());
        } else LogWriter.writeLog("Error with reference .." + value);
        return offset.elementAt(currentID);
    }

    /**
	 * general routine to turn reference into id with object name
	 */
    protected final int getOffset(String value) {
        int currentID = 0;
        if (value.endsWith("R") == true) {
            StringTokenizer values = new StringTokenizer(value);
            currentID = Integer.parseInt(values.nextToken());
            currentGeneration = Integer.parseInt(values.nextToken());
        } else LogWriter.writeLog("Error with reference .." + value);
        return offset.elementAt(currentID);
    }

    /**
	 * returns where in compressed stream value can be found
	 * (actually reuses getGen internally)
	 */
    protected final int getOffsetInCompressedStream(String value) {
        int currentID = 0;
        if (value.endsWith("R") == true) {
            StringTokenizer values = new StringTokenizer(value);
            currentID = Integer.parseInt(values.nextToken());
            currentGeneration = Integer.parseInt(values.nextToken());
        } else LogWriter.writeLog("Error with reference .." + value);
        return generation.elementAt(currentID);
    }

    /**
	 * general routine to turn reference into id with object name
	 */
    protected final int getGen(String value) {
        int currentID = 0;
        if (value.endsWith("R") == true) {
            StringTokenizer values = new StringTokenizer(value);
            currentID = Integer.parseInt(values.nextToken());
            currentGeneration = Integer.parseInt(values.nextToken());
        } else LogWriter.writeLog("Error with reference .." + value);
        return generation.elementAt(currentID);
    }

    /**
	 * general routine to turn reference into id with object name
	 */
    protected final boolean isCompressed(String value) {
        int currentID = 0;
        if (value.endsWith("R") == true) {
            StringTokenizer values = new StringTokenizer(value);
            currentID = Integer.parseInt(values.nextToken());
            currentGeneration = Integer.parseInt(values.nextToken());
        } else LogWriter.writeLog("Error with reference .." + value);
        return isCompressed.elementAt(currentID);
    }
}
