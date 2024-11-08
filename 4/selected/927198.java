package org.mcisb.massspectrometry;

import java.beans.*;
import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import mr.go.sgfilter.*;
import org.mcisb.util.*;
import org.mcisb.util.data.*;
import org.mcisb.util.io.*;
import org.mcisb.util.math.*;
import org.mcisb.util.xml.*;

/**
 * @author Neil Swainston
 */
public class SpectraUtils {

    /**
	 * 
	 * @param directory
	 * @param extension
	 * @throws Exception
	 */
    public static void convertFiles(final File directory, final String extension) throws Exception {
        if (PeakListWriterFactory.getSupportedExtensions().contains(extension) && directory.isDirectory()) {
            final File[] directoryFiles = directory.listFiles();
            for (final File file : directoryFiles) {
                if (PeakListReaderFactory.getSupportedExtensions().contains(FileUtils.getExtension(file))) {
                    convertFile(file, new File(file.getAbsolutePath() + "." + extension));
                }
            }
        }
    }

    /**
	 * @param in
	 * @param out
	 * @return int
	 * @throws Exception 
	 * @throws java.lang.Exception
	 */
    public static int convertFile(final File in, final File out) throws Exception {
        return convertFile(in, out, null);
    }

    /**
	 * @param in
	 * @param out
	 * @param peakList
	 * @return int
	 * @throws java.lang.Exception
	 */
    public static int convertFile(final File in, final File out, final PeakList peakList) throws java.lang.Exception {
        int returnValue = Executor.SUCCESS;
        File formattedIn = in;
        if (FileUtils.getExtension(in).equalsIgnoreCase(PropertyNames.RAW_EXTENSION)) {
            final String READW = "reAdW";
            final String P = "p";
            File mzDataFile = null;
            if (FileUtils.getExtension(out).equalsIgnoreCase(PropertyNames.MZXML_EXTENSION)) {
                mzDataFile = out;
            } else {
                mzDataFile = File.createTempFile("temp", "." + PropertyNames.MZXML_EXTENSION);
            }
            returnValue = ((Integer) new Executor(new String[] { READW, in.getAbsolutePath(), P, mzDataFile.getAbsolutePath() }).doTask()).intValue();
            if (FileUtils.getExtension(out).equalsIgnoreCase(PropertyNames.MZXML_EXTENSION)) {
                return returnValue;
            }
            if (returnValue != Executor.SUCCESS) {
                throw new IOException(Integer.toString(returnValue));
            }
            formattedIn = mzDataFile;
        }
        final PeakList currentPeakList = mergePeakLists(new File[] { formattedIn }, out, peakList != null);
        if (peakList != null) {
            peakList.addAll(currentPeakList);
        }
        return returnValue;
    }

    /**
	 * @param in
	 * @return File
	 * @throws java.io.IOException
	 * @throws XMLStreamException
	 */
    public static File stripSpectrum(final File in) throws java.io.IOException, XMLStreamException {
        final String SPECTRUM = "spectrum";
        final File out = File.createTempFile(in.getName(), "." + org.mcisb.massspectrometry.PropertyNames.MZDATA_EXTENSION);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(in);
            os = new FileOutputStream(out);
            XmlUtils.stripElements(is, os, Arrays.asList(SPECTRUM));
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
        return out;
    }

    /**
	 * 
	 * @param targetMass
	 * @param targetMassTolerance
	 * @param targetRt
	 * @param targetRtTolerance 
	 * @param rtScans 
	 * @param xValuesScans 
	 * @param yValuesScans 
	 * @return Map
	 */
    public static double[][] getExtractedMassChromatogram(final float targetMass, final float targetMassTolerance, final float targetRt, final float targetRtTolerance, final float[] rtScans, final List<double[]> xValuesScans, final List<double[]> yValuesScans) {
        final double[] xicRts = new double[rtScans.length];
        final double[] xicIntensities = new double[rtScans.length];
        int index = 0;
        for (int i = 0; i < rtScans.length; i++) {
            double xicIntensity = 0.0;
            if (rtScans[i] > (targetRt - targetRtTolerance) && rtScans[i] < (targetRt + targetRtTolerance)) {
                final double[] xValuesScan = xValuesScans.get(i);
                final double[] yValuesScan = yValuesScans.get(i);
                for (int j = 0; j < xValuesScan.length; j++) {
                    if (xValuesScan[j] < (targetMass - targetMassTolerance)) {
                        continue;
                    }
                    if (Math.abs(xValuesScan[j] - targetMass) < targetMassTolerance) {
                        xicIntensity += yValuesScan[j];
                    } else {
                        break;
                    }
                }
                if (xicIntensity > 0) {
                    xicRts[index] = rtScans[i];
                    xicIntensities[index] = xicIntensity;
                    index++;
                }
            }
        }
        final double[][] spectrum = new double[SpectrumUtils.SPECTRUM_DIMENSIONS][];
        spectrum[SpectrumUtils.X] = Arrays.copyOf(xicRts, index);
        spectrum[SpectrumUtils.Y] = Arrays.copyOf(xicIntensities, index);
        return spectrum;
    }

