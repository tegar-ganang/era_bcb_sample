package org.jpedal.examples.images;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ResourceBundle;
import org.jpedal.PdfDecoder;
import org.jpedal.io.JAIHelper;
import org.jpedal.objects.PdfImageData;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

/**
 * Sample code providing a workflow which extracts clipped images and places versions
 * scaled to specific hights Scope:<b>(Ent only)</b>
 * 
 * It is run using the format
 *
 * java -cp libraries_needed org/jpedal/examples/ ExtractClippedImages $inputDir $processedDir $logFile h1 dir1 h2 dir2 ... hn dirn 
 * 
 * Values with SPACES but be surrounded by "" as in "This is one value"
 * The values passed are
 *
 * $inputDir - directory containing files
 * $processedDIr - directory to put files in
 * $log - path and name of logfile
 *
 * Any number of h - height required in pixels as an integer for output (-1 means keep current size) dir1 - directory to write out images
 *
 * So to create 3 versions of the image (one at original size, one at 100 and one at 50 pixels high), you would use
 *
 * java -cp libraries_needed org/jpedal/examples/ ExtractScalesImages /export/files/ /export/processedFiles/ /logs/image.log -1 /output/raw/ 100 /output/medium/ 50 /output/thumbnail/
 *
 * Note image quality depends on the raw image in the original.
 * 
 * This can be VERY memory intensive 
 *
 */
public class ExtractClippedImages {

    /**flag to show if we print messages*/
    public static boolean outputMessages = false;

    /**directory to place files once decoded*/
    private static String processed_dir = "processed";

    /**rootDir containing files*/
    private static String inputDir = "";

    /**number of output directories*/
    private static int outputCount;

    /**sizes to output at -1 means unchanged*/
    private static float[] outputSizes;

    /**target directories for files*/
    private static String[] outputDirectories;

    /**the decoder object which decodes the pdf and returns a data object*/
    PdfDecoder decode_pdf = null;

    /**correct separator for OS */
    private static final String separator = System.getProperty("file.separator");

    /**location output files written to*/
    private String output_dir = "clippedImages";

    /**type of image to save*/
    private String imageType = "tiff";

    private static RenderingHints hint = null;

    static {
        hint = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        hint.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        hint.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
    }

    /**example method to extract the images from a directory*/
    public ExtractClippedImages(String rootDir) {
        processFiles(rootDir);
    }

    /**example method to extract the images from a directory*/
    private void processFiles(String rootDir) {
        if ((!rootDir.endsWith("\\")) && (!rootDir.endsWith("/"))) rootDir = rootDir + separator;
        if (!processed_dir.endsWith(separator)) processed_dir = processed_dir + separator;
        File testDir = new File(rootDir);
        if (!testDir.isDirectory()) {
            exit("No root directory " + rootDir);
        }
        String[] files = null;
        try {
            File inputFiles = new File(rootDir);
            System.out.println(inputFiles.getAbsolutePath());
            if (!inputFiles.isDirectory()) {
                System.err.println(rootDir + " is not a directory. Exiting program");
            }
            files = inputFiles.list();
        } catch (Exception ee) {
            exit("Exception trying to access file " + ee.getMessage());
        }
        long fileCount = files.length;
        for (int i = 0; i < fileCount; i++) {
            if (files[i].toLowerCase().endsWith(".pdf")) {
                if (outputMessages) System.out.println(rootDir + files[i]);
                decode(rootDir + files[i], 72);
                File currentFile = new File(rootDir + files[i]);
                currentFile.renameTo(new File(processed_dir + files[i]));
            }
        }
    }

    /**
	 * exit routine for code
	 */
    private static void exit(String string) {
        System.out.println("Exit message " + string);
        LogWriter.writeLog("Exit message " + string);
    }

