package it.tac.ct.ui.swixml;

import it.tac.ct.core.COLORS;
import it.tac.ct.core.ColorPalette;
import it.tac.ct.core.F;
import it.tac.ct.core.FCoordinate;
import it.tac.ct.core.GraphicalObjectCoordinate;
import it.tac.ct.core.Map4CT;
import it.tac.ct.core.MapsGenerator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.RepaintManager;
import no.geosoft.cc.geometry.Geometry;
import no.geosoft.cc.graphics.GInteraction;
import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GScene;
import no.geosoft.cc.graphics.GSegment;
import no.geosoft.cc.graphics.GStyle;
import no.geosoft.cc.graphics.GText;
import no.geosoft.cc.graphics.GWindow;
import org.swixml.SwingEngine;

/**
 * @author Mario Stefanutti
 * @version September 2007
 *          <p>
 *          Main program: It uses Swixml for GUI and the G graphical library for graphic
 *          </p>
 *          <p>
 *          MOVED TO SOURCEFORGE TRACKER:
 *          <li>Bug?: if method == F and maxNumber < 7 and automatic filter is active --> computation does not produce maps (only the basic one)
 *          <li>Bug?: Place somewhere else the variables: x, y, w, h and startRadius, stopRadius, ... of F?
 *          <li>Bug?: Review the indexes used for array and similar. Some begin from 0 and some from 1
 *          <li>Bug: When coloring the map, do not recreate it every time but change only the styles
 *          <li>Bug?: Verify if styles are created also when not needed (create 4 styles and use the same style for more objects)
 *          <li>Bug: If you push other buttons when automatic color algorithm is working (separated thread) coloring start acting strange
 *          <li>Add a coloring method based on the arbitrary choice of the three color for central face, ocean + another face adjoining the other two
 *          <li>Set a map manually (using the string that represents the "sequence of coordinates" as in logging on stdout)
 *          <li>Add a flag to not filter (remove) any map, not even if a not reach-able face has cardinality = 2
 *          <li>Add timing statistics
 *          <li>Color all maps (silent without visualization + progress bar)
 *          <li>Add a grid to easily understand each face which face it is (or mouse over)
 *          <li>Add a tool that from the picture of a graph or of a map (to upload), transform it into a simplified map
 *          <li>Make the user interface more compact and write an introduction (all before publishing it) - skins
 *          <li>Save and restore maps on disk (single and groups). Each map can be saved as a string that represents the "sequence of coordinates"
 *          <li>Save all images found (if list is small or +- 10 maps respect the current one)
 *          <li>Show the use of memory directly on the UI (heap)
 *          </p>
 *          <p>
 *          DONE (older than SourceForge):
 *          <li>Starting from a displayed map remove the last inserted face (not considering the Ocean) and create another view for compare coloring
 *          <li>Add a map searching tool: for example "find a map with 13 faces" or "find a map with with at least two F6 faces confining" or ... CANCELLED
 *          <li>Try to think to an highly distributed architecture using a grid of computers to distribute jobs and increase memory available CANCELLED
 *          <li>Transform a map that does'nt have an F5 ocean into a map with an F5 ocean (from plane back to sphere and hole an F5)
 *          <li>Permit zoom or color mode or find a way to have both at the same time (different buttons?) CANCELLED
 *          <li>When F mode is activated and while generating maps, filter "closed" maps that have reached F faces (only if >= 12)
 *          <li>Filter maps with less than F faces (considering the ocean)
 *          <li>Change the transparent slider to text and add an action to it (open a swixml2 BUG for sliders not handling actions)
 *          <li>CANCELLED: Use 64 bits JVM to use more memory. 64 bits JVM has many bugs
 *          <li>If the list of maps gets empty (after a filtering) also clean the screen with the visualized map
 *          <li>LinkedList have been changed to ArrayList to free memory (CPU is sacrificed)
 *          <li>Clean the code: Adjust the automatic coloring algorithm and then find some enhancement + some others + some others
 *          <li>Clean the code: Simplify hasUnreachableFWithCardinalityLessThanFive
 *          <li>Clean the code: Fix the fNumberAtIndex and fNumberAtIndexForColors problem
 *          <li>Set automatic method: compute, filter, copy, compute, filter, copy, etc. NOTE: FIXED_MAPS_LEN already daes it
 *          <li>VERIFIED (it works correctly): Filter less than 4 does not longer work: it remove all maps, no matter what
 *          <li>Automatic "color it" algorithm button
 *          <li>CANCELLED: For the NewYork mode version show a skyline and the statue of liberty
 *          <li>Permit to color the map from a four color palette
 *          <li>text/no-text for numbers
 *          <li>Add buttons to move and visualize maps in order: start, previous, random, next, last
 *          <li>Create the object Ocean to color it, through mouse selection as any other face
 *          <li>Add print button
 *          <li>Circles mode
 *          <li>3D mode: navigable in all directions + all angles (like google earth). It will be done for the JMonkey version
 *          <li>Remove maps that have also ocean < 5
 *          <li>Set todoList = maps
 *          <li>Use a LinkedList instead a Map (to free memory)
 *          <li>Build using Ant or Maven (consider also the configuration file)
 *          <li>Add more graphical controls: X-Ray, colors, etc.
 *          <li>Use JGoodies FormLayout
 *          <li>Find a graphic library for swing --> G: http://geosoft.no/graphics/
 *          </p>
 */
@SuppressWarnings("serial")
public class MapsGeneratorMain extends JFrame implements GInteraction {

    private MapsGenerator mapsGenerator = new MapsGenerator();

    private Map4CT map4CTCurrent = null;

    private int map4CTCurrentIndex = -1;

    private GScene scene = null;

    private GWindow window = null;

    private enum DRAW_METHOD {

        CIRCLES, RECTANGLES, RECTANGLES_NEW_YORK
    }

    ;

    private final JFrame mainframe = null;

    private final JTextField slowdownMillisec = null;

    private final JCheckBox logWhilePopulate = null;

    private final JCheckBox randomElaboration = null;

    private final JCheckBox processAll = null;

    private final JComboBox maxMethod = null;

    private final JTextField maxNumber = null;

    private final JButton startElaboration = null;

    private final JButton pauseElaboration = null;

    private final JButton filterLessThanFourElaboration = null;

    private final JButton filterLessThanFiveElaboration = null;

    private final JButton filterLessThanFacesElaboration = null;

    private final JButton copyMapsToTodoElaboration = null;

    private final JButton resetElaboration = null;

    private final JButton createMapFromTextRepresentation = null;

    private final JTextField mapTextRepresentation = null;

    private final JTextField mapsSize = null;

    private final JTextField currentMap = null;

