package org.mobicents.servlet.sip.alerting;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class EsendexSmsAlertServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(EsendexSmsAlertServlet.class);

    private static final String RESPONSE_BEGIN = "<HTML><BODY>";

    private static final String RESPONSE_END = "</BODY></HTML>";

    private static final String ESENDEX_URL = "https://www.esendex.com/secure/messenger/formpost/SendSMS.aspx";

    private static final String HTTP_METHOD = "POST";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        logger.info("the SmsAlertServlet has been started");
    }

    /**
	 * Handle the HTTP POST method on which alert can be sent so that the app
	 * sends an sms based on that
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String accountId = getServletContext().getInitParameter("esendexAccountRef");
        String email = getServletContext().getInitParameter("esendexUserName");
        String password = getServletContext().getInitParameter("esendexPassword");
        String alertId = request.getParameter("alertId");
        String tel = request.getParameter("tel");
        String alertText = request.getParameter("alertText");
        if (alertText == null || alertText.length() < 1) {
            byte[] content = new byte[request.getContentLength()];
            request.getInputStream().read(content, 0, request.getContentLength());
            alertText = new String(content);
        }
        alertText = "Alert Id : " + alertId + ",  Alert Details :" + alertText;
        StringBuffer responseBuffer = new StringBuffer();
        try {
            String data = URLEncoder.encode("EsendexUsername", "UTF-8") + "=" + URLEncoder.encode(email, "UTF-8");
            data += "&" + URLEncoder.encode("EsendexPassword", "UTF-8") + "=" + password;
            data += "&" + URLEncoder.encode("EsendexAccount", "UTF-8") + "=" + accountId;
            data += "&" + URLEncoder.encode("EsendexRecipient", "UTF-8") + "=" + tel;
            data += "&" + URLEncoder.encode("EsendexBody", "UTF-8") + "=" + URLEncoder.encode(alertText, "UTF-8");
            if (logger.isInfoEnabled()) {
                logger.info("Got an alert : \n alertID : " + alertId + " \n tel : " + tel + " \n text : " + alertText);
                logger.info("Esendex : \n Account Ref : " + accountId);
                logger.info("Esendex URL : \n URL : " + ESENDEX_URL + " \n data : " + data);
            }
            URL urlObject = new URL(ESENDEX_URL);
            HttpURLConnection con = (HttpURLConnection) urlObject.openConnection();
            con.setRequestMethod(HTTP_METHOD);
            con.setDoInput(true);
            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(data);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseBuffer = responseBuffer.append(inputLine);
            }
        } catch (Exception e) {
            logger.error("Exception caught sending SMS", e);
        }
        sendHttpResponse(response, RESPONSE_BEGIN + "response : " + responseBuffer.toString() + RESPONSE_END);
    }

    /**
	 * @param response
	 * @throws IOException
	 */
    private void sendHttpResponse(HttpServletResponse response, String body) throws IOException {
        PrintWriter out;
        response.setContentType("text/html");
        out = response.getWriter();
        out.println(body);
        out.close();
    }
}
