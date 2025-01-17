package taskblocks.bugzilla;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import taskblocks.utils.Utils;

/**
 * Instances of this class can submit new bugs to Bugzilla.
 * It uses it's bug_submit cgi script to submit new bug. The output html page is parsed 
 * to recognize the status of the operation.
 * 
 * @author j.neubauer
 */
public class BugzillaSubmitter {

    public static final String BUGID = "bug_id";

    /** Bug property name */
    public static final String KEYWORDS = "keywords";

    /** Bug property name */
    public static final String PRODUCT = "product";

    /** Bug property name */
    public static final String VERSION = "version";

    /** Bug property name */
    public static final String COMPONENT = "component";

    /** Bug property name */
    public static final String HARDWARE = "rep_platform";

    /** Bug property name */
    public static final String OS = "op_sys";

    /** Bug property name */
    public static final String PRIORITY = "priority";

    /** Bug property name */
    public static final String SEVERITY = "bug_severity";

    /**
	 * Bug property name Probably supported from bugzilla version 3.0, bug only
	 * NEW and ASSIGNED values
	 */
    public static final String STATUS = "bug_status";

    /** Bug property name */
    public static final String ASSIGNED_TO = "assigned_to";

    /** Bug property name */
    public static final String SUMMARY = "short_desc";

    /** Bug property name */
    public static final String DESCRIPTION = "comment";

    /** Bug property name (aka Original estimation) */
    public static final String ESTIMATED_TIME = "estimated_time";

    /** Bug property name (aka Hours left) */
    public static final String REMAINING_TIME = "remaining_time";

    /** Bug property name (aka Current estimation) */
    public static final String ACTUAL_TIME = "actual_time";

    /** Bug property name */
    public static final String BLOCKS = "blocked";

    /** Bug property name */
    public static final String DEPENDSON = "dependson";

    /** Must be enabled on bugzilla server */
    public static final String STATUS_WHITEBOARD = "status_whiteboard";

    /**
	 * Regular expression used to parse output from bugzilla and to find the submitted bug id.
	 * if not found, it is supposed that error occured.
	 */
    public String _successRegexpForSubmit = "Bug ([0-9]+) Submitted";

    public String _successRegexpForChange = "Changes submitted for";

    /**
	 * Regular expression used to find title of the error if submission doesn't
	 * success. By default, it is the title of the web page
	 */
    public String _errTitleRegexp = "<title>(.*)</title>";

    /**
	 * Regular expression used to find description of error if submission doesn't
	 * success. This one retrieves the main body of the page.
	 */
    public String _errDetailRegexp = "<div id=\"bugzilla-body\">(.*)</div>.*?<div id=\"footer\">";

    /** Regular expressions used to clean the detail error message. */
    public String[] _errDetailRemovalRegexps = new String[] { "(?s)<script.*?</script>", "(?s)<div id=\"docslinks\">.*?</div>" };

