package com.mapmidlet.options;

import java.io.*;
import java.util.*;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.rms.*;
import com.mapmidlet.CloudGps;
import com.mapmidlet.geocoding.Geocoder;
import com.mapmidlet.misc.*;
import com.mapmidlet.projection.WorldCoordinate;
import com.mapmidlet.routing.*;
import com.mapmidlet.tile.provider.AbstractTileFactory;
import com.mapmidlet.tile.ui.ScreenMarker;
import com.mapmidlet.ui.Skin;

/**
 * This class holds many setting related variables as well as current app
 * context. It could be replaced by static variables. It has the ability to
 * store and load variables using RMS. Many default option values are defined
 * here.
 * 
 * @author Damian Waradzyn
 */
public class Options {

    public static final int ROUTING_IDLE = 0;

    public static final int ROUTING_CALCULATING = 1;

    public static final int ROUTING_ERROR = 2;

    public boolean useFileApi;

    public boolean onlineMode;

    public boolean fadeEffect;

    public boolean debugMode;

    public int maxRetries;

    public boolean fileReadInSeparateThread;

    public String rootName;

    public boolean gpsEnabled;

    private static Options instance = null;

    public String gpsUrl;

    private AbstractTileFactory tileFactory;

    private final Hashtable tileFactories;

    private String tileFactoryName;

    private final Vector factoryNames;

    public int downloaded;

    public boolean loadedFromRms;

    public int zoom;

    public Skin skin;

    public String skinPath;

    public Geocoder geocoder;

    public String replayFileName;

    public String replayDir;

    public int replaySpeed;

    public int replayPosition;

    public int replayLength;

    public boolean replayMode = false;

    public Vector markers;

    public Vector searchResults;

    public WorldCoordinate center;

    public Vector routeEnds;

    public int routeEndIndex = -1;

    public String routeType;

    public Router router;

    public Route route;

    public boolean automaticRouteCalc = true;

    public int snapTolerance = 30;

    public int offRouteRecalculateMilis = 10000;

    public int offRouteRecalculateMeters = 100;

    public NavContext navContext = new NavContext();

    public int routingStatus;

    public boolean snapToRoad = false;

    static {
        instance = new Options();
    }

    private Options() {
        tileFactories = new Hashtable();
        factoryNames = new Vector();
    }

    public static Options getInstance() {
        return instance;
    }

    public void registerTileFactory(String name, AbstractTileFactory tileFactory) {
        tileFactories.put(name, tileFactory);
        factoryNames.addElement(name);
    }

    public void setTileFactory(String name) {
        if (name.equals(tileFactoryName)) {
            return;
        }
        AbstractTileFactory tileFactory = (AbstractTileFactory) tileFactories.get(name);
        if (tileFactory == null) {
            throw new RuntimeException("Internal error: Invalid tile factory name");
        }
        this.tileFactoryName = name;
        this.tileFactory = tileFactory;
    }

    public AbstractTileFactory getTileFactory() {
        return tileFactory;
    }

    public String getTileFactoryName() {
        return tileFactoryName;
    }

    public Vector getTileFactoryNames() {
        return factoryNames;
    }

