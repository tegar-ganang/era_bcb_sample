package cn.jsprun.filter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import cn.jsprun.utils.Base64Encoder;
import cn.jsprun.utils.Common;
import cn.jsprun.utils.FileCaptureResponseWrapper;
import cn.jsprun.utils.ForumInit;

public class FileCaptureFilter implements Filter {

    public void init(FilterConfig fc) throws ServletException {
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpSession session = request.getSession();
        Integer uid = (Integer) session.getAttribute("jsprun_uid");
        if (uid != null && uid == 0) {
            String accessPath = request.getRequestURI();
            int index = accessPath.lastIndexOf("/");
            if (index != -1 && accessPath.indexOf("archiver") == -1) {
                accessPath = accessPath.substring(index + 1);
            }
            HttpServletResponse response = (HttpServletResponse) res;
            Map<String, String> settings = ForumInit.settings;
            if (accessPath.equals("index.jsp") || accessPath.equals("") || accessPath.equals(settings.get("indexname"))) {
                int cacheindexlife = Common.toDigit(settings.get("cacheindexlife"));
                if (cacheindexlife > 0) {
                    String realPath = session.getServletContext().getRealPath("/");
                    String[] indexcache = getcacheinfo(0, realPath + settings.get("cachethreaddir"));
                    File file = new File(indexcache[0]);
                    if (file.exists() && file.length() == 0) {
                        file.delete();
                        int timestamp = (Integer) (request.getAttribute("timestamp"));
                        int timeoffset = (int) ((Float) session.getAttribute("timeoffset") * 3600);
                        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                        String debugtime = Common.gmdate(df, timestamp + timeoffset);
                        String filename = indexcache[0].substring(realPath.length());
                        FileCaptureResponseWrapper responseWrapper = new FileCaptureResponseWrapper(response);
                        chain.doFilter(request, responseWrapper);
                        String content = null;
                        if ("1".equals(settings.get("debug"))) {
                            content = "<script type=\"text/javascript\">document.getElementById(\"debuginfo\").innerHTML = \"Update at " + debugtime + ", Processed in 0.009121 second(s), 0 Queries\";</script>";
                        }
                        responseWrapper.writeFile(indexcache[0], content);
                        try {
                            request.getRequestDispatcher(filename).include(request, response);
                        } catch (Exception e) {
                        }
                        return;
                    }
                }
            } else if (accessPath.equals("viewthread.jsp") || accessPath.startsWith("thread-")) {
                int cacheindexlife = Common.toDigit(settings.get("cacheindexlife"));
                if (cacheindexlife > 0) {
                    String realPath = session.getServletContext().getRealPath("/");
                    String tid = request.getParameter("tid");
                    String page = request.getParameter("page");
                    if (accessPath.startsWith("thread-")) {
                        tid = accessPath.replaceAll("thread-([0-9]+)-([0-9]+)-([0-9]+)\\.html", "$1");
                        page = accessPath.replaceAll("thread-([0-9]+)-([0-9]+)-([0-9]+)\\.html", "$3");
                    }
                    page = page == null ? "1" : page;
                    String[] threadcache = getcacheinfo(Common.toDigit(tid), realPath + settings.get("cachethreaddir"));
                    File file = new File(threadcache[0]);
                    if (file.exists() && file.length() == 0 && page.equals("1")) {
                        file.delete();
                        int timestamp = (Integer) (request.getAttribute("timestamp"));
                        int timeoffset = (int) ((Float) session.getAttribute("timeoffset") * 3600);
                        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                        String debugtime = Common.gmdate(df, timestamp + timeoffset);
                        String filename = threadcache[0].substring(realPath.length());
                        FileCaptureResponseWrapper responseWrapper = new FileCaptureResponseWrapper(response);
                        chain.doFilter(request, responseWrapper);
                        String content = null;
                        if ("1".equals(settings.get("debug"))) {
                            content = "<script type=\"text/javascript\">document.getElementById(\"debuginfo\").innerHTML = \"Update at " + debugtime + ", Processed in 0.009121 second(s), 0 Queries\";</script>";
                        }
                        responseWrapper.writeFile(threadcache[0], content);
                        try {
                            request.getRequestDispatcher(filename).include(request, response);
                        } catch (Exception e) {
                        }
                        return;
                    }
                }
            }
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
    }

    private String[] getcacheinfo(int tid, String cachethreaddir) {
        String[] cache = new String[2];
        String tidmd5 = Base64Encoder.encode(tid + "", "utf-8");
        String fulldir = cachethreaddir + "/" + tidmd5.charAt(0) + "/" + tidmd5.charAt(1) + "/" + tidmd5.charAt(2) + "/";
        String filename = fulldir + tid + ".htm";
        File file = new File(filename);
        cache[0] = filename;
        if (!file.exists()) {
        } else {
            cache[1] = "0";
            File fullfile = new File(fulldir);
            if (!fullfile.exists()) {
                for (int i = 0; i < 3; i++) {
                    cachethreaddir += "/" + tidmd5.charAt(i);
                    File dirfile = new File(cachethreaddir);
                    if (!dirfile.exists()) {
                        dirfile.mkdir();
                    }
                }
            }
            fullfile = null;
        }
        file = null;
        return cache;
    }
}
