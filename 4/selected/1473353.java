package com.javable.dataview;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Class that represents group of data sets (channels) from a single recording
 */
public class DataGroup extends DefaultMutableTreeNode {

    private String name;

    private String info;

    private int xChannel;

    /**
     * Creates new DataGroup
     * 
     * @param name name of the group
     * @param info description
     */
    public DataGroup(String name, String info) {
        super();
        this.name = name;
        this.info = info;
    }

    /**
     * Adds a channel to this group
     * 
     * @param c channel to add
     */
    public void addChannel(DataChannel c) {
        this.add(new DefaultMutableTreeNode(c));
    }

    /**
     * Returns channel in the group
     * 
     * @return DataChannel for index
     * @param c channel index
     * @throws ClassCastException if object is not of type DataChannel
     */
    public DataChannel getChannel(int c) throws ClassCastException {
        return (DataChannel) ((DefaultMutableTreeNode) this.getChildAt(c)).getUserObject();
    }

    /**
     * Getter for property name
     * 
     * @return Value of property name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for property name
     * 
     * @param name New value of property name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for property info
     * 
     * @return Value of property info
     */
    public String getInfo() {
        return info;
    }

    /**
     * Setter for property info
     * 
     * @param info New value of property info
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * Getter for property xChannel.
     * 
     * @return Value of property xChannel.
     */
    public int getXChannel() {
        return xChannel;
    }

    /**
     * Setter for property xChannel.
     * 
     * @param x New value of property xChannel.
     */
    public void setXChannel(int x) {
        this.xChannel = x;
    }
}
