package lv.webkursi.klucis.eim.demo.miscellaneous.smtp;

import com.jscape.inet.email.EmailMessage;
import com.jscape.inet.smtpssl.SmtpSsl;

/**
 * This class sends an e-mail from Google account. 
 * Since the IP adddress of "gmail.com" does not match
 * your Internet address, it is likely to be marked
 * as spam in the receiver's mailbox.
 * 
 * TODO Try to avoid the commercial library "sinetfactory.jar"; 
 * see http://www.jscape.com/secureinetfactory/download.html
 * (they are sending spam to those, who download the software
 * "Secure iNet Factory").
 * Also - an SmtpException ("Unsupported record 
 * 
 * @author kap
 */
public class SmtpGmailSample {

    public static void main(String[] args) {
        SmtpSsl smtp = null;
        String username = "n.nelda@gmail.com";
        String password = "XXXXXX";
        String to = "kalvis.apsitis@accenture.com";
        try {
            smtp = new SmtpSsl("smtp.gmail.com", 465);
            smtp.connect();
            smtp.login(username, password);
            EmailMessage message = new EmailMessage();
            message.setTo(to);
            message.setFrom(username);
            message.setSubject("Sending email via Gmail SMTP");
            message.setBody("This is the body of the message");
            smtp.send(message);
            smtp.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
