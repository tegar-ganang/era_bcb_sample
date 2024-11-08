package de.flingelli.scrum.datastructure.reports;

import jancilla.system.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.flingelli.scrum.Global;
import de.flingelli.scrum.datastructure.Product;
import de.flingelli.scrum.datastructure.Sprint;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

public final class ReportGeneration {

    private static final String JASPER_REPORT = "jasperReport";

    private static final String JRXML_EXTENSION = ".jrxml";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportGeneration.class);

    private ReportGeneration() {
    }

    @SuppressWarnings("unchecked")
    public static JasperPrint compileReport(String mainReportFileNameWithoutExtension, String subReportFileNameWithoutExtension, JRDataSource mainDataSource, JRDataSource subReportDataSource) throws JRException, IOException {
        File tempSubreportsDirectory = new File(System.getProperty("java.io.tmpdir"), "subreports");
        if (!tempSubreportsDirectory.exists()) {
            tempSubreportsDirectory.mkdir();
        }
        File mainFileReport = File.createTempFile(JASPER_REPORT, JRXML_EXTENSION);
        File subFileReport = File.createTempFile(JASPER_REPORT, JRXML_EXTENSION, tempSubreportsDirectory);
        FileUtils.copyFile(new File(mainReportFileNameWithoutExtension + JRXML_EXTENSION), mainFileReport);
        FileUtils.copyFile(new File(subReportFileNameWithoutExtension + JRXML_EXTENSION), subFileReport);
        File tempReportFile = File.createTempFile("jasperreport", ".jasper");
        String prefix = subReportFileNameWithoutExtension.substring(subReportFileNameWithoutExtension.lastIndexOf('/') + 1);
        File tempSubReportFile = new File(tempSubreportsDirectory, prefix + ".jasper");
        JasperCompileManager.compileReportToFile(mainFileReport.getAbsolutePath(), tempReportFile.getAbsolutePath());
        JasperCompileManager.compileReportToFile(subFileReport.getAbsolutePath(), tempSubReportFile.getAbsolutePath());
        @SuppressWarnings("rawtypes") Map parameters = new HashMap();
        parameters.put("EXTERNAL_DATA", mainDataSource);
        JasperPrint result = JasperFillManager.fillReport(tempReportFile.getAbsolutePath(), parameters, subReportDataSource);
        try {
            mainFileReport.delete();
            subFileReport.delete();
            tempReportFile.delete();
            tempSubReportFile.delete();
            tempSubreportsDirectory.delete();
        } catch (Exception e) {
            LOGGER.error("Temp files could not be deleted.", e);
        }
        return result;
    }

    public static void removePages(int minimumElementCount, JasperPrint jasperPrint) {
        @SuppressWarnings("unchecked") List<JRPrintPage> pages = jasperPrint.getPages();
        for (Iterator<JRPrintPage> i = pages.iterator(); i.hasNext(); ) {
            JRPrintPage page = i.next();
            if (page.getElements().size() < minimumElementCount) {
                i.remove();
            }
        }
    }

    public static JasperPrint generateProductBacklogReport(Product product) {
        JasperPrint result = null;
        try {
            result = createReport(Global.REPORT_FILE_PRODUCT, Global.REPORT_FILE_BACKLOGITEMS, product);
            ReportGeneration.removePages(6, result);
        } catch (JRException e) {
            LOGGER.error("Product backlog report couldn't be created.", e);
        } catch (IOException e) {
            LOGGER.error("Sprint estimation report couldn't be created.", e);
        }
        return result;
    }

    public static JasperPrint generateSprintEstimationReport(Product product, Sprint sprint) {
        JasperPrint result = null;
        try {
            result = createSprintEstimationReport(Global.REPORT_FILE_SPRINT_ESTIMATION, Global.SUBREPORT_FILE_SPRINT_ESTIMATION, product, sprint);
        } catch (JRException e) {
            LOGGER.error("Sprint estimation report couldn't be created.", e);
        } catch (IOException e) {
            LOGGER.error("Sprint estimation report couldn't be created.", e);
        }
        return result;
    }

    private static JasperPrint createReport(String mainFileName, String subReportFileName, Product product) throws JRException, IOException {
        JasperPrint jasperPrint = ReportGeneration.compileReport(mainFileName, subReportFileName, new BacklogItemDataSource(product), new ProductDataSource(product));
        return jasperPrint;
    }

    private static JasperPrint createSprintEstimationReport(String mainFileName, String subReportFileName, Product product, Sprint sprint) throws JRException, IOException {
        JasperPrint jasperPrint = ReportGeneration.compileReport(mainFileName, subReportFileName, new SprintDataSource(product, sprint), new SprintInformationDataSource(sprint));
        return jasperPrint;
    }
}