    private final JTextField mapsRemoved = null;

    private final JTextField todoListSize = null;

    private final JTextField totalMemory = null;

    private final JTextField maxMemory = null;

    private final JTextField freeMemory = null;

    private final JComboBox drawMethod = null;

    private Enum<DRAW_METHOD> drawMethodValue = DRAW_METHOD.CIRCLES;

    private final JSlider transparency = null;

    private int transparencyValue = 255;

    private final JPanel mapExplorer = null;

    private final JCheckBox showFaceCardinality = null;

    private final JButton tait = null;

    private final JCheckBox soundWhileColoring = null;

    private final JComboBox colorOneInstrument = null;

    private final JTextField colorOneBaseNote = null;

    private final JTextField colorOneBaseDuration = null;

    private final JTextField colorOneBaseVelocity = null;

    private final JComboBox colorTwoInstrument = null;

    private final JTextField colorTwoBaseNote = null;

    private final JTextField colorTwoBaseDuration = null;

    private final JTextField colorTwoBaseVelocity = null;

    private final JComboBox colorThreeInstrument = null;

    private final JTextField colorThreeBaseNote = null;

    private final JTextField colorThreeBaseDuration = null;

    private final JTextField colorThreeBaseVelocity = null;

    private final JComboBox colorFourInstrument = null;

    private final JTextField colorFourBaseNote = null;

    private final JTextField colorFourBaseDuration = null;

    private final JTextField colorFourBaseVelocity = null;

    private Soundbank soundbank = null;

    private Synthesizer synthesizer = null;

    private Instrument[] instruments = null;

    private MidiChannel[] midiChannels = null;

    private final JButton selectColorOne = null;

    private final JButton selectColorTwo = null;

    private final JButton selectColorThree = null;

    private final JButton selectColorFour = null;

    private Color colorOne = null;

    private Color colorTwo = null;

    private Color colorThree = null;

    private Color colorFour = null;

    private boolean stopColorRequested = false;

    private Thread colorItThread = null;

    private Thread colorAllThread = null;

    public static final int LINE_WIDTH = 1;

    /**
     * Main program
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        new MapsGeneratorMain();
    }

    /**
     * Constructor
     */
    private MapsGeneratorMain() throws Exception {
        SwingEngine<MapsGeneratorMain> engine = new SwingEngine<MapsGeneratorMain>(this);
        URL configFileURL = this.getClass().getClassLoader().getResource("config/4ct-v2.xml");
        engine.render(configFileURL).setVisible(false);
        URL soundbankURL = this.getClass().getClassLoader().getResource("config/soundbank-deluxe.gm");
        if (soundbankURL.getProtocol().equals("jar")) {
            soundbank = MidiSystem.getSoundbank(soundbankURL);
        } else {
            File soundbankFile = new File(soundbankURL.toURI());
            soundbank = MidiSystem.getSoundbank(soundbankFile);
        }
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        synthesizer.loadAllInstruments(soundbank);
        instruments = synthesizer.getLoadedInstruments();
        midiChannels = synthesizer.getChannels();
        setInstrumentsNames(colorOneInstrument, instruments);
        setInstrumentsNames(colorTwoInstrument, instruments);
        setInstrumentsNames(colorThreeInstrument, instruments);
        setInstrumentsNames(colorFourInstrument, instruments);
        colorOne = new Color(255, 99, 71);
        colorTwo = new Color(50, 205, 50);
        colorThree = new Color(238, 238, 0);
        colorFour = new Color(176, 196, 222);
        selectColorOne.setForeground(colorOne);
        selectColorTwo.setForeground(colorTwo);
        selectColorThree.setForeground(colorThree);
        selectColorFour.setForeground(colorFour);
        new Thread(refreshManager).start();
        initMapExplorerForGraphic();
        RepaintManager.currentManager(this).setDoubleBufferingEnabled(true);
        validate();
        setVisible(true);
    }

    public void setInstrumentsNames(JComboBox jComboBox, Instrument[] instruments) {
        for (int i = 0; i < instruments.length; i++) {
            jComboBox.addItem(instruments[i].getName());
        }
    }

    public void initMapExplorerForGraphic() {
        window = new GWindow(colorFour);
        scene = new GScene(window);
        mapExplorer.removeAll();
        mapExplorer.add(window.getCanvas());
        scene.setWorldExtent(-0.1, -0.1, 1.2, 1.2);
        window.startInteraction(this);
    }

