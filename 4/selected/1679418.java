package net.sourceforge.olduvai.lrac.drawer.structure.strips;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sourceforge.jglchartutil.GLChart;
import net.sourceforge.jglchartutil.datamodels.AbstractDataSet;
import net.sourceforge.jglchartutil.datamodels.AbstractDrawableDataSeries;
import net.sourceforge.jglchartutil.datamodels.DataSeries2D;
import net.sourceforge.jglchartutil.datamodels.SimpleDataSeries;
import net.sourceforge.olduvai.accordiondrawer.SplitAxis;
import net.sourceforge.olduvai.accordiondrawer.SplitLine;
import net.sourceforge.olduvai.lrac.DataGrid;
import net.sourceforge.olduvai.lrac.LiveRAC;
import net.sourceforge.olduvai.lrac.drawer.structure.Cell;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Template;
import net.sourceforge.olduvai.lrac.genericdataservice.records.SwatchRecordInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.DataCellInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.StructuralObjectInterface;
import net.sourceforge.olduvai.lrac.logging.Loggable;
import org.jdom.Element;

/**
 * A palette object stores information mapping real data values to a template
 * and specifies an (optional) mathematical expression on the raw input data
 * before mapping to the template.
 * 
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 * 
 */
public class Strip implements Comparable<Strip>, Loggable, StructuralObjectInterface {

    static final String ROOTTAG = "strip";

    static final String NAMETAG = "name";

    static final String TEMPLATETAG = "template";

    static final String INPUTCHANNELTAG = "inputchannel";

    static final String THRESHOLDTAG = "threshold";

    static final String THRESHOLDVALUETAG = "threshvalue";

    static final String THRESHOLDINDEXTAG = "threshindex";

    static final String AGGREGATORTAG = "aggregator";

    static final String ACTIVETAG = "active";

    static final String SWATCHSIGFUNCTIONTAG = "sigfunction";

    static final String BOUNDCHANNELTAG = "boundchannel";

    public static final int HIGHESTPRIORITYSLOT = 0;

    /**
	 * Selected function for retrieving significant value
	 * 
	 */
    public static final int MEANFUNC = 0;

    public static final int MINFUNC = 1;

    public static final int MAXFUNC = 2;

    public static final int TOTALFUNC = 3;

    public static final String[] FUNCTIONNAMES = { "Mean", "Min", "Max", "Total" };

    private static final String DEFAULTNEWSTRIPNAME = "New inactive strip";

    static final double computeSig(double min, double max, double total, double numvalues, int sigFunction) {
        if (sigFunction == MEANFUNC) return total / numvalues; else if (sigFunction == MINFUNC) return min; else if (sigFunction == MAXFUNC) return max; else return total;
    }

    static final int getThresholdIndex(Element thresholdEl) {
        Element indexEl = thresholdEl.getChild(THRESHOLDINDEXTAG);
        if (indexEl == null) return 0;
        return Integer.parseInt(indexEl.getTextTrim());
    }

    static final Double getThresholdValue(Element thresholdEl) {
        Element valueEl = thresholdEl.getChild(THRESHOLDVALUETAG);
        if (valueEl == null) return 0d;
        return Double.parseDouble(valueEl.getTextTrim());
    }

    boolean useFixedDataFrame = true;

    private double minDataValue = Double.POSITIVE_INFINITY;

    private double maxDataValue = Double.NEGATIVE_INFINITY;

    private int prevHashCode = -1;

    /**
	 * Keeps a record of which cells contain chart objects.  
	 * Management of this data structure is left to the cells which must
	 * add or remove charts from this mappign as they are created / destroyed
	 */
    HashMap<Cell, GLChart> stripCharts = new HashMap<Cell, GLChart>();

    /**
	 * Specifies the minimum line boundary of this Strip.  Not saved to the backing store
	 * this field is bound when the Strip is added to the grid and cleared when it
	 * is removed.   
	 */
    private SplitLine minLine;

    /**
	 * Specifies whether this Strip has had a binding operation performed on it yet
	 */
    private boolean bound;

