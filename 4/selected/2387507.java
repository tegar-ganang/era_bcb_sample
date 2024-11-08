package com.atech.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import com.atech.graphics.components.JDecimalTextField;
import com.atech.i18n.I18nControlAbstract;

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
public class ATSwingUtils {

    /**
     * The color_background.
     */
    public Color color_background;

    /**
     * The color_foreground.
     */
    public Color color_foreground;

    /**
     * The border_line.
     */
    LineBorder border_line;

    /**
     * The fonts.
     */
    static Font[] fonts;

    private static I18nControlAbstract i18n_control = null;

    /**
     * The available l f_full.
     */
    Hashtable<String, String> availableLF_full = null;

    /**
     * Sets the i18n control.
     * 
     * @param ic the new i18n control
     */
    public static void setI18nControl(I18nControlAbstract ic) {
        ATSwingUtils.i18n_control = ic;
    }

    /**
     * Inits the library.
     */
    public static void initLibrary() {
        loadFonts();
    }

    /**
     * The Constant FONT_BIG_BOLD.
     */
    public static final int FONT_BIG_BOLD = 0;

    /**
     * The Constant FONT_NORMAL.
     */
    public static final int FONT_NORMAL = 1;

    /**
     * The Constant FONT_NORMAL_BOLD.
     */
    public static final int FONT_NORMAL_BOLD = 2;

    /**
     * The Constant FONT_NORMAL_P2.
     */
    public static final int FONT_NORMAL_P2 = 3;

    /**
     * The Constant FONT_NORMAL_BOLD_P2.
     */
    public static final int FONT_NORMAL_BOLD_P2 = 4;

    /**
     * The Constant FONT_NORMAL_BOLD_P2.
     */
    public static final int FONT_NORMAL_SMALLER = 5;

    /**
     * Load fonts.
     */
    public static void loadFonts() {
        if (fonts != null) return;
        JLabel label = new JLabel();
        Font f = label.getFont();
        fonts = new Font[6];
        fonts[0] = f.deriveFont(Font.BOLD, 22);
        fonts[1] = f.deriveFont(Font.PLAIN, 12);
        fonts[2] = f.deriveFont(Font.BOLD, 12);
        fonts[3] = f.deriveFont(Font.PLAIN, 14);
        fonts[4] = f.deriveFont(Font.BOLD, 14);
        fonts[5] = f.deriveFont(Font.PLAIN, 11);
    }

    /**
     * Gets the font.
     * 
     * @param font_id the font_id
     * 
     * @return the font
     */
    public static Font getFont(int font_id) {
        return fonts[font_id];
    }

    /**
     * Returns image. Used for extracting images from JAR files.
     *  
     * @param filename 
     * @param cmp 
     * @return 
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
     * Center j dialog.
     * 
     * @param dialog the dialog
     * @param parent the parent
     */
    public static void centerJDialog(JDialog dialog, Container parent) {
        Rectangle rec = parent.getBounds();
        int x = rec.width / 2;
        x += (rec.x);
        int y = rec.height / 2;
        y += rec.y;
        x -= (dialog.getBounds().width / 2);
        y -= (dialog.getBounds().height / 2);
        dialog.getBounds().x = x;
        dialog.getBounds().y = y;
        dialog.setBounds(x, y, dialog.getBounds().width, dialog.getBounds().height);
    }

    /**
     * Load colors.
     */
    public void loadColors() {
        ColorUIResource cui = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("textText");
        this.color_foreground = new Color(cui.getRed(), cui.getGreen(), cui.getBlue(), cui.getAlpha());
        ColorUIResource cui2 = (ColorUIResource) UIManager.getLookAndFeel().getDefaults().get("Label.background");
        this.color_background = new Color(cui2.getRed(), cui2.getGreen(), cui2.getBlue(), cui2.getAlpha());
        this.border_line = new LineBorder(this.color_foreground);
    }

    /**
     * Gets the months array.
     * 
     * @return the months array
     */
    public String[] getMonthsArray() {
        return null;
    }

