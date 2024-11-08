package au.com.cahaya.hubung.project.time;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
import au.com.cahaya.asas.ds.party.model.PartyAddressModel;
import au.com.cahaya.asas.ds.party.model.PartyModel;
import au.com.cahaya.asas.ds.party.model.PartyNameModel;
import au.com.cahaya.asas.ds.party.model.PartyRelationshipModel;
import au.com.cahaya.asas.ds.party.model.types.AddressType;
import au.com.cahaya.asas.ds.party.model.types.PartyNameType;
import au.com.cahaya.asas.ds.party.model.types.PartyRelationshipType;
import au.com.cahaya.asas.ds.party.model.types.PartyUsageType;
import au.com.cahaya.asas.ds.prefs.model.PreferenceCategoryModel;
import au.com.cahaya.asas.ds.prefs.model.PreferenceDefineModel;
import au.com.cahaya.asas.ds.prefs.model.PreferenceValueStringModel;
import au.com.cahaya.asas.ds.util.cli.PartyKeyOption;
import au.com.cahaya.asas.net.NetUtil;
import au.com.cahaya.asas.openoffice.OpenOfficeCalc;
import au.com.cahaya.asas.openoffice.OpenOfficeFunction;
import au.com.cahaya.asas.openoffice.OpenOfficeWriter;
import au.com.cahaya.asas.util.CalendarUtil;
import au.com.cahaya.asas.util.DateUtil;
import au.com.cahaya.asas.util.cli.DateOption;
import au.com.cahaya.asas.util.cli.DummyRunOption;
import au.com.cahaya.asas.util.cli.FileOption;
import au.com.cahaya.asas.util.cli.HelpOption;
import au.com.cahaya.asas.util.cli.PropertyFileOption;
import au.com.cahaya.asas.util.cli.TemplateDirOption;
import au.com.cahaya.hubung.project.ProjectPropertiesInterface;
import au.com.cahaya.hubung.project.adt.ProjectTaskTime;
import au.com.cahaya.hubung.project.model.FinancePartyModel;
import au.com.cahaya.hubung.project.model.ProjectModel;
import au.com.cahaya.hubung.project.model.ProjectTaskModel;
import au.com.cahaya.hubung.project.model.TimeRecordModel;
import au.com.cahaya.hubung.project.model.TimeRecordProjectTaskModel;
import au.com.cahaya.hubung.project.model.types.ReportGranularityType;
import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;
import com.sun.star.text.XTextTable;
import com.sun.star.text.XTextTableCursor;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

/**
 *
 *
 * @author Mathew Pole
 * @since June 2008
 * @version $Revision$
 */
public class ReportTimeMonth extends ReportTimeBase {

    /** The private logger for this class */
    private Logger myLog = LoggerFactory.getLogger(ReportTimeMonth.class);

    /** Should invoices be saved after creation? */
    private boolean myIsSave = true;

    /**
   * Constructor
   */
    public ReportTimeMonth() {
    }

    /**
   * 
   */
    public void setIsSave(boolean isSave) {
        myIsSave = isSave;
    }

    /**
   *
   */
    @Override
    public void report(File file, Date date, long ptyKey, EntityManagerFactory emf) {
        this.report(file, new File("invoice_daily.ott"), date, ptyKey, emf);
    }

