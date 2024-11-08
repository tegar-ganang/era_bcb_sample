package lu.pragmaconsult.appstorebot.itunes;

import static lu.pragmaconsult.appstorebot.itunes.Constants.DATE_LIST_AVAILABLE_DAYS;
import static lu.pragmaconsult.appstorebot.itunes.Constants.DATE_LIST_AVAILABLE_WEEKS;
import static lu.pragmaconsult.appstorebot.itunes.Constants.HEADER_CONTENT_DISPOSITION;
import static lu.pragmaconsult.appstorebot.itunes.Constants.SALES_URL;
import static lu.pragmaconsult.appstorebot.itunes.Constants.VIEW_STATE;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import lu.pragmaconsult.appstorebot.BotContext;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Type comment
 * @author Wilfried Vandenberghe (wilfried.vandenberghe at pragmaconsult.lu)
 */
class DownloadReportBotState extends AbstractBotState {

    private static final Logger logger = LoggerFactory.getLogger(DownloadReportBotState.class);

    private int reportCount;

    private int index;

    @Override
    protected HttpMethod createHttpMethod(BotContext context, HttpClient httpClient) {
        index = Integer.parseInt(context.getContextAttribute("reportDatesIndex"));
        String reportDates = context.getContextAttribute("reportDates");
        String dateStr = reportDates.split(",")[index];
        reportCount = reportDates.split(",").length;
        PostMethod method = new PostMethod(SALES_URL);
        NameValuePair theForm = new NameValuePair("theForm", "theForm");
        NameValuePair theFormNotNormal = new NameValuePair("theForm:xyz", "notnormal");
        NameValuePair theFormVendorType = new NameValuePair("theForm:vendorType", "Y");
        NameValuePair dateDay, dateWeek = null;
        if (context.isWeekly()) {
            dateDay = new NameValuePair("theForm:datePickerSourceSelectElementSales", context.getContextAttribute(DATE_LIST_AVAILABLE_DAYS).split(",")[0]);
            dateWeek = new NameValuePair("theForm:weekPickerSourceSelectElement", dateStr);
        } else {
            dateDay = new NameValuePair("theForm:datePickerSourceSelectElementSales", dateStr);
            dateWeek = new NameValuePair("theForm:weekPickerSourceSelectElement", context.getContextAttribute(DATE_LIST_AVAILABLE_WEEKS).split(",")[0]);
        }
        NameValuePair viewState = new NameValuePair("javax.faces.ViewState", context.getContextAttribute(VIEW_STATE));
        NameValuePair downloadLabel = new NameValuePair("theForm:downloadLabel2", "theForm:downloadLabel2");
        method.setRequestBody(new NameValuePair[] { theForm, theFormNotNormal, theFormVendorType, dateDay, dateWeek, viewState, downloadLabel });
        return method;
    }

    @Override
    protected BotState getNextState() {
        if (index == reportCount) {
            return null;
        } else {
            return new SelectDateBotState();
        }
    }

    @Override
    protected void processResponseBody(BotContext context, HttpMethod method) {
        Header filename = method.getResponseHeader(HEADER_CONTENT_DISPOSITION);
        String filenameStr = filename.getValue().split("=")[1];
        if (context.isVerbose()) {
            logger.info("File downloaded: " + filenameStr);
        }
        if (context.isUnzip()) {
            filenameStr = filenameStr.substring(0, filenameStr.length() - 3);
        }
        File f = null;
        if (context.getOutputDiretory() != null) {
            f = new File(context.getOutputDiretory(), filenameStr);
        } else {
            f = new File(filenameStr);
        }
        OutputStream out = null;
        try {
            if (!f.createNewFile()) {
                throw new Exception("Cannot create file: " + f.getPath());
            }
            out = new BufferedOutputStream(new FileOutputStream(f));
            InputStream in = null;
            if (context.isUnzip()) {
                in = new GZIPInputStream(method.getResponseBodyAsStream());
            } else {
                in = method.getResponseBodyAsStream();
            }
            byte[] buffer = new byte[1024];
            int readSize;
            while ((readSize = in.read(buffer)) != -1) {
                out.write(buffer, 0, readSize);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            index++;
            context.setContextAttribute("reportDatesIndex", Integer.toString(index));
        }
    }
}
