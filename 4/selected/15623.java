package net.sf.jqueryfaces.component.fileuploader;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import net.sf.jqueryfaces.component.dialog.Dialog;
import net.sf.jqueryfaces.util.JSFUtility;
import net.sf.jqueryfaces.util.JavaScriptFunction;

/**
 * This component, <code>FileUploader</code>, handles options that need to be 
 * applied to the JS ui object.  It also handles the rendering of this.
 * 
 * @author Jeremy Buis
 */
public class FileUploader extends UIComponentBase {

    public static final String COMPONENT_TYPE = "net.sf.jqueryfaces.FileUploader";

    public static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.FileUploader";

    protected static final String REQUEST_MAP_FILEUPLOADER = "fileuploader";

    public static final String STYLE = "class";

    public static final String STYLECLASS = "styleClass";

    public static final String TYPE = "type";

    public static final String NAME = "name";

    public static final String CLASS = "class";

    public static final String MAXLENGTH = "maxLength";

    public static final String ACCEPT = "accept";

    public static final String LIST = "list";

    public static final JavaScriptFunction ONFILEAPPEND = new JavaScriptFunction("onFileAppend");

    public static final JavaScriptFunction AFTERFILEAPPEND = new JavaScriptFunction("afterFileAppend");

    public static final JavaScriptFunction ONFILESELECT = new JavaScriptFunction("onFileSelect");

    public static final JavaScriptFunction AFTERFILESELECT = new JavaScriptFunction("afterFileSelect");

    public static final JavaScriptFunction ONFILEREMOVE = new JavaScriptFunction("onFileRemove");

    public static final JavaScriptFunction AFTERFILEREMOVE = new JavaScriptFunction("afterFileRemove");

    /**
     * Default constructor
     */
    public FileUploader() {
        super();
    }

    /**
      * @return
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
        String[] jsfiles = { "plugins/fileupload/jquery.MultiFile.js" };
        JSFUtility.renderScriptOnce(writer, context, this, jsfiles, null, REQUEST_MAP_FILEUPLOADER);
        writer.startElement("script", this);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText("$(document).ready(function(){\n", null);
        String clientId = getClientId(context);
        clientId = clientId.replace(":", "\\\\:");
        writer.writeText("$(\"#" + clientId + "\").MultiFile({", null);
        Map attr = this.getAttributes();
        JSFUtility.writeJSObjectOptions(writer, attr, FileUploader.class);
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
        writer.startElement("div", this);
        writer.writeAttribute("id", getClientId(context) + "_wrap", null);
        writer.startElement("input", this);
        writer.writeAttribute("id", getClientId(context), null);
        writer.writeAttribute("type", "file", null);
        writer.writeAttribute("name", "", null);
        if (attr.get(MAXLENGTH) != null) {
            writer.writeAttribute("maxlength", attr.get(MAXLENGTH), null);
        }
        if (attr.get(LIST) != null) {
            writer.writeAttribute("list", attr.get(MAXLENGTH), null);
        }
        if (attr.get(ACCEPT) != null) {
            writer.writeAttribute("accept", attr.get(ACCEPT), null);
        }
        if (attr.get(CLASS) != null) {
            writer.writeAttribute("class", attr.get(CLASS), null);
        }
        writer.endElement("input");
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
    }
}
