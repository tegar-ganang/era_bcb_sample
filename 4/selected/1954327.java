package net.sourceforge.olduvai.lrac.drawer.structure;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.media.opengl.GL;
import javax.swing.event.EventListenerList;
import net.sourceforge.jglchartutil.GLChart;
import net.sourceforge.jglchartutil.datamodels.AbstractDataSet;
import net.sourceforge.jglchartutil.datamodels.AbstractDrawableDataSeries;
import net.sourceforge.jglchartutil.datamodels.TimeDataSet;
import net.sourceforge.olduvai.accordiondrawer.AccordionDrawer;
import net.sourceforge.olduvai.accordiondrawer.CellGeom;
import net.sourceforge.olduvai.accordiondrawer.GridCell;
import net.sourceforge.olduvai.accordiondrawer.SplitAxis;
import net.sourceforge.olduvai.accordiondrawer.SplitLine;
import net.sourceforge.olduvai.lrac.DataGrid;
import net.sourceforge.olduvai.lrac.TimeRangeSampleIntervalRelation;
import net.sourceforge.olduvai.lrac.drawer.AccordionLRACDrawer;
import net.sourceforge.olduvai.lrac.drawer.CellChartHelper;
import net.sourceforge.olduvai.lrac.drawer.Groups;
import net.sourceforge.olduvai.lrac.drawer.queries.DetailQuery;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.StripChannelBinding;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Representation;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Template;
import net.sourceforge.olduvai.lrac.genericdataservice.cellviewer.CellEventListener;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.DetailQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.records.DetailRecord;
import net.sourceforge.olduvai.lrac.genericdataservice.records.SwatchRecordInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.DataCellInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;

/**
 * Class representing a 'cell' in the LiveRAC layout.  This cell contains the 
 * semantic zoom object for this particular metric, along with any local/cached
 * data needed to rebuild this visualization on a resize event between data refreshes.  
 * 
 * TODO: memory performance if we start running into memory problems, having to store too much info 
 * for charts, we could kick all the chart variables into a new class which then 
 * only gets instantiated if a chart is to be drawn for this object.  Its wasteful
 * to have a lot of storage allocated when only a handful of Cells are going 
 * to have actual chart data in them. This increases pointer indirection as a consequence, however.
 * 
 * TODO: performance.  We can eliminate object creation of the rectangle and dimension objects 
 * by allocating storage for them and re-using these objects as needed.  This increases 
 * memory overhead but reduces object creation during rendering.  
 * 
 * @author Peter McLachlan (spark343@cs.ubc.ca)
 *
 */
public class Cell implements DataCellInterface, CellGeom, Cloneable {

    private static final double[] DEFAULTDRAWINGRANGE = new double[] { 0.0, 1.0 };

    /**
	 * The swatch stores the underlying data that is used to determine how the cell
	 * will appear at the lowest level of zoom ("colored box").  It contains the
	 * most significant value and the number of times that value is seen.  This value
	 * can then be interpreted into a category by the Strip object, and rendered into
	 * a color using the associated Template.  
	 */
    SwatchRecordInterface swatch = null;

    /**
	 * Pointer to the device monitoring this metric
	 */
    SourceInterface source;

    /**
	 * A collection containing a chart, data set & the representation that was used to generate it
	 */
    RepChart repChart = null;

    /**
	 * Keep track of chart data
	 */
    Map<StripChannelBinding, AbstractDrawableDataSeries> bindingDataMap;

    /**
	 * Track any listeners for this cell
	 */
    EventListenerList listenerList;

    /**
	 * If a request has already been made for chart data, don't ask for more 
	 * until it has been serviced.  
	 */
    boolean chartDataRequested = false;

    boolean newData = false;

    long lastDetailQueryId = -1l;

    ;

    /**
	 * The bundle provides all the metadata for describing how the charts 
	 * for this object should be drawn
	 * 
	 */
    Strip strip;

    /**
	 * Check whether data should be removed if this cell is beneath the minimum size. 
	 */
    public void checkCleanData() {
        Representation rep = getTemplate().getRepresentation(getCellPixelSize());
        if (rep == null || rep.getChartType() == -1) {
            clearData();
        }
    }

    private Dimension getCellPixelSize() {
        return getCellPixelSize(computeCellRect());
    }

    public void addCellListener(CellEventListener listener) {
        if (listener == null) return;
        if (listenerList == null) listenerList = new EventListenerList();
        listenerList.add(CellEventListener.class, listener);
    }

