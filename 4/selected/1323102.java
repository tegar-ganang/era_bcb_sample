package org.apache.jsp.system.config;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.sql.*;

public final class user_005fmanage_005fmodify_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

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

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.release();
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005fnobody.release();
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.release();
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
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
            out.write("<script type=\"text/javascript\" src=\"system/js/public.js\"></script>\r\n");
            out.write("<link href=\"system/plugins/validateMyForm/css/plugin.css\" rel=\"stylesheet\" type=\"text/css\">\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/js/jquery-1.4.2.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/js/verify1.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/plugins/validateMyForm/js/jquery.validateMyForm.1.5.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\">  \r\n");
            out.write("\t$(document).ready(function(){ \r\n");
            out.write("\t\tvar userRoleIds=$(\"#currentRoleId\").val();\r\n");
            out.write("\t\t\r\n");
            out.write("\t\tvar roleIds=userRoleIds.split(\",\");\r\n");
            out.write("\t\tvar role_cbs=$(\"input[name=roleIds]\");\r\n");
            out.write("\t\tvar i;//形如1,3,4分割成[1,3,4]后记录用户roleId位置\r\n");
            out.write("\t\tvar j;//记录角色复选框元素位置\r\n");
            out.write("\t\tfor(i=0;i<roleIds.length;i++){\r\n");
            out.write("\t\t\tfor(j=0;j<role_cbs.length;j++){\r\n");
            out.write("\t\t\t\tif(roleIds[i]==role_cbs[j].value){\r\n");
            out.write("\t\t\t\t\trole_cbs[j].checked=true;\r\n");
            out.write("\t\t\t\t}\r\n");
            out.write("\t\t\t}\t\t\t\r\n");
            out.write("\t\t}\r\n");
            out.write("\t    $(\"#form1\").validateMyForm(); \r\n");
            out.write("\t\t$(\".email\").keypress(function(){\r\n");
            out.write("\t\t    $(\".email\").next().fadeOut();\r\n");
            out.write("\t\t});\r\n");
            out.write("\t}); \r\n");
            out.write("\t\r\n");
            out.write("\r\n");
            out.write("</script>  \r\n");
            out.write("</head>\r\n");
            out.write("<body>\r\n");
            out.write("<div class=\"main\">\r\n");
            out.write("\t<div class=\"position\">当前位置: <a href=\"/sysadm/desktop.jsp\">桌 面</a> → 修改部门</div>\r\n");
            out.write("\t<div class=\"mainbody\">\r\n");
            out.write("\t\t<div class=\"operate_info\">操作说明：带 * 号必填</div>\r\n");
            out.write("\t\t<div class=\"table\">\r\n");
            out.write("\t\t");
            if (_jspx_meth_mytag_005fView_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t<form id=\"form1\" action=\"system/config/user_manage_modify.do\" method=\"post\">\r\n");
            out.write("\t\t\t<input name=\"userId\" type=\"hidden\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\"/>\r\n");
            out.write("\t\t\t<input type=\"hidden\" id=\"currentDepartmentId\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.departmentId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\">\r\n");
            out.write("\t\t\t<input id=\"currentRoleId\" name=\"currentRoleId\" type=\"hidden\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.roleId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\"/><!-- 用来选中checkbox -->\r\n");
            out.write("\t\t\t<table width=\"100%\" border=\"0\" cellpadding=\"1\" cellspacing=\"1\" class=\"table_form\">\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">用户编号：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input id=\"userIdentifier\" name=\"userIdentifier\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userIdentifier}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" class=\"required\" onblur=\"verify('system/ajax/user.do?method=findIdentifier',this)\"/>&nbsp;<span style=\"color:red\">*</span></td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">密码：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"userPassword\" type=\"password\"\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userPassword}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" class=\"required\"/>&nbsp;<span style=\"color:red\">*</span> </td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">昵称：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"userNickname\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userNickname}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" /></td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">邮箱：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"text\" name=\"userEmail\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userEmail}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" class=\"email\">\t\t\t\t\t\t\t\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">部门：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t<select id=\"departmentId\" name=\"departmentId\" class=\"required\">\t\t\t\t\t\t\t\r\n");
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
                    out.write("\t\t\t\t\t\t\t\t");
                    if (_jspx_meth_c_005fif_005f0(_jspx_th_logic_005fiterate_005f0, _jspx_page_context)) return;
                    out.write("\r\n");
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
            out.write("\t\t\t\t\t\t\t<span style=\"color:red\">*</span> \r\n");
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
                    out.write("\t\t\t\t\t\t\t\t");
                    if (_jspx_meth_c_005fif_005f3(_jspx_th_logic_005fiterate_005f1, _jspx_page_context)) return;
                    out.write("\r\n");
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
            out.write("\t\t\t\t\t\t\t");
            if (_jspx_meth_c_005fif_005f4(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t");
            if (_jspx_meth_c_005fif_005f5(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr>\r\n");
            out.write("\t\t\t\t\t\t<td colspan=\"2\" class=\"form_button\" style=\"padding-top:10px;\">\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"submit\" value=\"更新\" />\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"reset\" value=\"重置\" />\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"button\" value=\"返回\" onClick=\"location.href='");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${pageScope.basePath }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("system/config/user_manage_list.jsp'\" />\r\n");
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
        _jspx_th_mytag_005fView_005f0.setTable("com.zhongkai.model.config.User");
        _jspx_th_mytag_005fView_005f0.setId((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${param.userId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
        _jspx_th_mytag_005fView_005f0.setName("user");
        int _jspx_eval_mytag_005fView_005f0 = _jspx_th_mytag_005fView_005f0.doStartTag();
        if (_jspx_th_mytag_005fView_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.reuse(_jspx_th_mytag_005fView_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.reuse(_jspx_th_mytag_005fView_005f0);
        return false;
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

    private boolean _jspx_meth_c_005fif_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_logic_005fiterate_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f0);
        _jspx_th_c_005fif_005f0.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user!=null&&department.departmentId!=null }", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
        if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t\t");
                if (_jspx_meth_c_005fif_005f1(_jspx_th_c_005fif_005f0, _jspx_page_context)) return true;
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t\t");
                if (_jspx_meth_c_005fif_005f2(_jspx_th_c_005fif_005f0, _jspx_page_context)) return true;
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t");
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

    private boolean _jspx_meth_c_005fif_005f1(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fif_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f1 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f1.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f0);
        _jspx_th_c_005fif_005f1.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.departmentId==department.departmentId }", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f1 = _jspx_th_c_005fif_005f1.doStartTag();
        if (_jspx_eval_c_005fif_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t<option value=\"");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${department.departmentId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("\" selected>");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${department.departmentName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("</option>\r\n");
                out.write("\t\t\t\t\t\t\t\t\t");
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

    private boolean _jspx_meth_c_005fif_005f2(javax.servlet.jsp.tagext.JspTag _jspx_th_c_005fif_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f2 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f2.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f0);
        _jspx_th_c_005fif_005f2.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.departmentId!=department.departmentId }", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f2 = _jspx_th_c_005fif_005f2.doStartTag();
        if (_jspx_eval_c_005fif_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t<option value=\"");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${department.departmentId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write('"');
                out.write(' ');
                out.write('>');
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${department.departmentName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("</option>\r\n");
                out.write("\t\t\t\t\t\t\t\t\t");
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

    private boolean _jspx_meth_c_005fif_005f3(javax.servlet.jsp.tagext.JspTag _jspx_th_logic_005fiterate_005f1, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f3 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f3.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f3.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f1);
        _jspx_th_c_005fif_005f3.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user!=null&&role.roleId!=null }", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f3 = _jspx_th_c_005fif_005f3.doStartTag();
        if (_jspx_eval_c_005fif_005f3 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\t\t\t\t\t\t\t\t\t\r\n");
                out.write("\t\t\t\t\t\t\t\t\t<input name=\"roleIds\" type=\"checkbox\" value=\"");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("\"><label>");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleName}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("</label>\r\n");
                out.write("\t\t\t\t\t\t\t\t");
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
        _jspx_th_c_005fif_005f4.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userSwitch==1}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f4 = _jspx_th_c_005fif_005f4.doStartTag();
        if (_jspx_eval_c_005fif_005f4 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t<input name=\"userSwitch\" type=\"radio\" value=\"1\" checked />是\r\n");
                out.write("\t\t\t\t\t\t\t\t<input name=\"userSwitch\" type=\"radio\" value=\"0\" />否\r\n");
                out.write("\t\t\t\t\t\t\t\r\n");
                out.write("\t\t\t\t\t\t\t");
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
        _jspx_th_c_005fif_005f5.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userSwitch==0}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f5 = _jspx_th_c_005fif_005f5.doStartTag();
        if (_jspx_eval_c_005fif_005f5 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t<input name=\"userSwitch\" type=\"radio\" value=\"1\" />是\r\n");
                out.write("\t\t\t\t\t\t\t\t<input name=\"userSwitch\" type=\"radio\" value=\"0\" checked />否\r\n");
                out.write("\t\t\t\t\t\t\t\r\n");
                out.write("\t\t\t\t\t\t\t");
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
