package net.sf.ussrp.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.ussrp.bus.Report;
import net.sf.ussrp.bus.web.ReportViewCommand;
import net.sf.ussrp.db.ReportDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRCsvExporterParameter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.JRXmlExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import java.sql.Connection;
import java.sql.DriverManager;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.*;

/**
 * @author Thomas Ludwig Kast (tomkast@users.sourceforge.net)
 * @version $Id: WebReportRunner.java,v 1.9 2006/05/15 17:41:13 tomkast Exp $
 */
public class WebReportRunner {

    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
	* This function is responsible for caching.  It will put the reports bytes (pdf only) or jasperPrint
	* in the model using the session object for per-user caching of report data.  The cache is flushed with
	* the query paramater "reload=true".
	* @param report the ussrp Report object to run
	* @param session the session object to put the report data into
	* @param model Map with command obejct data
	* @param style the style for this report (e.g. ReportViewCommand.PDF, etc...)
	* @param parameters another map with all of the report parameters
	* @param reload if reload=true, cache is flushed for this report id +params
	* @see net.sf.ussrp.bus.web.ReportViewCommand ReportViewCommand
	* @see net.sf.ussrp.web.WebReportRunner WebReportRunner
	* @see net.sf.ussrp.bus.web.ReportViewCommand
	* @see net.sf.ussrp.bus.web.ReportViewCommand#getParamMap(HttpServletRequest req, JasperDesign jasperDesign)
	*/
    public void runReportToWeb(Report report, HttpSession session, Map model, String style, Map parameters, String reload) {
        String cacheKey = report.getId() + parameters.values().toString();
        JasperPrint jasperPrint = (JasperPrint) session.getAttribute("jasperPrint" + cacheKey);
        byte[] bytes = (byte[]) session.getAttribute("bytes" + cacheKey);
        if ((style.equals(ReportViewCommand.PDF) && bytes == null) || (style.equals(ReportViewCommand.PDF) && reload != null)) {
            try {
                logger.info("runReportToWeb(), BYTES " + report.toString() + " about to call JasperRunManager for report " + report.getId() + ", name " + report.getJasperDesign().getName());
                if (report.getReportDataSource().getDescription().equals("JR Empty Data Source")) {
                    bytes = JasperRunManager.runReportToPdf(report.getJasperReport(), parameters, new JREmptyDataSource());
                } else {
                    Connection conn = report.getReportDataSource().getConnection();
                    bytes = JasperRunManager.runReportToPdf(report.getJasperReport(), parameters, conn);
                    conn.close();
                }
                logger.info("runReportToWeb(), BYTES " + report.toString() + " num bytes is " + bytes.length);
                session.setAttribute("bytes" + cacheKey, bytes);
            } catch (Exception e) {
                logger.error("handle() " + e);
                e.printStackTrace();
            }
        } else if (style.equals(ReportViewCommand.JRXML)) {
        } else if ((!style.equals(ReportViewCommand.PDF) && (jasperPrint == null)) || (!style.equals(ReportViewCommand.PDF) && reload != null)) {
            try {
                logger.info("runReportToWeb(), JASPER_PRINT " + report.toString() + "  about to call JasperFillManager for report " + report.getId() + ", name " + report.getJasperDesign().getName());
                if (report.getReportDataSource().getDescription().equals("JR Empty Data Source")) {
                    jasperPrint = JasperFillManager.fillReport(report.getJasperReport(), parameters, new JREmptyDataSource());
                } else {
                    if (report.getJasperReport() == null) logger.error("runReportToWeb(), got a null report.getJasperReport() is null");
                    Connection conn = report.getReportDataSource().getConnection();
                    jasperPrint = JasperFillManager.fillReport(report.getJasperReport(), parameters, conn);
                    conn.close();
                }
                logger.info("runReportToWeb(),  JASPER_PRINT " + report.toString() + " jasperPrint num pages is " + jasperPrint.getPages().size());
                session.setAttribute("jasperPrint" + cacheKey, jasperPrint);
            } catch (Exception e) {
                logger.error("handle() " + e);
                e.printStackTrace();
            }
        }
        model.put("bytes", bytes);
        model.put("jasperPrint", jasperPrint);
    }

    /**
	* This function runs several reports to a single PDF file.  All reports must run with a
	* single set of parameters.
	* @param reports the List of ussrp Report objects to run
	* @param session the session object to put the report data into
	* @param model Map with command obejct data
	* @param parameters another map with all of the report parameters
	* @see net.sf.ussrp.bus.web.ReportViewCommand ReportViewCommand
	* @see net.sf.ussrp.web.WebReportRunner WebReportRunner
	* @see net.sf.ussrp.bus.web.ReportViewCommand
	* @see net.sf.ussrp.bus.web.ReportViewCommand#getParamMap(HttpServletRequest req, JasperDesign jasperDesign)
	*/
    public void runReportsToWeb(ArrayList reports, HttpSession session, Map model, Map parameters) {
        logger.info("runReportsToWeb(), concatenation of several PDF reports");
        byte[] bytesTemp;
        ArrayList master = new ArrayList();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = null;
        PdfCopy writer = null;
        int pageOffset = 0;
        int i = 0;
        Iterator it = reports.iterator();
        Report report = null;
        while (it.hasNext()) {
            try {
                bytesTemp = null;
                report = (Report) it.next();
                if (report.getReportDataSource().getDescription().equals("JR Empty Data Source")) {
                    bytesTemp = JasperRunManager.runReportToPdf(report.getJasperReport(), parameters, new JREmptyDataSource());
                } else {
                    bytesTemp = JasperRunManager.runReportToPdf(report.getJasperReport(), parameters, report.getReportDataSource().getConnection());
                }
                logger.info("runReportsToWeb(), got report " + i + ", id " + report.getId() + ", desc " + report.getDescription() + ", num bytes is " + bytesTemp.length);
                PdfReader reader = new PdfReader(bytesTemp);
                reader.consolidateNamedDestinations();
                int n = reader.getNumberOfPages();
                List bookmarks = SimpleBookmark.getBookmark(reader);
                if (bookmarks != null) {
                    if (pageOffset != 0) SimpleBookmark.shiftPageNumbers(bookmarks, pageOffset, null);
                    master.addAll(bookmarks);
                }
                pageOffset += n;
                logger.info("runReportsToWeb(), there are " + n + " pages in report " + report.getJasperDesign().getName());
                if (i == 0) {
                    document = new Document(reader.getPageSizeWithRotation(1));
                    writer = new PdfCopy(document, baos);
                    document.open();
                }
                PdfImportedPage page;
                for (int j = 0; j < n; ) {
                    ++j;
                    page = writer.getImportedPage(reader, j);
                    writer.addPage(page);
                    logger.info("runReportsToWeb(), processed page " + j);
                }
            } catch (Exception e) {
                logger.error("runReportsToWeb(), " + e);
                e.printStackTrace();
            }
            i++;
        }
        if (master.size() > 0) writer.setOutlines(master);
        document.close();
        byte[] bytes = baos.toByteArray();
        model.put("bytes", bytes);
    }