    /**
     * Gets the date string.
     * 
     * @param date the date
     * 
     * @return the date string
     */
    public String getDateString(int date) {
        int year = date / 10000;
        int months = date - (year * 10000);
        months = months / 100;
        int days = date - (year * 10000) - (months * 100);
        if (year == 0) {
            return getLeadingZero(days, 2) + "/" + getLeadingZero(months, 2);
        } else return getLeadingZero(days, 2) + "/" + getLeadingZero(months, 2) + "/" + year;
    }

    /**
     * Gets the time string.
     * 
     * @param time the time
     * 
     * @return the time string
     */
    public String getTimeString(int time) {
        int hours = time / 100;
        int min = time - hours * 100;
        return getLeadingZero(hours, 2) + ":" + getLeadingZero(min, 2);
    }

    /**
     * Gets the date time string.
     * 
     * @param date the date
     * 
     * @return the date time string
     */
    public String getDateTimeString(long date) {
        return getDateTimeString(date, 1);
    }

    /**
     * Gets the date time as date string.
     * 
     * @param date the date
     * 
     * @return the date time as date string
     */
    public String getDateTimeAsDateString(long date) {
        return getDateTimeString(date, 2);
    }

    /**
     * The Constant DATE_TIME_ATECH_DATETIME.
     */
    public static final int DATE_TIME_ATECH_DATETIME = 1;

    /**
     * The Constant DATE_TIME_ATECH_DATE.
     */
    public static final int DATE_TIME_ATECH_DATE = 2;

    /**
     * The Constant DATE_TIME_ATECH_TIME.
     */
    public static final int DATE_TIME_ATECH_TIME = 3;

    /**
     * Gets the aT date time from gc.
     * 
     * @param gc the gc
     * @param type the type
     * 
     * @return the aT date time from gc
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
     * Gets the aT date time from parts.
     * 
     * @param day the day
     * @param month the month
     * @param year the year
     * @param hour the hour
     * @param minute the minute
     * @param type the type
     * 
     * @return the aT date time from parts
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
     * Gets the date from at date.
     * 
     * @param data the data
     * 
     * @return the date from at date
     */
    public long getDateFromATDate(long data) {
        int d2 = (int) (data / 10000);
        return d2;
    }

    /**
     * Gets the date time as time string.
     * 
     * @param date the date
     * 
     * @return the date time as time string
     */
    public String getDateTimeAsTimeString(long date) {
        return getDateTimeString(date, 3);
    }

    /**
     * The Constant DT_DATETIME.
     */
    public static final int DT_DATETIME = 1;

    /**
     * The Constant DT_DATE.
     */
    public static final int DT_DATE = 2;

    /**
     * The Constant DT_TIME.
     */
    public static final int DT_TIME = 3;

    /**
     * Gets the date time string.
     * 
     * @param dt the dt
     * @param ret_type the ret_type
     * 
     * @return the date time string
     */
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

    /**
     * Gets the date time string.
     * 
     * @param date the date
     * @param time the time
     * 
     * @return the date time string
     */
    public String getDateTimeString(int date, int time) {
        return getDateString(date) + " " + getTimeString(time);
    }

    /**
     * Gets the leading zero.
     * 
     * @param number the number
     * @param places the places
     * 
     * @return the leading zero
     */
    public String getLeadingZero(int number, int places) {
        String nn = "" + number;
        while (nn.length() < places) {
            nn = "0" + nn;
        }
        return nn;
    }

    /**
     * Gets the start year.
     * 
     * @return the start year
     */
    public int getStartYear() {
        return 1800;
    }

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
     * Creates the menu.
     * 
     * @param name the name
     * @param tool_tip the tool_tip
     * @param bar the bar
     * 
     * @return the j menu
     */
    public static JMenu createMenu(String name, String tool_tip, JMenuBar bar) {
        JMenu item = new JMenu(ATSwingUtils.i18n_control.getMessageWithoutMnemonic(name));
        item.setMnemonic(i18n_control.getMnemonic(name));
        if (tool_tip != null) {
            item.setToolTipText(tool_tip);
        }
        bar.add(item);
        return item;
    }

    public static JMenu createMenu(String name, String tool_tip, JMenuBar bar, I18nControlAbstract ic) {
        return createMenu(name, tool_tip, bar, ic, -1);
    }

