package com.netx.ut.lib.java;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Queue;
import java.util.Enumeration;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;
import com.netx.ut.lib.java.TestResources.Beeper;
import com.netx.ut.lib.java.TestResources.RWLock1;
import com.netx.ut.lib.java.TestResources.RWLock2;
import com.netx.ut.lib.java.TestResources.Thread1;
import com.netx.ut.lib.java.TestResources.Thread2;
import com.netx.generics.R1.util.UnitTester;

public class NTUtil extends UnitTester {

    public static void main(String[] args) throws Throwable {
        NTUtil nt = new NTUtil();
        nt.locale_listAvailableLocales();
        nt.println("done.");
    }

    public void time_compareSystemTime() {
        System.out.println(System.currentTimeMillis());
        System.out.println(Calendar.getInstance().getTimeInMillis());
    }

    public void time_printDateObject() {
        Date date = new Date(Calendar.getInstance().getTimeInMillis());
        println(date);
    }

    public void time_testDateFormat() throws Exception {
        println("DateFormat object");
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        Date date = new Date(System.currentTimeMillis());
        println("converting Date(" + date + ") to String:");
        println(df.format(date));
        String str = "26-09-2004";
        println("converting String(" + str + ") to Date:");
        println(df.parse(str));
    }

    public void time_testSimpleDateFormat() throws Exception {
        println("SimpleDateFormat object");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        println("converting Date(" + date + ") to String:");
        println(sdf.format(date));
        String str = "2004/09/26 13:12:56";
        println("converting String(" + str + ") to Date:");
        println(sdf.parse(str));
    }

