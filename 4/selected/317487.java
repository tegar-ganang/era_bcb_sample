package com.iver.cit.jdwglib.dwg;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.iver.cit.jdwglib.dwg.objects.DwgBlockHeader;
import com.iver.cit.jdwglib.dwg.objects.DwgInsert;
import com.iver.cit.jdwglib.dwg.objects.DwgLayer;
import com.iver.cit.jdwglib.dwg.readers.DwgFileV12Reader;
import com.iver.cit.jdwglib.dwg.readers.DwgFileV14Reader;
import com.iver.cit.jdwglib.dwg.readers.DwgFileV15Reader;
import com.iver.cit.jdwglib.dwg.readers.DwgFileVR2004Reader;
import com.iver.cit.jdwglib.dwg.readers.IDwgFileReader;

/**
 * The DwgFile class provides a revision-neutral interface for reading and handling
 * DWG files
 * Reading methods are useful for reading DWG files, and handling methods like
 * calculateDwgPolylines() are useful for handling more complex
 * objects in the DWG file
 *
 * @author jmorell
 * @author azabala
 */
public class DwgFile {

    /**
	 * It has all known DWG version's header.
	 * Extracted from Autodesk web.
	 */
    private static HashMap acadVersions = new HashMap();

    static {
        acadVersions.put("AC1004", "Autocad R9");
        acadVersions.put("AC1006", "Autocad R10");
        acadVersions.put("AC1007", "Autocad pre-R11");
        acadVersions.put("AC1007", "Autocad pre-R11");
        acadVersions.put("AC1008", "Autocad pre-R11b");
        acadVersions.put("AC1009", "Autocad R12");
        acadVersions.put("AC1010", "Autocad pre-R13 a");
        acadVersions.put("AC1011", "Autocad pre-R13 b");
        acadVersions.put("AC1012", "Autocad R13");
        acadVersions.put("AC1013", "Autocad pre-R14");
        acadVersions.put("AC1014", "Autocad R14");
        acadVersions.put("AC1500", "Autocad pre-2000");
        acadVersions.put("AC1015", "Autocad R2000, R2000i, R2002");
        acadVersions.put("AC402a", "Autocad pre-2004a");
        acadVersions.put("AC402b", "Autocad pre-2004b");
        acadVersions.put("AC1018", "Autocad R2004, R2005, R2006");
        acadVersions.put("AC1021", "Autocad R2007");
    }

    private static Logger logger = Logger.getLogger(DwgFile.class.getName());

    /**
	 * Path and name of the dwg file
	 * */
    private String fileName;

    /**
	 * DWG version of the file (AC1013, AC1018, etc.)
	 * */
    private String dwgVersion;

    /**
	 * Offsets to the DWG sections
	 */
    private ArrayList dwgSectionOffsets;

    /**
	 * Header vars readed from the HEADER section of the DWG file
	 * */
    private Map headerVars;

    /**
	 * This list contains what in OpenDWG specification is called
	 * the object map ( a collection of entries where each entry contains
	 * the seek of each object, and its size)
	 * */
    private ArrayList dwgObjectOffsets;

    /**
	 * For each entry in dwgObjectOffsets, we have an instance of
	 * DwgObject in the dwgObjects collection
	 *
	 * */
    private List dwgObjects;

    private HashMap handle_objects;

    private ArrayList dwgClasses;

    /**
	 * hash map that indexes all DwgLayer objects
	 * by its handle property
	 * */
    private HashMap layerTable;

    /**
	 * Specific reader of the DWG file version (12, 13, 14, 2000, etc., each
	 * version will have an specific reader)
	 * */
    private IDwgFileReader dwgReader;

    private boolean dwg3DFile = false;

    /**
	 * Memory mapped byte buffer of the whole DWG file
	 * */
    private ByteBuffer bb;

    /**
	 * Contains all IDwgPolyline implementations
	 * */
    private List dwgPolylines;

    /**
	 * Contains all INSERT entities of the dwg file
	 * (these entities must be processed in a second pass)
	 *
	 * */
    private List insertList;

    private List blockList;

