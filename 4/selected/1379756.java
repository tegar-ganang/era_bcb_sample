package net.sourceforge.jcoupling2.persistence;

public class Property {

    private Integer ID;

    private Integer channelID;

    private String Name;

    private String Description;

    private String Xpathpropexpression;

    private String DataType;

    public Property(String name, Integer channel, String type, String xpath) {
        this.Name = name;
        this.channelID = channel;
        this.DataType = type;
        this.Xpathpropexpression = xpath;
    }

    public Property(String name) {
        this.Name = name;
    }

    public void setID(Integer id) {
        this.ID = id;
    }

    public Integer getID() {
        return ID;
    }

    public void setDescription(String description) {
        this.Description = description;
    }

    public String getDescription() {
        return Description;
    }

    public void setXpathExpression(String xpath) {
        this.Xpathpropexpression = xpath;
    }

    public String getXpathExpression() {
        return Xpathpropexpression;
    }

    public void setChannelID(Integer channel) {
        this.channelID = channel;
    }

    public Integer getChannelID() {
        return channelID;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public String getName() {
        return Name;
    }

    public void setDataType(String type) {
        this.DataType = type;
    }

    public String getDataType() {
        return DataType;
    }
}