    public void removeCellListener(CellEventListener listener) {
        if (listenerList == null) {
            System.err.println("Attempting to remove listener '" + listener + "' from cell '" + this + "' but no listenerList exists");
            return;
        }
        if (listener != null) {
            listenerList.remove(CellEventListener.class, listener);
        }
        if (listenerList.getListenerCount() == 0) listenerList = null;
    }

    /**
	 * 
	 * @param source
	 * @param channelID
	 */
    public Cell(SourceInterface source, Strip strip) {
        if (source == null && strip == null) return;
        this.source = source;
        this.strip = strip;
        if (strip == null) {
            Exception e = new Exception("Cell:" + this + " null strip?");
            e.printStackTrace();
        }
    }

    /**
	 * Returns the index number of this Metric in the global ordering of metrics.  
	 * Use getPosition() instead.  
	 * @deprecated
	 * @return index number
	 */
    public int getKey() {
        return getPosition();
    }

    /**
	 * Returns the index number of this Cell in the global ordering of strips.
	 *  
	 * @return index number
	 */
    public int getPosition() {
        return source.getLRD().getStripAxis().getSplitIndex(strip.getMinLine());
    }

    /**
	 * @return String name of the strip for this cell
	 */
    public String getName() {
        return strip.getName();
    }

    /**
	 * Process new stat data to add to the chart for this object.
	 * Then, draw the chart  
	 * 
	 * Note: assumes previous data has been cleared
	 * @param set
	 * @return
	 */
    public boolean addProcessedDetailResults(DetailRecord resultSet, long updateNumber) {
        if (updateNumber != lastDetailQueryId) {
            bindingDataMap = new HashMap<StripChannelBinding, AbstractDrawableDataSeries>();
            this.lastDetailQueryId = updateNumber;
        }
        final StripChannelBinding binding = strip.getChannelBinding(resultSet.getInputChannelItem());
        final AbstractDrawableDataSeries drawableSeries = getStrip().makeDrawableSeries(resultSet.getSeries(), resultSet.getInputChannelItem());
        bindingDataMap.put(binding, drawableSeries);
        chartDataRequested = false;
        newData = true;
        checkCleanData();
        return true;
    }

    public AccordionLRACDrawer getLRD() {
        return source.getLRD();
    }

    public Template getTemplate() {
        return strip.getTemplate();
    }

    /**
	 * Oh Cell! Draw thyself! 
	 *   
	 * @param col
	 * @param plane
	 * @param drawingRange
	 * @param doFlash
	 */
    public void drawInCell(double plane, double[] drawingRange, boolean doFlash) {
        if (source == null || strip == null || swatch == null) {
            System.err.println("Could not find source, swatch or strip for cell: " + toString());
            return;
        }
        AccordionLRACDrawer.countDrawnFrame++;
        final DataGrid ds = DataGrid.getInstance();
        final AccordionLRACDrawer drawer = getLRD();
        final GL gl = (GL) drawer.getGL();
        final Rectangle2D rect = computeCellRect(drawingRange);
        final Dimension cellPixelDims = getCellPixelSize(rect);
        final Color col = getColor(cellPixelDims);
        drawer.setColorGL(col);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex3d(rect.getMinX(), rect.getMinY(), plane);
        gl.glVertex3d(rect.getMinX(), rect.getMaxY(), plane);
        gl.glVertex3d(rect.getMaxX(), rect.getMaxY(), plane);
        gl.glVertex3d(rect.getMaxX(), rect.getMinY(), plane);
        gl.glEnd();
        final Representation rep = getTemplate().getRepresentation(cellPixelDims);
        if (rep != null && rep.getChartType() != -1) {
            if (ds.isTimeChanged()) {
                chartDataRequested = false;
                dataRequest(ds.getSampleInterval());
                drawChart(rect, cellPixelDims);
                CellChartHelper.drawLoadingDialog(drawer, rect, plane);
            } else {
                if (!hasData()) {
                    dataRequest(ds.getSampleInterval());
                    CellChartHelper.drawLoadingDialog(drawer, rect, plane);
                } else {
                    drawChart(rect, cellPixelDims);
                    if (chartDataRequested) CellChartHelper.drawLoadingDialog(drawer, rect, plane);
                }
            }
        } else {
            clearData();
        }
    }

    private Rectangle2D computeCellRect() {
        return computeCellRect(DEFAULTDRAWINGRANGE);
    }

