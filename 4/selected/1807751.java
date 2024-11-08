package edu.ucsd.osdt.db;

import java.util.LinkedList;

/**
 * TableConfigColumn: represents one column entry of the table object in the config file
 */
public class TableConfigColumn {

    public static String TYPE_STRING = "STRING";

    public static String TYPE_INT = "INT";

    public static String TYPE_DOUBLE = "DOUBLE";

    private String name = null;

    private String channelMapping = null;

    private LinkedList<String> dataValue = null;

    private LinkedList<String> type = null;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setChannelMapping(String channelMapping) {
        this.channelMapping = channelMapping;
    }

    public String getChannelMapping() {
        return this.channelMapping;
    }

    public void setDataValue(String dataValue) {
        this.dataValue.add(dataValue);
    }

    public LinkedList<String> getDataValue() {
        return this.dataValue;
    }

    public void setType(String t) {
        this.type.add(t);
    }

    public LinkedList<String> getType() {
        return this.type;
    }
}