    public void time_testMilliseconds() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        println("0 milliseconds: " + DateFormat.getDateTimeInstance().format(cal.getTime()));
        cal.setTimeInMillis(-100);
        println("-100 milliseconds: " + DateFormat.getDateTimeInstance().format(cal.getTime()));
        cal.setTimeInMillis(Long.MIN_VALUE);
        println("Long.MIN_VALUE milliseconds: " + DateFormat.getDateTimeInstance().format(cal.getTime()));
        println("setting calendar to moment zero: ");
        cal.set(Calendar.YEAR, 0);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        println(cal.getTimeInMillis() + " milliseconds: " + DateFormat.getDateTimeInstance().format(cal.getTime()));
        cal.setTimeInMillis(-62219979010000L);
        println("Year: " + cal.get(Calendar.YEAR));
        println(cal.getTimeInMillis() + " milliseconds: " + DateFormat.getDateTimeInstance().format(cal.getTime()));
    }

    public void time_testCalendar() {
        Calendar cal1 = Calendar.getInstance();
        println("instance #1: " + DateFormat.getDateTimeInstance().format(cal1.getTime()));
        Calendar cal2 = Calendar.getInstance();
        cal2.set(Calendar.YEAR, 2005);
        println("instance #2: " + DateFormat.getDateTimeInstance().format(cal2.getTime()));
        println("instance #1 again: " + DateFormat.getDateTimeInstance().format(cal1.getTime()));
    }

    public void time_testDefaultDateFormat() throws Throwable {
        println("default date format: " + DateFormat.getInstance());
        SimpleDateFormat ddf = (SimpleDateFormat) DateFormat.getTimeInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        println("default time format's pattern: " + ddf.toPattern());
        Date d = new Date();
        println("date (now): " + d);
        String timeStr = "12:34:56.555";
        Date time = sdf.parse(timeStr);
        println(timeStr + " formatted with default date format: " + ddf.format(time));
        println(timeStr + " formatted with simple date format: " + sdf.format(time));
        println(timeStr + " formatted with US locale date format: " + DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.US).format(time));
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(-99999999999999L);
        println("beginning of the epoch: " + DateFormat.getDateTimeInstance().format(c.getTime()));
    }

    public void locale_listAvailableLocales() {
        println("supported locales:");
        for (Locale l : Locale.getAvailableLocales()) {
            println(l);
        }
        println("default locale: " + Locale.getDefault().toString());
        println("default locale language: " + Locale.getDefault().getLanguage());
        println("default locale country: " + Locale.getDefault().getCountry());
    }

    public void locale_listMonthNames() {
        DateFormatSymbols s = new SimpleDateFormat().getDateFormatSymbols();
        String[] months = s.getMonths();
        for (int i = 0; i < months.length; i++) {
            println(months[i]);
        }
    }

    public void locale_testLocaleDateFormat() throws Throwable {
        Locale.setDefault(new Locale("en_UK"));
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        println(DateFormat.getDateInstance().format(df.parse("19/03/2006")));
    }

    public void collections_testQueueIterator() {
        Queue<String> q = new LinkedList<String>();
        q.add("carlos");
        q.add(" ");
        q.add("da");
        q.add(" ");
        q.add("silva");
        q.add(" ");
        q.add("pereira");
        q.add("\n");
        for (Iterator<String> it = q.iterator(); it.hasNext(); ) {
            print(it.next());
        }
    }

    public void zip_compressFiles() throws Exception {
        FileInputStream in = null;
        File f1 = new File("C:\\WINDOWS\\regedit.exe");
        File f2 = new File("C:\\WINDOWS\\win.ini");
        File file = new File("C:\\" + NTUtil.class.getName() + ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        out.putNextEntry(new ZipEntry("regedit.exe"));
        in = new FileInputStream(f1);
        while (in.available() > 0) {
            out.write(in.read());
        }
        in.close();
        out.closeEntry();
        out.putNextEntry(new ZipEntry("win.ini"));
        in = new FileInputStream(f2);
        while (in.available() > 0) {
            out.write(in.read());
        }
        in.close();
        out.closeEntry();
        out.close();
    }

    public void zip_readFile() throws IOException {
        String path = "C:\\Shared\\04.Dev\\04.Test\\network\\servidordados\\Zips\\2005\\02\\05-02-1200.zip";
        ZipFile file = new ZipFile(path);
        Enumeration<? extends ZipEntry> e = file.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            println("entry: " + entry.getName() + "; size: " + entry.getSize() + "; compressed: " + entry.getCompressedSize());
        }
        file.close();
    }

    public void regexp_testPatterns() {
        String regexp = "[a-zA-Z_0-9]*";
        println(Pattern.matches(regexp, "098_abcd"));
        println(Pattern.matches(regexp, "abc"));
        String[] names = { "Carlos Silva", "Isabel Figueiredo", "Jo�o Madeira", "Darren King", "Johanna Wilson", "Lu�s Soares", "Farrokh Bulsara", "Rebekah Staunton", "Zhu Malik" };
        Pattern p = Pattern.compile("C.*");
        for (String name : names) {
            if (p.matcher(name).matches()) {
                println(name);
            }
        }
        println(Pattern.matches(".*", "a100.doc"));
        println(Pattern.matches(".*", "a100"));
        println(Pattern.matches(".*\\..*", "a100.doc"));
        println(Pattern.matches(".*\\..*", "a100"));
    }

    public void logging_testSimpleLogging() {
        Logger logger = Logger.getLogger("tests.logging");
        logger.log(Level.SEVERE, "erro aqui!", new Exception());
    }

    public void logging_testFileLogging() throws IOException {
        Logger logger = Logger.getLogger("tests.logging");
        FileHandler fh = new FileHandler("log.txt");
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
        logger.log(Level.SEVERE, "erro aqui!", new Exception());
        logger.log(Level.WARNING, "erro aqui!", new Exception());
    }

    public void concurrency_testBeep() {
        Beeper beeper = new Beeper();
        Thread1 thread1 = new Thread1(beeper, "BEEPER");
        Thread2 thread2 = new Thread2(beeper, "HONKER");
        thread1.start();
        thread2.start();
    }

    public void concurrency_testWait() {
    }

    public void concurrency_readWriteLock() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        RWLock1 thread1 = new RWLock1("RWLock1", lock);
        RWLock2 thread2 = new RWLock2("RWLock2", lock);
        thread1.start();
        thread2.start();
    }
}