    /**
	 * Name of this strip
	 */
    String name;

    /**
	 * This is used during the 'binding' stage to select the correct template to
	 * bind to.
	 */
    String templateInternalName;

    /**
	 * Handle to the correct Template for this Strip. Null until the binding
	 * stage is complete.
	 */
    Template template;

    boolean active = true;

    int sigFunction = 0;

    List<StripChannelBinding> channelBindings = new ArrayList<StripChannelBinding>();

    /**
	 * This gets populated AFTER the strip object is bound.  It is used for fast lookups
	 * of a StripChannelBinding.  
	 */
    private Map<InputChannelItemInterface, StripChannelBinding> channelBindingMap;

    /**
	 * Sorted array of thresholds in order of significance. This means that if
	 * the function being used is MINFUNCTION then the LOWEST value is actually
	 * the "most significant". If any other function is being used than the
	 * HIGHEST value is "most significant" in order.
	 *   
	 * thresholds[0] = least_significant_value
	 * thresholds[length - 1] = most_significant_value
	 */
    double[] thresholds = new double[0];

    ArrayList<DataAggregator> dataAggregators = new ArrayList<DataAggregator>();

    /**
	 * Creates a new inactive strip with default settings. 
	 *
	 * @param name
	 */
    public Strip(String name) {
        this.name = name;
        this.active = false;
    }

    /**
	 * Creates a Strip from an XML file.
	 * 
	 * @param root
	 * @param templates
	 * 
	 */
    @SuppressWarnings("unchecked")
    public Strip(Element root) {
        List l = root.getChildren();
        Element e;
        Iterator it = l.iterator();
        ArrayList<Double> tempThresholds = new ArrayList<Double>(10);
        while (it.hasNext()) {
            e = (Element) it.next();
            if (e.getName().equals(NAMETAG)) {
                name = e.getTextTrim();
            } else if (e.getName().equals(TEMPLATETAG)) {
                templateInternalName = e.getTextTrim();
            } else if (e.getName().equals(INPUTCHANNELTAG)) {
                StripChannelBinding p = new StripChannelBinding(e, this);
                channelBindings.add(p);
            } else if (e.getName().equals(THRESHOLDTAG)) {
                final Double threshValue = getThresholdValue(e);
                final int threshIndex = getThresholdIndex(e);
                tempThresholds.add(threshIndex, threshValue);
            } else if (e.getName().equals(AGGREGATORTAG)) {
                DataAggregator da = new DataAggregator(e);
                dataAggregators.add(da);
            } else if (e.getName().equals(ACTIVETAG)) {
                active = Boolean.parseBoolean(e.getTextTrim());
            } else if (e.getName().equals(SWATCHSIGFUNCTIONTAG)) {
                sigFunction = Integer.parseInt(e.getTextTrim());
            } else if (e.getName().equals(BOUNDCHANNELTAG)) {
                StripChannelBinding b = new StripChannelBinding(e, this);
                channelBindings.add(b);
            } else {
                System.err.println("Strip XML load: unhandled child node: " + e);
            }
        }
        thresholds = new double[tempThresholds.size()];
        for (int i = 0; i < tempThresholds.size(); i++) {
            thresholds[i] = tempThresholds.get(i);
        }
        Collections.sort(channelBindings);
    }

    public Strip() {
        this(DEFAULTNEWSTRIPNAME);
    }

    public void addAggregator(DataAggregator da) {
        dataAggregators.add(da);
    }

    /**
	 * Associates an {@link InputChannelItemInterface} to this strip by means of a StripChannelBinding.  
	 * This associates the {@link InputChannelItemInterface} to a specific slot on this strip's template
	 * as well as providing some user defined information to override channel defaults.  
	 *  
	 * @param b
	 */
    public void addChannelBinding(StripChannelBinding b) {
        if (!channelBindings.contains(b)) channelBindings.add(b);
        Collections.sort(channelBindings);
    }

    public void removeChannelBinding(StripChannelBinding b) {
        channelBindings.remove(b);
        Collections.sort(channelBindings);
    }

