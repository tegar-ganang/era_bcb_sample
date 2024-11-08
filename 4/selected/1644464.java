package ch.unibe.inkml;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import ch.unibe.eindermu.Messenger;
import ch.unibe.eindermu.utils.Aspect;
import ch.unibe.eindermu.utils.NotImplementedException;
import ch.unibe.eindermu.utils.Observer;
import ch.unibe.inkml.InkChannel.ChannelName;
import ch.unibe.inkml.util.Formatter;
import ch.unibe.inkml.util.Timespan;
import ch.unibe.inkml.util.TraceBound;
import ch.unibe.inkml.util.TraceVisitor;

public class InkTraceLeaf extends InkTrace {

    public static final String INKML_NAME = "trace";

    public static final String INKML_ATTR_TYPE = "type";

    public static final String INKML_ATTR_TYPE_VALUE_PENDOWN = "penDown";

    public static final String INKML_ATTR_TYPE_VALUE_PENUP = "penUp";

    public static final String INKML_ATTR_TYPE_VALUE_INDETERMINATE = "indeterminate";

    public static final String INKML_ATTR_CONTINUATION = "continuation";

    public static final String INKML_ATTR_CONTINUATION_VALUE_BEGIN = "begin";

    public static final String INKML_ATTR_CONTINUATION_VALUE_END = "end";

    public static final String INKML_ATTR_CONTINUATION_VALUE_MIDDLE = "middle";

    public static final String INKML_ATTR_PRIORREF = "priorRef";

    public static final String INKML_ATTR_BRUSHREF = "brushRef";

    public static final String INKML_ATTR_DURATION = "duration";

    public static final String INKML_ATTR_TIMEOFFSET = "timeOffset";

    public static final String ID_PREFIX = "t";

    /**
     * trace points, how they are stored in InkML
     */
    private double[][] sourcePoints;

    /**
     * trace points, how they are displayed in the canvas
     */
    private double[][] points;

    private int size = 0;

    public enum Type {

        PEN_DOWN, PEN_UP, INDETERMINATE;

        public String toString() {
            switch(this) {
                case PEN_DOWN:
                    return INKML_ATTR_TYPE_VALUE_PENDOWN;
                case PEN_UP:
                    return INKML_ATTR_TYPE_VALUE_PENUP;
                case INDETERMINATE:
                    return INKML_ATTR_TYPE_VALUE_INDETERMINATE;
                default:
                    return super.toString();
            }
        }

        public static Type getValue(String name) {
            for (Type t : Type.values()) {
                if (t.toString().equalsIgnoreCase(name)) {
                    return t;
                }
            }
            return null;
        }
    }

    ;

    /**
     * Type of this trace, this can be "penDown" | "penUp" | "indeterminate".
     * 
     * Explanation given by the InkML definition:
     * The type attribute of a Trace indicates the pen contact state (either
     * "penUp" or "penDown") during its recording. A value of "indeterminate" is
     * used if the contact-state is neither pen-up nor pen-down, and may be
     * either unknown or variable within the trace. For example, a signature may
     * be captured as a single indeterminate trace containing both the actual
     * writing and the trajectory of the pen between strokes.
     * 
     */
    private Type type;

    public enum Continuation {

        BEGIN, END, MIDDLE;

        public String toString() {
            switch(this) {
                case BEGIN:
                    return INKML_ATTR_CONTINUATION_VALUE_BEGIN;
                case END:
                    return INKML_ATTR_CONTINUATION_VALUE_END;
                case MIDDLE:
                    return INKML_ATTR_CONTINUATION_VALUE_MIDDLE;
            }
            return null;
        }

        public static Continuation getValue(String name) {
            for (Continuation t : Continuation.values()) {
                if (t.toString().equalsIgnoreCase(name)) {
                    return t;
                }
            }
            return null;
        }
    }

    ;

    /**
     * This attribute indicates whether this trace is a continuation trace, and
     * if it is the case, where this trace is located in the set of continuation
     * traces, it takes the values "begin" | "end" | "middle".
     * 
     * Explanation given by the InkML definition: If a <code>continuation</code>
     * attribute is present, it indicates that the current trace is a
     * continuation trace, i.e. its points are a temporally contiguous
     * continuation of (and thus should be connected to) another trace element.
     * The possible values of the attribute are:
     * <ul>
     * <li><code>begin</code>: the current trace is the first of the set of
     * continuation traces</li>
     * <li><code>end</code>: the current trace is the last of the set of
     * continuation traces</li>
     * <li><code>middle</code>: the current trace is a continuation trace, but
     * is neither the first nor the last in the set of traces</li>
     * </ul>
     * 
     */
    private Continuation continuation;

