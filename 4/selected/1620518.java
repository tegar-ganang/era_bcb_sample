package net.sf.babble;

import junit.framework.*;
import net.sf.babble.ChannelMode;
import net.sf.babble.EventManager;
import net.sf.babble.Listener;
import net.sf.babble.ModeAction;
import net.sf.babble.ReplyCode;
import net.sf.babble.StatsQuery;
import net.sf.babble.UserMode;
import net.sf.babble.events.*;

/**
 *
 * @author speakmon
 */
public class ListenerTest extends TestCase implements ActionEventHandler, AdminEventHandler, AwayEventHandler, ChannelListEventHandler, ChannelModeChangeEventHandler, ChannelModeRequestEventHandler, DisconnectedEventHandler, DisconnectingEventHandler, ErrorMessageEventHandler, InfoEventHandler, InviteEventHandler, InviteSentEventHandler, IsonEventHandler, JoinEventHandler, KickEventHandler, LinksEventHandler, ListEventHandler, LusersEventHandler, MotdEventHandler, NamesEventHandler, NickErrorEventHandler, NickEventHandler, PartEventHandler, PingEventHandler, PrivateActionEventHandler, PrivateMessageEventHandler, PrivateNoticeEventHandler, PublicMessageEventHandler, PublicNoticeEventHandler, QuitEventHandler, RawMessageReceivedEventHandler, RawMessageSentEventHandler, RegisteredEventHandler, ReplyEventHandler, StatsEventHandler, TimeEventHandler, TopicEventHandler, TopicRequestEventHandler, UserModeChangeEventHandler, UserModeRequestEventHandler, VersionEventHandler, WhoEventHandler, WhoisEventHandler, WhowasEventHandler {

    private EventManager eventManager;

    private Listener listener;

    private String[] testLines;

