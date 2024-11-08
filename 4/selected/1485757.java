package com.cameocontrol.cameo.dataStructure.jgraph;

import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.graph.GraphConstants;
import com.cameocontrol.cameo.action.ACTChanLocation;
import com.cameocontrol.cameo.action.ActionInterpreter;

public class ChannelMoveListener implements GraphModelListener {

    private ActionInterpreter _actInt;

    public ChannelMoveListener(ActionInterpreter ai) {
        _actInt = ai;
    }

    public void graphChanged(GraphModelEvent e) {
        for (Object o : e.getChange().getChanged()) if (o instanceof MagicGraphCell) {
            MagicGraphCell cell = (MagicGraphCell) o;
            double x = GraphConstants.getBounds(cell.getAttributes()).getCenterX();
            double y = GraphConstants.getBounds(cell.getAttributes()).getCenterY();
            _actInt.interprete(new ACTChanLocation(cell.getChannelNumber(), x, y));
        }
    }
}
