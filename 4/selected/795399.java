package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.activebpel.rt.bpel.impl.*;
import org.activebpel.rt.bpel.server.engine.*;
import org.activebpel.rt.bpel.server.admin.*;
import javax.xml.namespace.QName;
import java.text.*;

public final class deployment_005flog_005fdetail_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

    private static java.util.List _jspx_dependants;

    static {
        _jspx_dependants = new java.util.ArrayList(1);
        _jspx_dependants.add("/META-INF/aetags.tld");
    }

    private org.apache.jasper.runtime.TagHandlerPool _jspx_tagPool_ae_RequestEncoding_value_nobody;

    private org.apache.jasper.runtime.TagHandlerPool _jspx_tagPool_ae_GetResource_name_nobody;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _jspx_tagPool_ae_RequestEncoding_value_nobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _jspx_tagPool_ae_GetResource_name_nobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
    }

    public void _jspDestroy() {
        _jspx_tagPool_ae_RequestEncoding_value_nobody.release();
        _jspx_tagPool_ae_GetResource_name_nobody.release();
    }

    public void _jspService(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException, ServletException {
        JspFactory _jspxFactory = null;
        PageContext pageContext = null;
        HttpSession session = null;
        ServletContext application = null;
        ServletConfig config = null;
        JspWriter out = null;
        Object page = this;
        JspWriter _jspx_out = null;
        PageContext _jspx_page_context = null;
        try {
            _jspxFactory = JspFactory.getDefaultFactory();
            response.setContentType("text/html; charset=UTF-8");
            pageContext = _jspxFactory.getPageContext(this, request, response, null, true, 8192, true);
            _jspx_page_context = pageContext;
            application = pageContext.getServletContext();
            config = pageContext.getServletConfig();
            session = pageContext.getSession();
            out = pageContext.getOut();
            _jspx_out = out;
            out.write("\r\n");
            out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\r\n");
            out.write("\"http://www.w3.org/TR/html4/loose.dtd\">\r\n");
            out.write("<html>\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("   ");
            out.write("\r\n");
            out.write("   ");
            if (_jspx_meth_ae_RequestEncoding_0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\r\n");
            out.write("   ");
            org.activebpel.rt.bpeladmin.war.web.AeDeploymentLogsBean logBean = null;
            synchronized (_jspx_page_context) {
                logBean = (org.activebpel.rt.bpeladmin.war.web.AeDeploymentLogsBean) _jspx_page_context.getAttribute("logBean", PageContext.PAGE_SCOPE);
                if (logBean == null) {
                    logBean = new org.activebpel.rt.bpeladmin.war.web.AeDeploymentLogsBean();
                    _jspx_page_context.setAttribute("logBean", logBean, PageContext.PAGE_SCOPE);
                    out.write("\r\n");
                    out.write("   ");
                }
            }
            out.write("\r\n");
            out.write("      \r\n");
            out.write("   ");
            org.apache.jasper.runtime.JspRuntimeLibrary.include(request, response, "header_head.jsp", out, false);
            out.write("\r\n");
            out.write("   \r\n");
            out.write("   <body> \r\n");
            out.write("   \r\n");
            out.write("      <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" align=\"center\">\r\n");
            out.write("         <tr>\r\n");
            out.write("            <td valign=\"top\" width=\"20%\">\r\n");
            out.write("            ");
            org.apache.jasper.runtime.JspRuntimeLibrary.include(request, response, "header_nav.jsp", out, false);
            out.write("\r\n");
            out.write("            </td>\r\n");
            out.write("         \r\n");
            out.write("            <!-- spacer between nav and main -->\r\n");
            out.write("            <td width=\"3%\"></td>\r\n");
            out.write("         \r\n");
            out.write("            <td valign=\"top\">\r\n");
            out.write("               <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" align=\"left\">\r\n");
            out.write("                  <tr>\r\n");
            out.write("                     <th class=\"pageHeaders\" align=\"left\" nowrap=\"true\">&nbsp;");
            if (_jspx_meth_ae_GetResource_0(_jspx_page_context)) return;
            out.write("</th>\r\n");
            out.write("                  </tr>\r\n");
            out.write("                  <tr>\r\n");
            out.write("                    <td><textarea name=\"textarea\" style=\"width:99%; height:100%\" rows=\"25\" wrap=\"OFF\" readonly>");
            out.write(org.apache.jasper.runtime.JspRuntimeLibrary.toString((((org.activebpel.rt.bpeladmin.war.web.AeDeploymentLogsBean) _jspx_page_context.findAttribute("logBean")).getLogFile())));
            out.write("</textarea></td>\r\n");
            out.write("                  </tr>\r\n");
            out.write("               </table>\r\n");
            out.write("            </td>\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("            <!-- main and right margin       -->\r\n");
            out.write("            <td width=\"3%\"></td>\r\n");
            out.write("         </tr>\r\n");
            out.write("      </table>\r\n");
            out.write("   \r\n");
            out.write("      <br> \r\n");
            out.write("      ");
            org.apache.jasper.runtime.JspRuntimeLibrary.include(request, response, "footer.jsp", out, false);
            out.write("\r\n");
            out.write("\r\n");
            out.write("   </body>\r\n");
            out.write("</html>\r\n");
        } catch (Throwable t) {
            if (!(t instanceof SkipPageException)) {
                out = _jspx_out;
                if (out != null && out.getBufferSize() != 0) out.clearBuffer();
                if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
            }
        } finally {
            if (_jspxFactory != null) _jspxFactory.releasePageContext(_jspx_page_context);
        }
    }

    private boolean _jspx_meth_ae_RequestEncoding_0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.activebpel.rt.bpeladmin.war.tags.AeRequestEncodingTag _jspx_th_ae_RequestEncoding_0 = (org.activebpel.rt.bpeladmin.war.tags.AeRequestEncodingTag) _jspx_tagPool_ae_RequestEncoding_value_nobody.get(org.activebpel.rt.bpeladmin.war.tags.AeRequestEncodingTag.class);
        _jspx_th_ae_RequestEncoding_0.setPageContext(_jspx_page_context);
        _jspx_th_ae_RequestEncoding_0.setParent(null);
        _jspx_th_ae_RequestEncoding_0.setValue("UTF-8");
        int _jspx_eval_ae_RequestEncoding_0 = _jspx_th_ae_RequestEncoding_0.doStartTag();
        if (_jspx_th_ae_RequestEncoding_0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _jspx_tagPool_ae_RequestEncoding_value_nobody.reuse(_jspx_th_ae_RequestEncoding_0);
            return true;
        }
        _jspx_tagPool_ae_RequestEncoding_value_nobody.reuse(_jspx_th_ae_RequestEncoding_0);
        return false;
    }

    private boolean _jspx_meth_ae_GetResource_0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.activebpel.rt.bpeladmin.war.tags.AeGetResourceTag _jspx_th_ae_GetResource_0 = (org.activebpel.rt.bpeladmin.war.tags.AeGetResourceTag) _jspx_tagPool_ae_GetResource_name_nobody.get(org.activebpel.rt.bpeladmin.war.tags.AeGetResourceTag.class);
        _jspx_th_ae_GetResource_0.setPageContext(_jspx_page_context);
        _jspx_th_ae_GetResource_0.setParent(null);
        _jspx_th_ae_GetResource_0.setName("deployment_log");
        int _jspx_eval_ae_GetResource_0 = _jspx_th_ae_GetResource_0.doStartTag();
        if (_jspx_th_ae_GetResource_0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _jspx_tagPool_ae_GetResource_name_nobody.reuse(_jspx_th_ae_GetResource_0);
            return true;
        }
        _jspx_tagPool_ae_GetResource_name_nobody.reuse(_jspx_th_ae_GetResource_0);
        return false;
    }
}
