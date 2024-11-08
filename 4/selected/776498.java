package org.apache.jsp.system.book;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.sql.*;
import java.net.URLDecoder;

public final class car_005fregister_005fform_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

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

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fsequence_0026_005fkey_005fnobody;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005forderBy_005fname;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fmytag_005fsequence_0026_005fkey_005fnobody = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005forderBy_005fname = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname.release();
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.release();
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
        _005fjspx_005ftagPool_005fmytag_005fsequence_0026_005fkey_005fnobody.release();
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005forderBy_005fname.release();
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
            out.write("\r\n");
            out.write("\r\n");
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
            out.write("<meta http-equiv=\"pragma\" content=\"no-cache\">\r\n");
            out.write("<meta http-equiv=\"cache-control\" content=\"no-cache\">\r\n");
            out.write("<meta http-equiv=\"expires\" content=\"0\">\r\n");
            out.write("<title>车船登记</title>\r\n");
            out.write("<link type=\"text/css\" rel=\"stylesheet\" href=\"system/css/style.css\" />\r\n");
            out.write("<link type=\"text/css\" rel=\"stylesheet\" href=\"system/plugins/validateMyForm/css/plugin.css\" />\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/js/public.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/js/jquery-1.4.2.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/plugins/validateMyForm/js/jquery.validateMyForm.1.5.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/js/json2.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/js/NewDatePicker/WdatePicker.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/js/verify1.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" >\r\n");
            out.write("\t$(document).ready(\r\n");
            out.write("\t\tfunction(){\r\n");
            out.write("\t\t\t$(\"#form1\").validateMyForm();\r\n");
            out.write("\t\t}\r\n");
            out.write("\t);\r\n");
            out.write("</script>\r\n");
            out.write("</head>\r\n");
            out.write("<body>\r\n");
            out.write("<div class=\"main\">\r\n");
            out.write("\t<div class=\"position\">当前位置: <a href=\"sysadm/desktop.jsp\">桌 面</a> → 车船情况登记</div>\r\n");
            out.write("\t<div class=\"mainbody\">\r\n");
            out.write("\t\t<div class=\"operate_info\">操作说明：带 <span style=\"color: red;\">*</span> 号必填</div>\r\n");
            out.write("\t\t<div class=\"table\">\r\n");
            out.write("\t\t\t<form id=\"form1\" action=\"system/book/car_register_save.do\" method=\"post\">\r\n");
            out.write("\t\t\t\t<table width=\"100%\" border=\"0\" cellpadding=\"1\" cellspacing=\"1\" class=\"table_form\">\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">车主类别：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t");
            if (_jspx_meth_mytag_005fList_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t<select name=\"czlbDm\" id=\"czlbDm\" onchange=\"CZLBChange(this.value)\" class=\"required\">\r\n");
            out.write("\t\t\t\t\t\t\t\t<option value=\"\">---请选择---</option>\r\n");
            out.write("\t\t\t\t\t\t\t\t");
            org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f0 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
            _jspx_th_logic_005fiterate_005f0.setPageContext(_jspx_page_context);
            _jspx_th_logic_005fiterate_005f0.setParent(null);
            _jspx_th_logic_005fiterate_005f0.setId("czlb");
            _jspx_th_logic_005fiterate_005f0.setName("CZLB");
            int _jspx_eval_logic_005fiterate_005f0 = _jspx_th_logic_005fiterate_005f0.doStartTag();
            if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                java.lang.Object czlb = null;
                if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                    out = _jspx_page_context.pushBody();
                    _jspx_th_logic_005fiterate_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                    _jspx_th_logic_005fiterate_005f0.doInitBody();
                }
                czlb = (java.lang.Object) _jspx_page_context.findAttribute("czlb");
                do {
                    out.write("\r\n");
                    out.write("\t\t\t\t\t\t\t\t\t<option value=\"");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${czlb.czlbDm }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write('"');
                    out.write(' ');
                    if (_jspx_meth_c_005fif_005f0(_jspx_th_logic_005fiterate_005f0, _jspx_page_context)) return;
                    out.write('>');
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${czlb.czlbMc }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("</option>\r\n");
                    out.write("\t\t\t\t\t\t\t\t");
                    int evalDoAfterBody = _jspx_th_logic_005fiterate_005f0.doAfterBody();
                    czlb = (java.lang.Object) _jspx_page_context.findAttribute("czlb");
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
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">登记状态：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"djztDm\" id=\"djztDm\" value=\"正常使用\" readonly=\"readonly\" /></td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">纳税人编码：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"nsrbm\" id=\"nsrbm\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${param.nsrbm }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" onblur=\"searchCarOwner('system/ajax/common_findCarOwner.do',this)\" onkeypress=\"onlydigit(this)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">车船登记号：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"ccdjh\" id=\"ccdjh\" value=\"");
            if (_jspx_meth_mytag_005fsequence_005f0(_jspx_page_context)) return;
            out.write("\" readonly=\"readonly\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">车主名称：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"czMc\" id=\"czMc\" value=\"");
            out.print(request.getParameter("czMc") == null ? "" : URLDecoder.decode(request.getParameter("czMc"), "UTF-8"));
            out.write("\" class=\"required\"/>\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">身份证号码：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"sfzhm\" id=\"sfzhm\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${param.sfzhm }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" onblur=\"checkID(this)\" onfocus=\"tip(this,divProvince)\" class=\"required\"/>\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">地址：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"czDz\" id=\"czDz\" value=\"");
            out.print(request.getParameter("czDz") == null ? "" : URLDecoder.decode(request.getParameter("czDz"), "UTF-8"));
            out.write("\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">电话：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"czDh\" id=\"czDh\" value=\"");
            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${param.czDh }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
            out.write("\" onblur=\"checkPM(this)\" onfocus=\"tip(this,divProvince)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">车船牌照号码：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t粤T-<input name=\"ccpzh\" id=\"ccpzh\" onblur=\"verifyCarNo('system/ajax/common_findCarNo.do',this,jjcclxDm,ccdjh)\" class=\"required\"/>\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t\t<label id=\"lblccpzh\"></label>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">车船类型：</td>\r\n");
            out.write("\t\t\t\t\t\t<td>\r\n");
            out.write("\t\t\t\t\t\t\t");
            if (_jspx_meth_mytag_005fList_005f1(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t<select name=\"jjcclxDm\" id=\"jjcclxDm\" onblur=\"verifyCarNo('system/ajax/common_findCarNo.do',ccpzh,this,ccdjh); searchCarInfo('system/ajax/common_findCarInfo.do',this);\" class=\"required\">\r\n");
            out.write("\t\t\t\t\t\t\t\t<option value=\"\">---请选择---</option>\r\n");
            out.write("\t\t\t\t\t\t\t\t");
            org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f1 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
            _jspx_th_logic_005fiterate_005f1.setPageContext(_jspx_page_context);
            _jspx_th_logic_005fiterate_005f1.setParent(null);
            _jspx_th_logic_005fiterate_005f1.setId("jjcclx");
            _jspx_th_logic_005fiterate_005f1.setName("JJCCLX");
            int _jspx_eval_logic_005fiterate_005f1 = _jspx_th_logic_005fiterate_005f1.doStartTag();
            if (_jspx_eval_logic_005fiterate_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                java.lang.Object jjcclx = null;
                if (_jspx_eval_logic_005fiterate_005f1 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                    out = _jspx_page_context.pushBody();
                    _jspx_th_logic_005fiterate_005f1.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                    _jspx_th_logic_005fiterate_005f1.doInitBody();
                }
                jjcclx = (java.lang.Object) _jspx_page_context.findAttribute("jjcclx");
                do {
                    out.write("\r\n");
                    out.write("\t\t\t\t\t\t\t\t\t<option value=\"");
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${jjcclx.jjcclxDm }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write('"');
                    out.write('>');
                    out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${jjcclx.jjcclxMc }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                    out.write("</option>\r\n");
                    out.write("\t\t\t\t\t\t\t\t");
                    int evalDoAfterBody = _jspx_th_logic_005fiterate_005f1.doAfterBody();
                    jjcclx = (java.lang.Object) _jspx_page_context.findAttribute("jjcclx");
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
            out.write("\t\t\t\t\t\t\t</select>\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">发动机号：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"fdjh\" id=\"fdjh\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">车架号：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"cjh\" id=\"cjh\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">厂牌型号：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"cpxh\" id=\"cpxh\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">行驶证发证日期：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"fzRq\" id=\"fzRq\" onFocus=\"WdatePicker()\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">核定载重量（吨）：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"hdzzl\" id=\"hdzzl\" readonly=\"readonly\" onblur=\"CheckValue(this)\" onkeypress=\"onlynum(this)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">整备质量（吨）：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"zbzl\" id=\"zbzl\" readonly=\"readonly\" onblur=\"CheckValue(this)\" onkeypress=\"onlynum(this)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">核定载客（人）：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"hdzkl\" id=\"hdzkl\" readonly=\"readonly\" onblur=\"CheckValue(this)\" onkeypress=\"onlydigit(this)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">排气量（升）：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"pql\" id=\"pql\" readonly=\"readonly\" onblur=\"CheckValue(this)\" onkeypress=\"onlynum(this)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">登记日期：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"djRq\" id=\"djRq\" onFocus=\"WdatePicker({onpicked:function(){gdt('system/book/car_register_gdt.do',this,gzRq,sbqsny);}})\" onblur=\"gdt('system/book/car_register_gdt.do',this,gzRq,sbqsny)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">购置时间：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"gzRq\" id=\"gzRq\" onFocus=\"WdatePicker({onpicked:function(){gdt('system/book/car_register_gdt.do',djRq,this,sbqsny);}})\" onblur=\"gdt('system/book/car_register_gdt.do',djRq,this,sbqsny)\" class=\"required\" />\r\n");
            out.write("\t\t\t\t\t\t\t<span style=\"color: red;\">*</span>\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">申报起始年月：</td>\r\n");
            out.write("\t\t\t\t\t\t<td><input name=\"sbqsny\" id=\"sbqsny\" readonly=\"readonly\" /></td>\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\"></td>\r\n");
            out.write("\t\t\t\t\t\t<td></td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\">备注：</td>\r\n");
            out.write("\t\t\t\t\t\t<td colspan=\"3\">\r\n");
            out.write("\t\t\t\t\t\t\t<textarea name=\"bz\" id=\"bz\" cols=\"50\" rows=\"3\"></textarea>\r\n");
            out.write("\t\t\t\t\t\t\t非必填，长度不能超过100 \r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr onMouseOver=\"mouseOver(this)\" onMouseOut=\"mouseOut(this)\">\r\n");
            out.write("\t\t\t\t\t\t<td align=\"right\"></td>\r\n");
            out.write("\t\t\t\t\t\t<td colspan=\"3\"><input type=\"checkbox\" name=\"next\" id=\"next\" />保存成功后登记同一车主下一车辆</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t\t<tr>\r\n");
            out.write("\t\t\t\t\t\t<td colspan=\"4\" class=\"form_button\" style=\"padding-top:10px;\">\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"submit\" value=\"保存并继续登记\" />\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"submit\" value=\"保存并申报\" />\r\n");
            out.write("\t\t\t\t\t\t\t<input type=\"reset\" value=\"重置\" />\r\n");
            out.write("\t\t\t\t\t\t</td>\r\n");
            out.write("\t\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t</table>\r\n");
            out.write("\t\t\t\t<div id=\"divProvince\" name=\"divProvince\" style=\"display:none; position:absolute;width:260px;background-color:#BFEBEE; border:1px solid #BEC0BF;padding:5px;font-size:12px;\">  \r\n");
            out.write("\t\t\t\t        电话格式 ：<br/>手机：18,15,13开头 + 9位。<br/>固话：区号-电话。 例 ： 0760-88888888\r\n");
            out.write("\t\t\t\t</div>\r\n");
            out.write("\t\t\t</form>\r\n");
            out.write("\t\t</div>\r\n");
            out.write("\t</div>\r\n");
            out.write("</div>\r\n");
            out.write("<script type=\"text/javascript\">\r\n");
            out.write("\tfunction CZLBChange(value){\r\n");
            out.write("\t\tif(value==\"\"){\r\n");
            out.write("\t\t\t$('#nsrbm').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#nsrbm').attr(\"readonly\",false);\r\n");
            out.write("\t\t\t$('#sfzhm').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#sfzhm').attr(\"readonly\",false);\r\n");
            out.write("\t\t\t$('#czMc').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#czDz').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#czDh').attr(\"value\",'');\r\n");
            out.write("\t\t}\r\n");
            out.write("\t\tif(value==\"01\"){\r\n");
            out.write("\t\t\t$('#nsrbm').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#nsrbm').attr(\"readonly\",false);\r\n");
            out.write("\t\t\t$('#sfzhm').attr(\"value\",' ');\r\n");
            out.write("\t\t\t$('#sfzhm').attr(\"readonly\",true);\r\n");
            out.write("\t\t\t$('#czMc').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#czDz').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#czDh').attr(\"value\",'');\r\n");
            out.write("\t\t}\r\n");
            out.write("\t\tif(value==\"02\"){\r\n");
            out.write("\t\t\t$('#nsrbm').attr(\"value\",'00000099997');\r\n");
            out.write("\t\t\t$('#sfzhm').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#nsrbm').attr(\"readonly\",true);\r\n");
            out.write("\t\t\t$('#sfzhm').attr(\"readonly\",false);\r\n");
            out.write("\t\t\t$('#czMc').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#czDz').attr(\"value\",'');\r\n");
            out.write("\t\t\t$('#czDh').attr(\"value\",'');\r\n");
            out.write("\t\t}\r\n");
            out.write("\t}\r\n");
            out.write("</script>\r\n");
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
        com.zhongkai.web.tag.ListTag _jspx_th_mytag_005fList_005f0 = (com.zhongkai.web.tag.ListTag) _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname.get(com.zhongkai.web.tag.ListTag.class);
        _jspx_th_mytag_005fList_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fList_005f0.setParent(null);
        _jspx_th_mytag_005fList_005f0.setName("CZLB");
        _jspx_th_mytag_005fList_005f0.setTable("TDmCcsCzlb");
        int _jspx_eval_mytag_005fList_005f0 = _jspx_th_mytag_005fList_005f0.doStartTag();
        if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.pushBody();
                _jspx_th_mytag_005fList_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                _jspx_th_mytag_005fList_005f0.doInitBody();
            }
            do {
                out.write("xyBj='y' or xyBj='Y'");
                int evalDoAfterBody = _jspx_th_mytag_005fList_005f0.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
            if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.popBody();
            }
        }
        if (_jspx_th_mytag_005fList_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname.reuse(_jspx_th_mytag_005fList_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname.reuse(_jspx_th_mytag_005fList_005f0);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_logic_005fiterate_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f0);
        _jspx_th_c_005fif_005f0.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${param.czlbDm==czlb.czlbDm }", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
        if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("selected");
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

    private boolean _jspx_meth_mytag_005fsequence_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.SequenceTag _jspx_th_mytag_005fsequence_005f0 = (com.zhongkai.web.tag.SequenceTag) _005fjspx_005ftagPool_005fmytag_005fsequence_0026_005fkey_005fnobody.get(com.zhongkai.web.tag.SequenceTag.class);
        _jspx_th_mytag_005fsequence_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fsequence_005f0.setParent(null);
        _jspx_th_mytag_005fsequence_005f0.setKey("ccdjxx_seq");
        int _jspx_eval_mytag_005fsequence_005f0 = _jspx_th_mytag_005fsequence_005f0.doStartTag();
        if (_jspx_th_mytag_005fsequence_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fsequence_0026_005fkey_005fnobody.reuse(_jspx_th_mytag_005fsequence_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fsequence_0026_005fkey_005fnobody.reuse(_jspx_th_mytag_005fsequence_005f0);
        return false;
    }

    private boolean _jspx_meth_mytag_005fList_005f1(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.ListTag _jspx_th_mytag_005fList_005f1 = (com.zhongkai.web.tag.ListTag) _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005forderBy_005fname.get(com.zhongkai.web.tag.ListTag.class);
        _jspx_th_mytag_005fList_005f1.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fList_005f1.setParent(null);
        _jspx_th_mytag_005fList_005f1.setName("JJCCLX");
        _jspx_th_mytag_005fList_005f1.setTable("TDmCcsJjcclx");
        _jspx_th_mytag_005fList_005f1.setOrderBy("jjcclxDm");
        int _jspx_eval_mytag_005fList_005f1 = _jspx_th_mytag_005fList_005f1.doStartTag();
        if (_jspx_eval_mytag_005fList_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            if (_jspx_eval_mytag_005fList_005f1 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.pushBody();
                _jspx_th_mytag_005fList_005f1.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                _jspx_th_mytag_005fList_005f1.doInitBody();
            }
            do {
                out.write("xyBj='y' or xyBj='Y'");
                int evalDoAfterBody = _jspx_th_mytag_005fList_005f1.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
            if (_jspx_eval_mytag_005fList_005f1 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.popBody();
            }
        }
        if (_jspx_th_mytag_005fList_005f1.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005forderBy_005fname.reuse(_jspx_th_mytag_005fList_005f1);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005forderBy_005fname.reuse(_jspx_th_mytag_005fList_005f1);
        return false;
    }
}
