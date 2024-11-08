package fiji;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MediaWikiClient {

    final String wikiBaseURI;

    String sessionID;

    public MediaWikiClient() {
        this("http://pacific.mpi-cbg.de/wiki/index.php");
    }

    public MediaWikiClient(String wikiBaseURI) {
        this.wikiBaseURI = wikiBaseURI;
    }

    boolean hasSessionKey() {
        return cookies.containsKey("wikidb_session");
    }

    boolean loggedIn = false;

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean logIn(String user, String password) {
        if (loggedIn) return true;
        try {
            String[] getVars = { "title", "Special:Userlogin", "action", "submitlogin", "type", "login" };
            String[] postVars = { "wpName", user, "wpPassword", password };
            if (!hasSessionKey()) {
                sendRequest(getVars, postVars, null, true);
                if (!hasSessionKey()) {
                    System.err.println("Failed to " + "get session key!");
                    return false;
                }
            }
            String response = sendRequest(getVars, postVars);
            loggedIn = response.indexOf("Login error:") < 0;
            return loggedIn;
        } catch (IOException e) {
        }
        return false;
    }

    public boolean logOut() {
        if (!loggedIn) return true;
        try {
            String[] getVars = { "title", "Special:Userlogout" };
            String response = sendRequest(getVars, null);
            String expect = "You are now logged out.";
            loggedIn = response.indexOf(expect) < 0;
            return !loggedIn;
        } catch (IOException e) {
        }
        return false;
    }

    String valueRegex = "value=\"([^\"]*)\"";

    Pattern editFormPattern = Pattern.compile(".*" + "<input type='hidden' " + valueRegex + " name=\"wpEdittime\" />.*" + "<input type='hidden' " + valueRegex + " name=\"wpEditToken\" />.*" + "<input name=\"wpAutoSummary\" " + "type=\"hidden\" " + valueRegex + " />.*", Pattern.DOTALL);

    public boolean uploadPage(String title, String contents, String comment) {
        return uploadOrPreviewPage(title, contents, comment, false) != null;
    }

    public String uploadOrPreviewPage(String title, String contents, String comment, boolean previewOnly) {
        try {
            String[] getVars = { "title", title, "action", "edit" };
            String response = sendRequest(getVars, null);
            Matcher matcher = editFormPattern.matcher(response);
            if (!matcher.matches()) return null;
            getVars = new String[] { "title", title, "action", "submit" };
            String[] postVars = new String[] { "wpSave", "Save page", "wpTextbox1", contents, "wpSummary", comment, "wpEdittime", matcher.group(1), "wpEditToken", matcher.group(2), "wpAutoSummary", matcher.group(3) };
            if (previewOnly) {
                postVars[0] = "wpPreview";
                postVars[1] = "Show preview";
            }
            return sendRequest(getVars, postVars);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean uploadFile(String fileName, String summary, File file) {
        try {
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream in = new FileInputStream(file);
            in.read(buffer);
            in.close();
            return uploadFile(fileName, summary, buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean uploadFile(String fileName, String summary, byte[] contents) {
        String[] getVars = { "title", "Special:Upload" };
        String[] postVars = { "wpIgnoreWarning", "1", "wpSourceType", "file", "wpDestFile", fileName, "wpUploadDescription", summary, "wpUpload", "Upload file" };
        Object[] fileVars = { "wpUploadFile", fileName, contents };
        try {
            String response = sendRequest(getVars, postVars, fileVars, false);
            boolean success = response.indexOf("<h2>Successful " + "upload</h2>") > 0 || response.indexOf("No higher resolution " + "available.") > 0 || response.indexOf("Full resolution") > 0;
            if (!success) System.err.println("Failed: " + response);
            return success;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding:" + e);
        }
    }

    String sendRequest(String[] getVars, String[] postVars) throws IOException {
        return sendRequest(getVars, postVars, null, false);
    }

    String boundary = "---e69de29bb2d1d6434b8b29ae775ad8c2e48c5391";

    Map<String, String> cookies = new HashMap<String, String>();

    String response;

    String sendRequest(String[] getVars, String[] postVars, Object[] fileVars, boolean getSessionKey) throws IOException {
        String uri = wikiBaseURI;
        if (getVars != null) for (int i = 0; i + 1 < getVars.length; i += 2) uri += (i == 0 ? '?' : '&') + urlEncode(getVars[i]) + '=' + urlEncode(getVars[i + 1]);
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setUseCaches(false);
        if (!getSessionKey) {
            String cookie = "";
            for (String key : cookies.keySet()) cookie += (cookie.length() == 0 ? "" : "; ") + key + "=" + cookies.get(key);
            conn.setRequestProperty("Cookie", cookie);
        }
        if (fileVars != null) {
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.connect();
            PrintStream ps = new PrintStream(conn.getOutputStream());
            for (int i = 0; fileVars != null && i + 2 < fileVars.length; i += 3) {
                ps.print("--" + boundary + "\r\n");
                postFile(ps, conn, (String) fileVars[i], (String) fileVars[i + 1], (byte[]) fileVars[i + 2]);
            }
            for (int i = 0; postVars != null && i + 1 < postVars.length; i += 2) ps.print("--" + boundary + "\r\n" + "Content-Disposition: " + "form-data; name=\"" + postVars[i] + "\"\r\n\r\n" + postVars[i + 1] + "\r\n");
            ps.println("--" + boundary + "--");
            ps.close();
        } else if (postVars != null) {
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.connect();
            PrintStream ps = new PrintStream(conn.getOutputStream());
            for (int i = 0; postVars != null && i + 1 < postVars.length; i += 2) ps.print((i == 0 ? "" : "&") + urlEncode(postVars[i]) + "=" + urlEncode(postVars[i + 1]));
            ps.close();
        }
        int httpCode = conn.getResponseCode();
        if (httpCode != 200) throw new IOException("HTTP code: " + httpCode);
        if (getSessionKey) getCookies(conn.getHeaderFields().get("Set-Cookie"));
        InputStream in = conn.getInputStream();
        response = "";
        byte[] buffer = new byte[1 << 16];
        for (; ; ) {
            int len = in.read(buffer);
            if (len < 0) break;
            response += new String(buffer, 0, len);
        }
        in.close();
        return response;
    }

    void postFile(PrintStream ps, HttpURLConnection conn, String variableName, String fileName, byte[] contents) {
        String contentType = conn.guessContentTypeFromName(fileName);
        if (contentType == null) contentType = "application/octet-stream";
        ps.print("Content-Disposition: form-data; " + "name=\"" + variableName + "\"; " + "filename=\"" + fileName + "\"\r\n" + "Content-Type: " + contentType + "\r\n\r\n");
        ps.write(contents, 0, contents.length);
        ps.print("\r\n");
    }

    Pattern cookiePattern = Pattern.compile("^(wikidb_session)=([^;]*);.*$");

    void getCookies(List<String> headers) {
        if (headers == null) return;
        for (String s : headers) {
            Matcher matcher = cookiePattern.matcher(s);
            if (matcher.matches()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                cookies.put(key, value);
            }
        }
    }
}
