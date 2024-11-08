package net.sf.mxlosgi.starter;

import java.io.File;
import java.util.Hashtable;
import net.sf.mxlosgi.bookmarks.BookmarkManager;
import net.sf.mxlosgi.bookmarks.BookmarksExtension;
import net.sf.mxlosgi.chat.Chat;
import net.sf.mxlosgi.chat.XmppChatManager;
import net.sf.mxlosgi.chat.listener.ChatListener;
import net.sf.mxlosgi.chat.listener.ChatManagerListener;
import net.sf.mxlosgi.core.Future;
import net.sf.mxlosgi.core.XmppConnection;
import net.sf.mxlosgi.core.XmppException;
import net.sf.mxlosgi.core.XmppMainManager;
import net.sf.mxlosgi.core.filter.StanzaFilter;
import net.sf.mxlosgi.disco.DiscoInfoManager;
import net.sf.mxlosgi.disco.DiscoInfoPacketExtension;
import net.sf.mxlosgi.disco.DiscoItemsManager;
import net.sf.mxlosgi.disco.DiscoItemsPacketExtension;
import net.sf.mxlosgi.filetransfer.FileTransfer;
import net.sf.mxlosgi.filetransfer.FileTransferManager;
import net.sf.mxlosgi.filetransfer.FileTransferRequest;
import net.sf.mxlosgi.filetransfer.ReceiveFileTransfer;
import net.sf.mxlosgi.filetransfer.SendFileTransfer;
import net.sf.mxlosgi.filetransfer.listener.FileTransferListener;
import net.sf.mxlosgi.lastactivity.LastActivityManager;
import net.sf.mxlosgi.lastactivity.LastActivityPacketExtension;
import net.sf.mxlosgi.lastactivity.listener.LastActivityListener;
import net.sf.mxlosgi.muc.MucManager;
import net.sf.mxlosgi.muc.RoomInfo;
import net.sf.mxlosgi.muc.listener.MucListener;
import net.sf.mxlosgi.privacy.PrivacyManager;
import net.sf.mxlosgi.privatedata.PrivateDataManager;
import net.sf.mxlosgi.registration.RegisterExtension;
import net.sf.mxlosgi.registration.RegistrationManager;
import net.sf.mxlosgi.search.SearchExtension;
import net.sf.mxlosgi.search.SearchManager;
import net.sf.mxlosgi.softwareversion.SoftwareVersionExtension;
import net.sf.mxlosgi.softwareversion.SoftwareVersionManager;
import net.sf.mxlosgi.vcard.VCardManager;
import net.sf.mxlosgi.vcard.VCardPacketExtension;
import net.sf.mxlosgi.xmpp.JID;
import net.sf.mxlosgi.xmpp.Message;
import net.sf.mxlosgi.xmpp.PacketExtension;
import net.sf.mxlosgi.xmpp.Presence;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceTracker mainManagerServiceTracker;

    public void start(BundleContext context) throws Exception {
        mainManagerServiceTracker = new ServiceTracker(context, XmppMainManager.class.getName(), null);
        mainManagerServiceTracker.open();
        XmppMainManager mainManager = (XmppMainManager) mainManagerServiceTracker.getService();
        String serviceName = "gmail.com";
        XmppConnection connection = mainManager.createConnection(serviceName);
        Future future = connection.connect();
        future.complete();
        String username = null;
        String password = null;
        if ("gmail.com".equals(serviceName)) {
            username = "Noah.Shen87";
            password = "159357noah";
        } else if ("jabbercn.org".equals(serviceName)) {
            username = "Noah";
            password = "159357";
        } else if ("jabber.org".equals(serviceName)) {
            username = "NoahShen";
            password = "159357";
        }
        Future futureLogin = connection.login(username, password);
        futureLogin.complete();
        testFileTransfer(connection, context);
    }

    private void testFileTransfer(XmppConnection connection, BundleContext context) throws InterruptedException {
        Thread.sleep(10 * 1000);
        ServiceTracker fileTransferManagerServiceTracker = new ServiceTracker(context, FileTransferManager.class.getName(), null);
        fileTransferManagerServiceTracker.open();
        FileTransferManager fileTransferManager = (FileTransferManager) fileTransferManagerServiceTracker.getService();
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("isProxy", "true");
        context.registerService(JID.class.getName(), new JID("proxy.4business.nl"), properties);
        FileTransferListener fileTransferListener = new FileTransferListener() {

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
        };
        context.registerService(FileTransferListener.class.getName(), fileTransferListener, null);
        fileTransferManagerServiceTracker.close();
    }

    private void testBookMark(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker bookmarkManagerServiceTracker = new ServiceTracker(context, BookmarkManager.class.getName(), null);
        bookmarkManagerServiceTracker.open();
        BookmarkManager bookmarkManager = (BookmarkManager) bookmarkManagerServiceTracker.getService();
        bookmarkManager.addBookmarkedConference(connection, "name", new JID("conference.example.com"), false, "Noah", "password");
        bookmarkManager.addBookmarkedURL(connection, "http://www.g.cn", "google china");
        BookmarksExtension bookmarksExtension = bookmarkManager.getBookmarks(connection);
        System.out.println(bookmarksExtension.toXML());
        bookmarkManagerServiceTracker.close();
    }

    private void testVCard(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker vCardManagerServiceTracker = new ServiceTracker(context, VCardManager.class.getName(), null);
        vCardManagerServiceTracker.open();
        VCardManager vCardManager = (VCardManager) vCardManagerServiceTracker.getService();
        VCardPacketExtension vCard = vCardManager.getVCard(connection, new JID("xiaoi001", "gmail.com", null));
        System.out.println(vCard.getPhotoBinval().length());
        vCardManagerServiceTracker.close();
    }

    private void testSoftwareVersion(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker softwareVersionManagerServiceTracker = new ServiceTracker(context, SoftwareVersionManager.class.getName(), null);
        softwareVersionManagerServiceTracker.open();
        SoftwareVersionManager softwareVersionManager = (SoftwareVersionManager) softwareVersionManagerServiceTracker.getService();
        SoftwareVersionExtension version = softwareVersionManager.getSoftwareVersion(connection, new JID("Noah", "jabbercn.org", "Pidgin"));
        System.out.println(version.getName());
        softwareVersionManagerServiceTracker.close();
    }

    private void testSearch(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(8 * 1000);
        ServiceTracker searchManagerServiceTracker = new ServiceTracker(context, SearchManager.class.getName(), null);
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
        searchManagerServiceTracker.close();
    }

    private void testRegister(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker registrationManagerServiceTracker = new ServiceTracker(context, RegistrationManager.class.getName(), null);
        registrationManagerServiceTracker.open();
        RegistrationManager registrationManager = (RegistrationManager) registrationManagerServiceTracker.getService();
        System.out.println(registrationManager.isSupportRegistration(connection));
        RegisterExtension registerE = registrationManager.getRegisterExtension(connection);
        System.out.println(registerE.toXML());
        registrationManagerServiceTracker.close();
    }

    private void testPrivateData(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker privateDataManagerServiceTracker = new ServiceTracker(context, PrivateDataManager.class.getName(), null);
        privateDataManagerServiceTracker.open();
        PrivateDataManager privateDataManager = (PrivateDataManager) privateDataManagerServiceTracker.getService();
        PacketExtension extension = privateDataManager.getPrivateData(connection, "storage", "storage:bookmarks");
        System.out.println(extension.toXML());
        privateDataManagerServiceTracker.close();
    }

    private void testMuc(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker mucManagerServiceTracker = new ServiceTracker(context, MucManager.class.getName(), null);
        mucManagerServiceTracker.open();
        MucManager mucManager = (MucManager) mucManagerServiceTracker.getService();
        context.registerService(MucListener.class.getName(), new MucListener() {

            @Override
            public void declineReceived(XmppConnection connection, Message message) {
            }

            @Override
            public void invitationReceived(XmppConnection connection, Message message) {
                System.out.println("=====" + message.toXML());
            }
        }, null);
        JID mucServer = new JID("conference.ubuntu-jabber.de");
        JID mucRoom = new JID("ubuntu@conference.ubuntu-jabber.de");
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
        mucManagerServiceTracker.close();
    }

    private void testLastActivity(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker lastActivityManagerServiceTracker = new ServiceTracker(context, LastActivityManager.class.getName(), null);
        lastActivityManagerServiceTracker.open();
        LastActivityManager lastActivityManager = (LastActivityManager) lastActivityManagerServiceTracker.getService();
        LastActivityPacketExtension lastA = lastActivityManager.getLastActivity(connection, new JID("Noah.Shen87", "gmail.com", "Pidgin9F6228D2"));
        System.out.println(lastA.toXML());
        Hashtable<String, Long> properties = new Hashtable<String, Long>();
        properties.put("idleSecond", 10L);
        context.registerService(LastActivityListener.class.getName(), new LastActivityListener() {

            @Override
            public void idle() {
                System.out.println("idle=====================");
            }
        }, properties);
        lastActivityManagerServiceTracker.close();
    }

    private void testChat(XmppConnection connection, BundleContext context) throws InterruptedException {
        Thread.sleep(10 * 1000);
        ServiceTracker chatManagerServiceTracker = new ServiceTracker(context, XmppChatManager.class.getName(), null);
        chatManagerServiceTracker.open();
        XmppChatManager chatManager = (XmppChatManager) chatManagerServiceTracker.getService();
        context.registerService(ChatManagerListener.class.getName(), new ChatManagerListener() {

            @Override
            public void chatClosed(XmppChatManager chatManager, Chat chat) {
                System.out.println("chatClosed " + chat);
            }

            @Override
            public void chatCreated(XmppChatManager chatManager, Chat chat) {
                System.out.println("chatCreated " + chat);
            }
        }, null);
        context.registerService(ChatListener.class.getName(), new ChatListener() {

            @Override
            public void processMessage(Chat chat, Message message) {
                System.out.println("processMessage : " + message.getBody());
                chat.sendMessage("text");
            }

            @Override
            public void resourceChanged(Chat chat, String currentChatResource) {
                System.out.println("resourceChanged : " + currentChatResource);
            }
        }, null);
        Chat chat = chatManager.createChat(connection, new JID("Noah.Shen87", "gmail.com", null));
        chat.sendMessage("Hello!");
        chatManagerServiceTracker.close();
    }

    private void testPrivacy(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker privacyManagerServiceTracker = new ServiceTracker(context, PrivacyManager.class.getName(), null);
        privacyManagerServiceTracker.open();
        PrivacyManager privacyManager = (PrivacyManager) privacyManagerServiceTracker.getService();
        privacyManager.getPrivacyLists(connection);
        privacyManager.declineActiveList(connection);
        privacyManagerServiceTracker.close();
    }

    private void testDisconnect(XmppConnection connection) throws Exception {
        Thread.sleep(5 * 1000);
        connection.close(new Presence(Presence.Type.unavailable));
    }

    private void testDisco(XmppConnection connection, BundleContext context) throws InterruptedException, XmppException {
        Thread.sleep(10 * 1000);
        ServiceTracker discoInfoManagerServiceTracker = new ServiceTracker(context, DiscoInfoManager.class.getName(), null);
        discoInfoManagerServiceTracker.open();
        DiscoInfoManager discoInfoManager = (DiscoInfoManager) discoInfoManagerServiceTracker.getService();
        DiscoInfoPacketExtension discoInfo = discoInfoManager.getDiscoInfo(connection, new JID("Noah.Shen87", "gmail.com", "Pidgin6CB40157"));
        System.out.println("==========" + discoInfo.toXML());
        discoInfo = discoInfoManager.getDiscoInfo(connection, new JID(null, "gmail.com", null));
        System.out.println("==========" + discoInfo.toXML());
        discoInfoManagerServiceTracker.close();
        ServiceTracker discoItemsManagerServiceTracker = new ServiceTracker(context, DiscoItemsManager.class.getName(), null);
        discoItemsManagerServiceTracker.open();
        DiscoItemsManager discoItemsManager = (DiscoItemsManager) discoItemsManagerServiceTracker.getService();
        DiscoItemsPacketExtension discoItems = discoItemsManager.getDiscoItems(connection, new JID(null, "jabber.org", null));
        System.out.println("==========" + discoItems.toXML());
        discoItemsManagerServiceTracker.close();
    }

    public void stop(BundleContext context) throws Exception {
    }
}
