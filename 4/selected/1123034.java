package org.dinopolis.gpstool;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.dinopolis.gpstool.gpsinput.GPSDataProcessor;
import org.dinopolis.gpstool.gpsinput.GPSDevice;
import org.dinopolis.gpstool.gpsinput.GPSException;
import org.dinopolis.gpstool.gpsinput.GPSFileDevice;
import org.dinopolis.gpstool.gpsinput.GPSNetworkGpsdDevice;
import org.dinopolis.gpstool.gpsinput.GPSPosition;
import org.dinopolis.gpstool.gpsinput.GPSRawDataFileLogger;
import org.dinopolis.gpstool.gpsinput.GPSRawDataListener;
import org.dinopolis.gpstool.gpsinput.GPSRoute;
import org.dinopolis.gpstool.gpsinput.GPSSerialDevice;
import org.dinopolis.gpstool.gpsinput.GPSTrack;
import org.dinopolis.gpstool.gpsinput.GPSWaypoint;
import org.dinopolis.gpstool.gpsinput.SatelliteInfo;
import org.dinopolis.gpstool.gpsinput.garmin.GPSGarminDataProcessor;
import org.dinopolis.gpstool.gpsinput.nmea.GPSNmeaDataProcessor;
import org.dinopolis.gpstool.gpsinput.sirf.GPSSirfDataProcessor;
import org.dinopolis.gpstool.gpx.ReadGPX;
import org.dinopolis.util.ProgressListener;
import org.dinopolis.util.commandarguments.CommandArgumentException;
import org.dinopolis.util.commandarguments.CommandArguments;
import org.dinopolis.util.text.OneArgumentMessageFormat;

/**
 * Demo application to show the usage of the org.dinopolis.gpstool.gpsinput package (read and
 * interpret gps data from various devices (serial, file, ...).  <p>
 * It uses a velocity (http://jakarta.apache.org/velocity) template to
 * print the downloaded tracks, routes, and waypoints. See the help
 * output for details about the usable variables.
 *
 * @author Christof Dallermassl, Stefan Feitl
 * @version $Revision: 936 $
 */
public class GPSTool implements PropertyChangeListener, ProgressListener {

    protected boolean gui_ = true;

    protected GPSDataProcessor gps_processor_;

    public static final String DEFAULT_TEMPLATE = "<?xml version=\"1.0\"?>" + "$dateformatter.applyPattern(\"yyyy-MM-dd'T'HH:mm:ss'Z'\")" + "$longitudeformatter.applyPattern(\"0.000000\")" + "$latitudeformatter.applyPattern(\"0.000000\")" + "$altitudeformatter.applyPattern(\"0\")\n" + "<gpx" + "  version=\"1.0\"\n" + "  creator=\"$author\"\n" + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "  xmlns=\"http://www.topografix.com/GPX/1/0\"\n" + "  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n" + "  <time>$dateformatter.format($creation_date)</time>\n" + "  <bounds minlat=\"$min_latitude\" minlon=\"$min_longitude\"\n" + "          maxlat=\"$max_latitude\" maxlon=\"$max_longitude\"/>\n" + "\n" + "## print all waypoints that are available:\n" + "#if($printwaypoints)\n" + "#foreach( $point in $waypoints )\n" + "  <wpt lat=\"$latitudeformatter.format($point.Latitude)\" lon=\"$longitudeformatter.format($point.Longitude)\">\n" + "#if($point.hasValidAltitude())\n" + "    <ele>$altitudeformatter.format($point.Altitude)</ele>\n" + "#end\n" + "    <name>$!point.Identification</name>\n" + "#if($point.getComment().length() > 0)\n" + "    <desc>![CDATA[$!point.Comment]]</desc>\n" + "#end\n" + "#if($point.getSymbolName())\n" + "    <sym>$point.getSymbolName()</sym>\n" + "#end\n" + "  </wpt>\n" + "#end\n" + "#end\n" + "## print all routes that are available:\n" + "#if($printroutes)\n" + "#foreach( $route in $routes )\n" + "  <rte>\n" + "    <name>$!route.Identification</name>\n" + "#if($route.getComment().length() > 0)\n" + "    <desc>![CDATA[$!route.Comment]]</desc>\n" + "#end\n" + "    <number>$velocityCount</number>\n" + "#set ($points = $route.getWaypoints())\n" + "#foreach ($point in $points)\n" + "    <rtept lat=\"$latitudeformatter.format($point.Latitude)\" lon=\"$longitudeformatter.format($point.Longitude)\">\n" + "#if($point.hasValidAltitude())\n" + "        <ele>$altitudeformatter.format($point.Altitude)</ele>\n" + "#end\n" + "#if($point.getIdentification().length() > 0)\n" + "    <name>![CDATA[$!point.Identification]]</name>\n" + "#end\n" + "#if($point.getComment().length() > 0)\n" + "    <desc>![CDATA[$!point.Comment]]</desc>\n" + "#end\n" + "    </rtept>\n" + "#end\n" + "  </rte>\n" + "#end\n" + "#end\n" + "## print all tracks that are available:\n" + "#if($printtracks)\n" + "#foreach( $track in $tracks )\n" + "#set($close_segment = false)\n" + "  <trk>\n" + "    <name>$!track.Identification</name>\n" + "#if($point.getComment().length() > 0)\n" + "    <desc>![CDATA[$!point.Comment]]</desc>\n" + "#end\n" + "##      <number>$velocityCount</number>\n" + "#set ($points = $track.getWaypoints())##\n" + "#foreach ($point in $points)##\n" + "#if($point.isNewTrack())\n" + "#if($close_segment)## close trkseg, if not the first occurence\n" + "    </trkseg>\n" + "#end\n" + "    <trkseg>\n" + "#set($close_segment = true)\n" + "#end\n" + "      <trkpt lat=\"$latitudeformatter.format($point.Latitude)\" lon=\"$longitudeformatter.format($point.Longitude)\">\n" + "#if($point.hasValidAltitude())\n" + "        <ele>$altitudeformatter.format($point.Altitude)</ele>\n" + "#end\n" + "#if($point.getDate())## only if there is a time set! \n" + "        <time>$dateformatter.format($point.getDate())</time>\n" + "#end\n" + "      </trkpt>\n" + "#end\n" + "#if($close_segment)\n" + "  </trkseg>\n" + "#end\n" + "  </trk>\n" + "#end\n" + "#end\n" + "</gpx>\n";

