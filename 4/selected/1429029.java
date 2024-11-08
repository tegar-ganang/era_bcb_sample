package net.sf.jqueryfaces.component.tab;

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
 * This component is the parent class of <code>TabItem</code> components.  <code>
 * Tab</code> components should be the only type of children that are nested
 * inside of this.  This renders itself and its children.
 * 
 * @author Jeremy Buis
 */
public class Tabs extends UIComponentBase {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Tabs";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Tabs";

    public static final String STYLE = "style";

    public static final String STYLECLASS = "styleClass";

    public static final String AJAXOPTIONS = "ajaxOptions";

    public static final String CACHE = "cache";

    public static final String COLLAPSIBLE = "collapsible";

    public static final String COOKIE = "cookie";

    public static final String DESELECTABLE = "deselectable";

    public static final String DISABLED = "disabled";

    public static final String EVENT = "event";

    public static final String FX = "fx";

    public static final String IDPREFIX = "idPrefix";

    public static final String PANELTEMPLATE = "panelTemplate";

    public static final String SELECTED = "selected";

    public static final String SPINNER = "spinner";

    public static final String TABTEMPLATE = "tabTemplate";

    public static final String CONTENT = "content";

    public static final JavaScriptFunction ONSELECT = new JavaScriptFunction("onSelect");

    public static final JavaScriptFunction ONLOAD = new JavaScriptFunction("onLoad");

    public static final JavaScriptFunction ONSHOW = new JavaScriptFunction("onShow");

    public static final JavaScriptFunction ONADD = new JavaScriptFunction("onAdd");

    public static final JavaScriptFunction ONREMOVE = new JavaScriptFunction("onRemove");

    public static final JavaScriptFunction ONENABLE = new JavaScriptFunction("onEnable");

    public static final JavaScriptFunction ONDISABLE = new JavaScriptFunction("onDisable");

    ;

    private String _content;

    /**
     * Default constructor.
     */
    public Tabs() {
        super();
    }

    /**
     * @param content   Sets the value of <code>_content</code>.
     */
    public void setContent(String content) {
        _content = content;
    }

    /**
     * @return  Gets the value of <code>_content</code>.
     */
    public String getContent() {
        return (String) JSFUtility.componentGetter(_content, CONTENT, this);
    }

    /**
     * @return  Gets the <code>COMPONENT_FAMILY</code>
     */
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
     * By default this value is set to false
     * @return  Gets the value of <code>getRendersChildren()</code>.
     */
    public boolean getRendersChildren() {
        return true;
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
        writer.writeText("$(\"#" + clientId + "\").tabs({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, Tabs.class);
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.flush();
        writer.startElement("div", this);
        writer.writeAttribute("id", clientId, "id");
        writer.startElement("ul", this);
        for (UIComponent c : (List<UIComponent>) getChildren()) {
            TabItem t = (TabItem) c;
            writer.startElement("li", this);
            writer.startElement("a", this);
            String url = t.getUrl();
            if (url != null) {
                writer.writeAttribute("href", url, url);
            } else {
                writer.writeAttribute("href", "#" + c.getId(), "#" + c.getId());
            }
            writer.writeText(c.getId(), "childTab");
            writer.endElement("a");
            writer.endElement("li");
        }
        writer.endElement("ul");
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
        if (getContent() != null) {
            writer.write(getContent());
        }
        writer.endElement("div");
        writer.flush();
    }
}