    public ListenerTest(java.lang.String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ListenerTest.class);
        return suite;
    }

    protected void setUp() {
        eventManager = EventManager.getInstance();
        listener = new Listener();
        registerEventHandlers();
        assignLines();
    }

    protected void tearDown() {
    }

    private void registerEventHandlers() {
        eventManager.addActionEventHandler(this);
        eventManager.addAdminEventHandler(this);
        eventManager.addAwayEventHandler(this);
        eventManager.addChannelListEventHandler(this);
        eventManager.addChannelModeChangeEventHandler(this);
        eventManager.addChannelModeRequestEventHandler(this);
        eventManager.addDisconnectedEventHandler(this);
        eventManager.addDisconnectingEventHandler(this);
        eventManager.addErrorMessageEventHandler(this);
        eventManager.addInfoEventHandler(this);
        eventManager.addInviteEventHandler(this);
        eventManager.addInviteSentEventHandler(this);
        eventManager.addIsonEventHandler(this);
        eventManager.addJoinEventHandler(this);
        eventManager.addKickEventHandler(this);
        eventManager.addLinksEventHandler(this);
        eventManager.addListEventHandler(this);
        eventManager.addLusersEventHandler(this);
        eventManager.addMotdEventHandler(this);
        eventManager.addNamesEventHandler(this);
        eventManager.addNickErrorEventHandler(this);
        eventManager.addNickEventHandler(this);
        eventManager.addPartEventHandler(this);
        eventManager.addPingEventHandler(this);
        eventManager.addPrivateActionEventHandler(this);
        eventManager.addPrivateMessageEventHandler(this);
        eventManager.addPrivateNoticeEventHandler(this);
        eventManager.addPublicMessageEventHandler(this);
        eventManager.addPublicNoticeEventHandler(this);
        eventManager.addQuitEventHandler(this);
        eventManager.addRawMessageReceivedEventHandler(this);
        eventManager.addRawMessageSentEventHandler(this);
        eventManager.addRegisteredEventHandler(this);
        eventManager.addReplyEventHandler(this);
        eventManager.addStatsEventHandler(this);
        eventManager.addTimeEventHandler(this);
        eventManager.addTopicEventHandler(this);
        eventManager.addTopicRequestEventHandler(this);
        eventManager.addUserModeChangeEventHandler(this);
        eventManager.addUserModeRequestEventHandler(this);
        eventManager.addVersionEventHandler(this);
        eventManager.addWhoEventHandler(this);
        eventManager.addWhoisEventHandler(this);
        eventManager.addWhowasEventHandler(this);
    }

    private void assignLines() {
        testLines = new String[] { ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net PRIVMSG #sharktest :ACTION Looks around", ":irc.sventech.com 301 alphaX234 Scurvy :is away: (Be right back) [BX-MsgLog On]", ":irc.sventech.com 401 alphaX234 foxtrot :No such nick/channel", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net INVITE alphaX234 :#sharktest", ":irc.sventech.com 341 alphaX234 Scurvy #sharktest", ":irc.sventech.com 303 alphaX234 :Scurvy", ":alphaX234!~alphaX234@pcp825822pcs.nrockv01.md.comcast.net JOIN :#sharktest", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net KICK #sharktest alphaX234 :Go away", ":irc.sventech.com 322 alphaX234 #sun 17 :Send criticisms of new sun.com website to magellan-questions@sun.com - like http://wwws.sun.com/software/star/gnome/jtf/ is grossly wrong ?", ":irc.sventech.com 353 alphaX234 = #sharktest :alphaX234 @Scurvy", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net NICK :MudBall", ":irc.sventech.com 433 alphaX234 Scurvy :Nickname is already in use.", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net PART #sharktest :Later all", "PING :irc.sventech.com", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net PRIVMSG alphaX234 :H�tte ich das fr�her gewusst, m�sste ich jetzt wohl nicht das �l wechseln. Schei�e", ":Scurvyaaaaaaaaa!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net NOTICE alphaX234 :Private notice", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net PRIVMSG #sharktest :  Test  of public-message!", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net NOTICE #sharktest :public notice", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net QUIT :Bye all", ":irc.sventech.com 001 alphaX234 :Welcome to IRC", ":washington.dc.us.undernet.org 381 EchoBot PREFIX=(ov)@+ CHANMODES=b,k,l,imnpst CHARSET=rfc1459 NETWORK=Undernet :are supported by this server", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net TOPIC #sharktest :A new topic", ":irc.sventech.com 332 alphaX234 #sharktest :A new topic", ":irc.sventech.com 352 alphaX234 #sharktest ~Scurvy pcp825822pcs.nrockv01.md.comcast.net irc.sventech.com Scurvy H@ :0 Scurvy", ":irc.sventech.com 311 alphaX234 Scurvy ~Scurvy pcp825822pcs.nrockv01.md.comcast.net * :Scurvy", ":irc.sventech.com 319 alphaX234 Scurvy :@#sharktest", ":irc.sventech.com 312 alphaX234 Scurvy irc.sventech.com :GIMPnet IRC Server", ":irc.sventech.com 317 alphaX234 Scurvy 0 1018611059 :seconds idle, signon time", ":irc.sventech.com 318 alphaX234 Scurvy :End of /WHOIS list.", ":irc.sventech.com 314 alphaX234 Scurvy ~Scurvy pcp825822pcs.nrockv01.md.comcast.net * :Scurvy", ":deltabot MODE deltabot :+w", ":irc.sventech.com 221 deltabot +iw", ":irc.sventech.com 324 deltabot #sharktest +tpl 10", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net MODE #sharktest +b-i *!*@* +e *!*@*.fi", ":irc.sventech.com 367 deltabot #sharktest jocko!*@* Scurvy 120192129", ":Scurvy!~Scurvy@pcp825822pcs.nrockv01.md.comcast.net PRIVMSG nick :ACTION Looks around", ":irc.sventech.com 351 alphaX234 IRC: 1.203b ircd :some great comments", ":my.server.name 372 EchoBot :- Sunray is the sharkbite.org test server.", ":my.server.name 391 EchoBot :Sunray : 25 December 2002", ":my.server.name 371 EchoBot :A great piece of software", ":Sunray 256 EchoBot Sunray :Administrative info", ":NuclearFallout.CA.US.GamesNET.net 252 EchoBot 47 :operator(s) online", ":ircd.gimp.org 364 EchoBot irc.sventech.com irc.acc.umu.se :2 GIMPnet IRC Server", ":ircd.gimp.org 242 EchoBot :Server Up 46 days, 8:47:20" };
    }

    public void testParse() {
        for (String line : testLines) {
            listener.parse(line);
        }
    }

    public void onAction(ActionEvent actionEvent) {
        System.out.println("onAction");
        assertEquals("onAction(): userInfo.getNick()", "Scurvy", actionEvent.getUserInfo().getNick());
        assertEquals("onAction(): userInfo.getUser()", "~Scurvy", actionEvent.getUserInfo().getUser());
        assertEquals("onAction(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", actionEvent.getUserInfo().getHostName());
        assertEquals("onAction(): channel", "#sharktest", actionEvent.getChannel());
        assertEquals("onAction(): description", "Looks around", actionEvent.getDescription());
    }

    public void onAdmin(AdminEvent adminEvent) {
        System.out.println("onAdmin");
        assertEquals("onAdmin(): message", "Sunray :Administrative info", adminEvent.getMessage());
    }

    public void onAway(AwayEvent awayEvent) {
        System.out.println("onAway");
        assertEquals("onAway(): user", "Scurvy", awayEvent.getNick());
        assertEquals("onAway(): message", "is away: (Be right back) [BX-MsgLog On]", awayEvent.getAwayMessage());
    }

    public void onChannelList(ChannelListEvent channelListEvent) {
        System.out.println("onChannelList");
        assertEquals("onChannelList(): channel", "#sharktest", channelListEvent.getChannel());
        assertEquals("onChannelList(): mode", ChannelMode.BAN, channelListEvent.getChannelMode());
        assertEquals("onChannelList(): item", "jocko!*@*", channelListEvent.getItem());
        assertEquals("onChannelList(): who", "Scurvy", channelListEvent.getUserInfo().getNick());
        assertTrue("onChanneList(): when", 120192129 == channelListEvent.getWhenSet());
        assertTrue("onChannelList(): last", !channelListEvent.isLast());
    }

    public void onChannelModeChange(ChannelModeChangeEvent channelModeChangeEvent) {
        assertEquals("onChannelModeChange(): userInfo.getNick()", "Scurvy", channelModeChangeEvent.getUserInfo().getNick());
        assertEquals("onChannelModeChange(): userInfo.User", "~Scurvy", channelModeChangeEvent.getUserInfo().getUser());
        assertEquals("onChannelModeChange(): userInfo.Host", "pcp825822pcs.nrockv01.md.comcast.net", channelModeChangeEvent.getUserInfo().getHostName());
        assertEquals("onChannelModeChange(): channel", "#sharktest", channelModeChangeEvent.getChannel());
        assertEquals("onChannelModeChange(): action", ModeAction.ADD, channelModeChangeEvent.getModes()[0].getAction());
        assertEquals("onChannelModeChange(): size", 3, channelModeChangeEvent.getModes().length);
        assertEquals("onChannelModeChange(): ban", ChannelMode.BAN, channelModeChangeEvent.getModes()[0].getMode());
        assertEquals("onChannelModeChange(): param", "*!*@*", channelModeChangeEvent.getModes()[0].getParameter());
        assertEquals("onChannelModeChange(): invite action", ModeAction.REMOVE, channelModeChangeEvent.getModes()[1].getAction());
        assertEquals("onChannelModeChange(): invite mode", ChannelMode.INVITE_ONLY, channelModeChangeEvent.getModes()[1].getMode());
        assertEquals("onChannelModeChange(): exception action", ModeAction.ADD, channelModeChangeEvent.getModes()[2].getAction());
        assertEquals("onChannelModeChange(): exception mode", ChannelMode.EXCEPTION, channelModeChangeEvent.getModes()[2].getMode());
        assertEquals("onChannelModeChange(): exception param", "*!*@*.fi", channelModeChangeEvent.getModes()[2].getParameter());
    }

    public void onChannelModeRequest(ChannelModeRequestEvent channelModeRequestEvent) {
        System.out.println("onChannelModeRequest");
        assertEquals("onChannelModeRequest(): channel", "#sharktest", channelModeRequestEvent.getChannel());
        assertEquals("onChannelModeRequest(): size", 3, channelModeRequestEvent.getModes().length);
        assertEquals("onChannelModeRequest(): topic", ChannelMode.TOPIC_SETTABLE, channelModeRequestEvent.getModes()[0].getMode());
        assertEquals("onChannelModeRequest(): private", ChannelMode.PRIVATE, channelModeRequestEvent.getModes()[1].getMode());
        assertEquals("onChannelModeRequest(): limited", ChannelMode.USER_LIMIT, channelModeRequestEvent.getModes()[2].getMode());
        assertEquals("onChannelModeRequest(): param", "10", channelModeRequestEvent.getModes()[2].getParameter());
    }

    public void onDisconnected(DisconnectedEvent disconnectedEvent) {
    }

    public void onDisconnecting(DisconnectingEvent disconnectingEvent) {
    }

    public void onError(ErrorMessageEvent errorMessageEvent) {
        System.out.println("onError");
        assertEquals("onError(): message", "foxtrot :No such nick/channel", errorMessageEvent.getMessage());
        assertEquals("onError(): code", ReplyCode.ERR_NOSUCHNICK, errorMessageEvent.getCode());
    }

    public void onInfo(InfoEvent infoEvent) {
        System.out.println("onInfo");
        assertEquals("onInfo(): message", "A great piece of software", infoEvent.getMessage());
        assertTrue("onInfo(): done", !infoEvent.isLast());
    }

    public void onInvite(InviteEvent inviteEvent) {
        System.out.println("onInvite");
        assertEquals("onInvite(): userInfo.getNick()", "Scurvy", inviteEvent.getUser().getNick());
        assertEquals("onInvite(): userInfo.getUser()", "~Scurvy", inviteEvent.getUser().getUser());
        assertEquals("onInvite(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", inviteEvent.getUser().getHostName());
        assertEquals("onInvite(): channel", "#sharktest", inviteEvent.getChannel());
    }

    public void onInviteSent(InviteSentEvent inviteSentEvent) {
        System.out.println("onInviteSent");
        assertEquals("onInviteSent(): user", "Scurvy", inviteSentEvent.getNick());
        assertEquals("onInviteSent(): channel", "#sharktest", inviteSentEvent.getChannel());
    }

    public void onIson(IsonEvent isonEvent) {
        System.out.println("onIson");
        assertEquals("onIson(): nick", "Scurvy", isonEvent.getNick());
    }

    public void onJoin(JoinEvent joinEvent) {
        System.out.println("onJoin");
        assertEquals("onJoin(): userInfo.getNick()", "alphaX234", joinEvent.getUser().getNick());
        assertEquals("onJoin(): userInfo.getUser()", "~alphaX234", joinEvent.getUser().getUser());
        assertEquals("onJoin(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", joinEvent.getUser().getHostName());
        assertEquals("onJoin(): channel", "#sharktest", joinEvent.getChannel());
    }

    public void onKick(KickEvent kickEvent) {
        System.out.println("onKick");
        assertEquals("onKick(): userInfo.getNick()", "Scurvy", kickEvent.getUser().getNick());
        assertEquals("onKick(): userInfo.getUser()", "~Scurvy", kickEvent.getUser().getUser());
        assertEquals("onKick(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", kickEvent.getUser().getHostName());
        assertEquals("onKick(): channel", "#sharktest", kickEvent.getChannel());
        assertEquals("onKick(): kickee", "alphaX234", kickEvent.getKickee());
        assertEquals("onKick(): reason", "Go away", kickEvent.getReason());
    }

    public void onLinks(LinksEvent linksEvent) {
        System.out.println("onLinks");
        assertEquals("onLusers(): mask", "irc.sventech.com", linksEvent.getMask());
        assertEquals("onLusers(): hostname", "irc.acc.umu.se", linksEvent.getHostname());
        assertEquals("onLusers(): hopcount", 2, linksEvent.getHopCount());
        assertEquals("onLusers(): server info", "GIMPnet IRC Server", linksEvent.getServerInfo());
        assertTrue("onLusers(): done", !linksEvent.isDone());
    }

    public void onList(ListEvent listEvent) {
        System.out.println("onList");
        assertEquals("onList(): channel", "#sun", listEvent.getChannel());
        assertEquals("onList(): count", 17, listEvent.getVisibleNickCount());
        assertEquals("onList(): topic", "Send criticisms of new sun.com website to magellan-questions@sun.com - like http://wwws.sun.com/software/star/gnome/jtf/ is grossly wrong ?", listEvent.getTopic());
        assertTrue("onList(): done", !listEvent.isLast());
    }

    public void onLusers(LusersEvent lusersEvent) {
        System.out.println("onLusers");
        assertEquals("onLusers(): message", "47 :operator(s) online", lusersEvent.getMessage());
    }

    public void onMotd(MotdEvent motdEvent) {
        System.out.println("onMotd");
        assertEquals("onMotd(): message", "- Sunray is the sharkbite.org test server.", motdEvent.getMessage());
        assertFalse("onMotd(): last ", motdEvent.isLast());
    }

    public void onNames(NamesEvent namesEvent) {
        System.out.println("onNames");
        assertEquals("onNames(): channel", "#sharktest", namesEvent.getChannel());
        assertEquals("onNames(): nick[0]", "alphaX234", namesEvent.getNicks()[0]);
        assertEquals("onNames(): nick[1]", "@Scurvy", namesEvent.getNicks()[1]);
        assertTrue("onNames(): done", !namesEvent.isLast());
    }

    public void onNick(NickEvent nickEvent) {
        System.out.println("onNick");
        assertEquals("onNick(): userInfo.getNick()", "Scurvy", nickEvent.getUser().getNick());
        assertEquals("onNick(): userInfo.getUser()", "~Scurvy", nickEvent.getUser().getUser());
        assertEquals("onNick(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", nickEvent.getUser().getHostName());
        assertEquals("onNick(): newNick", "MudBall", nickEvent.getNewNick());
    }

    public void onNickError(NickErrorEvent nickErrorEvent) {
        ;
        System.out.println("onNickError");
        assertEquals("onNickError(): bad nick", "Scurvy", nickErrorEvent.getBadNick());
        assertEquals("onNickError(): reason", "Nickname is already in use.", nickErrorEvent.getReason());
    }

    public void onPart(PartEvent partEvent) {
        System.out.println("onPart");
        assertEquals("onPart(): userInfo.getNick()", "Scurvy", partEvent.getUser().getNick());
        assertEquals("onPart(): userInfo.getUser()", "~Scurvy", partEvent.getUser().getUser());
        assertEquals("onPart(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", partEvent.getUser().getHostName());
        assertEquals("onPart(): channel", "#sharktest", partEvent.getChannel());
        assertEquals("onPart(): reason", "Later all", partEvent.getReason());
    }

    public void onPing(PingEvent pingEvent) {
        System.out.println("onPing");
        assertEquals("onPing(): message", "irc.sventech.com", pingEvent.getMessage());
    }

    public void onPrivate(PrivateMessageEvent privateMessageEvent) {
        System.out.println("onPrivate");
        assertEquals("onPrivate(): userInfo.getNick()", "Scurvy", privateMessageEvent.getUser().getNick());
        assertEquals("onPrivate(): userInfo.getUser()", "~Scurvy", privateMessageEvent.getUser().getUser());
        assertEquals("onPrivate(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", privateMessageEvent.getUser().getHostName());
        assertEquals("onPrivate(): message", "H�tte ich das fr�her gewusst, m�sste ich jetzt wohl nicht das �l wechseln. Schei�e", privateMessageEvent.getMessage());
    }

    public void onPrivateAction(PrivateActionEvent privateActionEvent) {
        System.out.println("onPrivateAction");
        assertEquals("onPrivateAction(): userInfo.nick", "Scurvy", privateActionEvent.getUser().getNick());
        assertEquals("onPrivateAction(): userInfo.user", "~Scurvy", privateActionEvent.getUser().getUser());
        assertEquals("onPrivateAction(): userInfo.host", "pcp825822pcs.nrockv01.md.comcast.net", privateActionEvent.getUser().getHostName());
        assertEquals("onPrivateAction(): message", "Looks around", privateActionEvent.getDescription());
    }

    public void onPrivateNotice(PrivateNoticeEvent privateNoticeEvent) {
        System.out.println("onPrivateNotice");
        assertEquals("onPrivateNotice(): userInfo.getNick()", "Scurvyaaaaaaaaa", privateNoticeEvent.getUser().getNick());
        assertEquals("onPrivateNotice(): userInfo.getUser()", "~Scurvy", privateNoticeEvent.getUser().getUser());
        assertEquals("onPrivateNotice(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", privateNoticeEvent.getUser().getHostName());
        assertEquals("onPrivateNotice(): notice", "Private notice", privateNoticeEvent.getNotice());
    }

    public void onPublic(PublicMessageEvent publicMessageEvent) {
        System.out.println("onPublic");
        assertEquals("onPublic(): userInfo.getNick()", "Scurvy", publicMessageEvent.getUser().getNick());
        assertEquals("onPublic(): userInfo.getUser()", "~Scurvy", publicMessageEvent.getUser().getUser());
        assertEquals("onPublic(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", publicMessageEvent.getUser().getHostName());
        assertEquals("onPublic(): channel", "#sharktest", publicMessageEvent.getChannel());
        assertEquals("onPublic(): message", "  Test  of public-message!", publicMessageEvent.getMessage());
    }

    public void onPublicNotice(PublicNoticeEvent publicNoticeEvent) {
        System.out.println("onPublicNotice");
        assertEquals("onPublicNotice(): userInfo.getNick()", "Scurvy", publicNoticeEvent.getUser().getNick());
        assertEquals("onPublicNotice(): userInfo.getUser()", "~Scurvy", publicNoticeEvent.getUser().getUser());
        assertEquals("onPublicNotice(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", publicNoticeEvent.getUser().getHostName());
        assertEquals("onPublicNotice(): channel", "#sharktest", publicNoticeEvent.getChannel());
        assertEquals("onPublicNotice(): notice", "public notice", publicNoticeEvent.getNotice());
    }

    public void onQuit(QuitEvent quitEvent) {
        System.out.println("onQuit");
        assertEquals("onQuit(): userInfo.getNick()", "Scurvy", quitEvent.getUser().getNick());
        assertEquals("onQuit(): userInfo.getUser()", "~Scurvy", quitEvent.getUser().getUser());
        assertEquals("onQuit(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", quitEvent.getUser().getHostName());
        assertEquals("onQuit(): reason", "Bye all", quitEvent.getReason());
    }

    public void onRawMessageReceived(RawMessageReceivedEvent rawMessageReceivedEvent) {
    }

    public void onRawMessageSent(RawMessageSentEvent rawMessageSentEvent) {
    }

    public void onRegistered(RegisteredEvent registeredEvent) {
        System.out.println("onRegistered");
        assertTrue(true);
    }

    public void onReply(ReplyEvent replyEvent) {
        System.out.println("onReply");
        assertEquals("onReply(): code", ReplyCode.RPL_YOUREOPER, replyEvent.getCode());
        assertEquals("onReply(): message", "PREFIX=(ov)@+ CHANMODES=b,k,l,imnpst CHARSET=rfc1459 NETWORK=Undernet :are supported by this server", replyEvent.getMessage());
    }

    public void onStats(StatsEvent statsEvent) {
        System.out.println("onStats");
        assertEquals("onStats(): queryType", StatsQuery.UPTIME, statsEvent.getQueryType());
        assertEquals("onStats(): message", "Server Up 46 days, 8:47:20", statsEvent.getMessage());
        assertTrue("onStats(): done", !statsEvent.isDone());
    }

    public void onTime(TimeEvent timeEvent) {
        System.out.println("onTime");
        assertEquals("onTime(): time", "Sunray : 25 December 2002", timeEvent.getTime());
    }

    public void onTopicChanged(TopicEvent topicEvent) {
        System.out.println("onTopicChanged");
        assertEquals("onTopicChanged(): userInfo.getNick()", "Scurvy", topicEvent.getUser().getNick());
        assertEquals("onTopicChanged(): userInfo.getUser()", "~Scurvy", topicEvent.getUser().getUser());
        assertEquals("onTopicChanged(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", topicEvent.getUser().getHostName());
        assertEquals("onTopicChanged(): channel", "#sharktest", topicEvent.getChannel());
        assertEquals("onTopicChanged(): new topic", "A new topic", topicEvent.getNewTopic());
    }

    public void onTopicRequest(TopicRequestEvent topicRequestEvent) {
        System.out.println("onTopicRequest");
        assertEquals("onTopicRequest(): channel", "#sharktest", topicRequestEvent.getChannel());
        assertEquals("onTopicRequest(): topic", "A new topic", topicRequestEvent.getTopic());
    }

    public void onUserModeChange(UserModeChangeEvent userModeChangeEvent) {
        System.out.println("onUserModeChange");
        assertEquals("onUserModeChange(): action", ModeAction.ADD, userModeChangeEvent.getAction());
        assertEquals("onUserModeChange(): mode", UserMode.WALLOPS, userModeChangeEvent.getMode());
    }

    public void onUserModeRequest(UserModeRequestEvent userModeRequestEvent) {
        System.out.println("onUserModeRequest");
        assertEquals("onUserModeRequest(): size", 2, userModeRequestEvent.getModes().length);
        assertEquals("onUserModeRequest(): invisible", UserMode.INVISIBLE, userModeRequestEvent.getModes()[0]);
        assertEquals("onUserModeReqeust(): wallops", UserMode.WALLOPS, userModeRequestEvent.getModes()[1]);
    }

    public void onVersion(VersionEvent versionEvent) {
        System.out.println("onVersion");
        assertEquals("onVersion(): versionInfo", "IRC: 1.203b ircd :some great comments", versionEvent.getVersionInfo());
    }

    public void onWho(WhoEvent whoEvent) {
        System.out.println("onWho");
        assertEquals("onWho(): userInfo.getNick()", "Scurvy", whoEvent.getUser().getNick());
        assertEquals("onWho(): userInfo.getUser()", "~Scurvy", whoEvent.getUser().getUser());
        assertEquals("onWho(): userInfo.getHost()", "pcp825822pcs.nrockv01.md.comcast.net", whoEvent.getUser().getHostName());
        assertEquals("onWho(): channel", "#sharktest", whoEvent.getChannel());
        assertEquals("onWho(): ircServer", "irc.sventech.com", whoEvent.getIrcServer());
        assertEquals("onWho(): mask", "H@", whoEvent.getMask());
        assertEquals("onWho(): hopCount", 0, whoEvent.getHopCount());
        assertEquals("onWho(): real name", "Scurvy", whoEvent.getRealName());
        assertTrue("onWho(): last", !whoEvent.isLast());
    }

    public void onWhois(WhoisEvent whoisEvent) {
        System.out.println("onWhois");
        assertEquals("onWhois(): userInfo.nick", "Scurvy", whoisEvent.getWhoisInfo().getUserInfo().getNick());
        assertEquals("onWhois(): userInfo.user", "~Scurvy", whoisEvent.getWhoisInfo().getUserInfo().getUser());
        assertEquals("onWhois(): userInfo.host", "pcp825822pcs.nrockv01.md.comcast.net", whoisEvent.getWhoisInfo().getUserInfo().getHostName());
        assertEquals("onWhois(): channels", "@#sharktest", whoisEvent.getWhoisInfo().getChannels()[0]);
        assertTrue("onWhois(): idletime", 1018611059 == whoisEvent.getWhoisInfo().getIdleTime());
        assertTrue("onWhois(): operator", !whoisEvent.getWhoisInfo().isOperator());
        assertEquals("onWhois(): real name", "Scurvy", whoisEvent.getWhoisInfo().getRealName());
        assertEquals("onWhois(): server", "irc.sventech.com", whoisEvent.getWhoisInfo().getServer());
        assertEquals("onWhois(): server description", "GIMPnet IRC Server", whoisEvent.getWhoisInfo().getServerDescription());
    }

    public void onWhowas(WhowasEvent whowasEvent) {
        System.out.println("onWhowas");
        assertEquals("onWhowas(): userInfo.nick", "Scurvy", whowasEvent.getUser().getNick());
        assertEquals("onWhowas(): userInfo.user", "~Scurvy", whowasEvent.getUser().getUser());
        assertEquals("onWhowas(): userInfo.host", "pcp825822pcs.nrockv01.md.comcast.net", whowasEvent.getUser().getHostName());
        assertEquals("onWhowas(): realName", "Scurvy", whowasEvent.getRealName());
        assertTrue("onWhowas(): last", !whowasEvent.isLast());
    }
}