    /**
	 * 
	 * @param startRt 
	 * @param endRt 
	 * @param rtScans
	 * @param xValuesScans
	 * @param yValuesScans
	 * @return double[][][]
	 */
    public static double[][][] getPrecursorSpectra(final double startRt, final double endRt, final float[] rtScans, final List<double[]> xValuesScans, final List<double[]> yValuesScans) {
        final double[][][] precursorSpectra = new double[rtScans.length][SpectrumUtils.SPECTRUM_DIMENSIONS][];
        int index = 0;
        for (int i = 0; i < rtScans.length; i++) {
            if (rtScans[i] > endRt) {
                break;
            } else if (rtScans[i] < startRt) {
                continue;
            }
            precursorSpectra[index][SpectrumUtils.X] = xValuesScans.get(i);
            precursorSpectra[index][SpectrumUtils.Y] = yValuesScans.get(i);
            index++;
        }
        return Arrays.copyOf(precursorSpectra, index);
    }

    /**
	 * 
	 * @param xValues
	 * @param yValues
	 * @param xValue
	 * @param tolerance
	 * @return int[]
	 */
    public static int[] getPeakIndices(final double[] xValues, final double[] yValues, final double xValue, final float tolerance) {
        final int nearestXIndex = CollectionUtils.getNearestIndex(xValues, xValue, tolerance);
        int startIndex = nearestXIndex - 1;
        int endIndex = nearestXIndex + 1;
        int midIndex = nearestXIndex;
        if (nearestXIndex != NumberUtils.UNDEFINED) {
            final int PEAKS = 5;
            final int DEGREE = 3;
            final double[] coefficients = SGFilter.computeSGCoefficients(PEAKS, PEAKS, DEGREE);
            final SGFilter sgFilter = new SGFilter(PEAKS, PEAKS);
            final double[] smoothedYValues = sgFilter.smooth(yValues, coefficients);
            final double[] smoothedYDerivatives = MathUtils.derivative(smoothedYValues);
            if (smoothedYDerivatives[nearestXIndex] >= 0) {
                while (startIndex >= 0 && smoothedYDerivatives[startIndex] > 0) {
                    startIndex--;
                }
                while (endIndex < smoothedYDerivatives.length && smoothedYDerivatives[endIndex] > 0) {
                    endIndex++;
                }
                midIndex = endIndex - 1;
                while (endIndex < smoothedYDerivatives.length && smoothedYDerivatives[endIndex] < 0) {
                    endIndex++;
                }
            } else {
                while (startIndex >= 0 && smoothedYDerivatives[startIndex] < 0) {
                    startIndex--;
                }
                midIndex = startIndex + 1;
                while (startIndex >= 0 && smoothedYDerivatives[startIndex] > 0) {
                    startIndex--;
                }
                while (endIndex < smoothedYDerivatives.length && smoothedYDerivatives[endIndex] < 0) {
                    endIndex++;
                }
            }
        }
        startIndex++;
        endIndex--;
        return new int[] { startIndex, midIndex, endIndex };
    }

    /**
	 * @param in
	 * @param output
	 * @param peakListRequired 
	 * @return PeakList
	 * @throws Exception
	 */
    private static PeakList mergePeakLists(final File[] in, final File output, final boolean peakListRequired) throws Exception {
        PeakListReader reader = null;
        PeakListWriter writer = null;
        final PeakList peakList = new PeakList();
        PropertyChangeListener propertyChangeListener = null;
        try {
            writer = PeakListWriterFactory.newInstance(output);
            for (int i = 0; i < in.length; i++) {
                reader = PeakListReaderFactory.newInstance(in[i]);
                reader.addPropertyChangeListener(writer);
                if (peakListRequired) {
                    propertyChangeListener = new PropertyChangeListener() {

                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getPropertyName().equals(PeakListReader.ADD) && evt.getNewValue() instanceof Peak) {
                                peakList.add((Peak) evt.getNewValue());
                            }
                        }
                    };
                    reader.addPropertyChangeListener(propertyChangeListener);
                }
                reader.readPeaks();
                reader.close();
            }
            return peakListRequired ? peakList : null;
        } finally {
            if (reader != null) {
                reader.close();
                reader.removePropertyChangeListener(writer);
                reader.removePropertyChangeListener(propertyChangeListener);
            }
            if (writer != null) {
                writer.close();
            }
        }
    }
}
