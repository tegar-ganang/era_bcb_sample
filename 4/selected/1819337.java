package work.utilities;

import geometry.serialization.util.PolygonLoader;
import geometry.speedup.index.GeometryTesselationMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import util.OptionHelper;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.TopologyException;

/**
 * @author Sebastian Kuerten (sebastian.kuerten@fu-berlin.de)
 * 
 */
public class GeometrySeparator {

    static final Logger logger = LoggerFactory.getLogger(GeometrySeparator.class);

    private static final double DEFAULT_THRESHOLD = 0.9;

    private static final String HELP_MESSAGE = "GeometrySeparator [args] <files...>";

    private static final String OPTION_CONFIG = "config";

    private static final String OPTION_THRESHOLD = "threshold";

    /**
	 * @param args
	 *            the program arguments
	 */
    public static void main(String[] args) {
        Options options = new Options();
        OptionHelper.add(options, OPTION_CONFIG, true, true, "a configuration file that determines the mapping");
        OptionHelper.add(options, OPTION_THRESHOLD, true, false, "a threshold to use in coverage predicate " + "(this value is the relative coverage necessary " + "to include a given geometry)");
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
        String configFile = line.getOptionValue(OPTION_CONFIG);
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
        List<Mapping> mappings = null;
        try {
            mappings = readConfig(configFile);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mappings == null) {
            System.exit(1);
            return;
        }
        logger.debug("mappings: " + mappings);
        List<String> dirnames = new ArrayList<String>();
        List<Geometry> geometries = new ArrayList<Geometry>();
        for (Mapping mapping : mappings) {
            logger.debug("creating directory: " + mapping.dirname);
            String canonicalDirectory = createDirectory(mapping.dirname);
            if (canonicalDirectory == null) {
                logger.debug("unable to create directory: " + mapping.dirname);
                System.exit(1);
            }
            dirnames.add(canonicalDirectory);
        }
        for (Mapping mapping : mappings) {
            try {
                Geometry geometry = PolygonLoader.readPolygon(mapping.filename);
                geometries.add(geometry);
            } catch (IOException e) {
                logger.debug("unable to read geometry: " + mapping.filename);
                System.exit(1);
            }
        }
        GeometryTesselationMap<Entry> map = new GeometryTesselationMap<Entry>();
        for (int i = 0; i < geometries.size(); i++) {
            Geometry geometry = geometries.get(i);
            String dirname = dirnames.get(i);
            map.add(geometry, new Entry(geometry, dirname));
        }
        for (String filename : inputFiles) {
            File file = new File(filename);
            String name = file.getName();
            Collection<Entry> entries = executeMapping(map, filename, threshold);
            if (entries.size() == 0) {
                logger.info("unable to map: " + filename);
                continue;
            } else if (entries.size() > 1) {
                logger.info("mapped to " + entries.size() + ": " + filename);
            }
            for (Entry entry : entries) {
                String output = entry.dirname + File.separator + name;
                File outFile = new File(output);
                try {
                    logger.info("copying " + filename + " to " + output);
                    copyFile(file, outFile);
                } catch (IOException e) {
                    logger.debug("unable to copy from " + filename + " to " + output);
                }
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

    private static Collection<Entry> executeMapping(GeometryTesselationMap<Entry> map, String filename, double threshold) {
        List<Entry> selected = new ArrayList<Entry>();
        try {
            Geometry geometry = PolygonLoader.readPolygon(filename);
            logger.debug(geometry.getGeometryType());
            double area = geometry.getArea();
            logger.debug("area: " + area);
            Set<Entry> intersections = map.testForIntersections(geometry);
            logger.debug("intersections: " + intersections);
            for (Entry entry : intersections) {
                Geometry intersection = entry.geometry.intersection(geometry);
                double iarea = intersection.getArea();
                logger.debug("intersection area: " + iarea);
                double relative = iarea / area;
                logger.debug("relative: " + relative);
                if (relative > threshold) {
                    selected.add(entry);
                }
            }
        } catch (IOException e) {
            logger.info("unable to read geometry: " + filename);
        } catch (TopologyException e) {
            logger.info("TopologyException: " + e.getMessage());
        }
        return selected;
    }

    private static String createDirectory(String directory) {
        File file = new File(directory);
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
        if (!file.exists()) {
            boolean success = file.mkdirs();
            if (!success) {
                return null;
            }
        }
        if (!file.isDirectory()) {
            return null;
        }
        return canonicalPath;
    }

    private static List<Mapping> readConfig(String filename) throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        ConfigHandler handler = new ConfigHandler();
        File file = new File(filename);
        if (file.exists() && file.canRead()) {
            parser.parse(file, handler);
        } else {
            throw new IOException("unable to read configuration file");
        }
        return handler.rules;
    }
}

class Entry {

    Geometry geometry;

    String dirname;

    public Entry(Geometry geometry, String dirname) {
        this.geometry = geometry;
        this.dirname = dirname;
    }
}

class Mapping {

    final String filename;

    final String dirname;

    public Mapping(String filename, String dirname) {
        this.filename = filename;
        this.dirname = dirname;
    }
}

class ConfigHandler extends DefaultHandler {

    List<Mapping> rules = new ArrayList<Mapping>();

    int level = 0;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        level++;
        if (level == 2) {
            if (qName.equals("mapping")) {
                String geometry = attributes.getValue("geometry");
                String directory = attributes.getValue("directory");
                Mapping mapping = new Mapping(geometry, directory);
                rules.add(mapping);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        level--;
    }
}
