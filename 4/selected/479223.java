package com.javable.dataview;

import java.io.IOException;
import javax.swing.tree.DefaultMutableTreeNode;
import ucar.ma2.ArrayAbstract;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import com.javable.dataview.plots.ChannelAttribute;
import com.javable.dataview.plots.PlotSymbol;

/**
 * Container for all the data (i.e. model)
 */
public class DataStorage extends javax.swing.tree.DefaultTreeModel {

    private java.util.Hashtable limits;

    private DataChannel selectedChannel;

    private String delimiter;

    private String format;

    private int colorcount;

    private int symbolcount;

    /**
     * Default constructor
     */
    public DataStorage() {
        super(new DefaultMutableTreeNode(ResourceManager.getResource("Legend")));
        limits = new java.util.Hashtable();
        setLimit("MinY", Double.POSITIVE_INFINITY);
        setLimit("MaxY", Double.NEGATIVE_INFINITY);
        setLimit("MinX", Double.POSITIVE_INFINITY);
        setLimit("MaxX", Double.NEGATIVE_INFINITY);
        format = "0.##########";
        delimiter = String.valueOf('\t');
    }

    /**
     * Adds a group to the storage
     *
     * @param g a group to add
     */
    public void addGroup(DataGroup g) {
        ((DefaultMutableTreeNode) root).add(g);
        int i = getGroupsSize() - 1;
        setLimits(i);
        for (int j = 1; j < getChannelsSize(i); j++) {
            decorateChannel(g, getChannel(i, j));
        }
        int[] ind = new int[1];
        ind[0] = getGroupsSize() - 1;
        nodesWereInserted((DefaultMutableTreeNode) root, ind);
        ind = new int[1];
        ind[0] = getChannelsSize(i) - 1;
        nodesWereInserted((DefaultMutableTreeNode) root.getChildAt(i), ind);
    }

    /**
     * Adds a channel to the specific group in the storage
     *
     * @param c channel to add
     * @param g target group
     */
    public void addChannel(DataChannel c, int g) {
        ((DataGroup) root.getChildAt(g)).addChannel(c);
        decorateChannel((DataGroup) root.getChildAt(g), c);
        setLimits(g);
        int[] ind = new int[1];
        ind[0] = getChannelsSize(g) - 1;
        nodesWereInserted((DefaultMutableTreeNode) root.getChildAt(g), ind);
    }

    /**
     * Returns group from this storage
     *
     * @param g group index
     * @throws ClassCastException if object is not of type DataGroup
     * @return DataGroup for index
     */
    public DataGroup getGroup(int g) throws ClassCastException {
        return (DataGroup) root.getChildAt(g);
    }

    /**
     * Returns number of groups in this storage
     *
     * @return number of groups
     */
    public int getGroupsSize() {
        return root.getChildCount();
    }

    /**
     * Returns channel in the group from this storage
     *
     * @return DataChannel for index
     * @param c channel index in group g
     * @param g group index
     * @throws ClassCastException if object is not of type DataChannel
     */
    public DataChannel getChannel(int g, int c) throws ClassCastException {
        return ((DataGroup) root.getChildAt(g)).getChannel(c);
    }

    /**
     * Returns node that keeps channel in the group from this storage
     *
     * @return DataChannel for index
     * @param c channel index in group g
     * @param g group index
     * @throws ClassCastException if object is not of type
     *         DefaultMutableTreeNode
     */
    public DefaultMutableTreeNode getChannelNode(int g, int c) throws ClassCastException {
        return (DefaultMutableTreeNode) root.getChildAt(g).getChildAt(c);
    }

    /**
     * Returns number of channels in the specific group
     *
     * @param g target group
     * @return number of channels in group
     */
    public int getChannelsSize(int g) {
        return root.getChildAt(g).getChildCount();
    }

