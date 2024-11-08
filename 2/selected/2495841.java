package j3dworkbench.loader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;
import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.SceneGraphObject;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Color3f;
import org.j3d.geom.GeometryData;
import org.j3d.geom.terrain.ElevationGridGenerator;
import org.j3d.loaders.dem.DEMTypeARecord;
import org.j3d.loaders.dem.DEMTypeBRecord;
import org.j3d.loaders.dem.DEMTypeCRecord;
import org.j3d.renderer.java3d.loaders.BinaryLoader;
import org.j3d.renderer.java3d.loaders.HeightMapLoader;
import org.j3d.renderer.java3d.loaders.ManagedLoader;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.SceneBase;

/**
 * Loader for the USGS DEM file format.
 * <p>
 * 
 * The mesh produced is, by default, triangle strip arrays. The X axis
 * represents East-West and the Z-axis represents North-South. +X is east, -Z is
 * North. Texture coordinates are generated for the extents based on a single
 * 0-1 scale for the width of the object.
 * <p>
 * 
 * The loader produces a single mesh that represents the file's contents. No
 * further processing is performed in the current implementation to break the
 * points into smaller tiles or use multi-resolution terrain structures.
 * <p>
 * 
 * @author Justin Couch
 * @version $Revision: 1.3 $
 */
public class DEMTerrainLoaderFixed extends HeightMapLoader implements BinaryLoader, ManagedLoader {

    /** Current parser */
    private DEMTerrainParserFixed parser;

    /** Generator of the grid structure for the geometry */
    private ElevationGridGenerator generator;

    /** The map of the override capability bit settings */
    @SuppressWarnings("rawtypes")
    private Map overrideCapBitsMap;

    /** The map of the required capability bit settings */
    @SuppressWarnings("rawtypes")
    private Map requiredCapBitsMap;

    /** The map of the override capability bit settings */
    @SuppressWarnings("rawtypes")
    private Map overrideFreqBitsMap;

    /** The map of the required capability bit settings */
    @SuppressWarnings("rawtypes")
    private Map requiredFreqBitsMap;

    /** Flag for the API being new enough to have frequency bit setting */
    private final boolean haveFreqBitsAPI = true;

    /**
	 * Construct a new default loader with no flags set
	 */
    public DEMTerrainLoaderFixed() {
        this(0);
    }

    /**
	 * Construct a new loader with the given flags set.
	 * 
	 * @param flags
	 *            The list of flags to be set
	 */
    public DEMTerrainLoaderFixed(int flags) {
        super(flags);
    }

    /**
	 * Provide the set of mappings that override anything that the loader might
	 * set.
	 * <p>
	 * 
	 * If the key is set, but the value is null or zero length, then all
	 * capabilities on that node will be disabled. If the key is set the values
	 * override all settings that the loader may wish to normally make. This can
	 * be very dangerous if the loader is used for a file format that includes
	 * its own internal animation engine, so be very careful with this request.
	 * 
	 * @param capBits
	 *            The capability bits to be set
	 * @param freqBits
	 *            The frequency bits to be set
	 */
    @SuppressWarnings("rawtypes")
    public void setCapabilityOverrideMap(Map capBits, Map freqBits) {
        overrideCapBitsMap = capBits;
        overrideFreqBitsMap = freqBits;
    }

    /**
	 * Set the mapping of capability bits that the user would like to make sure
	 * is set. The end output is that the capabilities are the union of what the
	 * loader wants and what the user wants.
	 * <p>
	 * If the map contains a key, but the value is null or zero length, the
	 * request is ignored.
	 * 
	 * @param capBits
	 *            The capability bits to be set
	 * @param freqBits
	 *            The frequency bits to be set
	 */
    @SuppressWarnings("rawtypes")
    public void setCapabilityRequiredMap(Map capBits, Map freqBits) {
        requiredCapBitsMap = capBits;
        requiredFreqBitsMap = freqBits;
    }

    /**
	 * Load the scene from the given reader. Always throws an exception as the
	 * file format is binary only and readers don't handle this.
	 * 
	 * @param is
	 *            The source of input characters
	 * @return A description of the scene
	 * @throws IncorrectFormatException
	 *             The file is binary
	 */
    public Scene load(InputStream is) throws IncorrectFormatException, ParsingErrorException {
        return loadInternal(is);
    }

