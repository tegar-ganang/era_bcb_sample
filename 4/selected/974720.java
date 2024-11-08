package interkinetic.servlet;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Holds information about an HTML multipart form response field.
 * 
 * @version $Revision: 1.2 $ $State: Exp $
 * @author  $Author: dlkinney $
 */
public class MultipartFileInfo implements Comparable {

    private String name = null;

    private String contentType = null;

    private String filename = null;

    private byte[] data = new byte[0];

    private MultipartFileInfo(String name, String contentType, String filename) {
        super();
        this.name = name;
        this.contentType = contentType;
        this.filename = filename;
    }

    public MultipartFileInfo(String name, String contentType, String filename, InputStream in) throws IOException {
        this(name, contentType, filename);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read = in.read();
        while (read >= 0) {
            out.write((byte) read);
            read = in.read();
        }
        this.data = out.toByteArray();
    }

    public MultipartFileInfo(String name, String contentType, String filename, InputStream in, int length) throws IOException {
        this(name, contentType, filename);
        this.data = new byte[length];
        int index = 0;
        int read = in.read();
        while (read >= 0 && index < length) {
            data[index] = (byte) read;
            read = in.read();
            ++index;
        }
    }

    /** copies the array */
    public MultipartFileInfo(String name, String contentType, String filename, byte[] in) throws IOException {
        this(name, contentType, filename);
        this.data = new byte[in.length];
        for (int i = 0; i < in.length; ++i) {
            data[i] = in[i];
        }
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }

    public int getLength() {
        return data.length;
    }

    public InputStream getFile() {
        return new ByteArrayInputStream(data);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        sb.append("Name=" + name);
        sb.append(";");
        sb.append("Content-Type=" + contentType);
        sb.append(";");
        sb.append("Filename=" + filename);
        sb.append(";");
        sb.append("Length=" + data.length);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Provides "natural ordering" that is <i>consistent with equals</i>.  (See 
     * <code>java.lang.Comparable</code> and <code>java.util.Comparator</code> 
     * for the meaning of "natural ordering".)
     * <p>
     * The <code>MultipartFileInfo</code> class is only comparable with itself; 
     * attempts to compare it with other objects results in throwing a 
     * <code>ClassCastException</code>.  
     * <p>
     * First, the <code>getName</code> values are compared.  If they are the 
     * same, the files are byte-wise compared (byte values being ranged from 
     * 0 - 255) from beginning to end.  If no inequality is found yet, then the 
     * objects are considered equal.  
     * <p>
     * In all comparisons, if one value has not been set (which is to say, the 
     * value is <code>null</code>), that value is considered 
     * <i>greater-than</i> the other value.  Two <code>null</code> values are 
     * considered equal.  The purpose of this is to force incompete classes to 
     * the end of an ordered listing.
     * <p>
     * 
     * 
     * @see     java.lang.Comparable
     * @see     java.util.Comparator
     */
    public int compareTo(Object o) throws ClassCastException {
        MultipartFileInfo that = (MultipartFileInfo) o;
        int minLength = (this.data.length < that.data.length) ? this.data.length : that.data.length;
        int index = 0;
        int byteComp = 0;
        int nameComp = 0;
        if (that == null) {
            return -1;
        }
        if (this.name != null && that.name != null) {
            nameComp = this.name.compareTo(that.name);
        } else if (this.name == null && that.name == null) {
            nameComp = 0;
        } else if (this.name == null) {
            nameComp = 1;
        } else if (that.name == null) {
            nameComp = -1;
        }
        if (nameComp != 0) {
            return nameComp;
        }
        while (index < minLength) {
            byteComp = this.data[index] - that.data[index];
            if (byteComp != 0) {
                return byteComp;
            }
            ++index;
        }
        if (this.data.length < that.data.length) {
            return -1;
        }
        if (this.data.length > that.data.length) {
            return 1;
        }
        return 0;
    }

    /** 
     * Consistent with compareTo
     */
    public boolean equals(Object o) {
        MultipartFileInfo that = null;
        if (o == null) {
            return false;
        }
        if (!(o instanceof MultipartFileInfo)) {
            return false;
        }
        that = (MultipartFileInfo) o;
        if (!(this.data.length == that.data.length)) {
            return false;
        }
        if (this.name != null) {
            if (!this.name.equals(that.name)) {
                return false;
            }
        } else {
            if (that.name != null) {
                return false;
            }
        }
        for (int i = 0; i < this.data.length; ++i) {
            if (this.data[i] != that.data[i]) {
                return false;
            }
        }
        return true;
    }
}
