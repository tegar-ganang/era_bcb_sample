package org.pdfbox.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.pdfbox.cos.COSDictionary;
import org.pdfbox.cos.COSName;

/**
 * This is a filter for the CCITTFax Decoder.
 * 
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @author Marcel Kammer
 * @author Paul King
 * @version $Revision: 1.12 $
 */
public class CCITTFaxDecodeFilter implements Filter {

    private static final int TAG_COUNT = 15;

    private int offset = 8;

    private int tailingBytesCount = 0;

    private final ByteArrayOutputStream tailer = new ByteArrayOutputStream();

    /**
     * Constructor.
     */
    public CCITTFaxDecodeFilter() {
    }

    /**
     * This will decode some compressed data.
     * 
     * @param compressedData
     *            The compressed byte stream.
     * @param result
     *            The place to write the uncompressed byte stream.
     * @param options
     *            The options to use to encode the data.
     * 
     * @throws IOException
     *             If there is an error decompressing the stream.
     */
    public void decode(InputStream compressedData, OutputStream result, COSDictionary options) throws IOException {
        COSDictionary dict = (COSDictionary) options.getDictionaryObject("DecodeParms");
        int width = options.getInt("Width");
        int height = options.getInt("Height");
        int length = options.getInt(COSName.LENGTH);
        int compressionType = dict.getInt("K");
        boolean blackIs1 = dict.getBoolean("BlackIs1", false);
        writeTagHeader(result, length);
        int i = 0;
        byte[] buffer = new byte[32768];
        int lentoread = length;
        while ((lentoread > 0) && ((i = compressedData.read(buffer, 0, Math.min(lentoread, 32768))) != -1)) {
            result.write(buffer, 0, i);
            lentoread = lentoread - i;
        }
        while (lentoread > 0) {
            result.write(buffer, 0, Math.min(lentoread, 32768));
            lentoread = lentoread - Math.min(lentoread, 32738);
        }
        writeTagCount(result);
        writeTagWidth(result, width);
        writeTagHeight(result, height);
        writeTagBitsPerSample(result, 1);
        writeTagCompression(result, compressionType);
        writeTagPhotometric(result, blackIs1);
        writeTagStripOffset(result, 8);
        writeTagOrientation(result, 1);
        writeTagSamplesPerPixel(result, 1);
        writeTagRowsPerStrip(result, height);
        writeTagStripByteCount(result, length);
        writeTagXRes(result, 200, 1);
        writeTagYRes(result, 200, 1);
        writeTagResolutionUnit(result, 2);
        writeTagSoftware(result, "pdfbox".getBytes());
        writeTagDateTime(result, new Date());
        writeTagTailer(result);
    }

    private void writeTagHeader(OutputStream result, int length) throws IOException {
        byte[] header = { 'M', 'M', 0, '*' };
        result.write(header);
        offset += length;
        int i1 = offset / 16777216;
        int i2 = (offset - i1 * 16777216) / 65536;
        int i3 = (offset - i1 * 16777216 - i2 * 65536) / 256;
        int i4 = offset % 256;
        result.write(i1);
        result.write(i2);
        result.write(i3);
        result.write(i4);
    }

    private void writeTagCount(OutputStream result) throws IOException {
        result.write(TAG_COUNT / 256);
        result.write(TAG_COUNT % 256);
    }

    private void writeTagWidth(OutputStream result, int width) throws IOException {
        result.write(1);
        result.write(0);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(width / 256);
        result.write(width % 256);
        result.write(0);
        result.write(0);
    }

    private void writeTagHeight(OutputStream result, int height) throws IOException {
        result.write(1);
        result.write(1);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(height / 256);
        result.write(height % 256);
        result.write(0);
        result.write(0);
    }

    private void writeTagBitsPerSample(OutputStream result, int value) throws IOException {
        result.write(1);
        result.write(2);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(value / 256);
        result.write(value % 256);
        result.write(0);
        result.write(0);
    }

    /**
     * Write the tag compression.
     * 
     * @param result The stream to write to.
     * @param type The type to write.
     * @throws IOException If there is an error writing to the stream.
     */
    public void writeTagCompression(OutputStream result, int type) throws IOException {
        result.write(1);
        result.write(3);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(0);
        if (type < 0) {
            result.write(4);
        } else if (type == 0) {
            result.write(3);
        } else {
            result.write(2);
        }
        result.write(0);
        result.write(0);
    }

    private void writeTagPhotometric(OutputStream result, boolean blackIs1) throws IOException {
        result.write(1);
        result.write(6);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(0);
        if (blackIs1) {
            result.write(1);
        } else {
            result.write(0);
        }
        result.write(0);
        result.write(0);
    }

    private void writeTagStripOffset(OutputStream result, int value) throws IOException {
        result.write(1);
        result.write(17);
        result.write(0);
        result.write(4);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        int i1 = value / 16777216;
        int i2 = (value - i1 * 16777216) / 65536;
        int i3 = (value - i1 * 16777216 - i2 * 65536) / 256;
        int i4 = value % 256;
        result.write(i1);
        result.write(i2);
        result.write(i3);
        result.write(i4);
    }

    private void writeTagSamplesPerPixel(OutputStream result, int value) throws IOException {
        result.write(1);
        result.write(21);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(value / 256);
        result.write(value % 256);
        result.write(0);
        result.write(0);
    }

    private void writeTagRowsPerStrip(OutputStream result, int value) throws IOException {
        result.write(1);
        result.write(22);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(value / 256);
        result.write(value % 256);
        result.write(0);
        result.write(0);
    }

