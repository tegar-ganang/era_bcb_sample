package uqdsd.infosec.model;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.GraphConstants;
import uqdsd.infosec.GlobalProperties;

/**
 * @author InfoSec Project (c) 2008 UQ
 *
 * Represents a connection between PortComponents.
 */
public class DataFlow extends DefaultEdge {

    static final long serialVersionUID = 0;

    public static final int SINGLE_DIRECTION = 0;

    public static final int DOUBLE_DIRECTION = 1;

    protected int arrowDirection;

    public static final int UNKNOWN_DATA = 0;

    public static final int NO_DATA = 1;

    public static final int CONFIRMED_DATA = 2;

    protected int channelInformation;

    public DataFlow(int arrowDirection, int channelInformation) {
        super();
        this.arrowDirection = arrowDirection;
        doCosmetics();
    }

    private void doCosmetics() {
        switch(channelInformation) {
            case UNKNOWN_DATA:
                {
                    GraphConstants.setLineColor(attributes, GlobalProperties.getColor("Editor.UnconfirmedArcColour"));
                    break;
                }
            case NO_DATA:
                {
                    GraphConstants.setLineColor(attributes, GlobalProperties.getColor("Editor.SilentArcColour"));
                    GraphConstants.setDashPattern(attributes, new float[] { (float) 15.0, (float) 3.0 });
                    break;
                }
            case CONFIRMED_DATA:
                {
                    GraphConstants.setLineColor(attributes, GlobalProperties.getColor("Editor.ConfirmedArcColour"));
                    GraphConstants.setLineWidth(attributes, 2);
                }
        }
    }

    public int getArrowDirection() {
        return arrowDirection;
    }

    public void setArrowDirection(int i) {
        arrowDirection = i;
    }

    public int getChannelInformation() {
        return channelInformation;
    }

    public void setChannelInformation(int i) {
        switch(i) {
            case UNKNOWN_DATA:
            case NO_DATA:
            case CONFIRMED_DATA:
                channelInformation = i;
            default:
        }
        doCosmetics();
    }

    @Override
    public Object clone() {
        DataFlow clone = new DataFlow(arrowDirection, channelInformation);
        return clone;
    }
}
