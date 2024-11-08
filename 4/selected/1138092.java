package eu.davidgamez.mas.gui;

import java.awt.*;
import org.w3c.dom.Document;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.gui.dialog.TrackDialog;
import eu.davidgamez.mas.midi.*;

public class TrackGUI {

    /** The track managed by this GUI */
    private Track midiTrack;

    /** MainFrame stored for launching track dialog */
    private MainFrame mainFrame;

    private boolean selected = false;

    private boolean moving = false;

    private int xPos = 50, yPos = 50;

    private int size;

    private int buttonSize;

    private Rectangle muteRect = new Rectangle(0, 0, 1, 1);

    private Rectangle soloRect = new Rectangle(0, 0, 1, 1);

    private Rectangle agVisRect = new Rectangle(0, 0, 1, 1);

    private Rectangle connVisRect = new Rectangle(0, 0, 1, 1);

    private int nameYOffset;

    private int buttonYOffset;

    private int channelYOffset;

    private int buttonLetterOffset;

    private int letterWidth;

    private int smallLetterWidth;

    private int border;

    static final int NUMBER_OF_ZOOM_LEVELS = 10;

    private int zoomLevel = 5;

    private boolean agentsVisible = true;

    private boolean connectionsVisible = true;

    private Font trackFont;

    private Font smallTrackFont;

    private static Color trackColor = new Color(197, 197, 0);

    /** Standard constructor */
    public TrackGUI(Track track, MainFrame mainFrame) {
        this.midiTrack = track;
        this.mainFrame = mainFrame;
        setZoomLevel();
    }

    /** Constructor when loading from a file */
    public TrackGUI(String xmlStr, MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        loadFromXML(xmlStr);
        setZoomLevel();
    }

    public void setBackgroundColour(Color newColor) {
        trackColor = newColor;
    }

    /** Returns true if the class contains the click position. */
    public boolean contains(int x, int y) {
        double distanceFromCenter = Math.pow((double) x - (xPos + size / 2), 2.0) + Math.pow((double) y - (yPos + size / 2), 2.0);
        distanceFromCenter = Math.pow(distanceFromCenter, 0.5);
        if (distanceFromCenter < size / 2) return true;
        return false;
    }

    public boolean processMouseClicked(int mouseXPos, int mouseYPos, int clickCount) {
        if (muteRect.contains(mouseXPos, mouseYPos)) {
            if (midiTrack.muted()) midiTrack.setMuted(false); else midiTrack.setMuted(true);
            return true;
        } else if (soloRect.contains(mouseXPos, mouseYPos)) {
            if (midiTrack.soloed()) {
                midiTrack.setSoloed(false);
            } else {
                midiTrack.setSoloed(true);
            }
            return true;
        } else if (agVisRect.contains(mouseXPos, mouseYPos)) {
            if (agentsVisible) {
                agentsVisible = false;
                connectionsVisible = false;
            } else {
                agentsVisible = true;
                connectionsVisible = true;
            }
            return true;
        } else if (connVisRect.contains(mouseXPos, mouseYPos)) {
            if (connectionsVisible) connectionsVisible = false; else if (agentsVisible) connectionsVisible = true;
            return true;
        }
        if (clickCount == 2) {
            if (this.contains(mouseXPos, mouseYPos)) {
                new TrackDialog(mainFrame, midiTrack, xPos, yPos);
                return true;
            } else return false;
        } else if (this.contains(mouseXPos, mouseYPos)) return true;
        return false;
    }

    public void setPosition(int newXPos, int newYPos) {
        xPos = newXPos;
        yPos = newYPos;
        updateButtonRectangles();
    }

    private void setZoomLevel() {
        size = zoomLevel * 20;
        nameYOffset = (int) Math.round(zoomLevel * -2.5);
        buttonYOffset = -1 * zoomLevel;
        channelYOffset = 6 * zoomLevel;
        buttonLetterOffset = (int) Math.round(zoomLevel / 1.2);
        letterWidth = (int) Math.round(zoomLevel * 1.2);
        smallLetterWidth = (int) Math.round(zoomLevel * 0.85);
        border = (int) Math.round(zoomLevel / 2.0);
        muteRect.width = (int) Math.round(zoomLevel * 3.4);
        muteRect.height = (int) Math.round(zoomLevel * 3.4);
        soloRect.width = (int) Math.round(zoomLevel * 3.4);
        soloRect.height = (int) Math.round(zoomLevel * 3.4);
        agVisRect.width = (int) Math.round(zoomLevel * 3.4);
        agVisRect.height = (int) Math.round(zoomLevel * 3.4);
        connVisRect.width = (int) Math.round(zoomLevel * 3.4);
        connVisRect.height = (int) Math.round(zoomLevel * 3.4);
        trackFont = new Font("Arial", Font.PLAIN, (int) Math.round(zoomLevel * 2.5));
        smallTrackFont = new Font("Arial", Font.PLAIN, (int) Math.round(zoomLevel * 2.0));
        updateButtonRectangles();
    }

    public void translate(int dx, int dy, Rectangle boundingRectangle) {
        xPos += dx;
        yPos += dy;
        if (xPos < 0) xPos = 0;
        if (yPos < 0) yPos = 0;
        if (xPos + size > boundingRectangle.width) xPos = boundingRectangle.width - size;
        if (yPos + size > boundingRectangle.height) yPos = boundingRectangle.height - size;
        updateButtonRectangles();
    }

    protected Point getCentre() {
        return new Point(xPos + size / 2, yPos + size / 2);
    }

    protected Track getTrack() {
        return midiTrack;
    }

    public int getX() {
        return xPos;
    }

    public int getY() {
        return yPos;
    }

