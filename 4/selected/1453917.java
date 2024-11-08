package net.sf.jqueryfaces.component.borderlayout;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import net.sf.jqueryfaces.util.JSFUtility;

/**
* This plugin is ridiculous
*
*
*
*
*
*
*
*@author Jeremy Buis
*/
public class BorderLayout extends UIComponentBase {

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.BorderLayout";

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.BorderLayout";

    protected static final String REQUEST_MAP_BORDERLAYOUT = "borderlayout";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    public static final String APPLYDEFAULTSTYLES = "applyDefaultStyles";

    public static final String PANECLASS = "paneClass";

    public static final String RESIZERCLASS = "resizerClass";

    public static final String TOGGLERCLASS = "togglerClass";

    public static final String BUTTONCLASS = "buttonClass";

    public static final String MASKIFRAMESONRESIZE = "maskIframesOnResize";

    public static final String SLIDERTIP = "sliderTip";

    public static final String SLIDERCURSOR = "slideCursor";

    public static final String ENABLECURSORHOTKEY = "enableCursorHotkey";

    public static final String CUSTOMHOTKEYMODIFIER = "customHotkeyModifier";

    public static final String CUSTOMHOTKEY = "customHotkey";

    public static final String FXNAME = "fxName";

    public static final String FXNAME_OPEN = "fxName_open";

    public static final String FXNAME_CLOSE = "fxName_close";

    public static final String FXSPEED = "fxSpeed";

    public static final String FXSPEED_OPEN = "fxSpeed_open";

    public static final String FXSPEED_CLOSE = "fxSpeed_close";

    public static final String FXSETTINGS = "fxSettings";

    public static final String FXSETTINGS_OPEN = "fxSettings_open";

    public static final String FXSETTINGS_CLOSE = "fxSettings_close";

    /**
	* Default constructor.	
	*/
    public BorderLayout() {
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
        Object[] values = new Object[1];
        values[0] = super.saveState(context);
        return values;
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
        String[] jsfiles = { "plugins/layout/jquery.layout.js" };
        String[] cssfiles = { "plugins/layout/css/layout-default.css" };
        JSFUtility.renderScriptOnce(writer, context, this, jsfiles, cssfiles, REQUEST_MAP_BORDERLAYOUT);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(document).ready(function(){\n$(\"body\").layout({\n" + "				applyDefaultStyles: true\n" + "				//,center : {\n" + "				//	paneSelector : '#" + clientId + "'\n" + "				//}\n" + "			});", null);
        writer.writeText("window[\"" + getId() + "\"] = $(\"#" + clientId + "\").layout({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, BorderLayout.class);
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.startElement("div", this);
        writer.writeAttribute("id", getClientId(context), null);
        String style = (String) attr.get(STYLE);
        if (null == style || style.trim().equals("")) {
            style = "height:100%;position:relative;overflow:hidden;";
        } else if (style.endsWith(";")) {
            style += "height:100%;position:relative;overflow:hidden;";
        } else {
            style += ";height:100%;position:relative;overflow:hidden;";
        }
        writer.writeAttribute("style", style, null);
        if (this.getAttributes().get(STYLECLASS) != null) {
            writer.writeAttribute("class", getAttributes().get(STYLECLASS), null);
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
