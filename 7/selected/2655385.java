package de.sciss.fscape.io;

import java.io.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class ImageFile extends GenericFile {

    public static final String ERR_PARALLEL = "Colour planes not interleaved";

    public static final String ERR_COMPRESSED = "Unsupported compression";

    private boolean suckyPCdata = false;

    private boolean invert = false;

    private long stripOffsetOffs = 0L;

    private int stripOffsetSize;

    private int rowsPerStrip;

    private int numStrips;

    private int bytesPerRow;

    private int stripNum = -1;

    private int stripLeft = 0;

    private byte[] buf = null;

    private int bufSize;

    private int bufOffset;

    protected ImageStream stream = null;

    protected static final int NEWSUBTYPE_TAG = 0x00FE;

    protected static final int SUBTYPE_TAG = 0x00FF;

    protected static final int WIDTH_TAG = 0x0100;

    protected static final int HEIGHT_TAG = 0x0101;

    protected static final int BITSPERSMP_TAG = 0x0102;

    protected static final int CMPTYPE_TAG = 0x0103;

    protected static final int PHOTOMETR_TAG = 0x0106;

    protected static final int DESCR_TAG = 0x010E;

    protected static final int STRIPOFFS_TAG = 0x0111;

    protected static final int SMPPERPIXEL_TAG = 0x0115;

    protected static final int ROWSPERSTRIP_TAG = 0x0116;

    protected static final int STRIPCOUNT_TAG = 0x0117;

    protected static final int XRES_TAG = 0x011A;

    protected static final int YRES_TAG = 0x011B;

    protected static final int PLANE_TAG = 0x011C;

    protected static final int RESUNIT_TAG = 0x0128;

    protected static final int CMPTYPE_NONE = 0x0001;

    protected static final int CMPTYPE_LZW = 0x0005;

    protected static final int NEWSUBTYPE_THUMB = 0x0001;

    protected static final int NEWSUBTYPE_TRANSP = 0x0004;

    protected static final int SUBTYPE_THUMB = 0x0002;

    protected static final int PHOTOMETR_GRAYINV = 0x0000;

    protected static final int PHOTOMETR_GRAY = 0x0001;

    protected static final int PHOTOMETR_RGB = 0x0002;

    protected static final int PLANE_SERIAL = 0x0001;

    protected static final int PLANE_PARALLEL = 0x0002;

    protected static final int RESUNIT_NONE = ImageStream.RES_NONE;

    protected static final int RESUNIT_INCH = ImageStream.RES_INCH;

    protected static final int RESUNIT_CM = ImageStream.RES_CM;

    /**
	 *	Datei, die Imagedaten enthaelt bzw. enthalten soll, oeffnen
	 *
	 *	@param imageF	entsprechende Datei
	 *	@param mode		MODE_INPUT zum Lesen, MODE_OUTPUT zum Schreiben
	 */
    public ImageFile(File imageF, int mode) throws IOException {
        super(imageF, (mode & ~MODE_TYPEMASK) | MODE_TIFF);
    }

    public ImageFile(String fname, int mode) throws IOException {
        this(new File(fname), mode);
    }

    /**
	 *	erzeugt einen ImageStream, in den der Header der Datei
	 *	uebertragen wird
	 *	KEINE AENDERUNGEN AM SOUNDSTREAM VORNEHMEN!
	 */
    public ImageStream initReader() throws IOException {
        if (stream == null) {
            stream = new ImageStream();
            readHeader();
            bufSize = 131072;
            bufSize -= bufSize % bytesPerRow;
            buf = new byte[Math.max(bytesPerRow, bufSize)];
        }
        stripNum = -1;
        stripLeft = 0;
        bufOffset = bufSize;
        return stream;
    }

    /**
	 *	Meldet einen ImageStream zum Schreiben in das File an;
	 */
    public void initWriter(ImageStream strm) throws IOException {
        stream = strm;
        bytesPerRow = strm.width * strm.smpPerPixel * ((strm.bitsPerSmp + 7) >> 3);
        suckyPCdata = false;
        invert = false;
        rowsPerStrip = strm.height;
        numStrips = 1;
        bufSize = 131072;
        bufSize -= bufSize % bytesPerRow;
        buf = new byte[Math.max(bytesPerRow, bufSize)];
        bufOffset = 0;
        writeHeader();
    }

    /**
	 *	Erzeugt ein fuer diese Objekt zum Lesen/Schreiben geeignetes Array von Bytes
	 */
    public byte[] allocRow() {
        return new byte[stream.width * stream.smpPerPixel * ((stream.bitsPerSmp + 7) >> 3)];
    }

    protected void seekNewStrip() throws IOException {
        stripNum++;
        if (stripNum >= numStrips) throw new EOFException();
        if (stripOffsetSize == TIFFentry.LONG) {
            seek(stripOffsetOffs + (stripNum << 2));
            seek(readUniversalInt());
        } else {
            seek(stripOffsetOffs + stripNum << 1);
            seek(readUniversalUShort());
        }
        stripLeft = bytesPerRow * rowsPerStrip;
    }

    /**
	 *	Liest einen Zeile aus der Datei ein
	 *
	 *	@param	data	sollte mit allocRow() beschafft worden sein!
	 */
    public void readRow(byte[] data) throws IOException {
        int i, num;
        byte b;
        if (bufOffset >= bufSize) {
            if (stripLeft == 0) {
                seekNewStrip();
            }
            num = Math.min(bufSize, stripLeft);
            readFully(buf, bufSize - num, num);
            bufOffset = bufSize - num;
            stripLeft -= num;
            if (suckyPCdata && (stream.bitsPerSmp == 16)) {
                for (i = 0; i < bufSize; i += 2) {
                    b = data[i];
                    data[i] = data[i + 1];
                    data[i + 1] = b;
                }
            }
        }
        System.arraycopy(buf, bufOffset, data, 0, data.length);
        bufOffset += data.length;
        stream.rowsRead++;
        if (invert) {
            for (i = 0; i < data.length; i++) {
                data[i] = (byte) ~data[i];
            }
        }
    }

    /**
	 *	Schreibt einen Zeile in die Datei
	 *
	 *	@param	data	sollte mit allocRow() beschafft worden sein!
	 */
    public void writeRow(byte[] data) throws IOException {
        if (bufOffset >= bufSize) {
            write(buf);
            bufOffset = 0;
        }
        System.arraycopy(data, 0, buf, bufOffset, data.length);
        bufOffset += data.length;
        stream.rowsWritten++;
    }

    /**
	 *	Datei schliessen
	 */
    public void close() throws IOException {
        stream = null;
        int bufOffTmp = bufOffset;
        bufOffset = 0;
        byte bufTmp[] = buf;
        buf = null;
        if ((mode & MODE_FILEMASK) == MODE_OUTPUT) {
            if ((bufTmp != null) && (bufOffTmp > 0)) {
                write(bufTmp, 0, bufOffTmp);
            }
        }
        super.close();
    }

    /**
	 *	Format string besorgen
	 */
    public String getFormat() throws IOException {
        ImageStream tmpStream;
        tmpStream = stream;
        if (tmpStream == null) {
            tmpStream = initReader();
        }
        return (ImageStream.getFormat(tmpStream));
    }

    private void readHeader() throws IOException {
        int magic;
        long oldPos;
        int essentials;
        int offset;
        int i;
        int entries;
        boolean gray;
        TIFFentry entry = new TIFFentry();
        seek(0L);
        magic = readInt();
        switch(magic) {
            case TIFF_MAC_MAGIC:
                suckyPCdata = false;
                break;
            case TIFF_IBM_MAGIC:
                suckyPCdata = true;
                break;
            default:
                throw new UnsupportedEncodingException(ERR_ILLEGALFILE);
        }
        mode = (mode & ~MODE_TYPEMASK) | MODE_TIFF;
        do {
            offset = readUniversalInt();
            if (offset == 0) {
                throw new UnsupportedEncodingException(ERR_MISSINGDATA);
            }
            seek(offset);
            invert = false;
            gray = true;
            stream.hRes = 1.0f / 72.0f;
            stream.hRes = 1.0f / 72.0f;
            stream.resUnit = ImageStream.RES_INCH;
            stream.bitsPerSmp = 1;
            stream.smpPerPixel = 1;
            entries = readUniversalUShort();
            essentials = 5;
            IFD: for (i = 0; i < entries; i++) {
                readTIFFentry(entry);
                switch(entry.tag) {
                    case NEWSUBTYPE_TAG:
                        if ((entry.value == NEWSUBTYPE_THUMB) || (entry.value == NEWSUBTYPE_TRANSP)) {
                            break IFD;
                        }
                        break;
                    case SUBTYPE_TAG:
                        if (entry.value == SUBTYPE_THUMB) {
                            break IFD;
                        }
                        break;
                    case CMPTYPE_TAG:
                        if (entry.value != CMPTYPE_NONE) {
                            throw new UnsupportedEncodingException(ERR_COMPRESSED);
                        }
                        break;
                    case WIDTH_TAG:
                        essentials--;
                        stream.width = entry.value;
                        break;
                    case HEIGHT_TAG:
                        essentials--;
                        stream.height = entry.value;
                        break;
                    case XRES_TAG:
                        stream.hRes = Float.intBitsToFloat(entry.value);
                        break;
                    case YRES_TAG:
                        stream.vRes = Float.intBitsToFloat(entry.value);
                        break;
                    case RESUNIT_TAG:
                        stream.resUnit = entry.value & 0x03;
                        break;
                    case BITSPERSMP_TAG:
                        if (entry.count == 1) {
                            stream.bitsPerSmp = entry.value;
                        } else {
                            oldPos = getFilePointer();
                            seek(entry.value);
                            for (int j = 0, k = 0; j < entry.count; j++, k = stream.bitsPerSmp) {
                                stream.bitsPerSmp = readUniversalUShort();
                                if ((j > 0) & (k != stream.bitsPerSmp)) {
                                    throw new UnsupportedEncodingException(ERR_UNSUPPORTED);
                                }
                            }
                            seek(oldPos);
                        }
                        if ((stream.bitsPerSmp != 8) && (stream.bitsPerSmp != 16)) {
                            throw new UnsupportedEncodingException(ERR_UNSUPPORTED);
                        }
                        break;
                    case SMPPERPIXEL_TAG:
                        if ((entry.value < 1) || (gray && (entry.value != 1))) {
                            throw new UnsupportedEncodingException(ERR_CORRUPTED);
                        }
                        stream.smpPerPixel = entry.value;
                        break;
                    case PHOTOMETR_TAG:
                        essentials--;
                        switch(entry.value) {
                            case PHOTOMETR_GRAY:
                                gray = true;
                                invert = false;
                                break;
                            case PHOTOMETR_GRAYINV:
                                gray = true;
                                invert = true;
                                break;
                            case PHOTOMETR_RGB:
                                gray = false;
                                invert = false;
                                break;
                            default:
                                throw new UnsupportedEncodingException(ERR_UNSUPPORTED);
                        }
                        break;
                    case DESCR_TAG:
                        stream.descr = readTIFFstring(entry);
                        break;
                    case STRIPOFFS_TAG:
                        essentials--;
                        if (entry.count == 1) {
                            stripOffsetOffs = getFilePointer() - 4;
                            stripOffsetSize = TIFFentry.LONG;
                        } else {
                            stripOffsetOffs = entry.value;
                            stripOffsetSize = entry.type;
                        }
                        break;
                    case ROWSPERSTRIP_TAG:
                        essentials--;
                        rowsPerStrip = entry.value;
                        numStrips = (stream.height + rowsPerStrip - 1) / rowsPerStrip;
                        bytesPerRow = stream.width * stream.smpPerPixel * ((stream.bitsPerSmp + 7) >> 3);
                        break;
                    case STRIPCOUNT_TAG:
                        if (entry.count == 1) {
                        } else {
                        }
                        break;
                    case PLANE_TAG:
                        if (entry.value == PLANE_PARALLEL) {
                            throw new UnsupportedEncodingException(ERR_PARALLEL);
                        } else if (entry.value != PLANE_SERIAL) {
                            throw new UnsupportedEncodingException(ERR_UNSUPPORTED);
                        }
                        break;
                    default:
                        break;
                }
            }
            if ((i == entries) && (essentials > 0)) {
                throw new UnsupportedEncodingException(ERR_MISSINGDATA);
            }
            if (i < entries) {
                seek(getFilePointer() + 12 * (entries - i));
            }
        } while (essentials > 0);
    }

    protected int readUniversalUShort() throws IOException {
        if (!suckyPCdata) {
            return readUnsignedShort();
        } else {
            int i = readUnsignedShort();
            return ((i & 0x00FF) << 8 | ((i & 0xFF00) >> 8));
        }
    }

    protected int readUniversalInt() throws IOException {
        if (!suckyPCdata) {
            return readInt();
        } else {
            int i = readInt();
            return ((int) ((((long) i & 0xFF000000) >> 24) | ((i & 0x00FF0000) >> 8) | ((i & 0x0000FF00) << 8) | ((i & 0x000000FF) << 24)));
        }
    }

    protected void readTIFFentry(TIFFentry entry) throws IOException {
        entry.tag = readUniversalUShort();
        entry.type = readUniversalUShort();
        entry.count = readUniversalInt();
        entry.value = readUniversalInt();
        switch(entry.type) {
            case TIFFentry.BYTE:
                if (entry.count == 1) {
                    entry.value = (entry.value >> 24) & 0x000000FF;
                }
                break;
            case TIFFentry.SHORT:
                if (entry.count == 1) {
                    entry.value = (entry.value >> 16) & 0x0000FFFF;
                }
                break;
            case TIFFentry.RATIONAL:
                if (entry.count == 1) {
                    long oldPos = getFilePointer();
                    seek(entry.value);
                    long val = readLong();
                    seek(oldPos);
                    entry.value = Float.floatToIntBits((float) (val >> 32) / (float) (val & 0xFFFFFFFF));
                }
                break;
            default:
                break;
        }
    }

    protected void writeTIFFentry(int tag, int type, int count, int val) throws IOException {
        writeShort(tag);
        writeShort(type);
        writeInt(count);
        if (count == 1) {
            if (type == TIFFentry.SHORT) {
                val <<= 16;
            } else if (type == TIFFentry.BYTE) {
                val <<= 24;
            }
        }
        writeInt(val);
    }

    protected String readTIFFstring(TIFFentry entry) throws IOException {
        byte ascii[] = new byte[entry.count - 1];
        long oldPos = getFilePointer();
        seek(entry.value);
        readFully(ascii);
        seek(oldPos);
        return new String(ascii);
    }

    private void writeHeader() throws IOException {
        int entries = (stream.smpPerPixel == 1) ? 13 : 14;
        String descr = (stream.descr != null) ? stream.descr : "";
        int offset = 24 + 6 + (descr.length() + 2) & ~1;
        seek(0L);
        writeInt(TIFF_MAC_MAGIC);
        writeInt(offset);
        writeInt((int) (stream.hRes * 10000f));
        writeInt(10000);
        writeInt((int) (stream.vRes * 10000f));
        writeInt(10000);
        writeShort(stream.bitsPerSmp);
        writeShort(stream.bitsPerSmp);
        writeShort(stream.bitsPerSmp);
        if (descr.length() > 0) {
            writeBytes(descr);
        }
        if ((getFilePointer() & 1) == 0) {
            writeShort(0);
        } else {
            writeByte(0);
        }
        writeShort(entries);
        for (int tag = NEWSUBTYPE_TAG; tag <= RESUNIT_TAG; tag++) {
            switch(tag) {
                case NEWSUBTYPE_TAG:
                    writeTIFFentry(tag, TIFFentry.LONG, 1, 0);
                    break;
                case WIDTH_TAG:
                    writeTIFFentry(tag, TIFFentry.LONG, 1, stream.width);
                    break;
                case HEIGHT_TAG:
                case ROWSPERSTRIP_TAG:
                    writeTIFFentry(tag, TIFFentry.LONG, 1, stream.height);
                    break;
                case STRIPOFFS_TAG:
                    writeTIFFentry(tag, TIFFentry.LONG, 1, offset + entries * 12 + 4);
                    break;
                case STRIPCOUNT_TAG:
                    writeTIFFentry(tag, TIFFentry.LONG, 1, bytesPerRow * stream.height);
                    break;
                case XRES_TAG:
                    writeTIFFentry(tag, TIFFentry.RATIONAL, 1, 8);
                    break;
                case YRES_TAG:
                    writeTIFFentry(tag, TIFFentry.RATIONAL, 1, 16);
                    break;
                case RESUNIT_TAG:
                    writeTIFFentry(tag, TIFFentry.SHORT, 1, stream.resUnit);
                    break;
                case SMPPERPIXEL_TAG:
                    writeTIFFentry(tag, TIFFentry.SHORT, 1, stream.smpPerPixel);
                    break;
                case BITSPERSMP_TAG:
                    writeTIFFentry(tag, TIFFentry.SHORT, stream.smpPerPixel, (stream.smpPerPixel == 1) ? stream.bitsPerSmp : 24);
                    break;
                case CMPTYPE_TAG:
                    writeTIFFentry(tag, TIFFentry.SHORT, 1, CMPTYPE_NONE);
                    break;
                case PHOTOMETR_TAG:
                    writeTIFFentry(tag, TIFFentry.SHORT, 1, (stream.smpPerPixel == 1) ? PHOTOMETR_GRAY : PHOTOMETR_RGB);
                    break;
                case PLANE_TAG:
                    if (stream.smpPerPixel == 3) {
                        writeTIFFentry(tag, TIFFentry.SHORT, 1, PLANE_SERIAL);
                    }
                    break;
                case DESCR_TAG:
                    if (descr.length() > 0) {
                        writeTIFFentry(tag, TIFFentry.ASCII, descr.length() + 1, 30);
                    }
                    break;
                default:
                    break;
            }
        }
        writeShort(0);
    }
}

class TIFFentry {

    int tag;

    int type;

    int count;

    int value;

    static final int BYTE = 1;

    static final int ASCII = 2;

    static final int SHORT = 3;

    static final int LONG = 4;

    static final int RATIONAL = 5;
}