    private Rectangle2D computeCellRect(double[] drawingRange) {
        final int X = AccordionDrawer.X;
        final int Y = AccordionDrawer.Y;
        final double cellMin[] = { getMin(X), getMin(Y) };
        final double cellMax[] = { getMax(X), getMax(Y) };
        final double cellHeightRange = cellMax[Y] - cellMin[Y];
        cellMax[Y] = cellMin[Y] + drawingRange[1] * cellHeightRange;
        cellMin[Y] = cellMin[Y] + drawingRange[0] * cellHeightRange;
        final double cellWidthRange = cellMax[X] - cellMin[X];
        return new Rectangle2D.Double(cellMin[X], cellMin[Y], cellWidthRange, cellHeightRange);
    }

    /**
	 * Given the cell rectangle, return dimension of cell in pixels. 
	 *  
	 * @param cellRect
	 * @return Dimension encoding the size of this cell in pixels (screen space)
	 */
    private Dimension getCellPixelSize(Rectangle2D cellRect) {
        final AccordionLRACDrawer drawer = getLRD();
        return new Dimension(drawer.w2s(cellRect.getWidth(), AccordionDrawer.X), drawer.w2s(cellRect.getHeight(), AccordionDrawer.Y));
    }

    /**
	 * Retrieve the color of this cell
	 * @return
	 */
    public Color getColor() {
        return getColor(getCellPixelSize(computeCellRect()));
    }

    public Color getRawColor() {
        return getStrip().getThresholdColor(getSigValue());
    }

    /**
	 * Retrieves the color for the cell given the cells Dimension in pixels (screen space)
	 * 
	 * @param cellPixelDim Cells dimensions in pixels.
	 * @return
	 */
    public Color getColor(Dimension cellPixelDim) {
        final int pixelSize = (int) Math.round(getLRD().getMinCellDims(AccordionDrawer.X));
        return strip.getThresholdColor(strip.getSigValue(swatch), cellPixelDim, pixelSize);
    }

    @SuppressWarnings("unused")
    private void drawLabelBox(String label, Font f, double[] cellMin, double[] cellMax) {
        AccordionLRACDrawer d = source.getLRD();
        d.drawLabelBox(label, f, cellMin, cellMax, true);
    }

    private void returnChart(AccordionLRACDrawer drawer, RepChart repChart) {
        drawer.getCr().remChart(repChart.chart);
        strip.delStripChart(this);
        repChart.rep.returnChart(repChart.chart);
    }

    /**
	 * Build and place a request for chart data in the queue.  
	 * @param timeRangeSampleIntervalRelation Type of aggregation to request (none, hourly, daily)
	 * 
	 */
    private void dataRequest(TimeRangeSampleIntervalRelation timeRangeSampleIntervalRelation) {
        if (chartDataRequested) return;
        DetailQueryInterface req = new DetailQuery(source, strip, getBeginDate(), getEndDate(), timeRangeSampleIntervalRelation, DataGrid.getNextDetailQueryId());
        if (req != null) {
            chartDataRequested = true;
            getDataGrid().requestData(req);
        } else {
            System.err.println("Null chart request for: " + toString());
        }
    }

    public Date getBeginDate() {
        return getDataGrid().getBeginDate();
    }

    public Date getEndDate() {
        return getDataGrid().getEndDate();
    }

    public AbstractDataSet getChartDataSet() {
        if (repChart != null) {
            return repChart.chartDataSet;
        }
        return null;
    }

    private DataGrid getDataGrid() {
        return DataGrid.getInstance();
    }

    public boolean hasData() {
        if (bindingDataMap == null) return false;
        return true;
    }

