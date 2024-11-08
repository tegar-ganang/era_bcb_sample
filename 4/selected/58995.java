package org.marcont.rdf.utils;

import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.channels.*;
import com.skruk.elvis.admin.i18n.ResourceFormatter;
import com.skruk.elvis.admin.manage.marc21.*;

/**
 * @author marsyn
 * Wrapper class for parsing binary MARC21 files.
 * Allows getting parsed MARC21 file in MARC-XML format
 * or in formatted 'table'.
 */
public class Marc21Parser {

    /**
	 * input marc21 filename
	 */
    private String fileName;

    /**
	 * input marc21 uri
	 */
    @SuppressWarnings("unused")
    private URI fileUri;

    /**
	 * table for holding binary marc21 file contents
	 */
    private byte[] bmarc = null;

    boolean needsFileReading = false;

    /**
	 * marc21 in 'table' format
	 */
    private String marc = null;

    /**
	 * marc21 in marc-xml format
	 */
    private String marcxml = null;

    /**
	 * resource id put in marc-xml as jeromedl:id
	 * while parsing
	 */
    private String resourceId = null;

    /**
	 * default resource formatter
	 */
    private static ResourceFormatter rf = new ResourceFormatter("com.skruk.elvis.admin.i18n.ManageMarc21");

    /**
	 * line width for 'table' format
	 */
    private static final int TEXT_WIDTH = 48;

    /**
	 * Default constructor.
	 */
    public Marc21Parser() {
    }

    /**
	 * Constructor with default input filename setting.
	 * @param name input marc21 filename
	 */
    public Marc21Parser(String name) {
        fileName = name;
    }

    /**
	 * Constructor with default input uri setting
	 * @param uri input uri
	 */
    public Marc21Parser(URI uri) {
        fileUri = uri;
    }

    /**
	 * Getter for input filename
	 * @return input filename
	 */
    public String getFileName() {
        return fileName;
    }

    /**
	 * Setter for input filename
	 * @param name input filename
	 */
    public void setFileName(String name) {
        fileName = name;
        needsFileReading = true;
    }

    public void setMarcContents(byte[] table) {
        needsFileReading = false;
        bmarc = new byte[table.length];
        int i = 0;
        while (i < table.length) {
            bmarc[i] = table[i];
            i++;
        }
        bmarc = table;
    }

    /**
	 * Setter for resource id
	 * @param id id value
	 */
    public void setResourceId(String id) {
        resourceId = id;
    }

    /**
	 * Getter for resource id
	 * @return id
	 */
    public String getResourceId() {
        return resourceId;
    }

    /**
	 * Performs reading from binary MARC21 file.
	 */
    protected void readBinaryMarcFromFile() {
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            FileChannel fch = fis.getChannel();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) f.length());
            WritableByteChannel wbc = Channels.newChannel(baos);
            long pos = 0;
            long cnt = 0;
            while ((cnt = fch.transferTo(pos, f.length(), wbc)) > 0) {
                pos += cnt;
            }
            fis.close();
            this.bmarc = baos.toByteArray();
        } catch (FileNotFoundException fnfex) {
            fnfex.printStackTrace();
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
    }

    /**
	 * Performs processing binary MARC21 stored in bmarc[].
	 */
    protected void processBinaryMarc() {
        Marc21Description description = new Marc21Description();
        description.setLeader(this.extract(0, 24, "bold"));
        description.setLength(Integer.valueOf(this.extract(0, 5, "green")).intValue());
        description.setDataStart(Integer.valueOf(this.extract(12, 5, "green")).intValue());
        int dstart = description.getDataStart();
        int i = 24;
        while (i < dstart - 1) {
            Marc21Field field;
            int tag = Integer.valueOf(this.extract(i, 3, "red")).intValue();
            if (tag < 10) {
                field = new Marc21Controlfield();
            } else {
                field = new Marc21Datafield();
            }
            field.setTag(tag);
            field.setLength(Integer.valueOf(this.extract(i + 3, 4, null)).intValue());
            field.setStart(Integer.valueOf(this.extract(i + 7, 5, null)).intValue());
            description.addField(field);
            i += 12;
        }
        Iterator<Marc21Field> it = description.fieldsIterator();
        while (it.hasNext()) {
            Marc21Field f = it.next();
            int start = f.getStart();
            int length = f.getLength();
            int shft = (int) Math.floor((double) (start + dstart) / TEXT_WIDTH);
            try {
                if (this.bmarc[dstart + start + length - 1] == '') {
                    f.setText(new String(this.bmarc, dstart + start, length - 1, "UTF-8"));
                } else {
                    f.setText(new String(this.bmarc, dstart + start, length, "UTF-8"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.extract(dstart + start + shft, 1, "blue");
            if (f instanceof Marc21Datafield) {
                Marc21Datafield df = (Marc21Datafield) f;
                String ptr = new String(this.bmarc, dstart + start, 2);
                df.setPtr1(ptr.charAt(0));
                df.setPtr2(ptr.charAt(1));
                String[] sbfields = f.getText().split("");
                for (int j = 1; j < sbfields.length; j++) {
                    char key = sbfields[j].charAt(0);
                    int len = sbfields[j].length();
                    df.putSubfield(key, sbfields[j].substring(1, len));
                }
            }
        }
        this.marc = description.createMarc();
        this.marcxml = description.createXml(resourceId);
    }

    /**
	 * Peeforms actual parsing - a 'go!' method.
	 */
    public void parse() {
        if (needsFileReading) readBinaryMarcFromFile();
        processBinaryMarc();
    }

    /**
	 * Returns contents of parsed MARC21 in MARC-XML format
	 * @return marcxml contents
	 */
    public String getMarcXml() {
        return marcxml;
    }

    /**
	 * Returns contents of parsed MARC21 in 'table' format
	 * @return marc contents
	 */
    public String getMarc() {
        return marc;
    }

    /**
	 * Returns default resource formatter
	 * @return resource formatter
	 */
    public static ResourceFormatter getFormatter() {
        return rf;
    }

    /**
	 * Helper method for extracting bytes from bmarc[]
	 * @param offset indicates beginning of extracting bytes
	 * @param length indicates how many bytes to extract
	 * @param style not used
	 * @return extracted bytes as a String
	 */
    protected String extract(int offset, int length, String style) {
        StringBuffer result = new StringBuffer();
        int i = length;
        while (i > 0) {
            result.append((char) bmarc[offset + length - i]);
            i--;
        }
        return result.toString();
    }
}
