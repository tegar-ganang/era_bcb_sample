package com.werno.wmflib.records;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import com.werno.wmflib.WMFConstants;
import com.werno.wmflib.records.objects.Rect;

/**
 * This class writes some text to the WMF
 *
 * @author Peter Werno
 */
public class ExtTextOut extends ShapeRecord implements Record {

    /** Definitions for the option flags */
    public static final short OPAQUE = 0x0002;

    public static final short CLIPPED = 0x0004;

    public static final short GLYPH_INDEX = 0x0010;

    public static final short RTLREADING = 0x0080;

    public static final short NUMERICSLOCAL = 0x0400;

    public static final short NUMERICSLATIN = 0x0800;

    public static final short PDY = 0x2000;

    /** Properties */
    short x;

    short y;

    String text;

    short options;

    Rect rectangle;

    short[] dx;

    /**
     * Creates a new (blank) instance of ExtTextOut. The properties can then be
     * set via the read-method or via the property setters.
     */
    public ExtTextOut() {
        this.x = 0;
        this.y = 0;
        this.text = "";
        this.options = 0;
        this.rectangle = new Rect((short) 0, (short) 0, (short) 0, (short) 0);
        this.dx = null;
    }

    /**
     * Creates a new instance of ExtTextOut with position and text preset
     *
     * @param x (short) the x coordinate
     * @param y (short) the y coordinate
     * @param text (String) the text
     */
    public ExtTextOut(short x, short y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.options = 0;
        this.rectangle = new Rect(x, y, x, y);
        this.dx = null;
    }

    /**
     * Creates a new instance of ExtTextOut and reads all properties from
     * a stream
     *
     * @param in (InputStream) the stream
     * @throws IOException
     */
    public ExtTextOut(InputStream in) throws IOException {
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
     * Setter for the property options
     *
     * @param options (short) new value of the property options
     */
    public void setOptions(short options) {
        this.options = options;
    }

    /**
     * Getter for the property options
     *
     * @return the value of the property options (short)
     */
    public short getOptions() {
        return this.options;
    }

    /**
     * Setter for property rectangle
     *
     * @param rectangle (Rect) new value of the property rectangle
     */
    public void setRectangle(Rect rectangle) {
        this.rectangle = rectangle;
    }

    /**
     * Getter for property rectangle
     * 
     * @return the value of the property rectangle (Rect)
     */
    public Rect getRectangle() {
        return this.rectangle;
    }

    /**
     * Setter for the property dx
     * 
     * @param dx (short[]) new value of the property dx
     */
    public void setDx(short[] dx) {
        this.dx = dx;
    }

    /**
     * Getter for the property dx
     *
     * @return the value of the property dx (short[])
     */
    public short[] getDx() {
        return this.dx;
    }

    /**
     * Writes the exttextout record to a stream
     *
     * @param out (OutputStream) the stream
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException {
        int size = this.getSize();
        int strLen = this.text.getBytes().length;
        WMFConstants.writeLittleEndian(out, size);
        WMFConstants.writeLittleEndian(out, WMFConstants.WMF_RECORD_EXTTEXTOUT);
        WMFConstants.writeLittleEndian(out, y);
        WMFConstants.writeLittleEndian(out, x);
        WMFConstants.writeLittleEndian(out, (short) strLen);
        WMFConstants.writeLittleEndian(out, options);
        if ((options & (OPAQUE | CLIPPED)) != 0) this.rectangle.write(out);
        out.write(text.getBytes());
        if ((strLen % 2) == 1) out.write(0);
        if (dx != null) {
            int max = dx.length;
            if (max > strLen) max = strLen;
            for (int i = 0; i < max; i++) WMFConstants.writeLittleEndian(out, dx[i]);
            for (int i = max; i < strLen; i++) {
                out.write(0);
                out.write(0);
            }
        }
    }

    /**
     * Read the exttextout record from a stream
     *
     * @param in (InputStream) the stream
     * @throws IOException
     */
    public void read(InputStream in) throws IOException {
        this.y = WMFConstants.readLittleEndianShort(in);
        this.x = WMFConstants.readLittleEndianShort(in);
        int strLen = WMFConstants.readLittleEndianShort(in);
        this.options = WMFConstants.readLittleEndianShort(in);
        if ((this.options & (OPAQUE | CLIPPED)) != 0) {
            this.rectangle = new Rect(in);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < strLen; i++) {
            baos.write(in.read());
        }
        this.text = new String(baos.toByteArray());
        ArrayList<Short> rdDx = new ArrayList<Short>();
        try {
            short dxVal = WMFConstants.readLittleEndianShort(in);
            rdDx.add(new Short(dxVal));
        } catch (IOException ioe) {
        }
        int size = rdDx.size();
        if (size > 0) {
            this.dx = new short[size];
            for (int i = 0; i < size; i++) this.dx[i] = rdDx.get(i).shortValue();
        } else this.dx = null;
    }

    /**
     * Returns the size of the record
     *
     * @return the size (short)
     */
    public short getSize() {
        int strLen = text.getBytes().length;
        int dxLen = 0;
        int optRect = 0;
        if (dx != null) dxLen = dx.length;
        if ((dxLen != 0) && (dxLen < strLen)) dxLen = strLen;
        if (dxLen > strLen) dxLen = strLen;
        if ((options & (OPAQUE | CLIPPED)) != 0) optRect = 4;
        return (short) (7 + optRect + (strLen + 1) / 2 + dxLen);
    }

    /**
     * Returns the content of the exttextout record in human readable format
     *
     * @return the content (String)
     */
    @Override
    public String toString() {
        return "ExtTextOut (" + this.x + "," + this.y + ") \"" + this.text + "\"";
    }
}
