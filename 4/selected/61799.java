package com.atech.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import com.atech.db.hibernate.HibernateDb;

/**
 *  This file is part of ATech Tools library.
 *  
 *  <one line to give the library's name and a brief idea of what it does.>
 *  Copyright (C) 2007  Andy (Aleksander) Rozman (Atech-Software)
 *  
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
 *  
 *  
 *  For additional information about this project please visit our project site on 
 *  http://atech-tools.sourceforge.net/ or contact us via this emails: 
 *  andyrozman@users.sourceforge.net or andy@atech-software.com
 *  
 *  @author Andy
 *
*/
public class ATDataAccess extends ATDataAccessAbstract {

    /** 
     * checkPrerequisites
     */
    @Override
    public void checkPrerequisites() {
    }

    /** 
     * getApplicationName
     */
    @Override
    public String getApplicationName() {
        return "Atech-Tools";
    }

    /** 
     * getImagesRoot
     */
    @Override
    public String getImagesRoot() {
        return "/icons/";
    }

    /** 
     * loadBackupRestoreCollection
     */
    @Override
    public void loadBackupRestoreCollection() {
    }

    /**
     * The m_settings.
     */
    public Hashtable<String, String> m_settings = null;

    /**
     * The printing_plugin_installed.
     */
    public boolean printing_plugin_installed = false;

    private static ATDataAccess m_da = null;

    /**
                                              * The Constant DB_NOT_LOADED.
                                              */
    public static final int DB_NOT_LOADED = 0;

    /**
     * The Constant DB_BASE.
     */
    public static final int DB_BASE = 1;

    /**
     * The Constant DB_DIOCESE.
     */
    public static final int DB_DIOCESE = 2;

    /**
     * The Constant DB_DIOCESE_PERSONAL.
     */
    public static final int DB_DIOCESE_PERSONAL = 3;

    /**
     * The Constant DB_LOAD_COMPLETE.
     */
    public static final int DB_LOAD_COMPLETE = 10;

    private int db_loading_status = 0;

    /**
     * The yes_no_combo.
     */
    public Object[] yes_no_combo = null;

    /**
     * The config_icons.
     */
    public ImageIcon config_icons[] = null;

    /**
 * The gender_minus.
 */
    public String gender_minus[] = { m_i18n.getMessage("GENDER_M"), m_i18n.getMessage("GENDER_F") };

    /**
     * The gender.
     */
    public String gender[] = { m_i18n.getMessage("SELECT"), m_i18n.getMessage("GENDER_M"), m_i18n.getMessage("GENDER_F") };

    /**
     * The contact_types.
     */
    public String contact_types[] = { m_i18n.getMessage("SELECT"), m_i18n.getMessage("PHONE"), m_i18n.getMessage("GSM"), m_i18n.getMessage("FAX"), m_i18n.getMessage("EMAIL"), m_i18n.getMessage("WEB_PAGE"), m_i18n.getMessage("ICQ_MSNG"), m_i18n.getMessage("YAHOO_MSNG"), m_i18n.getMessage("AIM_MSNG"), m_i18n.getMessage("JABBER_MSNG"), m_i18n.getMessage("MSN_MSNG"), m_i18n.getMessage("SKYPE_MSNG"), m_i18n.getMessage("OTHER") };

    /**
     * The contact_icons.
     */
    public ImageIcon contact_icons[] = { null, new ImageIcon("images/c_phone.gif"), new ImageIcon("images/c_GSM.gif"), new ImageIcon("images/c_fax.gif"), new ImageIcon("images/c_email.gif"), new ImageIcon("images/c_hp.gif"), new ImageIcon("images/c_icq.gif"), new ImageIcon("images/c_yahoo.gif"), new ImageIcon("images/c_aim.gif"), new ImageIcon("images/c_jabber.gif"), new ImageIcon("images/c_msn.gif"), null, null };

    /**
     * The mass_status.
     */
    public ImageIcon mass_status[] = { new ImageIcon("images/dot_green.gif"), new ImageIcon("images/dot_orange.gif"), new ImageIcon("images/dot_blue.gif"), new ImageIcon("images/dot_red.gif") };

    /**
     * The config_types.
     */
    public String config_types[] = { m_i18n.getMessage("DB_SETTINGS"), m_i18n.getMessage("LOOK"), m_i18n.getMessage("MY_PARISHES"), m_i18n.getMessage("MASSES"), m_i18n.getMessage("USERS"), m_i18n.getMessage("LANGUAGE") };

    /**
     *
     *  This is DataAccess constructor; Since classes use Singleton Pattern,
     *  constructor is protected and can be accessed only with getInstance() 
     *  method.<br><br>
     *
     */
    private ATDataAccess() {
        super(ATI18nControl.getInstance());
    }

    /** 
     * initSpecial
     */
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
    public static ATDataAccess getInstance() {
        if (m_da == null) m_da = new ATDataAccess();
        return m_da;
    }

