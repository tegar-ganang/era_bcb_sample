package org.kaboum.util;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.kaboum.Kaboum;
import org.kaboum.KaboumOpMode;
import org.kaboum.geom.KaboumGeometry;
import org.kaboum.geom.KaboumGeometryDisplayDescriptor;
import org.kaboum.geom.KaboumGeometryGlobalDescriptor;
import org.kaboum.geom.KaboumGeometryPropertiesDescriptor;

/**
 * This class is similar to KaboumMapServerTools, except it deals with the kaboum feature objets server engine.<p>
 * Stores URL and parameters for this server and deals with geometry handling<p>
 * Deals with all geometries parameters (global, display, properties, tooltips, etc.).
 * This class should be the only entry point to get/set geometric parameters<p>
 * This classe keeps a reference on the kaboum Applet to be able to call its methods and variables.
 * (This is needed now that this class deals with geographic objects instead of Kaboum applet).
 *
 * @author Nicolas
 */
public class KaboumFeatureServerTools {

    /** indicates if features retrieval is on (true) or off.
     * Users can control this by sending the following kaboumCommand:
     * OBJECT|ON to allow feature retrieval<br>
     * OBJECT|OFF to block it, for instance when the map extent is too big
     */
    private boolean standby;

    /** this extent is used to test if it is necessary to reload features from server:
     * if this extent contains current map extent, it is not necessary to load objects
     * except if all objects where not retrieved because of a server count limit.
     * If current map extent contains this extent, it will be the new biggest extent
     */
    private KaboumExtent biggestExtent = null;

    /**If  True if geometry vertices be dragged with the mouse pointer. Default is false.*/
    public boolean dragAllowed = false;

    /**If true, user can cancel a geometric object digitalization by one click
     * (instead of removing each vertice of the geometry one by one).
     * default is false
     */
    public boolean cancelAllowed = false;

    /** If true, user can remove a geometric object. Default is false */
    public boolean suppressionAllowed = false;

    /**If true, user can create multi geometric ojects (MultiPoint, MultiLineString or MultiPolygon).
     * default is true
     */
    public boolean geometryCollectionAllowed = true;

    /** If true, user can create interior rings (holes) in closed geometries (Polygon or MultiPolygon).
     * default is true
     */
    public boolean holesAllowed = true;

    /** If true, user can remove an exterior ring from a multi geometric object
     * (MultiPoint, MultiLineString or MultiPolygon).
     * default is true
     */
    public boolean suppressGeometryWithinCollectionAllowed = true;

    /** Digitalization precision in maps units.
     * Two points within a distance lower or equal to this precision are considered equals.
     * N.B. : this precision is independant of the global coordinate precision.
     * default value: 1 map unit
     */
    public int computationPrecision = 1;

    /** Double click time in millisecond. This avoid java problem to recognize two consecutive clicks from a drag.
     * default is 500 ms
     */
    public int doubleClickTime = 500;

    /** Precision used for selection of geometric objects. Expressed in pixels
     * default value: 1 pixel
     */
    public int pixelPrecision = 1;

    /** The reference on Kaboum Applet */
    private Kaboum parent = null;

    /** The URL (absolute or relative) to the Kaboum Server Feature Engine */
    private String featureServerURL = null;

    /** The full path on the server to the feature objects properties file */
    private String featureFile = null;

    /** The initial Map Extent */
    private KaboumExtent mapExtent = null;

    /** The temporary shuttle containing validated objects, waiting for geometryOpMode to explicitly
     * re-add them into kaboum
     * TODO: change this weird mechanism
     */
    private KaboumFeatureShuttle tmpShuttle = null;

