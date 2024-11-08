package net.sf.jqueryfaces.component.progressbar;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import net.sf.jqueryfaces.util.JSFUtility;
import net.sf.jqueryfaces.util.JavaScriptFunction;

/**
 * This component takes care of the options that have been used on it.  It
 * also renders itself.
 * 
 * @author Jeremy Buis
 */
public class Progressbar extends UIComponentBase {

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Progressbar";

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Progressbar";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    public static final String VALUE = "value";

    public static final JavaScriptFunction ONCHANGE = new JavaScriptFunction("change");

    /**
     * Default constructor.
     */
    public Progressbar() {
        super();
    }

    /**
     * @return  Gets the <code>COMPONENT_FAMILY</code> of the component.
     */
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
       * @param context
       * @return
       */
    public Object saveState(FacesContext context) {
        Object values[] = new Object[1];
        values[0] = super.saveState(context);
        return (values);
    }

    /**
       * @param context
       * @param state
       */
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        JSFUtility.renderScriptOnce(writer, this, context);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText("$(document).ready(function(){\n", null);
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(\"#" + clientId + "\").progressbar({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, Progressbar.class);
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.startElement("div", this);
        writer.writeAttribute("id", getClientId(context), null);
        if (attr.get(STYLE) != null) {
            writer.writeAttribute("style", attr.get(STYLE), STYLE);
        }
        if (attr.get(STYLECLASS) != null) {
            writer.writeAttribute("class", attr.get(STYLECLASS), STYLECLASS);
        }
        writer.flush();
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeChildren(FacesContext context) throws IOException {
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
        writer.flush();
    }
}
