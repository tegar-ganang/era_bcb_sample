package com.newbee.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import com.newbee.database.Table;

public class Format {

    /**
     * 按指定格式返回当前日期字符串
     * 
     * @return String
     */
    public static final String getToday(String format) {
        SimpleDateFormat sd = new SimpleDateFormat(format);
        String todayStr = sd.format(new Date());
        return todayStr;
    }

    /**
	 * 按指定格式返回昨天的日期字符串
	 * @author GuoRui (2005-5-18 17:12:44)
	 * @param ft 要求返回的字符串格式
	 * @return String
	 */
    public static final String getYesterDay(String ft) {
        SimpleDateFormat sdf = new SimpleDateFormat(ft);
        Date today = new Date();
        long d = today.getTime() - 1000 * 60 * 60 * 24;
        Date yesDay = new Date(d);
        String yesDayStr = sdf.format(yesDay);
        return yesDayStr;
    }

    /**
	 * 按指定格式返回前几天的日期字符串
	 * @author GuoRui (2005-5-18 17:12:44)
	 * @param ft 要求返回的字符串格式
	 * @param x 前几天（负值）或后几天（正值）
	 * @return String
	 */
    public static final String getAnyDays(String ft, int x) {
        SimpleDateFormat sdf = new SimpleDateFormat(ft);
        Date today = new Date();
        long d = today.getTime() + (1000 * 60 * 60 * 24) * x;
        Date yesDay = new Date(d);
        String yesDayStr = sdf.format(yesDay);
        return yesDayStr;
    }

