package edu.sdsc.cleos;

import edu.sdsc.cleos.ISOtoRbnbTime;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SeabirdParser extends Hashtable {

    private static Logger logger = Logger.getLogger(SeabirdParser.class.getName());

    private HashMap<String, String> monthMap;

    private String[] channelNames;

    private String[] units;

    private String[] metadataChannels;

    public SeabirdParser() {
        super();
        initMonthMap();
        channelNames = new String[4];
        channelNames[0] = "Temperature";
        channelNames[1] = "Conductivity";
        channelNames[2] = "Pressure";
        channelNames[3] = "Salinity";
        units = new String[channelNames.length];
        units[0] = "C";
        units[1] = "S/M";
        units[2] = "dbar";
        units[3] = "psu";
        metadataChannels = new String[2];
        metadataChannels[0] = "Model";
        metadataChannels[1] = "Serial Number";
    }

    public void parseMetaData() {
        put("channels", channelNames);
        put("units", units);
        put("metadata-channels", metadataChannels);
        if (get("ds") != null) {
            parseAndPut((String) get("ds"), "\\A[S>ds]+[\\s]+(.+) SERIAL NO\\. [0-9]+ ", "model");
            parseAndPut((String) get("ds"), "SERIAL NO\\. ([0-9]+) ", "serial");
        } else {
            put("model", "SeacatPlus V 1.7");
            put("serial", "4974");
        }
    }

    protected void parseAndPut(String regexTarget, String regex, String hashKey) {
        logger.finer("Checking:" + regexTarget + " for: " + regex);
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(regexTarget);
        if (matcher.find()) {
            logger.finer("Matched group:" + matcher.group(1) + " for key:" + hashKey);
            put(hashKey, matcher.group(1).trim());
            logger.finer(hashKey + ":" + (String) get(hashKey) + " entered into hash");
        } else {
            logger.finer("No match for:" + hashKey);
        }
    }

    public double[] getData(String seabirdLine) throws ParseException, SeabirdException {
        logger.fine("getData line:" + seabirdLine);
        String[] seabirdLineSplit = seabirdLine.split("[\\s]*,[\\s]*");
        StringBuffer dateBuffer = new StringBuffer();
        for (int i = 0; i < seabirdLineSplit.length; i++) {
            logger.finest("Split:" + seabirdLineSplit[i]);
        }
        dateBuffer.append(seabirdLineSplit[seabirdLineSplit.length - 2]);
        dateBuffer.append(" ");
        dateBuffer.append(seabirdLineSplit[seabirdLineSplit.length - 1]);
        double timeNow = getRBNBDate(dateBuffer.toString());
        logger.finer("Seabird date string:" + dateBuffer.toString());
        logger.finer("RBNB date:" + timeNow);
        logger.finer("Nice RBNB date:" + ISOtoRbnbTime.formatDate((long) (timeNow * 1000)));
        double[] retval = new double[seabirdLineSplit.length - 1];
        for (int i = 0; i < retval.length - 1; i++) {
            try {
                retval[i] = Double.parseDouble(seabirdLineSplit[i]);
            } catch (NumberFormatException nfe) {
                throw new SeabirdException("Tokens munged:" + seabirdLineSplit[i]);
            }
        }
        retval[retval.length - 1] = timeNow;
        return retval;
    }

    public double getRBNBDate(String seabirdDate) throws ParseException, SeabirdException {
        String[] seabirdDateTokens = seabirdDate.split(" ");
        StringBuffer retval = new StringBuffer();
        if (seabirdDateTokens.length != 4) {
            throw new SeabirdException("Bad date string from Seabird: " + seabirdDate);
        }
        retval.append(seabirdDateTokens[2]);
        retval.append("-");
        String monthNumber = monthMap.get(seabirdDateTokens[1]);
        retval.append(monthNumber);
        retval.append("-");
        retval.append(seabirdDateTokens[0]);
        retval.append("T");
        retval.append(seabirdDateTokens[3]);
        retval.append(".00000");
        String iso8601String = retval.toString();
        logger.finer("ISO8601:" + iso8601String);
        ISOtoRbnbTime rbnbTimeConvert = new ISOtoRbnbTime(iso8601String);
        return rbnbTimeConvert.getValue();
    }

    private void initMonthMap() {
        monthMap = new HashMap(12);
        monthMap.put("Jan", "01");
        monthMap.put("Feb", "02");
        monthMap.put("Mar", "03");
        monthMap.put("Apr", "04");
        monthMap.put("May", "05");
        monthMap.put("Jun", "06");
        monthMap.put("Jul", "07");
        monthMap.put("Aug", "08");
        monthMap.put("Sep", "09");
        monthMap.put("Oct", "10");
        monthMap.put("Nov", "11");
        monthMap.put("Dec", "12");
    }

    public String[] getChannels() {
        return this.channelNames;
    }

    protected String getCVSVersionString() {
        return getSVNVersionString();
    }

    protected String getSVNVersionString() {
        return ("$LastChangedDate: 2008-04-15 20:12:19 -0400 (Tue, 15 Apr 2008) $\n" + "$LastChangedRevision: 36 $\n" + "$LastChangedBy: ljmiller.ucsd $\n" + "$HeadURL: http://oss-dataturbine.googlecode.com/svn/trunk/apps/oss-apps/src/edu/sdsc/cleos/SeabirdParser.java $");
    }
}
