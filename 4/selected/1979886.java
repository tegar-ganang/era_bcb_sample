package org.sss.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.sss.common.impl.ProxyModule;
import org.sss.common.impl.StreamImpl;
import org.sss.common.model.IBaseObject;
import org.sss.common.model.IDatafield;
import org.sss.common.model.II18n;
import org.sss.common.model.IModule;
import org.sss.common.model.IParent;
import org.sss.common.model.IStream;
import org.sss.common.model.OpType;

/**
 * 平台工具集合
 * @author Jason.Hoo (latest modification by $Author: hujianxin78728 $)
 * @version $Revision: 710 $ $Date: 2012-04-22 05:13:08 -0400 (Sun, 22 Apr 2012) $
 */
public class ContainerUtils {

    static final Log log = LogFactory.getLog(ContainerUtils.class);

    public static boolean isCompressed = false;

    static final Format format = new DecimalFormat("#.##");

    static final Format dateFormat = new SimpleDateFormat("yyyy/MM/dd");

    public static final void listVersions(ClassLoader cl) throws IOException {
        Properties p = new Properties();
        Enumeration<URL> e = cl.getResources("eibs.version.txt");
        while (e.hasMoreElements()) {
            p.clear();
            p.load(e.nextElement().openStream());
            log.info(p.getProperty("name") + "\t" + p.getProperty("version"));
        }
    }