    public void removeChannelBinding(int slotIndex) {
        channelBindings.remove(slotIndex);
    }

    /**
	 * Adds a chart to the strip chart tracker
	 * 
	 * @param cell
	 * @param chart
	 */
    public void addStripChart(Cell cell, GLChart chart) {
        stripCharts.put(cell, chart);
    }

    public void clearChartList() {
        stripCharts.clear();
    }

    /**
	 * This function can be called when there are two swatches for the same 
	 * cell and it needs to be determined which contains the more "significant" 
	 * (defined as "most severe") condition.  
	 * 
	 * This comparator is only valid for comparing two SwatchRecordInterface
	 * that are using the same sigFunction. It is possible to do otherwise but
	 * the results are undefined. The comparison operates on the output of
	 * getSigValue() from each CellSwatchRecord. The sigfunction from the first
	 * parameter is used.
	 * 
	 * Returns the more significant CellSwatchRecord based on the sigfunction.
	 * 
	 */
    public SwatchRecordInterface compareSwatches(SwatchRecordInterface c1, SwatchRecordInterface c2) {
        if (getSigValue(c1) == getSigValue(c2)) return c1;
        if (getSigFunction() == MINFUNC) {
            if (getSigValue(c1) < getSigValue(c2)) return c2; else return c1;
        } else {
            if (getSigValue(c1) < getSigValue(c2)) return c1; else return c2;
        }
    }

    private void computeMinMaxDataValuesThisFrame() {
        final int stripChartsHash = stripCharts.hashCode();
        if (this.prevHashCode == stripChartsHash) {
            return;
        }
        minDataValue = Double.POSITIVE_INFINITY;
        maxDataValue = Double.NEGATIVE_INFINITY;
        Iterator<GLChart> it = getStripChartIterator();
        while (it.hasNext()) {
            final AbstractDataSet chartDataSet = it.next().getDataSet();
            final double nextMinValue = chartDataSet.getDsMin(GLChart.Y);
            final double nextMaxValue = chartDataSet.getDsMax(GLChart.Y);
            if (nextMinValue < minDataValue) minDataValue = nextMinValue;
            if (nextMaxValue > maxDataValue) maxDataValue = nextMaxValue;
        }
        this.prevHashCode = stripCharts.hashCode();
    }

    /**
	 * Removes the chart for the specified cell from the tracker if it exists.
	 * Does nothing if no chart exists for the specified cell.
	 * 
	 * @param cell
	 */
    public void delStripChart(DataCellInterface cell) {
        stripCharts.remove(cell);
    }

    /**
	 * This is the fast way to get a channel binding.  Only returns bound channel bindings.
	 * @param inputChannel
	 * @return
	 */
    public StripChannelBinding getChannelBinding(InputChannelItemInterface inputChannel) {
        return channelBindingMap.get(inputChannel);
    }

    /**
	 * This is the slow way to get a channel binding.  But it can return unbound channel bindings.
	 * General {@link #getChannelBinding(InputChannelItemInterface)} is preferred.  
	 * 
	 * @param channelName
	 * @return
	 */
    public StripChannelBinding getChannelBinding(String channelName) {
        Iterator<StripChannelBinding> it = channelBindings.iterator();
        while (it.hasNext()) {
            StripChannelBinding channel = it.next();
            if (channel.getBoundChannelName().equals(channelName) || channel.getUserLabel().equals(channelName)) return channel;
        }
        return null;
    }

    public List<StripChannelBinding> getChannelBindings() {
        return channelBindings;
    }

    /**
	 * Retrieves a list of channels to which the StripChannelBindings 
	 * are bound. 
	 * 
	 * @return
	 */
    public List<InputChannelItemInterface> getChannels() {
        List<InputChannelItemInterface> channelList = new ArrayList<InputChannelItemInterface>(channelBindings.size());
        for (Iterator<StripChannelBinding> it = channelBindings.iterator(); it.hasNext(); ) {
            final StripChannelBinding b = it.next();
            if (b.getBoundChannel() != null) {
                channelList.add(b.getBoundChannel());
            }
        }
        return channelList;
    }