    /**
     * Creates the menu.
     * 
     * @param name the name
     * @param tool_tip the tool_tip
     * @param bar the bar
     * @param ic 
     * @param font_id 
     * 
     * @return the j menu
     */
    public static JMenu createMenu(String name, String tool_tip, JMenuBar bar, I18nControlAbstract ic, int font_id) {
        JMenu item = new JMenu(ic.getMessageWithoutMnemonic(name));
        item.setMnemonic(ic.getMnemonic(name));
        if (font_id != -1) item.setFont(getFont(font_id));
        if ((tool_tip != null) && (tool_tip.trim().length() > 0)) {
            item.setToolTipText(ic.getMessage(tool_tip));
        }
        if (bar != null) bar.add(item);
        return item;
    }

    /**
     * Creates the menu.
     * 
     * @param name the name
     * @param tool_tip the tool_tip
     * @param menu 
     * @param bar the bar
     * 
     * @return the j menu
     */
    public static JMenu createMenu(String name, String tool_tip, JMenu menu) {
        return ATSwingUtils.createMenu(name, tool_tip, menu, ATSwingUtils.i18n_control);
    }

    /**
     * Creates the menu.
     * 
     * @param name the name
     * @param tool_tip the tool_tip
     * @param menu 
     * @param ic 
     * @param bar the bar
     * 
     * @return the j menu
     */
    public static JMenu createMenu(String name, String tool_tip, JMenu menu, I18nControlAbstract ic) {
        JMenu item = new JMenu(ic.getMessageWithoutMnemonic(name));
        item.setMnemonic(ic.getMnemonic(name));
        if ((tool_tip != null) && (tool_tip.trim().length() > 0)) {
            item.setToolTipText(ic.getMessage(tool_tip));
        }
        menu.add(item);
        return item;
    }

    /**
     * Creates the menu item.
     * 
     * @param menu the menu
     * @param name the name
     * @param tip the tip
     * @param action_command the action_command
     * @param icon_small the icon_small
     * 
     * @return the j menu item
     */
    public JMenuItem createMenuItem(JMenu menu, String name, String tip, String action_command, String icon_small) {
        JMenuItem mi = new JMenuItem();
        mi.setText(i18n_control.getMessageWithoutMnemonic(name));
        mi.setActionCommand(action_command);
        if (tip != null) {
            mi.setToolTipText(i18n_control.getMessage(tip));
        }
        if (icon_small != null) {
            mi.setIcon(new ImageIcon(getClass().getResource(icon_small)));
        }
        if (menu != null) menu.add(mi);
        return mi;
    }

    /**
     * Gets the numeric text field.
     * 
     * @param columns the columns
     * @param decimal_places the decimal_places
     * @param value the value
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * 
     * @return the numeric text field
     */
    public static JDecimalTextField getNumericTextField(int columns, int decimal_places, Object value, int x, int y, int width, int height, Container cont) {
        JDecimalTextField tf = new JDecimalTextField(value, decimal_places);
        tf.setBounds(x, y, width, height);
        cont.add(tf);
        return tf;
    }

    /**
     * Gets the label.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * 
     * @return the label
     */
    public static JLabel getLabel(String text, int x, int y, int width, int height, Container cont) {
        return getLabel(text, x, y, width, height, cont, null);
    }

    /**
     * Gets the label.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font_id the font_id
     * 
     * @return the label
     */
    public static JLabel getLabel(String text, int x, int y, int width, int height, Container cont, int font_id) {
        return getLabel(text, x, y, width, height, cont, getFont(font_id));
    }

    /**
     * Gets the label.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font the font
     * 
     * @return the label
     */
    public static JLabel getLabel(String text, int x, int y, int width, int height, Container cont, Font font) {
        JLabel label_1 = new JLabel();
        if (text != null) label_1.setText(text);
        label_1.setBounds(x, y, width, height);
        if (font != null) label_1.setFont(font);
        cont.add(label_1);
        return label_1;
    }

    /**
     * Gets the title label.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font_id the font_id
     * 
     * @return the title label
     */
    public static JLabel getTitleLabel(String text, int x, int y, int width, int height, Container cont, int font_id) {
        return getTitleLabel(text, x, y, width, height, cont, getFont(font_id));
    }

