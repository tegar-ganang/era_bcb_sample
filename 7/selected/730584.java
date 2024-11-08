package seevolution;

import seevolution.animation.*;
import java.io.*;
import java.awt.*;
import java.util.*;
import javax.vecmath.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.geometry.*;

/**
 * The class Chromosome is the generic class to represent Chromosomes in Seevolution. Chromosomes can be linear or circular,
 * and both of them extend this class, which holds the common methods. Several abstract methods are used to obtain the
 * geometrical transformations that differentiate the two types.<br>
 * In order to be able to go back and forward in the animation of a single event, each animation HAS TO be called with count = 0
 * before any other calls. In the animations that involve changes in the structure of the chromosome, this call is used to
 * store the geometrics of the chromosome before the start of the animation, and every posterior call to that method uses
 * the stored data to set the chromosome at the desired state, independent of what the previous step was. This is used to 
 * change the model as the mouse is dragged over the tree.<br>
 * In animations that involve the creation of a marker (insertions, deletions, substitutions and gene conversions) the marker
 * is created when count = 0, so if it isn't the initial call the posterior operations will be performed on a marker that 
 * doesn't exist.
 *
 * @author Andres Esteban Marcos
 * @version 1.0
 */
public abstract class Chromosome extends BranchGroup {

    /**
	 * Each section is assigned a single solid color
	 */
    public static final int COLORING_SOLID = 0;

    /**
	 * The different sections are assigned colors following a gradient between user defined colors. More than one consecutive section can share the same color
	 */
    public static final int COLORING_GRADIENT = 1;

    /**
	 * The sections are assigned random colors. More than one consecutive section can share the same color
	 */
    public static final int COLORING_RANDOM = 2;

    /**
	 * Represents a circular chromosome
	 */
    public static final int CIRCULAR = 0;

    /**
	 * Represents a linear chromosome
	 */
    public static final int LINEAR = 1;

    protected static final int INSERTION = 0;

    protected static final int DELETION = 1;

    protected static final int CONVERSION = 2;

    protected static final int SUBSTITUTION = 3;

    protected static final int TEXTURE_WIDTH = 20;

    protected static final int TEXTURE_HEIGHT = 1;

    protected int type;

    protected String name;

    protected int length;

    protected int numSides;

    protected int numSegments;

    protected boolean multipleApps;

    protected ChromosomeSegment segments[];

    protected int markerInds[];

    protected BranchGroup markers[][];

    protected TransformGroup tg;

    protected TransformGroup transformGroups[];

    protected int currentPos[];

    protected Transform3D tempTransform;

    protected Transform3D tr;

    protected TransformGroup tempTransformGroups[];

    protected int temp[], tempIndices[];

    protected int colorMode;

    protected boolean useTextures;

    protected Color3f colors[];

    protected int breakPoints[];

    protected float heatmap[];

    protected Texture2D textures[];

    protected Appearance markerApp;

    protected Appearance eventSectionApp;

    protected TransformGroup eventSectionTransformGroup;

    protected BranchGroup eventSectionBranchGroup;

    /**
	 * Creates a new chromosome
	 * @param type The type of the chromosome, it can be either CIRCULAR or LINEAR
	 * @param name The name of the chromosome
	 * @param length The length of the chromosome in number of nucleotides
	 * @param numSides The number of sides used to approximate the circular section
	 * @param numSegments The number of segments that form this chromosome
	 */
    public Chromosome(int type, String name, int length, int numSides, int numSegments) {
        this.type = type;
        this.name = name;
        this.length = length;
        this.numSides = numSides;
        this.numSegments = numSegments;
    }

    /**
	 * Returns the transform necessary to "push out" or "bring in" a segment. It is generally called with
	 * increasing values of count and a constant max to give an impression of movement.
	 * @param in Whether the movement is inwards (true) or outwards(false)
	 * @param count The frame count
	 * @param max The maximum number of frames
	 * @return The transform
	 */
    public abstract Transform3D getDistanceTransform(boolean in, int count, int max);

    /**
	 * Returns the transform that initially places a segment on the chromosome
	 * @return The transform
	 */
    public abstract Transform3D getInitialTransform();

    /**
	 * Returns the transform that is used to incrementally modify the initial transform and place the other segments
	 * @return The transform
	 */
    public abstract Transform3D getModifierTransform();

