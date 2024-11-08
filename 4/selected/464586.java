package com.atech.mobile.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import com.atech.mobile.i18n.I18nControlAbstract;

public abstract class ATMobileDataAccessAbstract {

    Hashtable<String, String> config_db_values = null;

    public Hashtable<String, String> m_settings_ht = null;

    public static String pathPrefix = ".";

    public Color color_background, color_foreground;

    public boolean printing_plugin_installed = false;

    protected I18nControlAbstract m_i18n = null;

    public static final int DB_NOT_LOADED = 0;

    public static final int DB_BASE = 1;

    public static final int DB_DIOCESE = 2;

    public static final int DB_DIOCESE_PERSONAL = 3;

    public static final int DB_LOAD_COMPLETE = 10;

    private int db_loading_status = 0;

    public Object[] yes_no_combo = null;

    public Object[] typesAll = null;

    public LineBorder border_line;

    public Font fonts[] = null;

    protected Collator m_collator = null;

    protected Container parent = null;

    public static char real_decimal;

    public static char false_decimal;

    private static boolean decimals_set;

    public String days[] = new String[7];

    public String months[] = new String[12];

    /**
     * 
     * This is DataAccess constructor; Since classes use Singleton Pattern,
     * constructor is protected and can be accessed only with getInstance()
     * method.<br>
     * <br>
     * 
     */
    public ATMobileDataAccessAbstract(I18nControlAbstract ic) {
        this.m_i18n = ic;
        loadArraysTranslation();
        checkPrerequisites();
        loadFonts();
        m_settings_ht = new Hashtable<String, String>();
        this.m_collator = this.m_i18n.getCollationDefintion();
        if (!ATMobileDataAccessAbstract.decimals_set) initDecimals();
    }

    public abstract void initSpecial();

    public void loadArraysTranslation() {
        months[0] = m_i18n.getMessage("JANUARY");
        months[1] = m_i18n.getMessage("FEBRUARY");
        months[2] = m_i18n.getMessage("MARCH");
        months[3] = m_i18n.getMessage("APRIL");
        months[4] = m_i18n.getMessage("MAY");
        months[5] = m_i18n.getMessage("JUNE");
        months[6] = m_i18n.getMessage("JULY");
        months[7] = m_i18n.getMessage("AUGUST");
        months[8] = m_i18n.getMessage("SEPTEMBER");
        months[9] = m_i18n.getMessage("OCTOBER");
        months[10] = m_i18n.getMessage("NOVEMBER");
        months[11] = m_i18n.getMessage("DECEMBER");
        days[0] = m_i18n.getMessage("MONDAY");
        days[1] = m_i18n.getMessage("TUESDAY");
        days[2] = m_i18n.getMessage("WEDNESDAY");
        days[3] = m_i18n.getMessage("THURSDAY");
        days[4] = m_i18n.getMessage("FRIDAY");
        days[5] = m_i18n.getMessage("SATURDAY");
        days[6] = m_i18n.getMessage("SUNDAY");
    }

    private void initDecimals() {
        DecimalFormatSymbols dfs = new DecimalFormat().getDecimalFormatSymbols();
        ATMobileDataAccessAbstract.real_decimal = dfs.getDecimalSeparator();
        if (dfs.getDecimalSeparator() == '.') ATMobileDataAccessAbstract.false_decimal = ','; else ATMobileDataAccessAbstract.false_decimal = '.';
        ATMobileDataAccessAbstract.decimals_set = true;
    }

    protected ArrayList<Component> components = new ArrayList<Component>();

    public Component getCurrentComponentParent() {
        return this.components.get(this.components.size() - 2);
    }

    public Component getCurrentComponent() {
        return this.components.get(this.components.size() - 1);
    }

    public void addComponent(Component cmp) {
        this.components.add(cmp);
    }

    public void listComponents() {
        System.out.println("Lst: " + this.components);
    }

