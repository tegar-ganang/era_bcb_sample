package org.appspy.viewer.birt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.HashMap;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.appspy.server.bo.Report;
import org.appspy.viewer.service.ReportService;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Olivier HEDIN / olivier@appspy.org
 */
public class BirtFiltrer implements Filter {

    protected static Log sLog = LogFactory.getLog(BirtFiltrer.class);

    protected FilterConfig mFilterConfig = null;

    protected HashMap<Long, Timestamp> mReportVersions = new HashMap<Long, Timestamp>();

    public void destroy() {
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(mFilterConfig.getServletContext());
        ReportService reportService = (ReportService) ctx.getBean("reportService");
        String reportName = req.getParameter("__report");
        try {
            if (reportName != null) {
                Report report = reportService.findReportByName(reportName);
                if (report == null) {
                    if (sLog.isWarnEnabled()) {
                        sLog.warn("Report not found : " + reportName);
                    }
                } else {
                    if (sLog.isInfoEnabled()) {
                        sLog.debug("Report found : " + reportName);
                    }
                    Long reportId = report.getId();
                    if (mReportVersions.containsKey(reportId)) {
                        if (!mReportVersions.get(reportId).equals(report.getLastUpdate())) {
                            sLog.debug("Found a different version of the report on the file system, copying");
                            copyReportFile(req, reportName, report);
                            mReportVersions.put(reportId, report.getLastUpdate());
                        }
                    } else {
                        sLog.debug("Not found on the file system, copying");
                        copyReportFile(req, reportName, report);
                        mReportVersions.put(reportId, report.getLastUpdate());
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        chain.doFilter(req, res);
    }

    /**
	 * @param req
	 * @param reportName
	 * @param report
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    private void copyReportFile(ServletRequest req, String reportName, Report report) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, FileNotFoundException, IOException {
        String reportFileName = (String) Class.forName("org.eclipse.birt.report.utility.ParameterAccessor").getMethod("getReport", new Class[] { HttpServletRequest.class, String.class }).invoke(null, new Object[] { req, reportName });
        ByteArrayInputStream bais = new ByteArrayInputStream(report.getReportContent());
        FileOutputStream fos = new FileOutputStream(new File(reportFileName));
        IOUtils.copy(bais, fos);
        bais.close();
        fos.close();
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        mFilterConfig = filterConfig;
    }
}
