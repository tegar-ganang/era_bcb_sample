package work.utilities;

import geometry.serialization.util.PolygonLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.OptionHelper;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.TopologyException;

/**
 * @author Sebastian Kuerten (sebastian.kuerten@fu-berlin.de)
 * 
 *         This tool selects from a set of files those geometries that are being covered by a
 *         denoted boundary b to a certain degree and copies those matching geometry files to an
 *         output directory.
 */
public class GeometrySelector {

    static final Logger logger = LoggerFactory.getLogger(GeometrySelector.class);

    private static final double DEFAULT_THRESHOLD = 0.9;

    private static final String HELP_MESSAGE = "GeometrySelector [args] <files...>";

    private static final String OPTION_BOUNDARY = "boundary";

    private static final String OPTION_THRESHOLD = "threshold";

    private static final String OPTION_OUTPUT = "output";

    /**
	 * @param args
	 *            the program arguments
	 */
    public static void main(String[] args) {
        Options options = new Options();
        OptionHelper.add(options, OPTION_BOUNDARY, true, true, "a boundary to use for selection of files");
        OptionHelper.add(options, OPTION_THRESHOLD, true, false, "a threshold to use in coverage predicate " + "(this value is the relative coverage necessary " + "to include a given geometry)");
        OptionHelper.add(options, OPTION_OUTPUT, true, true, "a directory to copy the selected files to");
        CommandLine line = null;
        try {
            line = new GnuParser().parse(options, args);
        } catch (ParseException e) {
            System.out.println("unable to parse command line: " + e.getMessage());
            new HelpFormatter().printHelp(HELP_MESSAGE, options);
            System.exit(1);
        }
        if (line == null) return;
        String[] inputFiles = line.getArgs();
        if (inputFiles.length == 0) {
            new HelpFormatter().printHelp(HELP_MESSAGE, options);
            System.exit(1);
        }
        String boundaryFile = line.getOptionValue(OPTION_BOUNDARY);
        String outputDirectoryPath = line.getOptionValue(OPTION_OUTPUT);
        File outputDirectory = new File(outputDirectoryPath);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        if (!outputDirectory.exists() || !outputDirectory.canWrite() || !outputDirectory.isDirectory()) {
            System.out.println("unable to create or write to output directory");
            System.exit(1);
        }
        double threshold = DEFAULT_THRESHOLD;
        if (line.hasOption(OPTION_THRESHOLD)) {
            String thresholdArg = line.getOptionValue(OPTION_THRESHOLD);
            try {
                threshold = Double.parseDouble(thresholdArg);
            } catch (NumberFormatException e) {
                System.out.println("unable to parse threshold");
                System.exit(1);
            }
        }
        logger.info("loading selection boundary");
        Geometry boundary = null;
        try {
            boundary = PolygonLoader.readPolygon(boundaryFile);
        } catch (IOException e) {
            logger.debug("unable to read geometry: " + boundaryFile);
            System.exit(1);
        }
        logger.info("iterating files");
        for (String filename : inputFiles) {
            File file = new File(filename);
            String name = file.getName();
            boolean take = take(boundary, filename, threshold);
            if (!take) {
                continue;
            }
            File outFile = new File(outputDirectory, name);
            String output = outFile.getPath();
            try {
                logger.info("copying " + filename + " to " + output);
                copyFile(file, outFile);
            } catch (IOException e) {
                logger.debug("unable to copy from " + filename + " to " + output);
            }
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    private static boolean take(Geometry boundary, String filename, double threshold) {
        try {
            Geometry geometry = PolygonLoader.readPolygon(filename);
            logger.debug(geometry.getGeometryType());
            double area = geometry.getArea();
            logger.debug("area: " + area);
            if (!geometry.intersects(boundary)) {
                return false;
            }
            Geometry intersection = boundary.intersection(geometry);
            double iarea = intersection.getArea();
            logger.debug("intersection area: " + iarea);
            double relative = iarea / area;
            logger.debug("relative: " + relative);
            if (relative > threshold) {
                return true;
            }
        } catch (IOException e) {
            logger.info("unable to read geometry: " + filename);
        } catch (TopologyException e) {
            logger.info("TopologyException: " + e.getMessage());
        }
        return false;
    }
}
