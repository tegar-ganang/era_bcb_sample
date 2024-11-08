package com.drew.metadata.test;

import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.jpeg.JpegSegmentData;
import com.drew.imaging.jpeg.JpegSegmentReader;
import java.io.*;

/** @author Drew Noakes http://drewnoakes.com */
public class ExtractAppSegmentBytesToFileUtility {

    public static void main(String[] args) throws IOException, JpegProcessingException {
        if (args.length < 2) {
            System.err.println("Expects at least two arguments:\n\n    <filename> <appSegmentNumber> [<segmentOccurrence>]");
            System.exit(1);
        }
        byte segmentNumber = Byte.parseByte(args[1]);
        if (segmentNumber > 0xF) {
            System.err.println("Segment number must be between 0 (App0) and 15 (AppF).");
            System.exit(1);
        }
        byte segment = (byte) (JpegSegmentReader.SEGMENT_APP0 + segmentNumber);
        String filePath = args[0];
        JpegSegmentData segmentData = new JpegSegmentReader(new File(filePath)).getSegmentData();
        final int segmentCount = segmentData.getSegmentCount(segment);
        if (segmentCount == 0) {
            System.err.printf("No data was found in app segment %d.\n", segmentNumber);
            System.exit(1);
        }
        if (segmentCount != 1 && args.length == 2) {
            System.err.printf("%d occurrences of segment %d found.  You must specify which index to use (zero based).\n", segmentCount, segmentNumber);
            System.exit(1);
        }
        int occurrence = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        if (occurrence >= segmentCount) {
            System.err.printf("Invalid occurrence number.  Requested %d but only %d segments exist.\n", occurrence, segmentCount);
            System.exit(1);
        }
        String outputFilePath = filePath + ".app" + segmentNumber + "bytes";
        final byte[] bytes = segmentData.getSegment(segment, occurrence);
        System.out.println("Writing output to: " + outputFilePath);
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(outputFilePath);
            stream.write(bytes);
        } finally {
            if (stream != null) stream.close();
        }
    }

    public static byte[] read(File file) throws IOException {
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read;
            while ((read = ios.read(buffer)) != -1) ous.write(buffer, 0, read);
        } finally {
            if (ous != null) ous.close();
            if (ios != null) ios.close();
        }
        return ous.toByteArray();
    }
}