    public void removeComponent(Component cmp) {
        ArrayList<Component> cmps_new = new ArrayList<Component>();
        for (int i = 0; i < this.components.size(); i++) {
            if (this.components.get(i).equals(cmp)) {
                break;
            } else {
                cmps_new.add(this.components.get(i));
            }
        }
        this.components = cmps_new;
    }

    public static final int PARENT_FRAME = 1;

    public static final int PARENT_DIALOG = 2;

    public ArrayList<Container> cnt_list = new ArrayList<Container>();

    public void addContainer(Container cont) {
        System.out.println("!!!! addContainer: " + this.cnt_list + "\nDataAccess: " + this);
        this.cnt_list.add(cont);
        System.out.println("!!!! addContainer: " + this.cnt_list + "\nDataAccess: " + this);
    }

    public void removeContainer(Container cont) {
        System.out.println("!!!! removeContainer: " + this.cnt_list + "\nDataAccess: " + this);
        this.cnt_list.remove(cont);
        System.out.println("!!!! removeContainer: " + this.cnt_list + "\nDataAccess: " + this);
    }

    public int getLastContainerType() {
        if ((cnt_list.get(cnt_list.size() - 1)) instanceof JFrame) {
            return ATMobileDataAccessAbstract.PARENT_FRAME;
        } else return ATMobileDataAccessAbstract.PARENT_DIALOG;
    }

    public int getLastParentType() {
        if (this.cnt_list.size() < 2) {
            return ATMobileDataAccessAbstract.PARENT_FRAME;
        } else {
            if ((cnt_list.get(cnt_list.size() - 2)) instanceof JFrame) {
                return ATMobileDataAccessAbstract.PARENT_FRAME;
            } else return ATMobileDataAccessAbstract.PARENT_DIALOG;
        }
    }

    public Container getLastParent() {
        System.out.println("Conatiners: " + this.cnt_list);
        if (this.cnt_list.size() == 1) {
            return cnt_list.get(0);
        }
        if (this.cnt_list.size() < 2) {
            return this.getMainParent();
        } else {
            return (cnt_list.get(cnt_list.size() - 2));
        }
    }

    public JFrame getLastParentFrame() {
        return (JFrame) this.getLastParent();
    }

    public JDialog getLastParentDialog() {
        return (JDialog) this.getLastParent();
    }

    JFrame main_parent = null;

    public abstract String getApplicationName();

    /**
     * Must have ending back-slash
     * 
     * @return
     */
    public abstract String getImagesRoot();

    public int main_parent_type = 1;

    public void setMainParent(JFrame frame) {
        this.main_parent = frame;
        this.addComponent(this.main_parent);
    }

    public JFrame getMainParent() {
        return this.main_parent;
    }

    public abstract void checkPrerequisites();

    public I18nControlAbstract getI18nControlInstance() {
        return this.m_i18n;
    }

    public boolean config_loaded = false;

    public boolean wasConfigLoaded() {
        return this.config_loaded;
    }

    public Hashtable<String, String> loadPropertyFile(String filename) {
        Hashtable<String, String> config_db_values = new Hashtable<String, String>();
        Properties props = new Properties();
        this.config_loaded = true;
        try {
            File f = new File(".");
            System.out.println("File: " + f.getCanonicalPath());
            FileInputStream in = new FileInputStream(filename);
            props.load(in);
        } catch (Exception ex) {
            System.out.println("Error loading config file (" + filename + "): " + ex);
            this.config_loaded = false;
        }
        if (this.config_loaded) {
            for (Enumeration<Object> en = props.keys(); en.hasMoreElements(); ) {
                String key = (String) en.nextElement();
                config_db_values.put(key, props.getProperty(key));
            }
        } else return null;
        return config_db_values;
    }

    public void setDbLoadingStatus(int status) {
        this.db_loading_status = status;
    }

    public int getDbLoadingStatus() {
        return this.db_loading_status;
    }

