package cn.jsprun.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import cn.jsprun.domain.Members;
import cn.jsprun.foreg.utils.CookieUtil;
import cn.jsprun.service.system.DataBaseService;
import cn.jsprun.service.user.MemberService;
import cn.jsprun.utils.BeanFactory;
import cn.jsprun.utils.Common;
import cn.jsprun.utils.DataParse;
import cn.jsprun.utils.ForumInit;
import cn.jsprun.utils.HibernateUtil;
import cn.jsprun.utils.JspRunConfig;
import cn.jsprun.utils.Md5Token;

public class OnlineFilter implements Filter {

    public void init(FilterConfig fc) throws ServletException {
    }

    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        request.setAttribute("starttime", System.currentTimeMillis());
        int timestamp = Common.time();
        request.setAttribute("timestamp", timestamp);
        String accessPath = request.getRequestURI();
        int index = accessPath.lastIndexOf("/");
        if (index != -1) {
            accessPath = accessPath.substring(index + 1);
        }
        if (accessPath.startsWith("forum-")) {
            accessPath = "forumdisplay.jsp";
        } else if (accessPath.startsWith("thread-")) {
            accessPath = "viewthread.jsp";
        }
        request.setAttribute("CURSCRIPT", accessPath);
        if ("install.jsp".equals(accessPath)) {
            chain.doFilter(request, response);
            return;
        }
        if (HibernateUtil.getSessionFactory() == null) {
            request.setAttribute("errorinfo", HibernateUtil.getMessage());
            request.getRequestDispatcher("/errors/error_sql.jsp").forward(request, response);
            return;
        }
        Map<String, String> settings = ForumInit.settings;
        if (settings == null) {
            initForum(request.getSession().getServletContext());
            settings = ForumInit.settings;
        }
        int attackevasive = Common.toDigit(settings.get("attackevasive"));
        if (attackevasive > 0 && this.security(request, response, timestamp, attackevasive)) {
            return;
        }
        if ("1".equals(settings.get("nocacheheaders"))) {
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "private, post-check=0, pre-check=0, max-age=0");
            response.setDateHeader("Expires", 0);
        }
        HttpSession httpSession = request.getSession();
        if (httpSession.getAttribute("timeoffset") == null) {
            Common.setDateformat(httpSession, settings);
        }
        if (httpSession.getAttribute("refreshtime") == null) {
            Map forwardmap = ((DataParse) BeanFactory.getBean("dataParse")).characterParse(settings.get("msgforward"), false);
            httpSession.setAttribute("refreshtime", forwardmap == null ? "3" : forwardmap.get("refreshtime"));
            httpSession.setAttribute("quick", forwardmap == null ? (byte) 0 : Byte.valueOf(forwardmap.get("quick").toString()));
            httpSession.setAttribute("successmessages", forwardmap == null ? null : forwardmap.get("messages"));
            forwardmap = null;
        }
        if (request.getParameter("styleid") != null && !"admincp.jsp".equals(accessPath)) {
            httpSession.setAttribute("styleid", request.getParameter("styleid"));
        }
        if (httpSession.getAttribute("boardurl") == null) {
            httpSession.setAttribute("boardurl", (request.getScheme().concat("://").concat(request.getServerName()).concat(":") + request.getServerPort()).concat(request.getContextPath()).concat("/"));
        }
        String jsprun_sid = (String) httpSession.getAttribute("jsprun_sid");
        Integer jsprun_uid = (Integer) httpSession.getAttribute("jsprun_uid");
        String sid = CookieUtil.getCookie(request, "sid", true, settings);
        if (sid == null && jsprun_sid == null || sid != null && sid.equals("")) {
            sid = Common.getRandStr(6, false);
            CookieUtil.setCookie(request, response, "sid", sid, 604800, true, settings);
            httpSession.setAttribute("jsprun_sid", sid);
        } else if ((sid == null && jsprun_sid != null) || sid != null && jsprun_sid != null && !jsprun_sid.equals(sid)) {
            sid = jsprun_sid;
            CookieUtil.setCookie(request, response, "sid", jsprun_sid, 604800, true, settings);
        } else if (sid != null && jsprun_sid == null) {
            httpSession.setAttribute("jsprun_sid", sid);
        }
        if (jsprun_uid == null) {
            jsprun_uid = 0;
            String jsprun_userss = null;
            short groupid = 7;
            byte adminid = 0;
            String uid = CookieUtil.getCookie(request, "uid", true, settings);
            if (uid != null) {
                MemberService memberService = (MemberService) BeanFactory.getBean("memberService");
                Members member = memberService.findMemberById(Common.toDigit(uid, 1000000000L, 0L).intValue());
                if (member != null) {
                    String validateAuth = Md5Token.getInstance().getLongToken(member.getPassword() + "\t" + member.getSecques() + "\t" + member.getUid());
                    if (validateAuth.equals(CookieUtil.getCookie(request, "auth", true, settings))) {
                        jsprun_uid = member.getUid();
                        jsprun_userss = member.getUsername();
                        groupid = member.getGroupid();
                        adminid = member.getAdminid();
                        httpSession.setAttribute("user", member);
                        Common.setDateformat(httpSession, settings);
                        httpSession.setAttribute("jsprun_pw", member.getPassword());
                        if (member.getStyleid() > 0) {
                            httpSession.setAttribute("styleid", member.getStyleid().toString());
                        }
                    }
                }
            } else {
                CookieUtil.setCookie(request, response, "uid", String.valueOf(jsprun_uid), 604800, true, settings);
            }
            httpSession.setAttribute("jsprun_uid", jsprun_uid);
            httpSession.setAttribute("jsprun_userss", jsprun_userss != null ? jsprun_userss : "");
            httpSession.setAttribute("jsprun_groupid", groupid);
            httpSession.setAttribute("jsprun_adminid", adminid);
            httpSession.setAttribute("formhash", Common.getRandStr(8, false));
        }
        Common.sessionExists(request, response, sid, jsprun_uid, settings);
        Common.calcGroup(httpSession, request, response, settings);
        if (Common.allowAccessBbs(request, response, httpSession, settings, accessPath)) {
            return;
        }
        Common.setFtpValue(settings.get("ftp"), settings.get("authkey"));
        int rewritestatus = Integer.parseInt(settings.get("rewritestatus"));
        boolean forumdisplayurl = false;
        boolean viewthreadurl = false;
        boolean spaceurlurl = false;
        boolean tagsurl = false;
        forumdisplayurl = (rewritestatus & 1) > 0;
        viewthreadurl = (rewritestatus & 2) > 0;
        spaceurlurl = (rewritestatus & 4) > 0;
        tagsurl = (rewritestatus & 8) > 0;
        request.setAttribute("forumdisplayurl", forumdisplayurl);
        request.setAttribute("viewthreadurl", viewthreadurl);
        request.setAttribute("spaceurlurl", spaceurlurl);
        request.setAttribute("tagsurl", tagsurl);
        String statstatus = settings.get("statstatus");
        if (statstatus != null && statstatus.equals("1") && request.getParameter("inajax") == null) {
            Common.stats(request);
        }
        settings = null;
        chain.doFilter(request, response);
    }

    public void destroy() {
    }

    private synchronized void initForum(ServletContext context) {
        if (ForumInit.settings == null) {
            ForumInit.initServletContext(context);
        }
    }

    private boolean security(HttpServletRequest request, HttpServletResponse response, int timestamp, int attackevasive) {
        boolean attackevasive1 = (attackevasive & 1) > 0;
        boolean attackevasive2 = (attackevasive & 2) > 0;
        boolean attackevasive4 = (attackevasive & 4) > 0;
        boolean attackevasive8 = (attackevasive & 8) > 0;
        int lastrequest = 0;
        if (attackevasive1 || attackevasive4) {
            lastrequest = Common.toDigit(CookieUtil.getCookie(request, "lastrequest"));
            CookieUtil.setCookie(request, response, "lastrequest", String.valueOf(timestamp), timestamp + 816400);
        }
        if (attackevasive1) {
            if (timestamp - lastrequest < 1) {
                this.securitymessage(request, response, "attachsave_1_subject", "attachsave_1_message", true, false);
                return true;
            }
        }
        if (attackevasive2 && (request.getHeader("x-forwarded-for") != null || request.getHeader("via") != null)) {
            this.securitymessage(request, response, "attachsave_2_subject", "attachsave_2_message", false, false);
            return true;
        }
        if (attackevasive4) {
            if (lastrequest == 0 || timestamp - lastrequest > 60) {
                this.securitymessage(request, response, "attachsave_4_subject", "attachsave_4_message", true, false);
                return true;
            }
        }
        if (attackevasive8) {
            String questionanswer = null;
            String questiontime = null;
            String secqcode = CookieUtil.getCookie(request, "secqcode");
            if (secqcode != null) {
                String[] secqcodes = secqcode.split(",");
                if (secqcodes != null && secqcodes.length >= 2) {
                    questionanswer = secqcodes[0];
                    questiontime = secqcodes[1];
                }
            }
            String attackevasive_answer = CookieUtil.getCookie(request, "attackevasive_answer");
            if (questionanswer == null || questiontime == null || !questionanswer.equals(attackevasive_answer)) {
                String secqsubmit = request.getParameter("secqsubmit");
                String answer = request.getParameter("answer");
                if (secqsubmit == null || secqsubmit != null && !Md5Token.getInstance().getLongToken(answer).equals(attackevasive_answer)) {
                    CookieUtil.setCookie(request, response, "secqcode", "," + timestamp, timestamp + 816400);
                    this.securitymessage(request, response, updatesecqaa(request, response), "<input type='text' name='answer' size='8' maxlength='150' /><input class='button' type='submit' name='secqsubmit' value=' Submit ' />", false, true);
                    return true;
                } else {
                    CookieUtil.setCookie(request, response, "secqcode", attackevasive_answer + "," + timestamp, timestamp + 816400);
                }
            }
        }
        return false;
    }

    private void securitymessage(HttpServletRequest request, HttpServletResponse response, String subject, String message, boolean reload, boolean form) {
        Map<String, String> scuritylang = new HashMap<String, String>();
        scuritylang.put("attachsave_1_subject", "&#x9891;&#x7e41;&#x5237;&#x65b0;&#x9650;&#x5236;");
        scuritylang.put("attachsave_1_message", "&#x60a8;&#x8bbf;&#x95ee;&#x672c;&#x7ad9;&#x901f;&#x5ea6;&#x8fc7;&#x5feb;&#x6216;&#x8005;&#x5237;&#x65b0;&#x95f4;&#x9694;&#x65f6;&#x95f4;&#x5c0f;&#x4e8e;&#x4e24;&#x79d2;&#xff01;&#x8bf7;&#x7b49;&#x5f85;&#x9875;&#x9762;&#x81ea;&#x52a8;&#x8df3;&#x8f6c;&#x20;&#x2e;&#x2e;&#x2e;");
        scuritylang.put("attachsave_2_subject", "&#x4ee3;&#x7406;&#x670d;&#x52a1;&#x5668;&#x8bbf;&#x95ee;&#x9650;&#x5236;");
        scuritylang.put("attachsave_2_message", "&#x672c;&#x7ad9;&#x73b0;&#x5728;&#x9650;&#x5236;&#x4f7f;&#x7528;&#x4ee3;&#x7406;&#x670d;&#x52a1;&#x5668;&#x8bbf;&#x95ee;&#xff0c;&#x8bf7;&#x53bb;&#x9664;&#x60a8;&#x7684;&#x4ee3;&#x7406;&#x8bbe;&#x7f6e;&#xff0c;&#x76f4;&#x63a5;&#x8bbf;&#x95ee;&#x672c;&#x7ad9;&#x3002;");
        scuritylang.put("attachsave_4_subject", "&#x9875;&#x9762;&#x91cd;&#x8f7d;&#x5f00;&#x542f;");
        scuritylang.put("attachsave_4_message", "&#x6b22;&#x8fce;&#x5149;&#x4e34;&#x672c;&#x7ad9;&#xff0c;&#x9875;&#x9762;&#x6b63;&#x5728;&#x91cd;&#x65b0;&#x8f7d;&#x5165;&#xff0c;&#x8bf7;&#x7a0d;&#x5019;&#x20;&#x2e;&#x2e;&#x2e;");
        try {
            response.setContentType("text/html; charset=" + JspRunConfig.charset);
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Program", "no-cache");
            response.setDateHeader("Expirse", 0);
            boolean inajax = false;
            subject = scuritylang.get(subject) != null ? scuritylang.get(subject) : subject;
            PrintWriter out = response.getWriter();
            if (inajax) {
            } else {
                out.write("<html>");
                out.write("<head>");
                out.write("<title>" + subject + "</title>");
                out.write("</head>");
                out.write("<body bgcolor='#FFFFFF'>");
                if (reload) {
                    out.write("<script language='JavaScript'>");
                    out.write("function reload() {");
                    out.write("	document.location.reload();");
                    out.write("}");
                    out.write("setTimeout('reload()', 1001);");
                    out.write("</script>");
                }
                if (form) {
                    out.write("<form action='" + request.getRequestURI() + "' method='POST'>");
                }
                out.write("<table cellpadding='0' cellspacing='0' border='0' width='700' align='center' height='85%'>");
                out.write("  <tr align='center' valign='middle'>");
                out.write("    <td>");
                out.write("    <table cellpadding='10' cellspacing='0' border='0' width='80%' align='center' style='font-family: Verdana, Tahoma; color: #666666; font-size: 11px'>");
                out.write("    <tr>");
                out.write("      <td valign='middle' align='center' bgcolor='#EBEBEB'>");
                out.write("     	<br /><br /> <b style='font-size: 16px'>" + subject + "</b> <br /><br />");
                out.write(scuritylang.get(message) != null ? scuritylang.get(message) : message);
                out.write("        <br /><br />");
                out.write("      </td>");
                out.write("    </tr>");
                out.write("    </table>");
                out.write("    </td>");
                out.write("  </tr>");
                out.write("</table>");
                if (form) {
                    out.write("</form>");
                }
                out.write("</body>");
                out.write("</html>");
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String updatesecqaa(HttpServletRequest request, HttpServletResponse response) {
        List<Map<String, String>> itempools = ((DataBaseService) BeanFactory.getBean("dataBaseService")).executeQuery("SELECT question,answer FROM jrun_itempool ORDER BY rand() LIMIT 1");
        String attackevasive_question = null;
        if (itempools != null && itempools.size() > 0) {
            attackevasive_question = itempools.get(0).get("question");
            CookieUtil.setCookie(request, response, "attackevasive_answer", Md5Token.getInstance().getLongToken(itempools.get(0).get("answer")), 31536000);
        }
        return attackevasive_question;
    }
}