    public void load() {
        RecordStore recordStore = null;
        markers = new Vector();
        routeEnds = new Vector();
        for (int i = 0; i < 2; i++) {
            ScreenMarker routeEnd = new ScreenMarker();
            routeEnd.worldCoordinate = new WorldCoordinate();
            routeEnd.worldCoordinate.latitude = Double.NaN;
            routeEnd.worldCoordinate.longitude = Double.NaN;
            routeEnds.addElement(routeEnd);
            routeEnd.iconName = "route_point.png";
        }
        try {
            recordStore = RecordStore.openRecordStore("MapMidlet Options", true);
            if (recordStore != null && recordStore.getNumRecords() > 0) {
                RecordEnumeration records = recordStore.enumerateRecords(null, null, false);
                byte[] record = records.nextRecord();
                ByteArrayInputStream byteStream = new ByteArrayInputStream(record);
                DataInputStream dataStream = new DataInputStream(byteStream);
                useFileApi = dataStream.readBoolean();
                rootName = dataStream.readUTF();
                String tileProvider = dataStream.readUTF();
                setTileFactory(tileProvider);
                debugMode = dataStream.readBoolean();
                gpsUrl = dataStream.readUTF();
                gpsEnabled = dataStream.readBoolean();
                fadeEffect = dataStream.readBoolean();
                fileReadInSeparateThread = dataStream.readBoolean();
                maxRetries = dataStream.readInt();
                onlineMode = dataStream.readBoolean();
                center = new WorldCoordinate();
                center.latitude = dataStream.readDouble();
                center.longitude = dataStream.readDouble();
                zoom = dataStream.readInt();
                skinPath = dataStream.readUTF();
                routeEndIndex = dataStream.readInt();
                replaySpeed = 4;
                for (int i = 0; i < 2; i++) {
                    ScreenMarker routeEnd = (ScreenMarker) routeEnds.elementAt(i);
                    routeEnd.worldCoordinate.latitude = dataStream.readDouble();
                    routeEnd.worldCoordinate.longitude = dataStream.readDouble();
                    routeEnd.visible = dataStream.readBoolean();
                }
                routeType = dataStream.readUTF();
                loadedFromRms = true;
                recordStore.closeRecordStore();
            } else {
                setDefaults();
            }
        } catch (Exception e) {
            setDefaults();
            if (recordStore != null) {
                try {
                    deleteRecords(recordStore);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            CloudGps.setError(e);
        }
        refreshMarkers();
    }

    private void setDefaults() {
        useFileApi = false;
        if (CompatibilityTool.fileApiAvailable()) {
            Enumeration roots = FileSystemRegistry.listRoots();
            if (roots.hasMoreElements()) {
                rootName = (String) roots.nextElement();
            }
        }
        onlineMode = true;
        maxRetries = 2;
        fileReadInSeparateThread = false;
        fadeEffect = false;
        gpsEnabled = false;
        gpsUrl = "socket://localhost:20175";
        zoom = 1;
        setTileFactory((String) tileFactories.keys().nextElement());
        center = new WorldCoordinate();
        skinPath = "default";
        replayDir = IOTool.getDefaultNmeaLogsDir();
        replaySpeed = 1;
        debugMode = true;
    }

    public void save() {
        try {
            RecordStore recordStore = RecordStore.openRecordStore("MapMidlet Options", true);
            deleteRecords(recordStore);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(os);
            outputStream.writeBoolean(useFileApi);
            outputStream.writeUTF(rootName);
            outputStream.writeUTF(tileFactoryName);
            outputStream.writeBoolean(debugMode);
            outputStream.writeUTF(gpsUrl);
            outputStream.writeBoolean(gpsEnabled);
            outputStream.writeBoolean(fadeEffect);
            outputStream.writeBoolean(fileReadInSeparateThread);
            outputStream.writeInt(maxRetries);
            outputStream.writeBoolean(onlineMode);
            center = CloudGps.getTileCanvas().getCurrentCenter();
            outputStream.writeDouble(center.latitude);
            outputStream.writeDouble(center.longitude);
            outputStream.writeInt(zoom);
            outputStream.writeUTF(skin.path);
            outputStream.writeInt(routeEndIndex);
            for (int i = 0; i < 2; i++) {
                ScreenMarker routeEnd = (ScreenMarker) routeEnds.elementAt(i);
                outputStream.writeDouble(routeEnd.worldCoordinate.latitude);
                outputStream.writeDouble(routeEnd.worldCoordinate.longitude);
                outputStream.writeBoolean(routeEnd.visible);
            }
            outputStream.writeUTF(routeType);
            byte[] byteArray = os.toByteArray();
            recordStore.addRecord(byteArray, 0, byteArray.length);
            recordStore.closeRecordStore();
        } catch (Exception e) {
            CloudGps.setError(e);
        }
    }

    private void deleteRecords(RecordStore recordStore) throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        if (recordStore.getNumRecords() > 0) {
            RecordEnumeration records = recordStore.enumerateRecords(null, null, false);
            while (records.hasNextElement()) {
                recordStore.deleteRecord(records.nextRecordId());
            }
        }
    }

    public void refreshMarkers() {
        markers.removeAllElements();
        CollectionUtil.addAllElements(markers, searchResults);
        CollectionUtil.addAllElements(markers, routeEnds);
    }

    public void setRouter(Router router) {
        this.router = router;
        if (routeType == null || !router.getRouteTypes().contains(routeType)) {
            routeType = (String) router.getRouteTypes().elementAt(0);
        }
    }

    public static class NavContext {

        public long offRouteStartMilis = 1;

        public int directionIdx = -1;

        public OptimizedRoute optimizedRoute;

        public double tolerancePixels = 2.0;

        public double minRemovedPercent = 15;

        public int minRemovedCount = 10;
    }
}
