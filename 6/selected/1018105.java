package ua.snuk182.asia.services.xmpp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.ChatStateListener;
import org.jivesoftware.smackx.DefaultMessageEventRequestListener;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.MessageEventNotificationListener;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;
import ua.snuk182.asia.R;
import ua.snuk182.asia.core.dataentity.Buddy;
import ua.snuk182.asia.core.dataentity.BuddyGroup;
import ua.snuk182.asia.core.dataentity.FileInfo;
import ua.snuk182.asia.core.dataentity.FileMessage;
import ua.snuk182.asia.core.dataentity.MultiChatRoom;
import ua.snuk182.asia.core.dataentity.MultiChatRoomOccupants;
import ua.snuk182.asia.core.dataentity.OnlineInfo;
import ua.snuk182.asia.core.dataentity.PersonalInfo;
import ua.snuk182.asia.core.dataentity.TextMessage;
import ua.snuk182.asia.services.ServiceUtils;
import ua.snuk182.asia.services.api.AccountService;
import ua.snuk182.asia.services.api.IAccountServiceResponse;
import ua.snuk182.asia.services.api.ProtocolException;
import android.content.Context;

public class XMPPService extends AccountService implements ConnectionListener, MessageListener, ChatManagerListener, RosterListener, MessageEventNotificationListener, ChatStateListener, FileTransferListener {

    private static final String LOGIN_PORT = "loginport";

    private static final String LOGIN_HOST = "loginhost";

    private static final String PASSWORD = "password";

    private static final String JID = "jid";

    private static final Random random = new Random();

    private OnlineInfo onlineInfo;

    XMPPConnection connection;

    final Map<String, Chat> chats = new HashMap<String, Chat>();

    final Map<String, MultiUserChat> multichats = new HashMap<String, MultiUserChat>();

    final List<FileTransferRequest> fileTransfers = Collections.synchronizedList(new ArrayList<FileTransferRequest>());

    private volatile boolean isContactListReady = false;

    @SuppressWarnings("unused")
    private boolean isSecure = false;

    String groupchatService = null;

    private List<OnlineInfo> infos = Collections.synchronizedList(new ArrayList<OnlineInfo>());

    private final DefaultMessageEventRequestListener messageEventListener = new DefaultMessageEventRequestListener();

    MessageEventManager messageEventManager;

    FileTransferManager ftm;

    @Override
    public void processMessage(Chat chat, final Message message) {
        final String log = "message " + chat.getParticipant();
        log(log);
        processMessageInternal(message, false);
    }

