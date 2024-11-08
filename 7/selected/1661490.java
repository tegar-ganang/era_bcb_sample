package swisseph;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;

/**
* This class is a date class specialized for the use with the swisseph
* package. You will like to use it, if you need a Julian Day number or
* the deltaT for a date or a Julian Day or if like to convert from Gregorian
* to Julian calendar system or vice versa.<P>
* This is a port of the SwissEphemeris package to Java. See
* <A HREF="http://www.astro.ch">Astrodienst Z&uuml;rich</A>
* for more infos and the original authors.
* <P><I><B>You will find the complete documentation for the original
* SwissEphemeris package at <A HREF="http://www.astro.ch/swisseph/sweph_g.htm">
* http://www.astro.ch/swisseph/sweph_g.htm</A>. By far most of the information 
* there is directly valid for this port to Java as well.</B></I>
* @author Thomas Mack / mack@ifis.cs.tu-bs.de
* @version 1.0.0c
*/
public class SweDate {

    private static SwissEph sw = new SwissEph();

    /**
  * Constant for weekdays. SUNDAY is equal to 0.
  */
    public static final int SUNDAY = 0;

    /**
  * Constant for weekdays. MONDAY is equal to 1.
  */
    public static final int MONDAY = 1;

    /**
  * Constant for weekdays. TUESDAY is equal to 2.
  */
    public static final int TUESDAY = 2;

    /**
  * Constant for weekdays. WEDNESDAY is equal to 3.
  */
    public static final int WEDNESDAY = 3;

    /**
  * Constant for weekdays. THURSDAY is equal to 4.
  */
    public static final int THURSDAY = 4;

    /**
  * Constant for weekdays. FRIDAY is equal to 5.
  */
    public static final int FRIDAY = 5;

    /**
  * Constant for weekdays. SATURDAY is equal to 6.
  */
    public static final int SATURDAY = 6;

    public static final boolean SE_JUL_CAL = false;

    public static final boolean SE_GREG_CAL = true;

    public static final boolean SE_KEEP_DATE = true;

    public static final boolean SE_KEEP_JD = false;

    /**
  * Tidal acceleration value in the mean motion of the moon of DE403 (-25.8).
  */
    public static final double SE_TIDAL_DE403 = -25.8;

    /**
  * Tidal acceleration value in the mean motion of the moon of DE404 (-25.8).
  */
    public static final double SE_TIDAL_DE404 = -25.8;

    /**
  * Tidal acceleration value in the mean motion of the moon of DE405 (-25.7376).
  */
    public static final double SE_TIDAL_DE405 = -25.7376;

    /**
  * Tidal acceleration value in the mean motion of the moon of DE406 (-25.7376).
  */
    public static final double SE_TIDAL_DE406 = -25.7376;

    /**
  * Tidal acceleration value in the mean motion of the moon of DE200 (-23.8946).
  */
    public static final double SE_TIDAL_DE200 = -23.8946;

    /**
  * Tidal acceleration value in the mean motion of the moon of -26.
  */
    public static final double SE_TIDAL_26 = -26.0;

    /**
  * Default tidal acceleration value in the mean motion of the moon (=SE_TIDAL_DE406).
  * @see #SE_TIDAL_DE406
  */
    public static final double SE_TIDAL_DEFAULT = SE_TIDAL_DE406;

    /**
  * The Julian day number of 1970 January 1.0. Useful for conversions
  * from or to a Date object.
  * @see #getDate(long)
  */
    public static final double JD0 = 2440587.5;

    private double tid_acc = SE_TIDAL_DEFAULT;

    private static boolean init_dt_done = false;

    private double jd;

    private double jdCO = 2299160.5;

    private boolean calType;

    private int year;

    private int month;

    private int day;

    private double hour;

    private double deltaT;

    private boolean deltatIsValid = false;