    /**
 * Default constructor
 */
    public GPSTool() {
    }

    /**
 * Initialize the gps device, the gps data processor and handle all
 * command line arguments.
 * @param arguments the command line arguments
 */
    public void init(String[] arguments) {
        if (arguments.length < 1) {
            printHelp();
            return;
        }
        String[] valid_args = new String[] { "device*", "d*", "help", "h", "speed#", "s#", "file*", "f*", "gpsd*", "nmea", "n", "garmin", "g", "sirf", "i", "rawdata", "downloadtracks", "downloadwaypoints", "downloadroutes", "deviceinfo", "printposonce", "printpos", "p", "printalt", "printspeed", "printheading", "printsat", "template*", "outfile*", "screenshot*", "printdefaulttemplate", "helptemplate", "nmealogfile*", "l", "uploadtracks", "uploadroutes", "uploadwaypoints", "infile*" };
        CommandArguments args = null;
        try {
            args = new CommandArguments(arguments, valid_args);
        } catch (CommandArgumentException cae) {
            System.err.println("Invalid arguments: " + cae.getMessage());
            printHelp();
            return;
        }
        String filename = null;
        String serial_port_name = null;
        boolean gpsd = false;
        String gpsd_host = "localhost";
        int gpsd_port = 2947;
        int serial_port_speed = -1;
        GPSDataProcessor gps_data_processor;
        String nmea_log_file = null;
        if (args.isSet("help") || (args.isSet("h"))) {
            printHelp();
            return;
        }
        if (args.isSet("helptemplate")) {
            printHelpTemplate();
        }
        if (args.isSet("printdefaulttemplate")) {
            System.out.println(DEFAULT_TEMPLATE);
        }
        if (args.isSet("device")) {
            serial_port_name = (String) args.getValue("device");
        } else if (args.isSet("d")) {
            serial_port_name = (String) args.getValue("d");
        }
        if (args.isSet("speed")) {
            serial_port_speed = ((Integer) args.getValue("speed")).intValue();
        } else if (args.isSet("s")) {
            serial_port_speed = ((Integer) args.getValue("s")).intValue();
        }
        if (args.isSet("file")) {
            filename = (String) args.getValue("file");
        } else if (args.isSet("f")) {
            filename = (String) args.getValue("f");
        }
        if (args.isSet("gpsd")) {
            gpsd = true;
            String gpsd_host_port = (String) args.getValue("gpsd");
            if (gpsd_host_port != null && gpsd_host_port.length() > 0) {
                String[] params = gpsd_host_port.split(":");
                gpsd_host = params[0];
                if (params.length > 0) {
                    gpsd_port = Integer.parseInt(params[1]);
                }
            }
        }
        if (args.isSet("garmin") || args.isSet("g")) {
            gps_data_processor = new GPSGarminDataProcessor();
            serial_port_speed = 9600;
            if (filename != null) {
                System.err.println("ERROR: Cannot read garmin data from file, only serial port supported!");
                return;
            }
        } else if (args.isSet("sirf") || args.isSet("i")) {
            gps_data_processor = new GPSSirfDataProcessor();
            serial_port_speed = 19200;
            if (filename != null) {
                System.err.println("ERROR: Cannot read sirf data from file, only serial port supported!");
                return;
            }
        } else {
            gps_data_processor = new GPSNmeaDataProcessor();
            serial_port_speed = 4800;
        }
        if (args.isSet("nmealogfile") || (args.isSet("l"))) {
            if (args.isSet("nmealogfile")) nmea_log_file = args.getStringValue("nmealogfile"); else nmea_log_file = args.getStringValue("l");
        }
        if (args.isSet("rawdata")) {
            gps_data_processor.addGPSRawDataListener(new GPSRawDataListener() {

                public void gpsRawDataReceived(char[] data, int offset, int length) {
                    System.out.println("RAWLOG: " + new String(data, offset, length));
                }
            });
        }
        GPSDevice gps_device;
        Hashtable environment = new Hashtable();
        if (filename != null) {
            environment.put(GPSFileDevice.PATH_NAME_KEY, filename);
            gps_device = new GPSFileDevice();
        } else if (gpsd) {
            environment.put(GPSNetworkGpsdDevice.GPSD_HOST_KEY, gpsd_host);
            environment.put(GPSNetworkGpsdDevice.GPSD_PORT_KEY, new Integer(gpsd_port));
            gps_device = new GPSNetworkGpsdDevice();
        } else {
            if (serial_port_name != null) environment.put(GPSSerialDevice.PORT_NAME_KEY, serial_port_name);
            if (serial_port_speed > -1) environment.put(GPSSerialDevice.PORT_SPEED_KEY, new Integer(serial_port_speed));
            gps_device = new GPSSerialDevice();
        }
        try {
            gps_device.init(environment);
            gps_data_processor.setGPSDevice(gps_device);
            gps_data_processor.open();
            gps_data_processor.addProgressListener(this);
            if ((nmea_log_file != null) && (nmea_log_file.length() > 0)) {
                gps_data_processor.addGPSRawDataListener(new GPSRawDataFileLogger(nmea_log_file));
            }
            if (args.isSet("deviceinfo")) {
                System.out.println("GPSInfo:");
                String[] infos = gps_data_processor.getGPSInfo();
                for (int index = 0; index < infos.length; index++) {
                    System.out.println(infos[index]);
                }
            }
            if (args.isSet("screenshot")) {
                FileOutputStream out = new FileOutputStream((String) args.getValue("screenshot"));
                BufferedImage image = gps_data_processor.getScreenShot();
                ImageIO.write(image, "PNG", out);
            }
            boolean print_waypoints = args.isSet("downloadwaypoints");
            boolean print_routes = args.isSet("downloadroutes");
            boolean print_tracks = args.isSet("downloadtracks");
            if (print_waypoints || print_routes || print_tracks) {
                VelocityContext context = new VelocityContext();
                if (print_waypoints) {
                    List waypoints = gps_data_processor.getWaypoints();
                    if (waypoints != null) context.put("waypoints", waypoints); else print_waypoints = false;
                }
                if (print_tracks) {
                    List tracks = gps_data_processor.getTracks();
                    if (tracks != null) context.put("tracks", tracks); else print_tracks = false;
                }
                if (print_routes) {
                    List routes = gps_data_processor.getRoutes();
                    if (routes != null) context.put("routes", routes); else print_routes = false;
                }
                context.put("printwaypoints", new Boolean(print_waypoints));
                context.put("printtracks", new Boolean(print_tracks));
                context.put("printroutes", new Boolean(print_routes));
                Writer writer;
                Reader reader;
                if (args.isSet("template")) {
                    String template_file = (String) args.getValue("template");
                    reader = new FileReader(template_file);
                } else {
                    reader = new StringReader(DEFAULT_TEMPLATE);
                }
                if (args.isSet("outfile")) writer = new FileWriter((String) args.getValue("outfile")); else writer = new OutputStreamWriter(System.out);
                addDefaultValuesToContext(context);
                boolean result = printTemplate(context, reader, writer);
            }
            boolean read_waypoints = (args.isSet("uploadwaypoints") && args.isSet("infile"));
            boolean read_routes = (args.isSet("uploadroutes") && args.isSet("infile"));
            boolean read_tracks = (args.isSet("uploadtracks") && args.isSet("infile"));
            if (read_waypoints || read_routes || read_tracks) {
                ReadGPX reader = new ReadGPX();
                String in_file = (String) args.getValue("infile");
                reader.parseFile(in_file);
                if (read_waypoints) gps_data_processor.setWaypoints(reader.getWaypoints());
                if (read_routes) gps_data_processor.setRoutes(reader.getRoutes());
                if (read_tracks) gps_data_processor.setTracks(reader.getTracks());
            }
            if (args.isSet("printposonce")) {
                GPSPosition pos = gps_data_processor.getGPSPosition();
                System.out.println("Current Position: " + pos);
            }
            if (args.isSet("printpos") || args.isSet("p")) {
                gps_data_processor.addGPSDataChangeListener(GPSDataProcessor.LOCATION, this);
            }
            if (args.isSet("printalt")) {
                gps_data_processor.addGPSDataChangeListener(GPSDataProcessor.ALTITUDE, this);
            }
            if (args.isSet("printspeed")) {
                gps_data_processor.addGPSDataChangeListener(GPSDataProcessor.SPEED, this);
            }
            if (args.isSet("printheading")) {
                gps_data_processor.addGPSDataChangeListener(GPSDataProcessor.HEADING, this);
            }
            if (args.isSet("printsat")) {
                gps_data_processor.addGPSDataChangeListener(GPSDataProcessor.NUMBER_SATELLITES, this);
                gps_data_processor.addGPSDataChangeListener(GPSDataProcessor.SATELLITE_INFO, this);
            }
            if (args.isSet("printpos") || args.isSet("p") || args.isSet("printalt") || args.isSet("printsat") || args.isSet("printspeed") || args.isSet("printheading")) {
                gps_data_processor.startSendPositionPeriodically(1000L);
                try {
                    System.in.read();
                } catch (IOException ignore) {
                }
            }
            gps_data_processor.close();
        } catch (GPSException e) {
            e.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            System.err.println("ERROR: File not found: " + fnfe.getMessage());
        } catch (IOException ioe) {
            System.err.println("ERROR: I/O Error: " + ioe.getMessage());
        }
    }

