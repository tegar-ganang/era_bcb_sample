package org.photovault.image;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 Factory class used to crete ChannelMapOperation objects (which are immutable)
 */
public class ChannelMapOperationFactory {

    /** 
     Creates a new instance of ChannelMapOperationFactory with no defined 
     channel curves
     */
    public ChannelMapOperationFactory() {
    }

    /**
     Creates a new ChannelMapOperationFactory that initially creates objetcs that 
     are equal to an existing ChannelMapOperation.
     @param o The operation used as template. If <code>null</code> the result is 
     similar as if constructed with the no-argument constructor.
     */
    public ChannelMapOperationFactory(ChannelMapOperation o) {
        if (o == null) {
            return;
        }
        Iterator iter = o.channelPoints.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry) iter.next();
            String name = (String) e.getKey();
            Point2D[] points = (Point2D[]) e.getValue();
            ColorCurve c = new ColorCurve();
            for (int n = 0; n < points.length; n++) {
                Point2D p = points[n];
                c.addPoint(p.getX(), p.getY());
            }
            channelCurves.put(name, c);
        }
    }

    /**
     Map from channel name to the respective curve.
     */
    Map channelCurves = new HashMap();

    /**
     Set a new curve for a given channel
     @param channel channel name
     @param curve New curve for the channel
     */
    public void setChannelCurve(String channel, ColorCurve curve) {
        if (curve != null) {
            channelCurves.put(channel, curve);
        } else {
            channelCurves.remove(channel);
        }
    }

    /**
     Get the mapping curve for a color channel
     @param channel Color channel name
     @return Respective mapping curve or <code>null</code> if it has not been 
     specified.
     */
    public ColorCurve getChannelCurve(String channel) {
        return (ColorCurve) channelCurves.get(channel);
    }

    /**
     Remove a channel from mappings.
     @param channel Name of the channel to remove.
     */
    public void removeChannel(String channel) {
        channelCurves.remove(channel);
    }

    /**
     Create a new ChannelMappingOperation with the channel settings in this factory
     @return A new object.
     */
    public ChannelMapOperation create() {
        ChannelMapOperation ret = new ChannelMapOperation();
        Iterator iter = channelCurves.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry) iter.next();
            String name = (String) e.getKey();
            ColorCurve c = (ColorCurve) e.getValue();
            Point2D[] points = new Point2D.Double[c.getPointCount()];
            for (int n = 0; n < points.length; n++) {
                points[n] = new Point2D.Double(c.getX(n), c.getY(n));
            }
            ret.channelPoints.put(name, points);
        }
        return ret;
    }
}