    /**
	 * Returns a chromosome segment of the appropriate type and default length
	 * @return A new segment, used either to create the chromosome or in insertion/deletion events
	 */
    public abstract ChromosomeSegment getSegment();

    /**
	 * Returns a chromosome segment of the appropriate type and arbitrary length
	 * @param length The length of the segment, which will be scaled accordingly
	 * @return A new segment, used either to create the chromosome or in insertion/deletion events
	 */
    public abstract ChromosomeSegment getSegment(int length);

    /**
	 * Returns a parameter that identifies the size of this chromosome in the screen
	 * @return The size
	 */
    public abstract float getSize();

    /**
	 * Returns the transform necessary to place any element at the chosen site along the chromosome
	 * @param site The nucleotide on the chromosome on which the element will be placed
	 * @return The transform
	 */
    public abstract Transform3D getTransform(int site);

    public abstract Transform3D getTransform(int site, int width);

    /**
	 * Changes the chromosome to represent an inversion between orStart and orEnd.<br>
	 * Max is the total number of frames that the animation is run for, and count represents the frame that will be represented after the call to this method.
	 * @param orStart The starting point of the inversion
	 * @param orEnd The ending point of the inversion
	 * @param count The frame out of 'max' frames, the function must be called with count = 0 before any other value.
	 * @param max The total number of frames that form the animation
	 */
    public abstract void animateInversion(int orStart, int orEnd, int count, int max);

    /**
	 * Changes the chromosome to represent a transposition between start and end to insertion.<br>
	 * Max is the total number of frames that the animation is run for, and count represents the frame that will be represented after the call to this method.
	 * @param start The starting point of the transposition
	 * @param end The ending point of the transposition
	 * @param insertion The insertion point of the transposition
	 * @param count The frame out of 'max' frames, the function must be called with count = 0 before any other value.
	 * @param max The total number of frames that form the animation
	 */
    public abstract void animateTransposition(int start, int end, int insertion, float distance, int count, int max);

    public abstract void calculateBreakPoints(LinkedList<MutationEvent> path);

    public abstract void displayName(boolean displayName);

    public abstract void showGap(int gapIndex, int howMany, float initialSectionLength, float gapLength, float finalSectionLength);