    /**
	 * Constructor.
	 * @param fileName an absolute path to the DWG file
	 */
    public DwgFile(String fileName) {
        this.fileName = fileName;
        dwgSectionOffsets = new ArrayList();
        dwgObjectOffsets = new ArrayList();
        headerVars = new HashMap();
        dwgClasses = new ArrayList();
        dwgObjects = new ArrayList();
        handle_objects = new HashMap();
        layerTable = new HashMap();
        dwgPolylines = new ArrayList();
        insertList = new ArrayList();
        blockList = new ArrayList();
    }

    public String getDwgVersion() {
        return dwgVersion;
    }

    /**
	 * Reads a DWG file and put its objects in the dwgObjects Vector
	 * This method is version independent
	 *
	 * @throws IOException If the file location is wrong
	 */
    public void read() throws IOException, DwgVersionNotSupportedException {
        setDwgVersion();
        if (dwgVersion.equalsIgnoreCase("Autocad R2000, R2000i, R2002")) {
            dwgReader = new DwgFileV15Reader();
        } else if (dwgVersion.equalsIgnoreCase("Autocad pre-R14") || dwgVersion.equalsIgnoreCase("Autocad R14") || dwgVersion.equalsIgnoreCase("Autocad R13")) {
            dwgReader = new DwgFileV14Reader();
        } else if (dwgVersion.equalsIgnoreCase("Autocad R2004, R2005, R2006")) {
            dwgReader = new DwgFileVR2004Reader();
        } else if (dwgVersion.equalsIgnoreCase("Autocad R12") || dwgVersion.equalsIgnoreCase("Autocad pre-R13 a") || dwgVersion.equalsIgnoreCase("Autocad pre-R13 b")) {
            boolean isR13 = true;
            if (dwgVersion.equalsIgnoreCase("Autocad R12")) isR13 = false;
            dwgReader = new DwgFileV12Reader(isR13);
        } else {
            DwgVersionNotSupportedException exception = new DwgVersionNotSupportedException("Version de DWG no soportada");
            exception.setDwgVersion(dwgVersion);
            throw exception;
        }
        try {
            dwgReader.read(this, bb);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error leyendo dwg");
        }
    }

    private void setDwgVersion() throws IOException {
        File file = new File(fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        FileChannel fileChannel = fileInputStream.getChannel();
        long channelSize = fileChannel.size();
        bb = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, channelSize);
        byte[] versionBytes = { bb.get(0), bb.get(1), bb.get(2), bb.get(3), bb.get(4), bb.get(5) };
        ByteBuffer versionByteBuffer = ByteBuffer.wrap(versionBytes);
        String[] bs = new String[versionByteBuffer.capacity()];
        String versionString = "";
        for (int i = 0; i < versionByteBuffer.capacity(); i++) {
            bs[i] = new String(new byte[] { (byte) (versionByteBuffer.get(i)) });
            versionString = versionString + bs[i];
        }
        String version = (String) acadVersions.get(versionString);
        if (version == null) version = "Unknown Dwg format";
        this.dwgVersion = version;
    }

    public void setHeader(String key, Object value) {
        headerVars.put(key, value);
    }

    public Object getHeader(String key) {
        return headerVars.get(key);
    }

    protected void addDwgLayer(DwgLayer dwgLayer) {
        layerTable.put(new Integer(dwgLayer.getHandle().getOffset()), dwgLayer);
    }

    private void printInfoOfAObject(DwgObject entity) {
        logger.info("index = " + entity.getIndex() + " entity.type = " + entity.type + " entityClassName = " + entity.getClass().getName());
        logger.info("handleCode = " + entity.getHandle().getCode());
        logger.info("entityLayerHandle = " + entity.getHandle().getOffset());
        if (entity.hasLayerHandle()) {
            logger.info("layerHandleCode = " + entity.getLayerHandle().getCode());
            logger.info("layerHandle = " + entity.getLayerHandle().getOffset());
        }
        if (entity.hasSubEntityHandle()) {
            logger.info("subEntityHandleCode = " + entity.getSubEntityHandle().getCode());
            logger.info("subEntityHandle = " + entity.getSubEntityHandle().getOffset());
        }
        if (entity.hasNextHandle()) {
            logger.info("nextHandleCode = " + entity.getNextHandle().getCode());
            logger.info("nextHandle = " + entity.getNextHandle().getOffset());
        }
        if (entity.hasPreviousHandle()) {
            logger.info("previousHandleCode = " + entity.getPreviousHandle().getCode());
            logger.info("previousHandle = " + entity.getPreviousHandle().getOffset());
        }
        if (entity.hasXDicObjHandle()) {
            logger.info("xDicObjHandleCode = " + entity.getXDicObjHandle());
            logger.info("xDicObjHandle = " + entity.getXDicObjHandle());
        }
        if (entity.hasReactorsHandles()) {
            ArrayList reactorsHandles = entity.getReactorsHandles();
            int size = reactorsHandles.size();
            logger.info("NUMERO DE reactors = " + size);
            DwgHandleReference hr;
            for (int i = 0; i < size; i++) {
                hr = (DwgHandleReference) reactorsHandles.get(i);
                logger.info("reactorHandleCode = " + hr.getCode());
                logger.info(" reactorHandle = " + hr.getOffset());
            }
        }
    }

