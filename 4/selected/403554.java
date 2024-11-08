package net.sf.jqueryfaces.component.draggable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import net.sf.jqueryfaces.util.JSFUtility;
import net.sf.jqueryfaces.util.JavaScriptFunction;

/**
 * This is the <code>Draggable</code> component class.  It manages all the 
 * options that are applied to the jQuery draggable ui object
 * 
 * @author Jeremy Buis
 */
public class Draggable extends UIComponentBase {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Draggable";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Draggable";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    public static final String ADDCLASSES = "addClasses";

    public static final String APPENDTO = "appendTo";

    public static final String AXIS = "axis";

    public static final String CANCEL = "cancel";

    public static final String CONNECTTOSORTABLE = "connectToSortable";

    public static final String CONTAINMENT = "containment";

    public static final String CURSOR = "cursor";

    public static final String CURSORAT = "cursorAt";

    public static final String DELAY = "delay";

    public static final String DISTANCE = "distance";

    public static final String GRID = "grid";

    public static final String HANDLE = "handle";

    public static final String HELPER = "helper";

    public static final String IFRAMEFIX = "iframeFix";

    public static final String OPACITY = "opacity";

    public static final String REFRESHPOSITIONS = "refreshPositions";

    public static final String REVERT = "revert";

    public static final String REVERTDURATION = "revertDuration";

    public static final String SCOPE = "scope";

    public static final String SCROLL = "scroll";

    public static final String SCROLLSENSITIVITY = "scrollSensitivity";

    public static final String SCROLLSPEED = "scrollSpeed";

    public static final String SNAP = "snap";

    public static final String SNAPMODE = "snapMode";

    public static final String SNAPTOLERANCE = "snapTolerance";

    public static final String STACK = "stack";

    public static final String ZINDEX = "zIndex";

    public static final JavaScriptFunction ONSTART = new JavaScriptFunction("start");

    public static final JavaScriptFunction ONDRAG = new JavaScriptFunction("drag");

    public static final JavaScriptFunction ONSTOP = new JavaScriptFunction("stop");

    /**
     * Default constructor
     */
    public Draggable() {
        super();
    }

    /**
     * @return  Gets the <code>COMPONENT_FAMILY</code> value to be used in the JSF lifecycle
     */
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
     * @param context
     * @return
     */
    public Object saveState(FacesContext context) {
        Object values[] = new Object[2];
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
     */
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        JSFUtility.renderScriptOnce(writer, this, context);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText("$(document).ready(function(){\n", null);
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(\"#" + clientId + "\").draggable({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, Draggable.class);
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.startElement("div", this);
        writer.writeAttribute("id", this.getId(), null);
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
     */
    public void encodeChildren(FacesContext context) throws IOException {
        for (UIComponent cm : (List<UIComponent>) getChildren()) {
            cm.encodeBegin(context);
            cm.encodeChildren(context);
            cm.encodeEnd(context);
        }
    }

    /**
     * @param context
     */
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
        writer.flush();
    }
}