    /**
 * Adds some important values to the velocity context (e.g. date, ...).
 *
 * @param context the velocity context holding all the data
 */
    public void addDefaultValuesToContext(VelocityContext context) {
        DecimalFormat latitude_formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        latitude_formatter.applyPattern("0.0000000");
        DecimalFormat longitude_formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        longitude_formatter.applyPattern("0.0000000");
        DecimalFormat altitude_formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        altitude_formatter.applyPattern("000000");
        OneArgumentMessageFormat string_formatter = new OneArgumentMessageFormat("{0}", Locale.US);
        context.put("dateformatter", new SimpleDateFormat());
        context.put("latitudeformatter", latitude_formatter);
        context.put("longitudeformatter", longitude_formatter);
        context.put("altitudeformatter", altitude_formatter);
        context.put("stringformatter", string_formatter);
        Calendar now = Calendar.getInstance();
        context.put("creation_date", now.getTime());
        context.put("author", System.getProperty("user.name"));
        double min_latitude = 90.0;
        double min_longitude = 180.0;
        double max_latitude = -90.0;
        double max_longitude = -180.0;
        List routes = (List) context.get("routes");
        GPSRoute route;
        if (routes != null) {
            Iterator route_iterator = routes.iterator();
            while (route_iterator.hasNext()) {
                route = (GPSRoute) route_iterator.next();
                min_longitude = route.getMinLongitude();
                max_longitude = route.getMaxLongitude();
                min_latitude = route.getMinLatitude();
                max_latitude = route.getMaxLatitude();
            }
        }
        List tracks = (List) context.get("tracks");
        GPSTrack track;
        if (tracks != null) {
            Iterator track_iterator = tracks.iterator();
            while (track_iterator.hasNext()) {
                track = (GPSTrack) track_iterator.next();
                min_longitude = Math.min(min_longitude, track.getMinLongitude());
                max_longitude = Math.max(max_longitude, track.getMaxLongitude());
                min_latitude = Math.min(min_latitude, track.getMinLatitude());
                max_latitude = Math.max(max_latitude, track.getMaxLatitude());
            }
        }
        List waypoints = (List) context.get("waypoints");
        GPSWaypoint waypoint;
        if (waypoints != null) {
            Iterator waypoint_iterator = waypoints.iterator();
            while (waypoint_iterator.hasNext()) {
                waypoint = (GPSWaypoint) waypoint_iterator.next();
                min_longitude = Math.min(min_longitude, waypoint.getLongitude());
                max_longitude = Math.max(max_longitude, waypoint.getLongitude());
                min_latitude = Math.min(min_latitude, waypoint.getLatitude());
                max_latitude = Math.max(max_latitude, waypoint.getLatitude());
            }
        }
        context.put("min_latitude", new Double(min_latitude));
        context.put("min_longitude", new Double(min_longitude));
        context.put("max_latitude", new Double(max_latitude));
        context.put("max_longitude", new Double(max_longitude));
    }