    private void writeTagStripByteCount(OutputStream result, int value) throws IOException {
        result.write(1);
        result.write(23);
        result.write(0);
        result.write(4);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        int i1 = value / 16777216;
        int i2 = (value - i1 * 16777216) / 65536;
        int i3 = (value - i1 * 16777216 - i2 * 65536) / 256;
        int i4 = value % 256;
        result.write(i1);
        result.write(i2);
        result.write(i3);
        result.write(i4);
    }

    private void writeTagXRes(OutputStream result, int value1, int value2) throws IOException {
        result.write(1);
        result.write(26);
        result.write(0);
        result.write(5);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        int valueOffset = offset + 6 + 12 * TAG_COUNT + tailer.size();
        int i1 = valueOffset / 16777216;
        int i2 = (valueOffset - i1 * 16777216) / 65536;
        int i3 = (valueOffset - i1 * 16777216 - i2 * 65536) / 256;
        int i4 = valueOffset % 256;
        result.write(i1);
        result.write(i2);
        result.write(i3);
        result.write(i4);
        i1 = value1 / 16777216;
        i2 = (value1 - i1 * 16777216) / 65536;
        i3 = (value1 - i1 * 16777216 - i2 * 65536) / 256;
        i4 = value1 % 256;
        tailer.write(i1);
        tailer.write(i2);
        tailer.write(i3);
        tailer.write(i4);
        i1 = value2 / 16777216;
        i2 = (value2 - i1 * 16777216) / 65536;
        i3 = (value2 - i1 * 16777216 - i2 * 65536) / 256;
        i4 = value2 % 256;
        tailer.write(i1);
        tailer.write(i2);
        tailer.write(i3);
        tailer.write(i4);
        tailingBytesCount += 8;
    }

    private void writeTagYRes(OutputStream result, int value1, int value2) throws IOException {
        result.write(1);
        result.write(27);
        result.write(0);
        result.write(5);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        int valueOffset = offset + 6 + 12 * TAG_COUNT + tailer.size();
        int i1 = valueOffset / 16777216;
        int i2 = (valueOffset - i1 * 16777216) / 65536;
        int i3 = (valueOffset - i1 * 16777216 - i2 * 65536) / 256;
        int i4 = valueOffset % 256;
        result.write(i1);
        result.write(i2);
        result.write(i3);
        result.write(i4);
        i1 = value1 / 16777216;
        i2 = (value1 - i1 * 16777216) / 65536;
        i3 = (value1 - i1 * 16777216 - i2 * 65536) / 256;
        i4 = value1 % 256;
        tailer.write(i1);
        tailer.write(i2);
        tailer.write(i3);
        tailer.write(i4);
        i1 = value2 / 16777216;
        i2 = (value2 - i1 * 16777216) / 65536;
        i3 = (value2 - i1 * 16777216 - i2 * 65536) / 256;
        i4 = value2 % 256;
        tailer.write(i1);
        tailer.write(i2);
        tailer.write(i3);
        tailer.write(i4);
        tailingBytesCount += 8;
    }

    private void writeTagResolutionUnit(OutputStream result, int value) throws IOException {
        result.write(1);
        result.write(40);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(value / 256);
        result.write(value % 256);
        result.write(0);
        result.write(0);
    }

    private void writeTagOrientation(OutputStream result, int value) throws IOException {
        result.write(1);
        result.write(18);
        result.write(0);
        result.write(3);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(1);
        result.write(value / 256);
        result.write(value % 256);
        result.write(0);
        result.write(0);
    }

    private void writeTagTailer(OutputStream result) throws IOException {
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(tailer.toByteArray());
    }

    private void writeTagSoftware(OutputStream result, byte[] text) throws IOException {
        result.write(1);
        result.write(49);
        result.write(0);
        result.write(2);
        result.write(0);
        result.write(0);
        result.write((text.length + 1) / 256);
        result.write((text.length + 1) % 256);
        int valueOffset = offset + 6 + 12 * TAG_COUNT + tailer.size();
        int i1 = valueOffset / 16777216;
        int i2 = (valueOffset - i1 * 16777216) / 65536;
        int i3 = (valueOffset - i1 * 16777216 - i2 * 65536) / 256;
        int i4 = valueOffset % 256;
        result.write(i1);
        result.write(i2);
        result.write(i3);
        result.write(i4);
        tailer.write(text);
        tailer.write(0);
        tailingBytesCount += text.length + 1;
    }

    private void writeTagDateTime(OutputStream result, Date date) throws IOException {
        result.write(1);
        result.write(50);
        result.write(0);
        result.write(2);
        result.write(0);
        result.write(0);
        result.write(0);
        result.write(20);
        int valueOffset = offset + 6 + 12 * TAG_COUNT + tailer.size();
        int i1 = valueOffset / 16777216;
        int i2 = (valueOffset - i1 * 16777216) / 65536;
        int i3 = (valueOffset - i1 * 16777216 - i2 * 65536) / 256;
        int i4 = valueOffset % 256;
        result.write(i1);
        result.write(i2);
        result.write(i3);
        result.write(i4);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        String datetime = sdf.format(date);
        tailer.write(datetime.getBytes());
        tailer.write(0);
        tailingBytesCount += 20;
    }

    /**
     * This will encode some data.
     * 
     * @param rawData
     *            The raw data to encode.
     * @param result
     *            The place to write to encoded results to.
     * @param options
     *            The options to use to encode the data.
     * 
     * @throws IOException
     *             If there is an error compressing the stream.
     */
    public void encode(InputStream rawData, OutputStream result, COSDictionary options) throws IOException {
        System.err.println("Warning: CCITTFaxDecode.encode is not implemented yet, skipping this stream.");
    }
}
