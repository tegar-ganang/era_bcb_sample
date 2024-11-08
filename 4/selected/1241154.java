package net.sf.jqueryfaces.component.dialog;

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
 * This <code>Dialog</code> component manages all the appropriate options that
 * can be applied, as well as, the rendering of itself.
 * 
 * @author Steve Armstrong
 */
public class Dialog extends UIComponentBase {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Dialog";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Dialog";

    protected static final String CONTENT = "content";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    public static final String AUTOOPEN = "autoOpen";

    public static final String BGIFRAME = "bgiframe";

    protected static final String BUTTONS = "buttons";

    public static final String CLOSEONESCAPE = "closeOnEscape";

    public static final String DRAGGABLE = "draggable";

    public static final String DIALOGCLASS = "dialogClass";

    public static final String HEIGHT = "height";

    public static final String HIDE = "hide";

    public static final String MAXHEIGHT = "maxHeight";

    public static final String MAXWIDTH = "maxWidth";

    public static final String MINHEIGHT = "minHeight";

    public static final String MINWIDTH = "minWidth";

    public static final String MODAL = "modal";

    public static final String POSITION = "position";

    public static final String RESIZABLE = "resizable";

    public static final String SHOW = "show";

    public static final String STACK = "stack";

    public static final String TITLE = "title";

    public static final String WIDTH = "width";

    public static final String ZINDEX = "zIndex";

    public static final JavaScriptFunction ONBEFORECLOSE = new JavaScriptFunction("beforeclose");

    public static final JavaScriptFunction ONOPEN = new JavaScriptFunction("open");

    public static final JavaScriptFunction ONFOCUS = new JavaScriptFunction("focus");

    public static final JavaScriptFunction ONDRAGSTART = new JavaScriptFunction("dragstart");

    public static final JavaScriptFunction ONDRAG = new JavaScriptFunction("drag");

    public static final JavaScriptFunction ONDRAGSTOP = new JavaScriptFunction("dragstop");

    public static final JavaScriptFunction ONRESIZESTART = new JavaScriptFunction("resizestart");

    public static final JavaScriptFunction ONRESIZE = new JavaScriptFunction("resize");

    public static final JavaScriptFunction ONRESIZESTOP = new JavaScriptFunction("resizestop");

    public static final JavaScriptFunction ONCLOSE = new JavaScriptFunction("close");

    /**
     * Default constructor
     */
    public Dialog() {
        super();
    }

    /**
     * @return Gets the <code>COMPONENT_FAMILY</code> value.
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
     * @throws IOException
     */
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        JSFUtility.renderScriptOnce(writer, this, context);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText("$(document).ready(function(){\n", null);
        writer.writeText("$(\"#" + this.getId() + "\").dialog({", null);
        Map attr = this.getAttributes();
        boolean commaNeeded = JSFUtility.writeJSObjectOptions(writer, attr, Dialog.class);
        if (attr.get(BUTTONS) != null) {
            if (commaNeeded) {
                writer.write(",");
            }
            writer.write("buttons : {");
            writer.write((String) attr.get(BUTTONS));
            writer.write("}");
        }
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.startElement("div", this);
        writer.writeAttribute("id", getId(), "Dialog");
        if (attr.get(TITLE) != null) {
            writer.writeAttribute("title", (String) attr.get(TITLE), "Dialog");
        }
        writer.flush();
    }

    /**
     * @param context
     * @throws IOException
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
     * @throws IOException
     */
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
        writer.flush();
    }
}
