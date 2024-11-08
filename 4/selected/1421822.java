package net.sf.jqueryfaces.component.rte;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import net.sf.jqueryfaces.util.JSFUtility;

/**
 * 
 * @author Steve Armstrong
 */
public class RichTextEditor extends UIInput {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.FCKEditor";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.FCKEditor";

    protected static final String REQUEST_MAP_FCK = "fckeditor";

    protected static final String REQUEST_MAP_FCK_COUNT = "fckeditor_count";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    protected static final String ROWS = "rows";

    protected static final String COLS = "cols";

    protected static final String VALUE = "value";

    public static final String HEIGHT = "height";

    public static final String WIDTH = "width";

    public static final String TOOLBAR = "toolbar";

    public static final String CONFIG = "config";

    private String _width;

    private String _height;

    private String _toolbar;

    private String _config;

    private String _style;

    private String _styleClass;

    private String _rows;

    private String _cols;

    public RichTextEditor() {
        super();
    }

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /**
     * @param context
     * @return
     */
    public Object saveState(FacesContext context) {
        Object values[] = new Object[10];
        values[0] = super.saveState(context);
        values[1] = _width;
        values[2] = _height;
        values[3] = _toolbar;
        values[4] = _config;
        values[5] = _style;
        values[6] = _styleClass;
        values[7] = _rows;
        values[8] = _cols;
        return (values);
    }

    /**
     * @param context
     * @param state
     */
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        _width = (String) values[1];
        _height = (String) values[2];
        _toolbar = (String) values[3];
        _config = (String) values[4];
        _style = (String) values[5];
        _styleClass = (String) values[6];
        _rows = (String) values[7];
        _cols = (String) values[8];
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

    public void encodeBegin(FacesContext context) throws IOException {
    }

    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        JSFUtility.renderScriptOnce(writer, this, context);
        String[] jsfiles = { "plugins/fckeditor/jquery.MetaData.js", "plugins/fckeditor/jquery.form.js", "plugins/fckeditor/jquery.FCKEditor.js" };
        JSFUtility.renderScriptOnce(writer, context, this, jsfiles, null, REQUEST_MAP_FCK);
        Map requestMap = context.getExternalContext().getRequestMap();
        Integer fckCounter = (Integer) requestMap.get(REQUEST_MAP_FCK_COUNT);
        if (fckCounter == null) {
            fckCounter = new Integer(0);
        } else {
            fckCounter++;
        }
        requestMap.put(REQUEST_MAP_FCK_COUNT, fckCounter);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        Map attr = this.getAttributes();
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(document).ready(function(){\n", null);
        writer.writeText("$(\"#", null);
        writer.writeText(clientId, null);
        writer.writeText("\").fck({", null);
        boolean commaNeeded = JSFUtility.writeJSObjectOptions(writer, attr, RichTextEditor.class);
        if (commaNeeded == true) {
            writer.writeText(",", null);
        }
        writer.writeText("waitFor: 0, path: 'plugins/fckeditor/'}); });", null);
        writer.endElement("script");
        writer.startElement("textarea", this);
        writer.writeAttribute("id", getClientId(context), null);
        writer.writeAttribute("name", getClientId(context), null);
        if (getStyle() != null) {
            writer.writeAttribute("style", getStyle(), null);
        }
        if (getStyleClass() != null) {
            writer.writeAttribute("class", getStyleClass() + " jsfqueryfck" + fckCounter, null);
        } else {
            writer.writeAttribute("class", "jsfqueryfck" + fckCounter, null);
        }
        if (getRows() != null) {
            writer.writeAttribute("rows", getRows(), null);
        }
        if (getCols() != null) {
            writer.writeAttribute("cols", getCols(), null);
        }
        if (super.getValue() != null) {
            writer.writeText(super.getValue(), null);
        }
        writer.endElement("textarea");
        writer.flush();
    }

    public String getWidth() {
        return (String) JSFUtility.componentGetter(_width, WIDTH, this);
    }

    public void setWidth(String width) {
        _width = width;
    }

    public String getHeight() {
        return (String) JSFUtility.componentGetter(_height, HEIGHT, this);
    }

    public void setHeight(String height) {
        _height = height;
    }

    public String getToolbar() {
        return (String) JSFUtility.componentGetter(_toolbar, TOOLBAR, this);
    }

    public void setToolbar(String toolbar) {
        _toolbar = toolbar;
    }

    public String getConfig() {
        return (String) JSFUtility.componentGetter(_config, CONFIG, this);
    }

    public void setConfig(String config) {
        _config = config;
    }

    public String getStyle() {
        return (String) JSFUtility.componentGetter(_style, STYLE, this);
    }

    public void setStyle(String style) {
        _style = style;
    }

    public String getStyleClass() {
        return (String) JSFUtility.componentGetter(_styleClass, STYLECLASS, this);
    }

    public void setStyleClass(String styleClass) {
        _styleClass = styleClass;
    }

    public String getRows() {
        return (String) JSFUtility.componentGetter(_rows, ROWS, this);
    }

    public void setRows(String rows) {
        _rows = rows;
    }

    public String getCols() {
        return (String) JSFUtility.componentGetter(_cols, COLS, this);
    }

    public void setCols(String cols) {
        _cols = cols;
    }
}
