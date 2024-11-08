package org.apache.jsp.system.message;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class message_005flist_005fform_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

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

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005flogic_005fnotEmpty_0026_005fname;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fmytag_005fviewCol_0026_005ftable_005fname;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005fc_005fif_0026_005ftest;

    private org.apache.jasper.runtime.TagHandlerPool _005fjspx_005ftagPool_005flogic_005fempty_0026_005fname;

    private javax.el.ExpressionFactory _el_expressionfactory;

    private org.apache.AnnotationProcessor _jsp_annotationprocessor;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspInit() {
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005flogic_005fnotEmpty_0026_005fname = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fmytag_005fviewCol_0026_005ftable_005fname = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _005fjspx_005ftagPool_005flogic_005fempty_0026_005fname = org.apache.jasper.runtime.TagHandlerPool.getTagHandlerPool(getServletConfig());
        _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
        _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
    }

    public void _jspDestroy() {
        _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname.release();
        _005fjspx_005ftagPool_005flogic_005fnotEmpty_0026_005fname.release();
        _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.release();
        _005fjspx_005ftagPool_005fmytag_005fviewCol_0026_005ftable_005fname.release();
        _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.release();
        _005fjspx_005ftagPool_005flogic_005fempty_0026_005fname.release();
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
            out.write("<title>中山市中凯信息科技有限公司</title>\r\n");
            out.write("<link type=\"text/css\" rel=\"stylesheet\" href=\"system/css/style.css\" />\r\n");
            out.write("<link href=\"system/plugins/validateMyForm/css/plugin.css\" rel=\"stylesheet\" type=\"text/css\">\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/js/jquery-1.4.2.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/js/verify1.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" src=\"system/plugins/validateMyForm/js/jquery.validateMyForm.1.5.js\"></script>\r\n");
            out.write("<script type=\"text/javascript\" language=\"javascript\" src=\"system/js/public.js\"></script>\r\n");
            out.write("</head>\r\n");
            out.write("<body>\r\n");
            out.write("<div class=\"main\">\r\n");
            out.write("\t<div class=\"position\">当前位置: <a href=\"sysadm/desktop.jsp\">桌 面</a> → 用户信息中心</div>\r\n");
            out.write("\t<div class=\"mainbody\">\r\n");
            out.write("\t\t<div class=\"operate_info\">\r\n");
            out.write("\t\t\t未阅读的信息：<br/>\t\t\r\n");
            out.write("\t\t</div>\r\n");
            out.write("\t\t<div class=\"table\">\t\r\n");
            out.write("\t\t\t\t\r\n");
            out.write("\t\t\t<table width=\"100%\" border=\"0\" cellpadding=\"1\" cellspacing=\"1\" class=\"table_list\">\t\t\t\t\r\n");
            out.write("\t\t\t\t<tr>\r\n");
            out.write("\t\t\t\t\t<th>发件人</th>\r\n");
            out.write("\t\t\t\t\t<th>标题</th>\r\n");
            out.write("\t\t\t\t\t<th>内容</th>\r\n");
            out.write("\t\t\t\t\t<th>信息状态</th>\t\t\t\t\t\r\n");
            out.write("\t\t\t\t</tr>\r\n");
            out.write("\t\t\t\t");
            if (_jspx_meth_mytag_005fList_005f0(_jspx_page_context)) return;
            out.write("\t\t\t\r\n");
            out.write("\t\t\t\t");
            org.apache.struts.taglib.logic.NotEmptyTag _jspx_th_logic_005fnotEmpty_005f0 = (org.apache.struts.taglib.logic.NotEmptyTag) _005fjspx_005ftagPool_005flogic_005fnotEmpty_0026_005fname.get(org.apache.struts.taglib.logic.NotEmptyTag.class);
            _jspx_th_logic_005fnotEmpty_005f0.setPageContext(_jspx_page_context);
            _jspx_th_logic_005fnotEmpty_005f0.setParent(null);
            _jspx_th_logic_005fnotEmpty_005f0.setName("newMsgs");
            int _jspx_eval_logic_005fnotEmpty_005f0 = _jspx_th_logic_005fnotEmpty_005f0.doStartTag();
            if (_jspx_eval_logic_005fnotEmpty_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                do {
                    out.write("\t\t\t\r\n");
                    out.write("\t\t\t\t\t");
                    org.apache.struts.taglib.logic.IterateTag _jspx_th_logic_005fiterate_005f0 = (org.apache.struts.taglib.logic.IterateTag) _005fjspx_005ftagPool_005flogic_005fiterate_0026_005fname_005fid.get(org.apache.struts.taglib.logic.IterateTag.class);
                    _jspx_th_logic_005fiterate_005f0.setPageContext(_jspx_page_context);
                    _jspx_th_logic_005fiterate_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fnotEmpty_005f0);
                    _jspx_th_logic_005fiterate_005f0.setId("msg");
                    _jspx_th_logic_005fiterate_005f0.setName("newMsgs");
                    int _jspx_eval_logic_005fiterate_005f0 = _jspx_th_logic_005fiterate_005f0.doStartTag();
                    if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
                        java.lang.Object msg = null;
                        if (_jspx_eval_logic_005fiterate_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                            out = _jspx_page_context.pushBody();
                            _jspx_th_logic_005fiterate_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                            _jspx_th_logic_005fiterate_005f0.doInitBody();
                        }
                        msg = (java.lang.Object) _jspx_page_context.findAttribute("msg");
                        do {
                            out.write("\t\t\t\t\t\t\t\t\t\t\t\r\n");
                            out.write("\t\t\t\t\t<tr>\t\t\t\t\t\t\t\t\t\r\n");
                            out.write("\t\t\t\t\t   ");
                            if (_jspx_meth_mytag_005fviewCol_005f0(_jspx_th_logic_005fiterate_005f0, _jspx_page_context)) return;
                            out.write("\r\n");
                            out.write("\t\t\t\t\t\t<td>");
                            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${user.userAccount}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                            out.write("</td>\r\n");
                            out.write("\t\t\t\t\t\t<td>");
                            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${msg.xxbt }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                            out.write("</td>\r\n");
                            out.write("\t\t\t\t\t\t<td>");
                            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${msg.xxnr}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                            out.write("</td>\r\n");
                            out.write("\t\t\t\t\t\t<td><a href=\"system/message/read_message.do?xh=");
                            out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${msg.xh}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                            out.write("\">\r\n");
                            out.write("\t\t\t\t\t\t\t");
                            if (_jspx_meth_c_005fif_005f0(_jspx_th_logic_005fiterate_005f0, _jspx_page_context)) return;
                            out.write("\r\n");
                            out.write("\t\t\t\t\t\t\t");
                            if (_jspx_meth_c_005fif_005f1(_jspx_th_logic_005fiterate_005f0, _jspx_page_context)) return;
                            out.write("\r\n");
                            out.write("\t\t\t\t\t\t\t\r\n");
                            out.write("\t\t\t\t\t\t\t \r\n");
                            out.write("\t\t\t\t\t\t</a></td>\r\n");
                            out.write("\t\t\t\t\t\r\n");
                            out.write("\t\t\t\t\t</tr>\r\n");
                            out.write("\t\t\t\t\t");
                            int evalDoAfterBody = _jspx_th_logic_005fiterate_005f0.doAfterBody();
                            msg = (java.lang.Object) _jspx_page_context.findAttribute("msg");
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
                    out.write("\t\t\t\t");
                    int evalDoAfterBody = _jspx_th_logic_005fnotEmpty_005f0.doAfterBody();
                    if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
                } while (true);
            }
            if (_jspx_th_logic_005fnotEmpty_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
                _005fjspx_005ftagPool_005flogic_005fnotEmpty_0026_005fname.reuse(_jspx_th_logic_005fnotEmpty_005f0);
                return;
            }
            _005fjspx_005ftagPool_005flogic_005fnotEmpty_0026_005fname.reuse(_jspx_th_logic_005fnotEmpty_005f0);
            out.write("\t\t\t\r\n");
            out.write("\t\t\t\t");
            if (_jspx_meth_logic_005fempty_005f0(_jspx_page_context)) return;
            out.write("\r\n");
            out.write("\t\t\t\t\t\t\t\t\r\n");
            out.write("\t\t\t</table>\r\n");
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
        com.zhongkai.web.tag.ListTag _jspx_th_mytag_005fList_005f0 = (com.zhongkai.web.tag.ListTag) _005fjspx_005ftagPool_005fmytag_005fList_0026_005ftable_005fname.get(com.zhongkai.web.tag.ListTag.class);
        _jspx_th_mytag_005fList_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fList_005f0.setParent(null);
        _jspx_th_mytag_005fList_005f0.setName("newMsgs");
        _jspx_th_mytag_005fList_005f0.setTable("TXtXxts");
        int _jspx_eval_mytag_005fList_005f0 = _jspx_th_mytag_005fList_005f0.doStartTag();
        if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            if (_jspx_eval_mytag_005fList_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.pushBody();
                _jspx_th_mytag_005fList_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                _jspx_th_mytag_005fList_005f0.doInitBody();
            }
            do {
                out.write("sjrDm='");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${sessionScope.user.userIdentifier }", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                out.write('\'');
                out.write(' ');
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

    private boolean _jspx_meth_mytag_005fviewCol_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_logic_005fiterate_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        com.zhongkai.web.tag.ViewByColumnTag _jspx_th_mytag_005fviewCol_005f0 = (com.zhongkai.web.tag.ViewByColumnTag) _005fjspx_005ftagPool_005fmytag_005fviewCol_0026_005ftable_005fname.get(com.zhongkai.web.tag.ViewByColumnTag.class);
        _jspx_th_mytag_005fviewCol_005f0.setPageContext(_jspx_page_context);
        _jspx_th_mytag_005fviewCol_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f0);
        _jspx_th_mytag_005fviewCol_005f0.setName("user");
        _jspx_th_mytag_005fviewCol_005f0.setTable("com.zhongkai.model.config.User");
        int _jspx_eval_mytag_005fviewCol_005f0 = _jspx_th_mytag_005fviewCol_005f0.doStartTag();
        if (_jspx_eval_mytag_005fviewCol_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            if (_jspx_eval_mytag_005fviewCol_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.pushBody();
                _jspx_th_mytag_005fviewCol_005f0.setBodyContent((javax.servlet.jsp.tagext.BodyContent) out);
                _jspx_th_mytag_005fviewCol_005f0.doInitBody();
            }
            do {
                out.write("userIdentifier=");
                out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${msg.fjrDm}", java.lang.String.class, (PageContext) _jspx_page_context, null, false));
                int evalDoAfterBody = _jspx_th_mytag_005fviewCol_005f0.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
            if (_jspx_eval_mytag_005fviewCol_005f0 != javax.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE) {
                out = _jspx_page_context.popBody();
            }
        }
        if (_jspx_th_mytag_005fviewCol_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005fmytag_005fviewCol_0026_005ftable_005fname.reuse(_jspx_th_mytag_005fviewCol_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005fmytag_005fviewCol_0026_005ftable_005fname.reuse(_jspx_th_mytag_005fviewCol_005f0);
        return false;
    }

    private boolean _jspx_meth_c_005fif_005f0(javax.servlet.jsp.tagext.JspTag _jspx_th_logic_005fiterate_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f0 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f0.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f0.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f0);
        _jspx_th_c_005fif_005f0.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${msg.zt=='n'||msg.zt=='N'}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f0 = _jspx_th_c_005fif_005f0.doStartTag();
        if (_jspx_eval_c_005fif_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t<span style=\"color: red\">未读</span>\r\n");
                out.write("\t\t\t\t\t\t\t");
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

    private boolean _jspx_meth_c_005fif_005f1(javax.servlet.jsp.tagext.JspTag _jspx_th_logic_005fiterate_005f0, PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.taglibs.standard.tag.rt.core.IfTag _jspx_th_c_005fif_005f1 = (org.apache.taglibs.standard.tag.rt.core.IfTag) _005fjspx_005ftagPool_005fc_005fif_0026_005ftest.get(org.apache.taglibs.standard.tag.rt.core.IfTag.class);
        _jspx_th_c_005fif_005f1.setPageContext(_jspx_page_context);
        _jspx_th_c_005fif_005f1.setParent((javax.servlet.jsp.tagext.Tag) _jspx_th_logic_005fiterate_005f0);
        _jspx_th_c_005fif_005f1.setTest(((java.lang.Boolean) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${msg.zt=='y'||msg.zt=='Y'}", java.lang.Boolean.class, (PageContext) _jspx_page_context, null, false)).booleanValue());
        int _jspx_eval_c_005fif_005f1 = _jspx_th_c_005fif_005f1.doStartTag();
        if (_jspx_eval_c_005fif_005f1 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t\t\t<span style=\"color: green;\">已读</span>\r\n");
                out.write("\t\t\t\t\t\t\t");
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

    private boolean _jspx_meth_logic_005fempty_005f0(PageContext _jspx_page_context) throws Throwable {
        PageContext pageContext = _jspx_page_context;
        JspWriter out = _jspx_page_context.getOut();
        org.apache.struts.taglib.logic.EmptyTag _jspx_th_logic_005fempty_005f0 = (org.apache.struts.taglib.logic.EmptyTag) _005fjspx_005ftagPool_005flogic_005fempty_0026_005fname.get(org.apache.struts.taglib.logic.EmptyTag.class);
        _jspx_th_logic_005fempty_005f0.setPageContext(_jspx_page_context);
        _jspx_th_logic_005fempty_005f0.setParent(null);
        _jspx_th_logic_005fempty_005f0.setName("newMsgs");
        int _jspx_eval_logic_005fempty_005f0 = _jspx_th_logic_005fempty_005f0.doStartTag();
        if (_jspx_eval_logic_005fempty_005f0 != javax.servlet.jsp.tagext.Tag.SKIP_BODY) {
            do {
                out.write("\r\n");
                out.write("\t\t\t\t\t<tr>\r\n");
                out.write("\t\t\t\t\t\t<td colspan=\"3\">暂无记录</td>\r\n");
                out.write("\t\t\t\t\t</tr>\r\n");
                out.write("\t\t\t\t");
                int evalDoAfterBody = _jspx_th_logic_005fempty_005f0.doAfterBody();
                if (evalDoAfterBody != javax.servlet.jsp.tagext.BodyTag.EVAL_BODY_AGAIN) break;
            } while (true);
        }
        if (_jspx_th_logic_005fempty_005f0.doEndTag() == javax.servlet.jsp.tagext.Tag.SKIP_PAGE) {
            _005fjspx_005ftagPool_005flogic_005fempty_0026_005fname.reuse(_jspx_th_logic_005fempty_005f0);
            return true;
        }
        _005fjspx_005ftagPool_005flogic_005fempty_0026_005fname.reuse(_jspx_th_logic_005fempty_005f0);
        return false;
    }
}