    /**
	 * routine to decode a PDF file
	 */
    private void decode(String file_name, int dpi) {
        String name = "demo";
        LogWriter.writeLog("==================");
        LogWriter.writeLog("File " + file_name);
        int pointer = file_name.lastIndexOf(separator);
        if (pointer != -1) name = file_name.substring(pointer + 1, file_name.length() - 4);
        try {
            decode_pdf = new PdfDecoder(false);
            decode_pdf.setExtractionMode(PdfDecoder.FINALIMAGES + PdfDecoder.CLIPPEDIMAGES, dpi, 1);
            decode_pdf.openPdfFile(file_name);
        } catch (Exception e) {
            exit(Messages.getMessage("PdfViewerError.Exception") + " " + e + " " + Messages.getMessage("PdfViewerError.OpeningPdfFiles"));
        }
        if ((decode_pdf.isEncrypted() && (!decode_pdf.isPasswordSupplied())) && (!decode_pdf.isExtractionAllowed())) {
            exit(Messages.getMessage("PdfViewerError.EncryptedNotSupported"));
        } else {
            int start = 1, end = decode_pdf.getPageCount();
            try {
                for (int page = start; page < end + 1; page++) {
                    LogWriter.writeLog(Messages.getMessage("PdfViewerDecoding.page") + " " + page);
                    decode_pdf.decodePage(page);
                    PdfImageData pdf_images = decode_pdf.getPdfImageData();
                    int image_count = pdf_images.getImageCount();
                    if (image_count > 0) LogWriter.writeLog("page" + " " + page + "contains " + image_count + " images"); else LogWriter.writeLog("No bitmapped images on page " + page);
                    LogWriter.writeLog("Writing out images");
                    for (int i = 0; i < image_count; i++) {
                        String image_name = pdf_images.getImageName(i);
                        BufferedImage image_to_save;
                        float x1 = pdf_images.getImageXCoord(i);
                        float y1 = pdf_images.getImageYCoord(i);
                        float w = pdf_images.getImageWidth(i);
                        float h = pdf_images.getImageHeight(i);
                        for (int versions = 0; versions < outputCount; versions++) {
                            try {
                                String type = decode_pdf.getObjectStore().getImageType(image_name);
                                image_to_save = decode_pdf.getObjectStore().loadStoredImage("CLIP_" + image_name);
                                int index = file_name.lastIndexOf('\\');
                                if (index == -1) index = file_name.lastIndexOf('/');
                                if (index == -1) index = 0;
                                String nameToUse = file_name.substring(index, file_name.length() - 4);
                                String outputName = outputDirectories[versions] + nameToUse + "_" + page + "_" + i;
                                float scaling = 1;
                                int newHeight = image_to_save.getHeight();
                                if (outputSizes[versions] > 0) {
                                    scaling = outputSizes[versions] / newHeight;
                                    if (scaling > 1) {
                                        scaling = 1;
                                    } else {
                                        Image scaledImage = image_to_save.getScaledInstance(-1, (int) outputSizes[versions], BufferedImage.SCALE_SMOOTH);
                                        image_to_save = new BufferedImage(scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                                        Graphics2D g2 = image_to_save.createGraphics();
                                        g2.drawImage(scaledImage, 0, 0, null);
                                    }
                                }
                                String tiffFlag = System.getProperty("compress_tiff");
                                boolean compressTiffs = tiffFlag != null;
                                JAIHelper.confirmJAIOnClasspath();
                                if (compressTiffs && JAIHelper.isJAIused()) {
                                    com.sun.media.jai.codec.TIFFEncodeParam params = null;
                                    params = new com.sun.media.jai.codec.TIFFEncodeParam();
                                    params.setCompression(com.sun.media.jai.codec.TIFFEncodeParam.COMPRESSION_DEFLATE);
                                    FileOutputStream os = new FileOutputStream(outputName + ".tif");
                                    javax.media.jai.JAI.create("encode", image_to_save, os, "TIFF", params);
                                    os.flush();
                                    os.close();
                                } else {
                                    decode_pdf.getObjectStore().saveStoredImage(outputName, image_to_save, true, false, imageType);
                                }
                                OutputStreamWriter output_stream = new OutputStreamWriter(new FileOutputStream(outputName + ".xml"), "UTF-8");
                                output_stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                                output_stream.write("<!-- Pixel Location of image x1,y1,x2,y2\n");
                                output_stream.write("(x1,y1 is top left corner)\n");
                                output_stream.write("(origin is bottom left corner)  -->\n");
                                output_stream.write("\n\n<META>\n");
                                output_stream.write("<PAGELOCATION x1=\"" + x1 + "\" " + "y1=\"" + (y1 + h) + "\" " + "x2=\"" + (x1 + w) + "\" " + "y2=\"" + (y1) + "\" />\n");
                                output_stream.write("<FILE>" + file_name + "</FILE>\n");
                                output_stream.write("<ORIGINALHEIGHT>" + newHeight + "</ORIGINALHEIGHT>\n");
                                output_stream.write("<SCALEDHEIGHT>" + image_to_save.getHeight() + "</SCALEDHEIGHT>\n");
                                output_stream.write("<SCALING>" + scaling + "</SCALING>\n");
                                output_stream.write("</META>\n");
                                output_stream.close();
                            } catch (Exception ee) {
                                LogWriter.writeLog("Exception " + ee + " in extracting images");
                            }
                        }
                    }
                    decode_pdf.flushObjectValues(true);
                }
            } catch (Exception e) {
                decode_pdf.closePdfFile();
                LogWriter.writeLog("Exception " + e.getMessage());
            }
        }
        decode_pdf.closePdfFile();
    }

    /**
	 * @return Returns the output_dir.
	 */
    public String getOutputDir() {
        return output_dir;
    }

    /**
	 * main routine which checks for any files passed and runs the demo
	 */
    public static void main(String[] args) {
        Messages.setBundle(ResourceBundle.getBundle("org.jpedal.international.messages"));
        if (outputMessages) System.out.println("Simple demo to extract images from a page at various heights");
        if (((args.length & 1) == 0) | (args.length < 5)) {
            LogWriter.writeLog("Values read");
            LogWriter.writeLog("inputDir=" + inputDir);
            LogWriter.writeLog("processedDir=" + processed_dir);
            LogWriter.writeLog("logFile=" + LogWriter.log_name);
            LogWriter.writeLog("Directory and height pair values");
            for (int i = 3; i < outputCount; i++) LogWriter.writeLog(args[i]);
            if ((args.length < 5) | ((args.length & 1) == 0)) {
                System.out.println("Requires");
                System.out.println("inputDir processedDir logFile");
                System.out.println("height Directory (as many pairs as you like)");
                exit("Not enough parameters passed to software");
            } else exit("Incorrect number of values");
        }
        inputDir = args[0];
        processed_dir = args[1];
        File pdf_file = new File(inputDir);
        File processedDir = new File(processed_dir);
        if (!processedDir.exists()) processedDir.mkdirs();
        if (pdf_file.exists() == false) exit("Directory " + inputDir + " not found");
        outputCount = (args.length - 3) / 2;
        outputSizes = new float[outputCount];
        outputDirectories = new String[outputCount];
        for (int i = 0; i < outputCount; i++) {
            try {
                outputSizes[i] = Float.parseFloat(args[3 + (i * 2)]);
            } catch (Exception e) {
                exit("Exception " + e + " reading integer " + args[3 + (i * 2)]);
            }
            try {
                outputDirectories[i] = args[4 + (i * 2)];
                if ((!outputDirectories[i].endsWith("\\")) && (!outputDirectories[i].endsWith("/"))) outputDirectories[i] = outputDirectories[i] + separator;
                File dir = new File(outputDirectories[i]);
                if (!dir.exists()) dir.mkdirs();
            } catch (Exception e) {
                exit("Exception " + e + " with directory " + args[4 + (i * 2)]);
            }
        }
        ExtractClippedImages images1 = new ExtractClippedImages(inputDir);
        LogWriter.writeLog("Process completed");
    }
}
