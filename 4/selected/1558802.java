package org.apache.jsp.WEB_002dINF.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class home_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

    private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

    private static java.util.List _jspx_dependants;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005ffmt_005fsetBundle_0026_005fbasename_005fnobody.release();
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
            out.write("\r\n");
            out.write("\r\n");
            out.write("<div class=\"subcolumns equalize box-top\">\r\n");
            out.write("<div class=\"c33l\">\r\n");
            out.write("<div class=\"subcl\">\r\n");
            out.write("<h6>Tilgungsplan erstellen</h6>\r\n");
            out.write("<img width=\"180\" height=\"120\" border=\"0\" alt=\"\"\r\n");
            out.write("\tsrc=\"style/images/tilgungsplan_icon.png\" />\r\n");
            out.write("<p>Wir erstellen Ihnen einen Tilgungsplan, ganz nach Ihren Wünschen.</p>\r\n");
            out.write("<a href=\"#\" class=\"hideme\">&rarr; read more ...</a></div>\r\n");
            out.write("</div>\r\n");
            out.write("<div class=\"c33l\">\r\n");
            out.write("<div class=\"subc\">\r\n");
            out.write("<h6>Bonit&auml;t pr&uuml;fen</h6>\r\n");
            out.write("<img width=\"180\" height=\"120\" border=\"0\" alt=\"\"\r\n");
            out.write("\tsrc=\"style/images/bonitaet.jpg\" />\r\n");
            out.write("<p>Ob sich Ihre Ausgaben mit den Einnahmen decken, erfahren Sie\r\n");
            out.write("hier.</p>\r\n");
            out.write("<a href=\"#\" class=\"hideme\">&rarr; read more ...</a></div>\r\n");
            out.write("</div>\r\n");
            out.write("\r\n");
            out.write("<div class=\"c33r\">\r\n");
            out.write("<div class=\"subcr\">\r\n");
            out.write("<h6>Registrierung</h6>\r\n");
            out.write("<img width=\"180\" height=\"120\" border=\"0\" alt=\"\"\r\n");
            out.write("\tsrc=\"style/images/haushaltsdaten.jpg\" />\r\n");
            out.write("<p>Sie wollen auch schnell und einfach bares Geld in ihren Händen\r\n");
            out.write("halten? Dann zögern sie nicht und registrieren sie sich einfach bei uns.</p>\r\n");
            out.write("<a href=\"#\" class=\"hideme\">&rarr; read more ...</a></div>\r\n");
            out.write("</div>\r\n");
            out.write("</div>\r\n");
            out.write("<h3 class=\"hideme\">Summing up</h3>\r\n");
            out.write("<div class=\"subcolumns equalize no-ie-padding box-bottom\">\r\n");
            out.write("<div class=\"c33l\">\r\n");
            out.write("<div class=\"subcl\"><a href=\"index.html?c=redemption\"\r\n");
            out.write("\tclass=\"noprint\">&rarr; weiter<span class=\"hideme\"> about\r\n");
            out.write("Topic One</span></a></div>\r\n");
            out.write("</div>\r\n");
            out.write("<div class=\"c33l\">\r\n");
            out.write("<div class=\"subc\"><a href=\"index.html?c=solvency\" class=\"noprint\">&rarr;\r\n");
            out.write("weiter<span class=\"hideme\"> about Topic Two</span></a></div>\r\n");
            out.write("</div>\r\n");
            out.write("<div class=\"c33r\">\r\n");
            out.write("<div class=\"subcr\"><a\r\n");
            out.write("\thref=\"index.html?c=user&task=create");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${redirect}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" class=\"noprint\">&rarr;\r\n");
            out.write("weiter<span class=\"hideme\"> about Topic Tree</span></a></div>\r\n");
            out.write("</div>\r\n");
            out.write("</div>\r\n");
            out.write("\r\n");
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
}
