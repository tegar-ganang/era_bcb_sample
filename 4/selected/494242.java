package net.wotonomy.foundation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
* A pure java implementation of NSData, which
* is basically a wrapper on a byte array.
*
* @author michael@mpowers.net
* @author $Author: cgruber $
* @version $Revision: 893 $
*/
public class NSData {

    public static final NSData EmptyData = new NSData();

    protected byte[] bytes;

    /**
    * Default constructor creates a zero-data object.
    */
    public NSData() {
        bytes = new byte[0];
    }

    /**
    * Creates an object containing a copy of the specified bytes.
    */
    public NSData(byte[] data) {
        this(data, 0, data.length);
    }

    /**
    * Creates an object containing a copy of the bytes from the specified 
    * array within the specified range.
    */
    public NSData(byte[] data, int start, int length) {
        bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = data[start + i];
        }
    }

    /**
    * Creates an object containing the bytes of the specified string.
    */
    public NSData(String aString) {
        this(aString.getBytes());
    }

    /**
    * Creates an object containing the contents of the specified file.
    * Errors reading the file will produce an empty or partially blank array.
    */
    public NSData(File aFile) {
        int len = (int) aFile.length();
        byte[] data = new byte[len];
        try {
            new java.io.FileInputStream(aFile).read(data);
        } catch (Exception exc) {
        }
        bytes = data;
    }

    /**
    * Creates an object containing the contents of the specified URL.
    */
    public NSData(java.net.URL aURL) {
        throw new RuntimeException("Not Implemented");
    }

    /**
    * Creates an object containing a copy of the contents of the 
    * specified NSData object.
    */
    public NSData(NSData aData) {
        this(aData.bytes());
    }

    /**
	 * Creates a new NSData object from the bytes in the input stream.
	 * The input stream is read fully and is not closed.
	 * @param stream The stream to read from.
	 * @param chunkSize The buffer size used to read from the stream.
	 * @throws IOException if the stream cannot be read from.
	 */
    public NSData(InputStream stream, int chunkSize) throws IOException {
        super();
        byte[] b = new byte[chunkSize];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int read = 0;
        do {
            read = stream.read(b);
            if (read > 0) bout.write(b, 0, read);
        } while (read > 0);
        bytes = bout.toByteArray();
    }

    /**
    * Returns the length of the contained data.
    */
    public int length() {
        return bytes.length;
    }

    /**
    * Returns whether the specified data is equivalent to these data.
    */
    public boolean isEqualToData(NSData aData) {
        if (length() != aData.length()) return false;
        byte[] a = bytes();
        byte[] b = aData.bytes();
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    /**
    * Return the bytes within the data that fall within the specified range.
    */
    public NSData subdataWithRange(NSRange aRange) {
        int loc = aRange.location();
        byte[] src = bytes();
        byte[] data = new byte[aRange.length()];
        System.arraycopy(src, loc, data, 0, data.length);
        return new NSData(data);
    }

    /**
    * Writes the contents of this data to the specified URL.
    * If atomically is true, then the data is written to a temporary
    * file and then renamed to the name specified by the URL when
    * the data transfer is complete.
    */
    public boolean writeToURL(java.net.URL aURL, boolean atomically) {
        throw new RuntimeException("Not Implemented");
    }

    /**
    * Convenience to return the contents of the specified file.
    */
    public static NSData dataWithContentsOfMappedFile(java.io.File aFile) {
        return new NSData(aFile);
    }

    /**
    * Returns a copy of the bytes starting at the specified location 
    * and ranging for the specified length.
    */
    public byte[] bytes(int location, int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = bytes[location + i];
        }
        return data;
    }

    /**
    * Returns a copy of the bytes backing this data object.
    * NOTE: This method is not in the NSData spec and is
    * included for convenience only.
    */
    public byte[] bytes() {
        return bytes(0, length());
    }

    public String toString() {
        String hex = "0123456789ABCDEF";
        StringBuffer buf = new StringBuffer();
        buf.append(NSPropertyListSerialization.TOKEN_BEGIN[NSPropertyListSerialization.PLIST_DATA]);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            buf.append(hex.charAt((b & 0xf0) >> 4));
            buf.append(hex.charAt(b & 0x0f));
            if (i % 5 == 4) buf.append(' ');
        }
        buf.append(NSPropertyListSerialization.TOKEN_END[NSPropertyListSerialization.PLIST_DATA]);
        return buf.toString();
    }

    public boolean isEqual(Object obj) {
        if (obj == this) return true;
        if (obj instanceof NSData) return isEqualToData((NSData) obj);
        return false;
    }
}
