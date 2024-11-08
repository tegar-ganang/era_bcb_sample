package org.columba.core.gui.base;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * taken some code from the Kiwi Toolkit: http://www.dystance.net/ping/kiwi/
 * author: Mark Lindner
 * 
 */
public class DateChooser extends JPanel implements ActionListener {

    private static final int cellSize = 22;

    private static final int[] daysInMonth = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private static final int[] daysInMonthLeap = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private static final Color weekendColor = Color.red.darker();

    /** <i>Date changed</i> event command. */
    public static final String DATE_CHANGE_CMD = "dateChanged";

    /** <i>Month changed</i> event command. */
    public static final String MONTH_CHANGE_CMD = "monthChanged";

    /** <i>Year changed</i> event command. */
    public static final String YEAR_CHANGE_CMD = "yearChanged";

    CalendarPane calendarPane;

    private JLabel l_date;

    private JLabel l_month;

    private JButton b_lmonth;

    private JButton b_rmonth;

    private SimpleDateFormat datefmt = new SimpleDateFormat("E  d MMM yyyy");

    private Calendar selectedDate = null;

    private Calendar minDate = null;

    private Calendar maxDate = null;

    private int selectedDay;

    private int firstDay;

    private int minDay = -1;

    private int maxDay = -1;

    private String[] months;

    private String[] labels = new String[7];

    private Color highlightColor;

    private Color disabledColor;

    private boolean clipMin = false;

    private boolean clipMax = false;

    private boolean clipAllMin = false;

    private boolean clipAllMax = false;

    private int[] weekendCols = { 0, 0 };

    /**
	 * Construct a new <code>DateChooser</code>. The selection will be
	 * initialized to the current date.
	 */
    public DateChooser() {
        this(Calendar.getInstance());
    }