    /**
     * Utility class to run the generate() method of the MapsGenerator
     */
    private final Runnable refreshManager = new Runnable() {

        public void run() {
            while (true) {
                refreshInfo();
                refreshMemoryInfo();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
    };

    /**
     * Utility class to run the generate() method of the MapsGenerator
     */
    private final Runnable runnableGenerate = new Runnable() {

        public void run() {
            try {
                mapsGenerator.generate();
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                startElaboration.setEnabled(!(mapsGenerator.todoList.size() == 0));
                pauseElaboration.setEnabled(false);
                resetElaboration.setEnabled(true);
                filterLessThanFourElaboration.setEnabled(true);
                filterLessThanFiveElaboration.setEnabled(true);
                filterLessThanFacesElaboration.setEnabled(true);
                copyMapsToTodoElaboration.setEnabled(true);
                createMapFromTextRepresentation.setEnabled(true);
                refreshInfo();
            }
        }
    };

    /**
     * The G map to draw
     */
    private class GMap4CTRectangles extends GObject {

        private GSegment[] rectangles = null;

        private Map4CT map4CT = null;

        /**
         * Constructor
         * 
         * @param map4CTToDraw
         *            The map to draw
         */
        public GMap4CTRectangles(Map4CT map4CTToDraw) {
            map4CT = map4CTToDraw;
            rectangles = new GSegment[map4CT.faces.size()];
            for (int i = 0; i < map4CT.faces.size(); i++) {
                rectangles[i] = new GSegment();
                addSegment(rectangles[i]);
                rectangles[i].setUserData(map4CT.faces.get(i));
            }
        }

        /**
         * draw the map
         */
        @Override
        public void draw() {
            double normalizationXFactor = map4CT.sequenceOfCoordinates.sequence.size();
            double normalizationYFactor = map4CT.faces.size();
            List<GraphicalObjectCoordinate> graphicalObjectCoordinates = new ArrayList<GraphicalObjectCoordinate>(map4CT.faces.size());
            for (int i = 0; i < map4CT.faces.size(); i++) {
                graphicalObjectCoordinates.add(new GraphicalObjectCoordinate());
            }
            graphicalObjectCoordinates.get(0).x = 0.0f;
            graphicalObjectCoordinates.get(0).y = 0.0f;
            graphicalObjectCoordinates.get(0).w = 1.0f;
            graphicalObjectCoordinates.get(0).h = 1.0f;
            for (int i = 1; i < map4CT.sequenceOfCoordinates.sequence.size() - 1; i++) {
                GraphicalObjectCoordinate g = graphicalObjectCoordinates.get(map4CT.sequenceOfCoordinates.sequence.get(i).fNumber - 1);
                if (map4CT.sequenceOfCoordinates.sequence.get(i).type == FCoordinate.TYPE.BEGIN) {
                    g.x = (float) ((i / normalizationXFactor) + (1.0d / (2.0d * normalizationXFactor)));
                    g.y = (float) ((map4CT.sequenceOfCoordinates.sequence.get(i).fNumber - 1.0d) / normalizationYFactor);
                    g.w = (float) i;
                    g.h = (float) (1.0d - g.y);
                    if (drawMethodValue == DRAW_METHOD.RECTANGLES_NEW_YORK) {
                        g.y = 1 - g.y;
                        g.h = -1 * g.h;
                    }
                } else {
                    g.w = (float) ((i - g.w) * (1.0d / normalizationXFactor));
                }
            }
            for (int iFace = 0; iFace < map4CT.faces.size(); iFace++) {
                GraphicalObjectCoordinate g = graphicalObjectCoordinates.get(iFace);
                rectangles[iFace].setGeometryXy(Geometry.createRectangle(g.x, g.y, g.w, g.h));
                if (showFaceCardinality.isSelected()) {
                    rectangles[iFace].setText(new GText("" + map4CT.faces.get(iFace).cardinality));
                }
                GStyle faceStyle = styleFromFace(map4CT.faces.get(iFace));
                rectangles[iFace].setStyle(faceStyle);
            }
            if (showFaceCardinality.isSelected()) {
                rectangles[0].setText(new GText("" + (map4CT.faces.size() + 1) + ": " + map4CT.faces.get(0).cardinality + " - " + map4CT.sequenceOfCoordinates.numberOfVisibleEdgesAtBorders()));
            }
        }

        /**
         * draw just one rectangle of the map
         */
        public void drawN(int face) {
            double normalizationXFactor = map4CT.sequenceOfCoordinates.sequence.size();
            double normalizationYFactor = map4CT.faces.size();
            List<GraphicalObjectCoordinate> graphicalObjectCoordinates = new ArrayList<GraphicalObjectCoordinate>(map4CT.faces.size());
            for (int i = 0; i < map4CT.faces.size(); i++) {
                graphicalObjectCoordinates.add(new GraphicalObjectCoordinate());
            }
            graphicalObjectCoordinates.get(0).x = 0.0f;
            graphicalObjectCoordinates.get(0).y = 0.0f;
            graphicalObjectCoordinates.get(0).w = 1.0f;
            graphicalObjectCoordinates.get(0).h = 1.0f;
            for (int i = 1; i < map4CT.sequenceOfCoordinates.sequence.size() - 1; i++) {
                GraphicalObjectCoordinate g = graphicalObjectCoordinates.get(map4CT.sequenceOfCoordinates.sequence.get(i).fNumber - 1);
                if (map4CT.sequenceOfCoordinates.sequence.get(i).type == FCoordinate.TYPE.BEGIN) {
                    g.x = (float) ((i / normalizationXFactor) + (1.0d / (2.0d * normalizationXFactor)));
                    g.y = (float) ((map4CT.sequenceOfCoordinates.sequence.get(i).fNumber - 1.0d) / normalizationYFactor);
                    g.w = (float) i;
                    g.h = (float) (1.0d - g.y);
                    if (drawMethodValue == DRAW_METHOD.RECTANGLES_NEW_YORK) {
                        g.y = 1 - g.y;
                        g.h = -1 * g.h;
                    }
                } else {
                    g.w = (float) ((i - g.w) * (1.0d / normalizationXFactor));
                }
            }
            GraphicalObjectCoordinate g = graphicalObjectCoordinates.get(face);
            rectangles[face].setGeometryXy(Geometry.createRectangle(g.x, g.y, g.w, g.h));
            if (showFaceCardinality.isSelected()) {
                rectangles[face].setText(new GText("" + map4CT.faces.get(face).cardinality));
            }
            GStyle faceStyle = styleFromFace(map4CT.faces.get(face));
            rectangles[face].setStyle(faceStyle);
            if (face == 0) {
                if (showFaceCardinality.isSelected()) {
                    rectangles[0].setText(new GText("" + (map4CT.faces.size() + 1) + ": " + map4CT.faces.get(0).cardinality + " - " + map4CT.sequenceOfCoordinates.numberOfVisibleEdgesAtBorders()));
                }
            }
        }
    }

    ;

    /**
     * The G map to draw
     */
    private class GMap4CTCircles extends GObject {

        private GSegment[] rings = null;

        private Map4CT map4CT = null;

        private final float MAX_RADIUS = 0.5f;

        /**
         * Constructor
         * 
         * @param map4CTToDraw
         *            The map to draw
         */
        public GMap4CTCircles(Map4CT map4CTToDraw) {
            map4CT = map4CTToDraw;
            rings = new GSegment[map4CT.faces.size()];
            for (int i = 0; i < map4CT.faces.size(); i++) {
                rings[i] = new GSegment();
                addSegment(rings[i]);
                rings[i].setUserData(map4CT.faces.get(i));
            }
        }

        /**
         * draw the map
         */
        @Override
        public void draw() {
            double normalizationAngleFactor = map4CT.sequenceOfCoordinates.sequence.size();
            double spaceBetweenCircles = MAX_RADIUS / map4CT.faces.size();
            List<GraphicalObjectCoordinate> graphicalObjectCoordinates = new ArrayList<GraphicalObjectCoordinate>(map4CT.faces.size());
            for (int i = 0; i < map4CT.faces.size(); i++) {
                graphicalObjectCoordinates.add(new GraphicalObjectCoordinate());
            }
            GraphicalObjectCoordinate g = null;
            for (int i = 1; i < map4CT.sequenceOfCoordinates.sequence.size() - 1; i++) {
                g = graphicalObjectCoordinates.get(map4CT.sequenceOfCoordinates.sequence.get(i).fNumber - 1);
                if (map4CT.sequenceOfCoordinates.sequence.get(i).type == FCoordinate.TYPE.BEGIN) {
                    g.startAngle = (float) ((i / normalizationAngleFactor) * 2 * Math.PI);
                } else {
                    g.stopAngle = (float) ((i / normalizationAngleFactor) * 2 * Math.PI);
                }
            }
            rings[0].setGeometryXy(createCircle(0.5, 0.5, MAX_RADIUS));
            if (showFaceCardinality.isSelected()) {
                rings[0].setText(new GText("" + (map4CT.faces.size() + 1) + ": " + map4CT.faces.get(0).cardinality + " - " + map4CT.sequenceOfCoordinates.numberOfVisibleEdgesAtBorders()));
            }
            GStyle faceStyle = styleFromFace(map4CT.faces.get(0));
            rings[0].setStyle(faceStyle);
            for (int iFace = 1; iFace < map4CT.faces.size(); iFace++) {
                g = graphicalObjectCoordinates.get(iFace);
                g.startRadius = (float) (iFace * spaceBetweenCircles);
                g.stopRadius = MAX_RADIUS;
                rings[iFace].setGeometryXy(createRing(0.5, 0.5, g.startRadius, g.stopRadius, g.startAngle, g.stopAngle));
                if (showFaceCardinality.isSelected()) {
                    rings[iFace].setText(new GText("" + map4CT.faces.get(iFace).cardinality));
                }
                faceStyle = styleFromFace(map4CT.faces.get(iFace));
                rings[iFace].setStyle(faceStyle);
            }
        }

        /**
         * draw just one ring of the map
         */
        public void drawN(int face) {
            if (face == 0) {
                rings[0].setGeometryXy(createCircle(0.5, 0.5, MAX_RADIUS));
                if (showFaceCardinality.isSelected()) {
                    rings[0].setText(new GText("" + (map4CT.faces.size() + 1) + ": " + map4CT.faces.get(0).cardinality + " - " + map4CT.sequenceOfCoordinates.numberOfVisibleEdgesAtBorders()));
                }
                GStyle faceStyle = styleFromFace(map4CT.faces.get(0));
                rings[0].setStyle(faceStyle);
            } else {
                double normalizationAngleFactor = map4CT.sequenceOfCoordinates.sequence.size();
                double spaceBetweenCircles = MAX_RADIUS / map4CT.faces.size();
                List<GraphicalObjectCoordinate> graphicalObjectCoordinates = new ArrayList<GraphicalObjectCoordinate>(map4CT.faces.size());
                for (int i = 0; i < map4CT.faces.size(); i++) {
                    graphicalObjectCoordinates.add(new GraphicalObjectCoordinate());
                }
                GraphicalObjectCoordinate g = null;
                for (int i = 1; i < map4CT.sequenceOfCoordinates.sequence.size() - 1; i++) {
                    g = graphicalObjectCoordinates.get(map4CT.sequenceOfCoordinates.sequence.get(i).fNumber - 1);
                    if (map4CT.sequenceOfCoordinates.sequence.get(i).type == FCoordinate.TYPE.BEGIN) {
                        g.startAngle = (float) ((i / normalizationAngleFactor) * 2 * Math.PI);
                    } else {
                        g.stopAngle = (float) ((i / normalizationAngleFactor) * 2 * Math.PI);
                    }
                }
                g = graphicalObjectCoordinates.get(face);
                g.startRadius = (float) (face * spaceBetweenCircles);
                g.stopRadius = MAX_RADIUS;
                rings[face].setGeometryXy(createRing(0.5, 0.5, g.startRadius, g.stopRadius, g.startAngle, g.stopAngle));
                if (showFaceCardinality.isSelected()) {
                    rings[face].setText(new GText("" + map4CT.faces.get(face).cardinality));
                }
                GStyle faceStyle = styleFromFace(map4CT.faces.get(face));
                rings[face].setStyle(faceStyle);
            }
        }

        public double[] createRing(double xCenter, double yCenter, double startRadius, double stopRadius, double startAngle, double stopAngle) {
            double arcStep = Math.PI / 180.0;
            int internalPointsPerArc = (int) ((stopAngle - startAngle) / arcStep);
            int pointX = 0;
            double[] ring = new double[((internalPointsPerArc + 2) * 2 * 2) + 2];
            pointX = 0;
            ring[pointX] = (Math.cos(startAngle) * startRadius) + xCenter;
            ring[pointX + 1] = (Math.sin(startAngle) * startRadius) + yCenter;
            pointX += (internalPointsPerArc * 2) + 2;
            ring[pointX] = (Math.cos(stopAngle) * startRadius) + xCenter;
            ring[pointX + 1] = (Math.sin(stopAngle) * startRadius) + yCenter;
            pointX += 2;
            ring[pointX] = (Math.cos(stopAngle) * stopRadius) + xCenter;
            ring[pointX + 1] = (Math.sin(stopAngle) * stopRadius) + yCenter;
            pointX += (internalPointsPerArc * 2) + 2;
            ring[pointX] = (Math.cos(startAngle) * stopRadius) + xCenter;
            ring[pointX + 1] = (Math.sin(startAngle) * stopRadius) + yCenter;
            pointX += 2;
            ring[pointX] = ring[0];
            ring[pointX + 1] = ring[1];
            for (int i = 1; i <= internalPointsPerArc; i++) {
                pointX = i * 2;
                ring[pointX] = (Math.cos(startAngle + (arcStep * i)) * startRadius) + xCenter;
                ring[pointX + 1] = (Math.sin(startAngle + (arcStep * i)) * startRadius) + yCenter;
                pointX += (internalPointsPerArc * 2) + 4;
                ring[pointX] = (Math.cos(stopAngle - (arcStep * i)) * stopRadius) + xCenter;
                ring[pointX + 1] = (Math.sin(stopAngle - (arcStep * i)) * stopRadius) + yCenter;
            }
            return ring;
        }

        public double[] createCircle(double xCenter, double yCenter, double radius) {
            int points = 360;
            double[] circle = new double[(points * 2) + 2];
            for (int i = 0; i <= points; i++) {
                circle[i * 2] = (Math.cos(i * (Math.PI / 180)) * radius) + xCenter;
                circle[(i * 2) + 1] = (Math.sin(i * (Math.PI / 180)) * radius) + xCenter;
            }
            return circle;
        }
    }

    ;

    public Action quitAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            System.exit(NORMAL);
        }
    };

