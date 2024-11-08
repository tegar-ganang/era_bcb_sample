package com.hs.mail.imap.processor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import com.hs.mail.container.config.Config;
import com.hs.mail.imap.ImapSession;
import com.hs.mail.imap.mailbox.Mailbox;
import com.hs.mail.imap.mailbox.MailboxManager;
import com.hs.mail.imap.message.request.AppendRequest;
import com.hs.mail.imap.message.request.ImapRequest;
import com.hs.mail.imap.message.responder.Responder;
import com.hs.mail.imap.message.response.HumanReadableText;

/**
 * 
 * @author Won Chul Doh
 * @since Feb 1, 2010
 *
 */
public class AppendProcessor extends AbstractImapProcessor {

    @Override
    protected void doProcess(ImapSession session, ImapRequest message, Responder responder) throws Exception {
        AppendRequest request = (AppendRequest) message;
        String mailboxName = request.getMailbox();
        MailboxManager manager = getMailboxManager();
        Mailbox mailbox = manager.getMailbox(session.getUserID(), mailboxName);
        if (mailbox == null) {
            responder.taggedNo(request, "[TRYCREATE]", HumanReadableText.MAILBOX_NOT_FOUND);
        } else {
            File temp = File.createTempFile("mail", null, Config.getTempDirectory());
            ChannelBuffer buffer = request.getMessage();
            try {
                writeMessage(buffer, temp);
                manager.appendMessage(mailbox.getMailboxID(), request.getDatetime(), request.getFlags(), temp);
            } catch (Exception ex) {
                forceDelete(temp);
                throw ex;
            }
            responder.okCompleted(request);
        }
    }

    private void writeMessage(ChannelBuffer buffer, File dst) throws IOException {
        ChannelBufferInputStream is = new ChannelBufferInputStream(buffer);
        OutputStream os = null;
        try {
            os = new FileOutputStream(dst);
            IOUtils.copyLarge(is, os);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private void forceDelete(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
        }
    }
}
