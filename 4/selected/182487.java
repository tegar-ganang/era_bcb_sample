package net.sourceforge.olduvai.lrac.drawer.strips;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.jglchartutil.AbstractDataSet;
import net.sourceforge.jglchartutil.GLChart;
import net.sourceforge.olduvai.lrac.dataservice.records.CellSwatchRecord;
import net.sourceforge.olduvai.lrac.drawer.Cell;
import net.sourceforge.olduvai.lrac.drawer.templates.Template;
import net.sourceforge.olduvai.lrac.logging.Loggable;
import org.jdom.Element;

/**
 * A palette object stores information mapping real data values to a template and 
 * specifies an (optional) mathematical expression on the raw input data before 
 * mapping to the template.  
 * 
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class Strip implements Loggable {

    static final String TITLETAG = "title";

    static final String TEMPLATETAG = "template";

    static final String INPUTCHANNELTAG = "inputchannel";

    static final String THRESHOLDTAG = "threshold";

    static final String THRESHOLDVALUETAG = "threshvalue";

    static final String THRESHOLDINDEXTAG = "threshindex";

    static final String AGGREGATORTAG = "aggregator";

    static final String ACTIVETAG = "active";

    static final String FLIPTAG = "flipthresholds";

    static final String ROOTTAG = "strip";

    static final String SWATCHSIGFUNCTIONTAG = "sigfunction";

    public static final int HIGHESTPRIORITYSLOT = 0;

    public static final String ALARMSTRIPTITLE = "Alarms";

    public static final int NUMTHRESHOLDS = 6;

    boolean useFixedDataFrame = true;

    private double minDataValue = Double.POSITIVE_INFINITY;

    private double maxDataValue = Double.NEGATIVE_INFINITY;

    private int prevHashCode = -1;

    HashMap<Cell, GLChart> stripCharts = new HashMap<Cell, GLChart>();

    /**
	 * 
	 */
    Element rootElement;

    String stripTitle;

    Element stripTitleEl;

    Template template;

    Element templateEl;

    boolean flipThresholds = false;

    Element flipThresholdsEl;

    boolean active = true;

    Element activeEl;

    int sigFunction = 0;

    Element sigFunctionEl;

    ArrayList<StripChannel> channelList = new ArrayList<StripChannel>();

    ArrayList<Float> thresholds = new ArrayList<Float>();

    ArrayList<Element> thresholdElements = new ArrayList<Element>();

    ArrayList<DataAggregator> dataAggregators = new ArrayList<DataAggregator>();

    StripHandler stripHandler;

    /**
	 * @param root
	 * @param templates 
	 * 
	 */
    public Strip(Element root, HashMap<String, Template> templateList, StripHandler handler) {
        this.rootElement = root;
        this.stripHandler = handler;
        List l = root.getChildren();
        Element e;
        Iterator it = l.iterator();
        while (it.hasNext()) {
            e = (Element) it.next();
            if (e.getName().equals(TITLETAG)) {
                stripTitleEl = e;
                stripTitle = e.getTextTrim();
            } else if (e.getName().equals(TEMPLATETAG)) {
                templateEl = e;
                template = templateList.get(e.getTextTrim());
                if (template == null) {
                    System.err.println("Strip: Null template match on:" + e.getTextTrim());
                }
            } else if (e.getName().equals(INPUTCHANNELTAG)) {
                addStripChannel(e);
            } else if (e.getName().equals(THRESHOLDTAG)) {
                thresholdElements.add(e);
            } else if (e.getName().equals(AGGREGATORTAG)) {
                addAggregator(e);
            } else if (e.getName().equals(ACTIVETAG)) {
                active = Boolean.parseBoolean(e.getTextTrim());
                this.activeEl = e;
            } else if (e.getName().equals(FLIPTAG)) {
                flipThresholds = Boolean.parseBoolean(e.getTextTrim());
                this.flipThresholdsEl = e;
            } else if (e.getName().equals(SWATCHSIGFUNCTIONTAG)) {
                sigFunction = Integer.parseInt(e.getTextTrim());
                sigFunctionEl = e;
            } else {
                System.err.println("Strip: unhandled child node: " + e);
            }
        }
        organizeThresholds();
    }

    public void setFlipThresholds(boolean status) {
        this.flipThresholds = status;
        if (flipThresholdsEl == null) {
            Element el = new Element(FLIPTAG);
            rootElement.addContent(el);
            flipThresholdsEl = el;
        }
        flipThresholdsEl.setText(Boolean.toString(status));
    }

    public void setSigFunction(int function) {
        if (sigFunctionEl != null) rootElement.removeContent(sigFunctionEl);
        Element el = new Element(SWATCHSIGFUNCTIONTAG);
        el.setText(Integer.toString(function));
        rootElement.addContent(el);
        this.sigFunction = function;
    }

    public int getSigFunction() {
        return sigFunction;
    }

    private void addAggregator(Element e) {
        DataAggregator da = new DataAggregator(e);
        dataAggregators.add(da);
    }

    /**
	 * Assumes new aggregator is not already in an XML hierarchy
	 * 
	 * @param da
	 */
    public void addAggregator(DataAggregator da) {
        rootElement.addContent(da.rootEl);
        dataAggregators.add(da);
    }

    public void removeDataAggregator(DataAggregator da) {
        if (dataAggregators.remove(da)) rootElement.removeContent(da.rootEl);
    }

    /**
	 * Called once after strip has finished loading
	 *
	 */
    private void organizeThresholds() {
        ArrayList<Float> newThresholds = new ArrayList<Float>(thresholdElements.size());
        ArrayList<Element> newThresholdElements = new ArrayList<Element>(thresholdElements.size());
        Iterator<Element> threshIt = thresholdElements.iterator();
        while (threshIt.hasNext()) {
            final Element thresh = threshIt.next();
            final Float threshValue = getThresholdValue(thresh);
            final int threshIndex = getThresholdIndex(thresh);
            newThresholdElements.add(threshIndex, thresh);
            newThresholds.add(threshIndex, threshValue);
        }
        this.thresholds = newThresholds;
        this.thresholdElements = newThresholdElements;
    }

    public void setThresholds(float[] values) {
        if (values == null || values.length == 0) return;
        if (values[0] > values[values.length - 1]) {
            setFlipThresholds(true);
        } else {
            setFlipThresholds(false);
        }
        rootElement.removeChildren(THRESHOLDTAG);
        thresholdElements.clear();
        thresholds.clear();
        for (int i = 0; i < values.length; i++) {
            final Element threshRoot = new Element(THRESHOLDTAG);
            final Element threshIndex = new Element(THRESHOLDINDEXTAG);
            threshIndex.setText(Integer.toString(i));
            final Element threshValue = new Element(THRESHOLDVALUETAG);
            threshValue.setText(Float.toString(values[i]));
            threshRoot.addContent(threshIndex);
            threshRoot.addContent(threshValue);
            rootElement.addContent(threshRoot);
            thresholds.add(values[i]);
            thresholdElements.add(threshRoot);
        }
    }

    public int getThresholdIndex(float sigValue) {
        if (flipThresholds) {
            for (int i = 0; i < thresholds.size(); i++) {
                final float value = thresholds.get(i);
                if (sigValue >= value) return i;
            }
            return thresholds.size() - 1;
        } else {
            for (int i = 0; i < thresholds.size(); i++) {
                final float value = thresholds.get(i);
                if (sigValue < value) {
                    final int result = i - 1;
                    if (result < 0) {
                        System.err.println("Swatch sig value less than defined 'base' value for strip: " + this + " sigValue: " + sigValue + " value: " + value);
                        return 0;
                    } else {
                        return result;
                    }
                }
            }
            return thresholds.size() - 1;
        }
    }

    public void setActive(boolean active) {
        rootElement.removeChild(ACTIVETAG);
        Element e = new Element(ACTIVETAG);
        e.setText(Boolean.toString(active));
        rootElement.addContent(e);
        this.active = active;
    }

    /**
	 * Use the list of thresholds and associated template to 
	 * retrieve a color for the significant value. 
	 * 
	 * @param sigValue
	 * @return
	 */
    public Color getThresholdColor(float sigValue) {
        final int thresholdIndex = getThresholdIndex(sigValue);
        return template.getThresholdColor(thresholdIndex);
    }

    public Color getThresholdColor(int thresholdIndex, float saturation) {
        return template.getThresholdColor(thresholdIndex, saturation);
    }

    public Color getThresholdColor(float sigValue, int[] diffXY, int blockSize) {
        final int thresholdIndex = getThresholdIndex(sigValue);
        return template.getThresholdColor(thresholdIndex, diffXY, blockSize);
    }

    public boolean isHighestThreshold(CellSwatchRecord swatch) {
        final int threshIndex = getThresholdIndex(swatch.getSigValue());
        if (threshIndex >= thresholds.size() - 1) return true;
        return false;
    }

    public StripChannel newStripChannel() {
        Element e = new Element(INPUTCHANNELTAG);
        rootElement.addContent(e);
        return addStripChannel(e);
    }

    public boolean deleteStripChannel(StripChannel p) {
        if (!channelList.contains(p)) return false;
        rootElement.removeContent(p.rootElement);
        return channelList.remove(p);
    }

    private StripChannel addStripChannel(Element e) {
        StripChannel p = new StripChannel(e, this);
        channelList.add(p);
        Collections.sort(channelList);
        return p;
    }

    public String toString() {
        return getStripTitle();
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template stripTemplate) {
        this.template = stripTemplate;
        rootElement.removeContent(templateEl);
        templateEl = new Element(TEMPLATETAG);
        templateEl.setText(stripTemplate.getTemplateName());
        rootElement.addContent(templateEl);
    }

    public String getStripTitle() {
        return stripTitle;
    }

    public void setStripTitle(String stripTitle) {
        this.stripTitle = stripTitle;
        rootElement.removeContent(stripTitleEl);
        stripTitleEl = new Element(TITLETAG);
        stripTitleEl.setText(stripTitle);
        rootElement.addContent(stripTitleEl);
    }

    /**
	 * Retrieve the palette's input parameter for the specified slot.  
	 * 
	 * @param slot The slot # 0 - n 
	 * @return
	 */
    public StripChannel getStripChannel(int slot) {
        try {
            StripChannel p = channelList.get(slot);
            return p;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
	 * Retrieves the channel name at the specified index 
	 * 
	 * @param channelIndex
	 * @return null
	 */
    public String getChannelName(int channelIndex) {
        try {
            return getChannelList().get(channelIndex).channelID;
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<StripChannel> getChannelList() {
        return channelList;
    }

    public StripChannel getChannel(String channelName) {
        Iterator<StripChannel> it = channelList.iterator();
        while (it.hasNext()) {
            StripChannel channel = it.next();
            if (channel.getChannelID().equals(channelName) || channel.getLabel().equals(channelName)) return channel;
        }
        return null;
    }

    public ArrayList<Float> getThresholds() {
        return thresholds;
    }

    public ArrayList<DataAggregator> getDataAggregators() {
        return dataAggregators;
    }

    public boolean isUseFixedDataFrame() {
        return useFixedDataFrame;
    }

    public void setUseFixedDataFrame(boolean useFixedDataFrame) {
        this.useFixedDataFrame = useFixedDataFrame;
    }

    public boolean isActive() {
        return active;
    }

    public String getLogString() {
        StringBuilder b = new StringBuilder(stripTitle + "( ");
        Iterator<StripChannel> it = getChannelList().iterator();
        while (it.hasNext()) {
            final String channelName = it.next().channelID;
            b.append(channelName + ", ");
        }
        b.append(")");
        return b.toString();
    }

    public StripChannel getFirstChannel() {
        if (getChannelList().size() == 0) return null;
        return getChannelList().get(0);
    }

    /**
	 * Returns a list of concrete StripChannel objects if passed a virtual channel
	 * 
	 * @param channel
	 * @return
	 */
    public List<StripChannel> getChannelGroupList(StripChannel channel) {
        final HashMap<String, StripChannelGroup> groupMap = stripHandler.getChannelGroupMap();
        final StripChannelGroup scg = groupMap.get(channel.getChannelID());
        if (scg == null) {
            System.err.println("Could not find concrete StripChannels for virtual channel: " + channel);
            return new ArrayList<StripChannel>();
        }
        return scg.getChannelNames();
    }

    public StripHandler getStripHandler() {
        return stripHandler;
    }

    static final float getThresholdValue(Element thresholdEl) {
        Element valueEl = thresholdEl.getChild(THRESHOLDVALUETAG);
        if (valueEl == null) return 0f;
        return Float.parseFloat(valueEl.getTextTrim());
    }

    static final int getThresholdIndex(Element thresholdEl) {
        Element indexEl = thresholdEl.getChild(THRESHOLDINDEXTAG);
        if (indexEl == null) return 0;
        return Integer.parseInt(indexEl.getTextTrim());
    }

    /**
	 * Adds a chart to the strip chart tracker
	 * @param cell
	 * @param chart
	 */
    public void addStripChart(Cell cell, GLChart chart) {
        stripCharts.remove(cell);
        stripCharts.put(cell, chart);
    }

    /**
	 * Removes the chart for the specified cell from the tracker if it exists.  Does nothing if no chart exists for the 
	 * specified cell.  
	 * @param cell
	 */
    public void delStripChart(Cell cell) {
        stripCharts.remove(cell);
    }

    public Iterator<Cell> getStripChartCellIterator() {
        return stripCharts.keySet().iterator();
    }

    /**
	 * Returns an iterator over the charts asociated with this strip 
	 * @return
	 */
    private Iterator<GLChart> getStripChartIterator() {
        return stripCharts.values().iterator();
    }

    public double getMaxDataValue() {
        computeMinMaxDataValuesThisFrame();
        return maxDataValue;
    }

    public double getMinDataValue() {
        computeMinMaxDataValuesThisFrame();
        return minDataValue;
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

    public void clearChartList() {
        stripCharts.clear();
    }
}