    /**
	 * Load the scene from the given reader. Always throws an exception as the
	 * file format is binary only and readers don't handle this.
	 * 
	 * @param reader
	 *            The source of input characters
	 * @return A description of the scene
	 * @throws IncorrectFormatException
	 *             The file is binary
	 */
    public Scene load(java.io.Reader reader) throws IncorrectFormatException, ParsingErrorException {
        return loadInternal(reader);
    }

    /**
	 * Load a scene from the given filename. The scene instance returned by this
	 * loader will have textures already loaded.
	 * 
	 * @param filename
	 *            The name of the file to load
	 * @return A description of the scene
	 * @throws FileNotFoundException
	 *             The reader can't find the file
	 * @throws IncorrectFormatException
	 *             The file is not one our loader understands
	 * @throws ParsingErrorException
	 *             An error parsing the file
	 */
    public Scene load(String filename) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        File file = new File(filename);
        InputStream input = null;
        if (!file.exists()) throw new FileNotFoundException("File does not exist");
        if (file.isDirectory()) throw new FileNotFoundException("File is a directory");
        FileInputStream fis = new FileInputStream(file);
        input = new BufferedInputStream(fis);
        return loadInternal(input);
    }

    /**
	 * Load a scene from the named URL. The scene instance returned by this
	 * loader will have textures already loaded.
	 * 
	 * @param url
	 *            The URL instance to load data from
	 * @return A description of the scene
	 * @throws FileNotFoundException
	 *             The reader can't find the file
	 * @throws IncorrectFormatException
	 *             The file is not one our loader understands
	 * @throws ParsingErrorException
	 *             An error parsing the file
	 */
    public Scene load(URL url) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
        InputStream input = null;
        try {
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            if (is instanceof BufferedInputStream) input = (BufferedInputStream) is; else input = new BufferedInputStream(is);
        } catch (IOException ioe) {
            throw new FileNotFoundException(ioe.getMessage());
        }
        return loadInternal(input);
    }

    /**
	 * Return the height map created for the last stream parsed. If no stream
	 * has been parsed yet, this will return null.
	 * 
	 * @return The array of heights in [row][column] order or null
	 */
    public float[][] getHeights() {
        return parser.getHeights();
    }

    /**
	 * Fetch information about the real-world stepping sizes that this grid
	 * uses.
	 * 
	 * @return The stepping information for width and depth
	 */
    public float[] getGridStep() {
        return parser.getGridStep();
    }

    /**
	 * Get the header used to describe the last stream parsed. If no stream has
	 * been parsed yet, this will return null.
	 * 
	 * @return The header for the last read stream or null
	 */
    public DEMTypeARecord getTypeARecord() {
        return parser.getTypeARecord();
    }

    /**
	 * Fetch all of the type B records that were registered in this file. Will
	 * probably contain more than one record and is always non-null. The records
	 * will be in the order they were read from the file.
	 * 
	 * @return The list of all the Type B records parsed
	 */
    public DEMTypeBRecord[] getTypeBRecords() {
        return parser.getTypeBRecords();
    }

    /**
	 * Get the type C record from the file. If none was provided, then this will
	 * return null.
	 * 
	 * @return The type C record info or null
	 */
    public DEMTypeCRecord getTypeCRecord() {
        return parser.getTypeCRecord();
    }

    /**
	 * Do all the parsing work for an inputstream. Convenience method for all to
	 * call internally
	 * 
	 * @param is
	 *            The inputsource for this reader
	 * @return The scene description
	 * @throws IncorrectFormatException
	 *             The file is not one our loader understands
	 * @throws ParsingErrorException
	 *             An error parsing the file
	 */
    private Scene loadInternal(InputStream str) throws IncorrectFormatException, ParsingErrorException {
        if (parser == null) parser = new DEMTerrainParserFixed(str); else parser.reset(str);
        return load();
    }

    /**
	 * Do all the parsing work for a reader. Convenience method for all to call
	 * internally
	 * 
	 * @param is
	 *            The inputsource for this reader
	 * @return The scene description
	 * @throws IncorrectFormatException
	 *             The file is not one our loader understands
	 * @throws ParsingErrorException
	 *             An error parsing the file
	 */
    private Scene loadInternal(Reader rdr) throws IncorrectFormatException, ParsingErrorException {
        if (parser == null) parser = new DEMTerrainParserFixed(rdr); else parser.reset(rdr);
        return load();
    }

    /**
	 * Do all the parsing work. Convenience method for all to call internally
	 * 
	 * @param is
	 *            The inputsource for this reader
	 * @return The scene description
	 * @throws IncorrectFormatException
	 *             The file is not one our loader understands
	 * @throws ParsingErrorException
	 *             An error parsing the file
	 */
    private Scene load() throws IncorrectFormatException, ParsingErrorException {
        float[][] heights = null;
        try {
            heights = parser.parse(true);
        } catch (IOException ioe) {
            throw new ParsingErrorException("Error parsing stream: " + ioe);
        }
        float depth = heights.length;
        float width = heights[0].length;
        if (generator == null) {
            generator = new ElevationGridGenerator(width, depth, (int) width, (int) depth, heights, 0, false);
        } else {
            generator.setDimensions(width, depth, heights[0].length, heights.length);
            generator.setTerrainDetail(heights, 0);
        }
        GeometryData data = new GeometryData();
        data.geometryType = GeometryData.TRIANGLE_STRIPS;
        data.geometryComponents = GeometryData.NORMAL_DATA | GeometryData.TEXTURE_2D_DATA;
        generator.generate(data);
        SceneBase scene = new SceneBase();
        BranchGroup root_group = new BranchGroup();
        setCapBits(root_group);
        setFreqBits(root_group);
        int format = GeometryArray.COORDINATES | GeometryArray.NORMALS | GeometryArray.TEXTURE_COORDINATE_2;
        TriangleStripArray geom = new TriangleStripArray(data.vertexCount, format, data.stripCounts);
        geom.setCoordinates(0, data.coordinates);
        geom.setNormals(0, data.normals);
        geom.setTextureCoordinates(0, 0, data.textureCoordinates);
        Appearance app = new Appearance();
        PolygonAttributes poly = new PolygonAttributes();
        poly.setPolygonMode(PolygonAttributes.POLYGON_LINE);
        ColoringAttributes coloratt = new ColoringAttributes();
        Color3f color = new Color3f(0f, 0f, 1f);
        coloratt.setColor(color);
        app.setPolygonAttributes(poly);
        app.setColoringAttributes(coloratt);
        Shape3D shape = new Shape3D(geom, app);
        root_group.addChild(shape);
        scene.setSceneGroup(root_group);
        return scene;
    }

    /**
	 * Set the frequency bits on this scene graph object according to the
	 * pre-set settings.
	 * 
	 * @param sgo
	 *            The j3d node to set the capabilities on
	 */
    private void setCapBits(SceneGraphObject sgo) {
        Class<? extends SceneGraphObject> cls = sgo.getClass();
        Map bits_map = Collections.EMPTY_MAP;
        if (overrideCapBitsMap != null) bits_map = overrideCapBitsMap; else if (requiredCapBitsMap != null) bits_map = requiredCapBitsMap;
        int[] bits = (int[]) bits_map.get(cls);
        int size = (bits == null) ? 0 : bits.length;
        for (int i = 0; i < size; i++) sgo.setCapability(bits[i]);
    }

    /**
	 * Set the frequency bits on this scene graph object according to the
	 * pre-set settings. If the API version is < 1.3 then this method returns
	 * immediately.
	 * 
	 * @param sgo
	 *            The j3d node to set the capabilities on
	 */
    private void setFreqBits(SceneGraphObject sgo) {
        if (!haveFreqBitsAPI) return;
        Class cls = sgo.getClass();
        Map bits_map = Collections.EMPTY_MAP;
        if (overrideFreqBitsMap != null) bits_map = overrideFreqBitsMap; else if (requiredFreqBitsMap != null) bits_map = requiredFreqBitsMap;
        int[] bits = (int[]) bits_map.get(cls);
        int size = (bits == null) ? 0 : bits.length;
        for (int i = 0; i < size; i++) sgo.setCapabilityIsFrequent(bits[i]);
    }
}