    public boolean isDbLoadedForStatus(int status) {
        if ((this.db_loading_status == status) || (this.db_loading_status > status)) return true; else return false;
    }

    JDialog m_dialog = null;

    public JDialog getOpenedDialog() {
        return this.m_dialog;
    }

    public void setOpenedDialog(JDialog dialog) {
        this.m_dialog = dialog;
    }

    public static final int FONT_BIG_BOLD = 0;

    public static final int FONT_NORMAL = 1;

    public static final int FONT_NORMAL_BOLD = 2;

    public static final int FONT_NORMAL_P2 = 3;

    public static final int FONT_NORMAL_BOLD_P2 = 4;

    public static final int FONT_UPDATE_TREE_HEADER = 5;

    public static final int FONT_UPDATE_TREE_ITEM = 6;

    public void loadFonts() {
        fonts = new Font[7];
        fonts[0] = new Font("SansSerif", Font.BOLD, 22);
        fonts[1] = new Font("SansSerif", Font.PLAIN, 12);
        fonts[2] = new Font("SansSerif", Font.BOLD, 12);
        fonts[3] = new Font("SansSerif", Font.PLAIN, 14);
        fonts[4] = new Font("SansSerif", Font.BOLD, 14);
    }

    public Font getFont(int font_id) {
        return fonts[font_id];
    }

