package org.apache.jsp.WEB_002dINF.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class solvency_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

    private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

    private static java.util.List _jspx_dependants;

    static {
        _jspx_dependants = new java.util.ArrayList(1);
        _jspx_dependants.add("/WEB-INF/jsp/budgetDetail.jsp");
    }

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.release();
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
        _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.release();
    }

    public void _jspService(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException, ServletException {
        PageContext pageContext = null;
        HttpSession session = null;
        ServletContext application = null;
        ServletConfig config = null;
        JspWriter out = null;
        Object page = this;
        JspWriter _jspx_out = null;
        PageContext _jspx_page_context = null;
        try {
            response.setContentType("text/html; charset=UTF-8");
            pageContext = _jspxFactory.getPageContext(this, request, response, null, true, 8192, true);
            _jspx_page_context = pageContext;
            application = pageContext.getServletContext();
            config = pageContext.getServletConfig();
            session = pageContext.getSession();
            out = pageContext.getOut();
            _jspx_out = out;
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            if (_jspx_meth_fmt_005fsetBundle_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\r\n");
            out.write("<h3>Bonitätsprüfung</h3>\r\n");
            out.write("<form action=\"index.html?c=solvency&task=check");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${redirect}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" method=\"post\"\r\n");
            out.write("\taccept-charset=\"UTF-8\" class=\"yform columnar\">\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            if (_jspx_meth_fmt_005fsetBundle_005f1(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\r\n");
            if (_jspx_meth_c_005fif_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\r\n");
            if (_jspx_meth_c_005fif_005f1(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\r\n");
            out.write("<fieldset><legend>Hinweise</legend>\r\n");
            out.write("<ul>\r\n");
            out.write("\t<li>- Als Trennzeichen bitte NUR den PUNKT verwenden und kein\r\n");
            out.write("\tKomma!</li>\r\n");
            out.write("\t<li>- In den Eingabefeldern sind nur nummerische Werte erlaubt!</li>\r\n");
            out.write("\t<li>- Wenn sie für eine Positon keine Ausgaben haben, dann tragen\r\n");
            out.write("\tsie bitte den Wert 0 ein!</li>\r\n");
            out.write("</ul>\r\n");
            out.write("</fieldset>\r\n");
            out.write("\r\n");
            if (_jspx_meth_c_005fif_005f4(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\r\n");
            out.write("<fieldset><legend>Einkommen (Netto pro Monat)</legend>\r\n");
            out.write("<div class=\"type-text\"><label for=\"applicant\">Antragsteller:<sup>*</sup></label>\r\n");
            out.write("<input id=\"applicant\" name=\"applicant\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.applicant}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"coapplicant\">Mitantragsteller:<sup>*</sup></label>\r\n");
            out.write("<input id=\"coapplicant\" name=\"coapplicant\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.coApplicant}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"otherincome\">Sonstige\r\n");
            out.write("Einkünfte:<sup>*</sup></label> <input id=\"otherincome\" name=\"otherincome\"\r\n");
            out.write("\ttype=\"text\" size=\"30\" maxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.otherIncome}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("</fieldset>\r\n");
            out.write("\r\n");
            out.write("<fieldset><legend>Ausgaben (pro Monat)</legend>\r\n");
            out.write("<div class=\"type-text\"><label for=\"houserental\">Miete/Wohnungsbaudarlehen:<sup>*</sup></label>\r\n");
            out.write("<input id=\"houserental\" name=\"houserental\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.houseRental}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"habitcosts\">Wohnnebenkosten:<sup>*</sup></label>\r\n");
            out.write("<input id=\"habitcosts\" name=\"habitcosts\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.habitCosts}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"livecosts\">Lebenshaltungskosten:<sup>*</sup></label>\r\n");
            out.write("<input id=\"livecosts\" name=\"livecosts\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.liveCosts}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"phonecosts\">Telefon/Mobilfung:<sup>*</sup></label>\r\n");
            out.write("<input id=\"phonecosts\" name=\"phonecosts\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.phoneCosts}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"carcosts\">KFZ-Kosten:<sup>*</sup></label>\r\n");
            out.write("<input id=\"carcosts\" name=\"carcosts\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.carCosts}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"debts\">Ratenverpflichtungen:<sup>*</sup></label>\r\n");
            out.write("<input id=\"debts\" name=\"debts\" type=\"text\" size=\"30\" maxlength=\"30\"\r\n");
            out.write("\tvalue=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.debts}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"insurancecosts\">Versicherungen:<sup>*</sup></label>\r\n");
            out.write("<input id=\"insurancecosts\" name=\"insurancecosts\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.insuranceCosts}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"savingsplan\">Sparverträge:<sup>*</sup></label>\r\n");
            out.write("<input id=\"savingsplan\" name=\"savingsplan\" type=\"text\" size=\"30\"\r\n");
            out.write("\tmaxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.savingsPlan}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("<div class=\"type-text\"><label for=\"othercosts\">Sonstige\r\n");
            out.write("Ausgaben:<sup>*</sup></label> <input id=\"othercosts\" name=\"othercosts\"\r\n");
            out.write("\ttype=\"text\" size=\"30\" maxlength=\"30\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${budget.otherCosts}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></div>\r\n");
            out.write("</fieldset>\r\n");
            out.write("\r\n");
            out.write("<script type=\"text/javascript\">\r\n");
            out.write("\tnew LiveValidation('applicant').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('coapplicant').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('otherincome').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\r\n");
            out.write("\tnew LiveValidation('houserental').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('habitcosts').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('livecosts').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('phonecosts').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('carcosts').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('debts').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('insurancecosts').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('savingsplan').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("\tnew LiveValidation('othercosts').add(Validate.Numericality, {\r\n");
            out.write("\t\tonlyInteger : false\r\n");
            out.write("\t}).add(Validate.Presence);\r\n");
            out.write("</script>");
            out.write("\r\n");
            out.write("<fieldset>");
            if (_jspx_meth_c_005fif_005f5(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("<div class=\"type-button\"><input type=\"submit\" value=\"Prüfen\"\r\n");
            out.write("\tid=\"submit\" name=\"submit\" /></div>\r\n");
            out.write("</fieldset>\r\n");
            out.write("</form>");
        } catch (Throwable t) {
            if (!(t instanceof SkipPageException)) {
                out = _jspx_out;
                if (out != null && out.getBufferSize() != 0) try {
                    out.clearBuffer();
                } catch (java.io.IOException e) {
                }
                if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
            }
        } finally {
            _jspxFactory.releasePageContext(_jspx_page_context);
        }
    }

    private boolean _jspx_meth_fmt_005fsetBundle_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag _jspx_th_fmt_005fsetBundle_005f0 = (org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag) _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag.class);
        _jspx_th_fmt_005fsetBundle_005f0.setPageContext(_jspx_page_context);
        _jspx_th_fmt_005fsetBundle_005f0.setParent(null);
        _jspx_th_fmt_005fsetBundle_005f0.setBasename("Creditster");
        int _jspx_eval_fmt_005fsetBundle_005f0 = _jspx_th_fmt_005fsetBundle_005f0.doStartTag();
        if (_jspx_th_fmt_005fsetBundle_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.reuse(_jspx_th_fmt_005fsetBundle_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.reuse(_jspx_th_fmt_005fsetBundle_005f0);
        return false;
    }

    private boolean _jspx_meth_fmt_005fsetBundle_005f1(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag _jspx_th_fmt_005fsetBundle_005f1 = (org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag) _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.get(org.apache.taglibs.standard.tag.rt.fmt.SetBundleTag.class);
        _jspx_th_fmt_005fsetBundle_005f1.setPageContext(_jspx_page_context);
        _jspx_th_fmt_005fsetBundle_005f1.setParent(null);
        _jspx_th_fmt_005fsetBundle_005f1.setBasename("Creditster");
        int _jspx_eval_fmt_005fsetBundle_005f1 = _jspx_th_fmt_005fsetBundle_005f1.doStartTag();
        if (_jspx_th_fmt_005fsetBundle_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.reuse(_jspx_th_fmt_005fsetBundle_005f1);
            return true;
        }
        _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.reuse(_jspx_th_fmt_005fsetBundle_005f1);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f0.setParent(null);
        _jspx_th_c_005fif_005f0.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${task eq 'view'}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
        if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write('\r');
                out.write('\n');
                out.write('	');
                if (_jspx_meth_c_005fset_005f0(_jspx_th_c_005fif_005f0, _jspx_page_context)) return true;
                out.write('\r');
                out.write('\n');
                int evalDoAfterBody = _jspx_th_c_005fif_005f0.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
        }
        if (_jspx_th_c_005fif_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
        return false;
    }

    private boolean _jspx_meth_c_005fset_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fif_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.SetTag _jspx_th_c_005fset_005f0 = (org.apache.taglibs.standard.tag.rt.core.SetTag) _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.get(org.apache.taglibs.standard.tag.rt.core.SetTag.class);
        _jspx_th_c_005fset_005f0.setPageContext(_jspx_page_context);
        _jspx_th_c_005fset_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f0);
        _jspx_th_c_005fset_005f0.setVar("readonly");
        _jspx_th_c_005fset_005f0.setValue(new org.apache.jasper.el.JspValueExpression("/WEB-INF/jsp/budgetDetail.jsp(13,1) 'readonly'", _el_expressionfactory.createValueExpression("readonly", java.lang.Object.class)).getValue(_jspx_page_context.getELContext()));
        int _jspx_eval_c_005fset_005f0 = _jspx_th_c_005fset_005f0.doStartTag();
        if (_jspx_th_c_005fset_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.reuse(_jspx_th_c_005fset_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fc_005fset_0026_005fvar_005fvalue_005fnobody.reuse(_jspx_th_c_005fset_005f0);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f1(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f1 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f1.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f1.setParent(null);
        _jspx_th_c_005fif_005f1.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${not empty requestScope.msg}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f1 = _jspx_th_c_005fif_005f1.doStartTag();
        if (_jspx_eval_c_005fif_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t<fieldset><legend>Ergebnis</legend>\r\n");
                out.write("\t<div>");
                if (_jspx_meth_c_005fif_005f2(_jspx_th_c_005fif_005f1, _jspx_page_context)) return true;
                out.write(' ');
                if (_jspx_meth_c_005fif_005f3(_jspx_th_c_005fif_005f1, _jspx_page_context)) return true;
                out.write("</div>\r\n");
                out.write("\t</fieldset>\r\n");
                int evalDoAfterBody = _jspx_th_c_005fif_005f1.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
        }
        if (_jspx_th_c_005fif_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f1);
            return true;
        }
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f1);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f2(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fif_005f1, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f2 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f2.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f1);
        _jspx_th_c_005fif_005f2.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${requestScope.msg eq 'true'}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f2 = _jspx_th_c_005fif_005f2.doStartTag();
        if (_jspx_eval_c_005fif_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t<p id=\"msg_true\">Ein Kredit kann ihnen sofort gewährt werden.\r\n");
                out.write("\t\tSchauen sie doch bald in unserer Filiale vorbei.</p>\r\n");
                out.write("\t");
                int evalDoAfterBody = _jspx_th_c_005fif_005f2.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
        }
        if (_jspx_th_c_005fif_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f2);
            return true;
        }
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f2);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f3(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fif_005f1, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f3 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f3.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f1);
        _jspx_th_c_005fif_005f3.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${requestScope.msg eq 'false'}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f3 = _jspx_th_c_005fif_005f3.doStartTag();
        if (_jspx_eval_c_005fif_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t<p id=\"msg_false\">Leider können wir ihnen keinen Kredit gewähren,\r\n");
                out.write("\t\tda sie nicht kreditwürdig sind.</p>\r\n");
                out.write("\t");
                int evalDoAfterBody = _jspx_th_c_005fif_005f3.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
        }
        if (_jspx_th_c_005fif_005f3.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f3);
            return true;
        }
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f3);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f4(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f4 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f4.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f4.setParent(null);
        _jspx_th_c_005fif_005f4.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${task eq 'create' or task eq 'edit'}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f4 = _jspx_th_c_005fif_005f4.doStartTag();
        if (_jspx_eval_c_005fif_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t<fieldset><legend>Personendaten</legend>\r\n");
                out.write("\t<div class=\"type-text\"><label for=\"name\">Name:</label> <input\r\n");
                out.write("\t\tid=\"name\" name=\"name\" type=\"text\" size=\"30\" maxlength=\"30\"\r\n");
                out.write("\t\tvalue=\"");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${customer.lastName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("\" readonly=\"readonly\" /></div>\r\n");
                out.write("\t<div class=\"type-text\"><label for=\"firstname\">Vorname:</label> <input\r\n");
                out.write("\t\tid=\"firstname\" name=\"firstname\" type=\"text\" size=\"30\" maxlength=\"30\"\r\n");
                out.write("\t\tvalue=\"");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${customer.firstName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("\" readonly=\"readonly\" /></div>\r\n");
                out.write("\t</fieldset>\r\n");
                int evalDoAfterBody = _jspx_th_c_005fif_005f4.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
        }
        if (_jspx_th_c_005fif_005f4.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f4);
            return true;
        }
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f4);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f5(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f5 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f5.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f5.setParent(null);
        _jspx_th_c_005fif_005f5.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${customer.login ne null}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f5 = _jspx_th_c_005fif_005f5.doStartTag();
        if (_jspx_eval_c_005fif_005f5 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t<div class=\"type-check\"><label for=\"save\">Speichern:</label><input\r\n");
                out.write("\t\ttype=\"checkbox\" id=\"save\" name=\"save\" value=\"save\" /></div>\r\n");
                int evalDoAfterBody = _jspx_th_c_005fif_005f5.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
        }
        if (_jspx_th_c_005fif_005f5.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f5);
            return true;
        }
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f5);
        return false;
    }
}