    /**
 * Prints the given context with the given velocity template to the
 * given output writer.
 *
 * @param context the velocity context holding all the data
 * @param template the reader providing the template to use
 * @param out the writer to write the result to.
 * @return true if successfull, false otherwise (see velocity log for
 * details then).
 * @throws IOException if an error occurs
 */
    public boolean printTemplate(VelocityContext context, Reader template, Writer out) throws IOException {
        boolean result = false;
        try {
            Velocity.init();
            result = Velocity.evaluate(context, out, "gpstool", template);
            out.flush();
            out.close();
        } catch (ParseErrorException pee) {
            pee.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (result);
    }

    /**
 * Returns the default template used to print data from the gps device.
 *
 * @return the default template.
 */
    public static String getDefaultTEmplate() {
        return (DEFAULT_TEMPLATE);
    }

    /**
 * Callback for any changes of the gps data (position, heading, speed,
 * etc.).
 * @param event the event holding the information.
 */
    public void propertyChange(PropertyChangeEvent event) {
        Object value = event.getNewValue();
        String name = event.getPropertyName();
        if (name.equals(GPSDataProcessor.SATELLITE_INFO)) {
            SatelliteInfo[] infos = (SatelliteInfo[]) value;
            SatelliteInfo info;
            for (int count = 0; count < infos.length; count++) {
                info = infos[count];
                System.out.println("sat " + info.getPRN() + ": elev=" + info.getElevation() + " azim=" + info.getAzimuth() + " dB=" + info.getSNR());
            }
        } else System.out.println(event.getPropertyName() + ": " + event.getNewValue());
    }

    /**
 * Callback to inform listeners about an action to start.
 *
 * @param action_id the id of the action that is started. This id may
 * be used to display a message for the user.
 * @param min_value the minimum value of the progress counter.
 * @param max_value the maximum value of the progress counter. If the
 * max value is unknown, max_value is set to <code>Integer.NaN</code>.
 */
    public void actionStart(String action_id, int min_value, int max_value) {
        System.err.println("Starting '" + action_id + "' (" + max_value + " packages): ");
    }

    /**
 * Callback to inform listeners about progress going on. It is not
 * guaranteed that this method is called on every change of current
 * value (e.g. only call this method on every 10th change).
 *
 * @param action_id the id of the action that is started. This id may
 * be used to display a message for the user.
 * @param current_value the current value
 */
    public void actionProgress(String action_id, int current_value) {
        System.err.print("\r" + current_value);
    }

    /**
 * Callback to inform listeners about the end of the action.
 *
 * @param action_id the id of the action that is started. This id may
 * be used to display a message for the user.
 */
    public void actionEnd(String action_id) {
        System.err.println("\nfinished");
    }

    /**
 * Prints the help message for writing templates.
 */
    public static void printHelpTemplate() {
        System.out.println("GPSTool is able to write tracks, routes, and waypoints in various");
        System.out.println("formats. It uses a velocity template for this. Please see");
        System.out.println("http://jakarta.apache.org/velocity for details. GPSTool provides");
        System.out.println("the following objects to be used in the template (the type is");
        System.out.println("included in parentheses):");
        System.out.println("  $waypoints (List of GPSWaypoint objects): the waypoints from the gps device");
        System.out.println("  $routes (List of GPSRoute objects): the routes from the gps device");
        System.out.println("  $tracks (List of GPSTrack objects) the tracks from the gps device");
        System.out.println("  $printwaypoints (Boolean): true, if the user decided to download waypoints");
        System.out.println("  $printtracks (Boolean): true, if the user decided to download tracks");
        System.out.println("  $printroutes (Boolean): true, if the user decided to download routes");
        System.out.println("  $creation_date (java.util.Date): the creation date (now)");
        System.out.println("  $author (String): the system property 'user.name'");
        System.out.println("  $min_latitude (Double): the minimum latitude of all downloaded data");
        System.out.println("  $max_latitude (Double): the maximum latitude of all downloaded data");
        System.out.println("  $min_longitude (Double): the minimum longitude of all downloaded data");
        System.out.println("  $min_longitude (Double): the maximum longitude of all downloaded data");
        System.out.println("  $dateformatter (java.text.SimpleDateFormat): helper object to format dates");
        System.out.println("For an example use the commandline switch '--printdefaulttemplate'.");
    }

    /**
 * Prints the help messages
 */
    public static void printHelp() {
        System.out.println("GPSTool 0.5.2 - Communication between GPS-Devices and Computers via serial port");
        System.out.println("(c) 2000-2006 Christof Dallermassl\n");
        System.out.println("Usage: java org.dinopolis.gpstool.GPSTool [options]\n");
        System.out.println("Options:");
        System.out.println("--device, -d <device>, e.g. -d /dev/ttyS0 or COM1 (defaults depending on OS).");
        System.out.println("--speed,  -s <speed>, e.g. -s 4800 (default for nmea, 9600 for garmin, 19200 for sirf).");
        System.out.println("--file,   -f <filename>, the gps data is read from the given file.");
        System.out.println("--nmea,   -n, the gps data is interpreted as NMEA data (default).");
        System.out.println("--garmin, -g, the gps data is interpreted as garmin data.");
        System.out.println("--sirf, -i, the gps data is interpreted as sirf data.");
        System.out.println("--nmealogfile, -l <filename>, the gps data is logged into this file.");
        System.out.println("--rawdata, the raw (nmea or garmin) gps data is printed to stdout.");
        System.out.println("--printposonce, prints the current position and exits again.");
        System.out.println("--printpos, -p, prints the current position and any changes.");
        System.out.println("                Loops until the user presses 'enter'.");
        System.out.println("--printalt, prints the current altitude and any changes.");
        System.out.println("--printsat, prints the current satellite info altitude and any changes.");
        System.out.println("--printspeed, prints the current speed and any changes.");
        System.out.println("--printheading, prints the current heading and any changes.");
        System.out.println("--deviceinfo, prints some information about the gps device (if available)");
        System.out.println("--screenshot <filename>, saves a screenshot of the gps device in PNG format.");
        System.out.println("--downloadtracks, print tracks stored in the gps device.");
        System.out.println("--downloadwaypoints, print waypoints stored in the gps device.");
        System.out.println("--downloadroutes, print routes stored in the gpsdevice .");
        System.out.println("--outfile <filename>, the file to print the tracks, routes and waypoints to, stdout is default");
        System.out.println("--template <filename>, the velocity template to use for printing routes, tracks and waypoints");
        System.out.println("--printdefaulttemplate, prints the default template used to print routes, waypoints, and tracks.");
        System.out.println("--uploadtracks, reads track information from the file given at the infile\n" + "                parameter and uploads it to the gps device.");
        System.out.println("--uploadroutes, reads route information from the file given at the infile\n" + "                parameter and uploads it to the gps device.");
        System.out.println("--uploadwaypoints, reads waypoint information from the file given at the infile\n" + "                   parameter and uploads it to the gps device.");
        System.out.println("--infile <filename>, the GPX file to read the tracks, routes and waypoints from");
        System.out.println("--helptemplate, prints some more information on how to write a template.");
        System.out.println("--help -h, shows this page");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + "/" + System.getProperty("os.arch"));
    }

    /**
 * Main method
 * @param arguments the command line arguments
 */
    public static void main(String[] arguments) {
        new GPSTool().init(arguments);
    }
}