    /**
	* Works just like runReportToWeb except that the resulting report is saved to the file system.
	* There is no cache used for this function.
	* @param report the ussrp Report object to run
	* @param style the style for this report (e.g. ReportViewCommand.PDF, etc...)
	* @param parameters map with all of the report parameters
	* @param archive a fully-qualified path to a folder on the file system to save the report into.
	*/
    public void runReportToFile(Report report, String style, Map parameters, String archive) {
        String pathSep = System.getProperty("file.separator");
        String fileString = archive + pathSep + report.getJasperDesign().getName();
        byte[] bytes = null;
        JasperPrint jasperPrint = null;
        try {
            logger.info("runReportToFile, about to run " + style + ", " + fileString);
            if (style.equals(ReportViewCommand.PDF)) if (report.getReportDataSource().getDescription().equals("JR Empty Data Source")) {
                bytes = JasperRunManager.runReportToPdf(report.getJasperReport(), parameters, new JREmptyDataSource());
            } else {
                Connection conn = report.getReportDataSource().getConnection();
                bytes = JasperRunManager.runReportToPdf(report.getJasperReport(), parameters, conn);
                conn.close();
            } else if (report.getReportDataSource().getDescription().equals("JR Empty Data Source")) {
                jasperPrint = JasperFillManager.fillReport(report.getJasperReport(), parameters, new JREmptyDataSource());
            } else {
                Connection conn = report.getReportDataSource().getConnection();
                jasperPrint = JasperFillManager.fillReport(report.getJasperReport(), parameters, conn);
                conn.close();
            }
            if ((bytes != null) || (jasperPrint != null)) logger.info("runReportToFile, got data for " + style + ", " + fileString);
            if (style.equals(ReportViewCommand.JRXML)) {
                bytes = report.jasperDesignToXml(report.getJasperDesign()).getBytes();
                fileString += ".jrxml";
                FileOutputStream fw = new FileOutputStream(new File(fileString));
                fw.write(bytes, 0, bytes.length);
                fw.flush();
                fw.close();
                logger.info("runReportToFile, completed " + fileString + ", num bytes is..." + bytes.length);
            } else if (style.equals(ReportViewCommand.PDF)) {
                fileString += ".pdf";
                FileOutputStream fw = new FileOutputStream(new File(fileString));
                fw.write(bytes, 0, bytes.length);
                fw.flush();
                fw.close();
                logger.info("runReportToFile, completed " + fileString + ", num bytes is..." + bytes.length);
            } else if (style.equals(ReportViewCommand.CSV)) {
                fileString += ".csv";
                File f = new File(fileString);
                FileOutputStream fw = new FileOutputStream(f);
                JRCsvExporter exporter = new JRCsvExporter();
                exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
                exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, fw);
                exporter.exportReport();
                fw.flush();
                fw.close();
                logger.info("runReportToFile, completed " + fileString + ", num bytes is " + f.length());
            } else if (style.equals(ReportViewCommand.XLS)) {
                fileString += ".xls";
                File f = new File(fileString);
                FileOutputStream fw = new FileOutputStream(f);
                JRXlsExporter exporter = new JRXlsExporter();
                exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
                exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, fw);
                exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
                exporter.exportReport();
                fw.flush();
                fw.close();
                logger.info("runReportToFile, completed " + fileString + ", num bytes is " + f.length());
            } else if (style.equals(ReportViewCommand.XML)) {
                fileString += ".xml";
                File f = new File(fileString);
                FileOutputStream fw = new FileOutputStream(f);
                JRXmlExporter exporter = new JRXmlExporter();
                exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
                exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, fw);
                exporter.setParameter(JRXmlExporterParameter.IS_EMBEDDING_IMAGES, Boolean.FALSE);
                exporter.exportReport();
                fw.flush();
                fw.close();
                logger.info("runReportToFile, completed " + fileString + ", num bytes is " + f.length());
            } else if (style.equals(ReportViewCommand.RTF)) {
                fileString += ".rtf";
                File f = new File(fileString);
                FileOutputStream fw = new FileOutputStream(f);
                JRRtfExporter exporter = new JRRtfExporter();
                exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
                exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, fw);
                exporter.exportReport();
                fw.flush();
                fw.close();
                logger.info("runReportToFile, completed " + fileString + ", num bytes is " + f.length());
            }
        } catch (Exception e) {
            logger.error("runReportToFile() " + e);
            e.printStackTrace();
        }
    }
}
