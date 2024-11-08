package org.photovault.image;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 This class is the database representation of mapping from input color channels 
 to output cahnnels. It does not contain logic for the actual mapping; this is done
 in {@link ColorCurve}
 <p>
 This class is immutable, new objects should be created using {@link 
 ChannelMapOperationFactory}.
 */
public class ChannelMapOperation {

    /**
     * Creates a new instance of ChannelMapOperation. Should not be used by 
     application code, this is purely for OJB.
     */
    public ChannelMapOperation() {
    }

    /**
     Map from channel name to an array of control points (Point2D objects)
     */
    Map channelPoints = new HashMap();

    /**
     Get names of defined channels
     @return Array of all channel names
     */
    public String[] getChannelNames() {
        return (String[]) channelPoints.keySet().toArray(new String[0]);
    }

    /**
     Get the mapping curve for given channel. Note that the curve is a copy, 
     changes to it are not applied to this object.
     @param channel Name of the channel
     @return Mapping curve for the channel.
     */
    public ColorCurve getChannelCurve(String channel) {
        ColorCurve c = null;
        if (channelPoints.containsKey(channel)) {
            c = new ColorCurve();
            Point2D[] points = (Point2D[]) channelPoints.get(channel);
            for (int n = 0; n < points.length; n++) {
                Point2D p = points[n];
                c.addPoint(p.getX(), p.getY());
            }
        }
        return c;
    }

    /**
     Helper function to get a string to use as indentation
     */
    private static String getIndent(int i) {
        return "                                                  ".substring(0, i);
    }

    /**
     Get XML representation of the object
     @param i Number of spaces to add as indentation for the top level element
     @return XML representation of the object
     */
    public String getAsXml(int i) {
        StringBuffer buf = new StringBuffer();
        buf.append(getIndent(i)).append("<color-mapping>").append("\n");
        i += 2;
        Iterator iter = channelPoints.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry) iter.next();
            String name = (String) e.getKey();
            buf.append(getIndent(i)).append("<channel name=\"" + name + "\">").append("\n");
            i += 2;
            Point2D[] points = (Point2D[]) e.getValue();
            for (int n = 0; n < points.length; n++) {
                buf.append(getIndent(i)).append("<point x=\"").append(points[n].getX()).append("\" y=\"").append(points[n].getY()).append("\"/>\n");
            }
            i -= 2;
            buf.append(getIndent(i)).append("</channel>").append("\n");
        }
        i -= 2;
        buf.append(getIndent(i)).append("</color-mapping>").append("\n");
        return buf.toString();
    }

    /**
     Get XML representation of the object
     @return XML representation of the object
     */
    public String getAsXml() {
        return getAsXml(0);
    }

    /**
     Test for equality
     @param o The object to compare this object with
     @return true if o and this object are equal, false otherwise
     */
    public boolean equals(Object o) {
        if (!(o instanceof ChannelMapOperation)) {
            return false;
        }
        ChannelMapOperation c = (ChannelMapOperation) o;
        if (channelPoints.size() != c.channelPoints.size()) {
            return false;
        }
        String[] channelNames = getChannelNames();
        for (int n = 0; n < channelNames.length; n++) {
            Point2D[] p1 = (Point2D[]) channelPoints.get(channelNames[n]);
            Point2D[] p2 = (Point2D[]) c.channelPoints.get(channelNames[n]);
            if (p2 == null || p2.length != p1.length) {
                return false;
            }
            for (int i = 0; i < p1.length; i++) {
                if (!p1[i].equals(p2[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        String[] channelNames = getChannelNames();
        for (int n = 0; n < channelNames.length; n++) {
            hash = hash * 31 + channelNames[n].hashCode();
            Point2D[] p = (Point2D[]) channelPoints.get(channelNames[n]);
            hash = hash * 31 + p.hashCode();
        }
        return hash;
    }
}
