package org.rdv.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * A class to read data from a data file.
 * 
 * @author Jason P. Hanley
 */
public class DataFileReader {

    /** The data file */
    private URL file;

    /** The reader for the data file */
    private BufferedReader reader;

    /** The number of lines in the header */
    private int headerLength;

    /** The properties for the data file */
    private Map<String, String> properties;

    /** The channels in the data file */
    private List<DataChannel> channels;

    /** The delimiter for properties */
    private String propertyDelimiter = ":";

    /** The delimiters to try for the data items. Tab, comma, and semi-colon */
    private String delimiters = "[\t,;]";

    /** The last line read in the file. */
    private String line;

    /** The start time for the data */
    private double startTime;

    /** Indicates if there is a time column */
    private boolean hasTimeColumn;

    /** Indicates that the time column is in ISO8601 format */
    private boolean timeIsISO8601;

    /** The number of samples read */
    private int samples;

    /** The date format for ISO8601 */
    private static final SimpleDateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    static {
        ISO8601_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** The keys that can denote a list of channels */
    private static final String[] channelPropertyKeys = { "channel", "channels", "channel name", "channel names", "active channel", "active channels" };

    /** The keys that can denote a list of units */
    private static final String[] unitPropertyKeys = { "unit", "units", "channel unit", "channel units" };

    /** The keys that can have the start time */
    private static final String[] startTimePropertyKeys = { "start time" };

    /** The number of lines to parse before giving up on understanding the file */
    private static final int MAX_HEADER_LINES = 100;

    /**
   * Create a new data file reader and try to read the data file's header.
   * 
   * @param file          the data file
   * @throws IOException  if there is an error opening or reading the data file
   */
    public DataFileReader(File file) throws IOException {
        this(file.toURI().toURL());
    }

    /**
   * Create a new data file reader and try to read the data file's header.
   * 
   * @param file          the data file URL
   * @throws IOException  if there is an error opening or reading the data file
   */
    public DataFileReader(URL file) throws IOException {
        this.file = file;
        reader = getReader();
        properties = new Hashtable<String, String>();
        channels = new ArrayList<DataChannel>();
        hasTimeColumn = true;
        try {
            readHeader();
        } catch (IOException e) {
            reader.close();
            reader = getReader();
            properties.clear();
            channels.clear();
            delimiters = " +";
            readHeader();
        }
        startTime = 0;
        if (getProperty(startTimePropertyKeys) != null) {
            try {
                startTime = parseTimestamp(getProperty(startTimePropertyKeys));
            } catch (ParseException e) {
            }
        }
        samples = 0;
        parseUnitProperty();
        if (properties.get("samples") == null) {
            int samples = scanData();
            properties.put("samples", Long.toString(samples));
        }
    }

    /**
   * Get the property value for the specified key.
   * 
   * @param key  the property key
   * @return     the property value, or null if there is no property for the key
   */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
   * Get the properties in the data file header.
   * 
   * @return  the data file properties
   */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
   * Get a list of channels in the data file.
   * 
   * @return  a list of channels
   */
    public List<DataChannel> getChannels() {
        return channels;
    }

    /**
   * Reads and returns a data sample from the data file. Null will be returned
   * when the end of the file is reached.
   * 
   * @return              the data sample, or null if the end of the file is
   *                      reached
   * @throws IOException  if there is an error reading the data file
   */
    public NumericDataSample readSample() throws IOException {
        if (line == null) {
            return null;
        }
        int firstDataIndex;
        if (hasTimeColumn) {
            firstDataIndex = 1;
        } else {
            firstDataIndex = 0;
        }
        do {
            line = line.trim();
            String[] tokens = line.split(delimiters);
            if (tokens.length != channels.size() + firstDataIndex) {
                continue;
            }
            double timestamp;
            Number[] values = new Number[channels.size()];
            if (!hasTimeColumn) {
                timestamp = samples;
            } else if (timeIsISO8601) {
                try {
                    timestamp = parseTimestamp(tokens[0].trim());
                } catch (ParseException e) {
                    continue;
                }
            } else {
                try {
                    timestamp = startTime + Double.parseDouble(tokens[0].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
            }
            for (int i = firstDataIndex; i < tokens.length; i++) {
                Number value;
                try {
                    value = Double.parseDouble(tokens[i].trim());
                } catch (NumberFormatException e) {
                    value = null;
                }
                values[i - firstDataIndex] = value;
            }
            samples++;
            line = reader.readLine();
            return new NumericDataSample(timestamp, values);
        } while ((line = reader.readLine()) != null);
        return null;
    }

    /**
   * Gets a buffered reader for the data file. If the data file is compressed
   * with zip or gzip it will, the reader will output the uncompressed stream.
   * 
   * @return              a read for the data file
   * @throws IOException  if an error occurs creating the reader
   */
    private BufferedReader getReader() throws IOException {
        InputStream inputStream = file.openStream();
        String path = file.getPath().toLowerCase();
        if (path.endsWith(".zip")) {
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            zipInputStream.getNextEntry();
            inputStream = zipInputStream;
        } else if (path.endsWith(".gz")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    /**
   * Reads the header of the data file. This constructs the data file properties
   * and list of channels.
   * 
   * @throws IOException  if there is an error reading the data file
   */
    private void readHeader() throws IOException {
        headerLength = 0;
        while ((line = reader.readLine()) != null && headerLength++ < MAX_HEADER_LINES) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (channels.size() == 0) {
                String[] property = line.split(propertyDelimiter, 2);
                if (property.length == 2) {
                    properties.put(stripString(property[0]).toLowerCase(), stripString(property[1]));
                    continue;
                }
            }
            String[] tokens = line.split(delimiters);
            String firstToken = stripString(tokens[0]);
            if (channels.size() == 0 && (firstToken.compareToIgnoreCase("time") == 0 || isKey(firstToken, channelPropertyKeys))) {
                for (int i = 1; i < tokens.length; i++) {
                    DataChannel channel = new DataChannel(stripString(tokens[i]));
                    channels.add(channel);
                }
                if (channels.size() == 0) {
                    throw new IOException("No channels found in data file.");
                }
            } else if (channels.size() == 0 || tokens.length == channels.size() + 1) {
                if (isNumber(firstToken)) {
                    if (channels.size() == 0) {
                        hasTimeColumn = false;
                        generateFakeChannels(tokens.length);
                    }
                    return;
                } else if (isTimestamp(firstToken)) {
                    timeIsISO8601 = true;
                    if (channels.size() == 0) {
                        generateFakeChannels(tokens.length - 1);
                    }
                    return;
                } else if (channels.size() > 0 && (firstToken.toLowerCase().startsWith("sec") || isKey(firstToken, unitPropertyKeys))) {
                    for (int i = 1; i < tokens.length; i++) {
                        DataChannel channel = channels.get(i - 1);
                        channel.setUnit(stripString(tokens[i]));
                    }
                }
            }
        }
        if (channels.size() == 0) {
            throw new IOException("No channels found in data file.");
        } else {
            throw new IOException("No data found in data file.");
        }
    }

    /**
   * Scan the data and determine how many samples there are in the file. Note
   * that this will only return an approximation.
   * 
   * @return              an approximate number of samples in the file
   * @throws IOException  if there is an error reading the file
   */
    private int scanData() throws IOException {
        int samples = 1;
        while (reader.readLine() != null) {
            samples++;
        }
        reader.close();
        reader = getReader();
        int i = 0;
        while (i++ < headerLength) {
            reader.readLine();
        }
        return samples;
    }

    /**
   * Looks for the channel units in the properties and if found, puts them in
   * the <code>DataFileChannel</code>.
   *
   */
    private void parseUnitProperty() {
        String unitsString = getProperty(unitPropertyKeys);
        if (unitsString == null) {
            return;
        }
        String[] units = unitsString.trim().split(delimiters);
        if (units.length != channels.size()) {
            return;
        }
        for (int i = 0; i < units.length; i++) {
            DataChannel channel = channels.get(i);
            channel.setUnit(units[i]);
        }
    }

    /**
   * See if there is a property for any of the keys and return it if there is.
   * This will return the first property found.
   * 
   * @param keys  the keys to look for
   * @return      the property value, or null if not found
   */
    private String getProperty(String[] keys) {
        for (String key : keys) {
            String value = properties.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
   * See if the search key is contained in the keys.
   * 
   * @param searchKey  the key to search for
   * @param keys       the keys to look in
   * @return           true if the search key is found, false otherwise
   */
    private boolean isKey(String searchKey, String[] keys) {
        for (String key : keys) {
            if (key.compareToIgnoreCase(searchKey) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
   * Populate the list of channels. The channels will be named by their index.
   * 
   * @param numberOfChannels  the number of channels to create.
   */
    private void generateFakeChannels(int numberOfChannels) {
        for (int i = 0; i < numberOfChannels; i++) {
            DataChannel channel = new DataChannel(Integer.toString(i + 1));
            channels.add(channel);
        }
    }

    /**
   * Test if this string is a number.
   * 
   * @param value  the value string
   * @return       true if it is a number, false otherwise
   */
    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
   * Test if the string is an ISO8601 timestamp.
   * 
   * @param iso8601  the timestamp string
   * @return         true if it is a timestamp, false otherwise
   */
    private static boolean isTimestamp(String iso8601) {
        try {
            parseTimestamp(iso8601);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    /**
   * Parse the ISO8601 timestamp.
   * 
   * @param timestamp        the timestamp to parse
   * @return                 the time in seconds
   * @throws ParseException  if there is an error parsing the timestamp
   */
    private static double parseTimestamp(String timestamp) throws ParseException {
        if (timestamp.endsWith("Z")) {
            timestamp = timestamp.substring(0, timestamp.length() - 1);
        }
        String[] parts = timestamp.split("\\.", 2);
        timestamp = parts[0];
        double subseconds;
        if (parts.length == 2) {
            try {
                subseconds = Double.parseDouble("0." + parts[1]);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid subseconds field", timestamp.length());
            }
        } else {
            subseconds = 0;
        }
        double time = ISO8601_DATE_FORMAT.parse(timestamp).getTime() / 1000d;
        time += subseconds;
        return time;
    }

    /**
   * Removes leading and trailing white space from a string. If the string is
   * quoted, the quotes are also stripped.
   * 
   * @param cell  the string to strip
   * @return      the stripped string
   */
    private static String stripString(String cell) {
        cell = cell.trim();
        if (cell.startsWith("\"") && cell.endsWith("\"")) {
            if (cell.length() > 2) {
                cell = cell.substring(1, cell.length() - 1).trim();
            } else {
                cell = "";
            }
        }
        return cell;
    }
}
