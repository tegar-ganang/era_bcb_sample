package net.sourceforge.bulkmailer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.*;
import java.util.*;
import javax.swing.*;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

/**
 * Manages the interaction between EmailSender threads
 * and the SendingDialog.  It also starts the process
 * and cleans up the process once all the threads are
 * done.
 * <p>
 * Since multiple threads are hitting this object,
 * DestinatairesManager must be thread-safe.  The
 * goal is to put all the thread-handling code in this
 * object, not in EmailSender and SendingDialog 
 * (or even Campaign).
 * 
 * @author: Jean-Pierre Norguet
 */
public class DestinatairesManager implements Runnable {

    Logger logger;

    SendingDialog sendingDialog;

    private Iterator cardIterator;

    private ActivityLogWriter activityLogWriter;

    private ProcessedLogWriter processedLogWriter;

    private FailedLogWriter failedLogWriter;

    EmailSender[] sendingMailers;

    Thread[] sendingThreads;

    BulkMailResults bulkMailResults;

    Campaign campaign;

    public DestinatairesManager() {
        super();
    }

    public BulkMailResults sendBulkMail(JFrame parentFrame, Campaign campaign, Logger logger, SendingDialog sendDialog) throws FileNotFoundException, IOException, InvalidCsvException, Exception {
        this.cardIterator = campaign.getRecipientCardCollection().iterator();
        this.activityLogWriter = campaign.createActivityLogWriter();
        this.processedLogWriter = campaign.createProcessedLogWriter();
        this.failedLogWriter = campaign.createFailedLogWriter();
        try {
            this.sendingDialog = sendDialog;
            this.logger = logger;
            this.bulkMailResults = new BulkMailResults();
            this.campaign = campaign;
            sendingThreads = new Thread[campaign.getThreadCount()];
            sendingMailers = new EmailSender[campaign.getThreadCount()];
            for (int i = 0; i < sendingThreads.length; i++) {
                EmailSender sender = new EmailSender(this, i + 1);
                sendingMailers[i] = sender;
                Thread thread = new Thread(sender);
                sendingThreads[i] = thread;
                thread.setName("Thread-" + (i + 1));
                thread.start();
            }
            Thread managerThread = new Thread(this);
            managerThread.start();
            sendingDialog.setVisible(true);
            try {
                managerThread.join();
            } catch (InterruptedException e) {
            }
        } finally {
            this.activityLogWriter.close();
            this.processedLogWriter.close();
            this.failedLogWriter.close();
        }
        return this.bulkMailResults;
    }

    public void run() {
        for (int i = 0; i < campaign.getThreadCount(); i++) {
            try {
                sendingThreads[i].join();
            } catch (InterruptedException e) {
            }
        }
        sendingDialog.setVisible(false);
    }

    public synchronized void informSentEmail(Card card, int threadNum) {
        this.sendingDialog.incrementProgress();
        this.bulkMailResults.numSent++;
        try {
            Date now = new Date();
            this.activityLogWriter.write(now, card, "Sent", "", threadNum);
            this.processedLogWriter.write(now, card);
        } catch (Exception e) {
            handleLoggingError(e);
        }
    }

    public synchronized void informFailedEmail(Card card, int threadNum, Exception e) {
        this.sendingDialog.incrementProgress();
        this.bulkMailResults.numFailed++;
        try {
            Date now = new Date();
            this.activityLogWriter.write(now, card, "Failed", e.getMessage(), threadNum);
            this.failedLogWriter.write(now, card);
            this.processedLogWriter.write(now, card);
        } catch (Exception ex) {
            handleLoggingError(ex);
        }
    }

    public synchronized void informStartingEmail(Card card, int threadNum) {
        try {
            this.activityLogWriter.write(new Date(), card, "Starting", "", threadNum);
        } catch (Exception e) {
            handleLoggingError(e);
        }
    }

    public synchronized Card nextCard() throws InvalidCsvException, IOException {
        if (cardIterator.hasNext()) {
            return (Card) cardIterator.next();
        } else {
            return null;
        }
    }

    public synchronized void log(Level level, String message) {
        this.logger.log(level, message);
    }

    public synchronized void log(Level level, String message, Exception e) {
        this.logger.log(level, message, e);
    }

    public synchronized HtmlEmail buildMail() throws EmailException, FileNotFoundException, IOException, CampaignValidationException {
        HtmlEmail email = campaign.buildMail();
        if (campaign.getSMTPServerAuthenticationType().equals(Campaign.SMTP_AUTH_PASSWORD)) {
            email.setAuthentication(campaign.getSMTPUsername(), campaign.getSMTPPassword());
        }
        return email;
    }

    public synchronized long getThrottleMillisPerThread() {
        if (campaign.isThrottled()) {
            return (int) ((float) campaign.getThreadCount() * (float) campaign.getThrottledEmailCount() / (float) campaign.getThrottledMinutes() * 60 * 1000);
        } else {
            return 0;
        }
    }

    public synchronized void emergencyStop() {
        for (int i = 0; i < sendingMailers.length; i++) {
            sendingMailers[i].abortNicely();
        }
    }

    private void handleLoggingError(Exception e) {
        ErrorDialog.showDialog(sendingDialog, e);
    }
}