    private String priorRef;

    private String brushRef;

    private Double duration;

    private Double timeOffset;

    /**
     * caches the bounding time span of this trace
     */
    private Timespan cacheTimespan;

    /**
     * caches the bounding box of this trace
     */
    private TraceBound cacheBound;

    /**
     * Caches the center of gravity of this trace
     */
    private Point2D cacheCenterOfGravity = new Point2D.Double();

    /**
     * Fast access to source index
     */
    private Map<ChannelName, Integer> cacheSourceIndex;

    private boolean tainted = false;

    private InkTraceFormat targetFormat;

    public class ProxyInkTracePoint extends InkTracePoint {

        private int i = 0;

        public ProxyInkTracePoint(int index) {
            i = index;
        }

        /**
         * {@inheritDoc}
         * This method will notify the observers registered for {@link InkTrace#ON_CHANGE} on the trace responsible
         * for this point.
         */
        public void set(ChannelName channel, Object value) {
            set(channel, doubleize(channel, value));
        }

        /**
         * {@inheritDoc}
         * This method will notify the observers registered for {@link InkTrace#ON_CHANGE} on the trace responsible
         * for this point.
         */
        public void set(ChannelName name, double d) {
            points[i][getIndex(name)] = d;
            taint();
            notifyObserver(InkInk.ON_CHANGE);
        }

        public Object getObject(ChannelName name) {
            return objectify(name, get(name));
        }

        public double get(ChannelName t) {
            return points[i][getIndex(t)];
        }

        public int index() {
            return i;
        }
    }

    public InkTraceLeaf(InkInk ink, InkTraceGroup parent) {
        super(ink, parent);
        cacheSourceIndex = getSourceFormat().getIndex();
    }

    protected void initialize() {
        super.initialize();
        registerFor(ON_CHANGE, new Observer() {

            @Override
            public void notifyFor(Aspect event, Object subject) {
                renewCache();
                if (isRoot()) {
                    getInk().notifyObserver(InkInk.ON_CHANGE, subject);
                } else {
                    getParent().notifyObserver(InkTrace.ON_CHANGE, subject);
                }
                notifyObserver(InkTraceView.ON_DATA_CHANGE, subject);
            }
        });
    }

    private void taint() {
        tainted = true;
    }

    private void renewCache() {
        cacheCenterOfGravity = InkTracePoint.getCenterOfGravity(this);
        if (getPointCount() > 0) {
            if (!getTargetFormat().containsChannel(ChannelName.T)) {
                Messenger.error("point has no time coordinates can not deliver timeSpan");
            }
            int t = getIndex(ChannelName.T);
            cacheTimespan = new Timespan(points[0][t], points[size - 1][t]);
        }
        cacheBound = new TraceBound(getPoint(0));
        for (InkTracePoint p : this) {
            cacheBound.add(p);
        }
    }

    public TraceBound getBounds() {
        return cacheBound;
    }

    /**
     * Returns the center of gravity of all points
     * @return
     */
    public Point2D getCenterOfGravity() {
        return cacheCenterOfGravity;
    }

    public Timespan getTimeSpan() {
        return cacheTimespan;
    }

    public void backTransformPoints() throws InkMLComplianceException {
        getCanvasTransform().backTransform(points, sourcePoints, getTargetFormat(), getSourceFormat());
    }

    public InkCanvasTransform getCanvasTransform() {
        return this.getContext().getCanvasTransform();
    }

    /**
     * Returns the brush responsible to draw this trace
     * @return a brush
     */
    public InkBrush getBrush() {
        if (this.brushRef != null) {
            return (InkBrush) this.getInk().getDefinitions().get(this.brushRef);
        } else if (this.hasLocalContext() && this.getLocalContext().getBrush() != null) {
            return this.getLocalContext().getBrush();
        } else if (!this.isRoot()) {
            return this.getParent().getBrush();
        } else {
            return this.getContext().getBrush();
        }
    }

    /**
     * returns the format which was used to read this trace from the archive
     * and which will be used to write the trace back.
     * @return
     */
    public InkTraceFormat getSourceFormat() {
        return this.getContext().getSourceFormat();
    }

    /**
     * return the format which is used to access the trace points
     * @return
     */
    private InkTraceFormat getTargetFormat() {
        if (targetFormat == null) {
            targetFormat = getCanvasFormat();
        }
        return targetFormat;
    }

