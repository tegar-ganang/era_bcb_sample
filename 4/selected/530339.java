package net.lukemurphey.nsia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.Authenticator;

/**
 * This class contains a series of general methods that are intended to be be used at various locations within an application. 
 * @author luke
 *
 */
public class GenericUtils {

    /**
	* Reallocates an array with a new size, and copies the contents
	* of the old array to the new array.
	* @param oldArray  the old array, to be reallocated.
	* @param newSize   the new array size.
	* @return          A new array with the same contents.
	*/
    public static Object resizeArray(Object oldArray, int newSize) {
        if (newSize < 0) {
            return oldArray;
        }
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        if (oldSize == newSize) return oldArray;
        Class<?> elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
        int preserveLength = Math.min(oldSize, newSize);
        if (preserveLength > 0) System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
        return newArray;
    }

    /**
	 * Get a string version of time (like "10 mins").
	 * @param secs
	 * @return
	 */
    public static String getTimeDescription(long secs) {
        double doubleSecs = secs;
        DecimalFormat twoPlaces = new DecimalFormat("0.00");
        if (secs < 60) return secs + " sec"; else if (secs < 3600) return twoPlaces.format(doubleSecs / 60) + " min"; else if (secs < 86400) return twoPlaces.format(doubleSecs / 3600) + " hours"; else return twoPlaces.format(doubleSecs / 86400) + " days";
    }

    /**
	 * Shorten a string to the desired length and append "..." in the most appropriate location.
	 * @param input
	 * @param desiredLength
	 * @return
	 */
    public static String shortenString(String input, int desiredLength) {
        if (input == null || input.length() == 0) return "";
        if (desiredLength < 5) return "";
        if (input.length() <= desiredLength) return input;
        int finalDesiredLength = desiredLength - 4;
        int lastSpace = -1;
        input = input.trim();
        int nextLastSpace = input.indexOf(" ");
        for (; nextLastSpace > 0 && nextLastSpace < desiredLength; ) {
            lastSpace = nextLastSpace;
            nextLastSpace = input.indexOf(" ", nextLastSpace + 1);
        }
        int cutRatio = (100 * finalDesiredLength / lastSpace);
        if (lastSpace < 0) return input.substring(0, desiredLength) + "..."; else if ((lastSpace < 0 && finalDesiredLength < input.length()) || cutRatio < 80) return (input.substring(0, finalDesiredLength) + " ..."); else return (input.substring(0, lastSpace) + " ...");
    }

    /**
	 * This method takes the given directory and outputs the contents recursively to the given output stream 
	 * @param zipDir
	 * @param zos
	 * @throws IOException
	 */
    public static void zipDir(File zipDir, ZipOutputStream zos) throws IOException {
        String[] dirList;
        if (zipDir.isDirectory()) {
            dirList = zipDir.list();
        } else {
            dirList = new String[] { zipDir.getAbsolutePath() };
        }
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                zipDir(f, zos);
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            ZipEntry anEntry = new ZipEntry(f.getPath());
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
        }
    }

    /**
	 * Deletes the given directory and all files contained within
	 * @param path
	 * @return
	 */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
	 * Get the percentage progress based on how long the event is intended to take.
	 * @param expectedMaxTime
	 * @param startTime
	 * @return
	 */
    public static int twoStateProgress(int expectedMaxTime, int startTime) {
        int currentTime = (int) System.currentTimeMillis();
        int third = (expectedMaxTime / 3);
        int diff = currentTime - startTime;
        if (diff <= third) {
            return (100 * diff / third) / 2;
        } else if (diff > expectedMaxTime) {
            return 100;
        } else {
            return 50 + ((100 * (diff - third)) / (expectedMaxTime - third)) / 2;
        }
    }

    /**
	 * Send an email. Sends an email and returns true if the email was sent. If the application is not properly configured to send emails then the method will return false.
	 * @param toAddress
	 * @param subject
	 * @param body
	 * @throws MessagingException
	 * @throws MailServerConnectionFailedException 
	 */
    public static boolean sendMail(EmailAddress toAddress, String subject, String body) throws MessagingException {
        Application app = Application.getApplication();
        ApplicationConfiguration config = app.getApplicationConfiguration();
        try {
            return sendMail(toAddress, subject, body, config.getEmailFromAddress(), config.getEmailSMTPServer(), config.getEmailUsername(), config.getEmailPassword(), config.getEmailSMTPPort(), config.getEmailSMTPEncryption());
        } catch (UnknownHostException e) {
            throw new MessagingException("Host is unknown", e);
        } catch (NoDatabaseConnectionException e) {
            throw new MessagingException("No database connection", e);
        } catch (SQLException e) {
            throw new MessagingException("SQL exception", e);
        } catch (InputValidationException e) {
            throw new MessagingException("Input validation exception", e);
        } catch (InvalidLocalPartException e) {
            throw new MessagingException("Local part of email is invalid", e);
        }
    }

    /**
	 * Represents the type encryption used for the SMTP connection.
	 * @author Luke
	 *
	 */
    public enum SMTPEncryption {

        NONE, SSL, STARTTLS, TLS
    }

    /**
	 * The authenticator used to perform SMTP authentication.
	 * @author Luke
	 *
	 */
    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        private String username;

        private String password;

        public SMTPAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(this.username, this.password);
        }
    }

    /**
	 * Sends an email and returns true if the email was sent. If the application is not properly configured to send emails then the method will return false.
	 * @param toAddress
	 * @param subject
	 * @param body
	 * @param fromAddress
	 * @param smtpServer
	 * @param username
	 * @param password
	 * @param port
	 * @param encryption
	 * @throws MessagingException 
	 */
    public static boolean sendMail(EmailAddress toAddress, String subject, String body, EmailAddress fromAddress, String smtpServer, String username, String password, int port, SMTPEncryption encryption) throws MessagingException {
        if (smtpServer == null || fromAddress == null) {
            return false;
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.from", fromAddress.toString());
        props.put("mail.smtp.port", Integer.toString(port));
        if (username != null) {
            props.put("mail.smtp.auth", "true");
        }
        if (encryption == SMTPEncryption.STARTTLS) {
            props.put("mail.smtp.starttls.enable", "true");
        } else if (encryption == SMTPEncryption.SSL) {
            props.put("mail.smtp.ssl", "true");
        }
        Session session = null;
        if (encryption == SMTPEncryption.SSL || encryption == SMTPEncryption.STARTTLS || encryption == SMTPEncryption.TLS) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.socketFactory.port", Integer.toString(port));
            props.setProperty("mail.smtp.quitwait", "false");
            System.getSecurityManager();
            Authenticator auth = new SMTPAuthenticator(username, password);
            session = Session.getInstance(props, auth);
        } else {
            session = Session.getInstance(props, null);
        }
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom();
        msg.setRecipients(Message.RecipientType.TO, toAddress.toString());
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(body);
        Transport transport = session.getTransport("smtp");
        if (username != null) {
            transport.connect(smtpServer, username, password);
        }
        if (transport.isConnected() == false) {
            throw new MessagingException("Connection to the mail server failed");
        }
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();
        return true;
    }

    /**
	 * Add the number of days from the given date.
	 * @param aDate
	 * @param noOfDays
	 * @return
	 */
    public static Date addOrSubstractDaysFromDate(Date aDate, int noOfDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(aDate);
        calendar.add(Calendar.DATE, noOfDays);
        return calendar.getTime();
    }
}
