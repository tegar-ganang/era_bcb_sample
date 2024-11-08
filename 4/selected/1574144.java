package au.com.cahaya.hubung.project.time;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.com.cahaya.asas.ds.party.model.PartyModel;
import au.com.cahaya.asas.ds.prefs.model.PreferenceCategoryModel;
import au.com.cahaya.asas.ds.prefs.model.PreferenceDefineModel;
import au.com.cahaya.asas.ds.prefs.model.PreferenceValueIntegerModel;
import au.com.cahaya.asas.ds.util.cli.PartyKeyOption;
import au.com.cahaya.asas.net.NetUtil;
import au.com.cahaya.asas.openoffice.OpenOfficeCalc;
import au.com.cahaya.asas.openoffice.OpenOfficeFunction;
import au.com.cahaya.asas.util.CalendarUtil;
import au.com.cahaya.asas.util.DateUtil;
import au.com.cahaya.asas.util.cli.DateOption;
import au.com.cahaya.asas.util.cli.FileOption;
import au.com.cahaya.asas.util.cli.HelpOption;
import au.com.cahaya.asas.util.cli.PropertyFileOption;
import au.com.cahaya.hubung.project.ProjectPropertiesInterface;
import au.com.cahaya.hubung.project.adt.ProjectTaskTimeWeek;
import au.com.cahaya.hubung.project.model.ProjectTaskModel;
import au.com.cahaya.hubung.project.model.TimeRecordModel;
import au.com.cahaya.hubung.project.model.TimeRecordProjectTaskModel;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;
import com.sun.star.uno.Exception;
import com.sun.star.util.MalformedNumberFormatException;

/**
 *
 *
 * @author Mathew Pole
 * @since May 2008
 * @version $Revision$
 */
public class ReportTimeWeek extends ReportTimeBase {

    /** The private logger for this class */
    Logger myLog = LoggerFactory.getLogger(ReportTimeWeek.class);

    /**
   * Constructor
   */
    public ReportTimeWeek() {
    }

    /**
   *
   */
    @Override
    public void report(File file, Date date, long ptyKey, EntityManagerFactory emf) {
        myLog.debug("report(" + file + ") - enter");
        EntityManager em = emf.createEntityManager();
        PreferenceCategoryModel category = loadPreferences(em);
        try {
            OpenOfficeConnection connection = new SocketOpenOfficeConnection(8100);
            connection.connect();
            OpenOfficeCalc calc = new OpenOfficeCalc(connection);
            OpenOfficeFunction func = new OpenOfficeFunction(connection);
            String uri = NetUtil.uriToString(file.toURI());
            boolean isClose = calc.openDesktop(uri, false);
            if (!isClose) {
                calc.open(uri);
            }
            ProcessTimeRecord ptr = new ProcessTimeRecord();
            if (ptr.process(ptyKey, emf, calc, func)) {
                report(date, ptyKey, em, category, calc, func);
            } else {
                myLog.info("process - error in ProcessTimeRecord.process, so report not generated.");
            }
        } catch (Exception exc) {
            myLog.error("process", exc);
        } catch (MalformedURLException exc) {
            myLog.error("process", exc);
        } catch (java.lang.Exception exc) {
            myLog.error("process", exc);
        }
    }

    /**
   * @param date
   * @param ptyKey
   * @param em
   * @param category
   * @param calc
   * @param func
   * @throws IndexOutOfBoundsException
   */
    private void report(Date date, long ptyKey, EntityManager em, PreferenceCategoryModel category, OpenOfficeCalc calc, OpenOfficeFunction func) throws IndexOutOfBoundsException {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        Date start = CalendarUtil.getStartOfWeek(c);
        Date end = new Date(start.getTime() + DateUtil.cWeekMilliseconds);
        TreeSet<TimeRecordModel> treSet = retrieveTimeRecord(em, start, end);
        Map<ProjectTaskModel, ProjectTaskTimeWeek> map = process(treSet);
        PartyModel userParty = getParty(em, ptyKey);
        TimeRecordModel extraTre = fudge(map, userParty, category);
        XSpreadsheet sheet = calc.retrieveSheet(2);
        int lastRow = write(calc, sheet, map);
        if (extraTre != null) {
            write(calc, func, sheet, lastRow, extraTre);
        }
    }

    /**
   * @param treList
   * @return
   */
    private Map<ProjectTaskModel, ProjectTaskTimeWeek> process(Set<TimeRecordModel> treSet) {
        TreeMap<ProjectTaskModel, ProjectTaskTimeWeek> map = new TreeMap<ProjectTaskModel, ProjectTaskTimeWeek>();
        Calendar calendar = new GregorianCalendar();
        for (TimeRecordModel tre : treSet) {
            calendar.setTime(tre.getStart());
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            myLog.debug("tre {} to {} on " + day, tre.getStart(), tre.getStop());
            for (TimeRecordProjectTaskModel tpt : tre.getProjectTasks()) {
                ProjectTaskTimeWeek w = map.get(tpt.getProjectTask());
                if (w == null) {
                    myLog.debug("Create project task for {}", tpt.getProjectTask().getName());
                    w = new ProjectTaskTimeWeek(tpt.getProjectTask());
                    map.put(tpt.getProjectTask(), w);
                }
                myLog.debug("  {} adding " + tpt.getMinutes(), tpt.getProjectTask().getName());
                w.addTime(day, tpt.getMinutes());
            }
        }
        return map;
    }