    public void drawChart(Rectangle2D cellRect, Dimension cellPixelDim) {
        if (!hasData()) return;
        final AccordionLRACDrawer drawer = (AccordionLRACDrawer) source.getLRD();
        final GL gl = drawer.getGL();
        final Template template = strip.getTemplate();
        final Representation rep = template.getRepresentation(cellPixelDim);
        if ((rep == null || rep.getChartType() == -1)) {
            clearData();
            return;
        }
        GLChart chart = null;
        AbstractDataSet chartDataSet = null;
        if (repChart == null || rep != repChart.rep) {
            if (repChart != null) {
                returnChart(drawer, repChart);
                chartDataSet = repChart.chartDataSet;
                repChart = null;
            } else {
                chartDataSet = new TimeDataSet();
                for (Iterator<AbstractDrawableDataSeries> it = bindingDataMap.values().iterator(); it.hasNext(); ) {
                    chartDataSet.addSeries(it.next());
                }
            }
            chart = CellChartHelper.makeChart(rep, chartDataSet, bindingDataMap, getDateRange(), gl);
            repChart = new RepChart(rep, chart, chartDataSet);
            drawer.getCr().addChart(chart);
            strip.addStripChart(this, chart);
        } else {
            chart = repChart.chart;
            chartDataSet = repChart.chartDataSet;
            chartDataSet.removeAll();
            for (Iterator<AbstractDrawableDataSeries> it = bindingDataMap.values().iterator(); it.hasNext(); ) {
                chartDataSet.addSeries(it.next());
            }
            if (newData) {
                CellChartHelper.setChartData(rep, chart, chartDataSet, bindingDataMap, getDateRange(), gl);
            }
        }
        final double stripMinDataValue = strip.getMinDataValue();
        final double stripMaxDataValue = strip.getMaxDataValue();
        chartDataSet.setFixedDataFrame(stripMinDataValue, stripMaxDataValue);
        if (chart == null) {
            System.err.println("Null chart for: " + toString());
            return;
        }
        chart.setSize(cellPixelDim);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslated(cellRect.getMinX(), cellRect.getMaxY(), source.getLRD().getObjplane());
        gl.glScaled(1.0d, -1.0d, 1.0d);
        gl.glScaled(cellRect.getWidth() / cellPixelDim.getWidth(), cellRect.getHeight() / cellPixelDim.getHeight(), 1);
        chart.draw(gl);
        gl.glPopMatrix();
        newData = false;
    }

    /**
	 * Clear or reset all class data structures 
	 *
	 */
    private void clearData() {
        if (repChart != null) {
            returnChart(source.getLRD(), repChart);
        }
        repChart = null;
        bindingDataMap = null;
    }

    /**
	 * Creates a text label for this metric.
	 *  
	 * @return
	 */
    @SuppressWarnings("unused")
    private String getLabel() {
        if (swatch != null) return Double.toString(strip.getSigValue(getSwatch()));
        return "0";
    }

    /**
	 * @return A GridCell object configured for this Metric.  
	 */
    public GridCell getCell() {
        GridCell fakeCell = new GridCell(source.getLRD());
        for (int xy = 0; xy <= AccordionDrawer.Y; xy++) {
            fakeCell.setMinLine(getMinLine(xy), xy);
            fakeCell.setMaxLine(getMaxLine(xy), xy);
        }
        return fakeCell;
    }

    public SplitLine getMinLine(int xy) {
        if (xy == AccordionDrawer.X) {
            return strip.getMinLine();
        }
        return source.getMinLine();
    }

    public SplitLine getMaxLine(int xy) {
        if (xy == AccordionDrawer.X) {
            SplitAxis axis = source.getLRD().getSplitAxis(xy);
            return strip.getMaxLine(axis);
        }
        return source.getMaxLine();
    }

    public double getMin(int xy) {
        AccordionDrawer ad = source.getLRD();
        return ad.getSplitAxis(xy).getAbsoluteValue(getMinLine(xy), ad.getFrameNum());
    }

    public double getMax(int xy) {
        AccordionDrawer ad = source.getLRD();
        return ad.getSplitAxis(xy).getAbsoluteValue(getMaxLine(xy), ad.getFrameNum());
    }

    public String toString() {
        return source.getName() + "@" + strip.getName();
    }

    /**
	 * simpler cell draw call 
	 *
	 */
    public void drawInCell() {
        final double plane = source.getLRD().getObjplane();
        final double[] drawingRange = { 0d, 1d };
        drawInCell(plane, drawingRange, false);
    }

    /**
	 * @deprecated
	 */
    public void drawInCell(Color c, double plane) {
        System.out.println("not called by anything");
    }

    /**
	 * @deprecated
	 */
    public void drawInCell(double plane, boolean doFlash) {
        System.out.println("MetricCell.drawInCell(double plane, boolean doFlash) not called by anything");
    }

    /**
	 * @deprecated
	 */
    public boolean pick(int x, int y) {
        System.out.println("MetricCell.pick(x,y) not called by anything");
        return false;
    }

    /**
	 * Checks if this metric cell is critical, if it is, add it to the critical group
	 *
	 */
    public void checkAddToCritGroup() {
        if (strip.isHighestThreshold(swatch)) {
            final int myPosition = getPosition();
            if (myPosition < -1) {
                Exception e = new Exception("Position out of bounds for cell: " + toString());
                e.printStackTrace();
                return;
            }
            source.getLRD().getGroupRanges().addNodesToGroup(source, myPosition, myPosition, Groups.critGroup);
        }
    }

    private Date[] getDateRange() {
        return DataGrid.getInstance().getTimeRange();
    }