    /** Creates a new instance of KaboumFeatureServerTools with an URL to the server engine and a file path
     * to the feature objects properties
     */
    public KaboumFeatureServerTools(Kaboum applet, String featureServerURL, String featureFile, KaboumExtent mapExt) {
        parent = applet;
        this.featureFile = featureFile;
        this.featureServerURL = featureServerURL;
        this.mapExtent = mapExt;
        standby = false;
        biggestExtent = new KaboumExtent(mapExt.xMin + (mapExt.xMax - mapExt.xMin) * 0.01, mapExt.yMin + (mapExt.yMax - mapExt.yMin) * 0.01, mapExt.xMax - (mapExt.xMax - mapExt.xMin) * 0.01, mapExt.yMax - (mapExt.yMax - mapExt.yMin) * 0.01);
        parent.opModePropertiesHash.put("KABOUM_USE_TOOLTIP", parent.FALSE);
        parent.opModePropertiesHash.put("TOOLTIP_BOX_BORDER_SIZE", "1");
        parent.opModePropertiesHash.put("TOOLTIP_BOX_BORDER_COLOR", "black");
        parent.opModePropertiesHash.put("TOOLTIP_BOX_COLOR", "white");
        parent.opModePropertiesHash.put("TOOLTIP_TEXT_COLOR", "black");
        parent.opModePropertiesHash.put("TOOLTIP_HORIZONTAL_MARGIN", "5");
        parent.opModePropertiesHash.put("TOOLTIP_VERTICAL_MARGIN", "5");
        parent.opModePropertiesHash.put("TOOLTIP_OFFSET", "5");
        parent.opModePropertiesHash.put("KABOUM_FONT_NAME", "Courier");
        parent.opModePropertiesHash.put("KABOUM_FONT_STYLE", "plain");
        parent.opModePropertiesHash.put("KABOUM_FONT_SIZE", "12");
        parent.opModePropertiesHash.put("GEOMETRY_ROUGHNESS", "1");
        parent.defaultDD = new KaboumGeometryDisplayDescriptor("DEFAULT");
        parent.geometryDDHash.put("DEFAULT", parent.defaultDD);
        parent.currentDD = parent.defaultDD;
        parent.defaultPD = new KaboumGeometryPropertiesDescriptor("DEFAULT", true, false, false, false);
        parent.geometryPDHash.put("DEFAULT", parent.defaultPD);
        parent.currentPD = parent.defaultPD;
        parent.currentDD = parent.defaultDD;
        parent.currentPD = parent.defaultPD;
    }

