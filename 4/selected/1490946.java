package net.sf.jqueryfaces.component.dropdown;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import net.sf.jqueryfaces.util.JSFUtility;
import net.sf.jqueryfaces.util.JavaScriptFunction;

public class DropDown extends UIInput {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.SexyDropDown";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.SexyDropDown";

    protected static final String REQUEST_MAP_SEXCOMBO = "SexyCombo";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    protected static final String JSCREATE = "create";

    protected static final String NAME = "name";

    protected static final String DATA = "data";

    protected static final String MULTIPLE = "multiple";

    protected static final String CONTAINER = "container";

    protected static final String URL = "url";

    protected static final String AJAXDATA = "ajaxData";

    protected static final String VALUE = "value";

    public static final String SKIN = "skin";

    public static final String SUFFIX = "suffix";

    public static final String HIDDENSUFFIX = "hiddenSuffix";

    public static final String INITIALHIDDENVALUE = "initialHiddenValue";

    public static final String EMPTYTEXT = "emptyText";

    public static final String AUTOFILL = "autoFill";

    public static final String TRIGGERSELECTED = "triggerSelected";

    public static final JavaScriptFunction FILTERFN = new JavaScriptFunction("filterFn", "currVal,itemVal,textVal");

    public static final String DROPUP = "dropUp";

    public static final String SEPARATOR = "separator";

    public static final String READONLY = "readonly";

    public static final JavaScriptFunction SHOWLISTCALLBACK = new JavaScriptFunction("showListCallback");

    public static final JavaScriptFunction HIDELISTCALLBACK = new JavaScriptFunction("hideListCallback");

    public static final JavaScriptFunction INITCALLBACK = new JavaScriptFunction("initCallback");

    public static final JavaScriptFunction INITEVENTSCALLBACK = new JavaScriptFunction("initEventsCallback");

    public static final JavaScriptFunction CHANGECALLBACK = new JavaScriptFunction("changeCallback");

    public static final JavaScriptFunction TEXTCHANGECALLBACK = new JavaScriptFunction("textChangeCallback");

    /**
     * Default constructor
     */
    public DropDown() {
        super();
    }

    /**
     * @return
     */
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public void decode(FacesContext context) {
        Map requestMap = context.getExternalContext().getRequestParameterMap();
        String clientId = this.getClientId(context);
        String submittedValue = (String) requestMap.get(clientId);
        setSubmittedValue(submittedValue);
        ValueBinding vb = this.getValueBinding("value");
        if (vb != null) {
            vb.setValue(context, submittedValue);
        }
        setValid(true);
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
     * This method is usually included if the value is set to true.
     * The default value is false, and will be set as such.
     * 
     * @return  Gets the status of <code>getRendersChildren()</code> 
     */
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
        String[] jsfiles = { "plugins/sexycombo/jquery.sexy-combo-2.0.6.js" };
        String[] cssfiles = { "plugins/sexycombo/sexy-combo.css", "plugins/sexycombo/sexy.css" };
        JSFUtility.renderScriptOnce(writer, context, this, jsfiles, cssfiles, REQUEST_MAP_SEXCOMBO);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        Map attr = this.getAttributes();
        writer.writeText("$(document).ready(function(){\n", null);
        if (attr.get(JSCREATE) != null && (Boolean) attr.get(JSCREATE) == true) {
            writer.writeText("$.sexyCombo.create({", null);
            String clientId = getClientId(context);
            attr.put("clientId", clientId);
            boolean comma = false;
            comma = JSFUtility.writeJSObjectOption(writer, "id", attr, "clientId", String.class, comma);
            comma = JSFUtility.writeJSObjectOption(writer, NAME, attr, "clientId", String.class, comma);
            comma = JSFUtility.writeJSObjectOption(writer, MULTIPLE, attr, MULTIPLE, String.class, comma);
            if (attr.get(CONTAINER) == null || attr.get(CONTAINER).equals("")) {
                if (comma) {
                    writer.write(",");
                }
                writer.write("container:\"#" + getId() + "__container\"");
                comma = true;
            } else {
                comma = JSFUtility.writeJSObjectOption(writer, CONTAINER, attr, CONTAINER, String.class, comma);
            }
            comma = JSFUtility.writeJSObjectOption(writer, URL, attr, URL, String.class, comma);
            comma = JSFUtility.writeJSObjectOption(writer, AJAXDATA, attr, AJAXDATA, String.class, comma);
            if (comma) {
                writer.write(",");
            }
            JSFUtility.writeJSObjectOptions(writer, attr, DropDown.class);
            writer.writeText("});\n", null);
        } else {
            String clientId = getClientId(context);
            clientId = clientId.replace(":", "\\\\:");
            writer.writeText("$(\"#" + clientId + "\").sexyCombo({", null);
            JSFUtility.writeJSObjectOptions(writer, attr, DropDown.class);
            writer.writeText("});\n", null);
        }
        writer.writeText("});", null);
        writer.endElement("script");
        if (!Boolean.TRUE.equals(attr.get(JSCREATE))) {
            writer.startElement("select", this);
            writer.writeAttribute("id", getClientId(context), "DropDown");
            writer.writeAttribute("name", getClientId(context), "DropDown");
            if (getValue() != null) {
                writer.writeAttribute("value", getValue(), "value");
            }
            if (attr.get(STYLE) != null) {
                writer.writeAttribute("style", (String) attr.get(STYLE), "DropDown");
            }
            if (attr.get(STYLECLASS) != null) {
                writer.writeAttribute("class", (String) attr.get(STYLECLASS), "DropDown");
            }
        } else if (attr.get(CONTAINER) == null || attr.get(CONTAINER).equals("")) {
            writer.startElement("div", this);
            writer.writeAttribute("id", getId() + "__container", "id");
            writer.endElement("div");
        }
        writer.flush();
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeChildren(FacesContext context) throws IOException {
        if (Boolean.TRUE.equals(JSCREATE)) {
            return;
        }
        ResponseWriter writer = context.getResponseWriter();
        for (UIComponent cm : (List<UIComponent>) getChildren()) {
            UISelectItem si = (UISelectItem) cm;
            writer.startElement("option", si);
            writer.writeAttribute("value", si.getItemValue(), "value");
            writer.writeText(si.getItemLabel(), null);
            writer.endElement("option");
        }
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Map attr = this.getAttributes();
        if (!Boolean.TRUE.equals(attr.get(JSCREATE))) {
            writer.endElement("select");
        }
        writer.flush();
    }
}
