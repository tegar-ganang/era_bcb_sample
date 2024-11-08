package com.mystictri.neotexture;

import java.io.IOException;
import java.io.Writer;
import java.util.Scanner;
import java.util.Vector;
import engine.base.Logger;
import engine.graphics.synthesis.texture.Channel;

/**
 * A TextureNode represents a Channel (Pattern or Filter) from
 * the texture generation source code. It also manages the connections.
 * It contains also an absolute position that should be used for graph rendering.
 * 
 * !!TODO: simplify the location handling (and remove most of the methods)
 * 
 * @author Holger Dammertz
 * 
 */
public final class TextureGraphNode {

    public static final int width = 64 + 8;

    public static final int height = 64 + 16 + 12;

    public Object userData = null;

    Channel texChannel;

    int posX, posY;

    public void setLocation(int x, int y) {
        posX = x;
        posY = y;
    }

    public int getX() {
        return posX;
    }

    public int getY() {
        return posY;
    }

    public Channel getChannel() {
        return texChannel;
    }

    public void movePosition(int dx, int dy) {
        posX += dx;
        posY += dy;
    }

    public void save(Writer w, TextureGraphNode n) throws IOException {
        w.write(String.format("%d %d\n", n.getX(), n.getY()));
        Channel.saveChannel(w, texChannel);
    }

    public static TextureGraphNode load(Scanner s) {
        int x = s.nextInt();
        int y = s.nextInt();
        TextureGraphNode ret = new TextureGraphNode(Channel.loadChannel(s));
        ret.setLocation(x, y);
        return ret;
    }

    /**
	 * @return a true copy of the current node
	 */
    public TextureGraphNode cloneThisNode() {
        TextureGraphNode ret = new TextureGraphNode(Channel.cloneChannel(texChannel));
        ret.setLocation(getX(), getY());
        return ret;
    }

    /**
	 * Checks if the given point (in world coordinates) is contained inside the node
	 * by checking if (getX() <= x <= getX()+width) && (getY() <= y <= getY()+width))
	 * @param x the x position in world coordinates
	 * @param y the y position in world coordinates
	 * @return
	 */
    public boolean containsPoint(int x, int y) {
        return (x >= posX) && (x <= posX + width) && (y >= posY) && (y <= posY + height);
    }

    /**
	 * A ConnectionPoint is attached to a specific TextureGraphNode parent and represents
	 * either an input or an output of this TextureGraphNode. It has an internal position
	 * and size that are relative to the parent.
	 * 
	 * @author Holger Dammertz
	 *
	 */
    public static class ConnectionPoint {

        public int x, y;

        public Channel.OutputType type;

        public int channelIndex;

        public int width = 8;

        public int height = 8;

        public TextureGraphNode parent;

        public ConnectionPoint(TextureGraphNode parent, int x, int y, int index, Channel.OutputType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.parent = parent;
            this.channelIndex = index;
        }

        public int getWorldSpaceX() {
            return parent.getX() + x + 4;
        }

        public int getWorldSpaceY() {
            return parent.getY() + y + 4;
        }

        public boolean inside(int px, int py) {
            return ((px >= x + parent.getX()) && (px <= (x + parent.getX() + width)) && (py >= y + parent.getY()) && (py <= (y + parent.getY() + height)));
        }
    }

    /** 
	 * Each node has only a single output connection point which is stored
	 * here; it is also
	 * the first element in the connPoints list.
	 */
    ConnectionPoint outputConnectionPoint;

    public ConnectionPoint getOutputConnectionPoint() {
        return outputConnectionPoint;
    }

    /** 
	 * all connection points for this Node (the first one is the outputConnectionPoint
	 */
    Vector<ConnectionPoint> allConnectionPoints = new Vector<ConnectionPoint>();

    public Vector<ConnectionPoint> getAllConnectionPointsVector() {
        return allConnectionPoints;
    }

    public TextureGraphNode(Channel channel) {
        if (channel != null) setChannel(channel);
    }

    public ConnectionPoint getInputConnectionPointByChannelIndex(int index) {
        for (int i = 0; i < allConnectionPoints.size(); i++) {
            if (allConnectionPoints.get(i).channelIndex == index) return allConnectionPoints.get(i);
        }
        Logger.logError(this, "no connection point found for index " + index + " in class " + this);
        return null;
    }

    public void setChannel(Channel channel) {
        if (channel == texChannel) return;
        texChannel = channel;
        allConnectionPoints.clear();
        outputConnectionPoint = new ConnectionPoint(this, width / 2 - 4, height - 8, -1, texChannel.getOutputType());
        allConnectionPoints.add(outputConnectionPoint);
        int x = 8;
        for (int i = 0; i < texChannel.getNumInputChannels(); i++) {
            allConnectionPoints.add(new ConnectionPoint(this, x, 0, i, texChannel.getChannelInputType(i)));
            x += 12;
        }
    }
}
