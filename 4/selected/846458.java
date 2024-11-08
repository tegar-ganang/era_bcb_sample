package net.sf.jqueryfaces.component.accordion;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import net.sf.jqueryfaces.struct.*;
import net.sf.jqueryfaces.util.*;
import static net.sf.jqueryfaces.util.UtilConstants.*;

/**
 * This class renders an accordion component in HTML.
 * @see net.sf.jqueryfaces.component.accordion.AccordionComponent
 * @see net.sf.jqueryfaces.component.accordion.AccordionItemComponent
 * @see net.sf.jqueryfaces.component.accordion.AccordionItemHtmlRenderer
 * @see net.sf.jqueryfaces.component.accordion.AccordionItemTag
 * @see net.sf.jqueryfaces.component.accordion.AccordionTag
 */
public class AccordionHtmlRenderer extends Renderer {

    /**
	 * Creates an accordion HTML renderer.
	 */
    public AccordionHtmlRenderer() {
        super();
    }

    /**
	 * Encodes the beginning of the context.
	 * @param context The context to use.
	 * @param component The component to use.
	 * @throws IOException If an IO error occurs.
	 */
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Map attr = component.getAttributes();
        JSFUtility.renderScriptOnce(writer, component, context);
        writer.startElement(HTMLTAG_SCRIPT, component);
        writer.writeAttribute(HTMLATTR_TYPE, "text/javascript", null);
        writer.writeText("$(document).ready(function(){\n", null);
        String clientId = component.getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(\"#" + clientId + "\").accordion({", null);
        boolean comma = JSFUtility.writeJSObjectOptions(writer, attr, AccordionComponent.ATTRNAME_LIST);
        if (comma) {
            writer.write(CHAR_COMMA);
        }
        writer.writeText("\nheader:\"" + HTMLTAG_H3 + "\"", null);
        writer.writeText("});\n" + "});", null);
        writer.endElement(HTMLTAG_SCRIPT);
        writer.startElement(HTMLTAG_DIV, component);
        writer.writeAttribute(HTMLATTR_ID, component.getClientId(context), "Accordion");
        if (attr.get(Style.STYLE) != null) {
            writer.writeAttribute(Style.STYLE, attr.get(Style.STYLE), null);
        }
        if (attr.get(Style.STYLE_CLASS) != null) {
            writer.writeAttribute("class", attr.get(Style.STYLE_CLASS), null);
        }
        writer.flush();
    }

    /**
	 * Encodes the end of the context.
	 * @param context The context to use.
	 * @param component The component to use.
	 * @throws IOException If an IO error occurs.
	 */
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement(HTMLTAG_DIV);
        writer.flush();
    }
}
