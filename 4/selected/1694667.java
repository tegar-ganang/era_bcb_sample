package org.apache.jsp.system.config;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.sql.*;

public final class userform_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

    private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

    private static java.util.List _jspx_dependants;

    static {
        _jspx_dependants = new java.util.ArrayList(5);
        _jspx_dependants.add("/system/config/../include/taglib.jsp");
        _jspx_dependants.add("/WEB-INF/struts-bean.tld");
        _jspx_dependants.add("/WEB-INF/struts-logic.tld");
        _jspx_dependants.add("/WEB-INF/struts-html.tld");
        _jspx_dependants.add("/WEB-INF/tld/mytag.tld");
    }

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.release();
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.release();
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
            out.write('\r');
            out.write('\n');
            String path = request.getContextPath();
            String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + path + "/";
            out.write("\r\n");
            out.write("<html>\r\n");
            out.write("<head>\r\n");
            out.write(" <base href=\"");
            out.print(basePath);
            out.write("\">\r\n");
            out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\r\n");
            out.write("<title>中山市美斯特实业有限公司</title>\r\n");
            out.write("<link type=\"text/css\" rel=\"stylesheet\" href=\"system/css/style.css\" />\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/js/public.js\"></script>\r\n");
            out.write("<link href=\"system/plugins/validateMyForm/css/plugin.css\" rel=\"stylesheet\" type=\"text/css\">\r\n");
            out.write(" <script type=\"text/javascript\" src=\"system/js/jquery-1.4.2.js\"></script>\r\n");
            out.write(" <script type=\"text/javascript\" src=\"system/js/verify1.js\"></script>\r\n");
            out.write(" <script type=\"text/javascript\" src=\"system/plugins/validateMyForm/js/jquery.validateMyForm.1.5.js\"></script>\r\n");
            out.write(" <script type=\"text/javascript\">  \r\n");
            out.write("\t$(document).ready(function(){ \r\n");
            out.write("\t    $(\"#form1\").validateMyForm(); \r\n");
            out.write("\t    \r\n");
            out.write("\t    $(\"#form1\").validateMyForm(); \r\n");
            out.write("\t\t$(\".email\").keypress(function(){\r\n");
            out.write("\t\t    $(\".email\").next().fadeOut();\r\n");
            out.write("\t\t});\r\n");
            out.write("\t}); \r\n");
            out.write("</script>  \r\n");
            out.write("</head>\r\n");
            out.write("<body>\r\n");
            out.write("<div class=\"main\">\r\n");
            out.write("\t<div class=\"position\">当前位置: <a href=\"/sysadm/desktop.jsp\">桌 面</a> → 添加用户</div>\r\n");
            out.write("\t<div class=\"mainbody\">\r\n");
            out.write("\t\t<div class=\"operate_info\">操作说明：带 * 号必填</div>\r\n");
            out.write("\t\t<div class=\"table\">\r\n");
            out.write("\t\t<form id=\"form1\" action=\"system/config/user.do?method=save\" method=\"post\">\r\n");
            out.write("\t\t\t<input name=\"areaId\" type=\"hidden\" />\r\n");
            out.write("\t\t\t<table width=\"100%\" border=\"0\" cellpadding=\"1\" cellspacing=\"1\" class=\"table_form\">\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">用户编号：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input id=\"userIdentifier\" name=\"userIdentifier\" class=\"required\" onblur=\"verify('system/ajax/user.do?method=findIdentifier',this)\"/>&nbsp;<span style=\"color:red\">*</span>必填 </td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">用户名：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input id=\"userAccount\" name=\"userAccount\" class=\"required\" onblur=\"verify('system/ajax/user.do?method=findUserAccount',this)\" /><span style=\"color:red\">*</span> </td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\" >\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">密码：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t<input name=\"userPassword\" type=\"password\" class=\"required\"/>\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color:red\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">昵称：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"userNickname\" /></td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">邮箱：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t<input name=\"userEmail\" class=\"email\"/>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">部门：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t<select id=\"departmentId\" name=\"departmentId\" class=\"required\">\r\n");
            out.write("\t\t\t\t\t\t\t\t<option value=\"\" selected>---请选择---</option>\r\n");
            out.write("\t\t\t\t\t\t\t");
            if (_jspx_meth_mytag_005fList_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t");
            org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f0 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
            _jspx_th_logic_005fiterate_005f0.setPageContext(_jspx_page_context);
            _jspx_th_logic_005fiterate_005f0.setParent(null);
            _jspx_th_logic_005fiterate_005f0.setId("department");
            _jspx_th_logic_005fiterate_005f0.setName("departmentList");
            int _jspx_eval_logic_005fiterate_005f0 = _jspx_th_logic_005fiterate_005f0.doStartTag();
            if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                java.lang.Object department = null;
                if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                    out = _jspx_page_context.pushBody();
                    _jspx_th_logic_005fiterate_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                    _jspx_th_logic_005fiterate_005f0.doInitBody();
                }
                department = (java.lang.Object) _jspx_page_context.findAttribute("department");
                do {
                    out.write("\r\n");
                    out.write("\t\t\t\t\t\t\t\t<option value=\"");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${department.departmentId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write('"');
                    out.write('>');
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${department.departmentName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("</option>\r\n");
                    out.write("\t\t\t\t\t\t\t");
                    int evalDoAfterBody = _jspx_th_logic_005fiterate_005f0.doAfterBody();
                    department = (java.lang.Object) _jspx_page_context.findAttribute("department");
                    if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
                } while (true);
                if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                    out = _jspx_page_context.popBody();
                }
            }
            if (_jspx_th_logic_005fiterate_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.reuse(_jspx_th_logic_005fiterate_005f0);
                return;
            }
            _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.reuse(_jspx_th_logic_005fiterate_005f0);
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t</select>\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color:red\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">角色：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t");
            if (_jspx_meth_mytag_005fList_005f1(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t");
            org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f1 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
            _jspx_th_logic_005fiterate_005f1.setPageContext(_jspx_page_context);
            _jspx_th_logic_005fiterate_005f1.setParent(null);
            _jspx_th_logic_005fiterate_005f1.setId("role");
            _jspx_th_logic_005fiterate_005f1.setName("roleList");
            int _jspx_eval_logic_005fiterate_005f1 = _jspx_th_logic_005fiterate_005f1.doStartTag();
            if (_jspx_eval_logic_005fiterate_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                java.lang.Object role = null;
                if (_jspx_eval_logic_005fiterate_005f1 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                    out = _jspx_page_context.pushBody();
                    _jspx_th_logic_005fiterate_005f1.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                    _jspx_th_logic_005fiterate_005f1.doInitBody();
                }
                role = (java.lang.Object) _jspx_page_context.findAttribute("role");
                do {
                    out.write("\r\n");
                    out.write("\t\t\t\t\t\t\t\t<input id=\"roleId\" name=\"roleIds\" type=\"checkbox\" value=\"");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("\"><label>");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("</label>\r\n");
                    out.write("\t\t\t\t\t\t\t");
                    int evalDoAfterBody = _jspx_th_logic_005fiterate_005f1.doAfterBody();
                    role = (java.lang.Object) _jspx_page_context.findAttribute("role");
                    if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
                } while (true);
                if (_jspx_eval_logic_005fiterate_005f1 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                    out = _jspx_page_context.popBody();
                }
            }
            if (_jspx_th_logic_005fiterate_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.reuse(_jspx_th_logic_005fiterate_005f1);
                return;
            }
            _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.reuse(_jspx_th_logic_005fiterate_005f1);
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color:red\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">是否启用：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t<input name=\"userSwitch\" type=\"radio\" value=\"1\" checked />是\r\n");
            out.write("\t\t\t\t\t\t\t<input name=\"userSwitch\" type=\"radio\" value=\"0\" />否\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr>\r\n");
            out.write("\t\t\t\t\t\t<td colspan=\"2\" class=\"form_button\" style=\"padding-top:10px;\">\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"submit\" value=\"保存\" />\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"reset\" value=\"重置\" />\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"button\" value=\"返回\" onClick=\"history.back()\" />\r\n");
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

    private boolean _jspx_meth_mytag_005fList_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.ListTag _jspx_th_mytag_005fList_005f0 = (com.zhongkai.web.tag.ListTag) _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.get(com.zhongkai.web.tag.ListTag.class);
        _jspx_th_mytag_005fList_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fList_005f0.setParent(null);
        _jspx_th_mytag_005fList_005f0.setTable("Department");
        _jspx_th_mytag_005fList_005f0.setName("departmentList");
        int _jspx_eval_mytag_005fList_005f0 = _jspx_th_mytag_005fList_005f0.doStartTag();
        if (_jspx_th_mytag_005fList_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.reuse(_jspx_th_mytag_005fList_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.reuse(_jspx_th_mytag_005fList_005f0);
        return false;
    }

    private boolean _jspx_meth_mytag_005fList_005f1(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.ListTag _jspx_th_mytag_005fList_005f1 = (com.zhongkai.web.tag.ListTag) _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.get(com.zhongkai.web.tag.ListTag.class);
        _jspx_th_mytag_005fList_005f1.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fList_005f1.setParent(null);
        _jspx_th_mytag_005fList_005f1.setTable("Role");
        _jspx_th_mytag_005fList_005f1.setName("roleList");
        int _jspx_eval_mytag_005fList_005f1 = _jspx_th_mytag_005fList_005f1.doStartTag();
        if (_jspx_th_mytag_005fList_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.reuse(_jspx_th_mytag_005fList_005f1);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.reuse(_jspx_th_mytag_005fList_005f1);
        return false;
    }
}
