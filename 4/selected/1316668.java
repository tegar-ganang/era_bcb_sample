package uk.ac.ebi.intact.editor.util;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

/**
 * Utility methods for the components.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public final class ComponentUtils {

    private ComponentUtils() {
    }

    public static void writeEventListenerScript(FacesContext context, UIComponent component, String[] events, String javascript) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("script", component);
        writer.writeAttribute("type", "text/javascript", null);
        String callbackFunction = "curationChanged_" + functionNamePrefix(component.getClientId());
        writer.write("$(document).ready(function() {");
        writer.write("function " + callbackFunction + "(e) { ");
        writer.write(javascript + ";");
        writer.write(" }\n");
        for (String event : events) {
            String jQueryFriendlyId = component.getClientId().replaceAll(":", "\\\\\\\\:");
            writer.write("$('#" + jQueryFriendlyId + "').bind('" + event + "', " + callbackFunction + "); ");
        }
        writer.write("});");
        writer.endElement("script");
    }

    public static String functionNamePrefix(String clientId) {
        return clientId.replaceAll("\\:", "_");
    }
}
