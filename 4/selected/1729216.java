package net.sourceforge.fluxion.beans;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import net.sourceforge.fluxion.graph.ColorSchemes;
import net.sourceforge.fluxion.graph.Graph;
import net.sourceforge.fluxion.graph.view.GraphView;
import net.sourceforge.fluxion.graph.view.renderer.GraphViewRenderer;
import net.sourceforge.fluxion.runcible.graph.mapping.MappingManager;
import net.sourceforge.fluxion.runcible.graph.view.factory.FlexGraphViewFactory;
import net.sourceforge.fluxion.runcible.graph.view.renderer.FlexGraphViewRenderer;
import org.apache.commons.io.IOUtils;
import javax.servlet.http.HttpSession;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * An Ajax service that can take a {@link MappingManager} in an HttpSession and
 * use it to extract source and target graphs, and return a JSON object that
 * encapsulates an XML rendering of a view over the graph.
 *
 * @author Tony Burdett
 * @author Rob Davey
 */
@Ajaxified
public class RuncibleFlexGraphBean {

    public JSONObject getSourceGraph(HttpSession session, JSONObject json) throws JSONException {
        StringBuffer out = new StringBuffer();
        Graph src = null;
        MappingManager manager = (MappingManager) session.getAttribute(RuncibleConstants.MAPPING_MANAGER.key());
        try {
            src = manager.getSourceGraph();
            if (src != null) {
                FlexGraphViewFactory factory = new FlexGraphViewFactory();
                factory.setColorScheme(ColorSchemes.BLUES);
                factory.visit(src);
                GraphView view = factory.getGraphView();
                GraphViewRenderer renderer = new FlexGraphViewRenderer();
                renderer.setGraphView(view);
                InputStream xmlStream = renderer.renderGraphView();
                StringWriter writer = new StringWriter();
                IOUtils.copy(xmlStream, writer);
                writer.close();
                System.out.println(writer.toString());
                out.append(writer.toString());
            } else {
                out.append("No source graph loaded.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return JSONUtils.SimpleJSONError("Cannot load source graph: " + e.getMessage());
        }
        return JSONUtils.SimpleJSONResponse(out.toString());
    }

    public JSONObject getTargetGraph(HttpSession session, JSONObject json) throws JSONException {
        StringBuffer out = new StringBuffer();
        Graph tgt = null;
        MappingManager manager = (MappingManager) session.getAttribute(RuncibleConstants.MAPPING_MANAGER.key());
        try {
            tgt = manager.getTargetGraph();
            if (tgt != null) {
                FlexGraphViewFactory factory = new FlexGraphViewFactory();
                factory.setColorScheme(ColorSchemes.ORANGES);
                factory.visit(tgt);
                GraphView view = factory.getGraphView();
                GraphViewRenderer renderer = new FlexGraphViewRenderer();
                renderer.setGraphView(view);
                InputStream xmlStream = renderer.renderGraphView();
                StringWriter writer = new StringWriter();
                IOUtils.copy(xmlStream, writer);
                writer.close();
                System.out.println(writer.toString());
                out.append(writer.toString());
            } else {
                out.append("No target graph loaded.");
            }
        } catch (Exception e) {
            return JSONUtils.SimpleJSONError("Cannot load target graph: " + e.getMessage());
        }
        return JSONUtils.SimpleJSONResponse(out.toString());
    }
}
