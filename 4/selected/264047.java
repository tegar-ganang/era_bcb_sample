package com.atech.mobile.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;

public class ATMobileDataAccess extends ATMobileDataAccessAbstract {

    @Override
    public void checkPrerequisites() {
    }

    @Override
    public String getApplicationName() {
        return "Atech-Tools";
    }

    @Override
    public String getImagesRoot() {
        return "/icons/";
    }

    public void loadBackupRestoreCollection() {
    }

    String selectedLF = null;

    String subSelectedLF = null;

    Hashtable<String, String> config_db_values = null;

    public Hashtable<String, String> m_settings = null;

    public static String pathPrefix = ".";

    public Color color_background, color_foreground;

    public boolean printing_plugin_installed = false;

    public ATMobileI18nControl m_i18n = ATMobileI18nControl.getInstance();

    private static ATMobileDataAccess m_da = null;

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

    private Collator m_collator = null;

    public ImageIcon config_icons[] = null;

    Container parent = null;

    /**
     *
     *  This is DataAccess constructor; Since classes use Singleton Pattern,
     *  constructor is protected and can be accessed only with getInstance() 
     *  method.<br><br>
     *
     */
    private ATMobileDataAccess() {
        super(ATMobileI18nControl.getInstance());
    }

    public void initSpecial() {
        loadColors();
    }

    /**
     *
     *  This method returns reference to OmniI18nControl object created, or if no 
     *  object was created yet, it creates one.<br><br>
     *
     *  @return Reference to OmniI18nControl object
     * 
     */
    public static ATMobileDataAccess getInstance() {
        if (m_da == null) m_da = new ATMobileDataAccess();
        return m_da;
    }

    /**
     *  This method sets handle to DataAccess to null and deletes the instance. <br><br>
     */
    public void deleteInstance() {
        m_i18n = null;
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
        } catch (IOException ex) {
            ex.printStackTrace();
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

    public int compareUnicodeStrings(String s1, String s2) {
        return this.m_collator.compare(s1, s2);
    }

    public void loadColors() {
        ColorUIResource cui = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("textText");
        this.color_foreground = new Color(cui.getRed(), cui.getGreen(), cui.getBlue(), cui.getAlpha());
        ColorUIResource cui2 = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("Label.background");
        this.color_background = new Color(cui2.getRed(), cui2.getGreen(), cui2.getBlue(), cui2.getAlpha());
        this.border_line = new LineBorder(this.color_foreground);
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

    public String getLeadingZero(int number, int places) {
        String nn = "" + number;
        while (nn.length() < places) {
            nn = "0" + nn;
        }
        return nn;
    }

    public int getStartYear() {
        return 1800;
    }

    public String[] userTypes = { m_i18n.getMessage("SELECT"), m_i18n.getMessage("USER_NORMAL"), m_i18n.getMessage("USER_WORKER"), m_i18n.getMessage("USER_ADMINISTRATOR"), m_i18n.getMessage("USER_SUPERADMIN") };

    public static void notImplemented(String source) {
        System.out.println("Not Implemented: " + source);
    }

    public static void notImplemented(java.awt.Component parent, String source) {
        System.out.println("Not Implemented: " + source);
        JOptionPane.showMessageDialog(parent, "Not Implemented: \n" + source);
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
        StringBuffer returning = new StringBuffer();
        while (idx != -1) {
            returning.append(input.substring(0, idx));
            returning.append(replacement);
            input = input.substring(idx + replace.length());
            idx = input.indexOf(replace);
        }
        returning.append(input);
        return returning.toString();
    }

    public ArrayList<String> getSelectedParishesIDs() {
        ArrayList<String> l = new ArrayList<String>();
        l.add("62");
        l.add("63");
        return l;
    }

    public ArrayList<String> getSelectedPersonalIDs(int type) {
        ArrayList<String> l = new ArrayList<String>();
        l.add("2");
        l.add("3");
        l.add("4");
        return l;
    }

    public static boolean isFound(String text, String search_str) {
        if ((search_str.trim().length() == 0) || (text.trim().length() == 0)) return true;
        return text.trim().indexOf(search_str.trim()) != -1;
    }
}
