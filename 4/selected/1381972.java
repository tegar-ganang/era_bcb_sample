package net.sf.jqueryfaces.component.menu;

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
 * This component is the parent class of <code>MenuItem</code>, and should be 
 * used with these types children only.  This holds any global values that are
 * to be applied to this menu.  This renders itself, and tells its children when
 * to be rendered.
 * 
 * 
 * @author Jeremy Buis
 */
public class Menu extends UIComponentBase {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Menu";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Menu";

    protected static final String REQUEST_MAP_MENU = "menu";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    public static final String TYPE = "type";

    public static final String MODE = "mode";

    public static final String ITEMS = "items";

    public static final String APPENDTO = "appendTo";

    public static final String EXCLUSIVE = "exclusive";

    public static final String WIDTH = "width";

    public static final String MAXHEIGHT = "maxHeight";

    public static final String FORCEDIRECTION = "forceDirection";

    public static final String DIRECTION = "direction";

    public static final String FLYOUTDELAY = "flyoutDelay";

    public static final String CROSSSPEED = "crossSpeed";

    public static final String BACKLINK = "backLink";

    public static final String BACKLINKTEXT = "backLinkText";

    public static final String TOPLINKTEXT = "topLinkText";

    public static final String CRUMBDEFAULTTEXT = "crumbDefaultText";

    public static final String SELECTCATEGORIES = "selectCategories";

    public static final JavaScriptFunction ONBROWSE = new JavaScriptFunction("browse");

    public static final JavaScriptFunction ONCLOSE = new JavaScriptFunction("close");

    public static final JavaScriptFunction ONCHOOSE = new JavaScriptFunction("choose");

    public static final JavaScriptFunction ONOPEN = new JavaScriptFunction("open");

    /**
     * Default constructor
     */
    public Menu() {
        super();
    }

    /**
     * @return  Gets the <code>COMPONENT_FAMILY</code>.
     */
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
      * @param context
      * @return
      */
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
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

    public boolean getRendersChildren() {
        return true;
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        JSFUtility.renderScriptOnce(writer, this, context);
        String[] jsfiles = { "plugins/menu/jquery-ui-menu.js" };
        String[] cssfiles = { "plugins/menu/css/jquery-ui-menu.css", "plugins/menu/css/jquery-ui-themeroller.css" };
        JSFUtility.renderScriptOnce(writer, context, this, jsfiles, cssfiles, REQUEST_MAP_MENU);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText("$(document).ready(function(){\n", null);
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(\"#" + clientId + "\").menu({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, Menu.class);
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.startElement("div", this);
        writer.writeAttribute("id", getClientId(context), "Menu");
        writer.startElement("ul", this);
        writer.flush();
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeChildren(FacesContext context) throws IOException {
        List<UIComponent> children = getChildren();
        for (UIComponent child : children) {
            child.encodeBegin(context);
            child.encodeChildren(context);
            child.encodeEnd(context);
        }
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("ul");
        writer.endElement("div");
        writer.flush();
    }
}