    private void processMessageInternal(final Message message, boolean resourceAsWriterId) {
        try {
            TextMessage txtmessage = XMPPEntityAdapter.xmppMessage2TextMessage(message, serviceId, resourceAsWriterId);
            serviceResponse.respond(IAccountServiceResponse.RES_MESSAGE, getServiceId(), txtmessage);
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    public void chatCreated(final Chat chat, boolean createdLocally) {
        final String log = "chat " + chat.getParticipant();
        log(log);
        if (chats.get(XMPPEntityAdapter.normalizeJID(chat.getParticipant())) == null) {
            chat.addMessageListener(this);
            chats.put(XMPPEntityAdapter.normalizeJID(chat.getParticipant()), chat);
        }
    }

    @Override
    public void presenceChanged(final Presence presence) {
        final String log = "presence " + presence.getFrom();
        log(log);
        OnlineInfo info = XMPPEntityAdapter.presence2OnlineInfo(presence);
        synchronized (infos) {
            infos.add(info);
        }
        if (isContactListReady) {
            checkCachedInfos();
        }
    }

    private void checkCachedInfos() {
        synchronized (infos) {
            while (infos.size() > 0) {
                for (int i = infos.size() - 1; i >= 0; i--) {
                    OnlineInfo info = infos.remove(i);
                    try {
                        serviceResponse.respond(IAccountServiceResponse.RES_BUDDYSTATECHANGED, serviceId, info);
                    } catch (ProtocolException e) {
                        log(e);
                    }
                }
            }
        }
    }

    @Override
    public void entriesUpdated(Collection<String> addresses) {
    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {
    }

    @Override
    public void entriesAdded(Collection<String> addresses) {
    }

    private String un = null;

    private String pw = null;

    private String serviceName = null;

    private String loginHost = "jabber.ru";

    private int loginPort = 5222;

    public XMPPService(Context context, IAccountServiceResponse serviceResponse, byte serviceId) {
        super(context, serviceResponse, serviceId);
        options.put(JID, null);
        options.put(PASSWORD, null);
        options.put(LOGIN_HOST, loginHost);
        options.put(LOGIN_PORT, loginPort + "");
        options.put(PING_TIMEOUT, pingTimeout + "");
    }

    public XMPPService(Context context) {
        super(context, null, (byte) -1);
    }

    @Override
    public Object request(short action, final Object... args) throws ProtocolException {
        switch(action) {
            case AccountService.REQ_SENDFILE:
                List<File> files = new ArrayList<File>();
                files.add((File) args[1]);
                sendFile((Buddy) args[0], files);
                return (long) files.hashCode();
            case AccountService.REQ_FILERESPOND:
                fileRespond((FileMessage) args[0], (Boolean) args[1]);
                break;
            case AccountService.REQ_FILECANCEL:
                break;
            case AccountService.REQ_GETBUDDYINFO:
                break;
            case AccountService.REQ_ADDGROUP:
                addGroup((BuddyGroup) args[0]);
                break;
            case AccountService.REQ_ADDBUDDY:
                addBuddy((Buddy) args[0], (BuddyGroup) args[1]);
                break;
            case AccountService.REQ_REMOVEBUDDY:
                break;
            case AccountService.REQ_MOVEBUDDIES:
                break;
            case AccountService.REQ_REMOVEBUDDIES:
                break;
            case AccountService.REQ_SETSTATUS:
                if (connection != null) {
                    onlineInfo.userStatus = (Byte) args[0];
                    connection.sendPacket(XMPPEntityAdapter.userStatus2XMPPPresence(onlineInfo.userStatus));
                }
                break;
            case AccountService.REQ_SETEXTENDEDSTATUS:
                break;
            case AccountService.REQ_AUTHREQUEST:
                break;
            case AccountService.REQ_AUTHRESPONSE:
                break;
            case AccountService.REQ_SEARCHFORBUDDY_BY_UID:
                break;
            case AccountService.REQ_DISCONNECT:
                if (connection != null) {
                    connection.disconnect();
                }
                break;
            case AccountService.REQ_CONNECT:
                if (args.length > 0) {
                    onlineInfo = new OnlineInfo();
                    onlineInfo.protocolUid = getJID();
                    onlineInfo.userStatus = (Byte) args[0];
                }
                connect(args);
                break;
            case AccountService.REQ_GETCONTACTLIST:
                break;
            case AccountService.REQ_GETEXTENDEDSTATUS:
                break;
            case AccountService.REQ_RENAMEBUDDY:
                break;
            case AccountService.REQ_RENAMEGROUP:
                break;
            case AccountService.REQ_MOVEBUDDY:
                break;
            case AccountService.REQ_GETGROUPLIST:
                break;
            case AccountService.REQ_SENDMESSAGE:
                try {
                    sendMessage((TextMessage) args[0]);
                } catch (Exception e) {
                    log(e);
                }
                break;
            case AccountService.REQ_GETICON:
                if (connection == null) {
                    return null;
                }
                getPersonalInfo((String) args[0]);
                break;
            case AccountService.REQ_REMOVEGROUP:
                break;
            case AccountService.REQ_SENDTYPING:
                if (connection == null) {
                    return null;
                }
                sendTyping((String) args[0]);
                break;
            case AccountService.REQ_GET_CHAT_ROOMS:
                getAvailableChatRooms();
                break;
            case AccountService.REQ_CREATE_CHAT_ROOM:
                return createChatRoom((String) args[0], (String) args[1], (String) args[2], args.length > 3 ? (String) args[3] : null);
            case AccountService.REQ_JOIN_CHAT_ROOM:
                return joinChatRoom((String) args[0], (String) args[1], args.length > 2 ? (String) args[2] : null);
            case AccountService.REQ_CHECK_GROUPCHATS_AVAILABLE:
                return groupchatService != null;
            case AccountService.REQ_LEAVE_CHAT_ROOM:
                leaveChatRoom((String) args[0]);
                break;
            case AccountService.REQ_GET_CHAT_ROOM_OCCUPANTS:
                return getChatRoomOccupants((String) args[0], (Boolean) args[1]);
            case AccountService.REQ_GETFULLBUDDYINFO:
                getFullInfo((String) args[0]);
                break;
        }
        return null;
    }

    private void addGroup(final BuddyGroup buddyGroup) {
        new Thread() {

            @Override
            public void run() {
                try {
                    connection.getRoster().createGroup(buddyGroup.name);
                    serviceResponse.respond(IAccountServiceResponse.RES_GROUPADDED, serviceId, buddyGroup);
                } catch (ProtocolException e) {
                    try {
                        serviceResponse.respond(IAccountServiceResponse.RES_NOTIFICATION, serviceId, e.getLocalizedMessage());
                    } catch (ProtocolException e1) {
                        log(e);
                    }
                }
            }
        }.start();
    }

    private void addBuddy(final Buddy buddy, final BuddyGroup buddyGroup) {
        new Thread() {

            @Override
            public void run() {
                try {
                    connection.getRoster().createEntry(buddy.protocolUid, buddy.name, new String[] { buddyGroup.name });
                    serviceResponse.respond(IAccountServiceResponse.RES_BUDDYADDED, serviceId, buddy);
                } catch (XMPPException e) {
                    try {
                        serviceResponse.respond(IAccountServiceResponse.RES_NOTIFICATION, serviceId, e.getLocalizedMessage());
                    } catch (ProtocolException e1) {
                        log(e);
                    }
                } catch (ProtocolException e) {
                    log(e);
                }
            }
        }.start();
    }

    private void fileRespond(final FileMessage fileMessage, final Boolean accept) {
        new Thread("File transfer " + fileMessage.messageId) {

            @Override
            public void run() {
                for (FileTransferRequest request : fileTransfers) {
                    if (request.hashCode() == fileMessage.messageId) {
                        if (accept) {
                            IncomingFileTransfer transfer = request.accept();
                            try {
                                transfer.recieveFile(ServiceUtils.createLocalFileForReceiving(request.getFileName(), request.getFileSize(), 0));
                            } catch (XMPPException e1) {
                                log(e1);
                            }
                            while (!transfer.isDone()) {
                                try {
                                    if (transfer.getStatus() == Status.error) {
                                        serviceResponse.respond(IAccountServiceResponse.RES_FILEPROGRESS, serviceId, (long) request.hashCode(), request.getFileName(), 100L, (long) transfer.getProgress() * 100, false, transfer.getError().getMessage(), fileMessage.from);
                                    } else {
                                        serviceResponse.respond(IAccountServiceResponse.RES_FILEPROGRESS, serviceId, (long) request.hashCode(), request.getFileName(), 100L, (long) transfer.getProgress() * 100, false, null, fileMessage.from);
                                    }
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    log(e);
                                } catch (ProtocolException e) {
                                    log(e);
                                }
                            }
                        } else {
                            request.reject();
                        }
                        break;
                    }
                }
            }
        }.start();
    }

    private void sendFile(final Buddy buddy, final List<File> files) {
        new Thread("File transfer") {

            @Override
            public void run() {
                OutgoingFileTransfer transfer = ftm.createOutgoingFileTransfer(XMPPEntityAdapter.normalizeJID(buddy.protocolUid));
                for (File file : files) {
                    try {
                        transfer.sendFile(file, file.getName());
                    } catch (XMPPException e) {
                        log(e);
                        return;
                    }
                    while (!transfer.isDone()) {
                        try {
                            if (transfer.getStatus() == Status.error) {
                                serviceResponse.respond(IAccountServiceResponse.RES_FILEPROGRESS, serviceId, (long) files.hashCode(), file.getAbsolutePath(), 100L, (long) transfer.getProgress() * 100, false, transfer.getError().getMessage(), buddy.protocolUid);
                            } else {
                                serviceResponse.respond(IAccountServiceResponse.RES_FILEPROGRESS, serviceId, (long) files.hashCode(), file.getAbsolutePath(), 100L, (long) transfer.getProgress() * 100, false, null, buddy.protocolUid);
                            }
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log(e);
                        } catch (ProtocolException e) {
                            log(e);
                        }
                    }
                }
            }
        }.start();
    }

    private void getFullInfo(final String uid) throws ProtocolException {
        new Thread() {

            @Override
            public void run() {
                PersonalInfo info = new PersonalInfo();
                info.protocolUid = uid;
                if (groupchatService != null && uid.indexOf(groupchatService) > -1) {
                    try {
                        RoomInfo room = MultiUserChat.getRoomInfo(connection, uid);
                        info.properties.putCharSequence(PersonalInfo.INFO_CHAT_DESCRIPTION, room.getDescription());
                        info.properties.putCharSequence(PersonalInfo.INFO_CHAT_OCCUPANTS, room.getOccupantsCount() + "");
                        info.properties.putCharSequence(PersonalInfo.INFO_CHAT_SUBJECT, room.getSubject());
                    } catch (XMPPException e) {
                        log(e);
                    }
                } else {
                    VCard card = new VCard();
                    try {
                        card.load(connection, uid);
                        info.properties.putCharSequence(PersonalInfo.INFO_FIRST_NAME, card.getFirstName());
                        info.properties.putCharSequence(PersonalInfo.INFO_LAST_NAME, card.getLastName());
                        info.properties.putCharSequence(PersonalInfo.INFO_NICK, card.getNickName());
                        info.properties.putCharSequence(PersonalInfo.INFO_EMAIL, card.getEmailHome());
                        for (String prop : card.otherSimpleFields.keySet()) {
                            info.properties.putCharSequence(prop, card.getField(prop));
                        }
                    } catch (XMPPException e) {
                        log(e);
                    }
                }
                try {
                    serviceResponse.respond(IAccountServiceResponse.RES_USERINFO, serviceId, info);
                } catch (ProtocolException e) {
                    log(e);
                }
            }
        }.start();
    }

    private MultiChatRoomOccupants getChatRoomOccupants(String chatId, boolean loadOccupantIcons) throws ProtocolException {
        String roomJid = chatId.indexOf("@") > 1 ? chatId : chatId + "@" + groupchatService;
        MultiUserChat muc = multichats.get(roomJid);
        if (muc == null) {
            throw new ProtocolException("No joined chat found");
        }
        try {
            return XMPPEntityAdapter.xmppMUCOccupants2mcrOccupants(muc, this, loadOccupantIcons);
        } catch (XMPPException e) {
            throw new ProtocolException(e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("unused")
    private boolean amIOwner(MultiUserChat chat) throws XMPPException {
        for (Affiliate aff : chat.getOwners()) {
            if (XMPPEntityAdapter.normalizeJID(aff.getJid()).equals(getJID())) {
                return true;
            }
        }
        return false;
    }

    private void leaveChatRoom(String chatId) throws ProtocolException {
        String roomJid = chatId.indexOf("@") > 1 ? chatId : chatId + "@" + groupchatService;
        MultiUserChat muc = multichats.get(roomJid);
        if (muc == null) {
            throw new ProtocolException("No joined chat found");
        }
        muc.leave();
    }

    private Buddy joinChatRoom(String chatId, String chatPassword, String nickName) throws ProtocolException {
        String roomJid = chatId.indexOf("@") > 1 ? chatId : chatId + "@" + groupchatService;
        MultiUserChat chat = new MultiUserChat(connection, roomJid);
        try {
            chat.join(nickName == null ? un : nickName, chatPassword);
            fillWithListeners(chat);
            multichats.put(chat.getRoom(), chat);
            RoomInfo info = MultiUserChat.getRoomInfo(connection, roomJid);
            return XMPPEntityAdapter.chatRoomInfo2Buddy(this, info, getJID(), getServiceId(), true);
        } catch (XMPPException e) {
            notification(e.getLocalizedMessage());
            throw new ProtocolException(e.getLocalizedMessage());
        }
    }

    private Buddy createChatRoom(String chatId, String chatName, String chatPassword, String nickName) throws ProtocolException {
        try {
            String roomJid = chatId.indexOf("@") > 1 ? chatId : chatId + "@" + groupchatService;
            MultiUserChat chat = new MultiUserChat(connection, roomJid);
            chat.create(nickName == null ? un : nickName);
            Form form = chat.getConfigurationForm();
            Form submitForm = form.createAnswerForm();
            for (Iterator<FormField> fields = form.getFields(); fields.hasNext(); ) {
                FormField field = fields.next();
                log(field.getVariable());
                if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
                    submitForm.setDefaultAnswer(field.getVariable());
                }
            }
            try {
                List<String> owners = new ArrayList<String>();
                owners.add(getJID());
                submitForm.setAnswer("muc#roomconfig_roomowners", owners);
            } catch (Exception e) {
                log("Could not set XMPP muc room owners");
            }
            if (chatPassword != null) {
                submitForm.setAnswer("muc#roomconfig_roomsecret", chatPassword);
                submitForm.setAnswer("muc#roomconfig_publicroom", false);
                submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
            }
            chat.sendConfigurationForm(submitForm);
            chat.changeSubject(chatName);
            fillWithListeners(chat);
            multichats.put(chat.getRoom(), chat);
            return XMPPEntityAdapter.chatInfo2Buddy(this, chat.getRoom(), chatName, true);
        } catch (XMPPException e) {
            notification(e.getLocalizedMessage());
            throw new ProtocolException(e.getLocalizedMessage());
        }
    }

    private List<Buddy> getJoinedChatRooms() {
        Iterator<String> joinedRooms = MultiUserChat.getJoinedRooms(connection, getJID());
        List<Buddy> multiChatBuddies = new ArrayList<Buddy>();
        for (; joinedRooms.hasNext(); ) {
            String roomJid = joinedRooms.next();
            try {
                RoomInfo info = MultiUserChat.getRoomInfo(connection, roomJid);
                multiChatBuddies.add(XMPPEntityAdapter.chatRoomInfo2Buddy(this, info, getJID(), getServiceId(), true));
            } catch (XMPPException e) {
                log(e);
            }
        }
        for (final Buddy room : multiChatBuddies) {
            MultiUserChat chat = new MultiUserChat(connection, room.protocolUid);
            fillWithListeners(chat);
            multichats.put(room.protocolUid, chat);
        }
        return multiChatBuddies;
    }

    private void chatDefaultAction(String chatId, String serviceMessage) {
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_SERVICEMESSAGE, serviceId, chatId, serviceMessage);
            serviceResponse.respond(IAccountServiceResponse.RES_CHAT_PARTICIPANTS, serviceId, chatId, getChatRoomOccupants(chatId, false));
        } catch (ProtocolException e) {
            log(e);
        }
    }

    private void fillWithListeners(final MultiUserChat chat) {
        chat.addMessageListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                Message message = (Message) packet;
                if (message.getFrom().split("/")[1].equals(chat.getNickname())) {
                    return;
                }
                processMessageInternal(message, true);
            }
        });
        chat.addParticipantListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                try {
                    serviceResponse.respond(IAccountServiceResponse.RES_CHAT_PARTICIPANTS, serviceId, getChatRoomOccupants(chat.getRoom(), false));
                } catch (ProtocolException e) {
                    log(e);
                }
            }
        });
        chat.addInvitationRejectionListener(new InvitationRejectionListener() {

            @Override
            public void invitationDeclined(String invitee, String reason) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(invitee) + " declined invitation: " + reason);
            }
        });
        chat.addParticipantStatusListener(new ParticipantStatusListener() {

            @Override
            public void voiceRevoked(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " voice revoked");
            }

            @Override
            public void voiceGranted(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " voice granted");
            }

            @Override
            public void ownershipRevoked(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " ownership revoked");
            }

            @Override
            public void ownershipGranted(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " ownership granted");
            }

            @Override
            public void nicknameChanged(String participant, String newNickname) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " changed nick to " + newNickname);
            }

            @Override
            public void moderatorRevoked(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " moderator revoked");
            }

            @Override
            public void moderatorGranted(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " moderator granted");
            }

            @Override
            public void membershipRevoked(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " membership revoked");
            }

            @Override
            public void membershipGranted(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " membership granted");
            }

            @Override
            public void left(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " left the chat");
            }

            @Override
            public void kicked(String participant, String actor, String reason) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " was kicked by " + actor + ": " + reason);
            }

            @Override
            public void joined(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " joined the chat");
            }

            @Override
            public void banned(String participant, String actor, String reason) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " was banned by " + actor + ": " + reason);
            }

            @Override
            public void adminRevoked(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " admin revoked");
            }

            @Override
            public void adminGranted(String participant) {
                chatDefaultAction(chat.getRoom(), StringUtils.parseResource(participant) + " admin granted");
            }
        });
        chat.addSubjectUpdatedListener(new SubjectUpdatedListener() {

            @Override
            public void subjectUpdated(String subject, String from) {
                try {
                    serviceResponse.respond(IAccountServiceResponse.RES_SERVICEMESSAGE, serviceId, chat.getRoom(), StringUtils.parseResource(from) + " set chat topic to \"" + subject + "\"");
                    serviceResponse.respond(IAccountServiceResponse.RES_BUDDYMODIFIED, getServiceId(), XMPPEntityAdapter.chatInfo2Buddy(XMPPService.this, chat.getRoom(), subject, true));
                } catch (ProtocolException e) {
                    log(e);
                }
            }
        });
    }

    private void getAvailableChatRooms() throws ProtocolException {
        try {
            Collection<String> mucServices = MultiUserChat.getServiceNames(connection);
            if (mucServices.isEmpty()) {
                throw new ProtocolException(ProtocolException.ERROR_NO_GROUPCHAT_AVAILABLE, "This server does not support group chats");
            }
            List<HostedRoom> rooms = new ArrayList<HostedRoom>();
            for (String service : mucServices) {
                try {
                    rooms.addAll(MultiUserChat.getHostedRooms(connection, service));
                } catch (XMPPException e) {
                    log(e);
                }
            }
            List<MultiChatRoom> chats = XMPPEntityAdapter.xmppHostedRooms2MultiChatRooms(this, rooms);
            serviceResponse.respond(IAccountServiceResponse.RES_AVAILABLE_CHATS, serviceId, chats);
        } catch (XMPPException e) {
            throw new ProtocolException(e.getLocalizedMessage());
        }
    }

    private void sendTyping(String jid) {
        messageEventManager.sendComposingNotification(jid, random.nextLong() + "");
    }

    private void sendMessage(TextMessage textMessage) throws XMPPException {
        MultiUserChat muc = multichats.get(textMessage.to);
        if (muc != null) {
            muc.sendMessage(textMessage.text);
            return;
        }
        Chat chat = chats.get(textMessage.to);
        if (chat == null) {
            chat = connection.getChatManager().createChat(textMessage.to, this);
            chats.put(textMessage.to, chat);
        }
        chat.sendMessage(XMPPEntityAdapter.textMessage2XMPPMessage(textMessage, chat.getThreadID(), chat.getParticipant(), Message.Type.chat));
    }

    private String getJID() {
        return un + "@" + serviceName;
    }

    private void connect(Object[] args) throws ProtocolException {
        @SuppressWarnings("unchecked") Map<String, String> sharedPreferences = (Map<String, String>) serviceResponse.respond(IAccountServiceResponse.RES_GETFROMSTORAGE, getServiceId(), IAccountServiceResponse.SHARED_PREFERENCES, options.keySet());
        if (sharedPreferences == null) {
            throw new ProtocolException("Error getting preferences");
        }
        try {
            String jid = sharedPreferences.get(JID);
            String[] jidParams = jid.split("@");
            un = jidParams[0];
            serviceName = jidParams[1];
        } catch (Exception e2) {
            log(e2);
        }
        pw = sharedPreferences.get(PASSWORD);
        try {
            loginPort = Integer.parseInt(sharedPreferences.get(LOGIN_PORT));
        } catch (Exception e2) {
        }
        String host = sharedPreferences.get(LOGIN_HOST);
        if (host != null) {
            loginHost = host;
        }
        if (loginHost == null || loginPort < 1 || un == null || pw == null) {
            throw new ProtocolException("Error: no auth data");
        }
        if (serviceName == null) {
            serviceName = loginHost;
        }
        String ping = sharedPreferences.get(AccountService.PING_TIMEOUT);
        if (ping != null) {
            try {
                pingTimeout = Integer.parseInt(ping);
            } catch (Exception e) {
            }
        }
        if (args.length > 9) {
            isSecure = true;
        } else {
            isSecure = false;
        }
        new Thread("XMPP connector " + getJID()) {

            @Override
            public void run() {
                SmackConfiguration.setPacketReplyTimeout(120000);
                ConnectionConfiguration config = new ConnectionConfiguration(loginHost, loginPort);
                String login;
                if (isGmail()) {
                    config.setSASLAuthenticationEnabled(false);
                    login = un;
                } else {
                    login = un;
                }
                config.setServiceName(serviceName);
                configure(ProviderManager.getInstance());
                connection = new XMPPConnection(config);
                try {
                    isContactListReady = false;
                    serviceResponse.respond(IAccountServiceResponse.RES_CONNECTING, serviceId, 1);
                    connection.connect();
                    serviceResponse.respond(IAccountServiceResponse.RES_CONNECTING, serviceId, 2);
                    new ServiceDiscoveryManager(connection);
                    connection.login(login, pw, "AsiaIM");
                    Roster roster = connection.getRoster();
                    roster.addRosterListener(XMPPService.this);
                    connection.addConnectionListener(XMPPService.this);
                    serviceResponse.respond(IAccountServiceResponse.RES_CONNECTING, serviceId, 3);
                    connection.sendPacket(XMPPEntityAdapter.userStatus2XMPPPresence(onlineInfo.userStatus));
                    serviceResponse.respond(IAccountServiceResponse.RES_CONNECTING, serviceId, 5);
                    while (!roster.rosterInitialized) {
                        Thread.sleep(500);
                    }
                    List<Buddy> buddies = XMPPEntityAdapter.rosterEntryCollection2BuddyList(XMPPService.this, roster.getEntries());
                    List<BuddyGroup> groups = XMPPEntityAdapter.rosterGroupCollection2BuddyGroupList(roster.getGroups(), getJID(), serviceId);
                    serviceResponse.respond(IAccountServiceResponse.RES_CONNECTING, serviceId, 7);
                    connection.getChatManager().addChatListener(XMPPService.this);
                    try {
                        chechGroupChatAvailability();
                        buddies.addAll(getJoinedChatRooms());
                    } catch (Exception e) {
                        log(e);
                    }
                    serviceResponse.respond(IAccountServiceResponse.RES_CLUPDATED, getServiceId(), buddies, groups);
                    serviceResponse.respond(IAccountServiceResponse.RES_CONNECTING, serviceId, 9);
                    serviceResponse.respond(IAccountServiceResponse.RES_ACCOUNTUPDATED, serviceId, onlineInfo);
                    serviceResponse.respond(IAccountServiceResponse.RES_CONNECTED, getServiceId());
                    getPersonalInfo(getJID());
                    isContactListReady = true;
                    checkCachedInfos();
                    messageEventManager = new MessageEventManager(connection);
                    if (onlineInfo.userStatus != Buddy.ST_INVISIBLE) {
                        messageEventManager.addMessageEventRequestListener(messageEventListener);
                    }
                    messageEventManager.addMessageEventNotificationListener(XMPPService.this);
                    ftm = new FileTransferManager(connection);
                    ftm.addFileTransferListener(XMPPService.this);
                    sendKeepalive();
                } catch (Exception e) {
                    log(e);
                    connection = null;
                    connectionClosedOnError(e);
                }
            }
        }.start();
    }

    protected boolean isGmail() {
        return serviceName.equals("gmail.com") || serviceName.equals("googlemail.com");
    }

    protected void chechGroupChatAvailability() throws XMPPException {
        Collection<String> mucServices = MultiUserChat.getServiceNames(connection);
        if (!mucServices.isEmpty()) {
            groupchatService = mucServices.iterator().next();
        }
    }

    private void respondInfo(VCard card, String jid, PersonalInfo personalInfo) throws ProtocolException {
        personalInfo.protocolUid = jid;
        String fn;
        if (card.getNickName() != null && card.getNickName().length() > 0) {
            personalInfo.properties.putString(PersonalInfo.INFO_NICK, card.getNickName());
            serviceResponse.respond(IAccountServiceResponse.RES_USERINFO, getServiceId(), personalInfo);
        } else if ((fn = card.getField("FN")) != null && fn.length() > 0) {
            personalInfo.properties.putString(PersonalInfo.INFO_NICK, fn);
            serviceResponse.respond(IAccountServiceResponse.RES_USERINFO, getServiceId(), personalInfo);
        }
        byte[] icon = card.getAvatar();
        if (icon != null) {
            serviceResponse.respond(IAccountServiceResponse.RES_SAVEIMAGEFILE, getServiceId(), icon, jid, new String(card.getAvatarHash().hashCode() + ""));
        }
    }

    @Override
    public String getServiceName() {
        return context.getString(R.string.xmpp_service_name);
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public int getProtocolOptionNames() {
        return R.array.xmpp_preference_names;
    }

    @Override
    public int getProtocolOptionDefaults() {
        return R.array.xmpp_preference_defaults;
    }

    @Override
    public int getProtocolOptionStrings() {
        return R.array.xmpp_preference_strings;
    }

    private final void getPersonalInfo(final String jid) {
        new Thread("XMPP icon getter " + jid) {

            @Override
            public void run() {
                loadCard(jid);
            }
        }.start();
    }

    synchronized void loadCard(final String jid) {
        final VCard card = new VCard();
        try {
            card.load(connection, jid);
            log("got card " + jid + " " + card.getNickName() + " " + card.getAvatar());
            new Thread() {

                @Override
                public void run() {
                    try {
                        respondInfo(card, jid, new PersonalInfo());
                    } catch (ProtocolException e) {
                        log(e);
                    }
                }
            }.start();
        } catch (Exception e) {
            log(e);
        }
    }

    @Override
    protected void timeoutDisconnect() {
        closeKeepaliveThread();
        connection.disconnect();
    }

    @Override
    protected short getCurrentState() {
        return connection.isConnected() ? AccountService.STATE_CONNECTED : AccountService.STATE_DISCONNECTED;
    }

    @Override
    protected String getUserID() {
        return getJID();
    }

    @Override
    public void connectionClosed() {
        log("Connection closed " + getJID());
        closeKeepaliveThread();
        isContactListReady = false;
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_DISCONNECTED, serviceId);
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        log("Connection closed " + getJID());
        log(e);
        closeKeepaliveThread();
        isContactListReady = false;
        try {
            if ((e instanceof IOException) || (e instanceof XMPPException && ((XMPPException) e).getXMPPError() != null && ((XMPPException) e).getXMPPError().getCondition().equals("remote-server-timeout"))) {
                serviceResponse.respond(IAccountServiceResponse.RES_DISCONNECTED, getServiceId());
            } else {
                serviceResponse.respond(IAccountServiceResponse.RES_DISCONNECTED, getServiceId(), e.getLocalizedMessage());
            }
        } catch (ProtocolException e1) {
            log(e1);
        }
    }

    @Override
    public void reconnectingIn(int seconds) {
        log("Connection reconnect " + getJID() + " in " + seconds);
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_CONNECTING, serviceId, 1);
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    public void reconnectionSuccessful() {
        log("Reconnected " + getJID());
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_CONNECTED, getServiceId());
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    public void reconnectionFailed(Exception e) {
        log("Reconnection failed " + getJID());
        connectionClosedOnError(e);
    }

    @Override
    public void cancelledNotification(String from, String packetID) {
    }

    @Override
    public void composingNotification(String from, String packetID) {
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_TYPING, getServiceId(), XMPPEntityAdapter.normalizeJID(from));
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    public void deliveredNotification(String from, String packetID) {
        long messageId = Long.parseLong(packetID);
        log(getJID() + " - " + from + " delivered " + messageId);
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_MESSAGEACK, getServiceId(), XMPPEntityAdapter.normalizeJID(from), messageId, 2);
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    public void displayedNotification(String from, String packetID) {
    }

    @Override
    public void offlineNotification(String from, String packetID) {
        long messageId = Long.parseLong(packetID);
        log(getJID() + " - " + from + " delivered " + messageId);
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_MESSAGEACK, getServiceId(), XMPPEntityAdapter.normalizeJID(from), messageId, 1);
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    public void stateChanged(Chat chat, ChatState state) {
        if (state == ChatState.composing) {
            try {
                serviceResponse.respond(IAccountServiceResponse.RES_TYPING, getServiceId(), XMPPEntityAdapter.normalizeJID(chat.getParticipant()));
            } catch (ProtocolException e) {
                log(e);
            }
        }
    }

    private void configure(ProviderManager pm) {
        try {
            pm.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
        }
        pm.addExtensionProvider("addresses", "http://jabber.org/protocol/address", new MultipleAddressesProvider());
        pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
        pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
        pm.addIQProvider("query", "jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
        try {
            pm.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            log(e);
        }
        pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());
        pm.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());
        pm.addExtensionProvider("x", "jabber:x:event", new MessageEventProvider());
        pm.addExtensionProvider("active", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addIQProvider("si", "http://jabber.org/protocol/si", new StreamInitiationProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        pm.addIQProvider("open", "http://jabber.org/protocol/ibb", new OpenIQProvider());
        pm.addIQProvider("close", "http://jabber.org/protocol/ibb", new CloseIQProvider());
        pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb", new DataPacketProvider());
        pm.addExtensionProvider("x", "jabber:x:conference", new GroupChatInvitation.Provider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
        pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
        pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user", new MUCUserProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
        pm.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());
        pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());
        pm.addIQProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
        pm.addExtensionProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
        pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());
        pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        pm.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
    }

    private void notification(String text) {
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_NOTIFICATION, getServiceId(), text);
        } catch (ProtocolException e) {
            log(e);
        }
    }

    @Override
    protected void keepaliveRequest() {
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
        try {
            DiscoverItems discoItems = discoManager.discoverItems(serviceName);
            resetHeartbeat();
            Iterator<Item> it = discoItems.getItems();
            while (it.hasNext()) {
                DiscoverItems.Item item = (DiscoverItems.Item) it.next();
                System.out.println(item.getEntityID());
                System.out.println(item.getNode());
                System.out.println(item.getName());
            }
        } catch (XMPPException e) {
            log(e);
        }
    }

    @Override
    public void fileTransferRequest(FileTransferRequest request) {
        log("incoming file " + request.getFileName() + " from " + request.getRequestor());
        fileTransfers.add(request);
        FileMessage fm = new FileMessage(XMPPEntityAdapter.normalizeJID(request.getRequestor()));
        FileInfo fi = new FileInfo();
        fi.filename = request.getFileName();
        fi.size = request.getFileSize();
        fm.files.add(fi);
        fm.serviceId = serviceId;
        fm.messageId = request.hashCode();
        try {
            serviceResponse.respond(IAccountServiceResponse.RES_FILEMESSAGE, getServiceId(), fm);
        } catch (ProtocolException e) {
            log(e);
        }
    }
}