    public int getWidth() {
        return size;
    }

    public int getHeight() {
        return size;
    }

    protected String getID() {
        return midiTrack.getID();
    }

    protected boolean isMoving() {
        return moving;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setMoving(boolean mving) {
        moving = mving;
    }

    public void setSelected(boolean b) {
        selected = b;
    }

    public boolean showAgents() {
        return agentsVisible;
    }

    public boolean showConnections() {
        return connectionsVisible;
    }

    protected void paintTrack(Graphics2D g2D) {
        if (selected) g2D.setColor(Color.red); else g2D.setColor(Color.blue);
        g2D.fillOval(xPos, yPos, size, size);
        g2D.setColor(trackColor);
        g2D.fillOval(xPos + border, yPos + border, size - border * 2, size - border * 2);
        g2D.setColor(Color.black);
        int nameLength = midiTrack.getName().length();
        if (nameLength > 12) {
            g2D.setFont(smallTrackFont);
            g2D.drawString(midiTrack.getName(), xPos + size / 2 - nameLength / 2 * smallLetterWidth, yPos + size / 2 + nameYOffset);
        } else {
            g2D.setFont(trackFont);
            g2D.drawString(midiTrack.getName(), xPos + size / 2 - nameLength / 2 * letterWidth, yPos + size / 2 + nameYOffset);
        }
        g2D.setFont(trackFont);
        if (midiTrack.muted()) g2D.setColor(MASLookAndFeel.getTrackMuteOnColor()); else g2D.setColor(MASLookAndFeel.getTrackMuteOffColor());
        g2D.fillRect(muteRect.x, muteRect.y, muteRect.width, muteRect.height);
        g2D.setColor(Color.white);
        g2D.drawString("M", muteRect.x + buttonLetterOffset, muteRect.y + muteRect.height - buttonLetterOffset);
        if (midiTrack.soloed()) g2D.setColor(MASLookAndFeel.getTrackSoloOnColor()); else g2D.setColor(MASLookAndFeel.getTrackSoloOffColor());
        g2D.fillRect(soloRect.x, soloRect.y, soloRect.width, soloRect.height);
        g2D.setColor(Color.white);
        g2D.drawString("S", soloRect.x + buttonLetterOffset, soloRect.y + soloRect.height - buttonLetterOffset);
        g2D.setColor(MASLookAndFeel.getTrackAgentVisibleColor());
        g2D.fillRect(agVisRect.x, agVisRect.y, agVisRect.width, agVisRect.height);
        g2D.setColor(Color.white);
        g2D.drawString("A", agVisRect.x + buttonLetterOffset, agVisRect.y + agVisRect.height - buttonLetterOffset);
        if (!agentsVisible) {
            g2D.setColor(Color.red);
            g2D.drawLine(agVisRect.x, agVisRect.y, agVisRect.x + agVisRect.width, agVisRect.y + agVisRect.height);
            g2D.drawLine(agVisRect.x + agVisRect.width, agVisRect.y, agVisRect.x, agVisRect.y + agVisRect.height);
        }
        g2D.setColor(MASLookAndFeel.getTrackConnectionVisibleColor());
        g2D.fillRect(connVisRect.x, connVisRect.y, connVisRect.width, connVisRect.height);
        g2D.setColor(Color.white);
        g2D.drawString("C", connVisRect.x + buttonLetterOffset, connVisRect.y + connVisRect.height - buttonLetterOffset);
        if (!connectionsVisible) {
            g2D.setColor(Color.red);
            g2D.drawLine(connVisRect.x, connVisRect.y, connVisRect.x + connVisRect.width, connVisRect.y + connVisRect.height);
            g2D.drawLine(connVisRect.x + connVisRect.width, connVisRect.y, connVisRect.x, connVisRect.y + connVisRect.height);
        }
        g2D.setColor(Color.black);
        int midiChannel = midiTrack.getChannel() + 1;
        if (midiChannel < 10) g2D.drawString(Integer.toString(midiChannel), xPos + size / 2 - 3, yPos + size / 2 + channelYOffset); else g2D.drawString(Integer.toString(midiChannel), xPos + size / 2 - 5, yPos + size / 2 + channelYOffset);
    }

    /** Returns the GUI and MIDI track information in XML format */
    public String getXML(String indent) {
        String trackStr = indent + "<track>";
        trackStr += indent + "\t<position>";
        trackStr += indent + "\t\t<x>" + this.xPos + "</x><y>" + this.yPos + "</y>";
        trackStr += indent + "\t</position>";
        trackStr += midiTrack.getXML(indent + "\t");
        trackStr += indent + "</track>";
        return trackStr;
    }

    /** Loads the track GUI's parameters from the XML string and uses XML to create MIDI track */
    public void loadFromXML(String xmlStr) {
        String midiTrackStr = xmlStr.substring(xmlStr.indexOf("<midi_track>"), xmlStr.indexOf("</midi_track>") + 13);
        midiTrack = new Track(midiTrackStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            int newXPos = Util.getIntParameter("x", xmlDoc);
            int newYPos = Util.getIntParameter("y", xmlDoc);
            setPosition(newXPos, newYPos);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    private void updateButtonRectangles() {
        int yLoc = yPos + size / 2 + buttonYOffset;
        muteRect.x = xPos + size / 2 - muteRect.width - soloRect.width;
        muteRect.y = yLoc;
        soloRect.x = xPos + size / 2 - soloRect.width;
        soloRect.y = yLoc;
        agVisRect.x = xPos + size / 2;
        agVisRect.y = yLoc;
        connVisRect.x = xPos + size / 2 + agVisRect.width;
        connVisRect.y = yLoc;
    }
}
