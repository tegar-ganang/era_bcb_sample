package com.isa.jump.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jump.coordsys.CoordinateSystem;
import com.vividsolutions.jump.coordsys.Radius;
import com.vividsolutions.jump.coordsys.Reprojector;
import com.vividsolutions.jump.coordsys.Spheroid;
import com.vividsolutions.jump.coordsys.impl.PredefinedCoordinateSystems;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.GMLInputTemplate;
import com.vividsolutions.jump.io.IllegalParametersException;
import com.vividsolutions.jump.io.JUMPReader;
import com.vividsolutions.jump.io.JUMPWriter;
import com.vividsolutions.jump.io.ParseException;
import com.vividsolutions.jump.io.datasource.DelegatingCompressedFileHandler;
import com.vividsolutions.jump.io.datasource.StandardReaderWriterFileDataSource;

public class KMLReader extends DefaultHandler implements JUMPReader {

    private static final String collectionElement = "Folder";

    private static final String featureElement = "Placemark";

    private static final String simplefield = "SimpleField";

    private CoordinateSystem destination = null;

    private CoordinateSystem source = null;

    private double centralMeridian = 0;

    private int zoneInt = 0;

    private boolean zoneSouth = false;

    private String zoneStr = "";

    public KMLReader() {
        super();
        xr = new org.apache.xerces.parsers.SAXParser();
        xr.setContentHandler(this);
        xr.setErrorHandler(this);
    }

    private static class ClassicReaderWriterFileDataSource extends StandardReaderWriterFileDataSource {

        public ClassicReaderWriterFileDataSource(JUMPReader reader, JUMPWriter writer, String[] extensions) {
            super(new DelegatingCompressedFileHandler(reader, toEndings(extensions)), writer, extensions);
            this.extensions = extensions;
        }
    }

    public static class KML extends ClassicReaderWriterFileDataSource {

        public KML() {
            super(new KMLReader(), new KMLWriter(), new String[] { "kml" });
        }
    }

    private GMLInputTemplate makeTemplate() {
        String geometryElement = "***";
        String s = "";
        s += "<?xml version='1.0' encoding='UTF-8'?>";
        s += "<JCSGMLInputTemplate>";
        s += ("<CollectionElement>" + collectionElement + "</CollectionElement>");
        s += ("<FeatureElement>" + featureElement + "</FeatureElement>");
        s += ("<GeometryElement>" + geometryElement + "</GeometryElement>");
        s += "<ColumnDefinitions></ColumnDefinitions>";
        s += "<column>";
        s += "<name>name</name>";
        s += "<type>STRING</type>";
        s += "<valueelement elementname=\"name\"/>";
        s += "<valuelocation position=\"body\"/>";
        s += "</column>";
        s += "</JCSGMLInputTemplate>";
        GMLInputTemplate template = new GMLInputTemplate();
        StringReader sr = new StringReader(s);
        try {
            template.load(sr);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            sr.close();
        }
        return template;
    }

    /**
	 *  Main Entry - load in a GML file
	 *
	 *@param  dp                              Description of the Parameter
	 *@return                                 Description of the Return Value
	 *@exception  IllegalParametersException  Description of the Exception
	 *@exception  Exception                   Description of the Exception
	 */
    public FeatureCollection read(DriverProperties dp) throws IllegalParametersException, Exception {
        source = PredefinedCoordinateSystems.GEOGRAPHICS_WGS_84;
        destination = null;
        FeatureCollection fc;
        String inputFname;
        inputFname = dp.getProperty("File");
        if (inputFname == null) {
            inputFname = dp.getProperty("DefaultValue");
        }
        if (inputFname == null) {
            throw new IllegalParametersException("call to GMLReader.read() has DataProperties w/o a InputFile specified");
        }
        java.io.Reader r;
        GMLInputTemplate template = makeTemplate();
        setInputTemplate(template);
        r = new BufferedReader(new FileReader(inputFname));
        fc = read(r, inputFname);
        r.close();
        Envelope env = fc.getEnvelope();
        return fc;
    }

    /**
	 *  STATE   MEANING <br>
	 *  0      Init <br>
	 *  1      Waiting for Collection tag <br>
	 *  2      Waiting for Feature tag <br>
	 *  3      Getting jcs columns <br>
	 *  4      Parsing geometry (goes back to state 3) <br>
	 *  1000   Parsing Multi-geometry, recursion level =1 <br>
	 *  1001   Parsing Multi-geometry, recursion level =2 <br>
	 */
    static int STATE_INIT = 0;

