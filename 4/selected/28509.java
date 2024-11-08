package burster.sender;

import burster.settings.Settings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;

public abstract class Sender {

    protected Log log = LogFactory.getLog(Sender.class);

    protected Settings settings;

    protected String attachFilePath;

    private boolean quarantined = false;

    private boolean sent = false;

    private String quarantineFolder = null;

    public Sender(Settings settings) {
        this.settings = settings;
    }

    public void attachFile(String fileToAttachPath) {
        attachFilePath = fileToAttachPath;
    }

    protected void quarantine() {
        log.trace("quarantine()");
        try {
            File quarantineDir = new File(quarantineFolder);
            if (!quarantineDir.exists()) {
                quarantineDir.mkdirs();
            }
            FileUtils.copyFile(new File(attachFilePath), new File(quarantineFolder + "/" + FilenameUtils.getName(attachFilePath)));
            quarantined = true;
        } catch (IOException e) {
            log.error("Failed to quarantine : '" + attachFilePath + "'", e);
        }
    }

    public void send(String token) {
        log.trace("send(String token) : token=" + token);
        try {
            doSend(token);
            sent = true;
            log.info("Document: '" + attachFilePath + "' was sent successfully to address: '" + token + "'");
        } catch (Exception e) {
            if (settings.isQuarantineFiles()) {
                quarantine();
            }
            log.error("Failed to send document: '" + attachFilePath + "' to address: '" + token + "'", e);
        }
    }

    public boolean isQuarantined() {
        return quarantined;
    }

    public boolean isSent() {
        return sent;
    }

    public void setQuarantineFolder(String quarantineFolder) {
        this.quarantineFolder = quarantineFolder;
    }

    protected abstract void doSend(String token) throws Exception;

    protected abstract Object getSender();
}