    public DwgLayer getDwgLayer(DwgObject entity) {
        DwgHandleReference handle = entity.getLayerHandle();
        if (handle == null) {
            return null;
        }
        int handleCode = handle.getCode();
        int entityLayerHandle = entity.getLayerHandle().getOffset();
        int layerHandle = -1;
        int entityRecord;
        DwgObject object;
        switch(handleCode) {
            case 0x4:
                if (entity.hasNextHandle()) {
                    int nextHandleCode = entity.getNextHandle().getCode();
                    if (nextHandleCode == 0x5) {
                        layerHandle = entity.getNextHandle().getOffset();
                    } else {
                    }
                } else {
                    layerHandle = entity.getLayerHandle().getOffset();
                }
                break;
            case 0x5:
                layerHandle = entity.getLayerHandle().getOffset();
                break;
            case 0x8:
                if (entity.hasNextHandle()) {
                    int nextHandleCode = entity.getNextHandle().getCode();
                    if (nextHandleCode == 0x5) {
                        layerHandle = entity.getNextHandle().getOffset();
                    } else {
                    }
                } else {
                    layerHandle = entity.getHandle().getOffset() - 1;
                }
                break;
            case 0xC:
                if (entity.hasNextHandle()) {
                    int nextHandleCode = entity.getNextHandle().getCode();
                    if (nextHandleCode == 0x5) {
                        layerHandle = entity.getNextHandle().getOffset();
                    } else {
                    }
                } else {
                    layerHandle = entity.getHandle().getOffset() - entity.getLayerHandle().getOffset() + 1;
                }
                break;
            default:
        }
        if (layerHandle != -1) {
            Iterator lyrIterator = layerTable.values().iterator();
            while (lyrIterator.hasNext()) {
                DwgLayer lyr = (DwgLayer) lyrIterator.next();
                int lyrHdl = lyr.getHandle().getOffset();
                if (lyrHdl == layerHandle) {
                    return lyr;
                }
            }
        }
        return null;
    }

    public DwgObject getDwgSuperEntity(DwgObject entity) {
        if (entity.hasSubEntityHandle()) {
            int handleCode = entity.subEntityHandle.getCode();
            int offset = entity.subEntityHandle.getOffset();
            int handle = -1;
            DwgObject object;
            switch(handleCode) {
                case 0x4:
                case 0x5:
                    handle = offset;
                    break;
                case 0x8:
                    handle = entity.getHandle().getOffset() - 1;
                    break;
                case 0xA:
                    handle = entity.getHandle().getOffset() + offset;
                    break;
                case 0xC:
                    handle = entity.getHandle().getOffset() - offset;
                    break;
                default:
                    logger.warn("DwgObject.getDwgSuperEntity: handleCode " + handleCode + " no implementado. offset = " + offset);
            }
            if (handle != -1) {
                object = getDwgObjectFromHandle(handle);
                if (object != null) return object;
            }
        }
        return null;
    }

    public String getLayerName(DwgObject entity) {
        DwgLayer dwgLayer = getDwgLayer(entity);
        if (dwgLayer == null) {
            return "";
        } else {
            return dwgLayer.getName();
        }
    }

    /**
     * Returns the color of the layer of a DWG object
	 *
     * @param entity DWG object which we want to know its layer color
	 * @return int Layer color of the DWG object in the Autocad color code
	 */
    public int getColorByLayer(DwgObject entity) {
        int colorByLayer;
        DwgLayer dwgLyr = getDwgLayer(entity);
        if (dwgLyr == null) colorByLayer = 0; else colorByLayer = dwgLyr.getColor();
        return colorByLayer;
    }

