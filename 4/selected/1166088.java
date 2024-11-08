package com.javable.dataview;

/**
 * This class displays data corner for the given <code>DataView</code>
 */
public class DataCorner extends javax.swing.JTextField {

    /**
     * Specifies horizontal orientation of corner
     */
    public static final int CORNER_HORIZONTAL = 0;

    /**
     * Specifies vertical orientation of corner
     */
    public static final int CORNER_VERTICAL = 1;

    /**
     * Specifies empty corner
     */
    public static final int CORNER_EMPTY = 2;

    private int orientation;

    /**
     * Creates new corner
     * 
     * @param o orienation
     * @param text text label
     * @param columns number of columns for the label
     */
    public DataCorner(int o, java.lang.String text, int columns) {
        super(text, columns);
        setFont(new java.awt.Font("Courier", java.awt.Font.PLAIN, 12));
        setBackground(new java.awt.Color(50, 100, 150));
        setForeground(java.awt.Color.lightGray);
        setBorder(new javax.swing.border.LineBorder(java.awt.Color.black, 0));
        orientation = o;
    }

    /**
     * Returns corner orientation
     * 
     * @return corner orientation
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * Sets the content of the corner depending on the units in the storage
     * 
     * @param s storage
     * @param evt TreeModelEvent that is being emitted when storage is updated
     */
    public void setCorner(DataStorage s, javax.swing.event.TreeModelEvent evt) {
        String name = ResourceManager.getResource("units");
        String units = "";
        try {
            if (evt.getTreePath().getPathCount() > 1) {
                DataGroup group = s.getGroup(0);
                if (group != null) {
                    if (orientation == CORNER_HORIZONTAL) {
                        units = s.getChannel(0, group.getXChannel()).getUnits();
                        if (units.length() > 0) name = units;
                        this.setText(name);
                    }
                    if (orientation == CORNER_VERTICAL) {
                        units = s.getChannel(0, 1).getUnits();
                        if (units.length() > 0) name = units;
                        this.setText(name);
                    }
                }
            } else {
                this.setText(name);
            }
        } catch (Exception e) {
        }
    }
}