    /**
     * Parses list of parameters contained in the shuttle and refreshes kaboum <p>
     * The format for global parameters is:<br>
     *
     */
    private void loadGeoClassesParameters(KaboumFeatureShuttle shuttle) {
        if (shuttle == null) {
            KaboumUtil.debug("KaboumFeatureServerTools.loadGeoClassesParameters: null input shuttle");
            return;
        }
        if (shuttle.parameters == null || shuttle.parameters.size() == 0) {
            KaboumUtil.debug("KaboumFeatureServerTools.loadGeoClassesParameters: null or empty shuttle's parameters Hashtable");
            return;
        }
        String param = null;
        StringTokenizer st = null;
        parent.opModePropertiesHash.put("KABOUM_USE_TOOLTIP", shuttle.parameters.getProperty("KABOUM_USE_TOOLTIP", Kaboum.FALSE));
        parent.opModePropertiesHash.put("TOOLTIP_BOX_BORDER_SIZE", shuttle.parameters.getProperty("TOOLTIP_BOX_BORDER_SIZE", "1"));
        parent.opModePropertiesHash.put("TOOLTIP_BOX_BORDER_COLOR", shuttle.parameters.getProperty("TOOLTIP_BOX_BORDER_COLOR", "black"));
        parent.opModePropertiesHash.put("TOOLTIP_BOX_COLOR", shuttle.parameters.getProperty("TOOLTIP_BOX_COLOR", "white"));
        parent.opModePropertiesHash.put("TOOLTIP_TEXT_COLOR", shuttle.parameters.getProperty("TOOLTIP_TEXT_COLOR", "black"));
        parent.opModePropertiesHash.put("TOOLTIP_HORIZONTAL_MARGIN", shuttle.parameters.getProperty("TOOLTIP_HORIZONTAL_MARGIN", "5"));
        parent.opModePropertiesHash.put("TOOLTIP_VERTICAL_MARGIN", shuttle.parameters.getProperty("TOOLTIP_VERTICAL_MARGIN", "5"));
        parent.opModePropertiesHash.put("TOOLTIP_OFFSET", shuttle.parameters.getProperty("TOOLTIP_OFFSET", "5"));
        parent.opModePropertiesHash.put("KABOUM_FONT_NAME", shuttle.parameters.getProperty("KABOUM_FONT_NAME", "Courier"));
        parent.opModePropertiesHash.put("KABOUM_FONT_STYLE", shuttle.parameters.getProperty("KABOUM_FONT_STYLE", "plain"));
        parent.opModePropertiesHash.put("KABOUM_FONT_SIZE", shuttle.parameters.getProperty("KABOUM_FONT_SIZE", "12"));
        parent.opModePropertiesHash.put("GEOMETRY_ROUGHNESS", shuttle.parameters.getProperty("GEOMETRY_ROUGHNESS", "1"));
        parent.defaultDD = new KaboumGeometryDisplayDescriptor("DEFAULT", KaboumUtil.getColorParameter(shuttle.parameters.getProperty("DEFAULT_DD_COLOR"), null), KaboumUtil.getColorParameter(shuttle.parameters.getProperty("DEFAULT_DD_HILITE_COLOR"), null), KaboumUtil.getColorParameter(shuttle.parameters.getProperty("DEFAULT_DD_MODIFIED_COLOR"), null), KaboumCoordinate.stoi(shuttle.parameters.getProperty("DEFAULT_DD_POINT_TYPE")), KaboumUtil.stoi(shuttle.parameters.getProperty("DEFAULT_DD_POINT_HEIGHT")), KaboumUtil.stoi(shuttle.parameters.getProperty("DEFAULT_DD_POINT_WIDTH")), KaboumUtil.stoi(shuttle.parameters.getProperty("DEFAULT_DD_LINE_WIDTH")), KaboumUtil.getColorParameter(shuttle.parameters.getProperty("DEFAULT_DD_POINT_COLOR"), null), KaboumUtil.getColorParameter(shuttle.parameters.getProperty("DEFAULT_DD_POINT_HILITE_COLOR"), null), parent.readImage(parent, KaboumUtil.toURL(shuttle.parameters.getProperty("DEFAULT_DD_POINT_IMAGE"))), KaboumUtil.stob(shuttle.parameters.getProperty("DEFAULT_DD_IS_FILLED"), false));
        parent.geometryDDHash.put("DEFAULT", parent.defaultDD);
        parent.currentDD = parent.defaultDD;
        param = shuttle.parameters.getProperty("DD_CLASS_LIST");
        if (param != null) {
            KaboumGeometryDisplayDescriptor kaboumDisplayDescriptor;
            st = new StringTokenizer(param, ",");
            if (st.countTokens() == 0) {
                String s = param + ",";
                st = new StringTokenizer(param, ",");
            }
            while (st.hasMoreTokens()) {
                String geoName = st.nextToken();
                if (geoName.equals("DEFAULT")) {
                    continue;
                }
                int geoPointType;
                if (shuttle.parameters.getProperty(geoName + "_DD_POINT_TYPE") == null) {
                    geoPointType = parent.defaultDD.getPointType();
                } else {
                    geoPointType = KaboumCoordinate.stoi(shuttle.parameters.getProperty(geoName + "_DD_POINT_TYPE"));
                }
                Image geoPointImage;
                if (shuttle.parameters.getProperty(geoName + "_DD_POINT_IMAGE") == null) {
                    geoPointImage = parent.defaultDD.getPointImage();
                } else {
                    geoPointImage = parent.readImage(parent, KaboumUtil.toURL(shuttle.parameters.getProperty(geoName + "_DD_POINT_IMAGE")));
                }
                kaboumDisplayDescriptor = new KaboumGeometryDisplayDescriptor(geoName, KaboumUtil.getColorParameter(shuttle.parameters.getProperty(geoName + "_DD_COLOR"), parent.defaultDD.getColor()), KaboumUtil.getColorParameter(shuttle.parameters.getProperty(geoName + "_DD_HILITE_COLOR"), parent.defaultDD.getHiliteColor()), KaboumUtil.getColorParameter(shuttle.parameters.getProperty(geoName + "_DD_MODIFIED_COLOR"), parent.defaultDD.getModifiedColor()), geoPointType, KaboumUtil.stoi(shuttle.parameters.getProperty(geoName + "_DD_POINT_HEIGHT"), parent.defaultDD.getPointHeight()), KaboumUtil.stoi(shuttle.parameters.getProperty(geoName + "_DD_POINT_WIDTH"), parent.defaultDD.getPointWidth()), KaboumUtil.stoi(shuttle.parameters.getProperty(geoName + "_DD_LINE_WIDTH"), parent.defaultDD.getLineWidth()), KaboumUtil.getColorParameter(shuttle.parameters.getProperty(geoName + "_DD_POINT_COLOR"), parent.defaultDD.getPointColor()), KaboumUtil.getColorParameter(shuttle.parameters.getProperty(geoName + "_DD_HILITE_COLOR"), parent.defaultDD.getHiliteColor()), geoPointImage, KaboumUtil.stob(shuttle.parameters.getProperty(geoName + "_DD_IS_FILLED"), parent.defaultDD.getFilling()));
                parent.geometryDDHash.put(geoName, kaboumDisplayDescriptor);
            }
        }
        parent.defaultPD = new KaboumGeometryPropertiesDescriptor("DEFAULT", KaboumUtil.stob(shuttle.parameters.getProperty("DEFAULT_PD_IS_VISIBLE"), true), KaboumUtil.stob(shuttle.parameters.getProperty("DEFAULT_PD_IS_COMPUTED"), false), KaboumUtil.stob(shuttle.parameters.getProperty("DEFAULT_PD_IS_SURROUNDING"), false), KaboumUtil.stob(shuttle.parameters.getProperty("DEFAULT_PD_IS_LOCKED"), false));
        parent.geometryPDHash.put("DEFAULT", parent.defaultPD);
        parent.currentPD = parent.defaultPD;
        param = shuttle.parameters.getProperty("PD_CLASS_LIST");
        if (param != null) {
            KaboumGeometryPropertiesDescriptor kaboumObjectProperties;
            st = new StringTokenizer(param, ",");
            if (st.countTokens() == 0) {
                String s = param + ",";
                st = new StringTokenizer(param, ",");
            }
            while (st.hasMoreTokens()) {
                String propName = st.nextToken();
                if (propName.equals("DEFAULT")) {
                    continue;
                }
                kaboumObjectProperties = new KaboumGeometryPropertiesDescriptor(propName, KaboumUtil.stob(shuttle.parameters.getProperty(propName + "_PD_IS_VISIBLE"), parent.defaultPD.isVisible()), KaboumUtil.stob(shuttle.parameters.getProperty(propName + "_PD_IS_COMPUTED"), parent.defaultPD.isComputed()), KaboumUtil.stob(shuttle.parameters.getProperty(propName + "_PD_IS_SURROUNDING"), parent.defaultPD.isSurrounding()), KaboumUtil.stob(shuttle.parameters.getProperty(propName + "_PD_IS_LOCKED"), parent.defaultPD.isLocked()));
                parent.geometryPDHash.put(propName, kaboumObjectProperties);
            }
        }
        param = shuttle.parameters.getProperty("GEOMETRY_ACTIVE_DD");
        if (param != null) {
            parent.currentDD = (KaboumGeometryDisplayDescriptor) parent.geometryDDHash.get(param);
            if (parent.currentDD == null) {
                parent.currentDD = parent.defaultDD;
            }
        }
        param = shuttle.parameters.getProperty("GEOMETRY_ACTIVE_PD");
        if (param != null) {
            parent.currentPD = (KaboumGeometryPropertiesDescriptor) parent.geometryPDHash.get(param);
            if (parent.currentPD == null) {
                parent.currentPD = parent.defaultPD;
            }
        }
    }