    /**
     * Gets the title label.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font the font
     * 
     * @return the title label
     */
    public static JLabel getTitleLabel(String text, int x, int y, int width, int height, Container cont, Font font) {
        JLabel label_1 = getLabel(text, x, y, width, height, cont, font);
        label_1.setHorizontalAlignment(JLabel.CENTER);
        return label_1;
    }

    /**
     * Gets the combo box.
     * 
     * @param data the data
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font_id the font_id
     * 
     * @return the combo box
     */
    public static JComboBox getComboBox(Vector<?> data, int x, int y, int width, int height, Container cont, int font_id) {
        return ATSwingUtils.getComboBox(data, x, y, width, height, cont, getFont(font_id));
    }

    /**
     * Gets the combo box.
     * 
     * @param data the data
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font the font
     * 
     * @return the combo box
     */
    public static JComboBox getComboBox(Vector<?> data, int x, int y, int width, int height, Container cont, Font font) {
        JComboBox cb = new JComboBox(data);
        cb.setBounds(x, y, width, height);
        cb.setFont(font);
        cont.add(cb);
        return cb;
    }

    /**
     * Gets the combo box.
     * 
     * @param data the data
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font_id the font_id
     * 
     * @return the combo box
     */
    public static JComboBox getComboBox(Object[] data, int x, int y, int width, int height, Container cont, int font_id) {
        return ATSwingUtils.getComboBox(data, x, y, width, height, cont, getFont(font_id));
    }

    /**
     * Gets the combo box.
     * 
     * @param data the data
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font the font
     * 
     * @return the combo box
     */
    public static JComboBox getComboBox(Object[] data, int x, int y, int width, int height, Container cont, Font font) {
        JComboBox cb = new JComboBox(data);
        cb.setBounds(x, y, width, height);
        cb.setFont(font);
        cont.add(cb);
        return cb;
    }

    /**
     * Gets the button.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font_id the font_id
     * @param icon_name the icon_name
     * @param action_cmd the action_cmd
     * @param al the al
     * @param da the da
     * 
     * @return the button
     */
    public static JButton getButton(String text, int x, int y, int width, int height, Container cont, int font_id, String icon_name, String action_cmd, ActionListener al, ATDataAccessAbstract da) {
        return ATSwingUtils.getButton(text, x, y, width, height, cont, getFont(font_id), icon_name, action_cmd, al, da);
    }

    /**
     * Gets the button.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font the font
     * @param icon_name the icon_name
     * @param action_cmd the action_cmd
     * @param al the al
     * @param da the da
     * 
     * @return the button
     */
    public static JButton getButton(String text, int x, int y, int width, int height, Container cont, Font font, String icon_name, String action_cmd, ActionListener al, ATDataAccessAbstract da) {
        return ATSwingUtils.getButton(text, x, y, width, height, cont, font, icon_name, action_cmd, al, da, null);
    }

    public static JButton getIconButton(int x, int y, int width, int height, String tooltip, String icon_name, int icon_width, int icon_height, String action_cmd, ActionListener al, Container cont, ATDataAccessAbstract da) {
        JButton button = new JButton(da.getImageIcon(icon_name, icon_width, icon_height, cont));
        button.addActionListener(al);
        button.setActionCommand(action_cmd);
        button.setToolTipText(tooltip);
        button.setBounds(x, y, width, height);
        cont.add(button, null);
        return button;
    }

    /**
     * @param text
     * @param x
     * @param y
     * @param width
     * @param height
     * @param cont
     * @param font_id
     * @param icon_name
     * @param action_cmd
     * @param al
     * @param da
     * @param icon_size
     * @return
     */
    public static JButton getButton(String text, int x, int y, int width, int height, Container cont, int font_id, String icon_name, String action_cmd, ActionListener al, ATDataAccessAbstract da, int[] icon_size) {
        return ATSwingUtils.getButton(text, x, y, width, height, cont, getFont(font_id), icon_name, action_cmd, al, da, icon_size);
    }

