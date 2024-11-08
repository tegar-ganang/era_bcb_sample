package com.tegsoft.ivr;

import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.log4j.Level;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiRequest;
import com.tegsoft.pbx.TegsoftPBX;
import com.tegsoft.pbx.agi.IntelligentCallRouting;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.NullStatus;

public class BaseTegsoftIVR implements TegsoftIVR {

    private Dataset TBLPBXIVR;

    private Dataset TBLPBXIVROPT;

    private DataRow rowTBLPBXIVR;

    private String IVRID;

    private IntelligentCallRouting activeICR;

    private AgiRequest request;

    private AgiChannel channel;

    private int errorCount;

    private int timeoutCount;

    public static BaseTegsoftIVR initialize(String IVRID) throws Exception {
        BaseTegsoftIVR baseTegsoftIVR = new BaseTegsoftIVR();
        baseTegsoftIVR.loadIVR(IVRID);
        if (baseTegsoftIVR.getRowTBLPBXIVR() == null) {
            return null;
        }
        if ("COMPLEX".equals(baseTegsoftIVR.getRowTBLPBXIVR().getString("IVRTYPE"))) {
            BaseTegsoftIVR complexIVR = (BaseTegsoftIVR) Class.forName(baseTegsoftIVR.getRowTBLPBXIVR().getString("CLASSNAME")).newInstance();
            complexIVR.loadIVR(IVRID);
            return complexIVR;
        }
        return baseTegsoftIVR;
    }

    public void loadIVR(String IVRID) throws Exception {
        this.IVRID = IVRID;
        TBLPBXIVR = new Dataset("TBLPBXIVR", "TBLPBXIVR");
        Command command = new Command("SELECT * FROM TBLPBXIVR WHERE UNITUID={UNITUID} AND IVRID=");
        command.bind(IVRID);
        TBLPBXIVR.fill(command);
        if (TBLPBXIVR.getRowCount() >= 0) {
            rowTBLPBXIVR = TBLPBXIVR.getRow(0);
            TBLPBXIVROPT = new Dataset("TBLPBXIVROPT", "TBLPBXIVROPT");
            command = new Command("SELECT * FROM TBLPBXIVROPT WHERE UNITUID={UNITUID} AND IVRID=");
            command.bind(IVRID);
            TBLPBXIVROPT.fill(command);
        }
    }