    /**
     * Sends a validate command to the server. Builds a valid shuttle and sets it to the validate mode,
     * filling its geometry.
     * @param geom the geometry to validate
     * @param className the name of the geometry's class
     */
    public synchronized void validateGeometry(KaboumGeometry geom, String className) {
        tmpShuttle = new KaboumFeatureShuttle(this.featureFile, parent.mapServerTools.getExtent().externalString(" ", " "));
        Vector vec = new Vector(1);
        vec.addElement(geom);
        tmpShuttle.geometries.put(className, vec);
        tmpShuttle.mode = KaboumFeatureModes.K_VALIDATE;
        tmpShuttle = getServerResponse(tmpShuttle);
        if (tmpShuttle != null && tmpShuttle.errorCode > KaboumFeatureModes.ERROR_CODE_RANGE) {
            parent.kaboumResult("ALERT|" + KaboumFeatureModes.getErrorKeyword(tmpShuttle.errorCode));
            return;
        }
    }

    /**
     * Adds the latest validated geometry in Kaboum. This trick allows GeometryOpMode to clean itself
     * befor inserting a new geometry.
     * TODO: Must change this mechanism to validate and re-add geometries in one step.
     */
    public void addAfterValidate() {
        insertGeometries(tmpShuttle);
        parent.standbyOff();
        parent.respawnOpMode();
    }