    /** 
     * getHibernateDb
     */
    public HibernateDb getHibernateDb() {
        return null;
    }

    /**
     *  This method sets handle to DataAccess to null and deletes the instance. <br><br>
     */
    public void deleteInstance() {
        m_i18n = null;
    }

    /** 
     * setDbLoadingStatus
     */
    public void setDbLoadingStatus(int status) {
        this.db_loading_status = status;
    }

    /** 
     * getDbLoadingStatus
     */
    public int getDbLoadingStatus() {
        return this.db_loading_status;
    }

    /** 
     * isDbLoadedForStatus
     */
    public boolean isDbLoadedForStatus(int status) {
        if ((this.db_loading_status == status) || (this.db_loading_status > status)) return true; else return false;
    }

    /** 
     * getOpenedDialog
     */
    public JDialog getOpenedDialog() {
        return this.m_dialog;
    }

    /** 
     * setOpenedDialog
     */
    public void setOpenedDialog(JDialog dialog) {
        this.m_dialog = dialog;
    }

    /** 
     * getFont
     */
    public Font getFont(int font_id) {
        return fonts[font_id];
    }

    /** 
     * getImage
     */
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

    /** 
     * setParent
     */
    public void setParent(Container component) {
        this.parent = component;
    }

    /** 
     * getParent
     */
    public Container getParent() {
        return this.parent;
    }

    /** 
     * loadColors
     */
    public void loadColors() {
        ColorUIResource cui = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("textText");
        this.color_foreground = new Color(cui.getRed(), cui.getGreen(), cui.getBlue(), cui.getAlpha());
        ColorUIResource cui2 = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("Label.background");
        this.color_background = new Color(cui2.getRed(), cui2.getGreen(), cui2.getBlue(), cui2.getAlpha());
        this.border_line = new LineBorder(this.color_foreground);
    }

    /** 
     * getMonthsArray
     */
    public String[] getMonthsArray() {
        return this.months;
    }

    /** 
     * getATDateTimeFromGC
     */
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

    /** 
     * getATDateTimeFromParts
     */
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

    /** 
     * getDateFromATDate
     */
    public long getDateFromATDate(long data) {
        int d2 = (int) (data / 10000);
        return d2;
    }

    /** 
 * getDateTimeString
 */
    public String getDateTimeString(int date, int time) {
        return getDateString(date) + " " + getTimeString(time);
    }

    /** 
     * getStartYear
     */
    public int getStartYear() {
        return 1800;
    }

    /**
     * Gets the gender combo.
     * 
     * @return the gender combo
     */
    public Object[] getGenderCombo() {
        return gender;
    }

    /**
 * The user types.
 */
    public String[] userTypes = { m_i18n.getMessage("SELECT"), m_i18n.getMessage("USER_NORMAL"), m_i18n.getMessage("USER_WORKER"), m_i18n.getMessage("USER_ADMINISTRATOR"), m_i18n.getMessage("USER_SUPERADMIN") };

    /**
     * Not implemented.
     * 
     * @param source the source
     */
    public static void notImplemented(String source) {
        System.out.println("Not Implemented: " + source);
    }

    /**
     * Not implemented.
     * 
     * @param parent the parent
     * @param source the source
     */
    public static void notImplemented(java.awt.Component parent, String source) {
        System.out.println("Not Implemented: " + source);
        JOptionPane.showMessageDialog(parent, "Not Implemented: \n" + source);
    }

    /**
     * Checks if is found.
     * 
     * @param text the text
     * @param search_str the search_str
     * 
     * @return true, if is found
     */
    public static boolean isFound(String text, String search_str) {
        if ((search_str.trim().length() == 0) || (text.trim().length() == 0)) return true;
        return text.trim().indexOf(search_str.trim()) != -1;
    }

    /**
     * Load Graph Config Properties
     * 
     * @see com.atech.utils.ATDataAccessAbstract#loadGraphConfigProperties()
     */
    public void loadGraphConfigProperties() {
    }

    /** 
     * loadSpecialParameters
     */
    @Override
    public void loadSpecialParameters() {
    }

    /**
     * This method is intended to load additional Language info. Either special langauge configuration
     * or special data required for real Locale handling.
     */
    @Override
    public void loadLanguageInfo() {
    }

    /** 
     * getSelectedLangIndex
     */
    @Override
    public int getSelectedLangIndex() {
        return 0;
    }

    /** 
     * setSelectedLangIndex
     */
    @Override
    public void setSelectedLangIndex(int index) {
    }

    /** 
     * loadPlugIns
     */
    @Override
    public void loadPlugIns() {
    }

    /**
     * Get Max Decimals that will be used by DecimalHandler
     * 
     * @return
     */
    public int getMaxDecimalsUsedByDecimalHandler() {
        return 1;
    }
}