    public SourceInterface getSource() {
        return source;
    }

    /**
	 * Assigns the swatch object to this cell that contains the most
	 * significant value for the selected time range, as well as the number
	 * of times this critical value was encountered.  This can then be 
	 * interpreted by the Strip object and rendered using the Template object.
	 * 
	 * Note: if a StripChannelGroup is selected as the primary input channel 
	 * for the strip, it MAY be valid for multiple swatch records to be assigned to 
	 * the cell. When this occurs, only 1 swatch record is actually preserved, 
	 * depending on the swatchSigValue.    
	 * 
	 * @param newSwatch
	 */
    public void setSwatch(SwatchRecordInterface newSwatch) {
        if (this.swatch == null) {
            this.swatch = newSwatch;
            return;
        }
        if (newSwatch.getTimeStamp() > this.swatch.getTimeStamp()) {
            this.swatch = newSwatch;
            return;
        }
        this.swatch = strip.compareSwatches(this.swatch, newSwatch);
    }

    /**
	 * Shortcut for retrieving the sig value for the swatch assigned to this cell.
	 *  
	 * @return double representing 'significant' value for this swatch.  Double.NaN
	 * if no swatch is assigned to this Cell.
	 */
    public double getSigValue() {
        if (getSwatch() == null) return Double.NaN;
        return strip.getSigValue(getSwatch());
    }

    public Strip getStrip() {
        return strip;
    }

    public SwatchRecordInterface getSwatch() {
        return swatch;
    }

    public int getThreshIndex() {
        try {
            return strip.getThresholdIndex(swatch);
        } catch (NullPointerException e) {
            System.err.println("Null pointer for cell: " + this);
            System.out.println("Strip: " + strip);
            System.out.println("Swatch:" + swatch);
            return -1;
        }
    }

    static class RepChart {

        public RepChart(Representation rep, GLChart chart, AbstractDataSet dataset) {
            this.rep = rep;
            this.chart = chart;
            this.chartDataSet = dataset;
        }

        GLChart chart;

        Representation rep;

        AbstractDataSet chartDataSet;
    }

    /**
	 * Returns true if there are charts to draw
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
    public boolean setChartMark(int mouseX, int mouseY) {
        if (repChart == null) return false;
        final int X = AccordionDrawer.X;
        final AccordionLRACDrawer drawer = getLRD();
        final double cellMinX = getMin(X);
        final double mouseWorldX = drawer.s2w(mouseX, X) - cellMinX;
        final double chartSelectedValue = repChart.chart.getValueAtCoordinate(drawer.w2s(mouseWorldX, X), X);
        drawer.getCr().moveUpdate(repChart.chart, chartSelectedValue);
        return true;
    }

    public boolean setChartMarkedRange(int[] dragStart, int[] dragEnd) {
        if (repChart == null) return false;
        final int X = AccordionDrawer.X;
        final int Y = AccordionDrawer.Y;
        final AccordionLRACDrawer drawer = getLRD();
        if (dragStart == null && dragEnd == null) {
            drawer.getCr().markedRangeUpdate(null);
            return true;
        } else if (dragStart == null || dragEnd == null) {
            return false;
        }
        final SplitAxis sly = drawer.getSplitAxis(Y);
        final SplitLine[] rowLines = { source.getMinLine(), sly.getNextSplit(source.getMinLine()) };
        final double cellMin[] = { getMin(AccordionDrawer.X), sly.getAbsoluteValue(rowLines[0], drawer.getFrameNum()) };
        final double[] mStartCoords = { drawer.s2w(dragStart[X], X) - cellMin[X], drawer.s2w(dragStart[Y], Y) - cellMin[Y] };
        final double[] mEndCoords = { drawer.s2w(dragEnd[X], X) - cellMin[X], drawer.s2w(dragEnd[Y], Y) - cellMin[Y] };
        final double[] chartSelectedValues = { repChart.chart.getValueAtCoordinate(drawer.w2s(mStartCoords[X], X), X), repChart.chart.getValueAtCoordinate(drawer.w2s(mEndCoords[X], X), X) };
        drawer.getCr().markedRangeUpdate(chartSelectedValues);
        return true;
    }

    public void setSource(SourceInterface source) {
        this.source = source;
    }

    public void setStrip(Strip strip) {
        this.strip = strip;
    }

    public Map<StripChannelBinding, AbstractDrawableDataSeries> getBindingDataMap() {
        return bindingDataMap;
    }

    public long getLastDetailQueryId() {
        return lastDetailQueryId;
    }
}