    /**
	 * @Todo 计算指定时间减去当前时间的差(不是日期)
	 * @author GuoRui  (2005-4-26 20:49:48)
	 * @return long
	 */
    public static final long compareToTime(String time) {
        long diffTime = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        try {
            Date s = sdf.parse(time);
            Date current = sdf.parse(nowTime());
            diffTime = s.getTime() - current.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return diffTime;
    }

    /**
     * 当前时间
     * 格式HH:mm:ss
     * @return String
     */
    public static String nowTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    /**
     * 当前日期时间
     * 
     * @return
     */
    public static String getFullTime() {
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdt.format(new Date());
    }

    /**
     * 格式化数字
     * 
     * @param s 数字字符串
     * @param len 指定小数保留位数
     * @return String
     */
    public static String getNumber(String s, int len) {
        double d = 0;
        try {
            d = Double.parseDouble(s);
        } catch (Exception e) {
            return getNumber("0", len);
        }
        return getNumber(d, len);
    }

    /**
	 * @author GR
	 * @date (2005-4-7 20:07:25)
	 * Function :格式化流水号,长度参数大于等于1
	 */
    public static String getNo(int i, int len) {
        String fmt = "0";
        for (int j = 1; j < len; j++) {
            fmt += "0";
        }
        DecimalFormat decimalFormat = new DecimalFormat(fmt);
        return decimalFormat.format(i);
    }

    /**
     * 格式化数字
     * 
     * @param f
     * @param len指定小数保留位数
     * @return
     */
    public static String getNumber(float f, int len) {
        return getNumber((double) f, len);
    }

    /**
     * 格式化数字
     * 
     * @param d
     * @param len 指定保留的小数位数（四舍五入）
     * @return String
     */
    public static String getNumber(double d, int len) {
        BigInteger a1 = BigInteger.valueOf(10).pow(len);
        double a2 = d * a1.doubleValue();
        double e = (Math.round(a2)) / a1.doubleValue();
        String formatStr = "#0";
        if (len > 0) {
            formatStr += ".";
        }
        for (int i = 0; i < len; i++) {
            formatStr += "0";
        }
        return new DecimalFormat(formatStr).format(e);
    }

    /**
     * 从字符串表示的数字，得到一个整数
     * 
     * @param s 数字的字符串表示
     * @return 整数（四舍五入）
     */
    public static String getInt(String s) {
        return new DecimalFormat("#").format(new Double(s).doubleValue());
    }

    /**
     * 从字符串表示的数字，得到一个保留指定小数位数的百分数
     * 
     * @param s 数字的字符串表示
     * @param len 百分数保留的小数位数
     * @return String
     */
    public static String getPercent(String s, int len) {
        double d = 0;
        try {
            d = new Double(s).doubleValue() * 100;
        } catch (Exception e) {
            return "0%";
        }
        return getNumber(d, len) + "%";
    }

    /**
	 * 比较两个日期的先后，date2减去date1的差
	 * <br>使用默认格式 new SimpleDateFormat("yyyy/MM/dd");
	 * @author GuoRui (2005-6-1 17:52:18)
	 * @param date1 日期1
	 * @param date2 日期2
	 * @return int
	 */
    public static final int compareDate(String date1, String date2) {
        int tValue = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        try {
            Date oldT = sdf.parse(date1);
            Date newT = sdf.parse(date2);
            tValue = newT.compareTo(oldT);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return tValue;
    }

    /**
	 * 时间比较方法，newTime减去oldTime的差
	 * <br>使用默认格式 new SimpleDateFormat("HH:mm");
	 * @author GuoRui (2005-6-2 9:11:27)
	 * @param oldTime newTime 时间
	 * @return int
	 */
    public static final int compareTime(String oldTime, String newTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        int tValue = 0;
        try {
            Date oldT = sdf.parse(oldTime);
            Date newT = sdf.parse(newTime);
            tValue = newT.compareTo(oldT);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return tValue;
    }

    /**
	 * 把源串中的某个字符串替换为另一个字符串
	 * 
	 * @author GuoRui (2005-6-2 9:11:27)
	 * @param s 源字符串， pix 要替换下来的字符串， rs 要替换上去的字符串
	 * @return String
	 */
    public static String replaceAll(String s, String pix, String rs) {
        String newStr = s;
        int index = -1;
        while ((index = newStr.indexOf(pix)) != -1) {
            newStr = newStr.substring(0, index) + rs + newStr.substring(index + pix.length());
        }
        return newStr;
    }

    /**
	 * 为给定的日历字段添加或减去指定的时间量
	 * 
	 * @author GuoRui (2005-6-2 9:11:27)
	 * @param date 要修改的日期， field 日历字段， amount 添加或减去指定的时间量
	 * @return Date
	 */
    public static Date dateAdd(Date date, int field, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(field, amount);
        return cal.getTime();
    }

    public static String nvl(String s) {
        return s == null ? "" : s;
    }

    public static String getHTML(String s) {
        if (s != null) {
            s = s.replaceAll("\n", "<BR>");
            s = s.replaceAll("\r", "");
        }
        return s;
    }

    public static String getHTMLPel(String s) {
        if (s != null) {
            s = s.replaceAll("<", "&lt;");
            s = s.replaceAll(">", "&gt;");
            s = s.replaceAll(" ", "&nbsp;");
            s = s.replaceAll("\"", "&quot;");
        }
        return s;
    }

    /**
	 * 返回可以在数据库中插入单引号和双引号的字符串格式
	 * 
	 * @Author guo
	 * @EditTime 2008-11-13 下午08:41:54
	 */
    public static String getSQLInsertValue(String s) {
        if (s != null) {
            s = s.replaceAll("'", "''");
        }
        return s;
    }

    /**
	 * 返回可以在js中显示单引号和双引号的字符串格式
	 * 
	 * @Author guo
	 * @EditTime 2008-11-13 下午08:41:54
	 */
    public static String getScriptDispalyValue(String s) {
        if (s != null) {
            s = s.replaceAll("\'", "\\\\'").replaceAll("\"", "\\\\\"");
        }
        return s;
    }

    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }

    public static byte[] hex2byte(String h) {
        byte[] ret = new byte[h.length() / 2];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Integer.decode("#" + h.substring(2 * i, 2 * i + 2)).byteValue();
        }
        return ret;
    }

    /**
     * 文件转化为字节数组
     * @Author Sean.guo
     * @EditTime 2007-8-13 上午11:45:28
     */
    public static byte[] getBytesFromFile(File f) {
        if (f == null) {
            return null;
        }
        try {
            FileInputStream stream = new FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = stream.read(b)) != -1) out.write(b, 0, n);
            stream.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * 把字节数组保存为一个文件
     * @Author Sean.guo
     * @EditTime 2007-8-13 上午11:45:56
     */
    public static File getFileFromBytes(byte[] b, String outputFile) {
        BufferedOutputStream stream = null;
        File file = null;
        try {
            file = new File(outputFile);
            FileOutputStream fstream = new FileOutputStream(file);
            stream = new BufferedOutputStream(fstream);
            stream.write(b);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return file;
    }

    /**
     * 从字节数组获取对象
     * @Author Sean.guo
     * @EditTime 2007-8-13 上午11:46:34
     */
    public static Object getObjectFromBytes(byte[] objBytes) throws Exception {
        if (objBytes == null || objBytes.length == 0) {
            return null;
        }
        ByteArrayInputStream bi = new ByteArrayInputStream(objBytes);
        ObjectInputStream oi = new ObjectInputStream(bi);
        return oi.readObject();
    }

    /**
     * 从对象获取一个字节数组
     * @Author Sean.guo
     * @EditTime 2007-8-13 上午11:46:56
     */
    public static byte[] getBytesFromObject(Serializable obj) throws Exception {
        if (obj == null) {
            return null;
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(obj);
        return bo.toByteArray();
    }

    /**
	 * 返回字符串的真是长度，主要针对中文英文数字混合
	 * 字符串的长度设计
	 * @author GuoRui (2005-7-18 11:59:15)
	 * @param str 字符串
	 * @return int
	 */
    public static int doubleByte(String str) {
        char bt[] = str.toCharArray();
        int length = 0;
        for (int i = 0; i < bt.length; i++) {
            if (bt[i] > 255) {
                length += 2;
            } else {
                length += 1;
            }
        }
        return length;
    }

    /**
	 * 在指定范围内长生随机数，包含min和max
	 * 
	 * @param min	最小值
	 * @param max	最大值
	 * @return int	随机整数
	 */
    public static int getRandom(int min, int max) {
        return (int) (Math.random() * (max - min + 1) + min);
    }

    /**
	 * 读取Oracle脚本文件
	 * 
	 * @param filePath
	 * @return
	 */
    public static String getSqlFromFile(String filePath, String encoding) {
        String sql = "";
        String fn = "";
        BufferedReader reader = null;
        try {
            File f = new File(filePath);
            fn = f.getName();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), encoding));
            StringBuffer strbContent = new StringBuffer();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.startsWith("--")) continue;
                int pos = line.indexOf("--");
                if (pos > 0) line = line.substring(0, pos);
                strbContent.append(" " + line + " ");
            }
            sql = strbContent.toString();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
            }
        }
        System.out.println(fn);
        return sql;
    }

    /**
	 * 把查询结果保存为一个文件
	 * 
	 * @Param t 查询结果
	 * @EditTime 2008-10-6 下午02:13:06
	 * @Author sean
	 */
    public static void table2File(Table t) {
    }

    public static void main(String[] args) {
        try {
            System.out.println(new DecimalFormat("#").format(new Double("-10012.989").doubleValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
