package net.sf.jqueryfaces.component.datepicker;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import net.sf.jqueryfaces.util.JSFUtility;
import net.sf.jqueryfaces.util.JavaScriptFunction;

/**
 * <code>DatePicker</code> component that is used to render the DatePicker
 * jQuery UI object.  It holds all the options that can be applied to the
 * <code>DatePicker</code> component that the get applied to the JS
 * object. 
 * 
 * @author Jeremy Buis
 */
public class DatePicker extends UIInput {

    protected static final String COMPONENT_TYPE = "net.sf.jqueryfaces.Datepicker";

    protected static final String COMPONENT_FAMILY = "net.sf.jqueryfaces.DatePicker";

    protected static final String STYLE = "style";

    protected static final String STYLECLASS = "styleClass";

    protected static final String INLINE = "inline";

    protected static final String VALUE = "value";

    public static final String ALTFIELD = "altField";

    public static final String ALTFORMAT = "altFormat";

    public static final String APPENDTEXT = "appendText";

    public static final String BUTTONIMAGE = "buttonImage";

    public static final String BUTTONIMAGEONLY = "buttonImageOnly";

    public static final String BUTTONTEXT = "buttonText";

    public static final String CHANGEMONTH = "changeMonth";

    public static final String CHANGEYEAR = "changeYear";

    public static final String CLOSETEXT = "closeText";

    public static final String CONSTRAININPUT = "constrainInput";

    public static final String CURRENTTEXT = "currentText";

    private static final String DATEFORMAT = "dateFormat";

    protected static final String DAYNAMES = "dayNames";

    protected static final String DAYNAMESMIN = "dayNamesMin";

    protected static final String DAYNAMESSHORT = "dayNamesShort";

    public static final String DEFAULTDATE = "defaultDate";

    public static final String DURATION = "duration";

    public static final String FIRSTDAY = "firstDay";

    public static final String GOTOCURRENT = "goToCurrent";

    public static final String HIDEIFNOPREVNEXT = "hideIfNoPrevNext";

    public static final String ISRTL = "isRTL";

    protected static final String MAXDATE = "maxDate";

    protected static final String MINDATE = "minDate";

    protected static final String MONTHNAMES = "monthNames";

    protected static final String MONTHNAMESSHORT = "monthNamesShort";

    public static final String NAVIGATIONASDATEFORMAT = "navigationAsDateFormat";

    public static final String NEXTTEXT = "nextText";

    public static final String NUMBEROFMONTHS = "numberOfMonths";

    public static final String PREVTEXT = "prevText";

    public static final String SHORTYEARCUTOFF = "shortYearCutOff";

    public static final String SHOWANIM = "showAnim";

    public static final String SHOWBUTTONPANEL = "showButtonPanel";

    public static final String SHOWCURRENTATPOS = "showCurrentAtPos";

    public static final String SHOWMONTHAFTERYEAR = "showMonthAfterYear";

    public static final String SHOWON = "showOn";

    public static final String SHOWOPTIONS = "showOptions";

    public static final String SHOWOTHERMONTHS = "showOtherMonths";

    public static final String STEPMONTHS = "stepMonths";

    public static final String YEARRANGE = "yearRange";

    public static final JavaScriptFunction ONBEFORESHOW = new JavaScriptFunction("onbeforeShow", "input");

    public static final JavaScriptFunction ONBEFORESHOWDAY = new JavaScriptFunction("onNeforeShowDay", "date");

    public static final JavaScriptFunction ONCHANGEMONTHYEAR = new JavaScriptFunction("onChangeMonthYear", "date");

    public static final JavaScriptFunction ONCLOSE = new JavaScriptFunction("onClose", "date");

    public static final JavaScriptFunction ONSELECT = new JavaScriptFunction("onSelect", "dateText");

    private Date _value = null;

    /**
     * Default constructor
     */
    public DatePicker() {
        super();
        this.setConverter(new DatePickerConverter());
    }

    /**
     * @param value Sets the value of <code>_value</code>
     */
    public void setValue(Date value) {
        _value = value;
    }

    /**
     * @return  Gets the value of <code>_value</code>
     */
    public Date getValue() {
        return (Date) JSFUtility.componentGetter(_value, VALUE, this);
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
        Object values[] = new Object[2];
        values[0] = super.saveState(context);
        values[1] = _value;
        return (values);
    }