    /**
	 * Construct a new <code>DateChooser</code> with the specified selected
	 * date.
	 * 
	 * @param <code>date</code> The date for the selection.
	 */
    public DateChooser(Calendar date) {
        DateFormatSymbols sym = new DateFormatSymbols();
        months = sym.getShortMonths();
        String[] wkd = sym.getShortWeekdays();
        for (int i = 0; i < 7; i++) {
            int l = Math.min(wkd[i + 1].length(), 2);
            labels[i] = wkd[i + 1].substring(0, l);
        }
        highlightColor = UIManager.getColor("List.selectionBackground");
        disabledColor = Color.red;
        setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BorderLayout(5, 5));
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout(0, 0));
        top.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JPanel p1 = new JPanel();
        p1.setLayout(new BorderLayout());
        top.add(p1, BorderLayout.CENTER);
        b_lmonth = new JButton("<");
        b_lmonth.addActionListener(this);
        b_lmonth.setMargin(new Insets(0, 0, 0, 0));
        p1.add(b_lmonth, BorderLayout.WEST);
        l_month = new JLabel();
        l_date = new JLabel("Date");
        l_date.setAlignmentX(0);
        p1.add(l_date, BorderLayout.CENTER);
        b_rmonth = new JButton(">");
        b_rmonth.addActionListener(this);
        b_rmonth.setMargin(new Insets(0, 0, 0, 0));
        p1.add(b_rmonth, BorderLayout.EAST);
        add("North", top);
        calendarPane = new CalendarPane();
        calendarPane.setOpaque(false);
        add("Center", calendarPane);
        int fd = date.getFirstDayOfWeek();
        weekendCols[0] = (Calendar.SUNDAY - fd + 7) % 7;
        weekendCols[1] = (Calendar.SATURDAY - fd + 7) % 7;
        setSelectedDate(date);
    }

    public static boolean isLeapYear(int year) {
        return ((((year % 4) == 0) && ((year % 100) != 0)) || ((year % 400) == 0));
    }

    private Calendar copyDate(Calendar source, Calendar dest) {
        if (dest == null) {
            dest = Calendar.getInstance();
        }
        dest.set(Calendar.YEAR, source.get(Calendar.YEAR));
        dest.set(Calendar.MONTH, source.get(Calendar.MONTH));
        dest.set(Calendar.DATE, source.get(Calendar.DATE));
        return (dest);
    }

    /**
	 * Add a <code>ActionListener</code> to this component's list of
	 * listeners.
	 * 
	 * @param listener
	 *            The listener to add.
	 */
    public void addActionListener(ActionListener listener) {
    }

    /**
	 * Remove a <code>ActionListener</code> from this component's list of
	 * listeners.
	 * 
	 * @param listener
	 *            The listener to remove.
	 */
    public void removeActionListener(ActionListener listener) {
    }

    /**
	 * Set the highlight color for this component.
	 * 
	 * @param color
	 *            The new highlight color.
	 */
    public void setHighlightColor(Color color) {
        highlightColor = color;
    }

    /**
	 * Get the highlight color for this component.
	 * 
	 * @return The current highlight color.
	 */
    public Color getHighlightColor() {
        return (highlightColor);
    }

    /**
	 * Get a copy of the <code>Calendar</code> object that represents the
	 * currently selected date.
	 * 
	 * @return The currently selected date.
	 */
    public Calendar getSelectedDate() {
        return ((Calendar) selectedDate.clone());
    }

    /**
	 * Set the selected date for the chooser.
	 * 
	 * @param date
	 *            The date to select.
	 */
    public void setSelectedDate(Calendar date) {
        selectedDate = copyDate(date, selectedDate);
        selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH);
        _refresh();
    }

    /**
	 * Set the earliest selectable date for the chooser.
	 * 
	 * @param date
	 *            The (possibly <code>null</code>) minimum selectable date.
	 */
    public void setMinimumDate(Calendar date) {
        minDate = ((date == null) ? null : copyDate(date, minDate));
        minDay = ((date == null) ? (-1) : minDate.get(Calendar.DATE));
        _refresh();
    }

    /**
	 * Get the earliest selectable date for the chooser.
	 * 
	 * @return The minimum selectable date, or <code>null</code> if there is
	 *         no minimum date currently set.
	 */
    public Calendar getMinimumDate() {
        return (minDate);
    }

    /**
	 * Set the latest selectable date for the chooser.
	 * 
	 * @param date
	 *            The (possibly <code>null</code>) maximum selectable date.
	 */
    public void setMaximumDate(Calendar date) {
        maxDate = ((date == null) ? null : copyDate(date, maxDate));
        maxDay = ((date == null) ? (-1) : maxDate.get(Calendar.DATE));
        _refresh();
    }

    /**
	 * Get the latest selectable date for the chooser.
	 * 
	 * @return The maximum selectable date, or <code>null</code> if there is
	 *         no maximum date currently set.
	 */
    public Calendar getMaximumDate() {
        return (maxDate);
    }

    /**
	 * Set the format for the textual date display at the bottom of the
	 * component.
	 * 
	 * @param <code>format</code> The new date format to use.
	 */
    public void setDateFormat(SimpleDateFormat format) {
        datefmt = format;
        _refresh();
    }

    /** Handle events. This method is public as an implementation side-effect. */
    public void actionPerformed(ActionEvent evt) {
        Object o = evt.getSource();
        if (o == b_lmonth) {
            selectedDate.add(Calendar.MONTH, -1);
        } else if (o == b_rmonth) {
            selectedDate.add(Calendar.MONTH, 1);
        }
        selectedDay = 1;
        selectedDate.set(Calendar.DATE, selectedDay);
        _refresh();
    }

    private void _computeFirstDay() {
        int d = selectedDate.get(Calendar.DAY_OF_MONTH);
        selectedDate.set(Calendar.DAY_OF_MONTH, 1);
        firstDay = selectedDate.get(Calendar.DAY_OF_WEEK);
        selectedDate.set(Calendar.DAY_OF_MONTH, d);
    }

    private void _refresh() {
        l_date.setText(datefmt.format(selectedDate.getTime()));
        l_month.setText(months[selectedDate.get(Calendar.MONTH)]);
        _computeFirstDay();
        clipMin = clipMax = clipAllMin = clipAllMax = false;
        b_lmonth.setEnabled(true);
        b_rmonth.setEnabled(true);
        if (minDate != null) {
            int y = selectedDate.get(Calendar.YEAR);
            int y0 = minDate.get(Calendar.YEAR);
            int m = selectedDate.get(Calendar.MONTH);
            int m0 = minDate.get(Calendar.MONTH);
            if (y == y0) {
                b_lmonth.setEnabled(m > m0);
                if (m == m0) {
                    clipMin = true;
                    int d0 = minDate.get(Calendar.DATE);
                    if (selectedDay < d0) {
                        selectedDate.set(Calendar.DATE, selectedDay = d0);
                    }
                }
            }
            clipAllMin = ((m < m0) || (y < y0));
        }
        if (maxDate != null) {
            int y = selectedDate.get(Calendar.YEAR);
            int y1 = maxDate.get(Calendar.YEAR);
            int m = selectedDate.get(Calendar.MONTH);
            int m1 = maxDate.get(Calendar.MONTH);
            if (y == y1) {
                b_rmonth.setEnabled(m < m1);
                if (m == m1) {
                    clipMax = true;
                    int d1 = maxDate.get(Calendar.DATE);
                    if (selectedDay > d1) {
                        selectedDate.set(Calendar.DATE, selectedDay = d1);
                    }
                }
            }
            clipAllMax = ((m > m1) || (y > y1));
        }
        calendarPane.repaint();
    }

    private class CalendarPane extends JComponent {

        private int dp = 0;

        private int x0 = 0;

        private int y0 = 0;

        /** Construct a new <code>CalendarView</code>. */
        CalendarPane() {
            addMouseListener(new _MouseListener2());
        }

        /** Paint the component. */
        public void paint(Graphics gc) {
            gc.setFont(UIManager.getFont("Label.font"));
            FontMetrics fm = gc.getFontMetrics();
            Insets ins = getInsets();
            int h = fm.getMaxAscent();
            gc.setColor(Color.white);
            gc.fillRect(0, 0, getSize().width, getSize().height);
            dp = ((firstDay - selectedDate.getFirstDayOfWeek() + 7) % 7);
            int x = dp;
            int y = 0;
            y0 = ((getSize().height - getPreferredSize().height) / 2);
            int yp = y0;
            x0 = ((getSize().width - getPreferredSize().width) / 2);
            int xp = x0;
            paintBorder(gc);
            gc.setColor(Color.black);
            gc.clipRect(ins.left, ins.top, (getSize().width - ins.left - ins.right), (getSize().height - ins.top - ins.bottom));
            gc.translate(ins.left, ins.top);
            for (int i = 0, ii = selectedDate.getFirstDayOfWeek() - 1; i < 7; i++) {
                gc.drawString(labels[ii], xp + 5 + (i * (cellSize + 2)), yp + h);
                if (++ii == 7) {
                    ii = 0;
                }
            }
            yp += 20;
            xp += (dp * (cellSize + 2));
            int month = DateChooser.this.selectedDate.get(Calendar.MONTH);
            int dmax = (isLeapYear(DateChooser.this.selectedDate.get(Calendar.YEAR)) ? daysInMonthLeap[month] : daysInMonth[month]);
            for (int d = 1; d <= dmax; d++) {
                gc.setColor(Color.gray);
                if (d == selectedDay) {
                    gc.setColor(highlightColor);
                    gc.fillRect(xp + 1, yp + 1, cellSize - 2, cellSize - 2);
                }
                if ((clipMin && (d < minDay)) || (clipMax && (d > maxDay)) || clipAllMin || clipAllMax) {
                    gc.setColor(disabledColor);
                } else {
                    gc.setColor(((weekendCols[0] == x) || (weekendCols[1] == x)) ? weekendColor : Color.black);
                }
                String ss = String.valueOf(d);
                int sw = fm.stringWidth(ss);
                if (d == selectedDay) {
                    gc.setColor(UIManager.getColor("List.selectionForeground"));
                }
                gc.drawString(ss, xp - 3 + (cellSize - sw), yp + 3 + h);
                if (++x == 7) {
                    x = 0;
                    xp = x0;
                    y++;
                    yp += (cellSize + 2);
                } else {
                    xp += (cellSize + 2);
                }
            }
        }

        public Dimension getPreferredSize() {
            Insets ins = getInsets();
            return (new Dimension((((cellSize + 2) * 7) + ins.left + ins.right), (((cellSize + 2) * 6) + 20) + ins.top + ins.bottom));
        }

        public Dimension getMinimumSize() {
            return (getPreferredSize());
        }

        private int getDay(MouseEvent evt) {
            Insets ins = getInsets();
            int x = evt.getX() - ins.left - x0;
            int y = evt.getY() - ins.top - 20 - y0;
            int maxw = (cellSize + 2) * 7;
            int maxh = (cellSize + 2) * 6;
            if ((x < 0) || (x > maxw) || (y < 0) || (y > maxh)) {
                return (-1);
            }
            y /= (cellSize + 2);
            x /= (cellSize + 2);
            int d = ((7 * y) + x) - (dp - 1);
            if ((d < 1) || (d > selectedDate.getMaximum(Calendar.DAY_OF_MONTH))) {
                return (-1);
            }
            if ((clipMin && (d < minDay)) || (clipMax && (d > maxDay))) {
                return (-1);
            }
            return (d);
        }

        private class _MouseListener2 extends MouseAdapter {

            public void mouseReleased(MouseEvent evt) {
                int d = getDay(evt);
                if (d < 0) {
                    return;
                }
                selectedDay = d;
                selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay);
                _refresh();
            }
        }
    }
}