    /**
     * Returns the format which is used to access the trace points.
     * Same as {@link InkTraceLeaf#getTargetFormat()} but without caching
     * @return
     */
    public InkTraceFormat getCanvasFormat() {
        return this.getContext().getCanvasTraceFormat();
    }

    public List<InkTracePoint> getPoints() {
        List<InkTracePoint> l = new LinkedList<InkTracePoint>();
        for (int i = 0; i < getPointCount(); i++) {
            l.add(getPoint(i));
        }
        return l;
    }

    public Iterable<InkTracePoint> pointIterable() {
        return this;
    }

    public Iterator<InkTracePoint> iterator() {
        return new Iterator<InkTracePoint>() {

            private int pos = 0;

            public boolean hasNext() {
                return pos < getPointCount();
            }

            public InkTracePoint next() {
                return getPoint(pos++);
            }

            public void remove() {
                throw new NotImplementedException();
            }
        };
    }

    /**
     * returns the index of the specified channel to access to data 
     * @param name
     * @return
     */
    protected int getIndex(ChannelName name) {
        return getTargetFormat().indexOf(name);
    }

    /**
     * Turns the specified datapoint into the object which it acctualy represent.
     * This is specified by the channel specified by name
     * @param name Channel name to lookup the correc type
     * @param d value to transform
     * @return object in the correct type.
     */
    protected Object objectify(ChannelName name, double d) {
        return getTargetFormat().objectify(name, d);
    }

    /**
     * Turns the object into a double, according to the channel specified by name
     * @param name name of the channel which does the conversion
     * @param o object to convert
     * @return resulting double
     */
    protected double doubleize(ChannelName name, Object o) {
        return getTargetFormat().doubleize(name, o);
    }

    @Override
    public List<InkTracePoint> getPoints(String from, String to) {
        int f = Integer.parseInt(from) - 1;
        int t = (to != null) ? Integer.parseInt(to) : getPointCount();
        return getPoints().subList(f, t);
    }

    public int getPointCount() {
        return this.size;
    }

    public InkTracePoint getPoint(final int pos) {
        return new ProxyInkTracePoint(pos);
    }

    public void drawPolyLine(Graphics2D g) {
        Polygon p = getPolygon();
        g.drawPolyline(p.xpoints, p.ypoints, p.npoints);
    }

    public boolean isLeaf() {
        return true;
    }

    /**
     * {@inheritDoc}
     * This method will notify the observers registered for {@link InkTrace#ON_CHANGE}.
     */
    public void buildFromXMLNode(Element node) throws InkMLComplianceException {
        super.buildFromXMLNode(node);
        if (node.hasAttribute(INKML_ATTR_TYPE)) {
            this.type = Type.getValue(loadAttribute(node, INKML_ATTR_TYPE, null));
        }
        if (node.hasAttribute(INKML_ATTR_CONTINUATION)) {
            this.continuation = Continuation.getValue(node.getAttribute(INKML_ATTR_CONTINUATION));
        }
        if (this.continuation == Continuation.END || this.continuation == Continuation.MIDDLE) {
            this.priorRef = loadAttribute(node, INKML_ATTR_PRIORREF, null);
        }
        this.brushRef = loadAttribute(node, INKML_ATTR_BRUSHREF, null);
        if (node.hasAttribute(INKML_ATTR_DURATION)) {
            this.duration = Double.parseDouble(node.getAttribute(INKML_ATTR_DURATION));
        }
        if (node.hasAttribute(INKML_ATTR_TIMEOFFSET)) {
            this.duration = Double.parseDouble(node.getAttribute(INKML_ATTR_TIMEOFFSET));
        }
        final List<Formatter> formatter = new ArrayList<Formatter>();
        for (InkChannel c : this.getSourceFormat()) {
            formatter.add(c.formatterFactory());
        }
        final String input = node.getTextContent().trim();
        int total = 0;
        char[] chars = input.toCharArray();
        boolean onemore = false;
        for (int i = 0; i < chars.length; i++) {
            switch(chars[i]) {
                case ',':
                    total++;
                    onemore = false;
                    break;
                case ' ':
                case '\n':
                case '\t':
                case '\r':
                    break;
                default:
                    onemore = true;
            }
        }
        if (onemore) total++;
        addPoints(new PointConstructionBlock(total) {

            public void addPoints() throws InkMLComplianceException {
                Scanner stringScanner = new Scanner(input);
                Pattern pattern = Pattern.compile(",|F|T|\\*|\\?|[\"'!]?-?(\\.[0-9]+|[0-9]+\\.[0-9]+|[0-9]+)");
                int i = 0;
                while (true) {
                    String result = stringScanner.findWithinHorizon(pattern, input.length());
                    if (result == null) {
                        break;
                    }
                    if (result.equals(",") || i >= formatter.size()) {
                        next();
                        i = 0;
                        continue;
                    }
                    set(formatter.get(i).getChannel().getName(), formatter.get(i).consume(result));
                    i++;
                }
            }
        });
    }

