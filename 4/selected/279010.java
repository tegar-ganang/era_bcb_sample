package org.ttalbott.mytelly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.Calendar;
import java.util.Date;
import com.tms.webservices.applications.DataDirectException;
import com.tms.webservices.applications.xtvd.SOAPRequest;
import com.tms.webservices.applications.xtvd.Xtvd;

/**
 * @author Tom Talbott
 *
 */
public class ZapWebServiceUpdateThread extends Thread {

    public static final String WEBSERVICE_URI = "http://webservices.schedulesdirect.tmsdatadirect.com/schedulesdirect/tvlistings/xtvdService";

    UpdateDlg m_dlg;

    Config m_config;

    Date m_startDate;

    ZapWsGrabber m_grabber;

    /** Creates new UpdateThread */
    public ZapWebServiceUpdateThread(ZapWsGrabber grabber, UpdateDlg dlg, Config config, Date startDate) {
        m_dlg = dlg;
        m_config = config;
        m_startDate = startDate;
        m_grabber = grabber;
    }

    public void run() {
        m_dlg.start("Starting Update...");
        String userName = m_grabber.getUserID();
        String password = m_grabber.getPassword();
        if (m_startDate == null) m_startDate = new Date();
        try {
            SOAPRequest soapRequest = new SOAPRequest(userName, password);
            soapRequest.setWebserviceURI(WEBSERVICE_URI);
            try {
                PipedWriter out = new PipedWriter();
                PipedReader in = new PipedReader(out);
                soapRequest.setLog(out);
                new WSLogReaderThread(new BufferedReader(in), m_dlg).start();
            } catch (IOException e1) {
                m_dlg.update("Error: setting up logging", e1.getMessage());
                e1.printStackTrace();
            }
            Calendar start = Calendar.getInstance();
            start.setTime(m_startDate);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            Calendar end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_YEAR, m_grabber.getDaysToDownload());
            m_dlg.update("Getting data..", "Getting data..");
            if (m_dlg.getCancel()) {
                m_dlg.end("Cancelled", "Update Cancelled.", true);
                return;
            }
            Xtvd xtvd = new Xtvd();
            soapRequest.getData(start, end, xtvd);
            if (m_dlg.getCancel()) {
                m_dlg.end("Cancelled", "Update Cancelled.", true);
                return;
            }
            m_dlg.update("Importing XML...", "Importing XML...");
            SQLProgramData pData = (SQLProgramData) Programs.getInstance().getProgramData();
            SQLChannelData channelData = (SQLChannelData) Channels.getInstance().getChannelData();
            try {
                pData.resetPrograms();
                channelData.resetChannels();
                XTVDtoSQL xtvdsql = new XTVDtoSQL(pData, channelData);
                xtvdsql.beginImport(xtvd);
            } catch (Exception e) {
                e.printStackTrace();
                m_dlg.end("Error", "Unable to import XML: " + e.getMessage(), true);
            }
        } catch (DataDirectException e) {
            String msg = "";
            if (e.getCause().toString().indexOf("OutOfMemory") != -1) {
                msg = " - Increase the -Xmx128m parameter in the mytelly.bat file.  Try -Xmx192m or larger.";
            }
            m_dlg.end("Error making server request.", e.getCause() + msg, true);
            e.printStackTrace();
            return;
        }
        System.gc();
        m_dlg.end("Done", "Finished importing XML.", false);
    }
}