    /**
     * Returns X channel that corresponds to the given channel
     *
     * @param chan channel to test
     * @return X channel in the correspondent group
     */
    public DataChannel getXChannel(DataChannel chan) {
        try {
            for (int i = 0; i < getGroupsSize(); i++) {
                DataGroup group = getGroup(i);
                DataChannel xchannel = getChannel(i, group.getXChannel());
                for (int j = 0; j < getChannelsSize(i); j++) {
                    DataChannel channel = getChannel(i, j);
                    if (channel == chan) {
                        return xchannel;
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Returns true if given channel is an X channel in one of the groups
     *
     * @param chan channel to test
     * @return true if X channel
     */
    public boolean isXChannel(DataChannel chan) {
        try {
            for (int i = 0; i < getGroupsSize(); i++) {
                DataGroup group = getGroup(i);
                DataChannel xchannel = getChannel(i, group.getXChannel());
                if (xchannel == chan) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Sets an attribute of the given channel to X channel and adjusts all
     * entries in the correspondent group
     *
     * @param chan channel to test
     */
    public void setXChannel(DataChannel chan) {
        try {
            for (int i = 0; i < getGroupsSize(); i++) {
                DataGroup group = getGroup(i);
                for (int j = 0; j < getChannelsSize(i); j++) {
                    DataChannel xchannel = getChannel(i, j);
                    if (xchannel == chan) {
                        group.setXChannel(j);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Returns a currently selected channel
     *
     * @return selected channel
     */
    public DataChannel getSelectedChannel() {
        return selectedChannel;
    }

    /**
     * Sets the new selected channel
     *
     * @param selectedChannel selected channel
     */
    public void setSelectedChannel(DataChannel selectedChannel) {
        this.selectedChannel = selectedChannel;
    }

    /**
     * Returns limit for the current key
     *
     * @param k String key for the limit
     * @return limit
     */
    public double getLimit(String k) {
        return ((Double) limits.get(k)).doubleValue();
    }

    /**
     * Sets limit for the current key
     *
     * @param k String key for the limit
     * @param d limit data to be stored
     */
    public void setLimit(String k, double d) {
        Double dat = new Double(d);
        if (Double.isNaN(d)) dat = new Double(0.0);
        limits.put(k, dat);
    }

    /**
     * Exports data to ASCII
     *
     * Only channels with attribute NORMAL_CHANNEL (default) will be exported
     *
     * @return String containing formatted ASCII codes for data
     */
    public String exportDataASCII() {
        double recs = 0;
        DataGroup group = null;
        DataChannel channel = null;
        StringBuffer datOut = new StringBuffer();
        java.text.DecimalFormat datFormat = new java.text.DecimalFormat(format);
        try {
            datOut.append("# ");
            for (int i = 0; i < getGroupsSize(); i++) {
                for (int j = 0; j < getChannelsSize(i); j++) {
                    channel = getChannel(i, j);
                    if (channel.getAttribute().isNormal()) {
                        recs = Math.max(recs, channel.size());
                        datOut.append(channel.getName() + delimiter);
                    }
                }
            }
            datOut.append('\n');
            for (int k = 0; k < recs; k++) {
                for (int i = 0; i < getGroupsSize(); i++) {
                    group = getGroup(i);
                    for (int j = 0; j < getChannelsSize(i); j++) {
                        channel = getChannel(i, j);
                        if (channel.getAttribute().isNormal()) {
                            if (k < channel.size()) {
                                datOut.append(datFormat.format(channel.getData(k)) + delimiter);
                            } else {
                                datOut.append(" " + delimiter);
                            }
                        }
                    }
                }
                datOut.append('\n');
            }
        } catch (Exception e) {
        }
        return datOut.toString();
    }

    /**
     * Exports data to multidimensional array
     *
     * Only channels with attribute NORMAL_CHANNEL (default) will be exported
     *
     * @param datExp netCDF writtable file
     * @throws IOException if i/o problem occurs
     */
    public void exportDataMarray(NetcdfFileWriteable datExp) throws java.io.IOException {
        DataGroup group = null;
        DataChannel channel = null;
        Dimension xdim = datExp.addDimension("X", -1);
        for (int i = 0; i < getGroupsSize(); i++) {
            group = getGroup(i);
            DataChannel xchannel = getChannel(i, group.getXChannel());
            String var = "chn_" + i + "_x";
            datExp.addVariable(var, double.class, new Dimension[] { xdim });
            datExp.addVariableAttribute(var, "long_name", xchannel.getName());
            datExp.addVariableAttribute(var, "units", xchannel.getUnits());
            for (int j = 0; j < getChannelsSize(i); j++) {
                channel = getChannel(i, j);
                var = "chn_" + i + "_" + j + "_data";
                if (channel.getAttribute().isNormal() && (channel != xchannel)) {
                    datExp.addVariable(var, double.class, new Dimension[] { xdim });
                    datExp.addVariableAttribute(var, "long_name", channel.getName());
                    datExp.addVariableAttribute(var, "units", channel.getUnits());
                }
            }
        }
        datExp.create();
        for (int i = 0; i < getGroupsSize(); i++) {
            group = getGroup(i);
            DataChannel xchannel = getChannel(i, group.getXChannel());
            String var = "chn_" + i + "_x";
            int size = xchannel.size();
            double[] data = new double[size];
            for (int k = 0; k < size; k++) {
                data[k] = xchannel.getData(k);
            }
            datExp.write(var, ArrayAbstract.factory(data));
            for (int j = 0; j < getChannelsSize(i); j++) {
                channel = getChannel(i, j);
                var = "chn_" + i + "_" + j + "_data";
                if (channel.getAttribute().isNormal() && (channel != xchannel)) {
                    for (int k = 0; k < size; k++) {
                        data[k] = channel.getData(k);
                    }
                    datExp.write(var, ArrayAbstract.factory(data));
                }
            }
        }
    }

    /**
     * Getter for property delimiter
     *
     * @return Value of property delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Setter for property delimiter
     *
     * @param delimiter New value of property delimiter
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Getter for property format.
     *
     * @return Value of property format.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Setter for property format.
     *
     * @param format New value of property format.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    private void decorateChannel(DataGroup g, DataChannel c) {
        c.getAttribute().setColor(ChannelAttribute.colors[colorcount]);
        c.getAttribute().setSymbol(new PlotSymbol(symbolcount));
        colorcount++;
        symbolcount++;
        if (colorcount > ChannelAttribute.colors.length - 1) colorcount = 0;
        if (symbolcount > PlotSymbol.SYMBOL_NUMBER) symbolcount = 0;
    }

    private void setLimits(int i) {
        DataGroup group = getGroup(i);
        for (int j = 0; j < getChannelsSize(i); j++) {
            DataChannel channel = getChannel(i, j);
            if (j == group.getXChannel()) {
                setLimit("MinX", Math.min(getLimit("MinX"), channel.minLimit()));
                setLimit("MaxX", Math.max(getLimit("MaxX"), channel.maxLimit()));
            } else {
                setLimit("MinY", Math.min(getLimit("MinY"), channel.minLimit()));
                setLimit("MaxY", Math.max(getLimit("MaxY"), channel.maxLimit()));
            }
        }
    }
}
