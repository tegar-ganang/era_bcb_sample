package rez;

import java.io.*;
import java.util.*;
import java.lang.Math.*;
import java.lang.reflect.*;
import rez.RezTileFW.RootTile;
import rez.utils.Debug;
import rez.utils.FilePath;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities.*;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import com.sun.media.jai.codec.FileSeekableStream;

/**
 * <dl>
 * <p><dt><b>Summary :</b></dt></p>
 * <dd>
 * Rez is a terrain file translator framework.  Output can be pretty much anything as it is
 * determined by the output plugins used.  However most of the supplied code supports output to
 * a single tile, an array of tiles or a multiresolution tree of tiles as well as images.
 * Rez can sample the input and produce output files at a resolution and size controlled by you.
 * Some of the main goals of the framework are to be able
 * to handle large terrain datasets, build models unrestricted in size that can be displayed with
 * acceptable performance over the web, and to provide a structure that is highly flexible and for
 * which implementers can easily build plugins to process different formats.
 * </dd>
 * <p><dt><b>Description :</b></dt></p>
 * <dd>
 *
 * Rez is designed as a multi-adaptor (my term) with a central program (Rez.java) which
 * allows the input parser to be plugged in and the output generator to be plugged in.
 *
 * There is a GUI interface plugin facility so you can plug in a GUI and an interface for a scene generator.  The scene
 * generator is for creating the main world in which the generated models are displayed plus
 * viewpoints, tours etc.
 *
 * See RezIndex.html with this distribution, sourceforge.net planet-earth project or or
 * www.surak.com.au/~chris/vrml/RezIndex.html for details and
 * the examples of a Rez generated models and the files used to generae it.
 *
 * There are example input parser plugins for formats like DTED, VRML, ArvView asci and many others docuemnted
 * online and in the RezIndex.html contained with this distribution.
 *
 * There are example output format plugins for VRML, X3D, image and many other formats also documented in
 * RezIndex.html.
 *
 * The example parsers and Tiler plugins provided use gtopo30 ".dem" files as intermediate height files.  The parsers
 * produce the height grids in this format and the tilers read it.  It is possible to have
 * your own parser and tiler that use their own format.</P>
 *
 * A "tiles" directory is created to store the start grid and
 * subgrids for each level.  Each level is stored in its own
 * separate directory.</P>
 *
 * If sampling is turned on grids are sampled at lower detail levels to reduce file size.
 * Grids (tiles) are also gzipped if that option is switched on.
 *
 * Usually a "Display" file is craeted in another directory at the same level as the tiles directory.
 * This file contains the mail world nodes and inlines the generated terrain and models.</P>
 *
 * Input:</BR>
 * 1. Usually an elevation grid or multiple grids.</BR>
 * 2. A configuration file.  This file contains paths to plugin classes and other information.</P>
 *
 * See the web page for details on parameters.
 *
 * Output:</BR>
 * Output will depend on the output plugin.
 *
 * Rez will recognize that some output may consist of a binary or quadtree of tiles and others consisting of two
 * trees: one for the elevation tiles and one for the "tree" nodes which simply link the tiles into a mutlirez
 * hierarchy.
 *
 * -----------------------</P>
 * Known Bugs:</BR>
 * Does not like the height values before the grid parameters in ElevationGrids.</BR>
 * ----------------------</P>
 * </dd>
 * </dl>
 * <p>
 * @author Chris Thorne
 * @version 1.0
 * <p>
 */
public class Rez extends Thread {

    private static final boolean staticDebugOn = Boolean.getBoolean("debugOn");

    private static final int noGood = -999999;

    private static final int SCENE = 0;

    private static final int SPLITTING = 1;

    private static final int COMBINING = 2;

    private static final int WHOLE_SPLIT = 3;

    private static final int INDIVIDUAL = 4;

    private static final int SLICE_SPLIT = 5;

    private static final int CUT = 6;

    private boolean bsp = false;

    private boolean doesBtree = false;

    private boolean onlyBtree = false;

    private boolean noTrees = true;

    private Method treeMethod;

    private RootTile rootTile;

    private double xbboxSize = 1d;

    private double zbboxSize = 1d;

    private double originalXbboxSize = 1d;

    private double originalZbboxSize = 1d;

    private int firstTreeLevel = 0;

    private int finalTreeLevel = 4;

    private FileWriter debugFile;

    private int xDimension = -1;

    private int zDimension = -1;

    private double xSpacing;

    private double zSpacing;

    private String yScale;

    private String geoGridOrigin;

    private boolean outputNorthToSouthRows = false;

    private boolean sourceDataNorthToSouthRows = false;

    private boolean northToSouthRowsFound = false;

    private boolean sourceDataNorthToSouthRowsFound = false;

    private boolean sourceTileInDegrees = true;

    private boolean convertToMeters = false;

    private boolean outputInMeters = true;

    private Bounds sliceBounds;

    private boolean focusListed = false;

    private boolean sliceBoundsListed = false;

    private String tmpString;

    private String dirName;

    private String sourceDirectory = "";

    private String destinationDirectory = "";

    private Class Tile;

    private Class Tree;

    private Class Parser;

    private double gridx, gridz;

    private double height = 0;

    private StringTokenizer tokenizer;

    private String line;

    private BufferedReader d;

    private InputParameters parameters;

    private Scene thisScene = null;

    private GUI thisGUI;

    private String srcdirprename = sourceDirectory;

    private String dirprename = "";

    private double initialZbboxSize = 0;

    private GeoImage[] detailedImageFiles = null;

    private GeoObject[] objectList = null;

    private GeoObject[] slideList = null;

    private GridTile[] detailedTopoFiles = null;

    private double[] foci = null;

    private Double[] fociD = null;

    private int srcTileNumber;

    private RezTileFW multirezTile = null;

    private boolean maxDecimalPrecisionFound = false;

    private boolean adjustedOffsets = false;

    private int maxDecimalPrecision = 0;

    private boolean invertData = false;

    private String infoOnData = "";

    boolean tilerFound = false;

    private TileArray[] sourceTileArrays;

    private double[] NEGridPosition = null;

    private String PreferredOutput = "";

    private String setRadius = "";

    private String setSolid = "";

    private String setColourPerVertex = "";

    private ElevationParser thisParser;

    private String sceneGenerator = "";

    private String guiClass = "";

    private String parserClass;

    private String tileClass;

    private String lineSep = System.getProperty("line.separator");

    private int numInputTiles = 0;

    String parsedFilePath;

    int methodIndex = 0;