    /**
     * Gets the button.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font the font
     * @param icon_name the icon_name
     * @param action_cmd the action_cmd
     * @param al the al
     * @param da the da
     * @param icon_size 
     * 
     * @return the button
     */
    public static JButton getButton(String text, int x, int y, int width, int height, Container cont, Font font, String icon_name, String action_cmd, ActionListener al, ATDataAccessAbstract da, int[] icon_size) {
        JButton button = null;
        if ((text == null) || (text.trim().length() == 0)) button = new JButton(); else button = new JButton(text);
        if (icon_name != null) {
            if (icon_size == null) button.setIcon(da.getImageIcon_22x22(icon_name, cont)); else button.setIcon(da.getImageIcon(icon_name, icon_size[0], icon_size[1], cont));
        }
        button.setActionCommand(action_cmd);
        button.setFont(font);
        button.setBounds(x, y, width, height);
        button.addActionListener(al);
        cont.add(button);
        return button;
    }

    /**
     * Gets the check box.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font_id the font_id
     * 
     * @return the check box
     */
    public static JCheckBox getCheckBox(String text, int x, int y, int width, int height, Container cont, int font_id) {
        return ATSwingUtils.getCheckBox(text, x, y, width, height, cont, getFont(font_id));
    }

    /**
     * Gets the check box.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font the font
     * 
     * @return the check box
     */
    public static JCheckBox getCheckBox(String text, int x, int y, int width, int height, Container cont, Font font) {
        JCheckBox chb = new JCheckBox(text);
        chb.setBounds(x, y, width, height);
        chb.setFont(font);
        chb.setSelected(false);
        cont.add(chb);
        return chb;
    }

    /**
     * Gets the panel.
     * 
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param layout the layout
     * @param border the border
     * @param cont the cont
     * 
     * @return the panel
     */
    public static JPanel getPanel(int x, int y, int width, int height, LayoutManager layout, Border border, Container cont) {
        JPanel panel = new JPanel();
        panel.setLayout(layout);
        panel.setBorder(border);
        panel.setBounds(x, y, width, height);
        if (cont != null) cont.add(panel);
        return panel;
    }

    /**
     * Gets the text field.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * 
     * @return the text field
     */
    public static JTextField getTextField(String text, int x, int y, int width, int height, Container cont) {
        JTextField text_1 = new JTextField();
        if (text != null) text_1.setText(text);
        text_1.setBounds(x, y, width, height);
        cont.add(text_1);
        return text_1;
    }

    /**
     * Gets the text field.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font_id font id
     * 
     * @return the text field
     */
    public static JTextField getTextField(String text, int x, int y, int width, int height, Container cont, int font_id) {
        return getTextField(text, x, y, width, height, cont, getFont(font_id));
    }

    /**
     * Gets the text field.
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * @param font font instance
     * 
     * @return the text field
     */
    public static JTextField getTextField(String text, int x, int y, int width, int height, Container cont, Font font) {
        JTextField text_1 = new JTextField();
        if (text != null) text_1.setText(text);
        text_1.setBounds(x, y, width, height);
        if (font != null) text_1.setFont(font);
        cont.add(text_1);
        return text_1;
    }

    /**
     * Get Scroll Pane
     * 
     * @param element component to add to scroll pane
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont container
     * 
     * @return the scroll pane
     */
    public static JScrollPane getScrollPane(Component element, int x, int y, int width, int height, Container cont) {
        JScrollPane scr2 = new JScrollPane(element);
        scr2.setBounds(x, y, width, height);
        cont.add(scr2);
        return scr2;
    }

    /**
     * Adds the component.
     * 
     * @param comp the comp
     * @param posX the pos x
     * @param posY the pos y
     * @param width the width
     * @param height the height
     * @param parent the parent
     */
    public static void addComponent(JComponent comp, int posX, int posY, int width, int height, JPanel parent) {
        addComponent(comp, posX, posY, width, height, FONT_NORMAL, parent);
    }

    /**
     * Adds the component.
     * 
     * @param comp the comp
     * @param posX the pos x
     * @param posY the pos y
     * @param width the width
     * @param height the height
     * @param font_id the font_id
     * @param parent the parent
     */
    public static void addComponent(JComponent comp, int posX, int posY, int width, int height, int font_id, JPanel parent) {
        comp.setBounds(posX, posY, width, height);
        comp.setFont(getFont(font_id));
        parent.add(comp);
    }

