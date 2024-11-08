package org.atlantal.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.PrintStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:masurel@mably.com">Francois MASUREL</a>
 */
public final class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    static {
        LOGGER.setLevel(Level.INFO);
    }

    private static final String EMAIL_PATTERN = "^[0-9a-z]([-_.]?[0-9a-z])*@[0-9a-z]([-.]?[0-9a-z])*\\.[a-z]{2,3}";

    private static String stripHTMLTagsRegExp = "\\<.*?\\>";

    /**
     * Constructor
     */
    private Utils() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param classname Java class name
     * @return Java object
     * @throws IllegalAccessException a generic exception
     * @throws InstantiationException a generic exception
     * @throws ClassNotFoundException a generic exception
     */
    public static Object dynamicLoad(String classname) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        return Class.forName(classname).newInstance();
    }

    /**
     * @param filename File name
     * @return String array
     * @throws IOException a generic exception
     */
    public static String[] getTabFromFile2(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Cr�ation du fichier impossible");
        }
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        List vec = new ArrayList();
        String line = raf.readLine();
        while (line != null) {
            vec.add(line);
            line = raf.readLine();
        }
        raf.close();
        return (String[]) vec.toArray(new String[vec.size()]);
    }

    /**
     * @param filename File name
     * @return Vector
     * @throws IOException a generic exception
     */
    public static ArrayList getTabFromFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Cr�ation du fichier impossible");
        }
        FileReader fr = new FileReader(file);
        BufferedReader in = new BufferedReader(fr);
        ArrayList list = null;
        try {
            String line;
            list = new ArrayList();
            for (; ; ) {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
            list = null;
        } finally {
            in.close();
        }
        return list;
    }

    /**
     * @param fileName File name
     * @param list Vector
     * @return boolean
     * @throws IOException a generic exception
     */
    public static boolean getFileFromTab(String fileName, ArrayList list) throws IOException {
        File file = new File(fileName);
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Cr�ation du fichier impossible");
        }
        FileOutputStream out = new FileOutputStream(file);
        PrintStream p = new PrintStream(out);
        for (int i = 0; i < list.size(); i++) {
            p.println(list.get(i));
        }
        p.close();
        return true;
    }

    /**
     * @param email Email
     * @return boolean
     */
    public static boolean checkEmail(String email) {
        return email.matches(EMAIL_PATTERN);
    }

    /**
     * @param value value
     * @return html
     */
    public static String escapeHTML(String value) {
        String html;
        if (value == null) {
            html = "";
        } else {
            StringBuilder strval = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                switch(ch) {
                    case '"':
                        strval.append("&quot;");
                        break;
                    case '&':
                        strval.append("&amp;");
                        break;
                    case '<':
                        strval.append("&lt;");
                        break;
                    case '>':
                        strval.append("&gt;");
                        break;
                    default:
                        if (ch > 126) {
                            strval.append("&#" + String.valueOf(ch) + ";");
                        } else {
                            strval.append(ch);
                        }
                        break;
                }
            }
            html = strval.toString();
        }
        return html;
    }

    /**
     * @param html html
     * @return string
     */
    public static String stripHTMLTags(String html) {
        return html.replaceAll(stripHTMLTagsRegExp, "");
    }

    /**
     * @param html html
     * @return string
     */
    public static String convertHTMLToText(String html) {
        String text = html.replaceAll(stripHTMLTagsRegExp, "");
        text = StringEscapeUtils.unescapeHtml(text);
        return text;
    }

    /**
     * Analyseur de param�tre
     * @param params Parameters
     * @return Map array
     */
    public static Map[] syntaxExploder(String params) {
        return syntaxExploder(params, "=");
    }

    /**
     * Analyseur de param�tre
     * @param pparams Parameters
     * @param cplsep Separator
     * @return Map array
     */
    public static Map[] syntaxExploder(String pparams, String cplsep) {
        char separator = ',';
        char leftDelim = '{';
        char rightDelim = '}';
        String params = pparams;
        if (params.charAt(0) == leftDelim) {
            params = params.substring(1, params.length() - 1);
        }
        if (params.charAt(params.length() - 1) == rightDelim) {
            params = params.substring(0, params.length() - 2);
        }
        String sep = "\\" + rightDelim + separator + "\\" + leftDelim;
        String[] fields = params.split(sep);
        Map[] results = new AtlantalMap[fields.length];
        for (int i = 0; i < fields.length; i++) {
            Map fieldsMap = new AtlantalMap();
            String fieldstr = fields[i].trim();
            String[] couples = fieldstr.split("\\|");
            for (int j = 0; j < couples.length; j++) {
                String couplestr = couples[j];
                String[] couple = couplestr.split(cplsep);
                String name = couple[0];
                String value = couple[1];
                fieldsMap.put(name, value);
            }
            results[i] = fieldsMap;
        }
        return results;
    }

    /**
     * @param separator Separator
     * @param list List
     * @return String
     */
    public static String implode(String separator, List list) {
        String result = null;
        if (list.size() > 0) {
            StringBuilder temp = new StringBuilder();
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object element = it.next();
                if (temp.length() > 0) {
                    temp.append(separator);
                }
                temp.append(element.toString());
            }
            result = temp.toString();
        }
        return result;
    }

    /**
     * @param separator Separator
     * @param values int values
     * @return String
     */
    public static String implodeIntArray(String separator, int[] values) {
        String result = null;
        if (values.length > 0) {
            StringBuilder temp = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (temp.length() > 0) {
                    temp.append(separator);
                }
                temp.append(values[i]);
            }
            result = temp.toString();
        }
        return result;
    }

    /**
     * @param search Search
     * @param replace Replace
     * @param subject Subject
     * @return String
     */
    public static String strReplace(String search, String replace, String subject) {
        return subject.replaceAll(search, replace);
    }

    /**
     * @param number number
     * @param decimals decimals
     * @param decPoint decPoint
     * @param thousandsSep thousandsSep
     * @return String
     */
    public static String numberFormat(Double number, int decimals, char decPoint, char thousandsSep) {
        DecimalFormatSymbols nfs = new DecimalFormatSymbols();
        nfs.setDecimalSeparator(decPoint);
        nfs.setGroupingSeparator(thousandsSep);
        DecimalFormat nf = new DecimalFormat();
        nf.setDecimalFormatSymbols(nfs);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        return nf.format(number);
    }

    /**
     * @param obj1 obj1
     * @param obj2 obj2
     * @return boolean
     */
    public static boolean compareObjects(Object obj1, Object obj2) {
        boolean compare;
        if (obj1 == null) {
            compare = (obj2 == null);
        } else {
            if (obj2 == null) {
                compare = false;
            } else {
                compare = obj1.equals(obj2);
            }
        }
        return compare;
    }

    /**
     * @param paramsStr paramsStr
     * @return Map
     */
    public static Map stringParamsToMap(String paramsStr) {
        return stringParamsToMap(paramsStr, ";");
    }

    /**
     * @param paramsStr paramsStr
     * @param sep sep
     * @return Map
     */
    public static Map stringParamsToMap(String paramsStr, String sep) {
        Map params = null;
        Map tempParams = new AtlantalMap();
        if (stringParamsToMap(paramsStr, tempParams, sep)) {
            params = tempParams;
        }
        return params;
    }

    /**
     * @param paramsStr paramsStr
     * @param params params
     * @param sep sep
     * @return Map
     */
    public static boolean stringParamsToMap(String paramsStr, Map params, String sep) {
        boolean ok = false;
        if ((paramsStr != null) && (paramsStr.trim().length() > 0)) {
            String[] paramsTab = paramsStr.split(sep);
            for (int i = 0; i < (paramsTab.length - 1); i += 2) {
                params.put(paramsTab[i], paramsTab[i + 1]);
            }
            ok = true;
        }
        return ok;
    }

    /**
     * @param paramsMap paramsMap
     * @return String
     */
    public static String mapParamsToString(Map paramsMap) {
        return mapParamsToString(paramsMap, ";");
    }

    /**
     * {@inheritDoc}
     */
    public static Map string64ParamsToMap(String params) {
        Map subvalues;
        try {
            byte[] b = Base64.decodeBase64(params.getBytes());
            ByteArrayInputStream bais = new ByteArrayInputStream(b);
            ObjectInputStream ois = new ObjectInputStream(bais);
            subvalues = (Map) ois.readObject();
        } catch (IOException e) {
            subvalues = new AtlantalMap();
        } catch (ClassNotFoundException e) {
            subvalues = new AtlantalMap();
        }
        return subvalues;
    }

    /**
     * @param paramsMap paramsMap
     * @param sep sep
     * @return String
     */
    public static String mapParamsToString(Map paramsMap, String sep) {
        StringBuilder params = new StringBuilder();
        mapParamsToStringBuilder(paramsMap, sep, params);
        return params.toString();
    }

    /**
     * @param paramsMap paramsMap
     * @param sep sep
     * @param params params
     */
    public static void mapParamsToStringBuilder(Map paramsMap, String sep, StringBuilder params) {
        Set entrySet = paramsMap.entrySet();
        Iterator it = entrySet.iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            if (i++ > 0) {
                params.append(sep);
            }
            params.append(entry.getKey()).append(sep);
            params.append(entry.getValue());
        }
    }

    /**
     * @param in Source
     * @param out Target
     * @throws IOException a generic exception
     */
    public static void copyFileNIO(File in, File out) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(in);
            fos = new FileOutputStream(out);
            FileChannel sourceChannel = fis.getChannel();
            FileChannel targetChannel = fos.getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
            sourceChannel.close();
            targetChannel.close();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    /**
     * @param parg Original string
     * @return Corrected string
     */
    public static String toUpperCaseRemoveAccents(String parg) {
        StringBuilder sbuff = new StringBuilder();
        String arg = parg.toLowerCase(Locale.getDefault());
        try {
            for (int i = 0; i < arg.length(); i++) {
                char c = arg.charAt(i);
                switch(c) {
                    case '�':
                    case '�':
                    case '�':
                    case '�':
                        sbuff.append('e');
                        break;
                    case '�':
                    case '�':
                        sbuff.append('a');
                        break;
                    case '�':
                    case '�':
                        sbuff.append('i');
                        break;
                    case '�':
                        sbuff.append('o');
                        break;
                    case '�':
                    case '�':
                    case '�':
                        sbuff.append('u');
                        break;
                    case '�':
                        sbuff.append('c');
                        break;
                    default:
                        sbuff.append(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return sbuff.toString().toUpperCase(Locale.getDefault());
    }

    /**
     * @param html html
     * @return script
     */
    public static String htmlToJavascript(String html) {
        StringBuilder script = new StringBuilder();
        script.append("document.write(\"");
        script.append(html.replaceAll("\\\"", "\\\\\\\""));
        script.append("\");\n");
        return script.toString();
    }

    /**
     * @param str str
     * @param sep sep
     * @param src src
     * @param tgt tgt
     * @return new str
     */
    public static String replaceCharBetweenSeparators(String str, char sep, char src, char tgt) {
        String newstr = null;
        if (str != null) {
            newstr = "";
            boolean insep = false;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == sep) {
                    insep = !insep;
                } else {
                    if (c == src) {
                        if (insep) {
                            c = tgt;
                        }
                    }
                    newstr = newstr + c;
                }
            }
        }
        return newstr;
    }

    /**
     * {@inheritDoc}
     */
    public static String sqlWhereLike(String fieldname, String pquery) {
        boolean or = false;
        String op;
        StringBuilder where = new StringBuilder();
        String query = replaceCharBetweenSeparators(pquery, '"', ' ', '~');
        String[] values = query.split(" ");
        for (int i = 0; i < values.length; i++) {
            String val = values[i].toUpperCase();
            val = val.replace('~', ' ');
            if ("OR".equals(val) || "OU".equals(val)) {
                or = true;
                continue;
            }
            if ("AND".equals(val) || "ET".equals(val)) {
                continue;
            }
            switch(val.charAt(0)) {
                case '+':
                    op = "";
                    val = val.substring(1);
                    break;
                case '-':
                    op = " NOT";
                    val = val.substring(1);
                    break;
                default:
                    op = "";
            }
            if (val.length() < 3) {
                continue;
            }
            if (i > 0) {
                if (or) {
                    where.append(" OR ");
                    or = false;
                } else {
                    where.append(" AND ");
                }
            }
            String sansAccents = Utils.toUpperCaseRemoveAccents(val);
            where.append(fieldname).append(op);
            where.append(" LIKE '%").append(sansAccents).append("%'");
        }
        String whereStr;
        if (where.length() > 0) {
            whereStr = "(" + where.toString() + ")";
        } else {
            whereStr = where.toString();
        }
        return whereStr;
    }

    /**
     * Tries to exec the command, waits for it to finsih, logs errors if exit
     * status is nonzero, and returns true if exit status is 0 (success).
     *
     * @param command Description of the Parameter
     * @return Description of the Return Value
     */
    public static boolean exec(String[] command) {
        boolean ok;
        try {
            Process proc = Runtime.getRuntime().exec(command);
            int exitStatus;
            while (true) {
                try {
                    exitStatus = proc.waitFor();
                    break;
                } catch (java.lang.InterruptedException e) {
                    LOGGER.error("Interrupted: Ignoring and waiting");
                }
            }
            if (exitStatus != 0) {
                LOGGER.error("Error executing command: " + exitStatus);
            }
            ok = (exitStatus == 0);
        } catch (IOException e) {
            LOGGER.error("IOException while trying to execute");
            ok = false;
        }
        return ok;
    }

    /**
     * @param httpclient httpclient
     */
    public static void initHttpClientProxy(HttpClient httpclient) {
        Properties props = System.getProperties();
        String httpProxyHost = (String) props.get("http.proxyHost");
        if (httpProxyHost != null) {
            int httpProxyPort;
            try {
                String sHttpProxyPort = (String) props.get("http.proxyPort");
                httpProxyPort = Integer.parseInt(sHttpProxyPort);
            } catch (NumberFormatException e) {
                httpProxyPort = 80;
            }
            httpclient.getHostConfiguration().setProxyHost(new ProxyHost(httpProxyHost, httpProxyPort));
            String httpProxyLogin = (String) props.get("http.proxyLogin");
            String httpProxyPassword = (String) props.get("http.proxyPassword");
            if ((httpProxyLogin != null) && (httpProxyPassword != null)) {
                httpclient.getState().setProxyCredentials(new AuthScope(httpProxyHost, httpProxyPort), new UsernamePasswordCredentials(httpProxyLogin, httpProxyPassword));
            }
        }
    }
}
