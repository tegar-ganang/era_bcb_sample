package com.cameocontrol.cameo.dataStructure.jgraph;

import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.VertexView;
import com.cameocontrol.cameo.action.ActionInterpreter;
import com.cameocontrol.cameo.control.*;
import com.cameocontrol.cameo.gui.binder.KeyBinder;
import com.cameocontrol.cameo.output.CameoPatch;
import com.cameocontrol.cameo.output.LiveCue;

public class JGraphFactory {

    public enum Kind {

        CHANNEL, MAGIC
    }

    ;

    private int cellWidth = 45;

    private int cellHight = 50;

    private int cellWidthPad = 0;

    private int cellHightPad = 30;

    private int cellGroupWidthPad = 10;

    private int cellGroupHightPad = 10;

    private Color _graphBackgroundColor = Color.BLACK;

    private Color _cellBackgroundColor = Color.BLACK;

    private Color _cellBorderColor = Color.WHITE;

    private Color _cellTitleColor = Color.WHITE;

    private Color _notPatchedColor = Color.GRAY;

    private Color _cellSelectionColor = Color.RED;

    private ConsoleInquiry console;

    private ActionInterpreter _actInt;

    public JGraphFactory(ConsoleInquiry c, ActionInterpreter ai) {
        console = c;
        _actInt = ai;
    }

    public JGraph buildChannelJGraph(Kind k, ConsoleMagicSheet ms, KeyBinder kb, LiveCue c, ConsoleSettings cs) {
        GraphModel model = new DefaultGraphModel();
        JGraph graph = new JGraph(model);
        kb.bind(graph);
        graph.setInvokesStopCellEditing(true);
        graph.setEditable(false);
        graph.setJumpToDefaultPort(true);
        graph.setBackground(_graphBackgroundColor);
        graph.setHighlightColor(_cellSelectionColor);
        graph.setLockedHandleColor(_cellSelectionColor);
        graph.getGraphLayoutCache().setFactory(new DefaultCellViewFactory() {

            protected VertexView createVertexView(Object v) {
                if (v instanceof GraphCell) return new JGraphMultilineView(v);
                return super.createVertexView(v);
            }
        });
        createGraphNodes(k, ms, graph, c, cs.getChannelsPerLine(), cs.getChannelGrouping(), cs.getLineGrouping());
        graph.clearSelection();
        graph.addGraphSelectionListener(new ChannelSelectionListener(_actInt));
        graph.getModel().addGraphModelListener(new ChannelMoveListener(_actInt));
        return graph;
    }

    private void createGraphNodes(Kind k, ConsoleMagicSheet ms, JGraph graph, LiveCue c, int width, int widthGroup, int hightGroup) {
        GraphCell channelCell = null;
        int x = 0;
        int wSkip = 1;
        int hSkip = 1;
        while (x < console.getTotalChannels()) {
            if (k == Kind.CHANNEL) channelCell = createChannelVertex(x, x % width, x / width, wSkip, hSkip, cellWidth, cellHight, c); else if (k == Kind.MAGIC) {
                channelCell = createMagicVertex(x, 0, 0, cellWidth, cellHight, c);
                Rectangle2D bounds = GraphConstants.getBounds(channelCell.getAttributes());
                bounds.setRect(ms.getChannel(x).getX(), ms.getChannel(x).getY(), bounds.getWidth(), bounds.getHeight());
                Map transportMap = new Hashtable();
                GraphConstants.setBounds(transportMap, bounds);
                channelCell.changeAttributes(transportMap);
            }
            graph.getGraphLayoutCache().insert(channelCell);
            if ((x + 1) % width == 0) {
                if ((x + 1) / width % hightGroup == 0) hSkip++;
                wSkip = 1;
            } else if ((x + 1) % widthGroup == 0) wSkip++;
            x++;
        }
    }

    public ChannelGraphCell createChannelVertex(int number, double x, double y, double xskip, double yskip, double w, double h, LiveCue c) {
        ChannelGraphCell cell = new ChannelGraphCell(number, c);
        GraphConstants.setBounds(cell.getAttributes(), new Rectangle2D.Double((w + cellWidthPad) * x + cellGroupWidthPad * xskip, (h + cellHightPad) * y + cellGroupHightPad * yskip, w, h));
        GraphConstants.setBorder(cell.getAttributes(), BorderFactory.createLineBorder(_cellBorderColor, 1));
        GraphConstants.setBackground(cell.getAttributes(), _cellBackgroundColor);
        GraphConstants.setForeground(cell.getAttributes(), _cellTitleColor);
        if (!console.isPatchedChan(number)) {
            GraphConstants.setBorder(cell.getAttributes(), BorderFactory.createLineBorder(_notPatchedColor, 1));
            GraphConstants.setForeground(cell.getAttributes(), _notPatchedColor);
        }
        DefaultPort port = new DefaultPort();
        cell.add(port);
        port.setParent(cell);
        return cell;
    }

    public MagicGraphCell createMagicVertex(int number, double x, double y, double w, double h, LiveCue c) {
        MagicGraphCell cell = new MagicGraphCell(number, c);
        GraphConstants.setBounds(cell.getAttributes(), new Rectangle2D.Double((w + cellWidthPad) * x, (h + cellHightPad) * y, w, h));
        GraphConstants.setBorder(cell.getAttributes(), BorderFactory.createLineBorder(_cellBorderColor, 1));
        GraphConstants.setBackground(cell.getAttributes(), _cellBackgroundColor);
        GraphConstants.setForeground(cell.getAttributes(), _cellTitleColor);
        if (!console.isPatchedChan(number)) {
            GraphConstants.setBorder(cell.getAttributes(), BorderFactory.createLineBorder(_notPatchedColor, 1));
            GraphConstants.setForeground(cell.getAttributes(), _notPatchedColor);
        }
        DefaultPort port = new DefaultPort();
        cell.add(port);
        port.setParent(cell);
        return cell;
    }

    public DefaultEdge createEdge(DefaultGraphCell s, DefaultGraphCell t) {
        DefaultEdge edge = new DefaultEdge();
        edge.setSource(s.getChildAt(0));
        edge.setTarget(t.getChildAt(0));
        GraphConstants.setLineEnd(edge.getAttributes(), GraphConstants.ARROW_CLASSIC);
        GraphConstants.setEndFill(edge.getAttributes(), true);
        GraphConstants.setConnectable(edge.getAttributes(), false);
        GraphConstants.setDisconnectable(edge.getAttributes(), false);
        GraphConstants.setSelectable(edge.getAttributes(), false);
        return edge;
    }
}