    public Appearance getDefaultAppearance() {
        Appearance app = new Appearance();
        app.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_LINE_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
        app.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        app.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
        TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.FASTEST, 0f);
        ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        app.setTransparencyAttributes(ta);
        return app;
    }

    /**
	 * Returns the length of the chromosome in nucleotides
	 * @return The length
	 */
    public int getLength() {
        return length;
    }

    /**
	 * Returns the name of the chromosome
	 * @return The name
	 */
    public String getName() {
        return name;
    }

    public BranchGroup getSegmentBranchGroup(TransformGroup ttg) {
        BranchGroup bg = new BranchGroup();
        bg.addChild(ttg);
        return bg;
    }

    public TransformGroup getSegmentTransformGroup(int location, int width, ChromosomeSegment segment) {
        Transform3D transform = getTransform(location, width);
        TransformGroup ttg = new TransformGroup(transform);
        ttg.addChild(segment);
        ttg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        ttg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        ttg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        return ttg;
    }

    /**
	 * Changes the breakpoints used to color this chromosome, if the solid or random schemes are being used
	 * the breakpoints specify the points at which color must change.
	 * @param breakPoints The new break points, it must be an ordered array.
	 */
    public void setBreakPoints(int breakPoints[]) {
        this.breakPoints = breakPoints;
        colorChromosome();
    }

    /**
	 * Changes the coloring model used to color the Chromosome
	 * @param mode The new mode, see Chromosome for a description
	 */
    public void setColoring(int colorMode, Color3f colors[]) {
        this.colorMode = colorMode;
        this.colors = colors;
        colorChromosome();
    }

    /**
	 * Changes the heatmap associated with this chromosome. Heatmaps are only used
	 * in combination with textures to represent features smaller than the default segment length
	 */
    public void setHeatMap(float heatmap[]) {
        this.heatmap = heatmap;
    }

    /**
	 * Changes the length of the chromosome. This method shouldn't be used once the chromosome has been created.
	 * @param length The new length
	 */
    public void setLength(int length) {
        this.length = length;
    }

    /**
	 * Enables / disables lighting on the 3D model
	 * @param enable Lighting is enabled if true
	 */
    public void setLighting(boolean enable) {
        if (segments == null) return;
        if (enable) {
            if (multipleApps) for (int i = 0; i < segments.length; i++) segments[i].getAppearance().setMaterial(new Material()); else segments[0].getAppearance().setMaterial(new Material());
        } else {
            try {
                if (multipleApps) for (int i = 0; i < segments.length; i++) segments[i].getAppearance().setMaterial(null); else segments[0].getAppearance().setMaterial(null);
            } catch (NullPointerException npe) {
            }
        }
    }

    /**
	 * Changes the name of the chromosome
	 * @param name The new name
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * Sets the maximum number of markers for insertions, deletions and conversions.<br>
	 * It will include substitutions when they are migrated to regular markers, instead of what is used now.
	 * @param maxInsertionMarkers The new maximum number of insertion markers
	 * @param maxDeletionMarkers The new maximum number of deletion markers
	 * @param maxConversionMarkers The new maximum number of gene conversion markers
	 * @return
	 */
    public void setNumberOfMarkers(int maxInsertionMarkers, int maxDeletionMarkers, int maxConversionMarkers, int maxSubstitutionMarkers) {
        int maxInds[] = { maxInsertionMarkers, maxDeletionMarkers, maxConversionMarkers, maxSubstitutionMarkers };
        for (int type = 0; type < maxInds.length; type++) {
            if (maxInds[type] > 0) {
                BranchGroup newMarkers[] = new BranchGroup[maxInds[type]];
                if (markers[type] != null) {
                    int i, ind = 0;
                    for (i = markerInds[type]; ind <= maxInds[type]; ind++) {
                        newMarkers[ind] = markers[type][i];
                        i = (i - 1) % markers[type].length;
                    }
                    while (i != markerInds[type]) {
                        markers[type][i].detach();
                        i = (i - 1) % markers[type].length;
                    }
                    markerInds[type] = ind;
                } else markerInds[type] = 0;
                markers[type] = newMarkers;
            } else markers[type] = null;
        }
    }

    /**
	 * Enables / disables the use of textures to color the chromosome. It must be called after setColoring.
	 * @param enable Textures are enabled if true
	 */
    public void setTextures(boolean enable) {
        useTextures = enable;
        try {
            if (useTextures && textures != null) {
                for (int i = 0; i < segments.length; i++) segments[i].getAppearance().setTexture(textures[i]);
            } else {
                if (multipleApps) for (int i = 0; i < segments.length; i++) segments[i].getAppearance().setTexture(null); else segments[0].getAppearance().setTexture(null);
            }
        } catch (NullPointerException npe) {
        }
    }

    /**
	 * Enables / disables a wireframe representation of the 3D models
	 * @param mode Wireframe mode is enabled if true
	 */
    public void setWireframe(boolean enable) {
        if (segments == null) return;
        Appearance app;
        try {
            if (enable) {
                if (multipleApps) {
                    for (int i = 0; i < segments.length; i++) {
                        app = segments[i].getAppearance();
                        app.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_BACK, 0));
                        app.setLineAttributes(new LineAttributes(2, LineAttributes.PATTERN_SOLID, false));
                    }
                } else {
                    app = segments[0].getAppearance();
                    app.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_BACK, 0));
                    app.setLineAttributes(new LineAttributes(2, LineAttributes.PATTERN_SOLID, false));
                }
            } else {
                if (multipleApps) for (int i = 0; i < segments.length; i++) segments[i].getAppearance().setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK, 0)); else segments[0].getAppearance().setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK, 0));
            }
        } catch (NullPointerException npe) {
        }
    }

    /**
	 * Shows a gen conversion between left and right.<br>
	 * Max is the total number of frames that the animation is run for, and count represents the frame that will be represented after the call to this method.
	 * @param left The starting point of the gene conversion
	 * @param right The ending point of the gene conversion
	 * @param data An additional text message that is included shown when the marker is clicked
	 * @param color The color used for the conversion marker
	 * @param count The frame out of 'max' frames
	 * @param max The total number of frames that form the animation
	 */
    public void animateConversion(int left, int right, String data, Color color, int count, int max) {
        int site = (int) ((float) (left + right) / 2);
        if (count == 0) {
            String string = "Conversion: " + left + ", " + right + "\n" + data;
            createMarker(site, CONVERSION, color, string);
            tg.addChild(createEventSection(site, right - left, color));
        }
        if (markers[CONVERSION] != null) fade(count, max, true, markerApp);
        fade(count, max, true, eventSectionApp);
        Transform3D placement = getTransform(site);
        Transform3D distance = getDistanceTransform(true, count, max);
        placement.mul(distance);
        eventSectionTransformGroup.setTransform(placement);
        if (count == max) eventSectionBranchGroup.detach();
    }

    /**
	 * Shows a deletion between left and right.<br>
	 * Currently, deletions are only "shown" and don't have any effect on the aspect of the chromosome. 
	 * Due to the small size of most deletions, most wouldn't be perceptible, so for the time being it will probably stay like this.<br>
	 * Max is the total number of frames that the animation is run for, and count represents the frame that will be represented after the call to this method.
	 * @param left The starting point of the deleted section
	 * @param right The ending point of the deleted section
	 * @param color The color used for the deletion marker
	 * @param count The frame out of 'max' frames
	 * @param max The total number of frames that form the animation
	 */
    public void animateDeletion(int left, int right, Color color, int count, int max) {
        int site = (int) ((left + right) / 2f);
        if (count == 0) {
            String string = "Deletion: " + left + ", " + right;
            createMarker(site, DELETION, color, string);
            tg.addChild(createEventSection(site, right - left, color));
        }
        if (markers[DELETION] != null) fade(count, max, true, markerApp);
        fade(count, max, false, eventSectionApp);
        Transform3D placement = getTransform(site);
        Transform3D distance = getDistanceTransform(false, count, max);
        placement.mul(distance);
        eventSectionTransformGroup.setTransform(placement);
        if (count == max) eventSectionBranchGroup.detach();
    }

    /**
	 * Shows an insertion at site.<br>
	 * Currently, insertions are only "shown" and don't have any effect on the aspect of the chromosome. 
	 * Due to the small size of most insertions, most wouldn't be perceptible, so for the time being it will probably stay like this.<br>
	 * Max is the total number of frames that the animation is run for, and count represents the frame that will be represented after the call to this method.
	 * @param site The site at which the insertion is shown
	 * @param sequence The nucleotide sequence that is inserted, it's displayed on a message when the marker is clicked
	 * @param color The color used for the insertion marker
	 * @param count The frame out of 'max' frames
	 * @param max The total number of frames that form the animation
	 */
    public void animateInsertion(int site, String sequence, Color color, int count, int max) {
        site = (int) ((site + sequence.length()) / 2f);
        if (count == 0) {
            String string = "Insertion: " + site + ", " + sequence;
            createMarker(site, INSERTION, color, string);
            tg.addChild(createEventSection(site, sequence.length(), color));
        }
        if (markers[INSERTION] != null) fade(count, max, true, markerApp);
        fade(count, max, true, eventSectionApp);
        Transform3D placement = getTransform(site);
        Transform3D distance = getDistanceTransform(true, count, max);
        placement.mul(distance);
        eventSectionTransformGroup.setTransform(placement);
        if (count == max) eventSectionBranchGroup.detach();
    }

    /**
	 * Shows a nucleotide substitution at site.<br>
	 * Max is the total number of frames that the animation is run for, and count represents the frame that will be represented after the call to this method.
	 * @param site The site at which the substitution is shown
	 * @param color The color used for the substitution marker
	 * @param count The frame out of 'max' frames
	 * @param max The total number of frames that form the animation
	 */
    public void animateSubstitution(int site, char oldBase, char newBase, Color color, int count, int max) {
        if (count == 0) {
            String string = "Nucleotide substitution\n" + site + ": " + oldBase + " -> " + newBase;
            createMarker(site, SUBSTITUTION, color, string);
        }
        if (markers[SUBSTITUTION] != null) fade(count, max, true, markerApp);
    }

    /**
	 * Builds the scenegraph that represents the chromosome
	 * @param mode The coloring mode
	 * @param breakPoints The points at which the chromosome is broken. See the constructor.
	 */
    protected void buildChromosome() {
        segments = new ChromosomeSegment[numSegments];
        transformGroups = new TransformGroup[numSegments];
        tempTransformGroups = new TransformGroup[numSegments];
        currentPos = new int[numSegments];
        temp = new int[numSegments];
        tempTransform = new Transform3D();
        tr = new Transform3D();
        Transform3D initialTransform = getInitialTransform();
        Transform3D modifierTransform = getModifierTransform();
        int start = 0, segmentLength;
        for (int i = 0; i < numSegments; i++) {
            segments[i] = getSegment();
            segments[i].setPickable(true);
            transformGroups[i] = new TransformGroup(initialTransform);
            transformGroups[i].addChild(segments[i]);
            transformGroups[i].setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            transformGroups[i].setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
            transformGroups[i].setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
            tg.addChild(transformGroups[i]);
            currentPos[i] = i;
            segmentLength = (int) (((i + 1) / (float) numSegments) * length) - start;
            if (segmentLength <= 0) segmentLength = 1;
            segments[i].position = start;
            segments[i].length = segmentLength;
            segments[i].id = String.valueOf(i);
            start += segmentLength;
            initialTransform.mul(modifierTransform);
        }
    }

    /**
	 * Colors the chromosome using its colors, coloring scheme and texture attributes
	 */
    protected void colorChromosome() {
        if (colors == null) return;
        Appearance app = null;
        multipleApps = heatmap != null;
        if (multipleApps) {
            Color tempColors[] = new Color[colors.length];
            for (int i = 0; i < tempColors.length; i++) tempColors[i] = colors[i].get();
            TextureCreator tc = new TextureCreator(Chromosome.TEXTURE_WIDTH, Chromosome.TEXTURE_HEIGHT, heatmap, tempColors);
            textures = new Texture2D[numSegments];
            for (int i = 0; i < textures.length; i++) {
                textures[i] = tc.getTexture(segments[i].position, segments[i].position + segments[i].length, length);
                if (useTextures) {
                    app = getDefaultAppearance();
                    app.setTexture(textures[i]);
                    segments[i].setAppearance(app);
                }
            }
        }
        app = getDefaultAppearance();
        ColorCreator cc = new ColorCreator(numSegments, colors);
        Color3f color = new Color3f(Color.black);
        if (colorMode == COLORING_GRADIENT || (breakPoints != null)) color = cc.getNextColor(); else if (colorMode == COLORING_RANDOM) color = new Color3f((float) Math.random(), (float) Math.random(), (float) Math.random());
        int breakInd = 0;
        for (int i = 0; i < segments.length; i++) {
            ChromosomeSegment ts = segments[i];
            if (!multipleApps) ts.setAppearance(app);
            if (colorMode == COLORING_GRADIENT) ts.setColor(cc.getLastColor(), cc.getNextColor()); else {
                if (colorMode == COLORING_SOLID) cc.getNextColor();
                if (breakPoints == null || breakPoints.length == 0) ts.setColor(color); else {
                    if (breakPoints[breakInd] == i) {
                        if (colorMode == COLORING_RANDOM) color = new Color3f((float) Math.random(), (float) Math.random(), (float) Math.random()); else color = cc.getNextColor();
                        breakInd = (breakInd + 1) % breakPoints.length;
                    }
                    ts.setColor(color);
                }
            }
        }
    }

    /**
	 * Creates a "slice" of chromosome that can be inserted or taken from it at the desired site. The slice is a TorusSegment
	 * @param site The position in the chromosome where the slice is created
	 * @param angle The angle covered by the segment
	 * @param invert Invert the angle. Not used, will dissappear
	 * @param color The color of the slice
	 */
    protected BranchGroup createEventSection(int site, int length, Color color) {
        Transform3D transform = getTransform(site);
        ChromosomeSegment eventSection = getSegment(length);
        eventSectionApp = getDefaultAppearance();
        eventSectionApp.setColoringAttributes(new ColoringAttributes(new Color3f(color), ColoringAttributes.SHADE_FLAT));
        eventSection.setColor(new Color3f(color));
        eventSection.setAppearance(eventSectionApp);
        eventSectionTransformGroup = new TransformGroup(transform);
        eventSectionTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        eventSectionTransformGroup.addChild(eventSection);
        eventSectionBranchGroup = new BranchGroup();
        eventSectionBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
        eventSectionBranchGroup.addChild(eventSectionTransformGroup);
        return eventSectionBranchGroup;
    }

    /**
	 * Creates a Marker object at the desired position
	 * @param site The position in the chromosome
	 * @param type The type of marker, used to store in the proper array
	 * @param color The color of the marker
	 * @param string The string that will be displayed when the marker is clicked
	 */
    protected void createMarker(int site, int type, Color color, String string) {
        if (markers[type] == null) return;
        int index = locationToIndex(site);
        markerApp = new Appearance();
        markerApp.setColoringAttributes(new ColoringAttributes(new Color3f(color), ColoringAttributes.SHADE_FLAT));
        TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.FASTEST, 1f);
        ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
        markerApp.setTransparencyAttributes(ta);
        EventMarker marker = new EventMarker(0.03f, string);
        marker.setAppearance(markerApp);
        marker.setPickable(true);
        boolean internal = type != SUBSTITUTION;
        BranchGroup markerBranchGroup = segments[currentPos[index]].addMarker(site, marker, internal);
        transformGroups[currentPos[index]].addChild(markerBranchGroup);
        if (markers[type][markerInds[type]] != null) markers[type][markerInds[type]].detach();
        markers[type][markerInds[type]] = markerBranchGroup;
        markerInds[type] = (markerInds[type] + 1) % markers[type].length;
    }

    /** 
	 * Changes the transparency attribute of the provided Appearance. Usually called
	 * with growing values of count to give an impression of fading
	 * @param count The current frame count
	 * @param max The maximum number of frames to achieve the effect
	 * @param in When true, the object fades in as count grows bigger, otherwise it fades out
	 * @param all The appearance whose TransparencyAttributes will be changed
	 */
    protected void fade(int count, int max, boolean in, Appearance app) {
        float transparency = (float) count / (float) max;
        ;
        if (in) transparency = 1f - transparency;
        app.getTransparencyAttributes().setTransparency(transparency);
    }

    public int howMany(int width) {
        int avgLength = length / numSegments;
        if (width < avgLength * 0.1) return 0;
        int howMany;
        for (howMany = 1; howMany < numSegments; howMany++) if (width / howMany < avgLength * 1.3) break;
        return howMany;
    }

    /**
	 * Initializes some of the internal structures, needs to be called from the subclasses' constructors after they have
	 * initialized their own values.
	 */
    protected void initialize() {
        tg = new TransformGroup();
        addChild(tg);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        buildChromosome();
        breakPoints = null;
        markers = new BranchGroup[4][];
        markerInds = new int[4];
        setNumberOfMarkers(10, 10, 10, 3);
    }

    /**
	 * Inserts a value in an ordered array and returns the new number of elements in the array.<br>
	 * Used to create the break point list.
	 * @param value The value to be inserted, it is discarded if it's already foudn in the array
	 * @param array The array in which the value is inserted
	 * @param size The current number of elements in the array
	 * @return The new number of elements. It's size if value was already in array, size+1 otherwise
	 */
    protected int insert(int value, int array[], int size) {
        if (size >= array.length - 1) return size;
        int pos = 0;
        while (pos < size && array[pos] < value) pos++;
        if (array[pos] == value) return size;
        int counter = size;
        while (counter >= pos) {
            array[counter + 1] = array[counter];
            counter--;
        }
        array[pos] = value;
        return size + 1;
    }

    public int[] insertSegment(int location, int width, Color color) {
        return insertSegment(location, width, color, 1);
    }

    public int[] insertSegment(int location, int width, Color color, int howMany) {
        numSegments += howMany;
        ChromosomeSegment newSegments[] = new ChromosomeSegment[numSegments];
        TransformGroup newTransformGroups[] = new TransformGroup[numSegments];
        tempTransformGroups = new TransformGroup[numSegments];
        temp = new int[numSegments];
        int newPos[] = new int[numSegments];
        for (int i = 0; i < segments.length; i++) {
            newSegments[i] = segments[i];
            newTransformGroups[i] = transformGroups[i];
        }
        int index = locationToIndex(location);
        for (int i = 0; i < index; i++) newPos[i] = currentPos[i];
        for (int i = 0; i < howMany; i++) newPos[index + i] = newSegments.length - howMany + i;
        for (int i = index + howMany; i < newPos.length; i++) newPos[i] = currentPos[i - howMany];
        segments = newSegments;
        transformGroups = newTransformGroups;
        currentPos = newPos;
        int indexes[] = new int[howMany];
        Appearance app = getDefaultAppearance();
        int start = location;
        int remainWidth = width;
        for (int i = 0; i < howMany; i++) {
            index = numSegments - howMany + i;
            int thisWidth = remainWidth / (howMany - i);
            remainWidth -= thisWidth;
            ChromosomeSegment segment = getSegment(thisWidth);
            segment.setColor(new Color3f(color));
            segment.setPickable(true);
            segment.id = ("Inserted");
            segment.position = start;
            segment.length = thisWidth;
            TransformGroup ttg = getSegmentTransformGroup(start, width, segment);
            segments[index] = segment;
            transformGroups[index] = ttg;
            if (multipleApps) segment.setAppearance(getDefaultAppearance()); else segment.setAppearance(app);
            tg.addChild(getSegmentBranchGroup(ttg));
            start += thisWidth;
            indexes[i] = index;
        }
        return indexes;
    }

    public int locationToIndex(int location) {
        int index;
        for (index = 0; index < segments.length; index++) if (location < segments[currentPos[index]].position) break;
        return index - 1;
    }

    /**
	 * Prints the elements of an array.<br>
	 * Used for debugging purposes to print the current position of each segment
	 * @param array
	 */
    protected void printArray(int array[]) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) System.out.print(array[i] + ", ");
            System.out.println();
        }
    }

    public void removeSegment(int location) {
        numSegments--;
        tempTransformGroups = new TransformGroup[numSegments];
        temp = new int[numSegments];
        ChromosomeSegment newSegments[] = new ChromosomeSegment[numSegments];
        TransformGroup newTransformGroups[] = new TransformGroup[numSegments];
        int newPos[] = new int[numSegments];
        int index = locationToIndex(location);
        int deleted = currentPos[index];
        for (int i = 0; i < index; i++) newPos[i] = currentPos[i];
        for (int i = index; i < newPos.length; i++) newPos[i] = currentPos[i + 1];
        for (int i = 0; i < newPos.length; i++) if (newPos[i] > deleted) newPos[i]--;
        for (int i = 0; i < deleted; i++) {
            newSegments[i] = segments[i];
            newTransformGroups[i] = transformGroups[i];
        }
        for (int i = deleted; i < newSegments.length; i++) {
            newSegments[i] = segments[i + 1];
            newTransformGroups[i] = transformGroups[i + 1];
        }
        segments = newSegments;
        transformGroups = newTransformGroups;
        currentPos = newPos;
    }

    /**
	 * Rotates the whole chromosome and all its associated objects
	 * @param angle The angle that the chromosome is rotated
	 */
    public void rotate(double angle) {
        Transform3D tr = new Transform3D();
        tg.getTransform(tr);
        Transform3D rotation = new Transform3D();
        rotation.setRotation(new AxisAngle4d(0, 1, 0, angle));
        tr.mul(rotation);
        tg.setTransform(tr);
    }

    public void showGap(int location, int width, int frameCount, int maxCount, boolean opening) {
        showGap(location, width, frameCount, maxCount, opening, 1);
    }

    public void showGap(int location, int width, int frameCount, int maxCount, boolean opening, int howMany) {
        float mult = frameCount / (float) maxCount;
        if (!opening) mult = 1 - mult;
        float gapWidth = width / (float) length * mult;
        float rest = 1 - gapWidth;
        float initialWidth = location / (float) length - gapWidth / 2;
        float finalWidth = 1 - initialWidth - gapWidth;
        int index = locationToIndex(location);
        showGap(index, howMany, initialWidth, gapWidth, finalWidth);
    }

    public void test(TestEvent event, int frameCount, int max) {
        int location = event.int1;
        int width = event.int2;
        int index = locationToIndex(location);
        int howMany = Integer.parseInt(event.str1);
        if (frameCount == 0) {
            length += width;
            tempIndices = insertSegment(location, width, Color.green, howMany);
            updateLocation();
        } else showGap(location, width, frameCount, max, true, howMany);
        for (int i = 0; i < tempIndices.length; i++) {
            fade(frameCount, max, true, segments[tempIndices[i]].getAppearance());
            Transform3D placement = getTransform(segments[tempIndices[i]].position, width);
            placement.mul(getDistanceTransform(true, frameCount, max));
            transformGroups[tempIndices[i]].setTransform(placement);
        }
        if (frameCount == max && !multipleApps) {
            Appearance app = segments[0].getAppearance();
            for (int i = 0; i < tempIndices.length; i++) segments[tempIndices[i]].setAppearance(app);
        }
    }

    /**
	 * Updates the starting nucleotide of the segments between start and end.
	 * @param start The starting index of the segments to be updated
	 * @param end The ending index of the segments to be updated
	 */
    protected void updateLocation() {
        int soFar = 0;
        for (int i = 0; i < segments.length; i++) {
            segments[currentPos[i]].position = soFar;
            soFar += segments[currentPos[i]].length;
        }
        length = soFar;
    }
}