    public static final void copy(InputStream is, OutputStream os) throws IOException {
        try {
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    public static boolean isEmpty(String value) {
        if (value == null) return true;
        return "".equals(value.trim());
    }

    public static final int parseInt(Object value) {
        if (value == null) return 0;
        try {
            if (value instanceof String) return Integer.parseInt((String) value);
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public static final BigDecimal parseDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            if (value instanceof String) return new BigDecimal((String) value);
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public static final Date parseDate(Object value, String pattern) {
        if (value == null) return null;
        try {
            String v = value instanceof String ? (String) value : value.toString();
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            return format.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    public static final String getI18nStr(String name, String key) {
        return II18n.I18N_IDENT + name + II18n.I18N_IDENT + key + II18n.I18N_IDENT;
    }

    static final HashMap<String, Properties> pMap = new HashMap<String, Properties>();

    /**
   * 比较时间值差异
   */
    public static int diff(Date date1, Date date2) {
        return date1.compareTo(date2);
    }

    /**
   * 比较时间值差异
   */
    public static Date add(Date date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, days);
        return c.getTime();
    }

    /**
   * 获取当天时间
   */
    public static Date today() {
        return new Date();
    }

    /**
   * 格式化对象
   */
    public static String format(Object object, String pattern) {
        if (object == null) return null;
        if (object instanceof IDatafield) return format(((IDatafield) object).getValue(), pattern);
        if (pattern.startsWith("%")) return String.format(pattern, object);
        if (object instanceof Integer || object instanceof BigDecimal) return new DecimalFormat(pattern).format(object);
        if (object instanceof Date) return new SimpleDateFormat(pattern).format(object);
        return new MessageFormat(pattern).format(object);
    }

    /**
   * 检查对象是否为空
   */
    public static boolean isEmpty(Object object) {
        if (object == null) return true;
        if (object instanceof IDatafield) return isEmpty(((IDatafield) object).getValue());
        if (object instanceof Number) return ((Number) object).doubleValue() == 0;
        if (object instanceof String) return "".equals(object);
        if (object instanceof Collection) return ((Collection) object).isEmpty();
        return false;
    }

    public static synchronized void close() {
        pMap.clear();
    }

    private static synchronized Properties getProperties(String name) throws FileNotFoundException, IOException {
        Properties properties = pMap.get(name);
        if (properties == null) {
            properties = new Properties();
            properties.load(new FileInputStream(new File(name)));
            pMap.put(name, properties);
        }
        return properties;
    }

    /**
   * 读取指定配置文件对应域值
   */
    public static String getPropertyValue(String fileName, String key) {
        try {
            return getProperties(fileName).getProperty(key);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private static File mkdirs(String fileName) {
        File file = new File(fileName);
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        return file;
    }

    /**
   * 设置指定配置文件对应域值
   */
    public static void setPropertyValue(String fileName, String key, String value) {
        try {
            Properties properties = getProperties(fileName);
            properties.setProperty(key, value);
            properties.store(new FileOutputStream(mkdirs(fileName)), null);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
   * 拷贝指定文件或目录
   */
    public static void fileCopy(String sourceFileName, String targetFileName) {
        try {
            FileUtils.copyFile(new File(sourceFileName), mkdirs(targetFileName));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
   * 删除指定文件或目录
   */
    public static void fileDelete(String fileName) {
        try {
            FileUtils.forceDelete(new File(fileName));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
   * 判断指定文件或目录是否存在
   */
    public static boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }

    /**
   * 保存数据流至文件
   */
    public static void streamSave(IStream stream, String fileName) {
        try {
            ContainerUtils.copy(stream.getInputStream(), new FileOutputStream(mkdirs(fileName)));
            stream.close();
        } catch (Exception e) {
            log.error("streamSave error.", e);
        }
    }

    /**
   * 读取文件至数据流
   */
    public static void streamLoad(IStream stream, String fileName) {
        try {
            stream.close();
            ContainerUtils.copy(new FileInputStream(new File(fileName)), stream.getOutputStream());
        } catch (Exception e) {
            log.error("streamLoad error.", e);
        }
    }

    /**
   * 读取数据流后关闭它
   */
    public static void streamClear(IStream stream) {
        stream.close();
    }

    /**
   * 调用指定对象方法或类静态方法
   */
    public static Object invoke(Object object, String methodName, Object... args) {
        try {
            if (object instanceof String) return MethodUtils.invokeStaticMethod(Class.forName((String) object), methodName, args);
            return MethodUtils.invokeMethod(object, methodName, args);
        } catch (Exception e) {
            log.info("invoke errr.", e);
            return null;
        }
    }

    /**
   * 读取对象属性值
   */
    public static Object getProperty(Object object, String propertyName) {
        try {
            if (object instanceof ProxyModule) object = ((ProxyModule) object).getHost();
            return PropertyUtils.getProperty(object, propertyName);
        } catch (Exception e) {
            log.info("getProperty", e);
            return null;
        }
    }

    /**
   * 设置对象属性值
   */
    public static void setProperty(Object object, String propertyName, Object value) {
        try {
            if (object instanceof ProxyModule) object = ((ProxyModule) object).getHost();
            if (object instanceof IModule) ((IDatafield) ((IModule) object).get(propertyName)).setValue(value); else PropertyUtils.setProperty(object, propertyName, value);
        } catch (Exception e) {
            log.info("setProperty", e);
        }
    }

    /**
   * 合并文件字符串
   */
    public static String catPath(String path, String subPath) {
        return new File(new File(path), subPath).getAbsolutePath();
    }

    /**
   * 返回指定目录、指定后缀名文件列表
   */
    public static ArrayList<String> listFiles(String path, String extension) {
        ArrayList<String> list = new ArrayList();
        try {
            for (File file : FileUtils.listFiles(new File(path), new String[] { extension }, false)) list.add(file.getAbsolutePath());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return list;
    }

    public static void clear(Object object) {
        if (object instanceof IBaseObject) ((IBaseObject) object).clear(); else if (object instanceof Collection) ((Collection) object).clear();
    }

    public static IBaseObject getProperty(IParent parent, String name) {
        try {
            if (isEmpty(name)) return parent;
            if (parent instanceof ProxyModule) parent = ((ProxyModule) parent).getHost();
            return (IBaseObject) PropertyUtils.getProperty(parent, name);
        } catch (Exception e) {
            log.error(new StringBuffer("ModuleSession.getProperty error,module:").append(parent.getUrl()).append(",property:").append(name).append("\nError message: ").append(e.getMessage()));
            return null;
        }
    }

    public static Date sqlDate(Date date, OpType type) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        switch(type) {
            case LT:
            case LE:
                c.set(Calendar.HOUR_OF_DAY, 23);
                c.set(Calendar.MINUTE, 59);
                c.set(Calendar.SECOND, 59);
                c.set(Calendar.MILLISECOND, 999);
                if (type == OpType.LT) return new Timestamp(c.getTime().getTime());
                return new java.util.Date(c.getTime().getTime());
            case GT:
            case GE:
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                if (type == OpType.GT) return new Timestamp(c.getTime().getTime());
                return new java.util.Date(c.getTime().getTime());
            default:
                return date;
        }
    }

    public static final int loadExcel(List<String[]> list, IStream s, int sheetIndex, int rowOffset, int rowMax, int columnOffset, int columnCount) {
        Workbook wb = null;
        try {
            wb = new HSSFWorkbook(s.getInputStream());
        } catch (OfficeXmlFileException _) {
            try {
                InputStream is = s.getInputStream();
                is.reset();
                wb = new XSSFWorkbook(is);
            } catch (IOException e) {
                log.info("load excel error.", e);
                return -1;
            }
        } catch (IOException e) {
            log.info("load excel error.", e);
            return -1;
        }
        if (sheetIndex < 0 || sheetIndex >= wb.getNumberOfSheets()) {
            log.warn("load excel sheet with a illegal index.");
            return -3;
        }
        Sheet sheet = wb == null ? null : wb.getSheetAt(sheetIndex);
        if (sheet == null) {
            log.warn("load excel sheet error.");
            return -2;
        }
        int rows = rowMax > 0 ? Math.min(rowMax, sheet.getLastRowNum() - rowOffset) : sheet.getLastRowNum() - rowOffset;
        if (columnCount > 0) {
            for (int i = 0; i <= rows; i++) {
                Row row = sheet.getRow(i + rowOffset);
                String[] values = new String[columnCount];
                for (int j = 0; j < columnCount; j++) {
                    if (row == null) values[j] = ""; else {
                        Cell cell = row.getCell(j + columnOffset);
                        values[j] = cell != null ? getCellValue(cell, cell.getCellType()) : "";
                    }
                }
                list.add(values);
            }
        }
        return rows;
    }

    private static final String getCellValue(Cell cell, int type) {
        switch(type) {
            case Cell.CELL_TYPE_FORMULA:
                return getCellValue(cell, cell.getCachedFormulaResultType());
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            case Cell.CELL_TYPE_NUMERIC:
                int df = cell.getCellStyle().getDataFormat();
                boolean flag = df == 14;
                if (df == 176 || df == 177 || df == 178) {
                    String pattern = cell.getCellStyle().getDataFormatString();
                    if (!isEmpty(pattern) && pattern.contains("yy")) flag = true;
                }
                return flag ? dateFormat.format(cell.getDateCellValue()) : format.format(cell.getNumericCellValue());
            default:
                return cell.toString();
        }
    }

    public static final void unloadExcel(List<Object[]> list, IStream s, boolean flag) {
        Workbook wb = flag ? new XSSFWorkbook() : new HSSFWorkbook();
        try {
            Sheet sheet = wb.createSheet();
            int rowNum = 0;
            for (Object[] values : list) {
                Row row = sheet.createRow(rowNum);
                int cellNum = 0;
                for (Object value : values) {
                    Cell cell = row.createCell(cellNum);
                    if (value != null && value instanceof Number) {
                        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        if (value == null || value instanceof String) cell.setCellValue((String) value); else cell.setCellValue(value.toString());
                    }
                    cellNum++;
                }
                rowNum++;
            }
            OutputStream os = s.getOutputStream();
            wb.write(os);
            os.flush();
            os.close();
        } catch (IOException e) {
            log.error("unload Excel error.", e);
        }
    }

    public static final void main(String[] args) throws FileNotFoundException, IOException {
        ArrayList<String[]> result = new ArrayList<String[]>();
        IStream is = new StreamImpl();
        IOUtils.copy(new FileInputStream("H:\\7-项目预算表.xlsx"), is.getOutputStream());
        int count = loadExcel(result, is, 0, 0, -1, 16, 1);
        System.out.println(count);
        for (String[] rs : result) {
            for (String r : rs) {
                System.out.print(r + "\t");
            }
            System.out.println();
        }
    }
}