    /**
   * @param map
   * @return
   */
    private TimeRecordModel fudge(Map<ProjectTaskModel, ProjectTaskTimeWeek> map, PartyModel userParty, PreferenceCategoryModel category) {
        TimeRecordModel treExtra = null;
        String username = "mpole";
        long fidelity = 0;
        long minimumTime = 0;
        long daysInWeekRule = 127;
        PartyModel customerParty = null;
        PreferenceCategoryModel reportCategory = category.getChild(ProjectTimePreferencesInterface.cReportCategory);
        for (Iterator<ProjectTaskTimeWeek> i = map.values().iterator(); i.hasNext(); ) {
            ProjectTaskTimeWeek w = i.next();
            if (customerParty == null || !customerParty.equals(w.getProjectTask().getProject().getParty())) {
                customerParty = w.getProjectTask().getProject().getParty();
                if (reportCategory != null) {
                    PreferenceDefineModel define = reportCategory.getDefinition(ProjectTimePreferencesInterface.cReportFidelity);
                    PreferenceValueIntegerModel value = (PreferenceValueIntegerModel) (define.getValue(customerParty));
                    if (value != null) {
                        fidelity = value.getValue();
                    } else {
                        fidelity = 0;
                    }
                    define = reportCategory.getDefinition(ProjectTimePreferencesInterface.cReportMinimumTime);
                    value = (PreferenceValueIntegerModel) (define.getValue(customerParty));
                    if (value != null) {
                        minimumTime = value.getValue();
                    } else {
                        minimumTime = 0;
                    }
                    define = reportCategory.getDefinition(ProjectTimePreferencesInterface.cReportDaysInWeekRule);
                    value = (PreferenceValueIntegerModel) (define.getValue(customerParty));
                    if (value != null) {
                        daysInWeekRule = value.getValue();
                    } else {
                        daysInWeekRule = 127;
                    }
                }
                if (myLog.isDebugEnabled()) {
                    Object[] d = { customerParty.getKey(), fidelity, minimumTime, daysInWeekRule };
                    myLog.debug("fudge - ptyKey = {}, fidelity = {}, minimum time = {}, days in week rule = {}", d);
                }
            }
            myLog.debug(w.getProjectTask().getName());
            long weekRemainder = 0;
            if (daysInWeekRule == 124) {
                w.moveWeekendToFriday();
            }
            for (int d = 2; d < 7; d++) {
                long minutes = w.getMinutes(d);
                if (minutes > 0) {
                    if (fidelity > 0) {
                        long dayRemainder = minutes % fidelity;
                        if (dayRemainder != 0) {
                            if (weekRemainder == dayRemainder) {
                                myLog.debug("Subtracting: d = " + d + ", dayRemainder = " + dayRemainder + ", weekRemainder = " + weekRemainder);
                                w.addTime(d, -dayRemainder);
                                weekRemainder -= dayRemainder;
                            } else {
                                myLog.debug("Adding: d = " + d + ", dayRemainder = " + dayRemainder + ", weekRemainder = " + weekRemainder);
                                w.addTime(d, dayRemainder);
                                weekRemainder += dayRemainder;
                            }
                        }
                    }
                }
            }
            if (weekRemainder != 0) {
                if (treExtra == null) {
                    treExtra = new TimeRecordModel(userParty, new Date(), new Date(), username);
                }
                TimeRecordProjectTaskModel tpt = new TimeRecordProjectTaskModel(treExtra, w.getProjectTask(), 2, weekRemainder, "fudge", username);
                treExtra.addProjectTaskTimeRecord(tpt);
            }
        }
        return treExtra;
    }

