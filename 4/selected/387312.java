package org.middleheaven.ui.web.tags;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyContent;
import org.middleheaven.global.text.GlobalLabel;
import org.middleheaven.util.StringUtils;
import org.middleheaven.util.UrlStringUtils;

public class FormTag extends AbstractBodyTagSupport {

    private static Random random = new Random();

    private String action;

    String query;

    boolean isUpload = false;

    boolean hasIntegerFields = false;

    boolean hasDecimalFields = false;

    private List<String> dateFieldsIDs = new LinkedList<String>();

    void setHasIntegerFields(boolean hasIntegerFields) {
        this.hasIntegerFields = hasIntegerFields;
    }

    void setHasDecimalFields(boolean hasDecimalFields) {
        this.hasDecimalFields = hasDecimalFields;
    }

    void addDatePickedFieldID(String id) {
        this.dateFieldsIDs.add(id);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int doStartTag() throws JspException {
        if (id == null) {
            super.id = Integer.toString(random.nextInt());
        }
        action = UrlStringUtils.addContextPath(((HttpServletRequest) pageContext.getRequest()).getContextPath(), StringUtils.ensureStartsWith(action, "/"));
        writeValidationHookScript();
        writeMessages();
        write("<form ");
        writeAttribute("id", id);
        writeAttribute("method", "POST");
        writeAttribute("action", action);
        return EVAL_BODY_BUFFERED;
    }

    private void writeFieldsHookScript() throws JspTagException {
        if (dateFieldsIDs.isEmpty() && !hasIntegerFields && !hasDecimalFields) {
            return;
        }
        final String contextPath = ((HttpServletRequest) this.pageContext.getRequest()).getContextPath();
        write("<script type=\"text/javascript\" encoding=\"ISO-8859-1\"");
        write("src=\"" + contextPath + "/scripts/ui.datepicker.js\"");
        writeLine("></script>");
        writeLine("<script type='text/javascript' encoding=\"ISO-8859-1\">");
        writeLine("	$(document).ready(function(){");
        for (String id : dateFieldsIDs) {
            writeLine("		$('#" + id + "').datepicker();");
        }
        if (hasIntegerFields) {
            writeLine("$(\".digits\").keypress(function (e){");
            writeLine(" if( e.which!=8 && e.which!=0 && (e.which<48 || e.which>57)){");
            writeLine("	return false;");
            writeLine(" }");
            writeLine("});");
        }
        if (hasDecimalFields) {
            writeLine("$(\".number\").keypress(function (e){");
            writeLine(" if( e.which!=8 && e.which!=0 && (e.which<48 || e.which>57)){");
            writeLine("	return false;");
            writeLine(" }");
            writeLine("});");
        }
        writeLine("	});");
        writeLine("</script>");
    }

    private void writeValidationHookScript() throws JspTagException {
        final String contextPath = ((HttpServletRequest) this.pageContext.getRequest()).getContextPath();
        write("<script type=\"text/javascript\" encoding=\"ISO-8859-1\"");
        write("src=\"" + contextPath + "/scripts/jquery.validate.js\"");
        writeLine("></script>");
        write("<script type=\"text/javascript\" encoding=\"ISO-8859-1\"");
        write("src=\"" + contextPath + "/scripts/jquery.metadata.js\"");
        writeLine("></script>");
        writeLine("<script type=\"text/javascript\">");
        writeLine("	$(document).ready(function(){");
        writeLine("		$(\"#" + id + "\").validate();");
        writeLine("});");
        writeLine("</script>");
    }

    private void writeMessages() throws JspTagException {
    }

    private String getL10NMessage(String label) {
        return this.localize(new GlobalLabel(label));
    }

    public void doInitBody() throws JspException {
    }

    public int doAfterBody() throws JspException {
        BodyContent bc = getBodyContent();
        query = bc.getString();
        bc.clearBody();
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        if (isUpload) {
            writeAttribute("enctype", "multipart/form-data");
        }
        writeLine(" >");
        writeLine(query);
        writeLine("</form>");
        writeFieldsHookScript();
        return EVAL_PAGE;
    }

    @Override
    public void releaseState() {
    }
}