    /**
	 * Configure the geometry of the polylines in a DWG file from the vertex list in
	 * this DWG file. This geometry is given by an array of Points.
	 * Besides, manage closed polylines and polylines with bulges in a GIS Data model.
     * It means that the arcs of the polylines will be done through a set of points and
     * a distance between these points.
	 */
    public void calculateGisModelDwgPolylines() {
        if (!(dwgReader instanceof DwgFileV12Reader)) {
            for (int i = 0; i < dwgPolylines.size(); i++) {
                DwgObject pol = (DwgObject) dwgPolylines.get(i);
                if (pol instanceof IDwgPolyline) {
                    ((IDwgPolyline) pol).calculateGisModel(this);
                }
            }
        }
    }

    public void blockManagement2() {
        Iterator it = null;
        if (!(dwgReader instanceof DwgFileV12Reader)) {
            it = dwgObjects.iterator();
            int i = 0;
            while (it.hasNext()) {
                DwgObject entity = (DwgObject) it.next();
                DwgObject superEnt = getDwgSuperEntity(entity);
                if (superEnt instanceof DwgBlockHeader) {
                    DwgBlockHeader blk = (DwgBlockHeader) superEnt;
                    blk.addObject(entity);
                    it.remove();
                    i++;
                }
            }
        }
        it = insertList.iterator();
        while (it.hasNext()) {
            DwgInsert insert = (DwgInsert) it.next();
            if (insert.isProcessed()) {
                continue;
            }
            insert.setProcessed(true);
            double[] p = insert.getInsertionPoint();
            Point2D point = new Point2D.Double(p[0], p[1]);
            double[] scale = insert.getScale();
            double rot = insert.getRotation();
            int blockHandle = insert.getBlockHeaderHandle().getOffset();
            manageInsert2(point, scale, rot, blockHandle, dwgObjects, handle_objects);
        }
    }

    public void manageInsert2(Point2D insPoint, double[] scale, double rot, int bHandle, List dwgObjectsWithoutBlocks, Map handleObjectsWithoutBlocks) {
        DwgObject object = (DwgObject) handle_objects.get(new Integer(bHandle));
        if (object == null) {
            logger.error("No hemos encontrado el BlockHeader cuyo handle es " + bHandle);
            return;
        } else if (!(object instanceof DwgBlockHeader)) {
            logger.error("handle incorrecto." + object.getClass().getName() + " no es un blockheader");
            return;
        }
        DwgBlockHeader blockHeader = (DwgBlockHeader) object;
        double[] bPoint = blockHeader.getBasePoint();
        String bname = blockHeader.getName();
        if (bname.startsWith("*")) return;
        List entities = blockHeader.getObjects();
        if (entities.size() == 0) {
            logger.warn("El bloque " + blockHeader.getName() + " no tiene ninguna entidad");
        }
        Iterator blkEntIt = entities.iterator();
        while (blkEntIt.hasNext()) {
            DwgObject obj = (DwgObject) blkEntIt.next();
            manageBlockEntity(obj, bPoint, insPoint, scale, rot, dwgObjectsWithoutBlocks, handleObjectsWithoutBlocks);
        }
    }

    public int getIndexOf(DwgObject dwgObject) {
        return dwgObjects.indexOf(dwgObject);
    }

    /**
     * Changes the location of an object extracted from a block. This location will be
     * obtained through the insertion parameters from the block and the corresponding
     * insert.
     * @param entity, the entity extracted from the block.
     * @param bPoint, offset for the coordinates of the entity.
     * @param insPoint, coordinates of the insertion point for the entity.
     * @param scale, scale for the entity.
     * @param rot, rotation angle for the entity.
     * @param id, a count as a id.
     * @param dwgObjectsWithoutBlocks, a object list with the elements extracted from
     * the blocks.
     */
    private void manageBlockEntity(DwgObject entity, double[] bPoint, Point2D insPoint, double[] scale, double rot, List dwgObjectsWithoutBlocks, Map handleObjectsWithoutBlocks) {
        if (entity instanceof IDwgBlockMember) {
            IDwgBlockMember blockMember = (IDwgBlockMember) entity;
            blockMember.transform2Block(bPoint, insPoint, scale, rot, dwgObjectsWithoutBlocks, handleObjectsWithoutBlocks, this);
        }
    }

