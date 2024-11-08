package net.sf.mxlosgi.mxlosgistarter;

import java.io.File;
import java.util.Set;
import net.sf.mxlosgi.mxlosgibookmarksbundle.BookmarkManager;
import net.sf.mxlosgi.mxlosgibookmarksbundle.BookmarksExtension;
import net.sf.mxlosgi.mxlosgichatbundle.Chat;
import net.sf.mxlosgi.mxlosgichatbundle.XMPPChatManager;
import net.sf.mxlosgi.mxlosgichatbundle.listener.ChatListener;
import net.sf.mxlosgi.mxlosgichatbundle.listener.ChatManagerListener;
import net.sf.mxlosgi.mxlosgidiscobundle.DiscoInfoManager;
import net.sf.mxlosgi.mxlosgidiscobundle.DiscoInfoPacketExtension;
import net.sf.mxlosgi.mxlosgidiscobundle.DiscoItemsManager;
import net.sf.mxlosgi.mxlosgidiscobundle.DiscoItemsPacketExtension;
import net.sf.mxlosgi.mxlosgifiletransferbundle.FileTransfer;
import net.sf.mxlosgi.mxlosgifiletransferbundle.FileTransferManager;
import net.sf.mxlosgi.mxlosgifiletransferbundle.FileTransferRequest;
import net.sf.mxlosgi.mxlosgifiletransferbundle.ReceiveFileTransfer;
import net.sf.mxlosgi.mxlosgifiletransferbundle.SendFileTransfer;
import net.sf.mxlosgi.mxlosgifiletransferbundle.listener.FileTransferListener;
import net.sf.mxlosgi.mxlosgilastactivitybundle.LastActivityManager;
import net.sf.mxlosgi.mxlosgilastactivitybundle.LastActivityPacketExtension;
import net.sf.mxlosgi.mxlosgilastactivitybundle.listener.LastActivityListener;
import net.sf.mxlosgi.mxlosgimainbundle.Future;
import net.sf.mxlosgi.mxlosgimainbundle.XMPPConnection;
import net.sf.mxlosgi.mxlosgimainbundle.XMPPException;
import net.sf.mxlosgi.mxlosgimainbundle.XMPPMainManager;
import net.sf.mxlosgi.mxlosgimainbundle.ConnectionConfig.SecurityMode;
import net.sf.mxlosgi.mxlosgimainbundle.listener.ConnectionAdapter;
import net.sf.mxlosgi.mxlosgimucbundle.MucChat;
import net.sf.mxlosgi.mxlosgimucbundle.MucInitialPresenceExtension;
import net.sf.mxlosgi.mxlosgimucbundle.MucManager;
import net.sf.mxlosgi.mxlosgimucbundle.MucRoomUser;
import net.sf.mxlosgi.mxlosgimucbundle.RoomInfo;
import net.sf.mxlosgi.mxlosgimucbundle.listener.MucChatListener;
import net.sf.mxlosgi.mxlosgimucbundle.listener.MucListener;
import net.sf.mxlosgi.mxlosgiprivacybundle.PrivacyManager;
import net.sf.mxlosgi.mxlosgiprivatedatabundle.PrivateDataManager;
import net.sf.mxlosgi.mxlosgiregistrationbundle.RegisterExtension;
import net.sf.mxlosgi.mxlosgiregistrationbundle.RegistrationManager;
import net.sf.mxlosgi.mxlosgisearchbundle.SearchExtension;
import net.sf.mxlosgi.mxlosgisearchbundle.SearchManager;
import net.sf.mxlosgi.mxlosgisoftwareversionbundle.SoftwareVersionExtension;
import net.sf.mxlosgi.mxlosgisoftwareversionbundle.SoftwareVersionManager;
import net.sf.mxlosgi.mxlosgivcardbundle.VCardManager;
import net.sf.mxlosgi.mxlosgivcardbundle.VCardPacketExtension;
import net.sf.mxlosgi.mxlosgixmppbundle.JID;
import net.sf.mxlosgi.mxlosgixmppbundle.Message;
import net.sf.mxlosgi.mxlosgixmppbundle.Packet;
import net.sf.mxlosgi.mxlosgixmppbundle.PacketExtension;
import net.sf.mxlosgi.mxlosgixmppbundle.Presence;
import net.sf.mxlosgi.mxlosgixmppbundle.Privacy;
import net.sf.mxlosgi.mxlosgixmppbundle.PrivacyList;
import net.sf.mxlosgi.mxlosgixmppbundle.XMLStanza;
import net.sf.mxlosgi.mxlosgixmppparserbundle.ExtensionParser;
import net.sf.mxlosgi.mxlosgixmppparserbundle.XMPPParser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends ConnectionAdapter implements BundleActivator {

    private ServiceTracker privacyManagerServiceTracker;

    private ServiceTracker chatManagerServiceTracker;

    private ServiceTracker discoInfoManagerServiceTracker;

    private ServiceTracker discoItemsManagerServiceTracker;

    private ServiceTracker vCardManagerServiceTracker;

    private ServiceTracker softwareVersionManagerServiceTracker;

    private ServiceTracker lastActivityManagerServiceTracker;

    private ServiceTracker mucManagerServiceTracker;

    private ServiceTracker fileTransferManagerServiceTracker;

    private ServiceTracker privateDataManagerServiceTracker;

    private ServiceTracker bookmarkManagerServiceTracker;

    private ServiceTracker registrationManagerServiceTracker;

    private ServiceTracker searchManagerServiceTracker;

    private ServiceTracker mainManagerServiceTracker;

    public void start(BundleContext context) throws Exception {
        mainManagerServiceTracker = new ServiceTracker(context, XMPPMainManager.class.getName(), null);
        mainManagerServiceTracker.open();
        XMPPMainManager mainManager = (XMPPMainManager) mainManagerServiceTracker.getService();
        try {
            String serviceName = "gmail.com";
            mainManager.addConnectionListener(this);
            XMPPConnection connection = mainManager.createConnection(serviceName);
            Future future = connection.connect();
            future.complete();
            if ("gmail.com".equals(serviceName)) {
                connection.login("Noah.Shen87", "159357noah");
            } else if ("jabbercn.org".equals(serviceName)) {
                connection.login("Noah", "159357");
            } else if ("jabber.org".equals(serviceName)) {
                connection.login("NoahShen", "159357");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testDisconnect(XMPPConnection connection) throws Exception {
        Thread.sleep(5 * 1000);
        connection.close(new Presence(Presence.Type.unavailable));
    }

    private void testSearch(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(8 * 1000);
        searchManagerServiceTracker = new ServiceTracker(context, SearchManager.class.getName(), null);
        searchManagerServiceTracker.open();
        SearchManager searchManager = (SearchManager) searchManagerServiceTracker.getService();
        boolean b = searchManager.isSupportSearch(connection, new JID("users.szsport.org"));
        System.out.println(b);
        SearchExtension ex = searchManager.getSearchExtension(connection, new JID("users.szsport.org"));
        System.out.println(ex.toXML());
        SearchExtension extension = new SearchExtension();
        extension.getFields().put("nick", "how");
        SearchExtension extensionResult = searchManager.search(connection, extension, new JID("users.szsport.org"));
        for (SearchExtension.Item item : extensionResult.getItems()) {
            System.out.println(item);
            System.out.println(item.getJid());
            System.out.println(item.getFields());
        }
    }

    private void testRegister(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        registrationManagerServiceTracker = new ServiceTracker(context, RegistrationManager.class.getName(), null);
        registrationManagerServiceTracker.open();
        RegistrationManager registrationManager = (RegistrationManager) registrationManagerServiceTracker.getService();
        System.out.println(registrationManager.isSupportRegistration(connection));
        RegisterExtension registerE = registrationManager.getRegisterExtension(connection);
        System.out.println(registerE.toXML());
    }

    private void testBookMark(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        bookmarkManagerServiceTracker = new ServiceTracker(context, BookmarkManager.class.getName(), null);
        bookmarkManagerServiceTracker.open();
        BookmarkManager bookmarkManager = (BookmarkManager) bookmarkManagerServiceTracker.getService();
        bookmarkManager.addBookmarkedConference(connection, "name", new JID("conference.example.com"), false, "Noah", "password");
        bookmarkManager.addBookmarkedURL(connection, "http://www.g.cn", "google china");
        BookmarksExtension bookmarksExtension = bookmarkManager.getBookmarks(connection);
        System.out.println(bookmarksExtension.toXML());
    }

    private void testPrivateData(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        privateDataManagerServiceTracker = new ServiceTracker(context, PrivateDataManager.class.getName(), null);
        privateDataManagerServiceTracker.open();
        PrivateDataManager privateDataManager = (PrivateDataManager) privateDataManagerServiceTracker.getService();
        PacketExtension extension = privateDataManager.getPrivateData(connection, "storage", "storage:bookmarks");
        System.out.println(extension.toXML());
    }

    private void testFileTransfer(XMPPConnection connection, BundleContext context) throws InterruptedException {
        Thread.sleep(10 * 1000);
        fileTransferManagerServiceTracker = new ServiceTracker(context, FileTransferManager.class.getName(), null);
        fileTransferManagerServiceTracker.open();
        FileTransferManager fileTransferManager = (FileTransferManager) fileTransferManagerServiceTracker.getService();
        try {
            SendFileTransfer sendFileTransfer = fileTransferManager.createSendFileTransfer(connection, new JID("Noah@jabbercn.org/Psi"), new File("/home/noah/spark_2_5_8.tar.gz"));
            sendFileTransfer.sendFile();
            while (sendFileTransfer.getStatus() != FileTransfer.Status.cancelled && sendFileTransfer.getStatus() != FileTransfer.Status.error && sendFileTransfer.getStatus() != FileTransfer.Status.complete) {
                System.out.println(sendFileTransfer.getStatus());
                System.out.println(sendFileTransfer.getProgress());
                Thread.sleep(1000 * 2);
            }
            if (sendFileTransfer.getStatus() == FileTransfer.Status.error) {
                if (sendFileTransfer.getException() != null) {
                    sendFileTransfer.getException().printStackTrace();
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        fileTransferManager.addFileTransferListener(new FileTransferListener() {

            @Override
            public void fileTransferRequest(FileTransferRequest request) {
                try {
                    final ReceiveFileTransfer receive = request.accept("http://jabber.org/protocol/bytestreams");
                    Thread thread = new Thread() {

                        public void run() {
                            try {
                                receive.receiveFile(new File("/home/noah/spark_2_5_82.tar.gz"));
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            while (receive.getStatus() != FileTransfer.Status.cancelled && receive.getStatus() != FileTransfer.Status.error && receive.getStatus() != FileTransfer.Status.complete) {
                                System.out.println(receive.getStatus());
                                System.out.println(receive.getProgress());
                                try {
                                    Thread.sleep(1000 * 2);
                                } catch (InterruptedException e) {
                                }
                            }
                            if (receive.getStatus() == FileTransfer.Status.error) {
                                System.out.println(receive.getError());
                                Exception e = receive.getException();
                                if (e != null) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    thread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void testMuc(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        mucManagerServiceTracker = new ServiceTracker(context, MucManager.class.getName(), null);
        mucManagerServiceTracker.open();
        MucManager mucManager = (MucManager) mucManagerServiceTracker.getService();
        mucManager.addMucListener(new MucListener() {

            @Override
            public void declineReceived(XMPPConnection connection, Message message) {
            }

            @Override
            public void invitationReceived(XMPPConnection connection, Message message) {
                System.out.println("=====" + message.toXML());
            }
        });
        JID mucServer = new JID("conference.rooyee.biz");
        JID mucRoom = new JID("rooyee@conference.rooyee.biz");
        System.out.println(mucManager.isServerSupportMuc(connection, mucServer));
        DiscoItemsPacketExtension.Item items[] = mucManager.getRoomList(connection, mucServer);
        for (DiscoItemsPacketExtension.Item item : items) {
            System.out.println(item.toXML());
        }
        RoomInfo roomInfo = mucManager.getRoomInfo(connection, mucRoom);
        System.out.println(roomInfo.isMembersOnly());
        DiscoItemsPacketExtension.Item itemsUsers[] = mucManager.getUsers(connection, mucRoom);
        for (DiscoItemsPacketExtension.Item item : itemsUsers) {
            System.out.println("=======users : " + item.toXML());
        }
    }

    private void testLastActivity(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        lastActivityManagerServiceTracker = new ServiceTracker(context, LastActivityManager.class.getName(), null);
        lastActivityManagerServiceTracker.open();
        LastActivityManager lastActivityManager = (LastActivityManager) lastActivityManagerServiceTracker.getService();
        LastActivityPacketExtension lastA = lastActivityManager.getLastActivity(connection, new JID("Noah", "jabbercn.org", "Pidgin"));
        System.out.println(lastA.toXML());
        lastActivityManager.addLastActivityListener(new LastActivityListener() {

            @Override
            public void idle() {
                System.out.println("IDLE");
            }
        }, 10);
    }

    private void testSoftwareVersion(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        softwareVersionManagerServiceTracker = new ServiceTracker(context, SoftwareVersionManager.class.getName(), null);
        softwareVersionManagerServiceTracker.open();
        SoftwareVersionManager softwareVersionManager = (SoftwareVersionManager) softwareVersionManagerServiceTracker.getService();
        SoftwareVersionExtension version = softwareVersionManager.getSoftwareVersion(connection, new JID("Noah", "jabbercn.org", "Pidgin"));
        System.out.println(version.getName());
    }

    private void testVCard(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        vCardManagerServiceTracker = new ServiceTracker(context, VCardManager.class.getName(), null);
        vCardManagerServiceTracker.open();
        VCardManager vCardManager = (VCardManager) vCardManagerServiceTracker.getService();
        VCardPacketExtension vCard = vCardManager.getVCard(connection, new JID("Noah", "jabbercn.org", null));
        System.out.println(vCard.getPhotoBinval().length());
    }

    private void testDisco(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        discoInfoManagerServiceTracker = new ServiceTracker(context, DiscoInfoManager.class.getName(), null);
        discoInfoManagerServiceTracker.open();
        DiscoInfoManager discoInfoManager = (DiscoInfoManager) discoInfoManagerServiceTracker.getService();
        DiscoInfoPacketExtension discoInfo = discoInfoManager.getDiscoInfo(connection, new JID("Noah", "jabbercn.org", "Pidgin"));
        System.out.println("==========" + discoInfo.toXML());
        discoInfoManager.getDiscoInfo(connection, new JID(null, "jabbercn.org", null));
        discoItemsManagerServiceTracker = new ServiceTracker(context, DiscoItemsManager.class.getName(), null);
        discoItemsManagerServiceTracker.open();
        DiscoItemsManager discoItemsManager = (DiscoItemsManager) discoItemsManagerServiceTracker.getService();
        DiscoItemsPacketExtension discoItems = discoItemsManager.getDiscoItems(connection, new JID(null, "jabbercn.org", null));
        System.out.println("==========" + discoItems.toXML());
    }

    private void testChat(XMPPConnection connection, BundleContext context) throws InterruptedException {
        Thread.sleep(10 * 1000);
        chatManagerServiceTracker = new ServiceTracker(context, XMPPChatManager.class.getName(), null);
        chatManagerServiceTracker.open();
        XMPPChatManager chatManager = (XMPPChatManager) chatManagerServiceTracker.getService();
        chatManager.addChatManagerListener(new ChatManagerListener() {

            @Override
            public void chatClosed(XMPPChatManager chatManager, Chat chat) {
                System.out.println("chatClosed " + chat);
            }

            @Override
            public void chatCreated(XMPPChatManager chatManager, Chat chat) {
                System.out.println("chatCreated " + chat);
            }
        });
        Chat chat = chatManager.createChat(connection, new JID("Noah", "jabbercn.org", null), new ChatListener() {

            @Override
            public void processMessage(Chat chat, Message message) {
                System.out.println("processMessage : " + message.getBody());
                chat.sendMessage("text");
            }

            @Override
            public void resourceChanged(Chat chat, String currentChatResource) {
                System.out.println("resourceChanged : " + currentChatResource);
            }
        });
        chat.sendMessage("Hello!");
    }

    private void testPrivacy(XMPPConnection connection, BundleContext context) throws InterruptedException, XMPPException {
        Thread.sleep(10 * 1000);
        privacyManagerServiceTracker = new ServiceTracker(context, PrivacyManager.class.getName(), null);
        privacyManagerServiceTracker.open();
        PrivacyManager privacyManager = (PrivacyManager) privacyManagerServiceTracker.getService();
        privacyManager.getPrivacyLists(connection);
        privacyManager.declineActiveList(connection);
    }

    public void stop(BundleContext context) throws Exception {
        if (mainManagerServiceTracker != null) {
            mainManagerServiceTracker.close();
            mainManagerServiceTracker = null;
        }
        if (privacyManagerServiceTracker != null) {
            privacyManagerServiceTracker.close();
            privacyManagerServiceTracker = null;
        }
        if (chatManagerServiceTracker != null) {
            chatManagerServiceTracker.close();
            chatManagerServiceTracker = null;
        }
        if (discoInfoManagerServiceTracker != null) {
            discoInfoManagerServiceTracker.close();
            discoInfoManagerServiceTracker = null;
        }
        if (discoItemsManagerServiceTracker != null) {
            discoItemsManagerServiceTracker.close();
            discoItemsManagerServiceTracker = null;
        }
        if (vCardManagerServiceTracker != null) {
            vCardManagerServiceTracker.close();
            vCardManagerServiceTracker = null;
        }
        if (softwareVersionManagerServiceTracker != null) {
            softwareVersionManagerServiceTracker.close();
            softwareVersionManagerServiceTracker = null;
        }
        if (lastActivityManagerServiceTracker != null) {
            lastActivityManagerServiceTracker.close();
            lastActivityManagerServiceTracker = null;
        }
        if (privateDataManagerServiceTracker != null) {
            privateDataManagerServiceTracker.close();
            privateDataManagerServiceTracker = null;
        }
        if (searchManagerServiceTracker != null) {
            searchManagerServiceTracker.close();
            searchManagerServiceTracker = null;
        }
    }

    @Override
    public void connectionConnected(XMPPConnection connection) {
    }
}