    /**
   *
   */
    public void report(File file, File templateDir, Date date, long ptyKey, EntityManagerFactory emf) {
        myLog.debug("report(" + file + ") - enter");
        EntityManager em = emf.createEntityManager();
        PreferenceCategoryModel category = loadPreferences(em);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        Date start = CalendarUtil.getStartOfMonth(c, 0);
        Date end = CalendarUtil.getStartOfMonth(c, 1);
        TreeSet<TimeRecordModel> treSet = null;
        if (ptyKey == -1) {
            treSet = retrieveTimeRecord(em, start, end);
        } else {
            treSet = retrieveTimeRecord(em, start, end, ptyKey);
        }
        Map<ProjectTaskModel, ProjectTaskTime> map = process(treSet);
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
            XSpreadsheet sheet = calc.retrieveSheet(3);
            int lastRow = writeMonthSummary(calc, func, sheet, start, map);
            if (templateDir != null) {
                writeInvoice(em, connection, templateDir, start, category, map);
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
   *
   */
    private Map<ProjectTaskModel, ProjectTaskTime> process(Set<TimeRecordModel> treSet) {
        TreeMap<ProjectTaskModel, ProjectTaskTime> map = new TreeMap<ProjectTaskModel, ProjectTaskTime>();
        for (TimeRecordModel tre : treSet) {
            for (TimeRecordProjectTaskModel tpt : tre.getProjectTasks()) {
                ProjectTaskTime t = map.get(tpt.getProjectTask());
                if (t == null) {
                    t = new ProjectTaskTime(tpt.getProjectTask());
                    map.put(tpt.getProjectTask(), t);
                }
                t.addTimeRecord(tpt);
            }
        }
        return map;
    }

    /**
   * This method updates the month sheet in the spreadsheet.
   * 
   * @param map
   * @return last row that was updated
   * @throws IndexOutOfBoundsException
   */
    private int writeMonthSummary(OpenOfficeCalc calc, OpenOfficeFunction func, XSpreadsheet sheet, Date date, Map<ProjectTaskModel, ProjectTaskTime> map) throws IndexOutOfBoundsException {
        myLog.debug("write - {}", date);
        try {
            calc.setCell(func, sheet.getCellByPosition(1, 0), date, true, false, "MMMM YYYY");
        } catch (Exception exc) {
            myLog.error("write", exc);
        }
        int y = 1;
        int yStartParty = 1;
        PartyModel party = null;
        for (ProjectTaskTime t : map.values()) {
            myLog.debug("write - y = {}, t = {}", y, t.toString());
            if (party == null) {
                party = t.getProjectTask().getProject().getParty();
                sheet.getCellByPosition(0, y).setFormula(party.getNameAsString());
            } else if (party != t.getProjectTask().getProject().getParty()) {
                sheet.getCellByPosition(3, y - 1).setFormula("=sum(C" + (yStartParty + 1) + ":C" + y);
                party = t.getProjectTask().getProject().getParty();
                for (int x = 0; x < 9; x++) {
                    sheet.getCellByPosition(x, y).setFormula("");
                }
                y++;
                sheet.getCellByPosition(0, y).setFormula(party.getNameAsString());
                yStartParty = y;
            } else {
                sheet.getCellByPosition(0, y).setFormula("");
            }
            sheet.getCellByPosition(1, y).setFormula(t.getProjectTask().getProject().getName() + " - " + t.getProjectTask().getName());
            sheet.getCellByPosition(2, y).setValue(t.getMinutes() / 60.0);
            sheet.getCellByPosition(3, y).setFormula("");
            sheet.getCellByPosition(4, y).setFormula("");
            y++;
        }
        sheet.getCellByPosition(3, y - 1).setFormula("=sum(C" + (yStartParty + 1) + ":C" + y);
        for (int x = 0; x < 9; x++) {
            sheet.getCellByPosition(x, y).setFormula("");
        }
        y++;
        sheet.getCellByPosition(1, y).setFormula("Total for Month");
        sheet.getCellByPosition(2, y).setFormula("=sum(C2:C" + y);
        sheet.getCellByPosition(3, y).setFormula("=sum(C2:C" + y);
        myLog.debug("write - y = {}", y);
        int lastRow = y++;
        for (; y < 100; y++) {
            for (int x = 0; x < 9; x++) {
                sheet.getCellByPosition(x, y).setFormula("");
            }
        }
        return lastRow;
    }

    /**
   * This method creates an invoice for each party.
   * 
   * @throws Exception
   */
    private void writeInvoice(EntityManager em, OpenOfficeConnection connection, File templateDir, Date date, PreferenceCategoryModel category, Map<ProjectTaskModel, ProjectTaskTime> map) throws Exception {
        PartyModel party = null;
        List<ProjectTaskTime> partyList = new ArrayList<ProjectTaskTime>();
        for (ProjectTaskTime t : map.values()) {
            if (party == null) {
                myLog.debug("writeInvoice - first party {}", t.getProjectTask().getProject().getParty());
                party = t.getProjectTask().getProject().getParty();
            } else if (party != null && !party.isSame(t.getProjectTask().getProject().getParty())) {
                myLog.debug("writeInvoice - different party {}", t.getProjectTask().getProject().getParty());
                writeInvoiceParty(em, connection, templateDir, date, category, partyList);
                partyList = new ArrayList<ProjectTaskTime>();
                party = t.getProjectTask().getProject().getParty();
            }
            partyList.add(t);
        }
        if (partyList.size() > 0) {
            writeInvoiceParty(em, connection, templateDir, date, category, partyList);
        }
    }

    /**
   * This method creates an invoice for each party.
   * 
   * @throws Exception
   */
    private void writeInvoiceParty(EntityManager em, OpenOfficeConnection connection, File templateDir, Date date, PreferenceCategoryModel category, List<ProjectTaskTime> partyList) throws Exception {
        ProjectModel project = partyList.get(0).getProjectTask().getProject();
        PartyModel party = project.getParty();
        myLog.debug("writeInvoiceParty - party = {}", party);
        FinancePartyModel finance = retrieveFinance(em, party);
        ;
        PartyNameModel partyNameShort = party.getName(PartyNameType.eShort.getCode());
        String partyNameShortStr = "xxx";
        if (partyNameShort != null) {
            partyNameShortStr = partyNameShort.getName();
        } else {
            Object[] tokens = { PartyNameType.eShort, party.getKey(), party.getName() };
            myLog.warn("writeInvoiceParty - {} not specified for {} - {}", tokens);
        }
        PreferenceCategoryModel reportCategory = category.getChild(ProjectTimePreferencesInterface.cReportCategory);
        PreferenceDefineModel granularityDefine = reportCategory.getDefinition(ProjectTimePreferencesInterface.cReportMonthProjectGranularity);
        ReportGranularityType granularity = ReportGranularityType.eProjectTask;
        myLog.warn("writeInvoiceParty - granularityDefine = {}", granularityDefine);
        if (granularityDefine != null) {
            PreferenceValueStringModel value = (PreferenceValueStringModel) (granularityDefine.getValue(party));
            if (value != null) {
                granularity = ReportGranularityType.decode(value.getValue());
                myLog.debug("writeInvoiceParty - granularity = {}", granularity);
            }
            if (granularity == null) {
                myLog.debug("writeInvoiceParty - granularity is null");
                granularity = ReportGranularityType.eProjectTask;
            }
        } else {
            myLog.warn("writeInvoiceParty - preference definition {} not found.", ProjectTimePreferencesInterface.cReportMonthProjectGranularity);
        }
        PreferenceDefineModel templateDefine = reportCategory.getDefinition(ProjectTimePreferencesInterface.cReportInvoiceTemplate);
        String templateFileName = "invoice.ott";
        if (templateDefine != null) {
            PreferenceValueStringModel value = (PreferenceValueStringModel) (templateDefine.getValue(party));
            if (value != null) {
                templateFileName = value.getValue();
                myLog.debug("writeInvoiceParty - templateFileName = {}", templateFileName);
            }
            if (templateFileName == null) {
                myLog.debug("writeInvoiceParty - templateFileName is null");
                templateFileName = "invoice.ott";
            }
        } else {
            myLog.warn("writeInvoiceParty - preference definition {} not found.", ProjectTimePreferencesInterface.cReportInvoiceTemplate);
        }
        Date now = new Date();
        OpenOfficeWriter text = writeInvoiceHeader(connection, now, new File(templateDir, templateFileName), party, partyNameShortStr);
        myLog.debug("writeInvoiceParty - granularity = {}", granularity);
        switch(granularity) {
            case eProject:
                writeInvoiceTimeProject(text, finance, partyList);
                break;
            case eProjectTask:
                writeInvoiceTimeProjectTask(text, finance, partyList);
                break;
            case eDaily:
                writeInvoiceTimeDaily(text, finance, partyList);
                break;
            default:
                myLog.error("writeInvoiceParty - granularity {} not supported", granularity);
        }
        if (myIsSave) {
            File savePath = determineSavePath(templateDir, now);
            DefaultDocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
            DocumentFormat pdfFormat = formatRegistry.getFormatByFileExtension("pdf");
            saveInvoice(text, now, partyNameShortStr, savePath, pdfFormat);
        }
    }

    /**
   * Write the header portion of the invoice.
   */
    private OpenOfficeWriter writeInvoiceHeader(OpenOfficeConnection connection, Date now, File templateFile, PartyModel party, String partyNameShort) throws Exception {
        PartyNameModel partyName = party.getName();
        String partyNameStr = null;
        if (partyName != null) {
            partyNameStr = partyName.getName();
        } else {
            partyNameStr = Long.toString(party.getKey());
        }
        String invoiceNo = DateUtil.format(now, "yyyy-MM") + "-" + partyNameShort.toLowerCase() + "-001";
        PartyRelationshipModel invoicePartyRel = party.getRelationshipFrom(PartyRelationshipType.eContact, true);
        if (invoicePartyRel == null) {
            invoicePartyRel = party.getRelationshipFrom(PartyRelationshipType.eContact, false);
            if (invoicePartyRel == null) {
                invoicePartyRel = party.getRelationshipFrom(PartyRelationshipType.eAccounts, true);
                if (invoicePartyRel == null) {
                    invoicePartyRel = party.getRelationshipFrom(PartyRelationshipType.eAccounts, false);
                }
            }
        }
        PartyModel invoiceParty;
        String invoicePartyName = null;
        if (invoicePartyRel != null) {
            invoiceParty = invoicePartyRel.getPartyTo();
            invoicePartyName = invoiceParty.getNameAsString();
        } else {
            invoiceParty = party;
        }
        myLog.debug("writeInvoiceHeader - invoiceParty = {}", invoiceParty);
        PartyUsageType[] invoiceUsageTypes = { PartyUsageType.eWork, PartyUsageType.eHome };
        AddressType[] invoiceAddressTypes = { AddressType.ePost, AddressType.eLocation, AddressType.eDelivery };
        PartyAddressModel address = invoiceParty.getPartyAddress(invoiceUsageTypes, invoiceAddressTypes, true);
        if (address == null && invoiceParty != party) {
            address = party.getPartyAddress(invoiceUsageTypes, invoiceAddressTypes, true);
        }
        StringBuffer sb = new StringBuffer();
        boolean addSeparator = false;
        if (invoicePartyName != null && invoicePartyName.length() > 0) {
            sb.append(invoicePartyName);
            addSeparator = true;
        }
        if (invoicePartyRel != null && invoicePartyRel.getTitle() != null && invoicePartyRel.getTitle().length() > 0) {
            if (addSeparator) {
                sb.append("\n");
            } else {
                addSeparator = true;
            }
            sb.append(invoicePartyRel.getTitle());
        }
        if (addSeparator) {
            sb.append("\n");
        } else {
            addSeparator = true;
        }
        sb.append(partyNameStr);
        if (address != null) {
            if (addSeparator) {
                sb.append("\n");
            } else {
                addSeparator = true;
            }
            address.getAddress().toString(sb, "\n");
        }
        String templateUri = NetUtil.uriToString(templateFile.toURI());
        myLog.debug("writeInvoiceHeader - templateUri = {}", templateUri);
        OpenOfficeWriter text = new OpenOfficeWriter(connection);
        text.create(partyNameStr + " Invoice", templateUri, false);
        XTextTable table = text.getTable("InvoiceHeader");
        XCellRange xCellRange = (XCellRange) (UnoRuntime.queryInterface(XCellRange.class, table));
        text.setCell(xCellRange.getCellByPosition(1, 0), invoiceNo);
        text.setCell(xCellRange.getCellByPosition(1, 1), DateUtil.format(now, "d MMMM yyyy"));
        text.setCell(xCellRange.getCellByPosition(1, 2), sb.toString());
        return text;
    }

    /**
   * Generate an invoice which shows only project lines.
   * 
   * @throws WrappedTargetException 
   * @throws IndexOutOfBoundsException 
   */
    private void writeInvoiceTimeProject(OpenOfficeWriter text, FinancePartyModel finance, List<ProjectTaskTime> projectTaskTimes) throws WrappedTargetException, IndexOutOfBoundsException {
        ProjectModel project = null;
        int row = 0;
        long projectMinutes = 0;
        DecimalFormat decFormat = new DecimalFormat("$###,###,###.00");
        XTextTable table = text.getTable("InvoiceDetail");
        XCellRange xCellRange = (XCellRange) (UnoRuntime.queryInterface(XCellRange.class, table));
        for (ProjectTaskTime t : projectTaskTimes) {
            if (project == null || !project.equals(t.getProjectTask().getProject())) {
                project = t.getProjectTask().getProject();
                if (projectMinutes > 0) {
                    if (finance == null) {
                        text.setCell(xCellRange.getCellByPosition(3, row), projectMinutes + "m");
                    } else {
                        text.setCell(xCellRange.getCellByPosition(3, row), decFormat.format(finance.getInvoiceHour().multiply(new BigDecimal(projectMinutes / 60.0))));
                    }
                }
                row++;
                if (row > 1) {
                    table.getRows().insertByIndex(row, 1);
                }
                text.setCell(xCellRange.getCellByPosition(1, row), t.getProjectTask().getProject().getName());
                projectMinutes = 0;
            }
            projectMinutes += t.getMinutes();
        }
        if (projectMinutes > 0) {
            if (finance == null) {
                text.setCell(xCellRange.getCellByPosition(3, row), projectMinutes + "m");
            } else {
                text.setCell(xCellRange.getCellByPosition(3, row), decFormat.format(finance.getInvoiceHour().multiply(new BigDecimal(projectMinutes / 60.0))));
            }
        }
    }

    /**
   * Generate an invoice which shows only a line for each task in a project
   * 
   * @throws WrappedTargetException 
   * @throws IndexOutOfBoundsException 
   */
    private void writeInvoiceTimeProjectTask(OpenOfficeWriter text, FinancePartyModel finance, List<ProjectTaskTime> projectTaskTimes) throws WrappedTargetException, IndexOutOfBoundsException {
        int row = 0;
        DecimalFormat decFormat = new DecimalFormat("$###,###,###.00");
        XTextTable table = text.getTable("InvoiceDetail");
        XCellRange xCellRange = (XCellRange) (UnoRuntime.queryInterface(XCellRange.class, table));
        for (ProjectTaskTime t : projectTaskTimes) {
            row++;
            if (row > 1) {
                table.getRows().insertByIndex(row, 1);
            }
            text.setCell(xCellRange.getCellByPosition(1, row), t.getProjectTask().getProject().getName() + " - " + t.getProjectTask().getName());
            if (finance == null) {
                text.setCell(xCellRange.getCellByPosition(3, row), t.getMinutes() + "m");
            } else {
                text.setCell(xCellRange.getCellByPosition(3, row), decFormat.format(finance.getInvoiceHour().multiply(new BigDecimal(t.getMinutes() / 60.0))));
            }
        }
    }

    /**
   * Generate an invoice which shows the daily charges for each task in a project
   * 
   * @throws WrappedTargetException 
   * @throws IndexOutOfBoundsException 
   */
    private void writeInvoiceTimeDaily(OpenOfficeWriter text, FinancePartyModel finance, List<ProjectTaskTime> projectTaskTimes) throws WrappedTargetException, IndexOutOfBoundsException {
        int row = 0;
        DecimalFormat decFormat = new DecimalFormat("$###,###,###.00");
        XTextTable table = text.getTable("InvoiceDetail");
        XCellRange xCellRange = (XCellRange) (UnoRuntime.queryInterface(XCellRange.class, table));
        for (ProjectTaskTime t : projectTaskTimes) {
            row++;
            if (row > 1) {
                table.getRows().insertByIndex(row, 1);
            }
            int taskTitleRow = row;
            text.setCell(xCellRange.getCellByPosition(0, row), t.getProjectTask().getProject().getName() + " - " + t.getProjectTask().getName());
            XTextTableCursor cursor = table.createCursorByCellName("A" + (row + 1));
            cursor.gotoCellByName("C" + (row + 1), true);
            cursor.mergeRange();
            try {
                text.setCellParagraphStyle(table, 0, row, "TaskTitle");
                text.setCellParagraphStyle(table, 1, row, "TaskTotal");
            } catch (UnknownPropertyException exc) {
                myLog.error("writeInvoiceTimeDaily", exc);
            } catch (PropertyVetoException exc) {
                myLog.error("writeInvoiceTimeDaily", exc);
            } catch (IllegalArgumentException exc) {
                myLog.error("writeInvoiceTimeDaily", exc);
            }
            Calendar day = null;
            StringBuffer description = null;
            long dayMinutes = 0;
            for (TimeRecordProjectTaskModel tpt : t.getTimeRecords()) {
                if (day == null || !CalendarUtil.isSameDate(day, tpt.getTimeRecord().getStart())) {
                    if (day != null) {
                        table.getRows().insertByIndex(++row, 1);
                        text.setCell(xCellRange.getCellByPosition(0, row), DateUtil.format(day.getTime(), DateUtil.cShortFormatPattern));
                        text.setCell(xCellRange.getCellByPosition(1, row), description.toString());
                        if (finance == null) {
                            text.setCell(xCellRange.getCellByPosition(2, row), dayMinutes + "m");
                        } else {
                            text.setCell(xCellRange.getCellByPosition(2, row), decFormat.format(finance.getInvoiceHour().multiply(new BigDecimal(dayMinutes / 60.0))));
                        }
                    }
                    day = Calendar.getInstance();
                    day.setTime(tpt.getTimeRecord().getStart());
                    description = new StringBuffer();
                    dayMinutes = 0;
                }
                if (description.indexOf(tpt.getNote()) < 0) {
                    if (description.length() > 0) {
                        description.append("; ");
                    }
                    description.append(tpt.getNote());
                }
                dayMinutes += tpt.getMinutes();
            }
            if (day != null) {
                table.getRows().insertByIndex(++row, 1);
                text.setCell(xCellRange.getCellByPosition(0, row), DateUtil.format(day.getTime(), DateUtil.cShortFormatPattern));
                text.setCell(xCellRange.getCellByPosition(1, row), description.toString());
                if (finance == null) {
                    text.setCell(xCellRange.getCellByPosition(2, row), dayMinutes + "m");
                } else {
                    text.setCell(xCellRange.getCellByPosition(2, row), decFormat.format(finance.getInvoiceHour().multiply(new BigDecimal(dayMinutes / 60.0))));
                }
            }
            if (row > 1) {
                table.getRows().insertByIndex(++row, 1);
                if (finance == null) {
                    text.setCell(xCellRange.getCellByPosition(1, taskTitleRow), t.getMinutes() + "m");
                } else {
                    text.setCell(xCellRange.getCellByPosition(1, taskTitleRow), decFormat.format(finance.getInvoiceHour().multiply(new BigDecimal(t.getMinutes() / 60.0))));
                }
            }
        }
    }

    /**
   * @param templateDir
   * @param date
   * @return
   */
    private File determineSavePath(File templateDir, Date now) {
        StringBuilder dir = new StringBuilder("invoice_");
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        if (month < Calendar.JULY) {
            dir.append(year - 1);
            dir.append("_");
        }
        dir.append(year);
        if (month > Calendar.JUNE) {
            dir.append("_");
            dir.append(year + 1);
        }
        File path = new File(templateDir, dir.toString());
        if (!path.exists()) {
            path.mkdirs();
        }
        return path;
    }

    /**
   * @param text
   * @param now
   * @param partyNameShortStr
   * @param savePath
   * @param pdfFormat
   * @throws IOException
   */
    private void saveInvoice(OpenOfficeWriter text, Date now, String partyNameShortStr, File savePath, DocumentFormat pdfFormat) throws com.sun.star.io.IOException {
        myLog.debug("saveInvoice - pdfFormat = {}", pdfFormat);
        if (text != null && !"xxx".equals(partyNameShortStr)) {
            String fileName = "invoice_" + DateUtil.format(now, "yyyy_MM") + "_" + partyNameShortStr.toLowerCase() + "_001";
            String fileUri = NetUtil.uriToString(new File(savePath, fileName + ".pdf").toURI());
            text.save(fileUri, pdfFormat, true);
            fileUri = NetUtil.uriToString(new File(savePath, fileName + OpenOfficeWriter.cDefaultFileExtension).toURI());
            text.save(fileUri);
        }
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
        options.addOption(new TemplateDirOption());
        DateOption dateOption = new DateOption(DateUtil.cShortFormatPattern);
        options.addOption(dateOption);
        PartyKeyOption partyKeyOption = new PartyKeyOption();
        options.addOption(partyKeyOption);
        options.addOption(new DummyRunOption());
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption(HelpOption.cValue)) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp(ReportTimeWeek.class.toString(), options);
            } else {
                boolean isDummyRun = line.hasOption(DummyRunOption.cValue);
                Date d = dateOption.parse(line.getOptionValue(DateOption.cValue));
                File f = new File(line.getOptionValue(FileOption.cValue));
                File tf = null;
                if (line.hasOption(TemplateDirOption.cValue)) {
                    tf = new File(line.getOptionValue(TemplateDirOption.cValue));
                }
                if (!f.canRead()) {
                    System.out.println("Exiting - unable to read " + f);
                } else if (tf != null && !tf.canRead()) {
                    System.out.println("Exiting - unable to read " + tf);
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
                        emf = Persistence.createEntityManagerFactory("project");
                    }
                    long partyKey = -1;
                    if (line.hasOption(PartyKeyOption.cValue)) {
                        partyKey = partyKeyOption.parseAsLong();
                    }
                    ReportTimeMonth rtm = new ReportTimeMonth();
                    if (isDummyRun) {
                        rtm.setIsSave(false);
                    }
                    rtm.report(f, tf, d, partyKey, emf);
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