    /**
	 * encodes a form data from the given key-value pairs.
	 * 
	 * @param formData
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    private static String buildFormBody(Map<String, String> formData) throws UnsupportedEncodingException {
        StringBuilder body = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, String> e : formData.entrySet()) {
            if (count > 0) {
                body.append("&");
            }
            body.append(URLEncoder.encode(e.getKey(), "UTF-8"));
            body.append("=");
            body.append(URLEncoder.encode(e.getValue(), "UTF-8"));
            count++;
        }
        return body.toString();
    }

    /**
	 * Submits the given body with POST method to specified url
	 * 
	 * @param url must be http protocol
	 * @param body
	 * @return http reply data
	 * @throws IOException
	 */
    private String submit(URL url, String body) throws IOException {
        OutputStream out = null;
        InputStream in = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            if (body != null) {
                conn.setRequestMethod("POST");
                conn.setAllowUserInteraction(false);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=utf-8");
                conn.setRequestProperty("Content-length", Integer.toString(body.length()));
                out = conn.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print(body);
                pw.flush();
                pw.close();
            } else {
            }
            in = conn.getInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            CharArrayWriter result = new CharArrayWriter();
            char[] buf = new char[1024];
            int count = rdr.read(buf);
            while (count > 0) {
                result.write(buf, 0, count);
                count = rdr.read(buf);
            }
            return result.toString();
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void ensureDefault(Map<String, String> map, String key, String defaultValue) {
        if (!map.containsKey(key)) {
            map.put(key, defaultValue);
        }
    }

    /**
	 * Submits new bug to bugzilla server running at specified url.
	 * If bugzilla returns error page, and exception is thrown with error message
	 * extracted by parsing the result html page with regular expressions
	 * {@link #_errTitleRegexp}, {@link #_errDetailRegexp} and {@link #_errDetailRemovalRegexps}.
	 * Bug submission success is recognized by parsing output and finding bug id with
	 * regular expressiont {@link #_successRegexpForSubmit}.
	 * 
	 * 
	 * @param baseUrl
	 *          base url of bugzilla server
	 * @param user
	 *          user name for authentication
	 * @param password
	 *          password for authentication
	 * @param properties
	 *          properties of new bug. Use constants in this class as keys.
	 * @return submitted bug id.
	 *
	 * @throws IOException if connection error occures
	 * @throws Exception in other cases. If connection was successfull, error messages are
	 * extracted from the html page.
	 */
    public String submit(String baseUrl, String user, String password, Map<String, String> properties) throws Exception {
        ensureDefault(properties, STATUS, "NEW");
        ensureDefault(properties, SEVERITY, "normal");
        ensureDefault(properties, PRIORITY, "P2");
        ensureDefault(properties, "bug_file_loc", "http://");
        properties.put("form_name", "enter_bug");
        properties.put("Bugzilla_login", user);
        properties.put("Bugzilla_password", password);
        properties.put("GoAheadAndLogIn", "1");
        String formBody = buildFormBody(properties);
        String result = submit(new URL(baseUrl + "/post_bug.cgi"), formBody);
        Matcher m = Pattern.compile(_successRegexpForSubmit).matcher(result);
        if (m.find()) {
            String bugId = m.group(1);
            return bugId;
        } else {
            String errText = "";
            m = Pattern.compile(_errTitleRegexp).matcher(result);
            if (m.find()) {
                errText = m.group(1);
            }
            String errText2 = "";
            m = Pattern.compile(_errDetailRegexp, Pattern.DOTALL).matcher(result);
            if (m.find()) {
                errText2 = m.group(1);
            }
            if (errText2.length() > 0) {
                for (String removeRegexp : _errDetailRemovalRegexps) {
                    errText2 = errText2.replaceAll(removeRegexp, "");
                }
                errText2 = errText2.replaceAll("<[^>]*>", "");
                errText2 = errText2.replaceAll("\r?\n", " ");
                errText2 = errText2.replaceAll(" +", " ");
            }
            throw new Exception(errText + ": " + errText2);
        }
    }

    public void change(String baseUrl, String user, String password, String bugId, Map<String, String> properties) throws Exception {
        properties.put("id", bugId);
        properties.put("Bugzilla_login", user);
        properties.put("Bugzilla_password", password);
        properties.put("GoAheadAndLogIn", "1");
        String formBody = buildFormBody(properties);
        String result = submit(new URL(baseUrl + "/process_bug.cgi"), formBody);
        Matcher m = Pattern.compile(_successRegexpForChange).matcher(result);
        if (m.find()) {
            return;
        } else {
            String errText = "";
            m = Pattern.compile(_errTitleRegexp).matcher(result);
            if (m.find()) {
                errText = m.group(1);
            }
            String errText2 = "";
            m = Pattern.compile(_errDetailRegexp, Pattern.DOTALL).matcher(result);
            if (m.find()) {
                errText2 = m.group(1);
            }
            if (errText2.length() > 0) {
                for (String removeRegexp : _errDetailRemovalRegexps) {
                    errText2 = errText2.replaceAll(removeRegexp, "");
                }
                errText2 = errText2.replaceAll("<[^>]*>", "");
                errText2 = errText2.replaceAll("\r?\n", " ");
                errText2 = errText2.replaceAll(" +", " ");
            }
            throw new Exception(errText + ": " + errText2);
        }
    }

    public Map<String, Map<String, String>> query(String baseUrl, String user, String password, String[] bugs) throws MalformedURLException, IOException, SAXException, ParserConfigurationException {
        Map<String, String> formData = new HashMap<String, String>();
        formData.put("ctype", "xml");
        formData.put("Bugzilla_login", user);
        formData.put("Bugzilla_password", password);
        formData.put("excludefield", "attachmentdata");
        String body = buildFormBody(formData);
        for (String bugId : bugs) {
            body += "&";
            body += URLEncoder.encode("id", "UTF-8");
            body += "=";
            body += URLEncoder.encode(bugId, "UTF-8");
        }
        String result = submit(new URL(baseUrl + "/show_record.cgi"), body);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(result.getBytes("UTF-8")));
        Element rootE = doc.getDocumentElement();
        if (!rootE.getNodeName().equals("bugzilla")) {
            throw new IOException("Wrong xml answer, doesn't looks like bugzilla");
        }
        Map<String, Map<String, String>> resultData = new HashMap<String, Map<String, String>>();
        for (Element bugE : Utils.getChilds(rootE, "bug")) {
            Map<String, String> bugData = new HashMap<String, String>();
            String bugId = fillBugData(bugE, bugData);
            resultData.put(bugId, bugData);
        }
        return resultData;
    }

    private String fillBugData(Element bugE, Map<String, String> bugData) {
        String id = Utils.getFirstElemText(bugE, BUGID);
        bugData.put(STATUS_WHITEBOARD, Utils.getFirstElemText(bugE, STATUS_WHITEBOARD));
        bugData.put(ESTIMATED_TIME, Utils.getFirstElemText(bugE, ESTIMATED_TIME));
        bugData.put(ACTUAL_TIME, Utils.getFirstElemText(bugE, ACTUAL_TIME));
        bugData.put(REMAINING_TIME, Utils.getFirstElemText(bugE, REMAINING_TIME));
        bugData.put(DEPENDSON, Utils.getElemTexts(bugE, DEPENDSON));
        bugData.put(BLOCKS, Utils.getElemTexts(bugE, BLOCKS));
        return id;
    }
}
