package naru.aweb.handler;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import naru.aweb.config.Config;
import naru.aweb.config.Mapping;
import naru.aweb.http.HeaderParser;
import naru.aweb.http.WebServerHandler;
import naru.aweb.mapping.MappingResult;
import org.apache.log4j.Logger;

public class PublicHandler extends WebServerHandler {

    private static Logger logger = Logger.getLogger(PublicHandler.class);

    private static Config config = Config.getConfig();

    private static String PH_JS = "/js/ph.js";

    private static String PAC_PAGE = "/proxy.pac";

    private static String COOKIE_KEY = "PHANTOM_PAC";

    private static Pattern COOKIE_PATTERN = Pattern.compile(" *" + COOKIE_KEY + " *= *(\\S*)(;|\\z)");

    private String stripQuote(String value) {
        if (((value.startsWith("\"")) && (value.endsWith("\""))) || ((value.startsWith("'") && (value.endsWith("'"))))) {
            try {
                return value.substring(1, value.length() - 1);
            } catch (Exception ex) {
            }
        }
        return value;
    }

    private String getCookieAuth(HeaderParser requestHeader) {
        String cookieHeader = requestHeader.getHeader(HeaderParser.COOKIE_HEADER);
        if (cookieHeader == null) {
            return null;
        }
        Matcher matcher;
        synchronized (COOKIE_PATTERN) {
            matcher = COOKIE_PATTERN.matcher(cookieHeader);
        }
        if (matcher.find() == false) {
            return null;
        }
        String cookieAuth = matcher.group(1);
        return stripQuote(cookieAuth);
    }

    private static long MAX_AGE = (365l * 24l * 60l * 60l * 1000l);

    private String createSetCookieHeader(String loginId, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(COOKIE_KEY);
        sb.append("=");
        sb.append(loginId);
        sb.append("; expires=");
        String expires = HeaderParser.fomatDateHeader(new Date(System.currentTimeMillis() + MAX_AGE));
        sb.append(expires);
        sb.append("; path=");
        sb.append(path);
        return sb.toString();
    }

    private void setCookie(String loginId, String path) {
        String setCookieHeader = createSetCookieHeader(loginId, path);
        setHeader(HeaderParser.SET_COOKIE_HEADER, setCookieHeader);
    }

    private void responseProxyPac() {
        HeaderParser requestHeader = getRequestHeader();
        String cookieUserId = getCookieAuth(requestHeader);
        MappingResult mapping = getRequestMapping();
        mapping.setResolvePath(PAC_PAGE);
        mapping.setDesitinationFile(config.getAdminDocumentRoot());
        forwardHandler(Mapping.VELOCITY_PAGE_HANDLER);
    }

    public void startResponseReqBody() {
        MappingResult mapping = getRequestMapping();
        String path = mapping.getResolvePath();
        if (PAC_PAGE.equals(path)) {
            responseProxyPac();
            return;
        }
        mapping.setDesitinationFile(config.getPublicDocumentRoot());
        WebServerHandler response = (WebServerHandler) forwardHandler(Mapping.FILE_SYSTEM_HANDLER);
        logger.debug("responseObject:cid:" + getChannelId() + ":" + response + ":" + this);
    }
}