    public Rez(String configFileStr, String firstTreeLevelStr, String finalTreeLevelStr, String rangeScaleString, String zipFlag, String debugFlag, String samplingFlag, int samplingIncrease, String scaleStr, String heightScaleStr, String minOutputTileDimStr, String maxOutputTileDimStr, String transOffsetX, String transOffsetZ, String BSPStr, String splitStr, String initialLatDivisions, String initialLonDivisions) {
        boolean zipFile = false;
        boolean sampling = false;
        int operationType = SPLITTING;
        int minOutputTileDim;
        int maxOutputTileDim;
        double rangeScale = 1d;
        String titleText = "# This file was produced by Rez ";
        int fociIndex = 0;
        try {
            sourceTileArrays = new TileArray[1];
            geoGridOrigin = "geoGridOrigin \"-90 -180 0\"";
            String solid = "solid FALSE";
            String[] fileList = new String[1];
            World world = new World();
            if (staticDebugOn) {
                debugFile = new FileWriter("Rezdebug.txt");
                System.out.println("static debug turned on");
            }
            System.out.println(lineSep + "Rez: Started" + lineSep);
            if (zipFlag.equals("y")) zipFile = true;
            if (samplingFlag.equals("y")) sampling = true;
            if (BSPStr.equals("y")) bsp = true;
            System.out.println("Rez: bsp" + bsp + lineSep);
            if (splitStr.equals("0")) operationType = SCENE;
            if (splitStr.equals("y") || splitStr.equals("1")) operationType = SPLITTING; else if (splitStr.equals("n") || splitStr.equals("2")) operationType = COMBINING; else if (splitStr.equals("3")) operationType = WHOLE_SPLIT; else if (splitStr.equals("4")) operationType = INDIVIDUAL; else if (splitStr.equals("5")) operationType = SLICE_SPLIT; else if (splitStr.equals("6")) operationType = CUT; else {
                System.out.println("Invalid operation type ... exitting");
                System.exit(-1);
            }
            firstTreeLevel = Integer.parseInt(firstTreeLevelStr);
            finalTreeLevel = Integer.parseInt(finalTreeLevelStr);
            world.minTileDim = Integer.parseInt(minOutputTileDimStr);
            world.maxTileDim = Integer.parseInt(maxOutputTileDimStr);
            world.initialLatDivisions = Integer.parseInt(initialLatDivisions);
            world.initialLonDivisions = Integer.parseInt(initialLonDivisions);
            world.xTranslationOffset = Double.parseDouble(transOffsetX);
            world.zTranslationOffset = Double.parseDouble(transOffsetZ);
            World.xzScale = Double.parseDouble(scaleStr);
            rangeScale = Double.parseDouble(rangeScaleString);
            World.setHeightScale(Float.parseFloat(heightScaleStr));
            world.setDetailScale(rangeScale);
            if (firstTreeLevel < 0) {
                firstTreeLevel = 0;
            }
            if (finalTreeLevel < 0) {
                finalTreeLevel = 0;
            }
            titleText = titleText + "using the run-time parameters:" + lineSep + "# java -DdebugOn=false -cp -Xmx1024m -DdebugOn=false -cp ../dist/rez.jar rez.Rez configfile.txt firstTreeLevel finalTreeLevel rangeScale heightScale(y)" + " gzipFlag sampling  samplingIncrease mapScale(x-z) heightScale(y) minOutputTiledimension maxOutputTileDimension, transOffsetX transOffsetX BSPFlag(y)" + lineSep + "# java -classpath .;c:/java/classes/Rez.jar Rez " + configFileStr + " " + firstTreeLevelStr + " " + finalTreeLevelStr + " " + rangeScaleString + " " + zipFlag + " " + samplingFlag + " " + +samplingIncrease + " " + scaleStr + " " + heightScaleStr + minOutputTileDimStr + " " + maxOutputTileDimStr + " " + transOffsetX + " " + transOffsetZ + " " + BSPStr + lineSep + lineSep;
            String tmpFile = configFileStr;
            debug("XtranslationOffset" + world.xTranslationOffset);
            debug("ZtranslationOffset" + world.zTranslationOffset);
            d = new BufferedReader(new FileReader(tmpFile));
            boolean sourceTileArraysFound = false;
            boolean treeGeneratorFound = false;
            int InitialParams = 5;
            int numParams = 0;
            String tmpStr = "";
            SampledTileRow tileRow;
            Hashtable<String, Object> inputProperties = new Hashtable<String, Object>();
            ConfigFile configFile = new ConfigFile(tmpFile, world, d);
            try {
                sourceTileArraysFound = configFile.getInitialConfigParams(world, sourceTileArrays, inputProperties);
            } catch (NullPointerException e) {
                System.out.println("Rez: error reading config parameters. " + e);
            }
            sourceDirectory = (String) inputProperties.get("sourceDir");
            destinationDirectory = (String) inputProperties.get("destinationDir");
            sceneGenerator = (String) inputProperties.get("Scene");
            guiClass = (String) inputProperties.get("GUI");
            System.out.println("sourceDir: " + sourceDirectory);
            System.out.println("destinationDirectory: " + destinationDirectory);
            System.out.println("sceneGenerator: " + sceneGenerator);
            System.out.println("guiClass: " + guiClass);
            if (!sourceTileArraysFound) world.numTileArrays = 1;
            parameters = new InputParameters();
            boolean fileInputDone = false;
            boolean finished = false;
            int numTileArraysProcessed = 0;
            mainLoop: while (!parameters.quit() && !finished) {
                for (int arrayCount = 0; arrayCount < world.numTileArrays; arrayCount++) {
                    System.out.println("world.numTileArrays " + world.numTileArrays + " fileInputDone " + fileInputDone);
                    debug("world.numTileArrays " + world.numTileArrays + " fileInputDone " + fileInputDone);
                    fileInputDone = configFile.getInputTAProperties(sourceTileArrays, arrayCount, fileInputDone, inputProperties, NEGridPosition);
                    if (inputProperties.get("maxDecimalPrecision") != null) maxDecimalPrecision = (Integer) (inputProperties.get("maxDecimalPrecision"));
                    System.out.println("maxDecimalPrecision: " + maxDecimalPrecision);
                    maxDecimalPrecisionFound = (Boolean) inputProperties.get("maxDecimalPrecisionFound");
                    if (!maxDecimalPrecisionFound) maxDecimalPrecision = 0;
                    setRadius = (String) (inputProperties.get("setRadius"));
                    System.out.println("setRadius: " + setRadius);
                    PreferredOutput = (String) (inputProperties.get("PreferredOutput"));
                    setSolid = (String) (inputProperties.get("setSolid"));
                    System.out.println("setSolid: " + setSolid);
                    setColourPerVertex = (String) (inputProperties.get("setColourPerVertex"));
                    System.out.println("setColourPerVertex: " + setColourPerVertex);
                    if (inputProperties.get("parserClass") != null) parserClass = (String) inputProperties.get("parserClass");
                    System.out.println("parserClass: " + parserClass);
                    if (inputProperties.get("tileClass") != null) tileClass = (String) inputProperties.get("tileClass");
                    System.out.println("tileClass: " + tileClass);
                    if (inputProperties.get("GUI") != null) guiClass = (String) inputProperties.get("GUI");
                    System.out.println("guiClass: " + guiClass);
                    if (inputProperties.get("invertData") != null) invertData = (Boolean) inputProperties.get("invertData");
                    System.out.println("==== Rez: invertData: " + invertData);
                    if (inputProperties.get("outputNorthToSouthRows") != null) outputNorthToSouthRows = (Boolean) inputProperties.get("outputNorthToSouthRows");
                    if (inputProperties.get("sourceDataNorthToSouthRows") != null) sourceDataNorthToSouthRows = (Boolean) inputProperties.get("sourceDataNorthToSouthRows");
                    System.out.println("=== sourceDataNorthToSouthRows: " + sourceDataNorthToSouthRows);
                    if (inputProperties.get("tilerFound") != null) tilerFound = (Boolean) inputProperties.get("tilerFound");
                    if (!tilerFound) {
                        tileClass = "plugins.standard.VRMLElevationGridTile";
                    }
                    int sourceTileArrayRows = sourceTileArrays[arrayCount].numberRows;
                    int sourceTileArrayCols = sourceTileArrays[arrayCount].rowLength;
                    fileList = new String[sourceTileArrayRows * sourceTileArrayCols];
                    sourceTileArrays[arrayCount].tiles = new GridTile[sourceTileArrayRows * sourceTileArrayCols];
                    if (guiClass != null) {
                        if (!fileInputDone) {
                            parameters.setParameters(world, firstTreeLevel, finalTreeLevel, samplingIncrease, rangeScale, zipFile, sampling, bsp, sourceDirectory, destinationDirectory);
                            thisGUI = (GUI) Class.forName(guiClass).newInstance();
                            this.run();
                        }
                        boolean run = false;
                        try {
                            while (!run) {
                                sleep(100);
                                run = parameters.startRun();
                                if (parameters.quit()) continue mainLoop;
                            }
                        } catch (InterruptedException e) {
                        }
                        firstTreeLevel = parameters.getFirstTreeLevel();
                        finalTreeLevel = parameters.getFinalTreeLevel();
                        samplingIncrease = parameters.getSamplingIncrement();
                        rangeScale = parameters.getDetailScale();
                        zipFile = parameters.getGzipFlag();
                        sampling = parameters.getSamplingFlag();
                        bsp = parameters.getTreeType();
                        System.out.println("Rez: bsp after getTreeType" + bsp);
                        parameters.getWorldParams(world);
                        sourceDirectory = parameters.getSourceDirectory();
                        destinationDirectory = parameters.getDestinationDirectory();
                        if (setRadius != null) {
                            if (setRadius.length() > 0) {
                                World.setRadius(Double.parseDouble(setRadius));
                            }
                        }
                        if (firstTreeLevel < 0) {
                            firstTreeLevel = 0;
                        }
                        if (finalTreeLevel < 0) {
                            finalTreeLevel = 0;
                        }
                    }
                    debug("Rez: creating parserClass: " + " class:" + parserClass + lineSep);
                    thisParser = (ElevationParser) Class.forName(parserClass).newInstance();
                    if (!fileInputDone) {
                        configFile.parseObjects(sourceTileArrays, fileList, arrayCount, sourceTileArrayRows);
                    }
                    objectList = configFile.getObjectList();
                    slideList = configFile.slideList();
                    detailedImageFiles = configFile.detailedImageFiles();
                    detailedTopoFiles = configFile.detailedTopoFiles();
                    foci = configFile.foci();
                    fociD = configFile.fociD();
                    numInputTiles = configFile.getNumberTiles();
                    focusListed = configFile.getfocusListed();
                    sliceBoundsListed = configFile.getSliceBoundsListed();
                    if (sliceBoundsListed) sliceBounds = configFile.getSliceBounds();
                    srcTileNumber = 0;
                    String elevationFile = "";
                    for (int srcTileArrayRowCount = 0; srcTileArrayRowCount < sourceTileArrayRows; srcTileArrayRowCount++) {
                        for (int i = 0; i < numInputTiles; i++) {
                            if (numInputTiles > 0) elevationFile = fileList[srcTileNumber];
                            if (staticDebugOn) System.out.println("file " + elevationFile);
                            System.out.println(" elevation file " + elevationFile);
                            FilePath fp = new FilePath(elevationFile);
                            String prename = fp.getFileName();
                            srcdirprename = fp.getSrcDirPrename();
                            dirprename = fp.getDirPrename();
                            debug("Rez: elevationFile is: " + elevationFile);
                            debug("Rez: prename is: >" + prename + "<");
                            debug("Rez: dirprename is: >" + dirprename + "<");
                            debug("Rez: sourceDirectory is: >" + sourceDirectory + "<");
                            debug("Rez: srcdirprename is: >" + srcdirprename + "<");
                            System.out.println("Rez: elevationFile is: " + elevationFile);
                            System.out.println("Rez: sourceDirectory is: >" + sourceDirectory + "<");
                            Object dummy = new Object();
                            thisParser.parseElevation(sourceDirectory + elevationFile, dummy, World.getHeightScale(), infoOnData, invertData, true, sourceDataNorthToSouthRows);
                            debug("entering tile assignment");
                            if (thisParser.reqsMet) {
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].xDimension = thisParser.xDimension;
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].zDimension = thisParser.zDimension;
                                debug("xDim: " + thisParser.xDimension + ", zDim " + thisParser.zDimension);
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setxSpacing(thisParser.xSpacing);
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setzSpacing(thisParser.zSpacing);
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].yScale = thisParser.yScale;
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].creaseAngle = thisParser.creaseAngle;
                                initialZbboxSize = (thisParser.zDimension - 1) * thisParser.zSpacing;
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setLon(thisParser.gridx);
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setLat(thisParser.gridz);
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setTopLeftLon(thisParser.gridx);
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setTopLeftLat(thisParser.gridz);
                                System.out.println("Rez: lon/gridx: " + thisParser.gridx);
                                gridx = thisParser.gridx;
                                gridz = thisParser.gridz;
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].units = thisParser.units;
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setByteOrder(thisParser.getByteOrder());
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setNbits(thisParser.getNbits());
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setPath(thisParser.getOutputFilePath());
                                sourceTileArrays[arrayCount].tiles[srcTileNumber].setRealValues(thisParser.getRealValues());
                                if (staticDebugOn) debug("just after parse, XSpacing is: " + thisParser.xSpacing);
                                prename = "";
                                if (!thisParser.fileName.equals("")) {
                                    prename = thisParser.fileName;
                                    elevationFile = dirprename + XPlat.fileSep + prename;
                                }
                                if (staticDebugOn) {
                                    debug("Rez: elevationFile is: " + elevationFile);
                                    debug("Rez: prename is: >" + prename + "<");
                                    debug("Rez: dirprename is: >" + dirprename + "<");
                                    debug("Rez: sourceDirectory is: >" + sourceDirectory + "<");
                                    debug("Rez: srcdirprename is: >" + srcdirprename + "<");
                                }
                                String parserFilePath = sourceDirectory + elevationFile;
                                if (thisParser.getOutputFilePath() != null) {
                                    if (thisParser.getOutputFilePath().equals("")) {
                                        parserFilePath = thisParser.getOutputFilePath();
                                    }
                                } else {
                                    System.out.println("Error: output file (path) from parsing null");
                                    System.out.println("setting output file to parserFilePath");
                                    sourceTileArrays[arrayCount].tiles[srcTileNumber].path = parserFilePath;
                                }
                            } else {
                                System.out.println("Error: parser requirements not met");
                            }
                            srcTileNumber++;
                        }
                    }
                    if (sceneGenerator != null) {
                        thisScene = (Scene) Class.forName(sceneGenerator).newInstance();
                    }
                    srcTileNumber = 0;
                    String units = (outputInMeters) ? "meters" : "degrees";
                    switch(operationType) {
                        case SCENE:
                            {
                            }
                        case SPLITTING:
                        case SLICE_SPLIT:
                            {
                                for (int srcTileArrayRowCount = 0; srcTileArrayRowCount < sourceTileArrayRows; srcTileArrayRowCount++) {
                                    elevationFile = "";
                                    for (int srcTileArrayCol = 0; srcTileArrayCol < numInputTiles; srcTileArrayCol++) {
                                        if (numInputTiles > 0) elevationFile = fileList[srcTileNumber];
                                        FilePath fp = new FilePath(elevationFile);
                                        String prename = fp.getFileName();
                                        Object dummy = new Object();
                                        thisParser.parseElevation(sourceDirectory + elevationFile, dummy, World.getHeightScale(), infoOnData, invertData, false, sourceDataNorthToSouthRows);
                                        if (thisParser.reqsMet) {
                                            multirezTile = createOutputTiler(sourceTileArrays, arrayCount, world);
                                            if (multirezTile == null) {
                                                System.out.println("*** incorrect output tiler name specified? ");
                                                System.exit(-1);
                                            }
                                            System.out.println("=== split: initialisefor processing ");
                                            initialiseForprocessing(sourceTileArrays, arrayCount, world);
                                            performPreProcessing(detailedImageFiles, objectList, sliceBounds, slideList, detailedTopoFiles, sliceBoundsListed, world);
                                            if (bsp) {
                                                bsp = doesBtree;
                                            }
                                            System.out.println("Rez: bsp afterdoesBtree" + bsp);
                                            if (onlyBtree) {
                                                bsp = true;
                                            } else {
                                                if (bsp) {
                                                    System.out.println("User has asked for BSP tree");
                                                    if (!doesBtree) {
                                                        bsp = false;
                                                        System.out.println("Tiler can't do btrees - turning off bsp");
                                                    }
                                                }
                                            }
                                            System.out.println("Rez: bsp onlyBtree" + bsp);
                                            if (foci != null) {
                                                multirezTile.setFoci(foci);
                                            }
                                            if (outputInMeters) {
                                                multirezTile.setUnits("meters");
                                            } else {
                                                multirezTile.setUnits("degrees");
                                            }
                                            if (setSolid != null) {
                                                if (setSolid.length() > 0) {
                                                    multirezTile.setSolid(Boolean.parseBoolean(setSolid));
                                                }
                                            }
                                            if (setColourPerVertex != null) {
                                                if (setColourPerVertex.length() > 0) {
                                                    multirezTile.setColourPerVertex(Boolean.parseBoolean(setColourPerVertex));
                                                }
                                            }
                                            multirezTile.SetValues(world, sourceTileArrays[arrayCount], srcTileArrayRowCount, srcTileArrayCol, prename, srcdirprename, parsedFilePath, destinationDirectory, zipFile, rangeScale, sampling, samplingIncrease, maxDecimalPrecision, outputNorthToSouthRows, sourceDataNorthToSouthRows, bsp, firstTreeLevel, finalTreeLevel);
                                            System.out.println("Rez: xSpacing" + multirezTile.getxSpacing());
                                            if (staticDebugOn) debug("generating tiles");
                                            for (int level = firstTreeLevel; level <= finalTreeLevel; level++) {
                                                System.out.println("Rez Split: start of level " + level);
                                                int numDivisions = 1 << level;
                                                tileRow = new SampledTileRow();
                                                rootTile = multirezTile.buildTiles(world, sourceTileArrays[arrayCount], srcTileArrayRowCount, srcTileArrayCol, level, firstTreeLevel, finalTreeLevel, tileRow, operationType);
                                                if (!noTrees) {
                                                    if (staticDebugOn) debug("calling gentrees ");
                                                    rootTile = genTrees(world, level, firstTreeLevel, finalTreeLevel, dirprename, prename, destinationDirectory, zipFile, rangeScale, World.getxzScale(), tileRow, height, titleText, sourceTileArrays[arrayCount].tiles[srcTileNumber], multirezTile, methodIndex, operationType);
                                                }
                                                if (level == 0) {
                                                    xbboxSize = ((double) (xDimension - 1)) * xSpacing;
                                                    zbboxSize = ((double) (zDimension - 1)) * zSpacing;
                                                    if (staticDebugOn) debug(": rangeScale " + rangeScale);
                                                    if (staticDebugOn) debug(": xSpacing " + xSpacing + " Columns: " + (tileRow.xcolumns[0] - 1) + " horizontalScale " + World.getxzScale());
                                                    System.out.println("Rez: xbboxSize " + xbboxSize);
                                                    System.out.println("Rez: xSpacing " + xSpacing);
                                                    if (thisScene != null) {
                                                        System.out.println("creating main scene");
                                                        if (objectList == null) {
                                                            System.out.println("objectList ==null");
                                                        }
                                                        thisScene.createMainScene(world, objectList, multirezTile, xbboxSize * World.getxzScale(), zbboxSize * World.getxzScale(), dirprename, prename, destinationDirectory, zipFile, rangeScale, outputNorthToSouthRows, rootTile, noTrees);
                                                        if (objectList != null) {
                                                            thisScene.addObjects(objectList, world);
                                                        } else {
                                                            System.out.println("objectList ==null");
                                                        }
                                                        if (slideList != null) {
                                                            thisScene.addSlides(slideList, world);
                                                        }
                                                        thisScene.closeObjects();
                                                        thisScene.addPrecisionVpRoutes(objectList, world);
                                                        System.out.println("closing main scene file");
                                                        thisScene.close();
                                                    }
                                                }
                                            }
                                        } else {
                                            if (staticDebugOn) System.out.println("Parser Requirements not Met" + lineSep);
                                        }
                                        srcTileNumber++;
                                    }
                                    if (staticDebugOn) System.out.println("end loop for each row");
                                }
                            }
                            break;
                        case COMBINING:
                        case WHOLE_SPLIT:
                            {
                                if ((tileClass.indexOf("VRMLElevationGrid") != -1) || (tileClass.indexOf("VRMLCombineSplitTiler") != -1) || (tileClass.indexOf("geosurface") != -1)) {
                                    noTrees = false;
                                }
                                System.out.println("Rez: Combine-split: tilerClass: " + tileClass);
                                System.out.println("Rez: noTrees " + noTrees);
                                Class<?> multirezTileClass = Class.forName(tileClass);
                                multirezTile = (RezTileFW) (multirezTileClass.newInstance());
                                methodIndex = reflectOn(multirezTile, "buildTree");
                                doesBtree = multirezTile.bTree;
                                onlyBtree = multirezTile.justBinary;
                                if ((PreferredOutput.equals("degrees")) && (multirezTile.outputAny)) {
                                    outputInMeters = false;
                                    multirezTile.setOutputInMeters(outputInMeters);
                                } else if ((PreferredOutput.equals("meters")) && (multirezTile.outputAny)) {
                                    outputInMeters = true;
                                    multirezTile.setOutputInMeters(outputInMeters);
                                } else {
                                    outputInMeters = multirezTile.outputInMeters;
                                }
                                System.out.println("Rez: outputInMeters " + outputInMeters);
                                System.out.println("Rez: inputInDegrees " + thisParser.units.equals("degrees"));
                                world.outputInMeters = outputInMeters;
                                srcTileNumber = 0;
                                for (int srcTileArrayRowCount = 0; srcTileArrayRowCount < sourceTileArrayRows; srcTileArrayRowCount++) {
                                    elevationFile = "";
                                    for (int srcTileArrayCol = 0; srcTileArrayCol < numInputTiles; srcTileArrayCol++) {
                                        if (numInputTiles > 0) elevationFile = fileList[srcTileNumber];
                                        FilePath fp = new FilePath(elevationFile);
                                        String prename = fp.getFileName();
                                        Object dummy = new Object();
                                        thisParser.parseElevation(sourceDirectory + elevationFile, dummy, World.getHeightScale(), infoOnData, invertData, false, sourceDataNorthToSouthRows);
                                        if (thisParser.geoSystem != null) {
                                            world.geoSystem = thisParser.geoSystem;
                                        } else {
                                            System.out.println("geosystem null ");
                                        }
                                        sourceTileInDegrees = thisParser.units.equals("degrees");
                                        if (thisParser.reqsMet) {
                                            if (staticDebugOn) debug("before adjust: xTranslationOffset:" + world.xTranslationOffset + " ZtranslationOffset " + world.zTranslationOffset);
                                            System.out.println("before adjust: XtranslationOffset:" + world.xTranslationOffset + " ZtranslationOffset " + world.zTranslationOffset);
                                            System.out.println("before adjust: gridx:" + thisParser.gridx + " gridz " + thisParser.gridz);
                                            xDimension = sourceTileArrays[arrayCount].tiles[srcTileNumber].xDimension;
                                            zDimension = sourceTileArrays[arrayCount].tiles[srcTileNumber].zDimension;
                                            xSpacing = sourceTileArrays[arrayCount].tiles[srcTileNumber].getxSpacing();
                                            zSpacing = sourceTileArrays[arrayCount].tiles[srcTileNumber].getzSpacing();
                                            if (!adjustedOffsets) {
                                                adjustOffsetsAndSpacing(world, sourceTileArrays, arrayCount);
                                                adjustedOffsets = true;
                                            } else {
                                                System.out.println("already adjustedOffsets:");
                                            }
                                            sourceTileArrays[arrayCount].tiles[srcTileNumber].gridx = thisParser.gridx * World.xzScale;
                                            sourceTileArrays[arrayCount].tiles[srcTileNumber].gridz = thisParser.gridz * World.xzScale;
                                            sourceTileArrays[arrayCount].tiles[srcTileNumber].setxSpacing(xSpacing);
                                            sourceTileArrays[arrayCount].tiles[srcTileNumber].setzSpacing(zSpacing);
                                        } else {
                                            System.out.println("Rez: Error: Parser Requirements not Met" + lineSep);
                                        }
                                        srcTileNumber++;
                                    }
                                    if (staticDebugOn) System.out.println("end loop for each row");
                                }
                                if (thisParser.geoSystem != null) {
                                    world.geoSystem = thisParser.geoSystem;
                                } else {
                                    System.out.println("geosystem null ");
                                }
                                srcTileNumber = 0;
                                elevationFile = "";
                                if (numInputTiles > 0) elevationFile = fileList[srcTileNumber];
                                FilePath fp = new FilePath(elevationFile);
                                String prename = fp.getFileName();
                                parsedFilePath = sourceTileArrays[arrayCount].tiles[srcTileNumber].path;
                                System.out.println("Generating tiles");
                                if (staticDebugOn) debug("Rez: creating tilerClass: " + " class:" + tileClass + lineSep);
                                xbboxSize = ((double) (xDimension - 1)) * xSpacing;
                                zbboxSize = ((double) (zDimension - 1)) * zSpacing;
                                gridx = sourceTileArrays[arrayCount].tiles[srcTileNumber].gridx;
                                gridz = sourceTileArrays[arrayCount].tiles[srcTileNumber].gridz;
                                if (setSolid.length() > 0) {
                                    multirezTile.setSolid(Boolean.parseBoolean(setSolid));
                                }
                                multirezTile.setPosition(gridx * World.xzScale, 0, gridz * World.xzScale, (gridx + xbboxSize) * World.xzScale, (gridz + zbboxSize) * World.xzScale);
                                multirezTile.setxSpacing(xSpacing);
                                multirezTile.setzSpacing(zSpacing);
                                multirezTile.setInitialRowDivisions(world.initialLatDivisions);
                                multirezTile.setInitialColDivisions(world.initialLonDivisions);
                                System.out.println("set init divisions" + initialLatDivisions);
                                System.out.println("b4 preprocess");
                                performPreProcessing(detailedImageFiles, objectList, sliceBounds, slideList, detailedTopoFiles, sliceBoundsListed, world);
                                if (foci != null) {
                                    multirezTile.setFoci(foci);
                                }
                                multirezTile.SetValues(world, sourceTileArrays[arrayCount], 0, 0, prename, srcdirprename, parsedFilePath, destinationDirectory, zipFile, rangeScale, sampling, samplingIncrease, maxDecimalPrecision, outputNorthToSouthRows, sourceDataNorthToSouthRows, bsp, firstTreeLevel, finalTreeLevel);
                                if (staticDebugOn) debug("generating tiles");
                                tileRow = new SampledTileRow();
                                if (operationType == COMBINING) {
                                    int numDivisions = 1;
                                    rootTile = multirezTile.buildTiles(world, sourceTileArrays[arrayCount], 0, 0, 0, firstTreeLevel, finalTreeLevel, tileRow, operationType);
                                    if (!noTrees) {
                                        if (staticDebugOn) debug("calling gentrees ");
                                        rootTile = genTrees(world, 0, firstTreeLevel, finalTreeLevel, dirprename, prename, destinationDirectory, zipFile, rangeScale, World.getxzScale(), tileRow, height, titleText, sourceTileArrays[arrayCount].tiles[srcTileNumber], multirezTile, methodIndex, operationType);
                                    }
                                    xbboxSize = ((double) (xDimension - 1)) * xSpacing;
                                    zbboxSize = ((double) (zDimension - 1)) * zSpacing;
                                    if (staticDebugOn) debug("Rez: rangeScale " + rangeScale);
                                    if (staticDebugOn) debug("Rez: xSpacing " + xSpacing + " Columns: " + (tileRow.xcolumns[0] - 1) + " horizontalScale " + World.getxzScale());
                                } else {
                                    for (int localLevel = firstTreeLevel; localLevel <= finalTreeLevel; localLevel++) {
                                        System.out.println("localLevel: " + localLevel);
                                        rootTile = multirezTile.buildTiles(world, sourceTileArrays[arrayCount], 0, 0, localLevel, firstTreeLevel, finalTreeLevel, tileRow, operationType);
                                        System.out.println("Level #1: " + localLevel + " firstTreeLevel" + firstTreeLevel);
                                        System.out.println("Rez: whole-split: b4 call noTrees " + noTrees);
                                        if (!noTrees) {
                                            if (staticDebugOn) debug("calling gentrees ");
                                            rootTile = genTrees(world, localLevel, firstTreeLevel, finalTreeLevel, dirprename, prename, destinationDirectory, zipFile, rangeScale, World.getxzScale(), tileRow, height, titleText, sourceTileArrays[arrayCount].tiles[srcTileNumber], multirezTile, methodIndex, operationType);
                                        }
                                        System.out.println("Level #2: " + localLevel + " firstTreeLevel" + firstTreeLevel);
                                        if (localLevel == 0) {
                                            xbboxSize = ((double) (xDimension - 1)) * xSpacing;
                                            zbboxSize = ((double) (zDimension - 1)) * zSpacing;
                                            if (staticDebugOn) debug(": rangeScale " + rangeScale);
                                            if (staticDebugOn) debug(": xSpacing " + xSpacing + " Columns: " + (tileRow.xcolumns[0] - 1) + " horizontalScale " + World.getxzScale());
                                            if (thisScene != null) {
                                                System.out.println("creating main scene, xbboxSize: " + xbboxSize);
                                                thisScene.createMainScene(world, objectList, multirezTile, xbboxSize * World.getxzScale(), zbboxSize * World.getxzScale(), dirprename, prename, destinationDirectory, zipFile, rangeScale, outputNorthToSouthRows, rootTile, noTrees);
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                    }
                    if (staticDebugOn) System.out.println("end loop for each tile array");
                }
                finished = true;
                numTileArraysProcessed++;
                if (numTileArraysProcessed == world.numTileArrays) fileInputDone = true;
                if (guiClass == null) parameters.setQuit(true);
                System.out.println("Finished processing");
                if (staticDebugOn) System.out.println("end main loop ");
            }
            if (staticDebugOn) {
                System.out.println("After end main loop: close debug ");
            }
            if (thisGUI != null) {
                thisGUI.close();
            }
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        } catch (IllegalAccessException e) {
            System.out.println(e);
        } catch (InstantiationException e) {
            System.out.println(e);
        } catch (FileNotFoundException fnfe) {
            System.out.println("Cannot find file " + fnfe.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            try {
                debug(e + "-closing debug  file");
                if (staticDebugOn) debugFile.close();
            } catch (IOException f) {
                System.err.println(f);
            }
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                debug("finally - closing debug file");
                if (staticDebugOn) debugFile.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * Create the output tiles class dynamically based on the output tilser selected by the user.
     */
    private RezTileFW createOutputTiler(TileArray[] sourceTileArrays, int arrayCount, World world) {
        parsedFilePath = sourceTileArrays[arrayCount].tiles[srcTileNumber].path;
        try {
            Class<?> multirezTileClass = Class.forName(tileClass);
            multirezTile = (RezTileFW) (multirezTileClass.newInstance());
            methodIndex = reflectOn(multirezTile, "buildTree");
            if ((tileClass.indexOf("VRMLElevationGrid") != -1) || (tileClass.indexOf("VRMLCombineSplitTiler") != -1) || (tileClass.indexOf("geosurface") != -1)) {
                noTrees = false;
            }
            if ((tileClass.indexOf("geoX3D") != -1) || (tileClass.indexOf("geosurface") != -1)) {
                multirezTile.setGeoSpatialTilerFlag(true);
            }
            System.out.println("Rez: Output tiler class: " + tileClass);
            System.out.println("Rez: noTrees " + noTrees);
            doesBtree = multirezTile.bTree;
            onlyBtree = multirezTile.justBinary;
            System.out.println("Rez: outputAny " + multirezTile.outputAny);
            System.out.println("Rez: PreferredOutput " + PreferredOutput);
            if (PreferredOutput != null) {
                if ((PreferredOutput.equals("degrees")) && (multirezTile.outputAny)) {
                    outputInMeters = false;
                    multirezTile.setOutputInMeters(outputInMeters);
                } else if ((PreferredOutput.equals("meters")) && (multirezTile.outputAny)) {
                    outputInMeters = true;
                    multirezTile.setOutputInMeters(outputInMeters);
                }
            } else {
                outputInMeters = multirezTile.outputInMeters;
            }
            System.out.println("Rez- slice/split: outputInMeters " + outputInMeters);
            System.out.println("Rez: inputInDegrees " + thisParser.units.equals("degrees"));
            world.setOutputInMeters(outputInMeters);
            world.geoSystem = thisParser.geoSystem;
            sourceTileInDegrees = thisParser.units.equals("degrees");
            return multirezTile;
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        } catch (InstantiationException e) {
            System.out.println(e);
        } catch (IllegalAccessException e) {
            System.out.println(e);
        }
        return null;
    }

    private void initialiseForprocessing(TileArray[] sourceTileArrays, int arrayCount, World world) {
        if (staticDebugOn) debug("initialiseForprocessing: XtranslationOffset: " + world.xTranslationOffset + " ZtranslationOffset " + world.zTranslationOffset);
        if (staticDebugOn) debug("initialiseForprocessing: gridx: " + gridx + " gridz " + gridz);
        System.out.println("initialiseForprocessing: gridx: " + gridx + " gridz " + gridz);
        xDimension = sourceTileArrays[arrayCount].tiles[srcTileNumber].xDimension;
        zDimension = sourceTileArrays[arrayCount].tiles[srcTileNumber].zDimension;
        xSpacing = sourceTileArrays[arrayCount].tiles[srcTileNumber].getxSpacing();
        zSpacing = sourceTileArrays[arrayCount].tiles[srcTileNumber].getzSpacing();
        adjustOffsetsAndSpacing(world, sourceTileArrays, arrayCount);
        System.out.println("Generating tiles");
        if (staticDebugOn) debug("Rez: creating tilerClass: " + tileClass + lineSep);
        System.out.println("Rez: Be sure the input values parsed are accurate so that the following are accurate:");
        xbboxSize = ((double) (xDimension - 1)) * xSpacing;
        zbboxSize = ((double) (zDimension - 1)) * zSpacing;
        System.out.println("Rez: xbboxSize " + xbboxSize);
        System.out.println("Rez: xSpacing " + xSpacing);
        if (thisParser != null) {
            multirezTile.setUnits(thisParser.units);
        }
        System.out.println("=== init: multirezTile.setPosition() " + gridx * World.xzScale + " " + (gridx + xbboxSize) * World.xzScale + " " + gridz * World.xzScale + " " + (gridz + zbboxSize) * World.xzScale);
        multirezTile.setPosition(gridx * World.xzScale, 0, gridz * World.xzScale, (gridx + xbboxSize) * World.xzScale, (gridz + zbboxSize) * World.xzScale);
        multirezTile.setxSpacing(xSpacing);
        multirezTile.setzSpacing(zSpacing);
        multirezTile.setInitialRowDivisions(world.initialLatDivisions);
        multirezTile.setInitialColDivisions(world.initialLonDivisions);
        System.out.println("set init divisions " + world.initialLatDivisions);
        if (NEGridPosition != null) multirezTile.setNEPosition(NEGridPosition);
    }

    private void performPreProcessing(GeoImage[] detailedImageFiles, GeoObject[] objectList, Bounds sliceBounds, GeoObject[] slideList, GridTile[] detailedTopoFiles, boolean sliceBoundsListed, World world) {
        try {
            if (detailedImageFiles != null) {
                System.out.println(" preprocess");
                preprocessImages(detailedImageFiles);
                multirezTile.setDetailedImages(detailedImageFiles);
            }
            if (objectList != null) {
                System.out.println(" preprocess objects ");
                preprocessObjects(objectList);
                multirezTile.setGeoObjects(objectList);
            }
            if (sliceBoundsListed) {
                multirezTile.setSliceBounds(sliceBounds);
            }
            if (slideList != null) {
                preprocessObjects(slideList);
            }
            if (detailedTopoFiles != null) {
                processTopoHeaders(detailedTopoFiles);
                multirezTile.setDetailedTopoTiles(detailedTopoFiles);
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * Adjusts gridx,gridz which are also used in the tree generator section (if called)
     *
     * This decider assumes degrees/meters are the only alternatives but we will have to allow for other values
     * for units for both inputs and possibly outputs.  We could standardize on degrees/meters for output
     * interfaces though, however there is less control over the input data and we cannot standardize on
     * any particular units there.
     */
    private void adjustOffsetsAndSpacing(World world, TileArray[] sourceTileArrays, int arrayCount) {
        gridx = sourceTileArrays[arrayCount].tiles[srcTileNumber].gridx;
        gridz = sourceTileArrays[arrayCount].tiles[srcTileNumber].gridz;
        if (sourceTileInDegrees) {
            if (outputInMeters) {
                System.out.println(lineSep + "Possible origin Translation Offsets (in meters) you may wish to apply" + lineSep + "to avoid jitter (or to bring tile to (0,0) for viewpoints) are:" + lineSep + "longitude(x) Translation: " + (-gridx * World.getDegreesToMeters()) + lineSep + "latitude(z) Translation: " + (-gridz * World.getDegreesToMeters()));
                convertToMeters = true;
                System.out.println("Source Tile in Degrees - adjusting gridx,z by translation offsets");
                System.out.println("Adjusting origin (longitude(x),latitude(z)) by user given translation offsets: " + world.xTranslationOffset + " " + world.zTranslationOffset + " meters" + lineSep);
                gridx *= World.getDegreesToMeters() + world.xTranslationOffset;
                gridz *= World.getDegreesToMeters() + world.zTranslationOffset;
                xSpacing *= World.getDegreesToMeters();
                zSpacing *= World.getDegreesToMeters();
                System.out.println("output in meters" + lineSep + "after adjusting with offsets: longitude(x):" + gridx + " latitude(z) " + gridz + lineSep);
            } else {
                gridx += world.xTranslationOffset;
                gridz += world.zTranslationOffset;
                System.out.println("output in degrees" + lineSep + "after adjusting with offsets: gridx:" + gridx + " gridz " + gridz);
            }
        } else {
            if (staticDebugOn) debug("Input Tile in units of meters ");
            System.out.println(lineSep + "Possible origin Translation Offsets (in meters) you may wish to apply" + lineSep + "to avoid jitter (or to bring tile to (0,0) for viewpoints) are:" + lineSep + "longitude(x) Translation: " + (-gridx) + lineSep + " latitude(z) Translation: " + (-gridz));
            if (outputInMeters) {
                gridx += world.xTranslationOffset;
                gridz += world.zTranslationOffset;
                System.out.println("output in meters" + lineSep + "after adjusting with offsets: gridx:" + gridx + " gridz " + gridz);
            } else {
                xSpacing /= World.getDegreesToMeters();
                zSpacing /= World.getDegreesToMeters();
                gridx /= World.getDegreesToMeters() + world.xTranslationOffset / World.getDegreesToMeters();
                gridz /= World.getDegreesToMeters() + world.zTranslationOffset / World.getDegreesToMeters();
                System.out.println("output in degrees" + lineSep + "after adjusting with offsets: gridx:" + gridx + " gridz " + gridz);
            }
        }
    }

    /**
     *  Generates separate tree nodes for the elevation tiles.
     *  Subgrids are all generated as wrl files.
     */
    private RootTile genTrees(World world, int level, int firstTreeLevel, int finalTreeLevel, String dirprename, String fileName, String destinationDirectory, boolean zipFile, double rangeScale, double horizontalScale, SampledTileRow tileRow, double height, String titleText, GridTile thisTile, RezTileFW multirezTile, int methodIndex, int operationType) {
        double xPosition;
        double zPosition;
        double initialZbb = 0;
        int numRowDivisions = 1;
        int numColDivisions = 1;
        int nextNumRowDivisions = 1;
        int nextNumColDivisions = 1;
        boolean widthFirst = true;
        if (xDimension < zDimension) widthFirst = false;
        double tmpz = zSpacing * horizontalScale;
        double tmpx = xSpacing * horizontalScale;
        debug("genTrees: rangeScale " + rangeScale);
        debug("genTrees: xSpacing " + xSpacing);
        debug("genTrees:  Columns: " + (tileRow.xcolumns[0] - 1));
        try {
            if (level == firstTreeLevel) {
                if ((operationType == SPLITTING) || (operationType == INDIVIDUAL || (operationType == SLICE_SPLIT))) {
                    xbboxSize = ((double) (xDimension - 1)) * tmpx;
                    zbboxSize = ((double) (zDimension - 1)) * tmpz;
                } else {
                    xbboxSize = SamplingParams.getCombinedXbboxSize();
                    zbboxSize = SamplingParams.getCombinedZbboxSize();
                }
                originalXbboxSize = xbboxSize;
                originalZbboxSize = zbboxSize;
                initialZbb = zbboxSize;
                System.out.println("Generating trees, level: " + level + lineSep);
                debug("calling level 0 tree xb: " + xbboxSize + "," + tileRow.xcolumns[0] + " zbb " + zbboxSize);
                debug("calling level 0 tree gridx: " + gridx + " gridz " + gridz);
                if (bsp) {
                    if (!outputNorthToSouthRows) {
                        debug("Output South To North");
                        rootTile = invokeBuildTree(multirezTile, methodIndex, level, 0, xbboxSize, 0, 0, zbboxSize, 0, 0, 0, 1, "0-0");
                    } else {
                        rootTile = invokeBuildTree(multirezTile, methodIndex, level, gridx * horizontalScale, xbboxSize, 0, -gridz * horizontalScale, zbboxSize, 0, 0, 0, 1, "0-0");
                    }
                } else {
                    if (!outputNorthToSouthRows) {
                        debug("Output South To North");
                        rootTile = invokeBuildTree(multirezTile, methodIndex, level, 0, xbboxSize, 0, 0, zbboxSize, 0, 1, 0, 1, "0-0");
                    } else {
                        rootTile = invokeBuildTree(multirezTile, methodIndex, level, gridx * horizontalScale, xbboxSize, 0, -gridz * horizontalScale, zbboxSize, 0, 1, 0, 1, "0-0");
                    }
                }
            } else if (level <= finalTreeLevel) {
                if ((operationType == SPLITTING) || (operationType == WHOLE_SPLIT) || (operationType == SLICE_SPLIT)) {
                    if (bsp) {
                        if (widthFirst) {
                            numRowDivisions = 1 << (level / 2);
                            numColDivisions = 1 << ((level + 1) / 2);
                            nextNumRowDivisions = (level) % 2 + 1;
                            nextNumColDivisions = (level + 1) % 2 + 1;
                        } else {
                            numRowDivisions = 1 << ((level + 1) / 2);
                            numColDivisions = 1 << (level / 2);
                            nextNumRowDivisions = (level + 1) % 2 + 1;
                            nextNumColDivisions = (level) % 2 + 1;
                        }
                    } else {
                        numColDivisions = 1 << level;
                        numRowDivisions = numColDivisions;
                        nextNumColDivisions = 2;
                        nextNumRowDivisions = nextNumColDivisions;
                    }
                } else {
                    System.out.println("not splitting");
                    numRowDivisions = 1;
                    numColDivisions = 1;
                }
                zPosition = -gridz * horizontalScale;
                int startRow = 0;
                int endRow = nextNumRowDivisions - 1;
                debug("firstTreeLevel " + firstTreeLevel + " numRowDivisions " + numRowDivisions);
                debug("zbboxSize " + zbboxSize + " xbboxSize " + xbboxSize);
                debug("numRowDivisions " + numRowDivisions + " xbboxSize " + xbboxSize);
                zbboxSize = originalZbboxSize / (double) numRowDivisions;
                xbboxSize = originalXbboxSize / (double) numColDivisions;
                for (int zrow = 0; zrow < numRowDivisions; zrow++) {
                    tmpz = zSpacing * horizontalScale;
                    if (convertToMeters) {
                        debug(" adjusting spacing tmp values");
                        tmpz *= World.getDegreesToMeters();
                    }
                    if (!outputNorthToSouthRows) {
                        xPosition = 0;
                    } else {
                        xPosition = gridx * horizontalScale;
                    }
                    int startColumn = 0;
                    int endColumn = nextNumColDivisions - 1;
                    for (int xcolumn = 0; xcolumn < numColDivisions; xcolumn++) {
                        String suffix = new String(Integer.toString(zrow) + "-" + Integer.toString(xcolumn));
                        tmpx = xSpacing * horizontalScale;
                        if (convertToMeters) {
                            debug(" adjusting spacing tmp values");
                            tmpx *= World.getDegreesToMeters();
                        }
                        debug("GenTree: xcolumn: " + xcolumn + " xcolumns  " + tileRow.xcolumns[xcolumn]);
                        debug("Gentree: rows  " + tileRow.zrows[zrow]);
                        debug("Gentree column: " + xcolumn + "row: " + zrow + "xb: " + xbboxSize + "," + tileRow.xcolumns[xcolumn] + " zbb " + zbboxSize);
                        debug("Gentree calling tree xp: " + xPosition + " zP " + zPosition);
                        debug("Gentree level " + level + " finalTreeLevel " + finalTreeLevel);
                        if (level == finalTreeLevel) {
                            debug("Gentree calling leaf");
                            rootTile = invokeBuildTree(multirezTile, methodIndex, level, xPosition, xbboxSize, height, zPosition, zbboxSize, 0, 0, 0, 0, suffix);
                        } else {
                            rootTile = invokeBuildTree(multirezTile, methodIndex, level, xPosition, xbboxSize, height, zPosition, zbboxSize, startRow, endRow, startColumn, endColumn, suffix);
                        }
                        xPosition += xbboxSize;
                        startColumn += nextNumColDivisions;
                        endColumn += nextNumColDivisions;
                    }
                    zPosition += zbboxSize;
                    startRow += nextNumRowDivisions;
                    endRow += nextNumRowDivisions;
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e + " Check Plugin is one that generates tree nodes");
        }
        return (rootTile);
    }

    private RezTileFW.RootTile invokeBuildTree(RezTileFW object, int methodIndex, int level, double xTranslation, double xbboxSize, double height, double zTranslation, double zbboxSize, int startRow, int endRow, int startColumn, int endColumn, String suffix) {
        if (tileClass.indexOf("geosurface") != -1) {
            return (((rez.plugins.geosurface.GeoVRMLTile) object).buildTree(level, xTranslation, xbboxSize, height, zTranslation, zbboxSize, startRow, endRow, startColumn, endColumn, suffix));
        } else {
            Class c = object.getClass();
            Method[] theMethods = c.getMethods();
            Object[] arguments = new Object[11];
            RezTileFW.RootTile returnValue = null;
            arguments[0] = new Integer(level);
            arguments[1] = new Double(xTranslation);
            arguments[2] = (new Double(xbboxSize));
            arguments[3] = (new Double(height));
            arguments[4] = (new Double(zTranslation));
            arguments[5] = (new Double(zbboxSize));
            arguments[6] = (new Integer(startRow));
            arguments[7] = (new Integer(endRow));
            arguments[8] = (new Integer(startColumn));
            arguments[9] = (new Integer(endColumn));
            arguments[10] = suffix;
            returnValue = (RezTileFW.RootTile) invokeMethod(object, methodIndex, arguments);
            return (returnValue);
        }
    }

    private Object invokeMethod(Object object, int methodIndex, Object[] arguments) {
        Class c = object.getClass();
        Method[] theMethods = c.getMethods();
        Object returnValue = null;
        try {
            returnValue = theMethods[methodIndex].invoke(object, arguments);
        } catch (IllegalAccessException e) {
            System.out.println("failed to invoke " + e);
        } catch (InvocationTargetException e) {
            System.out.println("failed to invoke " + e);
        }
        return (returnValue);
    }

    private int reflectOn(RezTileFW object, String methodName) {
        int localMethodIndex = 0;
        Class<?> c = object.getClass();
        Method[] theMethods = c.getMethods();
        for (int i = 9; i < theMethods.length; i++) {
            String methodString = theMethods[i].getName();
            String returnString = theMethods[i].getReturnType().getName();
            if (methodString.equals(methodName)) {
                noTrees = false;
                localMethodIndex = i;
            }
        }
        return localMethodIndex;
    }

    private void processTopoHeaders(GridTile[] detailedTopos) throws IOException {
        for (int i = 0; i < detailedTopos.length; i++) {
            String fileName = detailedTopos[i].getPath();
            int dotindex = fileName.lastIndexOf(".");
            dotindex = (dotindex < 0) ? 0 : dotindex;
            String tmp = (dotindex < 1) ? fileName : fileName.substring(0, dotindex + 1) + "hdr";
            System.out.println("detailed topo header filename " + tmp);
            File worldFile = new File(tmp);
            if (!worldFile.exists()) {
                System.out.println("Rez: Could not find file: " + tmp);
                debug("Rez: Could not find directory: " + tmp);
                throw new IOException("File not Found");
            }
            BufferedReader worldFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(worldFile)));
            if (staticDebugOn) debug("b4nextline: ");
            line = worldFileReader.readLine();
            if (staticDebugOn) debug("line: " + line);
            if (line != null) {
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                System.out.println("line for nrows: " + line);
                double nrows = (double) getValue(tokenizer, line, "NROWS");
                line = worldFileReader.readLine();
                System.out.println("line for ncols: " + line);
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                double ncols = (double) getValue(tokenizer, line, "NCOLS");
                line = worldFileReader.readLine();
                System.out.println("line for gridx: " + line);
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                double lon = getdValue(tokenizer, line, "ULXMAP");
                line = worldFileReader.readLine();
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                double lat = getdValue(tokenizer, line, "ULYMAP");
                line = worldFileReader.readLine();
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                double lonSpacing = getdValue(tokenizer, line, "XDIM");
                line = worldFileReader.readLine();
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                double latSpacing = getdValue(tokenizer, line, "YDIM");
                debug("line: " + line);
                detailedTopos[i].setLon(lon);
                detailedTopos[i].setLat(lat);
                detailedTopos[i].setBottomRightx(lon + (ncols - 1) * lonSpacing);
                detailedTopos[i].setBottomRightz(lat + (nrows - 1) * latSpacing);
            }
        }
    }

    private void preprocessImages(GeoImage[] detailedImages) throws IOException {
        for (int i = 0; i < detailedImages.length; i++) {
            BufferedImage img = loadImage(detailedImages[i].getPath());
            detailedImages[i].setLatDim(img.getHeight());
            detailedImages[i].setLonDim(img.getWidth());
            freeImage(img);
            String fileName = detailedImages[i].getPath();
            int dotindex = fileName.lastIndexOf(".");
            dotindex = dotindex < 0 ? 0 : dotindex;
            String tmp = dotindex < 1 ? fileName : fileName.substring(0, dotindex + 3) + "w";
            System.out.println("filename " + tmp);
            File worldFile = new File(tmp);
            if (!worldFile.exists()) {
                System.out.println("Rez: Could not find file: " + tmp);
                debug("Rez: Could not find directory: " + tmp);
                throw new IOException("File not Found");
            }
            BufferedReader worldFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(worldFile)));
            if (staticDebugOn) debug("b4nextline: ");
            line = worldFileReader.readLine();
            if (staticDebugOn) debug("line: " + line);
            if (line != null) {
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                detailedImages[i].setLonSpacing(Double.valueOf(tokenizer.nextToken()).doubleValue());
                detailedImages[i].setLonExtent(detailedImages[i].getLonSpacing() * ((double) detailedImages[i].getLonDim() - 1d));
                System.out.println("setLonExtent " + detailedImages[i].getLonExtent());
                line = worldFileReader.readLine();
                if (staticDebugOn) debug("skip line: " + line);
                line = worldFileReader.readLine();
                if (staticDebugOn) debug("skip line: " + line);
                line = worldFileReader.readLine();
                if (staticDebugOn) debug("line: " + line);
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                detailedImages[i].setLatSpacing(Double.valueOf(tokenizer.nextToken()).doubleValue());
                detailedImages[i].setLatExtent(detailedImages[i].getLatSpacing() * ((double) detailedImages[i].getLatDim() - 1d));
                line = worldFileReader.readLine();
                if (staticDebugOn) debug("line: " + line);
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                detailedImages[i].setLon(Double.valueOf(tokenizer.nextToken()).doubleValue());
                line = worldFileReader.readLine();
                if (staticDebugOn) debug("line: " + line);
                tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                detailedImages[i].setLat(Double.valueOf(tokenizer.nextToken()).doubleValue());
                int slashindex = fileName.lastIndexOf(java.io.File.separator);
                slashindex = slashindex < 0 ? 0 : slashindex;
                if (slashindex == 0) {
                    slashindex = fileName.lastIndexOf("/");
                    slashindex = slashindex < 0 ? 0 : slashindex;
                }
                tmp = slashindex < 1 ? fileName : fileName.substring(slashindex + 1, fileName.length());
                System.out.println("filename " + destinationDirectory + XPlat.fileSep + tmp);
                detailedImages[i].setPath(tmp);
                DataInputStream dataIn = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
                DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destinationDirectory + XPlat.fileSep + tmp)));
                System.out.println("copying to " + destinationDirectory + XPlat.fileSep + tmp);
                for (; ; ) {
                    try {
                        dataOut.writeShort(dataIn.readShort());
                    } catch (EOFException e) {
                        break;
                    } catch (IOException e) {
                        break;
                    }
                }
                dataOut.close();
            } else {
                System.out.println("Rez: ERROR: World file for image is null");
            }
        }
    }

    private void preprocessObjects(GeoObject[] objects) throws IOException {
        System.out.println("objects.length " + objects.length);
        for (int i = 0; i < objects.length; i++) {
            String fileName = objects[i].getPath();
            int dotindex = fileName.lastIndexOf(".");
            dotindex = dotindex < 0 ? 0 : dotindex;
            String tmp = dotindex < 1 ? fileName : fileName.substring(0, dotindex + 3) + "w";
            System.out.println("i: " + " world filename " + tmp);
            File worldFile = new File(tmp);
            if (worldFile.exists()) {
                BufferedReader worldFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(worldFile)));
                if (staticDebugOn) debug("b4nextline: ");
                line = worldFileReader.readLine();
                if (staticDebugOn) debug("line: " + line);
                if (line != null) {
                    line = worldFileReader.readLine();
                    if (staticDebugOn) debug("line: " + line);
                    tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                    objects[i].setLon(Double.valueOf(tokenizer.nextToken()).doubleValue());
                    line = worldFileReader.readLine();
                    if (staticDebugOn) debug("line: " + line);
                    tokenizer = new StringTokenizer(line, " \n\t\r\"", false);
                    objects[i].setLat(Double.valueOf(tokenizer.nextToken()).doubleValue());
                }
            }
            File file = new File(objects[i].getPath());
            if (file.exists()) {
                System.out.println("object src file found ");
                int slashindex = fileName.lastIndexOf(java.io.File.separator);
                slashindex = slashindex < 0 ? 0 : slashindex;
                if (slashindex == 0) {
                    slashindex = fileName.lastIndexOf("/");
                    slashindex = slashindex < 0 ? 0 : slashindex;
                }
                tmp = slashindex < 1 ? fileName : fileName.substring(slashindex + 1, fileName.length());
                System.out.println("filename " + destinationDirectory + XPlat.fileSep + tmp);
                objects[i].setPath(tmp);
                file = new File(fileName);
                if (file.exists()) {
                    DataInputStream dataIn = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
                    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destinationDirectory + XPlat.fileSep + tmp)));
                    System.out.println("copying to " + destinationDirectory + XPlat.fileSep + tmp);
                    for (; ; ) {
                        try {
                            dataOut.writeShort(dataIn.readShort());
                        } catch (EOFException e) {
                            break;
                        } catch (IOException e) {
                            break;
                        }
                    }
                    dataOut.close();
                }
            }
        }
    }

    /**
     * Loads a java image given the fully qualified pathname. The image
     * formats are those supported by java. i.e JPEG or GIF images.
     *
     */
    private BufferedImage loadImage(String imageFileName) {
        System.out.println("Loading image " + imageFileName);
        FileSeekableStream stream = null;
        try {
            stream = new FileSeekableStream(imageFileName);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        RenderedOp image1 = JAI.create("stream", stream);
        return (image1.getAsBufferedImage());
    }

    void freeImage(BufferedImage image) {
        if (image != null) image.flush();
        image = null;
    }

    /**
     * A utility text line reader.  Skips over comment lines beginnign with a hash character.
     */
    private String nextLine() {
        try {
            String tmpStr = d.readLine();
            debug("nextLine: tmpStr : " + tmpStr);
            if (tmpStr != null) while (tmpStr.indexOf("#") != -1) {
                debug("nextLine, skipping: tmpStr : " + tmpStr);
                tmpStr = d.readLine();
            }
            return tmpStr;
        } catch (IOException e) {
            System.out.println("nextLine: error reading file " + e);
        }
        return null;
    }

    /**
     * A utility text line reader.  Skips over comment lines beginnign with a hash character.
     */
    private String nextLine(BufferedReader reader) throws IOException {
        String tmpStr = reader.readLine();
        debug("nextLine: tmpStr : " + tmpStr);
        if (tmpStr != null) while (tmpStr.indexOf("#") != -1) {
            tmpStr = reader.readLine();
        }
        return tmpStr;
    }

    private String buildString(BufferedReader reader) throws IOException {
        String str;
        String localLine;
        localLine = reader.readLine();
        str = "";
        while (localLine != null) {
            str = str + localLine + lineSep;
            localLine = reader.readLine();
        }
        reader.close();
        return (str);
    }

    private void debug(String str) {
        if (Debug.debugOn) {
            System.out.println(str);
            try {
                if (staticDebugOn) debugFile.write(str + lineSep);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    /** looks for a substring in the given string and if found will
     * return the following numeric value as an int.
     */
    private int getValue(StringTokenizer tokenizer, String line, String label) {
        String tmpToken;
        int tmp;
        tmpToken = tokenizer.nextToken();
        if (line.indexOf(label) != -1) {
            while (tmpToken.indexOf(label) == -1) tmpToken = tokenizer.nextToken();
            tmpToken = tokenizer.nextToken();
            tmp = Integer.parseInt(tmpToken);
            return tmp;
        } else return noGood;
    }

    /** looks for a substring in the given string and if found will
     * return the following string value.
     */
    private String getString(StringTokenizer tokenizer, String line, String label) {
        String tmpToken;
        tmpToken = tokenizer.nextToken();
        if (line.indexOf(label) != -1) {
            while (tmpToken.indexOf(label) == -1) tmpToken = tokenizer.nextToken();
            tmpToken = tokenizer.nextToken();
            return tmpToken;
        } else return "-999999";
    }

    /** looks for a substring in the given string and if found will
     * return the following numeric value as an int.
     */
    private float getfValue(StringTokenizer tokenizer, String line, String label) {
        String tmpToken;
        float tmp;
        tmpToken = tokenizer.nextToken();
        if (line.indexOf(label) != -1) {
            while (tmpToken.indexOf(label) == -1) {
                tmpToken = tokenizer.nextToken();
            }
            tmpToken = tokenizer.nextToken();
            tmp = Float.valueOf(tmpToken).floatValue();
            return tmp;
        } else return noGood;
    }

    /** looks for a substring in the given string and if found will
     * return the following numeric value as a double.
     */
    private double getdValue(StringTokenizer tokenizer, String line, String label) {
        String tmpToken;
        double tmp;
        tmpToken = tokenizer.nextToken();
        if (line.indexOf(label) != -1) {
            while (tmpToken.indexOf(label) == -1) {
                tmpToken = tokenizer.nextToken();
            }
            tmpToken = tokenizer.nextToken();
            tmp = Double.valueOf(tmpToken).doubleValue();
            return tmp;
        } else return noGood;
    }

    @Override
    public void run() {
        thisGUI.createGUI(parameters);
    }

    public static void main(String[] args) {
        System.out.println("args.length " + args.length);
        if ((args.length > 17) || (args.length < 3)) {
            System.out.println("Usage: java -cp CLASSPATH Rez configFile fromLevel toLevel rangeScale zipFlag \n" + "sampleFlag samplingIncrease detailScale(x-z) heightScale(y) minOutputTiledimension \n" + "maxOutputTileDimension, translationOffsetX translationOffsetZ treeType(y) operationType(1)\n" + "initLatDivisions(2),  initLongDivisions(2))\n" + "example(with defaults): \n" + "java -Xmx1024 -DdebugOn=false -cp [path to rez.jar] rez.Rez config.txt 1 3 2 n n 0 1 1 16 60 0 0 y 1\n");
        } else {
            String configFile = args[0];
            String fromLevelStr = args[1];
            String toLevelStr = args[2];
            String rangeScale = "1.0";
            String heightScale = "1.0";
            String zipFlag = "y";
            String debugFlag = "n";
            String sampleFlag = "n";
            String treeType = "n";
            String operationType = "1";
            String samplingIncreaseStr = "0";
            String minTileDimStr = "16";
            String maxTileDimStr = "60";
            String transOffsetX = "0";
            String transOffsetZ = "0";
            String initLatDivisions = "1";
            String initLongDivisions = "1";
            int samplingIncrease = 0;
            String xzScale = "1";
            if (args.length > 3) rangeScale = args[3];
            if (args.length > 4) zipFlag = args[4];
            if (args.length > 5) sampleFlag = args[5];
            if (args.length > 6) samplingIncreaseStr = args[6];
            if (args.length > 7) xzScale = args[7];
            if (args.length > 8) heightScale = args[8];
            if (args.length > 9) minTileDimStr = args[9];
            if (args.length > 10) maxTileDimStr = args[10];
            if (args.length > 11) transOffsetX = args[11];
            if (args.length > 12) transOffsetZ = args[12];
            if (args.length > 13) treeType = args[13];
            if (args.length > 14) operationType = args[14];
            if (args.length > 15) initLatDivisions = args[15];
            if (args.length > 16) initLongDivisions = args[16];
            samplingIncrease = Integer.parseInt(samplingIncreaseStr);
            new Rez(configFile, fromLevelStr, toLevelStr, rangeScale, zipFlag, debugFlag, sampleFlag, samplingIncrease, xzScale, heightScale, minTileDimStr, maxTileDimStr, transOffsetX, transOffsetZ, treeType, operationType, initLatDivisions, initLongDivisions);
            System.out.println("Finished\n");
        }
    }
}
