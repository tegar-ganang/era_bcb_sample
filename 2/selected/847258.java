package consumercredit;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.jws.WebService;

/**
 * This class was generated by Apache CXF 2.1.4 Mon May 11 03:15:21 CEST 2009
 * Generated source version: 2.1.4
 * 
 */
@WebService(endpointInterface = "consumercredit.WSsms", serviceName = "WSsmsService")
public class WSsmsImpl implements WSsms {

    private static final String urlRequest = "http://sms1.redoxygen.net/sms.dll?Action=SendSMS";

    private static final String smsAccountId = "CI00022115";

    private static final String smsEmail = "georgiana_anton@yahoo.com";

    private static final String smsPassword = "18changeme";

    public static void main(String[] args) throws Exception {
        WSsms sender = new WSsmsImpl();
        Integer nResult = sender.SendSMS("00491781537461", "Consumer credit was approved");
        System.out.println("Result Code = " + nResult + "\n");
    }

    public int SendSMS(java.lang.String smsNumber, java.lang.String smsMessage) {
        System.out.println(smsNumber);
        System.out.println(smsMessage);
        try {
            String sData;
            StringBuffer strResponse = new StringBuffer();
            int nResult = -1;
            try {
                sData = ("AccountId=" + URLEncoder.encode(smsAccountId, "UTF-8"));
                sData += ("&Email=" + URLEncoder.encode(smsEmail, "UTF-8"));
                sData += ("&Password=" + URLEncoder.encode(smsPassword, "UTF-8"));
                sData += ("&Recipient=" + URLEncoder.encode(smsNumber, "UTF-8"));
                sData += ("&Message=" + URLEncoder.encode(smsMessage, "UTF-8"));
                URL urlObject = new URL(urlRequest);
                HttpURLConnection con = (HttpURLConnection) urlObject.openConnection();
                con.setRequestMethod("POST");
                con.setDoInput(true);
                con.setDoOutput(true);
                DataOutputStream out;
                out = new DataOutputStream(con.getOutputStream());
                out.writeBytes(sData);
                out.flush();
                out.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer responseBuffer = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    responseBuffer = responseBuffer.append(inputLine);
                    responseBuffer = responseBuffer.append("\n\n\n");
                }
                strResponse.replace(0, 0, responseBuffer.toString());
                String sResultCode = strResponse.substring(0, 4);
                nResult = new Integer(sResultCode);
                in.close();
                System.out.println("Response Text = " + strResponse + "\n");
            } catch (Exception e) {
                System.out.println("Exception caught sending SMS\n");
                nResult = -1;
            }
            return nResult;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
