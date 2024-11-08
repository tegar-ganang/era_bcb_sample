package net.sf.jqueryfaces.component.tree;

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
 * This class is the main Tree parent for all the TreeItems that it could have.
 * From here all the global options that can be applied to a tree structure are
 * set.  This also renders itself.
 * 
 * @author Jeremy Buis
 */
public class Tree extends UIComponentBase {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Tree";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.Tree";

    protected static final String REQUEST_MAP_TREE = "tree";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    public static final String TITLE = "title";

    public static final String ROOTVISIBLE = "rootVisible";

    public static final String MINEXPANDLEVEL = "minExpandLevel";

    public static final String IMAGEPATH = "imagePath";

    public static final String INITID = "initId";

    public static final String INITAJAX = "initAjax";

    public static final String AUTOFOCUS = "autoFocus";

    public static final String KEYBOARD = "keyboard";

    public static final String PERSIST = "persist";

    public static final String AUTOCOLLAPSE = "autoCollapse";

    public static final String CLICKFOLDERMODE = "clickFolderMode";

    public static final String ACTIVEVISIBLE = "activeVisible";

    public static final String CHECKBOX = "checkbox";

    public static final String SELECTMODE = "selectMode";

    public static final String FX = "fx";

    public static final JavaScriptFunction ONCLICK = new JavaScriptFunction("onClick", "dtnode, event");

    public static final JavaScriptFunction ONDBLCLICK = new JavaScriptFunction("onDblClick", "dtnode, event");

    public static final JavaScriptFunction ONKEYDOWN = new JavaScriptFunction("onKeydown", "dtnode, event");

    public static final JavaScriptFunction ONKEYPRESS = new JavaScriptFunction("onKeypress", "dtnode, event");

    public static final JavaScriptFunction ONFOCUS = new JavaScriptFunction("onFocus", "dtnode, event");

    public static final JavaScriptFunction ONBLUR = new JavaScriptFunction("onBlur", "dtnode, event");

    public static final JavaScriptFunction ONQUERYACTIVATE = new JavaScriptFunction("onQueryActivate", "flag, dtnode");

    public static final JavaScriptFunction ONQUERYSELECT = new JavaScriptFunction("onQuerySelect", "flag, dtnode");

    public static final JavaScriptFunction ONQUERYEXPAND = new JavaScriptFunction("onQueryExpand", "flag, dtnode");

    public static final JavaScriptFunction ONACTIVATE = new JavaScriptFunction("onActivate", "dtnode");

    public static final JavaScriptFunction ONDEACTIVATE = new JavaScriptFunction("onDeactivate", "dtnode");

    public static final JavaScriptFunction ONSELECT = new JavaScriptFunction("onSelect", "flag, dtnode");

    public static final JavaScriptFunction ONEXPAND = new JavaScriptFunction("onExpand", "flag, dtnode");

    public static final JavaScriptFunction ONLAZYREAD = new JavaScriptFunction("onLazyRead", "dtnode");

    public static final String AJAXDEFAULTS = "ajaxDefaults";

    public static final String STRINGS = "strings";

    public static final String IDPREFIX = "idPrefix";

    public static final String COOKIEID = "cookieId";

    public static final String COOKIE = "cookie";

    public static final String DEBUGLEVEL = "debugLevel";

    /**
     * Default cosntructor
     */
    public Tree() {
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

    /**
     * @param context
     * @throws IOException
     */
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        JSFUtility.renderScriptOnce(writer, this, context);
        String[] jsfiles = { "plugins/dynatree/jquery.dynatree.min.js" };
        String[] cssfiles = { "plugins/dynatree/skin/ui.dynatree.css" };
        JSFUtility.renderScriptOnce(writer, context, this, jsfiles, cssfiles, REQUEST_MAP_TREE);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText("$(document).ready(function(){\n", null);
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(\"#" + clientId + "\").dynatree({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, Tree.class);
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.startElement("div", this);
        writer.writeAttribute("id", clientId, "Tree");
        writer.startElement("ul", this);
        writer.writeAttribute("id", clientId + "Data", "treeData");
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
        writer.endElement("ul");
        writer.endElement("div");
        writer.flush();
    }
}
