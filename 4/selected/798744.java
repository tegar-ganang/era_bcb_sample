package com.smartwish.documentburster.sender;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.aop.Profiled;
import com.smartwish.documentburster.context.BurstingContext;
import com.smartwish.documentburster.scripting.Scripting;
import java.io.File;
import java.util.concurrent.Callable;

public abstract class Sender implements Callable<Void> {

    protected Log log = LogFactory.getLog(Sender.class);

    protected Object msg;

    protected boolean simulateDistribution = false;

    private boolean quarantined = false;

    private boolean sent = false;

    private int numberOfAttachments = 0;

    protected BurstingContext originalCtx;

    protected Scripting scripting;

    protected BurstingContext ctx;

    public void setScripting(Scripting scripting) {
        this.scripting = scripting;
    }

    public BurstingContext getBurstingContext() {
        return ctx;
    }

    public BurstingContext getOriginalBurstingContext() {
        return originalCtx;
    }

    protected void quarantine() throws Exception {
        log.debug("quarantine()");
        File quarantineDir = new File(ctx.quarantineFolder);
        if (!quarantineDir.exists()) FileUtils.forceMkdir(quarantineDir);
        copyAttachmentsToQuarantine();
        quarantined = true;
        scripting.executeScript(ctx.scripts.quarantineDocument, ctx);
    }

    @Profiled
    public void send() throws Exception {
        log.debug("send()");
        processAttachments();
        try {
            scripting.executeScript(ctx.scripts.startDistributeDocument, ctx);
            doSend();
            scripting.executeScript(ctx.scripts.endDistributeDocument, ctx);
            originalCtx.numberOfDistributedFiles += numberOfAttachments;
        } catch (Exception e) {
            if (ctx.settings.isQuarantineFiles()) quarantine();
            throw e;
        }
        checkAndDeleteFile();
    }

    private void checkAndDeleteFile() {
        boolean isDeleteFiles = ctx.settings.isDeleteFiles();
        if (isDeleteFiles) {
            File extractedFile = new File(ctx.extractFilePath);
            if (extractedFile.delete()) {
                log.info("Document '" + ctx.extractFilePath + "' was deleted because {deletefiles} configuration is true");
            } else {
                log.error("Failed to delete " + ctx.extractFilePath);
            }
        }
    }

    public boolean isQuarantined() {
        return quarantined;
    }

    public boolean isSent() {
        return sent;
    }

    public void setBurstingContext(BurstingContext context) throws Exception {
        this.originalCtx = context;
        this.ctx = (BurstingContext) BeanUtils.cloneBean(context);
    }

    private void processAttachments() {
        if (ctx.attachments.size() > 0) {
            if (ctx.settings.isArchiveAttachments()) numberOfAttachments = 1; else numberOfAttachments = ctx.attachments.size();
        }
    }

    private void copyAttachmentsToQuarantine() throws Exception {
        if (ctx.settings.isArchiveAttachments()) {
            String path = ctx.archiveFilePath;
            FileUtils.copyFile(new File(path), new File(ctx.quarantineFolder + "/" + FilenameUtils.getName(path)));
            originalCtx.numberOfQuarantinedFiles += 1;
        } else for (String path : ctx.attachments) {
            FileUtils.copyFile(new File(path), new File(ctx.quarantineFolder + "/" + FilenameUtils.getName(path)));
            originalCtx.numberOfQuarantinedFiles += 1;
        }
    }

    protected abstract void doSend() throws Exception;

    public Void call() throws Exception {
        send();
        return null;
    }
}
