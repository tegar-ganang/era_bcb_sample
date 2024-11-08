package org.one.stone.soup.wiki.mailer;

import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SimpleSMTPHeader;
import org.one.stone.soup.util.Queue;
import org.one.stone.soup.wiki.jar.manager.SystemAPI;
import org.one.stone.soup.wiki.processor.JavascriptEngine;

public class Mailer extends SystemAPI {

    private Queue mailQueue;

    public class MailerThread implements Runnable {

        private String mailFrom;

        private String mailTo;

        private String subject;

        private String message;

        private MailerThread(String mailFrom, String mailTo, String subject, String message) {
            this.mailFrom = mailFrom;
            this.mailTo = mailTo;
            this.subject = subject;
            this.message = message;
            Thread thread = new Thread(this, "Sending Mail: " + subject);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        public void run() {
            try {
                _sendMail(mailFrom, mailTo, subject, message);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    public Mailer() {
        mailQueue = new Queue();
    }

    public void sendMail(String mailFrom, String mailTo, String subject, String message) throws Exception {
        new MailerThread(mailFrom, mailTo, subject, message);
    }

    public void _sendMail(String mailFrom, String mailTo, String subject, String message) throws Exception {
        try {
            String host = getString("/OpenForum/Actions/SendEMail", "host.txt");
            if (host.length() == 0 || host.equals("smtp.server")) {
                return;
            }
            mailTo = getString("/Admin/Users/" + mailTo + "/private", "email.txt");
            mailFrom = getString("/Admin/Users/" + mailFrom + "/private", "email.txt");
            SMTPClient client = (SMTPClient) getClass().getClassLoader().loadClass("org.apache.commons.net.smtp.SMTPClient").newInstance();
            client.connect(host);
            boolean result = client.login();
            client.setSender(mailFrom);
            client.addRecipient(mailTo);
            SimpleSMTPHeader header = new SimpleSMTPHeader(mailFrom, mailTo, subject);
            result = client.sendShortMessageData(header.toString() + message);
            client.logout();
            client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getString(String pageName, String fileName) {
        try {
            return getBuilder().getFileManager().getPageAttachmentAsString(pageName, fileName, getBuilder().getSystemLogin());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
