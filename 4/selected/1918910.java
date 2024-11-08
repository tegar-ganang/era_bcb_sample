package com.bigfoot.bugar.image;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * <p>
 * Encodes Java images to PNG streams.
 * </p>
 *
 * <p>
 * Here is a simple example of how you might use this class.  Let's say you
 * have an Image object that you want to save to a file called
 * &quot;image.png&quot;.  This is how you would do it:
 * </p>
 *
 * <p>
 * <pre>
 *    Image image = getImage();  // the image you want to encode as a PNG
 *
 *    OutputStream fileOut = new FileOutputStream( &quot;image.png&quot; );
 *
 *    PNGEncoder encoder = new PNGEncoder( image, fileOut );
 *    encoder.encodeImage();
 *
 *    fileOut.close();
 * </pre>
 * </p>
 *
 * <p>
 * The type of PNG output (single-channel grayscale, three-channel color,
 * palette indexed, etc.) depends on the source image.  For example, if there
 * are less than or equal to 256 distinct colors in the image, then the PNG will
 * be palette indexed.  The same sort of reasoning determines whether to add an
 * alpha channel (for images that contain transparency information).
 * </p>
 *
 * <p>
 * The maximum bit depth supported by this encoder is 8, since that is the bit
 * depth of the Java RGB color model.  The bit depth will always be 8 for
 * non-palette-indexed images (those with more than 256 colors).  The bit depth
 * for palette indexed images depends on how many colors there are in the
 * palette; in that case, it will be the smallest valid PNG bit depth that is
 * big enough to index all the palette entries.
 * </p>
 *
 * <p>
 * The default behavior of the <tt>PNGEncoder</tt> is to generate a
 * non-interlaced PNG stream with one big IDAT chunk, using
 * <tt>java.util.zip.Deflater.DEFAULT_COMPRESSION</tt> as the compression level.
 * You may want to change these parameters in certain situations.
 * </p>
 *
 * <p>
 * For instance, you might have a servlet that generates fairly large dynamic
 * images that it sends to client Web browsers over possibly slow connections.
 * In this case, you would probably want the image to be interlaced so that the
 * user could get an overview of the image relatively quickly.  More
 * importantly, though, you would want the user to start getting pieces of the
 * image as soon as they were encoded rather than have to wait for the image
 * to be completely encoded on the server before getting the first byte.  Here
 * is how you would take care of both these issues:
 * </p>
 *
 * <p>
 * <pre>
 *    public void doGet( HttpServletRequest request,
 *            HttpServletResponse response )
 *            throws IOException, ServletException {
 *        Image image = generateImage();
 *
 *        OutputStream out = response.getOutputStream();
 *
 *        PNGEncoder encoder = new PNGEncoder( image, out );
 *
 *        // Set some parameters of the encoder before encoding.
 *
 *        encoder.setInterlaced( true );  // generate an interlaced PNG
 *        encoder.setIDATSize( 200 );     // small buffer size, so the user will
 *                                        //  start getting data sooner
 *
 *        try {
 *            encoder.encodeImage();
 *        } catch ( PNGException exc ) {
 *            throw new ServletException( "Error encoding the PNG", exc );
 *        }  // end try/catch
 *
 *        out.close();
 *    }  // end doGet()
 * </pre>
 * </p>
 *
 * <p>
 * For details on these and other features, see the method descriptions.
 * </p>
 *
 * <p>
 * To learn about the PNG image format, see the
 * <a href="http://www.w3.org/TR/png.html" target="_top">PNG Specification, Version 1.0</a>
 * </p>
 *
 * <H3>Known Bugs</H3>
 * <p>
 * Attempting to encode very large images (e.g. 2000x1000 pixels) may cause the
 * JVM to crash with an OutOfMemoryError.  I am working on a possible fix for
 * this problem for the next release.
 * </p>
 *
 * <H3>License</H3>
 * <p>
 * Free reign. You may use this package any way you wish, with the one
 * exception that if you distribute the source code then, regardless of whether
 * you've modified it, please keep my name and email address within the
 * comments.
 * </p>
 *
 * <p>
 * Please send comments and bug reports to
 * <a href="mailto:Walter Brameld &lt;wbrameld4@yahoo.com&gt;">
 *  Walter Brameld &lt;wbrameld4@yahoo.com&gt;
 * </a>
 * </p>
 * <p>
 * Copyright 2001 Walter Brameld
 * </p>
 */
public class PNGEncoder {

    private ImageProducer producer;

    private OutputStream out;

    private boolean interlaced;

    private boolean outputCreationTime;

    private int bufferSize;

    private int compressionLevel;

    private int gamma;

    private Color backgroundColor;

    private Set textChunks;

    private static final long MAX_CHUNK_DATA_SIZE = 0x7fffffff;

    private final CRC32 crcGenerator = new CRC32();

    private static final Color COLOR_BLANK = new Color(255, 255, 255, 0);

    private byte[] filteredNone;

    private byte[] filteredSub;

    private byte[] filteredUp;

    private byte[] filteredAverage;

    private byte[] filteredPaeth;

    private static final byte FILTER_TYPE_NONE = 0;

    private static final byte FILTER_TYPE_SUB = 1;

    private static final byte FILTER_TYPE_UP = 2;

    private static final byte FILTER_TYPE_AVERAGE = 3;

    private static final byte FILTER_TYPE_PAETH = 4;

