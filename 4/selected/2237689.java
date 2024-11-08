package lebah.mail;

import java.util.*;
import javax.mail.*;
import com.sun.mail.pop3.*;

public class Test {

    public static void main(String[] args) throws Exception {
        String username = "sbahrin";
        String password = "bugz123";
        Store store = null;
        try {
            String host = "mail.in-fusion.com.my";
            String protocol = "pop3";
            User user = new User(username, password, host, protocol);
            Properties props = System.getProperties();
            Session msgSession = Session.getDefaultInstance(props, null);
            store = msgSession.getStore(new URLName(user.getUrl()));
            store.connect();
            getInbox(store);
        } catch (javax.mail.AuthenticationFailedException e) {
            System.out.println(e.getMessage());
        } finally {
            if (store != null) store.close();
        }
    }

    static void getInbox(Store store) {
        System.out.println("Getting INBOX...Please wait...");
        try {
            Folder folder = store.getDefaultFolder();
            folder = folder.getFolder("INBOX");
            try {
                folder.open(Folder.READ_WRITE);
                System.out.println("read_write");
            } catch (MessagingException ex) {
                folder.open(Folder.READ_ONLY);
                System.out.println("read_only");
            }
            if (folder instanceof POP3Folder) {
                folder = (POP3Folder) folder;
                System.out.println("This is POP3 folder.");
            }
            int totalMessages = folder.getMessageCount();
            System.out.println("Total Messages = " + totalMessages);
            Message[] msgs = folder.getMessages();
            MsgFolder msgFolder = new MsgFolder();
            for (int i = 0; i < msgs.length; i++) {
                int msgNumber = msgs[i].getMessageNumber();
                Message msg = folder.getMessage(msgNumber);
                msgFolder.addInbox(msg);
            }
            Object[] msgInfos = msgInfos = msgFolder.getInbox();
            int startno = 0;
            int msglength = 20;
            int i = 0, cnt = 0;
            for (cnt = 0, i = startno; cnt < msglength && i < msgInfos.length; i++, cnt++) {
                MsgInfo msgInfo = (MsgInfo) msgInfos[i];
                if (msgInfo.hasAttachment()) {
                }
                System.out.println(msgInfo.getTo());
                System.out.println(msgInfo.getMsgNum());
                System.out.println("From: " + msgInfo.getFrom());
                System.out.println("Date: " + msgInfo.getDate());
                System.out.println("Subject: " + msgInfo.getSubject());
                System.out.println("Size: " + msgInfo.getSize());
                if (msgInfos.length > 0) {
                    System.out.println(" Messages " + startno + " to " + i + "of " + msgInfos.length);
                } else {
                    System.out.println("This folder is empty.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Done..");
    }
}