    /**
     * Create Menu
     * 
     * @param name
     * @param tool_tip
     * @param ic
     * @return
     */
    public static JMenu createMenu(String name, String tool_tip, I18nControlAbstract ic) {
        JMenu item = new JMenu(ic.getMessageWithoutMnemonic(name));
        item.setMnemonic(ic.getMnemonic(name));
        if (tool_tip != null) {
            item.setToolTipText(ic.getMessage(tool_tip));
        }
        return item;
    }

    public static JMenuItem createMenuItem(JMenu menu, String name, String tip, String action_command, ActionListener al, String icon_small, I18nControlAbstract ic, ATDataAccessAbstract da, Container c) {
        return createMenuItem(menu, name, tip, action_command, al, icon_small, ic, da, c, -1);
    }

    /**
     * Create Menu Item 
     * 
     * @param menu
     * @param name
     * @param tip
     * @param action_command
     * @param al
     * @param icon_small
     * @param ic
     * @param da
     * @param c
     * 
     * @return 
     */
    public static JMenuItem createMenuItem(JMenu menu, String name, String tip, String action_command, ActionListener al, String icon_small, I18nControlAbstract ic, ATDataAccessAbstract da, Container c, int font_id) {
        JMenuItem mi = new JMenuItem(ic.getMessageWithoutMnemonic(name));
        mi.setMnemonic(ic.getMnemonic(name));
        mi.setActionCommand(action_command);
        mi.addActionListener(al);
        if (font_id != -1) mi.setFont(getFont(font_id));
        if (tip != null) {
            mi.setToolTipText(ic.getMessage(tip));
        }
        if (icon_small != null) {
            mi.setIcon(da.getImageIcon(icon_small, 15, 15, c));
        }
        if (menu != null) menu.add(mi);
        return mi;
    }

    /**
     * Gets the numeric text field.
     * 
     * @param value 
     * @param min 
     * @param max 
     * @param step 
     * @param x 
     * @param y 
     * @param width 
     * @param height 
     * @param cont 
     * 
     * 
     * @return the numeric text field
     */
    public static JSpinner getJSpinner(float value, int min, int max, float step, int x, int y, int width, int height, Container cont) {
        JSpinner sp = new JSpinner();
        sp.setBounds(x, y, width, height);
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
        sp.setModel(model);
        cont.add(sp);
        return sp;
    }

    /**
     * Gets the numeric text field.
     * 
     * @param value 
     * @param min 
     * @param max 
     * @param step 
     * @param x 
     * @param y 
     * @param width 
     * @param height 
     * @param cont 
     * 
     * 
     * @return the numeric text field
     */
    public static JSpinner getJSpinner(float value, float min, float max, float step, int x, int y, int width, int height, Container cont) {
        JSpinner sp = new JSpinner();
        sp.setBounds(x, y, width, height);
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
        sp.setModel(model);
        cont.add(sp);
        return sp;
    }

    /**
     * Show Error Dialog
     * 
     * @param cont
     * @param message
     * @param ic
     */
    public static void showErrorDialog(Container cont, String message, I18nControlAbstract ic) {
        JOptionPane.showMessageDialog(cont, ic.getMessage(message), ic.getMessage("ERROR"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show Warning Dialog
     * 
     * @param cont
     * @param message
     * @param ic
     */
    public static void showWarningDialog(Container cont, String message, I18nControlAbstract ic) {
        JOptionPane.showMessageDialog(cont, ic.getMessage(message), ic.getMessage("WARNING"), JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Gets the text area (is JScrollPane)
     * 
     * @param text the text
     * @param x the x
     * @param y the y
     * @param width the width
     * @param height the height
     * @param cont the cont
     * 
     * @return the text field
     */
    public static JTextArea getTextArea(String text, int x, int y, int width, int height, Container cont) {
        JTextArea jta = new JTextArea();
        if (text != null) jta.setText(text);
        JScrollPane scp = new JScrollPane(jta);
        scp.setBounds(x, y, width, height);
        cont.add(scp);
        return jta;
    }
}