    /**
	 * Add a DWG section offset to the dwgSectionOffsets vector
	 *
	 * @param key Define the DWG section
	 * @param seek Offset of the section
	 * @param size Size of the section
	 */
    public void addDwgSectionOffset(String key, int seek, int size) {
        DwgSectionOffset dso = new DwgSectionOffset(key, seek, size);
        dwgSectionOffsets.add(dso);
    }

    /**
     * Returns the offset of DWG section given by its key
	 *
     * @param key Define the DWG section
	 * @return int Offset of the section in the DWG file
	 */
    public int getDwgSectionOffset(String key) {
        int offset = 0;
        for (int i = 0; i < dwgSectionOffsets.size(); i++) {
            DwgSectionOffset dso = (DwgSectionOffset) dwgSectionOffsets.get(i);
            String ikey = dso.getKey();
            if (key.equals(ikey)) {
                offset = dso.getSeek();
                break;
            }
        }
        return offset;
    }

    /**
	 * Add a DWG object offset to the dwgObjectOffsets vector
	 *
	 * @param handle Object handle
	 * @param offset Offset of the object data in the DWG file
	 */
    public void addDwgObjectOffset(int handle, int offset) {
        DwgObjectOffset doo = new DwgObjectOffset(handle, offset);
        dwgObjectOffsets.add(doo);
    }

    /**
	 *
	 * Add a DWG object to the dwgObject vector
	 *
	 * @param dwgObject DWG object
	 */
    public void addDwgObject(DwgObject dwgObject) {
        dwgObjects.add(dwgObject);
        if (dwgObject instanceof DwgLayer) {
            this.addDwgLayer((DwgLayer) dwgObject);
        }
        if (dwgObject instanceof IDwgExtrusionable) {
            ((IDwgExtrusionable) dwgObject).applyExtrussion();
        }
        if (dwgObject instanceof IDwgPolyline) {
            dwgPolylines.add(dwgObject);
        }
        if (dwgObject instanceof IDwg3DTestable) {
            if (!isDwg3DFile()) {
                setDwg3DFile(((IDwg3DTestable) dwgObject).has3DData());
            }
        }
        if (dwgObject instanceof DwgInsert) {
            insertList.add(dwgObject);
        }
        if (dwgObject instanceof DwgBlockHeader) {
            blockList.add(dwgObject);
        }
        handle_objects.put(new Integer(dwgObject.getHandle().getOffset()), dwgObject);
    }

    /**
	 * Returns dwgObjects from its insertion order (position
	 * in the dwg file)
	 *
	 * @param index order in the dwg file
	 * @return position
	 * */
    public DwgObject getDwgObject(int index) {
        return (DwgObject) dwgObjects.get(index);
    }

    public DwgObject getDwgObjectFromHandle(int handle) {
        return (DwgObject) handle_objects.get(new Integer(handle));
    }

    /**
	 * Add a DWG class to the dwgClasses vector
	 *
	 * @param dwgClass DWG class
	 */
    public void addDwgClass(DwgClass dwgClass) {
        dwgClasses.add(dwgClass);
    }

    /**
	 * Add a DWG class to the dwgClasses vector
	 *
	 * @param dwgClass DWG class
	 */
    public void addDwgClass(DwgClass2004 dwgClass) {
        dwgClasses.add(dwgClass);
    }

    public void printClasses() {
        logger.info("#### CLASSES ####");
        for (int i = 0; i < dwgClasses.size(); i++) {
            DwgClass clazz = (DwgClass) dwgClasses.get(i);
            logger.info(clazz.toString());
        }
        logger.info("#############");
    }

    public List getDwgClasses() {
        return dwgClasses;
    }

    /**
     * @return Returns the dwgObjectOffsets.
     */
    public ArrayList getDwgObjectOffsets() {
        return dwgObjectOffsets;
    }

    /**
     * @return Returns the dwgObjects.
     */
    public List getDwgObjects() {
        return dwgObjects;
    }

    /**
     * @return Returns the fileName.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return Returns the dwg3DFile.
     */
    public boolean isDwg3DFile() {
        return dwg3DFile;
    }

    /**
     * @param dwg3DFile The dwg3DFile to set.
     */
    public void setDwg3DFile(boolean dwg3DFile) {
        this.dwg3DFile = dwg3DFile;
    }
}