    @Override
    public void exportToInkML(Element parent) throws InkMLComplianceException {
        if (this.isRoot() && parent.getNodeName().equals(InkInk.INKML_NAME) && this.getCurrentContext() != this.getInk().getCurrentContext()) {
            this.getCurrentContext().exportToInkML(parent);
        }
        if (tainted) {
            backTransformPoints();
        }
        Element traceNode = parent.getOwnerDocument().createElement(INKML_NAME);
        parent.appendChild(traceNode);
        super.exportToInkML(traceNode);
        writeAttribute(traceNode, INKML_ATTR_TYPE, this.getType().toString(), Type.PEN_DOWN.toString());
        if (getContinuation() != null) {
            writeAttribute(traceNode, INKML_ATTR_CONTINUATION, getContinuation().toString(), null);
        }
        writeAttribute(traceNode, INKML_ATTR_PRIORREF, priorRef, "");
        writeAttribute(traceNode, INKML_ATTR_BRUSHREF, brushRef, null);
        if (duration != null) writeAttribute(traceNode, INKML_ATTR_DURATION, duration.toString(), null);
        if (timeOffset != null) writeAttribute(traceNode, INKML_ATTR_TIMEOFFSET, timeOffset.toString(), null);
        StringBuffer pointString = new StringBuffer();
        List<Formatter> formatter = new ArrayList<Formatter>();
        for (InkChannel c : this.getSourceFormat()) {
            formatter.add(c.formatterFactory());
        }
        for (int i = 0; i < getPointCount(); i++) {
            for (int d = 0; d < formatter.size(); d++) {
                pointString.append(formatter.get(d).getNext(sourcePoints[i][d]));
            }
            pointString.append(",");
        }
        pointString.deleteCharAt(pointString.length() - 1);
        traceNode.setTextContent(pointString.toString());
    }

    /**
     * Specify whether this trace is a continuation trace, and
     * if it is the case, where this trace is located in the set of continuation
     * traces, it takes the values "begin" | "end" | "middle".
     * If this trace is not a continuation trace, then null is returned 
     * 
     * @see #continuation
     * @return null | "begin" | "end" | "middle".
     */
    public InkTraceLeaf.Continuation getContinuation() {
        return this.continuation;
    }

    /**
     * Returns a list containing the {@link InkTraceView} representing the points from "from" to "to"
     * @param tw {@link InkTraceViewContainer} which shall contain the new {@link InkTraceView}
     * @param from List of integers specifing the boundaries of the points 
     * @param to List of integers specifing the boundaries of the points
     * @return list of {@link InkTraceView}s
     */
    public List<InkTraceView> getSubSet(InkTraceViewContainer tw, List<Integer> from, List<Integer> to) {
        final InkTraceViewLeaf tv = new InkTraceViewLeaf(this.getInk(), tw);
        tv.setTraceDataRef(this.getIdNow(ID_PREFIX));
        if (!from.isEmpty()) {
            tv.setFrom(from.get(0).toString());
        }
        if (!to.isEmpty()) {
            tv.setTo(to.get(0).toString());
        }
        List<InkTraceView> l = new ArrayList<InkTraceView>();
        l.add(tv);
        return l;
    }

    /**
     * Returns an {@link InkTraceViewLeaf} representing all points contained by this trace
     * @return the InkTraaceViewLeaf
     */
    public InkTraceViewLeaf createView() {
        return createView(null);
    }

    /**
     * Returns an {@link InkTraceViewLeaf} representing all points contained by this trace
     * @param parent The traceView which will contain the new traceView
     * @return the InkTraceViewLeaf
     */
    public InkTraceViewLeaf createView(InkTraceViewContainer parent) {
        InkTraceViewLeaf i = new InkTraceViewLeaf(this.getInk(), parent);
        i.setTraceDataRef(this.getIdNow(ID_PREFIX));
        return i;
    }

