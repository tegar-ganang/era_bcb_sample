package com.gwtaf.core.server.gwt;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.gwtaf.core.server.i18n.I18NStringFactory;
import com.gwtaf.core.server.i18n.LocaleUtil;
import com.gwtaf.core.shared.i18n.util.I18N;
import com.gwtaf.core.shared.util.ExceptionUtil;
import com.gwtaf.core.shared.util.HTMLUtil;
import com.gwtaf.core.shared.util.StringUtil;
import com.gwtaf.core.shared.util.UnexpectedErrorException;

public class RemoteServiceServletEx extends RemoteServiceServlet {

    private static final String UNEXPECTED_FAILURE = "respondWithUnexpectedFailure failed while sending the previous failure to the client";

    private static final String UNEXPECTED_SERVICE_FAILURE_WAS_ENCOUNTERED = "<h2>An unexpected service failure was encountered.</h2>";

    private static final long serialVersionUID = 1L;

    static {
        I18N.setFactory(new I18NStringFactory(LocaleUtil.getGetter()));
    }

    protected void invalidateHttpSession() {
        HttpSession session = getThreadLocalRequest().getSession(false);
        if (session != null) session.invalidate();
    }

    protected boolean isHttpSession(boolean notNew) {
        HttpSession session = getThreadLocalRequest().getSession(false);
        return session != null && (notNew == false || !session.isNew());
    }

    public HttpSession getHttpSession() {
        return getThreadLocalRequest().getSession(true);
    }

    @Override
    public String processCall(String payload) throws SerializationException {
        LocaleUtil.setLocale(getThreadLocalRequest().getLocale());
        return doProcessCall(payload);
    }

    protected String doProcessCall(String payload) throws SerializationException {
        return super.processCall(payload);
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        ServletContext servletContext = getServletContext();
        if (e instanceof UnexpectedException) e = ((UnexpectedException) e).getCause();
        writeResponseForUnexpectedFailure(servletContext, getThreadLocalResponse(), e);
    }

    private void writeResponseForUnexpectedFailure(ServletContext servletContext, HttpServletResponse response, Throwable failure) {
        servletContext.log("Exception while dispatching incoming RPC call", failure);
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (isReportToClient()) {
            try {
                response.getWriter().write(formatExceptionHTML(failure, true));
            } catch (IOException ex) {
                servletContext.log(UNEXPECTED_FAILURE, ex);
            }
        } else try {
            String errorId = recordException(failure);
            if (StringUtil.isNull(errorId)) throw new UnexpectedErrorException("recordException does not return a valid error id");
            StringBuilder buff = new StringBuilder();
            buff.append("<html><body><div>");
            buff.append(UNEXPECTED_SERVICE_FAILURE_WAS_ENCOUNTERED);
            buff.append("The error has been recorded, error id=").append(errorId);
            buff.append("</div></body></html>");
            response.getWriter().write(buff.toString());
        } catch (RuntimeException ex) {
            servletContext.log("recordException failed while processing the previous failure", ex);
            StringBuilder buff = new StringBuilder();
            buff.append("<html><body><div>");
            buff.append(UNEXPECTED_SERVICE_FAILURE_WAS_ENCOUNTERED);
            buff.append("See server log for details.");
            buff.append("</div></body></html>");
            try {
                response.getWriter().write(buff.toString());
            } catch (IOException ex1) {
                servletContext.log(UNEXPECTED_FAILURE, ex1);
            }
            return;
        } catch (IOException ex) {
            servletContext.log(UNEXPECTED_FAILURE, ex);
            return;
        }
    }

    protected boolean isReportToClient() {
        return true;
    }

    protected String recordException(Throwable failure) {
        return null;
    }

    public static String formatExceptionHTML(Throwable failure, boolean addServerHint) {
        StringBuilder buff = new StringBuilder();
        buff.append("<html><body><div>");
        buff.append(HTMLUtil.inactivateAndNewline(failure.getMessage()));
        buff.append("</div><br>");
        String stackTraceHTML = ExceptionUtil.dumpStackTraceHTML(failure.getCause(), 8);
        if (StringUtil.isValid(stackTraceHTML)) {
            buff.append("<h2>Server callstack:</h2>");
            buff.append("<div id='details' style='border:solid 1px gray; padding:4; margin-top:8;'>");
            buff.append(stackTraceHTML);
            buff.append("</div>");
        }
        buff.append("<br>");
        if (addServerHint) {
            buff.append(SimpleDateFormat.getDateTimeInstance().format(new Date()));
            buff.append(", see server log for details.<br>");
        }
        buff.append("</body></html>");
        return buff.toString();
    }

    protected Cookie getCookie(String name) {
        Cookie[] cookies = getThreadLocalRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) if (cookie.getName().equals(name)) return cookie;
        }
        return null;
    }

    protected String getCookieValue(String name) {
        Cookie[] cookies = getThreadLocalRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    protected void addCookie(Cookie cookie) {
        getThreadLocalResponse().addCookie(cookie);
    }
}