    static int STATE_PARSE_GEOM_NESTED = 1000;

    static int STATE_FOUND_FEATURE_TAG = 3;

    static int STATE_PARSE_GEOM_SIMPLE = 4;

    static int STATE_WAIT_COLLECTION_TAG = 1;

    static int STATE_WAIT_FEATURE_TAG = 2;

    GMLInputTemplate GMLinput = null;

    int STATE = STATE_INIT;

    Point apoint;

    Feature currentFeature;

    int currentGeometryNumb = 1;

    FeatureCollection fc;

    FeatureSchema fcmd;

    Geometry finalGeometry;

    ArrayList geometry;

    GeometryFactory geometryFactory = new GeometryFactory();

    ArrayList innerBoundaries = new ArrayList();

    Attributes lastStartTag_atts;

    String lastStartTag_name;

    String lastStartTag_qName;

    String lastStartTag_uri;

    LineString lineString;

    LinearRing linearRing;

    LinearRing outerBoundary;

    ArrayList pointList = new ArrayList();

    Polygon polygon;

    ArrayList recursivegeometry = new ArrayList();

    Coordinate singleCoordinate = new Coordinate();

    String streamName;

    StringBuffer tagBody;

    XMLReader xr;

    int SRID = 0;

    public boolean parseSRID = false;

    /**
	 * true => for 'OBJECT' types, if you find more than 1 item, make a list and store all the results
	 */
    public boolean multiItemsAsLists = false;

    /**
	 * parse SRID information in geometry tags
	 * @param parseTheSRID true = parse
	 */
    public void acceptSRID(boolean parseTheSRID) {
        parseSRID = parseTheSRID;
    }

    public void processMultiItems(boolean accept) {
        multiItemsAsLists = accept;
    }

    /**
	 *  Attach a GMLInputTemplate information class.
	 *
	 *@param  template  The new inputTemplate value
	 */
    public void setInputTemplate(GMLInputTemplate template) {
        GMLinput = template;
    }

