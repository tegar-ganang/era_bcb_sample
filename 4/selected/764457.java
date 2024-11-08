package net.sf.jqueryfaces.component.slider;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import net.sf.jqueryfaces.util.JSFUtility;
import net.sf.jqueryfaces.util.JavaScriptFunction;

/**
 * This component class takes care of holding the options that have been used.
 * It also renders itself.
 * 
 * @author Jeremy Buis
 */
public class Slider extends UIComponentBase {

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Slider";

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Slider";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    public static final String ANIMATE = "animate";

    public static final String MAX = "max";

    public static final String MIN = "min";

    public static final String ORIENTATION = "orientation";

    public static final String RANGE = "range";

    public static final String STEP = "step";

    public static final String VALUE = "value";

    public static final String VALUES = "values";

    public static final JavaScriptFunction ONSTART = new JavaScriptFunction("start");

    public static final JavaScriptFunction ONSLIDE = new JavaScriptFunction("slide");

    public static final JavaScriptFunction ONCHANGE = new JavaScriptFunction("change");

    public static final JavaScriptFunction ONSTOP = new JavaScriptFunction("stop");

    /**
     * Default constructor
     */
    public Slider() {
        super();
    }

    /**
     * @return  Gets the <code>COMPONENT_FAMILY</code>
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
        writer.writeText("$(\"#" + clientId + "\").slider({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, Slider.class);
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