    public void execute(IntelligentCallRouting activeICR, AgiRequest request, AgiChannel channel) throws Exception {
        try {
            setActiveICR(activeICR);
            setRequest(request);
            setChannel(channel);
            setErrorCount(0);
            setTimeoutCount(0);
            if (rowTBLPBXIVR == null) {
                TegsoftPBX.logMessage(getChannel(), Level.ERROR, "Invalid IVRID " + IVRID + " please check configuration. Exiting now.");
                return;
            }
            TegsoftPBX.setVariable(getChannel(), "CONTEXTID", rowTBLPBXIVR.getString("CONTEXTID"));
            if ("COMPLEX".equals(rowTBLPBXIVR.getString("IVRTYPE"))) {
                TegsoftPBX.logMessage(getChannel(), Level.INFO, "Complex IVR (" + IVRID + ") loaded. Executing " + rowTBLPBXIVR.getString("CLASSNAME") + " now.");
                TegsoftIVR complexIVR = (TegsoftIVR) Class.forName(rowTBLPBXIVR.getString("CLASSNAME")).getConstructor(String.class).newInstance(IVRID);
                complexIVR.execute(getActiveICR(), getRequest(), getChannel());
                return;
            }
            TegsoftPBX.logMessage(getChannel(), Level.INFO, "IVR (" + IVRID + ") loaded. Executing answer now.");
            getChannel().answer();
            String LASTERROR = "TO";
            String digits = "";
            boolean playAnnounce = true;
            int timeout = 0;
            if (rowTBLPBXIVR.getDecimal("TIMEOUT") != null) {
                timeout = rowTBLPBXIVR.getDecimal("TIMEOUT").intValue();
            }
            int inputCount = 0;
            if (rowTBLPBXIVR.getDecimal("ERRORCOUNT") != null) {
                inputCount = rowTBLPBXIVR.getDecimal("ERRORCOUNT").intValue();
            }
            while (getErrorCount() <= inputCount) {
                if (playAnnounce && NullStatus.isNotNull(rowTBLPBXIVR.getString("ANNOUNCEID"))) {
                    digits = TegsoftPBX.readInput(getChannel(), rowTBLPBXIVR.getString("ANNOUNCEID"), true, timeout, 2, -1);
                    playAnnounce = false;
                } else {
                    digits = TegsoftPBX.readInput(getChannel(), null, true, timeout, 2, -1);
                    playAnnounce = false;
                }
                if (NullStatus.isNull(digits)) {
                    TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "IVR Timeout");
                    LASTERROR = "TO";
                    if (NullStatus.isNotNull(rowTBLPBXIVR.getString("TOMSGID"))) {
                        if ((getErrorCount() < inputCount)) {
                            TegsoftPBX.playBackground(getChannel(), rowTBLPBXIVR.getString("TOMSGID"));
                        }
                    }
                    if (Compare.isTrue(rowTBLPBXIVR.getString("LOOPTO"))) {
                        playAnnounce = true;
                    }
                    incTimeoutCount();
                    incErrorCount();
                } else {
                    TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "Checking IVR options for " + digits);
                    for (int i = 0; i < TBLPBXIVROPT.getRowCount(); i++) {
                        DataRow rowTBLPBXIVROPT = TBLPBXIVROPT.getRow(i);
                        TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "Comparing DTMF " + digits + " with IVR Option " + i + " as " + rowTBLPBXIVROPT.getString("DTMF"));
                        if (Compare.checkPattern(rowTBLPBXIVROPT.getString("DTMF"), digits)) {
                            TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "Option matched executing " + rowTBLPBXIVROPT.getString("DESTTYPE") + " " + rowTBLPBXIVROPT.getString("DESTPARAM"));
                            getActiveICR().dialDESTPARAM(digits, getRequest(), getChannel(), rowTBLPBXIVROPT.getString("DESTTYPE"), rowTBLPBXIVROPT.getString("DESTPARAM"));
                            return;
                        }
                    }
                    if ("EXTENTIONS".equals(rowTBLPBXIVR.getString("DIALOPTION"))) {
                        TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "EXTENTIONS allowed checking extentions for " + digits);
                        if (getActiveICR().checkExtention(digits, getRequest(), getChannel())) {
                            return;
                        }
                    }
                    if ("ALLIN".equals(rowTBLPBXIVR.getString("DIALOPTION"))) {
                        TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "ALL Inbound options allowed checking all inbound options for " + digits);
                        if (getActiveICR().checkIN(digits, getRequest(), getChannel())) {
                            return;
                        }
                    }
                    TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "IVR Invalid entry " + digits);
                    LASTERROR = "INV";
                    if (NullStatus.isNotNull(rowTBLPBXIVR.getString("INVALIDMSGID"))) {
                        if ((getErrorCount() < inputCount)) {
                            TegsoftPBX.playBackground(getChannel(), rowTBLPBXIVR.getString("INVALIDMSGID"));
                        }
                    }
                    if (Compare.isTrue(rowTBLPBXIVR.getString("LOOPINV"))) {
                        playAnnounce = true;
                    }
                    incErrorCount();
                }
                if (Compare.equal(digits, "-1")) {
                    TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "IVR Invalid entry " + digits);
                    LASTERROR = "INV";
                    if (NullStatus.isNotNull(rowTBLPBXIVR.getString("INVALIDMSGID"))) {
                        if ((getErrorCount() < inputCount)) {
                            TegsoftPBX.playBackground(getChannel(), rowTBLPBXIVR.getString("INVALIDMSGID"));
                        }
                    }
                    if (Compare.isTrue(rowTBLPBXIVR.getString("LOOPINV"))) {
                        playAnnounce = true;
                    }
                    incErrorCount();
                }
            }
            if ("TO".equals(LASTERROR)) {
                TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "Executing time-out destination");
                getActiveICR().dialDESTPARAM(digits, getRequest(), getChannel(), rowTBLPBXIVR.getString("TODESTTYPE"), rowTBLPBXIVR.getString("TODESTPARAM"));
                return;
            } else {
                TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "Executing Invalid destination");
                getActiveICR().dialDESTPARAM(digits, getRequest(), getChannel(), rowTBLPBXIVR.getString("INVDESTTYPE"), rowTBLPBXIVR.getString("INVDESTPARAM"));
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String menu(String announce, String emptyAnnounce, String errorAnnounce, boolean replayAnnounce, int inputCount, String validInputs[]) throws Exception {
        String digits = "";
        String errorType = "";
        setTimeoutCount(0);
        setErrorCount(0);
        int max = 0;
        for (int i = 0; i < validInputs.length; i++) {
            if (validInputs[i].length() > max) {
                max = validInputs[i].length();
            }
        }
        while ((getErrorCount() < inputCount) && (NullStatus.isNull(digits))) {
            if ((getErrorCount() == 0) || replayAnnounce) {
                digits = TegsoftPBX.readInput(getChannel(), announce, true, 4, 2, max);
            } else {
                digits = TegsoftPBX.readInput(getChannel(), null, true, 4, 2, max);
            }
            if (NullStatus.isNull(digits)) {
                incTimeoutCount();
                incErrorCount();
                errorType = "TO";
                if ((getErrorCount() < inputCount)) {
                    TegsoftPBX.playBackground(getChannel(), emptyAnnounce);
                }
            } else if (digits.length() > max) {
                digits = "";
                incErrorCount();
                errorType = "INV";
                if ((getErrorCount() < inputCount)) {
                    TegsoftPBX.playBackground(getChannel(), errorAnnounce);
                }
            } else {
                for (int i = 0; i < validInputs.length; i++) {
                    if (Compare.equal(validInputs[i], digits)) {
                        return validInputs[i];
                    }
                }
                digits = "";
                incErrorCount();
                errorType = "INV";
                if ((getErrorCount() < inputCount)) {
                    TegsoftPBX.playBackground(getChannel(), errorAnnounce);
                }
            }
        }
        return errorType;
    }

    public String readInput(String announce) throws Exception {
        return readInput(announce, null, null, true, true, 2, 4, 2, -1, 200);
    }

    public String readInput(String announce, String emptyAnnounce, String errorAnnounce) throws Exception {
        return readInput(announce, emptyAnnounce, errorAnnounce, true, true, 2, 4, 2, -1, 200);
    }

    public String readInput(String announce, String emptyAnnounce, String errorAnnounce, int maxdigits) throws Exception {
        return readInput(announce, emptyAnnounce, errorAnnounce, true, true, 1, 4, 2, -1, maxdigits);
    }

    public String readInput(String announce, String emptyAnnounce, String errorAnnounce, int mindigits, int maxdigits) throws Exception {
        return readInput(announce, emptyAnnounce, errorAnnounce, true, true, 1, 4, 2, mindigits, maxdigits);
    }

    public String readInput(String announce, String emptyAnnounce, String errorAnnounce, int initial_timeout, int interdigit_timeout, int mindigits, int maxdigits) throws Exception {
        return readInput(announce, emptyAnnounce, errorAnnounce, true, true, 1, initial_timeout, interdigit_timeout, mindigits, maxdigits);
    }

    public String readInput(String announce, String emptyAnnounce, String errorAnnounce, int inputCount, int initial_timeout, int interdigit_timeout, int mindigits, int maxdigits) throws Exception {
        return readInput(announce, emptyAnnounce, errorAnnounce, true, true, inputCount, initial_timeout, interdigit_timeout, mindigits, maxdigits);
    }

    public String readInput(String announce, String emptyAnnounce, String errorAnnounce, boolean replayAnnounce, boolean interruptible, int inputCount, int initial_timeout, int interdigit_timeout, int mindigits, int maxdigits) throws Exception {
        String digits = "";
        String errorType = "";
        setTimeoutCount(0);
        setErrorCount(0);
        if (inputCount == 0) {
            throw new Exception("ERROR!!");
        }
        while ((getErrorCount() < inputCount) && (NullStatus.isNull(digits))) {
            if ((getErrorCount() == 0) || replayAnnounce) {
                digits = TegsoftPBX.readInput(getChannel(), announce, interruptible, initial_timeout, initial_timeout, maxdigits);
            } else {
                digits = TegsoftPBX.readInput(getChannel(), announce, interruptible, initial_timeout, initial_timeout, maxdigits);
            }
            if (NullStatus.isNull(digits)) {
                incTimeoutCount();
                incErrorCount();
                errorType = "TO";
                TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "GOT timeout");
                if ((getErrorCount() < inputCount)) {
                    TegsoftPBX.playBackground(getChannel(), emptyAnnounce);
                }
            } else if (digits.length() > maxdigits) {
                TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "GOT invalid entry more than MAX " + digits);
                digits = "";
                incErrorCount();
                errorType = "INV";
                if ((getErrorCount() < inputCount)) {
                    TegsoftPBX.playBackground(getChannel(), errorAnnounce);
                }
            } else if (digits.length() < mindigits) {
                TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "GOT invalid entry less than MIN " + digits);
                digits = "";
                incErrorCount();
                errorType = "INV";
                if ((getErrorCount() < inputCount)) {
                    TegsoftPBX.playBackground(getChannel(), errorAnnounce);
                }
            } else {
                TegsoftPBX.logMessage(getChannel(), Level.DEBUG, "Returning " + digits);
                return digits;
            }
        }
        return errorType;
    }

    public static boolean sendFax(String extension, String fileName) {
        boolean returnValue = true;
        try {
            Runtime.getRuntime().exec("/usr/bin/sendfax -n -t1 -d " + extension + " " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            returnValue = false;
        }
        return returnValue;
    }

    public static boolean sendEMail(String mailSender, String to[], String mailSubject, String mailBody, String attachmentFiles[]) throws Exception {
        try {
            Properties properties = new Properties();
            properties.put("mail.transport.protocol", "smtp");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", "localhost");
            properties.put("localhost", "true");
            Session session = Session.getDefaultInstance(properties);
            InternetAddress mailFrom = new InternetAddress(mailSender);
            InternetAddress[] mailTo = new InternetAddress[to.length];
            for (int i = 0; i < to.length; i++) {
                mailTo[i] = new InternetAddress(to[i]);
            }
            Message message = new MimeMessage(session);
            message.setFrom(mailFrom);
            message.setRecipients(Message.RecipientType.TO, mailTo);
            message.setSubject(mailSubject);
            Multipart multipart = new MimeMultipart();
            MimeBodyPart bodyPartMessage = new MimeBodyPart();
            bodyPartMessage.setContent(mailBody, "text/html; charset=utf-8");
            multipart.addBodyPart(bodyPartMessage);
            MimeBodyPart bodyPartAttachment = new MimeBodyPart();
            ;
            for (int i = 0; i < attachmentFiles.length; i++) {
                FileDataSource fileDataSource = new FileDataSource(attachmentFiles[i]);
                bodyPartAttachment.setDataHandler(new DataHandler(fileDataSource));
                bodyPartAttachment.setFileName(fileDataSource.getName());
                multipart.addBodyPart(bodyPartAttachment);
            }
            message.setContent(multipart);
            Transport.send(message);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public void setLogLevel(Level level) throws Exception {
        TegsoftPBX.setLogLevel(getChannel(), level);
    }

    public void playBackground(String announce) throws Exception {
        TegsoftPBX.playBackground(getChannel(), announce);
    }

    public void playBack(String announce) throws Exception {
        TegsoftPBX.playBack(getChannel(), announce);
    }

    public void sayNumber(String number) throws Exception {
        TegsoftPBX.sayNumber(getChannel(), number);
    }

    public void sayMoney(String number, String dolars, String cents, String dolarsOnly) throws Exception {
        TegsoftPBX.sayMoney(getChannel(), number, dolars, cents, dolarsOnly);
    }

    public void sayDigits(String digits) throws Exception {
        TegsoftPBX.sayDigits(getChannel(), digits);
    }

    public void sayDate(String day, String month, String year) throws Exception {
        TegsoftPBX.sayDate(getChannel(), day, month, year);
    }

    public String getVariable(String varName) throws Exception {
        return TegsoftPBX.getVariable(getChannel(), varName);
    }

    public void setVariable(String name, String value) throws Exception {
        TegsoftPBX.setVariable(getChannel(), name, value);
    }

    public void logMessage(Level level, String message) throws Exception {
        TegsoftPBX.logMessage(getChannel(), level, message);
    }

    public boolean transfer(String extension) throws Exception {
        return TegsoftPBX.transfer(extension, getActiveICR(), getRequest(), getChannel());
    }

    @Override
    public boolean isConferenceIVR() throws Exception {
        return false;
    }

    public AgiChannel getChannel() {
        return channel;
    }

    public IntelligentCallRouting getActiveICR() {
        return activeICR;
    }

    public AgiRequest getRequest() {
        return request;
    }

    public String getIVRID() {
        return IVRID;
    }

    public void setIVRID(String iVRID) {
        IVRID = iVRID;
    }

    public Dataset getTBLPBXIVR() {
        return TBLPBXIVR;
    }

    public Dataset getTBLPBXIVROPT() {
        return TBLPBXIVROPT;
    }

    public DataRow getRowTBLPBXIVR() {
        return rowTBLPBXIVR;
    }

    public void setActiveICR(IntelligentCallRouting activeICR) {
        this.activeICR = activeICR;
    }

    public void setRequest(AgiRequest request) {
        this.request = request;
    }

    public void setChannel(AgiChannel channel) {
        this.channel = channel;
    }

    public void incErrorCount() {
        errorCount++;
    }

    public void incTimeoutCount() {
        timeoutCount++;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public int getTimeoutCount() {
        return timeoutCount;
    }

    public void setTimeoutCount(int timeoutCount) {
        this.timeoutCount = timeoutCount;
    }
}