    /**
     * @param context
     * @param state
     */
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        _value = (Date) values[1];
    }

    /**
     * @param context
     */
    public void decode(FacesContext context) {
        String clientId = getClientId(context);
        String submittedValue = (String) context.getExternalContext().getRequestParameterMap().get(clientId);
        setSubmittedValue(submittedValue);
        setValid(true);
    }

    /**
     * @param context
     */
    public void validate(FacesContext context) {
        super.validate(context);
        if (getLocalValue() != null) {
            Date date = (Date) getLocalValue();
            Date now = new Date();
            if (now.compareTo(date) < 0) {
                setValid(false);
            }
        }
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
        writer.writeText("$(\"#" + clientId + "\").datepicker({", null);
        Map attr = this.getAttributes();
        boolean needed = JSFUtility.writeJSObjectOptions(writer, attr, DatePicker.class);
        if (getAttributes().get(DATEFORMAT) != null) {
            if (needed == true) {
                writer.write(",");
            }
            writer.write(DATEFORMAT + " : '" + convertFormat(getAttributes().get(DATEFORMAT)) + "'");
            needed = true;
        }
        if (getAttributes().get(MINDATE) != null) {
            if (needed == true) {
                writer.write(",");
            }
            String val = (String) getAttributes().get(MINDATE);
            val = val.replace("'", "\\'");
            writer.write(MINDATE + " : (function(){try { return eval('" + val + "'); } catch(e){ return '" + val + "'; }})()");
            needed = true;
        }
        if (getAttributes().get(MAXDATE) != null) {
            if (needed == true) {
                writer.write(",");
            }
            String val = (String) getAttributes().get(MAXDATE);
            val = val.replace("'", "\\'");
            writer.write(MAXDATE + " : (function(){try { return eval('" + val + "'); } catch(e){ return '" + val + "'; }})()");
            needed = true;
        }
        if (getAttributes().get(DAYNAMES) != null) {
            if (needed == true) {
                writer.write(",");
            }
            String val = (String) getAttributes().get(DAYNAMES);
            val = val.replace("'", "\\'");
            writer.write(DAYNAMES + " : (function(){try { return eval('" + val + "'); } catch(e){ return '" + val + "'; }})()");
            needed = true;
        }
        if (getAttributes().get(DAYNAMESMIN) != null) {
            if (needed == true) {
                writer.write(",");
            }
            String val = (String) getAttributes().get(DAYNAMESMIN);
            val = val.replace("'", "\\'");
            writer.write(DAYNAMESMIN + " : (function(){try { return eval('" + val + "'); } catch(e){ return '" + val + "'; }})()");
            needed = true;
        }
        if (getAttributes().get(DAYNAMESSHORT) != null) {
            if (needed == true) {
                writer.write(",");
            }
            String val = (String) getAttributes().get(DAYNAMESSHORT);
            val = val.replace("'", "\\'");
            writer.write(DAYNAMESSHORT + " : (function(){try { return eval('" + val + "'); } catch(e){ return '" + val + "'; }})()");
            needed = true;
        }
        if (getAttributes().get(MONTHNAMES) != null) {
            if (needed == true) {
                writer.write(",");
            }
            String val = (String) getAttributes().get(MONTHNAMES);
            val = val.replace("'", "\\'");
            writer.write(MONTHNAMES + " : (function(){try { return eval('" + val + "'); } catch(e){ return '" + val + "'; }})()");
            needed = true;
        }
        if (getAttributes().get(MONTHNAMESSHORT) != null) {
            if (needed == true) {
                writer.write(",");
            }
            String val = (String) getAttributes().get(MONTHNAMESSHORT);
            val = val.replace("'", "\\'");
            writer.write(MONTHNAMESSHORT + " : (function(){try { return eval('" + val + "'); } catch(e){ return '" + val + "'; }})()");
            needed = true;
        }
        writer.writeText("});\n" + "});", null);
        writer.endElement("script");
    }

    private String convertFormat(Object currentFormat) {
        String nDate = (String) currentFormat;
        nDate = nDate.replace("a", "");
        nDate = nDate.replace("H", "");
        nDate = nDate.replace("k", "");
        nDate = nDate.replace("K", "");
        nDate = nDate.replace("h", "");
        nDate = nDate.replace("m", "");
        nDate = nDate.replace("s", "");
        nDate = nDate.replace("S", "");
        nDate = nDate.replace("z", "");
        nDate = nDate.replace("Z", "");
        nDate = nDate.replace("w", "");
        nDate = nDate.replace("W", "");
        nDate = nDate.replace("F", "");
        nDate = nDate.replace("G", "");
        if (nDate.contains("MMMM")) {
            nDate = nDate.replace("MMMM", "MM");
        } else if (nDate.contains("MMM")) {
            nDate = nDate.replace("MMM", "M");
        } else if (nDate.contains("MM")) {
            nDate = nDate.replace("MM", "mm");
        } else if (nDate.contains("M")) {
            nDate = nDate.replace("M", "m");
        }
        if (nDate.contains("yyyy")) {
            nDate = nDate.replace("yyyy", "yy");
        } else if (nDate.contains("yy")) {
            nDate = nDate.replace("yy", "y");
        }
        if (nDate.contains("D")) {
            nDate = nDate.replace("D", "o");
            while (nDate.contains("ooo")) nDate = nDate.replaceAll("ooo", "oo");
        }
        if (nDate.contains("EEEE")) {
            nDate = nDate.replace("EEEE", "DD");
        } else if (nDate.contains("E")) {
            nDate = nDate.replaceAll("E", "D");
            while (nDate.contains("DD")) nDate = nDate.replaceAll("DD", "D");
        }
        return nDate;
    }

    /**
     * @param context
     * @throws IOException
     */
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        if (this.getAttributes().get(INLINE) != null) {
            writer.startElement("div", this);
            writer.writeAttribute("id", this.getClientId(context), null);
            writer.endElement("div");
        } else {
            writer.startElement("input", this);
            writer.writeAttribute("type", "text", null);
            writer.writeAttribute("id", this.getClientId(context), null);
            writer.writeAttribute("name", this.getClientId(context), null);
            if (getValue() != null) {
                writer.writeAttribute("value", this.getConverter().getAsString(context, this, getValue()), null);
            } else {
            }
            if (this.getAttributes().get("style") != null) {
                writer.writeAttribute("style", this.getAttributes().get("style"), null);
            }
            if (this.getAttributes().get("styleClass") != null) {
                writer.writeAttribute("class", this.getAttributes().get("styleClass"), null);
            }
            writer.endElement("input");
        }
    }
}
