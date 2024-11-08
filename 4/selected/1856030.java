package org.apache.jsp.system.config;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.sql.*;

public final class menu_005fmanage_005fmodify_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

    private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

    private static java.util.List _jspx_dependants;

    static {
        _jspx_dependants = new java.util.ArrayList(5);
        _jspx_dependants.add("/system/include/taglib.jsp");
        _jspx_dependants.add("/WEB-INF/struts-bean.tld");
        _jspx_dependants.add("/WEB-INF/struts-logic.tld");
        _jspx_dependants.add("/WEB-INF/struts-html.tld");
        _jspx_dependants.add("/WEB-INF/tld/mytag.tld");
    }

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.release();
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
            response.setContentType("text/html; charset=utf-8");
            pageContext = _jspxFactory.getPageContext(this, request, response, "", true, 8192, true);
            _jspx_page_context = pageContext;
            application = pageContext.getServletContext();
            config = pageContext.getServletConfig();
            session = pageContext.getSession();
            out = pageContext.getOut();
            _jspx_out = out;
            out.write('\r');
            out.write('\n');
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            String path = request.getContextPath();
            String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + path + "/";
            pageContext.setAttribute("basePath", basePath);
            out.write("\r\n");
            out.write("<html>\r\n");
            out.write("<head>\r\n");
            out.write("<base href=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${pageScope.basePath }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\">\r\n");
            out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\r\n");
            out.write("<title>中山市美斯特实业有限公司</title>\r\n");
            out.write("<link type=\"text/css\" rel=\"stylesheet\" href=\"system/css/style.css\" />\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/js/public.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/js/jquery-1.4.2.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/js/validate.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\">\r\n");
            out.write("\t\t//验证按钮名是否为空\r\n");
            out.write("\t\t$(document).ready(function(){\t\t\r\n");
            out.write("\t\t\t\tvalidate_Effect(\"#menuName\",\"[菜单名不能为空！]\");\r\n");
            out.write("\t\t\t\tvalidateNumber_Effect(\"#menuSort\",\"排序非法，要求是正整数!\");\t\r\n");
            out.write("\t\t\t\tvalidate_Effect(\"#actionKey\",\"[操作键不能为空！]\");\t\t\r\n");
            out.write("\t\t\t\tvar old_actionKey=$(\"#actionKey\").val();\r\n");
            out.write("\t\t\t\t$(\"#actionKey\").change(function(){\r\n");
            out.write("\t\t\t\t  \t\tvar actionKey=$(\"#actionKey\").val();\r\n");
            out.write("\t\t\t\t  \t\tif(actionKey==old_actionKey) return false;\r\n");
            out.write("\t\t\t\t  \t\tajaxRequest_validate(\"#actionKey\",\"system/ajax/menu.do?method=validate\",{\"actionKey\":actionKey});\r\n");
            out.write("\t\t\t\t});\t\t\r\n");
            out.write("\t\r\n");
            out.write("\t\t\t});\t\r\n");
            out.write("\t\t\t\r\n");
            out.write("function checkForm(){\t\r\n");
            out.write("\tvar menuNameIsNull=validateValueIsNull(\"#menuName\");\r\n");
            out.write("\tif(menuNameIsNull){\r\n");
            out.write("\t\treturn false;\r\n");
            out.write("\t}\r\n");
            out.write("\tvar menuSortIsRight=validateNumberIsRight(\"#menuSort\");\r\n");
            out.write("\tif(!menuSortIsRight){\r\n");
            out.write("\t\treturn false;\r\n");
            out.write("\t}\r\n");
            out.write("\tvar actionKeyIsNull=validateValueIsNull(\"#actionKey\");\r\n");
            out.write("\tif(actionKeyIsNull){\r\n");
            out.write("\t\treturn false;\r\n");
            out.write("\t}\t\r\n");
            out.write("\telse{\r\n");
            out.write("\t\treturn true;\r\n");
            out.write("\t}\r\n");
            out.write("}\r\n");
            out.write("</script>\r\n");
            out.write("\r\n");
            out.write("</head>\r\n");
            out.write("<body>\r\n");
            out.write("<div class=\"main\">\r\n");
            out.write("\t<div class=\"position\">当前位置: <a href=\"sysadm/desktop.jsp\">桌 面</a> → 添加菜单</div>\r\n");
            out.write("\t<div class=\"mainbody\">\r\n");
            out.write("\t\t<div class=\"operate_info\">操作说明：带 * 号必填</div>\r\n");
            out.write("\t\t<div class=\"table\">\r\n");
            out.write("\t\t<form action=\"system/config/menu_manage_modify.do\" method=\"post\" onsubmit=\"return checkForm()\">\r\n");
            out.write("\t\t\t");
            if (_jspx_meth_mytag_005fView_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t<input type=\"hidden\" name=\"menuId\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${update_menu.menuId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\"/>\r\n");
            out.write("\t\t\t<table width=\"100%\" border=\"0\" cellpadding=\"1\" cellspacing=\"1\" class=\"table_form\">\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">菜单名称：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"menuName\" id=\"menuName\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${update_menu.menuName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\"/>　*必填 </td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">排序：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"update_menuSort\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${update_menu.menuSort}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" id=\"menuSort\" size=\"5\" />　*必填 默认 0 </td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">链接地址：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"menuUrl\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${update_menu.menuUrl}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\"/>　非必填 例如：addform.jsp </td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">操作键：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"actionKey\" id=\"actionKey\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${update_menu.actionKey}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\"/>　*必填 例如：adduser </td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t\r\n");
            out.write("\t\t\t\t\t<tr>\r\n");
            out.write("\t\t\t\t\t\t<td colspan=\"2\" class=\"form_button\" style=\"padding-top:10px;\">\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"submit\" value=\"更新\" />\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"button\" value=\"返回\" onClick=\"location.href='");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${pageScope.basePath }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("system/config/menu_manage_list.jsp'\" />\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t</table>\r\n");
            out.write("\t\t\t</form>\r\n");
            out.write("\t\t</div>\r\n");
            out.write("\t</div>\r\n");
            out.write("</div>\r\n");
            out.write("</body>\r\n");
            out.write("</html>\r\n");
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

    private boolean _jspx_meth_mytag_005fView_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.ViewTag _jspx_th_mytag_005fView_005f0 = (com.zhongkai.web.tag.ViewTag) _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.get(com.zhongkai.web.tag.ViewTag.class);
        _jspx_th_mytag_005fView_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fView_005f0.setParent(null);
        _jspx_th_mytag_005fView_005f0.setTable("com.zhongkai.model.config.Menu");
        _jspx_th_mytag_005fView_005f0.setId((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${param.menu_id }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
        _jspx_th_mytag_005fView_005f0.setName("update_menu");
        int _jspx_eval_mytag_005fView_005f0 = _jspx_th_mytag_005fView_005f0.doStartTag();
        if (_jspx_th_mytag_005fView_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.reuse(_jspx_th_mytag_005fView_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.reuse(_jspx_th_mytag_005fView_005f0);
        return false;
    }
}
