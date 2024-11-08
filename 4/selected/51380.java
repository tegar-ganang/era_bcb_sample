package es.devel.opentrats.view.controller;

import es.devel.opentrats.constants.OpenTratsConstants;
import es.devel.opentrats.service.IReportService;
import es.devel.opentrats.service.exception.ReportServiceException;
import es.devel.opentrats.view.controller.common.OpenTratsAbstractController;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Fran Serrano
 */
public class CustomerReportAbstractController extends OpenTratsAbstractController {

    private IReportService reportService;

    public CustomerReportAbstractController() {
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String customerId = (String) request.getParameter("idCustomer");
        try {
            byte[] reportByteArray = getReportService().CustomerReport(new Integer(customerId));
            transferFile(response, reportByteArray);
        } catch (ReportServiceException ex) {
            getLogService().error(ex);
        }
        return null;
    }

    private void transferFile(HttpServletResponse response, byte[] transferData) throws ServletException {
        ServletOutputStream stream = null;
        BufferedInputStream buf = null;
        try {
            stream = response.getOutputStream();
            response.setContentType(OpenTratsConstants.MIME_TYPE_PDF);
            response.addHeader("Content-Disposition", "inline; filename=HistorialCliente.pdf");
            response.setContentLength((int) transferData.length);
            ByteArrayInputStream bis = new ByteArrayInputStream(transferData);
            buf = new BufferedInputStream(bis);
            int readBytes = 0;
            while ((readBytes = buf.read()) != -1) {
                stream.write(readBytes);
            }
            stream.close();
            buf.close();
        } catch (IOException ioe) {
            throw new ServletException(ioe.getMessage());
        }
    }

    public IReportService getReportService() {
        return reportService;
    }

    public void setReportService(IReportService reportService) {
        this.reportService = reportService;
    }
}