    /** Sends an init command to the server. Builds a valid shuttle and sets the init mode.<p>
     * after the response, adds returned objects into Kaboum
     */
    public void init() {
        KaboumFeatureShuttle shuttle = new KaboumFeatureShuttle(this.featureFile, parent.mapServerTools.getExtent().externalString(" ", " "));
        shuttle.mode = KaboumFeatureModes.K_INIT;
        shuttle = getServerResponse(shuttle);
        if (shuttle != null && shuttle.errorCode != 0) {
            parent.kaboumResult("ALERT|" + KaboumFeatureModes.getErrorKeyword(shuttle.errorCode));
            return;
        }
        insertGeometries(shuttle);
    }

    public void insertGeometries(KaboumFeatureShuttle shuttle) {
        if (shuttle == null && shuttle.geometries == null) {
            KaboumUtil.debug("insertGeometries: null shuttle or null geometries");
            return;
        }
        Enumeration keys = shuttle.geometries.keys();
        while (keys.hasMoreElements()) {
            String className = (String) keys.nextElement();
            Vector vec = (Vector) shuttle.geometries.get(className);
            if (vec != null) {
                for (int i = 0; i < vec.size(); i++) {
                    if (i == 0) {
                        try {
                            KaboumWKTWriter writer = new KaboumWKTWriter(new KaboumPrecisionModel());
                            System.out.println("geom: " + writer.write((KaboumGeometry) vec.elementAt(i)));
                        } catch (Exception e) {
                        }
                    }
                    insertGeometry(className, (KaboumGeometry) vec.elementAt(i));
                }
            }
        }
    }

    /**
     * Inserts the given geometry in Kaboum under the given className
     * Caution: DD and PD names must be the same for a given geometry.
     * @param className the name of the geometry's class
     * @geom the geometry to insert
     */
    public void insertGeometry(String className, KaboumGeometry geom) {
        if (geom == null) {
            KaboumUtil.debug("null geometry in insertGeometry");
            return;
        }
        KaboumGeometryPropertiesDescriptor tmpPD = null;
        tmpPD = (KaboumGeometryPropertiesDescriptor) parent.geometryPDHash.get(className);
        if (tmpPD == null) {
            KaboumUtil.debug("WARNING ! : Properties Descriptor " + className + " doesn't exist. Using DEFAULT instead ");
            tmpPD = parent.defaultPD;
        }
        KaboumGeometryDisplayDescriptor tmpDD = null;
        tmpDD = (KaboumGeometryDisplayDescriptor) parent.geometryDDHash.get(className);
        if (tmpDD == null) {
            KaboumUtil.debug("WARNING ! : Display Descriptor " + className + " doesn't exist. Using DEFAULT instead ");
            tmpDD = parent.defaultDD;
        }
        if (geom != null) {
            KaboumUtil.debug("insertGeometry: adding geometry: " + geom.id);
            parent.GGDIndex.addGeometry(geom, tmpDD, tmpPD, null);
            int position = parent.GGDIndex.getGGDIndex(geom.id);
            KaboumGeometryGlobalDescriptor tmpGGD = (KaboumGeometryGlobalDescriptor) parent.GGDIndex.elementAt(position);
            if (tmpGGD != null) {
                tmpGGD.setModified(false);
            }
            if (geom.isClosed()) {
                parent.currentSurface = geom.getArea();
            }
            parent.currentPerimeter = geom.getPerimeter();
            parent.repaint();
        }
    }

