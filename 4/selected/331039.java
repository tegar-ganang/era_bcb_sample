package com.divrep.common;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.StringEscapeUtils;
import com.divrep.DivRep;
import com.divrep.DivRepEvent;

public class DivRepDate extends DivRepFormElement<Date> {

    DateFormat df_incoming;

    String jquery_format = "m/d/yy";

    String minDate = null;

    public DivRepDate(DivRep parent) {
        super(parent);
        value = new Date();
        value.setTime((value.getTime() / (1000L * 60)) * (1000L * 60));
        df_incoming = DateFormat.getDateInstance(DateFormat.SHORT);
        if (!(df_incoming instanceof SimpleDateFormat)) {
            df_incoming = new SimpleDateFormat("M/d/yy");
        }
        jquery_format = convertToJQueryFormat((SimpleDateFormat) df_incoming);
    }

    public String convertToJQueryFormat(SimpleDateFormat format) {
        String java_format = format.toPattern();
        String jquery_format = java_format;
        jquery_format = jquery_format.replaceAll("yy", "y");
        jquery_format = jquery_format.replaceAll("MM", "mm");
        jquery_format = jquery_format.replaceAll("M", "m");
        return jquery_format;
    }

    public void setMinDate(Date d) {
        minDate = "new Date(" + d.getTime() + ")";
    }

    protected void onEvent(DivRepEvent e) {
        if (e.action.equals("select")) {
            try {
                value = df_incoming.parse((String) e.value);
            } catch (ParseException e2) {
                alert(e2.getMessage());
            }
        } else {
            try {
                value = df_incoming.parse((String) e.value);
            } catch (ParseException e1) {
                alert(e1.getMessage() + ". Please specify a valid date");
            }
        }
        setFormModified();
        redraw();
    }

    public void close() {
        js("$(\"#" + getNodeID() + " .datepicker\").datepicker('hide', 'fast');");
    }

    public void render(PrintWriter out) {
        out.write("<div class=\"divrep_date\" id=\"" + getNodeID() + "\">");
        if (!hidden) {
            if (label != null) {
                out.print("<label>" + StringEscapeUtils.escapeHtml(label) + "</label><br/>");
            }
            String str = df_incoming.format(value);
            out.write("<input type=\"text\" class=\"datepicker\" value=\"" + str + "\" onchange=\"divrep('" + getNodeID() + "', null, $(this).val());\"/>");
            out.write("<script type=\"text/javascript\">");
            out.write("$(document).ready(function() { $(\"#" + getNodeID() + " .datepicker\").datepicker({" + "onSelect: function(value) {divrep('" + getNodeID() + "', null, value, 'select');}," + "dateFormat: '" + jquery_format + "'," + "showOn: 'button'," + "beforeShow: function() {$('#ui-datepicker-div').maxZIndex(); }," + "changeYear: true," + "constrainInput: false," + "defaultDate: new Date(" + value.getTime() + ")," + "changeMonth: true");
            out.write("});});");
            out.write("</script>");
            error.render(out);
        }
        out.write("</div>");
    }
}
