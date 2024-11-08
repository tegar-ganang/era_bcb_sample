package uk.ac.ebi.intact.editor.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;
import javax.faces.render.Renderer;
import java.io.IOException;

/**
 * TODO comment this class header.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@FacesRenderer(componentFamily = "uk.ac.ebi.intact.editor.component.SyncValue", rendererType = "uk.ac.ebi.intact.editor.component.SyncValueRenderer")
public class SyncValueRenderer extends Renderer {

    private static final String NEW_LINE = System.getProperty("line.separator");

    private final Logger logger = LoggerFactory.getLogger(SyncValueRenderer.class.getName());

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        if (!component.isRendered()) {
            return;
        }
        super.encodeEnd(context, component);
        UIComponent parent = component.getParent();
        SyncValue syncValue = (SyncValue) component;
        UIComponent withComponent = context.getViewRoot().findComponent(syncValue.getWith());
        if (withComponent == null) {
            logger.error("SyncValue: component not found: {}", syncValue.getWith());
            return;
        }
        final ResponseWriter writer = context.getResponseWriter();
        writer.startElement("script", syncValue);
        writer.writeAttribute("type", "text/javascript", syncValue.getClientId());
        writer.write("$(document).ready(function() {" + NEW_LINE);
        writer.write(updateValueBinding(parent.getClientId(), withComponent.getClientId()) + NEW_LINE);
        writer.write(updateValueBinding(withComponent.getClientId(), parent.getClientId()) + NEW_LINE);
        writer.write("});" + NEW_LINE);
        writer.endElement("script");
    }

    protected String updateValueBinding(String sourceClientId, String targetClientId) {
        StringBuilder sb = new StringBuilder();
        String functionName = functionNamePrefix(sourceClientId) + "_update";
        sb.append("function " + functionName + "() { ");
        sb.append("if (" + jQueryId(targetClientId) + ".length) { ");
        sb.append(jQueryId(targetClientId) + ".val(" + jQueryId(sourceClientId) + ".val()); ");
        sb.append("}};" + NEW_LINE);
        sb.append(jQueryId(sourceClientId) + ".bind('keyup', " + functionName + ");" + NEW_LINE);
        return sb.toString();
    }

    protected String functionNamePrefix(String clientId) {
        return clientId.replaceAll("\\:", "_");
    }

    protected String jQueryId(String componentClientId) {
        return "$('#" + componentClientId.replaceAll("\\:", "\\\\\\\\\\:") + "')";
    }
}