    public Image getImage(String filename, Component cmp) {
        Image img;
        InputStream is = this.getClass().getResourceAsStream(filename);
        if (is == null) System.out.println("Error reading image: " + filename);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int c;
            while ((c = is.read()) >= 0) baos.write(c);
            img = cmp.getToolkit().createImage(baos.toByteArray());
        } catch (Exception ex) {
            System.out.println("Image " + filename + " could not be created.");
            return null;
        }
        return img;
    }

    public void setParent(Container component) {
        this.parent = component;
    }

    public Container getParent() {
        return this.parent;
    }

    public ImageIcon getImageIcon_22x22(String name, Container comp) {
        return getImageIcon(name, 22, 22, comp);
    }

    public ImageIcon getImageIcon(String name, int width, int height, Container comp) {
        return getImageIcon(this.getImagesRoot(), name, width, height, comp);
    }

    public ImageIcon getImageIcon(String root, String name, int width, int height, Container comp) {
        return new ImageIcon(getImage(root + name, comp).getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    public ImageIcon getImageIcon(String name, Container comp) {
        return getImageIcon(this.getImagesRoot(), name);
    }

    public ImageIcon getImageIcon(String root, String name, Container comp) {
        return new ImageIcon(getImage(root + name, comp));
    }

    public ImageIcon getImageIcon(String name) {
        return getImageIcon(this.getImagesRoot(), name);
    }

    public ImageIcon getImageIcon(String root, String name) {
        File f = new File(".");
        System.out.println("Start path: " + f.getAbsolutePath());
        return new ImageIcon(getImage(root + name, this.getLastParent()));
    }

    public int compareUnicodeStrings(String s1, String s2) {
        return this.m_collator.compare(s1, s2);
    }

    public void centerJDialog(Component dialog) {
        Component cmp = this.getCurrentComponentParent();
        this.centerJDialog(dialog, cmp);
    }

    public void centerJDialog(Component dialog, Component parent) {
        Rectangle rec = parent.getBounds();
        int x = rec.width / 2;
        x += (rec.x);
        int y = rec.height / 2;
        y += rec.y;
        x -= (dialog.getBounds().width / 2);
        y -= (dialog.getBounds().height / 2);
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        dialog.setBounds(x, y, dialog.getBounds().width, dialog.getBounds().height);
    }

    public void loadColors() {
        ColorUIResource cui = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("textText");
        this.color_foreground = new Color(cui.getRed(), cui.getGreen(), cui.getBlue(), cui.getAlpha());
        ColorUIResource cui2 = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("Label.background");
        this.color_background = new Color(cui2.getRed(), cui2.getGreen(), cui2.getBlue(), cui2.getAlpha());
        this.border_line = new LineBorder(this.color_foreground);
    }

    public int getJFormatedTextValueInt(JFormattedTextField ftf) {
        try {
            ftf.commitEdit();
        } catch (Exception ex) {
            System.out.println("Exception on commit value:" + ex);
        }
        Object o = ftf.getValue();
        if (o instanceof Integer) {
            Integer l = (Integer) o;
            return l.intValue();
        } else if (o instanceof Long) {
            Long l = (Long) o;
            return l.intValue();
        } else if (o instanceof Byte) {
            Byte b = (Byte) o;
            return b.intValue();
        } else if (o instanceof Short) {
            Short s = (Short) o;
            return s.intValue();
        } else if (o instanceof Float) {
            Float f = (Float) o;
            return f.intValue();
        } else {
            Double d = (Double) o;
            return d.intValue();
        }
    }

    public long getJFormatedTextValueLong(JFormattedTextField ftf) {
        try {
            ftf.commitEdit();
        } catch (Exception ex) {
            System.out.println("Exception on commit value:" + ex);
        }
        Object o = ftf.getValue();
        if (o instanceof Long) {
            Long l = (Long) o;
            return l.longValue();
        } else if (o instanceof Integer) {
            Integer l = (Integer) o;
            return l.longValue();
        } else if (o instanceof Byte) {
            Byte b = (Byte) o;
            return b.longValue();
        } else if (o instanceof Short) {
            Short s = (Short) o;
            return s.longValue();
        } else if (o instanceof Float) {
            Float f = (Float) o;
            return f.longValue();
        } else {
            Double d = (Double) o;
            return d.longValue();
        }
    }

    public byte getJFormatedTextValueByte(JFormattedTextField ftf) {
        try {
            ftf.commitEdit();
        } catch (Exception ex) {
            System.out.println("Exception on commit value:" + ex);
        }
        Object o = ftf.getValue();
        if (o instanceof Byte) {
            Byte b = (Byte) o;
            return b.byteValue();
        } else if (o instanceof Short) {
            Short s = (Short) o;
            return s.byteValue();
        } else if (o instanceof Integer) {
            Integer l = (Integer) o;
            return l.byteValue();
        } else if (o instanceof Long) {
            Long l = (Long) o;
            return l.byteValue();
        } else if (o instanceof Float) {
            Float f = (Float) o;
            return f.byteValue();
        } else {
            Double d = (Double) o;
            return d.byteValue();
        }
    }

    public short getJFormatedTextValueShort(JFormattedTextField ftf) {
        try {
            ftf.commitEdit();
        } catch (Exception ex) {
            System.out.println("Exception on commit value:" + ex);
        }
        Object o = ftf.getValue();
        if (o instanceof Short) {
            Short s = (Short) o;
            return s.shortValue();
        } else if (o instanceof Byte) {
            Byte b = (Byte) o;
            return b.shortValue();
        } else if (o instanceof Integer) {
            Integer l = (Integer) o;
            return l.shortValue();
        } else if (o instanceof Long) {
            Long l = (Long) o;
            return l.shortValue();
        } else if (o instanceof Float) {
            Float f = (Float) o;
            return f.shortValue();
        } else {
            Double d = (Double) o;
            return d.shortValue();
        }
    }

    public float getJFormatedTextValueFloat(JFormattedTextField ftf) {
        try {
            ftf.commitEdit();
        } catch (Exception ex) {
            System.out.println("Exception on commit value:" + ex + "\nValue:" + ftf.getValue());
            ex.printStackTrace();
        }
        Object o = ftf.getValue();
        if (o instanceof Float) {
            Float f = (Float) o;
            return f.floatValue();
        } else if (o instanceof Double) {
            Double d = (Double) o;
            return d.floatValue();
        } else if (o instanceof Long) {
            Long l = (Long) o;
            return l.floatValue();
        } else if (o instanceof Integer) {
            Integer l = (Integer) o;
            return l.floatValue();
        } else if (o instanceof Byte) {
            Byte b = (Byte) o;
            return b.floatValue();
        } else {
            Short s = (Short) o;
            return s.floatValue();
        }
    }

    public double getJFormatedTextValueDouble(JFormattedTextField ftf) {
        try {
            ftf.commitEdit();
        } catch (Exception ex) {
            System.out.println("Exception on commit value:" + ex);
        }
        Object o = ftf.getValue();
        if (o instanceof Double) {
            Double d = (Double) o;
            return d.doubleValue();
        } else if (o instanceof Float) {
            Float f = (Float) o;
            return f.doubleValue();
        } else if (o instanceof Long) {
            Long l = (Long) o;
            return l.doubleValue();
        } else if (o instanceof Integer) {
            Integer l = (Integer) o;
            return l.doubleValue();
        } else if (o instanceof Byte) {
            Byte b = (Byte) o;
            return b.doubleValue();
        } else {
            Short s = (Short) o;
            return s.doubleValue();
        }
    }

    public String[] getMonthsArray() {
        return this.months;
    }

    public String getDateString(int date) {
        int year = date / 10000;
        int months = date - (year * 10000);
        months = months / 100;
        int days = date - (year * 10000) - (months * 100);
        if (year == 0) {
            return getLeadingZero(days, 2) + "/" + getLeadingZero(months, 2);
        } else return getLeadingZero(days, 2) + "/" + getLeadingZero(months, 2) + "/" + year;
    }

    public String getTimeString(int time) {
        int hours = time / 100;
        int min = time - hours * 100;
        return getLeadingZero(hours, 2) + ":" + getLeadingZero(min, 2);
    }

    public String getDateTimeString(long date) {
        return getDateTimeString(date, 1);
    }

    public String getDateTimeAsDateString(long date) {
        return getDateTimeString(date, 2);
    }

    public static final int DATE_TIME_ATECH_DATETIME = 1;

    public static final int DATE_TIME_ATECH_DATE = 2;

    public static final int DATE_TIME_ATECH_TIME = 3;

    public long getATDateTimeFromGC(GregorianCalendar gc, int type) {
        long dt = 0L;
        if (type == DATE_TIME_ATECH_DATETIME) {
            dt += gc.get(GregorianCalendar.YEAR) * 100000000L;
            dt += (gc.get(GregorianCalendar.MONTH) + 1) * 1000000L;
            dt += gc.get(GregorianCalendar.DAY_OF_MONTH) * 10000L;
            dt += gc.get(GregorianCalendar.HOUR_OF_DAY) * 100L;
            dt += gc.get(GregorianCalendar.MINUTE);
        } else if (type == DATE_TIME_ATECH_DATE) {
            dt += gc.get(GregorianCalendar.YEAR) * 10000L;
            dt += (gc.get(GregorianCalendar.MONTH) + 1) * 100L;
            dt += gc.get(GregorianCalendar.DAY_OF_MONTH);
        } else if (type == DATE_TIME_ATECH_TIME) {
            dt += gc.get(GregorianCalendar.HOUR_OF_DAY) * 100L;
            dt += gc.get(GregorianCalendar.MINUTE);
        }
        return dt;
    }

    public long getATDateTimeFromParts(int day, int month, int year, int hour, int minute, int type) {
        long dt = 0L;
        if (type == DATE_TIME_ATECH_DATETIME) {
            dt += year * 100000000L;
            dt += month * 1000000L;
            dt += day * 10000L;
            dt += hour * 100L;
            dt += minute;
        } else if (type == DATE_TIME_ATECH_DATE) {
            dt += year * 10000L;
            dt += month * 100L;
            dt += day;
        } else if (type == DATE_TIME_ATECH_TIME) {
            dt += hour * 100L;
            dt += minute;
        }
        return dt;
    }

    public long getDateFromATDate(long data) {
        int d2 = (int) (data / 10000);
        return d2;
    }

    public String getDateTimeAsTimeString(long date) {
        return getDateTimeString(date, 3);
    }

    public static final int DT_DATETIME = 1;

    public static final int DT_DATE = 2;

    public static final int DT_TIME = 3;

    public String getDateTimeString(long dt, int ret_type) {
        int y = (int) (dt / 100000000L);
        dt -= y * 100000000L;
        int m = (int) (dt / 1000000L);
        dt -= m * 1000000L;
        int d = (int) (dt / 10000L);
        dt -= d * 10000L;
        int h = (int) (dt / 100L);
        dt -= h * 100L;
        int min = (int) dt;
        if (ret_type == DT_DATETIME) {
            return getLeadingZero(d, 2) + "/" + getLeadingZero(m, 2) + "/" + y + "  " + getLeadingZero(h, 2) + ":" + getLeadingZero(min, 2);
        } else if (ret_type == DT_DATE) {
            return getLeadingZero(d, 2) + "/" + getLeadingZero(m, 2) + "/" + y;
        } else return getLeadingZero(h, 2) + ":" + getLeadingZero(min, 2);
    }

    public String getDateTimeString(int date, int time) {
        return getDateString(date) + " " + getTimeString(time);
    }

    public int getStartYear() {
        return 1800;
    }

    public static void notImplemented(String source) {
        System.out.println("Not Implemented: " + source);
    }

    public static void notImplemented(java.awt.Component parent, String source) {
        System.out.println("Not Implemented: " + source);
        JOptionPane.showMessageDialog(parent, "Not Implemented: \n" + source);
    }

    public String getLeadingZero(int number, int places) {
        String nn = "" + number;
        while (nn.length() < places) {
            nn = "0" + nn;
        }
        return nn;
    }

    public String getLeadingZero(String number, int places) {
        number = number.trim();
        while (number.length() < places) {
            number = "0" + number;
        }
        return number;
    }

    /**
     * For replacing strings.<br>
     * 
     * @param input   Input String
     * @param replace What to seatch for.
     * @param replacement  What to replace with.
     * 
     * @return Parsed string.
     */
    public String replaceExpression(String input, String replace, String replacement) {
        int idx;
        if ((idx = input.indexOf(replace)) == -1) {
            return input;
        }
        boolean finished = false;
        while (!finished) {
            StringBuffer returning = new StringBuffer();
            while (idx != -1) {
                returning.append(input.substring(0, idx));
                returning.append(replacement);
                input = input.substring(idx + replace.length());
                idx = input.indexOf(replace);
            }
            returning.append(input);
            input = returning.toString();
            if ((idx = returning.indexOf(replace)) == -1) {
                finished = true;
            }
        }
        return input;
    }

    public String parseExpression(String in, String expression, String replace) {
        StringBuffer buffer;
        int idx = in.indexOf(expression);
        if (replace == null) replace = "";
        if (idx == -1) return in;
        buffer = new StringBuffer();
        while (idx != -1) {
            buffer.append(in.substring(0, idx));
            buffer.append(replace);
            in = in.substring(idx + expression.length());
            idx = in.indexOf(expression);
        }
        buffer.append(in);
        return buffer.toString();
    }

    public String parseExpressionFull(String in, String expression, String replace) {
        String buffer;
        int idx = in.indexOf(expression);
        if (replace == null) replace = "";
        if (idx == -1) return in;
        buffer = "";
        if (idx != -1) {
            buffer = in.substring(0, idx) + replace + in.substring(idx + expression.length());
            idx = in.indexOf(expression);
            if (idx != -1) buffer = parseExpressionFull(buffer, expression, replace);
        }
        return buffer;
    }

    public boolean isEmptyOrUnset(String val) {
        if ((val == null) || (val.trim().length() == 0)) {
            return true;
        } else return false;
    }

    public static boolean isFound(String text, String search_str) {
        if ((search_str.trim().length() == 0) || (text.trim().length() == 0)) return true;
        return text.trim().indexOf(search_str.trim()) != -1;
    }

    public String[] splitString(String input, String delimiter) {
        String res[] = null;
        if (!input.contains(delimiter)) {
            res = new String[1];
            res[0] = input;
        } else {
            StringTokenizer strtok = new StringTokenizer(input, delimiter);
            res = new String[strtok.countTokens()];
            int i = 0;
            while (strtok.hasMoreTokens()) {
                res[i] = strtok.nextToken().trim();
                i++;
            }
        }
        return res;
    }

    public GregorianCalendar getGregorianCalendar(Date date) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        return gc;
    }

    public float getFloatValue(Object aValue) {
        float out = 0.0f;
        if (aValue == null) return out;
        if (aValue instanceof Float) {
            try {
                Float f = (Float) aValue;
                out = f.floatValue();
            } catch (Exception ex) {
            }
        } else if (aValue instanceof Double) {
            try {
                Double f = (Double) aValue;
                out = f.floatValue();
            } catch (Exception ex) {
            }
        } else if (aValue instanceof Integer) {
            try {
                Integer f = (Integer) aValue;
                out = f.floatValue();
            } catch (Exception ex) {
            }
        } else if (aValue instanceof String) {
            String s = (String) aValue;
            if (s.length() > 0) {
                try {
                    out = Float.parseFloat(s);
                } catch (Exception ex) {
                }
            }
        }
        return out;
    }

    public int getIntValue(Object aValue) {
        int out = 0;
        if (aValue == null) return out;
        if (aValue instanceof Integer) {
            try {
                Integer i = (Integer) aValue;
                out = i.intValue();
            } catch (Exception ex) {
            }
        } else if (aValue instanceof String) {
            String s = (String) aValue;
            if (s.length() > 0) {
                try {
                    out = Integer.parseInt(s);
                } catch (Exception ex) {
                }
            }
        }
        return out;
    }

    public long getLongValue(Object aValue) {
        long out = 0L;
        if (aValue == null) return out;
        if (aValue instanceof Long) {
            try {
                Long i = (Long) aValue;
                out = i.longValue();
            } catch (Exception ex) {
            }
        } else if (aValue instanceof String) {
            String s = (String) aValue;
            if (s.length() > 0) {
                try {
                    out = Long.parseLong(s);
                } catch (Exception ex) {
                }
            }
        }
        return out;
    }

    public float getFloatValueFromString(String aValue) {
        return this.getFloatValueFromString(aValue, 0.0f);
    }

    public float getFloatValueFromString(String aValue, float def_value) {
        float out = def_value;
        try {
            out = Float.parseFloat(aValue);
        } catch (Exception ex) {
            System.out.println("Error on parsing string to get float [" + aValue + "]:" + ex);
        }
        return out;
    }

    public int getIntValueFromString(String aValue) {
        return this.getIntValueFromString(aValue, 0);
    }

    public int getIntValueFromString(String aValue, int def_value) {
        int out = def_value;
        try {
            out = Integer.parseInt(aValue);
        } catch (Exception ex) {
            System.out.println("Error on parsing string to get int [" + aValue + "]:" + ex);
        }
        return out;
    }

    public long getLongValueFromString(String aValue) {
        return this.getLongValueFromString(aValue, 0L);
    }

    public long getLongValueFromString(String aValue, long def_value) {
        long out = def_value;
        try {
            out = Long.parseLong(aValue);
        } catch (Exception ex) {
            System.out.println("Error on parsing string to get long [" + aValue + "]:" + ex);
        }
        return out;
    }
}
