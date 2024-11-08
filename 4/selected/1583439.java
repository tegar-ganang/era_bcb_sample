package org.apache.jsp.system.config;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.util.*;
import com.zhongkai.model.config.Menu;
import com.zhongkai.model.config.Button;

public final class buttontestlist_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

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

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005flistMenuForActionSet_0026_005fmenu3_005fmenu2_005fmenu1_005fnobody;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005ffield;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fmytag_005flistMenuForActionSet_0026_005fmenu3_005fmenu2_005fmenu1_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005ffield = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.release();
        _005fjspx_005ftagPool_005fmytag_005flistMenuForActionSet_0026_005fmenu3_005fmenu2_005fmenu1_005fnobody.release();
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005ffield.release();
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
        java.lang.Object _jspx_menu2_1 = null;
        java.lang.Object _jspx_menu3_2 = null;
        try {
            response.setContentType("text/html; charset=utf-8");
            pageContext = _jspxFactory.getPageContext(this, request, response, null, true, 8192, true);
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
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("\r\n");
            out.write("<html>\r\n");
            out.write("<head>\r\n");
            out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\r\n");
            out.write("<title>角色权限管理</title>\r\n");
            out.write("<link type=\"text/css\" rel=\"stylesheet\" href=\"/system/css/style.css\" />\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"/system/js/public.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"/system/js/jquery-1.4.2.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\">\r\n");
            out.write("$(document).ready(function(){\r\n");
            out.write("\t$(\"#checkAll\").click(function(){\r\n");
            out.write("\t\t$(\"input[name='actionKey']\").attr(\"checked\",true);\r\n");
            out.write("\t});\r\n");
            out.write("\t$(\"#checkNon\").click(function(){\r\n");
            out.write("\t\t$(\"input[name='actionKey']\").attr(\"checked\",false);\r\n");
            out.write("\t});\r\n");
            out.write("});\r\n");
            out.write("function reback(url){\r\n");
            out.write("\tlocation.href=url;\r\n");
            out.write("}\r\n");
            out.write("</script>\r\n");
            out.write("</head>\r\n");
            out.write("<body>\r\n");
            if (_jspx_meth_mytag_005fView_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("<div class=\"main\">\r\n");
            out.write("\t<div class=\"position\">当前位置: <a href=\"/sysadm/desktop.jsp\">桌 面</a> → <a href=\"rolelist.jsp\">角色管理</a> → 权限管理</div>\r\n");
            out.write("\t<div class=\"mainbody\">\r\n");
            out.write("\t\t<div class=\"toolbar\">\r\n");
            out.write("\t\t\t工具栏：\r\n");
            out.write("\t\t\t<input id=\"checkAll\" type=\"button\" value=\"全选\" class=\"system_button\" />\r\n");
            out.write("\t\t\t<input id=\"checkNon\" type=\"button\" value=\"全不选\" class=\"system_button\" />\r\n");
            out.write("\t\t\t<input type=\"button\" value=\"返回\" class=\"system_button\" onclick=\"reback('rolelist.jsp')\" />\r\n");
            out.write("\t\t</div>\r\n");
            out.write("\t\t<div class=\"operate_info\">\r\n");
            out.write("\t\t\t<font color=\"#FF0000\"><label id=\"explaininfo\">当前选择角色：");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleName }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("</label></font>\r\n");
            out.write("\t\t</div>\r\n");
            out.write("\t\t<div class=\"table\">\r\n");
            out.write("\t\t\t<form action=\"/system/config/action.do\" method=\"post\">\r\n");
            out.write("\t\t\t\t<input name=\"roleId\" type=\"hidden\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" />\r\n");
            out.write("\t\t\t\t<table width=\"100%\" border=\"0\" cellpadding=\"1\" cellspacing=\"1\" class=\"table_list\">\r\n");
            out.write("\t\t\t\t\t<tr>\r\n");
            out.write("\t\t\t\t\t\t<th>菜单名称</th>\r\n");
            out.write("\t\t\t\t\t\t<th>链接地址</th>\r\n");
            out.write("\t\t\t\t\t\t<th>操作键</th>\r\n");
            out.write("\t\t\t\t\t\t<th>操作</th>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t");
            if (_jspx_meth_mytag_005flistMenuForActionSet_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t");
            if (_jspx_meth_mytag_005fList_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t");
            Object[] menuActionKeys = null;
            List menuActionList = (List) pageContext.getAttribute("menuActionList");
            if (menuActionList != null && menuActionList.size() > 0) {
                menuActionKeys = menuActionList.toArray();
                Arrays.sort(menuActionKeys);
            }
            out.write("\r\n");
            out.write("\t\t\t\t\t");
            org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f0 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
            _jspx_th_logic_005fiterate_005f0.setPageContext(_jspx_page_context);
            _jspx_th_logic_005fiterate_005f0.setParent(null);
            _jspx_th_logic_005fiterate_005f0.setId("menu1");
            _jspx_th_logic_005fiterate_005f0.setName("menuList1");
            int _jspx_eval_logic_005fiterate_005f0 = _jspx_th_logic_005fiterate_005f0.doStartTag();
            if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                java.lang.Object menu1 = null;
                if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                    out = _jspx_page_context.pushBody();
                    _jspx_th_logic_005fiterate_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                    _jspx_th_logic_005fiterate_005f0.doInitBody();
                }
                menu1 = (java.lang.Object) _jspx_page_context.findAttribute("menu1");
                do {
                    out.write("\r\n");
                    out.write("\t\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
                    out.write("\t\t\t\t\t\t\t<td>\r\n");
                    out.write("\t\t\t\t\t\t\t\t");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu1.menuName }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("\r\n");
                    out.write("\t\t\t\t\t\t\t\t<input name=\"");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu1.actionKey }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("\" type=\"hidden\" value=\"0\" />\r\n");
                    out.write("\t\t\t\t\t\t\t</td>\r\n");
                    out.write("\t\t\t\t\t\t\t<td>");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu1.menuUrl }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("</td>\r\n");
                    out.write("\t\t\t\t\t\t\t<td>");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu1.actionKey }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("</td>\r\n");
                    out.write("\t\t\t\t\t\t\t<td>\r\n");
                    out.write("\t\t\t\t\t\t\t\t<input type=\"button\" value=\"修改\" class=\"system_button\" onclick=\"mod(");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write(")\"  />\r\n");
                    out.write("\t\t\t\t\t\t\t\t<input type=\"button\" value=\"删除\" class=\"system_button\" onclick=\"del(");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write(")\"  />\r\n");
                    out.write("\t\t\t\t\t\t\t</td>\r\n");
                    out.write("\t\t\t\t\t\t</tr>\r\n");
                    out.write("\t\t\t\t\t\t");
                    org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f1 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
                    _jspx_th_logic_005fiterate_005f1.setPageContext(_jspx_page_context);
                    _jspx_th_logic_005fiterate_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f0);
                    _jspx_th_logic_005fiterate_005f1.setId("menu2");
                    _jspx_th_logic_005fiterate_005f1.setName("menuList2");
                    int _jspx_eval_logic_005fiterate_005f1 = _jspx_th_logic_005fiterate_005f1.doStartTag();
                    if (_jspx_eval_logic_005fiterate_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                        java.lang.Object menu2 = null;
                        if (_jspx_eval_logic_005fiterate_005f1 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                            out = _jspx_page_context.pushBody();
                            _jspx_th_logic_005fiterate_005f1.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                            _jspx_th_logic_005fiterate_005f1.doInitBody();
                        }
                        menu2 = (java.lang.Object) _jspx_page_context.findAttribute("menu2");
                        do {
                            out.write("\r\n");
                            out.write("\t\t\t\t\t\t\t");
                            org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
                            _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
                            _jspx_th_c_005fif_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f1);
                            _jspx_th_c_005fif_005f0.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu2.menuParent==menu1.menuId }", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
                            int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
                            if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                                do {
                                    out.write("\r\n");
                                    out.write("\t\t\t\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t<td style=\"padding-left:20px;\">\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t\t∟");
                                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu2.menuName }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                                    out.write("\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t\t<input name=\"");
                                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu2.actionKey }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                                    out.write("\" type=\"hidden\" value=\"0\" />\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t</td>\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t<td>");
                                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu2.menuUrl }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                                    out.write("</td>\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t<td>");
                                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu2.actionKey }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                                    out.write("</td>\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t<td>\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t\t<input type=\"button\" value=\"修改\" class=\"system_button\" onclick=\"mod(");
                                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                                    out.write(")\"  />\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t\t<input type=\"button\" value=\"删除\" class=\"system_button\" onclick=\"del(");
                                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                                    out.write(")\"  />\r\n");
                                    out.write("\t\t\t\t\t\t\t\t\t</td>\r\n");
                                    out.write("\t\t\t\t\t\t\t\t</tr>\r\n");
                                    out.write("\t\t\t\t\t\t\t\t");
                                    org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f2 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
                                    _jspx_th_logic_005fiterate_005f2.setPageContext(_jspx_page_context);
                                    _jspx_th_logic_005fiterate_005f2.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_c_005fif_005f0);
                                    _jspx_th_logic_005fiterate_005f2.setId("menu3");
                                    _jspx_th_logic_005fiterate_005f2.setName("menuList3");
                                    int _jspx_eval_logic_005fiterate_005f2 = _jspx_th_logic_005fiterate_005f2.doStartTag();
                                    if (_jspx_eval_logic_005fiterate_005f2 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                                        java.lang.Object menu3 = null;
                                        if (_jspx_eval_logic_005fiterate_005f2 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                                            out = _jspx_page_context.pushBody();
                                            _jspx_th_logic_005fiterate_005f2.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                                            _jspx_th_logic_005fiterate_005f2.doInitBody();
                                        }
                                        menu3 = (java.lang.Object) _jspx_page_context.findAttribute("menu3");
                                        do {
                                            out.write("\r\n");
                                            out.write("\t\t\t\t\t\t\t\t\t");
                                            if (_jspx_meth_c_005fif_005f1(_jspx_th_logic_005fiterate_005f2, _jspx_page_context)) return;
                                            out.write("\r\n");
                                            out.write("\t\t\t\t\t\t\t\t");
                                            int evalDoAfterBody = _jspx_th_logic_005fiterate_005f2.doAfterBody();
                                            menu3 = (java.lang.Object) _jspx_page_context.findAttribute("menu3");
                                            if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
                                        } while (true);
                                        if (_jspx_eval_logic_005fiterate_005f2 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                                            out = _jspx_page_context.popBody();
                                        }
                                    }
                                    if (_jspx_th_logic_005fiterate_005f2.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                                        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.reuse(_jspx_th_logic_005fiterate_005f2);
                                        return;
                                    }
                                    _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.reuse(_jspx_th_logic_005fiterate_005f2);
                                    out.write("\r\n");
                                    out.write("\t\t\t\t\t\t\t");
                                    int evalDoAfterBody = _jspx_th_c_005fif_005f0.doAfterBody();
                                    if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
                                } while (true);
                            }
                            if (_jspx_th_c_005fif_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                                _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
                                return;
                            }
                            _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.reuse(_jspx_th_c_005fif_005f0);
                            out.write("\r\n");
                            out.write("\t\t\t\t\t\t");
                            int evalDoAfterBody = _jspx_th_logic_005fiterate_005f1.doAfterBody();
                            menu2 = (java.lang.Object) _jspx_page_context.findAttribute("menu2");
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
                    out.write("\t\t\t\t\t");
                    int evalDoAfterBody = _jspx_th_logic_005fiterate_005f0.doAfterBody();
                    menu1 = (java.lang.Object) _jspx_page_context.findAttribute("menu1");
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
            out.write("\t\t\t\t</table>\r\n");
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
        _jspx_th_mytag_005fView_005f0.setName("role");
        _jspx_th_mytag_005fView_005f0.setTable("com.zhongkai.model.config.Role");
        _jspx_th_mytag_005fView_005f0.setId((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${param.rid }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
        int _jspx_eval_mytag_005fView_005f0 = _jspx_th_mytag_005fView_005f0.doStartTag();
        if (_jspx_th_mytag_005fView_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.reuse(_jspx_th_mytag_005fView_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fView_0026_005ftable_005fname_005fid_005fnobody.reuse(_jspx_th_mytag_005fView_005f0);
        return false;
    }

    private boolean _jspx_meth_mytag_005flistMenuForActionSet_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.ListMenuForActionSet _jspx_th_mytag_005flistMenuForActionSet_005f0 = (com.zhongkai.web.tag.ListMenuForActionSet) _005fjspx_005ftagPool_005fmytag_005flistMenuForActionSet_0026_005fmenu3_005fmenu2_005fmenu1_005fnobody.get(com.zhongkai.web.tag.ListMenuForActionSet.class);
        _jspx_th_mytag_005flistMenuForActionSet_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005flistMenuForActionSet_005f0.setParent(null);
        _jspx_th_mytag_005flistMenuForActionSet_005f0.setMenu1("menuList1");
        _jspx_th_mytag_005flistMenuForActionSet_005f0.setMenu2("menuList2");
        _jspx_th_mytag_005flistMenuForActionSet_005f0.setMenu3("menuList3");
        int _jspx_eval_mytag_005flistMenuForActionSet_005f0 = _jspx_th_mytag_005flistMenuForActionSet_005f0.doStartTag();
        if (_jspx_th_mytag_005flistMenuForActionSet_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005flistMenuForActionSet_0026_005fmenu3_005fmenu2_005fmenu1_005fnobody.reuse(_jspx_th_mytag_005flistMenuForActionSet_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005flistMenuForActionSet_0026_005fmenu3_005fmenu2_005fmenu1_005fnobody.reuse(_jspx_th_mytag_005flistMenuForActionSet_005f0);
        return false;
    }

    private boolean _jspx_meth_mytag_005fList_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.ListTag _jspx_th_mytag_005fList_005f0 = (com.zhongkai.web.tag.ListTag) _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005ffield.get(com.zhongkai.web.tag.ListTag.class);
        _jspx_th_mytag_005fList_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fList_005f0.setParent(null);
        _jspx_th_mytag_005fList_005f0.setField("actionKey");
        _jspx_th_mytag_005fList_005f0.setName("menuActionList");
        _jspx_th_mytag_005fList_005f0.setTable("Action");
        int _jspx_eval_mytag_005fList_005f0 = _jspx_th_mytag_005fList_005f0.doStartTag();
        if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.pushBody();
                _jspx_th_mytag_005fList_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                _jspx_th_mytag_005fList_005f0.doInitBody();
            }
            do {
                out.write("actionType=0 and roleId=");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                int evalDoAfterBody = _jspx_th_mytag_005fList_005f0.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
            if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.popBody();
            }
        }
        if (_jspx_th_mytag_005fList_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005ffield.reuse(_jspx_th_mytag_005fList_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname_005ffield.reuse(_jspx_th_mytag_005fList_005f0);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f1(javax.servlet.jsp.tagext.JspTag _jspx_th_logic_005fiterate_005f2, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f1 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f1.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f2);
        _jspx_th_c_005fif_005f1.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu3.menuParent==menu2.menuId }", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f1 = _jspx_th_c_005fif_005f1.doStartTag();
        if (_jspx_eval_c_005fif_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t<td style=\"padding-left:50px;\">\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t\t∟");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu3.menuName }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t\t<input name=\"");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu3.actionKey }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("\" type=\"hidden\" value=\"0\" />\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t</td>\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t<td>");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu3.menuUrl }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("</td>\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t<td>");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${menu3.actionKey }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write("</td>\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t<td>\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t\t<input type=\"button\" value=\"修改\" class=\"system_button\" onclick=\"mod(");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write(")\"  />\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t\t<input type=\"button\" value=\"删除\" class=\"system_button\" onclick=\"del(");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${role.roleId }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write(")\"  />\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t\t</td>\r\n");
                out.write("\t\t\t\t\t\t\t\t\t\t</tr>\r\n");
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
}