    public Action logWhilePopulateAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.logWhilePopulate = logWhilePopulate.isSelected();
        }
    };

    public Action startElaborationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.slowdownMillisec = Integer.parseInt(slowdownMillisec.getText());
            mapsGenerator.randomElaboration = randomElaboration.isSelected();
            mapsGenerator.logWhilePopulate = logWhilePopulate.isSelected();
            mapsGenerator.processAll = processAll.isSelected();
            if (maxMethod.getSelectedIndex() == 0) {
                mapsGenerator.maxMethod = MapsGenerator.MAX_METHOD.FIXED_MAPS_LEN;
            } else if (maxMethod.getSelectedIndex() == 1) {
                mapsGenerator.maxMethod = MapsGenerator.MAX_METHOD.MAPS;
            } else if (maxMethod.getSelectedIndex() == 2) {
                mapsGenerator.maxMethod = MapsGenerator.MAX_METHOD.F;
            }
            mapsGenerator.maxNumber = Integer.parseInt(maxNumber.getText());
            startElaboration.setEnabled(false);
            pauseElaboration.setEnabled(true);
            resetElaboration.setEnabled(false);
            filterLessThanFourElaboration.setEnabled(false);
            filterLessThanFiveElaboration.setEnabled(false);
            filterLessThanFacesElaboration.setEnabled(false);
            copyMapsToTodoElaboration.setEnabled(false);
            createMapFromTextRepresentation.setEnabled(false);
            new Thread(runnableGenerate).start();
        }
    };

    public Action pauseElaborationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.stopRequested = true;
        }
    };

    public Action filterLessThanFourElaborationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.removeMapsWithCardinalityLessThanFour();
            refreshInfo();
            if (mapsGenerator.maps.size() == 0) {
                map4CTCurrent = null;
                map4CTCurrentIndex = -1;
                drawCurrentMap();
            }
        }
    };

    public Action filterLessThanFiveElaborationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.removeMapsWithCardinalityLessThanFive();
            refreshInfo();
            if (mapsGenerator.maps.size() == 0) {
                map4CTCurrent = null;
                map4CTCurrentIndex = -1;
                drawCurrentMap();
            }
        }
    };

    public Action filterLessThanFacesElaborationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.removeMapsWithLessThanFFaces(Integer.parseInt(maxNumber.getText()));
            refreshInfo();
            if (mapsGenerator.maps.size() == 0) {
                map4CTCurrent = null;
                map4CTCurrentIndex = -1;
                drawCurrentMap();
            }
        }
    };

    public Action copyMapsToTodoElaborationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.todoList = new ArrayList<Map4CT>();
            mapsGenerator.copyMapsToTodo();
            if (mapsGenerator.todoList.size() != 0) {
                startElaboration.setEnabled(true);
            }
            refreshInfo();
        }
    };

    public Action resetElaborationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator = new MapsGenerator();
            map4CTCurrent = null;
            map4CTCurrentIndex = -1;
            startElaboration.setEnabled(true);
            pauseElaboration.setEnabled(false);
            resetElaboration.setEnabled(false);
            filterLessThanFourElaboration.setEnabled(false);
            filterLessThanFiveElaboration.setEnabled(false);
            filterLessThanFacesElaboration.setEnabled(false);
            copyMapsToTodoElaboration.setEnabled(false);
            createMapFromTextRepresentation.setEnabled(true);
            refreshInfo();
            drawCurrentMap();
        }
    };

    public Action createMapFromTextRepresentationAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            mapsGenerator.createMapFromTextRepresentation(mapTextRepresentation.getText());
            map4CTCurrentIndex = mapsGenerator.maps.size();
            map4CTCurrent = mapsGenerator.maps.get(map4CTCurrentIndex - 1);
            startElaboration.setEnabled(!(mapsGenerator.todoList.size() == 0));
            pauseElaboration.setEnabled(false);
            resetElaboration.setEnabled(true);
            filterLessThanFourElaboration.setEnabled(true);
            filterLessThanFiveElaboration.setEnabled(true);
            filterLessThanFacesElaboration.setEnabled(true);
            copyMapsToTodoElaboration.setEnabled(true);
            createMapFromTextRepresentation.setEnabled(true);
            refreshInfo();
            drawCurrentMap();
        }
    };

    public Action textRepresentationOfCurrentMapAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            if (map4CTCurrent != null) {
                mapTextRepresentation.setText(map4CTCurrent.sequenceOfCoordinates.sequence.toString().substring(1, map4CTCurrent.sequenceOfCoordinates.sequence.toString().length() - 1));
            }
        }
    };

    public Action drawCurrentMapSlowMotionAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            drawCurrentMapSlowMotion();
        }
    };

    public Action refreshInfoAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            refreshInfo();
        }
    };

    public Action drawMethodAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            drawCurrentMap();
            if (drawMethodValue == DRAW_METHOD.RECTANGLES) {
                tait.setEnabled(true);
            } else if (drawMethodValue == DRAW_METHOD.RECTANGLES_NEW_YORK) {
                tait.setEnabled(true);
            } else if (drawMethodValue == DRAW_METHOD.CIRCLES) {
                tait.setEnabled(false);
            }
        }
    };

    public final void setTransparencyValue(int value) {
        drawCurrentMap();
        transparencyValue = value;
    }

    public final int getTransparencyValue() {
        return transparencyValue;
    }

    public Action showFaceCardinalityAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            drawCurrentMap();
        }
    };

    public Action drawFirstMapAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            if (mapsGenerator.maps.size() != 0) {
                map4CTCurrentIndex = 0;
                map4CTCurrent = mapsGenerator.maps.get(map4CTCurrentIndex);
                drawCurrentMap();
            }
        }
    };

    public Action drawPreviousMapAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            if ((mapsGenerator.maps.size() != 0) && (map4CTCurrentIndex > 0)) {
                map4CTCurrentIndex = map4CTCurrentIndex - 1;
                map4CTCurrent = mapsGenerator.maps.get(map4CTCurrentIndex);
                drawCurrentMap();
            }
        }
    };

    public Action drawRandomMapAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            if (mapsGenerator.maps.size() != 0) {
                map4CTCurrentIndex = new Random().nextInt(mapsGenerator.maps.size());
                map4CTCurrent = mapsGenerator.maps.get(map4CTCurrentIndex);
                drawCurrentMap();
            }
        }
    };

    public Action drawNextMapAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            if ((mapsGenerator.maps.size() != 0) && (map4CTCurrentIndex < (mapsGenerator.maps.size() - 1))) {
                map4CTCurrentIndex = map4CTCurrentIndex + 1;
                map4CTCurrent = mapsGenerator.maps.get(map4CTCurrentIndex);
                drawCurrentMap();
            }
        }
    };

    public Action drawLastMapAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            if (mapsGenerator.maps.size() != 0) {
                map4CTCurrentIndex = mapsGenerator.maps.size() - 1;
                map4CTCurrent = mapsGenerator.maps.get(map4CTCurrentIndex);
                drawCurrentMap();
            }
        }
    };

    public Action taitAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            BufferedImage inputImage = new BufferedImage(window.getCanvas().getWidth(), window.getCanvas().getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = inputImage.createGraphics();
            window.getCanvas().paint(graphics2D);
            graphics2D.dispose();
            BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            int previousColor = 0;
            int currentColor = 0;
            int nextColor = 0;
            for (int iY = 1; iY < inputImage.getHeight() - 1; iY++) {
                for (int iX = 1; iX < inputImage.getWidth() - 1; iX++) {
                    currentColor = inputImage.getRGB(iX, iY);
                    if (currentColor == Color.BLACK.getRGB()) {
                        previousColor = inputImage.getRGB(iX - 1, iY);
                        nextColor = inputImage.getRGB(iX + 1, iY);
                        if (((previousColor == colorOne.getRGB()) && (nextColor == colorThree.getRGB())) || ((previousColor == colorThree.getRGB()) && (nextColor == colorOne.getRGB())) || ((previousColor == colorTwo.getRGB()) && (nextColor == colorFour.getRGB())) || ((previousColor == colorFour.getRGB()) && (nextColor == colorTwo.getRGB()))) {
                            outputImage.setRGB(iX - 1, iY, Color.RED.getRGB());
                            outputImage.setRGB(iX, iY, Color.RED.getRGB());
                            outputImage.setRGB(iX + 1, iY, Color.RED.getRGB());
                        }
                        if (((previousColor == colorOne.getRGB()) && (nextColor == colorTwo.getRGB())) || ((previousColor == colorTwo.getRGB()) && (nextColor == colorOne.getRGB())) || ((previousColor == colorThree.getRGB()) && (nextColor == colorFour.getRGB())) || ((previousColor == colorFour.getRGB()) && (nextColor == colorThree.getRGB()))) {
                            outputImage.setRGB(iX - 1, iY, Color.GREEN.getRGB());
                            outputImage.setRGB(iX, iY, Color.GREEN.getRGB());
                            outputImage.setRGB(iX + 1, iY, Color.GREEN.getRGB());
                        }
                        if (((previousColor == colorOne.getRGB()) && (nextColor == colorFour.getRGB())) || ((previousColor == colorFour.getRGB()) && (nextColor == colorOne.getRGB())) || ((previousColor == colorTwo.getRGB()) && (nextColor == colorThree.getRGB())) || ((previousColor == colorThree.getRGB()) && (nextColor == colorTwo.getRGB()))) {
                            outputImage.setRGB(iX - 1, iY, Color.BLUE.getRGB());
                            outputImage.setRGB(iX, iY, Color.BLUE.getRGB());
                            outputImage.setRGB(iX + 1, iY, Color.BLUE.getRGB());
                        }
                        previousColor = inputImage.getRGB(iX, iY - 1);
                        nextColor = inputImage.getRGB(iX, iY + 1);
                        if (((previousColor == colorOne.getRGB()) && (nextColor == colorThree.getRGB())) || ((previousColor == colorThree.getRGB()) && (nextColor == colorOne.getRGB())) || ((previousColor == colorTwo.getRGB()) && (nextColor == colorFour.getRGB())) || ((previousColor == colorFour.getRGB()) && (nextColor == colorTwo.getRGB()))) {
                            outputImage.setRGB(iX, iY - 1, Color.RED.getRGB());
                            outputImage.setRGB(iX, iY, Color.RED.getRGB());
                            outputImage.setRGB(iX, iY + 1, Color.RED.getRGB());
                        }
                        if (((previousColor == colorOne.getRGB()) && (nextColor == colorTwo.getRGB())) || ((previousColor == colorTwo.getRGB()) && (nextColor == colorOne.getRGB())) || ((previousColor == colorThree.getRGB()) && (nextColor == colorFour.getRGB())) || ((previousColor == colorFour.getRGB()) && (nextColor == colorThree.getRGB()))) {
                            outputImage.setRGB(iX, iY - 1, Color.GREEN.getRGB());
                            outputImage.setRGB(iX, iY, Color.GREEN.getRGB());
                            outputImage.setRGB(iX, iY + 1, Color.GREEN.getRGB());
                        }
                        if (((previousColor == colorOne.getRGB()) && (nextColor == colorFour.getRGB())) || ((previousColor == colorFour.getRGB()) && (nextColor == colorOne.getRGB())) || ((previousColor == colorTwo.getRGB()) && (nextColor == colorThree.getRGB())) || ((previousColor == colorThree.getRGB()) && (nextColor == colorTwo.getRGB()))) {
                            outputImage.setRGB(iX, iY - 1, Color.BLUE.getRGB());
                            outputImage.setRGB(iX, iY, Color.BLUE.getRGB());
                            outputImage.setRGB(iX, iY + 1, Color.BLUE.getRGB());
                        }
                    } else {
                        outputImage.setRGB(iX, iY, Color.WHITE.getRGB());
                    }
                }
            }
            JFrame taitFrame = new JFrame();
            JLabel taitImagelabel = new JLabel();
            taitImagelabel.setIcon(new ImageIcon(outputImage));
            taitFrame.setContentPane(taitImagelabel);
            taitFrame.pack();
            taitFrame.setVisible(true);
        }
    };

    public Action saveMapToImageAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            String fileName = null;
            String drawMethodName = "unknown";
            if (drawMethodValue == DRAW_METHOD.RECTANGLES) {
                drawMethodName = "rectangles";
            } else if (drawMethodValue == DRAW_METHOD.RECTANGLES_NEW_YORK) {
                drawMethodName = "rectangles_new_york";
            } else if (drawMethodValue == DRAW_METHOD.CIRCLES) {
                drawMethodName = "circles";
            }
            if (map4CTCurrent != null) {
                fileName = "save-" + drawMethodName + "-" + map4CTCurrent.hashCode() + ".png";
            } else {
                fileName = "save-" + drawMethodName + "-" + "000" + ".png";
            }
            try {
                File fileToSave = null;
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(null);
                chooser.setSelectedFile(new File(fileName));
                if (chooser.showOpenDialog(window.getCanvas()) == JFileChooser.APPROVE_OPTION) {
                    fileToSave = chooser.getSelectedFile();
                    BufferedImage bufferedImage = new BufferedImage(window.getCanvas().getWidth(), window.getCanvas().getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics2D = bufferedImage.createGraphics();
                    window.getCanvas().paint(graphics2D);
                    graphics2D.dispose();
                    ImageIO.write(bufferedImage, "png", fileToSave);
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    };

    public Action selectColorOneAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            colorOne = chooseNewColor(colorOne);
            selectColorOne.setForeground(colorOne);
        }
    };

    public Action selectColorTwoAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            colorTwo = chooseNewColor(colorTwo);
            selectColorTwo.setForeground(colorTwo);
        }
    };

    public Action selectColorThreeAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            colorThree = chooseNewColor(colorThree);
            selectColorThree.setForeground(colorThree);
        }
    };

    public Action selectColorFourAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            colorFour = chooseNewColor(colorFour);
            selectColorFour.setForeground(colorFour);
            initMapExplorerForGraphic();
        }
    };

    public Action selectColorDefaultAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            colorOne = Color.RED;
            colorTwo = Color.GREEN;
            colorThree = Color.BLUE;
            colorFour = Color.WHITE;
            selectColorOne.setForeground(colorOne);
            selectColorTwo.setForeground(colorTwo);
            selectColorThree.setForeground(colorThree);
            selectColorFour.setForeground(colorFour);
            initMapExplorerForGraphic();
        }
    };

    public Action colorItAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            stopColorRequested = true;
            try {
                Thread.sleep(120);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            stopColorRequested = false;
            if (colorItThread == null) {
                colorItThread = new Thread(runnableColorIt);
                colorItThread.start();
            } else if (colorItThread.isAlive() == false) {
                colorItThread = new Thread(runnableColorIt);
                colorItThread.start();
            }
        }
    };

    public Action colorAllAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            stopColorRequested = true;
            try {
                Thread.sleep(120);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            stopColorRequested = false;
            if (colorAllThread == null) {
                colorAllThread = new Thread(runnableColorAll);
                colorAllThread.start();
            } else if (colorAllThread.isAlive() == false) {
                colorAllThread = new Thread(runnableColorAll);
                colorAllThread.start();
            }
        }
    };

    public Action stopColorAction = new AbstractAction() {

        public void actionPerformed(ActionEvent e) {
            stopColorRequested = true;
            try {
                Thread.sleep(120);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            if (map4CTCurrent != null) {
                map4CTCurrent.resetColors();
                drawCurrentMap();
            }
        }
    };

    private final Runnable runnableColorIt = new Runnable() {

        public void run() {
            colorIt();
        }
    };

    private final Runnable runnableColorAll = new Runnable() {

        public void run() {
            colorAll();
        }
    };

    public void colorAll() {
        if (mapsGenerator.maps.size() != 0) {
            for (; (map4CTCurrentIndex < mapsGenerator.maps.size()) && (stopColorRequested == false); map4CTCurrentIndex++) {
                map4CTCurrent = mapsGenerator.maps.get(map4CTCurrentIndex);
                colorIt();
            }
        }
    }

    public void colorIt() {
        boolean endOfJob = false;
        boolean colorFound = false;
        boolean moveBackOneFace = false;
        int currentFaceIndex = 0;
        F faceToAnalyze = null;
        ColorPalette colorsFacingTheOcean = new ColorPalette(false);
        List<ColorPalette> mapsPalette = new ArrayList<ColorPalette>();
        if (map4CTCurrent != null) {
            Map4CT mapBeingColored = map4CTCurrent;
            for (int i = 0; i < mapBeingColored.faces.size(); i++) {
                mapsPalette.add(new ColorPalette(true));
            }
            mapBeingColored.resetColors();
            drawCurrentMap();
            faceToAnalyze = mapBeingColored.faces.get(0);
            while (!endOfJob && !stopColorRequested) {
                colorFound = false;
                moveBackOneFace = false;
                while (!colorFound && !moveBackOneFace) {
                    if (mapsPalette.get(currentFaceIndex).palette.size() == 0) {
                        moveBackOneFace = true;
                        mapsPalette.get(currentFaceIndex).resetToFull();
                        faceToAnalyze.color = COLORS.UNCOLORED;
                        if (mapBeingColored.isFaceFacingTheOcean(currentFaceIndex + 1) == true) {
                            colorsFacingTheOcean.palette.pop();
                        }
                        currentFaceIndex--;
                    } else {
                        faceToAnalyze.color = mapsPalette.get(currentFaceIndex).palette.pop();
                        if (mapBeingColored.isFaceFacingTheOcean(currentFaceIndex + 1) == true) {
                            if (colorsFacingTheOcean.palette.contains(faceToAnalyze.color) == false) {
                                colorsFacingTheOcean.palette.add(faceToAnalyze.color);
                            }
                        }
                        if (mapBeingColored.isFaceCorrectlyColoredRespectToPreviousNeighbors(faceToAnalyze) == true) {
                            if (colorsFacingTheOcean.palette.size() < 4) {
                                colorFound = true;
                                currentFaceIndex++;
                            }
                        }
                    }
                    changeInstruments();
                    if (soundWhileColoring.isSelected()) {
                        if (faceToAnalyze.color == COLORS.ONE) {
                            Integer note = Integer.parseInt(colorOneBaseNote.getText()) + currentFaceIndex;
                            if (note > 127) {
                                note = 127;
                            }
                            midiChannels[0].noteOn(note, Integer.parseInt(colorOneBaseVelocity.getText()));
                            try {
                                Thread.sleep(Integer.parseInt(colorOneBaseDuration.getText()));
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                            midiChannels[0].noteOff(note);
                        } else if (faceToAnalyze.color == COLORS.TWO) {
                            Integer note = Integer.parseInt(colorTwoBaseNote.getText()) + currentFaceIndex;
                            if (note > 127) {
                                note = 127;
                            }
                            midiChannels[1].noteOn(note, Integer.parseInt(colorTwoBaseVelocity.getText()));
                            try {
                                Thread.sleep(Integer.parseInt(colorTwoBaseDuration.getText()));
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                            midiChannels[1].noteOff(note);
                        } else if (faceToAnalyze.color == COLORS.THREE) {
                            Integer note = Integer.parseInt(colorThreeBaseNote.getText()) + currentFaceIndex;
                            midiChannels[2].noteOn(note, Integer.parseInt(colorThreeBaseVelocity.getText()));
                            try {
                                Thread.sleep(Integer.parseInt(colorThreeBaseDuration.getText()));
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                            midiChannels[2].noteOff(note);
                        } else if (faceToAnalyze.color == COLORS.FOUR) {
                            Integer note = Integer.parseInt(colorFourBaseNote.getText()) + currentFaceIndex;
                            midiChannels[3].noteOn(note, Integer.parseInt(colorFourBaseVelocity.getText()));
                            try {
                                Thread.sleep(Integer.parseInt(colorFourBaseDuration.getText()));
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                            midiChannels[3].noteOff(note);
                        }
                    }
                }
                if (currentFaceIndex == mapBeingColored.faces.size()) {
                    endOfJob = true;
                } else if (mapBeingColored != map4CTCurrent) {
                    endOfJob = true;
                } else {
                    faceToAnalyze = mapBeingColored.faces.get(currentFaceIndex);
                }
                drawCurrentMap();
            }
        }
    }

    /**
     * Refresh runtime info
     */
    public synchronized void refreshInfo() {
        currentMap.setText("" + (map4CTCurrentIndex + 1));
        mapsSize.setText("" + mapsGenerator.maps.size());
        mapsRemoved.setText("" + mapsGenerator.removed);
        todoListSize.setText("" + mapsGenerator.todoList.size());
    }

    /**
     * Refresh runtime info
     */
    public synchronized void refreshMemoryInfo() {
        totalMemory.setText("" + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " Mb");
        maxMemory.setText("" + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " Mb");
        freeMemory.setText("" + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " Mb");
    }

    public Color chooseNewColor(Color currentColor) {
        Color newColor = JColorChooser.showDialog(null, "Change color", currentColor);
        if (newColor == null) {
            newColor = currentColor;
        }
        return newColor;
    }

    /**
     * Draw current map (or reset graph if map4CTCurrent is null)
     */
    public synchronized void drawCurrentMap() {
        RepaintManager.currentManager(this).setDoubleBufferingEnabled(true);
        if (drawMethod.getSelectedIndex() == 0) {
            drawMethodValue = DRAW_METHOD.CIRCLES;
        } else if (drawMethod.getSelectedIndex() == 1) {
            drawMethodValue = DRAW_METHOD.RECTANGLES;
        } else {
            drawMethodValue = DRAW_METHOD.RECTANGLES_NEW_YORK;
        }
        if (map4CTCurrent != null) {
            scene.removeAll();
            if (drawMethodValue != DRAW_METHOD.CIRCLES) {
                GMap4CTRectangles gMap4CTRectangles = new GMap4CTRectangles(map4CTCurrent);
                scene.add(gMap4CTRectangles);
                gMap4CTRectangles.draw();
            } else {
                GMap4CTCircles gMap4CTCirlces = new GMap4CTCircles(map4CTCurrent);
                scene.add(gMap4CTCirlces);
                gMap4CTCirlces.draw();
            }
            scene.refresh();
            mapExplorer.validate();
        } else {
            scene.removeAll();
            scene.refresh();
        }
    }

    /**
     * Draw current map in slow motion mode
     */
    public synchronized void drawCurrentMapSlowMotion() {
        if (drawMethod.getSelectedIndex() == 0) {
            drawMethodValue = DRAW_METHOD.CIRCLES;
        } else if (drawMethod.getSelectedIndex() == 1) {
            drawMethodValue = DRAW_METHOD.RECTANGLES;
        } else {
            drawMethodValue = DRAW_METHOD.RECTANGLES_NEW_YORK;
        }
        if (map4CTCurrent != null) {
            scene.removeAll();
            if (drawMethodValue != DRAW_METHOD.CIRCLES) {
                GMap4CTRectangles gMap4CTRectangles = new GMap4CTRectangles(map4CTCurrent);
                scene.add(gMap4CTRectangles);
                for (int i = 0; i < map4CTCurrent.faces.size(); i++) {
                    gMap4CTRectangles.drawN(i);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    scene.refresh();
                    mapExplorer.validate();
                }
            } else {
                GMap4CTCircles gMap4CTCircles = new GMap4CTCircles(map4CTCurrent);
                scene.add(gMap4CTCircles);
                for (int i = 0; i < map4CTCurrent.faces.size(); i++) {
                    gMap4CTCircles.drawN(i);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    scene.refresh();
                    mapExplorer.validate();
                }
            }
        } else {
            scene.removeAll();
            scene.refresh();
        }
    }

    /**
     * Play sounds: set or change the selected instruments
     */
    public void changeInstruments() {
        midiChannels[0].programChange(colorOneInstrument.getSelectedIndex());
        midiChannels[1].programChange(colorTwoInstrument.getSelectedIndex());
        midiChannels[2].programChange(colorThreeInstrument.getSelectedIndex());
        midiChannels[3].programChange(colorFourInstrument.getSelectedIndex());
    }

    ;

    /**
     * @param face
     * @return The new style
     */
    public GStyle styleFromFace(F face) {
        Color colorToUse = null;
        GStyle faceStyle = new GStyle();
        if (face.color == COLORS.UNCOLORED) {
            colorToUse = new Color(255, 255, 255, transparency.getValue());
        } else if (face.color == COLORS.ONE) {
            colorToUse = new Color(colorOne.getRed(), colorOne.getGreen(), colorOne.getBlue(), transparency.getValue());
        } else if (face.color == COLORS.TWO) {
            colorToUse = new Color(colorTwo.getRed(), colorTwo.getGreen(), colorTwo.getBlue(), transparency.getValue());
        } else if (face.color == COLORS.THREE) {
            colorToUse = new Color(colorThree.getRed(), colorThree.getGreen(), colorThree.getBlue(), transparency.getValue());
        } else if (face.color == COLORS.FOUR) {
            colorToUse = new Color(colorFour.getRed(), colorFour.getGreen(), colorFour.getBlue(), transparency.getValue());
        }
        faceStyle.setForegroundColor(Color.black);
        faceStyle.setBackgroundColor(colorToUse);
        faceStyle.setLineWidth(LINE_WIDTH);
        return faceStyle;
    }

    public void event(GScene scene, int event, int x, int y) {
        if (event == GWindow.BUTTON1_DOWN) {
            GSegment interactionSegment = scene.findSegment(x, y);
            if (interactionSegment != null) {
                Color colorToUse = null;
                F face = (F) interactionSegment.getUserData();
                if (face.color == COLORS.UNCOLORED) {
                    colorToUse = new Color(colorOne.getRed(), colorOne.getGreen(), colorOne.getBlue(), transparency.getValue());
                    face.color = COLORS.ONE;
                } else if (face.color == COLORS.ONE) {
                    colorToUse = new Color(colorTwo.getRed(), colorTwo.getGreen(), colorTwo.getBlue(), transparency.getValue());
                    face.color = COLORS.TWO;
                } else if (face.color == COLORS.TWO) {
                    colorToUse = new Color(colorThree.getRed(), colorThree.getGreen(), colorThree.getBlue(), transparency.getValue());
                    face.color = COLORS.THREE;
                } else if (face.color == COLORS.THREE) {
                    colorToUse = new Color(colorFour.getRed(), colorFour.getGreen(), colorFour.getBlue(), transparency.getValue());
                    face.color = COLORS.FOUR;
                } else if (face.color == COLORS.FOUR) {
                    colorToUse = new Color(255, 255, 255, transparency.getValue());
                    face.color = COLORS.UNCOLORED;
                }
                GStyle style = new GStyle();
                style.setForegroundColor(Color.black);
                style.setBackgroundColor(colorToUse);
                style.setLineWidth(LINE_WIDTH);
                interactionSegment.setStyle(style);
                scene.refresh();
            }
        }
    }
}