    /**
  * This constructs a new SweDate with a default of the current date
  * and current time at GMT.
  */
    public SweDate() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate()");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        setFields(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60. + cal.get(Calendar.SECOND) / 3600. + cal.get(Calendar.MILLISECOND) / 3600000., SE_GREG_CAL);
        Trace.level--;
    }

    /**
  * This constructs a new SweDate with the given Julian Day number.
  * The calendar system will be Gregorian after October 15, 1582 or
  * Julian before that date.
  * @param jd Julian Day number
  */
    public SweDate(double jd) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate(double)");
        initDateFromJD(jd, jdCO <= jd ? SE_GREG_CAL : SE_JUL_CAL);
        Trace.level--;
    }

    /**
  * This constructs a new SweDate with the given Julian Day number.
  * The dates will be calculated according to the given calendar system
  * (Gregorian or Julian calendar).
  * @param jd Julian Day number
  * @param calType calendar type (Gregorian or Julian calendar system)
  * @see #SE_GREG_CAL
  * @see #SE_JUL_CAL
  */
    public SweDate(double jd, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate(double, boolean)");
        initDateFromJD(jd, calType);
        Trace.level--;
    }

    /**
  * This constructs a new SweDate with the given date and time. The calendar
  * type is automatically adjusted to Julian calendar system before October 15,
  * 1582, and to Gregorian calendar system after and including that date. The
  * dates from October 5 to October 14, 1582 had been skipped during the
  * conversion to the Gregorian calendar, so we just convert any such date to
  * Julian calendar system even though no such date did exist.
  * @param year The year of the date
  * @param month The month of the date
  * @param day The day-number in a month of that date
  * @param hour The hour of the day
  */
    public SweDate(int year, int month, int day, double hour) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate(int, int, int, double)");
        setFields(year, month, day, hour);
        Trace.level--;
    }

    /**
  * This constructs a new SweDate with the given date and time. The
  * date numbers will be interpreted according to the given calendar
  * system (Gregorian or Julian calendar).
  * @param year The year of the date
  * @param month The month of the date
  * @param day The day-number of the date
  * @param hour The hour of the day
  * @param calType calendar type (Gregorian or Julian calendar system)
  * @see #SE_GREG_CAL
  * @see #SE_JUL_CAL
  */
    public SweDate(int year, int month, int day, double hour, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate(int, int, int, double, boolean)");
        setFields(year, month, day, hour, calType);
        Trace.level--;
    }

    /**
  * Queries the Julian Day number of this object.
  * @return Julian Day number
  */
    public double getJulDay() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getJulDay()");
        Trace.level--;
        return this.jd;
    }

    /**
  * Queries the Julian Day number of the given date in Gregorian calendar
  * system - this is a static method.
  * @param year The year of the date
  * @param month The month of the date
  * @param day The day-number of the date
  * @param hour The hour of the day
  * @return Julian Day number
  */
    public static double getJulDay(int year, int month, int day, double hour) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getJulDay(int, int, int, double)");
        double sjd = swe_julday(year, month, day, hour, SE_GREG_CAL);
        Trace.level--;
        return sjd;
    }

    /**
  * Queries the Julian Day number of the given date that is interpreted as
  * a date in the given calendar system - this is a static method.
  * @param year The year of the date
  * @param month The month of the date
  * @param day The day-number of the date
  * @param hour The hour of the day
  * @param calType calendar type (Gregorian or Julian calendar system)
  * @return Julian Day number
  * @see #SE_GREG_CAL
  * @see #SE_JUL_CAL
  */
    public static double getJulDay(int year, int month, int day, double hour, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getJulDay(int, int, int, double, boolean)");
        double sjd = swe_julday(year, month, day, hour, calType);
        Trace.level--;
        return sjd;
    }

    /**
  * Queries the day of the week, i.e. Sunday to Saturday as represented by
  * an integer. Sunday is represented by 0, Saturday by 6. Any discontinuity
  * in the sequence of weekdays is <b>not</b> taken into account!
  * <B>Attention: the numbers are different from the numbers returned by the
  * java.awt.Calendar class!</B>
  * @return Number of the day of week
  * @see #SUNDAY
  * @see #MONDAY
  * @see #TUESDAY
  * @see #WEDNESDAY
  * @see #THURSDAY
  * @see #FRIDAY
  * @see #SATURDAY
  */
    public int getDayOfWeekNr() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDayOfWeekNr()");
        Trace.level--;
        return ((int) (this.jd - 5.5)) % 7;
    }

    /**
  * Queries the day of the week of the given Julian Day number (interpreted
  * in the gregorian calendar system!). Sunday is represented by 0, Saturday
  * by 6. Any discontinuity in the sequence of weekdays is <b>not</b> taken
  * into account! <B>Attention: the numbers are different from the numbers
  * returned by the java.awt.Calendar class!</B>
  * @param jd The Julian Day number of the date
  * @return Number of the day of week
  * @see #SUNDAY
  * @see #MONDAY
  * @see #TUESDAY
  * @see #WEDNESDAY
  * @see #THURSDAY
  * @see #FRIDAY
  * @see #SATURDAY
  */
    public static synchronized int getDayOfWeekNr(double jd) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDayOfWeekNr(double)");
        Trace.level--;
        return ((int) (jd - 5.5)) % 7;
    }

    /**
  * Queries the day of the week of the given date that is interpreted as
  * being a date in the Gregorian or Julian calendar system depending on
  * the date, the switch from Julian to Gregorian calendar system occured.
  * Sunday is represented by 0, Saturday by 6. Any discontinuity in the
  * sequence of weekdays is <b>not</b> taken into account! <B>Attention:
  * the numbers are different from the numbers returned by the
  * java.awt.Calendar class!</B>
  * @return Number of the day of week
  * @param year The year of the date
  * @param month The month of the date
  * @param day The day-number of the date
  * @see #SUNDAY
  * @see #MONDAY
  * @see #TUESDAY
  * @see #WEDNESDAY
  * @see #THURSDAY
  * @see #FRIDAY
  * @see #SATURDAY
  */
    public static int getDayOfWeekNr(int year, int month, int day) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDayOfWeekNr(int, int, int)");
        int sdow = ((int) (swe_julday(year, month, day, 0.0, SE_GREG_CAL) - 5.5)) % 7;
        Trace.level--;
        return sdow;
    }

    /**
  * Queries the day of the week of the given date that is interpreted as
  * being a date in the given calendar system. Sunday is represented by 0,
  * Saturday by 6. Any discontinuity in the sequence of weekdays is
  * <b>not</b> taken into account! <B>Attention: the numbers are different
  * from the numbers returned by the java.awt.Calendar class!</B>
  * @return Number of the day of week
  * @param year The year of the date
  * @param month The month of the date
  * @param day The day-number of the date
  * @param calType calendar type (Gregorian or Julian calendar system)
  * @see #SE_GREG_CAL
  * @see #SE_JUL_CAL
  * @see #SUNDAY
  * @see #MONDAY
  * @see #TUESDAY
  * @see #WEDNESDAY
  * @see #THURSDAY
  * @see #FRIDAY
  * @see #SATURDAY
  */
    public static int getDayOfWeekNr(int year, int month, int day, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDayOfWeekNr(int, int, int, boolean)");
        int sdow = ((int) (swe_julday(year, month, day, 0.0, calType) - 5.5)) % 7;
        Trace.level--;
        return sdow;
    }

    /**
  * Queries the type of calendar in effect - Gregorian or Julian calendar.
  * This will effect what date you will get back for a given Julian Day.
  * @return Calendar type
  * @see #SE_GREG_CAL
  * @see #SE_JUL_CAL
  */
    public boolean getCalendarType() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getCalendarType()");
        Trace.level--;
        return this.calType;
    }

    /**
  * Queries the year of this SweDate object.
  * @return year
  */
    public int getYear() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getYear()");
        Trace.level--;
        return this.year;
    }

    /**
  * Queries the month of this SweDate object.
  * @return month <B>Attention:</B> The month ranges from 1 to 12, this is
  * different to the java.util.Calendar class!
  */
    public int getMonth() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getMonth()");
        Trace.level--;
        return this.month;
    }

    /**
  * Queries the day of this SweDate object.
  * @return day number
  */
    public int getDay() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDay()");
        Trace.level--;
        return this.day;
    }

    /**
  * Queries the hour of the day of this SweDate object.
  * @return hour
  */
    public double getHour() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getHour()");
        Trace.level--;
        return this.hour;
    }

    /**
  * Queries the delta T value for the date of this object.
  * @return delta T
  */
    public double getDeltaT() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDeltaT()");
        if (deltatIsValid) {
            return this.deltaT;
        }
        this.deltaT = calc_deltaT(this.getJulDay());
        deltatIsValid = true;
        Trace.level--;
        return this.deltaT;
    }

    /**
  * Queries the delta T value for the given Julian Day number - this is a
  * static method. Delta T is calculated with a tidal acceleration of
  * SE_TIDAL_DEFAULT.
  * @param tjd Julian Day number
  * @return delta T
  * @see #SE_TIDAL_DEFAULT
  */
    public static double getDeltaT(double tjd) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDeltaT(double)");
        double sdt = calc_deltaT(tjd, SE_TIDAL_DEFAULT);
        Trace.level--;
        return sdt;
    }

    /**
  * This will return a java.util.Date object with the date of this
  * SweDate object. This is needed often in internationalisation of date
  * and time formats. You can add an offset in milliseconds to account for
  * timezones or daylight savings time, as SweDate is meant to be in GMT
  * time always.
  * @param offset An offset in milliseconds to be added to the current
  * date and time.
  */
    public Date getDate(long offset) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDate(long)");
        long millis = (long) ((getJulDay() - JD0) * 24L * 3600L * 1000L) + offset;
        Trace.level--;
        return new Date(millis);
    }

    /**
  * This will return a java.util.Date object from a julian day number.
  * @param jd The julian day number for which to create a Date object.
  */
    public static Date getDate(double jd) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getDate(double)");
        long millis = (long) ((jd - JD0) * 24L * 3600L * 1000L);
        Trace.level--;
        return new Date(millis);
    }

    /**
  * Sets the new Julian Day for this object. This operation does NOT
  * change the calendar type (Gregorian or Julian calendar). Use methods
  * setCalendarType() or updateCalendarType() for this.
  * @param newJD Julian Day number
  */
    public void setJulDay(double newJD) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setJulDay(double)");
        this.jd = newJD;
        deltatIsValid = false;
        IDate dt = swe_revjul(newJD, this.calType);
        this.year = dt.year;
        this.month = dt.month;
        this.day = dt.day;
        this.hour = dt.hour;
        Trace.level--;
    }

    /**
  * Sets the calendar type for this object.
  * @param newCalType Calendar type (Greogorian or Julian calendar)
  * @param keepDate Determines, if the date or the julian day should
  * be fix in this operation.
  * @see #SE_GREG_CAL
  * @see #SE_JUL_CAL
  * @see #SE_KEEP_DATE
  * @see #SE_KEEP_JD
  */
    public void setCalendarType(boolean newCalType, boolean keepDate) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setCalendarType(boolean, boolean)");
        if (this.calType != newCalType) {
            this.calType = newCalType;
            deltatIsValid = false;
            if (keepDate) {
                this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
            } else {
                IDate dt = swe_revjul(this.jd, newCalType);
                this.year = dt.year;
                this.month = dt.month;
                this.day = dt.day;
                this.hour = dt.hour;
            }
        }
        Trace.level--;
    }

    /**
  * Update the calendar type according to the Gregorian calendar start
  * date and the date of this object.
  */
    public void updateCalendarType() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.updateCalendarType()");
        this.calType = (this.jdCO <= this.jd ? SE_GREG_CAL : SE_JUL_CAL);
        ;
        Trace.level--;
    }

    /**
  * Sets a new date for this object.
  * @param newYear the year-part of the new date
  * @param newMonth the month-part of the new date [1-12]
  * @param newDay the day-part of the new date [1-31]
  * @param newHour the hour of the new date
  * @return true
  */
    public boolean setDate(int newYear, int newMonth, int newDay, double newHour) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setDate(int, int, int, double)");
        this.year = newYear;
        this.month = newMonth;
        this.day = newDay;
        this.hour = newHour;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        Trace.level--;
        return true;
    }

    /**
  * Sets a new date for this object. The input can be checked, if it is a
  * valid date and can be modified, if not. See parameter "check".
  * @param newYear the year-part of the new date
  * @param newMonth the month-part of the new date [1-12]
  * @param newDay the day-part of the new date [1-31]
  * @param newHour the hour of the new date
  * @param check to see, if the new date is a valid date
  * @return true, if check==true, otherwise return true only, if the date is
  * valid
  */
    public boolean setDate(int newYear, int newMonth, int newDay, double newHour, boolean check) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setDate(int, int, int, double, boolean)");
        this.year = newYear;
        double oldMonth = this.month;
        double oldDay = this.day;
        this.month = newMonth;
        this.day = newDay;
        this.hour = newHour;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        if (check) {
            IDate dt = swe_revjul(this.jd, this.calType);
            this.year = dt.year;
            this.month = dt.month;
            this.day = dt.day;
            this.hour = dt.hour;
            Trace.level--;
            return (this.year == newYear && this.month == oldMonth && this.day == oldDay);
        }
        Trace.level--;
        return true;
    }

    /**
  * Sets the year-part of the date.
  * @param newYear The new year
  * @return true
  */
    public boolean setYear(int newYear) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setYear(int)");
        this.year = newYear;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        Trace.level--;
        return true;
    }

    /**
  * Sets the year-part of the date. The input can be checked, if the result
  * is a valid date and can be modified, if not. E.g., the date was 29th of
  * february 2000, and the year gets set to 2001. 2001 does not have a
  * 29th of february, so if parameter check is set to true, it will
  * return false and modify the date to 1st of march 2001.
  * @param newYear The new year
  * @param check check, if the resulting new date is a valid date and
  * adjust the values for day, month or year if necessary
  * @return true, if check==true, otherwise return true only, if the date is
  * valid
  */
    public boolean setYear(int newYear, boolean check) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setYear(int, boolean)");
        this.year = newYear;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        if (check) {
            double oldMonth = this.month;
            double oldDay = this.day;
            IDate dt = swe_revjul(this.jd, this.calType);
            this.year = dt.year;
            this.month = dt.month;
            this.day = dt.day;
            this.hour = dt.hour;
            Trace.level--;
            return (this.year == newYear && this.month == oldMonth && this.day == oldDay);
        }
        Trace.level--;
        return true;
    }

    public boolean setMonth(int newMonth) {
        System.out.println(System.currentTimeMillis() + " SweDate.setMonth(int)");
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setMonth(int)");
        this.month = newMonth;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        Trace.level--;
        return true;
    }

    /**
  * Sets the year-part of the date. The input can be checked, if the result
  * is a valid date and can be modified, if not.
  * @param newMonth The new year
  * @param check check, if the resulting new date is a valid date and
  * adjust the values for day, month or year if necessary
  * @return true, if check==true, otherwise return true only, if the date is
  * valid
  * @see SweDate#setYear(int, boolean)
  */
    public boolean setMonth(int newMonth, boolean check) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setMonth(int, boolean)");
        this.month = newMonth;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        if (check) {
            double oldYear = this.year;
            double oldDay = this.day;
            IDate dt = swe_revjul(this.jd, this.calType);
            this.year = dt.year;
            this.month = dt.month;
            this.day = dt.day;
            this.hour = dt.hour;
            Trace.level--;
            return (this.year == oldYear && this.month == newMonth && this.day == oldDay);
        }
        Trace.level--;
        return true;
    }

    /**
  * Sets the day-part of the date.
  * @param newDay The new day
  * @return true
  */
    public boolean setDay(int newDay) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setDay(int)");
        this.day = newDay;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        Trace.level--;
        return true;
    }

    /**
  * Sets the day-part of the date. The input can be checked, if the result
  * is a valid date and can be modified, if not.
  * @param newDay The new day
  * @param check check, if the resulting new date is a valid date and
  * adjust the values for day, month or year if necessary
  * @return true, if check==true, otherwise return true only, if the date is
  * valid
  * @see SweDate#setYear(int, boolean)
  */
    public boolean setDay(int newDay, boolean check) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setDay(int, boolean)");
        this.day = newDay;
        deltatIsValid = false;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        if (check) {
            double oldYear = this.year;
            double oldMonth = this.month;
            IDate dt = swe_revjul(this.jd, this.calType);
            this.year = dt.year;
            this.month = dt.month;
            this.day = dt.day;
            this.hour = dt.hour;
            Trace.level--;
            return (this.year == oldYear && this.month == oldMonth && this.day == newDay);
        }
        Trace.level--;
        return true;
    }

    /**
  * Sets a new hour.
  * @param newHour The new hour
  * @return true
  */
    public boolean setHour(double newHour) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setHour(double)");
        this.hour = newHour;
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        Trace.level--;
        return true;
    }

    /**
  * Checks the date to see, if it is a valid date.
  * @return true, if the date is valid, false, if not
  */
    public boolean checkDate() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.checkDate()");
        boolean cd = checkDate(this.year, this.month, this.day, this.hour);
        Trace.level--;
        return cd;
    }

    /**
  * Checks the given date to see, if it is a valid date.
  * @param year the year, for which is to be checked
  * @param month the month, for which is to be checked
  * @param day the day, for which is to be checked
  * @return true, if the date is valid, false, if not
  */
    public boolean checkDate(int year, int month, int day) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.checkDate(int, int, int)");
        boolean cd = checkDate(year, month, day, 0.0);
        Trace.level--;
        return cd;
    }

    /**
  * Checks the given date to see, if it is a valid date.
  * @param year the year, for which is to be checked
  * @param month the month, for which is to be checked
  * @param day the day, for which is to be checked
  * @param hour the hour, for which is to be checked
  * @return true, if the date is valid, false, if not
  */
    public boolean checkDate(int year, int month, int day, double hour) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.checkDate(int, int, int, hour)");
        double jd = swe_julday(year, month, day, hour, SE_GREG_CAL);
        IDate dt = swe_revjul(jd, SE_GREG_CAL);
        Trace.level--;
        return (dt.year == year && dt.month == month && dt.day == day);
    }

    /**
  * Makes the date to be a valid date.
  */
    public void makeValidDate() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.makeValidDate()");
        double jd = swe_julday(this.year, this.month, this.day, this.hour, SE_GREG_CAL);
        IDate dt = swe_revjul(jd, SE_GREG_CAL);
        this.year = dt.year;
        this.month = dt.month;
        this.day = dt.day;
        this.hour = dt.hour;
        Trace.level--;
    }

    /**
  * Returns the julian day number on which the Gregorian calendar system
  * comes to be in effect.
  */
    public double getGregorianChange() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getGregorianChange()");
        Trace.level--;
        return this.jdCO;
    }

    /**
  * Changes the date of the start of the Gregorian calendar system.
  * This method will keep the date and change the julian day number
  * of the date of this SweDate object if required.
  * @param year The year (in Gregorian system) for the new start date
  * @param month The month (in Gregorian system) for the new start date.
  * Adversely to java.util.Calendar, the month is to be given in the
  * range of 1 for January to 12 for December!
  * @param day The day of the month (in Gregorian system, from 1 to 31)
  * for the new start date
  */
    public void setGregorianChange(int year, int month, int day) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setGregorianChange(int, int, int)");
        this.year = year;
        this.month = month;
        this.day = day;
        deltatIsValid = false;
        this.calType = SE_GREG_CAL;
        if (this.year < year || (this.year == year && this.month < month) || (this.year == year && this.month == month && this.day < day)) {
            this.calType = SE_JUL_CAL;
        }
        this.jdCO = swe_julday(year, month, day, 0., SE_GREG_CAL);
        this.jd = swe_julday(this.year, this.month, this.day, this.hour, this.calType);
        Trace.level--;
    }

    /**
  * Changes the date of the start of the Gregorian calendar system.
  * This method will keep the julian day number and change year,
  * month and day of the date of this SweDate object if required.
  * @param newJDCO The julian day number, on which the Gregorian calendar
  * came into effect.
  */
    public void setGregorianChange(double newJDCO) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setGregorianChange(double)");
        this.jdCO = newJDCO;
        this.calType = (this.jd >= this.jdCO ? SE_GREG_CAL : SE_JUL_CAL);
        IDate dt = swe_revjul(this.jd, this.calType);
        this.year = dt.year;
        this.month = dt.month;
        this.day = dt.day;
        this.hour = dt.hour;
        Trace.level--;
    }

    /**
  * Returns the tidal acceleration used in calculations of delta T.
  * @return Tidal acceleration
  */
    public double getTidalAcc() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.getTidalAcc()");
        Trace.level--;
        return this.tid_acc;
    }

    /**
  * Sets the tidal acceleration used in calculations of delta T.
  * @param tid_acc tidal acceleration
  * @see #SE_TIDAL_DE403
  * @see #SE_TIDAL_DE404
  * @see #SE_TIDAL_DE405
  * @see #SE_TIDAL_DE406
  * @see #SE_TIDAL_DE200
  * @see #SE_TIDAL_26
  * @see #SE_TIDAL_DEFAULT
  */
    public void setTidalAcc(double tid_acc) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setTidalAcc(double)");
        this.tid_acc = tid_acc;
        Trace.level--;
    }

    /**
  * Returns the date, calendar type (gregorian / julian), julian day
  * number and the deltaT value of this object.
  * @return Infos about this object
  */
    public String toString() {
        double hour = getHour();
        String h = (int) hour + ":";
        hour = 60 * (hour - (int) hour);
        h += (int) hour + ":";
        hour = 60 * (hour - (int) hour);
        h += hour;
        return "(YYYY/MM/DD) " + getYear() + "/" + (getMonth() < 10 ? "0" : "") + getMonth() + "/" + (getDay() < 10 ? "0" : "") + getDay() + ", " + h + "h " + (getCalendarType() ? "(greg)" : "(jul)") + "\n" + "Jul. Day: " + getJulDay() + "; " + "DeltaT: " + getDeltaT();
    }

    private static synchronized double swe_julday(int year, int month, int day, double hour, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.julday(int, int, int, double, boolean)");
        double jd;
        double u, u0, u1, u2;
        u = year;
        if (month < 3) {
            u -= 1;
        }
        u0 = u + 4712.0;
        u1 = month + 1.0;
        if (u1 < 4) {
            u1 += 12.0;
        }
        jd = SMath.floor(u0 * 365.25) + SMath.floor(30.6 * u1 + 0.000001) + day + hour / 24.0 - 63.5;
        if (calType == SE_GREG_CAL) {
            u2 = SMath.floor(SMath.abs(u) / 100) - SMath.floor(SMath.abs(u) / 400);
            if (u < 0.0) {
                u2 = -u2;
            }
            jd = jd - u2 + 2;
            if ((u < 0.0) && (u / 100 == SMath.floor(u / 100)) && (u / 400 != SMath.floor(u / 400))) {
                jd -= 1;
            }
        }
        Trace.level--;
        return jd;
    }

    private synchronized IDate swe_revjul(double jd, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.swe_revjul(double, boolean)");
        IDate dt = new IDate();
        double u0, u1, u2, u3, u4;
        u0 = jd + 32082.5;
        if (calType == SE_GREG_CAL) {
            u1 = u0 + SMath.floor(u0 / 36525.0) - SMath.floor(u0 / 146100.0) - 38.0;
            if (jd >= 1830691.5) {
                u1 += 1;
            }
            u0 = u0 + SMath.floor(u1 / 36525.0) - SMath.floor(u1 / 146100.0) - 38.0;
        }
        u2 = SMath.floor(u0 + 123.0);
        u3 = SMath.floor((u2 - 122.2) / 365.25);
        u4 = SMath.floor((u2 - SMath.floor(365.25 * u3)) / 30.6001);
        dt.month = (int) (u4 - 1.0);
        if (dt.month > 12) {
            dt.month -= 12;
        }
        dt.day = (int) (u2 - SMath.floor(365.25 * u3) - SMath.floor(30.6001 * u4));
        dt.year = (int) (u3 + SMath.floor((u4 - 2.0) / 12.0) - 4800);
        dt.hour = (jd - SMath.floor(jd + 0.5) + 0.5) * 24.0;
        Trace.level--;
        return dt;
    }

    private static final int TABSTART = 1620;

    private static final int TABEND = 2014;

    private static final int TABSIZ = TABEND - TABSTART + 1;

    private static final int TABSIZ_SPACE = TABSIZ + 50;

    private static short dt[] = new short[] { 12400, 11900, 11500, 11000, 10600, 10200, 9800, 9500, 9100, 8800, 8500, 8200, 7900, 7700, 7400, 7200, 7000, 6700, 6500, 6300, 6200, 6000, 5800, 5700, 5500, 5400, 5300, 5100, 5000, 4900, 4800, 4700, 4600, 4500, 4400, 4300, 4200, 4100, 4000, 3800, 3700, 3600, 3500, 3400, 3300, 3200, 3100, 3000, 2800, 2700, 2600, 2500, 2400, 2300, 2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1400, 1300, 1200, 1200, 1100, 1100, 1000, 1000, 1000, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1100, 1200, 1200, 1200, 1200, 1200, 1200, 1200, 1200, 1200, 1200, 1300, 1300, 1300, 1300, 1300, 1300, 1300, 1400, 1400, 1400, 1400, 1400, 1400, 1400, 1500, 1500, 1500, 1500, 1500, 1500, 1500, 1600, 1600, 1600, 1600, 1600, 1600, 1600, 1600, 1600, 1600, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1700, 1600, 1600, 1600, 1600, 1500, 1500, 1400, 1400, 1370, 1340, 1310, 1290, 1270, 1260, 1250, 1250, 1250, 1250, 1250, 1250, 1250, 1250, 1250, 1250, 1250, 1240, 1230, 1220, 1200, 1170, 1140, 1110, 1060, 1020, 960, 910, 860, 800, 750, 700, 660, 630, 600, 580, 570, 560, 560, 560, 570, 580, 590, 610, 620, 630, 650, 660, 680, 690, 710, 720, 730, 740, 750, 760, 770, 770, 780, 780, 788, 782, 754, 697, 640, 602, 541, 410, 292, 182, 161, 10, -102, -128, -269, -324, -364, -454, -471, -511, -540, -542, -520, -546, -546, -579, -563, -564, -580, -566, -587, -601, -619, -664, -644, -647, -609, -576, -466, -374, -272, -154, -2, 124, 264, 386, 537, 614, 775, 913, 1046, 1153, 1336, 1465, 1601, 1720, 1824, 1906, 2025, 2095, 2116, 2225, 2241, 2303, 2349, 2362, 2386, 2449, 2434, 2408, 2402, 2400, 2387, 2395, 2386, 2393, 2373, 2392, 2396, 2402, 2433, 2483, 2530, 2570, 2624, 2677, 2728, 2778, 2825, 2871, 2915, 2957, 2997, 3036, 3072, 3107, 3135, 3168, 3218, 3268, 3315, 3359, 3400, 3447, 3503, 3573, 3654, 3743, 3829, 3920, 4018, 4117, 4223, 4337, 4449, 4548, 4646, 4752, 4853, 4959, 5054, 5138, 5217, 5296, 5379, 5434, 5487, 5532, 5582, 5630, 5686, 5757, 5831, 5912, 5998, 6078, 6163, 6230, 6297, 6347, 6383, 6409, 6430, 6447, 6457, 6469, 6485, 6515, 6548, 6580, 6620, 6660, 6700, 6750, 6800, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private static final int TAB2_SIZ = 27;

    private static final int TAB2_START = -1000;

    private static final int TAB2_END = 1600;

    private static final int TAB2_STEP = 100;

    private static final int LTERM_EQUATION_YSTART = 1820;

    private static final int LTERM_EQUATION_COEFF = 32;

    private static short dt2[] = new short[] { 25400, 23700, 22000, 21000, 19040, 17190, 15530, 14080, 12790, 11640, 10580, 9600, 8640, 7680, 6700, 5710, 4740, 3810, 2960, 2200, 1570, 1090, 740, 490, 320, 200, 120 };

    private static final int TAB2_START = -500;

    private static final int TAB2_END = 1600;

    private static final int TAB2_STEP = 50;

    private static final int LTERM_EQUATION_YSTART = 1735;

    private static final int LTERM_EQUATION_COEFF = 35;

    private static short dt2[] = new short[] { 16800, 16000, 15300, 14600, 14000, 13400, 12800, 12200, 11600, 11100, 10600, 10100, 9600, 9100, 8600, 8200, 7700, 7200, 6700, 6200, 5700, 5200, 4700, 4300, 3800, 3400, 3000, 2600, 2200, 1900, 1600, 1350, 1100, 900, 750, 600, 470, 380, 300, 230, 180, 140, 110 };

    private synchronized double calc_deltaT(double tjd) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.deltaT(double)");
        double sdt = calc_deltaT(tjd, this.tid_acc);
        Trace.level--;
        return sdt;
    }

    private static synchronized double calc_deltaT(double tjd, double tid_acc) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.deltaT(double, double)");
        double ans = 0., ans2, ans3;
        double p, B = 0., B2, Y = 0., Ygreg, dd;
        int d[] = new int[6];
        int i, iy, k;
        int tabsiz = TABSIZ;
        int tabsiz = init_dt();
        int tabend = TABSTART + tabsiz - 1;
        Y = 2000.0 + (tjd - SwephData.J2000) / 365.25;
        Ygreg = 2000.0 + (tjd - SwephData.J2000) / 365.2425;
        if (Y < TAB2_START) {
            B = (Y - LTERM_EQUATION_YSTART) * 0.01;
            ans = -20 + LTERM_EQUATION_COEFF * B * B;
            ans = adjust_for_tidacc(tid_acc, ans, Y);
            if (Y >= TAB2_START - 100) {
                ans2 = adjust_for_tidacc(tid_acc, dt2[0], TAB2_START);
                B = (TAB2_START - LTERM_EQUATION_YSTART) * 0.01;
                ans3 = -20 + LTERM_EQUATION_COEFF * B * B;
                ans3 = adjust_for_tidacc(tid_acc, ans3, Y);
                dd = ans3 - ans2;
                B = (Y - (TAB2_START - 100)) * 0.01;
                ans = ans - dd * B;
            }
        }
        if (Y >= TAB2_START && Y < TAB2_END) {
            double Yjul = 2000 + (tjd - 2451557.5) / 365.25;
            p = SMath.floor(Yjul);
            iy = (int) ((p - TAB2_START) / TAB2_STEP);
            dd = (Yjul - (TAB2_START + TAB2_STEP * iy)) / TAB2_STEP;
            ans = dt2[iy] + (dt2[iy + 1] - dt2[iy]) * dd;
            ans = adjust_for_tidacc(tid_acc, ans, Y);
        }
        if (Y >= TAB2_END && Y < TABSTART) {
            B = TABSTART - TAB2_END;
            iy = (TAB2_END - TAB2_START) / TAB2_STEP;
            dd = (Y - TAB2_END) / B;
            ans = dt2[iy] + dd * (dt[0] / 100.0 - dt2[iy]);
            ans = adjust_for_tidacc(tid_acc, ans, Y);
        }
        if (Y >= TABSTART && Y <= tabend) {
            p = SMath.floor(Y);
            iy = (int) (p - TABSTART);
            ans = dt[iy];
            k = iy + 1;
            if (k >= tabsiz) return deltatIsDone(ans, Y, B, tid_acc, tabsiz, tabend);
            p = Y - p;
            ans += p * (dt[k] - dt[iy]);
            if ((iy - 1 < 0) || (iy + 2 >= tabsiz)) return deltatIsDone(ans, Y, B, tid_acc, tabsiz, tabend);
            k = iy - 2;
            for (i = 0; i < 5; i++) {
                if ((k < 0) || (k + 1 >= tabsiz)) d[i] = 0; else d[i] = dt[k + 1] - dt[k];
                k += 1;
            }
            for (i = 0; i < 4; i++) d[i] = d[i + 1] - d[i];
            B = 0.25 * p * (p - 1.0);
            ans += B * (d[1] + d[2]);
            if (iy + 2 >= tabsiz) return deltatIsDone(ans, Y, B, tid_acc, tabsiz, tabend);
            for (i = 0; i < 3; i++) d[i] = d[i + 1] - d[i];
            B = 2.0 * B / 3.0;
            ans += (p - 0.5) * B * d[1];
            if ((iy - 2 < 0) || (iy + 3 > tabsiz)) return deltatIsDone(ans, Y, B, tid_acc, tabsiz, tabend);
            for (i = 0; i < 2; i++) d[i] = d[i + 1] - d[i];
            B = 0.125 * B * (p + 1.0) * (p - 2.0);
            ans += B * (d[0] + d[1]);
        }
        return deltatIsDone(ans, Y, B, tid_acc, tabsiz, tabend);
    }

    private static synchronized double deltatIsDone(double ans, double Y, double B, double tid_acc, int tabsiz, int tabend) {
        double ans2, ans3, B2, dd;
        if (Y >= TABSTART && Y <= tabend) {
            ans *= 0.01;
            ans = adjust_for_tidacc(tid_acc, ans, Y);
        }
        if (Y > tabend) {
            B = 0.01 * (Y - 1820);
            ans = -20 + 31 * B * B;
            if (Y <= tabend + 100) {
                B2 = 0.01 * (tabend - 1820);
                ans2 = -20 + 31 * B2 * B2;
                ans3 = dt[tabsiz - 1] * 0.01;
                dd = (ans2 - ans3);
                ans += dd * (Y - (tabend + 100)) * 0.01;
            }
        }
        Trace.level--;
        return ans / 86400.0;
    }

    private static int init_dt() {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.init_dt()");
        FilePtr fp = null;
        int year;
        int tab_index;
        int tabsiz;
        int i;
        String s;
        if (!init_dt_done) {
            init_dt_done = true;
            try {
                if ((fp = sw.swi_fopen(-1, "sedeltat.txt", sw.swed.ephepath, null)) == null) {
                    Trace.level--;
                    return TABSIZ;
                }
            } catch (SwissephException se) {
                Trace.level--;
                return TABSIZ;
            }
            try {
                while ((s = fp.readLine()) != null) {
                    s.trim();
                    if (s.length() == 0 || s.charAt(0) == '#') {
                        continue;
                    }
                    year = SwissLib.atoi(s);
                    tab_index = year - TABSTART;
                    if (tab_index >= TABSIZ_SPACE) continue;
                    if (s.length() > 4) {
                        s = s.substring(4).trim();
                    }
                    dt[tab_index] = (short) (SwissLib.atof(s) * 100 + 0.5);
                }
            } catch (java.io.IOException e) {
            } catch (java.nio.BufferUnderflowException e) {
            }
            try {
                fp.close();
            } catch (java.io.IOException e) {
            }
        }
        tabsiz = 2001 - TABSTART + 1;
        for (i = tabsiz - 1; i < TABSIZ_SPACE; i++) {
            if (dt[i] == 0) break; else tabsiz++;
        }
        tabsiz--;
        Trace.level--;
        return tabsiz;
    }

    private static double adjust_for_tidacc(double tid_acc_local, double ans, double Y) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.adjust_for_tidacc(double, double, double");
        double B;
        if (Y < 1955.0) {
            B = (Y - 1955.0);
            ans += -0.000091 * (tid_acc_local + 26.0) * B * B;
        }
        Trace.level--;
        return ans;
    }

    /**
  * Sets the year, month, day, hour, calType and jd fields of this
  * SweDate instance.
  */
    private void initDateFromJD(double jd, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.initDateFromJD(double, boolean");
        this.jd = jd;
        this.calType = calType;
        IDate dt = swe_revjul(jd, calType);
        this.year = dt.year;
        this.month = dt.month;
        this.day = dt.day;
        this.hour = dt.hour;
        Trace.level--;
    }

    /**
  * Sets the year, month, day, hour, calType and jd fields of this
  * object.
  */
    private void setFields(int year, int month, int day, double hour) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setFields(int, int, int, double");
        IDate dt = swe_revjul(jdCO, SE_GREG_CAL);
        boolean calType = SE_GREG_CAL;
        if (dt.year > year || (dt.year == year && dt.month > month) || (dt.year == year && dt.month == month && dt.day > day)) {
            calType = SE_JUL_CAL;
        }
        setFields(year, month, day, hour, calType);
        Trace.level--;
    }

    /**
  * Sets the year, month, day, hour, calType and jd fields of this
  * object.
  */
    private void setFields(int year, int month, int day, double hour, boolean calType) {
        Trace.level++;
        Trace.trace(Trace.level, "SweDate.setFields(int, int, int, double, boolean");
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.calType = calType;
        this.jd = swe_julday(year, month, day, hour, calType);
        Trace.level--;
    }
}

class IDate {

    public int year;

    public int month;

    public int day;

    public double hour;
}
