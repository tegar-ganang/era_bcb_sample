package com.exedosoft.plat.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.exedosoft.plat.bo.BOInstance;
import com.exedosoft.plat.bo.DOBOProperty;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 主要是对String相关操作的类，还提供其它一些常见的帮助方法，是平台中使用最多的帮助类。
 */
public class StringUtil {

    private static Log log = LogFactory.getLog(StringUtil.class);

    private static final int fillchar = '=';

    private static final String cvt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789+/";

    /**
	 * Encodes a String as a base64 String.
	 * 
	 * @param data
	 *            a String to encode.
	 * @return a base64 encoded String.
	 */
    public static StringUtil newInstance() {
        return new StringUtil();
    }

    public static String encodeBase64(String data) {
        return com.oreilly.servlet.Base64Encoder.encode(data.getBytes());
    }

    /**
	 * Decodes a base64 String.
	 * 
	 * @param data
	 *            a base64 encoded String to decode.
	 * @return the decoded String.
	 */
    public static String decodeBase64(String data) {
        return com.oreilly.servlet.Base64Decoder.decode(data);
    }

    public static String encodeFromISO2UTF(String str) {
        if (null == str) {
            return str;
        }
        try {
            return new String(str.getBytes("iso-8859-1"), "utf-8");
        } catch (Exception ex) {
            return str;
        }
    }

    public static String encodeFromUTF2ISO(String str) {
        if (null == str) {
            return str;
        }
        try {
            return new String(str.getBytes("utf-8"), "iso-8859-1");
        } catch (Exception ex) {
            return str;
        }
    }

    public static String encodeFromISO2GBK(String str) {
        if (null == str) {
            return str;
        }
        try {
            return new String(str.getBytes("iso-8859-1"), "gbk");
        } catch (Exception ex) {
            return str;
        }
    }

    public static String encodeFromGBK2ISO(String str) {
        if (null == str) {
            return str;
        }
        try {
            return new String(str.getBytes("gbk"), "iso-8859-1");
        } catch (Exception ex) {
            return str;
        }
    }

    public static String encodeFromUTF2GBK(String str) {
        if (null == str) {
            return str;
        }
        try {
            return new String(str.getBytes("utf-8"), "gbk");
        } catch (Exception ex) {
            return str;
        }
    }

