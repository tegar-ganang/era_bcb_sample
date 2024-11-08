package br.revista.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import br.revista.exception.ReportException;

public class PdfReport implements WebReport {

    public PdfReport() {
    }

    public void download(String filename, InputStream reportStream, Map params, List list) throws ReportException {
        String pathReport = filename;
        File report = new File(pathReport);
        byte[] buffer = null;
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
            if (params == null) {
                params = new HashMap();
            }
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(list);
            JasperPrint print = JasperFillManager.fillReport(reportStream, params, ds);
            JasperExportManager.exportReportToPdfFile(print, pathReport);
            FileInputStream fis = new FileInputStream(report);
            OutputStream os = response.getOutputStream();
            int read = 0;
            buffer = new byte[1024];
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            os.close();
            fis.close();
            FacesContext.getCurrentInstance().responseComplete();
        } catch (Throwable ex) {
            throw new ReportException(ex);
        } finally {
            buffer = null;
            report.delete();
        }
    }
}