    /**
	 * Retrieves the channel name at the specified index
	 * 
	 * @param channelIndex
	 * @return null
	 */
    public String getChannelName(int channelIndex) {
        try {
            return getChannelBindings().get(channelIndex).boundChannelName;
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<DataAggregator> getDataAggregators() {
        return dataAggregators;
    }

    public StripChannelBinding getFirstChannel() {
        if (getChannelBindings().size() == 0) return null;
        return getChannelBindings().get(0);
    }

    public String getInternalName() {
        return name;
    }

    public String getInternalType() {
        return null;
    }

    public String getLogString() {
        StringBuilder b = new StringBuilder(name + "( ");
        Iterator<StripChannelBinding> it = getChannelBindings().iterator();
        while (it.hasNext()) {
            final String channelName = it.next().boundChannelName;
            b.append(channelName + ", ");
        }
        b.append(")");
        return b.toString();
    }

    public double getMaxDataValue() {
        computeMinMaxDataValuesThisFrame();
        return maxDataValue;
    }

    public double getMinDataValue() {
        computeMinMaxDataValuesThisFrame();
        return minDataValue;
    }

    public String getName() {
        return name;
    }

    public int getSigFunction() {
        return sigFunction;
    }

    /**
	 * Retrieves the significant value for a given swatch, this is then used to
	 * match against the thresholds.
	 * 
	 * @param swatch
	 * @return
	 */
    public double getSigValue(SwatchRecordInterface swatch) {
        return computeSig(swatch.getMinValue(), swatch.getMaxValue(), swatch.getTotalValue(), swatch.getNumValues(), sigFunction);
    }

    /**
	 * Retrieve the strip input parameter for the specified slot.
	 * 
	 * @param slot
	 *            The slot # 0 - n
	 * @return
	 */
    public StripChannelBinding getChannelBinding(int slot) {
        try {
            StripChannelBinding p = channelBindings.get(slot);
            if (p == null) throw new IndexOutOfBoundsException();
            return p;
        } catch (IndexOutOfBoundsException e) {
            return new StripChannelBinding(slot);
        }
    }

    public Iterator<Cell> getStripChartCellIterator() {
        return stripCharts.keySet().iterator();
    }

    /**
	 * Returns an iterator over the charts asociated with this strip
	 * 
	 * @return
	 */
    private Iterator<GLChart> getStripChartIterator() {
        return stripCharts.values().iterator();
    }

    public Template getTemplate() {
        return template;
    }

    /**
	 * Use the list of thresholds and associated template to retrieve a color
	 * for the significant value.
	 * 
	 * @param sigValue
	 * @return
	 */
    public Color getThresholdColor(double sigValue) {
        final int thresholdIndex = getThresholdIndex(sigValue);
        return template.getThresholdColor(thresholdIndex);
    }

    public Color getThresholdColor(double sigValue, Dimension cellDim, int blockSize) {
        final int thresholdIndex = getThresholdIndex(sigValue);
        return template.getThresholdColor(thresholdIndex, cellDim, blockSize);
    }

    public Color getThresholdColor(int thresholdIndex, float saturation) {
        return template.getThresholdColor(thresholdIndex, saturation);
    }

    /**
	 * Tricky: there should always be one more color value from Template than
	 * there are swatches to handle the case where the value is not significant
	 * at all...
	 * 
	 * @param sigValue
	 * @return
	 */
    public int getThresholdIndex(double sigValue) {
        if (getSigFunction() == MINFUNC) {
            for (int i = thresholds.length - 1; i >= 0; i--) {
                if (thresholds[i] >= sigValue) return i;
            }
            return thresholds.length - 1;
        } else {
            for (int i = thresholds.length - 1; i >= 0; i--) {
                if (thresholds[i] <= sigValue) return i;
            }
            return thresholds.length - 1;
        }
    }

    public int getThresholdIndex(SwatchRecordInterface swatch) {
        return getThresholdIndex(getSigValue(swatch));
    }

    public double[] getThresholds() {
        return thresholds;
    }

    /**
	 * Not implemented for strips
	 */
    public String getType() {
        return null;
    }

    /**
	 * Retrieve a complete XML description of this structural object.
	 * 
	 * @return
	 */
    public Element getXML() {
        Element rootElement = new Element(ROOTTAG);
        Element swatchSigEl = new Element(SWATCHSIGFUNCTIONTAG);
        swatchSigEl.setText(Integer.toString(getSigFunction()));
        rootElement.addContent(swatchSigEl);
        Iterator<DataAggregator> aggIt = dataAggregators.iterator();
        while (aggIt.hasNext()) {
            rootElement.addContent(aggIt.next().getXML());
        }
        Iterator<StripChannelBinding> boundIt = channelBindings.iterator();
        while (boundIt.hasNext()) {
            StripChannelBinding c = boundIt.next();
            rootElement.addContent(c.getXML());
        }
        for (int threshCount = 0; threshCount < thresholds.length; threshCount++) {
            final double threshValue = thresholds[threshCount];
            final Element threshRoot = new Element(THRESHOLDTAG);
            final Element threshIndex = new Element(THRESHOLDINDEXTAG);
            threshIndex.setText(Integer.toString(threshCount));
            final Element threshValueEl = new Element(THRESHOLDVALUETAG);
            threshValueEl.setText(Double.toString(threshValue));
            threshRoot.addContent(threshIndex);
            threshRoot.addContent(threshValueEl);
            rootElement.addContent(threshRoot);
        }
        Element activeEl = new Element(ACTIVETAG);
        activeEl.setText(Boolean.toString(active));
        rootElement.addContent(activeEl);
        Element templateEl = new Element(TEMPLATETAG);
        if (template == null) templateEl.setText(""); else templateEl.setText(template.getTemplateName());
        rootElement.addContent(templateEl);
        Element stripTitleEl = new Element(NAMETAG);
        stripTitleEl.setText(name);
        rootElement.addContent(stripTitleEl);
        return rootElement;
    }

    /**
	 * A strip must be both bound, and flagged active in order to be used in the grid
	 * @return
	 */
    public boolean isActive() {
        return active && bound;
    }

    public boolean isHighestThreshold(SwatchRecordInterface swatch) {
        final int threshIndex = getThresholdIndex(getSigValue(swatch));
        if (threshIndex >= thresholds.length - 1) return true;
        return false;
    }

    public boolean isUseFixedDataFrame() {
        return useFixedDataFrame;
    }

    public void removeDataAggregator(DataAggregator da) {
        dataAggregators.remove(da);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setInternalName(String internalName) {
        this.name = internalName;
    }

    /**
	 * TODO: implement types of strip?
	 */
    public void setInternalType(String type) {
    }

    public void setName(String stripTitle) {
        this.name = stripTitle;
    }

    public void setSigFunction(int function) {
        this.sigFunction = function;
    }

    public void setTemplate(Template stripTemplate) {
        this.template = stripTemplate;
    }

    public void setThresholds(double[] thresholds) {
        if (thresholds == null || thresholds.length == 0) return;
        this.thresholds = thresholds;
        if (isActive() && isBound()) {
            final DataGrid dg = DataGrid.getInstance();
            dg.fireCellContentsChange();
        }
    }

    public void setType(String type) {
    }

    public void setUseFixedDataFrame(boolean useFixedDataFrame) {
        this.useFixedDataFrame = useFixedDataFrame;
    }

    public String toString() {
        return getName();
    }

    public SplitLine getMinLine() {
        return minLine;
    }

    public void setMinLine(SplitLine minLine) {
        this.minLine = minLine;
    }

    /**
	 * Retrieve the InputChannelItemInterface backing a ChannelBinding 
	 * by priority.  (0 is highest priority.) 
	 * 
	 * @param index
	 * @return null if index out of range or if channelBinding is not yet 
	 * bound
	 */
    public InputChannelItemInterface getChannel(int index) {
        if (index >= channelBindings.size()) return null;
        return channelBindings.get(index).getBoundChannel();
    }

    /**
	 * Initiates a binding operation on this strip that will tie this strip to a 
	 * Template and loaded InputChannels.  
	 * 
	 * @param templateList
	 * @param channelList
	 * @param channelGroupList
	 * @return
	 */
    public boolean bind(List<Template> templateList, Map<String, InputChannelInterface> channelList, Map<String, InputChannelGroupInterface> channelGroupList) {
        for (Iterator<Template> it = templateList.iterator(); it.hasNext(); ) {
            final Template t = it.next();
            if (t.getTemplateName().equals(templateInternalName)) {
                this.template = t;
                break;
            }
        }
        if (template == null) {
            unbind();
            return false;
        }
        channelBindingMap = new HashMap<InputChannelItemInterface, StripChannelBinding>();
        for (Iterator<StripChannelBinding> it = channelBindings.iterator(); it.hasNext(); ) {
            final StripChannelBinding bc = it.next();
            if (bc.bind(channelList, channelGroupList)) channelBindingMap.put(bc.getBoundChannel(), bc);
        }
        if (channelBindingMap.size() == 0) {
            unbind();
            return false;
        } else {
            bound = true;
            return true;
        }
    }

    /**
	 * Flag this strip as unbound and clear binding state
	 */
    public void unbind() {
        for (Iterator<StripChannelBinding> it = channelBindings.iterator(); it.hasNext(); ) {
            final StripChannelBinding bc = it.next();
            bc.unbind();
        }
        channelBindingMap = null;
        bound = false;
    }

    public boolean isBound() {
        return bound;
    }

    /**
	 * 
	 * @param axis The axis on which the minline lies
	 * @return
	 */
    public SplitLine getMaxLine(SplitAxis axis) {
        return axis.getNextSplit(getMinLine());
    }

    /**
	 * Applies the 'lipstick' to this raw data series that allows it to be 
	 * drawable.  
	 * 
	 * @param simpleSeries
	 * @return
	 */
    public AbstractDrawableDataSeries makeDrawableSeries(SimpleDataSeries simpleSeries, InputChannelItemInterface channelItem) {
        if (!isActive()) {
            System.err.println("Attempt to configure series when strip is inactive.");
            return null;
        }
        StripChannelBinding binding;
        final DataSeries2D series = new DataSeries2D(simpleSeries);
        if (channelItem instanceof InputChannelGroupInterface) {
            final InputChannelGroupInterface channelGroup = (InputChannelGroupInterface) channelItem;
            binding = channelGroup.getBinding(this, getTemplate(), simpleSeries);
        } else {
            final InputChannelInterface inputChannel = (InputChannelInterface) channelItem;
            binding = channelBindingMap.get(inputChannel);
            final String userLabel = binding.getUserLabel();
            if (userLabel == null || userLabel.equals("")) series.setLabel(inputChannel.getLabel()); else series.setLabel(userLabel);
        }
        getTemplate().getCrayon(binding.getTemplateSlot()).configureSeries(series);
        series.setPriority(binding.getTemplateSlot());
        series.setUniqueID(binding.getBoundChannelName());
        return series;
    }

    /**
	 * Compares one strip to another by means of the strip's indexed position 
	 * in the list of strips being displayed.  If one strip is not active and is 
	 * not being displayed, the active strip is greater than the inactive strip.
	 * If both strips are inactive, the strips are compared by strip name.  
	 */
    public int compareTo(Strip s) {
        if (getMinLine() == null && s.getMinLine() != null) {
            return -1;
        } else if (getMinLine() != null && s.getMinLine() == null) {
            return 1;
        } else if (getMinLine() == null && s.getMinLine() == null) return getName().compareTo(s.getName());
        final SplitAxis stripAxis = LiveRAC.getInstance().getLrd().getStripAxis();
        int myIndex = stripAxis.getSplitIndex(getMinLine());
        int oIndex = stripAxis.getSplitIndex(s.getMinLine());
        if (myIndex < oIndex) return -1; else if (myIndex > oIndex) return 1; else return 0;
    }
}
