package imageutils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

/** <p>Convenience class to convert between the various image types using the Java
 * Advanced Imaging I/O API.  Will also show what ImageReader(s) and ImageWriter(s)
 * are available on the user's local system.  Some code borrowed from a book
 * titled, &quot;The Java Developers Almanac 1.4&quot; of which examples are
 * posted here: http://www.exampledepot.com/egs/javax.imageio/pkg.html</p>
 *
 * <p>Note: For JPEG 2000 support, large .jp2 files do not seem to be supported
 * well with JAI.  There are hints that a memory leak is still present in the
 * ImageIO#write method for files larger than 1MB as tested 17 NOV 2009.</p>
 *
 * @version $Id: ImageConverter.java 97 2009-11-19 04:11:18Z tnorbraten $
 * <p>
 *   <b>History:</b>
 *   <pre><b>
 *     Date:     17 NOV 2009
 *     Time:     0102Z
 *     Author:   <a href="mailto:tdnorbra@nps.edu?subject=imageutils.ImageConverter">Terry Norbraten, NPS MOVES</a>
 *     Comments: 1) Initial
 *   </b></pre>
 * </p>
 * @author <a href="mailto:tdnorbra@nps.edu?subject=imageutils.ImageConverter">Terry Norbraten</a>
 */
public class ImageConverter {

    public static final String JPEG2000 = "jpeg2000";

    public static final String JPEG_2000 = "jpeg 2000";

    /** Java Advanced Imaging does not seem to support lossles JPEG */
    public static final String JPEG_LOSSLESS = "jpeg-lossless";

    /** Java Advanced Imaging does not seem to support nearly-lossles JPEG */
    public static final String JPEG_LS = "jpeg-ls";

    public static final String JPEG = "jpeg";

    public static final String TIFF = "tiff";

    /** Print out this ImageConverter's usage message */
    public static void printUsageMessage() {
        System.out.println("**************");
        System.out.println("ImageConverter:");
        System.out.println("**************");
        System.out.println("Usage:");
        System.out.println("<path>/image.file, full path to the image");
        System.out.println("the formal format name given from the tables at: http://download.java.net/media/jai-imageio/javadoc/1.1");
    }

    /** Returns the format of the image in the file 'f'.
     * Returns null if the format is not known.
     * @param f the file to determine image format from
     * @return the format of the image from the file 'f' or null if the format is
     * not known.
     */
    public static String getFormatInFile(File f) {
        return getFormatName(f);
    }

    /** Returns the format of the image in the input stream 'is'.
     * Returns null if the format is not known.
     * @param is the InputStream to deterine image format from
     * @return the format of the image from the input stream 'is', or null if the
     * format is not known.
     */
    public static String getFormatFromStream(InputStream is) {
        return getFormatName(is);
    }

    /** Returns the format name of the image in the object 'o'.
     * 'o' can be either a File or InputStream object.
     * Returns null if the format is not known.
     * @param o the File or InputStream object to determine image format from
     * @return the format of the image from the given obeject parameter, or null
     * if the format is not known.
     */
    private static String getFormatName(Object o) {
        String result = null;
        ImageReader reader = null;
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(o);
            reader = getUniqueImageReader(iis);
            iis.close();
            result = reader.getFormatName();
            reader.dispose();
        } catch (IOException e) {
        }
        return result;
    }

    /** Retrieve a unique ImageReader for a given image format iff one is 
     * available
     * @param iis an input stream of an image file
     * @return a unique ImageReader for a given image format iff one is 
     * available, or null if not
     */
    public static ImageReader getUniqueImageReader(ImageInputStream iis) {
        Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
        return iter.hasNext() ? iter.next() : null;
    }

    /** Show registered ImageReader(s) */
    public static void showRegisteredImageReaders() {
        IIORegistry reg = IIORegistry.getDefaultInstance();
        Iterator<ImageReaderSpi> spIt = reg.getServiceProviders(ImageReaderSpi.class, true);
        ImageReaderSpi readerSpi = null;
        while (spIt.hasNext()) {
            readerSpi = spIt.next();
            System.out.println(readerSpi.getVendorName() + "\t" + readerSpi.getVersion() + "\t" + readerSpi.getDescription(Locale.getDefault()));
        }
    }

    /** Show registered ImageWriter(s) */
    public static void showRegisteredImageWriters() {
        IIORegistry reg = IIORegistry.getDefaultInstance();
        Iterator<ImageWriterSpi> spIt = reg.getServiceProviders(ImageWriterSpi.class, true);
        ImageWriterSpi writerSpi = null;
        while (spIt.hasNext()) {
            writerSpi = spIt.next();
            System.out.println(writerSpi.getVendorName() + "\t" + writerSpi.getVersion() + "\t" + writerSpi.getDescription(Locale.getDefault()));
        }
    }

    /** Show image type (format name) that registered ImageReader(s) can handle */
    public static void showRegisteredImageReaderFormatNames() {
        String[] formatNames = ImageIO.getReaderFormatNames();
        formatNames = unique(formatNames);
        for (String name : formatNames) {
            System.out.println("Registered ImageReader found for format: " + name);
        }
    }

    /** Show image type (format name) that registered ImageWriter(s) can handle */
    public static void showRegisteredImageWriterFormatNames() {
        String[] formatNames = ImageIO.getWriterFormatNames();
        formatNames = unique(formatNames);
        for (String name : formatNames) {
            System.out.println("Registered ImageWriter found for format: " + name);
        }
    }

    /** Converts all strings in 'strings' to lowercase
     * and returns an array containing the unique values.
     * All returned values are lowercase.
     * @param strings values to modify
     * @return unique values in lowercase
     */
    private static String[] unique(String[] strings) {
        Set<String> set = new HashSet<String>();
        for (String name : strings) {
            set.add(name.toLowerCase());
        }
        return set.toArray(new String[0]);
    }

    /** Command line entry for ImageConverter
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsageMessage();
            System.exit(1);
        }
        String outputFileName = null;
        File inputFile = new File(args[0]);
        PlanarImage image = JAI.create("ImageRead", inputFile);
        String arg1 = args[1];
        String fileExtension = null;
        if (arg1.equals(JPEG2000) || arg1.equals(JPEG_2000)) fileExtension = "jp2"; else if (arg1.equals(JPEG_LOSSLESS) || arg1.equals(JPEG_LS)) fileExtension = "jls"; else if (arg1.equals(JPEG)) fileExtension = "jpg"; else if (arg1.equals(TIFF)) fileExtension = "tif"; else fileExtension = arg1;
        outputFileName = inputFile.getAbsolutePath().substring(0, inputFile.getAbsolutePath().indexOf(".")) + "." + fileExtension;
        File outputFile = new File(outputFileName);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            ImageIO.write(image, arg1, out);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageConverter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ImageConverter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                image.dispose();
                out.flush();
                out.close();
                FileInputStream fis = new FileInputStream(outputFile);
                if (fis.getChannel().size() > 1) {
                    Logger.getLogger(ImageConverter.class.getName()).log(Level.INFO, "Conversion Done!");
                } else {
                    Logger.getLogger(ImageConverter.class.getName()).log(Level.WARNING, "Conversion problem encountered, no joy on " + outputFile.getName());
                }
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(ImageConverter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