    public static String encodeFromGBK2UTF(String str) {
        if (null == str) {
            return str;
        }
        try {
            return new String(str.getBytes("gbk"), "utf-8");
        } catch (Exception ex) {
            return str;
        }
    }

    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) hs = hs + "0" + stmp; else hs = hs + stmp;
            if (n < b.length - 1) hs = hs;
        }
        return hs.toUpperCase();
    }

    public static String SHA(String input) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA");
            return byte2hex(sha.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * 处理一个表达式，这个表达式可能在 DOFormModel 或者 NodeInstance
	 * 中用到。DOFormModel里面主要是显示的字段是经过计算而来. NodeInstance主要是对表达式的判断
	 * 
	 * @param expression
	 *            需要处理的表达式
	 * @param bi
	 *            包含参数数据
	 * @param boProperties
	 *            需要替换的参数 采用boProperties 不是从bi.getBO获取，可以提供更灵活的按需替换。
	 * @param placeHolder
	 *            占位符
	 * @return
	 */
    public static String getCalException(String expression, BOInstance bi, Collection boProperties, String placeHolder) {
        if (placeHolder == null) {
            placeHolder = "0";
        }
        if (expression == null) {
            return null;
        }
        if ((expression.indexOf("doForm") != -1) || (expression.indexOf("doInstance") != -1)) {
            return expression;
        }
        if (boProperties == null || boProperties.size() == 0 || bi == null) {
            return expression;
        } else {
            List<StringLengthSort> list = new ArrayList<StringLengthSort>();
            for (Iterator it = boProperties.iterator(); it.hasNext(); ) {
                DOBOProperty property = (DOBOProperty) it.next();
                String varName = property.getColName();
                list.add(new StringLengthSort(varName));
            }
            Collections.sort(list);
            for (Iterator<StringLengthSort> it = list.iterator(); it.hasNext(); ) {
                StringLengthSort ssl = it.next();
                String value = bi.getValue(ssl.getString());
                if (value == null) {
                    value = placeHolder;
                }
                expression = expression.replaceAll(ssl.getString(), value);
            }
        }
        return expression;
    }

    /**
	 * 根据总的记录数和每页显示的行数判断共有多少页
	 * 
	 * @param totalSize
	 * @param rowSize
	 *            每页显示的行数
	 * @return
	 */
    public static int getPageSize(int totalSize, int rowSize) {
        if (rowSize == 0) {
            return 0;
        }
        if (totalSize % rowSize == 0) {
            return totalSize / rowSize;
        } else {
            return totalSize / rowSize + 1;
        }
    }

    public static String getDotName(String boName) {
        String dotName = boName.replaceAll("_", ".").trim().toLowerCase();
        return dotName;
    }

    public static String get_Name(String boName) {
        if (boName.indexOf(".") == -1) {
            return boName.trim().toLowerCase();
        }
        String _Name = boName.replace(".", "_").trim().toLowerCase();
        return _Name;
    }

    /**
	 * 划分字符串
	 * 
	 * @param mapFormatStr
	 * @param isAddLastZero
	 *            TODO
	 * @return
	 */
    public static List getMapList(String mapFormatStr, boolean isAddLastZero) {
        List list = new ArrayList();
        if (mapFormatStr == null) {
            return list;
        }
        mapFormatStr = mapFormatStr.substring(mapFormatStr.indexOf(",", 2) + 1, mapFormatStr.length());
        String[] strs = mapFormatStr.split(",");
        int count = 1;
        HashMap oneTr = new HashMap();
        boolean lastConti = false;
        boolean isMain = true;
        boolean isHasChild = false;
        for (int i = 0; i < strs.length; i++) {
            String oneStr = strs[i];
            if ("0".equals(oneStr)) {
                lastConti = true;
                continue;
            }
            if (lastConti && oneStr.length() <= 2) {
                oneTr = new HashMap();
                oneTr.put("id", String.valueOf(count));
                if (isMain) {
                    oneTr.put("jd", "0");
                    isMain = false;
                } else {
                    isHasChild = true;
                    oneTr.put("jd", "-1");
                }
                oneTr.put("wd", "0");
                list.add(oneTr);
                count++;
                continue;
            }
            lastConti = false;
            if (i % 2 == 0) {
                oneTr = new HashMap();
                oneTr.put("id", String.valueOf(count));
                oneTr.put("jd", oneStr);
                count++;
            } else {
                oneTr.put("wd", oneStr);
                list.add(oneTr);
            }
        }
        if (isAddLastZero && isHasChild) {
            oneTr = new HashMap();
            oneTr.put("id", String.valueOf(count));
            oneTr.put("jd", "-1");
            oneTr.put("wd", "0");
            list.add(oneTr);
        }
        return list;
    }

    /**
	 * 划分字符串
	 * 
	 * @param mapFormatStr
	 * @param isAddZero
	 *            TODO
	 * @return
	 */
    public static List getMapListCK(String mapFormatStr, boolean isAddZero) {
        List list = new ArrayList();
        if (mapFormatStr == null) {
            return list;
        }
        mapFormatStr = mapFormatStr.substring(mapFormatStr.indexOf(",", 1) + 1, mapFormatStr.length());
        String[] strs = mapFormatStr.split(",");
        int secondStr = Integer.parseInt(strs[0]);
        int count = 1;
        HashMap oneTr = new HashMap();
        int computerStr = secondStr * 3 + 5;
        System.out.println("拐点数为：" + secondStr);
        if (computerStr >= strs.length) {
            oneTr = new HashMap();
            oneTr.put("cid", String.valueOf(count));
            oneTr.put("gdh", secondStr);
            oneTr.put("xzg", strs[computerStr - 4]);
            oneTr.put("yzg", strs[computerStr - 3]);
            oneTr.put("ktbs", strs[computerStr - 2]);
            oneTr.put("xz", strs[computerStr - 1]);
            list.add(oneTr);
        }
        for (; computerStr < strs.length; ) {
            oneTr = new HashMap();
            oneTr.put("cid", String.valueOf(count));
            oneTr.put("gdh", secondStr);
            oneTr.put("xzg", strs[computerStr - 4]);
            oneTr.put("yzg", strs[computerStr - 3]);
            oneTr.put("ktbs", strs[computerStr - 2]);
            oneTr.put("xz", strs[computerStr - 1]);
            list.add(oneTr);
            count++;
            System.out.println("拐点数为：" + strs[computerStr]);
            secondStr = Integer.parseInt(strs[computerStr]);
            computerStr += secondStr * 3 + 5;
            System.out.println("转化" + computerStr);
            System.out.println("转化" + strs.length);
            if (computerStr == strs.length) {
                oneTr = new HashMap();
                oneTr.put("cid", String.valueOf(count));
                oneTr.put("gdh", secondStr);
                oneTr.put("xzg", strs[computerStr - 4]);
                oneTr.put("yzg", strs[computerStr - 3]);
                oneTr.put("ktbs", strs[computerStr - 2]);
                oneTr.put("xz", strs[computerStr - 1]);
                list.add(oneTr);
                count++;
            }
        }
        return list;
    }

    /**
	 * 把字符串转化为列表形式
	 * 
	 * @param src
	 * @return
	 */
    public static List getStaticList(String src) {
        List list = new ArrayList();
        if (src == null) {
            return list;
        }
        if (src.indexOf("@") != -1) {
            src = src.substring(0, src.indexOf("@"));
        }
        String[] onelist = src.split(";");
        for (int i = 0; i < onelist.length; i++) {
            String one = (String) onelist[i];
            if (one.indexOf("),") != -1) {
                String[] halfs = new String[2];
                halfs[0] = one.substring(1, one.indexOf("),"));
                halfs[1] = one.substring(one.indexOf("),") + 2);
                list.add(halfs);
            } else {
                String[] halfs = one.split(",");
                list.add(halfs);
            }
        }
        return list;
    }

    public static String getValueByKey(String src, String aKey) {
        if (src == null || aKey == null) {
            return null;
        }
        List list = getStaticList(src);
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            String[] halfs = (String[]) it.next();
            if ((aKey.equals(halfs[0]))) {
                return halfs[1];
            }
        }
        return "";
    }

    /**
	 * 替换< > " ' 等特殊字符
	 * 
	 * @param args
	 */
    public static String dealSpecChar(String src) {
        src = src.replaceAll("\\\\'", "&apos;");
        src = src.replaceAll("\"", "&quot;");
        src = src.replaceAll("<", "&lt;");
        src = src.replaceAll(">", "&gt;");
        return src;
    }

    /**
	 * 添加Tab空格
	 * 
	 * @param buffer
	 */
    public static void tabSpace(StringBuffer buffer, int times) {
        for (int i = 1; i <= times; i++) {
            buffer.append("     ");
        }
    }

    public static String MD5(String plainTxt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainTxt.getBytes());
            byte[] b = md.digest();
            int i = 0;
            StringBuffer buffer = new StringBuffer();
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buffer.append("0");
                }
                buffer.append(Integer.toHexString(i));
            }
            return buffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    /**
	 * 防止跨站脚本攻击xss
	 * 
	 * @param value
	 * @return
	 */
    public static String filter(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        StringBuffer result = null;
        String filtered = null;
        for (int i = 0; i < value.length(); i++) {
            filtered = null;
            switch(value.charAt(i)) {
                case '<':
                    filtered = "&lt;";
                    break;
                case '>':
                    filtered = "&gt;";
                    break;
                case '&':
                    filtered = "&amp;";
                    break;
                case '\"':
                    filtered = "&quot;";
                    break;
                case '\'':
                    filtered = "&#39;";
                    break;
            }
            if (result == null) {
                if (filtered != null) {
                    result = new StringBuffer(value.length() + 50);
                    if (i > 0) {
                        result.append(value.substring(0, i));
                    }
                    result.append(filtered);
                }
            } else {
                if (filtered == null) {
                    result.append(value.charAt(i));
                } else {
                    result.append(filtered);
                }
            }
        }
        return result == null ? value : result.toString();
    }

    public static String unFilterXss(String txt) {
        if (txt == null || "".equals(txt.trim())) {
            return "";
        }
        txt = txt.replaceAll("&amp;", "&");
        txt = txt.replaceAll("&lt;", "<");
        txt = txt.replaceAll("&gt;", ">");
        txt = txt.replaceAll("&quot;", "\"");
        txt = txt.replaceAll("&#39;", "'");
        return txt;
    }

    /**
	 * 获取附件文件，相对路径（相对与系统设定的下载上传路径）
	 * 
	 * @param theFileName
	 * @return
	 */
    public static StringBuffer getAttachMentFile(String theFileName) {
        String aPath = DOGlobals.UPLOAD_TEMP + theFileName;
        log.info("The DownLoad File Path::" + aPath);
        return getAttachMentFileAbstract(aPath, theFileName);
    }

    /**
	 * * 获取附件文件，绝对路径
	 * 
	 * @param theFileName
	 * @return
	 */
    public static StringBuffer getAttachMentFileAbstract(String theFilePath, String theFileName) {
        StringBuffer fileUrl = new StringBuffer();
        try {
            fileUrl.append(DOGlobals.PRE_FULL_FOLDER).append("file/downloadfile_hd.jsp?filePath=").append(URLEncoder.encode(Escape.escape(theFilePath), "utf-8")).append("&fileName=").append(URLEncoder.encode(Escape.escape(theFileName.substring(theFileName.indexOf("/") + 1)), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return fileUrl;
    }

    public static String getCurrentDayStr() {
        return String.format("%1$tY-%1$tm-%1$td", Calendar.getInstance());
    }

    public static boolean isLong(String str) {
        if ("0".equals(str.trim())) {
            return true;
        }
        Pattern pattern = Pattern.compile("^[^0]\\d*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isSupportDate(String str) {
        if (str == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2})|(\\d{4}[.]{1}\\d{1,2}[.]{1}\\d{1,2})|(\\d{4}[/]{1}\\d{1,2}[/]{1}\\d{1,2})");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isFloat(String str) {
        if (isLong(str)) {
            return true;
        }
        Pattern pattern = Pattern.compile("\\d*\\.{1}\\d+");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isFloat(String str, int precision) {
        String regStr = "\\d*\\.{1}\\d{" + precision + "}";
        Pattern pattern = Pattern.compile(regStr);
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isNumber(String str) {
        if (isLong(str)) {
            return true;
        }
        Pattern pattern = Pattern.compile("(-)?(\\d*)\\.{0,1}(\\d*)");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isEMail(String str) {
        Pattern pattern = Pattern.compile("(\\S)+[@]{1}(\\S)+[.]{1}(\\w)+");
        Matcher isEMail = pattern.matcher(str);
        if (!isEMail.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isHTTPNET(String str) {
        Pattern pattern = Pattern.compile("^[http://]{1}\\S+");
        Matcher isEMail = pattern.matcher(str);
        if (!isEMail.matches()) {
            return false;
        }
        return true;
    }

    /**
	 * 检查正则表达式
	 * 
	 * @param str
	 * @param regExt
	 * @return
	 */
    public static boolean isValid(String str, String regExt) {
        log.info("目标字符串::::" + str);
        log.info("正则表达式:::" + regExt);
        str = str.trim();
        regExt = regExt.trim();
        Pattern pattern = Pattern.compile(regExt);
        Matcher doMatcher = pattern.matcher(str);
        if (doMatcher.matches()) {
            return true;
        }
        log.info("目标字符串没有满足正则表达");
        return false;
    }

    /**
	 * db2 的排序强制不能采用table.col这种形式 只能采用col的形式，如果col重复，请as othername
	 */
    public static String getOrderByCol(String str) {
        str = str.replaceAll(",(\\s)*", ",");
        String ret = "";
        ret = getColHelper(str, "order(\\s)+by(\\s)");
        if (ret != null && !ret.equals("")) {
            return ret;
        }
        if (str.indexOf(" group ") != -1) {
            return getColHelper(str, "group(\\s)+by(\\s)");
        }
        return "";
    }

    private static String getColHelper(String str, String p) {
        String ret = "";
        Pattern pattern = Pattern.compile(p + "+(.)+(\\s)*");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            ret = matcher.group().replaceAll(p + "+", "");
        }
        return ret;
    }

    public static void test_replace() {
        Pattern p = null;
        Matcher m = null;
        StringBuffer sb = null;
        int i = 0;
        p = Pattern.compile("\\{\\w*\\}");
        m = p.matcher("one {df} two {xxx}s in the yard");
        sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "dog" + i);
            i++;
        }
        m.appendTail(sb);
        System.out.println(sb.toString());
    }

    /**
	 * 
	 * @param aWholeStr
	 *            需要处理的SQL 语句
	 * @param anOldCol
	 *            旧的列明
	 * @param aNewCol
	 *            新的列名
	 * @return
	 */
    public static String replaceSqlCol(String aWholeStr, String anOldCol, String aNewCol) {
        if (aWholeStr == null || anOldCol == null || aNewCol == null) {
            return aWholeStr;
        }
        Pattern p = null;
        Matcher m = null;
        StringBuffer sb = new StringBuffer();
        p = Pattern.compile("[(]{1}(\\s)*" + anOldCol + "(\\s)*[,]{1}");
        m = p.matcher(aWholeStr);
        if (m.find()) {
            m.appendReplacement(sb, "(" + aNewCol + ",");
        }
        m.appendTail(sb);
        aWholeStr = sb.toString();
        sb = new StringBuffer();
        p = Pattern.compile("[,]{1}(\\s)*" + anOldCol + "(\\s)*[,]{1}");
        Matcher m2 = p.matcher(aWholeStr);
        if (m2.find()) {
            m2.appendReplacement(sb, "," + aNewCol + ",");
        }
        m2.appendTail(sb);
        aWholeStr = sb.toString();
        sb = new StringBuffer();
        p = Pattern.compile("(,){1}(\\s)*" + anOldCol + "(\\s)*[\\)]{1}");
        m = p.matcher(aWholeStr);
        if (m.find()) {
            m.appendReplacement(sb, "," + aNewCol + ")");
        }
        m.appendTail(sb);
        aWholeStr = sb.toString();
        sb = new StringBuffer();
        p = Pattern.compile("(\\s)+" + anOldCol + "(\\s)*[=]{1}");
        m = p.matcher(aWholeStr);
        if (m.find()) {
            m.appendReplacement(sb, " " + aNewCol + "=");
        }
        m.appendTail(sb);
        aWholeStr = sb.toString();
        sb = new StringBuffer();
        p = Pattern.compile("[,]{1}(\\s)*" + anOldCol + "(\\s)*[=]{1}");
        m = p.matcher(aWholeStr);
        if (m.find()) {
            m.appendReplacement(sb, "," + aNewCol + "=");
        }
        m.appendTail(sb);
        aWholeStr = sb.toString();
        return aWholeStr;
    }

    /**
	 * 把小数点形式转换为经纬度坐标
	 * 
	 * @param beforStr
	 * @return
	 */
    public static String transDot(String beforStr) {
        if (beforStr == null) {
            return "";
        }
        BigDecimal a = BigDecimal.valueOf(Double.parseDouble(beforStr));
        a = a.add(BigDecimal.valueOf(0.000001));
        beforStr = a.toString();
        if (beforStr.indexOf(".") != -1) {
            String beforDot = beforStr.substring(0, beforStr.indexOf("."));
            String afterDot = beforStr.substring(beforStr.indexOf(".") + 1, beforStr.length());
            double dAfterDot = Double.parseDouble("0." + afterDot);
            String ffStr = String.valueOf(dAfterDot * 60);
            String ff = ffStr.substring(0, ffStr.indexOf("."));
            String afterFF = ffStr.substring(ffStr.indexOf(".") + 1, ffStr.length());
            double dAfterFF = Double.parseDouble("0." + afterFF);
            double dMM = dAfterFF * 60;
            beforStr = new StringBuilder(beforDot).append("°").append(ff).append("′").append((int) dMM).append("″").toString();
        } else {
            beforStr = beforStr + "°00′00″";
        }
        return beforStr;
    }

    /**
	 * Copy 整个目录一般方式 svn控制文件不copy
	 * 
	 * @param file1
	 * @param file2
	 * @throws IOException
	 */
    public static void copyDirectiory(String file1, String file2) throws IOException {
        (new File(file1)).mkdirs();
        File[] file = (new File(file2)).listFiles();
        for (int i = 0; i < file.length; i++) {
            if (file[i].isFile()) {
                FileInputStream input = new FileInputStream(file[i]);
                FileOutputStream output = new FileOutputStream(file1 + "/" + file[i].getName());
                byte[] b = new byte[1024 * 5];
                int len;
                while ((len = input.read(b)) != -1) {
                    output.write(b, 0, len);
                }
                output.flush();
                output.close();
                input.close();
            }
            if (file[i].isDirectory() && !file[i].getName().equals(".svn")) {
                copyDirectiory(file1 + "/" + file[i].getName(), file2 + "/" + file[i].getName());
            }
        }
    }

    /**
	 * Copy 某个特定文件，nio 方式
	 * 
	 * @param in
	 * @param out
	 * @throws Exception
	 */
    public static void copyFile(File in, File out) {
        try {
            FileChannel sourceChannel = new FileInputStream(in).getChannel();
            FileChannel destinationChannel = new FileOutputStream(out).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
            sourceChannel.close();
            destinationChannel.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getDateStr(java.util.Date aDate, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(aDate);
    }

    public static void main(String[] args) throws Exception {
    }
}