    /**
     * Retrieves geometric objects for the current extent from the server.
     *<p>
     * If currentExtent is within previous extent, does nothing as objects were already loaded
     *</p>
     * If standbyOn() was called on this class, this method does nothing.<br>
     * re-enable features retrieval by calling standbyOff().
     *TODO: in case of feature count limit, this mechanism is not true: some objects can be missing
     */
    public void getFeatures() {
        if (standby) {
            return;
        }
        if (biggestExtent.contains(parent.mapServerTools.extent)) {
            System.out.println("skipping feature loading...");
        } else {
            KaboumFeatureShuttle shuttle = new KaboumFeatureShuttle(this.featureFile, parent.mapServerTools.getExtent().externalString(" ", " "));
            shuttle.mode = KaboumFeatureModes.K_GET_FEATURES;
            shuttle = getServerResponse(shuttle);
            if (shuttle != null && shuttle.errorCode != 0) {
                parent.kaboumResult("ALERT|" + KaboumFeatureModes.getErrorKeyword(shuttle.errorCode));
                return;
            }
            insertGeometries(shuttle);
        }
        if (parent.mapServerTools.getExtent().contains(biggestExtent)) {
            System.out.println("swapping current and biggest extent");
            biggestExtent.set(parent.mapServerTools.getExtent());
        }
    }

    /** Sends shuttle object to the server, asking it to response with a filled shuttle,
     * based on the mode and extra parameters.
     */
    public KaboumFeatureShuttle getServerResponse(KaboumFeatureShuttle shuttleIn) {
        KaboumFeatureShuttle shuttleOut = null;
        ObjectOutputStream oout = null;
        ObjectInputStream oin = null;
        URL serverURL = KaboumUtil.toURL(featureServerURL);
        URLConnection con = null;
        try {
            long t0 = System.currentTimeMillis();
            KaboumUtil.debug("FeatureServer call: " + serverURL.toString());
            con = serverURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);
            con.setRequestProperty("Content-Type", "application/octet-stream");
            long tcon = System.currentTimeMillis();
            KaboumUtil.debug("sending shuttle: " + shuttleIn.toString());
            oout = new ObjectOutputStream(con.getOutputStream());
            oout.writeObject(shuttleIn);
            oout.flush();
            oout.close();
            shuttleIn = null;
            long twrite = System.currentTimeMillis();
            oin = new ObjectInputStream(con.getInputStream());
            shuttleOut = (KaboumFeatureShuttle) oin.readObject();
            KaboumUtil.debug("received shuttle: " + shuttleOut.toString());
            long tread = System.currentTimeMillis();
            loadGeoClassesParameters(shuttleOut);
            oin.close();
            long tend = System.currentTimeMillis();
            System.out.println("time taken for entire connection : " + (tend - t0));
            System.out.println("time taken for openConnection    : " + (tcon - t0));
            System.out.println("time taken for sending shuttle   : " + (twrite - tcon));
            System.out.println("time taken for receiving shuttle : " + (tread - twrite));
            System.out.println("time taken for params parsing    : " + (tend - tread));
        } catch (Exception ioe) {
            ioe.printStackTrace();
            KaboumUtil.debug(ioe.getMessage());
        }
        return shuttleOut;
    }

    /**
     * Returns a KaboumFeatureShuttle set to init mode
     *@returns a KaboumFeatureShuttle set to init mode.
     */
    private KaboumFeatureShuttle getInitShuttle() {
        KaboumFeatureShuttle init = new KaboumFeatureShuttle();
        init.mode = KaboumFeatureModes.K_INIT;
        init.parameters.put("mapext", parent.mapServerTools.getExtent().externalString(" ", " "));
        init.parameters.put("featurefile", this.featureFile);
        return init;
    }

    /**
     * blocks server features retrieval. No more server connection will be made after this call.<br>
     * call standbyOff() to re-enable features retrieval.
     *<p>
     *This method is controled by the OBJECT|OFF kaboum command
     *</p>
     */
    public void standbyOn() {
        this.standby = true;
    }

    /**
     * Allows server features retrieval. Call this method to re-enable features retrieval
     * after a call to standbyOn().
     * This method is controled by the OBJECT|ON kaboum command
     */
    public void standbyOff() {
        this.standby = false;
    }
}