    /**
     * Constructing method: sets the brush reponsible to draw this trace, if it is different than the
     * brush specified by the responsible context which is accesible by {@link #getContext()}
     * It is expected the this brush is registered in the <code>definitions</code> of the {@link InkInk}
     * @param b the new brush
     */
    public void setBrush(InkBrush b) {
        this.brushRef = b.getIdNow(InkBrush.ID_PREFIX);
    }

    /**
     * Returns the type of this trace. See {@link #type} for more information
     * @see #type
     * @return "penDown" | "penUp" | "indeterminate".
     */
    public InkTraceLeaf.Type getType() {
        if (this.type == null) {
            return InkTraceLeaf.Type.PEN_DOWN;
        }
        return this.type;
    }

    @Override
    public boolean isView() {
        return false;
    }

    public Polygon getPolygon() {
        int[] xpoints = new int[getPointCount()];
        int[] ypoints = new int[getPointCount()];
        int x = getIndex(ChannelName.X);
        int y = getIndex(ChannelName.Y);
        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = (int) points[i][x];
            ypoints[i] = (int) points[i][y];
        }
        return new Polygon(xpoints, ypoints, getPointCount());
    }

    /**
     * Retransforms the points from the source. This discards all changes to the trace made till now.
     * This is usefull if the {@link InkCanvasTransform} has been changed and therefor the points
     * on the canvas change their location.
     * This method will notify the observers registered for {@link InkTrace#ON_CHANGE}.
     * @throws InkMLComplianceException
     */
    public void reloadPoints() throws InkMLComplianceException {
        transform();
    }

    /**
     * Transforms the source points to the target points with the {@link InkCanvasTransform} reponsible for this trace.
     * This method will notify the observers registered for {@link InkTrace#ON_CHANGE}.
     * @throws InkMLComplianceException
     */
    private void transform() throws InkMLComplianceException {
        if (points == null) {
            points = new double[size][getTargetFormat().getChannelCount()];
        }
        getCanvasTransform().transform(sourcePoints, points, getSourceFormat(), getTargetFormat());
        notifyObserver(ON_CHANGE);
    }

    /**
     * Returns the index of the point within this trace.
     * This Method returns -1 if this point can not be found in this trace
     * 
     * @param point
     * @return int The index of the point within this trace
     */
    public int indexOfPoint(InkTracePoint point) {
        if (point instanceof ProxyInkTracePoint) {
            return ((ProxyInkTracePoint) point).index();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean testFormat(InkTraceFormat canvasTraceFormat) {
        return true;
    }

    /**
     * The point construction block encapsulate the construction of a trace
     * and ensures that at the end, the transformation is executed. 
     * @author emanuel
     *
     */
    public abstract class PointConstructionBlock {

        private int i = 0;

        public PointConstructionBlock(int length) {
            sourcePoints = new double[length][cacheSourceIndex.size()];
            size = length;
        }

        /**
         * In this methods all points are added using the methods {@link #set()}, {@link #next()}, and {@link #reduce()}
         * @throws InkMLComplianceException
         */
        public abstract void addPoints() throws InkMLComplianceException;

        /**
         * sets the value of the Channel name to the current point
         * @param name Channel name
         * @param value value of the point's channel
         */
        public void set(ChannelName name, double value) {
            sourcePoints[i][cacheSourceIndex.get(name)] = value;
        }

        /**
         * proceed to the next point. Unset, intermittent channels are filled with the value "unknown"
         * which internally is represented by Double.NaN
         */
        public void next() {
            if (i >= size) {
                throw new IndexOutOfBoundsException("Index " + i + " is larger than Bound: " + size);
            }
            i++;
            if (i < size) {
                for (int c = 0; c < sourcePoints[i - 1].length; c++) {
                    sourcePoints[i][c] = Double.NaN;
                }
            }
        }

        /**
         * If a point can not be added, because it is corrupt or so, this method can bee callen, then the
         * the length of the trace specified in the constructor is reduced by 1
         */
        public void reduce() {
            size--;
        }

        private void finish() {
        }
    }

    /**
     * Accepts an anonymous class implementing a {@link PointConstructionBlock} which when callen 
     * the method {@link PointConstructionBlock#addPoints()} adds the points.
     * This method will notify the observer registerd for {@link InkTrace#ON_CHANGE}.
     * @param pointConstructionBlock
     * @throws InkMLComplianceException 
     */
    public void addPoints(PointConstructionBlock block) throws InkMLComplianceException {
        block.addPoints();
        block.finish();
        transform();
    }

    public void accept(TraceVisitor visitor) {
        visitor.visit(this);
    }
}