    /**
   * @param calc TODO
   * @param map
   * @return last row that was updated
   * @throws IndexOutOfBoundsException
   */
    private int write(OpenOfficeCalc calc, XSpreadsheet sheet, Map<ProjectTaskModel, ProjectTaskTimeWeek> map) throws IndexOutOfBoundsException {
        int y = 1;
        for (ProjectTaskTimeWeek w : map.values()) {
            myLog.debug(w.toString());
            sheet.getCellByPosition(0, y).setFormula(w.getProjectTask().getProject().getName() + " - " + w.getProjectTask().getName());
            for (int d = 2; d < 8; d++) {
                if (w.getMinutes(d) == 0) {
                    sheet.getCellByPosition(d, y).setFormula("");
                } else {
                    XCell cell = sheet.getCellByPosition(d, y);
                    cell.setValue(w.getMinutes(d) / 60.0);
                    try {
                        calc.setCellFormat(cell, "0.0");
                    } catch (MalformedNumberFormatException exc) {
                        myLog.error("write", exc);
                    } catch (UnknownPropertyException exc) {
                        myLog.error("write", exc);
                    } catch (PropertyVetoException exc) {
                        myLog.error("write", exc);
                    } catch (IllegalArgumentException exc) {
                        myLog.error("write", exc);
                    } catch (WrappedTargetException exc) {
                        myLog.error("write", exc);
                    }
                }
            }
            if (w.getMinutes(1) == 0) {
                sheet.getCellByPosition(8, y).setFormula("");
            } else {
                XCell cell = sheet.getCellByPosition(8, y);
                cell.setValue(w.getMinutes(1) / 60.0);
                try {
                    calc.setCellFormat(cell, "0.0");
                } catch (MalformedNumberFormatException exc) {
                    myLog.error("write", exc);
                } catch (UnknownPropertyException exc) {
                    myLog.error("write", exc);
                } catch (PropertyVetoException exc) {
                    myLog.error("write", exc);
                } catch (IllegalArgumentException exc) {
                    myLog.error("write", exc);
                } catch (WrappedTargetException exc) {
                    myLog.error("write", exc);
                }
            }
            y++;
        }
        int lastRow = y - 1;
        for (; y < 100; y++) {
            for (int x = 0; x < 9; x++) {
                sheet.getCellByPosition(x, y).setFormula("");
            }
        }
        return lastRow;
    }

    /**
   * @param map
   * @return last row that was updated
   * @throws IndexOutOfBoundsException
   */
    private int write(OpenOfficeCalc calc, OpenOfficeFunction func, XSpreadsheet sheet, int row, TimeRecordModel timeRecord) throws IndexOutOfBoundsException {
        int y = Math.max(row + 5, 25);
        for (TimeRecordProjectTaskModel tpt : timeRecord.getProjectTasks()) {
            if (y == row + 5) {
                try {
                    calc.setCell(func, sheet.getCellByPosition(0, y), tpt.getTimeRecord().getStart(), true, false, "DD/MM/YY");
                    calc.setCell(func, sheet.getCellByPosition(1, y), tpt.getTimeRecord().getStart(), false, true, "HH:MM:SS");
                    calc.setCell(func, sheet.getCellByPosition(2, y), tpt.getTimeRecord().getStop(), false, true, "HH:MM:SS");
                } catch (Exception exc) {
                    myLog.error("write", exc);
                }
            }
            sheet.getCellByPosition(3, y).setValue(tpt.getMinutes());
            sheet.getCellByPosition(4, y).setFormula(tpt.getProjectTask().getProject().getName());
            sheet.getCellByPosition(5, y).setValue(tpt.getProjectTask().getKey());
            sheet.getCellByPosition(6, y).setFormula("Admin");
            try {
                calc.setCellFormat(sheet.getCellByPosition(3, y), null);
                calc.setCellFormat(sheet.getCellByPosition(5, y), null);
            } catch (MalformedNumberFormatException exc) {
                myLog.error("write", exc);
            } catch (UnknownPropertyException exc) {
                myLog.error("write", exc);
            } catch (PropertyVetoException exc) {
                myLog.error("write", exc);
            } catch (IllegalArgumentException exc) {
                myLog.error("write", exc);
            } catch (WrappedTargetException exc) {
                myLog.error("write", exc);
            }
            y++;
        }
        return y - 1;
    }

    /**
   * @param args
   */
    public static void main(String[] args) {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(new HelpOption());
        PropertyFileOption propOption = new PropertyFileOption();
        options.addOption(propOption);
        options.addOption(new FileOption());
        options.addOption(new PartyKeyOption());
        DateOption dateOption = new DateOption(DateUtil.cShortFormatPattern);
        options.addOption(dateOption);
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption(HelpOption.cValue)) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp(ReportTimeWeek.class.toString(), options);
            } else {
                Date d = dateOption.parse(line.getOptionValue(DateOption.cValue));
                int ptyKey = Integer.parseInt(line.getOptionValue(PartyKeyOption.cValue));
                File f = new File(line.getOptionValue(FileOption.cValue));
                if (!f.canRead()) {
                    System.out.println("Exiting - unable to read " + f);
                } else if (d == null) {
                    System.out.println("Exiting - date " + line.getOptionValue(DateOption.cValue) + " not recognised");
                } else {
                    EntityManagerFactory emf = null;
                    if (line.hasOption(PropertyFileOption.cValue)) {
                        try {
                            String unitName = System.getProperty(ProjectPropertiesInterface.cPersistenceUnitName, ProjectPropertiesInterface.cPersistenceUnitNameDefault);
                            emf = Persistence.createEntityManagerFactory(unitName, propOption.load());
                        } catch (IOException exc) {
                            System.err.println("Error reading properties from " + propOption.getValue());
                        }
                    } else {
                        emf = Persistence.createEntityManagerFactory("contact");
                    }
                    ReportTimeWeek rtw = new ReportTimeWeek();
                    rtw.report(f, d, ptyKey, emf);
                    emf.close();
                }
            }
            System.exit(0);
        } catch (ParseException exc) {
            System.out.println("Unexpected exception: " + exc.getMessage());
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp(ReportTimeWeek.class.toString(), options);
            System.exit(1);
        }
    }
}