    private static final ImmutableByteArray PNG_SIGNATURE = new ImmutableByteArray(new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 });

    private static final ImmutableByteArray CHUNK_TYPE_IHDR = new ImmutableByteArray(new byte[] { (byte) 'I', (byte) 'H', (byte) 'D', (byte) 'R' });

    private static final ImmutableByteArray CHUNK_TYPE_tIME = new ImmutableByteArray(new byte[] { (byte) 't', (byte) 'I', (byte) 'M', (byte) 'E' });

    private static final ImmutableByteArray CHUNK_TYPE_tEXt = new ImmutableByteArray(new byte[] { (byte) 't', (byte) 'E', (byte) 'X', (byte) 't' });

    private static final ImmutableByteArray CHUNK_TYPE_zTXt = new ImmutableByteArray(new byte[] { (byte) 'z', (byte) 'T', (byte) 'X', (byte) 't' });

    private static final ImmutableByteArray CHUNK_TYPE_gAMA = new ImmutableByteArray(new byte[] { (byte) 'g', (byte) 'A', (byte) 'M', (byte) 'A' });

    private static final ImmutableByteArray CHUNK_TYPE_PLTE = new ImmutableByteArray(new byte[] { (byte) 'P', (byte) 'L', (byte) 'T', (byte) 'E' });

    private static final ImmutableByteArray CHUNK_TYPE_tRNS = new ImmutableByteArray(new byte[] { (byte) 't', (byte) 'R', (byte) 'N', (byte) 'S' });

    private static final ImmutableByteArray CHUNK_TYPE_bKGD = new ImmutableByteArray(new byte[] { (byte) 'b', (byte) 'K', (byte) 'G', (byte) 'D' });

    private static final ImmutableByteArray CHUNK_TYPE_IDAT = new ImmutableByteArray(new byte[] { (byte) 'I', (byte) 'D', (byte) 'A', (byte) 'T' });

    private static final ImmutableByteArray CHUNK_TYPE_IEND = new ImmutableByteArray(new byte[] { (byte) 'I', (byte) 'E', (byte) 'N', (byte) 'D' });

    /**
     * Creates a PNG encoder that writes the given image to the given
     * <tt>OutputStream</tt>.
     *
     * @param	image	the image to encode as a PNG
     * @param	out	the output stream to write to
     *
     * @throws	NullPointerException	if <tt>image</tt> is
     *		<strong>null</strong>
     * @throws	NullPointerException	if <tt>out</tt> is <strong>null</strong>
     */
    public PNGEncoder(Image image, OutputStream out) {
        this(image.getSource(), out);
    }

    /**
     * Creates a PNG encoder that writes the image from the given image producer
     * to the given <tt>OutputStream</tt>.
     *
     * @param	producer	the source of the image data
     * @param	out	the output stream to write to
     *
     * @throws	NullPointerException	if <tt>producer</tt> is
     *		<strong>null</strong>
     * @throws	NullPointerException	if <tt>out</tt> is <strong>null</strong>
     */
    public PNGEncoder(ImageProducer producer, OutputStream out) {
        if (producer == null) {
            throw new NullPointerException("producer");
        }
        if (out == null) {
            throw new NullPointerException("out");
        }
        this.interlaced = false;
        this.outputCreationTime = false;
        this.bufferSize = (int) MAX_CHUNK_DATA_SIZE;
        this.compressionLevel = Deflater.DEFAULT_COMPRESSION;
        this.gamma = -1;
        this.backgroundColor = null;
        this.producer = producer;
        this.out = out;
        this.textChunks = new TreeSet(new TextChunkComparator());
    }

    /**
     * <p>
     * Sets the size of the IDAT chunks in the output image (except
     * for the last chunk, which is whatever is left).
     * </p>
     * <p>
     * By default, the size is the maximum chunk data length as defined in the
     * PNG Specification, Version 1.0, which is (2^31) - 1
     * </p>
     *
     * @param	size	the new maximum IDAT chunk size
     *
     * @throws	IllegalArgumentException	if <tt>size &lt;= 0 || size
     *		&gt; (2^31) - 1</tt>
     */
    public synchronized void setIDATSize(int size) {
        if (size <= 0 || size > MAX_CHUNK_DATA_SIZE) {
            throw new IllegalArgumentException("size: " + size);
        }
        this.bufferSize = size;
    }

    /**
     * <p>
     * Sets the compression level for the compressed portions of the PNG stream
     * (<tt>IDAT</tt> and <tt>zTXt</tt> chunks).  Valid values are 0
     * (no compression, fastest) through 9 (best compression, slowest).
     * </p>
     * <p>
     * By default, the compression level is
     * <tt>java.util.zip.Deflater.DEFAULT_COMPRESSION</tt>
     * </p>
     *
     * @param	compressionLevel	the new compression level
     *
     * @throws	IllegalArgumentException	if <tt>compressionLevel</tt> is
     *		less than 0 or greater than 9
     */
    public synchronized void setCompressionLevel(int compressionLevel) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel: " + compressionLevel);
        }
        this.compressionLevel = compressionLevel;
    }

    /**
     * <p>
     * According to the PNG Specification, Version 1,0, this sets
     * <blockquote>
     * the gamma of the camera (or simulated camera) that produced the image,
     * and thus the gamma of the image with respect to the original scene.
     * </blockquote>
     * </p>
     *
     * <p>
     * The highest gamma supported by PNG is 21474.836, but this should
     * never pose a severe limitation since typical gamma values are between 0.4
     * and 2.5
     * </p>
     *
     * <p>
     * By default, no gamma value is output.  If you wish to remove the gamma
     * value after setting it, use the <tt>removeGamma()</tt> method.
     * </p>
     *
     * @param	gamma	the new gamma for the image
     *
     * @throws	IllegalArgumentException	if <tt>gamma &lt;= 0.0f</tt>
     *		of <tt>gamma &gt; 21474.836f</tt>
     */
    public synchronized void setGamma(float gamma) {
        if (gamma <= 0.0f || gamma > 21474.836f) {
            throw new IllegalArgumentException("gamma: " + gamma);
        }
        this.gamma = (int) (gamma * 100000.0f);
    }

    /**
     * <p>
     * Causes this <tt>PNGEncoder</tt> to not output a gamma value for the
     * image.
     * </p>
     *
     * <p>
     * By default, no gamma value is output.
     * </p>
     */
    public synchronized void removeGamma() {
        this.gamma = -1;
    }

    /**
     * <p>
     * Sets the background color for the image.  This is the color against
     * which a viewer may choose to display the image.  This is especially
     * useful for images that contain transparency information.
     * </p>
     * <p>
     * Only the red, green, and blue components of the specified color are used
     * to define the background.  The alpha component is ignored.
     * </p>
     * <p>
     * A couple words of warning:
     * </p>
     * <ul>
     * <li>
     * <p>
     * If your image is grayscale, and you want it to be encoded as grayscale
     * rather than true color in order to save space, then the background color,
     * if supplied, must be grayscale.
     * </p>
     * </li>
     * <li>
     * <p>
     * If you know that your image contains exactly 256 colors, and you want it
     * to be encoded as a palette-indexed PNG, then the background color, if
     * supplied, must match one of the colors that occurs in the image.  If it
     * does not, then your image will end up having 257 colors and will not be
     * encoded as a palette-indexed PNG.
     * </p>
     * </li>
     * <p>
     * By default, no background color information is output to the PNG stream.
     * </p>
     *
     * @param	color	the background color, or <strong>null</strong> to
     *		prevent background color information from appearing in the PNG
     *		stream
     */
    public synchronized void setBackgroundColor(Color color) {
        this.backgroundColor = color.getAlpha() == 255 ? color : new Color(color.getRed(), color.getBlue(), color.getGreen());
    }

    /**
     * <p>
     * Sets whether to encode the image in interlaced mode.
     * </p>
     * <p>
     * By default, images are encoded in non-interlaced mode.
     * </p>
     *
     * @param	interlaced	<strong>true</strong> if the image should be
     *		encoded in interlaced mode, <strong>false</strong> if not
     */
    public synchronized void setInterlaced(boolean interlaced) {
        this.interlaced = interlaced;
    }

    /**
     * <p>
     * Sets whether to automatically output a tEXt chunk with the
     * <tt>Creation Time</tt> keyword.  If <strong>true</strong>, then the
     * encoder will generate a <tt>Creation Time</tt> tEXt chunk that records
     * the date and time in GMT that the stream was created.
     * </p>
     * <p>
     * By default, a <tt>Creation Time</tt> tEXt chunk is not output.
     * </p>
     *
     * @param	outputCreationTime	<strong>true</strong> if a <tt>Creation
     *		Time</tt> tEXt chunk should automatically be generated and
     *		output to the PNG stream, <strong>false</strong> if not
     */
    public synchronized void setOutputCreationTime(boolean outputCreationTime) {
        this.outputCreationTime = outputCreationTime;
    }

    /**
     * <p>
     * Adds a text chunk that contains the given keyword and text.  This will
     * produce a <tt>tEXt</tt> chunk in the PNG stream if the text is less than
     * 1024 bytes; a <tt>zTXt</tt> (compressed text) chunk will be produced
     * instead if the text is greater than or equal to 1024 bytes.  If a
     * <tt>zTXt</tt> chunk is produced, then the compression level used will
     * be the same as for the <tt>IDAT</TT> (image data) chunks.
     * </p>
     *
     * <p>
     * According to the PNG Specification, Version 1.0, the following keywords
     * are predefined and should be used where appropriate:
     * </p>
     *
     * <p>
     * <center>
     * <table width="70%" border="0">
     *  <tr>
     *   <td width="30%">
     *    Title
     *   <td>
     *   <td>
     *    Short (one line) title or caption for image
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Author
     *   <td>
     *   <td>
     *    Name of image's creator
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Description
     *   <td>
     *   <td>
     *    Description of image (possibly long)
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Copyright
     *   <td>
     *   <td>
     *    Copyright notice
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Creation Time
     *   <td>
     *   <td>
     *    Time of original image creation
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Software
     *   <td>
     *   <td>
     *    Software used to create the image
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Disclaimer
     *   <td>
     *   <td>
     *    Legal disclaimer
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Warning
     *   <td>
     *   <td>
     *    Warning of nature of content
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Source
     *   <td>
     *   <td>
     *    Device used to create the image
     *   <td>
     *  </tr>
     *  <tr>
     *   <td width="30%">
     *    Comment
     *   <td>
     *   <td>
     *    Miscellaneous comment; conversion from GIF comment
     *   <td>
     *  </tr>
     * </table>
     * </center>
     * </p>
     *
     * <p>
     * Note that keywords are case-sensitive.  &quot;title&quot; is not the
     * same as &quot;Title&quot;
     * </p>
     *
     * <p>
     * The PNG Specification, Version 1.0, places some restrictions on the
     * formats of the keyword and text:
     * </p>
     * <ul>
     * <li>
     * <p>
     * The keyword's length must be at least 1 and at most 79 characters.
     * </p>
     * </li>
     * <li>
     * <p>
     * The keyword must not start or end with a space, nor may it contain
     * consecutive spaces.
     * </p>
     * </li>
     * <li>
     * <p>
     * The keyword may not contain any character whose ASCII code is not in the
     * range 32-126 or 161-255.
     * </p>
     * </li>
     * <li>
     * <p>
     * The text string may contain characters in the ISO 8859-1 (Latin-1)
     * character set (ASCII codes 32-127 and 160-255).  According to the PNG
     * Specification, Verstion 1.0, it is recommended that text chunks do not
     * contain any control characters besides newline (ASCII code 10).  This PNG
     * encoder forbids all control characters in the text except for newline.
     * </p>
     * </li>
     * </ul>
     *
     * @param	keyword	the text chunk's keyword
     * @param	text	the text that goes in the chunk
     *
     * @throws	NullPointerException	if <tt>keyword</tt> is
     *		<strong>null</strong>
     * @throws	NullPointerException	if <tt>text</tt> is
     *		<strong>null</strong>
     * @throws	IllegalArgumentExceptions	if any of the above restrictions
     *		on the keyword or text are broken
     */
    public synchronized void addTextChunk(String keyword, String text) {
        if (keyword == null) {
            throw new NullPointerException("keyword");
        }
        if (text == null) {
            throw new NullPointerException("text");
        }
        validateKeyword(keyword);
        validateText(text);
        this.textChunks.add(new String[] { keyword, text });
    }

    /**
     * Enodes the image as a PNG and writes it to the output stream.
     *
     * @throws	IOException	if an I/O error occurs
     * @throws	PNGException	if an error retrieving the image data occurs
     */
    public synchronized void encodeImage() throws IOException, PNGException {
        Consumer consumer = new Consumer();
        producer.startProduction(consumer);
        while (!consumer.isDone()) {
            try {
                wait();
            } catch (InterruptedException exc) {
            }
        }
        if (consumer.getStatus() == ImageConsumer.IMAGEABORTED) {
            throw new PNGException("image production aborted");
        } else if (consumer.getStatus() == ImageConsumer.IMAGEERROR) {
            throw new PNGException("image production failed");
        }
        producer.removeConsumer(consumer);
        writePNG(consumer.getImageData());
    }

    private void writePNG(ImageData data) throws IOException {
        writePNGSignature();
        writeHeader(data);
        writeTime();
        if (gamma != -1) {
            writeGamma();
        }
        if (data.hasPalette()) {
            writePalette(data.getPalette());
        } else if (data.hasSingleAlpha()) {
            writeTransparency(data.getAlphaColor(), data.isGrayscale());
        }
        if (backgroundColor != null) {
            writeBackground(data);
        }
        writeData(data);
        writeUserTextChunks();
        writeEnd();
        out.flush();
    }

    private void writePNGSignature() throws IOException {
        out.write(PNG_SIGNATURE.getBytes());
    }

    private void writeHeader(ImageData data) throws IOException {
        ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
        writeInt(data.getWidth(), chunkData);
        writeInt(data.getHeight(), chunkData);
        chunkData.write(data.getBitDepth());
        int colorType = 0;
        if (data.isPaletteIndexed()) {
            colorType = 3;
        } else {
            if (!data.isGrayscale()) {
                colorType |= 2;
            }
            if (data.hasAlphaChannel()) {
                colorType |= 4;
            }
        }
        chunkData.write(colorType);
        chunkData.write(0);
        chunkData.write(0);
        chunkData.write(interlaced ? 1 : 0);
        chunkData.close();
        writeChunk(CHUNK_TYPE_IHDR.getBytes(), chunkData.toByteArray());
    }

    private void writeTime() throws IOException {
        String[] months = new String[] { null, "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        Calendar time = new GregorianCalendar();
        time.setTimeZone(TimeZone.getTimeZone("GMT"));
        byte[] data = new byte[7];
        int year = time.get(Calendar.YEAR);
        data[0] = (byte) (year >> 8);
        data[1] = (byte) year;
        data[2] = (byte) decodeMonth(time.get(Calendar.MONTH));
        data[3] = (byte) time.get(Calendar.DATE);
        data[4] = (byte) time.get(Calendar.HOUR_OF_DAY);
        data[5] = (byte) time.get(Calendar.MINUTE);
        data[6] = (byte) time.get(Calendar.SECOND);
        writeChunk(CHUNK_TYPE_tIME.getBytes(), data);
        if (!outputCreationTime) {
            return;
        }
        String hourString = Byte.toString(data[4]);
        if (hourString.length() == 1) {
            hourString = '0' + hourString;
        }
        String minuteString = Byte.toString(data[5]);
        if (minuteString.length() == 1) {
            minuteString = '0' + minuteString;
        }
        String secondString = Byte.toString(data[6]);
        if (secondString.length() == 1) {
            secondString = '0' + secondString;
        }
        writeTextChunk("Creation Time", Byte.toString(data[3]) + ' ' + months[data[2]] + ' ' + Integer.toString(year) + ' ' + hourString + ':' + minuteString + ':' + secondString + ' ' + "GMT");
    }

    private int decodeMonth(int month) {
        switch(month) {
            case Calendar.JANUARY:
                return 1;
            case Calendar.FEBRUARY:
                return 2;
            case Calendar.MARCH:
                return 3;
            case Calendar.APRIL:
                return 4;
            case Calendar.MAY:
                return 5;
            case Calendar.JUNE:
                return 6;
            case Calendar.JULY:
                return 7;
            case Calendar.AUGUST:
                return 8;
            case Calendar.SEPTEMBER:
                return 9;
            case Calendar.OCTOBER:
                return 10;
            case Calendar.NOVEMBER:
                return 11;
            case Calendar.DECEMBER:
                return 12;
        }
        return 1;
    }

    private void writeGamma() throws IOException {
        byte[] data = new byte[4];
        data[0] = (byte) ((gamma >> 24) & 0xff);
        data[1] = (byte) ((gamma >> 16) & 0xff);
        data[2] = (byte) ((gamma >> 8) & 0xff);
        data[3] = (byte) ((gamma) & 0xff);
        writeChunk(CHUNK_TYPE_gAMA.getBytes(), data);
    }

    private void writePalette(Palette palette) throws IOException {
        byte[] data = new byte[palette.getSize() * 3];
        for (int i = 0; i < palette.getSize(); ++i) {
            Color color = palette.getColorAt(i);
            data[i * 3] = (byte) color.getRed();
            data[i * 3 + 1] = (byte) color.getGreen();
            data[i * 3 + 2] = (byte) color.getBlue();
        }
        writeChunk(CHUNK_TYPE_PLTE.getBytes(), data);
        byte[] alphas = palette.getAlphas();
        if (alphas.length > 0) {
            writeChunk(CHUNK_TYPE_tRNS.getBytes(), alphas);
        }
    }

    private void writeTransparency(Color color, boolean grayscale) throws IOException {
        byte[] data = grayscale ? new byte[2] : new byte[6];
        data[0] = (byte) color.getRed();
        if (!grayscale) {
            data[2] = (byte) color.getGreen();
            data[4] = (byte) color.getBlue();
        }
        writeChunk(CHUNK_TYPE_tRNS.getBytes(), data);
    }

    private void writeBackground(ImageData data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(6);
        if (data.isPaletteIndexed()) {
            bytes.write(data.getPalette().getColorIndex(backgroundColor));
        } else {
            bytes.write(0);
            bytes.write(backgroundColor.getRed());
            if (!data.isGrayscale()) {
                bytes.write(0);
                bytes.write(backgroundColor.getGreen());
                bytes.write(0);
                bytes.write(backgroundColor.getBlue());
            }
        }
        bytes.close();
        writeChunk(CHUNK_TYPE_bKGD.getBytes(), bytes.toByteArray());
    }

    private void writeData(ImageData data) throws IOException {
        int scanLineSize = data.getBitsPerPixel() * data.getWidth() / 8 + 1;
        filteredNone = new byte[scanLineSize + 1];
        filteredSub = new byte[scanLineSize + 1];
        filteredUp = new byte[scanLineSize + 1];
        filteredAverage = new byte[scanLineSize + 1];
        filteredPaeth = new byte[scanLineSize + 1];
        filteredNone[0] = FILTER_TYPE_NONE;
        filteredSub[0] = FILTER_TYPE_SUB;
        filteredUp[0] = FILTER_TYPE_UP;
        filteredAverage[0] = FILTER_TYPE_AVERAGE;
        filteredPaeth[0] = FILTER_TYPE_PAETH;
        ScanLineProvider provider = interlaced ? (ScanLineProvider) new InterlaceScanLineProvider(data) : (ScanLineProvider) new NonInterlaceScanLineProvider(data);
        OutputStream out = new DeflaterOutputStream(new IDATOutputStream(), new Deflater(compressionLevel));
        byte[] prevScanLine = null;
        while (provider.hasNextScanLine()) {
            byte[] scanLine = provider.nextScanLine();
            out.write(filterScanLine(scanLine, prevScanLine, data), 0, scanLine.length + 1);
            prevScanLine = provider.passFinished() ? null : scanLine;
        }
        out.close();
        filteredNone = null;
        filteredSub = null;
        filteredUp = null;
        filteredAverage = null;
        filteredPaeth = null;
    }

    private byte[] filterScanLine(byte[] scanLine, byte[] prevScanLine, ImageData data) {
        System.arraycopy(scanLine, 0, filteredNone, 1, scanLine.length);
        if (data.isPaletteIndexed()) {
            return filteredNone;
        }
        int bytesPerPixel = data.getBitsPerPixel() / 8;
        int sumNone = 0;
        int sumSub = 0;
        int sumUp = 0;
        int sumAverage = 0;
        int sumPaeth = 0;
        int stop = scanLine.length <= 50 ? scanLine.length : scanLine.length / 4;
        for (int i = 0; i < stop; ++i) {
            sumNone += Math.abs((int) filteredNone[i + 1]);
            sumSub += filterSub(scanLine, i, bytesPerPixel);
            sumUp += filterUp(scanLine, i, prevScanLine);
            sumAverage += filterAverage(scanLine, i, prevScanLine, bytesPerPixel);
            sumPaeth += filterPaeth(scanLine, i, prevScanLine, bytesPerPixel);
        }
        int type = FILTER_TYPE_NONE;
        byte[] result = null;
        if (sumPaeth <= sumAverage && sumPaeth <= sumUp && sumPaeth <= sumSub && sumPaeth <= sumNone) {
            type = FILTER_TYPE_PAETH;
            result = filteredPaeth;
        } else if (sumAverage <= sumUp && sumAverage <= sumSub && sumAverage <= sumNone) {
            type = FILTER_TYPE_AVERAGE;
            result = filteredAverage;
        } else if (sumUp <= sumSub && sumUp <= sumNone) {
            type = FILTER_TYPE_UP;
            result = filteredUp;
        } else if (sumSub <= sumNone) {
            type = FILTER_TYPE_SUB;
            result = filteredSub;
        }
        if (type == FILTER_TYPE_NONE) {
            return filteredNone;
        }
        for (int i = stop; i < scanLine.length; ++i) {
            switch(type) {
                case FILTER_TYPE_SUB:
                    filterSub(scanLine, i, bytesPerPixel);
                    break;
                case FILTER_TYPE_UP:
                    filterUp(scanLine, i, prevScanLine);
                    break;
                case FILTER_TYPE_AVERAGE:
                    filterAverage(scanLine, i, prevScanLine, bytesPerPixel);
                    break;
                case FILTER_TYPE_PAETH:
                    filterPaeth(scanLine, i, prevScanLine, bytesPerPixel);
                    break;
            }
        }
        return result;
    }

    private int filterSub(byte[] scanLine, int i, int bytesPerPixel) {
        int left = (i - bytesPerPixel < 0) ? 0 : scanLine[i - bytesPerPixel] & 0xff;
        int result = (scanLine[i] & 0xff) - left;
        while (result < 0) {
            result += 256;
        }
        result %= 256;
        filteredSub[i + 1] = (byte) result;
        return Math.abs((int) filteredSub[i + 1]);
    }

    private int filterUp(byte[] scanLine, int i, byte[] prevScanLine) {
        int up = prevScanLine == null ? 0 : prevScanLine[i] & 0xff;
        int result = (scanLine[i] & 0xff) - up;
        while (result < 0) {
            result += 256;
        }
        result %= 256;
        filteredUp[i + 1] = (byte) result;
        return Math.abs((int) filteredUp[i + 1]);
    }

    private int filterAverage(byte[] scanLine, int i, byte[] prevScanLine, int bytesPerPixel) {
        int left = (i - bytesPerPixel < 0) ? 0 : scanLine[i - bytesPerPixel] & 0xff;
        int up = prevScanLine == null ? 0 : prevScanLine[i] & 0xff;
        int average = (left + up) >> 1;
        int result = (scanLine[i] & 0xff) - average;
        while (result < 0) {
            result += 256;
        }
        result %= 256;
        filteredAverage[i + 1] = (byte) result;
        return Math.abs((int) filteredAverage[i + 1]);
    }

    private int filterPaeth(byte[] scanLine, int i, byte[] prevScanLine, int bytesPerPixel) {
        int a = (i - bytesPerPixel < 0) ? 0 : scanLine[i - bytesPerPixel] & 0xff;
        int b = (prevScanLine == null) ? 0 : prevScanLine[i] & 0xff;
        int c = (i - bytesPerPixel < 0 || prevScanLine == null) ? 0 : prevScanLine[i - bytesPerPixel] & 0xff;
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        int result = scanLine[i] & 0xff;
        if (pa <= pb && pa <= pc) {
            result -= a;
        } else if (pb <= pc) {
            result -= b;
        } else {
            result -= c;
        }
        while (result < 0) {
            result += 256;
        }
        result %= 256;
        filteredPaeth[i + 1] = (byte) result;
        return Math.abs((int) filteredPaeth[i + 1]);
    }

    private void writeUserTextChunks() throws IOException {
        Iterator iterator = textChunks.iterator();
        while (iterator.hasNext()) {
            String[] keyValue = (String[]) iterator.next();
            writeTextChunk(keyValue[0], keyValue[1]);
        }
    }

    private void writeEnd() throws IOException {
        writeChunk(CHUNK_TYPE_IEND.getBytes(), new byte[0]);
    }

    private void writeTextChunk(String keyword, String text) throws IOException {
        if (text.length() >= 1024) {
            writeCompressedTextChunk(keyword, text);
            return;
        }
        validateKeyword(keyword);
        validateText(text);
        byte[] data = new byte[keyword.length() + 1 + text.length()];
        System.arraycopy(keyword.getBytes(), 0, data, 0, keyword.length());
        data[keyword.length()] = 0;
        System.arraycopy(text.getBytes(), 0, data, keyword.length() + 1, text.length());
        writeChunk(CHUNK_TYPE_tEXt.getBytes(), data);
    }

    private void writeCompressedTextChunk(String keyword, String text) throws IOException {
        validateKeyword(keyword);
        validateText(text);
        ByteArrayOutputStream data = new ByteArrayOutputStream(keyword.length() + 1 + text.length());
        data.write(keyword.getBytes());
        data.write(0);
        data.write(0);
        DeflaterOutputStream compressor = new DeflaterOutputStream(data, new Deflater(compressionLevel));
        compressor.write(text.getBytes());
        compressor.close();
        data.close();
        writeChunk(CHUNK_TYPE_zTXt.getBytes(), data.toByteArray());
    }

    private void validateKeyword(String keyword) {
        if (keyword.length() == 0) {
            throw new IllegalArgumentException("empty keyword");
        }
        if (keyword.length() > 79) {
            throw new IllegalArgumentException("keyword too long: " + keyword.length() + ": \"" + keyword + "\"");
        }
        if (keyword.charAt(0) == ' ') {
            throw new IllegalArgumentException("keyword contains space at beginning: \"" + keyword + "\"");
        }
        if (keyword.charAt(keyword.length() - 1) == ' ') {
            throw new IllegalArgumentException("keyword contains space at end: \"" + keyword + "\"");
        }
        if (keyword.indexOf("  ") != -1) {
            throw new IllegalArgumentException("keyword contains consecutive spaces: \"" + keyword + "\"");
        }
        for (int i = 0; i < keyword.length(); ++i) {
            int ch = (int) keyword.charAt(i);
            if (!(ch >= 32 && ch <= 126) && !(ch >= 161 && ch <= 255)) {
                throw new IllegalArgumentException("keyword contains illegal character at index " + i + ": \"" + keyword + "\"");
            }
        }
    }

    private void validateText(String text) {
        for (int i = 0; i < text.length(); ++i) {
            int ch = (int) text.charAt(i);
            if (!(ch >= 32 && ch <= 127) && !(ch >= 160 && ch <= 255) && ch != '\n') {
                throw new IllegalArgumentException("text contains illegal character at index " + i + ": \"" + text + "\"");
            }
        }
    }

    private void writeChunk(byte[] type, byte[] data) throws IOException {
        if (data.length > MAX_CHUNK_DATA_SIZE) {
            throw new IllegalArgumentException("data too long: " + data.length);
        }
        writeInt(data.length, out);
        out.write(type);
        out.write(data);
        crcGenerator.reset();
        crcGenerator.update(type);
        crcGenerator.update(data);
        writeInt((int) crcGenerator.getValue(), out);
        out.flush();
    }

    private void writeInt(int value, OutputStream out) throws IOException {
        out.write((value >> 24) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value) & 0xff);
    }

    private class ImageData {

        private int width;

        private int height;

        private Palette palette;

        private boolean paletteIndexed;

        private boolean grayscale;

        private boolean gotAlphaChannel;

        private Color singleAlpha;

        private Color[][] pixels;

        private Random random = new Random();

        public ImageData(Color[][] pixels) {
            this.pixels = pixels;
            this.width = pixels.length == 0 ? 0 : pixels[0].length;
            this.height = pixels.length;
            this.paletteIndexed = true;
            this.grayscale = true;
            this.gotAlphaChannel = false;
            this.singleAlpha = null;
            Set paletteColors = new HashSet();
            if (backgroundColor != null) {
                this.processColor(backgroundColor, paletteColors);
            }
            for (int row = 0; row < height; ++row) {
                for (int col = 0; col < width; ++col) {
                    Color c = pixels[row][col];
                    if (c == null) {
                        pixels[row][col] = COLOR_BLANK;
                        c = COLOR_BLANK;
                    }
                    if (!paletteColors.contains(c)) {
                        this.processColor(c, paletteColors);
                    }
                }
            }
            if (this.paletteIndexed) {
                this.palette = new Palette(paletteColors);
            } else if (this.singleAlpha != null) {
                this.singleAlpha = findUniqueColor(paletteColors);
            }
        }

        private void processColor(Color color, Set paletteColors) {
            if (!this.gotAlphaChannel) {
                if (color.getAlpha() == 0) {
                    if (this.singleAlpha == null) {
                        this.singleAlpha = color;
                    }
                } else if (color.getAlpha() < 255) {
                    this.gotAlphaChannel = true;
                    this.singleAlpha = null;
                }
            }
            if (this.grayscale == true && (color.getRed() != color.getGreen() || color.getRed() != color.getBlue() || color.getGreen() != color.getBlue())) {
                this.grayscale = false;
            }
            paletteColors.add(color);
            if (this.paletteIndexed && paletteColors.size() > 256) {
                this.paletteIndexed = false;
            }
        }

        private Color findUniqueColor(Set colors) {
            int[] reds = getScrambledColorComponents();
            int[] greens = getScrambledColorComponents();
            int[] blues = getScrambledColorComponents();
            for (int r = 0; r <= 255; ++r) {
                for (int g = 0; g <= 255; ++g) {
                    for (int b = 0; b <= 255; ++b) {
                        Color color = new Color(reds[r], greens[g], blues[b]);
                        if (!colors.contains(color)) {
                            return color;
                        }
                    }
                }
            }
            return Color.black;
        }

        private int[] getScrambledColorComponents() {
            int[] array = new int[256];
            for (int i = 0; i < array.length; ++i) {
                array[i] = i;
            }
            for (int i = 0; i < array.length; ++i) {
                int j = i + random.nextInt(array.length - i);
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
            return array;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getBitDepth() {
            if (!isPaletteIndexed()) {
                return 8;
            }
            int size = palette.getSize();
            if (size > 16) {
                return 8;
            } else if (size > 4) {
                return 4;
            } else if (size > 2) {
                return 2;
            } else {
                return 1;
            }
        }

        public int getChannelCount() {
            if (isPaletteIndexed()) {
                return 3;
            }
            int count = isGrayscale() ? 1 : 3;
            if (hasAlphaChannel()) {
                ++count;
            }
            return count;
        }

        public boolean isPaletteIndexed() {
            return paletteIndexed;
        }

        public boolean hasPalette() {
            return palette != null;
        }

        public Palette getPalette() {
            return palette;
        }

        public boolean isGrayscale() {
            return grayscale;
        }

        public boolean hasAlphaChannel() {
            return gotAlphaChannel;
        }

        public boolean hasSingleAlpha() {
            return singleAlpha != null;
        }

        public Color getAlphaColor() {
            return singleAlpha;
        }

        public int getBitsPerPixel() {
            if (isPaletteIndexed()) {
                return getBitDepth();
            } else {
                return getBitDepth() * getChannelCount();
            }
        }

        public byte[] getPixelBits(int x, int y) {
            int bitsPerPixel = getBitsPerPixel();
            byte[] bits = new byte[bitsPerPixel % 8 == 0 ? bitsPerPixel / 8 : bitsPerPixel / 8 + 1];
            if (isPaletteIndexed()) {
                bits[0] = (byte) palette.getColorIndex(pixels[y][x]);
                return bits;
            }
            int off = 0;
            bits[off++] = (byte) pixels[y][x].getRed();
            if (!isGrayscale()) {
                bits[off++] = (byte) pixels[y][x].getGreen();
                bits[off++] = (byte) pixels[y][x].getBlue();
            }
            if (hasAlphaChannel()) {
                bits[off++] = (byte) pixels[y][x].getAlpha();
            }
            return bits;
        }

        public Color[][] getPixels() {
            return pixels;
        }
    }

    private static class Palette {

        private Color[] colors;

        private Map indexes;

        private byte[] alphas;

        public Palette(Set colors) {
            this.colors = (Color[]) colors.toArray(new Color[0]);
            Arrays.sort(this.colors, new AlphaComparator());
            this.indexes = new HashMap();
            List alphas = new Vector(this.colors.length);
            for (int i = 0; i < this.colors.length; ++i) {
                indexes.put(this.colors[i], new Integer(i));
                if (this.colors[i].getAlpha() < 255) {
                    alphas.add(new Byte((byte) this.colors[i].getAlpha()));
                }
            }
            this.alphas = new byte[alphas.size()];
            for (int i = 0; i < this.alphas.length; ++i) {
                this.alphas[i] = ((Byte) alphas.get(i)).byteValue();
            }
        }

        public int getSize() {
            return colors.length;
        }

        public Color getColorAt(int index) {
            return colors[index];
        }

        public int getColorIndex(Color color) {
            return ((Integer) indexes.get(color)).intValue();
        }

        public byte[] getAlphas() {
            return alphas;
        }
    }

    private static class AlphaComparator implements Comparator {

        public int compare(Object object1, Object object2) {
            int alpha1 = ((Color) object1).getAlpha();
            int alpha2 = ((Color) object2).getAlpha();
            if (alpha1 < alpha2) {
                return -1;
            } else if (alpha1 > alpha2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private class Consumer implements ImageConsumer {

        private boolean done;

        private int status;

        private Color[][] data;

        private ImageData imageData;

        public void imageComplete(int status) {
            synchronized (PNGEncoder.this) {
                if (done) {
                    return;
                }
                if (status == STATICIMAGEDONE || status == SINGLEFRAMEDONE) {
                    imageData = new ImageData(data);
                }
                this.status = status;
                this.done = true;
                PNGEncoder.this.notifyAll();
            }
        }

        public void setDimensions(int width, int height) {
            this.data = new Color[height][width];
        }

        public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {
            for (int row = 0, base = off; row < h; ++row, base += scansize) {
                int yIndex = y + row;
                for (int col = 0; col < w; ++col) {
                    int argb = model.getRGB(pixels[base + col] & 0xff);
                    int a = (argb >> 24) & 0xff;
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >> 8) & 0xff;
                    int b = (argb) & 0xff;
                    data[yIndex][x + col] = new Color(r, g, b, a);
                }
            }
        }

        public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {
            for (int row = 0, base = off; row < h; ++row, base += scansize) {
                int yIndex = y + row;
                for (int col = 0; col < w; ++col) {
                    int argb = model.getRGB(pixels[base + col]);
                    int a = (argb >> 24) & 0xff;
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >> 8) & 0xff;
                    int b = (argb) & 0xff;
                    data[yIndex][x + col] = new Color(r, g, b, a);
                }
            }
        }

        public boolean isDone() {
            synchronized (PNGEncoder.this) {
                return done;
            }
        }

        public int getStatus() {
            if (!isDone()) {
                throw new IllegalStateException("not done yet");
            }
            return status;
        }

        public ImageData getImageData() {
            if (!isDone()) {
                throw new IllegalStateException("not done yet");
            }
            return imageData;
        }

        public void setProperties(Hashtable props) {
        }

        public void setColorModel(ColorModel model) {
        }

        public void setHints(int hintflags) {
        }
    }

    private static class ImmutableByteArray {

        private byte[] bytes;

        public ImmutableByteArray(byte[] bytes) {
            this.bytes = new byte[bytes.length];
            System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
        }

        public byte[] getBytes() {
            byte[] toGo = new byte[bytes.length];
            System.arraycopy(bytes, 0, toGo, 0, bytes.length);
            return toGo;
        }
    }

    private interface ScanLineProvider {

        public boolean hasNextScanLine();

        public byte[] nextScanLine();

        public boolean passFinished();
    }

    private class NonInterlaceScanLineProvider implements ScanLineProvider {

        private ImageData data;

        private int index;

        private ScanLineBuilder scanLineBuilder;

        public NonInterlaceScanLineProvider(ImageData data) {
            this.data = data;
            this.index = 0;
            this.scanLineBuilder = new ScanLineBuilder(data);
        }

        public boolean hasNextScanLine() {
            return index < data.getHeight();
        }

        public byte[] nextScanLine() {
            if (!this.hasNextScanLine()) {
                throw new NoSuchElementException("no more scan lines");
            }
            for (int i = 0; i < data.getWidth(); ++i) {
                scanLineBuilder.writePixel(data.getPixelBits(i, index));
            }
            ++index;
            return scanLineBuilder.finish();
        }

        public boolean passFinished() {
            return !this.hasNextScanLine();
        }
    }

    private class InterlaceScanLineProvider implements ScanLineProvider {

        private ImageData data;

        private int pass;

        private int index;

        private ScanLineBuilder scanLineBuilder;

        private boolean passFinished;

        private boolean done;

        private int[] baseX = { 0, 4, 0, 2, 0, 1, 0 };

        private int[] baseY = { 0, 0, 4, 0, 2, 0, 1 };

        private int[] incrX = { 8, 8, 4, 4, 2, 2, 1 };

        private int[] incrY = { 8, 8, 8, 4, 4, 2, 2 };

        public InterlaceScanLineProvider(ImageData data) {
            this.data = data;
            this.pass = 0;
            this.index = baseY[pass];
            this.scanLineBuilder = new ScanLineBuilder(data);
        }

        public boolean hasNextScanLine() {
            return !done;
        }

        public byte[] nextScanLine() {
            if (!this.hasNextScanLine()) {
                throw new NoSuchElementException("no more scan lines");
            }
            for (int i = baseX[pass]; i < data.getWidth(); i += incrX[pass]) {
                scanLineBuilder.writePixel(data.getPixelBits(i, index));
            }
            passFinished = false;
            index += incrY[pass];
            while (!done && (index >= data.getHeight() || baseX[pass] >= data.getWidth())) {
                passFinished = true;
                ++pass;
                if (pass >= baseY.length) {
                    done = true;
                } else {
                    index = baseY[pass];
                }
            }
            return scanLineBuilder.finish();
        }

        public boolean passFinished() {
            return passFinished;
        }
    }

    private class ScanLineBuilder {

        private int buffer;

        private int bufferIndex;

        private int bitsPerPixel;

        private ByteArrayOutputStream scanLine;

        public ScanLineBuilder(ImageData data) {
            this.bitsPerPixel = data.getBitsPerPixel();
            this.scanLine = new ByteArrayOutputStream(data.getWidth() * bitsPerPixel / 8 + 1);
        }

        public void writePixel(byte[] pixel) {
            if (bitsPerPixel < 8) {
                int shift = 8 - bufferIndex - bitsPerPixel;
                buffer = ((buffer >> shift) | (pixel[0] & 0xff)) << shift;
                bufferIndex += bitsPerPixel;
                if (bufferIndex >= 8) {
                    this.flush();
                }
            } else {
                scanLine.write(pixel, 0, pixel.length);
            }
        }

        public byte[] finish() {
            if (bufferIndex > 0) {
                this.flush();
            }
            byte[] line = scanLine.toByteArray();
            scanLine.reset();
            return line;
        }

        private void flush() {
            scanLine.write(buffer);
            bufferIndex = 0;
            buffer = 0;
        }
    }

    private class IDATOutputStream extends OutputStream {

        private ByteArrayOutputStream buffer;

        private boolean closed;

        public IDATOutputStream() {
            this.buffer = new ByteArrayOutputStream(2048);
            this.closed = false;
        }

        public void close() throws IOException {
            if (closed) {
                return;
            }
            this.flush();
            this.buffer.close();
            this.closed = true;
        }

        public void flush() throws IOException {
            if (closed) {
                throw new IOException("stream is closed");
            }
            if (buffer.size() == 0) {
                return;
            }
            writeChunk(CHUNK_TYPE_IDAT.getBytes(), buffer.toByteArray());
            buffer.reset();
        }

        public void write(byte[] b) throws IOException {
            this.write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("stream is closed");
            }
            while (buffer.size() + len > bufferSize) {
                int numToWrite = bufferSize - buffer.size();
                buffer.write(b, off, numToWrite);
                this.flush();
                off += numToWrite;
                len -= numToWrite;
            }
            buffer.write(b, off, len);
            if (buffer.size() >= bufferSize) {
                this.flush();
            }
        }

        public void write(int b) throws IOException {
            if (closed) {
                throw new IOException("stream is closed");
            }
            buffer.write(b);
            if (buffer.size() >= bufferSize) {
                this.flush();
            }
        }
    }

    private class TextChunkComparator implements Comparator {

        public int compare(Object object1, Object object2) {
            String[] chunk1 = (String[]) object1;
            String[] chunk2 = (String[]) object2;
            return chunk1[0].compareTo(chunk2[0]);
        }
    }
}
