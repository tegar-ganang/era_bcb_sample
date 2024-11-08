package com.werno.wmflib.records;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.werno.wmflib.WMFConstants;

/**
 * This class implements the textout record
 *
 * @author Peter Werno
 */
public class TextOut extends ShapeRecord implements Record {

    /** Properties */
    short x;

    short y;

    String text;

    /**
     * Creates a new (blank) instance of TextOut. The properties can then be set
     * via the read-method or the property setters.
     */
    public TextOut() {
        this.x = 0;
        this.y = 0;
        this.text = "";
    }

    /**
     * Creates a new instance of TextOut
     *
     * @param x (short) the x coordinate
     * @param y (short) the y coordinate
     * @param text (String) the text to show
     */
    public TextOut(short x, short y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
    }

    /**
     * Creates a new instance of TextOut and reads all properties from a stream
     *
     * @param in (InputStream) the stream
     * @throws IOException
     */
    public TextOut(InputStream in) throws IOException {
        this.read(in);
    }

    /**
     * Setter for property x
     *
     * @param x (short) new value of the property x
     */
    public void setX(short x) {
        this.x = x;
    }

    /**
     * Getter for property x
     *
     * @return the value of the property x (short)
     */
    public short getX() {
        return this.x;
    }

    /**
     * Setter for property y
     *
     * @param y (short) new value of the property y
     */
    public void setY(short y) {
        this.y = y;
    }

    /**
     * Getter for property y
     *
     * @return the value of the property y (short)
     */
    public short getY() {
        return this.y;
    }

    /**
     * Setter for the property text
     *
     * @param text (String) new value of the property text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Getter for the property text
     *
     * @return the value of the property text (String)
     */
    public String getText() {
        return this.text;
    }

    /**
     * Writes the textout record to a stream
     *
     * @param out (OutputStream) the stream
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException {
        int size = this.getSize();
        byte[] content = this.text.getBytes();
        short stringLength = (short) content.length;
        WMFConstants.writeLittleEndian(out, size);
        WMFConstants.writeLittleEndian(out, WMFConstants.WMF_RECORD_TEXTOUT);
        WMFConstants.writeLittleEndian(out, stringLength);
        out.write(content);
        if ((stringLength % 2) == 1) out.write(' ');
        WMFConstants.writeLittleEndian(out, y);
        WMFConstants.writeLittleEndian(out, x);
    }

    /**
     * Reads the textout record from a stream
     *
     * @param in (InputStream) the stream
     * @throws IOException
     */
    public void read(InputStream in) throws IOException {
        short strLen = WMFConstants.readLittleEndianShort(in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < strLen; i++) baos.write(in.read());
        if ((strLen % 2) == 1) in.read();
        this.text = new String(baos.toByteArray());
        this.y = WMFConstants.readLittleEndianShort(in);
        this.x = WMFConstants.readLittleEndianShort(in);
    }

    /**
     * Returns the size of the record
     *
     * @return the size (short)
     */
    public short getSize() {
        return (short) (6 + (this.text.getBytes().length + 1) / 2);
    }

    /**
     * Returns the content of the textout record in a human readable format
     *
     * @return the content (String)
     */
    @Override
    public String toString() {
        return "TextOut (" + this.x + "," + this.y + "): \"" + text + "\"";
    }
}
