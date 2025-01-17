package com.liferay.portal.servlet.filters.sso.opensso;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.security.auth.AutoLoginException;
import com.liferay.util.CookieUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

/**
 * <a href="OpenSSOUtil.java.html"><b><i>View Source</i></b></a>
 *
 * <p>
 * See http://support.liferay.com/browse/LEP-5943.
 * </p>
 *
 * @author Prashant Dighe
 * @author Brian Wing Shun Chan
 *
 */
public class OpenSSOUtil {

    public static Map<String, String> getAttributes(HttpServletRequest request, String serviceUrl) throws AutoLoginException {
        return _instance._getAttributes(request, serviceUrl);
    }

    public static String getSubjectId(HttpServletRequest request, String serviceUrl) {
        return _instance._getSubjectId(request, serviceUrl);
    }

    public static boolean isAuthenticated(HttpServletRequest request, String serviceUrl) throws IOException {
        return _instance._isAuthenticated(request, serviceUrl);
    }

    private OpenSSOUtil() {
    }

    private Map<String, String> _getAttributes(HttpServletRequest request, String serviceUrl) throws AutoLoginException {
        Map<String, String> nameValues = new HashMap<String, String>();
        String url = serviceUrl + _GET_ATTRIBUTES;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection urlc = (HttpURLConnection) urlObj.openConnection();
            urlc.setDoOutput(true);
            urlc.setRequestMethod("POST");
            urlc.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            String[] cookieNames = _getCookieNames(serviceUrl);
            _setCookieProperty(request, urlc, cookieNames);
            OutputStreamWriter osw = new OutputStreamWriter(urlc.getOutputStream());
            osw.write("dummy");
            osw.flush();
            int responseCode = urlc.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) urlc.getContent()));
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("userdetails.attribute.name=")) {
                        String name = line.replaceFirst("userdetails.attribute.name=", "");
                        line = br.readLine();
                        if (line.startsWith("userdetails.attribute.value=")) {
                            String value = line.replaceFirst("userdetails.attribute.value=", "");
                            nameValues.put(name, value);
                        } else {
                            throw new AutoLoginException("Invalid user attribute: " + line);
                        }
                    }
                }
            } else if (_log.isDebugEnabled()) {
                _log.debug("Attributes response code " + responseCode);
            }
        } catch (MalformedURLException mfue) {
            _log.error(mfue.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug(mfue, mfue);
            }
        } catch (IOException ioe) {
            _log.error(ioe.getMessage());
            if (_log.isDebugEnabled()) {
                _log.debug(ioe, ioe);
            }
        }
        return nameValues;
    }

    private String[] _getCookieNames(String serviceUrl) {
        String[] cookieNames = _cookieNamesMap.get(serviceUrl);
        if (cookieNames != null) {
            return cookieNames;
        }
        List<String> cookieNamesList = new ArrayList<String>();
        try {
            String cookieName = null;
            String url = serviceUrl + _GET_COOKIE_NAME;
            URL urlObj = new URL(url);
            HttpURLConnection urlc = (HttpURLConnection) urlObj.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) urlc.getContent()));
            int responseCode = urlc.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (_log.isDebugEnabled()) {
                    _log.debug(url + " has response code " + responseCode);
                }
            } else {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("string=")) {
                        line = line.replaceFirst("string=", "");
                        cookieName = line;
                    }
                }
            }
            url = serviceUrl + _GET_COOKIE_NAMES;
            urlObj = new URL(url);
            urlc = (HttpURLConnection) urlObj.openConnection();
            br = new BufferedReader(new InputStreamReader((InputStream) urlc.getContent()));
            if (urlc.getResponseCode() != HttpURLConnection.HTTP_OK) {
                if (_log.isDebugEnabled()) {
                    _log.debug(url + " has response code " + responseCode);
                }
            } else {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("string=")) {
                        line = line.replaceFirst("string=", "");
                        if (cookieName.equals(line)) {
                            cookieNamesList.add(0, cookieName);
                        } else {
                            cookieNamesList.add(line);
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            if (_log.isWarnEnabled()) {
                _log.warn(ioe, ioe);
            }
        }
        cookieNames = cookieNamesList.toArray(new String[cookieNamesList.size()]);
        _cookieNamesMap.put(serviceUrl, cookieNames);
        return cookieNames;
    }

    private String _getSubjectId(HttpServletRequest request, String serviceUrl) {
        String cookieName = _getCookieNames(serviceUrl)[0];
        return CookieUtil.get(request, cookieName);
    }

    private boolean _isAuthenticated(HttpServletRequest request, String serviceUrl) throws IOException {
        boolean authenticated = false;
        String url = serviceUrl + _VALIDATE_TOKEN;
        URL urlObj = new URL(url);
        HttpURLConnection urlc = (HttpURLConnection) urlObj.openConnection();
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        String[] cookieNames = _getCookieNames(serviceUrl);
        _setCookieProperty(request, urlc, cookieNames);
        OutputStreamWriter osw = new OutputStreamWriter(urlc.getOutputStream());
        osw.write("dummy");
        osw.flush();
        int responseCode = urlc.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            authenticated = true;
        } else if (_log.isDebugEnabled()) {
            _log.debug("Authentication response code " + responseCode);
        }
        return authenticated;
    }

    private void _setCookieProperty(HttpServletRequest request, HttpURLConnection urlc, String[] cookieNames) {
        StringBuilder sb = new StringBuilder();
        for (String cookieName : cookieNames) {
            String cookieValue = CookieUtil.get(request, cookieName);
            sb.append(cookieName);
            sb.append(StringPool.EQUAL);
            sb.append(cookieValue);
            sb.append(StringPool.SEMICOLON);
        }
        if (sb.length() > 0) {
            urlc.setRequestProperty("Cookie", sb.toString());
        }
    }

    private static final String _GET_ATTRIBUTES = "/identity/attributes";

    private static final String _GET_COOKIE_NAME = "/identity/getCookieNameForToken";

    private static final String _GET_COOKIE_NAMES = "/identity/getCookieNamesToForward";

    private static final String _VALIDATE_TOKEN = "/identity/isTokenValid";

    private static Log _log = LogFactoryUtil.getLog(OpenSSOUtil.class);

    private static OpenSSOUtil _instance = new OpenSSOUtil();

    private Map<String, String[]> _cookieNamesMap = new ConcurrentHashMap<String, String[]>();
}