    /**
	 *  SAX handler - store and accumulate tag bodies
	 *
	 *@param  ch                Description of the Parameter
	 *@param  start             Description of the Parameter
	 *@param  length            Description of the Parameter
	 *@exception  SAXException  Description of the Exception
	 */
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            tagBody.append(ch, start, length);
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }
    }

    /**
	 *  SAX HANDLER - move to state 0
	 */
    public void endDocument() {
        STATE = STATE_INIT;
    }

    /**
	 *  SAX handler - handle state information and transitions based on ending
	 *  elements Most of the work of the parser is done here.
	 *@exception  SAXException  Description of the Exception
	 */
    public void endElement(String uri, String name, String qName) throws SAXException {
        try {
            int index;
            if (STATE == STATE_INIT) {
                tagBody = new StringBuffer();
                return;
            }
            if (STATE > STATE_FOUND_FEATURE_TAG) {
                if (isMultiGeometryTag(qName)) {
                    if (STATE == STATE_PARSE_GEOM_NESTED) {
                        STATE = STATE_PARSE_GEOM_SIMPLE;
                    } else {
                        Geometry g = geometryFactory.buildGeometry(geometry);
                        geometry = (ArrayList) recursivegeometry.get(STATE - STATE_PARSE_GEOM_NESTED - 1);
                        geometry.add(g);
                        recursivegeometry.remove(STATE - STATE_PARSE_GEOM_NESTED);
                        g = null;
                        STATE--;
                    }
                }
                if (qName.compareToIgnoreCase("X") == 0) {
                    singleCoordinate.x = (new Double(tagBody.toString())).doubleValue();
                } else if (qName.compareToIgnoreCase("Y") == 0) {
                    singleCoordinate.y = (new Double(tagBody.toString())).doubleValue();
                } else if (qName.compareToIgnoreCase("Z") == 0) {
                    singleCoordinate.z = (new Double(tagBody.toString())).doubleValue();
                } else if (qName.compareToIgnoreCase("COORD") == 0) {
                    pointList.add(new Coordinate(singleCoordinate));
                } else if (qName.compareToIgnoreCase("COORDINATES") == 0) {
                    parsePoints(tagBody.toString(), geometryFactory);
                } else if (qName.compareToIgnoreCase("linearring") == 0) {
                    Coordinate[] c = new Coordinate[0];
                    c = (Coordinate[]) pointList.toArray(c);
                    linearRing = geometryFactory.createLinearRing(c);
                } else if (qName.compareToIgnoreCase("outerBoundaryIs") == 0) {
                    outerBoundary = linearRing;
                } else if (qName.compareToIgnoreCase("innerBoundaryIs") == 0) {
                    innerBoundaries.add(linearRing);
                } else if (qName.compareToIgnoreCase("polygon") == 0) {
                    LinearRing[] lrs = new LinearRing[0];
                    lrs = (LinearRing[]) innerBoundaries.toArray(lrs);
                    polygon = geometryFactory.createPolygon(outerBoundary, lrs);
                    geometry.add(polygon);
                } else if (qName.compareToIgnoreCase("linestring") == 0) {
                    Coordinate[] c = new Coordinate[0];
                    c = (Coordinate[]) pointList.toArray(c);
                    lineString = geometryFactory.createLineString(c);
                    geometry.add(lineString);
                } else if (qName.compareToIgnoreCase("point") == 0) {
                    apoint = geometryFactory.createPoint((Coordinate) pointList.get(0));
                    geometry.add(apoint);
                }
            } else if (STATE == STATE_FOUND_FEATURE_TAG) {
                if (qName.compareToIgnoreCase(featureElement) == 0) {
                    tagBody = new StringBuffer();
                    STATE = STATE_WAIT_FEATURE_TAG;
                    if (currentFeature.getGeometry() == null) {
                        Geometry g = currentFeature.getGeometry();
                        if (g != null) {
                            System.out.println(g.toString());
                        }
                        throw new ParseException("no geometry specified in feature");
                    }
                    fc.add(currentFeature);
                    currentFeature = null;
                    return;
                } else {
                    try {
                        if (((index = GMLinput.match(lastStartTag_qName, lastStartTag_atts)) > -1) && (lastStartTag_qName.equalsIgnoreCase(qName))) currentFeature.setAttribute(GMLinput.columnName(index), GMLinput.getColumnValue(index, tagBody.toString(), lastStartTag_atts));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    tagBody = new StringBuffer();
                }
            } else if (STATE == STATE_WAIT_FEATURE_TAG) {
                if (qName.compareToIgnoreCase(collectionElement) == 0) {
                    STATE = STATE_INIT;
                    tagBody = new StringBuffer();
                    return;
                }
            } else if (STATE == STATE_WAIT_COLLECTION_TAG) {
                tagBody = new StringBuffer();
                return;
            }
            if (isGeometryTag(qName)) {
                tagBody = new StringBuffer();
                STATE = STATE_FOUND_FEATURE_TAG;
                finalGeometry = geometryFactory.buildGeometry(geometry);
                reprojectGeometry(finalGeometry);
                currentFeature.setGeometry(finalGeometry);
                currentGeometryNumb++;
                return;
            }
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }
    }

    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }

    /**
	 *  Main function to read a GML file. You should have already called
	 *  setInputTempalate().
	 *
	 *@param  r              reader to read the GML File from
	 *@param  readerName     what to call the reader for error reporting
	 *@return                Description of the Return Value
	 *@exception  Exception  Description of the Exception
	 */
    public FeatureCollection read(java.io.Reader r, String readerName) throws Exception {
        LineNumberReader myReader = new LineNumberReader(r);
        if (GMLinput == null) {
            throw new ParseException("you must set the GMLinput template first!");
        }
        streamName = readerName;
        fcmd = GMLinput.toFeatureSchema();
        fc = new FeatureDataset(fcmd);
        try {
            xr.parse(new InputSource(myReader));
        } catch (SAXParseException e) {
            throw new ParseException(e.getMessage() + "  Last Opened Tag: " + lastStartTag_qName + ".  Reader reports last line read as " + myReader.getLineNumber(), streamName + " - " + e.getPublicId() + " (" + e.getSystemId() + ") ", e.getLineNumber(), e.getColumnNumber());
        } catch (SAXException e) {
            throw new ParseException(e.getMessage() + "  Last Opened Tag: " + lastStartTag_qName, streamName, myReader.getLineNumber(), 0);
        }
        return fc;
    }

    /**
	 *  SAX handler - move to state 1
	 */
    public void startDocument() {
        tagBody = new StringBuffer();
        STATE = STATE_WAIT_COLLECTION_TAG;
    }

    /**
	 *  SAX handler. Handle state and state transitions based on an element
	 *  starting
	 *@exception  SAXException  Description of the Exception
	 */
    public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
        try {
            tagBody = new StringBuffer();
            lastStartTag_uri = uri;
            lastStartTag_name = name;
            lastStartTag_qName = qName;
            lastStartTag_atts = atts;
            if (STATE == STATE_INIT) {
                return;
            }
            if ((STATE == STATE_WAIT_COLLECTION_TAG) && (qName.compareToIgnoreCase(collectionElement) == 0)) {
                STATE = STATE_WAIT_FEATURE_TAG;
                return;
            }
            if ((STATE == STATE_WAIT_FEATURE_TAG) && (qName.compareToIgnoreCase(featureElement) == 0)) {
                currentFeature = new BasicFeature(fcmd);
                STATE = STATE_PARSE_GEOM_SIMPLE;
                recursivegeometry = new ArrayList();
                geometry = new ArrayList();
                recursivegeometry.add(geometry);
                finalGeometry = null;
                SRID = 0;
                if (geometryFactory.getSRID() != SRID) geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
                return;
            }
            if (parseSRID && (STATE >= STATE_PARSE_GEOM_SIMPLE) && isGeometryTag(qName)) {
                int newSRID = parseSRID(atts.getValue("srsName"));
                if (newSRID != 0) {
                    SRID = newSRID;
                    if (geometryFactory.getSRID() != SRID) geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
                }
            }
            if ((STATE >= STATE_PARSE_GEOM_SIMPLE) && ((qName.compareToIgnoreCase("coord") == 0) || (qName.compareToIgnoreCase("gml:coord") == 0))) {
                singleCoordinate.x = Double.NaN;
                singleCoordinate.y = Double.NaN;
                singleCoordinate.z = Double.NaN;
            }
            if ((STATE >= STATE_PARSE_GEOM_SIMPLE) && (!((qName.compareToIgnoreCase("X") == 0) || (qName.compareToIgnoreCase("y") == 0) || (qName.compareToIgnoreCase("z") == 0) || (qName.compareToIgnoreCase("coord") == 0)))) {
                pointList.clear();
            }
            if ((STATE >= STATE_PARSE_GEOM_SIMPLE) && ((qName.compareToIgnoreCase("polygon") == 0))) {
                innerBoundaries.clear();
            }
            if ((STATE > STATE_FOUND_FEATURE_TAG) && (isMultiGeometryTag(qName))) {
                if (STATE == STATE_PARSE_GEOM_SIMPLE) {
                    STATE = STATE_PARSE_GEOM_NESTED;
                } else {
                    STATE++;
                    geometry = new ArrayList();
                    recursivegeometry.add(geometry);
                }
            }
        } catch (Exception e) {
            throw new SAXException(e.getMessage());
        }
    }

    public void warning(SAXParseException exception) throws SAXException {
        throw exception;
    }

    /**
	 *  returns true if the the string represents a  geometry type
	 *  ie. "gml:linestring" or "linestring"
	 *
	 *@param  s  Description of the Parameter
	 *@return    true if this is a geometry tag
	 */
    private boolean isGeometryTag(String s) {
        if ((s.length() > 5) && (s.substring(0, 4).compareToIgnoreCase("gml:") == 0)) {
            s = s.substring(4);
        }
        if ((s.compareToIgnoreCase("multigeometry") == 0) || (s.compareToIgnoreCase("multipoint") == 0) || (s.compareToIgnoreCase("multilinestring") == 0) || (s.compareToIgnoreCase("multipolygon") == 0) || (s.compareToIgnoreCase("polygon") == 0) || (s.compareToIgnoreCase("linestring") == 0) || (s.compareToIgnoreCase("point") == 0) || (s.compareToIgnoreCase("geometrycollection") == 0)) {
            return true;
        }
        return false;
    }

    /**
	 *  returns true if the the string represents a multi* geometry type
	 *
	 *@param  s  Description of the Parameter
	 *@return    The multiGeometryTag value
	 */
    private boolean isMultiGeometryTag(String s) {
        if ((s.length() > 5) && (s.substring(0, 4).compareToIgnoreCase("gml:") == 0)) {
            s = s.substring(4);
        }
        if ((s.compareToIgnoreCase("multigeometry") == 0) || (s.compareToIgnoreCase("multipoint") == 0) || (s.compareToIgnoreCase("multilinestring") == 0) || (s.compareToIgnoreCase("multipolygon") == 0)) {
            return true;
        }
        return false;
    }

    /**
	 *  Parse a bunch of points - stick them in pointList. Handles 2d and 3d.
	 *
	 *@param  ptString         string containing a bunch of coordinates
	 *@param  geometryFactory  JTS point/coordinate factory
	 */
    private void parsePoints(String ptString, GeometryFactory geometryFactory) {
        String aPoint;
        StringTokenizer stokenizerPoint;
        Coordinate coord = new Coordinate();
        int dim;
        String numb;
        StringBuffer sb;
        int t;
        char ch;
        sb = new StringBuffer(ptString);
        for (t = 0; t < sb.length(); t++) {
            ch = sb.charAt(t);
            if ((ch == '\n') || (ch == '\r')) {
                sb.setCharAt(t, ' ');
            }
        }
        StringTokenizer stokenizer = new StringTokenizer(new String(sb), " ", false);
        while (stokenizer.hasMoreElements()) {
            aPoint = stokenizer.nextToken();
            stokenizerPoint = new StringTokenizer(aPoint, ",", false);
            coord.x = coord.y = coord.z = Double.NaN;
            dim = 0;
            while (stokenizerPoint.hasMoreElements()) {
                numb = stokenizerPoint.nextToken();
                if (dim == 0) {
                    coord.x = Double.parseDouble(numb);
                } else if (dim == 1) {
                    coord.y = Double.parseDouble(numb);
                } else if (dim == 2) {
                    coord.z = Double.parseDouble(numb);
                }
                dim++;
            }
            if ((coord.x != coord.x) || (coord.y != coord.y)) {
                throw new IllegalArgumentException("GML error - coordinate list isnt valid GML. Watch your spaces and commas!");
            }
            pointList.add(coord);
            coord = new Coordinate();
            stokenizerPoint = null;
        }
    }

    /**
	 *  parses the given srs text and returns the SRID
	 * @param srsName srsName of the type "EPSG:<number>"
	 * @return srid or 0 if there is a problem
	 */
    private int parseSRID(String srsName) {
        try {
            int semicolonLoc = srsName.lastIndexOf(':');
            if (semicolonLoc == -1) return 0;
            srsName = srsName.substring(semicolonLoc + 1).trim();
            return Integer.parseInt(srsName);
        } catch (Exception e) {
            return 0;
        }
    }

    private void setDestinationProjection(final int zoneInt, final boolean zoneSouth, final double centralMeridian) {
        destination = new CoordinateSystem("UTM " + zoneStr + " / WGS 84", 32600 + zoneInt, new UniversalTransverseMercator() {

            {
                setSpheroid(new Spheroid(new Radius(Radius.GRS80)));
                setParameters(zoneInt, zoneSouth, centralMeridian);
            }
        });
    }

    private void setDestinationProjection(Coordinate coord) {
        getZone(coord.y, coord.x);
        setDestinationProjection(zoneInt, zoneSouth, centralMeridian);
    }

    public String getZone(double latitude, double longitude) {
        double zoneDec = (longitude + 180.0) / 6.0;
        zoneInt = (int) zoneDec;
        if (zoneDec - zoneInt > 0) zoneInt++;
        if (zoneInt <= 0) zoneInt = 1;
        if (zoneInt > 60) zoneInt = 60;
        centralMeridian = zoneInt * 6 - 183.0;
        if (latitude >= 0) {
            zoneSouth = false;
            zoneStr = zoneInt + "N";
        } else {
            zoneSouth = true;
            zoneStr = zoneInt + "S";
        }
        return zoneStr;
    }

    private void reprojectGeometry(Geometry geometry) {
        geometry.apply(new CoordinateFilter() {

            public void filter(Coordinate coord) {
                if (destination == null) {
                    setDestinationProjection(coord);
                }
                Reprojector.instance().reproject(coord, source, destination);
            }
        });
        geometry.geometryChanged();
    }
}
